package dev.tasker2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Raw Room access. Only [LocalTaskRepository] uses it. The default queries hide soft-deleted rows;
 * the `getAll*` queries include them (for sync).
 */
@Dao
interface TaskDao {
    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY position, id")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY position, id")
    suspend fun getCategories(): List<Category>

    @Query("SELECT * FROM categories ORDER BY position, id")
    suspend fun getAllCategories(): List<Category>

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategories(categories: List<Category>)

    @Query("SELECT * FROM tasks WHERE deleted = 0")
    fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE deleted = 0")
    suspend fun getTasks(): List<Task>

    @Query("SELECT * FROM tasks ORDER BY id")
    suspend fun getAllTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id AND deleted = 0")
    suspend fun getTask(id: Long): Task?

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTasks(tasks: List<Task>)
}
