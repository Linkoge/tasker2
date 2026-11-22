package dev.tasker2

import dev.tasker2.ui.TabLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class TabLayoutTest {
    @Test fun gapIsThreeSpaces() {
        assertEquals("   ", TabLayout.gapText())
        assertEquals(15, TabLayout.FONT_SP)
    }
}
