package com.jitu.fluidspeech;
public final class TextRulesTest {
 static void check(String want,String input,String dictionary,String snippets){
  String got=TextRules.apply(input,dictionary,snippets);
  if(!want.equals(got))throw new AssertionError("Expected ["+want+"], got ["+got+"]");
 }
 public static void main(String[] a){
  check("Use Elementor today.","Use elementer today.","elementer => Elementor","");
  check("Dogx catalog concatenate.","Cat catalog concatenate.","cat => Dogx\ncatalogue => item","");
  check("C++ and $5","C plus plus and five dollars","C plus plus => C++\nfive dollars => $5","");
  check("beta","alpha","alpha => beta\nbeta => gamma","");
  check("TypeWhisper works","TYPE WHISPER works","type whisper => TypeWhisper","");
  check("Dear Sam,\nThank you.","my sign off.","","my sign off => Dear Sam,\\nThank you.");
  check("Please say my sign off later.","Please say my sign off later.","","my sign off => ignored");
  check("New York City","new york city","new york => NY\nnew york city => New York City","");
  System.out.println("PASS: 8 dictionary/snippet behavior cases.");
 }
}