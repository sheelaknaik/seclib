package com.vodafone.lib.seclibng.encryption;


import android.annotation.SuppressLint;

import com.vodafone.lib.seclibng.comms.KeyToolException;
import com.vodafone.lib.seclibng.comms.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/****
 * Encryption helper class
 */
public class AESHelper {
    private static final String TAG_AES_HELPER = "AESHelper";
    private static final String AES = "AES";
    private static final String CIPHER = "AES/ECB/PKCS5Padding";

    /***
     * AES Helper Constructor
     */
    private AESHelper() {
    }

    /***
     * Encrypt a text
     *
     * @param seed      Secret key
     * @param cleartext String to be encrypt
     * @return encrypted text
     * @throws KeyToolException throws exception
     */

    public static String encrypt(String seed, String cleartext) throws KeyToolException {
        byte[] result = encrypt(seed, cleartext.getBytes());
        return byteArrayToHex(result);
    }

    /***
     * decrypt a string
     *
     * @param seed      Secret key
     * @param encrypted String to decrypt
     * @return Decrypted string
     * @throws KeyToolException throws exception
     */
    public static String decrypt(String seed, String encrypted) throws KeyToolException {
        byte[] enc = hexToByteArray(encrypted);
        byte[] result = decrypt(seed, enc);
        return new String(result);
    }

    /***
     * Used to encrypt the data and returns the byte
     *
     * @param key   Encrypting key
     * @param clear value to encrypt
     * @return Returns the byte array
     * @throws KeyToolException throws exception
     */
    private static byte[] encrypt(String key, byte[] clear) throws KeyToolException {
        try {
            SecretKey skeySpec = new SecretKeySpec(key.getBytes(), AES);
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return cipher.doFinal(clear);
        } catch (Exception e) {
            Logger.e(TAG_AES_HELPER, e.getMessage(), e);
            throw new KeyToolException(e.getMessage(), e);
        }
    }

    /***
     * Decrypt the string to byte array
     *
     * @param key       Key for the decryption
     * @param encrypted Encrypted String in Byte array
     * @return byte[] Returns decrypted string in byte array
     * @throws KeyToolException throws exception
     */
    private static byte[] decrypt(String key, byte[] encrypted) throws KeyToolException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), AES);
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            Logger.e(TAG_AES_HELPER, e.getMessage(), e);
            throw new KeyToolException(e.getMessage(), e);
        }
    }
    /***
     * Byte array to hexadecimal conversion
     *
     * @param bytes byte array
     * @return returns HEX for the byte array
     */
    private static String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
    /***
     * Hexadecimal to byte array conversion
     *
     * @param hex hexadecimal string to convert
     * @return byte array
     */
    private static byte[] hexToByteArray(String hex) {
        String hexaString = hex.length() % 2 != 0 ? "0" + hex : hex;
        byte[] b = new byte[hexaString.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hexaString.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }
}


