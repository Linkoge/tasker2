package dev.tasker2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Category::class, Task::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        val DEFAULT_CATEGORIES = listOf("Daily", "Weekly", "Monthly")

        /** v1 -> v2: adds the crossed-out flag; existing rows keep their data (default 0). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN crossed_out INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v2 -> v3: permanent UUIDs, updated_at and soft-delete flags. All existing rows are kept (see [Migrations]). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Migrations.V2_TO_V3.forEach { db.execSQL(it) }
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tasker2.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val now = System.currentTimeMillis()
                        DEFAULT_CATEGORIES.forEachIndexed { i, name ->
                            db.execSQL(
                                "INSERT INTO categories (name, position, uuid, created_at, updated_at, deleted) VALUES (?, ?, ?, ?, ?, 0)",
                                arrayOf<Any>(name, i, newUuid(), now, now),
                            )
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
