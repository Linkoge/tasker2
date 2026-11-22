package dev.tasker2

import dev.tasker2.data.Category
import dev.tasker2.domain.CategoryMenu
import dev.tasker2.ui.CategoryOps
import dev.tasker2.ui.CategoryTabController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class TabFakeOps : CategoryOps {
    val log = mutableListOf<String>()
    var nextId = 100L
    override fun move(from: Int, to: Int) { log += "move $from $to" }
    override fun rename(category: Category, name: String) { log += "rename ${category.id} $name" }
    override fun delete(category: Category) { log += "delete ${category.id}" }
    override fun add(name: String, onAdded: (Long) -> Unit) { log += "add $name"; onAdded(nextId++) }
}

class CategoryTabMenuTest {
    private val cats = listOf(Category(1, "A", 0), Category(2, "B", 1), Category(3, "C", 2))
    private var selected = 2L
    private val ops = TabFakeOps()
    private val ctl = CategoryTabController(ops, { selected }, { selected = it })

    @Test fun moveAtTheEndsIsRefused() {
        assertFalse(ctl.moveLeft(cats, 0))
        assertFalse(ctl.moveRight(cats, 2))
        assertTrue(ops.log.isEmpty())
        assertFalse(CategoryMenu.canMoveLeft(0)); assertTrue(CategoryMenu.canMoveLeft(1))
        assertTrue(CategoryMenu.canMoveRight(1, 3)); assertFalse(CategoryMenu.canMoveRight(2, 3))
        assertFalse(CategoryMenu.canMoveRight(0, 1))
    }

    @Test fun moveInTheMiddleUsesNeighbourIndex() {
        assertTrue(ctl.moveLeft(cats, 1)); assertTrue(ctl.moveRight(cats, 1))
        assertEquals(listOf("move 1 0", "move 1 2"), ops.log)
    }

    @Test fun renameRejectsBlankAndKeepsUnicode() {
        assertFalse(ctl.rename(cats[0], "   ")); assertFalse(ctl.rename(cats[0], ""))
        assertTrue(ops.log.isEmpty())
        assertTrue(ctl.rename(cats[0], "  საქმეები  "))
        assertEquals(listOf("rename 1 საქმეები"), ops.log)
        assertNull(CategoryMenu.validName(" \t"))
    }

    @Test fun deleteHappensOnlyAfterConfirmation() {
        ctl.requestDelete(cats[1])
        assertEquals(cats[1], ctl.pendingDelete)
        assertTrue(ops.log.isEmpty())
        ctl.cancelDelete()
        assertNull(ctl.pendingDelete)
        assertFalse(ctl.confirmDelete(cats))   // nothing pending: nothing deleted
        assertTrue(ops.log.isEmpty())
        ctl.requestDelete(cats[1])
        assertTrue(ctl.confirmDelete(cats))
        assertEquals(listOf("delete 2"), ops.log)
        assertNull(ctl.pendingDelete)
    }

    @Test fun deleteSelectsNeighbour() {
        // selected B (middle): right neighbour C
        selected = 2; ctl.requestDelete(cats[1]); ctl.confirmDelete(cats); assertEquals(3L, selected)
        // selected C (last): left neighbour B
        selected = 3; ctl.requestDelete(cats[2]); ctl.confirmDelete(cats); assertEquals(2L, selected)
        // deleting another tab keeps the selection
        selected = 1; ctl.requestDelete(cats[2]); ctl.confirmDelete(cats); assertEquals(1L, selected)
        assertNull(CategoryMenu.selectionAfterDelete(listOf(cats[0]), 1, 1))
    }

    @Test fun deleteMessageMentionsTaskCount() {
        assertTrue(CategoryMenu.deleteMessage("Daily", 5).contains("5 tasks"))
        assertTrue(CategoryMenu.deleteMessage("Daily", 1).contains("1 task "))
        assertTrue(CategoryMenu.deleteMessage("Daily", 5).contains("Daily"))
    }

    @Test fun plusAddsAtEndAndSelectsIt() {
        assertFalse(ctl.add("  ")); assertTrue(ops.log.isEmpty())
        assertTrue(ctl.add(" New "))
        assertEquals(listOf("add New"), ops.log)
        assertEquals(100L, selected)
    }
}
