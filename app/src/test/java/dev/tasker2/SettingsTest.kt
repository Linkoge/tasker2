package dev.tasker2

import dev.tasker2.data.IntStore
import dev.tasker2.data.SettingsRepository
import dev.tasker2.domain.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemStore(val map: MutableMap<String, Int> = mutableMapOf()) : IntStore {
    override fun getInt(key: String, default: Int) = map[key] ?: default
    override fun putInts(values: Map<String, Int>) { map.putAll(values) }
}

class SettingsTest {
    @Test fun defaults() {
        val s = AppSettings()
        assertEquals(15, s.tabFontSp); assertEquals(17, s.listFontSp); assertEquals(6, s.tabGapSpaces)
        assertEquals(AppSettings(), SettingsRepository(MemStore()).settings.value)
    }

    @Test fun rangesClamp() {
        val low = AppSettings(0, 0, 0).normalized()
        assertEquals(11, low.tabFontSp); assertEquals(12, low.listFontSp); assertEquals(1, low.tabGapSpaces)
        val high = AppSettings(99, 99, 99).normalized()
        assertEquals(24, high.tabFontSp); assertEquals(24, high.listFontSp); assertEquals(12, high.tabGapSpaces)
    }

    @Test fun notesScaleWithTitle() {
        assertEquals(13f, AppSettings().notesFontSp, 0.001f)
        assertEquals(24 * 13f / 17f, AppSettings(listFontSp = 24).notesFontSp, 0.001f)
    }

    @Test fun updatePersistsAndAppliesImmediately() {
        val store = MemStore()
        val repo = SettingsRepository(store)
        repo.update { it.copy(tabFontSp = 20, tabGapSpaces = 3) }
        assertEquals(20, repo.settings.value.tabFontSp)
        // A new repository over the same storage sees the saved values.
        val again = SettingsRepository(store).settings.value
        assertEquals(AppSettings(tabFontSp = 20, listFontSp = 17, tabGapSpaces = 3), again)
    }

    @Test fun outOfRangeValuesAreClampedWhenSavedAndLoaded() {
        val repo = SettingsRepository(MemStore())
        repo.update { it.copy(listFontSp = 100) }
        assertEquals(24, repo.settings.value.listFontSp)
        val bad = MemStore(mutableMapOf(SettingsRepository.KEY_TAB_GAP to -5))
        assertEquals(1, SettingsRepository(bad).settings.value.tabGapSpaces)
    }

    @Test fun resetRestoresDefaultsAndPersists() {
        val store = MemStore()
        val repo = SettingsRepository(store)
        repo.update { AppSettings(24, 24, 12) }
        repo.reset()
        assertEquals(AppSettings(), repo.settings.value)
        assertEquals(AppSettings(), SettingsRepository(store).settings.value)
    }

    @Test fun newSettingsDefaultsPersistenceAndReset() {
        val d = AppSettings()
        assertEquals(0, d.taskGapDp); assertTrue(d.showPriorityNumber); assertTrue(d.showPriorityBar)
        val store = MemStore()
        val repo = SettingsRepository(store)
        repo.update { it.copy(taskGapDp = 10, showPriorityNumber = false, showPriorityBar = false) }
        val again = SettingsRepository(store).settings.value
        assertEquals(10, again.taskGapDp); assertFalse(again.showPriorityNumber); assertFalse(again.showPriorityBar)
        repo.update { it.copy(taskGapDp = 99) }
        assertEquals(24, repo.settings.value.taskGapDp)
        repo.update { it.copy(taskGapDp = -3) }
        assertEquals(0, repo.settings.value.taskGapDp)
        repo.reset()
        assertEquals(AppSettings(), SettingsRepository(store).settings.value)
    }
}
