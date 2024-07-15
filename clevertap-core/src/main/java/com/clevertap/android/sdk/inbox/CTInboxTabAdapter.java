package com.clevertap.android.sdk.inbox;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom PagerAdapter for Notification Inbox tabs
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@RestrictTo(Scope.LIBRARY)
public class CTInboxTabAdapter extends FragmentPagerAdapter {

    private final Fragment[] fragmentList;

    private final List<String> fragmentTitleList = new ArrayList<>();

    public CTInboxTabAdapter(FragmentManager fm, int size) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        fragmentList = new Fragment[size];
    }

    @Override
    public int getCount() {
        return fragmentList.length;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragmentList[position];
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return fragmentTitleList.get(position);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Object ret = super.instantiateItem(container, position);
        fragmentList[position] = (Fragment) ret;
        return ret;
    }

    void addFragment(Fragment fragment, String title, int position) {
        fragmentList[position] = fragment;
        fragmentTitleList.add(title);
    }
}
