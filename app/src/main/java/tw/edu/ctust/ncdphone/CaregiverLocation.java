package tw.edu.ctust.ncdphone;

import android.Manifest;
import android.content.*;
import android.location.*;
import android.os.*;
import android.telephony.SmsManager;
import java.util.*;

final class CaregiverLocation {
    static long lastSent=-300000;
    static boolean pending;
    static void status(Context c,String s){Store.prefs(c).edit().putString("smsStatus",s).apply();}
    static void incoming(Context c,String caller){
        Context app=c.getApplicationContext();String target=Store.prefs(c).getString("caregiver","");
        if(!Store.flag(c,"locationSms")||!Rules.samePhone(target,caller)||pending)return;
        long now=SystemClock.elapsedRealtime();if(now-lastSent<60000)return;
        if(c.checkSelfPermission(Manifest.permission.SEND_SMS)!=android.content.pm.PackageManager.PERMISSION_GRANTED||c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED||c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){status(c,"無法回傳：請授權簡訊、精確定位及背景定位");return;}
        LocationManager lm=c.getSystemService(LocationManager.class);if(!lm.isLocationEnabled()){status(c,"無法回傳：手機定位已關閉");return;}
        pending=true;status(c,"照顧者來電：正在取得位置");Handler h=new Handler(Looper.getMainLooper());
        LocationListener listener=new LocationListener(){boolean done;Location best;
            @Override public void onLocationChanged(Location l){if(done)return;if(l.hasAccuracy()&&(best==null||l.getAccuracy()<best.getAccuracy()))best=l;if(l.hasAccuracy()&&l.getAccuracy()<=100)finish(l);}
            void finish(Location l){if(done)return;done=true;pending=false;lm.removeUpdates(this);h.removeCallbacksAndMessages(null);
                if(l==null){status(app,"無法回傳：30 秒內未取得新定位，未傳送舊座標");return;}
                // Re-read consent and recipient: a settings change during acquisition cancels transmission.
                if(!Store.flag(app,"locationSms")||!target.equals(Store.prefs(app).getString("caregiver",""))){status(app,"已取消定位簡訊");return;}
                String body=String.format(Locale.US,"NCDPhone 定位（誤差約 %.0f 公尺）\nhttps://maps.google.com/?q=%.6f,%.6f",l.getAccuracy(),l.getLatitude(),l.getLongitude());
                try{SmsManager sms=app.getSystemService(SmsManager.class);sms.sendMultipartTextMessage(target,null,sms.divideMessage(body),null,null);lastSent=SystemClock.elapsedRealtime();status(app,"定位簡訊已交付系統送出（不代表對方已收到）");}catch(Exception e){status(app,"簡訊送出失敗：請檢查 SIM 卡、預設簡訊 SIM 與權限");}
            }
            @Override public void onProviderDisabled(String p){}
            @Override public void onProviderEnabled(String p){}
            @Override public void onStatusChanged(String p,int s,Bundle b){}
            {h.postDelayed(()->finish(best),30000);}
        };
        try{boolean requested=false;for(String provider:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER})if(lm.isProviderEnabled(provider)){lm.requestLocationUpdates(provider,1000,0,listener,Looper.getMainLooper());requested=true;}if(!requested){h.removeCallbacksAndMessages(null);pending=false;status(c,"沒有可用的定位提供者");}}
        catch(SecurityException e){lm.removeUpdates(listener);h.removeCallbacksAndMessages(null);pending=false;status(c,"定位授權不足");}
    }
}
