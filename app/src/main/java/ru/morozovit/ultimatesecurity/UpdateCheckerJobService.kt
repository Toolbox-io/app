package ru.morozovit.ultimatesecurity

import android.app.job.JobParameters
import android.app.job.JobService

class UpdateCheckerJobService: JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        UpdateCheckerService.start(applicationContext)
        return true
    }

    override fun onStopJob(params: JobParameters?) = true
}