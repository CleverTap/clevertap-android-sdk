package com.clevertap.android.sdk;


import org.json.JSONObject;

public class Event {

    private final String eventName;

    private final int eventType;

    private final JSONObject payload;

    private Event(final Builder builder) {
        eventName = builder.eventName;
        eventType = builder.eventType;
        payload = builder.payload;
    }

    public static class Builder {

        private final String eventName;

        private final int eventType;

        private JSONObject payload;

        public Builder(final String eventName, final int eventType) {
            this.eventName = eventName;
            this.eventType = eventType;
        }

        public Builder payload(JSONObject jsonObject) {
            this.payload = jsonObject;
            return this;
        }

        public Event build() {
            return new Event(this);
        }
    }
}
