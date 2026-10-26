package com.jitu.fluidspeech;
import java.io.*;import java.nio.*;import java.util.*;import java.util.concurrent.*;
public final class EngineSmokeTest {
 public static void main(String[] args)throws Exception {
  File model=new File(args[0]),wav=new File(args[1]);
  byte[] bytes=AsrEngine.readBytes(wav);ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
  int start=-1,len=0;for(int p=12;p+8<=bytes.length;){int n=b.getInt(p+4);String id=new String(bytes,p,4,"US-ASCII");if(id.equals("data")){start=p+8;len=n;break;}p+=8+n+(n&1);}
  if(start<0)throw new Exception("No WAV audio");
  float[] audio=new float[len/2];b.position(start);for(int i=0;i<audio.length;i++)audio[i]=b.getShort()/32768f;
  long began=System.nanoTime();
  try(AsrEngine engine=new AsrEngine(model,1)){
   System.out.println("LOAD_SECONDS="+(System.nanoTime()-began)/1e9);began=System.nanoTime();
   String text=engine.transcribe(audio);System.out.println("TRANSCRIPT="+text);System.out.println("INFERENCE_SECONDS="+(System.nanoTime()-began)/1e9);
   String lower=text.toLowerCase(Locale.ROOT);
   if(!lower.contains("fellow americans")||!lower.contains("ask not"))throw new AssertionError("Known speech was not recognized");
   if(!engine.transcribe(new float[0]).isEmpty())throw new AssertionError("Empty input");
   boolean rejected=false;
   try{engine.transcribe(audio,()->false);}catch(CancellationException expected){rejected=true;}
   if(!rejected)throw new AssertionError("Cancelled task was allowed to start");
   final java.util.concurrent.atomic.AtomicBoolean valid=new java.util.concurrent.atomic.AtomicBoolean(true);
   final java.util.concurrent.atomic.AtomicBoolean cancelled=new java.util.concurrent.atomic.AtomicBoolean(false);
   Thread task=new Thread(()->{try{engine.transcribe(audio,valid::get);}catch(Exception expected){cancelled.set(true);}});
   task.start();Thread.sleep(150);valid.set(false);engine.cancel();task.join(5000);
   if(task.isAlive()||!cancelled.get())throw new AssertionError("In-flight cancellation failed");
   String reused=engine.transcribe(audio);
   if(!reused.toLowerCase(Locale.ROOT).contains("ask not"))throw new AssertionError("Engine did not recover after cancellation");
   System.out.println("PASS: stale-task rejection, in-flight cancellation and engine reuse.");
  }
  System.out.println("PASS: complete ONNX encoder, frontend, decoder, text decoding and unload.");
 }
}