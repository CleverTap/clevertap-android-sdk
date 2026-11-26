package com.clevertap.android.sdk.inbox

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import com.clevertap.android.sdk.CTInboxStyleConfig
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DidClickForHardPermissionListener
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.PushPermissionHandler
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.InboxVideoStates
import com.google.android.material.tabs.TabLayout
import java.lang.ref.WeakReference

/**
 * This activity shows the [CTInboxMessage] objects as per [CTInboxStyleConfig] style parameters
 *
 * Stays public: [CleverTapAPI] implements the nested [InboxActivityListener].
 */
@RestrictTo(Scope.LIBRARY)
public open class CTInboxActivity : FragmentActivity(), CTInboxListViewFragment.InboxListener,
    DidClickForHardPermissionListener {

    public interface InboxActivityListener {

        public fun messageDidClick(
            ctInboxActivity: CTInboxActivity?, contentPageIndex: Int, inboxMessage: CTInboxMessage?, data: Bundle?,
            keyValue: HashMap<String, String>?, buttonIndex: Int
        )

        public fun messageDidShow(ctInboxActivity: CTInboxActivity?, inboxMessage: CTInboxMessage?, data: Bundle?)
    }

    internal var styleConfig: CTInboxStyleConfig? = null

    private var config: CleverTapInstanceConfig? = null

    private var listenerWeakReference = WeakReference<InboxActivityListener>(null)

    /** Reading it logs when no listener is attached (or it has been garbage collected). */
    internal var listener: InboxActivityListener?
        get() = listenerWeakReference.get().also {
            if (it == null) {
                // Only the lists read this, and they exist only once onCreate has set the config.
                val config = checkNotNull(config) { "CTInboxActivity has no config" }
                config.logger.verbose(config.accountId, "InboxActivityListener is null for notification inbox ")
            }
        }
        set(value) {
            listenerWeakReference = WeakReference(value)
        }

    private var cleverTapAPI: CleverTapAPI? = null

    private var pushPermissionHandler: PushPermissionHandler? = null

    /** Where the videos were left, shared by the lists of all tabs of this screen. */
    internal val videoStates = InboxVideoStates()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState) // the lists save the video that is playing now first
        val saved = videoStates.save()
        if (!saved.isEmpty) {
            outState.putBundle(VIDEO_STATES_KEY, saved)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { videoStates.restore(it.getBundle(VIDEO_STATES_KEY)) }
        try {
            val extras = intent.extras ?: throw IllegalArgumentException()
            @Suppress("DEPRECATION")
            styleConfig = extras.getParcelable("styleConfig")
            val configBundle = extras.getBundle("configBundle")
            if (configBundle != null) {
                @Suppress("DEPRECATION")
                config = configBundle.getParcelable("config")
            }
            val config = config
            val instance = instanceWithConfig(applicationContext, config)
            cleverTapAPI = instance
            // a non-null instance implies a non-null config
            if (instance != null && config != null) {
                listener = instance
                pushPermissionHandler = PushPermissionHandler(
                    config,
                    instance.coreState.callbackManager.pushPermissionResponseListenerList
                )
            }
            orientation = resources.configuration.orientation
        } catch (t: Throwable) {
            Logger.v("Cannot find a valid notification inbox bundle to show!", t)
            return
        }

        setContentView(R.layout.inbox_activity)

        // Java dereferenced these without a check: the screen cannot run without them.
        val cleverTapAPI = checkNotNull(cleverTapAPI) { "No CleverTap instance for the inbox" }
        cleverTapAPI.coreState.coreMetaData.appInboxActivity = this
        val config = checkNotNull(config) { "A CleverTap instance implies its config" }
        val styleConfig = checkNotNull(styleConfig) { "No inbox style config" }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = styleConfig.navBarTitle
        toolbar.setTitleTextColor(Color.parseColor(styleConfig.navBarTitleColor))
        toolbar.setBackgroundColor(Color.parseColor(styleConfig.navBarColor))
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ct_ic_arrow_back_white_24dp, null)
        @Suppress("DEPRECATION")
        drawable?.setColorFilter(Color.parseColor(styleConfig.backButtonColor), PorterDuff.Mode.SRC_IN)
        toolbar.navigationIcon = drawable
        toolbar.navigationContentDescription = getString(R.string.ct_inbox_back_button_content_description)
        toolbar.setNavigationOnClickListener { finish() }

        val linearLayout = findViewById<LinearLayout>(R.id.inbox_linear_layout)
        linearLayout.setBackgroundColor(Color.parseColor(styleConfig.inboxBackgroundColor))
        val tabLayout = linearLayout.findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = linearLayout.findViewById<ViewPager>(R.id.view_pager)
        val bundle = Bundle().apply {
            putParcelable("config", config)
            putParcelable("styleConfig", styleConfig)
        }

        if (!styleConfig.isUsingTabs) {
            viewPager.visibility = View.GONE
            tabLayout.visibility = View.GONE
            // The fragment owns the empty state (its "no messages" view sits inside the
            // SwipeRefreshLayout), so it is created even when the inbox is empty —
            // otherwise pull-to-refresh has no surface on an empty inbox.
            val listViewFragmentLayout = findViewById<FrameLayout>(R.id.list_view_fragment)
            listViewFragmentLayout.visibility = View.VISIBLE
            // On recreation (rotation, process restore) the FragmentManager restores the
            // previously added fragment before this code runs — adding again would stack
            // a duplicate list (and a duplicate video player) into the same container.
            val fragmentTag = "${config.accountId}:CT_INBOX_LIST_VIEW_FRAGMENT"
            if (supportFragmentManager.findFragmentByTag(fragmentTag) == null) {
                val listView = CTInboxListViewFragment()
                listView.arguments = bundle
                supportFragmentManager.beginTransaction()
                    .add(R.id.list_view_fragment, listView, fragmentTag)
                    .commit()
            }
            return
        }

        viewPager.visibility = View.VISIBLE
        val tabs = styleConfig.getTabs()
        val inboxTabAdapter = CTInboxTabAdapter(supportFragmentManager, tabs.size + 1)
        tabLayout.visibility = View.VISIBLE
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        tabLayout.tabMode = TabLayout.MODE_FIXED
        tabLayout.setSelectedTabIndicatorColor(Color.parseColor(styleConfig.selectedTabIndicatorColor))
        tabLayout.setTabTextColors(
            Color.parseColor(styleConfig.unselectedTabColor),
            Color.parseColor(styleConfig.selectedTabColor)
        )
        tabLayout.setBackgroundColor(Color.parseColor(styleConfig.tabBackgroundColor))

        val all = CTInboxListViewFragment()
        all.arguments = (bundle.clone() as Bundle).apply { putInt("position", 0) }
        inboxTabAdapter.addFragment(all, styleConfig.firstTabTitle, 0)

        tabs.forEachIndexed { i, filter ->
            val pos = i + 1
            val frag = CTInboxListViewFragment()
            frag.arguments = (bundle.clone() as Bundle).apply {
                putInt("position", pos)
                putString("filter", filter)
            }
            inboxTabAdapter.addFragment(frag, filter, pos)
            viewPager.offscreenPageLimit = pos
        }

        viewPager.adapter = inboxTabAdapter
        inboxTabAdapter.notifyDataSetChanged()
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        viewPager.addOnPageChangeListener(createPageSettledListener(viewPager, inboxTabAdapter))
        tabLayout.setupWithViewPager(viewPager)
    }

    override fun onResume() {
        super.onResume()
        pushPermissionHandler?.onActivityResume(this)
    }

    override fun didClickForHardPermissionWithFallbackSettings(fallbackToSettings: Boolean) {
        pushPermissionHandler?.requestPermission(this, fallbackToSettings)
    }

    override fun didCancelPermissionRequest() {
        pushPermissionHandler?.notifyPushPermissionExternalListeners(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        pushPermissionHandler?.onRequestPermissionsResult(this, requestCode, grantResults)
    }

    override fun onDestroy() {
        // Java dereferenced these without a check (they are missing when onCreate bailed out early).
        val cleverTapAPI = checkNotNull(cleverTapAPI) { "No CleverTap instance for the inbox" }
        cleverTapAPI.coreState.coreMetaData.appInboxActivity = null

        val styleConfig = checkNotNull(styleConfig) { "No inbox style config" }
        if (styleConfig.isUsingTabs) {
            for (fragment in supportFragmentManager.fragments) {
                if (fragment is CTInboxListViewFragment) {
                    Logger.v("Removing fragment - $fragment")
                    supportFragmentManager.fragments.remove(fragment)
                }
            }
        }
        super.onDestroy()
    }

    /**
     * Repaints every resident inbox list fragment from the already-committed message
     * store, so tabs never show contradicting data after a refresh. Called only from
     * the pull-to-refresh success path (explicit user action) — never from background
     * updates, so an on-screen list never changes without a user gesture.
     */
    internal fun refreshAllInboxListFragments() {
        var refreshed = 0
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is CTInboxListViewFragment && fragment.isAdded) {
                fragment.refreshList()
                refreshed++
            }
        }
        Logger.v("refreshAllInboxListFragments: refreshed $refreshed inbox fragment(s)")
    }

    override fun messageDidClick(
        contentPageIndex: Int, inboxMessage: CTInboxMessage, data: Bundle?,
        keyValue: HashMap<String, String>?, buttonIndex: Int
    ) {
        didClick(data, contentPageIndex, inboxMessage, keyValue, buttonIndex)
    }

    override fun messageDidShow(inboxMessage: CTInboxMessage, data: Bundle?) {
        Logger.v(
            "CTInboxActivity:messageDidShow() called with: data = [$data], inboxMessage = [${inboxMessage.messageId}]"
        )
        didShow(data, inboxMessage)
    }

    internal fun didClick(
        data: Bundle?,
        contentPageIndex: Int,
        inboxMessage: CTInboxMessage,
        keyValue: HashMap<String, String>?,
        buttonIndex: Int
    ) {
        listener?.messageDidClick(this, contentPageIndex, inboxMessage, data, keyValue, buttonIndex)
    }

    internal fun didShow(data: Bundle?, inboxMessage: CTInboxMessage) {
        Logger.v("CTInboxActivity:didShow() called with: data = [$data], inboxMessage = [${inboxMessage.messageId}]")
        listener?.messageDidShow(this, inboxMessage, data)
    }

    public companion object {

        internal var orientation: Int = 0

        internal const val VIDEO_STATES_KEY = "videoStates"

        /**
         * Tells the tab that just came to rest it is fully on screen, so its video can start.
         * onPageSelected() fires while the page is still animating, when rows still measure zero
         * visible height - only SCROLL_STATE_IDLE means settled.
         */
        internal fun createPageSettledListener(
            pager: ViewPager,
            adapter: CTInboxTabAdapter
        ): ViewPager.OnPageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state != ViewPager.SCROLL_STATE_IDLE) {
                    return
                }
                val position = pager.currentItem
                if (position !in 0 until adapter.count) {
                    return
                }
                (adapter.getItem(position) as? CTInboxListViewFragment)?.onPageSettled()
            }
        }

        /**
         * [CleverTapAPI.instanceWithConfig] declares a non-null config but answers a null one with
         * this log line and `null`; keep that for a missing config bundle.
         */
        internal fun instanceWithConfig(context: Context, config: CleverTapInstanceConfig?): CleverTapAPI? {
            if (config == null) {
                Logger.v("CleverTapInstanceConfig cannot be null")
                return null
            }
            return CleverTapAPI.instanceWithConfig(context, config)
        }
    }
}
