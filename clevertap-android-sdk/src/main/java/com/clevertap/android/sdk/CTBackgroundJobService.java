package com.clevertap.android.sdk;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * Background Job service to sync up for new notifications
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CTBackgroundJobService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters params) {
        Logger.v("Job Service is starting");
        new Thread(new Runnable() {
            @Override
            public void run() {
                //PushAmpDiagnosticUtil.raiseEvent(getApplicationContext(), Constants.CT_PUSH_AMP_SERVICE_START);
                CleverTapAPI.runJobWork(getApplicationContext(), params);
                jobFinished(params, true);
                Logger.d("Job Service Finished Sent");
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; //to ensure reschedule
    }

}
