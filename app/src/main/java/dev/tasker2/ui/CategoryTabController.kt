package dev.tasker2.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.tasker2.data.Category
import dev.tasker2.domain.CategoryMenu

/** The repository/ViewModel operations the tab menu reuses (the same ones behind Manage categories). */
interface CategoryOps {
    fun move(from: Int, to: Int)
    fun rename(category: Category, name: String)
    fun delete(category: Category)
    /** Adds at the end; [onAdded] receives the new category's id. */
    fun add(name: String, onAdded: (Long) -> Unit)
}

/** Logic behind the tab menu and the "+" button. Deleting only ever happens through [requestDelete] then [confirmDelete]. */
class CategoryTabController(
    private val ops: CategoryOps,
    private val selected: () -> Long,
    private val select: (Long) -> Unit,
) {
    /** The category waiting for the user's delete confirmation; null when no dialog is showing. */
    var pendingDelete: Category? by mutableStateOf(null)
        private set

    fun moveLeft(categories: List<Category>, index: Int): Boolean {
        if (!CategoryMenu.canMoveLeft(index) || index !in categories.indices) return false
        ops.move(index, index - 1); return true
    }

    fun moveRight(categories: List<Category>, index: Int): Boolean {
        if (!CategoryMenu.canMoveRight(index, categories.size)) return false
        ops.move(index, index + 1); return true
    }

    /** false (and nothing changes) for a blank name. */
    fun rename(category: Category, raw: String): Boolean {
        val name = CategoryMenu.validName(raw) ?: return false
        ops.rename(category, name); return true
    }

    /** New category goes last and becomes selected; false for a blank name. */
    fun add(raw: String): Boolean {
        val name = CategoryMenu.validName(raw) ?: return false
        ops.add(name) { id -> select(id) }
        return true
    }

    fun requestDelete(category: Category) { pendingDelete = category }
    fun cancelDelete() { pendingDelete = null }

    /** Deletes the pending category (if any) and picks a neighbouring tab. */
    fun confirmDelete(categories: List<Category>): Boolean {
        val c = pendingDelete ?: return false
        pendingDelete = null
        val next = CategoryMenu.selectionAfterDelete(categories, c.id, selected())
        ops.delete(c)
        if (next != null && next != selected()) select(next)
        return true
    }
}
