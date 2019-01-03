package com.vodafone.lib.seclibng.comms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.os.ConfigurationCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import com.vodafone.lib.seclibng.SecLibNG;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * Class to retrieve event header values
 */

public class EventHeaders {
    private static final String TAG = "EventHeaders";
    private static NetworkInfo network = null;
    private static final int SIM_MCC = 1;
    private static final int SIM_MNC = 2;

    /***
     * Returns the content type for the http request
     *
     * @return content type String
     */
    public static String getContentType() {
        return "application/json";
    }

    /***
     * Returns the trace source for the http request
     *
     * @return trace source String
     */
    public static String getTraceSource(Context context) {
        if (context != null) {
            if (!TextUtils.isEmpty(SecLibNG.getInstance().getTraceSource(context))) {
                return Config.KEYNAME_ANDROID + SecLibNG.getInstance().getTraceSource(context);
            } else {
                return Config.KEYNAME_ANDROID + context.getPackageName();
            }
        }
        return Config.DEFAULT_NA;
    }

    /***
     * Returns the network bearer
     *
     * @return content type String
     */
    @SuppressLint("MissingPermission")
    public static String getNetWorkBearer(Context context) {
        try {
            if (context != null && EventHeaders.getNetworkTypeName(context).equalsIgnoreCase(Config.KEYNAME_WIFI)) {
                return Config.KEYNAME_WIFI;
            } else if (context != null && EventHeaders.getNetworkTypeName(context).equalsIgnoreCase(Config.KEYNAME_Mobile)) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                if (networkInfo != null) {
                    int networkType = networkInfo.getSubtype();
                    switch (networkType) {
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_IDEN: //api<8 : replace by 11
                            return Config.KEYNAME_TWOG;
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B: //api<9 : replace by 14
                        case TelephonyManager.NETWORK_TYPE_EHRPD:  //api<11 : replace by 12
                        case TelephonyManager.NETWORK_TYPE_HSPAP:  //api<13 : replace by 15
                        case TelephonyManager.NETWORK_TYPE_TD_SCDMA:  //api<25 : replace by 17
                            return Config.KEYNAME_THREEG;
                        case TelephonyManager.NETWORK_TYPE_LTE:    //api<11 : replace by 13
                        case TelephonyManager.NETWORK_TYPE_IWLAN:  //api<25 : replace by 18
                        case 19:  //LTE_CA
                            return Config.KEYNAME_FOURG;
                        default:
                            return Config.DEFAULT_NA;
                    }
                }
                return Config.DEFAULT_NA;
            }
            return Config.DEFAULT_NA;

        } catch (SecurityException e) {
            Logger.d(TAG, "Missing permission,android.permission.ACCESS_NETWORK_STATE", e);
            return Config.DEFAULT_NA;
        }
    }

    /***
     * Returns the trace source version
     *
     * @return content type String
     */
    public static String getTraceSourceVersion(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e("applicationVersion", Config.DEFAULT_NA, e);
            return "Unknown";
        }
    }

    /***
     * Returns Subject ID
     *
     * @return content type String
     */
    @SuppressLint("HardwareIds")
    public static String getSubjectId(Context context) {
        try {

            return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (SecurityException e) {
            Logger.e(TAG, "Unable to retrieve Android ID " + e.getMessage(), e);
            return Config.DEFAULT_NA;

        }
    }

    /***
     * Returns Network type Name
     *
     * @return content type String
     */
    public static String getNetworkTypeName(Context context) {
        String networkTypeName = null;
        NetworkCapabilities networkCapabilities = null;
        Network network = null;
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                network = connMgr.getActiveNetwork();
                if (network == null) {
                    return Config.DEFAULT_NA;
                }
                networkCapabilities = connMgr.getNetworkCapabilities(network);
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    networkTypeName = Config.KEYNAME_WIFI;
                    return networkTypeName;
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    networkTypeName = Config.KEYNAME_Mobile;
                    ;
                    return networkTypeName;
                }
                return Config.DEFAULT_NA;
            } else {
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected()) {
                    return Config.DEFAULT_NA;
                }
                networkTypeName = networkInfo.getTypeName();
                return networkTypeName;
            }
        } catch (SecurityException e) {
            Logger.d("Event.java", "Missing permission,android.permission.ACCESS_NETWORK_STATE", e);
            return Config.DEFAULT_NA;
        }
    }


    /***
     * Returns install ID
     *
     * @return String
     */
    public static String getInstallId(Context context) {
        return SharedPref.getInstallId(context);
    }

    /***
     * Returns Seclib client version
     *
     * @return String
     */
    public static String getClientVersion() {
        return "2.7.1";
    }

    /***
     * Returns the Platform
     *
     * @return String
     */
    @SuppressWarnings("SameReturnValue")
    public static String getPlatform() {
        return "Android";
    }

    /***
     * Returns the OS Version
     *
     * @return String
     */
    public static String getOsVersion() {
        return Build.VERSION.RELEASE;
    }

    /***
     * Returns the OS Name
     *
     * @return String
     */
    public static String getOsName() {

        Field[] fields = Build.VERSION_CODES.class.getFields();
        String osName = null;

        for (Field field : fields) {
            osName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());

                if (fieldValue == Build.VERSION.SDK_INT) {
                    return osName;
                }
            } catch (IllegalArgumentException e) {
                Logger.e(TAG, "IllegalArgumentException getting osName " + e.getMessage(), e);

            } catch (IllegalAccessException e) {
                Logger.e(TAG, "IllegalAccessException getting osName " + e.getMessage(), e);
            } catch (NullPointerException e) {
                Logger.e(TAG, "NullPointerException getting osName " + e.getMessage(), e);
            }
        }

        return "NA";
    }

    /***
     * Returns the Application Name
     *
     * @return Application name String
     */
    public static String getApplicationName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
            return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : Config.DEFAULT_NA);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(TAG, "Exception while retrieving the Application name");
            return Config.DEFAULT_NA;
        }
    }

    /***
     * Returns the User Agent name
     *
     * @return Application name String
     */
    public static String getUserAgent() {
        String userAgent = System.getProperty("http.agent");
        String model = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;

        if (!(model == null || model.isEmpty())) {
            userAgent += Config.DEVICE_LABEL + model;
        }

        if (!(manufacturer == null || manufacturer.isEmpty())) {
            userAgent += Config.MANUFACTURER_LABEL + manufacturer;
        }

        return userAgent;
    }

    /***
     * Returns region of current registered country code (Visiting country)
     *
     * @return String
     */
    public static String getSubjectRegion(Context context) {
        try {
            String region = null;
            TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            region = mTelephonyManager.getNetworkCountryIso();

            if (region != null && region.length() > 0)
                return region;
            else
                return Config.DEFAULT_NA;
        } catch (Exception e) {
            return Config.DEFAULT_NA;
        }
    }

    /***
     * Returns the MCC from sim details
     *
     * @return String
     */
    public static String getMcc(Context context) {
        return simDetails(context, SIM_MCC);
    }

    /***
     * Returns the MNC from sim details
     *
     * @return String
     */
    public static String getMnc(Context context) {
        return simDetails(context, SIM_MNC);
    }

    /***
     * Returns Locale
     *
     * @return String
     */
    public static String getLocale() {

        try {
            Locale locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);

            String countryISO = locale.getCountry();

            if (countryISO != null && countryISO.length() > 0)
                return countryISO;
            else
                return Config.DEFAULT_NA;
        } catch (Exception e) {
            Logger.e(TAG, "Missing permission,android.permission.ACCESS_NETWORK_STATE", e);

            return Config.DEFAULT_NA;
        }
    }

    /***
     * Returns Device screen height
     *
     * @return String
     */
    public static String getHeight(Context context) {
        return Integer.toString(displayMetrics(context).x);
    }

    /***
     * Returns Device screen height
     *
     * @return String
     */
    public static String getWidth(Context context) {
        return Integer.toString(displayMetrics(context).y);
    }

    /***
     * Retrieves the sim details includes MCC MNC and region.
     *
     * @param context Context object
     * @param item    MCC/MNC/Region int
     * @return Corresponding values.
     */
    private static String simDetails(Context context, int item) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String simOperator = tm.getSimOperator();
            if (simOperator != null && (simOperator.length() >= 4)) {
                String mcc = simOperator.substring(0, 3);
                switch (item) {
                    case SIM_MCC:
                        return mcc;
                    default:
                        return simOperator.substring(3);
                }
            } else {
                return Config.DEFAULT_NA;
            }
        } catch (Exception e) {
            return Config.DEFAULT_NA;
        }
    }

    /***
     * Display metrics able to find the height and width of the screen
     *
     * @param context Context object
     * @return result as POINT object.
     */
    private static Point displayMetrics(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

}