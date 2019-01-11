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
import java.util.List;

class DBAdapter {

    public enum Table {
        EVENTS("events"),
        PROFILE_EVENTS("profileEvents"),
        USER_PROFILES("userProfiles"),
        INBOX_MESSAGES("inboxMessages"),
        PUSH_NOTIFICATIONS("pushNotifications"),
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


    //Notification Inbox Messages Table fields
    private static final String _ID = "_id";
    private static final String IS_READ = "isRead";
    private static final String EXPIRES = "expires";
    private static final String TAGS = "tags";
    private static final String USER_ID = "messageUser";
    private static final String CAMPAIGN = "campaignId";
    private static final String WZRKPARAMS = "wzrkParams";

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

    private static final String CREATE_INBOX_MESSAGES_TABLE =
            "CREATE TABLE " + Table.INBOX_MESSAGES.getName() + " (_id STRING NOT NULL, " +
                    KEY_DATA + " TEXT NOT NULL, " +
                    WZRKPARAMS + " TEXT NOT NULL, " +
                    CAMPAIGN + " STRING NOT NULL, " +
                    TAGS + " TEXT NOT NULL, " +
                    IS_READ + " INTEGER NOT NULL DEFAULT 0, " +
                    EXPIRES + " INTEGER NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL, " +
                    USER_ID + " STRING NOT NULL);";


    private static final String INBOX_MESSAGES_COMP_ID_USERID_INDEX =
            "CREATE UNIQUE INDEX IF NOT EXISTS userid_id_idx ON " + Table.INBOX_MESSAGES.getName() +
                    " (" + USER_ID +"," + _ID + ");";

    private static final String EVENTS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.EVENTS.getName() +
                    " (" + KEY_CREATED_AT + ");";

    private static final String PROFILE_EVENTS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.PROFILE_EVENTS.getName() +
                    " (" + KEY_CREATED_AT + ");";

    private static final String CREATE_PUSH_NOTIFICATIONS_TABLE =
            "CREATE TABLE " + Table.PUSH_NOTIFICATIONS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_DATA + " STRING NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL," +
                    IS_READ + " INTEGER NOT NULL);";

    private static final String PUSH_NOTIFICATIONS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.PUSH_NOTIFICATIONS.getName() +
                    " (" + KEY_CREATED_AT + ");";

    private static final String CREATE_UNINSTALL_TS_TABLE =
            "CREATE TABLE " + Table.UNINSTALL_TS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL);";

    private static final String UNINSTALL_TS_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.UNINSTALL_TS.getName() +
                    " (" + KEY_CREATED_AT + ");";


