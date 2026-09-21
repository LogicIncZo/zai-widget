package `in`.cashlessconsumer.zaiwidget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun `newer patch version detected`() {
        assertTrue(UpdateChecker.isNewer("0.1.1", "v0.1.2"))
    }

    @Test
    fun `newer minor and major detected`() {
        assertTrue(UpdateChecker.isNewer("0.1.1", "v0.2.0"))
        assertTrue(UpdateChecker.isNewer("0.9.9", "v1.0.0"))
    }

    @Test
    fun `same or older version not flagged`() {
        assertFalse(UpdateChecker.isNewer("0.2.0", "v0.2.0"))
        assertFalse(UpdateChecker.isNewer("0.2.0", "v0.1.9"))
        assertFalse(UpdateChecker.isNewer("1.0.0", "v0.9.0"))
    }

    @Test
    fun `handles missing segments`() {
        assertTrue(UpdateChecker.isNewer("0.1", "v0.1.1"))
        assertFalse(UpdateChecker.isNewer("0.1.1", "v0.1"))
    }
}
