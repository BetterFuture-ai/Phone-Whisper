package com.jitu.fluidspeech;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Compact visible controls with at least 48dp touch areas. */
final class OverlayControls {
 private OverlayControls(){}
 static int hitSize(Context c,AppPrefs prefs){return Ui.dp(c,Math.max(48,prefs.p.getInt("action_size",38)));}
 static View button(Context c,AppPrefs prefs,boolean confirm,Runnable action){
  int hit=hitSize(c,prefs),disc=Ui.dp(c,Math.max(32,Math.min(56,prefs.p.getInt("action_size",38))));
  boolean outline=prefs.p.getInt("action_style",0)==1;
  FrameLayout view=new FrameLayout(c);view.setMinimumWidth(hit);view.setMinimumHeight(hit);
  GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);
  int accent=confirm?Ui.NAVY:0xffac6159;
  circle.setColor(outline?Color.WHITE:accent);
  if(outline)circle.setStroke(Ui.dp(c,1.5f),accent);
  View surface=new View(c);surface.setBackground(circle);surface.setElevation(Ui.dp(c,3));
  FrameLayout.LayoutParams bg=new FrameLayout.LayoutParams(disc,disc,Gravity.CENTER);view.addView(surface,bg);
  // Keep the glyph above the elevated background surface. The earlier custom
  // Icon had no elevation, so Android drew the colored circle over the X/✓.
  TextView mark=new TextView(c);
  mark.setText(confirm?"✓":"✕");mark.setTextColor(outline?accent:Color.WHITE);
  mark.setTextSize(23);mark.setGravity(Gravity.CENTER);
  mark.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.BOLD));
  mark.setIncludeFontPadding(false);mark.setElevation(Ui.dp(c,5));
  view.addView(mark,new FrameLayout.LayoutParams(disc,disc,Gravity.CENTER));
  GradientDrawable mask=new GradientDrawable();mask.setShape(GradientDrawable.OVAL);mask.setColor(Color.WHITE);
  view.setForeground(new RippleDrawable(ColorStateList.valueOf(0x33000000),null,mask));
  view.setContentDescription(confirm?"Finish or paste dictation":"Cancel and discard dictation");
  view.setClickable(true);view.setFocusable(true);view.setOnClickListener(v->action.run());
  return view;
 }
 static View dismiss(Context c){
  LinearLayout row=Ui.row(c);row.setGravity(Gravity.CENTER);row.setPadding(Ui.dp(c,16),0,Ui.dp(c,16),0);
  row.setBackground(Ui.shape(c,0xffaa554d,28));
  Ui.Icon icon=new Ui.Icon(c,"close",Color.WHITE);row.addView(icon,new LinearLayout.LayoutParams(Ui.dp(c,19),Ui.dp(c,19)));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=Ui.dp(c,8);
  TextView label=Ui.text(c,"Release to hide",13,Color.WHITE,true);row.addView(label,lp);
  return row;
 }
}
