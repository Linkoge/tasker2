package dev.tasker2

import dev.tasker2.data.LocalTaskRepository
import dev.tasker2.data.Task
import dev.tasker2.data.TaskRepository
import dev.tasker2.domain.BulkEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** Stable UUIDs, createdAt/updatedAt, soft deletes: the data model prepared for sharing. */
class RepositoryIdentityTest {
    private val dao = FakeDao()
    private var now = 1_000L
    private val repo: TaskRepository = LocalTaskRepository(dao) { now }

    private fun tick() { now += 10 }

    @Test
    fun everyCategoryAndTaskGetsAUniqueValidUuidAndTimestamps() = runBlocking {
        val a = repo.addCategory("A")
        val b = repo.addCategory("B")
        repo.addTask(a, "t1", "", 3)
        repo.addTasks(b, listOf(BulkEntry("t2"), BulkEntry("t3")))
        val snap = repo.snapshot()
        val ids = snap.categories.map { it.uuid } + snap.tasks.map { it.uuid }
        assertEquals(5, ids.toSet().size)
        ids.forEach { assertEquals(it, UUID.fromString(it).toString()) }
        (snap.categories.map { it.createdAt to it.updatedAt } + snap.tasks.map { it.createdAt to it.updatedAt })
            .forEach { (c, u) -> assertTrue(c > 0 && u >= c) }
    }

    @Test
    fun taskUuidAndCreatedAtSurviveEveryKindOfChangeAndUpdatedAtIsBumped() = runBlocking {
        val a = repo.addCategory("A")
        val b = repo.addCategory("B")
        val id = repo.addTask(a, "t", "n", 2)
        val first = repo.getTask(id)!!
        var last = first.updatedAt
        val steps: List<suspend () -> Unit> = listOf(
            { repo.updateTask(repo.getTask(id)!!.copy(title = "renamed")) },
            { repo.updateTask(repo.getTask(id)!!.copy(notes = "other notes")) },
            { repo.updateTask(repo.getTask(id)!!.copy(priority = 9)) },
            { repo.toggleCrossedOut(repo.getTask(id)!!) },
            { repo.setCompleted(repo.getTask(id)!!, true) },
            { repo.setCompleted(repo.getTask(id)!!, false) },
            { repo.moveTask(repo.getTask(id)!!, b) },
        )
        for (step in steps) {
            tick()
            step()
            val t = repo.getTask(id)!!
            assertEquals(first.uuid, t.uuid)
            assertEquals(first.createdAt, t.createdAt)
            assertTrue("updatedAt must grow", t.updatedAt > last)
            last = t.updatedAt
        }
        val t = repo.getTask(id)!!
        assertEquals(listOf("renamed", "other notes", 9, true, false, b), listOf(t.title, t.notes, t.priority, t.crossedOut, t.completed, t.categoryId))
    }

    @Test
    fun updateTaskCannotChangeIdentity() = runBlocking {
        val id = repo.addTask(repo.addCategory("A"), "t", "", 1)
        val t = repo.getTask(id)!!
        tick()
        repo.updateTask(t.copy(uuid = "forged", createdAt = 1, deleted = true, title = "x"))
        val after = repo.getTask(id)!!
        assertEquals(t.uuid, after.uuid)
        assertEquals(t.createdAt, after.createdAt)
        assertFalse(after.deleted)
    }

    @Test
    fun noOpUpdateDoesNotBumpUpdatedAt() = runBlocking {
        val id = repo.addTask(repo.addCategory("A"), "t", "", 1)
        val t = repo.getTask(id)!!
        tick()
        repo.updateTask(t)
        repo.moveTask(t, t.categoryId)
        assertEquals(t.updatedAt, repo.getTask(id)!!.updatedAt)
    }

    @Test
    fun categoryUuidSurvivesRenameAndReorderAndOnlyMovedCategoriesAreStamped() = runBlocking {
        val a = repo.addCategory("A"); tick()
        val b = repo.addCategory("B"); tick()
        val c = repo.addCategory("C"); tick()
        val before = repo.categories.first().associateBy { it.id }
        now += 100
        repo.moveCategory(2, 1) // A, C, B
        val moved = repo.categories.first()
        assertEquals(listOf("A", "C", "B"), moved.map { it.name })
        moved.forEach { assertEquals(before[it.id]!!.uuid, it.uuid) }
        assertEquals(before[a]!!.updatedAt, moved.first { it.id == a }.updatedAt)
        assertTrue(moved.first { it.id == b }.updatedAt > before[b]!!.updatedAt)
        assertTrue(moved.first { it.id == c }.updatedAt > before[c]!!.updatedAt)
        tick()
        repo.renameCategory(moved.first { it.id == a }, "Alpha")
        val renamed = repo.categories.first().first { it.id == a }
        assertEquals(before[a]!!.uuid, renamed.uuid)
        assertEquals("Alpha", renamed.name)
        assertTrue(renamed.updatedAt > before[a]!!.updatedAt)
    }

    @Test
    fun deletedTaskIsHiddenButRecordedAsTombstone() = runBlocking {
        val id = repo.addTask(repo.addCategory("A"), "gone", "", 1)
        val uuid = repo.getTask(id)!!.uuid
        tick()
        repo.deleteTask(repo.getTask(id)!!)
        assertTrue(repo.tasks.first().isEmpty())
        assertEquals(null, repo.getTask(id))
        val tomb = repo.snapshot().tasks.single()
        assertEquals(uuid, tomb.uuid)
        assertTrue(tomb.deleted)
        assertTrue(tomb.updatedAt > tomb.createdAt)
    }

    @Test
    fun deletedCategoryTombstonesItsTasksAndCompactsOrder() = runBlocking {
        val a = repo.addCategory("A")
        val b = repo.addCategory("B")
        val c = repo.addCategory("C")
        repo.addTask(b, "in b", "", 1)
        repo.addTask(a, "in a", "", 1)
        tick()
        repo.deleteCategory(repo.categories.first().first { it.id == b })
        assertEquals(listOf("A" to 0, "C" to 1), repo.categories.first().map { it.name to it.position })
        assertEquals(listOf("in a"), repo.tasks.first().map { it.title })
        val snap = repo.snapshot()
        assertEquals(3, snap.categories.size)
        assertTrue(snap.categories.single { it.id == b }.deleted)
        assertTrue(snap.tasks.single { it.title == "in b" }.deleted)
        assertFalse(snap.tasks.single { it.title == "in a" }.deleted)
        assertNotEquals(0L, snap.categories.single { it.id == c }.updatedAt)
        // The tab that shifted left is stamped as changed.
        assertEquals(now, snap.categories.single { it.id == c }.updatedAt)
    }

    @Test
    fun uuidsAreRandomAcrossRepositories() {
        assertNotEquals(Task(categoryId = 1, title = "a").uuid, Task(categoryId = 1, title = "a").uuid)
    }
}
