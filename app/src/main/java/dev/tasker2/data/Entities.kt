package dev.tasker2.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A category is shown as a tab. [position] defines the tab order (ascending). */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
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
    indices = [Index("categoryId")],
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val notes: String = "",
    /** 1..10, 10 = most urgent. */
    val priority: Int = DEFAULT_PRIORITY,
    val completed: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    /** "Crossed out": struck through and grayed, but the task stays in the open list. */
    @ColumnInfo(name = "crossed_out", defaultValue = "0") val crossedOut: Boolean = false,
) {
    companion object {
        const val MIN_PRIORITY = 1
        const val MAX_PRIORITY = 10
        const val DEFAULT_PRIORITY = 5
    }
}
