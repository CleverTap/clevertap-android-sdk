package com.clevertap.android.sdk;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager {

    private static TaskManager sInstance;
    private final ExecutorService service;

    private TaskManager() {
        this.service = Executors.newFixedThreadPool(10);
    }

    public static synchronized TaskManager getInstance() {
        if (sInstance == null)
            sInstance = new TaskManager();
        return sInstance;
    }

    public <Params, Result> void execute(final TaskListener<Params, Result> listener) {
        execute(null, listener);
    }

    public <Params, Result> void execute(final Params params, final TaskListener<Params, Result> listener) {
        final WeakReference<TaskListener<Params, Result>> listenerWeakReference = new WeakReference<>(listener);
        service.execute(new Runnable() {
            @Override
            public void run() {
                if (listenerWeakReference.get() != null) {
                    final Result result = listenerWeakReference.get().doInBackground(params);
                    Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listenerWeakReference.get() != null) {
                                listenerWeakReference.get().onPostExecute(result);
                            }
                        }
                    });
                }


            }
        });
    }

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
}