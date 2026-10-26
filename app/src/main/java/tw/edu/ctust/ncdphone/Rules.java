package tw.edu.ctust.ncdphone;

/** Pure policy rules also exercised by the standalone JVM checks. */
final class Rules {
    static boolean sameLineName(String a,String b){return a!=null&&b!=null&&!a.trim().isEmpty()&&a.trim().equalsIgnoreCase(b.trim());}
    static String normalize(String value) {
        String s=value==null?"":value.replaceAll("[^0-9+]", "");
        if(s.startsWith("00886")) s="0"+s.substring(5);
        else if(s.startsWith("+886")) s="0"+s.substring(4);
        return s;
    }
    static boolean samePhone(String a,String b) {
        String x=normalize(a), y=normalize(b);
        return x.length()>=7 && x.equals(y);
    }
    static boolean validPhone(String s) { return s.matches("\\+?[0-9 ()-]{3,25}") && normalize(s).length()>=3; }
    static int pageCount(int size) { return Math.max(1,(size+3)/4); }
    static int contactIndex(int page,int slot,int size) {
        int i=page*4+slot; return i<size?i:-1;
    }
}
