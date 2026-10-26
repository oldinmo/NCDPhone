package tw.edu.ctust.ncdphone;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.regex.*;

/** Reads data literals only: never evaluates downloaded JavaScript. */
final class CwaData {
    static String literal(String text,String variable){
        Matcher m=Pattern.compile("\\bvar\\s+"+Pattern.quote(variable)+"\\s*=\\s*([\\[{])").matcher(text);
        if(!m.find())throw new IllegalArgumentException("Missing data: "+variable);
        int start=m.end()-1,depth=0;char quote=0;boolean escaped=false;
        for(int i=start;i<text.length();i++){char c=text.charAt(i);
            if(quote!=0){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c==quote)quote=0;continue;}
            if(c=='\''||c=='"'){quote=c;continue;}
            if(c=='{'||c=='[')depth++;else if(c=='}'||c==']'){depth--;if(depth==0)return text.substring(start,i+1);}
        }throw new IllegalArgumentException("Incomplete weather data");
    }
    static long updated(String text){Matcher m=Pattern.compile("Updated:\\s*(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2})").matcher(text);if(!m.find())throw new IllegalArgumentException("Missing update time");return LocalDateTime.parse(m.group(1),DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")).atZone(ZoneId.of("Asia/Taipei")).toInstant().toEpochMilli();}
    static long time(String label,long reference){
        Matcher m=Pattern.compile("^(\\d{2}) (\\d{2})/(\\d{2})").matcher(label);if(!m.find())throw new IllegalArgumentException("Invalid time");
        int year=Instant.ofEpochMilli(reference).atZone(ZoneId.of("Asia/Taipei")).getYear();long best=0,distance=Long.MAX_VALUE;
        for(int y=year-1;y<=year+1;y++){try{long t=LocalDateTime.of(y,Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),Integer.parseInt(m.group(1)),0).atZone(ZoneId.of("Asia/Taipei")).toInstant().toEpochMilli();long d=Math.abs(t-reference);if(d<distance){best=t;distance=d;}}catch(DateTimeException ignored){}}
        if(distance==Long.MAX_VALUE)throw new IllegalArgumentException("Invalid date");return best;
    }
    static String name(String s){return s.replace('台','臺').replace('区','區').replace('乡','鄉').replace('镇','鎮');}
    static String symbol(String description,long at){
        int hour=Instant.ofEpochMilli(at).atZone(ZoneId.of("Asia/Taipei")).getHour();String suffix=hour<6||hour>=18?"_night":"_day";
        if(description.contains("雷"))return "rainandthunder";
        if(description.contains("雪"))return description.contains("雨")?"sleet":"snow";
        if(description.contains("雨"))return description.contains("大雨")||description.contains("豪雨")?"heavyrain":"rain";
        if(description.contains("霧"))return "fog";
        if(description.contains("多雲")||description.contains("晴時")||description.contains("晴轉"))return "partlycloudy"+suffix;
        if(description.contains("陰"))return "cloudy";
        if(description.contains("晴"))return "clearsky"+suffix;
        return "unknown";
    }
}
