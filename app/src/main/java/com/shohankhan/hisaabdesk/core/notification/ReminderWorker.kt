package com.shohankhan.hisaabdesk.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shohankhan.hisaabdesk.HisaabDeskApplication
import com.shohankhan.hisaabdesk.MainActivity
import com.shohankhan.hisaabdesk.R
import com.shohankhan.hisaabdesk.core.formatting.Dates
import com.shohankhan.hisaabdesk.core.formatting.Money
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Posts reminders for bills and upcoming loan installments that are due soon or
 * overdue. A reminder is ONLY a notification — it never creates payment records.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? HisaabDeskApplication ?: return Result.success()
        val container = app.container

        val settings = container.settingsRepository.settings.first()
        if (!settings.remindersEnabled) return Result.success()

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission denied — degrade gracefully, never crash or nag.
            return Result.success()
        }

        val today = LocalDate.now()
        val fromDay = Dates.dayKey(today)
        val toDay = Dates.dayKey(today.plusDays(7))
        val digit = settings.digitStyle

        val dueBills = container.planningRepository.dueItems(fromDay)
        for (bill in dueBills.take(5)) {
            notify(
                id = bill.id.toInt(),
                title = bill.name,
                text = "পরিশোধের নির্ধারিত তারিখ ${Dates.format(Dates.localDateOf(bill.nextDateMillis), digit)} — ${Money.format(bill.amountPaisa, digit)}"
            )
        }

        val upcoming = container.loanRepository.observeUpcomingInstallments(Dates.millisOf(today.plusDays(7)), 5).first()
        for (inst in upcoming) {
            val loan = container.loanRepository.getLoan(inst.loanId) ?: continue
            val remaining = inst.scheduledPaisa - inst.paidPaisa
            if (remaining <= 0) continue
            notify(
                id = (100000 + inst.id).toInt(),
                title = "${loan.institution} কিস্তি",
                text = "বাকি ${Money.format(remaining, digit)} — নির্ধারিত ${Dates.format(Dates.localDateOf(inst.dueDateMillis), digit)}"
            )
        }

        val overdue = container.loanRepository.observeOverdueInstallments(System.currentTimeMillis(), 5).first()
        for (inst in overdue) {
            val loan = container.loanRepository.getLoan(inst.loanId) ?: continue
            val remaining = inst.scheduledPaisa - inst.paidPaisa
            if (remaining <= 0) continue
            notify(
                id = (200000 + inst.id).toInt(),
                title = "মেয়াদ পেরিয়েছে: ${loan.institution}",
                text = "কিস্তির বাকি ${Money.format(remaining, digit)}"
            )
        }

        val needingReminder = container.planningRepository.needingReminder(fromDay, toDay)
        for (item in needingReminder.take(5)) {
            val daysBefore = item.remindDaysBefore
            val due = Dates.localDateOf(item.nextDateMillis)
            val days = Dates.daysBetween(today, due)
            if (days in 0..daysBefore.coerceAtLeast(0)) {
                notify(
                    id = (300000 + item.id).toInt(),
                    title = item.name,
                    text = "$days দিন পর পরিশোধ — ${Money.format(item.amountPaisa, digit)}"
                )
            }
        }

        return Result.success()
    }

    private fun notify(id: Int, title: String, text: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, HisaabDeskApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        } catch (e: SecurityException) {
            // permission revoked mid-flight — ignore
        }
    }
}
