package dev.tasker2.domain

import dev.tasker2.data.Task

/** One row of the bulk-add screen: title plus its own notes and priority. */
data class BulkEntry(
    val title: String = "",
    val notes: String = "",
    val priority: Int = Task.DEFAULT_PRIORITY,
)

/** Bulk-add form state. Pure and Android-free so it can be unit tested. */
data class BulkState(
    val entries: List<BulkEntry> = List(MIN_FIELDS) { BulkEntry() },
    val selected: Int = 0,
) {
    val current: BulkEntry get() = entries[selected]

    /** Field [i] is enabled when the previous title has text, or it has text itself. Field 0 always is. */
    fun isEnabled(i: Int): Boolean =
        i in entries.indices && (i == 0 || entries[i - 1].title.isNotBlank() || entries[i].title.isNotBlank())

    /** Focus/select title [i]; ignored for disabled fields. Notes and priority live per entry, so switching keeps them. */
    fun select(i: Int): BulkState = if (isEnabled(i)) copy(selected = i) else this

    fun setTitle(i: Int, title: String): BulkState =
        if (!isEnabled(i)) this else update(i) { it.copy(title = title) }

    fun setNotes(notes: String): BulkState = update(selected) { it.copy(notes = notes) }

    fun setPriority(priority: Int): BulkState = update(selected) { it.copy(priority = TaskLogic.clampPriority(priority)) }

    private fun update(i: Int, f: (BulkEntry) -> BulkEntry): BulkState =
        copy(entries = entries.mapIndexed { idx, e -> if (idx == i) f(e) else e }).normalized()

    /** Exactly one empty field after the last non-empty title, at least [MIN_FIELDS] fields, never dropping the selected one. */
    fun normalized(): BulkState {
        val last = entries.indexOfLast { it.title.isNotBlank() }
        val size = maxOf(MIN_FIELDS, last + 2, selected + 1)
        val list = if (entries.size >= size) entries.take(size) else entries + List(size - entries.size) { BulkEntry() }
        return copy(entries = list)
    }

    companion object {
        const val MIN_FIELDS = 2
    }
}
