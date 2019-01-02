package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DBAdapter {

    public enum Table {
        EVENTS("events"),
        PROFILE_EVENTS("profileEvents"),
        USER_PROFILES("userProfiles"),
        PUSH_NOTIFICATIONS("pushNotifications"),
        //PUSH_NOTIFICATION_VIEWED("notificationViewed"),
        UNINSTALL_TS("uninstallTimestamp");

        Table(String name) {
            tableName = name;
        }

        public String getName() {
            return tableName;
        }

        private final String tableName;
    }

    private static final String KEY_DATA = "data";
    private static final String KEY_CREATED_AT = "created_at";
    private static final long DATA_EXPIRATION = 1000 * 60 * 60 * 24 * 5;

    private static final int DB_UPDATE_ERROR = -1;
    private static final int DB_OUT_OF_MEMORY_ERROR = -2;
    @SuppressWarnings("unused")
    public static final int DB_UNDEFINED_CODE = -3;

    private static final String DATABASE_NAME = "clevertap";
    private static final int DATABASE_VERSION = 2;

    private static final String CREATE_EVENTS_TABLE =
            "CREATE TABLE " + Table.EVENTS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_DATA + " STRING NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL);";

    private static final String CREATE_PROFILE_EVENTS_TABLE =
            "CREATE TABLE " + Table.PROFILE_EVENTS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_DATA + " STRING NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL);";

    private static final String CREATE_USER_PROFILES_TABLE =
            "CREATE TABLE " + Table.USER_PROFILES.getName() + " (_id STRING UNIQUE PRIMARY KEY, " +
                    KEY_DATA + " STRING NOT NULL);";

    private static final String EVENTS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.EVENTS.getName() +
                    " (" + KEY_CREATED_AT + ");";

    private static final String PROFILE_EVENTS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.PROFILE_EVENTS.getName() +
                    " (" + KEY_CREATED_AT + ");";

    private static final String CREATE_PUSH_NOTIFICATIONS_TABLE =
            "CREATE TABLE " + Table.PUSH_NOTIFICATIONS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_DATA + " STRING NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL);";

    private static final String CREATE_UNINSTALL_TS_TABLE =
            "CREATE TABLE " + Table.UNINSTALL_TS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL);";

    private static final String UNINSTALL_TS_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.UNINSTALL_TS.getName() +
                    " (" + KEY_CREATED_AT + ");";


    private final DatabaseHelper dbHelper;
    private CleverTapInstanceConfig config;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
            databaseFile = context.getDatabasePath(dbName);
        }

        void deleteDatabase() {
            close();
            //noinspection ResultOfMethodCallIgnored
            databaseFile.delete();
        }

        @SuppressLint("SQLiteString")
        @Override
        public void onCreate(SQLiteDatabase db) {

            Logger.v("Creating CleverTap DB");

            db.execSQL(CREATE_EVENTS_TABLE);
            db.execSQL(CREATE_PROFILE_EVENTS_TABLE);
            db.execSQL(CREATE_USER_PROFILES_TABLE);
            db.execSQL(CREATE_PUSH_NOTIFICATIONS_TABLE);
            //db.execSQL(CREATE_NOTIFICATION_VIEWED_TABLE);
            db.execSQL(CREATE_UNINSTALL_TS_TABLE);

            db.execSQL(EVENTS_TIME_INDEX);
            db.execSQL(PROFILE_EVENTS_TIME_INDEX);
            db.execSQL(UNINSTALL_TS_INDEX);
        }

        @SuppressLint("SQLiteString")
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            Logger.v("Recreating CleverTap DB on upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Table.EVENTS.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.PROFILE_EVENTS.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.USER_PROFILES.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.PUSH_NOTIFICATIONS.getName());
            //db.execSQL("DROP TABLE IF EXISTS " + Table.PUSH_NOTIFICATION_VIEWED.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.UNINSTALL_TS.getName());

            db.execSQL(CREATE_EVENTS_TABLE);
            db.execSQL(CREATE_PROFILE_EVENTS_TABLE);
            db.execSQL(CREATE_USER_PROFILES_TABLE);
            db.execSQL(CREATE_PUSH_NOTIFICATIONS_TABLE);
            //db.execSQL(CREATE_NOTIFICATION_VIEWED_TABLE);
            db.execSQL(CREATE_UNINSTALL_TS_TABLE);

            db.execSQL(EVENTS_TIME_INDEX);
            db.execSQL(PROFILE_EVENTS_TIME_INDEX);
            db.execSQL(UNINSTALL_TS_INDEX);

        }

        boolean belowMemThreshold() {
            //noinspection SimplifiableIfStatement
            if (databaseFile.exists()) {
                return Math.max(databaseFile.getUsableSpace(), DB_LIMIT) >= databaseFile.length();
            }
            return true;
        }

        private final File databaseFile;

        private final int DB_LIMIT = 20 * 1024 * 1024; //20mb
    }

    private static String getDatabaseName(CleverTapInstanceConfig config) {
        return config.isDefaultInstance() ? DATABASE_NAME : DATABASE_NAME + "_" + config.getAccountId();
    }

    DBAdapter(Context context, CleverTapInstanceConfig config){
        this(context, getDatabaseName(config));
        this.config = config;

    }

    private DBAdapter(Context context, String dbName) {
        dbHelper = new DatabaseHelper(context, dbName);
    }

    private Logger getConfigLogger(){
        return this.config.getLogger();
    }

    /**
     * Adds a JSON string representing to the DB.
     *
     * @param obj   the JSON to record
     * @param table the table to insert into
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     */
    public int storeObject(JSONObject obj, Table table) {
        if (!this.belowMemThreshold()) {
            Logger.v("There is not enough space left on the device to store data, data discarded");
            return DB_OUT_OF_MEMORY_ERROR;
        }

        final String tableName = table.getName();

        Cursor cursor = null;
        int count = DB_UPDATE_ERROR;

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();

            final ContentValues cv = new ContentValues();
            cv.put(KEY_DATA, obj.toString());
            cv.put(KEY_CREATED_AT, System.currentTimeMillis());
            db.insert(tableName, null, cv);
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            cursor.moveToFirst();
            count = cursor.getInt(0);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + tableName + " Recreating DB");

            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            dbHelper.deleteDatabase();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            dbHelper.close();
        }
        return count;
    }

    /**
     * Adds a JSON string representing to the DB.
     *
     * @param obj the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     */
    public long storeUserProfile(String id, JSONObject obj) {

        if (id == null) return DB_UPDATE_ERROR;

        if (!this.belowMemThreshold()) {
            getConfigLogger().verbose("There is not enough space left on the device to store data, data discarded");
            return DB_OUT_OF_MEMORY_ERROR;
        }

        final String tableName = Table.USER_PROFILES.getName();

        long ret = DB_UPDATE_ERROR;

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            final ContentValues cv = new ContentValues();
            cv.put(KEY_DATA, obj.toString());
            cv.put("_id", id);
            ret = db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + tableName + " Recreating DB");
            dbHelper.deleteDatabase();
        } finally {
            dbHelper.close();
        }
        return ret;
    }

    /**
     * remove the user profile with id from the db.
     */
    public void removeUserProfile(String id) {

        if (id == null) return;
        final String tableName = Table.USER_PROFILES.getName();
        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(tableName, "_id = ?", new String[]{id});
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error removing user profile from " + tableName + " Recreating DB");
            dbHelper.deleteDatabase();
        } finally {
            dbHelper.close();
        }
    }

    public JSONObject fetchUserProfileById(final String id) {

        if (id == null) return null;

        final String tName = Table.USER_PROFILES.getName();
        JSONObject profile = null;
        Cursor cursor = null;

        try {
            final SQLiteDatabase db = dbHelper.getReadableDatabase();

            cursor = db.rawQuery("SELECT * FROM " + tName + " WHERE _id = ?", new String[]{id});

            if (cursor != null && cursor.moveToFirst()) {
                try {
                    profile = new JSONObject(cursor.getString(cursor.getColumnIndex(KEY_DATA)));
                } catch (final JSONException e) {
                    // Ignore
                }
            }
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Could not fetch records out of database " + tName + ".", e);
        } finally {
            dbHelper.close();
            if (cursor != null) {
                cursor.close();
            }
        }

        return profile;
    }

    /**
     * Removes all events  from table
     *
     * @param table  the table to remove events
     */
    public void removeEvents(Table table) {
        final String tName = table.getName();

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(tName, null, null);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error removing all events from table " + tName + " Recreating DB");
            deleteDB();
        } finally {
            dbHelper.close();
        }
    }

    /**
     * Removes sent events with an _id <= last_id from table
     *
     * @param lastId the last id to delete
     * @param table  the table to remove events
     */
    public void cleanupEventsFromLastId(String lastId, Table table) {
        final String tName = table.getName();

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(tName, "_id <= " + lastId, null);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error removing sent data from table " + tName + " Recreating DB");
            deleteDB();
        } finally {
            dbHelper.close();
        }
    }

    /**
     * Removes stale events.
     *
     * @param table the table to remove events
     */
    public void cleanupStaleEvents(Table table) {
        cleanInternal(table, DATA_EXPIRATION);
    }


    public void cleanUpPushNotifications(){
        cleanInternal(Table.PUSH_NOTIFICATIONS,0);//Expiry time is stored in PUSH_NOTIFICATIONS table
    }

    private void cleanInternal(Table table, long expiration){

        final long time = System.currentTimeMillis() - expiration;
        final String tName = table.getName();

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(tName, KEY_CREATED_AT + " <= " + time, null);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error removing stale event records from " + tName + ". Recreating DB.", e);
            deleteDB();
        } finally {
            dbHelper.close();
        }

    }
    private void deleteDB() {
        dbHelper.deleteDatabase();
    }

    /**
     * Returns a JSONObject keyed with the lastId retrieved and a value of a JSONArray of the retrieved JSONObject events
     *
     * @param table the table to read from
     * @return JSONObject containing the max row ID and a JSONArray of the JSONObject events or null
     */
    public JSONObject fetchEvents(Table table, final int limit) {
        final String tName = table.getName();
        Cursor cursor = null;
        String lastId = null;

        final JSONArray events = new JSONArray();

        try {
            final SQLiteDatabase db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM " + tName +
                    " ORDER BY " + KEY_CREATED_AT + " ASC LIMIT " + limit, null);


            while (cursor.moveToNext()) {
                if (cursor.isLast()) {
                    lastId = cursor.getString(cursor.getColumnIndex("_id"));
                }
                try {
                    final JSONObject j = new JSONObject(cursor.getString(cursor.getColumnIndex(KEY_DATA)));
                    events.put(j);
                } catch (final JSONException e) {
                    // Ignore
                }
            }
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Could not fetch records out of database " + tName + ".", e);
            lastId = null;
        } finally {
            dbHelper.close();
            if (cursor != null) {
                cursor.close();
            }
        }

        if (lastId != null) {
            try {
                final JSONObject ret = new JSONObject();
                ret.put(lastId, events);
                return ret;
            } catch (JSONException e) {
                // ignore
            }
        }

        return null;
    }


    /**
     * Adds a String representing to the DB.
     *
     * @param id the String value of Push Notification Id
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     */
    public void storePushNotificationId(String id, long ttl, String wzrk_id) {

        if (id == null) return ;

        if (!this.belowMemThreshold()) {
            getConfigLogger().verbose("There is not enough space left on the device to store data, data discarded");
            return ;
        }
        final String tableName = Table.PUSH_NOTIFICATIONS.getName();


        if(ttl <= 0) {
           ttl = System.currentTimeMillis() + Constants.DEFAULT_PUSH_TTL;
        }

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            final ContentValues cv = new ContentValues();
            cv.put(KEY_DATA, id);
            cv.put(KEY_CREATED_AT, ttl);
            db.insert(tableName, null, cv);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + tableName + " Recreating DB");
            dbHelper.deleteDatabase();
        } finally {
            dbHelper.close();
        }

    }

    private String fetchPushNotificationId(String id){
        final String tName = Table.PUSH_NOTIFICATIONS.getName();
        Cursor cursor = null;
        String pushId = "";

        try{
            final SQLiteDatabase db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM " + tName +
                    " WHERE " + KEY_DATA + " = ?" , new String[]{id});
            if(cursor!=null && cursor.moveToFirst()){
                pushId = cursor.getString(cursor.getColumnIndex(KEY_DATA));
            }
        }catch (final SQLiteException e) {
            getConfigLogger().verbose("Could not fetch records out of database " + tName + ".", e);
        } finally {
            dbHelper.close();
            if (cursor != null) {
                cursor.close();
            }
        }
        return pushId;
    }

    String[] fetchPushNotificationIds(){
        final String tName = Table.PUSH_NOTIFICATIONS.getName();
        Cursor cursor = null;
        List<String> pushIds = new ArrayList<>();

        try{
            final SQLiteDatabase db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM " + tName, null);
            if(cursor!=null && cursor.moveToFirst()){
                pushIds.add(cursor.getString(cursor.getColumnIndex(KEY_DATA)));
            }
        }catch (final SQLiteException e) {
            getConfigLogger().verbose("Could not fetch records out of database " + tName + ".", e);
        } finally {
            dbHelper.close();
            if (cursor != null) {
                cursor.close();
            }
        }
        return pushIds.toArray(new String[0]);
    }

    boolean doesPushNotificationIdExist(String id){
        return id.equals(fetchPushNotificationId(id));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean belowMemThreshold() {
        return dbHelper.belowMemThreshold();
    }

    /**
     * Adds a String timestamp representing uninstall flag to the DB.
     *
     */
    void storeUninstallTimestamp() {

        if (!this.belowMemThreshold()) {
            getConfigLogger().verbose("There is not enough space left on the device to store data, data discarded");
            return ;
        }
        final String tableName = Table.UNINSTALL_TS.getName();

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            final ContentValues cv = new ContentValues();
            cv.put(KEY_CREATED_AT, System.currentTimeMillis());
            db.insert(tableName, null, cv);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + tableName + " Recreating DB");
            dbHelper.deleteDatabase();
        } finally {
            dbHelper.close();
        }

    }

    long getLastUninstallTimestamp(){
        final String tName = Table.UNINSTALL_TS.getName();
        Cursor cursor = null;
        long timestamp = 0;
        try{
            final SQLiteDatabase db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM " + tName +
                    " ORDER BY " + KEY_CREATED_AT + " DESC LIMIT 1",null);
            if(cursor!=null && cursor.moveToFirst()){
                timestamp = cursor.getLong(cursor.getColumnIndex(KEY_CREATED_AT));
            }
        }catch (final SQLiteException e) {
            getConfigLogger().verbose("Could not fetch records out of database " + tName + ".", e);
        } finally {
            dbHelper.close();
            if (cursor != null) {
                cursor.close();
            }
        }
        return timestamp;
    }
}
