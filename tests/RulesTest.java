package tw.edu.ctust.ncdphone;
public class RulesTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Check "+checks+" failed");}
    public static void main(String[] args){
        check(WeatherKind.district("舊社里","北屯區","臺中市").equals("北屯區"));check(WeatherKind.district("舊社里",null,null).isEmpty());
        check(WeatherKind.kind("clearsky_day").equals("clear"));check(WeatherKind.kind("clearsky_night").equals("clear_night"));
        check(WeatherKind.kind("partlycloudy_day").equals("partly"));check(WeatherKind.kind("cloudy").equals("cloudy"));
        check(WeatherKind.kind("heavyrainandthunder").equals("thunder"));check(WeatherKind.kind("heavyrain").equals("heavy_rain"));
        check(WeatherKind.kind("sleetshowers_night").equals("sleet"));check(WeatherKind.kind("lightsnowshowersandthunder_day").equals("snow_thunder"));
        check(WeatherKind.kind("fog").equals("fog"));check(WeatherKind.kind("unknown_wind").equals("unknown"));
        check(WeatherKind.kind("typhoon").equals("typhoon"));check(WeatherKind.kind(null).equals("unknown"));
        FlipDetector flip=new FlipDetector();check(!flip.update(-9,0));check(!flip.update(9,200));
        check(!flip.update(-9,1000));check(flip.update(0,1800));check(!flip.update(9,1900));
        check(!flip.update(-9,2000));check(flip.update(9,70000));
        check(!flip.update(-9,71000));flip.reset();check(!flip.update(9,72000));
        check(Rules.isPageSwipe(-180,20,72));check(Rules.isPageSwipe(150,-30,72));check(!Rules.isPageSwipe(30,3,72));check(!Rules.isPageSwipe(80,100,72));
        check(Rules.sameLineName("Cathy tu","Cathy Tu"));check(!Rules.sameLineName("Cathy","Cathy Tu"));check(!Rules.sameLineName(""," "));
        check(Rules.samePhone("0912-345-678","+886912345678"));
        check(Rules.samePhone("02 1234 5678","00886212345678"));
        check(!Rules.samePhone("",""));check(!Rules.samePhone("0912345678","0992345678"));
        check(!Rules.samePhone("12345678","9912345678"));
        check(Rules.validPhone("+886 912-345-678"));check(!Rules.validPhone("*21*0912345678#"));check(!Rules.validPhone("abc"));
        check(Rules.pageCount(0)==1);check(Rules.pageCount(4)==1);check(Rules.pageCount(5)==2);check(Rules.pageCount(9)==3);
        check(Rules.contactIndex(1,0,5)==4);check(Rules.contactIndex(1,1,5)==-1);
        check(CwaData.literal("var X = {'a':['}',1]}; ignored();","X").equals("{'a':['}',1]}"));
        boolean incomplete=false;try{CwaData.literal("var X = {'a':[1]","X");}catch(IllegalArgumentException ex){incomplete=true;}check(incomplete);
        long yearEnd=CwaData.updated("// Updated: 2026/12/31 23:00:00");
        check(CwaData.time("00 01/01<br>",yearEnd)-yearEnd==3600000L);
        check(CwaData.time("22 12/31<br>",yearEnd)-yearEnd==-3600000L);
        check(CwaData.name("台中市北屯区").equals("臺中市北屯區"));
        check(CwaData.symbol("多雲短暫陣雨",yearEnd).equals("rain"));
        check(CwaData.symbol("陰有雷陣雨",yearEnd).equals("rainandthunder"));
        check(CwaData.symbol("晴",yearEnd).equals("clearsky_night"));
        check(CwaData.symbol("陰",yearEnd).equals("cloudy"));
        check(CwaData.symbol("多雲",yearEnd).equals("partlycloudy_night"));
        System.out.println(checks+" policy checks passed");
    }
}
