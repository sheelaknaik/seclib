package com.vodafone.lib.seclibng.internal;

import android.content.Context;
import android.os.AsyncTask;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.encryption.KeytoolHelper;
import com.vodafone.lib.seclibng.storage.SqliteDb;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;

/**
 * Async task to do decryption without effecting the UI.
 */

public class DecryptionAsync extends AsyncTask<String, String, String> {

    String decryptionKey;
    private static final String DECRYPTION_ASYNC_TAG = "DecryptionAsync";
    private Context mContext;
    private SqliteDb db;

    public DecryptionAsync(Context context) {
        mContext = context;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            String encKey = KeytoolHelper.getPublicKey(mContext).replace("\n", "");
            decryptionKey= KeytoolHelper.doDecryption(encKey);
        }catch (KeyStoreException e) {
            Logger.e(DECRYPTION_ASYNC_TAG, "KeyStore issue:KeyStore not Initialized", e);
        } catch (UnrecoverableEntryException e) {
            Logger.e(DECRYPTION_ASYNC_TAG, "KeyStore issue:KeyPair not recovered", e);
        } catch (NoSuchAlgorithmException e) {
            Logger.e(DECRYPTION_ASYNC_TAG, "KeyStore issue:RSA not supported", e);
        } catch (InvalidKeyException e) {
            Logger.e(DECRYPTION_ASYNC_TAG, "KeyStore issue:Invalid Key", e);
        } catch (SignatureException e) {
            Logger.e(DECRYPTION_ASYNC_TAG, "KeyStore issue:Invalid Signature", e);
        } catch (IOException e) {
            Logger.e(DECRYPTION_ASYNC_TAG, "KeyStore issue:IO Exception " +e.getMessage(), e);
        }
        catch (Exception e) {
            Logger.e(DECRYPTION_ASYNC_TAG, "Exception while inserting event " + e.getMessage(), e);
        }
        return decryptionKey;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        KeytoolHelper.setDecKey(s);

        if (Event.getPendingEvents() != null) {
            db = SqliteDb.getInstance(mContext);
            db.insertEvent(Event.getPendingEvents(), true);
        }
    }
}