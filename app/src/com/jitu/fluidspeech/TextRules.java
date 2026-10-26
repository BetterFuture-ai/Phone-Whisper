package com.jitu.fluidspeech;
import java.util.*;
import java.util.regex.*;
import java.text.SimpleDateFormat;
/** Literal, single-pass corrections. Replacements cannot cascade into each other. */
public final class TextRules {
 public static String apply(String text,String dictionary,String snippets){
  LinkedHashMap<String,String> rules=parse(dictionary);
  String result=replace(text,rules);
  for(Map.Entry<String,String> e:parse(snippets).entrySet()){
   String spoken=result.trim().replaceAll("[.!?]+$","").trim();
   if(spoken.equalsIgnoreCase(e.getKey())){
    result=e.getValue().replace("{{DATE}}",new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date())).replace("{{TIME}}",new SimpleDateFormat("HH:mm",Locale.ROOT).format(new Date()));
    break;
   }
  }
  return result;
 }
 static LinkedHashMap<String,String> parse(String lines){
  LinkedHashMap<String,String> out=new LinkedHashMap<>();
  for(String line:lines.split("\\r?\\n")){
   int at=line.indexOf("=>");if(at<1)continue;
   String from=line.substring(0,at).trim(),to=line.substring(at+2).trim();
   if(!from.isEmpty()&&!to.isEmpty())out.put(from,to.replace("\\n","\n"));
  }return out;
 }
 private static String replace(String text,LinkedHashMap<String,String> rules){
  if(rules.isEmpty())return text;
  List<String> keys=new ArrayList<>(rules.keySet());Collections.sort(keys,(a,b)->Integer.compare(b.length(),a.length()));
  StringBuilder p=new StringBuilder("(?<![\\p{L}\\p{N}_])(?:");
  for(String key:keys){if(p.charAt(p.length()-1)!=':')p.append('|');p.append(Pattern.quote(key));}
  p.append(")(?![\\p{L}\\p{N}_])");
  Matcher m=Pattern.compile(p.toString(),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(text);StringBuffer out=new StringBuffer();
  while(m.find()){
   String target=m.group();for(String key:keys)if(key.equalsIgnoreCase(m.group())){target=rules.get(key);break;}
   m.appendReplacement(out,Matcher.quoteReplacement(target));
  }m.appendTail(out);return out.toString();
 }
}