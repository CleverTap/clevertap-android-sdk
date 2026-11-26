package com.clevertap.android.sdk.inbox

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.annotation.RestrictTo

/**
 * Custom PagerAdapter for Notification Inbox tabs
 */
@Suppress("DEPRECATION")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class CTInboxTabAdapter(fm: FragmentManager, size: Int) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragmentList: Array<Fragment?> = arrayOfNulls(size)

    private val fragmentTitleList = mutableListOf<String?>()

    override fun getCount(): Int = fragmentList.size

    override fun getItem(position: Int): Fragment =
        checkNotNull(fragmentList[position]) {
            "Every slot is filled through addFragment() before the adapter is attached"
        }

    /**
     * The fragment in [position], or null while that slot is still empty. The page-change listener
     * uses this instead of [getItem] (which must be non-null for FragmentPagerAdapter): like the
     * original Java adapter, a not-yet-filled tab is simply skipped.
     */
    fun fragmentAt(position: Int): Fragment? = fragmentList[position]

    override fun getPageTitle(position: Int): CharSequence? = fragmentTitleList[position]

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val ret = super.instantiateItem(container, position)
        fragmentList[position] = ret as Fragment
        return ret
    }

    fun addFragment(fragment: Fragment, title: String?, position: Int) {
        fragmentList[position] = fragment
        fragmentTitleList.add(title)
    }
}
