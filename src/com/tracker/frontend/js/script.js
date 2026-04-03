// ═══════════════════════════════════════════════════════════════
//  script.js — Sports Performance Tracker
//  Shared by login.html and dashboard.html
//
//  login()          → POST /login     → AuthController
//  saveData()       → POST /save      → PerformanceController
//  fetchDashboard() → GET  /dashboard → DashboardController
//  fetchAthletes()  → GET  /athletes  → AthleteListController
// ═══════════════════════════════════════════════════════════════

const BASE_URL = "http://localhost:8080";
let chartInstance;

// ── Sport configuration ─────────────────────────────────────────
// Each sport defines sport-specific metric labels replacing generic
// "Accuracy" and "Stamina" inputs.
// m1: first sport-specific metric (always shown, 0–100).
// m2: second sport-specific metric (shown only when showM2 = true, 0–100).
// Weights match PerformanceService.SPORT_WEIGHTS on the server.

var SPORT_CONFIG = {
    "Running": {
        dist: "Distance (m)",       distPh: "400",
        time: "Time (sec)",         timePh: "60",
        m1:   "Sprint Form Score (0–100)",  m1Ph: "75",  showM1: true,
        m2:   null,                          m2Ph: "0",   showM2: false,
        speed: "Speed (m/s)",
        wSpeed: 0.60, wM1: 0.40, wM2: 0.00, maxSpeed: 10.5
    },
    "Swimming": {
        dist: "Distance (m)",       distPh: "100",
        time: "Time (sec)",         timePh: "90",
        m1:   "Stroke Efficiency (0–100)",  m1Ph: "70",  showM1: true,
        m2:   null,                          m2Ph: "0",   showM2: false,
        speed: "Speed (m/s)",
        wSpeed: 0.60, wM1: 0.40, wM2: 0.00, maxSpeed: 2.5
    },
    "Basketball": {
        dist: "Shots Attempted",    distPh: "20",
        time: "Duration (sec)",     timePh: "2880",
        m1:   "Shooting Efficiency (0–100)", m1Ph: "60", showM1: true,
        m2:   "Defense Rating (0–100)",      m2Ph: "65", showM2: true,
        speed: "Shot Rate",
        wSpeed: 0.00, wM1: 0.55, wM2: 0.45, maxSpeed: 1.0
    },
    "Football": {
        dist: "Distance Covered (m)", distPh: "10000",
        time: "Duration (sec)",       timePh: "5400",
        m1:   "Pass Completion % (0–100)", m1Ph: "75", showM1: true,
        m2:   "Fitness Score (0–100)",     m2Ph: "70", showM2: true,
        speed: "Speed (m/s)",
        wSpeed: 0.30, wM1: 0.40, wM2: 0.30, maxSpeed: 3.0
    },
    "Tennis": {
        dist: "Rallies",            distPh: "20",
        time: "Duration (sec)",     timePh: "3600",
        m1:   "First Serve % (0–100)",  m1Ph: "60", showM1: true,
        m2:   "Footwork Score (0–100)", m2Ph: "70", showM2: true,
        speed: "Rally Pace",
        wSpeed: 0.00, wM1: 0.55, wM2: 0.45, maxSpeed: 1.0
    },
    "Cycling": {
        dist: "Distance (km)",      distPh: "40",
        time: "Time (sec)",         timePh: "3600",
        m1:   "Cadence Score (0–100)", m1Ph: "75", showM1: true,
        m2:   null,                    m2Ph: "0",  showM2: false,
        speed: "Speed (km/s)",
        wSpeed: 0.70, wM1: 0.30, wM2: 0.00, maxSpeed: 0.015
    },
    "Weightlifting": {
        dist: "Weight (kg)",        distPh: "80",
        time: "Reps",               timePh: "8",
        m1:   "Technique Score (0–100)", m1Ph: "75", showM1: true,
        m2:   null,                      m2Ph: "0",  showM2: false,
        speed: "Load (kg/rep)",
        wSpeed: 0.40, wM1: 0.60, wM2: 0.00, maxSpeed: 25.0
    }
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

    // M1 field (always visible)
    setEl("lblM1", cfg.m1 || "Metric 1 (0–100)");
    setPlaceholder("m1", cfg.m1Ph);

    // M2 field — show or hide based on sport
    var m2Field = document.getElementById("m2Field");
    if (m2Field) m2Field.style.display = cfg.showM2 ? "" : "none";
    if (cfg.showM2) {
        setEl("lblM2", cfg.m2 || "Metric 2 (0–100)");
        setPlaceholder("m2", cfg.m2Ph);
    }

    // Update table headers
    setEl("thDistance", cfg.dist.split(" ")[0]);
    setEl("thSpeed",    cfg.speed);
    setEl("thM1",       cfg.m1 ? cfg.m1.split(" ")[0] : "M1");
    var thM2 = document.getElementById("thM2");
    if (thM2) {
        thM2.textContent = cfg.showM2 ? (cfg.m2 ? cfg.m2.split(" ")[0] : "M2") : "";
        thM2.style.display = cfg.showM2 ? "" : "none";
    }

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
 * POST /login → AuthController → AuthService → UserDAO → SQLite
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
        sessionStorage.setItem("userRole",  data.role || "athlete");
        var dest = (data.role === "coach") ? "dashboard.html" : "athlete.html";
        setTimeout(function () { window.location.href = dest; }, 800);
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
    // Redirect athletes away from the coach dashboard
    var role = sessionStorage.getItem("userRole") || "guest";
    if (role !== "coach" && role !== "guest") {
        window.location.href = "athlete.html"; return;
    }

    // Show logged-in user in navbar
    var email = sessionStorage.getItem("userEmail") || "coach@example.com";
    var badge = document.getElementById("userBadge");
    if (badge) badge.textContent = email;
    var roleBadge = document.getElementById("roleBadge");
    if (roleBadge) roleBadge.textContent = role === "coach" ? "Coach" : "Guest";

    // Pre-fill athlete input with coach's own email (can be overridden)
    var athleteInput = document.getElementById("athleteInput");
    if (athleteInput) athleteInput.value = email;

    // Populate athlete datalist from registered users
    fetchAthletes();

    // Apply initial sport labels
    onSportChange();
    // onSportChange() calls fetchDashboard() and updateUI(), so no extra calls needed
}

