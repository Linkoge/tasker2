package dev.tasker2.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** A new permanent identity for a task or category. Never changes once assigned. */
fun newUuid(): String = UUID.randomUUID().toString()

/**
 * A category is shown as a tab. [position] defines the tab order (ascending).
 *
 * [id] is only the local row handle (UI routes, selection). Identity across devices/sync is [uuid]; it is
 * assigned once and survives rename, reorder and delete. [deleted] is a soft-delete tombstone: the row is kept
 * (with a bumped [updatedAt]) so a future sync can propagate the deletion; the UI never sees deleted rows.
 */
@Entity(tableName = "categories", indices = [Index(value = ["uuid"], unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
    @ColumnInfo(defaultValue = "''") val uuid: String = newUuid(),
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = createdAt,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId"), Index(value = ["uuid"], unique = true)],
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val notes: String = "",
    /** 0 = no priority (the default); otherwise 1..10, 10 = most urgent. Existing rows keep their values. */
    val priority: Int = DEFAULT_PRIORITY,
    val completed: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    /** "Crossed out": struck through and grayed, but the task stays in the open list. */
    @ColumnInfo(name = "crossed_out", defaultValue = "0") val crossedOut: Boolean = false,
    /** Permanent unique identity (see [Category]). Kept across edits, moves between categories, completing, restoring. */
    @ColumnInfo(defaultValue = "''") val uuid: String = newUuid(),
    /** Bumped on every change (title, notes, priority, crossed-out, completed, category, delete). */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = createdAt,
    /** Soft-delete tombstone; hidden from the UI. */
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
) {
    companion object {
        /** Stored as 0 so the column stays non-null and needs no migration (real priorities are 1..10). */
        const val NO_PRIORITY = 0
        const val MIN_PRIORITY = 1
        const val MAX_PRIORITY = 10
        const val DEFAULT_PRIORITY = NO_PRIORITY
    }
}
