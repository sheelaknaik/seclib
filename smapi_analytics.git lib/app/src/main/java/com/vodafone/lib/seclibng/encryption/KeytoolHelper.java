package com.vodafone.lib.seclibng.encryption;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.KeyToolException;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

/**
 * Keytool helper responsible for creating/maintaining Keytool encryption/decryption.
 */

public class KeytoolHelper {
    public static final String EMPTY_STRING = "nil";
    private static String decKey = null;
    private static final String KEY_STORE_NAME = "AndroidKeyStore";
    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final String TAG_KEYTOOL_HELPER = "KeyToolHelper";

    private KeytoolHelper() {
    }

    /***
     * get Decrypted key if already generated
     *
     * @return Returns the decrypted key
     */
    public static String getDecKey() {
        return decKey;
    }

    /***
     * @param decKey Decryption key to set
     */
    public static void setDecKey(String decKey) {
        KeytoolHelper.decKey = decKey;
    }

    /***
     * Retrieves the pubic key
     *
     * @param context Context object
     * @return Returns
     */
    public static String getPublicKey(Context context) {
        SharedPreferences sf = context.getSharedPreferences(SharedPref.SHARED_PREFERENCE_NAME, 0);

        return sf.getString(Config.KEY_PUBLIC_KEY_ENCRYPTED, EMPTY_STRING);
    }

    /***
     * Generate keytool for key encryption
     *
     * @param context Context object
     * @throws KeyToolException exception
     */
    public static void genkey(Context context) throws KeyToolException {
        try {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 30);
            @SuppressLint("InlinedApi") KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEY_STORE_NAME);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(Config.KEYTOOL_NAME, KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setCertificateSubject(new X500Principal("CN=" + Config.KEYTOOL_NAME))
                        .setCertificateNotBefore(start.getTime())
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .setCertificateNotAfter(end.getTime())
                        .build();
                generator.initialize(spec);
            } else {
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(Config.KEYTOOL_NAME)
                        .setSubject(new X500Principal("CN=" + Config.KEYTOOL_NAME + ", O=Android Authority"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                generator.initialize(spec);
            }
            generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new KeyToolException("No such Algorithm exc"+e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            throw new KeyToolException("No such Provider exc"+e.getMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new KeyToolException("Invalid Algorithm Parameter exc", e);
        } catch (Exception e) {
            throw new KeyToolException("Keytool Exception"+e.getMessage(), e);
        }
    }

    /***
     * Start encryption using the keytool and store key into shared preference
     *
     * @param uuid    Key for the encryption
     * @param context Context object
     */
    public static void doEncryption(String uuid, Context context) throws KeyStoreException,
            UnrecoverableEntryException, NoSuchAlgorithmException, InvalidKeyException,
            SignatureException, IOException, CertificateException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {

        KeyStore ks = KeyStore.getInstance(KEY_STORE_NAME);
        ks.load(null);

        KeyStore.Entry entry = ks.getEntry(Config.KEYTOOL_NAME, null);
        if (entry == null) {
            Logger.e(TAG_KEYTOOL_HELPER, "KeyStore issue:No key found under alias");
            return;
        }

        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Logger.e(TAG_KEYTOOL_HELPER, "KeyStore issue:Not an instance of a PrivateKeyEntry");
            return;
        }

        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

        PublicKey publicKey = privateKeyEntry.getCertificate().getPublicKey();

        Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encodedUser = c.doFinal(Base64.decode(uuid, Base64.DEFAULT));
        String encUuid = Base64.encodeToString(encodedUser, Base64.DEFAULT);
        SharedPreferences sf = context.getSharedPreferences(SharedPref.SHARED_PREFERENCE_NAME, 0);
        SharedPreferences.Editor sfEditor = sf.edit();
        sfEditor.putString(Config.KEY_PUBLIC_KEY_ENCRYPTED, encUuid);
        sfEditor.apply();
    }


    /***
     * Decrypt the key
     *
     * @param encoded Encrypted key from the shared preference
     * @return Returns the string
     */
    public static String doDecryption(String encoded) throws KeyStoreException,
            UnrecoverableEntryException, NoSuchAlgorithmException, InvalidKeyException,
            SignatureException, IOException, CertificateException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {

        KeyStore ks = KeyStore.getInstance(KEY_STORE_NAME);
        ks.load(null);

        KeyStore.Entry entry = ks.getEntry(Config.KEYTOOL_NAME, null);

        if (entry == null) {
            Logger.e(TAG_KEYTOOL_HELPER, "KeyStore issue:No key found under alias");
            return null;
        }
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Logger.e(TAG_KEYTOOL_HELPER, "KeyStore issue:Not an instance of a PrivateKeyEntry");
            return null;
        }

        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

        PrivateKey privateKey = privateKeyEntry.getPrivateKey();
        Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] data = Base64.decode(encoded, Base64.DEFAULT);
        byte[] res = c.doFinal(data);
        return Base64.encodeToString(res, Base64.NO_WRAP);
    }
}
