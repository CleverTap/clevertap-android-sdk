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

public class DBAdapter {

    public enum Table {
        EVENTS("events"),
        PROFILE_EVENTS("profileEvents"),
        USER_PROFILES("userProfiles"),
        INBOX_USER("inboxUser"),
        INBOX_MESSAGES("inboxMessages");

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

    //Notification Inbox User Table fields
    private static final String ACCOUNT_ID = "accountId";
    private static final String GUID = "guid";
    private static final String USER_ID = "userId";

    //Notification Inbox Messages Table fields
    private static final String ID = "id";
    private static final String IS_READ = "isRead";
    private static final String EXPIRES = "expires";
    private static final String TAGS = "tags";
    private static final String MESSAGE_USER = "messageUser";

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

    private static final String CREATE_INBOX_USER_TABLE =
            "CREATE TABLE " + Table.INBOX_USER.getName() + " ("+ USER_ID + " TEXT PRIMARY KEY," +
                    ACCOUNT_ID + " TEXT NOT NULL, " +
                    GUID + " TEXT NOT NULL);";

    private static final String CREATE_INBOX_MESSAGES_TABLE =
            "CREATE TABLE " + Table.INBOX_MESSAGES.getName() + " (" + ID + " TEXT NOT NULL," +
                    KEY_DATA + " TEXT NOT NULL, " +
                    TAGS + " TEXT NOT NULL, " +
                    IS_READ + " INTEGER NOT NULL DEFAULT 0, " +
                    EXPIRES + " INTEGER NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL, " +
                    MESSAGE_USER + " TEXT NOT NULL, " +
                    " FOREIGN KEY ("+MESSAGE_USER+") REFERENCES "+Table.INBOX_USER.getName()+"("+USER_ID+"));";

    private static final String EVENTS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.EVENTS.getName() +
                    " (" + KEY_CREATED_AT + ");";

    private static final String PROFILE_EVENTS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.PROFILE_EVENTS.getName() +
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
            db.execSQL(CREATE_INBOX_USER_TABLE);
            Logger.v(CREATE_INBOX_USER_TABLE);
            db.execSQL(CREATE_INBOX_MESSAGES_TABLE);

            db.execSQL(EVENTS_TIME_INDEX);
            db.execSQL(PROFILE_EVENTS_TIME_INDEX);
        }

