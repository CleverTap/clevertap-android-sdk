package com.clevertap.android.sdk.inbox

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * Custom PagerAdapter for Notification Inbox tabs
 */
@Suppress("DEPRECATION")
internal open class CTInboxTabAdapter(fm: FragmentManager, size: Int) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragmentList: Array<Fragment?> = arrayOfNulls(size)

    private val fragmentTitleList: MutableList<String?> = ArrayList()

    override fun getCount(): Int {
        return fragmentList.size
    }

    override fun getItem(position: Int): Fragment {
        // Every slot is filled through addFragment() before the adapter is attached.
        return fragmentList[position]!!
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentTitleList[position]
    }

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
