package tw.edu.ctust.ncdphone;

import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.telecom.TelecomManager;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    static boolean foregroundActive;
    final Handler handler=new Handler(Looper.getMainLooper());
    String screen="home"; int page=0; long lastTouch, ignoreUntil; boolean resumed, touching;
    float swipeX,swipeY;boolean pageGesture,swiping;
    EditText caregiverField, secondsField;
    TextView saveStatus;
    LinearLayout root,grid,shortcutRow; TextView pages;
    Store.Person editing; EditText name,phone,line; String photo="";
    BannerController ads;LocalWeather weather;
    TextView clockText,dateText,weatherText;WeatherIcon weatherIcon;
    long weatherCheckAt;
    final Runnable tick=new Runnable(){public void run(){
        if(resumed&&screen.equals("home")){updateClock();if(SystemClock.elapsedRealtime()>=weatherCheckAt){weatherCheckAt=SystemClock.elapsedRealtime()+60000;weather.refresh();}}
        if(resumed && !touching && !PhoneService.busy() && !AssistService.lineBusy()) {
            long idle=SystemClock.elapsedRealtime()-lastTouch;
            if(!screen.equals("home") && idle>Store.prefs(MainActivity.this).getInt("idleSeconds",60)*1000L) home();
        }
        if(resumed)handler.postDelayed(this,1000);
    }};
    final Runnable rotate=new Runnable(){public void run(){
        if(!resumed||!screen.equals("home"))return;
        if(!touching&&SystemClock.elapsedRealtime()-lastTouch>1500) {
            int n=Rules.pageCount(Store.people(MainActivity.this).size()); if(n>1){page=(page+1)%n;renderGrid();}
        }
        handler.postDelayed(this,10000);
    }};
    @Override public void onCreate(Bundle state){super.onCreate(state);weather=new LocalWeather(this);ads=new BannerController(this);lastTouch=SystemClock.elapsedRealtime();home();ads.start();
        if(state!=null&&state.containsKey("editId")){Store.Person p=new Store.Person();p.id=state.getString("editId");p.name=state.getString("editName","");p.phone=state.getString("editPhone","");p.line=state.getString("editLine","");p.photo=state.getString("editPhoto","");edit(p);}
        else handleIntent(getIntent());}
    @Override protected void onSaveInstanceState(Bundle out){if(screen.equals("edit")){stashEdit();out.putString("editId",editing.id);out.putString("editName",editing.name);out.putString("editPhone",editing.phone);out.putString("editLine",editing.line);out.putString("editPhoto",editing.photo);}super.onSaveInstanceState(out);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(!PhoneService.busy())home();handleIntent(i);}
    void handleIntent(Intent i){if(i.getBooleanExtra("previewIncoming",false)){i.removeExtra("previewIncoming");previewIncoming();return;}if(Intent.ACTION_DIAL.equals(i.getAction())){dialPad(i.getData()==null?"":i.getData().getSchemeSpecificPart());}}
    @Override protected void onResume(){super.onResume();AssistService.returnedToHome(this);Store.prefs(this).edit().remove("externalIdlePaused").apply();resumed=true;foregroundActive=true;touching=false;activity();AssistService.settingsUntil=0;handler.removeCallbacks(tick);handler.postDelayed(tick,1000);if(PhoneService.busy()){startActivity(new Intent(this,CallActivity.class));return;}if(screen.equals("home")){refreshShortcut();renderGrid();scheduleRotation();updateClock();weather.start(this::showWeather);ads.resume();}}
    @Override protected void onPause(){saveSettings();resumed=false;foregroundActive=false;touching=false;handler.removeCallbacks(tick);handler.removeCallbacks(rotate);weather.stop();ads.pause();super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);weather.destroy();ads.destroy();super.onDestroy();}
    void activity(){lastTouch=SystemClock.elapsedRealtime();if(AssistService.instance!=null)AssistService.instance.lastInput=lastTouch;}
    @Override public void onUserInteraction(){super.onUserInteraction();activity();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){activity();return super.dispatchKeyEvent(e);}
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){
        activity();int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){touching=true;swiping=false;pageGesture=false;swipeX=e.getX();swipeY=e.getY();
            android.graphics.Rect bounds=new android.graphics.Rect();
            pageGesture=screen.equals("home")&&grid!=null&&Rules.pageCount(Store.people(this).size())>1&&grid.getGlobalVisibleRect(bounds)&&bounds.contains((int)e.getRawX(),(int)e.getRawY());
        }
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)touching=false;
        if(lastTouch<ignoreUntil)return true;
        if(pageGesture){
            float dx=e.getX()-swipeX,dy=e.getY()-swipeY;
            if(!swiping&&action==MotionEvent.ACTION_MOVE&&Rules.isPageSwipe(dx,dy,Ui.dp(this,72))){
                swiping=true;MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();
            }
            if(swiping){if(action==MotionEvent.ACTION_UP){turnPage(dx<0?1:-1);pageGesture=false;swiping=false;}else if(action==MotionEvent.ACTION_CANCEL){pageGesture=false;swiping=false;}return true;}
        }
        return super.dispatchTouchEvent(e);
    }
    void turnPage(int direction){int count=Rules.pageCount(Store.people(this).size());page=Math.floorMod(page+direction,count);renderGrid();scheduleRotation();}
    @Override public void onBackPressed(){home();}
    void scheduleRotation(){handler.removeCallbacks(rotate);if(resumed&&screen.equals("home"))handler.postDelayed(rotate,10000);}
    void base(String title){shortcutRow=null;saveSettings();caregiverField=null;secondsField=null;saveStatus=null;handler.removeCallbacks(rotate);if(weather!=null)weather.stop();if(ads!=null)ads.pause();activity();ignoreUntil=SystemClock.elapsedRealtime()+500;root=Ui.root(this);if(!title.isEmpty())Ui.add(root,Ui.text(this,title,28));}
    void home(){screen="home";page=0;base("");
        root.addView(ads.attach(),new LinearLayout.LayoutParams(-1,-2));
        LinearLayout bar=new LinearLayout(this);
        bar.addView(Ui.button(this,"⚙ 設定",Ui.BLUE,this::settings),new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
        bar.addView(Ui.button(this,"ⓘ 開發者",Ui.BLUE,this::about),new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
        ImageButton originalHome=new ImageButton(this);originalHome.setImageResource(R.drawable.ic_home);originalHome.setBackground(Ui.bg(Ui.BLUE));originalHome.setPadding(Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,10));originalHome.setContentDescription("開啟原桌面");Ui.forgiving(originalHome,()->AppShortcuts.openHome(this));bar.addView(originalHome,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,52)));Ui.add(root,bar);
        LinearLayout info=new LinearLayout(this);root.addView(info,new LinearLayout.LayoutParams(-1,Ui.dp(this,180)));
        LinearLayout clockPanel=new LinearLayout(this);clockPanel.setOrientation(LinearLayout.VERTICAL);clockPanel.setBackground(Ui.bg(android.graphics.Color.WHITE));info.addView(clockPanel,new LinearLayout.LayoutParams(0,-1,1));
        clockText=Ui.text(this,"",60);clockText.setTextColor(Ui.NAVY);clockText.setTypeface(null,android.graphics.Typeface.BOLD);clockText.setMaxLines(1);clockText.setAutoSizeTextTypeUniformWithConfiguration(24,80,2,android.util.TypedValue.COMPLEX_UNIT_SP);
        clockPanel.addView(clockText,new LinearLayout.LayoutParams(-1,0,1));dateText=Ui.text(this,"",24);dateText.setTextColor(Ui.NAVY);dateText.setMaxLines(2);dateText.setAutoSizeTextTypeUniformWithConfiguration(16,26,1,android.util.TypedValue.COMPLEX_UNIT_SP);clockPanel.addView(dateText,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout weatherRow=new LinearLayout(this);weatherRow.setOrientation(LinearLayout.VERTICAL);weatherRow.setBackground(Ui.bg(android.graphics.Color.WHITE));info.addView(weatherRow,new LinearLayout.LayoutParams(0,-1,1));
        weatherIcon=new WeatherIcon(this);weatherRow.addView(weatherIcon,new LinearLayout.LayoutParams(-1,0,1));
        weatherText=Ui.text(this,"正在取得當地氣象",24);weatherText.setTextColor(Ui.NAVY);weatherText.setMaxLines(3);weatherText.setAutoSizeTextTypeUniformWithConfiguration(16,26,1,android.util.TypedValue.COMPLEX_UNIT_SP);weatherRow.addView(weatherText,new LinearLayout.LayoutParams(-1,0,1.4f));Ui.forgiving(weatherRow,this::weatherDetails);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int available=v.getHeight()-v.getPaddingTop()-v.getPaddingBottom();int h=Math.round(available*.20f);if(h>0&&info.getLayoutParams().height!=h){info.getLayoutParams().height=h;info.requestLayout();}});
        shortcutRow=new LinearLayout(this);root.addView(shortcutRow,new LinearLayout.LayoutParams(-1,-2));refreshShortcut();
        grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);root.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        pages=Ui.text(this,"",18);Ui.add(root,pages);renderGrid();scheduleRotation();updateClock();if(resumed)weather.start(this::showWeather);
    }
    void refreshShortcut(){
        if(shortcutRow==null)return;shortcutRow.removeAllViews();android.content.pm.ResolveInfo app=AppShortcuts.selected(this);
        shortcutRow.setVisibility(app==null?View.GONE:View.VISIBLE);if(app==null)return;
        LinearLayout button=new LinearLayout(this);button.setGravity(Gravity.CENTER_VERTICAL);button.setBackground(Ui.bg(Ui.BLUE));button.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);
        String label=app.loadLabel(getPackageManager()).toString();button.setContentDescription("開啟 "+label);
        ImageView icon=new ImageView(this);icon.setImageDrawable(app.loadIcon(getPackageManager()));icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);button.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,40)));
        TextView text=Ui.text(this,label,26);text.setMaxLines(1);text.setAutoSizeTextTypeUniformWithConfiguration(18,26,1,android.util.TypedValue.COMPLEX_UNIT_SP);button.addView(text,new LinearLayout.LayoutParams(0,-1,1));
        Ui.forgiving(button,()->AppShortcuts.openShortcut(this));LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,Ui.dp(this,60));layout.setMargins(0,Ui.dp(this,4),0,Ui.dp(this,4));shortcutRow.addView(button,layout);
    }
    void updateClock(){if(clockText==null)return;Date now=new Date();String period=new java.text.SimpleDateFormat("a",Locale.TAIWAN).format(now);android.text.SpannableString time=new android.text.SpannableString(period+" "+new java.text.SimpleDateFormat("h:mm",Locale.TAIWAN).format(now));time.setSpan(new android.text.style.AbsoluteSizeSpan(20,true),0,period.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);time.setSpan(new android.text.style.ForegroundColorSpan(Ui.RED),0,period.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);clockText.setText(time);dateText.setText(new java.text.SimpleDateFormat("M月d日\nEEEE",Locale.TAIWAN).format(now));}
    void showWeather(LocalWeather.Snapshot value,String status){if(!screen.equals("home")||weatherText==null)return;
        if(value==null){weatherIcon.symbol(null);weatherText.setText(status);return;}
        weatherIcon.symbol(value.symbol);weatherText.setText(value.district+"\n"+value.description+"\n"+Math.round(value.temperature)+"°C");
    }
    void weatherDetails(){
        if(!weather.permission()){requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},16);return;}
        new AlertDialog.Builder(this).setTitle("當地氣象").setMessage("資料來源：交通部中央氣象署鄉鎮天氣預報\n行政區：Android 地理編碼\n\n只向中央氣象署下載所在縣市的公開預報，經緯度不傳給氣象署。每 30 分鐘更新，過期資料不顯示；無資料時不改用國外預報。\n\n本頁不提供颱風警報，請以中央氣象署發布為準。")
            .setPositiveButton("知道了",null).setNeutralButton("資料來源",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.cwa.gov.tw/V8/C/W/Town/Town.html")))).show();
    }
    void renderGrid(){if(grid==null||!screen.equals("home"))return;grid.removeAllViews();ArrayList<Store.Person> list=Store.people(this);page%=Rules.pageCount(list.size());
        for(int r=0;r<2;r++){LinearLayout row=new LinearLayout(this);grid.addView(row,new LinearLayout.LayoutParams(-1,0,1));
            for(int col=0;col<2;col++) {int index=Rules.contactIndex(page,r*2+col,list.size());Store.Person p=index<0?null:list.get(index);
                if(p==null){row.addView(new View(this),new LinearLayout.LayoutParams(0,-1,1));continue;}
                LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(8,8,8,8);tile.setBackground(Ui.bg(Ui.BLUE));
                LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-1,1);tp.setMargins(5,5,5,5);row.addView(tile,tp);
                tile.addView(Ui.photo(this,p),new LinearLayout.LayoutParams(-1,0,1));TextView label=Ui.text(this,p.name,28);label.setMaxLines(2);tile.addView(label);
                tile.setContentDescription("聯絡 "+p.name);Ui.forgiving(tile,()->detail(p));
            }
        }
        pages.setText("第"+(page+1)+"/"+Rules.pageCount(list.size())+"頁");
    }
    void detail(Store.Person p){screen="detail";base("打電話");
        // Keep the call target at one quarter of the usable screen, with the photo taking remaining space.
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);
        top.addView(Ui.photo(this,p),new LinearLayout.LayoutParams(-1,0,1));top.addView(Ui.text(this,p.name,32));top.addView(Ui.text(this,p.line.isEmpty()?"一般電話":"LINE 視訊通話",20));
        root.addView(top,new LinearLayout.LayoutParams(-1,0,1));
        View call=Ui.largeCall(this,()->{if(p.line.isEmpty())call(p.phone);else startLine(p.line);});root.addView(call,new LinearLayout.LayoutParams(-1,Ui.dp(this,160)));
        View back=Ui.button(this,"返回主畫面",Ui.RED,this::home);root.addView(back,new LinearLayout.LayoutParams(-1,Ui.dp(this,100)));
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int available=v.getHeight()-v.getPaddingTop()-v.getPaddingBottom();int height=Math.round(available*.25f),backHeight=Math.round(available*.15f);if(height>0&&call.getLayoutParams().height!=height){call.getLayoutParams().height=height;call.requestLayout();}if(backHeight>0&&back.getLayoutParams().height!=backHeight){back.getLayoutParams().height=backHeight;back.requestLayout();}});
    }
    void call(String number){
        if(!Rules.validPhone(number)){Ui.toast(this,"請輸入有效電話號碼");return;}
        if(!getSystemService(RoleManager.class).isRoleHeld(RoleManager.ROLE_DIALER)){Ui.toast(this,"請先在設定中指定 NCDPhone 為預設電話");return;}
        if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CALL_PHONE},10);Ui.toast(this,"授權後請再按打電話");return;}
        try{getSystemService(TelecomManager.class).placeCall(Uri.fromParts("tel",number,null),new Bundle());}catch(Exception e){Ui.toast(this,"無法撥號，請確認 SIM 卡與電話權限");}
    }
    void startLine(String target){
        if(AssistService.instance==null){Ui.toast(this,"請先在設定啟用 NCDPhone 操作協助");return;}
        Intent i=getPackageManager().getLaunchIntentForPackage("jp.naver.line.android");if(i==null){Ui.toast(this,"請先安裝並登入 LINE");return;}
        AssistService.instance.beginLine(target);startActivity(i);
    }
    LinearLayout form(String title){base(title);ScrollView s=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);s.addView(content);root.addView(s,new LinearLayout.LayoutParams(-1,0,1));Ui.add(root,Ui.button(this,"返回主畫面",Ui.BLUE,this::home));return content;}
    void previewIncoming(){startActivity(new Intent(this,CallActivity.class).putExtra("previewIncoming",true));}
    void about(){screen="about";LinearLayout c=form("開發者資訊");Ui.add(c,Ui.text(this,"劉崇汎\n中臺科技大學\n高齡健康照護系",30));Ui.add(c,Ui.text(this,"版本 "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")",24));Ui.add(c,Ui.button(this,"預覽來電畫面（不撥號）",Ui.BLUE,this::previewIncoming));}
    EditText field(LinearLayout c,String title,String value,int type){Ui.add(c,Ui.text(this,title,22));EditText e=new EditText(this);e.setText(value);e.setTextSize(24);e.setSingleLine(true);e.setInputType(type);e.setTextColor(android.graphics.Color.BLACK);e.setBackgroundColor(android.graphics.Color.WHITE);e.setPadding(14,14,14,14);e.setContentDescription(title);Ui.add(c,e);watch(e,this::activity);return e;}
    void watch(EditText e,Runnable action){e.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){action.run();}public void afterTextChanged(android.text.Editable s){}});}
    void saveSettings(){
        if(caregiverField==null||secondsField==null)return;
        String number=caregiverField.getText().toString().trim();int seconds;
        boolean phoneValid=number.isEmpty()||Rules.validPhone(number);
        try{seconds=Integer.parseInt(secondsField.getText().toString());}catch(NumberFormatException e){seconds=0;}
        boolean secondsValid=seconds>=15&&seconds<=3600;
        // Save valid fields independently; incomplete typing never replaces a valid timeout.
        if(phoneValid)Store.prefs(this).edit().putString("caregiver",number).apply();
        if(secondsValid)Store.prefs(this).edit().putInt("idleSeconds",seconds).apply();
        if(saveStatus!=null)saveStatus.setText(!phoneValid?"電話格式不正確，尚未儲存此欄":!secondsValid?"秒數須為 15～3600，暫時保留原值":"設定已自動儲存");
    }
    void settings(){screen="settings";LinearLayout c=form("照顧者設定");
        Ui.add(c,Ui.button(this,"管理通訊錄",Ui.GREEN,this::contacts));
        Ui.add(c,Ui.text(this,"首頁 App 快捷鍵",26));
        android.content.pm.ResolveInfo selected=AppShortcuts.selected(this);String saved=Store.prefs(this).getString(AppShortcuts.KEY,"");
        Ui.add(c,Ui.text(this,selected==null?(saved.isEmpty()?"尚未設定（首頁隱藏）":"原 App 無法使用，請重新選擇"):"目前："+selected.loadLabel(getPackageManager()),22));
        Ui.add(c,Ui.button(this,"選擇／變更 App 快捷鍵",Ui.BLUE,()->AppShortcuts.choose(this,this::settings)));
        Button clear=Ui.button(this,"清除 App 快捷鍵",Ui.RED,()->{Store.prefs(this).edit().remove(AppShortcuts.KEY).apply();settings();});clear.setEnabled(!saved.isEmpty());Ui.add(c,clear);
        caregiverField=field(c,"照顧者電話（定位簡訊收件人）",Store.prefs(this).getString("caregiver",""),3);
        secondsField=field(c,"閒置返回秒數（15～3600）",""+Store.prefs(this).getInt("idleSeconds",60),2);
        saveStatus=Ui.text(this,"設定會自動儲存",18);Ui.add(c,saveStatus);
        watch(caregiverField,this::saveSettings);watch(secondsField,this::saveSettings);
        Ui.add(c,Ui.text(this,"啟用後，照顧者來電會自動傳送本機位置；可能產生簡訊費用。請先取得手機使用者同意。",20));
        Switch sms=new Switch(this);sms.setText("照顧者來電自動回傳定位");sms.setTextColor(-1);sms.setTextSize(22);sms.setChecked(Store.flag(this,"locationSms"));Ui.add(c,sms);
        sms.setOnCheckedChangeListener((v,enabled)->{activity();saveSettings();if(enabled&&Rules.normalize(Store.prefs(this).getString("caregiver","")).length()<7){sms.setChecked(false);Ui.toast(this,"請先填寫完整照顧者電話");return;}Store.prefs(this).edit().putBoolean("locationSms",enabled).apply();});
        toggle(c,"點亮螢幕／解鎖後回主畫面","wakeHome");toggle(c,"翻面再翻回時回主畫面","flipHome");toggle(c,"在其他畫面閒置時回主畫面","globalIdle");toggle(c,"開機回主畫面（需預設桌面）","bootHome");
        Ui.add(c,Ui.text(this,"系統授權與設定",26));
        Ui.add(c,Ui.button(this,"設定／變更預設電話",Ui.BLUE,()->role(RoleManager.ROLE_DIALER)));
        Ui.add(c,Ui.button(this,"設定／變更預設桌面",Ui.BLUE,()->role(RoleManager.ROLE_HOME)));
        Ui.add(c,Ui.button(this,"授權電話、通知、定位與簡訊",Ui.BLUE,this::permissions));
        Ui.add(c,Ui.button(this,"變更／取消應用程式權限",Ui.BLUE,this::appSettings));
        Ui.add(c,Ui.button(this,"啟用／停用輔助操作",Ui.BLUE,()->openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        Ui.add(c,Ui.button(this,"設定／取消背景定位",Ui.BLUE,()->{
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){Ui.toast(this,"請先授權精確定位");return;}
            if(Build.VERSION.SDK_INT==29&&checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},12);
            else {Ui.toast(this,"請進入權限 → 位置，選擇允許方式或取消授權");appSettings();}
        }));
        if(Build.VERSION.SDK_INT>=34)Ui.add(c,Ui.button(this,"設定／取消來電全螢幕通知",Ui.BLUE,()->openSettings(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())))));
        Ui.add(c,Ui.button(this,"授權狀態檢查",Ui.BLUE,()->{RoleManager rm=getSystemService(RoleManager.class);new AlertDialog.Builder(this).setTitle("目前狀態").setMessage("預設電話："+rm.isRoleHeld(RoleManager.ROLE_DIALER)+"\n預設桌面："+rm.isRoleHeld(RoleManager.ROLE_HOME)+"\n輔助操作："+(AssistService.instance!=null)+"\n背景定位："+(checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED)+"\n定位簡訊："+Store.prefs(this).getString("smsStatus","尚未觸發")).setPositiveButton("知道了",null).show();}));
        Ui.add(c,Ui.button(this,"數字撥號",Ui.BLUE,()->dialPad("")));
        Ui.add(c,Ui.button(this,"廣告隱私選項",Ui.BLUE,()->ads.privacy()));
        Ui.add(c,Ui.text(this,"LINE 請填寫聊天顯示名稱，名稱必須唯一。LINE 更新後須重新實機驗證。\n輔助操作與預設桌面負責返回主畫面；鎖定螢幕不會被繞過。側載 App 可能須在應用程式資訊允許受限制的設定。",20));
    }
    void toggle(LinearLayout c,String title,String key){Switch s=new Switch(this);s.setText(title);s.setTextSize(22);s.setTextColor(-1);s.setPadding(0,20,0,20);s.setChecked(Store.flag(this,key));s.setOnCheckedChangeListener((v,b)->Store.prefs(this).edit().putBoolean(key,b).apply());Ui.add(c,s);}
    void openSettings(Intent intent){saveSettings();AssistService.settingsUntil=SystemClock.elapsedRealtime()+30*60*1000L;try{startActivity(intent);}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    void appSettings(){openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
    void role(String role){RoleManager r=getSystemService(RoleManager.class);if(r.isRoleHeld(role)){openSettings(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));return;}if(r.isRoleAvailable(role)){AssistService.settingsUntil=SystemClock.elapsedRealtime()+30*60*1000L;startActivityForResult(r.createRequestRoleIntent(role),15);}else Ui.toast(this,"此裝置不支援此角色");}
    void permissions(){ArrayList<String> list=new ArrayList<>(Arrays.asList(Manifest.permission.CALL_PHONE,Manifest.permission.READ_PHONE_STATE,Manifest.permission.SEND_SMS,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION));if(Build.VERSION.SDK_INT>=33)list.add(Manifest.permission.POST_NOTIFICATIONS);list.removeIf(p->checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED);if(list.isEmpty()){appSettings();return;}requestPermissions(list.toArray(new String[0]),10);}
    void dialPad(String number){screen="dial";base("數字撥號");
        EditText e=new EditText(this);e.setText(number);e.setTextSize(32);e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_PHONE);e.setShowSoftInputOnFocus(false);e.setTextColor(android.graphics.Color.BLACK);e.setBackgroundColor(android.graphics.Color.WHITE);e.setContentDescription("電話號碼");Ui.add(root,e);
        LinearLayout keys=new LinearLayout(this);keys.setOrientation(LinearLayout.VERTICAL);root.addView(keys,new LinearLayout.LayoutParams(-1,0,1));
        String[] values={"1","2","3","4","5","6","7","8","9","*","0","#"};
        for(int r=0;r<4;r++){LinearLayout row=new LinearLayout(this);keys.addView(row,new LinearLayout.LayoutParams(-1,0,1));for(int c=0;c<3;c++){String digit=values[r*3+c];Button key=Ui.button(this,digit,Ui.BLUE,()->{if(e.length()<32){int at=Math.max(0,e.getSelectionStart());e.getText().insert(at,digit);}});key.setTextSize(36);row.addView(key,new LinearLayout.LayoutParams(0,-1,1));}}
        LinearLayout editRow=new LinearLayout(this);editRow.addView(Ui.button(this,"清除",Ui.BLUE,()->e.setText("")),new LinearLayout.LayoutParams(0,-2,1));editRow.addView(Ui.button(this,"⌫ 刪除",Ui.BLUE,()->{int at=e.getSelectionStart();if(at>0)e.getText().delete(at-1,at);}),new LinearLayout.LayoutParams(0,-2,1));Ui.add(root,editRow);
        Ui.add(root,Ui.button(this,"☎ 打電話",Ui.GREEN,()->call(e.getText().toString())));Ui.add(root,Ui.button(this,"返回主畫面",Ui.RED,this::home));e.setSelection(e.length());
    }
    void contacts(){screen="contacts";LinearLayout c=form("通訊錄");Ui.add(c,Ui.button(this,"＋ 新增聯絡人",Ui.GREEN,()->edit(new Store.Person())));Ui.add(c,Ui.button(this,"從手機電話簿引用",Ui.BLUE,()->{
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},11);Ui.toast(this,"授權後請再按引用");return;}
        startActivityForResult(new Intent(Intent.ACTION_PICK,ContactsContract.CommonDataKinds.Phone.CONTENT_URI),20);
    }));for(Store.Person p:Store.people(this))Ui.add(c,Ui.button(this,p.name+" · 編輯",Ui.BLUE,()->edit(p)));}
    void edit(Store.Person p){screen="edit";editing=p;photo=p.photo;LinearLayout c=form("聯絡人資料");name=field(c,"名字",p.name,1);phone=field(c,"電話（與 LINE 至少填一個）",p.phone,3);line=field(c,"LINE 聊天顯示名稱",p.line,1);
        Ui.add(c,Ui.button(this,"選擇照片",Ui.BLUE,()->{stashEdit();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,21);}));
        ImageView image=Ui.photo(this,p);c.addView(image,new LinearLayout.LayoutParams(-1,Ui.dp(this,150)));
        Ui.add(c,Ui.button(this,"儲存聯絡人",Ui.GREEN,()->{
            stashEdit();if(p.name.isEmpty()||(p.phone.isEmpty()&&p.line.isEmpty())){Ui.toast(this,"請填名字，電話與 LINE 至少填一個");return;}if(!p.phone.isEmpty()&&!Rules.validPhone(p.phone)){Ui.toast(this,"電話號碼格式不正確");return;}
            ArrayList<Store.Person> all=Store.people(this);int index=-1;for(int i=0;i<all.size();i++)if(all.get(i).id.equals(p.id))index=i;if(index<0)all.add(p);else all.set(index,p);Store.save(this,all);contacts();
        }));
        Ui.add(c,Ui.button(this,"刪除此聯絡人",Ui.RED,()->new AlertDialog.Builder(this).setMessage("刪除 "+p.name+"？").setNegativeButton("取消",null).setPositiveButton("刪除",(d,w)->{ArrayList<Store.Person>a=Store.people(this);a.removeIf(x->x.id.equals(p.id));Store.save(this,a);contacts();}).show()));
    }
    void stashEdit(){if(editing!=null&&name!=null){editing.name=name.getText().toString().trim();editing.phone=phone.getText().toString().trim();editing.line=line.getText().toString().trim();editing.photo=photo;}}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;
        if(req==20){try(Cursor cur=getContentResolver().query(data.getData(),new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.PHOTO_URI},null,null,null)){if(cur!=null&&cur.moveToFirst()){Store.Person p=new Store.Person();p.name=cur.getString(0);p.phone=cur.getString(1);String u=cur.getString(2);if(u!=null)p.photo=copyPhoto(Uri.parse(u));edit(p);}}catch(Exception e){Ui.toast(this,"無法讀取此聯絡人");}}
        if(req==21&&editing!=null){try{photo=copyPhoto(data.getData());editing.photo=photo;edit(editing);}catch(Exception e){Ui.toast(this,"無法讀取照片，請選另一張");}}
    }
    String copyPhoto(Uri uri)throws IOException {
        // Decode sampled thumbnails; never retain a temporary provider URI or full-resolution photo.
        android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inJustDecodeBounds=true;
        try(InputStream in=getContentResolver().openInputStream(uri)){android.graphics.BitmapFactory.decodeStream(in,null,opts);}
        if(opts.outWidth<=0)throw new IOException("Invalid image");opts.inSampleSize=1;while(opts.outWidth/opts.inSampleSize>800||opts.outHeight/opts.inSampleSize>800)opts.inSampleSize*=2;opts.inJustDecodeBounds=false;
        android.graphics.Bitmap b;try(InputStream in=getContentResolver().openInputStream(uri)){b=android.graphics.BitmapFactory.decodeStream(in,null,opts);}if(b==null)throw new IOException("Invalid image");
        File dest=new File(getFilesDir(),UUID.randomUUID()+".jpg");try(OutputStream out=new FileOutputStream(dest)){b.compress(android.graphics.Bitmap.CompressFormat.JPEG,88,out);}finally{b.recycle();}return Uri.fromFile(dest).toString();
    }
}
