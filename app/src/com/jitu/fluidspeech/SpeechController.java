package com.jitu.fluidspeech;
import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** All model work is serialized off the UI thread. No persistent microphone service. */
final class SpeechController {
 interface Callback {
  void status(String message);
  void result(String raw,String text,long milliseconds);
  void error(String message);
 }
 private static SpeechController INSTANCE;
 static synchronized SpeechController get(Context c){
  if(INSTANCE==null)INSTANCE=new SpeechController(c.getApplicationContext());return INSTANCE;
 }
 final Context app;final AppPrefs prefs;
 final Handler main=new Handler(Looper.getMainLooper());
 final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);r.run();},"fluid-inference");t.setPriority(Thread.NORM_PRIORITY);return t;});
 volatile AsrEngine engine;
 volatile String state="Model unloaded";
 volatile boolean busy,recording,loading;
 volatile String lastRaw="",lastText="";
 volatile long lastMillis;
 volatile float rmsLevel;
 private volatile AudioRecord recorder;
 private volatile boolean captureCancelled;
 private final AtomicInteger generation=new AtomicInteger();
 private Object owner;
 private ModelStore downloading;
 private volatile long lastUse=SystemClock.elapsedRealtime();
 private final Runnable idleCheck;
 private SpeechController(Context c){app=c;prefs=new AppPrefs(c);
  File[] orphans=c.getCacheDir().listFiles((dir,name)->name.startsWith("phone-whisper-")&&name.endsWith(".pcm"));
  if(orphans!=null)for(File f:orphans)if(System.currentTimeMillis()-f.lastModified()>3600000L)f.delete();
  idleCheck=()->{
  int idle=prefs.idle();
  if(!busy&&engine!=null&&idle>0&&SystemClock.elapsedRealtime()-lastUse>=idle*1000L)unload();
  else scheduleIdle();
 };}
 boolean ready(){try{return new ModelStore(app).ready();}catch(Exception e){return false;}}
 private boolean hot(){
  if(Build.VERSION.SDK_INT<29)return false;
  PowerManager p=(PowerManager)app.getSystemService(Context.POWER_SERVICE);
  return p!=null&&p.getCurrentThermalStatus()>=PowerManager.THERMAL_STATUS_SEVERE;
 }
 synchronized boolean start(Object who,Callback cb,boolean historyAllowed){
  if(busy){cb.error("Finish or cancel the current task first.");return false;}
  if(app.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){cb.error("Open Phone Whisper once and allow microphone access.");return false;}
  if(!ready()){cb.error("Open Phone Whisper and download the model first.");return false;}
  if(hot()){cb.error("Your phone is hot. Let it cool before dictating.");return false;}
  main.removeCallbacks(idleCheck);owner=who;busy=true;recording=true;captureCancelled=false;
  final int id=generation.incrementAndGet();final int max=prefs.maxSeconds();
  state=engine==null?"Listening · loading model":"Listening";
  cb.status(state);
  worker.execute(()->{
   if(generation.get()!=id)return;
   try{ensureLoaded();if(generation.get()==id&&!busy)state="Ready · model loaded";}catch(Throwable e){fail(id,cb,e);}
  });
  Thread capture=new Thread(()->capture(id,cb,max,historyAllowed),"fluid-microphone");
  capture.start();return true;
 }
 private void capture(int id,Callback cb,int maxSeconds,boolean historyAllowed){
  if(maxSeconds==0||maxSeconds>60){captureLong(id,cb,maxSeconds,historyAllowed);return;}
  short[] pcm=new short[16000*maxSeconds];int count=0;
  AudioRecord rec=null;Throwable error=null;
  try{
   int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
   if(min<0)throw new IllegalStateException("16 kHz recording is unavailable.");
   rec=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,8192));
   if(rec.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Microphone could not start.");
   recorder=rec;
   if(generation.get()!=id)return;
   if(recording)rec.startRecording();long last=0;
   while(recording&&generation.get()==id&&count<pcm.length){
    int n=rec.read(pcm,count,Math.min(2048,pcm.length-count),AudioRecord.READ_BLOCKING);
    if(n<0){if(recording)throw new IllegalStateException("Microphone read error "+n);break;}
    double energy=0;for(int i=count;i<count+n;i++){float sample=pcm[i]/32768f;energy+=sample*sample;}rmsLevel=(float)Math.min(1,Math.sqrt(energy/Math.max(1,n))*12);count+=n;
    long now=SystemClock.elapsedRealtime();
    if(now-last>500){last=now;final String s="Listening · "+count/16000+"s / "+maxSeconds+"s";state=s;post(id,()->cb.status(s));}
    if(hot()){throw new IllegalStateException("Recording stopped because your phone is hot.");}
   }
  }catch(Throwable e){error=e;}finally{
   if(rec!=null){try{rec.stop();}catch(Exception ignored){}rec.release();}
   if(recorder==rec)recorder=null;
  }
  if(generation.get()!=id||captureCancelled)return;
  recording=false;rmsLevel=0;
  if(error!=null){fail(id,cb,error);return;}
  if(count<3200){finish(id);post(id,()->cb.error("Hold the mic a little longer, then release."));return;}
  final float[] audio=new float[count];double sum=0;
  for(int i=0;i<count;i++){audio[i]=pcm[i]/32768f;sum+=audio[i]*audio[i];}
  if(Math.sqrt(sum/count)<0.0003){finish(id);post(id,()->cb.error("No audible speech. Check your microphone."));return;}
  state="Transcribing";post(id,()->cb.status(state));
  worker.execute(()->{
   if(generation.get()!=id)return;
   final long began=SystemClock.elapsedRealtime();
   final Runnable timeout=()->{if(generation.get()==id&&busy&&!recording){cancel(whoFor(id));cb.error("Transcription stopped after 60 seconds. Try a shorter recording.");}};
   main.postDelayed(timeout,60000);
   try{
    ensureLoaded();if(generation.get()!=id)return;
    String raw=engine.transcribe(audio,()->generation.get()==id);
    if(generation.get()!=id)return;
    String text=prefs.correct(raw).trim();
    lastMillis=SystemClock.elapsedRealtime()-began;
    if(historyAllowed){lastRaw=raw;lastText=text;prefs.remember(raw,text);}
    finish(id);post(id,()->cb.result(raw,text,lastMillis));
   }catch(Throwable e){if(generation.get()==id)fail(id,cb,e);}
   finally{main.removeCallbacks(timeout);}
  });
 }
 private void captureLong(int id,Callback cb,int maxSeconds,boolean historyAllowed){
  File file=null;AudioRecord rec=null;long count=0;Throwable error=null;
  try{
   file=File.createTempFile("phone-whisper-", ".pcm", app.getCacheDir());
   int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
   if(min<0)throw new IllegalStateException("16 kHz recording is unavailable.");
   rec=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,8192));
   if(rec.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Microphone could not start.");
   recorder=rec;if(generation.get()!=id){file.delete();return;}
   if(recording)rec.startRecording();
   short[] block=new short[2048];long last=0,spaceCheck=0;
   try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))){
    while(recording&&generation.get()==id&&(maxSeconds==0||count<(long)maxSeconds*16000)){
     int n=rec.read(block,0,Math.min(block.length,(int)Math.min(Integer.MAX_VALUE,maxSeconds==0?block.length:(long)maxSeconds*16000-count)),AudioRecord.READ_BLOCKING);
     if(n<0){if(recording)throw new IOException("Microphone read error "+n);break;}
     double energy=0;for(int i=0;i<n;i++){out.writeShort(block[i]);float sample=block[i]/32768f;energy+=sample*sample;}
     rmsLevel=(float)Math.min(1,Math.sqrt(energy/Math.max(1,n))*12);count+=n;
     long now=SystemClock.elapsedRealtime();
     if(now-last>700){last=now;final String stateNow="Listening · "+count/16000+"s"+(maxSeconds==0?"":" / "+maxSeconds+"s");state=stateNow;post(id,()->cb.status(stateNow));}
     if(now-spaceCheck>10000){spaceCheck=now;if(new android.os.StatFs(app.getCacheDir().getPath()).getAvailableBytes()<100L*1024*1024)throw new IOException("Recording stopped: phone storage is low.");}
     if(hot())throw new IllegalStateException("Recording stopped because your phone is hot.");
    }
   }
  }catch(Throwable e){error=e;}finally{
   if(rec!=null){try{rec.stop();}catch(Exception ignored){}rec.release();}
   if(recorder==rec)recorder=null;
  }
  rmsLevel=0;
  if(generation.get()!=id||captureCancelled){if(file!=null)file.delete();return;}
  recording=false;
  if(error!=null){if(file!=null)file.delete();fail(id,cb,error);return;}
  if(count<3200){if(file!=null)file.delete();finish(id);post(id,()->cb.error("Speak a little longer, then finish."));return;}
  final File source=file;final long samples=count;state="Transcribing";post(id,()->cb.status(state));
  worker.execute(()->{
   final long began=SystemClock.elapsedRealtime();
   try{
    if(generation.get()!=id)return;ensureLoaded();
    StringBuilder raw=new StringBuilder();
    try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(source)))){
     long left=samples;int part=0,total=(int)((samples+479999)/480000);
     while(left>0){
      if(generation.get()!=id)throw new java.util.concurrent.CancellationException("Cancelled");
      int length=(int)Math.min(left,480000);float[] audio=new float[length];double sum=0;
      for(int i=0;i<length;i++){audio[i]=in.readShort()/32768f;sum+=audio[i]*audio[i];}
      left-=length;part++;final String progress="Transcribing part "+part+" of "+total;state=progress;post(id,()->cb.status(progress));
      if(Math.sqrt(sum/length)<0.0003)continue;
      final Runnable timeout=()->{if(generation.get()==id&&busy&&!recording){cancel(whoFor(id));cb.error("This part took over 60 seconds. Try a shorter recording or a lower speed setting.");}};
      main.postDelayed(timeout,60000);
      String chunk;
      try{chunk=engine.transcribe(audio,()->generation.get()==id).trim();}
      finally{main.removeCallbacks(timeout);}
      if(!chunk.isEmpty()){if(raw.length()>0)raw.append(' ');raw.append(chunk);}
     }
    }
    if(generation.get()!=id)return;
    String original=raw.toString(),text=prefs.correct(original).trim();lastMillis=SystemClock.elapsedRealtime()-began;
    if(historyAllowed){lastRaw=original;lastText=text;prefs.remember(original,text);}
    finish(id);post(id,()->cb.result(original,text,lastMillis));
   }catch(Throwable e){if(generation.get()==id)fail(id,cb,e);}
   finally{source.delete();}
  });
 }
 private synchronized Object whoFor(int id){return generation.get()==id?owner:null;}
 synchronized void stop(Object who){
  if(owner!=who||!recording)return;
  recording=false;rmsLevel=0;state="Finishing recording";
  AudioRecord r=recorder;if(r!=null)try{r.stop();}catch(Exception ignored){}
 }
 synchronized void cancel(Object who){
  if(who==null||owner!=who)return;
  captureCancelled=true;generation.incrementAndGet();recording=false;
  AudioRecord r=recorder;if(r!=null)try{r.stop();}catch(Exception ignored){}
  AsrEngine e=engine;if(e!=null)e.cancel();
  if(downloading!=null)downloading.cancelled=true;
  busy=false;owner=null;state=engine==null?"Model unloaded":"Cancelled · model loaded";lastUse=SystemClock.elapsedRealtime();scheduleIdle();
 }
 private void ensureLoaded()throws Exception{
  if(engine==null){
   android.app.ActivityManager manager=(android.app.ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
   android.app.ActivityManager.MemoryInfo memory=new android.app.ActivityManager.MemoryInfo();manager.getMemoryInfo(memory);
   if(memory.lowMemory||memory.availMem<700L*1024*1024)throw new IllegalStateException("Not enough free memory. Close a heavy app and try again.");
   state="Loading Fluid Speech Mini";engine=new AsrEngine(new ModelStore(app).directory,prefs.threads());
  }
 }
 private void post(int id,Runnable action){main.post(()->{if(generation.get()==id)action.run();});}
 private void fail(int id,Callback cb,Throwable e){
  if(generation.get()!=id)return;
  captureCancelled=true;recording=false;
  AudioRecord r=recorder;if(r!=null)try{r.stop();}catch(Exception ignored){}
  finish(id);String message=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
  state="Stopped: "+message;post(id,()->cb.error(message));
 }
 private synchronized void finish(int id){
  if(generation.get()!=id)return;busy=false;recording=false;owner=null;lastUse=SystemClock.elapsedRealtime();state=engine==null?"Model unloaded":"Ready · model loaded";scheduleIdle();
 }
 void preload(){
  synchronized(this){if(busy||loading||engine!=null||!ready()||hot())return;loading=true;state="Loading Fluid Speech Mini";}
  worker.execute(()->{try{ensureLoaded();if(!busy)state="Ready · model loaded";}catch(Throwable e){if(!busy)state="Model could not load: "+e.getMessage();}finally{loading=false;lastUse=SystemClock.elapsedRealtime();scheduleIdle();}});
 }
 void unload(){
  synchronized(this){if(busy){state="Finish or cancel dictation before unloading.";return;}state="Unloading model";}
  worker.execute(()->{
   synchronized(this){if(busy)return;AsrEngine e=engine;engine=null;if(e!=null)e.close();state="Model unloaded";}
  });
 }
 void scheduleIdle(){main.removeCallbacks(idleCheck);int sec=prefs.idle();if(sec>0)main.postDelayed(idleCheck,Math.max(1000,sec*1000L));}
 void download(Object who,Callback cb){
  synchronized(this){if(busy){cb.error("Finish the current task first.");return;}busy=true;owner=who;}
  final int id=generation.incrementAndGet();state="Preparing model download";
  worker.execute(()->{
   try{
    downloading=new ModelStore(app);
    downloading.download(s->{state=s;post(id,()->cb.status(s));});
    finish(id);state="Model ready · tap Load or hold the keyboard mic";post(id,()->cb.status(state));
   }catch(Throwable e){fail(id,cb,e);}finally{downloading=null;}
  });
 }
}
