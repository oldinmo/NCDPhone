package tw.edu.ctust.ncdphone;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import java.util.*;

/** Launch only enabled, exported activities discovered through HOME / LAUNCHER intents. */
final class AppShortcuts {
    static final String KEY="shortcutComponent", HOME="originalHomeComponent";
    static ComponentName component(ResolveInfo r){return new ComponentName(r.activityInfo.packageName,r.activityInfo.name);}
    static ArrayList<ResolveInfo> candidates(Activity a,String category){
        ArrayList<ResolveInfo> out=new ArrayList<>();Set<String> packages=new HashSet<>();
        for(ResolveInfo r:a.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)){
            ActivityInfo i=r.activityInfo;
            if(i!=null&&i.exported&&i.enabled&&i.applicationInfo.enabled&&!i.packageName.equals(a.getPackageName())&&packages.add(i.packageName))out.add(r);
        }
        java.text.Collator order=java.text.Collator.getInstance(Locale.TAIWAN);
        out.sort((x,y)->order.compare(x.loadLabel(a.getPackageManager()).toString(),y.loadLabel(a.getPackageManager()).toString()));return out;
    }
    static ResolveInfo selected(Activity a){
        String saved=Store.prefs(a).getString(KEY,"");if(saved.isEmpty())return null;
        for(ResolveInfo r:candidates(a,Intent.CATEGORY_LAUNCHER))if(component(r).flattenToString().equals(saved))return r;return null;
    }
    interface Pick {void chosen(ResolveInfo info);}
    static void picker(Activity a,String title,ArrayList<ResolveInfo> items,Pick pick){
        if(items.isEmpty()){Ui.toast(a,"沒有可開啟的應用程式");return;}
        String[] labels=new String[items.size()];for(int i=0;i<items.size();i++)labels[i]=items.get(i).loadLabel(a.getPackageManager()).toString();
        new AlertDialog.Builder(a).setTitle(title).setItems(labels,(d,index)->pick.chosen(items.get(index))).setNegativeButton("取消",null).show();
    }
    static void choose(Activity a,Runnable changed){picker(a,"選擇首頁 App 快捷鍵",candidates(a,Intent.CATEGORY_LAUNCHER),r->{Store.prefs(a).edit().putString(KEY,component(r).flattenToString()).apply();changed.run();});}
    static void openShortcut(Activity a){ResolveInfo r=selected(a);if(r==null){Ui.toast(a,"此 App 已無法開啟，請在設定重新選擇");return;}launch(a,r,Intent.CATEGORY_LAUNCHER);}
    static void openHome(Activity a){
        ArrayList<ResolveInfo> homes=candidates(a,Intent.CATEGORY_HOME);
        if(homes.isEmpty()){Ui.toast(a,"找不到其他桌面");return;}
        String saved=Store.prefs(a).getString(HOME,"");
        for(ResolveInfo r:homes)if(component(r).flattenToString().equals(saved)){launch(a,r,Intent.CATEGORY_HOME);return;}
        ResolveInfo preferred=a.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
        if(preferred!=null)for(ResolveInfo r:homes)if(component(r).equals(component(preferred))){rememberHome(a,r);return;}
        ArrayList<ResolveInfo> system=new ArrayList<>();for(ResolveInfo r:homes)if((r.activityInfo.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)!=0)system.add(r);
        if(homes.size()==1){rememberHome(a,homes.get(0));return;}
        if(system.size()==1){rememberHome(a,system.get(0));return;}
        picker(a,"選擇原桌面",homes,r->rememberHome(a,r));
    }
    static void rememberHome(Activity a,ResolveInfo r){if(launch(a,r,Intent.CATEGORY_HOME))Store.prefs(a).edit().putString(HOME,component(r).flattenToString()).apply();}
    static boolean launch(Activity a,ResolveInfo r,String category){
        try{a.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(category).setComponent(component(r)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));return true;}
        catch(ActivityNotFoundException|SecurityException e){Ui.toast(a,"無法開啟，請重新選擇");return false;}
    }
}
