package com.clevertap.starter;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.exceptions.CleverTapMetaDataNotFoundException;
import com.clevertap.android.sdk.exceptions.CleverTapPermissionsNotSatisfied;

// import java.util.HashMap;

public class MainActivity extends Activity {

    private CleverTapAPI ct = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            // CleverTap

            CleverTapAPI.setDebugLevel(1); // optional

            ct = CleverTapAPI.getInstance(getApplicationContext());

            ct.enablePersonalization();  // optional

        } catch (CleverTapMetaDataNotFoundException e) {
            // handle appropriately
            e.printStackTrace();

        } catch (CleverTapPermissionsNotSatisfied e) {
            // handle appropriately
            e.printStackTrace();
        }
        assert ct != null;

        // Read key value pairs from an incoming notification
        try {
            Bundle extras = getIntent().getExtras();
            for (String key : extras.keySet()) {
                Log.i("StarterProject", "key = " + key + "; value = " + extras.get(key).toString());

            }
        } catch (Exception e) {
            // Ignore
        }

        // set CleverTap User Profile
        /*
        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("Name", "Jack Montana"); // String
        profileUpdate.put("Identity", "6541182"); // String or number
        profileUpdate.put("Email", "jack@gmail.com"); // Email address of the user
        profileUpdate.put("Phone", 4155551234); // Phone(without the country
        code)
        profileUpdate.put("Gender", "M"); // Can be either M or F
        profileUpdate.put("Employed", "Y"); // Can be either Y or N
        profileUpdate.put("DOB", new Date()); // set the Date object to the appropriate value first
        profileUpdate.put("Age", 28); // Not required if DOB is set
        profileUpdate.put("MSG-email", false); // Disable email notifications
        profileUpdate.put("MSG-push", true); // Enable push notifications
        profileUpdate.put("MSG-sms", false); // Disable SMS notifications
        ct.profile.push(profileUpdate);
        */

        // create CleverTap event for a User Action
        //ct.event.push("Video played");
    }

}
