package tw.edu.ctust.ncdphone;

import android.app.*;
import android.os.*;
import android.telecom.*;
import android.widget.*;

public class CallActivity extends Activity {
    long ignoreUntil; boolean preview; int previewState=Call.STATE_RINGING;
    final Runnable refresh=this::render;
    @Override public void onCreate(Bundle b){super.onCreate(b);preview=getIntent().getBooleanExtra("previewIncoming",false);if(b!=null)previewState=b.getInt("previewState",Call.STATE_RINGING);setShowWhenLocked(true);setTurnScreenOn(true);render();}
    @Override protected void onResume(){super.onResume();PhoneService.listeners.add(refresh);render();}
    @Override protected void onPause(){PhoneService.listeners.remove(refresh);super.onPause();}
    @Override public void onBackPressed(){if(!PhoneService.busy()){Ui.home(this);finish();}}
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){if(SystemClock.elapsedRealtime()<ignoreUntil)return true;return super.dispatchTouchEvent(e);}
    void render(){
        Call call=PhoneService.current();if(call!=null)preview=false;if(preview){renderPreview();return;}if(call==null){Ui.home(this);finish();return;}
        ignoreUntil=SystemClock.elapsedRealtime()+850;
        LinearLayout root=Ui.root(this);String number=PhoneService.number(call);Store.Person p=Store.find(this,number);
        Ui.add(root,Ui.text(this,call.getState()==Call.STATE_RINGING?"電話來了":status(call.getState()),28));
        ImageView photo=Ui.photo(this,p);root.addView(photo,new LinearLayout.LayoutParams(-1,0,1));
        if(call.getState()==Call.STATE_RINGING){photo.setContentDescription("點照片接聽"+(p==null?"來電":p.name+"的來電"));Ui.forgiving(photo,()->{if(call.getState()==Call.STATE_RINGING)call.answer(0);});}
        Ui.add(root,Ui.text(this,p==null?(number.isEmpty()?"未顯示號碼":number):p.name,32));
        if(call.getState()==Call.STATE_SELECT_PHONE_ACCOUNT){
            java.util.List<PhoneAccountHandle> accounts=call.getDetails().getExtras()==null?null:call.getDetails().getExtras().getParcelableArrayList(Call.AVAILABLE_PHONE_ACCOUNTS);
            if(accounts!=null)for(PhoneAccountHandle a:accounts){PhoneAccount pa=getSystemService(TelecomManager.class).getPhoneAccount(a);Ui.add(root,Ui.button(this,"使用 "+(pa==null?"SIM 卡":pa.getLabel()),Ui.BLUE,()->call.phoneAccountSelected(a,false)));}
        }
        if(call.getState()==Call.STATE_RINGING)root.addView(Ui.callControl(this,true,()->{if(call.getState()==Call.STATE_RINGING)call.answer(0);}),new LinearLayout.LayoutParams(-1,Ui.dp(this,130)));
        else {LinearLayout options=new LinearLayout(this);options.addView(Ui.button(this,"擴音",Ui.BLUE,()->{if(PhoneService.instance!=null)PhoneService.instance.setAudioRoute(CallAudioState.ROUTE_SPEAKER);}),new LinearLayout.LayoutParams(0,-2,1));options.addView(Ui.button(this,"聽筒",Ui.BLUE,()->{if(PhoneService.instance!=null)PhoneService.instance.setAudioRoute(CallAudioState.ROUTE_WIRED_OR_EARPIECE);}),new LinearLayout.LayoutParams(0,-2,1));Ui.add(root,options);
            Ui.add(root,Ui.button(this,"數字鍵盤",Ui.BLUE,()->keypad(call)));
            if(call.getState()==Call.STATE_HOLDING)Ui.add(root,Ui.button(this,"恢復通話",Ui.GREEN,call::unhold));
        }
        root.addView(Ui.callControl(this,false,()->{if(call.getState()==Call.STATE_RINGING)call.reject(false,null);else call.disconnect();}),new LinearLayout.LayoutParams(-1,Ui.dp(this,130)));
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("previewState",previewState);super.onSaveInstanceState(out);}
    void renderPreview(){
        ignoreUntil=SystemClock.elapsedRealtime()+850;
        LinearLayout root=Ui.root(this);
        Ui.add(root,Ui.text(this,"畫面預覽 · 非真實來電",20));
        Ui.add(root,Ui.text(this,previewState==Call.STATE_RINGING?"電話來了":"通話中（模擬）",28));
        Store.Person person=null;for(Store.Person candidate:Store.people(this))if(!candidate.phone.isEmpty()){person=Store.find(this,candidate.phone);if(person!=null&&!person.photo.isEmpty())break;}
        ImageView photo=Ui.photo(this,person);root.addView(photo,new LinearLayout.LayoutParams(-1,0,1));
        Runnable answer=()->{previewState=Call.STATE_ACTIVE;render();};
        if(previewState==Call.STATE_RINGING){photo.setContentDescription("點照片模擬接聽");Ui.forgiving(photo,answer);}
        Ui.add(root,Ui.text(this,person==null?"測試來電":person.name,32));
        if(previewState==Call.STATE_RINGING)root.addView(Ui.callControl(this,true,answer),new LinearLayout.LayoutParams(-1,Ui.dp(this,130)));
        else Ui.add(root,Ui.text(this,"已模擬接聽，沒有建立真實通話",22));
        root.addView(Ui.callControl(this,false,()->{Ui.home(this);finish();}),new LinearLayout.LayoutParams(-1,Ui.dp(this,130)));
    }
    String status(int s){switch(s){case Call.STATE_ACTIVE:return "通話中";case Call.STATE_HOLDING:return "通話保留中";case Call.STATE_DISCONNECTING:return "正在掛斷";default:return "正在連線";}}
    void keypad(Call c){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);for(String row:new String[]{"123","456","789","*0#"}){LinearLayout r=new LinearLayout(this);for(char ch:row.toCharArray())r.addView(Ui.button(this,""+ch,Ui.BLUE,()->{c.playDtmfTone(ch);new Handler(Looper.getMainLooper()).postDelayed(c::stopDtmfTone,200);}),new LinearLayout.LayoutParams(0,-2,1));box.addView(r);}new AlertDialog.Builder(this).setTitle("通話數字鍵盤").setView(box).setPositiveButton("關閉",null).show();}
}
