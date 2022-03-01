package com.clevertap.android.directcall.init;

import com.clevertap.android.directcall.models.DirectCallTemplates;
import com.clevertap.android.directcall.models.MissedCallActions;

import org.json.JSONObject;

import java.util.List;

public class DirectCallInitOptions {
    //required
    private JSONObject initJson;

    //optional
    private Boolean enableReadPhoneState = false;
    private List<MissedCallActions> missedCallReceiverActions = null, missedCallInitiatorActions = null;
    private String missedCallReceiverHost = null, missedCallInitiatorHost = null;
    private DirectCallTemplates.PinView pinview;
    private DirectCallTemplates.ScratchCard scratchCard;

    /**
     * InitOptions can only be constructed via {@link DirectCallInitOptions.Builder}
     */
    private DirectCallInitOptions() {

    }

    private DirectCallInitOptions(Builder builder) {
        this.initJson = builder.initJson;
        this.enableReadPhoneState = builder.enableReadPhoneState;
        this.missedCallReceiverActions = builder.missedCallReceiverActions;
        this.missedCallReceiverHost = builder.missedCallReceiverHost;
        this.missedCallInitiatorActions = builder.missedCallInitiatorActions;
        this.missedCallInitiatorHost = builder.missedCallInitiatorHost;
        this.pinview = builder.pinview;
        this.scratchCard = builder.scratchCard;
    }

    public JSONObject getInitJson() {
        return this.initJson;
    }

    Boolean isReadPhoneStateEnabled() {
        return this.enableReadPhoneState;
    }

    List<MissedCallActions> getMissedCallReceiverActions() {
        return this.missedCallReceiverActions;
    }

    String getMissedCallReceiverHost() {
        return this.missedCallReceiverHost;
    }

    List<MissedCallActions> getMissedCallInitiatorActions() {
        return this.missedCallInitiatorActions;
    }

    String getMissedCallInitiatorHost() {
        return this.missedCallInitiatorHost;
    }

    public DirectCallTemplates.PinView getPinViewConfig() {
        return pinview;
    }

    public DirectCallTemplates.ScratchCard getScratchCardConfig() {
        return scratchCard;
    }


    public static class Builder {
        //required
        private JSONObject initJson;

        //optional
        private Boolean enableReadPhoneState = false;
        private List<MissedCallActions> missedCallReceiverActions = null, missedCallInitiatorActions = null;
        private String missedCallReceiverHost = null, missedCallInitiatorHost = null;
        private DirectCallTemplates.PinView pinview;
        private DirectCallTemplates.ScratchCard scratchCard;

        public Builder(JSONObject initJson) {
            this.initJson = initJson;
        }

        public Builder enableReadPhoneState(Boolean enable) {
            this.enableReadPhoneState = enable;
            return this;
        }

        public Builder setMissedCallInitiatorActions(List<MissedCallActions> missedCallInitiatorActions, String missedCallInitiatorHost) {
            this.missedCallInitiatorActions = missedCallInitiatorActions;
            this.missedCallInitiatorHost = missedCallInitiatorHost;
            return this;
        }

        public Builder setMissedCallReceiverActions(List<MissedCallActions> missedCallReceiverActions, String missedCallReceiverHost) {
            this.missedCallReceiverActions = missedCallReceiverActions;
            this.missedCallReceiverHost = missedCallReceiverHost;
            return this;
        }

        public Builder enablePinViewTemplate(DirectCallTemplates.PinView pinView) {
            this.pinview = pinView;
            return this;
        }

        public Builder enableScratchCardTemplate(DirectCallTemplates.ScratchCard scratchCard) {
            this.scratchCard = scratchCard;
            return this;
        }

        public DirectCallInitOptions build() {
            return new DirectCallInitOptions(this);
        }

    }
}