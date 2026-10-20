package com.shohankhan.hisaabdesk

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import com.shohankhan.hisaabdesk.core.notification.ReminderScheduler

class HisaabDeskApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        // Scheduling must never crash app startup (e.g. WorkManager not yet initialized
        // in unit tests or unusual process states). Reminders resume on next real launch.
        runCatching { ReminderScheduler.schedulePeriodic(this) }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService<NotificationManager>() ?: return
            val channel = NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_reminders_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
    }
}
