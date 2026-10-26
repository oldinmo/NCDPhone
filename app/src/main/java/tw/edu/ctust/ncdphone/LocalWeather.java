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

/** User-authorized foreground weather lookup; MET receives coordinates rounded to two decimals. */
final class LocalWeather {
    interface Listener {void changed(Snapshot value,String status);}
    static final class Snapshot {
        final String district,symbol;final double temperature;final long forecastAt,fetchedAt;
        Snapshot(String d,String s,double t,long at,long fetched){district=d;symbol=s;temperature=t;forecastAt=at;fetchedAt=fetched;}
        String json()throws JSONException{return new JSONObject().put("district",district).put("symbol",symbol).put("temp",temperature).put("at",forecastAt).put("fetched",fetchedAt).toString();}
        static Snapshot parse(String text)throws JSONException{JSONObject o=new JSONObject(text);return new Snapshot(o.getString("district"),o.getString("symbol"),o.getDouble("temp"),o.getLong("at"),o.getLong("fetched"));}
    }
    final Context app;final Handler main=new Handler(Looper.getMainLooper());final ExecutorService workers=Executors.newCachedThreadPool();
    Listener listener;Snapshot cached;boolean active,busy;int generation;long nextAttempt;
    LocationListener locationListener;
    LocalWeather(Context c){app=c.getApplicationContext();try{cached=Snapshot.parse(Store.prefs(app).getString("weatherCache",""));}catch(Exception ignored){}nextAttempt=Store.prefs(app).getInt("weatherFormat",0)==1?Store.prefs(app).getLong("weatherExpires",0):0;}
    void start(Listener l){listener=l;active=true;refresh();}
    void stop(){active=false;listener=null;generation++;busy=false;stopLocation();main.removeCallbacksAndMessages(null);}
    void destroy(){stop();workers.shutdownNow();}
    boolean permission(){return app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    void emit(String status){if(active&&listener!=null)listener.changed(cached,status);}
    void refresh(){
        if(!active)return;
        if(!permission()){emit("點此允許定位顯示氣象");return;}
        if(cached!=null&&System.currentTimeMillis()-cached.forecastAt>6*3600000L)cached=null;
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
                String district=district(location);
                if(!active||token!=generation)return;
                String key=String.format(Locale.US,"%.2f,%.2f",location.getLatitude(),location.getLongitude());String[] xy=key.split(",");
                URL url=new URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat="+xy[0]+"&lon="+xy[1]);
                HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(12000);connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent","NCDPhone/"+BuildConfig.VERSION_NAME+" github.com/oldinmo/NCDPhone");
                String modified=Store.prefs(app).getString("weatherModified","");String oldKey=Store.prefs(app).getString("weatherGrid","");
                if(key.equals(oldKey)&&!modified.isEmpty())connection.setRequestProperty("If-Modified-Since",modified);
                String body;long expires;String lastModified;
                try{
                    long storedExpiry=Store.prefs(app).getLong("weatherExpires",0);
                    boolean freshResponse=key.equals(oldKey)&&storedExpiry>System.currentTimeMillis()&&!Store.prefs(app).getString("weatherResponse","").isEmpty();
                    int code=freshResponse?304:connection.getResponseCode();expires=freshResponse?storedExpiry:Math.max(System.currentTimeMillis()+15*60000L+(long)(Math.random()*120000),connection.getHeaderFieldDate("Expires",0));lastModified=freshResponse?modified:connection.getHeaderField("Last-Modified");
                    if(code==304)body=Store.prefs(app).getString("weatherResponse","");
                    else if(code==200||code==203){try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>524288)throw new IOException("Response too large");out.write(buffer,0,n);}body=out.toString(StandardCharsets.UTF_8.name());}}
                    else throw new IOException("Weather HTTP "+code);
                }finally{connection.disconnect();}
                Snapshot value=parseForecast(body,district,System.currentTimeMillis());
                final String response=body,modifiedValue=lastModified;
                main.post(()->{if(!active||token!=generation)return;cached=value;busy=false;nextAttempt=expires;try{Store.prefs(app).edit().putInt("weatherFormat",1).putString("weatherCache",value.json()).putString("weatherResponse",response).putString("weatherGrid",key).putString("weatherModified",modifiedValue==null?modified:modifiedValue).putLong("weatherExpires",expires).apply();}catch(JSONException ignored){}emit("");});
            }catch(Exception e){main.post(()->failed(token,"氣象更新失敗，請確認網路"));}
        });
    }
    static Snapshot parseForecast(String body,String district,long now)throws JSONException {
        JSONArray series=new JSONObject(body).getJSONObject("properties").getJSONArray("timeseries");JSONObject chosen=null;long at=0;
        for(int i=0;i<series.length();i++){JSONObject point=series.getJSONObject(i);long time=Instant.parse(point.getString("time")).toEpochMilli();if(chosen==null||time<=now){chosen=point;at=time;}if(time>now)break;}
        if(chosen==null||Math.abs(now-at)>6*3600000L)throw new JSONException("No current forecast");
        JSONObject data=chosen.getJSONObject("data"),period=data.optJSONObject("next_1_hours");if(period==null)period=data.optJSONObject("next_6_hours");
        String symbol=period==null?"unknown":period.getJSONObject("summary").getString("symbol_code");double temp=data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature");
        if(!Double.isFinite(temp)||temp < -100||temp>70)throw new JSONException("Invalid temperature");return new Snapshot(district,symbol,temp,at,now);
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
