# Sports Performance Tracker

A full-stack Java web application for tracking and analyzing athlete performance.

## Tech Stack
- **Backend**: Java (plain HTTP server — no frameworks)
- **Database**: PostgreSQL via JDBC
- **Frontend**: HTML + CSS + JavaScript + Chart.js

## Project Structure
```
sports-performance-tracker/
├── database/
│   └── schema.sql              ← Run this first in PostgreSQL
├── lib/
│   └── postgresql-42.7.4.jar   ← Download and place here
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
│   │   ├── DBConnection.java         PostgreSQL connection factory
│   │   ├── UserDAO.java
│   │   └── PerformanceDAO.java
│   └── frontend/
│       ├── login.html
│       ├── register.html
│       ├── dashboard.html
│       ├── compare.html
│       └── js/script.js
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

### Step 1 — Setup PostgreSQL
```bash
# Create the database
psql -U postgres -c "CREATE DATABASE sports_tracker;"

# Load the schema and seed data
psql -U postgres -d sports_tracker -f database/Schema.sql
```

### Step 2 — Add PostgreSQL JDBC Driver
Download `postgresql-42.7.4.jar` from https://jdbc.postgresql.org/download/  
Place it in the `lib/` folder.

### Step 3 — Set environment variables
```bash
export DB_URL=jdbc:postgresql://localhost:5432/sports_tracker
export DB_USER=postgres
export DB_PASS=your_password
```

### Step 4 — Test DB connection
```bash
make test-db
```
You should see: `Connected to Database ✅`

If `make` is not available, run manually:
```bash
javac -cp lib/postgresql-42.7.4.jar TestDB.java
java  -cp .:lib/postgresql-42.7.4.jar TestDB
```

### Step 5 — Run the server
```bash
make run
```
Or in VS Code, add the JAR to your Java build path and run `Main.java`.

### Step 6 — Open in browser
Open `src/com/tracker/frontend/login.html`

**Demo login**: `coach@example.com` / `safePassword123`

## Features
- Login & Registration with PostgreSQL
- Save performance sessions (distance, time, accuracy, stamina)
- Auto-calculated speed and weighted score
- Dashboard with trend analysis, avg score, and DB-powered stats
- Rule-based training suggestions (SuggestionService)
- Athlete comparison page
- Chart.js speed visualization
- Offline fallback when server is down