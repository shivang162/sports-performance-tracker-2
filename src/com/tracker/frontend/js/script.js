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
 * POST /login → AuthController → AuthService → UserDAO → MySQL
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
    .then(function (res) {
        if (!res.ok) throw new Error("Invalid credentials");
        return res.text();
    })
    .then(function (msg) {
        showSuc(msg);
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

    fetchDashboard();   // pull DB stats + suggestion
    updateUI();         // render localStorage records in table + chart
}

// ── GET /dashboard → DashboardController ───────────────────────

/**
 * Returns: summary{average,trend,improvement,level,totalSessions}, scores[], suggestion
 * Updates: totalSessions, avgScore, stat-trend stat cards + suggestion box
 */
function fetchDashboard() {
    fetch(BASE_URL + "/dashboard")
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
 * Sends: athlete, distance, time, accuracy, stamina
 * Server: validates → speed = distance/time → calculateScore → level → MySQL
 * Returns: {success, athlete, speed, accuracy, stamina, score, level}
 * On offline: calculates score locally, saves to localStorage
 */
function saveData() {
    var d        = parseFloat(document.getElementById("distance").value);
    var t        = parseFloat(document.getElementById("time").value);
    var accuracy = parseFloat(document.getElementById("accuracy").value);
    var stamina  = parseFloat(document.getElementById("stamina").value);

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
            distance: d,
            time:     t,
            accuracy: accuracy,
            stamina:  stamina
        })
    })
    .then(function (res) { return res.json(); })
    .then(function (data) {
        if (!data.success) { showToast(data.error || "Save failed.", "red"); return; }
        saveToLocal(d, t, data.speed, accuracy, stamina, data.score, data.level);
        showLevelBadge(data.level, data.score);
        showToast("Saved! Score: " + parseFloat(data.score).toFixed(1) + " — " + data.level);
        clearForm();
        updateUI();
        fetchDashboard();
    })
    .catch(function () {
        // Offline fallback — mirrors Java calculateScore formula
        var score = (speed * 0.4) + (accuracy * 0.3) + (stamina * 0.3);
        var level = localLevel(score);
        saveToLocal(d, t, speed, accuracy, stamina, score, level);
        showLevelBadge(level, score);
        showToast("Saved locally (server offline). Score: " + score.toFixed(1), "red");
        clearForm();
        updateUI();
    });
}

// ── Table + Chart ───────────────────────────────────────────────

function updateUI() {
    var records = getRecords();
    var tbody   = document.getElementById("tableBody");
    if (!tbody) return;

    if (!records.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="9">No sessions yet. Add your first one!</td></tr>';
        setEl("avgSpeed", "0.00"); setEl("bestSpeed", "0.00");
        drawChart([]); return;
    }

    var totalSpeed = 0, best = 0;
    tbody.innerHTML = "";

    records.forEach(function (r, i) {
        totalSpeed += r.speed;
        if (r.speed > best) best = r.speed;
        var cls = r.level === "Needs Improvement" ? "level-Needs" : "level-" + r.level;
        tbody.innerHTML +=
            "<tr>" +
            "<td>" + (i + 1) + "</td>" +
            "<td>" + r.d + " m</td>" +
            "<td>" + r.t + " s</td>" +
            "<td>" + r.speed.toFixed(2) + "</td>" +
            "<td>" + r.accuracy + "</td>" +
            "<td>" + r.stamina + "</td>" +
            "<td>" + r.score.toFixed(1) + "</td>" +
            "<td><span class='level-badge " + cls + "' style='font-size:11px;padding:3px 10px'>" + r.level + "</span></td>" +
            "<td><button class='del-btn' onclick='deleteRow(" + i + ")'>✕</button></td>" +
            "</tr>";
    });

    setEl("avgSpeed",  (totalSpeed / records.length).toFixed(2));
    setEl("bestSpeed", best.toFixed(2));
    drawChart(records);
}

function drawChart(records) {
    var canvas = document.getElementById("chart");
    if (!canvas) return;
    var labels = records.map(function (_, i) { return "#" + (i + 1); });
    var speeds = records.map(function (r) { return r.speed.toFixed(2); });
    if (chartInstance) chartInstance.destroy();
    chartInstance = new Chart(canvas, {
        type: "line",
        data: {
            labels: labels,
            datasets: [{
                label:                "Speed (m/s)",
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
    var records = getRecords();
    records.splice(i, 1);
    localStorage.setItem("records", JSON.stringify(records));
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

function saveToLocal(d, t, speed, accuracy, stamina, score, level) {
    var records = getRecords();
    records.push({ d: d, t: t, speed: speed, accuracy: accuracy, stamina: stamina, score: score, level: level });
    localStorage.setItem("records", JSON.stringify(records));
}

function setEl(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
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
    var cls = level === "Needs Improvement" ? "level-Needs" : "level-" + level;
    badge.innerHTML = '<span class="level-badge ' + cls + '">Score: ' + parseFloat(score).toFixed(1) + ' — ' + level + '</span>';
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