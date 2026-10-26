package tw.edu.ctust.ncdphone;

import android.content.*;
import org.json.*;
import java.util.*;

final class Store {
    static final class Person {
        String id = UUID.randomUUID().toString(), name = "", phone = "", line = "", photo = "";
        JSONObject json() throws JSONException {
            return new JSONObject().put("id", id).put("name", name).put("phone", phone).put("line", line).put("photo", photo);
        }
    }
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("ncd", Context.MODE_PRIVATE); }
    static ArrayList<Person> people(Context c) {
        ArrayList<Person> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("people", "[]"));
            for (int i=0;i<a.length();i++) {
                JSONObject o=a.getJSONObject(i); Person p=new Person();
                p.id=o.getString("id"); p.name=o.optString("name"); p.phone=o.optString("phone");
                p.line=o.optString("line"); p.photo=o.optString("photo"); out.add(p);
            }
        } catch (JSONException e) { android.util.Log.e("NCDPhone", "Contact storage invalid", e); }
        return out;
    }
    static void save(Context c, List<Person> people) {
        JSONArray a=new JSONArray();
        try { for(Person p:people) a.put(p.json()); } catch(JSONException e) { throw new IllegalStateException(e); }
        prefs(c).edit().putString("people",a.toString()).apply();
    }
    static Person find(Context c, String number) {
        for(Person p:people(c)) if(Rules.samePhone(p.phone,number)) return p;
        return null;
    }
    static boolean flag(Context c,String key) { return prefs(c).getBoolean(key,false); }
}
