package tw.edu.ctust.ncdphone;

import android.app.Activity;
import android.view.*;
import android.widget.*;
import com.google.android.gms.ads.*;
import com.google.android.ump.*;

/** One banner per Activity. Never alters SDK touch handling or automatically clicks an ad. */
final class BannerController {
    final Activity activity;
    final ConsentInformation consent;
    AdView banner;
    FrameLayout slot;
    boolean ready,initializing,destroyed,visible;
    BannerController(Activity a){activity=a;consent=UserMessagingPlatform.getConsentInformation(a);}
    void start(){
        consent.requestConsentInfoUpdate(activity,new ConsentRequestParameters.Builder().build(),
            ()->UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity,error->initializeIfAllowed()),
            error->initializeIfAllowed());
        initializeIfAllowed();
    }
    void initializeIfAllowed(){
        boolean officialTestUnit=BuildConfig.BANNER_AD_UNIT_ID.startsWith("ca-app-pub-3940256099942544/");
        if(destroyed||initializing||(!officialTestUnit&&!consent.canRequestAds()))return;
        initializing=true;
        new Thread(()->MobileAds.initialize(activity.getApplicationContext(),status->activity.runOnUiThread(()->{if(!destroyed){ready=true;load();}})),"ads-init").start();
    }
    FrameLayout attach(){
        visible=true;slot=new FrameLayout(activity);slot.setMinimumHeight(Ui.dp(activity,50));
        TextView label=Ui.text(activity,"廣告",12);label.setTextColor(0xffb7c8dd);slot.addView(label,new FrameLayout.LayoutParams(-1,-1));
        if(banner!=null){detachBanner();slot.addView(banner,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));banner.resume();}
        slot.post(this::load);return slot;
    }
    void load(){
        if(!ready||destroyed||!visible||slot==null||banner!=null||slot.getWidth()==0)return;
        int width=(int)(slot.getWidth()/activity.getResources().getDisplayMetrics().density);
        banner=new AdView(activity);banner.setAdUnitId(BuildConfig.BANNER_AD_UNIT_ID);
        banner.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity,width));
        slot.addView(banner,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));
        banner.loadAd(new AdRequest.Builder().build());
    }
    void detachBanner(){if(banner!=null&&banner.getParent() instanceof ViewGroup)((ViewGroup)banner.getParent()).removeView(banner);}
    void pause(){visible=false;if(banner!=null)banner.pause();}
    void resume(){visible=true;if(banner!=null)banner.resume();load();}
    void privacy(){UserMessagingPlatform.showPrivacyOptionsForm(activity,error->{if(error!=null)Ui.toast(activity,"目前沒有可調整的廣告隱私選項");});}
    void destroy(){destroyed=true;if(banner!=null){detachBanner();banner.destroy();banner=null;}}
}
