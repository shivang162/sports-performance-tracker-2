// ═══════════════════════════════════════════════════════════════
//  script.js — Sports Performance Tracker
//  Shared by login.html and dashboard.html
//
//  login()          → POST /login     → AuthController
//  saveData()       → POST /save      → PerformanceController
//  fetchDashboard() → GET  /dashboard → DashboardController
//                                       (includes suggestion from SuggestionService)
//  Author: Team Lead
// ═══════════════════════════════════════════════════════════════

const BASE_URL = "http://localhost:8080";
let chartInstance;

// ── Sport configuration: labels, placeholders, offline weights ──

var SPORT_CONFIG = {
    "Running":       { dist: "Distance (m)",    time: "Time (sec)",      acc: "Form (0–100)",          sta: "Stamina (0–100)", speed: "Speed (m/s)",  distPh: "400", timePh: "60",  wSpeed: 0.60, wAcc: 0.20, wSta: 0.20 },
    "Swimming":      { dist: "Distance (m)",    time: "Time (sec)",      acc: "Technique (0–100)",      sta: "Stamina (0–100)", speed: "Speed (m/s)",  distPh: "100", timePh: "90",  wSpeed: 0.40, wAcc: 0.30, wSta: 0.30 },
    "Basketball":    { dist: "Shots Attempted", time: "Duration (sec)",  acc: "Shot Accuracy (0–100)",  sta: "Stamina (0–100)", speed: "Pace",         distPh: "20",  timePh: "120", wSpeed: 0.20, wAcc: 0.50, wSta: 0.30 },
    "Football":      { dist: "Distance (m)",    time: "Time (sec)",      acc: "Pass Accuracy (0–100)",  sta: "Stamina (0–100)", speed: "Speed (m/s)",  distPh: "500", timePh: "90",  wSpeed: 0.35, wAcc: 0.35, wSta: 0.30 },
    "Tennis":        { dist: "Rallies",         time: "Duration (sec)",  acc: "Shot Accuracy (0–100)",  sta: "Stamina (0–100)", speed: "Rally Pace",   distPh: "15",  timePh: "60",  wSpeed: 0.25, wAcc: 0.45, wSta: 0.30 },
    "Cycling":       { dist: "Distance (km)",   time: "Time (sec)",      acc: "Form (0–100)",           sta: "Stamina (0–100)", speed: "Speed (m/s)",  distPh: "20",  timePh: "3600",wSpeed: 0.60, wAcc: 0.10, wSta: 0.30 },
    "Weightlifting": { dist: "Weight (kg)",     time: "Duration (sec)",  acc: "Form Score (0–100)",     sta: "Stamina (0–100)", speed: "Lift Rate",    distPh: "80",  timePh: "60",  wSpeed: 0.20, wAcc: 0.40, wSta: 0.40 }
};

function getSport() {
    var sel = document.getElementById("sportSelect");
    return sel ? sel.value : "Running";
}

/** Called when sport dropdown changes — update labels and refresh dashboard */
function onSportChange() {
    var sport = getSport();
    var cfg   = SPORT_CONFIG[sport] || SPORT_CONFIG["Running"];

    // Update form labels
    setEl("lblDistance", cfg.dist);
    setEl("lblTime",     cfg.time);
    setEl("lblAccuracy", cfg.acc);
    setEl("lblStamina",  cfg.sta);

    // Update table headers
    setEl("thDistance", cfg.dist.split(" ")[0]);
    setEl("thSpeed",    cfg.speed);
    setEl("thAccuracy", cfg.acc.split(" ")[0]);

    // Update input placeholders
    setPlaceholder("distance", cfg.distPh);
    setPlaceholder("time",     cfg.timePh);

    // Reload dashboard stats for this sport
    fetchDashboard();
    updateUI();
}

// ── Page detection ─────────────────────────────────────────────

window.onload = function () {
    if (document.getElementById("loginBtn"))  initLoginPage();
    if (document.getElementById("tableBody")) initDashboard();
};

// ═══════════════════════════════════════════════════════════════
//  LOGIN PAGE
// ═══════════════════════════════════════════════════════════════

