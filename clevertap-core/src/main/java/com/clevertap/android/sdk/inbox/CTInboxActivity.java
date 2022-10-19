package com.clevertap.android.sdk.inbox;

import static com.clevertap.android.sdk.CTXtensions.isPackageAndOsTargetsAbove;
import static com.clevertap.android.sdk.inapp.InAppController.IS_FIRST_TIME_PERMISSION_REQUEST;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import com.clevertap.android.sdk.CTInboxListener;
import com.clevertap.android.sdk.CTInboxStyleConfig;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.InAppNotificationActivity;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.inapp.AlertDialogPromptForSettings;
import com.google.android.material.tabs.TabLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import kotlin.Unit;

/**
 * This activity shows the {@link CTInboxMessage} objects as per {@link CTInboxStyleConfig} style parameters
 */
@RestrictTo(Scope.LIBRARY)
public class CTInboxActivity extends FragmentActivity implements CTInboxListViewFragment.InboxListener, CTInboxListener {

    public interface InboxActivityListener {

        void messageDidClick(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data,
                HashMap<String, String> keyValue,boolean isBodyClick);

        void messageDidShow(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data);
    }

    public static int orientation;

    CTInboxTabAdapter inboxTabAdapter;

    CTInboxStyleConfig styleConfig;

    TabLayout tabLayout;

    ViewPager viewPager;

    private CleverTapInstanceConfig config;

    private WeakReference<InboxActivityListener> listenerWeakReference;
    private CleverTapAPI cleverTapAPI;
    private CTInboxListener inboxContentUpdatedListener = null;

