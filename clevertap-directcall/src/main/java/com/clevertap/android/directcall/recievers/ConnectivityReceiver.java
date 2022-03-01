package com.clevertap.android.directcall.recievers;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.clevertap.android.directcall.init.DirectCallAPI;

/**
 * This class keeps a check of network connectivity of the user. whether the user has active internet connection or not.
 */

public class ConnectivityReceiver extends BroadcastReceiver {

    private ConnectivityReceiverListener mConnectivityReceiverListener;

    public ConnectivityReceiver(ConnectivityReceiverListener listener) {
        mConnectivityReceiverListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            boolean isConnected = isConnected(context);
            mConnectivityReceiverListener.onNetworkConnectionChanged(isConnected);
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while receiving the changes in the network state: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public interface ConnectivityReceiverListener {
        void onNetworkConnectionChanged(boolean isConnected);
    }
}