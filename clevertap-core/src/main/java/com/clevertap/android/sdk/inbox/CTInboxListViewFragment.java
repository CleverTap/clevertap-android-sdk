package com.clevertap.android.sdk.inbox;

import static com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_INDEX;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.clevertap.android.sdk.CTInboxStyleConfig;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.DidClickForHardPermissionListener;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.customviews.MediaPlayerRecyclerView;
import com.clevertap.android.sdk.customviews.VerticalSpaceItemDecoration;
import com.clevertap.android.sdk.video.VideoLibChecker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class CTInboxListViewFragment extends Fragment {

    interface InboxListener {

        void messageDidClick(int contentPageIndex, CTInboxMessage inboxMessage, Bundle data,
                HashMap<String, String> keyValue, int buttonIndex);

        void messageDidShow(CTInboxMessage inboxMessage, Bundle data);
    }

    CleverTapInstanceConfig config;

    boolean haveVideoPlayerSupport = VideoLibChecker.haveVideoPlayerSupport;

    ArrayList<CTInboxMessage> inboxMessages = new ArrayList<>();

    LinearLayout linearLayout;

    MediaPlayerRecyclerView mediaRecyclerView;

    RecyclerView recyclerView;

    TextView noMessageView;

    private CTInboxMessageAdapter inboxMessageAdapter;


    CTInboxStyleConfig styleConfig;

    private boolean firstTime = true;

    private WeakReference<CTInboxListViewFragment.InboxListener> listenerWeakReference;

    private int tabPosition;

    private DidClickForHardPermissionListener didClickForHardPermissionListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Bundle bundle = getArguments();
        if (bundle != null) {
            config = bundle.getParcelable("config");
            styleConfig = bundle.getParcelable("styleConfig");
            tabPosition = bundle.getInt("position", -1);
            updateInboxMessages();
            if (context instanceof CTInboxActivity) {
                setListener((CTInboxListViewFragment.InboxListener) getActivity());
            }
            /*Initializes the below listener only when inbox payload has CTInbox activity as their host activity
            when requesting permission for notification.*/
            if (context instanceof DidClickForHardPermissionListener) {
                didClickForHardPermissionListener = (DidClickForHardPermissionListener) context;
            }
        }
    }
    private void updateInboxMessages(){
        ArrayList<CTInboxMessage> freshMessages = fetchFreshMessages();
        if (freshMessages != null) {
            inboxMessages = freshMessages;
        }
    }

    @Nullable
    ArrayList<CTInboxMessage> fetchFreshMessages() {
        Bundle bundle = getArguments();
        if (bundle == null) return null;
        final String filter = bundle.getString("filter", null);
        CleverTapAPI cleverTapAPI = CleverTapAPI.instanceWithConfig(getActivity(), config);
        if (cleverTapAPI == null) return null;
        Logger.v("CTInboxListViewFragment: fetching messages with: tabPosition = [" + tabPosition + "], filter = [" + filter + "]");
        ArrayList<CTInboxMessage> allMessages = cleverTapAPI.getAllInboxMessages();
        return filter != null ? filterMessages(allMessages, filter) : allMessages;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View allView = inflater.inflate(R.layout.inbox_list_view, container, false);
        SwipeRefreshLayout swipeRefreshLayout = allView.findViewById(R.id.ct_inbox_swipe_refresh);
        wireSwipeToRefresh(swipeRefreshLayout);
        linearLayout = allView.findViewById(R.id.list_view_linear_layout);
        linearLayout.setBackgroundColor(Color.parseColor(styleConfig.getInboxBackgroundColor()));
        noMessageView = allView.findViewById(R.id.list_view_no_message_view);

        if (inboxMessages.size() <= 0) {
            showNoMessageView();
            return allView;
        }

        noMessageView.setVisibility(View.GONE);
        buildListView();
        return allView;
    }

    private void showNoMessageView() {
        noMessageView.setText(styleConfig.getNoMessageViewText());
        noMessageView.setTextColor(Color.parseColor(styleConfig.getNoMessageViewTextColor()));
        noMessageView.setVisibility(View.VISIBLE);
    }

    private void buildListView() {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        inboxMessageAdapter = new CTInboxMessageAdapter(inboxMessages, this);
        if (haveVideoPlayerSupport) {
            mediaRecyclerView = new MediaPlayerRecyclerView(getActivity());
            mediaRecyclerView.setVisibility(View.VISIBLE);
            mediaRecyclerView.setLayoutManager(linearLayoutManager);
            mediaRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(18));
            mediaRecyclerView.setItemAnimator(new DefaultItemAnimator());

            mediaRecyclerView.setAdapter(inboxMessageAdapter);
            inboxMessageAdapter.notifyDataSetChanged();
            linearLayout.addView(mediaRecyclerView);

            if (firstTime && shouldAutoPlayOnFirstLaunch()) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mediaRecyclerView.playVideo();
                    }
                }, 1000);
                firstTime = false;
            }

        } else {
            recyclerView = linearLayout.findViewById(R.id.list_view_recycler_view);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(18));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(inboxMessageAdapter);
            inboxMessageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable("recyclerLayoutState");
            if (mediaRecyclerView != null) {
                if (mediaRecyclerView.getLayoutManager() != null) {
                    mediaRecyclerView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
                }
            }

            if (recyclerView != null) {
                if (recyclerView.getLayoutManager() != null) {
                    recyclerView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mediaRecyclerView != null) {
            mediaRecyclerView.onRestartPlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaRecyclerView != null) {
            mediaRecyclerView.onPausePlayer();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mediaRecyclerView != null) {
            if (mediaRecyclerView.getLayoutManager() != null) {
                outState.putParcelable("recyclerLayoutState",
                        mediaRecyclerView.getLayoutManager().onSaveInstanceState());
            }
        }

        if (recyclerView != null) {
            if (recyclerView.getLayoutManager() != null) {
                outState.putParcelable("recyclerLayoutState", recyclerView.getLayoutManager().onSaveInstanceState());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaRecyclerView != null) {
            mediaRecyclerView.stop();
        }
    }

    /**
     * Repaints this fragment's list from the already-committed message store.
     * Main thread only. Called from the pull-to-refresh success path — never from
     * background updates, so an on-screen list never changes without a user action.
     */
    void refreshList() {
        if (getActivity() == null) {
            return;
        }
        ArrayList<CTInboxMessage> freshMessages = fetchFreshMessages();
        if (freshMessages == null) {
            return;
        }

        // The view holders mark the on-screen copies read instantly, but the store's
        // write is async — never let a refresh flip a just-read message back to unread.
        int preservedReads = mergeReadStateForward(inboxMessages, freshMessages);
        if (preservedReads > 0) {
            Logger.v("refreshList: preserved read state for " + preservedReads + " just-read message(s)");
        }

        if (isContentIdentical(inboxMessages, freshMessages)) {
            Logger.v("refreshList: content identical (" + freshMessages.size()
                    + " messages) — skipping repaint, video untouched");
            return; // nothing changed — keep any playing video untouched
        }

        if (getView() == null || noMessageView == null) {
            // View not created (or already destroyed) — refresh the data only.
            Logger.v("refreshList: view not available — data-only refresh ("
                    + freshMessages.size() + " messages)");
            replaceMessagesInPlace(freshMessages);
            return;
        }

        int oldCount = inboxMessages.size();
        if (mediaRecyclerView != null) {
            // Detach the shared video surface while its holder is still known;
            // rebinding with the surface attached is the SDK-2330 video regression.
            mediaRecyclerView.prepareForListRebind();
        }

        replaceMessagesInPlace(freshMessages);

        if (inboxMessages.isEmpty()) {
            Logger.v("refreshList: list now empty — showing no-message view");
            if (mediaRecyclerView != null) {
                mediaRecyclerView.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            if (inboxMessageAdapter != null) {
                inboxMessageAdapter.notifyDataSetChanged();
            }
            showNoMessageView();
            return;
        }

        noMessageView.setVisibility(View.GONE);
        if (inboxMessageAdapter == null) {
            Logger.v("refreshList: first messages arrived (" + inboxMessages.size()
                    + ") — building list UI");
            buildListView(); // tab was opened empty — the list UI is built lazily now
            return;
        }
        Logger.v("refreshList: repainting list, " + oldCount + " -> " + inboxMessages.size() + " messages");
        RecyclerView activeRecyclerView = mediaRecyclerView != null ? mediaRecyclerView : recyclerView;
        if (activeRecyclerView != null) {
            activeRecyclerView.setVisibility(View.VISIBLE);
        }
        inboxMessageAdapter.notifyDataSetChanged();
        if (mediaRecyclerView != null) {
            // Post so findBestVisibleMediaHolder() sees the re-laid-out children.
            mediaRecyclerView.post(mediaRecyclerView::playVideo);
        }
    }

    /**
     * The adapter aliases {@link #inboxMessages}; the field must never be reassigned
     * after the adapter exists, or click positions resolve against the wrong list.
     */
    private void replaceMessagesInPlace(ArrayList<CTInboxMessage> freshMessages) {
        inboxMessages.clear();
        inboxMessages.addAll(freshMessages);
    }

    /** @return how many fresh copies were upgraded to read. */
    static int mergeReadStateForward(List<CTInboxMessage> currentMessages, List<CTInboxMessage> freshMessages) {
        HashSet<String> readIds = new HashSet<>();
        for (CTInboxMessage message : currentMessages) {
            if (message.isRead()) {
                readIds.add(message.getMessageId());
            }
        }
        if (readIds.isEmpty()) {
            return 0;
        }
        int upgraded = 0;
        for (CTInboxMessage message : freshMessages) {
            if (!message.isRead() && readIds.contains(message.getMessageId())) {
                message.setRead(true);
                upgraded++;
            }
        }
        return upgraded;
    }

    static boolean isContentIdentical(List<CTInboxMessage> currentMessages, List<CTInboxMessage> freshMessages) {
        if (currentMessages.size() != freshMessages.size()) {
            return false;
        }
        for (int i = 0; i < currentMessages.size(); i++) {
            CTInboxMessage current = currentMessages.get(i);
            CTInboxMessage fresh = freshMessages.get(i);
            if (!current.getMessageId().equals(fresh.getMessageId())
                    || current.isRead() != fresh.isRead()
                    || current.getDate() != fresh.getDate()
                    || !contentJson(current).equals(contentJson(fresh))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The "msg" sub-object only: the top-level read-state key must stay out of this
     * comparison because the on-screen copies' backing JSON is fetch-time stale
     * relative to their in-place-mutated read flag.
     */
    private static String contentJson(CTInboxMessage message) {
        JSONObject data = message.getData();
        if (data == null) {
            return "";
        }
        JSONObject msg = data.optJSONObject(Constants.KEY_MSG);
        return msg != null ? msg.toString() : "";
    }

    void wireSwipeToRefresh(@NonNull final SwipeRefreshLayout swipeRefreshLayout) {
        // Direct child is a LinearLayout, whose canScrollVertically(-1) is always false.
        // Delegate to whichever RecyclerView is actually on screen so mid-scroll pulls
        // don't falsely trigger a refresh.
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> {
            RecyclerView target = mediaRecyclerView != null ? mediaRecyclerView : recyclerView;
            return target != null && target.canScrollVertically(-1);
        });

        // If the V2 fetch endpoint was already disabled this session, disable the widget
        // before registering the listener so no zombie callback is ever attached.
        CleverTapAPI api = CleverTapAPI.instanceWithConfig(requireContext().getApplicationContext(), config);
        if (api != null && api.isInboxFetchDisabledForSession()) {
            swipeRefreshLayout.setEnabled(false);
            return;
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Re-resolve at swipe time — do not capture the outer `api` reference, as the
            // listener lambda can fire long after onCreateView (e.g. after rotation).
            CleverTapAPI refreshApi = CleverTapAPI.instanceWithConfig(requireContext().getApplicationContext(), config);
            if (refreshApi == null) {
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            refreshApi.fetchInbox(success -> {
                Activity activity = getActivity();
                if (activity == null) return;
                activity.runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    if (!success) {
                        Logger.v("pull-to-refresh: fetch failed or throttled — list unchanged");
                        // First fetch that hit a 403 — hide the widget now that the spinner is done.
                        if (refreshApi.isInboxFetchDisabledForSession()) {
                            swipeRefreshLayout.setEnabled(false);
                        }
                        return; // fetch failed or throttled — the stored data is unchanged
                    }
                    // Re-check inside the runnable: state can change between post and run.
                    if (!isAdded()) return;
                    Activity currentActivity = getActivity();
                    if (currentActivity instanceof CTInboxActivity && !currentActivity.isFinishing()) {
                        // All resident tab fragments repaint from the same committed
                        // snapshot, so no tab keeps showing a server-deleted message.
                        Logger.v("pull-to-refresh success: refreshing all inbox tabs");
                        ((CTInboxActivity) currentActivity).refreshAllInboxListFragments();
                    } else {
                        Logger.v("pull-to-refresh success: host is not CTInboxActivity — refreshing this list only");
                        refreshList();
                    }
                });
            });
        });
    }

    void didClick(Bundle data, int position, int contentPageIndex, HashMap<String, String> keyValuePayload, int buttonIndex) {
        // A row's click listener remembers its position from the moment the row was
        // drawn. Touch events are queued, and after a refresh shrinks the list the
        // rows are redrawn only on the NEXT frame — so a tap can arrive carrying a
        // position from the old, longer list. Indexing with it would go out of
        // bounds, so such a tap is ignored instead of acted on.
        if (position < 0 || position >= inboxMessages.size()) {
            Logger.v("didClick: stale position " + position + ", ignoring click");
            return;
        }
        CTInboxListViewFragment.InboxListener listener = getListener();
        if (listener != null) {
            listener.messageDidClick(contentPageIndex, inboxMessages.get(position), data, keyValuePayload, buttonIndex);
        }
    }

    @SuppressWarnings("SameParameterValue")
    void didShow(Bundle data, CTInboxMessage inboxMessage) {
        CTInboxListViewFragment.InboxListener listener = getListener();
        if (listener != null) {
            Logger.v("CTInboxListViewFragment:didShow() called with: data = [" + data + "], messageId = [" + inboxMessage.getMessageId() + "]");
            listener.messageDidShow(inboxMessage, data);
        }
    }

    void fireUrlThroughIntent(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("\n", "").replace("\r", "")));
            if (getActivity() != null) {
                Utils.setPackageNameFromResolveInfoList(getActivity(), intent);
            }
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
    }

    CTInboxListViewFragment.InboxListener getListener() {
        CTInboxListViewFragment.InboxListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            Logger.v("InboxListener is null for messages");
        }
        return listener;
    }

    void setListener(CTInboxListViewFragment.InboxListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    MediaPlayerRecyclerView getMediaRecyclerView() {
        return this.mediaRecyclerView;
    }

    void setMediaRecyclerView(MediaPlayerRecyclerView mediaRecyclerView) {
        this.mediaRecyclerView = mediaRecyclerView;
    }

    void handleClick(int position, int viewPagerPosition, String buttonText, JSONObject jsonObject, HashMap<String, String> keyValuePayload, int buttonIndex) {
        boolean isInboxMessageButtonClick = jsonObject != null;

        try {
            if (isInboxMessageButtonClick) {
                String linkType = inboxMessages.get(position).getInboxMessageContents().
                        get(0).getLinktype(jsonObject);
                if (linkType.equalsIgnoreCase(Constants.KEY_URL)) {
                    String actionUrl = inboxMessages.get(position).getInboxMessageContents().get(0)
                            .getLinkUrl(jsonObject);
                    if (actionUrl != null) {
                        fireUrlThroughIntent(actionUrl);
                    }
                }
                else if (linkType.contains(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION)
                        && didClickForHardPermissionListener != null) {
                    boolean isFallbackSettings = inboxMessages.get(position).
                            getInboxMessageContents().get(0).isFallbackSettingsEnabled(jsonObject);
                    didClickForHardPermissionListener.didClickForHardPermissionWithFallbackSettings(isFallbackSettings);
                }
            } else {
                String actionUrl = inboxMessages.get(position).getInboxMessageContents().get(0).getActionUrl();
                if (actionUrl != null) {
                    fireUrlThroughIntent(actionUrl);
                }
            }

            Bundle data = new Bundle();
            JSONObject wzrkParams = inboxMessages.get(position).getWzrkParams();
            Iterator<String> iterator = wzrkParams.keys();
            while (iterator.hasNext()) {
                String keyName = iterator.next();
                if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                    data.putString(keyName, wzrkParams.getString(keyName));
                }
            }

            if (buttonText != null && !buttonText.isEmpty()) {
                data.putString("wzrk_c2a", buttonText);
            }
            didClick(data, position, viewPagerPosition, keyValuePayload, buttonIndex);
        } catch (Throwable t) {
            Logger.d("Error handling notification button click: " + t.getCause());
        }
    }

    void handleViewPagerClick(int position, int viewPagerPosition) {
        try {
            Bundle data = new Bundle();
            JSONObject wzrkParams = inboxMessages.get(position).getWzrkParams();
            Iterator<String> iterator = wzrkParams.keys();
            while (iterator.hasNext()) {
                String keyName = iterator.next();
                if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                    data.putString(keyName, wzrkParams.getString(keyName));
                }
            }
            //pass APP_INBOX_ITEM_INDEX as value of buttonIndex to indicate the item click not the button.
            didClick(data, position, viewPagerPosition,null, APP_INBOX_ITEM_INDEX);
            String actionUrl = inboxMessages.get(position).getInboxMessageContents().get(viewPagerPosition)
                    .getActionUrl();
            fireUrlThroughIntent(actionUrl);
        } catch (Throwable t) {
            Logger.d("Error handling notification button click: " + t.getCause());
        }
    }

    private ArrayList<CTInboxMessage> filterMessages(ArrayList<CTInboxMessage> messages, String filter) {
        ArrayList<CTInboxMessage> filteredMessages = new ArrayList<>();
        for (CTInboxMessage inboxMessage : messages) {
            if (inboxMessage.getTags() != null && inboxMessage.getTags().size() > 0) {
                for (String stringTag : inboxMessage.getTags()) {
                    if (stringTag.equalsIgnoreCase(filter)) {
                        filteredMessages.add(inboxMessage);
                    }
                }
            }
        }
        return filteredMessages;
    }

    private boolean shouldAutoPlayOnFirstLaunch() {
        return tabPosition <= 0;
    }
}
