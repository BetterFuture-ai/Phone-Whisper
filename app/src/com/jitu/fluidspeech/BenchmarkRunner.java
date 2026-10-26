package com.jitu.fluidspeech;
import android.app.*;import android.os.*;import java.io.*;import java.nio.*;import org.json.*;
/** ADB-only repeatable test with a supplied public-domain sample, never microphone audio. */
public final class BenchmarkRunner extends Instrumentation {
 private Bundle args;
 @Override public void onCreate(Bundle b){super.onCreate(b);args=b;start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   int threads=Integer.parseInt(args.getString("threads","1")),priority=Integer.parseInt(args.getString("priority","10"));
   android.os.Process.setThreadPriority(priority);
   PowerManager power=(PowerManager)getTargetContext().getSystemService("power");
   if(power.getCurrentThermalStatus()>=PowerManager.THERMAL_STATUS_SEVERE)throw new Exception("Phone too hot for speed test");
   File wav=new File(getTargetContext().getExternalFilesDir(null),"benchmark.wav");byte[] bytes=AsrEngine.readBytes(wav);ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
   int start=-1,len=0;for(int p=12;p+8<=bytes.length;){int n=b.getInt(p+4);if(new String(bytes,p,4,"US-ASCII").equals("data")){start=p+8;len=n;break;}p+=8+n+(n&1);}
   if(start<0)throw new Exception("WAV missing");float[] audio=new float[len/2];b.position(start);for(int i=0;i<audio.length;i++)audio[i]=b.getShort()/32768f;
   long begin=SystemClock.elapsedRealtime();
   try(AsrEngine e=new AsrEngine(new ModelStore(getTargetContext()).directory,threads)){
    long load=SystemClock.elapsedRealtime()-begin;begin=SystemClock.elapsedRealtime();final long deadline=begin+60000;
    String text=e.transcribe(audio,()->SystemClock.elapsedRealtime()<deadline);long ms=SystemClock.elapsedRealtime()-begin;
    JSONObject j=new JSONObject();j.put("threads",threads);j.put("priority",priority);j.put("load_ms",load);j.put("inference_ms",ms);j.put("features_ms",e.featureMs);j.put("frontend_ms",e.frontendMs);j.put("encoder_ms",e.encoderMs);j.put("decode_ms",e.decodeMs);j.put("text",text);j.put("audio_seconds",audio.length/16000.0);j.put("thermal",power.getCurrentThermalStatus());j.put("pss_kb",Debug.getPss());
    result.putString("metrics",j.toString());
    if(!text.toLowerCase(java.util.Locale.ROOT).contains("ask not"))throw new Exception("Transcript check failed: "+text);
   }
   finish(Activity.RESULT_OK,result);
  }catch(Throwable e){result.putString("error",e.toString());finish(Activity.RESULT_CANCELED,result);}
 }
}
