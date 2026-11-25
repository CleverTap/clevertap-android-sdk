package com.clevertap.android.sdk.events;

@SuppressWarnings({"unused", "WeakerAccess"})
public class EventDetail {

    private final int count;

    private final int firstTime;

    private final int lastTime;

    private final String name;

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
