package com.cooreo.sippy.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cooreo.sippy.MainActivity
import com.cooreo.sippy.R
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.data.readStoredState
import com.cooreo.sippy.util.Dates
import com.cooreo.sippy.domain.water.WaterMath
import java.util.concurrent.TimeUnit

/**
 * Reminders (brief): off by default, at most 4 a day, scheduled with
 * WorkManager, permission requested only when the user turns them on, and a
 * reminder is skipped when the goal is already met.
 */
object ReminderScheduler {

    private const val PREFIX = "sippy-reminder-"

    fun sync(context: Context, settings: Settings) {
        val workManager = WorkManager.getInstance(context)
        for (slot in 0 until Settings.MAX_REMINDERS) {
            workManager.cancelUniqueWork("$PREFIX$slot")
        }
        if (!settings.remindersEnabled) return
        Notifications.ensureChannel(context)
        val times = Dates.sanitizeTimes(settings.reminderTimes).take(Settings.MAX_REMINDERS)
        times.forEachIndexed { slot, hhmm ->
            val minutes = Dates.minutesUntil(hhmm).coerceAtLeast(1L)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(ReminderWorker.KEY_SLOT to slot))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build(),
                )
                .addTag("sippy-reminders")
                .build()
            workManager.enqueueUniquePeriodicWork(
                "$PREFIX$slot",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
        }
    }
}

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Read the document directly: a worker has no Activity and must not
        // depend on the process-wide graph still being alive.
        val state = readStoredState(applicationContext)
        if (!state.settings.remindersEnabled) return Result.success()

        val today = Dates.todayEpochDay()
        val todayEntries = state.log.filter { it.dayEpoch == today }
        val hydrated = WaterMath.hydratedMl(todayEntries)
        if (WaterMath.goalMet(hydrated, state.settings.goalMl)) return Result.success()

        val slot = inputData.getInt(KEY_SLOT, 0)
        Notifications.showReminder(applicationContext, slot, hydrated, state.settings.goalMl)
        return Result.success()
    }

    companion object {
        const val KEY_SLOT = "slot"
    }
}

object Notifications {

    private const val CHANNEL_ID = "sippy-reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private val lines = listOf(
        "Time for a sip — %d of %d ml so far.",
        "Your water is waiting. %d / %d ml today.",
        "A small glass now keeps the streak alive. %d / %d ml.",
        "Dippy is thirsty. %d of %d ml done.",
    )

    fun showReminder(context: Context, slot: Int, hydratedMl: Int, goalMl: Int) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = String.format(lines[slot % lines.size], hydratedMl, goalMl)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            ) {
                NotificationManagerCompat.from(context).notify(1000 + slot, notification)
            }
        } catch (t: SecurityException) {
            // Permission revoked between enabling and firing: drop quietly.
        }
    }
}
