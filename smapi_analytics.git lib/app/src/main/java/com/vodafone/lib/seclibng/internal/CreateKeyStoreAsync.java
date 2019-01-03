package com.vodafone.lib.seclibng.internal;

import android.content.Context;
import android.os.AsyncTask;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;
import com.vodafone.lib.seclibng.encryption.KeytoolHelper;
import com.vodafone.lib.seclibng.storage.SqliteDb;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;

/**
 * Create keystore for storing key
 */

public class CreateKeyStoreAsync extends AsyncTask<String, Void, String> {
    int repeatCounter;
    private Context mContext;
    private String keyStr;
    private SqliteDb db;

    private static final String KEYSTORE_ASYNC_TAG = "CreateKeyStoreAsync";

    public CreateKeyStoreAsync(int repeatCounter,Context context) {
        this.repeatCounter = repeatCounter + 1;
        mContext = context;
    }

    @Override
    protected String doInBackground(String... uuid) {
        try {

            keyStr = uuid[0];
            KeytoolHelper.genkey(mContext);
        } catch (Exception e) {
            Logger.e(KEYSTORE_ASYNC_TAG, "Error while creating Keytool " + e.getMessage(), e);
            return null;
        }
        return uuid[0];
    }

    @Override
    protected void onPostExecute(String uuid) {
        super.onPostExecute(uuid);
        if (uuid == null) {
            Logger.i(KEYSTORE_ASYNC_TAG, "Error while creating encryption key in attempt #" + repeatCounter);
            if (repeatCounter <= Config.MAX_KEYGEN_RETRY_COUNT) {

                CreateKeyStoreAsync createKeyStoreAsync = new CreateKeyStoreAsync(repeatCounter,mContext);
                createKeyStoreAsync.execute(keyStr);
            } else {
                SharedPref.setEncryptionKeyStatus(mContext, false);
            }
        } else {
            try {

                KeytoolHelper.doEncryption(uuid, mContext);
               
                if(KeytoolHelper.getDecKey()==null) {
                    SharedPref.setEncryptionKeyStatus(mContext, true);

                    DecryptionAsync decryptionAsync =  new DecryptionAsync(mContext);
                    decryptionAsync.execute();
                }

            }catch (KeyStoreException e) {
                Logger.e(KEYSTORE_ASYNC_TAG, "KeyStore issue:KeyStore not Initialized", e);
            } catch (UnrecoverableEntryException e) {
                Logger.e(KEYSTORE_ASYNC_TAG, "KeyStore issue:KeyPair not recovered", e);
            } catch (NoSuchAlgorithmException e) {
                Logger.e(KEYSTORE_ASYNC_TAG, "KeyStore issue:RSA not supported", e);
            } catch (InvalidKeyException e) {
                Logger.e(KEYSTORE_ASYNC_TAG, "KeyStore issue:Invalid Key", e);
            } catch (SignatureException e) {
                Logger.e(KEYSTORE_ASYNC_TAG, "KeyStore issue:Invalid Signature", e);
            } catch (IOException e) {
                Logger.e(KEYSTORE_ASYNC_TAG, "KeyStore issue:IO Exception " +e.getMessage(), e);
            }
            catch (Exception e) {
                Logger.e(KEYSTORE_ASYNC_TAG, "Exception while inserting event " + e.getMessage(), e);
            }
        }
    }
}

