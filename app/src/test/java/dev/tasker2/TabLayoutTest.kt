package dev.tasker2

import dev.tasker2.domain.AppSettings
import dev.tasker2.ui.TabLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class TabLayoutTest {
    @Test fun defaultGapIsSixSpaces() {
        assertEquals("      ", TabLayout.gapText())
        assertEquals(6, TabLayout.GAP_SPACES)
        assertEquals(6, AppSettings().tabGapSpaces)
        assertEquals(15, TabLayout.FONT_SP)
    }

    @Test fun gapTextFollowsSetting() {
        assertEquals(" ", TabLayout.gapText(1))
        assertEquals(12, TabLayout.gapText(12).length)
    }
}
