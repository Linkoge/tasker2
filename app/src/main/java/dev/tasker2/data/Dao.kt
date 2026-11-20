package dev.tasker2.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM categories ORDER BY position, id")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY position, id")
    suspend fun getCategories(): List<Category>

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategories(categories: List<Category>)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): Task?

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
