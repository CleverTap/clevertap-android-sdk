package com.clevertap.android.sdk.inbox;

import static com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_INDEX;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import java.util.Iterator;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class CTInboxListViewFragment extends Fragment {

    interface InboxListener {

        void messageDidClick(Context baseContext, int contentPageIndex, CTInboxMessage inboxMessage, Bundle data,
                HashMap<String, String> keyValue, int buttonIndex);

        void messageDidShow(Context baseContext, CTInboxMessage inboxMessage, Bundle data);
    }

    CleverTapInstanceConfig config;

    boolean haveVideoPlayerSupport = VideoLibChecker.haveVideoPlayerSupport;

    ArrayList<CTInboxMessage> inboxMessages = new ArrayList<>();

    LinearLayout linearLayout;

    MediaPlayerRecyclerView mediaRecyclerView;

    RecyclerView recyclerView;
    private  CTInboxMessageAdapter inboxMessageAdapter;


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
        Bundle bundle = getArguments();
        if(bundle==null) return;
        final String filter = bundle.getString("filter", null);
        CleverTapAPI cleverTapAPI = CleverTapAPI.instanceWithConfig(getActivity(), config);
        if (cleverTapAPI != null) {
            Logger.v( "CTInboxListViewFragment:onAttach() called with: tabPosition = [" + tabPosition + "], filter = [" + filter + "]");
            ArrayList<CTInboxMessage> allMessages = cleverTapAPI.getAllInboxMessages();
            inboxMessages = filter != null ? filterMessages(allMessages, filter) : allMessages;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View allView = inflater.inflate(R.layout.inbox_list_view, container, false);
        linearLayout = allView.findViewById(R.id.list_view_linear_layout);
        linearLayout.setBackgroundColor(Color.parseColor(styleConfig.getInboxBackgroundColor()));
        TextView noMessageView = allView.findViewById(R.id.list_view_no_message_view);

        if (inboxMessages.size() <= 0) {
            noMessageView.setVisibility(View.VISIBLE);
            noMessageView.setText(styleConfig.getNoMessageViewText());
            noMessageView.setTextColor(Color.parseColor(styleConfig.getNoMessageViewTextColor()));
            return allView;
        }

        noMessageView.setVisibility(View.GONE);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        inboxMessageAdapter= new CTInboxMessageAdapter(inboxMessages, this);
        if (haveVideoPlayerSupport) {
            mediaRecyclerView = new MediaPlayerRecyclerView(getActivity());
            mediaRecyclerView.setVisibility(View.VISIBLE);
            mediaRecyclerView.setLayoutManager(linearLayoutManager);
            mediaRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(18));
            mediaRecyclerView.setItemAnimator(new DefaultItemAnimator());
            applySystemBarsInsets(mediaRecyclerView);

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
            recyclerView = allView.findViewById(R.id.list_view_recycler_view);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(18));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            applySystemBarsInsets(recyclerView);
            recyclerView.setAdapter(inboxMessageAdapter);
            inboxMessageAdapter.notifyDataSetChanged();
        }
        return allView;
    }

    private void applySystemBarsInsets(RecyclerView view) {
        view.setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
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

    void didClick(Bundle data, int position, int contentPageIndex, HashMap<String, String> keyValuePayload, int buttonIndex) {
        CTInboxListViewFragment.InboxListener listener = getListener();
        if (listener != null) {
            //noinspection ConstantConditions
            listener.messageDidClick(getActivity().getBaseContext(), contentPageIndex, inboxMessages.get(position), data, keyValuePayload, buttonIndex);
        }
    }

    @SuppressWarnings("SameParameterValue")
    void didShow(Bundle data, int position) {
        CTInboxListViewFragment.InboxListener listener = getListener();
        if (listener != null) {
            Logger.v("CTInboxListViewFragment:didShow() called with: data = [" + data + "], position = [" + position + "]");
            //noinspection ConstantConditions
            listener.messageDidShow(getActivity().getBaseContext(), inboxMessages.get(position), data);
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
