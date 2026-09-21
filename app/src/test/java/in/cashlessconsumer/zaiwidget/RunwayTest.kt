package `in`.cashlessconsumer.zaiwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunwayTest {

    private val now = 1_700_000_000_000L

    private fun snap(
        fivePct: Int? = null,
        fiveResetIn: Long? = null,
        weekPct: Int? = null,
        toolsPct: Int? = null,
    ): Snapshot {
        val limits = buildList {
            fivePct?.let { add(Limit(3, it, fiveResetIn?.let { t -> now + t })) }
            weekPct?.let { add(Limit(6, it, null)) }
            toolsPct?.let { add(Limit(5, it, null)) }
        }
        return Snapshot(Quota(limits), Resets(emptyList(), emptyList()), now)
    }

    @Test
    fun `etaHours is infinite before any usage`() {
        assertTrue(Runway.etaHours(0.0, 1.0, 5.0) == Double.POSITIVE_INFINITY)
    }

    @Test
    fun `etaHours is zero when quota exhausted or window nearly over`() {
        assertEquals(0.0, Runway.etaHours(100.0, 1.0, 5.0)!!, 1e-9)
        assertEquals(0.0, Runway.etaHours(20.0, 0.1, 5.0)!!, 1e-9)
    }

    @Test
    fun `etaHours caps at remaining window`() {
        // 50% used in 1h → burn rate 50%/h → 1h of run-left left
        assertEquals(1.0, Runway.etaHours(50.0, 1.0, 5.0)!!, 1e-9)
        // 10% used in 0.5h → naive 47.5h, capped at 4.5h
        assertEquals(4.5, Runway.etaHours(10.0, 0.5, 5.0)!!, 1e-9)
    }

    @Test
    fun `hoursInto returns null when reset missing or stale`() {
        assertNull(Runway.hoursInto(null, now))
        assertNull(Runway.hoursInto(now - 3_600_000, now)) // reset an hour ago
    }

    @Test
    fun `hoursInto measures elapsed window from reset timestamp`() {
        // reset in 2.5h → 2.5h into the window
        assertEquals(2.5, Runway.hoursInto(now + 2 * 3_600_000 + 1_800_000, now)!!, 1e-9)
    }

    @Test
    fun `view picks limits by unit and formats packs note`() {
        val v = Runway.view(snap(fivePct = 47, fiveResetIn = 133 * 60_000, weekPct = 12, toolsPct = 38))
        assertEquals(47, v.fivePct!!)
        assertEquals(12, v.weekPct!!)
        assertEquals(38, v.toolsPct!!)
        assertNull(v.packsNote)
    }

    @Test
    fun `view notes available reset packs`() {
        val s = Snapshot(
            Quota(listOf(Limit(3, 47, now + 133 * 60_000))),
            Resets(listOf(ResetPack(true, now + 47 * 86_400_000L)), emptyList()),
            now,
        )
        assertTrue(Runway.view(s).packsNote!!.contains("1 reset pack"))
    }

    @Test
    fun `needle sweeps a full circle across the window`() {
        val v = Runway.view(snap(fivePct = 1, fiveResetIn = 5 * 3_600_000))
        assertEquals(0f, v.needleDegrees(now))
        val v2 = Runway.view(snap(fivePct = 1, fiveResetIn = 2 * 3_600_000 + 1_800_000))
        assertEquals(180f, v2.needleDegrees(now))
    }
}
