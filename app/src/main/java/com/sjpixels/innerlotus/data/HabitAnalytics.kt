package com.sjpixels.innerlotus.data

import java.time.LocalDate

/** Pure analytics over [HabitState]. Days are epoch days (LocalDate.toEpochDay()). */
object HabitAnalytics {

    fun isScheduled(h: Habit, day: Long): Boolean {
        if (day < h.created) return false
        if (h.days.isEmpty()) return true
        return LocalDate.ofEpochDay(day).dayOfWeek.value in h.days
    }

    fun count(s: HabitState, h: Habit, day: Long): Int = s.logs[h.id]?.get(day) ?: 0

    fun isDone(s: HabitState, h: Habit, day: Long): Boolean = count(s, h, day) >= h.target

    /** Consecutive scheduled days completed, ending today (today is skipped if not done yet). */
    fun currentStreak(s: HabitState, h: Habit, today: Long): Int {
        var day = today
        if (isScheduled(h, day) && !isDone(s, h, day)) day -= 1
        var streak = 0
        var guard = 0
        while (day >= h.created && guard < 3660) {
            if (isScheduled(h, day)) {
                if (isDone(s, h, day)) streak++ else break
            }
            day--; guard++
        }
        return streak
    }

    fun bestStreak(s: HabitState, h: Habit, today: Long): Int {
        var best = 0
        var cur = 0
        var day = h.created
        while (day <= today) {
            if (isScheduled(h, day)) {
                if (isDone(s, h, day)) { cur++; if (cur > best) best = cur } else if (day != today) cur = 0
            }
            day++
        }
        return best
    }

    /** Completion rate in [0,1] over the last [days] days (only scheduled days count). */
    fun completionRate(s: HabitState, h: Habit, today: Long, days: Int): Float {
        var scheduled = 0
        var done = 0
        for (d in (today - days + 1)..today) {
            if (isScheduled(h, d)) {
                scheduled++
                if (isDone(s, h, d)) done++
            }
        }
        return if (scheduled == 0) 0f else done.toFloat() / scheduled
    }

    fun totalCompletions(s: HabitState, h: Habit): Int =
        s.logs[h.id].orEmpty().count { (day, n) -> n >= h.target && day >= h.created }

    /** Overall fraction of scheduled habits completed on a day; null when nothing was scheduled. */
    fun dayRate(s: HabitState, day: Long): Float? {
        val scheduled = s.habits.filter { isScheduled(it, day) }
        if (scheduled.isEmpty()) return null
        return scheduled.count { isDone(s, it, day) }.toFloat() / scheduled.size
    }

    /** Last [n] days ending today, oldest first. */
    fun dailyRates(s: HabitState, today: Long, n: Int): List<Pair<Long, Float?>> =
        ((today - n + 1)..today).map { it to dayRate(s, it) }

    /** Average completion by weekday (index 0 = Monday) over the last [days] days. */
    fun weekdayPattern(s: HabitState, today: Long, days: Int = 56): List<Float> {
        val sums = FloatArray(7)
        val counts = IntArray(7)
        for (d in (today - days + 1)..today) {
            val r = dayRate(s, d) ?: continue
            val wd = LocalDate.ofEpochDay(d).dayOfWeek.value - 1
            sums[wd] += r; counts[wd]++
        }
        return List(7) { if (counts[it] == 0) 0f else sums[it] / counts[it] }
    }

    fun perfectDays(s: HabitState, today: Long, days: Int): Int =
        ((today - days + 1)..today).count { dayRate(s, it) == 1f }
}