    private final DatabaseHelper dbHelper;
    private CleverTapInstanceConfig config;
    private boolean rtlDirtyFlag = true;

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
            db.execSQL(CREATE_INBOX_MESSAGES_TABLE);
            db.execSQL(CREATE_PUSH_NOTIFICATIONS_TABLE);
            db.execSQL(CREATE_UNINSTALL_TS_TABLE);
            db.execSQL(EVENTS_TIME_INDEX);
            db.execSQL(PROFILE_EVENTS_TIME_INDEX);
            db.execSQL(UNINSTALL_TS_INDEX);
            db.execSQL(PUSH_NOTIFICATIONS_TIME_INDEX);
            db.execSQL(INBOX_MESSAGES_COMP_ID_USERID_INDEX);
        }

        @SuppressLint("SQLiteString")
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Logger.v("Upgrading CleverTap DB to version " + newVersion);

            // For DB Version 2, just adding Push Notifications, Uninstall TS and Inbox Messages tables and related indices
            db.execSQL("DROP TABLE IF EXISTS " + Table.PUSH_NOTIFICATIONS.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.UNINSTALL_TS.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.INBOX_MESSAGES.getName());

            db.execSQL(CREATE_PUSH_NOTIFICATIONS_TABLE);
            db.execSQL(CREATE_UNINSTALL_TS_TABLE);
            db.execSQL(CREATE_INBOX_MESSAGES_TABLE);

            db.execSQL(UNINSTALL_TS_INDEX);
            db.execSQL(PUSH_NOTIFICATIONS_TIME_INDEX);
            db.execSQL(INBOX_MESSAGES_COMP_ID_USERID_INDEX);
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
     * Adds a JSON string to the DB.
     *
     * @param obj   the JSON to record
     * @param table the table to insert into
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     */
    synchronized int storeObject(JSONObject obj, Table table) {
        if (!this.belowMemThreshold()) {
            Logger.v("There is not enough space left on the device to store data, data discarded");
            return DB_OUT_OF_MEMORY_ERROR;
        }

        final String tableName = table.getName();

        Cursor cursor = null;
        int count = DB_UPDATE_ERROR;

        //noinspection TryFinallyCanBeTryWithResources
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
    synchronized long storeUserProfile(String id, JSONObject obj) {

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
    synchronized void removeUserProfile(String id) {

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

    synchronized JSONObject fetchUserProfileById(final String id) {

        if (id == null) return null;

        final String tName = Table.USER_PROFILES.getName();
        JSONObject profile = null;
        Cursor cursor = null;

        //noinspection TryFinallyCanBeTryWithResources
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
     * Removes all events from table
     *
     * @param table  the table to remove events
     */
    synchronized void removeEvents(Table table) {
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
    synchronized void cleanupEventsFromLastId(String lastId, Table table) {
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
    synchronized void cleanupStaleEvents(Table table) {
        cleanInternal(table, DATA_EXPIRATION);
    }

    synchronized void cleanUpPushNotifications(){
        //In Push_Notifications, KEY_CREATED_AT is stored as a future epoch, i.e. currentTimeMillis() + ttl,
        //so comparing to the current time for removal is correct
        cleanInternal(Table.PUSH_NOTIFICATIONS,0);
    }

    private void cleanInternal(Table table, long expiration){

        final long time = (System.currentTimeMillis() - expiration)/1000;
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
    synchronized JSONObject fetchEvents(Table table, final int limit) {
        final String tName = table.getName();
        Cursor cursor = null;
        String lastId = null;

        final JSONArray events = new JSONArray();

        //noinspection TryFinallyCanBeTryWithResources
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

    synchronized void storePushNotificationId(String id, long ttl) {

        if (id == null) return;

        if (!this.belowMemThreshold()) {
            getConfigLogger().verbose("There is not enough space left on the device to store data, data discarded");
            return;
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
            cv.put(IS_READ,0);
            db.insert(tableName, null, cv);
            rtlDirtyFlag = true;
            Logger.v("Stored PN - "+ id + " with TTL - "+ ttl);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + tableName + " Recreating DB");
            dbHelper.deleteDatabase();
        } finally {
            dbHelper.close();
        }
    }

    synchronized private String fetchPushNotificationId(String id){
        final String tName = Table.PUSH_NOTIFICATIONS.getName();
        Cursor cursor = null;
        String pushId = "";

        //noinspection TryFinallyCanBeTryWithResources
        try{
            final SQLiteDatabase db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM " + tName +
                    " WHERE " + KEY_DATA + " = ?" , new String[]{id});
            if(cursor!=null && cursor.moveToFirst()){
                pushId = cursor.getString(cursor.getColumnIndex(KEY_DATA));
            }
            Logger.v("Fetching PID for check - " + pushId);
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

    synchronized String[] fetchPushNotificationIds(){
        if(!rtlDirtyFlag) return new String[0];

        final String tName = Table.PUSH_NOTIFICATIONS.getName();
        Cursor cursor = null;
        List<String> pushIds = new ArrayList<>();

        //noinspection TryFinallyCanBeTryWithResources
        try{
            final SQLiteDatabase db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM " + tName + " WHERE " + IS_READ + " = 0", null);
            if(cursor!=null){
                while(cursor.moveToNext()) {
                    Logger.v("Fetching PID - " + cursor.getString(cursor.getColumnIndex(KEY_DATA)));
                    pushIds.add(cursor.getString(cursor.getColumnIndex(KEY_DATA)));
                }
                cursor.close();
            }
        }catch (final SQLiteException e) {
            getConfigLogger().verbose("Could not fetch records out of database " + tName + ".", e);
        } finally {
            dbHelper.close();
            if (cursor != null) {
                cursor.close();
            }
        }
        return pushIds.toArray(new String[pushIds.size()]);
    }

    synchronized void updatePushNotificationIds(String[] ids){
        if(ids.length == 0){
            return;
        }

        if (!this.belowMemThreshold()) {
            Logger.v("There is not enough space left on the device to store data, data discarded");
            return;
        }
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            final ContentValues cv = new ContentValues();
            cv.put(IS_READ, 1);
            StringBuilder questionMarksBuilder = new StringBuilder();
            questionMarksBuilder.append("?");
            for(int i=0; i< ids.length-1; i++){
                questionMarksBuilder.append(", ?");
            }
            db.update(Table.PUSH_NOTIFICATIONS.getName(), cv,  KEY_DATA+" IN ( " + questionMarksBuilder.toString() + " )",ids);
            rtlDirtyFlag = false;
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + Table.PUSH_NOTIFICATIONS.getName() + " Recreating DB");
            dbHelper.deleteDatabase();
        } finally {
            dbHelper.close();
        }
    }

    synchronized boolean doesPushNotificationIdExist(String id){
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
    synchronized void storeUninstallTimestamp() {

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

    synchronized long getLastUninstallTimestamp(){
        final String tName = Table.UNINSTALL_TS.getName();
        Cursor cursor = null;
        long timestamp = 0;
        //noinspection TryFinallyCanBeTryWithResources
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

    /**
     * Stores a list of inbox messages
     * @param inboxMessages ArrayList of type {@link CTMessageDAO}
     *
     */
    synchronized void upsertMessages(ArrayList<CTMessageDAO> inboxMessages){
        if (!this.belowMemThreshold()) {
            Logger.v("There is not enough space left on the device to store data, data discarded");
            return;
        }
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            for(CTMessageDAO messageDAO : inboxMessages) {
                final ContentValues cv = new ContentValues();
                cv.put(_ID, messageDAO.getId());
                cv.put(KEY_DATA, messageDAO.getJsonData().toString());
                cv.put(WZRKPARAMS, messageDAO.getWzrkParams().toString());
                cv.put(CAMPAIGN,messageDAO.getCampaignId());
                cv.put(TAGS, messageDAO.getTags());
                cv.put(IS_READ, messageDAO.isRead());
                cv.put(EXPIRES, messageDAO.getExpires());
                cv.put(KEY_CREATED_AT,messageDAO.getDate());
                cv.put(USER_ID,messageDAO.getUserId());
                db.insertWithOnConflict(Table.INBOX_MESSAGES.getName(),null,cv,SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + Table.INBOX_MESSAGES.getName());
        } finally {
            dbHelper.close();
        }
    }


    /**
     * Deletes the inbox message for given messageId
     * @param messageId String messageId
     * @return boolean value based on success of operation
     */
    synchronized boolean deleteMessageForId(String messageId, String userId){
        if(messageId == null || userId == null) return false;

        final String tName = Table.INBOX_MESSAGES.getName();

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(tName, _ID + " = ? AND " + USER_ID + " = ?", new String[]{messageId,userId});
            return true;
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error removing stale records from " + tName, e);
            return false;
        } finally {
            dbHelper.close();
        }
    }

    /**
     * Marks inbox message as read for given messageId
     * @param messageId String messageId
     * @return boolean value depending on success of operation
     */
    synchronized boolean markReadMessageForId(String messageId, String userId){
        if(messageId == null || userId == null) return false;

        final String tName = Table.INBOX_MESSAGES.getName();
        try{
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(IS_READ,1);
            db.update(Table.INBOX_MESSAGES.getName(), cv,_ID + " = ? AND " + USER_ID + " = ?",new String[]{messageId,userId});
            return true;
        }catch (final SQLiteException e){
            getConfigLogger().verbose("Error removing stale records from " + tName, e);
            return false;
        } finally {
            dbHelper.close();
        }
    }

    /**
     * Retrieves list of inbox messages based on given userId
     * @param userId String userid
     * @return ArrayList of {@link CTMessageDAO}
     */
    synchronized ArrayList<CTMessageDAO> getMessages(String userId){
        final String tName = Table.INBOX_MESSAGES.getName();
        Cursor cursor;
        ArrayList<CTMessageDAO> messageDAOArrayList = new ArrayList<>();
        try{
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            cursor= db.rawQuery("SELECT * FROM "+tName+" WHERE " + USER_ID + " = ? ORDER BY " + KEY_CREATED_AT+ " DESC", new String[]{userId});
            if(cursor != null) {
                while(cursor.moveToNext()){
                    CTMessageDAO ctMessageDAO = new CTMessageDAO();
                    ctMessageDAO.setId(cursor.getString(cursor.getColumnIndex(_ID)));
                    ctMessageDAO.setJsonData(new JSONObject(cursor.getString(cursor.getColumnIndex(KEY_DATA))));
                    ctMessageDAO.setWzrkParams(new JSONObject(cursor.getString(cursor.getColumnIndex(WZRKPARAMS))));
                    ctMessageDAO.setDate(cursor.getLong(cursor.getColumnIndex(KEY_CREATED_AT)));
                    ctMessageDAO.setExpires(cursor.getLong(cursor.getColumnIndex(EXPIRES)));
                    ctMessageDAO.setRead(cursor.getInt(cursor.getColumnIndex(IS_READ)));
                    ctMessageDAO.setUserId(cursor.getString(cursor.getColumnIndex(USER_ID)));
                    ctMessageDAO.setTags(cursor.getString(cursor.getColumnIndex(TAGS)));
                    ctMessageDAO.setCampaignId(cursor.getString(cursor.getColumnIndex(CAMPAIGN)));
                    messageDAOArrayList.add(ctMessageDAO);
                }
                cursor.close();
            }
            return messageDAOArrayList;
        }catch (final SQLiteException e){
            getConfigLogger().verbose("Error retrieving records from " + tName, e);
            return null;
        } catch (JSONException e) {
            getConfigLogger().verbose("Error retrieving records from " + tName, e.getMessage());
            return null;
        } finally {
            dbHelper.close();
        }
    }
}
