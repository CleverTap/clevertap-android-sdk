package com.clevertap.android.sdk;

public class EventDetail {
    private int count, firstTime, lastTime;
    private String name;

    @SuppressWarnings({"unused", "WeakerAccess"})
    public EventDetail(int count, int firstTime, int lastTime, String name) {
        this.count = count;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public int getFirstTime() {
        return firstTime;
    }

    public int getLastTime() {
        return lastTime;
    }

    public String getName() {
        return name;
    }
}
