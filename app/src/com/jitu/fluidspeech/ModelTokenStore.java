package com.jitu.fluidspeech;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Optional Hugging Face token. The encryption key never leaves Android Keystore. */
final class ModelTokenStore {
 private static final String ALIAS="phone-whisper-hf-token";
 private static final String PREFS="model-source-secrets";
 private static final String ENTRY="hf_token_v1";
 private ModelTokenStore(){}
 private static SecretKey key()throws Exception{
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(store.containsAlias(ALIAS))return ((KeyStore.SecretKeyEntry)store.getEntry(ALIAS,null)).getSecretKey();
  KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
  return generator.generateKey();
 }
 static void save(Context context,String token)throws Exception{
  if(token==null||token.trim().isEmpty()){clear(context);return;}
  String value=token.trim();
  if(!value.matches("hf_[A-Za-z0-9_\\-]{8,}"))throw new IllegalArgumentException("Enter a Hugging Face access token starting with hf_.");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
  byte[] iv=cipher.getIV(),body=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
  String encrypted=Base64.encodeToString(iv,Base64.NO_WRAP)+":"+Base64.encodeToString(body,Base64.NO_WRAP);
  context.getSharedPreferences(PREFS,0).edit().putString(ENTRY,encrypted).apply();
 }
 static String read(Context context)throws Exception{
  String entry=context.getSharedPreferences(PREFS,0).getString(ENTRY,"");
  if(entry.isEmpty())return "";
  String[] fields=entry.split(":",-1);if(fields.length!=2)throw new IllegalStateException("Saved token is damaged. Remove and save it again.");
  byte[] iv=Base64.decode(fields[0],Base64.NO_WRAP),body=Base64.decode(fields[1],Base64.NO_WRAP);
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
  return new String(cipher.doFinal(body),StandardCharsets.UTF_8);
 }
 static boolean saved(Context context){return !context.getSharedPreferences(PREFS,0).getString(ENTRY,"").isEmpty();}
 static void clear(Context context){context.getSharedPreferences(PREFS,0).edit().remove(ENTRY).apply();}
}
