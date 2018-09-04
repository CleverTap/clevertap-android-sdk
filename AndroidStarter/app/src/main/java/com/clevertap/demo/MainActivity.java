package com.clevertap.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Button event, chargedEvent, eventWithProps, profileEvent;
    private CleverTapAPI cleverTapDefaultInstance, cleverTapInstanceTwo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        event = findViewById(R.id.event);
        chargedEvent = findViewById(R.id.charged_event);
        eventWithProps = findViewById(R.id.event_with_props);
        profileEvent = findViewById(R.id.profile_event);
        //Set Debug level for CleverTap
        CleverTapAPI.setDebugLevel(3);

        //Create CleverTap's default instance
        cleverTapDefaultInstance = CleverTapAPI.getDefaultInstance(this);
        if (cleverTapDefaultInstance != null) {
            cleverTapDefaultInstance.enableDeviceNetworkInfoReporting(false);
        }

        //With CleverTap Android SDK v3.2.0 you can create additional instances to send data to multiple CleverTap accounts
        //Create config object for an additional instance
        //While using this app, replace the below Account Id and token with your Account Id and token
        CleverTapInstanceConfig config =  CleverTapInstanceConfig.createInstance(this,"YOUR_ACCOUNT_ID","YOUR_ACCOUNT_TOKEN");

        //Use the config object to create a custom instance
        cleverTapInstanceTwo = CleverTapAPI.instanceWithConfig(this,config);

        //Record an event
        event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleverTapDefaultInstance.pushEvent("EventName");
                //OR
                //cleverTapInstanceTwo.pushEvent("EventName");
            }
        });

        //Record an event with properties
        eventWithProps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, Object> prodViewedAction = new HashMap<>();
                prodViewedAction.put("Product Name", "Casio Chronograph Watch");
                prodViewedAction.put("Category", "Mens Accessories");
                prodViewedAction.put("Price", 59.99);
                prodViewedAction.put("Date", new java.util.Date());

                cleverTapDefaultInstance.pushEvent("Product viewed", prodViewedAction);
                //OR
                //cleverTapInstanceTwo.pushEvent("Product viewed", prodViewedAction);

            }
        });

        //Record a Charged (Transactional) event
        chargedEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, Object> chargeDetails = new HashMap<>();
                chargeDetails.put("Amount", 300);
                chargeDetails.put("Payment Mode", "Credit card");
                chargeDetails.put("Charged ID", 24052013);

                HashMap<String, Object> item1 = new HashMap<>();
                item1.put("Product category", "books");
                item1.put("Book name", "The Millionaire next door");
                item1.put("Quantity", 1);

                HashMap<String, Object> item2 = new HashMap<>();
                item2.put("Product category", "books");
                item2.put("Book name", "Achieving inner zen");
                item2.put("Quantity", 1);

                HashMap<String, Object> item3 = new HashMap<>();
                item3.put("Product category", "books");
                item3.put("Book name", "Chuck it, let's do it");
                item3.put("Quantity", 5);

                ArrayList<HashMap<String, Object>> items = new ArrayList<>();
                items.add(item1);
                items.add(item2);
                items.add(item3);

                cleverTapDefaultInstance.pushChargedEvent(chargeDetails, items);
                //OR
                //cleverTapInstanceTwo.pushChargedEvent(chargeDetails, items);
            }
        });

        //Push a profile to CleverTap
        profileEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Record a profile
                HashMap<String, Object> profileUpdate = new HashMap<>();
                profileUpdate.put("Name", "User Name");    // String
                profileUpdate.put("Email", "User@gmail.com"); // Email address of the user
                profileUpdate.put("Phone", "+14155551234");   // Phone (with the country code, starting with +)
                profileUpdate.put("Gender", "M");             // Can be either M or F
                profileUpdate.put("Employed", "Y");           // Can be either Y or N
                profileUpdate.put("Education", "Graduate");   // Can be either Graduate, College or School
                profileUpdate.put("Married", "Y");            // Can be either Y or N
                profileUpdate.put("DOB", new Date());         // Date of Birth. Set the Date object to the appropriate value first
                profileUpdate.put("Age", 28);                 // Not required if DOB is set
                profileUpdate.put("MSG-email", false);        // Disable email notifications
                profileUpdate.put("MSG-push", true);          // Enable push notifications
                profileUpdate.put("MSG-sms", false);          // Disable SMS notifications
                cleverTapDefaultInstance.pushProfile(profileUpdate);
                //OR
                //cleverTapInstanceTwo.pushProfile(profileUpdate);
            }
        });

    }
}
