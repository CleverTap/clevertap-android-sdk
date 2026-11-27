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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(markerClass = [UnstableApi::class])
internal open class CTInboxListViewFragment : Fragment() {

    @RestrictTo(Scope.LIBRARY)
    interface InboxListener {

        fun messageDidClick(
            contentPageIndex: Int, inboxMessage: CTInboxMessage, data: Bundle?,
            keyValue: HashMap<String, String>?, buttonIndex: Int
        )

        fun messageDidShow(inboxMessage: CTInboxMessage, data: Bundle?)
    }

    internal var config: CleverTapInstanceConfig? = null

    internal var haveVideoPlayerSupport: Boolean = VideoLibChecker.haveVideoPlayerSupport

    internal val inboxMessages: MutableList<CTInboxMessage> = ArrayList()

    internal var linearLayout: LinearLayout? = null

    internal var mediaRecyclerView: MediaPlayerRecyclerView? = null

    private var ownVideoStates: InboxVideoStates? = null

    internal var recyclerView: RecyclerView? = null

    internal var noMessageView: TextView? = null

    private var inboxMessageAdapter: CTInboxMessageAdapter? = null

    internal var styleConfig: CTInboxStyleConfig? = null

    private var firstTime = true

    private var listenerWeakReference = WeakReference<InboxListener>(null)

    /** Reading it logs when no listener is attached (or it has been garbage collected). */
    internal var listener: InboxListener?
        get() = listenerWeakReference.get().also {
            if (it == null) {
                Logger.v("InboxListener is null for messages")
            }
        }
        set(value) {
            listenerWeakReference = WeakReference(value)
        }

    private var tabPosition = 0

    private var didClickForHardPermissionListener: DidClickForHardPermissionListener? = null

