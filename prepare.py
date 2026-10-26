#!/usr/bin/env python3
"""Project-local dependencies and a checksum-verified model. No global installs."""
from pathlib import Path
import concurrent.futures, hashlib, json, os, urllib.request, zipfile
ROOT=Path(__file__).resolve().parents[1]
D=ROOT/'.deps'; M=ROOT/'.validation/model'
D.mkdir(exist_ok=True); M.mkdir(parents=True,exist_ok=True)
REV='fc592620325027419883cf272e70588d8a788c0a'
REPO='FluidVoice-Labs/fluid-speech-mini-onnx'
BASE='https://huggingface.co/'+REPO+'/resolve/'+REV+'/'
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def download(url,path,expected=None):
 if path.exists() and (not expected or sha(path)==expected):return path
 temp=path.with_suffix(path.suffix+'.part')
 with urllib.request.urlopen(url,timeout=90) as r,temp.open('wb') as f:
  for b in iter(lambda:r.read(1024*1024),b''):f.write(b)
 if expected and sha(temp)!=expected:raise RuntimeError('Checksum mismatch: '+path.name)
 temp.replace(path);print('Ready:',path.name,path.stat().st_size,flush=True)
 return path
for name,url in [
 ('ecj.jar','https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0.jar'),
 ('json.jar','https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar'),
 ('ort-desktop.jar','https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.31.0/onnxruntime-1.31.0.jar'),
 ('ort-android.aar','https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.31.0/onnxruntime-android-1.31.0.aar')]:
 download(url,D/name)
with zipfile.ZipFile(D/'ort-android.aar') as z:
 for name in ['classes.jar','jni/arm64-v8a/libonnxruntime.so','jni/arm64-v8a/libonnxruntime4j_jni.so']:
  p=D/'ort-android'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(name))
meta=json.load(urllib.request.urlopen('https://huggingface.co/api/models/'+REPO+'/revision/'+REV+'?blobs=true',timeout=30))
wanted=['config.json','decoder.onnx','encoder.onnx','encoder.onnx.data','frontend.onnx','joint.onnx','mel_filters.npy','tokenizer.json','transcribe.py','LICENSE','NOTICE']
entries=[(x['rfilename'],x.get('lfs',{}).get('sha256')) for x in meta['siblings'] if x['rfilename'] in wanted]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
 list(ex.map(lambda x:download(BASE+x[0],M/x[0],x[1]),entries))
manifest={'repository':REPO,'revision':REV,'files':[{'name':n,'size':(M/n).stat().st_size,'sha256':sha(M/n)} for n,_ in entries]}
assets=ROOT/'app/assets';assets.mkdir(parents=True,exist_ok=True)
(assets/'model-manifest.json').write_text(json.dumps(manifest,indent=2))
for name in ['LICENSE','NOTICE']:
 (assets/('MODEL-'+name+'.txt')).write_bytes((M/name).read_bytes())
(assets/'ONNX-RUNTIME-NOTICE.txt').write_text('ONNX Runtime 1.31.0, Copyright Microsoft Corporation. MIT License. https://github.com/microsoft/onnxruntime/blob/v1.31.0/LICENSE\n')
download('https://raw.githubusercontent.com/ggml-org/whisper.cpp/master/samples/jfk.wav',ROOT/'.validation/jfk.wav')
print('Pinned dependencies, model and test audio are ready.',flush=True)
