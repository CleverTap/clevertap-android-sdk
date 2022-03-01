package com.clevertap.android.directcall.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Cli {
    private String cc;
    private String phone;

    public Cli(String cc, String phone) {
        this.cc = cc;
        this.phone = phone;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public JSONObject toJson() {
        JSONObject jo = new JSONObject();
        try {
            jo.put("cc", cc);
            jo.put("phone", phone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }
}
