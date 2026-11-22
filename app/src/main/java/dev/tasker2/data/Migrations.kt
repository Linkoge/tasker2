package dev.tasker2.data

/**
 * Schema migration SQL, kept as plain single-line strings so tests/test_migration_sql.py can run exactly these
 * statements against a real SQLite database holding 1.8 data. Pure SQL, no Kotlin logic: every existing row keeps
 * its data and gets a random version-4 UUID and timestamps.
 */
object Migrations {
    // Builds a random v4 UUID string in SQL (SQLite has no uuid function).
    private const val UUID_SQL = "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || substr('89ab', 1 + (abs(random()) % 4), 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"

    // MIGRATION_2_3_BEGIN
    val V2_TO_V3: List<String> = listOf(
        "ALTER TABLE categories ADD COLUMN uuid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE categories ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE categories ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE categories ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0",
        "UPDATE categories SET uuid = $UUID_SQL, created_at = CAST(strftime('%s','now') AS INTEGER) * 1000, updated_at = CAST(strftime('%s','now') AS INTEGER) * 1000",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_uuid ON categories (uuid)",
        "ALTER TABLE tasks ADD COLUMN uuid TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE tasks ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE tasks ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0",
        "UPDATE tasks SET uuid = $UUID_SQL, updated_at = COALESCE(completed_at, created_at)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_uuid ON tasks (uuid)",
    )
    // MIGRATION_2_3_END
}
