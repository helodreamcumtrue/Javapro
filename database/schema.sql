-- ==========================================================
-- Goal Decomposition Engine - Database Schema (SQLite)
-- ==========================================================
-- NOTE: You do NOT need to run this file manually.
-- Hibernate (spring.jpa.hibernate.ddl-auto=update) creates and
-- updates these tables automatically on application startup,
-- inside goal_decomposition.db in the project root.
--
-- This file is provided purely for documentation / evaluation
-- purposes, so the schema can be inspected without running the app.
-- ==========================================================

CREATE TABLE IF NOT EXISTS goals (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    title            TEXT NOT NULL,
    difficulty       TEXT NOT NULL,             -- 'easy' | 'medium' | 'hard'
    created_at       TIMESTAMP NOT NULL,
    match_type       TEXT,                      -- 'TOPIC' | 'VERB' | 'DEFAULT'
    matched_keyword  TEXT,                      -- keyword that triggered the rule
    explanation      TEXT                       -- human-readable decomposition reasoning
);

CREATE TABLE IF NOT EXISTS tasks (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    description         TEXT NOT NULL,
    parent_id           INTEGER,           -- NULL for top-level tasks, else references tasks.id (tree structure)
    goal_id             INTEGER NOT NULL,  -- references goals.id
    priority            TEXT NOT NULL,     -- 'HIGH' | 'MEDIUM' | 'LOW'
    order_index         INTEGER NOT NULL,  -- position among sibling tasks
    estimated_days      INTEGER NOT NULL,  -- rough time estimate, difficulty-adjusted
    depends_on_task_id  INTEGER,           -- task that must be completed first (sequential dependency)
    score               REAL NOT NULL,     -- computed importance score
    completed           BOOLEAN NOT NULL DEFAULT 0,

    FOREIGN KEY (goal_id) REFERENCES goals(id),
    FOREIGN KEY (parent_id) REFERENCES tasks(id),
    FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id)
);

-- Example query: get all top-level tasks for a goal, in order
-- SELECT * FROM tasks WHERE goal_id = 1 AND parent_id IS NULL ORDER BY order_index;

-- Example query: get all sub-tasks of a specific task
-- SELECT * FROM tasks WHERE parent_id = 4 ORDER BY order_index;
