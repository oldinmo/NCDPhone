package tw.edu.ctust.ncdphone;

import android.app.*;
import android.content.*;
import android.os.*;
import android.telecom.*;
import java.util.*;

public class PhoneService extends InCallService {
    static PhoneService instance;
    static final ArrayList<Call> calls=new ArrayList<>();
    static final ArrayList<Runnable> listeners=new ArrayList<>();
    final Map<Call,Call.Callback> callbacks=new HashMap<>();
    final Set<Call> located=Collections.newSetFromMap(new IdentityHashMap<>());
    static boolean busy(){for(Call c:calls)if(c.getState()!=Call.STATE_DISCONNECTED)return true;return false;}
    static Call current(){for(Call c:calls)if(c.getState()==Call.STATE_RINGING)return c;for(Call c:calls)if(c.getState()!=Call.STATE_DISCONNECTED&&c.getState()!=Call.STATE_HOLDING)return c;for(Call c:calls)if(c.getState()!=Call.STATE_DISCONNECTED)return c;return null;}
    static String number(Call c){return c==null||c.getDetails()==null||c.getDetails().getHandle()==null?"":c.getDetails().getHandle().getSchemeSpecificPart();}
    @Override public void onCreate(){super.onCreate();instance=this;
        NotificationChannel channel=new NotificationChannel("calls","來電與通話",NotificationManager.IMPORTANCE_HIGH);channel.setDescription("顯示大型接聽與掛斷畫面");channel.setSound(null,null);channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    @Override public void onCallAdded(Call call){super.onCallAdded(call);calls.add(call);
        Call.Callback cb=new Call.Callback(){@Override public void onStateChanged(Call c,int state){changed();}@Override public void onDetailsChanged(Call c,Call.Details d){locate(c);changed();}};
        callbacks.put(call,cb);call.registerCallback(cb);locate(call);changed();
        if(call.getState()!=Call.STATE_RINGING)show();
    }
    void locate(Call c){if(c.getState()==Call.STATE_RINGING&&!located.contains(c)&&!number(c).isEmpty()){located.add(c);CaregiverLocation.incoming(this,number(c));}}
    @Override public void onCallRemoved(Call call){call.unregisterCallback(callbacks.remove(call));calls.remove(call);located.remove(call);super.onCallRemoved(call);changed();if(!busy())Ui.home(this);}
    void show(){try{startActivity(new Intent(this,CallActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}
    void changed(){
        Call c=current();NotificationManager nm=getSystemService(NotificationManager.class);
        if(c==null)nm.cancel(100);else{
            Intent intent=new Intent(this,CallActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi=PendingIntent.getActivity(this,100,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            boolean ring=c.getState()==Call.STATE_RINGING;
            Notification.Builder b=new Notification.Builder(this,"calls").setSmallIcon(android.R.drawable.sym_action_call).setContentTitle(ring?"NCDPhone 來電":"NCDPhone 通話中").setContentText("點此開啟接聽／掛斷畫面").setCategory(Notification.CATEGORY_CALL).setOngoing(true).setContentIntent(pi).setVisibility(Notification.VISIBILITY_PUBLIC);
            if(ring)b.setFullScreenIntent(pi,true);
            nm.notify(100,b.build());
        }
        for(Runnable r:new ArrayList<>(listeners))r.run();
    }
    @Override public void onDestroy(){for(Map.Entry<Call,Call.Callback> e:callbacks.entrySet())e.getKey().unregisterCallback(e.getValue());callbacks.clear();calls.clear();instance=null;super.onDestroy();}
}
