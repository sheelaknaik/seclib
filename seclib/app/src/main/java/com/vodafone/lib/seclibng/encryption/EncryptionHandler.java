package com.vodafone.lib.seclibng.encryption;

import android.util.Base64;

import com.vodafone.lib.seclibng.comms.KeyToolException;
import com.vodafone.lib.seclibng.comms.Logger;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

    public class EncryptionHandler {

    private static final String CIPHER = "AES/CBC/PKCS5Padding";
    private static final int ivSize =16;
    private static final String TAG_AES_HELPER = "EncryptionHandler";

    private EncryptionHandler() {
    }

    public static String encrypt(String key,String message)  {

        String encryptedData = null;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {

            try {
                encryptedData = AESHelper.encrypt(key,message);
            } catch (KeyToolException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES encryption "+ e.getMessage(), e);
            }
            return encryptedData;
        }
        else {
            try {
                byte[] srcBuff = message.getBytes();

                // Generating IV.
                byte[] iv = new byte[ivSize];
                SecureRandom random = new SecureRandom();
                random.nextBytes(iv);
                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

                SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

                // Encrypt.
                Cipher cipher = Cipher.getInstance(CIPHER);
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
                byte[] encrypted = cipher.doFinal(srcBuff);

                // Combine IV and encrypted part.
                byte[] encryptedIVAndText = new byte[ivSize + encrypted.length];
                System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize);
                System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.length);

                String base64 = Base64.encodeToString(encryptedIVAndText, Base64.DEFAULT);

                encryptedData= base64;
            }
            catch (NoSuchAlgorithmException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES CBC encryption "+ e.getMessage(), e);
            } catch (InvalidKeyException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES CBC encryption "+ e.getMessage(), e);
            } catch (InvalidAlgorithmParameterException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES CBC encryption "+ e.getMessage(), e);
            } catch (NoSuchPaddingException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES CBC encryption "+ e.getMessage(), e);
            } catch (BadPaddingException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES CBC encryption "+ e.getMessage(), e);
            } catch (UnsupportedEncodingException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES CBC encryption "+ e.getMessage(), e);
            } catch (IllegalBlockSizeException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES CBC encryption "+ e.getMessage(), e);
            }
        }
        return encryptedData;
    }

    public static String decrypt(String key, String encrypted) {
       String decryptedData=null;

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {

            try {
                decryptedData = AESHelper.decrypt(key,encrypted);
            } catch (KeyToolException e) {
                Logger.e(TAG_AES_HELPER,"Exception in AES decryption "+ e.getMessage(), e);
                e.printStackTrace();
            }
            return decryptedData;
        }else {
         try {
             byte[] encryptedData = Base64.decode(encrypted, Base64.DEFAULT);

             // Extract IV.
             byte[] iv = new byte[ivSize];
             System.arraycopy(encryptedData, 0, iv, 0, iv.length);
             IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

             // Extract encrypted part.
             int encryptedSize = encryptedData.length - ivSize;
             byte[] encryptedBytes = new byte[encryptedSize];
             System.arraycopy(encryptedData, ivSize, encryptedBytes, 0, encryptedSize);

             SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

             // Decrypt.
             Cipher cipherDecrypt = Cipher.getInstance(CIPHER);
             cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
             byte[] decrypted = cipherDecrypt.doFinal(encryptedBytes);

             decryptedData= new String(decrypted);
         }
          catch (NoSuchAlgorithmException e) {
              Logger.e(TAG_AES_HELPER,"Exception in AES CBC decryption "+ e.getMessage(), e);
         } catch (InvalidKeyException e) {
             Logger.e(TAG_AES_HELPER,"Exception in AES CBC decryption "+ e.getMessage(), e);
         } catch (InvalidAlgorithmParameterException e) {
             Logger.e(TAG_AES_HELPER,"Exception in AES CBC decryption "+ e.getMessage(), e);
         } catch (NoSuchPaddingException e) {
             Logger.e(TAG_AES_HELPER,"Exception in AES CBC decryption "+ e.getMessage(), e);
         } catch (BadPaddingException e) {
             Logger.e(TAG_AES_HELPER,"Exception in AES CBC decryption "+ e.getMessage(), e);
             e.printStackTrace();
         } catch (UnsupportedEncodingException e) {
             Logger.e(TAG_AES_HELPER,"Exception in AES CBC decryption "+ e.getMessage(), e);
             e.printStackTrace();
         } catch (IllegalBlockSizeException e) {
             Logger.e(TAG_AES_HELPER,"Exception in AES CBC decryption "+ e.getMessage(), e);
             e.printStackTrace();
         }
        }
        return decryptedData;
    }
}