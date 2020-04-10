package com.clevertap.android.sdk;

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

    //TODO @atul can we make this private?
    public <Params, Result> void execute(final Params params, final TaskListener<Params, Result> listener) {

        service.execute(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    final Result result = listener.doInBackground(params);
                    Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {//TODO @atul do we need this check?
                                listener.onPostExecute(result);
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