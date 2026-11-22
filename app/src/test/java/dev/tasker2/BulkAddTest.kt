package dev.tasker2

import dev.tasker2.domain.BulkState
import dev.tasker2.domain.TaskLogic
import dev.tasker2.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkAddTest {
    @Test fun startsWithTwoFieldsOnlyFirstEnabled() {
        val s = BulkState()
        assertEquals(2, s.entries.size)
        assertTrue(s.isEnabled(0)); assertFalse(s.isEnabled(1))
    }

    @Test fun nextFieldEnabledAndAppendedWhenTyping() {
        var s = BulkState().setTitle(0, "a")
        assertTrue(s.isEnabled(1))
        assertEquals(2, s.entries.size)
        s = s.setTitle(1, "b")
        assertEquals(3, s.entries.size)
        assertTrue(s.isEnabled(2)); assertFalse(s.isEnabled(3))
        // cannot fill 3 before 2
        assertEquals(s, s.setTitle(2 + 1, "x"))
    }

    @Test fun disabledFieldCannotBeSelected() {
        val s = BulkState().select(1)
        assertEquals(0, s.selected)
    }

    @Test fun clearingLastTrimsTrailingFieldsButKeepsMinimum() {
        var s = BulkState().setTitle(0, "a").setTitle(1, "b")
        s = s.setTitle(1, "")
        assertEquals(2, s.entries.size)
        s = s.setTitle(0, "")
        assertEquals(2, s.entries.size)
    }

    @Test fun middleFieldWithTextStaysEnabledAfterPreviousCleared() {
        val s = BulkState().setTitle(0, "a").setTitle(1, "b").setTitle(0, "")
        assertTrue(s.isEnabled(1))
    }

    @Test fun manyEntriesGrowOneAtATime() {
        var s = BulkState()
        for (i in 0 until 150) s = s.setTitle(i, "t$i")
        assertEquals(151, s.entries.size)
    }

    @Test fun notesAndPriorityRetainedPerTitle() {
        var s = BulkState().setTitle(0, "a").setNotes("n1").setPriority(9)
        s = s.setTitle(1, "b").select(1)
        assertEquals("", s.current.notes); assertEquals(0, s.current.priority)
        s = s.setNotes("n2").setPriority(2).select(0)
        assertEquals("n1", s.current.notes); assertEquals(9, s.current.priority)
        s = s.select(1)
        assertEquals("n2", s.current.notes); assertEquals(2, s.current.priority)
    }

    @Test fun emptyAndWhitespaceTitlesSkipped() {
        val s = BulkState().setTitle(0, "a").setTitle(1, "  ").setTitle(2, "c")
        // field 2 is disabled while 1 is blank, so it stays empty
        assertEquals(listOf("a"), TaskLogic.bulkTasksToCreate(s.entries).map { it.title })
        val t = BulkState().setTitle(0, "a").setTitle(1, "b").setTitle(0, " ")
        assertEquals(listOf("b"), TaskLogic.bulkTasksToCreate(t.entries).map { it.title })
    }
}

class BulkAddFocusTest {
    @Test fun enterMovesToNextTitleOnlyWhenThisOneHasText() {
        val empty = BulkState()
        assertEquals(null, empty.nextFocusIndex(0))          // blank title: next field is disabled
        val s = empty.setTitle(0, "a")
        assertEquals(1, s.nextFocusIndex(0))                 // next field exists (auto-appended) and is enabled
        assertEquals(null, s.nextFocusIndex(1))              // blank last field: nowhere to go
        assertEquals(null, s.nextFocusIndex(5))              // out of range
        assertEquals(2, s.setTitle(1, "b").nextFocusIndex(1))
    }

    @Test fun focusRegionIncludesNeighbours() {
        val (top, bottom) = BulkState.focusRegion(56f, 64f)
        assertEquals(-64f, top, 0f)
        assertEquals(120f, bottom, 0f)
    }
}
