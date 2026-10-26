package com.jitu.fluidspeech;
import android.content.*;import org.json.*;
final class AppPrefs {
 final android.content.SharedPreferences p;
 AppPrefs(Context c){p=c.getSharedPreferences("fluid-keys",0);}
 int idle(){return p.getInt("idle",60);}
 int threads(){return Math.max(1,Math.min(8,p.getInt("threads",2)));}
 int maxSeconds(){return Math.max(0,p.getInt("seconds",30));}
 boolean preload(){return p.getBoolean("preload",false);}
 String correct(String raw){return TextRules.apply(raw,p.getString("dictionary",""),p.getString("snippets",""));}
 void remember(String raw,String text){
  if(!p.getBoolean("history",false))return;
  try{
   JSONArray old=new JSONArray(p.getString("entries","[]")),a=new JSONArray();JSONObject row=new JSONObject();row.put("time",System.currentTimeMillis());row.put("raw",raw);row.put("text",text);a.put(row);
   for(int i=0;i<Math.min(old.length(),19);i++)a.put(old.get(i));
   p.edit().putString("entries",a.toString()).apply();
  }catch(JSONException ignored){}
 }
}