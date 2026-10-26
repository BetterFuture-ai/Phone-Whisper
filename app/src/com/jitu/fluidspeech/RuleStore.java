package com.jitu.fluidspeech;
import java.util.*;
/** Keeps existing user entries and backup format compatible with the original app. */
final class RuleStore {
 static LinkedHashMap<String,String> read(AppPrefs p,String key){return TextRules.parse(p.p.getString(key,""));}
 static void save(AppPrefs p,String key,LinkedHashMap<String,String> entries){
  StringBuilder b=new StringBuilder();
  for(Map.Entry<String,String> e:entries.entrySet()){if(b.length()>0)b.append('\n');b.append(e.getKey()).append(" => ").append(e.getValue().replace("\r","").replace("\n","\\n"));}
  p.p.edit().putString(key,b.toString()).apply();
 }
}
