package com.clevertap.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom PagerAdapter for Notification Inbox tabs
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CTInboxTabAdapter extends FragmentPagerAdapter {

    private final Fragment[] fragmentList ;
    private final List<String> fragmentTitleList = new ArrayList<>();

    public CTInboxTabAdapter(FragmentManager fm, int size) {
        super(fm);
        fragmentList = new Fragment[size];
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentList[position];
    }

    @Override
    public int getCount() {
        return fragmentList.length;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return fragmentTitleList.get(position);
    }

    void addFragment(Fragment fragment, String title, int position){
        fragmentList[position] = fragment;
        fragmentTitleList.add(title);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Object ret = super.instantiateItem(container, position);
        fragmentList[position] = (Fragment) ret;
        return ret;
    }
}
