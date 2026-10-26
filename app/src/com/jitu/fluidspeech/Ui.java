package com.jitu.fluidspeech;
import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.content.res.ColorStateList;import android.view.*;import android.widget.*;
final class Ui {
 static final int BG=Color.rgb(248,247,244),INK=Color.rgb(30,29,28),MUTED=Color.rgb(102,99,96),ACCENT=Color.rgb(169,84,61),MINT=Color.rgb(245,233,226),LINE=Color.rgb(230,226,220),NAVY=Color.rgb(34,33,32);
 static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static GradientDrawable shape(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
 static TextView text(Context c,String s,float size,int color,boolean bold){
  TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setLineSpacing(dp(c,3),1);t.setIncludeFontPadding(false);return t;
 }
 static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(1);return v;}
 static LinearLayout row(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(0);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 static LinearLayout card(Context c,boolean dark){
  LinearLayout v=column(c);v.setPadding(dp(c,18),dp(c,18),dp(c,18),dp(c,18));
  GradientDrawable d=shape(c,dark?NAVY:Color.WHITE,20);if(!dark)d.setStroke(dp(c,1),LINE);v.setBackground(d);
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,7),0,dp(c,7));v.setLayoutParams(p);return v;
 }
 static Button button(Context c,String s,boolean primary,Runnable action){
  Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.create("sans-serif-medium",0));b.setTextColor(primary?Color.WHITE:ACCENT);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setPadding(dp(c,14),dp(c,8),dp(c,14),dp(c,8));b.setStateListAnimator(null);
  b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22A9543D),shape(c,primary?ACCENT:MINT,12),null));b.setOnClickListener(v->action.run());
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,4),0,dp(c,4));b.setLayoutParams(p);return b;
 }
 static Button smallButton(Context c,String s,Runnable action){
  Button b=button(c,s,false,action);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setTextSize(13);b.setPadding(dp(c,12),0,dp(c,12),0);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x18A9543D),shape(c,BG,10),null));
  b.setLayoutParams(new LinearLayout.LayoutParams(-2,dp(c,48)));return b;
 }
 static void gap(LinearLayout p,int n){View v=new View(p.getContext());p.addView(v,new LinearLayout.LayoutParams(1,dp(p.getContext(),n)));}
 static final class Icon extends View {
  private final Paint p=new Paint(3);final String name;int color;
  Icon(Context c,String name,int color){super(c);this.name=name;this.color=color;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas original){
   super.onDraw(original);Canvas c=original;int save=c.save();c.scale(getWidth()/24f,getHeight()/24f);
   p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
   if(name.equals("brand")){
    p.setStyle(Paint.Style.FILL);c.drawRoundRect(1,1,23,23,7,7,p);p.setColor(Color.WHITE);
    c.drawRoundRect(6,9,8.5f,15,1.25f,1.25f,p);c.drawRoundRect(10.75f,5.5f,13.25f,18.5f,1.25f,1.25f,p);c.drawRoundRect(15.5f,8,18,16,1.25f,1.25f,p);
   }else if(name.equals("close")){
    c.drawLine(6,6,18,18,p);c.drawLine(18,6,6,18,p);
   }else if(name.equals("mic")){
    c.drawRoundRect(9,3,15,14,3,3,p);Path a=new Path();a.moveTo(5.5f,11);a.lineTo(5.5f,13);a.cubicTo(5.5f,21,18.5f,21,18.5f,13);a.lineTo(18.5f,11);c.drawPath(a,p);c.drawLine(12,20,12,23,p);
   }else if(name.equals("home")){
    Path a=new Path();a.moveTo(3,10);a.lineTo(12,3);a.lineTo(21,10);a.moveTo(5,9);a.lineTo(5,21);a.lineTo(19,21);a.lineTo(19,9);a.moveTo(9,21);a.lineTo(9,14);a.lineTo(15,14);a.lineTo(15,21);c.drawPath(a,p);
   }else if(name.equals("model")){
    c.drawRoundRect(5,5,19,19,3,3,p);c.drawRoundRect(9,9,15,15,1,1,p);for(int i=8;i<=16;i+=4){c.drawLine(i,2,i,5,p);c.drawLine(i,19,i,22,p);c.drawLine(2,i,5,i,p);c.drawLine(19,i,22,i,p);}
   }else if(name.equals("words")){
    c.drawRoundRect(4,3,20,21,2,2,p);c.drawLine(8,3,8,21,p);c.drawLine(11,8,17,8,p);c.drawLine(11,12,17,12,p);c.drawLine(11,16,15,16,p);
   }else if(name.equals("history")){
    c.drawCircle(12,12,8.5f,p);c.drawLine(12,7,12,12,p);c.drawLine(12,12,16,14,p);
   }else if(name.equals("settings")){
    c.drawCircle(12,12,3,p);c.drawCircle(12,12,8,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine((float)(12+8*Math.cos(a)),(float)(12+8*Math.sin(a)),(float)(12+10*Math.cos(a)),(float)(12+10*Math.sin(a)),p);}
   }else if(name.equals("check")){
    Path a=new Path();a.moveTo(5,12);a.lineTo(10,17);a.lineTo(20,6);c.drawPath(a,p);
   }else if(name.equals("download")){
    c.drawLine(12,3,12,15,p);c.drawLine(7,10,12,15,p);c.drawLine(17,10,12,15,p);Path a=new Path();a.moveTo(4,16);a.lineTo(4,21);a.lineTo(20,21);a.lineTo(20,16);c.drawPath(a,p);
   }else if(name.equals("arrow")){
    c.drawLine(5,12,19,12,p);c.drawLine(14,7,19,12,p);c.drawLine(14,17,19,12,p);
   }else if(name.equals("clipboard")){
    c.drawRoundRect(5,5,19,21,2,2,p);c.drawRoundRect(9,2,15,7,1,1,p);c.drawLine(9,11,15,11,p);c.drawLine(9,15,15,15,p);
   }else if(name.equals("emoji")){
    c.drawCircle(12,12,9,p);c.drawPoint(9,9,p);c.drawPoint(15,9,p);c.drawArc(7,8,17,17,25,130,false,p);
   }else{c.drawCircle(12,12,8,p);c.drawLine(4,12,20,12,p);c.drawOval(8,4,16,20,p);}
   c.restoreToCount(save);
  }
 }
}
