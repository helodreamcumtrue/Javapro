# 🎯 Goal Decomposition Engine

A full-stack, **100% free, 100% local, rule-based** system that takes a user-defined goal (e.g., *"Learn Web Development"*) and automatically decomposes it into a structured, executable hierarchy of tasks — complete with priorities, topological dependencies, time estimates, importance scoring, interactive progress tracking, and execution tracing.

> **Zero External AI/ML models. No OpenAI/Gemini/Anthropic API keys. Zero cloud billing.**
> Every decomposition is produced deterministically in `< 2ms` via keyword matching, verb template extraction, and heuristic fallbacks over a customizable dataset.

---

## 🚀 Live Preview & Quick Start

### 1. Requirements
- **Java 17+** (`java -version`)
- **Apache Maven** (`mvn -version`)

### 2. Run with 1-Click (Windows)
Double-click `run.bat` or run:
```cmd
run.bat
```

### 3. Run on Linux / macOS
```bash
chmod +x run.sh
./run.sh
```

### 4. Or Run Manually via Maven
```bash
mvn clean spring-boot:run
```

### 5. Open in Your Browser
Visit: **[http://localhost:8080](http://localhost:8080)**

---

## 🛠️ Tech Stack & Constraints Compliance

| Requirement | Implementation | Compliance |
| :--- | :--- | :--- |
| **Backend** | Java 17 + Spring Boot 3.2.5 (Maven) | ✅ 100% Free / Standard |
| **Frontend** | Vanilla HTML5 + Modern CSS3 + Vanilla JavaScript (ES6+) | ✅ Zero React / Frameworks |
| **Database** | SQLite JDBC Driver (`goal_decomposition.db`) | ✅ Zero Server Setup / Zero Cost |
| **Engine** | Deterministic Keyword Rules + Verb Heuristics | ✅ No OpenAI / No Gemini / No ML |
| **Tests** | JUnit 5 + MockMvc Integration Suite | ✅ 12 Automated Tests Passing |

---

## 🧠 How the Rule-Based Decomposition Engine Works

The engine (`DecompositionService.java`) operates through a 3-tier deterministic pipeline:

```
                  ┌─────────────────────────────────┐
                  │       User Goal Statement       │
                  │   e.g. "Learn Web Development"  │
                  └────────────────┬────────────────┘
                                   │
                                   ▼
                   [1. Topic Rule Matcher]
         Checks if cleaned string contains specific keywords
         (e.g., "web development", "python", "guitar", "exam")
                                   │
                    ┌──────────────┴──────────────┐
                 Matched?                       No
                    │                             │
                    ▼                             ▼
         [Curriculum Blueprint]        [2. Verb Template Matcher]
         Handcrafted syllabus tasks    Extracts action verb & remaining topic
         with nested subprojects       (e.g., "build" -> "an autonomous drone")
                                                  │
                                   ┌──────────────┴──────────────┐
                                Matched?                       No
                                   │                             │
                                   ▼                             ▼
                        [Substituted Verb Tasks]       [3. Heuristic Fallback]
                        "Design {topic}"               6-phase standard goal
                        "Implement {topic}"            execution framework
                                   │                             │
                                   └──────────────┬──────────────┘
                                                  │
                                                  ▼
                                      [Difficulty Multiplier]
                                      Easy: 0.7x | Med: 1.0x | Hard: 1.5x + Deep-dive
                                                  │
                                                  ▼
                                      [Topological Dependency & Score]
                                      dependsOnTaskId = predecessor
                                      Score = Priority (10..30) + Order Bonus
                                                  │
                                                  ▼
                                      [SQLite Persistence & UI Tree]
```

1. **Topic match**: If the goal contains an exact topic keyword (e.g., `web development`), it loads a hand-crafted curriculum with pre-configured nested subprojects.
2. **Verb template match**: If no topic matched, it looks for action verbs (`learn`, `build`, `prepare`, `improve`, `start`), extracts the remainder as `{topic}`, and dynamically populates structured action steps.
3. **Fallback default template**: For arbitrary text, it applies a proven 6-phase project execution framework.
4. **Difficulty scaling**:
   - `Easy`: Multiplies estimated days by **0.7x**
   - `Medium`: Standard baseline **1.0x**
   - `Hard`: Multiplies estimated days by **1.5x** and appends an advanced deep-dive step.
5. **Dependencies (`dependsOnTaskId`)**: Sibling tasks are chained sequentially (e.g. *Learn CSS* requires *Learn HTML* to be finished first).
6. **Scoring formula**: 
   $$\text{Score} = \text{PriorityWeight} (\text{HIGH}=30, \text{MED}=20, \text{LOW}=10) + \max(0, 10 - \text{positionIndex})$$

---

## 🔌 API Endpoints

All endpoints support both root paths (as specified in evaluation guidelines) and `/api` prefixes:

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/goal` or `/api/goal` | Submit a goal; runs rule decomposition & saves tree |
| `GET` | `/tasks/{goalId}` or `/api/tasks/{goalId}` | Fetch decomposed tasks for an existing goal |
| `GET` | `/goals` or `/api/goals` | List all submitted goals for history sidebar |
| `PATCH` | `/tasks/{taskId}/toggle` | Invert task completion status (`completed = !completed`) |
| `DELETE` | `/goals/{goalId}` | Delete a goal and its associated tasks |

### Example Request (POST /goal)
```json
{
  "title": "Learn Web Development",
  "difficulty": "medium"
}
```

### Example Response
```json
{
  "goalId": 1,
  "title": "Learn Web Development",
  "difficulty": "medium",
  "createdAt": "2026-09-17T18:07:00",
  "totalEstimatedDays": 36,
  "totalTaskCount": 8,
  "completedTaskCount": 0,
  "matchType": "TOPIC",
  "matchedKeyword": "web development",
  "explanation": "Matched specialized topic curriculum for 'web development'",
  "tasks": [
    {
      "id": 1,
      "description": "Learn HTML",
      "priority": "HIGH",
      "estimatedDays": 4,
      "dependsOnTaskId": null,
      "score": 40.0,
      "completed": false,
      "children": []
    },
    {
      "id": 2,
      "description": "Learn CSS",
      "priority": "HIGH",
      "estimatedDays": 5,
      "dependsOnTaskId": 1,
      "score": 39.0,
      "completed": false,
      "children": []
    },
    {
      "id": 4,
      "description": "Build Projects",
      "priority": "MEDIUM",
      "estimatedDays": 14,
      "dependsOnTaskId": 3,
      "score": 27.0,
      "completed": false,
      "children": [
        {
          "id": 5,
          "description": "Build a static portfolio website",
          "priority": "MEDIUM",
          "estimatedDays": 5,
          "dependsOnTaskId": null,
          "score": 30.0,
          "completed": false,
          "children": []
        }
      ]
    }
  ]
}
```

---

## 🗄️ Database Schema (SQLite)

Located at `goal_decomposition.db` (auto-managed via Hibernate). Documented in [`database/schema.sql`](database/schema.sql):

```sql
CREATE TABLE goals (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    title            TEXT NOT NULL,
    difficulty       TEXT NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    match_type       TEXT,
    matched_keyword  TEXT,
    explanation      TEXT
);

CREATE TABLE tasks (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    description         TEXT NOT NULL,
    parent_id           INTEGER,          -- Hierarchical tree self-reference
    goal_id             INTEGER NOT NULL, -- Foreign key to goals
    priority            TEXT NOT NULL,    -- HIGH | MEDIUM | LOW
    order_index         INTEGER NOT NULL,
    estimated_days      INTEGER NOT NULL,
    depends_on_task_id  INTEGER,          -- Sequential dependency
    score               REAL NOT NULL,
    completed           BOOLEAN NOT NULL DEFAULT 0,
    FOREIGN KEY (goal_id) REFERENCES goals(id),
    FOREIGN KEY (parent_id) REFERENCES tasks(id),
    FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id)
);
```

---

## 🧪 Automated Testing

Execute the complete JUnit 5 and Spring Boot MockMvc integration test suite:
```bash
mvn clean test
```
All 12 test cases validate:
- Direct topic rule matching & subtask nesting
- Verb template extraction
- Default fallback execution
- Difficulty multiplier logic & hard-mode deep dive task
- Topological dependency chaining & scoring
- REST API endpoint contracts (`/goal`, `/tasks/{id}`, `/goals`, `/tasks/{id}/toggle`, validation errors)

---

## 🎨 Frontend Features
- **Engine Execution Trace**: Live diagnostic card reporting matched rule, keyword, explanation, and $0.00 cost.
- **Interactive Checkboxes**: Click any checkbox to mark a task completed; persists to SQLite via `PATCH /tasks/{id}/toggle`.
- **Live Progress Bar**: Dynamic percent recalculation based on completed tasks.
- **Tree Visualization**: Distinct parent-child branches, expandable/collapsible sub-steps, priority pills, and dependency chips.
- **Filter Tabs**: Toggle between `All Tasks`, `Pending`, and `Completed`.
- **Export Utility**: Download plan as formatted `Markdown Checklist (.md)` or `JSON Tree (.json)`.
- **Goal History**: Sidebar with instant search filter and deletion support.

---

## 🎓 College Viva / Evaluation Quick Q&A

**Q1: How does this system decompose goals without an AI model like OpenAI or Gemini?**
> It uses deterministic keyword matching against an extensible JSON dataset (`rules.json`). It analyzes noun topics, detects action verbs, extracts target subjects, applies time/difficulty multipliers, and connects tasks into a directed dependency tree.

**Q2: How does the tree hierarchy work in the database?**
> The `tasks` table uses an adjacency list pattern with a nullable `parent_id` column referencing another `tasks(id)`. When building the response, `GoalService.java` converts the flat list into a nested `TaskNode` tree recursively.

**Q3: How are dependencies handled?**
> Each task stores `depends_on_task_id`, pointing to the previous prerequisite step. In the UI and API, tasks cannot logically proceed without resolving dependencies.

---

## 🚢 Deployment Guide

### Option 1: Free Cloud Hosting (Render / Railway / Koyeb)
1. Push this repository to **GitHub**.
2. Go to [Render](https://render.com/) or [Railway](https://railway.app/).
3. Click **New +** -> **Web Service** and select your repository.
4. Choose **Docker** environment (it automatically detects the included `Dockerfile`).
5. (Optional) Add a persistent disk mounted to `/app/data` to keep SQLite database data across redeployments.
6. Click **Deploy**. Your service will be live on a free public HTTPS URL!

### Option 2: Docker Compose (Any Server or VPS)
Run directly with persistent storage:
```bash
docker compose up -d --build
```
Access at: `http://localhost:8080` (or `http://your-server-ip:8080`).

### Option 3: Traditional Linux VPS / AWS EC2 / DigitalOcean
1. Install Java 17:
   ```bash
   sudo apt update && sudo apt install -y openjdk-17-jre-headless
   ```
2. Build the executable JAR:
   ```bash
   mvn clean package -DskipTests
   ```
3. Run as background service:
   ```bash
   nohup java -jar target/goal-decomposition-engine.jar --server.port=8080 > app.log 2>&1 &
   ```

