package dev.tasker2

import dev.tasker2.data.IntStore
import dev.tasker2.data.SettingsRepository
import dev.tasker2.domain.AppSettings
import dev.tasker2.domain.CrossedOutStyle
import dev.tasker2.domain.TaskLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class StyleMemStore(val map: MutableMap<String, Int> = mutableMapOf()) : IntStore {
    override fun getInt(key: String, default: Int) = map[key] ?: default
    override fun putInts(values: Map<String, Int>) { map.putAll(values) }
}

class CrossedStyleTest {
    private fun sat(argb: Int): Float {
        val r = ((argb shr 16) and 0xFF) / 255f; val g = ((argb shr 8) and 0xFF) / 255f; val b = (argb and 0xFF) / 255f
        val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
        val l = (mx + mn) / 2
        return if (mx == mn) 0f else (mx - mn) / (1 - Math.abs(2 * l - 1))   // HSL saturation
    }

    private fun colors(p: TaskLogic.CrossedPalette) = listOf(p.text, p.accent, p.badgeFill, p.barTrack, p.checkFill, p.checkmark)

    @Test fun grayModeIsPureGrayForEveryPriority() {
        for (p in 0..10) assertTrue(colors(TaskLogic.crossedPalette(p, CrossedOutStyle.GRAY)).all(TaskLogic.Gray::isGray))
    }

    @Test fun fadedModeIsLowSaturationButRecognisable() {
        for (p in 1..10) {
            val pal = TaskLogic.crossedPalette(p, CrossedOutStyle.FADED)
            for (c in listOf(pal.accent, pal.checkFill)) assertTrue("p=$p sat=${sat(c)}", sat(c) <= 0.25f)
            assertTrue(TaskLogic.Gray.isGray(pal.text))   // title stays gray
            assertTrue(TaskLogic.Gray.isGray(pal.barTrack))
        }
        val red = TaskLogic.crossedPalette(10, CrossedOutStyle.FADED).accent
        assertFalse(TaskLogic.Gray.isGray(red))
        assertTrue(((red shr 16) and 0xFF) > ((red shr 8) and 0xFF))   // reddish, dull
        assertTrue(sat(red) > 0.1f)
    }

    @Test fun fadedWithoutPriorityStaysGray() {
        assertTrue(colors(TaskLogic.crossedPalette(0, CrossedOutStyle.FADED)).all(TaskLogic.Gray::isGray))
    }

    @Test fun thicknessDefaultRangeAndPersistence() {
        assertEquals(CrossedOutStyle.FADED, AppSettings().crossedOutStyle)
        assertEquals(1f, AppSettings().strikeThicknessDp, 0f)
        assertEquals(1f, AppSettings(strikeHalfDp = -5).normalized().strikeThicknessDp, 0f)
        assertEquals(6f, AppSettings(strikeHalfDp = 99).normalized().strikeThicknessDp, 0f)
        assertEquals(2..12, AppSettings.STRIKE_HALF_RANGE)
        val store = StyleMemStore()
        val repo = SettingsRepository(store)
        repo.update { it.copy(strikeHalfDp = 7, crossedOutStyle = CrossedOutStyle.FADED) }
        val again = SettingsRepository(store).settings.value
        assertEquals(3.5f, again.strikeThicknessDp, 0f)
        assertEquals(CrossedOutStyle.FADED, again.crossedOutStyle)
        repo.reset()
        val reset = SettingsRepository(store).settings.value
        assertEquals(AppSettings(), reset)
        assertEquals(1f, reset.strikeThicknessDp, 0f)
    }

    @Test fun unknownStoredStyleFallsBackToGray() {
        assertEquals(CrossedOutStyle.FADED, SettingsRepository(StyleMemStore(mutableMapOf(SettingsRepository.KEY_CROSSED_STYLE to 9))).settings.value.crossedOutStyle)
    }
}
