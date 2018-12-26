package com.clevertap.android.sdk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class CTInboxActivity extends FragmentActivity implements CTInboxTabBaseFragment.InboxListener {
    interface InboxActivityListener{
        void messageDidShow(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data);
        void messageDidClick(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data);
    }

    private ArrayList<CTInboxMessage> inboxMessageArrayList;
    private CleverTapInstanceConfig config;
    private CTInboxStyleConfig styleConfig;
    private WeakReference<InboxActivityListener> listenerWeakReference;
    private LinearLayout linearLayout;
    private ExoPlayerRecyclerView exoPlayerRecyclerView;
    private RecyclerView recyclerView;
    private CTInboxMessageAdapter inboxMessageAdapter;
    private boolean firstTime = true;
    private ViewPager viewPager;

    void setListener(InboxActivityListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    InboxActivityListener getListener() {
        InboxActivityListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(),"InboxActivityListener is null for notification inbox " );
        }
        return listener;
    }



    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        try{
            Bundle extras = getIntent().getExtras();
            if(extras == null) throw new IllegalArgumentException();
            styleConfig = extras.getParcelable("styleConfig");
            inboxMessageArrayList = extras.getParcelableArrayList("messageList");
            config = extras.getParcelable("config");
            setListener((InboxActivityListener) CleverTapAPI.instanceWithConfig(getApplicationContext(),config));
        }catch (Throwable t){
            Logger.v("Cannot find a valid notification inbox bundle to show!", t);
            return;
        }

        setContentView(R.layout.inbox_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(styleConfig.getNavBarTitle());
        toolbar.setTitleTextColor(Color.parseColor(styleConfig.getNavBarTitleColor()));
        toolbar.setBackgroundColor(Color.parseColor(styleConfig.getNavBarColor()));
        Drawable drawable = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
        drawable.setColorFilter(Color.parseColor(styleConfig.getBackButtonColor()),PorterDuff.Mode.SRC_IN);
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        linearLayout = findViewById(R.id.inbox_linear_layout);
        TabLayout tabLayout = linearLayout.findViewById(R.id.tab_layout);
        viewPager = linearLayout.findViewById(R.id.view_pager);
        if(styleConfig.isUsingTabs()){
            CTInboxTabAdapter inboxTabAdapter = new CTInboxTabAdapter(getSupportFragmentManager());
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setSelectedTabIndicatorColor(Color.parseColor(styleConfig.getSelectedTabIndicatorColor()));
            tabLayout.setTabTextColors(Color.parseColor(styleConfig.getUnselectedTabColor()),Color.parseColor(styleConfig.getSelectedTabColor()));
            tabLayout.setBackgroundColor(Color.parseColor(styleConfig.getTabBackgroundColor()));
            tabLayout.addTab(tabLayout.newTab().setText("ALL"));
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("inboxMessages", inboxMessageArrayList);
            bundle.putParcelable("config", config);
            bundle.putParcelable("styleConfig",styleConfig);
            CTInboxAllTabFragment ctInboxAllTabFragment = new CTInboxAllTabFragment();
            ctInboxAllTabFragment.setArguments(bundle);
            inboxTabAdapter.addFragment(ctInboxAllTabFragment,"ALL");
            if(styleConfig.getFirstTab() != null) {
                CTInboxFirstTabFragment ctInboxFirstTabFragment = new CTInboxFirstTabFragment();
                ctInboxFirstTabFragment.setArguments(bundle);
                tabLayout.addTab(tabLayout.newTab().setText(styleConfig.getFirstTab()));
                inboxTabAdapter.addFragment(ctInboxFirstTabFragment, styleConfig.getFirstTab());
                viewPager.setOffscreenPageLimit(1);
            }
            if(styleConfig.getSecondTab() != null) {
                CTInboxSecondTabFragment ctInboxSecondTabFragment = new CTInboxSecondTabFragment();
                ctInboxSecondTabFragment.setArguments(bundle);
                tabLayout.addTab(tabLayout.newTab().setText(styleConfig.getSecondTab()));
                inboxTabAdapter.addFragment(ctInboxSecondTabFragment, styleConfig.getSecondTab());
                viewPager.setOffscreenPageLimit(2);
            }
            viewPager.setAdapter(inboxTabAdapter);
            tabLayout.setupWithViewPager(viewPager);
        }else{
            viewPager.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            if(checkInboxMessagesContainVideo(inboxMessageArrayList)) {
                exoPlayerRecyclerView = findViewById(R.id.activity_exo_recycler_view);
                exoPlayerRecyclerView.setVisibility(View.VISIBLE);
                exoPlayerRecyclerView.setVideoInfoList(inboxMessageArrayList);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                exoPlayerRecyclerView.setLayoutManager(linearLayoutManager);
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                        linearLayoutManager.getOrientation());
                exoPlayerRecyclerView.addItemDecoration(dividerItemDecoration);
                exoPlayerRecyclerView.setItemAnimator(new DefaultItemAnimator());

                inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessageArrayList, this,null);
                exoPlayerRecyclerView.setAdapter(inboxMessageAdapter);
                inboxMessageAdapter.notifyDataSetChanged();
                if (firstTime) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            exoPlayerRecyclerView.playVideo();
                        }
                    },1000);
                    firstTime = false;
                }
            }else{
                recyclerView = findViewById(R.id.activity_recycler_view);
                recyclerView.setVisibility(View.VISIBLE);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                recyclerView.setLayoutManager(linearLayoutManager);
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                        linearLayoutManager.getOrientation());
                recyclerView.addItemDecoration(dividerItemDecoration);
                recyclerView.setItemAnimator(new DefaultItemAnimator());

                inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessageArrayList, this,null);
                recyclerView.setAdapter(inboxMessageAdapter);
                inboxMessageAdapter.notifyDataSetChanged();
            }
        }
    }

    boolean checkInboxMessagesContainVideo(ArrayList<CTInboxMessage> inboxMessageArrayList){
        boolean videoPresent = false;
        for(CTInboxMessage inboxMessage : inboxMessageArrayList){
            if(inboxMessage.getInboxMessageContents().get(0).mediaIsVideo()){
                videoPresent = true;
                break;
            }
        }
        return videoPresent;
    }

    @Override
    public void messageDidShow(Context baseContext, CTInboxMessage inboxMessage, Bundle data) {
        didShow(data,inboxMessage);
    }

    @Override
    public void messageDidClick(Context baseContext, CTInboxMessage inboxMessage, Bundle data) {
        didClick(data,inboxMessage);
    }

    void didClick(Bundle data, CTInboxMessage inboxMessage) {
        InboxActivityListener listener = getListener();
        if (listener != null) {
            listener.messageDidClick(this,inboxMessage, data);
        }
    }

    void didShow(Bundle data, CTInboxMessage inboxMessage) {
        InboxActivityListener listener = getListener();
        if (listener != null) {
            listener.messageDidShow(this,inboxMessage, data);
        }
    }

    void handleClick(int position, String buttonText){
        try {
            Bundle data = new Bundle();

            data.putString(Constants.NOTIFICATION_ID_TAG,inboxMessageArrayList.get(position).getCampaignId());
            if(buttonText != null && !buttonText.isEmpty())
                data.putString("wzrk_c2a", buttonText);
            didClick(data,inboxMessageArrayList.get(position));

            String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(0).getActionUrl();
            if (actionUrl != null) {
                fireUrlThroughIntent(actionUrl, data);
                return;
            }
        } catch (Throwable t) {
            config.getLogger().debug("Error handling notification button click: " + t.getCause());
        }
    }

    void handleViewPagerClick(int position, int viewPagerPosition){
        try {
            Bundle data = new Bundle();

            data.putString(Constants.NOTIFICATION_ID_TAG,inboxMessageArrayList.get(position).getCampaignId());
            didClick(data,inboxMessageArrayList.get(position));
            String actionUrl = inboxMessageArrayList.get(position).getInboxMessageContents().get(viewPagerPosition).getActionUrl();
            fireUrlThroughIntent(actionUrl, data);
            return;
        }catch (Throwable t){
            config.getLogger().debug("Error handling notification button click: " + t.getCause());
        }
    }

    void fireUrlThroughIntent(String url, Bundle formData) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
    }
}
