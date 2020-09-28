package com.clevertap.android.sdk;

import org.json.JSONArray;

@SuppressWarnings("unused")
final class QueueCursor {

    private JSONArray data; // the db objects

    private String lastId; // the id of the last object returned from the db, used to remove sent objects

    private DBAdapter.Table tableName;

    @Override
    public String toString() {
        return (this.isEmpty()) ? "tableName: " + tableName + " | numItems: 0" :
                "tableName: " + tableName + " | lastId: " + lastId + " | numItems: " + data.length() + " | items: "
                        + data.toString();
    }

    JSONArray getData() {
        return data;
    }

    void setData(JSONArray data) {
        this.data = data;
    }

    String getLastId() {
        return lastId;
    }

    void setLastId(String lastId) {
        this.lastId = lastId;
    }

    DBAdapter.Table getTableName() {
        return tableName;
    }

    void setTableName(DBAdapter.Table tableName) {
        this.tableName = tableName;
    }

    Boolean isEmpty() {
        return (lastId == null || data == null || data.length() <= 0);
    }

    private void resetForTableName(DBAdapter.Table tName) {
        tableName = tName;
        data = null;
        lastId = null;
    }
}
