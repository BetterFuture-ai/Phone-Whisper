package com.jitu.fluidspeech;
import android.content.*;
import android.inputmethodservice.InputMethodService;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import org.json.*;
import java.util.*;

public final class VoiceKeyboard extends InputMethodService {
 private SpeechController speech;
 private LinearLayout root,panel;
 private TextView status;
 private Button mic;
 private boolean shift=false,symbols=false,visible=false,sensitive=false,historyAllowed=true;
 private long fieldVersion;
 private final Object captureOwner=new Object();
 private final Handler repeat=new Handler(Looper.getMainLooper());
 private Runnable repeatDelete;
 private final int BG=Color.rgb(17,26,33),KEY=Color.rgb(38,51,60),INK=Color.rgb(239,245,242),ACCENT=Color.rgb(255,94,13);
 @Override public void onCreate(){super.onCreate();speech=SpeechController.get(this);}
 @Override public boolean onEvaluateFullscreenMode(){return false;}
 @Override public View onCreateInputView(){return makeKeyboard();}
 private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
 private GradientDrawable background(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(9));return g;}
 private Button button(String label){
  Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(INK);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinimumHeight(0);b.setMinHeight(0);b.setPadding(dp(2),0,dp(2),0);b.setBackground(background(KEY));return b;
 }
 private void add(LinearLayout row,Button b,float weight,int height){
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(height),weight);p.setMargins(dp(2),dp(3),dp(2),dp(3));row.addView(b,p);
 }
 private LinearLayout row(LinearLayout parent){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);parent.addView(r,new LinearLayout.LayoutParams(-1,-2));return r;}
 private View makeKeyboard(){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(4),dp(5),dp(4),dp(8));root.setBackgroundColor(BG);
  LinearLayout toolbar=row(root);
  Button globe=button("🌐");globe.setContentDescription("Switch keyboard");globe.setOnClickListener(v->switchKeyboard());add(toolbar,globe,0.7f,40);
  Button clips=button("📋");clips.setContentDescription("Clipboard");clips.setOnClickListener(v->showClipboard());add(toolbar,clips,0.7f,40);
  Button emojis=button("☺");emojis.setContentDescription("Emoji");emojis.setOnClickListener(v->showEmoji());add(toolbar,emojis,0.7f,40);
  mic=button("🎤 Hold to speak");mic.setTextColor(BG);mic.setBackground(background(ACCENT));mic.setContentDescription("Hold to speak, release to insert text");mic.setOnTouchListener((v,e)->micTouch(e));add(toolbar,mic,2.7f,40);
  Button settings=button("⚙");settings.setContentDescription("Phone Whisper settings");settings.setOnClickListener(v->{speech.cancel(captureOwner);startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));});add(toolbar,settings,0.7f,40);
  status=new TextView(this);status.setTextColor(Color.rgb(158,183,181));status.setTextSize(12);status.setPadding(dp(7),dp(3),dp(7),dp(3));status.setText(sensitive?"Voice disabled in password fields":"Fluid Speech Mini · English · hold mic, then release");root.addView(status);
  panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);root.addView(panel);
  showKeys();return root;
 }
 private boolean micTouch(MotionEvent e){
  if(e.getAction()==MotionEvent.ACTION_DOWN){
   if(sensitive){status.setText("Voice is disabled in password fields.");return true;}
   final long token=fieldVersion;
   boolean started=speech.start(captureOwner,new SpeechController.Callback(){
    public void status(String s){if(token==fieldVersion&&visible)status.setText(s);}
    public void error(String s){if(token==fieldVersion&&visible){status.setText(s);resetMic();}}
    public void result(String raw,String text,long millis){
     if(token!=fieldVersion||!visible)return;
     resetMic();InputConnection ic=getCurrentInputConnection();
     if(ic==null){status.setText("Text field closed. Last dictation is in Phone Whisper.");return;}
     if(text.isEmpty()){status.setText("No speech recognized. Hold to try again.");return;}
     CharSequence selected=ic.getSelectedText(0),before=ic.getTextBeforeCursor(1,0);
     boolean prefix=(selected==null||selected.length()==0)&&before!=null&&before.length()>0&&Character.isLetterOrDigit(before.charAt(before.length()-1));
     ic.commitText((prefix?" ":"")+text,1);
     if(speech.prefs.p.getBoolean("copy",false)){
      ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Dictation",text));
     }
     status.setText("Inserted · "+String.format(Locale.ROOT,"%.1f",millis/1000.0)+"s · hold to speak again");
    }
   },historyAllowed);
   if(started){mic.setText("● Listening");mic.setBackground(background(Color.rgb(237,132,119)));}
   return true;
  }
  if(e.getAction()==MotionEvent.ACTION_UP){speech.stop(captureOwner);resetMic();return true;}
  if(e.getAction()==MotionEvent.ACTION_CANCEL){speech.cancel(captureOwner);resetMic();return true;}
  return true;
 }
 private void resetMic(){if(mic!=null){mic.setText("🎤 Hold to speak");mic.setBackground(background(ACCENT));}}
 private void showKeys(){
  panel.removeAllViews();
  String[] rows=symbols?new String[]{"1234567890","@#$%&*-+=","()!?;:'\"/"}:new String[]{"qwertyuiop","asdfghjkl","zxcvbnm"};
  for(int n=0;n<rows.length;n++){
   LinearLayout r=row(panel);
   if(n==2){Button sh=button(symbols?"ABC":(shift?"⇧ ON":"⇧"));sh.setOnClickListener(v->{if(symbols)symbols=false;else shift=!shift;showKeys();});add(r,sh,1.4f,43);}
   for(char ch:rows[n].toCharArray()){
    String value=symbols?String.valueOf(ch):(shift?String.valueOf(ch).toUpperCase(Locale.ROOT):String.valueOf(ch));
    Button key=button(value);key.setOnClickListener(v->{commit(value);if(shift&&!symbols){shift=false;showKeys();}});add(r,key,1,43);
   }
   if(n==2){Button del=button("⌫");del.setContentDescription("Delete");del.setOnTouchListener((v,e)->{
    if(e.getAction()==MotionEvent.ACTION_DOWN){delete();repeatDelete=new Runnable(){public void run(){delete();repeat.postDelayed(this,65);}};repeat.postDelayed(repeatDelete,350);}
    else if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){repeat.removeCallbacksAndMessages(null);}
    return true;
   });add(r,del,1.4f,43);}
  }
  LinearLayout bottom=row(panel);
  Button sym=button(symbols?"ABC":"?123");sym.setOnClickListener(v->{symbols=!symbols;showKeys();});add(bottom,sym,1.2f,44);
  Button comma=button(",");comma.setOnClickListener(v->commit(","));add(bottom,comma,0.7f,44);
  Button space=button("space");space.setOnClickListener(v->commit(" "));add(bottom,space,4,44);
  Button dot=button(".");dot.setOnClickListener(v->commit("."));add(bottom,dot,0.7f,44);
  Button enter=button("↵");enter.setContentDescription("Enter");enter.setOnClickListener(v->enter());add(bottom,enter,1.2f,44);
 }
 private void commit(String s){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);}
 private void delete(){InputConnection ic=getCurrentInputConnection();if(ic==null)return;CharSequence selected=ic.getSelectedText(0);if(selected!=null&&selected.length()>0)ic.commitText("",1);else ic.deleteSurroundingTextInCodePoints(1,0);}
 private void enter(){
  InputConnection ic=getCurrentInputConnection();EditorInfo info=getCurrentInputEditorInfo();if(ic==null)return;
  int action=info==null?EditorInfo.IME_ACTION_NONE:info.imeOptions&EditorInfo.IME_MASK_ACTION;
  if(action!=EditorInfo.IME_ACTION_NONE&&action!=EditorInfo.IME_ACTION_UNSPECIFIED&&(info.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION)==0)ic.performEditorAction(action);else ic.commitText("\n",1);
 }
 private void switchKeyboard(){speech.cancel(captureOwner);android.view.inputmethod.InputMethodManager m=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);m.showInputMethodPicker();}
 private void panelHeader(String title){
  panel.removeAllViews();LinearLayout r=row(panel);Button back=button("← Keys");back.setOnClickListener(v->showKeys());add(r,back,1,38);
  Button text=button(title);text.setEnabled(false);add(r,text,3,38);
 }
 private void showEmoji(){
  panelHeader("Emoji");
  String[][] rows={{"😀","😂","😊","😍","🥰","😎"},{"👍","🙏","❤️","🔥","✅","🎉"},{"🙂","🤔","😅","😭","👀","💯"}};
  for(String[] values:rows){LinearLayout r=row(panel);for(String s:values){Button b=button(s);b.setTextSize(23);b.setOnClickListener(v->commit(s));add(r,b,1,43);}}
 }
 private void showClipboard(){
  panelHeader("Clipboard");
  if(sensitive){status.setText("Clipboard is hidden in password fields.");return;}
  String clip="";
  try{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm.hasPrimaryClip()&&cm.getPrimaryClip().getItemCount()>0){CharSequence c=cm.getPrimaryClip().getItemAt(0).coerceToText(this);if(c!=null)clip=c.toString();}}catch(Exception ignored){}
  final String current=clip;
  if(!current.isEmpty()){Button paste=button("Paste: "+shorten(current));paste.setOnClickListener(v->commit(current));panel.addView(paste,new LinearLayout.LayoutParams(-1,dp(48)));}
  if(!speech.lastText.isEmpty()){Button last=button("Last dictation: "+shorten(speech.lastText));last.setOnClickListener(v->commit(speech.lastText));panel.addView(last,new LinearLayout.LayoutParams(-1,dp(48)));}
  if(current.isEmpty()&&speech.lastText.isEmpty()){TextView empty=new TextView(this);empty.setText("Your current copied text will appear here.\nClipboard is read only when you open this panel.");empty.setTextColor(INK);empty.setPadding(dp(12),dp(12),dp(12),dp(12));panel.addView(empty);}
 }
 private String shorten(String s){s=s.replace('\n',' ');return s.length()>55?s.substring(0,55)+"…":s;}
 @Override public void onStartInput(EditorInfo info,boolean restarting){
  super.onStartInput(info,restarting);speech.cancel(captureOwner);fieldVersion++;
  int variation=info.inputType&InputType.TYPE_MASK_VARIATION;
  sensitive=(info.inputType&InputType.TYPE_MASK_CLASS)==InputType.TYPE_CLASS_TEXT&&(variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
  sensitive|=(info.inputType&InputType.TYPE_MASK_CLASS)==InputType.TYPE_CLASS_NUMBER&&variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD;
  historyAllowed=!sensitive&&(info.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)==0;
  symbols=(info.inputType&InputType.TYPE_MASK_CLASS)==InputType.TYPE_CLASS_NUMBER||(info.inputType&InputType.TYPE_MASK_CLASS)==InputType.TYPE_CLASS_PHONE;
 }
 @Override public void onStartInputView(EditorInfo info,boolean restarting){
  super.onStartInputView(info,restarting);visible=true;setInputView(makeKeyboard());
  if(!sensitive&&speech.prefs.preload())speech.preload();
 }
 @Override public void onFinishInputView(boolean finishingInput){visible=false;fieldVersion++;speech.cancel(captureOwner);repeat.removeCallbacksAndMessages(null);if(speech.prefs.p.getBoolean("unload_hide",false))speech.unload();else speech.scheduleIdle();super.onFinishInputView(finishingInput);}
 @Override public void onFinishInput(){visible=false;fieldVersion++;speech.cancel(captureOwner);super.onFinishInput();}
 @Override public void onDestroy(){speech.cancel(captureOwner);repeat.removeCallbacksAndMessages(null);super.onDestroy();}
}
