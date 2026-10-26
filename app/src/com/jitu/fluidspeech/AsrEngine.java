package com.jitu.fluidspeech;

import ai.onnxruntime.*;
import org.json.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Original Android/Java port of FluidVoice's published ONNX inference contract.
 * Model and reference algorithm: FluidVoice, CC BY 4.0. No network access here.
 */
public final class AsrEngine implements AutoCloseable {
    private static final OrtEnvironment ENV = OrtEnvironment.getEnvironment();
    private OrtSession encoder, frontend, decoder, joint;
    private final JSONObject cfg;
    private final float[][] filters;
    private final Map<Integer,String> pieces = new HashMap<>();
    private final Set<Integer> skipped = new HashSet<>();
    private final int[] durations;
    private final String space;
    private volatile OrtSession.RunOptions currentRun;
    private volatile boolean terminated;
    public long featureMs,frontendMs,encoderMs,decodeMs;
    private volatile java.util.function.BooleanSupplier stillValid=()->true;

    public AsrEngine(File folder, int threads) throws Exception {
        cfg = new JSONObject(read(new File(folder,"config.json")));
        filters = readFilters(new File(folder,"mel_filters.npy"));
        JSONObject tok = new JSONObject(read(new File(folder,"tokenizer.json")));
        JSONObject vocab = tok.getJSONObject("model").getJSONObject("vocab");
        Iterator<String> keys = vocab.keys();
        while(keys.hasNext()) { String s=keys.next(); pieces.put(vocab.getInt(s),s); }
        skipped.add(cfg.getInt("blank_token_id")); skipped.add(cfg.getInt("pad_token_id"));
        JSONArray added=tok.optJSONArray("added_tokens");
        if(added!=null) for(int i=0;i<added.length();i++) {
            JSONObject a=added.getJSONObject(i); pieces.put(a.getInt("id"),a.getString("content"));
            if(a.optBoolean("special")) skipped.add(a.getInt("id"));
        }
        JSONObject dec=tok.optJSONObject("decoder");
        space=dec==null?"▁":dec.optString("replacement","▁");
        JSONArray ds=cfg.getJSONArray("durations");durations=new int[ds.length()];
        for(int i=0;i<ds.length();i++)durations[i]=ds.getInt(i);
        try(OrtSession.SessionOptions big=options(threads); OrtSession.SessionOptions small=options(1)) {
            try {
                encoder=ENV.createSession(new File(folder,"encoder.onnx").getPath(),big);
                frontend=ENV.createSession(new File(folder,"frontend.onnx").getPath(),big);
                decoder=ENV.createSession(new File(folder,"decoder.onnx").getPath(),small);
                joint=ENV.createSession(new File(folder,"joint.onnx").getPath(),small);
            } catch(Exception e) { close();throw e; }
        }
    }
    private static OrtSession.SessionOptions options(int threads) throws OrtException {
        OrtSession.SessionOptions o=new OrtSession.SessionOptions();
        o.setIntraOpNumThreads(Math.max(1,Math.min(threads,8)));
        o.setInterOpNumThreads(1);
        o.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);
        o.setCPUArenaAllocator(false);
        o.setMemoryPatternOptimization(false);
        o.addConfigEntry("session.intra_op.allow_spinning","0");
        o.addConfigEntry("session.inter_op.allow_spinning","0");
        return o;
    }
    public void cancel() {
        terminated=true;
        OrtSession.RunOptions r=currentRun;
        if(r!=null) try { r.setTerminate(true); } catch(OrtException ignored){}
    }
    private void check() {
        if(terminated || !stillValid.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new CancellationException("Cancelled");
    }
    public String transcribe(float[] audio) throws Exception { return transcribe(audio,()->true); }
    public String transcribe(float[] audio,java.util.function.BooleanSupplier permissionToRun) throws Exception {
        stillValid=permissionToRun;
        terminated=false;
        if(audio.length<320)return "";
        try(OrtSession.RunOptions run=new OrtSession.RunOptions()) {
            currentRun=run;
            check();
            long phase=System.nanoTime();
            float[][] feats=features(audio,filters);featureMs=(System.nanoTime()-phase)/1000000;phase=System.nanoTime();
            check();
            int total=subsampled(feats.length), step=cfg.getInt("frontend_chunk_frames"), factor=8;
            float[][] hidden=new float[total][];
            for(int a=0;a<total;a+=step) {
                check();
                int b=Math.min(a+step,total);
                int s=a==0?0:factor*(a-1),e=Math.min(feats.length,factor*(b+1));
                float[][] part=Arrays.copyOfRange(feats,s,e);
                long[][] mask=new long[1][e-s];
                for(int i=s;i<e;i++)mask[0][i-s]=i<audio.length/160?1:0;
                Object[] result=run(frontend,map("input_features",new float[][][]{part},"attention_mask",mask),run);
                float[][] y=((float[][][])result[0])[0];
                int skip=a==0?0:1;
                if(y.length<skip+b-a)throw new IOException("Unexpected frontend output shape");
                for(int i=a;i<b;i++)hidden[i]=y[skip+i-a];
            }
            feats=null;frontendMs=(System.nanoTime()-phase)/1000000;phase=System.nanoTime();
            int valid=subsampled(audio.length/160);
            float[][] encoded=((float[][][])run(encoder,map("hidden_states",new float[][][]{hidden},"length",new long[]{valid}),run)[0])[0];
            hidden=null;encoderMs=(System.nanoTime()-phase)/1000000;phase=System.nanoTime();
            check();
            int layers=cfg.getInt("decoder_layers"),size=cfg.getInt("decoder_hidden_size");
            float[][][] zero=new float[layers][1][size];
            int blank=cfg.getInt("blank_token_id"),vocab=cfg.getInt("vocab_size");
            int eos=cfg.optInt("eos_token_id",-1);
            Object[] state=run(decoder,map("token",new long[][]{{blank}},"h_in",zero,"c_in",zero),run);
            List<Integer> ids=new ArrayList<>();
            int t=0,budget=cfg.getInt("max_symbols_per_step")*encoded.length-1;
            while(true) {
                check();
                float[] row=((float[][])run(joint,map("encoder_frame",new float[][]{encoded[Math.min(t,encoded.length-1)]},"decoder_out",state[0]),run)[0])[0];
                int token=argmax(row,0,vocab),duration=durations[argmax(row,vocab,row.length)-vocab];
                if(token==blank&&duration==0)duration=1;
                ids.add(token);t+=duration;
                if(t>=valid||ids.size()>=budget||token==eos)break;
                if(token!=blank)state=run(decoder,map("token",new long[][]{{token}},"h_in",state[1],"c_in",state[2]),run);
            }
            StringBuilder out=new StringBuilder();
            for(int id:ids)if(!skipped.contains(id))out.append(pieces.containsKey(id)?pieces.get(id):"");
            decodeMs=(System.nanoTime()-phase)/1000000;
            String text=out.toString().replace(space," ");
            return text.startsWith(" ")?text.substring(1):text;
        } finally {currentRun=null;stillValid=()->true;}
    }
    private int subsampled(int n)throws JSONException {
        int kernel=cfg.getInt("subsampling_kernel"),stride=cfg.getInt("subsampling_stride");
        int pad=((kernel-1)/2)*2-kernel;
        for(int i=0;i<cfg.getInt("subsampling_layers");i++)n=(int)Math.floor((n+pad)/(double)stride+1);
        return n;
    }
    private static int argmax(float[] x,int a,int b) {
        int winner=a;for(int i=a+1;i<b;i++)if(x[i]>x[winner])winner=i;return winner;
    }
    private static Map<String,Object> map(Object... a) {
        Map<String,Object> m=new LinkedHashMap<>();
        for(int i=0;i<a.length;i+=2)m.put((String)a[i],a[i+1]);return m;
    }
    private static Object[] run(OrtSession session,Map<String,Object> values,OrtSession.RunOptions options)throws OrtException {
        Map<String,OnnxTensor> tensors=new LinkedHashMap<>();
        try {
            for(Map.Entry<String,Object> e:values.entrySet())tensors.put(e.getKey(),OnnxTensor.createTensor(ENV,e.getValue()));
            try(OrtSession.Result result=session.run(tensors,options)) {
                Object[] out=new Object[result.size()];
                for(int i=0;i<out.length;i++)out[i]=result.get(i).getValue();
                return out;
            }
        } finally {for(OnnxTensor t:tensors.values())t.close();}
    }
    /** 16 kHz, 512-point FFT, 400-sample symmetric Hann, per-bin normalization. */
    public static float[][] features(float[] audio,float[][] filters) {
        int frames=1+audio.length/160,valid=audio.length/160;
        if(valid<1)throw new IllegalArgumentException("Audio too short");
        float[] pre=new float[audio.length];pre[0]=audio[0];
        for(int i=1;i<audio.length;i++)pre[i]=audio[i]-0.97f*audio[i-1];
        double[] window=new double[512];
        for(int i=0;i<400;i++)window[56+i]=(float)(0.5-0.5*Math.cos(2*Math.PI*i/399));
        float[][] mel=new float[frames][128];
        for(int t=0;t<frames;t++) {
            double[] real=new double[512],imag=new double[512];
            for(int j=0;j<512;j++) {int s=t*160+j-256;real[j]=(s>=0&&s<pre.length?pre[s]:0)*window[j];}
            fft(real,imag);
            float[] power=new float[257];
            for(int j=0;j<257;j++)power[j]=(float)(real[j]*real[j]+imag[j]*imag[j]);
            for(int m=0;m<128;m++) {
                float sum=0;
                for(int j=0;j<257;j++)sum+=power[j]*filters[m][j];
                mel[t][m]=(float)Math.log(sum+(float)Math.pow(2,-24));
            }
        }
        for(int m=0;m<128;m++) {
            float mean=0,var=0;
            for(int t=0;t<valid;t++)mean+=mel[t][m];mean/=valid;
            for(int t=0;t<valid;t++){float d=mel[t][m]-mean;var+=d*d;}
            var/=Math.max(valid-1,1);float denominator=(float)Math.sqrt(var)+1e-5f;
            for(int t=0;t<frames;t++)mel[t][m]=t<valid?(mel[t][m]-mean)/denominator:0;
        }
        return mel;
    }
    private static void fft(double[] re,double[] im) {
        int n=re.length;
        for(int i=1,j=0;i<n;i++) {
            int bit=n>>1;for(; (j&bit)!=0;bit>>=1)j^=bit;j^=bit;
            if(i<j){double tmp=re[i];re[i]=re[j];re[j]=tmp;}
        }
        for(int len=2;len<=n;len<<=1) {
            double angle=-2*Math.PI/len,wr=Math.cos(angle),wi=Math.sin(angle);
            for(int i=0;i<n;i+=len) {
                double ur=1,ui=0;
                for(int j=0;j<len/2;j++) {
                    int a=i+j,b=a+len/2;double vr=re[b]*ur-im[b]*ui,vi=re[b]*ui+im[b]*ur;
                    re[b]=re[a]-vr;im[b]=im[a]-vi;re[a]+=vr;im[a]+=vi;
                    double next=ur*wr-ui*wi;ui=ur*wi+ui*wr;ur=next;
                }
            }
        }
    }
    public static float[][] readFilters(File file)throws IOException {
        byte[] bytes=readBytes(file);
        if(bytes.length<12||bytes[0]!=(byte)0x93||bytes[1]!='N')throw new IOException("Invalid mel filter file");
        ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int version=bytes[6]&255,start;
        if(version==1)start=10+(b.getShort(8)&65535);else start=12+b.getInt(8);
        if(bytes.length-start!=128*257*4)throw new IOException("Unexpected filter dimensions");
        b.position(start);float[][] out=new float[128][257];
        for(int m=0;m<128;m++)for(int i=0;i<257;i++)out[m][i]=b.getFloat();
        return out;
    }
    public static String read(File f)throws IOException {return new String(readBytes(f),"UTF-8");}
    public static byte[] readBytes(File f)throws IOException {
        try(InputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return out.toByteArray();
        }
    }
    @Override public void close() {
        for(OrtSession s:new OrtSession[]{joint,decoder,frontend,encoder})if(s!=null)try{s.close();}catch(OrtException ignored){}
        joint=null;decoder=null;frontend=null;encoder=null;
    }
}
