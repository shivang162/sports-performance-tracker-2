# Sports Performance Tracker

A full-stack Java web application for tracking and analyzing athlete performance.

## Tech Stack
- **Backend**: Java (plain HTTP server — no frameworks)
- **Database**: SQLite via JDBC (embedded — no external database server required)
- **Frontend**: HTML + CSS + JavaScript + Chart.js

## Project Structure
```
sports-performance-tracker/
├── database/
│   └── Schema.sql              ← SQLite schema (auto-applied on first run)
├── lib/
│   ├── sqlite-jdbc-3.45.3.0.jar  ← SQLite JDBC driver
│   ├── slf4j-api-2.0.13.jar      ← Required by sqlite-jdbc
│   └── slf4j-nop-2.0.13.jar      ← SLF4J no-op logger
├── src/com/tracker/
│   ├── Main.java               ← Entry point, starts HTTP server
│   ├── controller/
│   │   ├── AuthController.java       POST /login
│   │   ├── RegisterController.java   POST /register
│   │   ├── PerformanceController.java POST /save
│   │   ├── DashboardController.java  GET /dashboard
│   │   └── RecordsController.java    GET /records
│   ├── service/
│   │   ├── PerformanceService.java   Core logic (avg, trend, score)
│   │   ├── PerformanceAnalyzer.java  Benchmark comparison
│   │   ├── SuggestionService.java    Rule-based training tips
│   │   ├── AuthService.java
│   │   ├── ValidationService.java
│   │   ├── PerformanceLevel.java
│   │   └── RecordFormatter.java      JSON builder
│   ├── dao/
│   │   ├── DBConnection.java         SQLite connection factory (auto-init schema)
│   │   ├── UserDAO.java
│   │   └── PerformanceDAO.java
│   └── frontend/
│       └── index.html              ← Single-file SPA (login · register · dashboard · athlete · compare)
├── TestDB.java     ← Run to verify DB connection
└── TestLogic.java  ← Run to verify all service logic

```

## API Routes
| Method | Route        | Description                          |
|--------|-------------|--------------------------------------|
| POST   | /login      | Authenticate user                    |
| POST   | /register   | Register new user                    |
| POST   | /save       | Save performance session to DB       |
| GET    | /dashboard  | Stats + trend + suggestion from DB   |
| GET    | /records    | All records (used by compare page)   |

## How to Run

No external database setup is required. The SQLite database file (`sports_tracker.db`)
is created automatically when the server starts for the first time.

### Step 1 — Test DB connection (optional)
```bash
make test-db
```
You should see: `Connected to SQLite ✅`

### Step 2 — Run the server
```bash
make run
```
Or in VS Code, add the JARs in `lib/` to your Java build path and run `Main.java`.

### Step 3 — Open in browser
Open `src/com/tracker/frontend/index.html`

**Demo login**: `coach@example.com` / `safePassword123`

### Override the database file path (optional)
```bash
export DB_URL=jdbc:sqlite:/path/to/your/sports_tracker.db
make run
```

## Features
- Login & Registration with SQLite
- Save performance sessions (distance, time, accuracy, stamina)
- Auto-calculated speed and weighted score
- Dashboard with trend analysis, avg score, and DB-powered stats
- Rule-based training suggestions (SuggestionService)
- Athlete comparison page
- Chart.js speed visualization
- Offline fallback when server is down
