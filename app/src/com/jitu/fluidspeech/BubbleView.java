package com.jitu.fluidspeech;
import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.SystemClock;import android.view.*;
/** Small overlay footprint. Only this circle intercepts touches; animation stays inside it. */
final class BubbleView extends View {
 final AppPrefs prefs;private final Paint paint=new Paint(3);
 private Drawable logo;private Bitmap custom;private long customStamp;
 boolean recording,processing,preview;float level;
 BubbleView(Context c,AppPrefs p){super(c);prefs=p;setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);setClickable(true);reload();}
 void reload(){
  logo=getContext().getApplicationInfo().loadIcon(getContext().getPackageManager());
  java.io.File f=new java.io.File(getContext().getFilesDir(),"bubble-logo.png");
  if(f.lastModified()!=customStamp){customStamp=f.lastModified();if(custom!=null)custom.recycle();custom=f.exists()?BitmapFactory.decodeFile(f.getPath()):null;}
  invalidate();
 }
 void state(boolean rec,boolean busy,float volume){recording=rec;processing=busy;level=volume;invalidate();}
 @Override protected void onDraw(Canvas canvas){
  float size=Math.min(getWidth(),getHeight()),r=size/2-2,cx=getWidth()/2f,cy=getHeight()/2f;
  boolean rec=recording||preview;paint.setStyle(Paint.Style.FILL);paint.setColor(rec?Ui.NAVY:Ui.ACCENT);canvas.drawCircle(cx,cy,r,paint);
  float time=SystemClock.uptimeMillis()/1000f*prefs.p.getInt("wave_speed",100)/100f;
  int style=prefs.p.getInt("wave_style",0);float volume=preview?(float)(.45+.4*Math.sin(time*2)):Math.min(1,level*prefs.p.getInt("wave_strength",100)/100f);
  if(rec){
   paint.setColor(Ui.ACCENT);
   if(style==0){paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(size*.065f);for(int i=0;i<5;i++){float h=size*(.09f+.29f*volume)*(float)(.55+.45*Math.sin(time*6+i*1.8));float x=cx+(i-2)*size*.125f;canvas.drawLine(x,cy-h,x,cy+h,paint);}}
   else if(style==1){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(size*.055f);canvas.drawCircle(cx,cy,r*(.45f+.32f*volume),paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(cx,cy,size*.11f,paint);}
   else if(style==2){paint.setAlpha(80);canvas.drawCircle(cx,cy,r*(.5f+.4f*volume),paint);paint.setAlpha(255);canvas.drawCircle(cx,cy,size*.16f,paint);}
   else{paint.setColor(Ui.MINT);canvas.drawRoundRect(cx-size*.14f,cy-size*.14f,cx+size*.14f,cy+size*.14f,size*.04f,size*.04f,paint);}
  }else if(processing){
   paint.setColor(Ui.MINT);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(size*.06f);canvas.drawArc(cx-r*.62f,cy-r*.62f,cx+r*.62f,cy+r*.62f,time*180,250,false,paint);
  }else{
   int icon=prefs.p.getInt("bubble_icon",0);float inner=size*.54f;
   if(icon==3&&custom!=null){int save=canvas.save();Path clip=new Path();clip.addCircle(cx,cy,inner/2,Path.Direction.CW);canvas.clipPath(clip);canvas.drawBitmap(custom,null,new RectF(cx-inner/2,cy-inner/2,cx+inner/2,cy+inner/2),paint);canvas.restoreToCount(save);}
   else if(icon==2||icon==0){
    paint.setColor(Color.WHITE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(size*.07f);
    for(int i=0;i<(icon==0?3:5);i++){int middle=icon==0?1:2;float h=size*(i==middle?.23f:(Math.abs(i-middle)==1?.16f:.10f));float x=cx+(i-middle)*size*(icon==0?.18f:.12f);canvas.drawLine(x,cy-h,x,cy+h,paint);}
   }
   else{paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(size*.05f);canvas.drawRoundRect(cx-size*.09f,cy-size*.23f,cx+size*.09f,cy+size*.09f,size*.08f,size*.08f,paint);canvas.drawArc(cx-size*.18f,cy-size*.1f,cx+size*.18f,cy+size*.2f,0,180,false,paint);canvas.drawLine(cx,cy+size*.2f,cx,cy+size*.29f,paint);}
  }
  paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
  if(isAttachedToWindow()&&(rec&&style!=3||processing))postInvalidateDelayed(40);
 }
}
