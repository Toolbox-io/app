package ru.morozovit.ultimatesecurity

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REPLACED
import android.util.Log
import java.util.concurrent.TimeUnit


class UpdateCheckerBroadcastReceiver: BroadcastReceiver() {
    companion object {
        const val ACTION_START_UPDATE_CHECKER = "ru.morozovit.ultimatesecurity." +
                "UpdateCheckerBroadcastReceiver.START_UPDATE_CHECKER"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null &&
            context != null && (
                    intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == ACTION_START_UPDATE_CHECKER ||
                    intent.action == ACTION_PACKAGE_REPLACED ||
                    intent.action == ACTION_PACKAGE_ADDED
                    )
            ) {
            if (!UpdateCheckerService.running) context.startService(
                Intent(context, UpdateCheckerService::class.java)
            )
        }
    }

    private fun scheduleJob(context: Context) {
        val jobService = ComponentName(context, UpdateCheckerJobService::class.java)
        val exerciseJobBuilder = JobInfo.Builder(
            JobIdManager.getJobId(
                JobIdManager.JOB_TYPE_CHANNEL_PROGRAMS,
                2
            ),
            jobService
        )
        exerciseJobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
        exerciseJobBuilder.setRequiresDeviceIdle(false)
        exerciseJobBuilder.setRequiresCharging(false)
        exerciseJobBuilder.setBackoffCriteria(
            TimeUnit.SECONDS.toMillis(10),
            JobInfo.BACKOFF_POLICY_LINEAR
        )

        Log.i("UpdateChecker", "scheduleJob: adding job to scheduler")

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(exerciseJobBuilder.build())
    }
}