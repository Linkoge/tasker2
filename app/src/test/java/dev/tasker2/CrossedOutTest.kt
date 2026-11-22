package dev.tasker2

import androidx.sqlite.db.SupportSQLiteDatabase
import dev.tasker2.data.AppDatabase
import dev.tasker2.data.Task
import dev.tasker2.domain.TaskLogic
import java.io.File
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossedOutTest {
    private fun task(crossed: Boolean = false, done: Boolean = false) =
        Task(id = 1, categoryId = 1, title = "t", crossedOut = crossed, completed = done)

    @Test fun toggleFlipsAndKeepsTaskOpen() {
        val t = TaskLogic.toggleCrossedOut(task())
        assertTrue(t.crossedOut)
        assertFalse(t.completed)
        assertEquals(listOf(t), TaskLogic.sortOpen(listOf(t)))
        assertTrue(TaskLogic.sortCompleted(listOf(t)).isEmpty())
        assertEquals(1, TaskLogic.openCounts(listOf(t))[1L])
        assertFalse(TaskLogic.toggleCrossedOut(t).crossedOut)
    }

    @Test fun completingKeepsCrossedState() {
        assertTrue(TaskLogic.setCompleted(task(crossed = true), true).crossedOut)
    }

    @Test fun migrationAddsColumnWithDefaultSoExistingRowsSurvive() {
        val sql = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, m, args -> if (m.name == "execSQL") sql += args[0] as String; null } as SupportSQLiteDatabase
        AppDatabase.MIGRATION_1_2.migrate(db)
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)
        assertEquals(listOf("ALTER TABLE tasks ADD COLUMN crossed_out INTEGER NOT NULL DEFAULT 0"), sql)
        assertTrue(sql.none { it.contains("DROP", ignoreCase = true) })
    }

    @Test fun exportedSchemaV2HasColumn() {
        val f = File("schemas/dev.tasker2.data.AppDatabase/2.json")
        assertTrue("schema 2.json missing", f.exists())
        val text = f.readText()
        assertTrue(text.contains("\"crossed_out\""))
        assertTrue(text.contains("\"defaultValue\": \"0\""))
    }

    @Test fun priorityGradientHasNoBlue() {
        for (p in 1..10) {
            val c = TaskLogic.priorityColorArgb(p)
            val r = (c shr 16) and 0xFF; val b = c and 0xFF
            assertTrue("priority $p too blue", b <= r)
        }
    }
}
