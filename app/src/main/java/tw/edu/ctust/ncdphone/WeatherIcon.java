package tw.edu.ctust.ncdphone;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Resolution-independent high-contrast weather artwork, composed locally without image downloads. */
final class WeatherIcon extends View {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);String kind="unknown";
    WeatherIcon(Context c){super(c);}
    void symbol(String value){kind=WeatherKind.kind(value);setContentDescription(WeatherKind.label(value));invalidate();}
    void color(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);}
    void sun(Canvas c){color(0xffa65d00);c.drawCircle(46,43,23,p);p.setStrokeWidth(5);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(46+(float)Math.cos(a)*29,43+(float)Math.sin(a)*29,46+(float)Math.cos(a)*38,43+(float)Math.sin(a)*38,p);}}
    void moon(Canvas c){color(0xff9a6700);Path path=new Path();path.moveTo(69,15);path.cubicTo(23,5,15,72,66,78);path.cubicTo(41,59,44,29,69,15);c.drawPath(path,p);}
    void cloud(Canvas c){color(0xff4b647c);c.drawCircle(41,63,20,p);c.drawCircle(64,49,27,p);c.drawCircle(88,64,19,p);c.drawRoundRect(23,62,108,86,13,13,p);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);Canvas c=canvas;c.save();float size=Math.min(getWidth(),getHeight());c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/128,size/128);p.setStrokeCap(Paint.Cap.ROUND);
        if(kind.equals("unknown")){color(0xff4b647c);p.setTextSize(76);p.setTextAlign(Paint.Align.CENTER);c.drawText("?",64,92,p);}
        else if(kind.equals("typhoon")){color(0xff6b3db5);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(10);c.drawArc(28,28,100,100,10,290,false,p);c.drawArc(8,8,120,120,180,130,false,p);c.drawArc(8,8,120,120,0,130,false,p);color(0xffffffff);c.drawCircle(64,64,10,p);}
        else if(kind.equals("clear"))sun(c);
        else if(kind.equals("clear_night"))moon(c);
        else {
            if(kind.equals("partly"))sun(c);if(kind.equals("partly_night"))moon(c);cloud(c);
            if(kind.contains("rain")||kind.equals("sleet")||kind.contains("thunder")){color(0xff0072a8);p.setStrokeWidth(kind.equals("heavy_rain")?6:4);for(int i=0;i<3;i++)c.drawLine(40+i*25,94,33+i*25,108,p);}
            if(kind.contains("snow")||kind.contains("sleet")){color(0xff12648b);p.setStrokeWidth(3);for(int i=0;i<3;i++){float x=35+i*28,y=108;c.drawLine(x-6,y,x+6,y,p);c.drawLine(x,y-6,x,y+6,p);}}
            if(kind.contains("thunder")){color(0xffa65d00);Path bolt=new Path();bolt.moveTo(69,72);bolt.lineTo(52,100);bolt.lineTo(65,100);bolt.lineTo(56,125);bolt.lineTo(86,88);bolt.lineTo(71,88);bolt.close();c.drawPath(bolt,p);}
            if(kind.equals("fog")){color(0xff4b647c);p.setStrokeWidth(5);c.drawLine(23,98,108,98,p);c.drawLine(34,111,97,111,p);}
        }c.restore();
    }
}
