package com.clevertap.android.sdk.inbox;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import com.clevertap.android.sdk.CTInboxStyleConfig;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;
import com.google.android.material.tabs.TabLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This activity shows the {@link CTInboxMessage} objects as per {@link CTInboxStyleConfig} style parameters
 */
@RestrictTo(Scope.LIBRARY)
public class CTInboxActivity extends FragmentActivity implements CTInboxListViewFragment.InboxListener {

    public interface InboxActivityListener {

        void messageDidClick(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data,
                HashMap<String, String> keyValue);

        void messageDidShow(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data);
    }

    public static int orientation;

    CTInboxTabAdapter inboxTabAdapter;

    CTInboxStyleConfig styleConfig;

    TabLayout tabLayout;

    ViewPager viewPager;

    private CleverTapInstanceConfig config;

    private WeakReference<InboxActivityListener> listenerWeakReference;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CleverTapAPI cleverTapAPI;
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
    public void messageDidClick(Context baseContext, CTInboxMessage inboxMessage, Bundle data,
            HashMap<String, String> keyValue) {
        didClick(data, inboxMessage, keyValue);
    }

    @Override
    public void messageDidShow(Context baseContext, CTInboxMessage inboxMessage, Bundle data) {
        didShow(data, inboxMessage);
    }

    void didClick(Bundle data, CTInboxMessage inboxMessage, HashMap<String, String> keyValue) {
        InboxActivityListener listener = getListener();
        if (listener != null) {
            listener.messageDidClick(this, inboxMessage, data, keyValue);
        }
    }

    void didShow(Bundle data, CTInboxMessage inboxMessage) {
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