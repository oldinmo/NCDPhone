package tw.edu.ctust.ncdphone;

final class WeatherKind {
    static String district(String...candidates){for(String s:candidates)if(s!=null&&s.matches(".*[區区鄉乡鎮镇]$"))return s.replace('区','區').replace('乡','鄉').replace('镇','鎮');for(String s:candidates)if(s!=null&&!s.isEmpty()&&!s.endsWith("里")&&!s.endsWith("村"))return s;return "";}
    static String kind(String symbol){
        if(symbol==null)return "unknown";
        String s=symbol.toLowerCase(java.util.Locale.ROOT);
        if(s.equals("typhoon"))return "typhoon"; // Reserved for a verified official alert, never inferred from wind.
        if(s.contains("thunder"))return s.contains("snow")?"snow_thunder":s.contains("sleet")?"sleet_thunder":"thunder";
        if(s.contains("sleet"))return "sleet";
        if(s.contains("snow"))return s.contains("heavy")?"heavy_snow":"snow";
        if(s.contains("rain"))return s.contains("heavy")?"heavy_rain":s.contains("light")?"light_rain":"rain";
        if(s.contains("fog"))return "fog";
        if(s.contains("partlycloudy")||s.contains("fair"))return s.endsWith("_night")?"partly_night":"partly";
        if(s.contains("cloudy"))return "cloudy";
        if(s.contains("clearsky"))return s.endsWith("_night")?"clear_night":"clear";
        return "unknown";
    }
    static String label(String symbol){switch(kind(symbol)){
        case "clear":return "晴天";case "clear_night":return "晴朗夜晚";case "partly":return "晴時多雲";case "partly_night":return "夜間多雲";case "cloudy":return "陰天";
        case "light_rain":return "小雨";case "rain":return "有雨";case "heavy_rain":return "大雨";case "fog":return "有霧";
        case "thunder":return "雷雨";case "sleet":return "雨夾雪";case "snow":return "降雪";case "heavy_snow":return "大雪";
        case "sleet_thunder":return "雷雨夾雪";case "snow_thunder":return "雷雪";case "typhoon":return "颱風警報";default:return "氣象暫無資料";
    }}
}
