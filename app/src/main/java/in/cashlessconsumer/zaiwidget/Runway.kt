package `in`.cashlessconsumer.zaiwidget

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Pure runway math, ported from the zai-usage CLI (etaHours + Z8 time handling). */
object Runway {
    private val Z8 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    data class View(
        val fivePct: Int?,
        val fiveResetAt: Long?,
        val weekPct: Int?,
        val monthlyPct: Int?,
        val toolsPct: Int?,
        val packsNote: String?,
    ) {
        val fiveWindowHours: Double = 5.0
        fun fiveEtaHours(now: Long): Double? {
            val pct = fivePct?.toDouble() ?: return null
            val into = hoursInto(fiveResetAt, now) ?: return null
            return etaHours(pct, into, fiveWindowHours)
        }

        /** Needle angle in degrees, 0 = pointing up (reset imminent), sweeping clockwise over the window. */
        fun needleDegrees(now: Long): Float {
            val into = hoursInto(fiveResetAt, now) ?: return 0f
            return (into.coerceIn(0.0, fiveWindowHours) / fiveWindowHours * 360.0).toFloat()
        }
    }

    /** Z.ai epoch-ms reset timestamp = end of the current 5-hour window. */
    fun hoursInto(resetAt: Long?, now: Long): Double? {
        resetAt ?: return null
        val left = (resetAt - now) / 3_600_000.0
        if (left < -0.5) return null // stale reset time
        return (5.0 - left.coerceAtLeast(0.0)).coerceAtMost(5.0)
    }

    /** Port of zai-usage etaHours: hours of run-left at the current burn rate. */
    fun etaHours(pctUsed: Double, hoursIntoWindow: Double, windowHours: Double): Double? {
        if (pctUsed <= 0) return Double.POSITIVE_INFINITY
        if (pctUsed >= 100 || hoursIntoWindow <= 0.25) return 0.0
        val rate = pctUsed / hoursIntoWindow
        if (rate <= 0) return null
        return ((100.0 - pctUsed) / rate).coerceAtMost(windowHours - hoursIntoWindow)
    }

    fun view(snap: Snapshot): View {
        val limits = snap.quota.limits
        val five = limits.firstOrNull { it.unit == 3 }
        val week = limits.firstOrNull { it.unit == 6 }
        val month = limits.firstOrNull { it.unit == 4 }
        val tools = limits.firstOrNull { it.unit == 5 }
        val packs = snap.resets.fiveHourResets.count { it.available } +
            snap.resets.weekResets.count { it.available }
        return View(
            fivePct = five?.percentage,
            fiveResetAt = five?.nextResetTime,
            weekPct = week?.percentage,
            monthlyPct = month?.percentage,
            toolsPct = tools?.percentage,
            packsNote = if (packs > 0) "$packs reset pack${if (packs == 1) "" else "s"}" else null,
        )
    }

    fun parseZ8(s: String?): Long? {
        s ?: return null
        return try {
            LocalDateTime.parse(s.trim().take(19), Z8).toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    fun countDown(resetAt: Long?, now: Long): String? {
        resetAt ?: return null
        val mins = ((resetAt - now) / 60_000).toInt()
        if (mins <= 0) return "resetting…"
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
