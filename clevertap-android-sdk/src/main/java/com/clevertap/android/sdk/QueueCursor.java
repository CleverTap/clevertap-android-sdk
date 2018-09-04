package com.clevertap.android.sdk;

import org.json.JSONArray;

final class QueueCursor {

    private JSONArray data; // the db objects
    private String lastId; // the id of the last object returned from the db, used to remove sent objects
    private DBAdapter.Table tableName;

    JSONArray getData() {
        return data;
    }

    Boolean isEmpty() {
        return (lastId == null || data == null || data.length() <= 0);
    }

    @SuppressWarnings("unused")
    private void resetForTableName(DBAdapter.Table tName) {
        tableName = tName;
        data = null;
        lastId = null;
    }

    @Override
    public String toString() {
        return (this.isEmpty()) ? "tableName: " + tableName + " | numItems: 0" :
                "tableName: " + tableName + " | lastId: " + lastId + " | numItems: " + data.length() + " | items: " + data.toString();
    }

    public void setData(JSONArray data) {
        this.data = data;
    }

    public String getLastId() {
        return lastId;
    }

    public void setLastId(String lastId) {
        this.lastId = lastId;
    }

    public DBAdapter.Table getTableName() {
        return tableName;
    }

    public void setTableName(DBAdapter.Table tableName) {
        this.tableName = tableName;
    }
}
