package com.sjpixels.innerlotus.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads on-device app usage from Android's UsageStatsManager (requires the user to grant
 * "Usage access"). Nothing is stored or sent anywhere; results are computed on demand.
 */
object UsageRepo {

    data class AppUsage(val pkg: String, val label: String, val millis: Long)

    private const val RESUMED = 1 // UsageEvents.Event.ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
    private const val PAUSED = 2  // UsageEvents.Event.ACTIVITY_PAUSED / MOVE_TO_BACKGROUND

    private val labelCache = HashMap<String, String>()
    private val iconCache = HashMap<String, Bitmap?>()

    fun hasPermission(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openPermissionSettings(context: Context) {
        val withPkg = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + context.packageName))
        val plain = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        for (i in listOf(withPkg, plain)) {
            try {
                context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return
            } catch (_: Exception) {
            }
        }
    }

    fun startOfDay(day: LocalDate): Long = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Foreground time per app between [start] and [end] (epoch millis). */
    fun usage(context: Context, start: Long, end: Long): List<AppUsage> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val totals = HashMap<String, Long>()
        val now = System.currentTimeMillis()
        val until = minOf(end, now)
        val days = (until - start) / 86_400_000L
        if (days <= 7) {
            val events = usm.queryEvents(start, until)
            val ev = UsageEvents.Event()
            val openSince = HashMap<String, Long>()
            val seen = HashSet<String>()
            while (events.hasNextEvent()) {
                events.getNextEvent(ev)
                val pkg = ev.packageName ?: continue
                when (ev.eventType) {
                    RESUMED -> { if (!openSince.containsKey(pkg)) openSince[pkg] = ev.timeStamp; seen.add(pkg) }
                    PAUSED -> {
                        val from = openSince.remove(pkg) ?: if (pkg !in seen) start else null
                        seen.add(pkg)
                        if (from != null) totals[pkg] = (totals[pkg] ?: 0L) + (ev.timeStamp - from).coerceAtLeast(0L)
                    }
                }
            }
            for ((pkg, from) in openSince) totals[pkg] = (totals[pkg] ?: 0L) + (until - from).coerceAtLeast(0L)
        } else {
            val stats = usm.queryAndAggregateUsageStats(start, until)
            for ((pkg, s) in stats) if (s.totalTimeInForeground > 0) totals[pkg] = s.totalTimeInForeground
        }
        val home = homePackage(context)
        return totals.filter { (pkg, ms) -> ms >= 30_000 && pkg != home && isUserApp(pm, pkg) }
            .map { (pkg, ms) -> AppUsage(pkg, label(pm, pkg), ms) }
            .sortedByDescending { it.millis }
    }

    /** Total screen time for each of the last [n] days, oldest first. */
    fun dailyTotals(context: Context, n: Int): List<Pair<LocalDate, Long>> {
        val today = LocalDate.now()
        return (n - 1 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            val s = startOfDay(d)
            d to usage(context, s, s + 86_400_000L).sumOf { it.millis }
        }
    }

    private fun isUserApp(pm: PackageManager, pkg: String): Boolean =
        try { pm.getLaunchIntentForPackage(pkg) != null } catch (_: Exception) { false }

    private fun homePackage(context: Context): String? = try {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    } catch (_: Exception) { null }

    fun label(pm: PackageManager, pkg: String): String = labelCache.getOrPut(pkg) {
        try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { pkg }
    }

    fun icon(context: Context, pkg: String): Bitmap? = iconCache.getOrPut(pkg) {
        try { context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96) } catch (_: Exception) { null }
    }

    fun formatDuration(ms: Long, h: String, m: String): String {
        val totalMin = ms / 60_000
        val hours = totalMin / 60
        val mins = totalMin % 60
        return if (hours > 0) "$hours$h $mins$m" else "$mins$m"
    }
}
