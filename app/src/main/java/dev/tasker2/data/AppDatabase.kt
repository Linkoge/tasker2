package dev.tasker2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Category::class, Task::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        val DEFAULT_CATEGORIES = listOf("Daily", "Weekly", "Monthly")

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tasker2.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DEFAULT_CATEGORIES.forEachIndexed { i, name ->
                            db.execSQL("INSERT INTO categories (name, position) VALUES (?, ?)", arrayOf<Any>(name, i))
                        }
                    }
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("PRAGMA foreign_keys=ON")
                    }
                })
                .build()
                .also { instance = it }
        }
    }
}
