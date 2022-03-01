package com.clevertap.android.directcall.models;

import com.clevertap.android.directcall.interfaces.OutgoingCallResponse;

import org.json.JSONObject;

public class PushAPF {
    String calleeCC = "";
    String calleePhone = "";
    String cuid = "";
    String context = "";
    JSONObject callOptions = new JSONObject();
    OutgoingCallResponse outgoingCallResponse = null;

    public PushAPF(String calleeCC, String calleePhone, String cuid, String context, JSONObject callOptions, OutgoingCallResponse outgoingCallResponse) {
        this.calleeCC = calleeCC;
        this.calleePhone = calleePhone;
        this.cuid = cuid;
        this.context = context;
        this.callOptions = callOptions;
        this.outgoingCallResponse = outgoingCallResponse;
    }

    public String getCalleeCC() {
        return calleeCC;
    }

    public String getCalleePhone() {
        return calleePhone;
    }

    public String getCuid() {
        return cuid;
    }

    public String getContext() {
        return context;
    }

    public JSONObject getCallOptions() {
        return callOptions;
    }

    public OutgoingCallResponse getOutgoingCallResponse() {
        return outgoingCallResponse;
    }
}