        @SuppressLint("SQLiteString")
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            Logger.v("Recreating CleverTap DB on upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Table.EVENTS.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.PROFILE_EVENTS.getName());
            db.execSQL("DROP TABLE IF EXISTS " + Table.USER_PROFILES.getName());
            db.execSQL(CREATE_EVENTS_TABLE);
            db.execSQL(CREATE_PROFILE_EVENTS_TABLE);
            db.execSQL(CREATE_USER_PROFILES_TABLE);
            db.execSQL(CREATE_INBOX_USER_TABLE);
            db.execSQL(CREATE_INBOX_MESSAGES_TABLE);

            db.execSQL(EVENTS_TIME_INDEX);
            db.execSQL(PROFILE_EVENTS_TIME_INDEX);
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

        long DATA_EXPIRATION = 1000 * 60 * 60 * 24 * 5;
        final long time = System.currentTimeMillis() - DATA_EXPIRATION;
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean belowMemThreshold() {
        return dbHelper.belowMemThreshold();
    }

    //Notification Inbox CRUD methods
    public boolean createUserTable(){
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + Table.USER_PROFILES.getName());
            db.execSQL(CREATE_INBOX_USER_TABLE);
            return true;
        }catch (Throwable t){
            return false;
        }
    }

    /**
     * fetches or creates a user with given parameters
     * @param userId String userId
     * @param accountId String accountId
     * @param guid String guid
     * @return Object of type {@link CTUserDAO}
     */
    CTUserDAO fetchOrCreateUser(String userId, String accountId, String guid){
        if (userId == null) return null;

        final String tName = Table.INBOX_USER.getName();
        CTUserDAO userDAO = null;
        Cursor cursor = null;
        int count = 0;

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();

            cursor = db.rawQuery("SELECT * FROM " + tName + " WHERE "+USER_ID+" = ?", new String[]{userId});

            if (cursor != null && cursor.moveToFirst()) {
                userDAO = new CTUserDAO();
                userDAO.setUserId(userId);
                userDAO.setGuid(cursor.getString(cursor.getColumnIndex(GUID)));
                userDAO.setAccountId(cursor.getString(cursor.getColumnIndex(ACCOUNT_ID)));
            }

            if(userDAO == null){
                try {
                    final ContentValues cv = new ContentValues();
                    cv.put(USER_ID,userId);
                    cv.put(ACCOUNT_ID,accountId);
                    cv.put(GUID,guid);
                    db.insert(Table.INBOX_USER.getName(), null, cv);
                } catch (final SQLiteException e) {
                    getConfigLogger().verbose("Error adding data to table " + Table.INBOX_USER.getName() + " Recreating DB");
                    dbHelper.deleteDatabase();
                } finally {
                    dbHelper.close();
                }
                userDAO = new CTUserDAO();
                userDAO.setUserId(userId);
                userDAO.setAccountId(accountId);
                userDAO.setGuid(guid);
            }
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Could not fetch records out of database " + tName + ".", e);
        } finally {
            dbHelper.close();
            if (cursor != null) {
                cursor.close();
            }
        }

        return userDAO;
    }

    /**
     * Stores a list of inbox messages
     * @param inboxMessages ArrayList of type {@link CTMessageDAO}
     * @return int
     */
    int storeMessagesForUser(ArrayList<CTMessageDAO> inboxMessages){
        if (!this.belowMemThreshold()) {
            Logger.v("There is not enough space left on the device to store data, data discarded");
            return DB_OUT_OF_MEMORY_ERROR;
        }

        Cursor cursor = null;
        int count = DB_UPDATE_ERROR;

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            for(CTMessageDAO messageDAO : inboxMessages) {
                final ContentValues cv = new ContentValues();
                cv.put(ID, messageDAO.getId());
                cv.put(KEY_DATA, messageDAO.getJsonData().toString());
                cv.put(TAGS, messageDAO.getTags());
                cv.put(IS_READ, messageDAO.isRead());
                cv.put(EXPIRES, messageDAO.getExpires());
                cv.put(KEY_CREATED_AT,messageDAO.getDate());
                cv.put(MESSAGE_USER,messageDAO.getUserId());
                db.insert(Table.INBOX_MESSAGES.getName(), null, cv);
            }
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + Table.INBOX_MESSAGES.getName(), null);
            cursor.moveToFirst();
            count = cursor.getInt(0);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + Table.INBOX_MESSAGES.getName() + " Recreating DB");

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
     * Updates a list on inbox messages
     * @param inboxMessages ArrayList of {@link CTMessageDAO}
     * @return int
     */
    int updateMessagesForUser(ArrayList<CTMessageDAO> inboxMessages){
        if (!this.belowMemThreshold()) {
            Logger.v("There is not enough space left on the device to store data, data discarded");
            return DB_OUT_OF_MEMORY_ERROR;
        }

        Cursor cursor = null;
        int count = DB_UPDATE_ERROR;

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            for(CTMessageDAO messageDAO : inboxMessages) {
                final ContentValues cv = new ContentValues();
                cv.put(KEY_DATA, messageDAO.getJsonData().toString());
                cv.put(IS_READ, messageDAO.isRead());
                cv.put(TAGS,messageDAO.getTags());
                cv.put(EXPIRES, messageDAO.getExpires());
                cv.put(KEY_CREATED_AT,messageDAO.getDate());
                cv.put(MESSAGE_USER,messageDAO.getUserId());
                db.update(Table.INBOX_MESSAGES.getName(), cv,ID + " = ?",new String[]{messageDAO.getId()});
            }
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + Table.INBOX_MESSAGES.getName(), null);
            cursor.moveToFirst();
            count = cursor.getInt(0);
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error adding data to table " + Table.INBOX_MESSAGES.getName() + " Recreating DB");

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
     * Returns inbox message for given messageId
     * @param messageId String messageId
     * @return CTMessageDAO object
     */
    CTMessageDAO getMessageForId(String messageId){
        if (messageId == null) return null;

        final String tName = Table.INBOX_MESSAGES.getName();
        CTMessageDAO messageDAO = new CTMessageDAO();
        Cursor cursor = null;

        try {
            final SQLiteDatabase db = dbHelper.getReadableDatabase();

            cursor = db.rawQuery("SELECT * FROM " + tName + " WHERE id = ?", new String[]{messageId});

            if (cursor != null && cursor.moveToFirst()) {
                try {
                    messageDAO.setId(messageId);
                    messageDAO.setDate(cursor.getInt(cursor.getColumnIndex(KEY_CREATED_AT)));
                    messageDAO.setExpires(cursor.getInt(cursor.getColumnIndex(EXPIRES)));
                    messageDAO.setJsonData(new JSONObject(cursor.getString(cursor.getColumnIndex(KEY_DATA))));
                    messageDAO.setRead(cursor.getInt(cursor.getColumnIndex(IS_READ)));
                    messageDAO.setUserId(cursor.getString(cursor.getColumnIndex(MESSAGE_USER)));
                    messageDAO.setTags(cursor.getString(cursor.getColumnIndex(TAGS)));
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

        return messageDAO;
    }

    /**
     * Deletes the inbox message for given messageId
     * @param messageId String messageId
     * @return boolean value based on success of operation
     */
    boolean deleteMessageForId(String messageId){
        if(messageId == null) return false;

        final String tName = Table.INBOX_MESSAGES.getName();

        try {
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(tName, ID + " = " + messageId, null);
            return true;
        } catch (final SQLiteException e) {
            getConfigLogger().verbose("Error removing stale records from " + tName + ". Recreating DB.", e);
            deleteDB();
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
    boolean markReadMessageForId(String messageId){
        if(messageId == null) return false;

        final String tName = Table.INBOX_MESSAGES.getName();
        try{
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(IS_READ,1);
            db.update(tName,cv,ID + " = " + messageId,null);
            return true;
        }catch (final SQLiteException e){
            getConfigLogger().verbose("Error removing stale records from " + tName + ". Recreating DB.", e);
            deleteDB();
            return false;
        } finally {
            dbHelper.close();
        }
    }

    int getUnreadCount(){
        final String tName = Table.INBOX_MESSAGES.getName();
        Cursor cursor = null;
        int count = -1;
        try{
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            cursor= db.rawQuery("SELECT COUNT(*) FROM "+tName+" WHERE "+IS_READ+" = '" + 0 + "' ", null);
            if(cursor!=null) {
                cursor.moveToFirst();
                count = cursor.getInt(0);
                cursor.close();
            }
            return count;
        }catch (final SQLiteException e){
            getConfigLogger().verbose("Error counting records from " + tName + ". Recreating DB.", e);
            deleteDB();
            return count;
        }finally {
            dbHelper.close();
        }
    }

    /**
     * Retrieves list of inbox messages based on given userId
     * @param userId String userid
     * @return ArrayList of {@link CTMessageDAO}
     */
    ArrayList<CTMessageDAO> getMessages(String userId){
        final String tName = Table.INBOX_MESSAGES.getName();
        Cursor cursor = null;
        ArrayList<CTMessageDAO> messageDAOArrayList = new ArrayList<>();
        try{
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            cursor= db.rawQuery("SELECT * FROM "+tName+" WHERE " + MESSAGE_USER+ " = ? ", new String[]{userId});
            if(cursor != null) {
                while(cursor.moveToNext()){
                    CTMessageDAO ctMessageDAO = new CTMessageDAO();
                    ctMessageDAO.setId(cursor.getString(cursor.getColumnIndex(ID)));
                    ctMessageDAO.setJsonData(new JSONObject(cursor.getString(cursor.getColumnIndex(KEY_DATA))));
                    ctMessageDAO.setDate(cursor.getInt(cursor.getColumnIndex(KEY_CREATED_AT)));
                    ctMessageDAO.setExpires(cursor.getInt(cursor.getColumnIndex(EXPIRES)));
                    ctMessageDAO.setRead(cursor.getInt(cursor.getColumnIndex(IS_READ)));
                    ctMessageDAO.setUserId(cursor.getString(cursor.getColumnIndex(MESSAGE_USER)));
                    ctMessageDAO.setTags(cursor.getString(cursor.getColumnIndex(TAGS)));
                    messageDAOArrayList.add(ctMessageDAO);
                }
                cursor.close();
            }
            return messageDAOArrayList;
        }catch (final SQLiteException e){
            getConfigLogger().verbose("Error retrieving records from " + tName + ". Recreating DB.", e);
            deleteDB();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            getConfigLogger().verbose("Error retrieving records from " + tName + ". Recreating DB.", e);
            deleteDB();
            return null;
        } finally {
            dbHelper.close();
        }
    }

    /**
     * Retrieves list of unread inbox messages based on given userId
     * @param userId String userId
     * @return ArrayList of {@link CTMessageDAO}
     */
    ArrayList<CTMessageDAO> getUnreadMessages(String userId){
        final String tName = Table.INBOX_MESSAGES.getName();
        Cursor cursor = null;
        ArrayList<CTMessageDAO> messageDAOArrayList = new ArrayList<>();
        try{
            final SQLiteDatabase db = dbHelper.getWritableDatabase();
            cursor = db.rawQuery("SELECT * FROM "+tName+" WHERE "+IS_READ+" = ? AND " + MESSAGE_USER+ " = ? ", new String[]{"0",userId});
            if(cursor != null) {
                while(cursor.moveToNext()){
                    CTMessageDAO ctMessageDAO = new CTMessageDAO();
                    ctMessageDAO.setId(cursor.getString(cursor.getColumnIndex(ID)));
                    ctMessageDAO.setJsonData(new JSONObject(cursor.getString(cursor.getColumnIndex(KEY_DATA))));
                    ctMessageDAO.setDate(cursor.getInt(cursor.getColumnIndex(KEY_CREATED_AT)));
                    ctMessageDAO.setExpires(cursor.getInt(cursor.getColumnIndex(EXPIRES)));
                    ctMessageDAO.setRead(cursor.getInt(cursor.getColumnIndex(IS_READ)));
                    ctMessageDAO.setUserId(cursor.getString(cursor.getColumnIndex(MESSAGE_USER)));
                    ctMessageDAO.setTags(cursor.getString(cursor.getColumnIndex(TAGS)));
                    messageDAOArrayList.add(ctMessageDAO);
                }
                cursor.close();
            }
            return messageDAOArrayList;
        }catch (final SQLiteException e){
            getConfigLogger().verbose("Error retrieving records from " + tName + ". Recreating DB.", e);
            deleteDB();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            getConfigLogger().verbose("Error retrieving records from " + tName + ". Recreating DB.", e);
            deleteDB();
            return null;
        } finally {
            dbHelper.close();
        }
    }
}
