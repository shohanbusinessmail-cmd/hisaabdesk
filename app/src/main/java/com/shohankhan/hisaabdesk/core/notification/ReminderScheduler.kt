package com.shohankhan.hisaabdesk.core.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Reminder delivery uses WorkManager periodic work (every ~6 hours).
 * This is best-effort by Android design — we never promise minute-perfect
 * delivery, and we do not request exact-alarm permissions.
 */
object ReminderScheduler {
    const val WORK_NAME = "hisaabdesk_reminders"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
