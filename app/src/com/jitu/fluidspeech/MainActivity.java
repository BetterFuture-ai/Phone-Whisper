package com.jitu.fluidspeech;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.graphics.Color;import android.os.*;import android.provider.Settings;import android.view.*;import android.view.inputmethod.InputMethodManager;import android.widget.*;import org.json.*;import java.io.*;import java.text.DateFormat;import java.util.*;
public final class MainActivity extends Activity {
 private SpeechController speech;private AppPrefs prefs;private LinearLayout content;
 private TextView status,modelBadge;private EditText transcript;private Button hold;
 private int tab=0,wordSection=0;private BubbleView bubblePreview;private LinearLayout controlPreview;private String exportPending,draft="";
 private final int GREEN=Ui.ACCENT,DARK=Ui.INK,MUTED=Ui.MUTED;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable refresh=new Runnable(){public void run(){if(status!=null)status.setText(speech.state);if(modelBadge!=null)modelBadge.setText(speech.engine!=null?"MODEL LOADED":(speech.ready()?"READY ON DEVICE":"SETUP NEEDED"));handler.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle state){super.onCreate(state);setTheme(R.style.AppTheme);speech=SpeechController.get(this);prefs=speech.prefs;getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);render();}
 private int dp(int n){return Ui.dp(this,n);}
 private TextView label(String s,int size,int color){return Ui.text(this,s,size,color,false);}
 private void render(){
  if(transcript!=null)draft=transcript.getText().toString();
  status=null;modelBadge=null;transcript=null;hold=null;bubblePreview=null;controlPreview=null;
  LinearLayout outer=Ui.column(this);outer.setBackgroundColor(Ui.BG);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  content=Ui.column(this);content.setPadding(dp(22),dp(22),dp(22),dp(24));scroll.addView(content);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout brand=Ui.row(this);brand.addView(new Ui.Icon(this,"brand",Ui.ACCENT),new LinearLayout.LayoutParams(dp(22),dp(22)));
  TextView name=Ui.text(this,"Phone Whisper",17,Ui.INK,true);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.leftMargin=dp(9);brand.addView(name,np);brand.addView(Ui.text(this,"PRIVATE · OFFLINE",10,Ui.MUTED,true));content.addView(brand);Ui.gap(content,24);
  if(tab==0)homePage();else if(tab==1)modelPage();else if(tab==2)wordsPage();else if(tab==3)historyPage();else settingsPage();
  LinearLayout nav=Ui.row(this);nav.setPadding(dp(9),dp(8),dp(9),dp(8));nav.setBackgroundColor(Color.WHITE);
  String[] names={"Home","Model","Words","History","Settings"},icons={"home","model","words","history","settings"};
  for(int i=0;i<names.length;i++){final int n=i;LinearLayout item=Ui.column(this);item.setGravity(Gravity.CENTER);item.setPadding(dp(2),dp(8),dp(2),dp(8));if(tab==i)item.setBackground(Ui.shape(this,Ui.MINT,14));
   item.addView(new Ui.Icon(this,icons[i],tab==i?Ui.ACCENT:Ui.MUTED),new LinearLayout.LayoutParams(dp(21),dp(21)));Ui.gap(item,5);TextView navLabel=Ui.text(this,names[i],10,tab==i?Ui.ACCENT:Ui.MUTED,tab==i);navLabel.setGravity(Gravity.CENTER);item.addView(navLabel);
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(62),1);p.setMargins(dp(2),0,dp(2),0);nav.addView(item,p);item.setContentDescription(names[i]);item.setClickable(true);item.setFocusable(true);item.setOnClickListener(v->{speech.cancel(this);tab=n;render();});}
  outer.addView(nav,new LinearLayout.LayoutParams(-1,-2));scroll.setFocusableInTouchMode(true);scroll.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);setContentView(outer);scroll.requestFocus();scroll.post(()->scroll.scrollTo(0,0));
 }
 private void heading(String s){content.addView(Ui.text(this,s,21,Ui.INK,true));Ui.gap(content,8);}
 private void text(String s){content.addView(Ui.text(this,s,14,Ui.MUTED,false));Ui.gap(content,8);}
 private Button button(String s,Runnable r){Button b=Ui.button(this,s,false,r);content.addView(b);return b;}
 private Button primary(String s,Runnable r){Button b=Ui.button(this,s,true,r);content.addView(b);return b;}
 private void card(Runnable r){LinearLayout parent=content,c=Ui.card(this,false);parent.addView(c);content=c;r.run();content=parent;}
 private void homePage(){
  TextView headline=Ui.text(this,"Speak freely.\nKeep Gboard.",35,Ui.INK,true);headline.setLineSpacing(dp(1),1);content.addView(headline);Ui.gap(content,12);text("A quieter way to type. Your voice becomes text, without leaving the keyboard you know.");
  LinearLayout hero=Ui.card(this,true);content.addView(hero);modelBadge=Ui.text(this,speech.engine!=null?"MODEL LOADED":(speech.ready()?"READY ON DEVICE":"SETUP NEEDED"),10,Ui.MINT,true);hero.addView(modelBadge);Ui.gap(hero,14);
  hero.addView(Ui.text(this,"Fluid Speech Mini",23,Color.WHITE,true));Ui.gap(hero,8);hero.addView(Ui.text(this,"English · offline · on your phone",13,0xffd5cfca,false));Ui.gap(hero,14);
  status=Ui.text(this,speech.state,13,0xffd5cfca,false);hero.addView(status);Ui.gap(hero,12);
  Button setup=Ui.button(this,CompanionService.enabled(this)?"Voice button settings":"Finish setup",true,()->{if(!speech.ready()){tab=1;render();}else if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);else openCompanionSettings();});
  setup.setTextColor(Ui.NAVY);setup.setBackground(Ui.shape(this,Ui.MINT,12));hero.addView(setup);Ui.gap(content,18);content.addView(Ui.text(this,"GET READY",11,Ui.MUTED,true));Ui.gap(content,8);
  setupRow("download","Download your speech model","One download. No subscription.",speech.ready(),()->{tab=1;render();});
  setupRow("mic","Allow your microphone","Records only when you press the mic.",checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED,()->requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1));
  setupRow("close","Keep your current keyboard","Enable the companion voice button.",CompanionService.enabled(this),this::openCompanionSettings);Ui.gap(content,16);
  if(prefs.p.getBoolean("bubble_hidden",false))card(()->{heading("Voice button hidden");text("You dragged it to the red zone. Your keyboard and accessibility access are still ready.");primary("Show floating button",()->{prefs.p.edit().putBoolean("bubble_hidden",false).putBoolean("companion",true).apply();render();});});
  card(()->{heading("Try your voice");text(prefs.p.getInt("record_mode",1)==1?"Tap once to speak. Tap again to finish.":"Hold, speak naturally, then release.");recordButton();
   transcript=new EditText(this);transcript.setText(draft.isEmpty()?speech.lastText:draft);transcript.setHint("Your words will appear here…");transcript.setTextSize(16);transcript.setTextColor(Ui.INK);transcript.setHintTextColor(Ui.MUTED);transcript.setPadding(dp(12),dp(12),dp(12),dp(12));transcript.setGravity(Gravity.TOP);transcript.setMinLines(3);transcript.setBackground(Ui.shape(this,Ui.BG,12));content.addView(transcript,new LinearLayout.LayoutParams(-1,dp(120)));Ui.gap(content,6);
   LinearLayout actions=Ui.row(this);Button copy=Ui.smallButton(this,"Copy text",()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Dictation",transcript.getText()));Toast.makeText(this,"Copied",0).show();});Button cancel=Ui.smallButton(this,"Clear",()->{speech.cancel(this);draft="";speech.lastText="";speech.lastRaw="";if(transcript!=null)transcript.setText("");resetHold();});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1);cp.rightMargin=dp(8);actions.addView(copy,cp);actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(actions);});
 }
 private void setupRow(String icon,String title,String sub,boolean done,Runnable action){
  LinearLayout row=Ui.row(this);row.setPadding(dp(14),dp(16),dp(14),dp(16));row.setBackground(Ui.shape(this,Color.WHITE,16));row.addView(new Ui.Icon(this,done?"check":"close",done?Ui.ACCENT:Ui.MUTED),new LinearLayout.LayoutParams(dp(23),dp(23)));
  LinearLayout words=Ui.column(this);words.addView(Ui.text(this,title,14,Ui.INK,true));Ui.gap(words,5);words.addView(Ui.text(this,sub,12,Ui.MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(13);p.rightMargin=dp(8);row.addView(words,p);row.addView(new Ui.Icon(this,"arrow",Ui.MUTED),new LinearLayout.LayoutParams(dp(16),dp(16)));
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,dp(4),0,dp(4));content.addView(row,rp);row.setOnClickListener(v->action.run());row.setClickable(true);row.setFocusable(true);row.setContentDescription(title+(done?", completed":""));
 }
 private void modelPage(){
  heading("Your speech model");text("Designed for English dictation. Runs locally, so your audio stays on your phone.");
  card(()->{heading("Fluid Speech Mini");status=Ui.text(this,speech.state,14,Ui.ACCENT,false);content.addView(status);Ui.gap(content,8);
   LinearLayout downloadRow=Ui.row(this);
   Button download=Ui.button(this,speech.ready()?"Verify model files":"Download model · 300 MB",true,()->speech.download(this,callbacks()));
   LinearLayout.LayoutParams downloadParams=new LinearLayout.LayoutParams(0,dp(52),1);downloadParams.rightMargin=dp(8);downloadRow.addView(download,downloadParams);
   FrameLayout configure=new FrameLayout(this);configure.setBackground(Ui.shape(this,Ui.MINT,12));
   configure.addView(new Ui.Icon(this,"settings",Ui.ACCENT),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
   configure.setContentDescription("Configure model download source");configure.setClickable(true);configure.setFocusable(true);
   configure.setOnClickListener(v->modelSourceDialog());downloadRow.addView(configure,new LinearLayout.LayoutParams(dp(52),dp(52)));
   content.addView(downloadRow);Ui.gap(content,8);
   text("Keep this page open while downloading. Verified files are reused if you retry.");
   LinearLayout row=Ui.row(this);Button load=Ui.button(this,"Load now",false,()->speech.preload()),unload=Ui.button(this,"Unload",false,()->speech.unload());LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.rightMargin=dp(5);LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1);right.leftMargin=dp(5);row.addView(load,left);row.addView(unload,right);content.addView(row);button("Cancel current task",()->speech.cancel(this));});
  card(()->{heading("Ready when you are");toggle("Preload when keyboard opens","preload",false);text("Off: loads when you first speak.");idleChoice();text("Longer waits mean fewer reloads, but use memory while idle.");toggle("Unload when keyboard closes","unload_hide",false);});
  card(()->{heading("Performance");text("Choose how quickly the phone turns speech into text.");performanceChoice();
   recordingChoice();text("Long recordings use temporary storage and are transcribed in 30-second parts. Recording stops if storage is low or the phone gets hot.");});
 }
 private void modelSourceDialog(){
  LinearLayout frame=Ui.column(this);frame.setPadding(dp(20),dp(20),dp(20),dp(16));frame.setBackgroundColor(Color.WHITE);
  LinearLayout header=Ui.row(this);
  TextView title=Ui.text(this,"Model download source",19,Ui.INK,true);
  header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  TextView close=Ui.text(this,"✕",23,Ui.INK,true);close.setGravity(Gravity.CENTER);
  close.setContentDescription("Close model settings");close.setClickable(true);close.setFocusable(true);
  header.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));
  frame.addView(header);Ui.gap(frame,10);
  ScrollView scroll=new ScrollView(this);LinearLayout body=Ui.column(this);scroll.addView(body);
  String base;try{base=new ModelStore(this).sourceBase();}catch(Exception e){base="Check your saved source URL";}
  body.addView(Ui.text(this,prefs.p.getString("model_base_url","").isEmpty()?"HUGGING FACE · DEFAULT":"CUSTOM HTTPS SOURCE",11,Ui.ACCENT,true));Ui.gap(body,8);
  TextView source=Ui.text(this,base,12,Ui.MUTED,false);source.setTextIsSelectable(true);body.addView(source);Ui.gap(body,10);
  Button edit=Ui.button(this,"Change source URL",false,()->{});body.addView(edit);
  Ui.gap(body,8);
  body.addView(Ui.text(this,ModelTokenStore.saved(this)?"Access token saved securely on this phone":"Access token · optional",12,Ui.MUTED,false));Ui.gap(body,6);
  Button token=Ui.button(this,"Manage Hugging Face token",false,()->{});body.addView(token);
  Ui.gap(body,12);
  body.addView(Ui.text(this,"The exact ONNX files must pass their size and SHA-256 checks. A token cannot fix an outage; a compatible HTTPS mirror may help.",12,Ui.MUTED,false));
  Ui.gap(body,12);
  TextView location=Ui.text(this,"PRIVATE MODEL STORAGE\\n/data/user/0/com.jitu.fluidspeech/files/fluid-speech-mini/",12,Ui.MUTED,false);
  location.setTextIsSelectable(true);body.addView(location);Ui.gap(body,10);
  body.addView(Ui.text(this,"Updating keeps downloaded files. Uninstalling deletes them.",12,Ui.MUTED,false));
  frame.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
  AlertDialog dialog=new AlertDialog.Builder(this).setView(frame).create();
  close.setOnClickListener(v->dialog.dismiss());
  edit.setOnClickListener(v->{dialog.dismiss();sourceDialog();});
  token.setOnClickListener(v->{dialog.dismiss();tokenDialog();});
  dialog.show();
 }
 private void sourceDialog(){
  EditText input=new EditText(this);input.setSingleLine(false);input.setTextSize(14);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
  try{input.setText(new ModelStore(this).sourceBase());}catch(Exception ignored){}
  input.setSelectAllOnFocus(true);LinearLayout wrap=Ui.column(this);wrap.setPadding(dp(20),0,dp(20),0);wrap.addView(input);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Download folder URL").setMessage("Paste an HTTPS folder ending in /resolve/revision/ or a mirror with the same files. File names are added automatically.")
   .setView(wrap).setNegativeButton("Cancel",null).setNeutralButton("Use default",(d,w)->{prefs.p.edit().remove("model_base_url").apply();render();}).setPositiveButton("Save",null).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{
   String validated=ModelStore.validateBase(input.getText().toString());prefs.p.edit().putString("model_base_url",validated).apply();dialog.dismiss();render();
  }catch(Exception e){input.setError(e.getMessage());}}));dialog.show();
 }
 private void tokenDialog(){
  EditText input=new EditText(this);input.setHint("hf_…");input.setTextSize(16);
  input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
  LinearLayout wrap=Ui.column(this);wrap.setPadding(dp(20),0,dp(20),0);wrap.addView(input);
  AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle("Hugging Face access token").setMessage("Optional for repositories needing access. Use a read-only token. It is encrypted on this phone and sent only to huggingface.co, never to a mirror.")
   .setView(wrap).setNegativeButton("Cancel",null).setPositiveButton("Save token",null);
  if(ModelTokenStore.saved(this))builder.setNeutralButton("Remove token",(d,w)->{ModelTokenStore.clear(this);render();});
  AlertDialog dialog=builder.create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{ModelTokenStore.save(this,input.getText().toString());dialog.dismiss();render();}catch(Exception e){input.setError(e.getMessage());}}));dialog.show();
 }
 private void performanceChoice(){
  content.addView(Ui.text(this,"TRANSCRIPTION SPEED",11,Ui.MUTED,true));Ui.gap(content,8);
  String[] labels={"Battery saver · 1 core","Everyday · 2 cores","Faster · 4 cores","Extra fast · 6 cores","Ultra fast · 8 cores"};int[] cores={1,2,4,6,8};
  Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));spinner.setPadding(dp(14),0,dp(10),0);spinner.setBackground(Ui.shape(this,Ui.BG,12));
  int selected=1;for(int i=0;i<cores.length;i++)if(prefs.threads()==cores[i])selected=i;spinner.setSelection(selected);content.addView(spinner,new LinearLayout.LayoutParams(-1,dp(52)));Ui.gap(content,10);
  LinearLayout panel=Ui.card(this,true);content.addView(panel);panel.addView(Ui.text(this,"ON YOUR NORD 4",11,Ui.MINT,true));Ui.gap(panel,8);
  TextView speed=metric(panel,"11-second sample"),memory=metric(panel,"App memory"),battery=metric(panel,"Battery in 10 min");
  panel.addView(Ui.text(this,"One run per mode. Memory can vary. More cores may warm the phone; the 8-core run was slightly slower than 6 cores.",12,0xffd5cfca,false));
  updatePerformance(selected,speed,memory,battery);
  spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int at,long id){
   updatePerformance(at,speed,memory,battery);if(prefs.threads()!=cores[at]){prefs.p.edit().putInt("threads",cores[at]).apply();speech.unload();}
  }});Ui.gap(content,12);
 }
 private TextView metric(LinearLayout panel,String title){
  LinearLayout line=Ui.row(this);line.setMinimumHeight(dp(36));line.addView(Ui.text(this,title,13,0xffd5cfca,false),new LinearLayout.LayoutParams(0,-2,1));
  TextView value=Ui.text(this,"",13,Color.WHITE,true);value.setGravity(Gravity.END);line.addView(value,new LinearLayout.LayoutParams(-2,-2));panel.addView(line);return value;
 }
 private void updatePerformance(int i,TextView speed,TextView memory,TextView battery){
  String[] times={"7.8 s","4.2 s","2.4 s","2.2 s","2.3 s"},ram={"~450 MB","~444 MB","~430 MB","~454 MB","~446 MB"};
  speed.setText(times[i]);memory.setText(ram[i]);battery.setText("Not measured");
 }
 private void startTest(){
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);return;}
  if(speech.start(this,callbacks(),true)){hold.setText(prefs.p.getInt("record_mode",1)==1?"Tap to finish":"Release to finish");hold.setBackground(Ui.shape(this,Ui.NAVY,12));}
 }
 private void recordButton(){
  hold=primary(prefs.p.getInt("record_mode",1)==1?"Tap to speak":"Hold to speak",()->{});
  hold.setOnTouchListener((v,e)->{
   boolean tap=prefs.p.getInt("record_mode",1)==1;
   if(e.getActionMasked()==MotionEvent.ACTION_DOWN){if(!tap)startTest();return true;}
   if(e.getActionMasked()==MotionEvent.ACTION_UP){if(tap){if(speech.recording){speech.stop(this);resetHold();}else if(!speech.busy)startTest();}else{speech.stop(this);resetHold();}return true;}
   if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){if(!tap){speech.cancel(this);resetHold();}return true;}return true;
  });
 }
 private void resetHold(){if(hold!=null){hold.setText(prefs.p.getInt("record_mode",1)==1?"Tap to speak":"Hold to speak");hold.setBackground(Ui.shape(this,Ui.ACCENT,12));}}
 private SpeechController.Callback callbacks(){return new SpeechController.Callback(){public void status(String s){if(status!=null)status.setText(s);}public void error(String s){resetHold();Toast.makeText(MainActivity.this,s,Toast.LENGTH_LONG).show();if(status!=null)status.setText(s);}public void result(String raw,String text,long ms){draft=text;resetHold();if(transcript!=null)transcript.setText(text);Toast.makeText(MainActivity.this,"Transcribed in "+String.format(Locale.ROOT,"%.1f",ms/1000.0)+" seconds",0).show();}};}
 private void settingsPage(){
  heading("Fits your everyday typing");text("Keep Gboard, or use the optional basic keyboard.");
  card(()->{heading("Use with Gboard");text("A small voice icon appears with your keyboard. Drag it away from send buttons or anything you need to see. Keep its swipe typing, predictions, emoji, clipboard, languages and other features.");primary(CompanionService.enabled(this)?"Manage voice-button access":"Enable companion voice button",this::openCompanionSettings);toggle("Show the companion voice button","companion",true);text("Requires Accessibility access to detect the keyboard and insert dictated text. Screen content is not saved, and your words are not sent to a server. Some apps may restrict this integration.");});
  bubbleSettings();confirmationSettings();
  card(()->{heading("Staying open");toggle("Keep screen awake while this app is open","keep_awake",true);text("Prevents your phone’s normal screen timeout while viewing Phone Whisper. Turn it off to save battery. Android may still move apps to the background.");});
  card(()->{heading("Microphone");button("Check microphone permission",()->requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1));toggle("Also copy dictation to clipboard","copy",false);});
  card(()->{heading("Optional basic keyboard");text("A lightweight fallback with its own mic, clipboard and emoji buttons. It does not duplicate all Gboard features.");button("Enable Phone Whisper keyboard",()->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));button("Choose keyboard",()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());});
  card(()->{heading("Your settings");button("Export settings & dictionary",this::exportSettings);button("Import a backup",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,12);});text("Includes button controls and corrections. Model files, history and the access token are excluded.");});
  text("Phone Whisper · Personal build 0.4.4\nFluid Speech Mini by FluidVoice, built on NVIDIA Parakeet TDT v3 and Moondream Parakeet Ultra. CC BY 4.0. ONNX Runtime: MIT.");
 }
 private void screenWake(){if(prefs.p.getBoolean("keep_awake",true))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
 private void openCompanionSettings(){
  if(CompanionService.enabled(this)){startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));return;}
  // Google Play requires a separate, prominent, affirmative disclosure for
  // general-purpose apps that request AccessibilityService access.
  new AlertDialog.Builder(this)
   .setTitle("Accessibility access for voice typing")
   .setMessage("To work alongside Gboard, Phone Whisper needs Android Accessibility access. Android gives this permission broad access to screen and input content. Phone Whisper uses it to detect when a keyboard and text field are active, read up to one character before the cursor for spacing, and insert only your dictated words into that field.\n\nIt does not save or upload on-screen content or send voice recordings to a server. You control when recording begins and can turn the service off in Android Settings at any time.\n\nAllow Phone Whisper to guide you to Android's Accessibility settings?")
   .setNegativeButton("Not now",null)
   .setPositiveButton("I agree · Continue",(dialog,which)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
   .show();
 }
 private void wordsPage(){
  heading("Your words");text("Names that sound right. Shortcuts that save time.");
  LinearLayout sections=Ui.row(this);String[] labels={"Corrections","Snippets"};
  for(int i=0;i<2;i++){final int n=i;Button b=Ui.smallButton(this,labels[i],()->{wordSection=n;render();});if(wordSection==i){b.setTextColor(Color.WHITE);b.setBackground(Ui.shape(this,Ui.NAVY,12));}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.setMargins(i==0?0:dp(5),0,i==0?dp(5):0,0);sections.addView(b,p);}content.addView(sections);Ui.gap(content,18);
  String key=wordSection==0?"dictionary":"snippets";int count=RuleStore.read(prefs,key).size();
  LinearLayout title=Ui.row(this);title.addView(Ui.text(this,count+" "+(wordSection==0?(count==1?"correction":"corrections"):(count==1?"snippet":"snippets")),14,Ui.MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
  Button backup=Ui.smallButton(this,"Backup",()->{});backup.setOnClickListener(v->wordsBackupMenu(v));LinearLayout.LayoutParams backupParams=new LinearLayout.LayoutParams(-2,dp(48));backupParams.rightMargin=dp(8);title.addView(backup,backupParams);title.addView(Ui.smallButton(this,"+ Add",()->ruleDialog(key,null)),new LinearLayout.LayoutParams(-2,dp(48)));content.addView(title);Ui.gap(content,8);
  text(wordSection==0?"Fix a word the model often mishears. Corrections run automatically after you speak.":"Say a short trigger to insert a longer response. Use {{DATE}} or {{TIME}} in the response.");
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setHint("Search "+(wordSection==0?"corrections":"snippets"));search.setPadding(dp(14),dp(8),dp(14),dp(8));search.setBackground(Ui.shape(this,Color.WHITE,12));content.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));Ui.gap(content,8);
  LinearLayout list=Ui.column(this);content.addView(list);showRules(key,"",list);
  search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence v,int a,int c,int f){}public void onTextChanged(CharSequence v,int a,int before,int c){showRules(key,v.toString(),list);}public void afterTextChanged(android.text.Editable e){}});
 }
 private void showRules(String key,String query,LinearLayout list){
  list.removeAllViews();LinkedHashMap<String,String> entries=RuleStore.read(prefs,key);int shown=0;
  for(Map.Entry<String,String> e:entries.entrySet()){
   String from=e.getKey(),to=e.getValue();if(!(from+" "+to).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;shown++;
   LinearLayout c=Ui.card(this,false);c.setPadding(dp(16),dp(14),dp(16),dp(12));list.addView(c);
   LinearLayout line=Ui.row(this);LinearLayout names=Ui.column(this);
   names.addView(Ui.text(this,key.equals("dictionary")?"MISHEARD":"TRIGGER",10,Ui.MUTED,true));Ui.gap(names,4);TextView term=Ui.text(this,from,15,Ui.INK,true);term.setSingleLine(true);term.setEllipsize(android.text.TextUtils.TruncateAt.END);names.addView(term);
   LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(0,-2,1);nameParams.rightMargin=dp(10);line.addView(names,nameParams);Button edit=Ui.smallButton(this,"Edit",()->ruleDialog(key,from));LinearLayout.LayoutParams editParams=new LinearLayout.LayoutParams(-2,dp(48));editParams.rightMargin=dp(8);line.addView(edit,editParams);
   Button del=Ui.smallButton(this,"⋮",()->{});del.setContentDescription("More options for "+from);del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete entry?").setMessage(from).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,x)->{LinkedHashMap<String,String> all=RuleStore.read(prefs,key);all.remove(from);RuleStore.save(prefs,key,all);showRules(key,query,list);}).show());line.addView(del,new LinearLayout.LayoutParams(dp(48),dp(48)));c.addView(line);
   Ui.gap(c,10);LinearLayout result=Ui.row(this);result.addView(new Ui.Icon(this,"arrow",Ui.ACCENT),new LinearLayout.LayoutParams(dp(16),dp(16)));
   TextView answer=Ui.text(this,to,14,Ui.MUTED,false);answer.setMaxLines(key.equals("snippets")?3:2);answer.setEllipsize(android.text.TextUtils.TruncateAt.END);
   LinearLayout.LayoutParams answerLayout=new LinearLayout.LayoutParams(0,-2,1);answerLayout.leftMargin=dp(10);result.addView(answer,answerLayout);c.addView(result);
  }
  if(shown==0){LinearLayout c=Ui.card(this,false);c.setGravity(Gravity.CENTER);Ui.Icon icon=new Ui.Icon(this,"words",Ui.ACCENT);c.addView(icon,new LinearLayout.LayoutParams(dp(36),dp(36)));Ui.gap(c,14);c.addView(Ui.text(this,query.isEmpty()?"Make it sound like you":"No matching entries",18,Ui.INK,true));Ui.gap(c,8);TextView example=Ui.text(this,query.isEmpty()?(key.equals("dictionary")?"Example: “elementer” → “Elementor”":"Example: “my sign off” → your full signature"):"Try another word.",14,Ui.MUTED,false);example.setGravity(Gravity.CENTER);c.addView(example);list.addView(c);}
 }
 private void ruleDialog(String key,String old){ruleDialog(key,old,"","");}
 private void ruleDialog(String key,String old,String suggestedFrom,String suggestedTo){
  boolean snippet=key.equals("snippets");LinkedHashMap<String,String> all=RuleStore.read(prefs,key);
  LinearLayout form=Ui.column(this);form.setPadding(dp(22),dp(8),dp(22),dp(8));
  form.addView(Ui.text(this,snippet?"What you say":"What the model hears",12,Ui.MUTED,true));EditText from=new EditText(this);from.setSingleLine(true);from.setText(old==null?suggestedFrom:old);from.setHint(snippet?"my sign off":"elementer");from.setTextSize(16);form.addView(from);Ui.gap(form,18);
  form.addView(Ui.text(this,snippet?"Text to insert":"Correct spelling",12,Ui.MUTED,true));EditText to=new EditText(this);to.setText(old==null?suggestedTo:all.get(old));to.setHint(snippet?"Thanks, Abhishek":"Elementor");to.setTextSize(16);to.setMinLines(snippet?3:1);to.setMaxLines(snippet?8:1);to.setInputType(android.text.InputType.TYPE_CLASS_TEXT|(snippet?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));form.addView(to);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle((old==null?"Add ":"Edit ")+(snippet?"snippet":"correction")).setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
  dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
   String a=from.getText().toString().trim(),value=to.getText().toString().trim();
   if(a.isEmpty()||a.contains("=>")||a.contains("\n")||a.length()>200){from.setError("Enter a short phrase, without =>.");return;}
   if(value.isEmpty()||value.length()>8000){to.setError("Enter text, up to 8,000 characters.");return;}
   LinkedHashMap<String,String> changed=RuleStore.read(prefs,key);
   for(String k:changed.keySet())if(k.equalsIgnoreCase(a)&&!k.equals(old)){from.setError("This phrase already exists.");return;}
   if(old!=null)changed.remove(old);changed.put(a,value);
   int total=0;for(Map.Entry<String,String> entry:changed.entrySet())total+=entry.getKey().length()+entry.getValue().length()+5;if(total>32000){to.setError("Your saved entries exceed the 32,000 character limit.");return;}
   RuleStore.save(prefs,key,changed);dialog.dismiss();render();
  }));dialog.show();
 }
 private void historyPage(){
  heading("Your dictations");text("A little less typing. A little more done.");
  card(()->{toggle("Save history on this phone","history",false);text("Up to 20 dictations. Audio is never saved.");});
  try{
   JSONArray entries=new JSONArray(prefs.p.getString("entries","[]"));
   LinearLayout summary=Ui.row(this);summary.addView(Ui.text(this,entries.length()+" saved",13,Ui.MUTED,true),new LinearLayout.LayoutParams(0,-2,1));
   if(entries.length()>0)summary.addView(Ui.smallButton(this,"Clear all",()->new AlertDialog.Builder(this).setTitle("Clear history?").setMessage("This removes all saved dictations.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{prefs.p.edit().remove("entries").apply();render();}).show()));content.addView(summary);Ui.gap(content,8);
   if(entries.length()==0){card(()->{content.setGravity(Gravity.CENTER);content.addView(new Ui.Icon(this,"history",Ui.ACCENT),new LinearLayout.LayoutParams(dp(38),dp(38)));Ui.gap(content,14);content.addView(Ui.text(this,"Your words will land here",18,Ui.INK,true));Ui.gap(content,9);text("Turn on history to keep future dictations for copying or reviewing.");});}
   for(int i=0;i<entries.length();i++){
    final int index=i;JSONObject entry=entries.getJSONObject(i);String value=entry.getString("text"),raw=entry.optString("raw",value);long time=entry.getLong("time");
    LinearLayout c=Ui.card(this,false),top=Ui.row(this);content.addView(c);
    top.addView(Ui.text(this,DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(time)),12,Ui.MUTED,true),new LinearLayout.LayoutParams(0,-2,1));
    Button more=Ui.smallButton(this,"⋮",()->{});more.setContentDescription("Dictation actions");more.setOnClickListener(v->{PopupMenu menu=new PopupMenu(this,more);menu.getMenu().add("Delete");menu.getMenu().add("Create a correction");menu.setOnMenuItemClickListener(item->{if(item.getTitle().equals("Delete")){try{JSONArray a=new JSONArray(prefs.p.getString("entries","[]"));a.remove(index);prefs.p.edit().putString("entries",a.toString()).apply();render();}catch(Exception ignored){}}else ruleDialog("dictionary",null,raw,value);return true;});menu.show();});top.addView(more);c.addView(top);Ui.gap(c,12);
    TextView body=Ui.text(this,value,14,Ui.INK,false);body.setTextIsSelectable(true);c.addView(body);Ui.gap(c,14);
    int words=value.trim().isEmpty()?0:value.trim().split("\\s+").length;
    LinearLayout foot=Ui.row(this);foot.addView(Ui.text(this,words+" words"+(entry.has("ms")?"  ·  "+String.format(Locale.ROOT,"%.1f s",entry.optLong("ms")/1000.0):""),11,Ui.MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
    Button copy=Ui.smallButton(this,"Copy text",()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Dictation",value));Toast.makeText(this,"Copied",0).show();});copy.setContentDescription("Copy this dictation");foot.addView(copy);c.addView(foot);
    if(!raw.equals(value)){Ui.gap(c,6);TextView original=Ui.text(this,raw,13,Ui.MUTED,false);original.setVisibility(View.GONE);Button reveal=Ui.smallButton(this,"Show original",()->{boolean show=original.getVisibility()!=View.VISIBLE;original.setVisibility(show?View.VISIBLE:View.GONE);});c.addView(reveal);c.addView(original);}
   }
  }catch(JSONException ignored){}
 }
 private void wordsBackupMenu(View anchor){
  PopupMenu menu=new PopupMenu(this,anchor);menu.getMenu().add("Export words");menu.getMenu().add("Import words");
  menu.setOnMenuItemClickListener(item->{if(item.getTitle().equals("Export words"))exportWords();else{
   Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,22);
  }return true;});menu.show();
 }
 private void exportWords(){
  try{JSONObject j=new JSONObject();j.put("format","phone-whisper-words-1");j.put("dictionary",prefs.p.getString("dictionary",""));j.put("snippets",prefs.p.getString("snippets",""));exportPending=j.toString(2);
   Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"PhoneWhisper-Words.json");startActivityForResult(i,21);
  }catch(Exception e){Toast.makeText(this,"Could not export words",1).show();}
 }
 private byte[] readBackup(Intent data)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(data.getData())){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(bytes.size()+n>1024*1024)throw new IOException("Backup is too large.");bytes.write(buf,0,n);}}
  return bytes.toByteArray();
 }
 private void recordingChoice(){
  text("Recording length");
  String[] labels={"15 seconds","30 seconds","1 minute","2 minutes","3 minutes","5 minutes","No app time limit","Custom minutes…"};int[] values={15,30,60,120,180,300,0,-1};
  int saved=prefs.maxSeconds(),selected=7;for(int i=0;i<7;i++)if(saved==values[i])selected=i;
  if(selected==7)labels[7]="Custom · "+Math.max(1,saved/60)+" minutes";
  Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));spinner.setPadding(dp(14),0,dp(10),0);spinner.setBackground(Ui.shape(this,Ui.BG,12));spinner.setSelection(selected);final boolean[] first={true};
  spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int at,long id){if(first[0]){first[0]=false;return;}if(values[at]<0){customRecording();return;}prefs.p.edit().putInt("seconds",values[at]).apply();}});
  content.addView(spinner,new LinearLayout.LayoutParams(-1,dp(52)));Button custom=Ui.smallButton(this,"Set custom minutes…",this::customRecording);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(48));p.topMargin=dp(10);p.bottomMargin=dp(8);content.addView(custom,p);
 }
 private void customRecording(){
  EditText minutes=new EditText(this);minutes.setHint("Minutes");minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);minutes.setText(String.valueOf(Math.max(1,prefs.maxSeconds()/60)));
  LinearLayout wrap=Ui.column(this);wrap.setPadding(dp(22),0,dp(22),0);wrap.addView(minutes);
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Custom recording length").setMessage("Recording stops after this many minutes. Choose No app time limit if you prefer to stop it yourself.").setView(wrap).setNegativeButton("Cancel",(x,y)->render()).setPositiveButton("Save",null).create();
  d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{long n=Long.parseLong(minutes.getText().toString());if(n<1||n>Integer.MAX_VALUE/60L)throw new NumberFormatException();prefs.p.edit().putInt("seconds",(int)(n*60)).apply();d.dismiss();render();}catch(Exception e){minutes.setError("Enter a valid whole number of minutes.");}}));d.show();
 }
 private void idleChoice(){
  text("Unload after inactivity");String[] labels={"15 seconds","30 seconds","1 minute","5 minutes","30 minutes","1 hour","Keep loaded","Custom minutes…"};
  int[] values={15,30,60,300,1800,3600,0,-1};int saved=prefs.idle(),selected=7;for(int i=0;i<7;i++)if(saved==values[i])selected=i;
  if(selected==7)labels[7]="Custom · "+String.format(Locale.ROOT,"%.2f",saved/60.0).replaceAll("0+$","").replaceAll("\\.$","")+" minutes";
  Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));spinner.setPadding(dp(14),0,dp(10),0);spinner.setBackground(Ui.shape(this,Ui.BG,12));spinner.setSelection(selected);final boolean[] first={true};
  spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int at,long id){if(first[0]){first[0]=false;return;}if(values[at]<0){customIdle();return;}prefs.p.edit().putInt("idle",values[at]).apply();speech.scheduleIdle();}});
  content.addView(spinner,new LinearLayout.LayoutParams(-1,dp(52)));Button custom=Ui.smallButton(this,"Set custom minutes…",this::customIdle);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(48));p.topMargin=dp(10);p.bottomMargin=dp(8);content.addView(custom,p);
 }
 private void customIdle(){
  EditText minutes=new EditText(this);minutes.setHint("Minutes");minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);minutes.setText(String.valueOf(Math.max(1,prefs.idle()/60)));
  LinearLayout wrap=Ui.column(this);wrap.setPadding(dp(22),0,dp(22),0);wrap.addView(minutes);
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Custom unload time").setMessage("How many minutes should the model stay ready after you stop using it?").setView(wrap).setNegativeButton("Cancel",(x,y)->render()).setPositiveButton("Save",null).create();
  d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{long n=Long.parseLong(minutes.getText().toString());if(n<1||n>Integer.MAX_VALUE/60L)throw new NumberFormatException();prefs.p.edit().putInt("idle",(int)(n*60)).apply();speech.scheduleIdle();d.dismiss();render();}catch(Exception e){minutes.setError("Enter a whole number from 1 to 35,791,394.");}}));d.show();
 }
 private void bubbleSettings(){
  card(()->{heading("Make the button yours");text("Drag the floating icon anywhere on screen. Its position is remembered.");
   choice("How you record","record_mode",new String[]{"Hold to record · release to finish","Tap to start · tap again to finish"},new int[]{0,1},1);
   text("Drag to move in either mode. X cancels without pasting; ✓ finishes recording and automatically pastes when transcription finishes.");
   LinearLayout preview=Ui.column(this);preview.setGravity(Gravity.CENTER);preview.setPadding(0,dp(16),0,dp(16));preview.setBackground(Ui.shape(this,Ui.BG,14));content.addView(preview);
   bubblePreview=new BubbleView(this,prefs);bubblePreview.preview=true;int size=dp(prefs.p.getInt("bubble_size",48));preview.addView(bubblePreview,new LinearLayout.LayoutParams(size,size));Ui.gap(preview,10);preview.addView(Ui.text(this,"RECORDING PREVIEW · no microphone",10,Ui.MUTED,true));
   slider("Button size","bubble_size",36,80,48," dp");slider("Opacity","bubble_opacity",40,100,100,"%");
   choice("Button icon","bubble_icon",new String[]{"Phone Whisper logo","Microphone","Sound bars","Your image"},new int[]{0,1,2,3},0);
   content.addView(Ui.smallButton(this,"Choose your own image",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,13);}));
   choice("Recording animation","wave_style",new String[]{"Sound bars","Audio ring","Soft pulse","Still · no movement"},new int[]{0,1,2,3},0);
   choice("Animation pace","wave_speed",new String[]{"Relaxed","Natural","Lively"},new int[]{60,100,150},100);slider("Wave response","wave_strength",50,180,100,"%");
   content.addView(Ui.smallButton(this,"Reset position",()->{prefs.p.edit().remove("bubble_x").remove("bubble_y").apply();Toast.makeText(this,"Position reset",0).show();}));
  });
 }
 private void confirmationSettings(){
  card(()->{heading("Confirm or discard");text("X cancels and leaves your text untouched. ✓ finishes recording and pastes automatically when transcription completes. No extra confirmation.");
   controlPreview=Ui.row(this);controlPreview.setGravity(Gravity.CENTER);controlPreview.setPadding(0,dp(12),0,dp(12));controlPreview.setBackground(Ui.shape(this,Ui.BG,14));content.addView(controlPreview);renderControlPreview();Ui.gap(content,12);
   slider("Action button size","action_size",32,56,38," dp");
   choice("Action button look","action_style",new String[]{"Filled · quiet contrast","Outline · light"},new int[]{0,1},0);
   text("Drag the voice button to the red zone above Android navigation to hide it. Restore it from Home without changing accessibility access.");
  });
 }
 private void renderControlPreview(){
  if(controlPreview==null)return;controlPreview.removeAllViews();
  int hit=OverlayControls.hitSize(this,prefs),size=dp(prefs.p.getInt("bubble_size",48));
  View x=OverlayControls.button(this,prefs,false,()->{}),yes=OverlayControls.button(this,prefs,true,()->{});
  x.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);yes.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  controlPreview.addView(x,new LinearLayout.LayoutParams(hit,hit));
  BubbleView logo=new BubbleView(this,prefs);logo.preview=true;LinearLayout.LayoutParams middle=new LinearLayout.LayoutParams(size,size);middle.leftMargin=dp(10);middle.rightMargin=dp(10);controlPreview.addView(logo,middle);
  controlPreview.addView(yes,new LinearLayout.LayoutParams(hit,hit));
 }
 private void slider(String title,String key,int min,int max,int fallback,String unit){
  TextView value=Ui.text(this,title+" · "+prefs.p.getInt(key,fallback)+unit,14,Ui.INK,true);content.addView(value);SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(prefs.p.getInt(key,fallback)-min);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int n,boolean user){if(!user)return;int v=n+min;prefs.p.edit().putInt(key,v).apply();value.setText(title+" · "+v+unit);refreshPreview();}});content.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));Ui.gap(content,8);
 }
 private void refreshPreview(){if(bubblePreview!=null){int size=dp(prefs.p.getInt("bubble_size",48));bubblePreview.setLayoutParams(new LinearLayout.LayoutParams(size,size));bubblePreview.setAlpha(prefs.p.getInt("bubble_opacity",100)/100f);bubblePreview.reload();}renderControlPreview();}

 private void toggle(String title,String key,boolean fallback){
  Switch s=new Switch(this);s.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Ui.ACCENT,0xffb8b5b1}));s.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Ui.MINT,0xffdedbd7}));s.setText(title);s.setTextColor(Ui.INK);s.setTextSize(14);s.setPadding(0,dp(12),0,dp(12));s.setChecked(prefs.p.getBoolean(key,fallback));s.setOnCheckedChangeListener((b,value)->{prefs.p.edit().putBoolean(key,value).apply();speech.scheduleIdle();if(key.equals("keep_awake"))screenWake();});content.addView(s,new LinearLayout.LayoutParams(-1,-2));
 }
 private void choice(String title,String key,String[] labels,int[] values,int fallback){
  text(title);Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));s.setPadding(dp(14),0,dp(10),0);s.setBackground(Ui.shape(this,Ui.BG,12));int chosen=0,current=prefs.p.getInt(key,fallback);for(int i=0;i<values.length;i++)if(values[i]==current)chosen=i;s.setSelection(chosen);s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int at,long id){if(prefs.p.getInt(key,fallback)==values[at])return;prefs.p.edit().putInt(key,values[at]).apply();if(key.equals("threads"))speech.unload();speech.scheduleIdle();refreshPreview();}});content.addView(s,new LinearLayout.LayoutParams(-1,dp(52)));Ui.gap(content,12);
 }
 private void exportSettings(){
  try{
   JSONObject j=new JSONObject();j.put("format","fluid-keys-settings-1");
   for(String k:new String[]{"dictionary","snippets"})j.put(k,prefs.p.getString(k,""));
   for(String k:new String[]{"preload","unload_hide","copy","history","companion","keep_awake","bubble_hidden"})j.put(k,prefs.p.getBoolean(k,k.equals("companion")||k.equals("keep_awake")));
   for(String k:new String[]{"record_mode","bubble_size","bubble_opacity","bubble_icon","wave_style","wave_speed","wave_strength","bubble_x","bubble_y","action_size","action_style"})if(prefs.p.contains(k))j.put(k,prefs.p.getInt(k,0));
   j.put("idle",prefs.idle());j.put("threads",prefs.threads());j.put("seconds",prefs.maxSeconds());j.put("model_base_url",prefs.p.getString("model_base_url",""));exportPending=j.toString(2);
   Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"PhoneWhisper-settings.json");startActivityForResult(i,11);
  }catch(Exception e){Toast.makeText(this,e.getMessage(),1).show();}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
  try{
   if((request==11||request==21)&&exportPending!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(exportPending.getBytes("UTF-8"));}exportPending=null;Toast.makeText(this,request==21?"Words exported":"Settings exported",0).show();}
   if(request==13){
    byte[] image;try(InputStream in=getContentResolver().openInputStream(data.getData());ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>12*1024*1024)throw new IOException("Choose an image smaller than 12 MB.");out.write(buf,0,n);}image=out.toByteArray();}
    android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(image,0,image.length,options);
    if(options.outWidth<1||options.outHeight<1)throw new IOException("This image could not be opened.");options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>512)options.inSampleSize*=2;options.inJustDecodeBounds=false;
    android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(image,0,image.length,options);if(bitmap==null)throw new IOException("Choose a PNG or JPG image.");
    File temp=new File(getFilesDir(),"bubble-logo.tmp"),dest=new File(getFilesDir(),"bubble-logo.png");try(OutputStream out=new FileOutputStream(temp)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();if(!temp.renameTo(dest))throw new IOException("Could not save image.");prefs.p.edit().putInt("bubble_icon",3).apply();render();
   }
   if(request==22){
    JSONObject j=new JSONObject(new String(readBackup(data),"UTF-8"));String format=j.optString("format");
    if(!format.equals("phone-whisper-words-1")&&!format.equals("fluid-keys-settings-1"))throw new IOException("Choose a Phone Whisper words backup.");
    LinkedHashMap<String,String> dictionary=RuleStore.read(prefs,"dictionary"),snippets=RuleStore.read(prefs,"snippets");
    dictionary.putAll(TextRules.parse(j.optString("dictionary","")));snippets.putAll(TextRules.parse(j.optString("snippets","")));
    int size=0;for(Map.Entry<String,String> x:dictionary.entrySet())size+=x.getKey().length()+x.getValue().length()+5;
    if(size>32000)throw new IOException("Corrections exceed the storage limit.");
    size=0;for(Map.Entry<String,String> x:snippets.entrySet())size+=x.getKey().length()+x.getValue().length()+5;
    if(size>32000)throw new IOException("Snippets exceed the storage limit.");
    RuleStore.save(prefs,"dictionary",dictionary);RuleStore.save(prefs,"snippets",snippets);render();Toast.makeText(this,"Words imported",0).show();
   }
   if(request==12){
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(data.getData())){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(bytes.size()+n>1024*1024)throw new IOException("Settings file is too large.");bytes.write(buf,0,n);}}
    JSONObject j=new JSONObject(bytes.toString("UTF-8"));if(!"fluid-keys-settings-1".equals(j.optString("format")))throw new IOException("This is not a Phone Whisper backup.");
    android.content.SharedPreferences.Editor e=prefs.p.edit();
    for(String k:new String[]{"dictionary","snippets"}){String value=j.optString(k,"");if(value.length()>32000)throw new IOException("Dictionary is too large.");e.putString(k,value);}
    for(String k:new String[]{"preload","unload_hide","copy","history","companion","keep_awake"})e.putBoolean(k,j.optBoolean(k,k.equals("companion")||k.equals("keep_awake")));
    if(j.has("bubble_hidden"))e.putBoolean("bubble_hidden",j.getBoolean("bubble_hidden"));
    if(j.has("model_base_url")){String url=j.getString("model_base_url");if(!url.isEmpty())url=ModelStore.validateBase(url);e.putString("model_base_url",url);}
    int idle=j.optInt("idle",60),threads=j.optInt("threads",2),seconds=j.optInt("seconds",30);
    if(idle<0||threads<1||threads>8||seconds<0)throw new IOException("Invalid performance settings.");
    for(String k:new String[]{"record_mode","bubble_size","bubble_opacity","bubble_icon","wave_style","wave_speed","wave_strength","bubble_x","bubble_y","action_size","action_style"})if(j.has(k)){int v=j.getInt(k);int low=0,high=1000;if(k.equals("record_mode")||k.equals("action_style"))high=1;else if(k.equals("action_size")){low=32;high=56;}else if(k.equals("bubble_size")){low=36;high=80;}else if(k.equals("bubble_opacity")){low=40;high=100;}else if(k.equals("bubble_icon")||k.equals("wave_style"))high=3;else if(k.equals("wave_speed")){low=60;high=150;}else if(k.equals("wave_strength")){low=50;high=180;}if(v<low||v>high)throw new IOException("Invalid button settings.");e.putInt(k,v);}
    e.putInt("idle",idle).putInt("threads",threads).putInt("seconds",seconds).apply();speech.unload();render();Toast.makeText(this,"Settings imported",0).show();
   }
  }catch(Exception e){Toast.makeText(this,"Could not finish: "+e.getMessage(),1).show();}
 }

 @Override protected void onResume(){super.onResume();screenWake();render();handler.removeCallbacks(refresh);handler.post(refresh);}
 @Override protected void onPause(){super.onPause();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);handler.removeCallbacks(refresh);speech.cancel(this);}
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);render();}
}
