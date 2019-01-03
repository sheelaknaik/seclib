package com.vodafone.lib.seclibng.certpinning;

import android.content.Context;
import android.net.http.X509TrustManagerExtensions;
import android.util.Base64;

import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * CertHelper is the Helper class for Certification validation process.
 */

public class CertHelper {
    private Context context;
    private static final String TAG_CERT_HELPER = "CertHelper";

    /***
     * CertHelper Constructor
     *
     * @param context app context
     */
    public CertHelper(Context context) {
        this.context = context;

    }

    /***
     * Validate pins from the host.if its valid,returns else throws Exception,
     *
     * @param trustManagerExt Trust manager extensions.
     * @param conn            Http url connection
     * @param validPins       Already stored pins from the host.
     * @throws SSLException throws exception
     */
    private boolean validatePinning(X509TrustManagerExtensions trustManagerExt, HttpsURLConnection conn, Set<String> validPins) throws IOException {

        boolean verified = false;
        try {
            if (validPins == null || validPins.isEmpty())
                return verified;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            List<X509Certificate> trustedChain = trustedChain(trustManagerExt, conn);
            for (X509Certificate cert : trustedChain) {
                try {
                    cert.checkValidity();
                    byte[] publicKey = cert.getPublicKey().getEncoded();
                    md.update(publicKey, 0, publicKey.length);
                    String pin = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                    if (validPins.contains(pin)) {
                        verified = true;
                        break;
                    }
                } catch (CertificateExpiredException cee) {
                    Logger.e(TAG_CERT_HELPER, "Expired Certificate " + cee.getMessage(), cee);
                } catch (CertificateNotYetValidException cee) {
                    Logger.e(TAG_CERT_HELPER, "Certificate not yet validated exception " + cee.getMessage(), cee);
                }
            }
            return verified;
        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG_CERT_HELPER, "NoSuchAlgorithmException " + e.getMessage(), e);
            return verified;
        } catch (SSLPeerUnverifiedException e) {
            Logger.e(TAG_CERT_HELPER, "SSLPeerUnverifiedException " + e.getMessage(), e);
            return verified;
        }
    }

    /****
     * Retrieves the certificates from the Host address.
     *
     * @param trustManagerExt Trust manger extensions
     * @param conn            HttpsUrl connection
     * @return list of certificates (X509)
     * @throws SSLException throws exception
     */
    private List<X509Certificate> trustedChain(X509TrustManagerExtensions trustManagerExt, HttpsURLConnection conn) throws SSLException {

        Certificate[] serverCerts = conn.getServerCertificates();
        X509Certificate[] untrustedCerts = Arrays.copyOf(serverCerts, serverCerts.length, X509Certificate[].class);
        String host = conn.getURL().getHost();
        try {
            return trustManagerExt.checkServerTrusted(untrustedCerts, "RSA", host);
        } catch (Exception e) {
            throw new SSLException(e);
        }
    }

    /***
     * Start Certification validation
     *
     * @param urlConnection HTTPS url connection
     * @return Validates the algorithm and returns true if the certificate is valid.
     */
    public boolean cert(HttpsURLConnection urlConnection) {
        boolean validated = false;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            X509TrustManager x509TrustManager = null;
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    x509TrustManager = (X509TrustManager) trustManager;
                    break;
                }
            }
            X509TrustManagerExtensions trustManagerExt = new X509TrustManagerExtensions(x509TrustManager);
            @SuppressWarnings("unchecked") Set<String> validPins = SharedPref.getKeys(context);
            validated = validatePinning(trustManagerExt, urlConnection, validPins);
        } catch (NoSuchAlgorithmException exe) {
            Logger.e(TAG_CERT_HELPER, "No such algorithm Exception " + exe.getMessage(), exe);
        } catch (KeyStoreException exe) {
            Logger.e(TAG_CERT_HELPER, "Keystore Exception " + exe.getMessage(), exe);
        } catch (IOException iE) {
            Logger.e(TAG_CERT_HELPER, "IO exception Exception " + iE.getMessage(), iE);
        }
        return validated;
    }

}
