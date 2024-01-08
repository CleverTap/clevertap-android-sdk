package com.clevertap.android.sdk.db;

import org.json.JSONArray;

@SuppressWarnings("unused")
public final class QueueCursor {

    private JSONArray data; // the db objects

    private String lastId; // the id of the last object returned from the db, used to remove sent objects

    private Table tableName;

    @Override
    public String toString() {
        return (this.isEmpty()) ? "tableName: " + tableName + " | numItems: 0" :
                "tableName: " + tableName + " | lastId: " + lastId + " | numItems: " + data.length() + " | items: "
                        + data.toString();
    }

    public JSONArray getData() {
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

    Table getTableName() {
        return tableName;
    }

    void setTableName(Table tableName) {
        this.tableName = tableName;
    }

    public boolean isEmpty() {
        return lastId == null || data == null || data.length() <= 0;
    }

    private void resetForTableName(Table tName) {
        tableName = tName;
        data = null;
        lastId = null;
    }
}
