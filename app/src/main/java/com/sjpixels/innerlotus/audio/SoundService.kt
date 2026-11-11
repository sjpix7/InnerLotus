package com.sjpixels.innerlotus.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.sjpixels.innerlotus.MainActivity
import com.sjpixels.innerlotus.R
import com.sjpixels.innerlotus.core.tr

/** Keeps Sound Healing playing with the screen off and shows a "Stop" notification. */
class SoundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            SoundEngine.stopAll()
            stopSelf()
            return START_NOT_STICKY
        }
        ensureChannel(this)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, SoundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(tr("ui.sound.notif_title"))
            .setContentText(SoundEngine.currentMixName.ifEmpty { tr("ui.sound.notif_text") })
            .setContentIntent(open)
            .addAction(0, tr("ui.common.stop"), stop)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, n, type)
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "InnerLotus:sound").apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L) // safety cap: 4 hours
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing if the user swipes the app away only while a sleep timer is set.
        if (SoundEngine.sleepEndsAt == 0L) {
            SoundEngine.stopAll()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val CHANNEL = "sound"
        const val NOTIF_ID = 108
        const val ACTION_STOP = "com.sjpixels.innerlotus.STOP_SOUND"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, context.getString(R.string.sound_channel_name), NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }
}
