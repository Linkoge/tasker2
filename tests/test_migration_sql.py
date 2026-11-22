"""Runs the real 2 -> 3 migration SQL (app/.../data/Migrations.kt) on a database holding 1.8 data."""
import re
import sqlite3
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/dev/tasker2"
UUID_RE = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

V2_SCHEMA = [
    "CREATE TABLE categories (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL)",
    "CREATE TABLE tasks (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` INTEGER NOT NULL, `title` TEXT NOT NULL, "
    "`notes` TEXT NOT NULL, `priority` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, "
    "`completed_at` INTEGER, `crossed_out` INTEGER NOT NULL DEFAULT 0, "
    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
    "CREATE INDEX index_tasks_categoryId ON tasks (categoryId)",
]


def migration_statements():
    text = (SRC / "data/Migrations.kt").read_text()
    uuid_sql = re.search(r'UUID_SQL = "(.*)"', text).group(1)
    body = text.split("MIGRATION_2_3_BEGIN")[1].split("MIGRATION_2_3_END")[0]
    stmts = [s.replace("$UUID_SQL", uuid_sql) for s in re.findall(r'^\s*"(.*)",$', body, re.M)]
    assert len(stmts) >= 8
    return stmts


@pytest.fixture
def migrated():
    db = sqlite3.connect(":memory:")
    for s in V2_SCHEMA:
        db.execute(s)
    db.executemany("INSERT INTO categories (id, name, position) VALUES (?,?,?)",
                   [(1, "Daily", 0), (2, "ყოველკვირეული", 2), (7, "Monthly", 1)])
    db.executemany(
        "INSERT INTO tasks (id, categoryId, title, notes, priority, completed, created_at, completed_at, crossed_out) VALUES (?,?,?,?,?,?,?,?,?)",
        [(1, 1, "open", "n1", 0, 0, 1000, None, 0),
         (2, 1, "crossed", "", 7, 0, 2000, None, 1),
         (3, 2, "done", "notes ✓", 10, 1, 3000, 5000, 0),
         (4, 7, "same time", "", 3, 0, 3000, None, 0)])
    before = db.execute("SELECT * FROM tasks ORDER BY id").fetchall()
    for s in migration_statements():
        db.execute(s)
    return db, before


def test_all_rows_and_old_columns_are_preserved(migrated):
    db, before = migrated
    old_cols = "id, categoryId, title, notes, priority, completed, created_at, completed_at, crossed_out"
    assert db.execute(f"SELECT {old_cols} FROM tasks ORDER BY id").fetchall() == before
    assert db.execute("SELECT id, name, position FROM categories ORDER BY id").fetchall() == [
        (1, "Daily", 0), (2, "ყოველკვირეული", 2), (7, "Monthly", 1)]


def test_every_row_gets_a_unique_uuid_and_timestamps(migrated):
    db, _ = migrated
    uuids = [r[0] for r in db.execute("SELECT uuid FROM categories UNION ALL SELECT uuid FROM tasks")]
    assert len(uuids) == 7 and len(set(uuids)) == 7
    assert all(UUID_RE.match(u) for u in uuids), uuids
    for created, updated, deleted in db.execute("SELECT created_at, updated_at, deleted FROM categories"):
        assert created > 1_600_000_000_000 and updated >= created and deleted == 0
    # Task updated_at: completion time for completed tasks, else created_at; nothing is deleted.
    assert db.execute("SELECT id, updated_at, deleted FROM tasks ORDER BY id").fetchall() == [
        (1, 1000, 0), (2, 2000, 0), (3, 5000, 0), (4, 3000, 0)]


def test_uuid_columns_are_unique_indexed(migrated):
    db, _ = migrated
    with pytest.raises(sqlite3.IntegrityError):
        db.execute("UPDATE tasks SET uuid = (SELECT uuid FROM tasks WHERE id = 1) WHERE id = 2")