    private WeakReference<InAppNotificationActivity.PermissionCallback> permissionCallbackWeakReference;
    private static final int PERMISSION_REQUEST_CODE = 2;
    public static final String ANDROID_PERMISSION_STRING = "android.permission.POST_NOTIFICATIONS";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                throw new IllegalArgumentException();
            }
            styleConfig = extras.getParcelable("styleConfig");
            Bundle configBundle = extras.getBundle("configBundle");
            if (configBundle != null) {
                config = configBundle.getParcelable("config");
            }
            cleverTapAPI = CleverTapAPI.instanceWithConfig(getApplicationContext(), config);
            if (cleverTapAPI != null) {
                setListener(cleverTapAPI);
                inboxContentUpdatedListener = cleverTapAPI.getCTNotificationInboxListener();
                cleverTapAPI.setCTNotificationInboxListener(this);

                setPermissionCallback(CleverTapAPI.instanceWithConfig(this, config).getCoreState()
                        .getInAppController());
            }
            orientation = getResources().getConfiguration().orientation;
        } catch (Throwable t) {
            Logger.v("Cannot find a valid notification inbox bundle to show!", t);
            return;
        }

        setContentView(R.layout.inbox_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(styleConfig.getNavBarTitle());
        toolbar.setTitleTextColor(Color.parseColor(styleConfig.getNavBarTitleColor()));
        toolbar.setBackgroundColor(Color.parseColor(styleConfig.getNavBarColor()));
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ct_ic_arrow_back_white_24dp, null);
        if (drawable != null) {
            drawable.setColorFilter(Color.parseColor(styleConfig.getBackButtonColor()), PorterDuff.Mode.SRC_IN);
        }
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        LinearLayout linearLayout = findViewById(R.id.inbox_linear_layout);
        linearLayout.setBackgroundColor(Color.parseColor(styleConfig.getInboxBackgroundColor()));
        tabLayout = linearLayout.findViewById(R.id.tab_layout);
        viewPager = linearLayout.findViewById(R.id.view_pager);
        TextView noMessageView = findViewById(R.id.no_message_view);
        Bundle bundle = new Bundle();
        bundle.putParcelable("config", config);
        bundle.putParcelable("styleConfig", styleConfig);

        if (!styleConfig.isUsingTabs()) {
            viewPager.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            final FrameLayout listViewFragmentLayout = findViewById(R.id.list_view_fragment);
            listViewFragmentLayout.setVisibility(View.VISIBLE);
            if (cleverTapAPI != null && cleverTapAPI.getInboxMessageCount() == 0) {
                noMessageView.setBackgroundColor(Color.parseColor(styleConfig.getInboxBackgroundColor()));
                noMessageView.setVisibility(View.VISIBLE);
                noMessageView.setText(styleConfig.getNoMessageViewText());
                noMessageView.setTextColor(Color.parseColor(styleConfig.getNoMessageViewTextColor()));
            } else {
                boolean fragmentExists = false;
                noMessageView.setVisibility(View.GONE);
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment.getTag() != null && !fragment.getTag().equalsIgnoreCase(getFragmentTag())) {
                        fragmentExists = true;
                    }
                }
                if (!fragmentExists) {
                    CTInboxListViewFragment listView = new CTInboxListViewFragment();
                    listView.setArguments(bundle);
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.list_view_fragment, listView, getFragmentTag())
                            .commit();
                }
            }
        } else {
            viewPager.setVisibility(View.VISIBLE);
            ArrayList<String> tabs = styleConfig.getTabs();
            inboxTabAdapter = new CTInboxTabAdapter(getSupportFragmentManager(), tabs.size() + 1);
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            tabLayout.setTabMode(TabLayout.MODE_FIXED);
            tabLayout.setSelectedTabIndicatorColor(Color.parseColor(styleConfig.getSelectedTabIndicatorColor()));
            tabLayout.setTabTextColors(Color.parseColor(styleConfig.getUnselectedTabColor()),
                    Color.parseColor(styleConfig.getSelectedTabColor()));
            tabLayout.setBackgroundColor(Color.parseColor(styleConfig.getTabBackgroundColor()));

            Bundle _allBundle = (Bundle) bundle.clone();
            _allBundle.putInt("position", 0);
            CTInboxListViewFragment all = new CTInboxListViewFragment();
            all.setArguments(_allBundle);
            inboxTabAdapter.addFragment(all, styleConfig.getFirstTabTitle(), 0);

            for (int i = 0; i < tabs.size(); i++) {
                String filter = tabs.get(i);
                int pos = i + 1;
                Bundle _bundle = (Bundle) bundle.clone();
                _bundle.putInt("position", pos);
                _bundle.putString("filter", filter);
                CTInboxListViewFragment frag = new CTInboxListViewFragment();
                frag.setArguments(_bundle);
                inboxTabAdapter.addFragment(frag, filter, pos);
                viewPager.setOffscreenPageLimit(pos);
            }

            viewPager.setAdapter(inboxTabAdapter);
            inboxTabAdapter.notifyDataSetChanged();
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    //no-op
                }

                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    CTInboxListViewFragment fragment = (CTInboxListViewFragment) inboxTabAdapter
                            .getItem(tab.getPosition());
                    fragment.updateAdapterContent();
                    if (fragment.getMediaRecyclerView() != null) {
                        fragment.getMediaRecyclerView().onRestartPlayer();
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    CTInboxListViewFragment fragment = (CTInboxListViewFragment) inboxTabAdapter
                            .getItem(tab.getPosition());
                    if (fragment.getMediaRecyclerView() != null) {
                        fragment.getMediaRecyclerView().onPausePlayer();
                    }
                }
            });
            tabLayout.setupWithViewPager(viewPager);
        }
    }

    @SuppressLint("NewApi")
    public void showHardPermissionPrompt(boolean fbSettings){
        if (isPackageAndOsTargetsAbove(this, 32)) {
            requestPermission(fbSettings);
        }
    }

    @RequiresApi(api = 33)
    public void requestPermission(boolean isFallbackSettingsEnabled) {
        int permissionStatus = ContextCompat.checkSelfPermission(CTInboxActivity.this,
                Manifest.permission.POST_NOTIFICATIONS);

        if (permissionStatus == PackageManager.PERMISSION_DENIED){
            boolean isFirstTimeRequest = StorageHelper.getBoolean(CTInboxActivity.this,
                    IS_FIRST_TIME_PERMISSION_REQUEST,true);
            if (!isFirstTimeRequest) {
                if (isFallbackSettingsEnabled) {
                    showFallbackAlertDialog();
                    return;
                }
                permissionCallbackWeakReference.get().onReject();
                return;
            }

            ActivityCompat.requestPermissions(CTInboxActivity.this,
                    new String[]{ANDROID_PERMISSION_STRING}, PERMISSION_REQUEST_CODE);
        }else{
            permissionCallbackWeakReference.get().onAccept();
        }
    }

    public void showFallbackAlertDialog() {
        AlertDialogPromptForSettings.show(this, () -> {
            Utils.navigateToAndroidSettingsForNotifications(CTInboxActivity.this);
            return Unit.INSTANCE;
        }, () -> {
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        StorageHelper.putBoolean(CTInboxActivity.this,IS_FIRST_TIME_PERMISSION_REQUEST,
                false);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED;
            if (granted) {
                permissionCallbackWeakReference.get().onAccept();
            }else {
                permissionCallbackWeakReference.get().onReject();
            }
        }
    }

    public void setPermissionCallback(InAppNotificationActivity.PermissionCallback callback) {
        permissionCallbackWeakReference = new WeakReference<>(callback);
    }

    @Override
    protected void onDestroy() {
        if (styleConfig.isUsingTabs()) {
            List<Fragment> allFragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : allFragments) {
                if (fragment instanceof CTInboxListViewFragment) {
                    Logger.v("Removing fragment - " + fragment.toString());
                    getSupportFragmentManager().getFragments().remove(fragment);
                }
            }
        }
        super.onDestroy();
    }


    @Override
    public void inboxDidInitialize() {

        Logger.d("CTInboxActivity: called inboxDidInitialize");
        if(inboxContentUpdatedListener !=null) {
            inboxContentUpdatedListener.inboxDidInitialize();
        }


    }

    @Override
    public void inboxMessagesDidUpdate() {
        Logger.v("CTInboxActivity|inboxMessagesDidUpdate called");
        try {
            boolean isCUListenerAvailable = inboxContentUpdatedListener != null;
            Logger.v("CTInboxActivity|inboxMessagesDidUpdate: inboxContentUpdatedListener available:" + isCUListenerAvailable);

            boolean isUsingMultipleTabs = styleConfig.isUsingTabs();
            Logger.v("CTInboxActivity|inboxMessagesDidUpdate: isUsingMultipleTabs : " + isUsingMultipleTabs);

            if (isCUListenerAvailable) {
                inboxContentUpdatedListener.inboxMessagesDidUpdate();
            }
            if (isUsingMultipleTabs) {
                int position = viewPager.getCurrentItem();
                CTInboxListViewFragment fragment = (CTInboxListViewFragment) inboxTabAdapter.getItem(position);
                fragment.updateAdapterContent();
            }
            else {
                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                String singleTabTag = getFragmentTag();
                for (Fragment f : fragments) {
                    String currentFTag = f.getTag();
                    if (f instanceof CTInboxListViewFragment && currentFTag != null && currentFTag.equalsIgnoreCase(singleTabTag)) {
                        ((CTInboxListViewFragment) f).updateAdapterContent();
                    }
                }

            }

        } catch (Throwable t) {
            Logger.i("Something Went Wrong", t);
        }


    }

    @Override
    public void messageDidClick(Context baseContext, CTInboxMessage inboxMessage, Bundle data,
            HashMap<String, String> keyValue, boolean isBodyClick) {
        didClick(data, inboxMessage, keyValue,isBodyClick);
    }

    @Override
    public void messageDidShow(Context baseContext, CTInboxMessage inboxMessage, Bundle data) {
        Logger.v("CTInboxActivity:messageDidShow() called with: data = [" + data + "], inboxMessage = [" + inboxMessage .getMessageId()+ "]");
        didShow(data, inboxMessage);
    }

    void didClick(Bundle data, CTInboxMessage inboxMessage, HashMap<String, String> keyValue, boolean isBodyClick) {
        InboxActivityListener listener = getListener();
        if (listener != null) {
            listener.messageDidClick(this, inboxMessage, data, keyValue,isBodyClick);
        }
    }

    void didShow(Bundle data, CTInboxMessage inboxMessage) {
        Logger.v( "CTInboxActivity:didShow() called with: data = [" + data + "], inboxMessage = [" + inboxMessage.getMessageId() + "]");
        InboxActivityListener listener = getListener();
        if (listener != null) {
            listener.messageDidShow(this, inboxMessage, data);
        }
    }

    InboxActivityListener getListener() {
        InboxActivityListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger()
                    .verbose(config.getAccountId(), "InboxActivityListener is null for notification inbox ");
        }
        return listener;
    }

    void setListener(InboxActivityListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    private String getFragmentTag() {
        return config.getAccountId() + ":CT_INBOX_LIST_VIEW_FRAGMENT";
    }
}