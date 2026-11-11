package com.sjpixels.innerlotus.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.MainActivity
import com.sjpixels.innerlotus.R
import com.sjpixels.innerlotus.audio.SoundService
import com.sjpixels.innerlotus.content.Affirm
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import java.util.Calendar

/** Daily local reminders (no internet, no exact-alarm permission needed). */
object Reminders {
    const val CHANNEL = "reminders"
    const val EXTRA_KIND = "kind"
    const val EXTRA_OPEN = "open"
    private const val ACTION = "com.sjpixels.innerlotus.REMIND"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
        SoundService.ensureChannel(context)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Re-applies both reminders from settings. */
    fun apply(context: Context) {
        val s = App.settings
        if (s.remindAffirm) schedule(context, "affirm", s.remindAffirmAt) else cancel(context, "affirm")
        if (s.remindJournal) schedule(context, "journal", s.remindJournalAt) else cancel(context, "journal")
    }

    private fun pending(context: Context, kind: String): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).setAction("$ACTION.$kind").putExtra(EXTRA_KIND, kind)
        return PendingIntent.getBroadcast(context, kind.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun schedule(context: Context, kind: String, minuteOfDay: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending(context, kind))
    }

    private fun cancel(context: Context, kind: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, kind))
    }

    fun show(context: Context, kind: String) {
        if (!canNotify(context)) return
        I18n.ensureLoaded(context, App.settings.lang.ifEmpty { I18n.lang.code })
        val (title, text, open) = if (kind == "journal") {
            Triple(tr("ui.remind.journal_title"), tr("ui.remind.journal_text"), "journal")
        } else {
            val card = Affirm.daily()
            Triple(tr("ui.remind.affirm_title"), card.main, "daily")
        }
        val tap = PendingIntent.getActivity(
            context, kind.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN, open).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(kind.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        App.init(context.applicationContext)
        Reminders.show(context, intent.getStringExtra(Reminders.EXTRA_KIND) ?: "affirm")
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            App.init(context.applicationContext)
            Reminders.apply(context)
        }
    }
}