function initLoginPage() {
    document.addEventListener("keydown", function (e) {
        if (e.key === "Enter") login();
    });
}

/**
 * POST /login → AuthController → AuthService → UserDAO → PostgreSQL
 * On success: stores email in sessionStorage, redirects to dashboard.html
 */
function login() {
    var email = document.getElementById("username").value.trim();
    var pass  = document.getElementById("password").value;
    var btn   = document.getElementById("loginBtn");

    hideMsg("errMsg"); hideMsg("sucMsg");

    if (!email || !pass) { showErr("Please fill in both fields."); return; }

    btn.textContent = "AUTHENTICATING...";
    btn.disabled    = true;

    fetch(BASE_URL + "/login", {
        method:  "POST",
        headers: { "Content-Type": "application/json" },
        body:    JSON.stringify({ email: email, password: pass })
    })
    .then(function (res) { return res.json(); })
    .then(function (data) {
        if (!data.success) throw new Error(data.error || "Invalid credentials");
        showSuc(data.message || "Login Successful");
        sessionStorage.setItem("userEmail", email);
        setTimeout(function () { window.location.href = "dashboard.html"; }, 800);
    })
    .catch(function (err) {
        showErr(err.message || "Server not reachable. Is Java running on port 8080?");
        btn.textContent = "LOGIN";
        btn.disabled    = false;
    });
}

function guestLogin() {
    sessionStorage.setItem("guest",     "true");
    sessionStorage.setItem("userEmail", "Guest");
    window.location.href = "dashboard.html";
}

// ═══════════════════════════════════════════════════════════════
//  DASHBOARD PAGE
// ═══════════════════════════════════════════════════════════════

function initDashboard() {
    // Show logged-in user in navbar
    var email = sessionStorage.getItem("userEmail") || "coach@example.com";
    var badge = document.getElementById("userBadge");
    if (badge) badge.textContent = email;

    // Apply initial sport labels
    onSportChange();
    // onSportChange() calls fetchDashboard() and updateUI(), so no extra calls needed
}

// ── GET /dashboard → DashboardController ───────────────────────

/**
 * Returns: summary{average,trend,improvement,level,totalSessions}, scores[], suggestion
 * Updates: totalSessions, avgScore, stat-trend stat cards + suggestion box
 */
function fetchDashboard() {
    var email = sessionStorage.getItem("userEmail") || "coach@example.com";
    var sport = getSport();
    fetch(BASE_URL + "/dashboard?athlete=" + encodeURIComponent(email) + "&sport=" + encodeURIComponent(sport))
    .then(function (res) { return res.json(); })
    .then(function (data) {
        var s = data.summary;

        setEl("totalSessions", s.totalSessions !== undefined ? s.totalSessions : "—");
        setEl("avgScore",      parseFloat(s.average).toFixed(2));

        var trendEl = document.getElementById("stat-trend");
        if (trendEl) {
            trendEl.textContent = s.trend;
            trendEl.className   = "stat-value " + (s.trend === "Improving" ? "green" : s.trend === "Declining" ? "red" : "yellow");
        }

        // Suggestion from SuggestionService
        if (data.suggestion) {
            var box  = document.getElementById("suggestionBox");
            var text = document.getElementById("suggestionText");
            if (box && text) {
                text.textContent = data.suggestion;
                box.classList.add("show");
            }
        }

        console.log("[Dashboard] DB stats:", s);
    })
    .catch(function () {
        console.warn("[Dashboard] /dashboard unavailable — offline mode.");
        setEl("totalSessions", "—");
        setEl("avgScore",      "—");
        setEl("stat-trend",    "—");
    });
}

// ── POST /save → PerformanceController ─────────────────────────

/**
 * Sends: athlete, sport, distance, time, accuracy, stamina
 * Server: validates → speed = distance/time → calculateScore (sport weights) → level → PostgreSQL
 * Returns: {success, athlete, sport, speed, accuracy, stamina, score, level}
 * On offline: calculates score locally using sport weights, saves to localStorage
 */
