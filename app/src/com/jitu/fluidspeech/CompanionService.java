package com.jitu.fluidspeech;
import android.accessibilityservice.*;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.view.accessibility.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;

/** Optional Android 13+ companion: leave the user's existing keyboard selected. */
public final class CompanionService extends AccessibilityService {
 private SpeechController speech;
 private WindowManager windows;
 private BubbleView bubble;
 private BubbleGestures gestures;
 private View reject,accept,dropTarget;
 private WindowManager.LayoutParams rejectPos,acceptPos,dropPos;
 private boolean controlsAttached,dropAttached,dropHot,activeSession;
 private String pendingText,pendingRaw;private long pendingToken;private String pendingPackage;private boolean pendingRemember;
 private final Rect safe=new Rect();private boolean moving;
 private final Runnable animate=new Runnable(){public void run(){if(bubble!=null&&keyboardVisible){bubble.state(speech.recording,speech.busy&&!speech.recording,speech.rmsLevel);ui.postDelayed(this,80);}}};
 private WindowManager.LayoutParams position;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private boolean attached,keyboardVisible,foreground,touching,sensitive;
 private long fieldVersion;
 private String targetPackage="";
 private int targetField;
 private final Object owner=new Object();
 private final Runnable update=this::updateOverlay;
 private final android.content.SharedPreferences.OnSharedPreferenceChangeListener preference=(p,k)->{if("action_size".equals(k)||"action_style".equals(k))removeControls();scheduleUpdate();};
 static boolean enabled(Context c){
  String all=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
  if(all==null)return false;
  ComponentName mine=new ComponentName(c,CompanionService.class);
  for(String name:all.split(":"))if(mine.equals(ComponentName.unflattenFromString(name)))return true;
  return false;
 }
 @Override protected void onServiceConnected(){
  speech=SpeechController.get(this);windows=(WindowManager)getSystemService(WINDOW_SERVICE);
  AccessibilityServiceInfo info=getServiceInfo();
  info.flags|=AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR|AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
  setServiceInfo(info);
  speech.prefs.p.registerOnSharedPreferenceChangeListener(preference);
  NotificationChannel channel=new NotificationChannel("dictation","Active dictation",NotificationManager.IMPORTANCE_LOW);
  channel.setDescription("Shown only while you dictate or transcribe.");channel.setSound(null,null);
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
  scheduleUpdate();
 }
 @Override public android.accessibilityservice.InputMethod onCreateInputMethod(){
  return new android.accessibilityservice.InputMethod(this){
   @Override public void onStartInput(EditorInfo info,boolean restarting){
    super.onStartInput(info,restarting);
    if(!restarting||targetField!=info.fieldId||!Objects.equals(targetPackage,info.packageName)){fieldVersion++;cancelDictation();}
    targetField=info.fieldId;targetPackage=info.packageName==null?"":info.packageName;
    int kind=info.inputType&InputType.TYPE_MASK_CLASS,v=info.inputType&InputType.TYPE_MASK_VARIATION;
    sensitive=(kind==InputType.TYPE_CLASS_TEXT&&(v==InputType.TYPE_TEXT_VARIATION_PASSWORD||v==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||v==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))||(kind==InputType.TYPE_CLASS_NUMBER&&v==InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    scheduleUpdate();
   }
   @Override public void onFinishInput(){fieldVersion++;cancelDictation();hideOverlay();super.onFinishInput();}
  };
 }
 @Override public void onAccessibilityEvent(AccessibilityEvent event){scheduleUpdate();}
 private void scheduleUpdate(){ui.removeCallbacks(update);ui.postDelayed(update,120);}
 private void updateOverlay(){
  if(speech==null||windows==null)return;
  KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
  android.accessibilityservice.InputMethod method=getInputMethod();
  if(!speech.prefs.p.getBoolean("companion",true)||speech.prefs.p.getBoolean("bubble_hidden",false)||keyguard.isKeyguardLocked()||sensitive||method==null||!method.getCurrentInputStarted()){hideOverlay();return;}
  Rect bounds=null;
  try{for(AccessibilityWindowInfo window:getWindows())if(window.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){Rect r=new Rect();window.getBoundsInScreen(r);if(r.height()>80)bounds=r;}}
  catch(Exception ignored){}
  if(bounds==null){hideOverlay();return;}
  boolean wasVisible=keyboardVisible;keyboardVisible=true;
  if(bubble==null)createBubble();
  WindowMetrics metrics=windows.getCurrentWindowMetrics();Rect screen=metrics.getBounds();
  android.graphics.Insets insets=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
  safe.set(insets.left+Ui.dp(this,6),insets.top+Ui.dp(this,6),screen.width()-insets.right-Ui.dp(this,6),screen.height()-insets.bottom-Ui.dp(this,6));
  int size=Ui.dp(this,Math.max(36,Math.min(80,speech.prefs.p.getInt("bubble_size",48))));position.width=size;position.height=size;
  if(!moving){
   int nx=speech.prefs.p.getInt("bubble_x",-1),ny=speech.prefs.p.getInt("bubble_y",-1);
   position.x=nx<0?safe.left+Ui.dp(this,10):safe.left+Math.round(nx/1000f*Math.max(0,safe.width()-size));
   position.y=ny<0?Math.max(safe.top,bounds.top-size-Ui.dp(this,56)):safe.top+Math.round(ny/1000f*Math.max(0,safe.height()-size));
  }
  clamp();keepControlsOnScreen();bubble.setAlpha(Math.max(.4f,Math.min(1,speech.prefs.p.getInt("bubble_opacity",100)/100f)));bubble.reload();
  try{if(!attached){windows.addView(bubble,position);attached=true;ui.removeCallbacks(animate);ui.post(animate);}else{
   ViewGroup.LayoutParams old=bubble.getLayoutParams();
   // Framework window events can be caused by our overlay. Only mutate when bounds change.
   if(bubble.getWidth()!=size||bubble.getHeight()!=size||lastX!=position.x||lastY!=position.y)windows.updateViewLayout(bubble,position);
  }lastX=position.x;lastY=position.y;}catch(Exception ignored){attached=false;}
  syncControls();
  if(!wasVisible&&speech.prefs.preload())speech.preload();
 }
 private int lastX=-1,lastY=-1;
 private void clamp(){position.x=Math.max(safe.left,Math.min(position.x,safe.right-position.width));position.y=Math.max(safe.top,Math.min(position.y,safe.bottom-position.height));}
 private void move(float dx,float dy){moving=true;position.x+=Math.round(dx);position.y+=Math.round(dy);clamp();keepControlsOnScreen();if(attached)try{windows.updateViewLayout(bubble,position);lastX=position.x;lastY=position.y;}catch(Exception ignored){}syncControls();showDropTarget();}
 private void savePosition(){
  if(overDropTarget()){hideDropTarget();moving=false;cancelDictation();speech.prefs.p.edit().putBoolean("bubble_hidden",true).apply();hideOverlay();Toast.makeText(this,"Voice button hidden. Open Phone Whisper → Home to show it again.",Toast.LENGTH_LONG).show();return;}
  hideDropTarget();moving=false;int x=Math.round(1000f*(position.x-safe.left)/Math.max(1,safe.width()-position.width)),y=Math.round(1000f*(position.y-safe.top)/Math.max(1,safe.height()-position.height));
  speech.prefs.p.edit().putInt("bubble_x",x).putInt("bubble_y",y).apply();
 }
 private boolean controlsVisible(){return activeSession;}
 private WindowManager.LayoutParams overlayParams(int width,int height,boolean touchable){
  int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
  if(!touchable)flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,flags,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.LEFT;return p;
 }
 private void keepControlsOnScreen(){
  if(!controlsVisible()||position==null)return;
  int side=OverlayControls.hitSize(this,speech.prefs)+Ui.dp(this,8);
  int left=safe.left+side,right=safe.right-position.width-side;
  if(right>=left)position.x=Math.max(left,Math.min(position.x,right));
 }
 private void syncControls(){
  if(!attached||!keyboardVisible||!controlsVisible()){removeControls();return;}
  int previous=position.x;keepControlsOnScreen();
  if(previous!=position.x)try{windows.updateViewLayout(bubble,position);lastX=position.x;}catch(Exception ignored){}
  int hit=OverlayControls.hitSize(this,speech.prefs),gap=Ui.dp(this,8),y=position.y+(position.height-hit)/2;
  int rejectX=position.x-hit-gap,acceptX=position.x+position.width+gap;
  if(!controlsAttached){
   reject=OverlayControls.button(this,speech.prefs,false,this::discard);accept=OverlayControls.button(this,speech.prefs,true,this::confirm);
   rejectPos=overlayParams(hit,hit,true);acceptPos=overlayParams(hit,hit,true);
   rejectPos.x=rejectX;rejectPos.y=y;acceptPos.x=acceptX;acceptPos.y=y;
   try{windows.addView(reject,rejectPos);windows.addView(accept,acceptPos);controlsAttached=true;}catch(Exception e){removeControls();return;}
  }else{
   // Overlay layout updates can themselves trigger Accessibility events.
   // Only reposition changed windows so they cannot create an event feedback loop.
   try{
    if(rejectPos.x!=rejectX||rejectPos.y!=y){rejectPos.x=rejectX;rejectPos.y=y;windows.updateViewLayout(reject,rejectPos);}
    if(acceptPos.x!=acceptX||acceptPos.y!=y){acceptPos.x=acceptX;acceptPos.y=y;windows.updateViewLayout(accept,acceptPos);}
   }catch(Exception e){removeControls();return;}
  }
 }
 private void removeControls(){if(windows!=null){if(reject!=null)try{windows.removeView(reject);}catch(Exception ignored){}if(accept!=null)try{windows.removeView(accept);}catch(Exception ignored){}}reject=null;accept=null;rejectPos=null;acceptPos=null;controlsAttached=false;}
 private Rect dropBounds(){int width=Ui.dp(this,172),height=Ui.dp(this,56);return new Rect(safe.centerX()-width/2,safe.bottom-height-Ui.dp(this,5),safe.centerX()+width/2,safe.bottom-Ui.dp(this,5));}
 private boolean overDropTarget(){if(position==null||safe.isEmpty())return false;Rect r=dropBounds();return r.contains(position.x+position.width/2,position.y+position.height/2);}
 private void showDropTarget(){if(windows==null||safe.isEmpty())return;Rect r=dropBounds();if(!dropAttached){dropTarget=OverlayControls.dismiss(this);dropPos=overlayParams(r.width(),r.height(),false);dropPos.x=r.left;dropPos.y=r.top;try{windows.addView(dropTarget,dropPos);dropAttached=true;}catch(Exception ignored){dropTarget=null;return;}}
  dropHot=overDropTarget();dropTarget.setAlpha(dropHot?1f:.72f);
 }
 private void hideDropTarget(){if(dropAttached&&windows!=null)try{windows.removeView(dropTarget);}catch(Exception ignored){}dropAttached=false;dropTarget=null;dropPos=null;dropHot=false;}
 private void clearPending(){pendingText=null;pendingRaw=null;pendingPackage=null;pendingRemember=false;}
 private void toggleRecording(){
  if(activeSession&&speech.recording){touching=false;speech.stop(owner);setCaption("Transcribing",false);}
  else if(activeSession&&speech.busy){Toast.makeText(this,"Finishing your words…",Toast.LENGTH_SHORT).show();}
  else begin();
 }
 private void createBubble(){
  bubble=new BubbleView(this,speech.prefs);bubble.setElevation(Ui.dp(this,6));
  position=new WindowManager.LayoutParams(Ui.dp(this,48),Ui.dp(this,48),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  position.gravity=Gravity.TOP|Gravity.LEFT;
  gestures=new BubbleGestures(new BubbleGestures.Actions(){
   public boolean tapMode(){return speech.prefs.p.getInt("record_mode",1)==1;}
   public boolean startHold(){if(speech.busy)return false;begin();return touching;}
   public void stopHold(){touching=false;speech.stop(owner);setCaption("Transcribing",false);}
   public void cancelHold(){cancelDictation();}
   public void tap(){toggleRecording();}
   public void drag(float dx,float dy){move(dx,dy);}
   public void dropped(){savePosition();}
   public void aborted(){moving=false;hideDropTarget();scheduleUpdate();}
  },Ui.dp(this,8));bubble.setOnTouchListener(gestures);
  bubble.setAccessibilityDelegate(new View.AccessibilityDelegate(){
   @Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);}
   @Override public boolean performAccessibilityAction(View host,int action,Bundle args){if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK){toggleRecording();return true;}return super.performAccessibilityAction(host,action,args);}
  });setCaption("Ready",false);
 }
 private void begin(){
  android.accessibilityservice.InputMethod method=getInputMethod();EditorInfo editor=method==null?null:method.getCurrentInputEditorInfo();
  if(editor==null||sensitive||!keyboardVisible)return;
  if(!speech.ready()){Toast.makeText(this,"Open Phone Whisper and download the speech model first.",Toast.LENGTH_LONG).show();return;}
  if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"Open Phone Whisper once and allow microphone access.",Toast.LENGTH_LONG).show();return;}
  try{
   PendingIntent settings=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification notification=new Notification.Builder(this,"dictation").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Phone Whisper dictation").setContentText("Tap again or release the voice button to finish.").setContentIntent(settings).setOngoing(true).setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE).build();
   startForeground(41,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);foreground=true;
  }catch(Exception e){Toast.makeText(this,"Android blocked microphone access. Open Phone Whisper to check permissions.",Toast.LENGTH_LONG).show();return;}
  clearPending();final long token=fieldVersion;final String app=targetPackage;
  final boolean remember=(editor.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)==0;
  touching=speech.start(owner,new SpeechController.Callback(){
   public void status(String s){if(token==fieldVersion){if(s.startsWith("Listening"))setCaption("Listening…",true);else setCaption("Transcribing…",false);}}
   public void error(String s){stopRecordingNotice();touching=false;activeSession=false;clearPending();setCaption("Try again",false);scheduleUpdate();Toast.makeText(CompanionService.this,s,Toast.LENGTH_LONG).show();resetLater();}
   public void result(String raw,String text,long milliseconds){
    stopRecordingNotice();touching=false;activeSession=false;
    if(token!=fieldVersion||!keyboardVisible||!app.equals(targetPackage)||sensitive){clearPending();setCaption("Field changed",false);return;}
    if(text.isEmpty()){clearPending();setCaption("No speech heard",false);resetLater();return;}
    // The user already finished recording. Insert as soon as recognition
    // completes, but only into the exact still-active input field.
    pendingText=text;pendingRaw=raw;pendingToken=token;pendingPackage=app;pendingRemember=remember;
    confirm();
   }
  },false);
  activeSession=touching;
  if(touching){bubble.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);setCaption("Listening…",true);}else stopRecordingNotice();
 }
 private void discard(){cancelDictation();setCaption("Discarded",false);scheduleUpdate();resetLater();}
 private void confirm(){
  if(activeSession&&speech.recording){touching=false;speech.stop(owner);setCaption("Transcribing",false);return;}
  if(activeSession&&speech.busy){Toast.makeText(this,"Still transcribing…",Toast.LENGTH_SHORT).show();return;}
  if(pendingText==null)return;
  if(pendingToken!=fieldVersion||!keyboardVisible||sensitive||!pendingPackage.equals(targetPackage)){discard();Toast.makeText(this,"The text field changed. Nothing was pasted.",Toast.LENGTH_LONG).show();return;}
  android.accessibilityservice.InputMethod method=getInputMethod();
  android.accessibilityservice.InputMethod.AccessibilityInputConnection ic=method==null?null:method.getCurrentInputConnection();
  if(ic==null){discard();Toast.makeText(this,"No text field is active. Nothing was pasted.",Toast.LENGTH_LONG).show();return;}
  String text=pendingText,raw=pendingRaw;boolean remember=pendingRemember;
  try{
   SurroundingText around=ic.getSurroundingText(1,0,0);boolean prefix=false;
   if(around!=null&&around.getText()!=null&&around.getSelectionStart()==around.getSelectionEnd()&&around.getSelectionStart()>0&&around.getSelectionStart()<=around.getText().length()){
    char ch=around.getText().charAt(around.getSelectionStart()-1);prefix=Character.isLetterOrDigit(ch);
   }
   ic.commitText((prefix?" ":"")+text,1,null);
   if(speech.prefs.p.getBoolean("copy",false)){ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("Dictation",text));}
   if(remember)speech.prefs.remember(raw,text);
   clearPending();setCaption("Pasted",false);scheduleUpdate();resetLater();
  }catch(Exception e){
   ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("Dictation",text));
   if(remember)speech.prefs.remember(raw,text);
   clearPending();setCaption("Copy available",false);scheduleUpdate();
   Toast.makeText(this,"Could not insert. Dictation copied to clipboard.",Toast.LENGTH_LONG).show();resetLater();
  }
 }
 private void setCaption(String s,boolean listening){
  if(bubble!=null){bubble.state(speech.recording,speech.busy&&!speech.recording,speech.rmsLevel);bubble.setContentDescription("Phone Whisper. "+s+". "+(activeSession?"Check finishes and pastes; X discards.":"Tap to speak.")+" Drag to move.");syncControls();}
 }
 private void resetLater(){ui.postDelayed(()->{if(!touching&&speech!=null&&!speech.busy&&pendingText==null)setCaption("Ready",false);},1800);}
 private void stopRecordingNotice(){if(foreground){stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;}}
 private void cancelDictation(){touching=false;activeSession=false;clearPending();if(speech!=null)speech.cancel(owner);stopRecordingNotice();syncControls();}
 private void hideOverlay(){
  clearPending();removeControls();hideDropTarget();
  if(keyboardVisible){cancelDictation();if(speech!=null){if(speech.prefs.p.getBoolean("unload_hide",false))speech.unload();else speech.scheduleIdle();}}
  keyboardVisible=false;ui.removeCallbacks(animate);if(gestures!=null)gestures.clear();
  if(attached&&windows!=null)try{windows.removeView(bubble);}catch(Exception ignored){}finally{attached=false;}
 }
 @Override public void onInterrupt(){cancelDictation();hideOverlay();}
 @Override public void onDestroy(){
  cancelDictation();hideOverlay();ui.removeCallbacksAndMessages(null);
  if(speech!=null)speech.prefs.p.unregisterOnSharedPreferenceChangeListener(preference);super.onDestroy();
 }
}
