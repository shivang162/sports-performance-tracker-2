# Sports Performance Tracker

A full-stack Java web application for tracking and analyzing athlete performance.

## Tech Stack
- **Backend**: Java (plain HTTP server — no frameworks)
- **Database**: MySQL via JDBC
- **Frontend**: HTML + CSS + JavaScript + Chart.js

## Project Structure
```
sports-performance-tracker/
├── database/
│   └── schema.sql              ← Run this first in MySQL
├── lib/
│   └── mysql-connector-j.jar   ← Download and place here
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
│   │   ├── DBConnection.java         MySQL singleton
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

### Step 1 — Setup MySQL
```bash
mysql -u root -p < database/schema.sql
```

### Step 2 — Add MySQL JDBC Driver
Download `mysql-connector-j-8.x.x.jar` from https://dev.mysql.com/downloads/connector/j/  
Place it in the `lib/` folder.

### Step 3 — Update your password
Open `src/com/tracker/dao/DBConnection.java` and set `DB_PASS` to your MySQL password.

### Step 4 — Test DB connection
```bash
javac -cp lib/mysql-connector-j-*.jar TestDB.java
java  -cp .:lib/mysql-connector-j-*.jar TestDB
```
You should see: `Connected to Database ✅`

### Step 5 — Run the server (VS Code)
Add the JAR to your VS Code Java build path, then run `Main.java`.

### Step 6 — Open in browser
Open `src/com/tracker/frontend/login.html`

**Demo login**: `coach@example.com` / `safePassword123`

## Features
- Login & Registration with MySQL
- Save performance sessions (distance, time, accuracy, stamina)
- Auto-calculated speed and weighted score
- Dashboard with trend analysis, avg score, and DB-powered stats
- Rule-based training suggestions (SuggestionService)
- Athlete comparison page
- Chart.js speed visualization
- Offline fallback when server is down