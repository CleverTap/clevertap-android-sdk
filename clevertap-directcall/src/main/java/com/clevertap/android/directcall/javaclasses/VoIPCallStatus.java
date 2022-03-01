package com.clevertap.android.directcall.javaclasses;

public class VoIPCallStatus {

    private static final String _CALL_OVER = "CALL_OVER";
    private static final String _CALL_ANSWERED = "CALL_ANSWERED";
    private static final String _CALL_DECLINED = "CALL_DECLINED";
    private static final String _CALL_MISSED = "CALL_MISSED";
    private static final String _CALLEE_BUSY_ON_ANOTHER_CALL = "CALLEE_BUSY_ON_ANOTHER_CALL";

    public static final VoIPCallStatus CALL_OVER = new VoIPCallStatus(_CALL_OVER);
    public static final VoIPCallStatus CALL_ANSWERED = new VoIPCallStatus(_CALL_ANSWERED);
    public static final VoIPCallStatus CALL_DECLINED = new VoIPCallStatus(_CALL_DECLINED);
    public static final VoIPCallStatus CALL_MISSED = new VoIPCallStatus(_CALL_MISSED);
    public static final VoIPCallStatus CALLEE_BUSY_ON_ANOTHER_CALL = new VoIPCallStatus(_CALLEE_BUSY_ON_ANOTHER_CALL);

    private String status;

    public VoIPCallStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}

