#!/usr/bin/env python3
"""Small native Android build. Tools stay inside this project; no Gradle daemon."""
from pathlib import Path
import os, subprocess, zipfile, shutil, sys, hashlib, json, secrets
ROOT=Path(__file__).resolve().parents[1]
D=ROOT/'.deps'; B=ROOT/'.build'; OUT=ROOT/'Install'
B.mkdir(exist_ok=True); OUT.mkdir(exist_ok=True)
JAVA=os.environ.get('JAVA_BIN','/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java' if Path('/opt/homebrew/opt/openjdk@17').exists() else 'java')
sdk=Path(os.environ.get('ANDROID_SDK_ROOT','/opt/homebrew/share/android-commandlinetools'))
android=(sdk/'platforms/android-35/android.jar') if sdk.exists() else next((D/'platform').rglob('android.jar'))
tool=(sdk/'build-tools/35.0.0') if sdk.exists() else next((D/'build-tools').rglob('aapt2')).parent
def run(cmd):
 print('Running',str(cmd[0]),flush=True)
 subprocess.run([str(x) for x in cmd],cwd=ROOT,check=True,env={**os.environ,'JAVA_OPTS':'-Xmx256m'})
run([tool/'aapt2','compile','--dir',ROOT/'app/res','-o',B/'resources.zip'])
run([tool/'aapt2','link','-o',B/'base.ap_','-I',android,'--manifest',ROOT/'app/AndroidManifest.xml','--java',B/'generated','--auto-add-overlay','-A',ROOT/'app/assets',B/'resources.zip'])
classes=B/'classes'; classes.mkdir(exist_ok=True)
sources=list((ROOT/'app/src').rglob('*.java'))+list((B/'generated').rglob('*.java'))
javac=str(Path(JAVA).parent/'javac') if '/' in JAVA else 'javac'
run([javac,'-J-Xmx256m','-source','8','-target','8','-Xlint:none','-encoding','UTF-8','-cp',str(android)+os.pathsep+str(D/'ort-android/classes.jar'),'-d',classes,*sources])
jar=B/'app-classes.jar'
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
 for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes))
dex=B/'dex';dex.mkdir(exist_ok=True)
run([JAVA,'-Xmx256m','-cp',tool/'lib/d8.jar','com.android.tools.r8.D8','--min-api','33','--lib',android,'--output',dex,jar,D/'ort-android/classes.jar'])
shutil.copyfile(B/'base.ap_',B/'unsigned.ap_')
with zipfile.ZipFile(B/'unsigned.ap_','a',zipfile.ZIP_DEFLATED) as z:
 for p in dex.glob('*.dex'):z.write(p,p.name)
 for p in (D/'ort-android/jni/arm64-v8a').glob('*.so'):z.write(p,'lib/arm64-v8a/'+p.name)
run([tool/'zipalign','-f','-p','4',B/'unsigned.ap_',B/'aligned.ap_'])
# Never store release-key passwords in tracked source. The private key and
# signing.json both live in the gitignored .deps/ directory.
key=Path(os.environ.get('PHONE_WHISPER_KEYSTORE',str(D/'personal-release.jks')))
signing_file=D/'signing.json'
if signing_file.exists():os.chmod(signing_file,0o600)
saved=json.loads(signing_file.read_text()) if signing_file.exists() else {}
alias=os.environ.get('PHONE_WHISPER_KEY_ALIAS',saved.get('alias','fluidkeys'))
password=os.environ.get('PHONE_WHISPER_KEYSTORE_PASSWORD',saved.get('password',''))
key_password=os.environ.get('PHONE_WHISPER_KEY_PASSWORD',saved.get('key_password',password))
keytool=os.environ.get('KEYTOOL_BIN',str(Path(JAVA).parent/'keytool') if '/' in JAVA else 'keytool')
if not key.exists():
 if not password:password=secrets.token_urlsafe(32)
 if not key_password:key_password=password
 run([keytool,'-genkeypair','-keystore',key,'-storepass',password,'-keypass',key_password,'-alias',alias,'-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Phone Whisper local build'])
 if not signing_file.exists() and not os.environ.get('PHONE_WHISPER_KEYSTORE_PASSWORD'):
  fd=os.open(signing_file,os.O_CREAT|os.O_WRONLY|os.O_EXCL,0o600)
  with os.fdopen(fd,'w') as f:json.dump({'alias':alias,'password':password,'key_password':key_password},f)
if not password:sys.exit('Signing credentials missing. Set PHONE_WHISPER_KEYSTORE_PASSWORD, or add .deps/signing.json (ignored by Git).')
apk=OUT/'PhoneWhisper.apk'
run([JAVA,'-Xmx128m','-jar',tool/'lib/apksigner.jar','sign','--ks',key,'--ks-pass','pass:'+password,'--key-pass','pass:'+key_password,'--ks-key-alias',alias,'--min-sdk-version','24','--v2-signing-enabled','true','--v3-signing-enabled','true','--v4-signing-enabled','false','--out',apk,B/'aligned.ap_'])
run([JAVA,'-Xmx128m','-jar',tool/'lib/apksigner.jar','verify','--verbose',apk])
legacy=OUT/'FluidKeys.apk'
if legacy.exists():legacy.unlink()
print('APK:',apk,'bytes:',apk.stat().st_size,'SHA256:',hashlib.sha256(apk.read_bytes()).hexdigest())
