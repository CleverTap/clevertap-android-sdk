package com.clevertap.android.directcall.recievers;

import static com.clevertap.android.directcall.Constants.ACTION_OUTGOING_HANGUP;
import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_INITIATOR_HOST;
import static com.clevertap.android.directcall.Constants.KEY_MISSED_CALL_RECEIVER_HOST;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.interfaces.CallNotificationAction;
import com.clevertap.android.directcall.interfaces.OnMissedCallHostSetReceiver;
import com.clevertap.android.directcall.javaclasses.DataStore;
import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.models.MissedCallNotificationOpenResult;
import com.clevertap.android.directcall.models.MissedCallActions;
import com.clevertap.android.directcall.ui.DirectCallingActivity;
import com.clevertap.android.directcall.ui.DirectIncomingCallFragment;
import com.clevertap.android.directcall.utils.NotificationHandler;
import com.clevertap.android.directcall.utils.Utils;

import org.json.JSONObject;

public class CallNotificationActionReceiver extends BroadcastReceiver {

    private static CallNotificationAction callNotificationActionListener;
    public static Boolean isAnswerClickEnabled = false;

    public static void setCallNotificationActionListener(CallNotificationAction actionListener) {
        callNotificationActionListener = actionListener;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            Intent notificationTrayCloseIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(notificationTrayCloseIntent);
        }

        if (intent.hasExtra("actionType") && intent.getStringExtra("actionType") != null) {
            String actionType = intent.getStringExtra("actionType");

            switch (actionType) {
                case ACTION_OUTGOING_HANGUP:
                case "Hangup_Ongoing":
                    if (callNotificationActionListener != null)
                        callNotificationActionListener.onActionClick(actionType);
                    break;
                case "Decline":
                    //It will make prevent to revise the missed call status to decline by tapping on delayed IncomingcallnotificationService
                    if (!DataStore.getInstance().isClientbusyOnVoIP()) {
                        return;
                    }
                    if (DirectIncomingCallFragment.getInstance() != null && callNotificationActionListener != null) {
                        callNotificationActionListener.onActionClick(actionType);
                    } else {
                        Utils.getInstance().callDeclined(context);
                    }
                    break;
                case "Answer":
                    try {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                            if (isAnswerClickEnabled) {
                                return;
                            }
                        }
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                            isAnswerClickEnabled = true;
                        }
                        Intent directCallingIntent = new Intent(DataStore.getInstance().getContext() != null ? DataStore.getInstance().getContext() : DataStore.getInstance().getAppContext(), DirectCallingActivity.class);

                        if (DirectIncomingCallFragment.getInstance() != null) {
                            directCallingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                directCallingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            launchCallingActivity(directCallingIntent);
                            callNotificationActionListener.onActionClick(actionType);
                        } else {
                            if (intent.hasExtra("callDetails") && intent.getStringExtra("callDetails") != null) {
                                directCallingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    directCallingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                }
                                directCallingIntent.putExtra("callDetails", intent.getStringExtra("callDetails"));
                                directCallingIntent.putExtra("sid", intent.hasExtra("sid") ? intent.getStringExtra("sid") : "");
                                directCallingIntent.putExtra(context.getString(R.string.screen), context.getString(R.string.incoming));
                                directCallingIntent.putExtra(context.getString(R.string.call_answer), context.getString(R.string.call_answer));
                                launchCallingActivity(directCallingIntent);
                            }
                        }
                    } catch (Exception e) {
                        DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error while answering the call: " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                    break;
                case "Missed":
                    try {
                        final MissedCallActions missedCallActions;
                        SharedPreferences sharedPref = StorageHelper.getPreferences(context);
                        NotificationHandler.CallNotificationHandler.getInstance(context).removeNotification(
                                NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);

                        if (intent.hasExtra("missedCallActions") && intent.getStringExtra("missedCallActions") != null) {

                            missedCallActions = MissedCallActions.fromJson(new JSONObject(intent.getStringExtra("missedCallActions")));

                            String callDirection = intent.getStringExtra("callDirection");

                            MissedCallNotificationOpenResult result = new MissedCallNotificationOpenResult();
                            result.action = new MissedCallNotificationOpenResult.MissedCallNotificationAction();
                            result.callDetails = new MissedCallNotificationOpenResult.CallDetails();
                            result.action.actionID = missedCallActions.getActionId();
                            result.action.actionLabel = missedCallActions.getActionLabel();
                            if (intent.hasExtra("callerCuid")) {
                                result.callDetails.callerCuid = intent.getStringExtra("callerCuid");
                            }
                            if (intent.hasExtra("calleeCuid")) {
                                result.callDetails.calleeCuid = intent.getStringExtra("calleeCuid");
                            }
                            if (intent.hasExtra("callContext")) {
                                result.callDetails.callContext = intent.getStringExtra("callContext");
                            }
                            if (callDirection != null && callDirection.equals("outgoing")) {
                                final String missedCallInitiatorHost = sharedPref.getString(KEY_MISSED_CALL_INITIATOR_HOST, null);
                                DataStore.getInstance().setMissedCallInitiatorHost(missedCallInitiatorHost, new OnMissedCallHostSetReceiver() {
                                    @Override
                                    public void onSetMissedCallReceiver(DirectCallAPI.MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler) {
                                        try {
                                            if (missedCallNotificationOpenedHandler != null) {
                                                missedCallNotificationOpenedHandler.onMissedCallNotificationOpened(context.getApplicationContext(), result);
                                            }
                                        } catch (Exception e) {
                                            //no-op
                                        }
                                    }
                                });
                            } else if (callDirection != null && callDirection.equals("incoming")) {
                                final String missedCallReceiverHost = sharedPref.getString(KEY_MISSED_CALL_RECEIVER_HOST, null);
                                DataStore.getInstance().setMissedCallReceiverHost(missedCallReceiverHost, new OnMissedCallHostSetReceiver() {
                                    @Override
                                    public void onSetMissedCallReceiver(DirectCallAPI.MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler) {
                                        try {
                                            if (missedCallNotificationOpenedHandler != null) {
                                                missedCallNotificationOpenedHandler.onMissedCallNotificationOpened(context.getApplicationContext(), result);
                                            }
                                        } catch (Exception e) {
                                            //no-op
                                        }
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Error handling missed call notification click: " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                    break;
                default:
            }
        }
    }

    private void launchCallingActivity(Intent intent) {
        Context context = DataStore.getInstance().getContext();

        if (context == null) {
            context = DataStore.getInstance().getAppContext();
        }
        if (context instanceof Activity) {
            DataStore.getInstance().getContext().startActivity(intent);
        } else if (context instanceof Application) {
            DataStore.getInstance().getAppContext().startActivity(intent);
        }
    }
}