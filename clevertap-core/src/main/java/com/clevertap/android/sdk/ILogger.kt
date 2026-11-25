package com.clevertap.android.sdk;

public interface ILogger {
    void debug(String message);

    void debug(String suffix, String message);

    void debug(String suffix, String message, Throwable t);

    @SuppressWarnings("unused")
    void debug(String message, Throwable t);

    @SuppressWarnings("unused")
    void info(String message);

    void info(String suffix, String message);

    @SuppressWarnings("unused")
    void info(String suffix, String message, Throwable t);

    @SuppressWarnings("unused")
    void info(String message, Throwable t);

    void verbose(String message);

    void verbose(String suffix, String message);

    void verbose(String suffix, String message, Throwable t);

    void verbose(String message, Throwable t);
}
