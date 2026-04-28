# Sports Performance Tracker

A full-stack Java web application for tracking and analyzing athlete performance.

## Tech Stack
- **Backend**: Java (plain HTTP server — no frameworks)
- **Database**: SQLite via JDBC (embedded — no external database server required)
- **Frontend**: HTML + CSS + JavaScript + Chart.js

## Project Structure
```
.
├── database/
│   └── Schema.sql                 # SQLite schema (auto-applied on first run)
├── lib/
│   ├── sqlite-jdbc-3.45.3.0.jar   # SQLite JDBC driver
│   ├── slf4j-api-2.0.13.jar       # Required by sqlite-jdbc
│   └── slf4j-nop-2.0.13.jar       # SLF4J no-op logger
├── src/com/tracker/
│   ├── Main.java                  # Entry point, starts HTTP server
│   ├── controller/
│   │   ├── AuthController.java        # POST /login
│   │   ├── RegisterController.java    # POST /register
│   │   ├── PerformanceController.java # POST /save
│   │   ├── DashboardController.java   # GET /dashboard
│   │   └── RecordsController.java     # GET /records
│   ├── service/
│   │   ├── PerformanceService.java    # Core logic (avg, trend, score)
│   │   ├── PerformanceAnalyzer.java   # Benchmark comparison
│   │   ├── SuggestionService.java     # Rule-based training tips
│   │   ├── AuthService.java
│   │   ├── ValidationService.java
│   │   ├── PerformanceLevel.java
│   │   └── RecordFormatter.java       # JSON builder
│   ├── dao/
│   │   ├── DBConnection.java          # SQLite connection factory (auto-init schema)
│   │   ├── UserDAO.java
│   │   └── PerformanceDAO.java
│   └── frontend/
│       └── index.html              # Single-file SPA (login/register/dashboard/compare)
├── TestDB.java                     # Verify DB connection
└── TestLogic.java                  # Verify service logic
```

## API Routes

| Method | Route       | Description                        |
|--------|-------------|------------------------------------|
| POST   | `/login`    | Authenticate user                  |
| POST   | `/register` | Register new user                  |
| POST   | `/save`     | Save performance session to DB     |
| GET    | `/dashboard`| Stats + trend + suggestion from DB |
| GET    | `/records`  | All records (used by compare page) |

## How to Run

No external database setup is required. The SQLite database file (`sports_tracker.db`) is created automatically when the server starts for the first time.

### 1) Test DB connection (optional)

```bash
run TestDB.java
```

You should see: `Connected to SQLite ✅`

### 2) Run the server

```bash
run Main.java
```

Or in VS Code, add the JARs in `lib/` to your Java build path and run `Main.java`.

### 3) Open the frontend

Open `src/com/tracker/frontend/index.html` in your browser.

**Demo login**: `coach@example.com` / `safePassword123`

### Override the database file path (optional)

```bash
export DB_URL=jdbc:sqlite:/path/to/your/sports_tracker.db
make run
```

## Features
- Login & registration with SQLite
- Save performance sessions (distance, time, accuracy, stamina)
- Auto-calculated speed and weighted score
- Dashboard with trend analysis, average score, and DB-powered stats
- Rule-based training suggestions (`SuggestionService`)
- Athlete comparison page
- Chart.js speed visualization
- Offline fallback when server is down