    /** The list on screen: the video-capable one when the video library is present. */
    private val activeRecyclerView: RecyclerView?
        get() = mediaRecyclerView ?: recyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = arguments ?: return
        @Suppress("DEPRECATION")
        config = bundle.getParcelable("config")
        @Suppress("DEPRECATION")
        styleConfig = bundle.getParcelable("styleConfig")
        tabPosition = bundle.getInt("position", -1)
        updateInboxMessages()
        if (context is CTInboxActivity) {
            listener = activity as InboxListener?
        }
        /*Initializes the below listener only when inbox payload has CTInbox activity as their host activity
        when requesting permission for notification.*/
        if (context is DidClickForHardPermissionListener) {
            didClickForHardPermissionListener = context
        }
    }

    private fun updateInboxMessages() {
        fetchFreshMessages()?.let { replaceMessagesInPlace(it) }
    }

    internal fun fetchFreshMessages(): MutableList<CTInboxMessage>? {
        val bundle = arguments ?: return null
        val filter = bundle.getString("filter", null)
        // Application context, not the Activity: instanceWithConfig may retain the Context for the
        // instance's lifetime, which would leak the Activity.
        val cleverTapAPI = CTInboxActivity.instanceWithConfig(requireContext().applicationContext, config)
            ?: return null
        Logger.v("CTInboxListViewFragment: fetching messages with: tabPosition = [$tabPosition], filter = [$filter]")
        val allMessages = cleverTapAPI.allInboxMessages
        return if (filter != null) { filterMessages(allMessages, filter) } else { allMessages }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val allView = inflater.inflate(R.layout.inbox_list_view, container, false)
        val swipeRefreshLayout = allView.findViewById<SwipeRefreshLayout>(R.id.ct_inbox_swipe_refresh)
        wireSwipeToRefresh(swipeRefreshLayout)
        val listLayout = allView.findViewById<LinearLayout>(R.id.list_view_linear_layout)
        linearLayout = listLayout
        listLayout.setBackgroundColor(Color.parseColor(requireStyleConfig().inboxBackgroundColor))
        val emptyView = allView.findViewById<TextView>(R.id.list_view_no_message_view)
        noMessageView = emptyView

        if (inboxMessages.isEmpty()) {
            showNoMessageView(emptyView)
            return allView
        }

        emptyView.visibility = View.GONE
        buildListView(listLayout)
        return allView
    }

    /** The inbox always opens its tabs with a style config; a missing one is a programming error. */
    private fun requireStyleConfig(): CTInboxStyleConfig =
        checkNotNull(styleConfig) { "CTInboxListViewFragment needs a styleConfig argument" }

    private fun showNoMessageView(noMessageView: TextView) {
        val styleConfig = requireStyleConfig()
        noMessageView.text = styleConfig.noMessageViewText
        noMessageView.setTextColor(Color.parseColor(styleConfig.noMessageViewTextColor))
        noMessageView.visibility = View.VISIBLE
    }

    /** In the inbox screen the tabs share the screen's states; a fragment outside it keeps its own. */
    @VisibleForTesting
    internal fun videoStates(): InboxVideoStates {
        val activity = activity
        if (activity is CTInboxActivity) {
            return activity.videoStates
        }
        return ownVideoStates ?: InboxVideoStates().also { ownVideoStates = it }
    }

    private fun buildListView(linearLayout: LinearLayout) {
        val linearLayoutManager = LinearLayoutManager(activity)
        val adapter = CTInboxMessageAdapter(inboxMessages, this)
        inboxMessageAdapter = adapter
        if (haveVideoPlayerSupport) {
            val mediaList = MediaPlayerRecyclerView(requireActivity(), videoStates())
            mediaRecyclerView = mediaList
            mediaList.visibility = View.VISIBLE
            mediaList.layoutManager = linearLayoutManager
            mediaList.addItemDecoration(VerticalSpaceItemDecoration(LIST_ITEM_SPACING))
            mediaList.itemAnimator = DefaultItemAnimator()

            mediaList.adapter = adapter
            adapter.notifyDataSetChanged()
            linearLayout.addView(mediaList)

            if (firstTime && shouldAutoPlayOnFirstLaunch()) {
                // Plays whichever list is current by then
                Handler(Looper.getMainLooper()).postDelayed({ mediaRecyclerView?.playVideo() }, FIRST_AUTOPLAY_DELAY_MS)
                firstTime = false
            }
        } else {
            val plainList = linearLayout.findViewById<RecyclerView>(R.id.list_view_recycler_view)
            recyclerView = plainList
            plainList.visibility = View.VISIBLE
            plainList.layoutManager = linearLayoutManager
            plainList.addItemDecoration(VerticalSpaceItemDecoration(LIST_ITEM_SPACING))
            plainList.itemAnimator = DefaultItemAnimator()
            plainList.adapter = adapter
            adapter.notifyDataSetChanged()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState ?: return
        @Suppress("DEPRECATION")
        val savedRecyclerLayoutState = savedInstanceState.getParcelable<Parcelable>("recyclerLayoutState")
        mediaRecyclerView?.layoutManager?.onRestoreInstanceState(savedRecyclerLayoutState)
        if (activity !is CTInboxActivity) {
            // Outside the inbox screen the fragment keeps its own states, even before its list is built
            videoStates().restore(savedInstanceState.getBundle(CTInboxActivity.VIDEO_STATES_KEY))
        }
        recyclerView?.layoutManager?.onRestoreInstanceState(savedRecyclerLayoutState)
    }

    override fun onResume() {
        super.onResume()
        mediaRecyclerView?.let { mediaRecyclerView ->
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
        mediaRecyclerView?.playVideo()
    }

    /**
     * Releases this tab's player when another tab is chosen. A dialog or split-screen pause does
     * not; onStop() does.
     */
    open fun onPageHidden() {
        mediaRecyclerView?.releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mediaRecyclerView?.let { mediaRecyclerView ->
            mediaRecyclerView.layoutManager?.let {
                outState.putParcelable("recyclerLayoutState", it.onSaveInstanceState())
            }
            mediaRecyclerView.rememberPlayingVideo() // into the states the activity or this fragment keeps
            ownVideoStates?.let { outState.putBundle(CTInboxActivity.VIDEO_STATES_KEY, it.save()) }
        }

        recyclerView?.layoutManager?.let {
            outState.putParcelable("recyclerLayoutState", it.onSaveInstanceState())
        }
    }

    override fun onStop() {
        super.onStop()
        // Free the player while the inbox is not visible; each video resumes from where it was.
        mediaRecyclerView?.releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecyclerView?.releasePlayer()
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
                "refreshList: content identical (${freshMessages.size} messages) — skipping repaint, video untouched"
            )
            return  // nothing changed — keep any playing video untouched
        }

        val noMessageView = noMessageView
        if (view == null || noMessageView == null) {
            // View not created (or already destroyed) — refresh the data only.
            Logger.v("refreshList: view not available — data-only refresh (${freshMessages.size} messages)")
            replaceMessagesInPlace(freshMessages)
            return
        }

        repaintList(freshMessages = freshMessages, noMessageView = noMessageView)
    }

    /** The view-bound part of [refreshList]: swaps in [freshMessages] and repaints the list or empty state. */
    private fun repaintList(freshMessages: List<CTInboxMessage>, noMessageView: TextView) {
        val oldCount = inboxMessages.size
        // Detach the shared video surface while its holder is still known;
        // rebinding with the surface attached is the SDK-2330 video regression.
        mediaRecyclerView?.prepareForListRebind()

        replaceMessagesInPlace(freshMessages)

        if (inboxMessages.isEmpty()) {
            Logger.v("refreshList: list now empty — showing no-message view")
            mediaRecyclerView?.visibility = View.GONE
            recyclerView?.visibility = View.GONE
            inboxMessageAdapter?.notifyDataSetChanged()
            showNoMessageView(noMessageView)
            return
        }

        noMessageView.visibility = View.GONE
        val inboxMessageAdapter = inboxMessageAdapter
        if (inboxMessageAdapter == null) {
            Logger.v("refreshList: first messages arrived (${inboxMessages.size}) — building list UI")
            // tab was opened empty — the list UI is built lazily now
            buildListView(checkNotNull(linearLayout) { "onCreateView sets linearLayout with noMessageView" })
            return
        }
        Logger.v("refreshList: repainting list, $oldCount -> ${inboxMessages.size} messages")
        activeRecyclerView?.visibility = View.VISIBLE
        inboxMessageAdapter.notifyDataSetChanged()
        mediaRecyclerView?.let { mediaRecyclerView ->
            // Post so findBestVisibleMediaHolder() sees the re-laid-out children.
            mediaRecyclerView.post(mediaRecyclerView::playVideo)
        }
    }

    /**
     * The adapter aliases [inboxMessages]; the field must never be reassigned
     * after the adapter exists, or click positions resolve against the wrong list.
     */
    private fun replaceMessagesInPlace(freshMessages: List<CTInboxMessage>) {
        inboxMessages.clear()
        inboxMessages.addAll(freshMessages)
    }

    internal fun wireSwipeToRefresh(swipeRefreshLayout: SwipeRefreshLayout) {
        // Direct child is a LinearLayout, whose canScrollVertically(-1) is always false.
        // Delegate to whichever RecyclerView is actually on screen so mid-scroll pulls
        // don't falsely trigger a refresh.
        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            activeRecyclerView?.canScrollVertically(-1) == true
        }

        // If the V2 fetch endpoint was already disabled this session, disable the widget
        // before registering the listener so no zombie callback is ever attached.
        val api = CTInboxActivity.instanceWithConfig(requireContext().applicationContext, config)
        if (api?.isInboxFetchDisabledForSession == true) {
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
                activity.runOnUiThread {
                    swipeRefreshLayout.isRefreshing = false
                    if (!success) {
                        Logger.v("pull-to-refresh: fetch failed or throttled — list unchanged")
                        // First fetch that hit a 403 — hide the widget now that the spinner is done.
                        if (refreshApi.isInboxFetchDisabledForSession) {
                            swipeRefreshLayout.isEnabled = false
                        }
                        return@runOnUiThread  // fetch failed or throttled — the stored data is unchanged
                    }
                    // Re-check inside the runnable: state can change between post and run.
                    if (!isAdded) {
                        return@runOnUiThread
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
                }
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
        if (position !in inboxMessages.indices) {
            Logger.v("didClick: stale position $position, ignoring click")
            return
        }
        listener?.messageDidClick(contentPageIndex, inboxMessages[position], data, keyValuePayload, buttonIndex)
    }

    internal fun didShow(data: Bundle?, inboxMessage: CTInboxMessage) {
        val listener = listener ?: return
        Logger.v(
            "CTInboxListViewFragment:didShow() called with: data = [$data], messageId = [${inboxMessage.messageId}]"
        )
        listener.messageDidShow(inboxMessage, data)
    }

    internal fun fireUrlThroughIntent(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("\n", "").replace("\r", "")))
            activity?.let { Utils.setPackageNameFromResolveInfoList(it, intent) }
            startActivity(intent)
        } catch (t: Throwable) {
            // Ignore
        }
    }

    internal fun handleClick(
        position: Int,
        viewPagerPosition: Int,
        buttonText: String?,
        jsonObject: JSONObject?,
        keyValuePayload: HashMap<String, String>?,
        buttonIndex: Int
    ) {
        try {
            val firstContent = inboxMessages[position].inboxMessageContents[0]
            handleClickLink(firstContent = firstContent, jsonObject = jsonObject)

            val data = wzrkParamsBundle(position)
            if (!buttonText.isNullOrEmpty()) {
                data.putString("wzrk_c2a", buttonText)
            }
            didClick(data, position, viewPagerPosition, keyValuePayload, buttonIndex)
        } catch (t: Throwable) {
            Logger.d("Error handling notification button click: ${t.cause}")
        }
    }

    /**
     * Opens the clicked item's action url or, for a button ([jsonObject] non-null), its url link, or forwards
     * a notification-permission link to the hard-permission listener.
     */
    private fun handleClickLink(firstContent: CTInboxMessageContent, jsonObject: JSONObject?) {
        if (jsonObject == null) {
            firstContent.actionUrl?.let { fireUrlThroughIntent(it) }
            return
        }
        // A null link type threw in Java too; the catch in handleClick logs only the (null) cause.
        val linkType = checkNotNull(firstContent.getLinktype(jsonObject))
        val hardPermissionListener = didClickForHardPermissionListener
        if (linkType.equals(Constants.KEY_URL, ignoreCase = true)) {
            firstContent.getLinkUrl(jsonObject)?.let { fireUrlThroughIntent(it) }
        } else if (linkType.contains(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION) &&
            hardPermissionListener != null
        ) {
            val isFallbackSettings = firstContent.isFallbackSettingsEnabled(jsonObject)
            hardPermissionListener.didClickForHardPermissionWithFallbackSettings(isFallbackSettings)
        }
    }

    internal fun handleViewPagerClick(position: Int, viewPagerPosition: Int) {
        try {
            val data = wzrkParamsBundle(position)
            //pass APP_INBOX_ITEM_INDEX as value of buttonIndex to indicate the item click not the button.
            didClick(data, position, viewPagerPosition, null, APP_INBOX_ITEM_INDEX)
            inboxMessages[position].inboxMessageContents[viewPagerPosition].actionUrl?.let {
                fireUrlThroughIntent(it)
            }
        } catch (t: Throwable) {
            Logger.d("Error handling notification button click: ${t.cause}")
        }
    }

    /** The message's `wzrk_*` params, as strings. */
    private fun wzrkParamsBundle(position: Int): Bundle {
        val wzrkParams = inboxMessages[position].wzrkParams
        val bundle = Bundle()
        for (keyName in wzrkParams.keys()) {
            if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                bundle.putString(keyName, wzrkParams.getString(keyName))
            }
        }
        return bundle
    }

    /** A message is added once per tag matching [filter] (ignoring case), as it always was. */
    private fun filterMessages(messages: List<CTInboxMessage>, filter: String): MutableList<CTInboxMessage> =
        messages.flatMapTo(ArrayList()) { message ->
            message.tags.filter { it.equals(filter, ignoreCase = true) }.map { message }
        }

    private fun shouldAutoPlayOnFirstLaunch(): Boolean = tabPosition <= 0

    companion object {
        private const val LIST_ITEM_SPACING = 18

        /** Delay before the first video auto-plays, so the list has been laid out. */
        private const val FIRST_AUTOPLAY_DELAY_MS = 1000L

        /** @return how many fresh copies were upgraded to read. */
        internal fun mergeReadStateForward(
            currentMessages: List<CTInboxMessage>,
            freshMessages: List<CTInboxMessage>
        ): Int {
            val readIds = currentMessages.filter { it.isRead }.mapTo(HashSet()) { it.messageId }
            if (readIds.isEmpty()) {
                return 0
            }
            val upgraded = freshMessages.filter { !it.isRead && it.messageId in readIds }
            upgraded.forEach { it.setRead(true) }
            return upgraded.size
        }

        internal fun isContentIdentical(
            currentMessages: List<CTInboxMessage>,
            freshMessages: List<CTInboxMessage>
        ): Boolean = currentMessages.size == freshMessages.size &&
                currentMessages.zip(freshMessages).all { (current, fresh) ->
                    current.messageId == fresh.messageId &&
                            current.isRead == fresh.isRead &&
                            current.date == fresh.date &&
                            contentJson(current) == contentJson(fresh)
                }

        /**
         * The "msg" sub-object only: the top-level read-state key must stay out of this
         * comparison because the on-screen copies' backing JSON is fetch-time stale
         * relative to their in-place-mutated read flag.
         */
        private fun contentJson(message: CTInboxMessage): String =
            message.data.optJSONObject(Constants.KEY_MSG)?.toString().orEmpty()
    }
}