// ── GET /athletes → populate datalist ──────────────────────────

function fetchAthletes() {
    fetch(BASE_URL + "/athletes")
    .then(function (res) { return res.json(); })
    .then(function (emails) {
        var dl = document.getElementById("athleteList");
        if (!dl) return;
        dl.innerHTML = "";
        emails.forEach(function (email) {
            var opt = document.createElement("option");
            opt.value = email;
            dl.appendChild(opt);
        });
    })
    .catch(function () {
        console.warn("[Dashboard] /athletes unavailable");
    });
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
 * Sends: athlete, sport, distance, time, accuracy(=m1), stamina(=m2)
 * Server: validates → speed = distance/time → normalised score → level → SQLite
 *         → generates accuracy report
 * Returns: {success, athlete, sport, speed, m1, m2, score, level, report{...}}
 */
function saveData() {
    var d    = parseFloat(document.getElementById("distance").value);
    var t    = parseFloat(document.getElementById("time").value);
    var m1   = parseFloat(document.getElementById("m1").value);
    var sport= getSport();
    var cfg  = SPORT_CONFIG[sport] || SPORT_CONFIG["Running"];

    // m2 is optional — 0 when the sport doesn't use a second metric
    var m2El = document.getElementById("m2");
    var m2   = (cfg.showM2 && m2El && m2El.value !== "") ? parseFloat(m2El.value) : 0;

    var athleteInput = document.getElementById("athleteInput");
    var athlete = (athleteInput && athleteInput.value.trim())
                  ? athleteInput.value.trim()
                  : (sessionStorage.getItem("userEmail") || "Demo Athlete");

    if (!d || !t || isNaN(m1)) {
        showToast("Please fill in all required fields.", "red"); return;
    }
    if (t <= 0)             { showToast("Time must be greater than 0.", "red"); return; }
    if (m1 < 0 || m1 > 100){ showToast(cfg.m1 + " must be 0–100.", "red"); return; }
    if (cfg.showM2 && (m2 < 0 || m2 > 100)){ showToast(cfg.m2 + " must be 0–100.", "red"); return; }

    fetch(BASE_URL + "/save", {
        method:  "POST",
        headers: { "Content-Type": "application/json" },
        body:    JSON.stringify({
            athlete:  athlete,
            sport:    sport,
            distance: d,
            time:     t,
            accuracy: m1,   // stored in accuracy column, labelled as sport-specific m1
            stamina:  m2    // stored in stamina column, labelled as sport-specific m2
        })
    })
    .then(function (res) { return res.json(); })
    .then(function (data) {
        if (!data.success) { showToast(data.error || "Save failed.", "red"); return; }
        saveToLocal(athlete, sport, d, t, data.speed, m1, m2, data.score, data.level);
        showLevelBadge(data.level, data.score);
        showToast("Saved for " + escapeHtml(athlete) + "! Score: " + parseFloat(data.score).toFixed(1) + " — " + data.level);
        if (data.report) showAccuracyReport(data, cfg);
        clearForm();
        updateUI();
        fetchDashboard();
    })
    .catch(function () {
        // Offline fallback — mirrors server calculateScore with normalization
        var speed      = d / t;
        var maxSpd     = cfg.maxSpeed > 0 ? cfg.maxSpeed : 10.0;
        var normSpeed  = Math.min(speed / maxSpd * 100, 100);
        var score      = Math.min((normSpeed * cfg.wSpeed) + (m1 * cfg.wM1) + (m2 * cfg.wM2), 100);
        var level      = localLevel(score);
        saveToLocal(athlete, sport, d, t, speed, m1, m2, score, level);
        showLevelBadge(level, score);
        showToast("Saved locally (server offline). Score: " + score.toFixed(1), "red");
        clearForm();
        updateUI();
    });
}

// ── Accuracy Report ─────────────────────────────────────────────

/**
 * Renders a detailed accuracy/performance report after each save.
 * data: save response from the server (contains data.report).
 * cfg:  SPORT_CONFIG entry for the current sport.
 */
function showAccuracyReport(data, cfg) {
    var rpt = data.report;
    var reportDiv = document.getElementById("accuracyReport");
    var reportContent = document.getElementById("reportContent");
    if (!reportDiv || !reportContent) return;

    var score     = parseFloat(data.score).toFixed(1);
    var level     = escapeHtml(data.level);
    var levelCls  = data.level === "Needs Improvement" ? "level-Needs" : "level-" + level;

    // Score change indicator
    var changeHtml = "";
    if (rpt.hasPrev) {
        var chg   = parseFloat(rpt.scoreChange);
        var sign  = chg >= 0 ? "+" : "";
        var color = chg > 0 ? "var(--green)" : chg < 0 ? "var(--red)" : "var(--muted)";
        changeHtml = "<span style='color:" + color + ";font-size:13px'>" + sign + chg.toFixed(1) + " pts vs last session</span>";
    } else {
        changeHtml = "<span style='color:var(--muted);font-size:13px'>First session for this sport</span>";
    }

    // Distance to next level
    var nextHtml = "";
    if (rpt.nextLevel !== "Peak") {
        nextHtml = "<div style='font-size:13px;color:var(--muted);margin-top:6px'>" +
                   parseFloat(rpt.pointsToNext).toFixed(1) + " pts to reach <strong style='color:var(--text)'>" +
                   escapeHtml(rpt.nextLevel) + "</strong></div>";
    } else {
        nextHtml = "<div style='font-size:13px;color:var(--green);margin-top:6px'>🏆 Peak performance!</div>";
    }

    // Score breakdown bars
    var bars = "";
    var breakdownItems = [
        { label: "Speed (" + cfg.speed + ")", contrib: rpt.speedContrib, color: "#E83A2F",   hide: cfg.wSpeed === 0 },
        { label: cfg.m1 || "Metric 1",        contrib: rpt.m1Contrib,    color: "#F5C518",   hide: false },
        { label: cfg.m2 || "Metric 2",        contrib: rpt.m2Contrib,    color: "#60a5fa",   hide: !cfg.showM2 }
    ];
    breakdownItems.forEach(function (item) {
        if (item.hide) return;
        var pct = Math.round(parseFloat(item.contrib));
        bars += "<div class='report-bar-row'>" +
                "<span class='report-bar-label'>" + escapeHtml(item.label) + "</span>" +
                "<div class='report-bar-track'><div class='report-bar-fill' style='width:" + pct + "%;background:" + item.color + "'></div></div>" +
                "<span class='report-bar-pct'>" + pct + " pts</span>" +
                "</div>";
    });

    reportContent.innerHTML =
        "<div class='report-grid'>" +
        "<div class='report-item'>" +
          "<div class='report-item-label'>Score</div>" +
          "<div class='report-item-value' style='color:var(--accent)'>" + score + "</div>" +
          "<div style='margin-top:4px'><span class='level-badge " + levelCls + "' style='font-size:11px;padding:3px 10px'>" + level + "</span></div>" +
        "</div>" +
        "<div class='report-item'>" +
          "<div class='report-item-label'>Sessions (this sport)</div>" +
          "<div class='report-item-value' style='color:var(--green)'>" + (rpt.sessionCount || 1) + "</div>" +
          changeHtml +
        "</div>" +
        "<div class='report-item'>" +
          "<div class='report-item-label'>Next Goal</div>" +
          "<div class='report-item-value' style='color:var(--blue);font-size:20px'>" + (rpt.nextLevel === "Peak" ? "🏆" : escapeHtml(rpt.nextLevel)) + "</div>" +
          nextHtml +
        "</div>" +
        "</div>" +
        "<div style='margin-bottom:8px;font-size:12px;text-transform:uppercase;letter-spacing:1px;color:var(--muted)'>Score Breakdown</div>" +
        bars;

    reportDiv.style.display = "";
    reportDiv.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

// ── Table + Chart ───────────────────────────────────────────────

function updateUI() {
    var sport   = getSport();
    var records = getRecords().filter(function (r) { return r.sport === sport; });
    var cfg     = SPORT_CONFIG[sport] || SPORT_CONFIG["Running"];
    var tbody   = document.getElementById("tableBody");
    if (!tbody) return;

    if (!records.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="11">No ' + escapeHtml(sport) + ' sessions yet. Add your first one!</td></tr>';
        setEl("avgSpeed", "0.00"); setEl("bestSpeed", "0.00");
        drawChart([], cfg); return;
    }

    var totalSpeed = 0, best = 0;
    tbody.innerHTML = "";

    records.forEach(function (r, i) {
        var sportLabel = escapeHtml(r.sport);
        var d          = parseFloat(r.d);
        var t          = parseFloat(r.t);
        var speed      = parseFloat(r.speed);
        var m1         = parseFloat(r.accuracy);
        var m2         = parseFloat(r.stamina);
        var score      = parseFloat(r.score);
        var level      = escapeHtml(r.level);
        var athleteLabel = escapeHtml(r.athlete || "—");
        totalSpeed += speed;
        if (speed > best) best = speed;
        var cls = r.level === "Needs Improvement" ? "level-Needs" : "level-" + level;
        var m2Cell = cfg.showM2 ? "<td>" + m2 + "</td>" : "<td></td>";
        tbody.innerHTML +=
            "<tr>" +
            "<td>" + (i + 1) + "</td>" +
            "<td style='font-size:12px;color:var(--muted)'>" + athleteLabel + "</td>" +
            "<td>" + sportLabel + "</td>" +
            "<td>" + d + "</td>" +
            "<td>" + t + " s</td>" +
            "<td>" + speed.toFixed(2) + "</td>" +
            "<td>" + m1 + "</td>" +
            m2Cell +
            "<td>" + score.toFixed(1) + "</td>" +
            "<td><span class='level-badge " + cls + "' style='font-size:11px;padding:3px 10px'>" + level + "</span></td>" +
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
    var speeds = records.map(function (r) { return parseFloat(r.speed).toFixed(2); });
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

function saveToLocal(athlete, sport, d, t, speed, m1, m2, score, level) {
    var records = getRecords();
    // Store m1 in accuracy field and m2 in stamina field for local records
    records.push({ athlete: athlete, sport: sport, d: d, t: t, speed: speed, accuracy: m1, stamina: m2, score: score, level: level });
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
    ["distance", "time", "m1", "m2"].forEach(function (id) {
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