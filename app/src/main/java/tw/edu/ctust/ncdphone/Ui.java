package tw.edu.ctust.ncdphone;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;

final class Ui {
    static final int NAVY=Color.rgb(16,29,48), BLUE=Color.rgb(0,65,132), GREEN=Color.rgb(0,103,51), RED=Color.rgb(176,21,36);
    static int dp(Context c,int n) { return (int)(n*c.getResources().getDisplayMetrics().density+.5f); }
    static GradientDrawable bg(int color) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(18); return d; }
    static LinearLayout root(Activity a) {
        LinearLayout l=new LinearLayout(a); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(NAVY); l.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,8));
        a.setContentView(l);
        l.setOnApplyWindowInsetsListener((v,insets)->{ if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(a,12)+i.left,dp(a,8)+i.top,dp(a,12)+i.right,dp(a,8)+i.bottom);}return insets; });
        l.requestApplyInsets(); return l;
    }
    static TextView text(Context c,String s,int sp) {
        TextView t=new TextView(c); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.WHITE); t.setGravity(Gravity.CENTER); t.setPadding(8,10,8,10); return t;
    }
    static Button button(Context c,String text,int color,Runnable action) {
        Button b=new Button(c); b.setText(text); b.setTextSize(24); b.setTextColor(Color.WHITE); b.setTypeface(null,Typeface.BOLD); b.setAllCaps(false); b.setBackground(bg(color)); b.setMinHeight(dp(c,64));
        if(color==RED){b.setText(text.replace("☎","").trim());b.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hangup,0,0,0);b.setPadding(dp(c,24),8,dp(c,24),8);}
        forgiving(b,action); return b;
    }
    static View largeCall(Context c,Runnable action){
        LinearLayout tile=new LinearLayout(c);tile.setOrientation(LinearLayout.HORIZONTAL);tile.setGravity(Gravity.CENTER);tile.setBackground(bg(GREEN));tile.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));
        ImageView icon=new ImageView(c);icon.setImageResource(R.drawable.ic_phone);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(icon,new LinearLayout.LayoutParams(0,-1,1));
        TextView label=text(c,"打電話",44);label.setAutoSizeTextTypeUniformWithConfiguration(28,44,2,android.util.TypedValue.COMPLEX_UNIT_SP);label.setMaxLines(1);label.setTypeface(null,Typeface.BOLD);label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(label,new LinearLayout.LayoutParams(0,-1,1));
        tile.setContentDescription("打電話");forgiving(tile,action);return tile;
    }
    // Execute once on release, tolerate long holds and movement inside bounds; suppress duplicate taps.
    static void forgiving(View v,Runnable action) {
        final long[] last={-2000}; final boolean[] tracking={false},inside={false};
        v.setClickable(true); v.setFocusable(true);
        v.setOnClickListener(w->{if(w.getContext() instanceof MainActivity)((MainActivity)w.getContext()).activity();long now=SystemClock.elapsedRealtime(); if(now-last[0]<850)return;last[0]=now;action.run();});
        v.setOnTouchListener((w,e)->{
            switch(e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: tracking[0]=true;inside[0]=true;w.setPressed(true);return true;
                case MotionEvent.ACTION_MOVE: inside[0]=e.getX()>=0&&e.getY()>=0&&e.getX()<w.getWidth()&&e.getY()<w.getHeight();w.setPressed(inside[0]);return true;
                case MotionEvent.ACTION_UP: boolean fire=tracking[0]&&inside[0];tracking[0]=false;w.setPressed(false);if(fire)w.performClick();return true;
                case MotionEvent.ACTION_CANCEL: tracking[0]=false;w.setPressed(false);return true;
                default:return true;
            }
        });
    }
    static void add(LinearLayout l,View v) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,6,0,6);l.addView(v,p); }
    static void toast(Context c,String s){Toast.makeText(c,s,Toast.LENGTH_LONG).show();}
    static void home(Context c) { c.startActivity(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("home",true)); }
    static ImageView photo(Context c,Store.Person p) {
        ImageView v=new ImageView(c);v.setScaleType(ImageView.ScaleType.CENTER_CROP);v.setBackground(bg(Color.rgb(219,231,245)));
        if(p!=null&&!p.photo.isEmpty()) {try{v.setImageURI(android.net.Uri.parse(p.photo));}catch(Exception ignored){} }
        if(v.getDrawable()==null) {v.setImageResource(R.drawable.ic_person_silhouette);v.setScaleType(ImageView.ScaleType.FIT_CENTER);int pad=dp(c,12);v.setPadding(pad,pad,pad,pad);}
        v.setContentDescription(p==null?"聯絡人照片":p.name+"的照片");return v;
    }
}
