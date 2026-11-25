package com.clevertap.android.sdk.inbox

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_INDEX
import com.clevertap.android.sdk.DidClickForHardPermissionListener
import com.clevertap.android.sdk.FetchInboxCallback
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.customviews.InboxVideoStates
import com.clevertap.android.sdk.customviews.MediaPlayerRecyclerView
import com.clevertap.android.sdk.customviews.VerticalSpaceItemDecoration
import com.clevertap.android.sdk.video.VideoLibChecker
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Stays public (with [RestrictTo]): [CTInboxActivity], which [com.clevertap.android.sdk.CleverTapAPI]
 * reaches through its listener, implements the nested [InboxListener].
 */
@RestrictTo(Scope.LIBRARY)
@OptIn(markerClass = [UnstableApi::class])
open class CTInboxListViewFragment : Fragment() {

    @RestrictTo(Scope.LIBRARY)
    interface InboxListener {

        fun messageDidClick(
            contentPageIndex: Int, inboxMessage: CTInboxMessage?, data: Bundle?,
            keyValue: HashMap<String, String>?, buttonIndex: Int
        )

        fun messageDidShow(inboxMessage: CTInboxMessage?, data: Bundle?)
    }

    internal var config: CleverTapInstanceConfig? = null

    internal var haveVideoPlayerSupport: Boolean = VideoLibChecker.haveVideoPlayerSupport

    internal var inboxMessages = ArrayList<CTInboxMessage>()

    internal var linearLayout: LinearLayout? = null

    internal var mediaRecyclerView: MediaPlayerRecyclerView? = null

    private var ownVideoStates: InboxVideoStates? = null

    internal var recyclerView: RecyclerView? = null

    internal var noMessageView: TextView? = null

    private var inboxMessageAdapter: CTInboxMessageAdapter? = null

    internal var styleConfig: CTInboxStyleConfig? = null

    private var firstTime = true

    private var listenerWeakReference: WeakReference<InboxListener>? = null

    private var tabPosition = 0

