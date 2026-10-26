package tw.edu.ctust.ncdphone;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.HttpsURLConnection;

/** Foreground CWA forecast lookup; no coordinates are sent to the weather provider. */
final class LocalWeather {
    interface Listener {void changed(Snapshot value,String status);}
    static final class Snapshot {
        final String district,symbol,description;final double temperature;final long forecastAt,fetchedAt;
        Snapshot(String d,String s,String desc,double t,long at,long fetched){district=d;symbol=s;description=desc;temperature=t;forecastAt=at;fetchedAt=fetched;}
        String json()throws JSONException{return new JSONObject().put("district",district).put("symbol",symbol).put("description",description).put("temp",temperature).put("at",forecastAt).put("fetched",fetchedAt).toString();}
        static Snapshot parse(String text)throws JSONException{JSONObject o=new JSONObject(text);return new Snapshot(o.getString("district"),o.getString("symbol"),o.getString("description"),o.getDouble("temp"),o.getLong("at"),o.getLong("fetched"));}
    }
    final Context app;final Handler main=new Handler(Looper.getMainLooper());final ExecutorService workers=Executors.newCachedThreadPool();
    Listener listener;Snapshot cached;volatile boolean active;boolean busy;volatile int generation;long nextAttempt;
    LocationListener locationListener;
    LocalWeather(Context c){app=c.getApplicationContext();try{cached=Snapshot.parse(Store.prefs(app).getString("cwaCache",""));}catch(Exception ignored){}nextAttempt=Store.prefs(app).getInt("cwaFormat",0)==1?Store.prefs(app).getLong("cwaExpires",0):0;}
    void start(Listener l){listener=l;active=true;refresh();}
    void stop(){active=false;listener=null;generation++;busy=false;stopLocation();main.removeCallbacksAndMessages(null);}
    void destroy(){stop();workers.shutdownNow();}
    boolean permission(){return app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    void emit(String status){if(active&&listener!=null)listener.changed(cached,status);}
    void refresh(){
        if(!active)return;
        if(!permission()){emit("點此允許定位顯示氣象");return;}
        if(cached!=null&&System.currentTimeMillis()-cached.forecastAt>3*3600000L)cached=null;
        if(busy){emit(cached==null?"正在更新氣象":"");return;}
        if(System.currentTimeMillis()<nextAttempt){emit(cached==null?"氣象暫無資料，稍後重試":"");return;}
        LocationManager lm=app.getSystemService(LocationManager.class);
        if(!lm.isLocationEnabled()){emit("請開啟手機定位");return;}
        busy=true;int token=++generation;emit(cached==null?"正在取得當地位置":"");
        try {
            Location best=null;
            for(String provider:lm.getProviders(true)){Location l=lm.getLastKnownLocation(provider);if(fresh(l)&&(best==null||l.getTime()>best.getTime()))best=l;}
            if(best!=null){fetch(best,token);return;}
            locationListener=new LocationListener(){@Override public void onLocationChanged(Location l){if(locationListener!=this||!active||token!=generation||!fresh(l))return;stopLocation();fetch(l,token);}@Override public void onProviderEnabled(String p){}@Override public void onProviderDisabled(String p){}@Override public void onStatusChanged(String p,int status,Bundle b){}};
            boolean requested=false;for(String provider:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER})if(lm.isProviderEnabled(provider)){lm.requestLocationUpdates(provider,1000,0,locationListener,Looper.getMainLooper());requested=true;}
            if(!requested){failed(token,"暫時無法取得位置");return;}
            main.postDelayed(()->{if(token==generation&&locationListener!=null){stopLocation();failed(token,"暫時無法取得位置");}},15000);
        }catch(SecurityException e){failed(token,"請允許位置權限");}
    }
    boolean fresh(Location l){return l!=null&&System.currentTimeMillis()-l.getTime()>=0&&System.currentTimeMillis()-l.getTime()<30*60000L;}
    void stopLocation(){if(locationListener!=null){try{app.getSystemService(LocationManager.class).removeUpdates(locationListener);}catch(SecurityException ignored){}locationListener=null;}}
    void failed(int token,String message){if(token!=generation||!active)return;stopLocation();busy=false;nextAttempt=System.currentTimeMillis()+5*60000L;emit(message);}
    void fetch(Location location,int token){
        stopLocation();workers.execute(()->{
            try{
                String area=CwaData.name(district(location));
                if(!active||token!=generation)return;
                String[] town=findTown(area);
                if(town==null){main.post(()->failed(token,"此行政區暫無官方氣象"));return;}
                URL url=new URL("https://www.cwa.gov.tw/Data/js/3hr/ChartData_3hr_T_"+town[0]+".js");
                HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(12000);connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent","NCDPhone/"+BuildConfig.VERSION_NAME+" github.com/oldinmo/NCDPhone");
                String body;
                try{if(connection.getResponseCode()!=200)throw new IOException("Weather unavailable");try(InputStream in=connection.getInputStream()){body=read(in,2097152);}}finally{connection.disconnect();}
                Snapshot value=parseForecast(body,town[1],town[2],System.currentTimeMillis());
                main.post(()->{if(!active||token!=generation)return;cached=value;busy=false;nextAttempt=System.currentTimeMillis()+30*60000L;try{Store.prefs(app).edit().putInt("cwaFormat",1).putString("cwaCache",value.json()).putLong("cwaExpires",nextAttempt).apply();}catch(JSONException ignored){}emit("");});
            }catch(Exception e){main.post(()->failed(token,"氣象更新失敗，請確認網路"));}
        });
    }
    static String read(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Response too large");out.write(buffer,0,n);}return out.toString(StandardCharsets.UTF_8.name());}
    String[] findTown(String area)throws Exception{
        String counties,towns;try(InputStream in=app.getAssets().open("cwa-counties.js")){counties=read(in,100000);}try(InputStream in=app.getAssets().open("cwa-towns.js")){towns=read(in,200000);}
        JSONArray cities=new JSONArray(CwaData.literal(counties,"Info_County"));JSONObject all=new JSONObject(CwaData.literal(towns,"Info_Town"));
        for(int i=0;i<cities.length();i++){JSONObject city=cities.getJSONObject(i);String cname=city.getJSONObject("Name").getString("C"),cid=city.getString("ID");JSONArray places=all.getJSONArray(cid);
            for(int j=0;j<places.length();j++){JSONObject place=places.getJSONObject(j);String full=cname+place.getJSONObject("Name").getString("C");if(CwaData.name(full).equals(area))return new String[]{cid,place.getString("ID"),full};}
        }return null;
    }
    static Snapshot parseForecast(String body,String town,String district,long now)throws JSONException {
        long updated=CwaData.updated(body);if(now-updated>6*3600000L||updated-now>3600000L)throw new JSONException("Stale weather feed");
        JSONArray times=new JSONObject(CwaData.literal(body,"Time_3hr")).getJSONArray("C");
        int selected=-1;long at=0;
        for(int i=0;i<times.length();i++){long t=CwaData.time(times.getString(i),updated);if(t<=now&&t>=at){selected=i;at=t;}}
        if(selected<0||now-at>3*3600000L)throw new JSONException("No current forecast");
        JSONObject data=new JSONObject(CwaData.literal(body,"TempArray_3hr")).getJSONObject(town);
        double temp=data.getJSONObject("C").getJSONArray("T").getDouble(selected);
        String desc=data.getJSONObject("Wx").getJSONArray("C").getJSONArray(selected).getString(1);
        if(!Double.isFinite(temp)||temp < -100||temp>70||desc.isEmpty())throw new JSONException("Invalid forecast");
        return new Snapshot(district,CwaData.symbol(desc,at),desc,temp,at,now);
    }
    String district(Location location){
        Future<String> f=workers.submit(()->{
            if(!Geocoder.isPresent())return "行政區暫無資料";
            List<Address> found=new Geocoder(app,Locale.TRADITIONAL_CHINESE).getFromLocation(location.getLatitude(),location.getLongitude(),1);
            if(found==null||found.isEmpty())return "行政區暫無資料";Address a=found.get(0);
            String area=WeatherKind.district(a.getSubLocality(),a.getLocality(),a.getSubAdminArea());
            String city=a.getAdminArea();if(city==null)city="";if(!area.isEmpty()&&!city.equals(area)&&!area.startsWith(city))return city+area;
            return area.isEmpty()?(city.isEmpty()?"行政區暫無資料":city):area;
        });
        try{return f.get(6,TimeUnit.SECONDS);}catch(Exception e){f.cancel(true);return "行政區暫無資料";}
    }
}
