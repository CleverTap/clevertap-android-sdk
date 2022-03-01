package com.clevertap.android.directcall.models;

import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;

public class MissedCallActions {
    private final String actionId;
    private final String actionLabel;

     public MissedCallActions(String actionId, String actionLabel) {
         this.actionId = actionId;
         this.actionLabel = actionLabel;
     }

     public String getActionId() {
         return actionId;
     }

     public String getActionLabel() {
         return actionLabel;
     }


     public JSONObject toJson() {
         JSONObject jo = new JSONObject();
         try {
             jo.put("actionId", actionId);
             jo.put("actionLabel", actionLabel);
         } catch (JSONException e) {
             e.printStackTrace();
         }
         return jo;
     }

     public static MissedCallActions fromJson(JSONObject jsonObject) {
         MissedCallActions missedCallActions = null;
         try {
             missedCallActions =  new MissedCallActions(jsonObject.getString("actionId"), jsonObject.getString("actionLabel"));
         } catch (JSONException e) {
             e.printStackTrace();
         }
         return missedCallActions;
     }

     @NonNull
     @Override
     public String toString() {
         return "MissedCallActions{" +
                 "actionId='" + actionId + '\'' +
                 ", actionLabel='" + actionLabel + '\'' +
                 '}';
     }
 }