    private var didClickForHardPermissionListener: DidClickForHardPermissionListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = arguments
        if (bundle != null) {
            @Suppress("DEPRECATION")
            config = bundle.getParcelable("config")
            @Suppress("DEPRECATION")
            styleConfig = bundle.getParcelable("styleConfig")
            tabPosition = bundle.getInt("position", -1)
            updateInboxMessages()
            if (context is CTInboxActivity) {
                setListener(activity as InboxListener?)
            }
            /*Initializes the below listener only when inbox payload has CTInbox activity as their host activity
            when requesting permission for notification.*/
            if (context is DidClickForHardPermissionListener) {
                didClickForHardPermissionListener = context
            }
        }
    }

    private fun updateInboxMessages() {
        val freshMessages = fetchFreshMessages()
        if (freshMessages != null) {
            inboxMessages = freshMessages
        }
    }

    internal fun fetchFreshMessages(): ArrayList<CTInboxMessage>? {
        val bundle = arguments ?: return null
        val filter = bundle.getString("filter", null)
        // Application context, not the Activity: instanceWithConfig may retain the Context for the
        // instance's lifetime, which would leak the Activity.
        val cleverTapAPI = CTInboxActivity.instanceWithConfig(requireContext().applicationContext, config)
            ?: return null
        Logger.v("CTInboxListViewFragment: fetching messages with: tabPosition = [$tabPosition], filter = [$filter]")
        val allMessages = cleverTapAPI.allInboxMessages
        return if (filter != null) filterMessages(allMessages, filter) else allMessages
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val allView = inflater.inflate(R.layout.inbox_list_view, container, false)
        val swipeRefreshLayout = allView.findViewById<SwipeRefreshLayout>(R.id.ct_inbox_swipe_refresh)
        wireSwipeToRefresh(swipeRefreshLayout)
        val linearLayout = allView.findViewById<LinearLayout>(R.id.list_view_linear_layout)
        this.linearLayout = linearLayout
        linearLayout.setBackgroundColor(Color.parseColor(styleConfig!!.inboxBackgroundColor))
        val noMessageView = allView.findViewById<TextView>(R.id.list_view_no_message_view)
        this.noMessageView = noMessageView

        if (inboxMessages.size <= 0) {
            showNoMessageView()
            return allView
        }

        noMessageView.visibility = View.GONE
        buildListView()
        return allView
    }

    private fun showNoMessageView() {
        val noMessageView = noMessageView!!
        noMessageView.text = styleConfig!!.noMessageViewText
        noMessageView.setTextColor(Color.parseColor(styleConfig!!.noMessageViewTextColor))
        noMessageView.visibility = View.VISIBLE
    }

    /** In the inbox screen the tabs share the screen's states; a fragment outside it keeps its own. */
    @VisibleForTesting
    internal fun videoStates(): InboxVideoStates {
        val activity = activity
        if (activity is CTInboxActivity) {
            return activity.videoStates
        }
        if (ownVideoStates == null) {
            ownVideoStates = InboxVideoStates()
        }
        return ownVideoStates!!
    }

    private fun buildListView() {
        val linearLayoutManager = LinearLayoutManager(activity)
        val inboxMessageAdapter = CTInboxMessageAdapter(inboxMessages, this)
        this.inboxMessageAdapter = inboxMessageAdapter
        if (haveVideoPlayerSupport) {
            val mediaRecyclerView = MediaPlayerRecyclerView(activity!!, videoStates())
            this.mediaRecyclerView = mediaRecyclerView
            mediaRecyclerView.visibility = View.VISIBLE
            mediaRecyclerView.layoutManager = linearLayoutManager
            mediaRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(18))
            mediaRecyclerView.itemAnimator = DefaultItemAnimator()

            mediaRecyclerView.adapter = inboxMessageAdapter
            inboxMessageAdapter.notifyDataSetChanged()
            linearLayout!!.addView(mediaRecyclerView)

            if (firstTime && shouldAutoPlayOnFirstLaunch()) {
                Handler(Looper.getMainLooper()).postDelayed({ this.mediaRecyclerView!!.playVideo() }, 1000)
                firstTime = false
            }

        } else {
            val recyclerView = linearLayout!!.findViewById<RecyclerView>(R.id.list_view_recycler_view)
            this.recyclerView = recyclerView
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = linearLayoutManager
            recyclerView.addItemDecoration(VerticalSpaceItemDecoration(18))
            recyclerView.itemAnimator = DefaultItemAnimator()
            recyclerView.adapter = inboxMessageAdapter
            inboxMessageAdapter.notifyDataSetChanged()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            @Suppress("DEPRECATION")
            val savedRecyclerLayoutState = savedInstanceState.getParcelable<Parcelable>("recyclerLayoutState")
            if (mediaRecyclerView != null) {
                if (mediaRecyclerView!!.layoutManager != null) {
                    mediaRecyclerView!!.layoutManager!!.onRestoreInstanceState(savedRecyclerLayoutState)
                }
            }
            if (activity !is CTInboxActivity) {
                // Outside the inbox screen the fragment keeps its own states, even before its list is built
                videoStates().restore(savedInstanceState.getBundle(CTInboxActivity.VIDEO_STATES_KEY))
            }

            if (recyclerView != null) {
                if (recyclerView!!.layoutManager != null) {
                    recyclerView!!.layoutManager!!.onRestoreInstanceState(savedRecyclerLayoutState)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val mediaRecyclerView = mediaRecyclerView
        if (mediaRecyclerView != null) {
            mediaRecyclerView.onRestartPlayer()
            // Retry after layout: a tab restored without a scroll - after rotation - gets no
            // idle callback, and onRestartPlayer() alone runs too early to find any row.
            mediaRecyclerView.post(mediaRecyclerView::playVideo)
        }
    }

    /**
     * Called by [CTInboxActivity] once this page has finished animating into place.
     * [onResume] is too early: it runs while the pager is still settling, so every row
     * measures zero visible height and `playVideo()` finds no candidate.
     */
    open fun onPageSettled() {
        if (mediaRecyclerView != null) {
            mediaRecyclerView!!.playVideo()
        }
    }

    override fun onPause() {
        super.onPause()
        // A tab that is not the current one must not keep a player and its decoder
        if (mediaRecyclerView != null) {
            mediaRecyclerView!!.releasePlayer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mediaRecyclerView = mediaRecyclerView
        if (mediaRecyclerView != null) {
            if (mediaRecyclerView.layoutManager != null) {
                outState.putParcelable(
                    "recyclerLayoutState",
                    mediaRecyclerView.layoutManager!!.onSaveInstanceState()
                )
            }
            mediaRecyclerView.rememberPlayingVideo() // into the states the activity or this fragment keeps
            if (ownVideoStates != null) {
                outState.putBundle(CTInboxActivity.VIDEO_STATES_KEY, ownVideoStates!!.save())
            }
        }

        val recyclerView = recyclerView
        if (recyclerView != null) {
            if (recyclerView.layoutManager != null) {
                outState.putParcelable("recyclerLayoutState", recyclerView.layoutManager!!.onSaveInstanceState())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Free the player while the inbox is not visible; each video resumes from where it was.
        if (mediaRecyclerView != null) {
            mediaRecyclerView!!.releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaRecyclerView != null) {
            mediaRecyclerView!!.releasePlayer()
        }
    }

    /**
     * Repaints this fragment's list from the already-committed message store.
     * Main thread only. Called from the pull-to-refresh success path — never from
     * background updates, so an on-screen list never changes without a user action.
     */
    open fun refreshList() {
        if (activity == null) {
            return
        }
        val freshMessages = fetchFreshMessages() ?: return

        // The view holders mark the on-screen copies read instantly, but the store's
        // write is async — never let a refresh flip a just-read message back to unread.
        val preservedReads = mergeReadStateForward(inboxMessages, freshMessages)
        if (preservedReads > 0) {
            Logger.v("refreshList: preserved read state for $preservedReads just-read message(s)")
        }

        if (isContentIdentical(inboxMessages, freshMessages)) {
            Logger.v(
                "refreshList: content identical (" + freshMessages.size +
                        " messages) — skipping repaint, video untouched"
            )
            return  // nothing changed — keep any playing video untouched
        }

        val noMessageView = noMessageView
        if (view == null || noMessageView == null) {
            // View not created (or already destroyed) — refresh the data only.
            Logger.v(
                "refreshList: view not available — data-only refresh (" +
                        freshMessages.size + " messages)"
            )
            replaceMessagesInPlace(freshMessages)
            return
        }

        val oldCount = inboxMessages.size
        if (mediaRecyclerView != null) {
            // Detach the shared video surface while its holder is still known;
            // rebinding with the surface attached is the SDK-2330 video regression.
            mediaRecyclerView!!.prepareForListRebind()
        }

        replaceMessagesInPlace(freshMessages)

        if (inboxMessages.isEmpty()) {
            Logger.v("refreshList: list now empty — showing no-message view")
            if (mediaRecyclerView != null) {
                mediaRecyclerView!!.visibility = View.GONE
            }
            if (recyclerView != null) {
                recyclerView!!.visibility = View.GONE
            }
            if (inboxMessageAdapter != null) {
                inboxMessageAdapter!!.notifyDataSetChanged()
            }
            showNoMessageView()
            return
        }

        noMessageView.visibility = View.GONE
        val inboxMessageAdapter = inboxMessageAdapter
        if (inboxMessageAdapter == null) {
            Logger.v(
                "refreshList: first messages arrived (" + inboxMessages.size +
                        ") — building list UI"
            )
            buildListView() // tab was opened empty — the list UI is built lazily now
            return
        }
        Logger.v("refreshList: repainting list, " + oldCount + " -> " + inboxMessages.size + " messages")
        val activeRecyclerView: RecyclerView? = if (mediaRecyclerView != null) mediaRecyclerView else recyclerView
        if (activeRecyclerView != null) {
            activeRecyclerView.visibility = View.VISIBLE
        }
        inboxMessageAdapter.notifyDataSetChanged()
        val mediaRecyclerView = mediaRecyclerView
        if (mediaRecyclerView != null) {
            // Post so findBestVisibleMediaHolder() sees the re-laid-out children.
            mediaRecyclerView.post(mediaRecyclerView::playVideo)
        }
    }

    /**
     * The adapter aliases [inboxMessages]; the field must never be reassigned
     * after the adapter exists, or click positions resolve against the wrong list.
     */
    private fun replaceMessagesInPlace(freshMessages: ArrayList<CTInboxMessage>) {
        inboxMessages.clear()
        inboxMessages.addAll(freshMessages)
    }

    internal fun wireSwipeToRefresh(swipeRefreshLayout: SwipeRefreshLayout) {
        // Direct child is a LinearLayout, whose canScrollVertically(-1) is always false.
        // Delegate to whichever RecyclerView is actually on screen so mid-scroll pulls
        // don't falsely trigger a refresh.
        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            val target: RecyclerView? = if (mediaRecyclerView != null) mediaRecyclerView else recyclerView
            target != null && target.canScrollVertically(-1)
        }

        // If the V2 fetch endpoint was already disabled this session, disable the widget
        // before registering the listener so no zombie callback is ever attached.
        val api = CTInboxActivity.instanceWithConfig(requireContext().applicationContext, config)
        if (api != null && api.isInboxFetchDisabledForSession) {
            swipeRefreshLayout.isEnabled = false
            return
        }

        swipeRefreshLayout.setOnRefreshListener {
            // Re-resolve at swipe time — do not capture the outer `api` reference, as the
            // listener lambda can fire long after onCreateView (e.g. after rotation).
            val refreshApi = CTInboxActivity.instanceWithConfig(requireContext().applicationContext, config)
            if (refreshApi == null) {
                swipeRefreshLayout.isRefreshing = false
                return@setOnRefreshListener
            }
            refreshApi.fetchInbox(FetchInboxCallback { success ->
                val activity = activity ?: return@FetchInboxCallback
                activity.runOnUiThread(Runnable {
                    swipeRefreshLayout.isRefreshing = false
                    if (!success) {
                        Logger.v("pull-to-refresh: fetch failed or throttled — list unchanged")
                        // First fetch that hit a 403 — hide the widget now that the spinner is done.
                        if (refreshApi.isInboxFetchDisabledForSession) {
                            swipeRefreshLayout.isEnabled = false
                        }
                        return@Runnable  // fetch failed or throttled — the stored data is unchanged
                    }
                    // Re-check inside the runnable: state can change between post and run.
                    if (!isAdded) {
                        return@Runnable
                    }
                    val currentActivity = this.activity
                    if (currentActivity is CTInboxActivity && !currentActivity.isFinishing) {
                        // All resident tab fragments repaint from the same committed
                        // snapshot, so no tab keeps showing a server-deleted message.
                        Logger.v("pull-to-refresh success: refreshing all inbox tabs")
                        currentActivity.refreshAllInboxListFragments()
                    } else {
                        Logger.v("pull-to-refresh success: host is not CTInboxActivity — refreshing this list only")
                        refreshList()
                    }
                })
            })
        }
    }

    internal fun didClick(
        data: Bundle?,
        position: Int,
        contentPageIndex: Int,
        keyValuePayload: HashMap<String, String>?,
        buttonIndex: Int
    ) {
        // A row's click listener remembers its position from the moment the row was
        // drawn. Touch events are queued, and after a refresh shrinks the list the
        // rows are redrawn only on the NEXT frame — so a tap can arrive carrying a
        // position from the old, longer list. Indexing with it would go out of
        // bounds, so such a tap is ignored instead of acted on.
        if (position < 0 || position >= inboxMessages.size) {
            Logger.v("didClick: stale position $position, ignoring click")
            return
        }
        val listener = getListener()
        listener?.messageDidClick(contentPageIndex, inboxMessages[position], data, keyValuePayload, buttonIndex)
    }

    internal fun didShow(data: Bundle?, inboxMessage: CTInboxMessage?) {
        val listener = getListener()
        if (listener != null) {
            Logger.v(
                "CTInboxListViewFragment:didShow() called with: data = [" + data + "], messageId = [" +
                        inboxMessage!!.messageId + "]"
            )
            listener.messageDidShow(inboxMessage, data)
        }
    }

    internal fun fireUrlThroughIntent(url: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url!!.replace("\n", "").replace("\r", "")))
            val activity = activity
            if (activity != null) {
                Utils.setPackageNameFromResolveInfoList(activity, intent)
            }
            startActivity(intent)
        } catch (t: Throwable) {
            // Ignore
        }
    }

    internal fun getListener(): InboxListener? {
        var listener: InboxListener? = null
        try {
            listener = listenerWeakReference!!.get()
        } catch (t: Throwable) {
            // no-op
        }
        if (listener == null) {
            Logger.v("InboxListener is null for messages")
        }
        return listener
    }

    internal fun setListener(listener: InboxListener?) {
        listenerWeakReference = WeakReference(listener)
    }

    internal fun handleClick(
        position: Int,
        viewPagerPosition: Int,
        buttonText: String?,
        jsonObject: JSONObject?,
        keyValuePayload: HashMap<String, String>?,
        buttonIndex: Int
    ) {
        val isInboxMessageButtonClick = jsonObject != null

        try {
            if (isInboxMessageButtonClick) {
                val linkType = inboxMessages[position].inboxMessageContents[0].getLinktype(jsonObject)
                if (linkType!!.equals(Constants.KEY_URL, ignoreCase = true)) {
                    val actionUrl = inboxMessages[position].inboxMessageContents[0]
                        .getLinkUrl(jsonObject)
                    if (actionUrl != null) {
                        fireUrlThroughIntent(actionUrl)
                    }
                } else if (linkType.contains(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION) &&
                    didClickForHardPermissionListener != null
                ) {
                    val isFallbackSettings = inboxMessages[position]
                        .inboxMessageContents[0].isFallbackSettingsEnabled(jsonObject)
                    didClickForHardPermissionListener!!.didClickForHardPermissionWithFallbackSettings(
                        isFallbackSettings
                    )
                }
            } else {
                val actionUrl = inboxMessages[position].inboxMessageContents[0].actionUrl
                if (actionUrl != null) {
                    fireUrlThroughIntent(actionUrl)
                }
            }

            val data = Bundle()
            val wzrkParams = inboxMessages[position].wzrkParams
            val iterator = wzrkParams.keys()
            while (iterator.hasNext()) {
                val keyName = iterator.next()
                if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                    data.putString(keyName, wzrkParams.getString(keyName))
                }
            }

            if (!buttonText.isNullOrEmpty()) {
                data.putString("wzrk_c2a", buttonText)
            }
            didClick(data, position, viewPagerPosition, keyValuePayload, buttonIndex)
        } catch (t: Throwable) {
            Logger.d("Error handling notification button click: " + t.cause)
        }
    }

    internal fun handleViewPagerClick(position: Int, viewPagerPosition: Int) {
        try {
            val data = Bundle()
            val wzrkParams = inboxMessages[position].wzrkParams
            val iterator = wzrkParams.keys()
            while (iterator.hasNext()) {
                val keyName = iterator.next()
                if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                    data.putString(keyName, wzrkParams.getString(keyName))
                }
            }
            //pass APP_INBOX_ITEM_INDEX as value of buttonIndex to indicate the item click not the button.
            didClick(data, position, viewPagerPosition, null, APP_INBOX_ITEM_INDEX)
            val actionUrl = inboxMessages[position].inboxMessageContents[viewPagerPosition]
                .actionUrl
            fireUrlThroughIntent(actionUrl)
        } catch (t: Throwable) {
            Logger.d("Error handling notification button click: " + t.cause)
        }
    }

    private fun filterMessages(messages: ArrayList<CTInboxMessage>, filter: String): ArrayList<CTInboxMessage> {
        val filteredMessages = ArrayList<CTInboxMessage>()
        for (inboxMessage in messages) {
            if (inboxMessage.tags.size > 0) {
                for (stringTag in inboxMessage.tags) {
                    if (stringTag.equals(filter, ignoreCase = true)) {
                        filteredMessages.add(inboxMessage)
                    }
                }
            }
        }
        return filteredMessages
    }

    private fun shouldAutoPlayOnFirstLaunch(): Boolean {
        return tabPosition <= 0
    }

    companion object {

        /** @return how many fresh copies were upgraded to read. */
        @JvmStatic
        internal fun mergeReadStateForward(
            currentMessages: List<CTInboxMessage>,
            freshMessages: List<CTInboxMessage>
        ): Int {
            val readIds = HashSet<String?>()
            for (message in currentMessages) {
                if (message.isRead) {
                    readIds.add(message.messageId)
                }
            }
            if (readIds.isEmpty()) {
                return 0
            }
            var upgraded = 0
            for (message in freshMessages) {
                if (!message.isRead && readIds.contains(message.messageId)) {
                    message.setRead(true)
                    upgraded++
                }
            }
            return upgraded
        }

        @JvmStatic
        internal fun isContentIdentical(
            currentMessages: List<CTInboxMessage>,
            freshMessages: List<CTInboxMessage>
        ): Boolean {
            if (currentMessages.size != freshMessages.size) {
                return false
            }
            for (i in currentMessages.indices) {
                val current = currentMessages[i]
                val fresh = freshMessages[i]
                if (current.messageId != fresh.messageId ||
                    current.isRead != fresh.isRead ||
                    current.date != fresh.date ||
                    contentJson(current) != contentJson(fresh)
                ) {
                    return false
                }
            }
            return true
        }

        /**
         * The "msg" sub-object only: the top-level read-state key must stay out of this
         * comparison because the on-screen copies' backing JSON is fetch-time stale
         * relative to their in-place-mutated read flag.
         */
        private fun contentJson(message: CTInboxMessage): String {
            val data = message.data
            val msg = data.optJSONObject(Constants.KEY_MSG)
            return msg?.toString() ?: ""
        }
    }
}
