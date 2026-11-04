/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.keybox;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Re-checks the active keybox about twice a day, including after a reboot. */
public class KeyboxStatusJobService extends JobService {

    private final List<JobParameters> mRunning = new ArrayList<>();

    public static void ensureScheduled(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        if (scheduler.getPendingJob(R.integer.keybox_status_check) != null) return;
        ComponentName component = new ComponentName(context, KeyboxStatusJobService.class);
        scheduler.schedule(new JobInfo.Builder(R.integer.keybox_status_check, component)
                .setPersisted(true)
                .setPeriodic(TimeUnit.HOURS.toMillis(12))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build());
    }

    /** Runs one check as soon as a network is available. */
    public static void enqueueSoon(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        ComponentName component = new ComponentName(context, KeyboxStatusJobService.class);
        scheduler.schedule(new JobInfo.Builder(R.integer.keybox_status_check_now, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(TimeUnit.MINUTES.toMillis(30))
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        synchronized (mRunning) {
            mRunning.add(params);
        }
        KeyboxChecker.refreshActive(this, result -> finishIfRunning(params));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        take(params);
        return params.getJobId() == R.integer.keybox_status_check_now;
    }

    private void finishIfRunning(JobParameters params) {
        if (take(params)) jobFinished(params, false);
    }

    private boolean take(JobParameters params) {
        synchronized (mRunning) {
            for (int i = 0; i < mRunning.size(); i++) {
                if (mRunning.get(i) == params) {
                    mRunning.remove(i);
                    return true;
                }
            }
            return false;
        }
    }
}
