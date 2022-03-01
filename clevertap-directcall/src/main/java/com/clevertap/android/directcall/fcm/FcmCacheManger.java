package com.clevertap.android.directcall.fcm;

import com.clevertap.android.directcall.enums.CallStatus;

public class FcmCacheManger {
    private static final FcmCacheManger ourInstance = new FcmCacheManger();

    public static FcmCacheManger getInstance() {
        return ourInstance;
    }

    private FcmCacheManger() {
    }

    private Boolean isAnswered = false;
    private Boolean isRejected = false;  //normal decline from UI or due to invalid_cuid/microphone-permission/userBusy
    private CallStatus.DeclineReason rejectedReason = null;  //normal decline from UI or due to invalid_cuid/microphone-permission/userBusy
    private Integer rejectedReasonCode = null;
    private Boolean shouldMarkNotificationStatusDelivered = false;

    public Boolean isAnswered() {
        return isAnswered;
    }

    public void setAnswered(Boolean answered) {
        isAnswered = answered;
    }

    public Boolean isRejected() {
        return isRejected;
    }

    public void setRejected(Boolean rejected) {
        isRejected = rejected;
    }

    public CallStatus.DeclineReason getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(CallStatus.DeclineReason rejectedReason) {
        this.rejectedReason = rejectedReason;
    }

    public Integer getRejectedReasonCode() {
        return rejectedReasonCode;
    }

    public void setRejectedReasonCode(Integer rejectedReasonCode) {
        this.rejectedReasonCode = rejectedReasonCode;
    }

    public Boolean getShouldMarkNotificationStatusDelivered() {
        return shouldMarkNotificationStatusDelivered;
    }

    public void setShouldMarkNotificationStatusDelivered(Boolean shouldMarkNotificationStatusDelivered) {
        this.shouldMarkNotificationStatusDelivered = shouldMarkNotificationStatusDelivered;
    }

    //reset the cache every time only when new incoming call is received via FCM
    public void resetFcmCache(){
        this.isAnswered = false;
        this.isRejected = false;
        this.shouldMarkNotificationStatusDelivered = false;
        this.rejectedReason = null;
        this.rejectedReasonCode = null;
    }
}


