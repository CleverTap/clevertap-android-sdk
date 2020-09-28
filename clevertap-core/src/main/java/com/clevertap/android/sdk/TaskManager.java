package com.clevertap.android.sdk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton class to do heavy loaded task in the background and get the result on the main thread.
 * Suitable for Android general purpose use cases.
 */
public class TaskManager {

    /**
     * Interface for the callbacks
     */
    public interface TaskListener<Params, Result> {

        /**
         * does task in the background thread
         */
        Result doInBackground(Params params);

        /**
         * Gives callback on the main thread
         */
        void onPostExecute(Result result);
    }

    private static TaskManager sInstance;

    private final ExecutorService service;

    public static synchronized TaskManager getInstance() {
        if (sInstance == null) {
            sInstance = new TaskManager();
        }
        return sInstance;
    }

    private TaskManager() {
        this.service = Executors.newFixedThreadPool(10);
    }

    /**
     * Execute task in the background with a callback
     *
     * @param listener - to get the callback
     * @param <Params> - no parameter
     * @param <Result> - result returned by the background task
     */
    public <Params, Result> void execute(final TaskListener<Params, Result> listener) {
        execute(null, listener);
    }

    /**
     * Execute the task with parameters with a callback
     *
     * @param params   params to be passed on the background execution
     * @param listener - to get the callback
     * @param <Params> - params to be passed on the background execution
     * @param <Result> - result returned by the background task
     */
    public <Params, Result> void execute(final Params params, final TaskListener<Params, Result> listener) {

        service.execute(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    final Result result = listener.doInBackground(params);

                    // post the result callback on the main thread
                    Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPostExecute(result);
                        }
                    });
                }
            }
        });
    }
}