package com.clevertap.android.directcall.enums;

public enum CallStatus {
    CALL_MISSED("missed"),
    CALL_DECLINED("declined"),
    CALL_CANCELLED("cancelled"),
    CALL_OVER("over");

    private final String name;

    CallStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public enum DeclineReason {
        USER_BUSY("user busy"),
        INVALID_CUID("invalid CUID"),
        MICROPHONE_PERMISSION_NOT_GRANTED("microphone permission not granted");

        private final String name;

        DeclineReason(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}