function saveData() {
    var d        = parseFloat(document.getElementById("distance").value);
    var t        = parseFloat(document.getElementById("time").value);
    var accuracy = parseFloat(document.getElementById("accuracy").value);
    var stamina  = parseFloat(document.getElementById("stamina").value);
    var sport    = getSport();

    if (!d || !t || isNaN(accuracy) || isNaN(stamina)) {
        showToast("Please fill in all fields.", "red"); return;
    }
    if (t <= 0) { showToast("Time must be greater than 0.", "red"); return; }

    var speed = d / t;
    if (speed  < 0 || speed  > 200) { showToast("Speed out of range (0–200).",  "red"); return; }
    if (accuracy < 0 || accuracy > 100) { showToast("Accuracy must be 0–100.", "red"); return; }
    if (stamina  < 0 || stamina  > 100) { showToast("Stamina must be 0–100.",  "red"); return; }

    fetch(BASE_URL + "/save", {
        method:  "POST",
        headers: { "Content-Type": "application/json" },
        body:    JSON.stringify({
            athlete:  sessionStorage.getItem("userEmail") || "Demo Athlete",
            sport:    sport,
            distance: d,
            time:     t,
            accuracy: accuracy,
            stamina:  stamina
        })
    })
    .then(function (res) { return res.json(); })
    .then(function (data) {
        if (!data.success) { showToast(data.error || "Save failed.", "red"); return; }
        saveToLocal(sport, d, t, data.speed, accuracy, stamina, data.score, data.level);
        showLevelBadge(data.level, data.score);
        showToast("Saved! Score: " + parseFloat(data.score).toFixed(1) + " — " + data.level);
        clearForm();
        updateUI();
        fetchDashboard();
    })
    .catch(function () {
        // Offline fallback — mirrors Java calculateScore with sport-specific weights
        var cfg   = SPORT_CONFIG[sport] || SPORT_CONFIG["Running"];
        var score = (speed * cfg.wSpeed) + (accuracy * cfg.wAcc) + (stamina * cfg.wSta);
        var level = localLevel(score);
        saveToLocal(sport, d, t, speed, accuracy, stamina, score, level);
        showLevelBadge(level, score);
        showToast("Saved locally (server offline). Score: " + score.toFixed(1), "red");
        clearForm();
        updateUI();
    });
}

// ── Table + Chart ───────────────────────────────────────────────

function updateUI() {
    var sport   = getSport();
    var records = getRecords().filter(function (r) { return r.sport === sport; });
    var cfg     = SPORT_CONFIG[sport] || SPORT_CONFIG["Running"];
    var tbody   = document.getElementById("tableBody");
    if (!tbody) return;

    if (!records.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="10">No ' + escapeHtml(sport) + ' sessions yet. Add your first one!</td></tr>';
        setEl("avgSpeed", "0.00"); setEl("bestSpeed", "0.00");
        drawChart([], cfg); return;
    }

    var totalSpeed = 0, best = 0;
    tbody.innerHTML = "";

    records.forEach(function (r, i) {
        totalSpeed += r.speed;
        if (r.speed > best) best = r.speed;
        var cls = r.level === "Needs Improvement" ? "level-Needs" : "level-" + escapeHtml(r.level);
        tbody.innerHTML +=
            "<tr>" +
            "<td>" + (i + 1) + "</td>" +
            "<td>" + escapeHtml(r.sport) + "</td>" +
            "<td>" + r.d + "</td>" +
            "<td>" + r.t + " s</td>" +
            "<td>" + r.speed.toFixed(2) + "</td>" +
            "<td>" + r.accuracy + "</td>" +
            "<td>" + r.stamina + "</td>" +
            "<td>" + r.score.toFixed(1) + "</td>" +
            "<td><span class='level-badge " + cls + "' style='font-size:11px;padding:3px 10px'>" + escapeHtml(r.level) + "</span></td>" +
            "<td><button class='del-btn' onclick='deleteRow(" + i + ")'>✕</button></td>" +
            "</tr>";
    });

    setEl("avgSpeed",  (totalSpeed / records.length).toFixed(2));
    setEl("bestSpeed", best.toFixed(2));
    drawChart(records, cfg);
}

