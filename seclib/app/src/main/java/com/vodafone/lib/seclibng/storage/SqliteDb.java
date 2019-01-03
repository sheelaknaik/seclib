package com.vodafone.lib.seclibng.storage;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.util.Log;

import com.vodafone.lib.seclibng.Event;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.comms.SharedPref;
import com.vodafone.lib.seclibng.encryption.EncryptionHandler;
import com.vodafone.lib.seclibng.encryption.KeytoolHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * This class deals with SQLITE Operations
 */
public class SqliteDb extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Database_SecLib";
    private static final int DATABASE_VERSION = 2;
    private static SqliteDb dbInstance;
    private Context context;
    private SQLiteDatabase db;
    private static final String TABLE_NAME = "SecLibEvents";
    private static final String TABLE_COLUMNID = "id";
    private static final String TABLE_COLUMN_USER = "user";
    private static final String TABLE_COLUMN_EVENT_TYPE = "event_type";
    private static final String TABLE_COLMNEVENTDATA = "event_data";
    private static final String TABLE_COLMNEVENTTIME = "event_creation_time";
    private static final String TAG_SQLITE_DB = "SqliteDB";
    private static long nouserIdcurrentTime;
    private static final String CREATE_TABLE = "create table " + TABLE_NAME + " (id INTEGER PRIMARY KEY,event_data TEXT,user TEXT,event_type TEXT,event_creation_time INTEGER)";

    /***
     * INITIALIZE THE SQLITEDB WITH CONTEXT OBJECT
     *
     * @param ctxt Context Object
     */
    private SqliteDb(Context ctxt) {
        super(ctxt, DATABASE_NAME, null, DATABASE_VERSION);
        context = ctxt;
    }

    /***
     * SINGLE INSTANCE OF SqliteDb
     *
     * @param context Context object
     * @return SqliteDb object instantiation.
     */
    public static synchronized SqliteDb getInstance(Context context) {
        if (dbInstance == null)
            dbInstance = new SqliteDb(context);
        return dbInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Logger.i(TAG_SQLITE_DB, "Upgrading DB from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 2){
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }

    /***
     * time that checked for no user id event age
     */
    public long getNouserIdcurrentTime() {
        return nouserIdcurrentTime;
    }

    /***
     * Opening Database  if it is not Open
     */
    private void openDb() {
        try {
            if (db == null) {
                db = this.getWritableDatabase();
            }
            if (!db.isOpen()) {
                db = this.getWritableDatabase();
            }

        } catch (Exception e) {
            Logger.e(TAG_SQLITE_DB, "Exception while opening the DB", e);
        }
    }

    /**
     * insertEvent insert events to Sqlite db
     *
     * @param events      List of events
     * @param pendingList the event list is pending list or not
     */
    public void insertEvent(List<Event> events, boolean pendingList) {
        InsertEventAsync insertEventAsync = new InsertEventAsync(events, pendingList);
        insertEventAsync.execute();
    }

    public void insertEventNonAsync(List<Event> events, boolean pendingList) {

        try {
            if (KeytoolHelper.getPublicKey(context).equalsIgnoreCase(KeytoolHelper.EMPTY_STRING) || KeytoolHelper.getDecKey() == null) {

                if (SharedPref.getEncryptionKeyStatus(context)) {
                    Event.setPendingEvents(events);
                    Logger.i(TAG_SQLITE_DB, "Adding events into pending list");
                 
                } else {
                    if (Event.getPendingEvents() != null) {
                        Event.clearEventArray();
                        Logger.e(TAG_SQLITE_DB, "Unable to create encryption key.Ignoring all events");
                    }
                }
                return;
            }

            String key = KeytoolHelper.getDecKey();
            String userId = SharedPref.getConfigKeys(context, Config.KEYNAME_USER_ID, Config.KEYNAME_USER_ID_UNKNOWN);
            String encryptedStringUser = userId.equals(Config.KEYNAME_USER_ID_UNKNOWN) ? userId : EncryptionHandler.encrypt(key, userId);
            String sql = "INSERT INTO " + TABLE_NAME + " ( " + TABLE_COLMNEVENTDATA + "," + TABLE_COLMNEVENTTIME +
                    "," + TABLE_COLUMN_EVENT_TYPE + "," + TABLE_COLUMN_USER + " )" + " VALUES (?,?,?,? )";
            openDb();
            db.beginTransactionNonExclusive();
            SQLiteStatement stmt = db.compileStatement(sql);
            for (int i = 0; i < events.size(); i++) {
                Event event = events.get(i);
                if (event != null) {
                    Logger.i(TAG_SQLITE_DB, event.getJSONObject().toString());

                    String encryptedString = EncryptionHandler.encrypt(key, event.getJSONObject().toString());
                    stmt.bindString(1, encryptedString);
                    stmt.bindLong(2, System.currentTimeMillis());
                    stmt.bindString(3, event.getEventType().toString());
                    stmt.bindString(4, encryptedStringUser);
                    if (event.getJSONObject().length() != 0) {
                        stmt.execute();
                    }
                    stmt.clearBindings();
                } else
                    events.remove(i);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            SharedPref.setEventCountInDatabase(context, events.size(), false);
            long currentDbCount = SharedPref.getEventCountInDatabase(context);
            int maxEventsinb = Integer.parseInt(SharedPref.getConfigKeys(context, Config.KEYNAME_MAX_EVENTS_IN_DATABASE, Config.DEFAULT_MAX_NO_EVENTS_IN_DB));
            if (currentDbCount > maxEventsinb) {
                openDb();
                int no = db.delete(TABLE_NAME, TABLE_COLMNEVENTTIME + "<=(select " + TABLE_COLMNEVENTTIME + " from " + TABLE_NAME + " order by " + TABLE_COLUMNID + " desc limit 1 offset " + maxEventsinb + ")", null);
                SharedPref.setEventCountInDatabase(context, no, true);
            }

            Config.setAlarm(context);
            if (pendingList) {
                Event.clearEventArray();
            }
        } catch (SQLiteException ex) {
            Logger.e(TAG_SQLITE_DB, "Exception with SQL transaction: " + ex.getMessage(), ex);
        } catch (Exception e) {
            Logger.e(TAG_SQLITE_DB, "Error while inserting  item into database", e);
        }


    }

    class InsertEventAsync extends AsyncTask<Void, Void, Void> {
        List<Event> events;
        boolean pendingList;

        public InsertEventAsync(List<Event> events, boolean pendingList) {
            this.events = new ArrayList<>();
            this.events.addAll(events);
            this.pendingList = pendingList;
        }
        @Override
        protected Void doInBackground(Void... voids) {

            insertEventNonAsync(events,pendingList);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    /**
     * One or more events stored in Sqlite db or not
     *
     * @return true, if events
     */

    public boolean hasMoreEvents() {
        openDb();
        boolean returnVale = false;
        Cursor c = db.query(TABLE_NAME, new String[]{TABLE_COLUMNID}, null, null, null, null, null, null);
        try {
            if (c.moveToFirst()) {
                returnVale = true;
            }
            return returnVale;
        } catch (IllegalStateException e) {
            Logger.e(TAG_SQLITE_DB, "Sqlite DB Exception " + e.getMessage());
            return false;
        } finally {
            if (c != null) {
                c.close();

            }
        }
    }

    /**
     * Get Events from Sqlite
     *
     * @return Event[] array of Events
     */
    public Event[] getEventDataArrayAll() {
        openDb();
        int maxEventLimit = Integer.parseInt(SharedPref.getConfigKeys(context, Config.KEYNAME_MAX_NO_OF_EVENTS, Config.DEFAULT_MAX_NO_OF_EVENTS));
        int limit = maxEventLimit - SharedPref.getCurrentEventsSentCount(context);
        Cursor c = db.query(TABLE_NAME, new String[]{TABLE_COLMNEVENTTIME, TABLE_COLMNEVENTDATA}, null, null, null, null, TABLE_COLUMNID + " ASC", Integer.toString(limit));
        ArrayList<Event> eventList = new ArrayList<>();
        String invalidEvents = "";
        try {
            if (!c.moveToFirst()) {
                return new Event[0];
            }
            do {
                String decryptedString = "{}";
                String key = KeytoolHelper.getDecKey();

                decryptedString = (key == null) ? decryptedString : EncryptionHandler.decrypt(key, c.getString(1));

                if (!"{}".equals(decryptedString)) {
                    Event event = new Event(decryptedString, c.getString(0));
                    if (!Config.blackListStatus(event.getJSONObject(), Config.blackListString(context))) {
                        eventList.add(event);
                    } else {
                        invalidEvents = (("").equalsIgnoreCase(invalidEvents)) ? c.getString(0) : invalidEvents + "," + c.getString(0);
                    }
                }
            }
            while (c.moveToNext());
            if (!invalidEvents.isEmpty()) {
                deleteEvents(invalidEvents, true);
            }
            Event[] e = new Event[eventList.size()];
            for (int j = 0; j < eventList.size(); j++) {
                e[j] = eventList.get(j);
            }
            return e;
        } catch (Exception exc) {
            Logger.e(TAG_SQLITE_DB, "Database exception : " + exc.getMessage(), exc);
            return new Event[0];
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Get Events from Sqlite
     *
     * @return Event[] array of Events
     */
    public Event[] getEventDataArrayAllNoUserIdEvents() {
        openDb();
        Calendar cToday = Calendar.getInstance();
        int maxEventAge = Integer.parseInt(SharedPref.getConfigKeys(context, Config.KEYNAME_MAX_NO_USER_ID_EVENT_AGE, Config.DEFAULT_MAX_NO_USER_ID_EVENT_AGE));
        cToday.add(Calendar.DATE, -maxEventAge);
        nouserIdcurrentTime = cToday.getTimeInMillis();
        int maxEventLimit = Integer.parseInt(SharedPref.getConfigKeys(context, Config.KEYNAME_MAX_NO_OF_EVENTS, Config.DEFAULT_MAX_NO_OF_EVENTS));
        int limit = maxEventLimit - SharedPref.getCurrentEventsSentCount(context);
        openDb();
        Cursor c = db.query(TABLE_NAME, new String[]{TABLE_COLMNEVENTTIME, TABLE_COLMNEVENTDATA}, TABLE_COLMNEVENTTIME + "<? and " + TABLE_COLUMN_USER + "=?", new String[]{String.valueOf(nouserIdcurrentTime), Config.KEYNAME_USER_ID_UNKNOWN}, null, null, TABLE_COLUMNID + " ASC", Integer.toString(limit));
        ArrayList<Event> eventList = new ArrayList<>();
        String invalidEvents = "";
        try {
            if (!c.moveToFirst()) {
                return new Event[0];
            }
            do {
                String decryptedString = "{}";
                String key = KeytoolHelper.getDecKey();
                decryptedString = (key == null) ? decryptedString : EncryptionHandler.decrypt(key, c.getString(1));

                if (!decryptedString.equals("{}")) {
                    Event event = new Event(decryptedString, c.getString(0));
                    if (!Config.blackListStatus(event.getJSONObject(), Config.blackListString(context))) {
                        eventList.add(event);
                    } else {
                        invalidEvents = (("").equalsIgnoreCase(invalidEvents)) ? c.getString(0) : invalidEvents + "," + c.getString(0);
                    }
                }
            }
            while (c.moveToNext());
            if (!invalidEvents.isEmpty()) {
                deleteEvents(invalidEvents, true);
            }
            Event[] e = new Event[eventList.size()];
            for (int j = 0; j < eventList.size(); j++) {
                e[j] = eventList.get(j);
            }
            return e;
        } catch (Exception exc) {
            Logger.e(TAG_SQLITE_DB, "Database exception : " + exc.getMessage());
            return new Event[0];
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Delete Events based on row IDs
     *
     * @param ids      Row id to delete
     * @param multiple Flag to check whether it delete multiple events or not
     */
    public void deleteEvents(String ids, boolean multiple) {
        int deletedNo = 0;
        try {
            openDb();
            if (multiple) {
                deletedNo = db.delete(TABLE_NAME, TABLE_COLMNEVENTTIME + " IN(" + ids + ")", null);
            } else {
                deletedNo = db.delete(TABLE_NAME, TABLE_COLMNEVENTTIME + "<=?", new String[]{ids});
            }
        } catch (IllegalStateException e) {
            Logger.e(TAG_SQLITE_DB, "Sqlite DB Exception " + e.getMessage(), e);
        } finally {
            if (deletedNo > 0)
                SharedPref.setEventCountInDatabase(context, deletedNo, true);
        }
    }

    /**
     * Delete Events based on time for no_user_id_event
     */
    public void deleteEventsNoUserIdEvents(long currentTime) {
        int deletedNo = 0;
        try {
            openDb();
            deletedNo += db.delete(TABLE_NAME, TABLE_COLMNEVENTTIME + "<? and " + TABLE_COLUMN_USER + "=?", new String[]{String.valueOf(currentTime), Config.KEYNAME_USER_ID_UNKNOWN});
        } catch (IllegalStateException e) {
            Logger.e(TAG_SQLITE_DB, "Sqlite DB Exception " + e.getMessage());
        } finally {
            if (deletedNo > 0)
                SharedPref.setEventCountInDatabase(context, deletedNo, true);
        }
    }

    /***
     * Delete the Data from database which based on settings api, either MAX_EVENT_AGE or NO_USER_ID_EVENT_AGE
     */
    public void deleteOldData() {
        Calendar cToday = Calendar.getInstance();
        String currentDate = cToday.get(Calendar.DAY_OF_MONTH) + "-" + cToday.get(Calendar.MONTH);
        int maxEventAge = Integer.parseInt(SharedPref.getConfigKeys(context, Config.KEYNAME_MAX_EVENT_AGE, Config.DEFAULT_ID_EVENT_AGE));
        cToday.add(Calendar.DATE, -maxEventAge);
        long currentTime = cToday.getTimeInMillis();
        int deletedNos = 0;
        try {
            openDb();
            deletedNos = db.delete(TABLE_NAME, TABLE_COLMNEVENTTIME + "<?", new String[]{String.valueOf(currentTime)});
            cToday = Calendar.getInstance();
            maxEventAge = Integer.parseInt(SharedPref.getConfigKeys(context, Config.KEYNAME_MAX_NO_USER_ID_EVENT_AGE, Config.DEFAULT_MAX_NO_USER_ID_EVENT_AGE));
            cToday.add(Calendar.DATE, -maxEventAge);
            SharedPref.setConfigKeys(context, Config.KEYNAME_DB_LAST_CHECKED_DATE, currentDate);
        } catch (IllegalStateException e) {
            Logger.e(TAG_SQLITE_DB, "Sqlite DB Exception " + e.getMessage(), e);
        } finally {
            if (deletedNos > 0)
                SharedPref.setEventCountInDatabase(context, deletedNos, true);
        }
    }
    /****
     * Checks the Sqlite after new settings received
     */
    public void checkDbAfterSettings() {
        try {
            long currentDbCount = SharedPref.getEventCountInDatabase(context);
            int maxEventLimit = Integer.parseInt(SharedPref.getConfigKeys(context, Config.KEYNAME_MAX_EVENTS_IN_DATABASE, Config.DEFAULT_MAX_NO_EVENTS_IN_DB));
            if (currentDbCount > maxEventLimit) {
                openDb();
                int no = db.delete(TABLE_NAME, TABLE_COLMNEVENTTIME + "<=(select " + TABLE_COLMNEVENTTIME + " from " + TABLE_NAME + " order by " + TABLE_COLUMNID + " desc limit 1 offset " + maxEventLimit + ")", null);
                SharedPref.setEventCountInDatabase(context, no, true);
            }
        } catch (IllegalStateException e) {
            Logger.e(TAG_SQLITE_DB, "Sqlite DB Exception " + e.getMessage(), e);
        } catch (Exception e) {
            Logger.e(TAG_SQLITE_DB, "No Database Exception", e);
        }
    }
    /***
     * last inserted event for testing
     *
     * @return event
     */
    public Event getlastInsertedValue() {
        return getlastInsertedValue(true);
    }

    /***
     * last inserted event for testing
     *
     * @return event
     */
    public Event getlastInsertedValue(@SuppressWarnings("SameParameterValue") boolean isDesc) {
        openDb();
        Cursor c = db.query(TABLE_NAME, new String[]{TABLE_COLMNEVENTTIME, TABLE_COLMNEVENTDATA}, null, null, null, null, TABLE_COLUMNID + (isDesc ? " DESC" : " ASC"), "1");
        ArrayList<Event> eventList = new ArrayList<>();
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            do {
                String decryptedString = "{}";
                String key = KeytoolHelper.getDecKey();
                decryptedString = (key == null) ? decryptedString : EncryptionHandler.decrypt(key, c.getString(1));

                if (!decryptedString.equals("{}")) {
                    return new Event(decryptedString, c.getString(0));

                }
            } while (c.moveToNext());
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /***
     * last inserted events for testing
     *
     * @param count No of events to be return
     * @return list of events
     */
    public ArrayList<Event> getlastInsertedValue(int count) {
        openDb();
        Cursor c = db.query(TABLE_NAME, new String[]{TABLE_COLMNEVENTTIME, TABLE_COLMNEVENTDATA}, null, null, null, null, TABLE_COLUMNID + " DESC", Integer.toString(count));
        ArrayList<Event> eventList = new ArrayList<>();

        try {
            if (!c.moveToFirst()) {
                return null;
            }
            do {
                String decryptedString = "{}";
                String key = KeytoolHelper.getDecKey();
                decryptedString = (key == null) ? decryptedString : EncryptionHandler.decrypt(key, c.getString(1));

                if (!decryptedString.equals("{}")) {
                    Event e = new Event(decryptedString, c.getString(0));
                    eventList.add(e);
                }
            } while (c.moveToNext());
        } catch (Exception e) {
            Logger.e(TAG_SQLITE_DB, "Exception");
        }
            return eventList;
    }

    /***
     * Update event type for Exception event
     */
    public int deleteExceptionEvent() {

        try {
            openDb();
            return db.delete(SqliteDb.TABLE_NAME, Config.QUERY_DELETE_EXCEPTION, null);
        } catch (Exception e) {
            Logger.e(TAG_SQLITE_DB, "Exception in deleteExceptionEvent method "+ e.getMessage(), e);
        }
        return 0;
    }


    /***
     * CLear the db before each ui testing
     */
    public void clearDb(){
        try {
            openDb();
            int noOfEvents =db.delete(TABLE_NAME, null, null);
        }catch (IllegalStateException e) {
            Logger.e(TAG_SQLITE_DB, "Sqlite DB Exception " + e.getMessage(), e);
        } catch (Exception e) {
            Logger.e(TAG_SQLITE_DB, "No Database Exception", e);
        }
    }
}
