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
        assertEquals("", s.current.notes); assertEquals(Task.DEFAULT_PRIORITY, s.current.priority)
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
