package com.clevertap.android.directcall.events;

import com.clevertap.android.directcall.enums.CallStatus;

/**
 * Contains information regarding the system events to be raised.
 */
public final class DCSystemEventInfo {

    /*private String accountId;
    private String cuID;
    private String cleverTapID;*/
    private String callId;
    private String callContext;
    private CallStatus callStatus;
    private CallStatus.DeclineReason declineReason;

    /**
     * DCSystemEventInfo can only be constructed via {@link DCSystemEventInfo.Builder}
     */
    private DCSystemEventInfo(){

    }

    public DCSystemEventInfo(Builder builder) {
        this.callId = builder.callId;
        this.callContext = builder.callContext;
        this.callStatus = builder.callStatus;
        this.declineReason = builder.declineReason;
    }

    public String getCallId() {
        return callId;
    }

    public String getCallContext() {
        return callContext;
    }

    public CallStatus getCallStatus() {
        return callStatus;
    }

    public CallStatus.DeclineReason getDeclineReason() {
        return declineReason;
    }

    public static class Builder {
        //required
        /*private String accountId;
        private String cuID;
        private String cleverTapID;*/
        private String callId;
        private String callContext;

        //optional
        private CallStatus callStatus;
        private CallStatus.DeclineReason declineReason;

        public Builder(String callId, String callContext) {
            this.callId = callId;
            this.callContext = callContext;
        }

        public Builder setCallId(String callId) {
            this.callId = callId;
            return this;
        }

        public Builder setCallContext(String callContext) {
            this.callContext = callContext;
            return this;
        }

        public Builder setCallStatus(CallStatus callStatus) {
            this.callStatus = callStatus;
            return this;
        }

        public Builder setDeclineReason(CallStatus.DeclineReason declineReason) {
            this.declineReason = declineReason;
            return this;
        }

        public DCSystemEventInfo build() {
            return new DCSystemEventInfo(this);
        }

    }
}
