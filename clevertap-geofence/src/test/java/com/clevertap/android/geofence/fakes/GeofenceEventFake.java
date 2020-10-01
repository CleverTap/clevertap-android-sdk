package com.clevertap.android.geofence.fakes;

import android.location.Location;
import com.google.android.gms.location.Geofence;
import java.util.ArrayList;
import java.util.List;

public class GeofenceEventFake {

    public static List<Geofence> getDoubleMatchingTriggeredGeofenceList() {
        List<Geofence> geofenceList = new ArrayList<>();
        geofenceList.add(new Geofence.Builder().setRequestId("310001")
                .setTransitionTypes(1)
                .setCircularRegion(19.092962, 72.849717,
                        400)
                .setExpirationDuration(-1)
                .build());
        geofenceList.add(new Geofence.Builder().setRequestId("310002")
                .setTransitionTypes(1)
                .setCircularRegion(19.092962, 72.849717,
                        400)
                .setExpirationDuration(-1)
                .build());

        return geofenceList;
    }

    public static List<Geofence> getNonMatchingTriggeredGeofenceList() {
        List<Geofence> geofenceList = new ArrayList<>();
        geofenceList.add(new Geofence.Builder().setRequestId("312201")
                .setTransitionTypes(1)
                .setCircularRegion(19.092962, 72.849717,
                        400)
                .setExpirationDuration(-1)
                .build());

        return geofenceList;
    }

    public static List<Geofence> getSingleMatchingTriggeredGeofenceList() {
        List<Geofence> geofenceList = new ArrayList<>();
        geofenceList.add(new Geofence.Builder().setRequestId("310001")
                .setTransitionTypes(1)
                .setCircularRegion(19.092962, 72.849717,
                        400)
                .setExpirationDuration(-1)
                .build());

        return geofenceList;
    }

    public static Location getTriggeredLocation() {
        Location location = new Location("");
        location.setLatitude(19.092962);
        location.setLongitude(72.849717);

        return location;
    }
}
