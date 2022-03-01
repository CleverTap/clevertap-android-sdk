package com.clevertap.android.directcall.models;

public class MissedCallNotificationOpenResult {
    public MissedCallNotificationAction action;
    public CallDetails callDetails;

    public static class CallDetails {
        public String callerCuid;
        public String calleeCuid;
        public String callContext;
    }

    public static final class MissedCallNotificationAction {
        // The type of the notification action
        //public ActionType actionType;

        public String actionID;

        public String actionLabel;
    }
}


