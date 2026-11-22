package dev.tasker2

import dev.tasker2.data.IntStore
import dev.tasker2.data.SettingsRepository
import dev.tasker2.domain.AppSettings
import dev.tasker2.domain.StrikeLine
import dev.tasker2.domain.TaskRowMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class Mem(val map: MutableMap<String, Int> = mutableMapOf()) : IntStore {
    override fun getInt(key: String, default: Int) = map[key] ?: default
    override fun putInts(values: Map<String, Int>) { map.putAll(values) }
}

class TaskRowMetricsTest {
    @Test fun defaultIsMiddleOfRangeAndKeepsOldLook() {
        val r = TaskRowMetrics.GAP_RANGE
        assertEquals(0, AppSettings.DEFAULT_TASK_GAP_DP)
        assertEquals(-r.first, r.last)
        val m = TaskRowMetrics.metrics(0)
        assertEquals(0f, m.listSpacingDp, 0f); assertEquals(4f, m.paddingDp, 0f); assertEquals(48f, m.targetDp, 0f)
        assertEquals(56f, m.minRowHeightDp, 0f)
    }

    @Test fun positiveGapOnlyAddsListSpacing() {
        val m = TaskRowMetrics.metrics(10)
        assertEquals(10f, m.listSpacingDp, 0f); assertEquals(56f, m.minRowHeightDp, 0f)
    }

    @Test fun negativeGapShrinksPaddingThenTargets() {
        val six = TaskRowMetrics.metrics(-6)
        assertEquals(0f, six.listSpacingDp, 0f); assertEquals(1f, six.paddingDp, 0f); assertEquals(48f, six.targetDp, 0f)
        val eight = TaskRowMetrics.metrics(-8)
        assertEquals(0f, eight.paddingDp, 0f); assertEquals(48f, eight.targetDp, 0f)
        val twelve = TaskRowMetrics.metrics(-12)
        assertEquals(0f, twelve.paddingDp, 0f); assertEquals(44f, twelve.targetDp, 0f)
    }

    @Test fun rowHeightNeverDropsBelowTallestControl() {
        // Tallest fixed elements: touch target, priority badge (28) / bar (32). The title line is handled by the layout (max).
        for (g in -100..100) {
            val m = TaskRowMetrics.metrics(g)
            assertTrue("gap $g", m.listSpacingDp >= 0f && m.paddingDp >= 0f)
            assertTrue("gap $g", m.targetDp >= TaskRowMetrics.MIN_TARGET_DP)
            assertTrue("gap $g", m.minRowHeightDp >= 40f && m.minRowHeightDp >= 32f)
            assertTrue("gap $g", m.targetDp >= TaskRowMetrics.CIRCLE_DP && m.targetDp >= TaskRowMetrics.CHECKBOX_DP)
        }
        assertEquals(40f, TaskRowMetrics.metrics(TaskRowMetrics.MIN_GAP_DP).minRowHeightDp, 0f)
    }

    @Test fun belowTheFloorIsClamped() {
        assertEquals(TaskRowMetrics.metrics(-16), TaskRowMetrics.metrics(-99))
        assertEquals(TaskRowMetrics.metrics(16), TaskRowMetrics.metrics(99))
        assertEquals(-16, AppSettings(taskGapDp = -99).normalized().taskGapDp)
        assertEquals(16, AppSettings(taskGapDp = 99).normalized().taskGapDp)
    }

    @Test fun labelHasSign() {
        assertEquals("−6 dp", TaskRowMetrics.label(-6))
        assertEquals("+6 dp", TaskRowMetrics.label(6))
        assertEquals("0 dp", TaskRowMetrics.label(0))
    }

    @Test fun legacyValuesMigrate() {
        // 1.5 stored 0..24: those that fit keep their meaning, larger ones clamp to the new maximum.
        assertEquals(0, SettingsRepository(Mem()).settings.value.taskGapDp)
        for (old in 0..16) assertEquals(old, SettingsRepository(Mem(mutableMapOf(SettingsRepository.KEY_TASK_GAP to old))).settings.value.taskGapDp)
        assertEquals(16, SettingsRepository(Mem(mutableMapOf(SettingsRepository.KEY_TASK_GAP to 24))).settings.value.taskGapDp)
        assertEquals(16, TaskRowMetrics.migrateLegacyGap(24))
    }

    @Test fun negativeValuePersistsAndResetGoesToZero() {
        val store = Mem()
        val repo = SettingsRepository(store)
        repo.update { it.copy(taskGapDp = -9) }
        assertEquals(-9, SettingsRepository(store).settings.value.taskGapDp)
        repo.update { it.copy(taskGapDp = -50) }
        assertEquals(-16, repo.settings.value.taskGapDp)
        repo.reset()
        assertEquals(0, SettingsRepository(store).settings.value.taskGapDp)
    }

    @Test fun notesAreThreeQuartersOfListFont() {
        assertEquals(17 * 0.75f, AppSettings().notesFontSp, 0.001f)
        assertEquals(24 * 0.75f, AppSettings(listFontSp = 24).notesFontSp, 0.001f)
    }

    @Test fun strikeSpansCircleLeftEdgeToCheckboxRightEdge() {
        // Row 400px wide, 48px touch targets: circle box at 8, checkbox box at 400-4-48=348; visible 22 and 20.
        val s = StrikeLine.span(8f, 48f, 22f, 348f, 48f, 20f)!!
        assertEquals(21f, s.start, 0f)    // 8 + (48-22)/2
        assertEquals(382f, s.end, 0f)     // 348 + (48+20)/2: right edge of the visible checkbox
        assertEquals(361f, s.length, 0f)
        assertTrue(s.end <= 400f - 4f)    // never past the checkbox slot
    }

    @Test fun strikeFollowsSmallerTouchTargets() {
        val s = StrikeLine.span(8f, 40f, 22f, 356f, 40f, 20f)!!
        assertEquals(17f, s.start, 0f); assertEquals(386f, s.end, 0f)
    }

    @Test fun strikeIsNullWhenRowTooNarrow() {
        assertNotNull(StrikeLine.span(8f, 48f, 22f, 100f, 48f, 20f))
        assertNull(StrikeLine.span(100f, 48f, 22f, 8f, 48f, 20f))
    }
}