function drawChart(records, cfg) {
    var canvas = document.getElementById("chart");
    if (!canvas) return;
    var sportCfg = cfg || SPORT_CONFIG[getSport()] || SPORT_CONFIG["Running"];
    var labels = records.map(function (_, i) { return "#" + (i + 1); });
    var speeds = records.map(function (r) { return r.speed.toFixed(2); });
    if (chartInstance) chartInstance.destroy();
    chartInstance = new Chart(canvas, {
        type: "line",
        data: {
            labels: labels,
            datasets: [{
                label:                sportCfg.speed,
                data:                 speeds,
                fill:                 true,
                borderColor:          "#E83A2F",
                backgroundColor:      "rgba(232,58,47,0.08)",
                pointBackgroundColor: "#E83A2F",
                tension:              0.4
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { labels: { color: "#666" } } },
            scales: {
                x: { ticks: { color: "#666" }, grid: { color: "#1e1e1e" } },
                y: { ticks: { color: "#666" }, grid: { color: "#1e1e1e" } }
            }
        }
    });
}

// ── Delete / Reset / Logout ────────────────────────────────────

function deleteRow(i) {
    var sport   = getSport();
    var all     = getRecords();
    // Get the indices of records matching this sport, then remove the i-th match
    var sportIdxs = [];
    all.forEach(function (r, idx) { if (r.sport === sport) sportIdxs.push(idx); });
    if (i < sportIdxs.length) {
        all.splice(sportIdxs[i], 1);
        localStorage.setItem("records", JSON.stringify(all));
    }
    updateUI();
    showToast("Record removed.");
}

function resetData() {
    if (!confirm("Reset all local records?")) return;
    localStorage.removeItem("records");
    updateUI();
    showToast("All records cleared.");
}

function logout() {
    sessionStorage.clear();
    window.location.href = "login.html";
}

// ── Helpers ────────────────────────────────────────────────────

function getRecords() {
    return JSON.parse(localStorage.getItem("records")) || [];
}

function saveToLocal(sport, d, t, speed, accuracy, stamina, score, level) {
    var records = getRecords();
    records.push({ sport: sport, d: d, t: t, speed: speed, accuracy: accuracy, stamina: stamina, score: score, level: level });
    localStorage.setItem("records", JSON.stringify(records));
}

function setEl(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
}

/** Escapes HTML special characters to prevent XSS when inserting into innerHTML */
function escapeHtml(str) {
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function setPlaceholder(id, val) {
    var el = document.getElementById(id);
    if (el) el.placeholder = "e.g. " + val;
}

function clearForm() {
    ["distance", "time", "accuracy", "stamina"].forEach(function (id) {
        var el = document.getElementById(id); if (el) el.value = "";
    });
}

// Mirrors Java PerformanceLevel.getLevel() — offline fallback only
function localLevel(score) {
    if (score >= 85) return "Excellent";
    if (score >= 70) return "Good";
    if (score >= 50) return "Average";
    return "Needs Improvement";
}

function showLevelBadge(level, score) {
    var badge = document.getElementById("levelBadge"); if (!badge) return;
    var safeLevel = escapeHtml(level);
    var cls = level === "Needs Improvement" ? "level-Needs" : "level-" + safeLevel;
    badge.innerHTML = '<span class="level-badge ' + cls + '">Score: ' + parseFloat(score).toFixed(1) + ' — ' + safeLevel + '</span>';
}

function showToast(msg, type) {
    var t = document.getElementById("toast"); if (!t) return;
    t.textContent = msg;
    t.className   = "toast " + (type || "green") + " show";
    setTimeout(function () { t.classList.remove("show"); }, 2800);
}

function showErr(msg) {
    var el = document.getElementById("errMsg"); if (!el) return;
    el.textContent = msg; el.classList.add("show"); hideMsg("sucMsg");
}

function showSuc(msg) {
    var el = document.getElementById("sucMsg"); if (!el) return;
    el.textContent = msg; el.classList.add("show"); hideMsg("errMsg");
}

function hideMsg(id) {
    var el = document.getElementById(id); if (el) el.classList.remove("show");
}