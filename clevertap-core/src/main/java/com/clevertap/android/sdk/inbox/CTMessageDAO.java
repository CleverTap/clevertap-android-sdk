package com.clevertap.android.sdk.inbox;

import android.text.TextUtils;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Message Data Access Object class interfacing with Database
 */
@RestrictTo(Scope.LIBRARY)
public class CTMessageDAO {

    private String campaignId;

    private long date;

    private long expires;

    private String id;

    private JSONObject jsonData;

    private boolean read;

    private List<String> tags = new ArrayList<>();

    private String userId;

    private JSONObject wzrkParams;

    public CTMessageDAO() {
    }

    private CTMessageDAO(String id, JSONObject jsonData, boolean read, long date, long expires, String userId,
            List<String> tags, String campaignId, JSONObject wzrkParams) {
        this.id = id;
        this.jsonData = jsonData;
        this.read = read;
        this.date = date;
        this.expires = expires;
        this.userId = userId;
        this.tags = tags;
        this.campaignId = campaignId;
        this.wzrkParams = wzrkParams;
    }

    boolean containsVideoOrAudio() {
        Logger.d("CTMessageDAO:containsVideoOrAudio() called");
        CTInboxMessageContent content = new CTInboxMessage(this.toJSON()).getInboxMessageContents().get(0);
        return (content.mediaIsVideo() || content.mediaIsAudio());
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JSONObject getJsonData() {
        return jsonData;
    }

    public void setJsonData(JSONObject jsonData) {
        this.jsonData = jsonData;
    }

    public String getTags() {
        return TextUtils.join(",", tags);
    }

    public void setTags(String tags) {
        String[] tagsArray = tags.split(",");
        this.tags.addAll(Arrays.asList(tagsArray));

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public JSONObject getWzrkParams() {
        return wzrkParams;
    }

    public void setWzrkParams(JSONObject wzrk_params) {
        this.wzrkParams = wzrk_params;
    }

    public int isRead() {
        if (read) {
            return 1;
        } else {
            return 0;
        }
    }

    public void setRead(int read) {
        this.read = read == 1;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", this.id);
            jsonObject.put("msg", this.jsonData);
            jsonObject.put("isRead", this.read);
            jsonObject.put("date", this.date);
            jsonObject.put("wzrk_ttl", this.expires);
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < this.tags.size(); i++) {
                jsonArray.put(tags.get(i));
            }
            jsonObject.put("tags", jsonArray);
            jsonObject.put("wzrk_id", campaignId);
            jsonObject.put("wzrkParams", wzrkParams);
            return jsonObject;
        } catch (JSONException e) {
            Logger.v("Unable to convert CTMessageDao to JSON - " + e.getLocalizedMessage());
            return jsonObject;
        }
    }

    static CTMessageDAO initWithJSON(JSONObject inboxMessage, String userId) {
        try {
            String id = inboxMessage.has("_id") ? inboxMessage.getString("_id") : null;
            long date = inboxMessage.has("date") ? inboxMessage.getInt("date") : System.currentTimeMillis() / 1000;
            long expires = inboxMessage.has("wzrk_ttl") ? inboxMessage.getInt("wzrk_ttl")
                    : (System.currentTimeMillis() + 24 * 60 * Constants.ONE_MIN_IN_MILLIS) / 1000;
            JSONObject cellObject = inboxMessage.has("msg") ? inboxMessage.getJSONObject("msg") : null;
            List<String> tagsList = new ArrayList<>();
            if (cellObject != null) {//Part of "msg" object
                JSONArray tagsArray = cellObject.has("tags") ? cellObject.getJSONArray("tags") : null;
                if (tagsArray != null) {
                    for (int i = 0; i < tagsArray.length(); i++) {
                        tagsList.add(tagsArray.getString(i));
                    }
                }
            }
            String campaignId = inboxMessage.has("wzrk_id") ? inboxMessage.getString("wzrk_id") : "0_0";
            if (campaignId.equalsIgnoreCase("0_0")) {
                inboxMessage.put("wzrk_id", campaignId);//For test inbox Notification Viewed
            }
            JSONObject wzrkParams = getWzrkFields(inboxMessage);
            return (id == null) ? null
                    : new CTMessageDAO(id, cellObject, false, date, expires, userId, tagsList, campaignId,
                            wzrkParams);
        } catch (JSONException e) {
            Logger.d("Unable to parse Notification inbox message to CTMessageDao - " + e.getLocalizedMessage());
            return null;
        }
    }

    private static JSONObject getWzrkFields(JSONObject root) throws JSONException {
        final JSONObject fields = new JSONObject();
        Iterator<String> iterator = root.keys();

        while (iterator.hasNext()) {
            String keyName = iterator.next();
            if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                fields.put(keyName, root.get(keyName));
            }
        }

        return fields;
    }
}
