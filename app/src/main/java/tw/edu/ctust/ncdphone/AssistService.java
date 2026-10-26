package tw.edu.ctust.ncdphone;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.hardware.*;
import android.media.AudioManager;
import android.os.*;
import android.view.accessibility.*;
import android.widget.Toast;
import java.util.*;

/** Bounded, visible-text LINE flow. Never uses guessed coordinates or fuzzy name matches. */
public class AssistService extends AccessibilityService implements SensorEventListener {
    static AssistService instance;
    static long settingsUntil;
    static final String LINE="jp.naver.line.android";
    final Handler handler=new Handler(Looper.getMainLooper());
    long lastInput,lastAction,deadline;String foreground="",target="";int stage=0,missingCall=0;
    boolean sawCall, menuTestOnly; SensorManager sensors;
    final FlipDetector flip=new FlipDetector();
    final LineCallGuard lineCallGuard=new LineCallGuard();
    static boolean lineBusy(){return instance!=null&&instance.stage>0;}
    boolean calling(){int m=getSystemService(AudioManager.class).getMode();return PhoneService.busy()||stage==6||m==AudioManager.MODE_IN_CALL||m==AudioManager.MODE_IN_COMMUNICATION;}
    final BroadcastReceiver wake=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(Store.flag(c,"wakeHome")&&!calling())handler.postDelayed(()->returnHome(),400);}};
    final Runnable timer=new Runnable(){public void run(){
        long now=SystemClock.elapsedRealtime();
        if(stage>0&&stage!=6&&now>deadline)fail("LINE 操作逾時，請確認顯示名稱與 LINE 版本");
        if(stage>0)step();
        AccessibilityNodeInfo activeRoot=getRootInActiveWindow();
        String activePackage=activeRoot==null?foreground:String.valueOf(activeRoot.getPackageName());
        if(Store.flag(AssistService.this,"globalIdle")&&!Store.flag(AssistService.this,"externalIdlePaused")&&!MainActivity.foregroundActive&&!calling()&&!activePackage.equals(getPackageName())&&!activePackage.equals("com.android.settings")&&now-lastInput>Store.prefs(AssistService.this).getInt("idleSeconds",60)*1000L)returnHome();
        handler.postDelayed(this,1000);
    }};
    @Override protected void onServiceConnected(){instance=this;lastInput=SystemClock.elapsedRealtime();
        if(Store.flag(this,"lineCallProtected")){lineCallGuard.start();stage=6;target=Store.prefs(this).getString("lineCallTarget","");}
        IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_USER_PRESENT);if(Build.VERSION.SDK_INT>=33)registerReceiver(wake,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(wake,f);
        sensors=getSystemService(SensorManager.class);Sensor sensor=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);if(sensor!=null)sensors.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL);handler.post(timer);
    }
    void returnHome(){if(SystemClock.elapsedRealtime()<settingsUntil||calling()||getSystemService(KeyguardManager.class).isKeyguardLocked())return;cancelPreparation();lastInput=SystemClock.elapsedRealtime();Ui.home(this);}
    void cancelPreparation(){if(stage>0&&stage!=6){stage=0;target="";Store.prefs(this).edit().putString("lineStatus","已取消 LINE 操作並返回主畫面").apply();}}
    void beginLine(String name){beginLine(name,false);}
    void clearLineCall(){lineCallGuard.clear();stage=0;target="";Store.prefs(this).edit().remove("lineCallProtected").remove("lineCallTarget").apply();}
    static void returnedToHome(Context c){
        if(c.getSystemService(AudioManager.class).getMode()!=AudioManager.MODE_NORMAL)return;
        if(instance!=null&&instance.stage==6)instance.clearLineCall();
        else if(instance==null)Store.prefs(c).edit().remove("lineCallProtected").remove("lineCallTarget").apply();
    }
    void beginLine(String name,boolean testOnly){target=name;menuTestOnly=testOnly;stage=1;sawCall=false;missingCall=0;lastAction=0;flip.reset();deadline=SystemClock.elapsedRealtime()+45000;Store.prefs(this).edit().putString("lineStatus","正在搜尋").apply();}
    void fail(String text){stage=0;target="";Store.prefs(this).edit().putString("lineStatus",text).apply();Toast.makeText(this,text,Toast.LENGTH_LONG).show();lastInput=SystemClock.elapsedRealtime();}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED&&e.getPackageName()!=null)foreground=e.getPackageName().toString();
        int type=e.getEventType();if(type==AccessibilityEvent.TYPE_VIEW_CLICKED||type==AccessibilityEvent.TYPE_VIEW_SCROLLED||type==AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED||type==AccessibilityEvent.TYPE_TOUCH_INTERACTION_START)lastInput=SystemClock.elapsedRealtime();
        if(stage>0&&LINE.contentEquals(e.getPackageName()==null?"":e.getPackageName()))step();
    }
    void step(){
        long now=SystemClock.elapsedRealtime();if(now-lastAction<1200)return;
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null||!LINE.contentEquals(root.getPackageName()))return;
        List<AccessibilityNodeInfo> nodes=new ArrayList<>();collect(root,nodes,0);
        // Only a verified return to this chat with inactive audio confirms completion.
        boolean end=has(nodes,"結束通話","掛斷","End call","Hang up");
        if(stage==6){
            boolean audioActive=getSystemService(AudioManager.class).getMode()!=AudioManager.MODE_NORMAL;
            if(lineCallGuard.observe(end,audioActive,chatTitleMatches(nodes))){clearLineCall();Ui.home(this);}
            lastAction=now;return;
        }
        if(stage==1){if(chatTitleMatches(nodes)){stage=5;}else if(searchEditor(nodes)!=null){stage=3;}else if(has(nodes,"搜尋","Search")){stage=2;}else if(clickChatTab(nodes)){stage=2;lastAction=now;}else if(byId(nodes,"header_up_button_bg")!=null){byId(nodes,"header_up_button_bg").performAction(AccessibilityNodeInfo.ACTION_CLICK);lastAction=now;}}
        else if(stage==2){if(searchEditor(nodes)!=null){stage=3;}else if(clickUnique(nodes,"搜尋","Search")){stage=3;lastAction=now;}}
        else if(stage==3){List<AccessibilityNodeInfo> edits=new ArrayList<>();for(AccessibilityNodeInfo n:nodes)if(n.isEditable()&&n.isVisibleToUser())edits.add(n);
            if(edits.size()==1){Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,target);if(edits.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args)){stage=8;lastAction=now;}}
        }
        else if(stage==8){
            AccessibilityNodeInfo editor=singleEditor(nodes);
            if(editor==null||!target.contentEquals(editor.getText()==null?"":editor.getText()))return;
            boolean submitted=false;
            if(Build.VERSION.SDK_INT>=30)submitted=editor.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
            if(!submitted)submitted=clickSearchSubmit(nodes);
            if(submitted){stage=9;lastAction=now;}
            else {List<AccessibilityNodeInfo> results=targetMatches(nodes);results.removeIf(n->n.isEditable()||containsEditor(n,0));if(!results.isEmpty()){stage=9;lastAction=now;}}
        }
        else if(stage==9){if(clickUnique(nodes,"聊天","Chats")){stage=4;lastAction=now;}else if(!has(nodes,"全部","All")){stage=4;lastAction=now;}}
        else if(stage==4){List<AccessibilityNodeInfo> matches=targetMatches(nodes);matches.removeIf(AccessibilityNodeInfo::isEditable);
            List<AccessibilityNodeInfo> actions=new ArrayList<>();for(AccessibilityNodeInfo n:matches){AccessibilityNodeInfo a=clickable(n);if(a!=null&&!containsEditor(a,0)&&!actions.contains(a))actions.add(a);}
            if(actions.size()>1){fail("找到多個同名 LINE 聯絡人，為避免誤撥，請手動選擇");return;}
            if(actions.size()==1&&actions.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK)){stage=5;lastAction=now;}
        }
        else if(stage==5){
            // Scope to the verified chat header; historical calls in the message list are never targets.
            if(chatTitleMatches(nodes)){
                if(byId(nodes,"chat_ui_singlecall_layer_video_button")!=null){stage=7;lastAction=now;}
                else if(clickHeaderCall(nodes)){stage=7;lastAction=now;}
            }
        }
        else if(stage==7){AccessibilityNodeInfo video=byId(nodes,"chat_ui_singlecall_layer_video_button");
            if(chatTitleMatches(nodes)&&video!=null&&video.isClickable()&&video.isEnabled()){
                if(menuTestOnly){fail("LINE 通話選單測試成功，已找到視訊通話按鈕，未撥號");return;}
                if(video.performAction(AccessibilityNodeInfo.ACTION_CLICK)){lineCallGuard.start();Store.prefs(this).edit().putBoolean("lineCallProtected",true).putString("lineCallTarget",target).apply();stage=6;lastAction=now;}
            }
        }
    }
    AccessibilityNodeInfo byId(List<AccessibilityNodeInfo> nodes,String id){AccessibilityNodeInfo found=null;for(AccessibilityNodeInfo n:nodes)if(n.isVisibleToUser()&&(LINE+":id/"+id).equals(n.getViewIdResourceName())){if(found!=null)return null;found=n;}return found;}
    boolean chatTitleMatches(List<AccessibilityNodeInfo> nodes){AccessibilityNodeInfo title=byId(nodes,"header_title");return title!=null&&Rules.sameLineName(target,title.getText()==null?null:title.getText().toString());}
    boolean clickHeaderCall(List<AccessibilityNodeInfo> nodes){List<AccessibilityNodeInfo> matches=new ArrayList<>();for(AccessibilityNodeInfo n:exact(nodes,"通話 按鍵","通話","Call button","Call"))if((LINE+":id/header_button_layout").equals(n.getViewIdResourceName())){AccessibilityNodeInfo a=clickable(n);if(a!=null&&!matches.contains(a))matches.add(a);}return matches.size()==1&&matches.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);}
    AccessibilityNodeInfo singleEditor(List<AccessibilityNodeInfo> nodes){AccessibilityNodeInfo found=null;for(AccessibilityNodeInfo n:nodes)if(n.isEditable()&&n.isVisibleToUser()){if(found!=null)return null;found=n;}return found;}
    List<AccessibilityNodeInfo> targetMatches(List<AccessibilityNodeInfo> nodes){List<AccessibilityNodeInfo> out=new ArrayList<>();for(AccessibilityNodeInfo n:nodes)if(n.isVisibleToUser()&&(Rules.sameLineName(target,n.getText()==null?null:n.getText().toString())||Rules.sameLineName(target,n.getContentDescription()==null?null:n.getContentDescription().toString())))out.add(n);return out;}
    AccessibilityNodeInfo searchEditor(List<AccessibilityNodeInfo> nodes){AccessibilityNodeInfo n=singleEditor(nodes);if(n==null)return null;String evidence=(""+n.getViewIdResourceName()+" "+n.getHintText()+" "+n.getContentDescription()).toLowerCase(Locale.ROOT);return evidence.contains("search")||evidence.contains("搜尋")||evidence.contains("搜索")?n:null;}
    boolean clickChatTab(List<AccessibilityNodeInfo> nodes){for(AccessibilityNodeInfo n:exact(nodes,"聊天","Chats")){String id=n.getViewIdResourceName();if(id!=null&&id.endsWith("/bnb_button_text")){AccessibilityNodeInfo a=clickable(n);if(a!=null)return a.performAction(AccessibilityNodeInfo.ACTION_CLICK);}}return false;}
    boolean containsEditor(AccessibilityNodeInfo n,int depth){if(n.isEditable())return true;if(depth>8)return false;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null&&containsEditor(child,depth+1))return true;}return false;}
    boolean clickSearchSubmit(List<AccessibilityNodeInfo> nodes){List<AccessibilityNodeInfo> actions=new ArrayList<>();for(AccessibilityNodeInfo n:exact(nodes,"搜尋","搜索","Search")){AccessibilityNodeInfo a=clickable(n);if(a!=null&&!containsEditor(a,0)&&!actions.contains(a))actions.add(a);}return actions.size()==1&&actions.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);}
    void collect(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> out,int depth){if(depth>35||out.size()>1200)return;out.add(n);for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null)collect(child,out,depth+1);}}
    List<AccessibilityNodeInfo> exact(List<AccessibilityNodeInfo> nodes,String...labels){List<AccessibilityNodeInfo> out=new ArrayList<>();for(AccessibilityNodeInfo n:nodes){if(!n.isVisibleToUser())continue;for(String label:labels)if(label.contentEquals(n.getText()==null?"":n.getText())||label.contentEquals(n.getContentDescription()==null?"":n.getContentDescription())){out.add(n);break;}}return out;}
    boolean has(List<AccessibilityNodeInfo> nodes,String...labels){return !exact(nodes,labels).isEmpty();}
    AccessibilityNodeInfo clickable(AccessibilityNodeInfo n){for(int i=0;n!=null&&i<5;i++,n=n.getParent())if(n.isClickable()&&n.isEnabled())return n;return null;}
    boolean clickUnique(List<AccessibilityNodeInfo> nodes,String...labels){List<AccessibilityNodeInfo> actions=new ArrayList<>();for(AccessibilityNodeInfo n:exact(nodes,labels)){AccessibilityNodeInfo a=clickable(n);if(a!=null&&!actions.contains(a))actions.add(a);}return actions.size()==1&&actions.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);}
    @Override public void onSensorChanged(SensorEvent e){if(!Store.flag(this,"flipHome")||calling()){flip.reset();return;}if(flip.update(e.values[2],SystemClock.elapsedRealtime()))returnHome();}
    @Override public void onAccuracyChanged(Sensor s,int a){}
    @Override public void onInterrupt(){if(stage!=6)fail("操作協助已中斷");}
    @Override public void onDestroy(){instance=null;handler.removeCallbacksAndMessages(null);if(sensors!=null)sensors.unregisterListener(this);try{unregisterReceiver(wake);}catch(Exception ignored){}super.onDestroy();}
}
