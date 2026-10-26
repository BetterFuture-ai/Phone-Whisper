package com.jitu.fluidspeech;
import android.content.Context;
import android.os.StatFs;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Locale;
import java.util.concurrent.CancellationException;

final class ModelStore {
 interface Progress {void update(String message);}
 private final Context app;
 final File directory;
 private final JSONObject manifest;
 volatile boolean cancelled;
 ModelStore(Context c)throws Exception {
  app=c.getApplicationContext();directory=new File(app.getFilesDir(),"fluid-speech-mini");
  try(InputStream in=app.getAssets().open("model-manifest.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
   manifest=new JSONObject(out.toString("UTF-8"));
  }
 }
 String defaultBase()throws Exception{return "https://huggingface.co/"+manifest.getString("repository")+"/resolve/"+manifest.getString("revision")+"/";}
 String sourceBase()throws Exception{
  String saved=app.getSharedPreferences("fluid-keys",0).getString("model_base_url","").trim();
  return saved.isEmpty()?defaultBase():validateBase(saved);
 }
 static String validateBase(String input)throws Exception{
  URI uri=new URI(input.trim());
  if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null)
   throw new IllegalArgumentException("Use an HTTPS model folder URL without a password, query or fragment.");
  String path=uri.getRawPath();if(path==null||path.contains("..")||path.contains("%2f")||path.contains("%2F"))throw new IllegalArgumentException("Invalid model folder path.");
  String host=uri.getHost().toLowerCase(Locale.ROOT),base=uri.toASCIIString();
  if(host.equals("huggingface.co")&&path.matches("/[^/]+/[^/]+/?"))throw new IllegalArgumentException("Paste a Hugging Face /resolve/<revision>/ folder URL, not the model page.");
  return base.endsWith("/")?base:base+"/";
 }
 private HttpURLConnection connect(URL start,String token)throws Exception{
  URL current=start;
  for(int redirects=0;redirects<=5;redirects++){
   if(!"https".equalsIgnoreCase(current.getProtocol()))throw new IOException("Model source must use HTTPS.");
   HttpURLConnection con=(HttpURLConnection)current.openConnection();con.setInstanceFollowRedirects(false);
   con.setConnectTimeout(20000);con.setReadTimeout(15000);con.setRequestProperty("User-Agent","PhoneWhisper/0.4.4 Android");
   if(current.getHost().equalsIgnoreCase("huggingface.co")&&!token.isEmpty())con.setRequestProperty("Authorization","Bearer "+token);
   int code=con.getResponseCode();
   if(code==200)return con;
   if(code==301||code==302||code==303||code==307||code==308){String location=con.getHeaderField("Location");con.disconnect();if(location==null)throw new IOException("Model server redirect is missing its destination.");current=new URL(current,location);continue;}
   con.disconnect();if(code==401||code==403)throw new IOException("Model access denied. Check your Hugging Face token and permissions.");
   throw new IOException("Model server returned "+code+". Check the source URL or retry later.");
  }
  throw new IOException("Model server redirected too many times.");
 }
 boolean ready() {
  if(!new File(directory,"installed.ok").exists())return false;
  try {
   JSONArray files=manifest.getJSONArray("files");
   for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);File p=new File(directory,f.getString("name"));if(!p.isFile()||p.length()!=f.getLong("size"))return false;}
   return true;
  }catch(Exception e){return false;}
 }
 void download(Progress progress)throws Exception {
  cancelled=false;directory.mkdirs();
  JSONArray files=manifest.getJSONArray("files");long total=0,done=0,missing=0;
  for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);total+=f.getLong("size");if(new File(directory,f.getString("name")).length()!=f.getLong("size"))missing+=f.getLong("size");}
  if(new StatFs(app.getFilesDir().getPath()).getAvailableBytes()<missing+150*1024*1024L)throw new IOException("Not enough storage. Free about 500 MB, then retry.");
  String base=sourceBase();String token=null;
  for(int i=0;i<files.length();i++){
   if(cancelled)throw new CancellationException();
   JSONObject f=files.getJSONObject(i);String name=f.getString("name");long size=f.getLong("size");
   File dest=new File(directory,name);
   if(dest.isFile()&&dest.length()==size&&sha(dest).equals(f.getString("sha256"))){done+=size;continue;}
   File part=new File(directory,name+".part");
   if(token==null)token=ModelTokenStore.saved(app)?ModelTokenStore.read(app):"";
   URL url=new URL(base+name);
   HttpURLConnection con=connect(url,token);
   try {
    try(InputStream in=con.getInputStream();OutputStream out=new FileOutputStream(part)){
     byte[] b=new byte[65536];int n;long received=0,last=0;
     while((n=in.read(b))!=-1){
      if(cancelled)throw new CancellationException();
      if(received+n>size)throw new IOException("Unexpected model file size");
      out.write(b,0,n);received+=n;
      long now=System.currentTimeMillis();if(now-last>250){last=now;progress.update("Downloading model · "+((done+received)*100/total)+"%");}
     }
    }
    if(part.length()!=size||!sha(part).equals(f.getString("sha256")))throw new IOException("Model verification failed: "+name+". Retry download.");
    if(dest.exists()&&!dest.delete())throw new IOException("Cannot replace "+name);
    if(!part.renameTo(dest))throw new IOException("Cannot install "+name);
    done+=size;
   }finally{con.disconnect();}
  }
  if(cancelled)throw new CancellationException();
  try(FileOutputStream out=new FileOutputStream(new File(directory,"installed.ok"))){out.write(manifest.getString("revision").getBytes("UTF-8"));}
  progress.update("Model downloaded and verified");
 }
 private static String sha(File f)throws Exception {
  MessageDigest h=MessageDigest.getInstance("SHA-256");
  try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)h.update(b,0,n);}
  StringBuilder s=new StringBuilder();for(byte b:h.digest())s.append(String.format("%02x",b&255));return s.toString();
 }
}