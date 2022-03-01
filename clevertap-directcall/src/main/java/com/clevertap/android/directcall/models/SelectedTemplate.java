package com.clevertap.android.directcall.models;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class SelectedTemplate {

    private String name;
    private String type;
    private String url;

    public SelectedTemplate(String name, String type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JSONObject toJson() {
        JSONObject jo = new JSONObject();
        try {
            jo.put("name", name);
            jo.put("type", type);
            jo.put("url", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    public static SelectedTemplate fromJson(JSONObject jsonObject) {
        SelectedTemplate selectedTemplate = null;
        try {
            selectedTemplate =  new SelectedTemplate(
                    jsonObject.getString("name"),
                    jsonObject.getString("type"),
                    jsonObject.getString("url"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return selectedTemplate;
    }

    @NonNull
    @Override
    public String toString() {
        return "SelectedTemplate{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}