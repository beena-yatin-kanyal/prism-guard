<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Prism Guard Report<#if projectName?has_content> — ${projectName}</#if></title>
<style>
  /* ============================================================
     RESET & BASE
  ============================================================ */
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  html { font-size: 15px; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, sans-serif;
    background: #0d0d1a;
    color: #c9d1d9;
    min-height: 100vh;
    line-height: 1.6;
  }
  a { color: #58a6ff; text-decoration: none; }
  a:hover { text-decoration: underline; }

  /* ============================================================
     LAYOUT
  ============================================================ */
  .container { max-width: 1280px; margin: 0 auto; padding: 0 1.5rem; }
  .grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1.25rem; }
  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.25rem; }
  .grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1.25rem; }
  @media (max-width: 900px) {
    .grid-4 { grid-template-columns: repeat(2, 1fr); }
    .grid-3 { grid-template-columns: repeat(2, 1fr); }
  }
  @media (max-width: 600px) {
    .grid-4, .grid-3, .grid-2 { grid-template-columns: 1fr; }
  }

  /* ============================================================
     HEADER
  ============================================================ */
  .site-header {
    background: linear-gradient(135deg, #0f3460 0%, #16213e 60%, #1a1a2e 100%);
    border-bottom: 2px solid #e94560;
    padding: 1.5rem 0 1.25rem;
  }
  .header-inner {
    display: flex;
    align-items: center;
    gap: 1.25rem;
    flex-wrap: wrap;
  }
  .logo-svg {
    flex-shrink: 0;
    filter: drop-shadow(0 0 8px #e9456088);
  }
  .header-text { flex: 1; min-width: 200px; }
  .header-text h1 {
    font-size: 1.7rem;
    font-weight: 700;
    color: #ffffff;
    letter-spacing: 0.04em;
    line-height: 1.2;
  }
  .header-text .subtitle {
    color: #8b949e;
    font-size: 0.875rem;
    margin-top: 0.2rem;
  }
  .header-score-badge {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: rgba(255,255,255,0.06);
    border: 2px solid var(--grade-color, #58a6ff);
    border-radius: 12px;
    padding: 0.75rem 1.5rem;
    min-width: 110px;
  }
  .header-score-badge .score-num {
    font-size: 2.2rem;
    font-weight: 800;
    color: var(--grade-color, #58a6ff);
    line-height: 1;
  }
  .header-score-badge .score-grade {
    font-size: 0.875rem;
    color: #8b949e;
    margin-top: 0.2rem;
  }

  /* ============================================================
     MAIN CONTENT SECTIONS
  ============================================================ */
  .section { padding: 2rem 0 0; }
  .section-title {
    font-size: 1.1rem;
    font-weight: 700;
    color: #e6edf3;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    margin-bottom: 1rem;
    padding-bottom: 0.5rem;
    border-bottom: 1px solid #21262d;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .section-title .dot {
    width: 8px; height: 8px;
    border-radius: 50%;
    background: #e94560;
    display: inline-block;
    flex-shrink: 0;
  }

  /* ============================================================
     STAT CARDS
  ============================================================ */
  .stat-card {
    background: #161b22;
    border: 1px solid #21262d;
    border-radius: 10px;
    padding: 1.1rem 1.25rem;
    transition: border-color 0.2s;
  }
  .stat-card:hover { border-color: #30363d; }
  .stat-card .stat-label {
    font-size: 0.78rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #8b949e;
    margin-bottom: 0.4rem;
  }
  .stat-card .stat-value {
    font-size: 1.8rem;
    font-weight: 700;
    color: #e6edf3;
    line-height: 1.1;
  }
  .stat-card .stat-sub {
    font-size: 0.78rem;
    color: #6e7681;
    margin-top: 0.2rem;
  }

  /* ============================================================
     SCORE RING
  ============================================================ */
  .score-ring-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 1.5rem 0;
  }
  .score-ring {
    position: relative;
    width: 160px;
    height: 160px;
    border-radius: 50%;
    background: conic-gradient(
      var(--ring-color, #58a6ff) calc(var(--pct, 75) * 1%),
      #21262d calc(var(--pct, 75) * 1%)
    );
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .score-ring::before {
    content: "";
    position: absolute;
    inset: 14px;
    background: #161b22;
    border-radius: 50%;
  }
  .score-ring-inner {
    position: relative;
    z-index: 1;
    text-align: center;
  }
  .score-ring-inner .num {
    font-size: 2.2rem;
    font-weight: 800;
    color: var(--ring-color, #58a6ff);
    line-height: 1;
  }
  .score-ring-inner .grade-lbl {
    font-size: 0.8rem;
    color: #8b949e;
    margin-top: 0.2rem;
  }

  /* ============================================================
     PROGRESS BARS (category scores)
  ============================================================ */
  .bar-row {
    display: grid;
    grid-template-columns: 200px 1fr 60px 50px;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 0.75rem;
  }
  @media (max-width: 640px) {
    .bar-row { grid-template-columns: 1fr; }
  }
  .bar-label { font-size: 0.875rem; color: #c9d1d9; }
  .bar-track {
    background: #21262d;
    border-radius: 999px;
    height: 10px;
    overflow: hidden;
  }
  .bar-fill {
    height: 100%;
    border-radius: 999px;
    background: var(--bar-color, #58a6ff);
    width: var(--bar-w, 0%);
    transition: width 0.6s ease;
  }
  .bar-score { font-size: 0.875rem; color: #8b949e; text-align: right; }
  .grade-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px; height: 30px;
    border-radius: 6px;
    font-weight: 700;
    font-size: 0.9rem;
    background: var(--badge-bg, #161b22);
    color: var(--badge-color, #58a6ff);
    border: 1px solid var(--badge-color, #58a6ff);
  }

  /* ============================================================
     SEVERITY BADGES
  ============================================================ */
  .sev-badge {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 5px;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    white-space: nowrap;
  }
  .sev-CRITICAL { background: #6e000022; color: #ff4444; border: 1px solid #ff444455; }
  .sev-HIGH     { background: #7d300022; color: #f97316; border: 1px solid #f9731655; }
  .sev-MEDIUM   { background: #5a4a0022; color: #eab308; border: 1px solid #eab30855; }
  .sev-LOW      { background: #00286a22; color: #60a5fa; border: 1px solid #60a5fa55; }
  .sev-INFO     { background: #1e1e1e;   color: #8b949e; border: 1px solid #30363d; }

  /* ============================================================
     FINDINGS TABLE
  ============================================================ */
  .table-toolbar {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
    margin-bottom: 1rem;
    align-items: center;
  }
  .search-input {
    flex: 1;
    min-width: 180px;
    padding: 0.45rem 0.75rem;
    background: #161b22;
    border: 1px solid #30363d;
    border-radius: 7px;
    color: #c9d1d9;
    font-size: 0.875rem;
    outline: none;
  }
  .search-input:focus { border-color: #58a6ff; }
  .filter-select {
    padding: 0.45rem 0.75rem;
    background: #161b22;
    border: 1px solid #30363d;
    border-radius: 7px;
    color: #c9d1d9;
    font-size: 0.875rem;
    outline: none;
    cursor: pointer;
  }
  .filter-select:focus { border-color: #58a6ff; }
  .findings-count {
    font-size: 0.8rem;
    color: #8b949e;
    white-space: nowrap;
  }

  .table-wrap { overflow-x: auto; }
  table.findings-tbl {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.855rem;
  }
  table.findings-tbl thead th {
    background: #161b22;
    color: #8b949e;
    text-align: left;
    padding: 0.65rem 0.75rem;
    border-bottom: 2px solid #21262d;
    white-space: nowrap;
    cursor: pointer;
    user-select: none;
  }
  table.findings-tbl thead th:hover { color: #c9d1d9; }
  table.findings-tbl thead th .sort-icon { margin-left: 4px; opacity: 0.5; font-size: 0.7rem; }
  table.findings-tbl thead th.sorted .sort-icon { opacity: 1; color: #58a6ff; }
  table.findings-tbl tbody tr {
    border-bottom: 1px solid #21262d;
    transition: background 0.15s;
  }
  table.findings-tbl tbody tr:hover { background: #161b22; }
  table.findings-tbl tbody td {
    padding: 0.6rem 0.75rem;
    vertical-align: top;
    color: #c9d1d9;
  }
  table.findings-tbl tbody tr.hidden-row { display: none; }
  .rule-id-cell { font-family: "SFMono-Regular", Consolas, monospace; font-size: 0.8rem; color: #79c0ff; }
  .cat-chip {
    display: inline-block;
    padding: 0.1rem 0.45rem;
    background: #1f2937;
    border-radius: 4px;
    font-size: 0.72rem;
    color: #9ca3af;
    white-space: nowrap;
  }
  .file-cell { font-family: monospace; font-size: 0.78rem; color: #6e7681; white-space: nowrap; }
  .msg-cell { max-width: 360px; }
  .rec-cell { max-width: 280px; font-size: 0.8rem; color: #8b949e; }
  .no-results {
    text-align: center;
    padding: 2rem;
    color: #6e7681;
    font-size: 0.875rem;
    display: none;
  }

  /* ============================================================
     MODULE SCORES
  ============================================================ */
  .module-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.875rem;
  }
  .module-table th {
    background: #161b22;
    color: #8b949e;
    text-align: left;
    padding: 0.55rem 0.75rem;
    border-bottom: 2px solid #21262d;
  }
  .module-table td {
    padding: 0.55rem 0.75rem;
    border-bottom: 1px solid #21262d;
    color: #c9d1d9;
  }

  /* ============================================================
     SUMMARY SEVERITY CARDS
  ============================================================ */
  .sev-summary-grid {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
    margin-bottom: 1.5rem;
  }
  .sev-card {
    flex: 1;
    min-width: 90px;
    background: #161b22;
    border: 1px solid #21262d;
    border-radius: 9px;
    padding: 0.75rem 1rem;
    text-align: center;
  }
  .sev-card .sev-count { font-size: 1.6rem; font-weight: 700; }
  .sev-card .sev-name  { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.07em; margin-top: 0.2rem; }
  .sev-card.sev-CRITICAL .sev-count { color: #ff4444; }
  .sev-card.sev-HIGH     .sev-count { color: #f97316; }
  .sev-card.sev-MEDIUM   .sev-count { color: #eab308; }
  .sev-card.sev-LOW      .sev-count { color: #60a5fa; }
  .sev-card.sev-INFO     .sev-count { color: #8b949e; }

  /* ============================================================
     FOOTER
  ============================================================ */
  .site-footer {
    margin-top: 3rem;
    padding: 1.5rem 0;
    border-top: 1px solid #21262d;
    background: #0d0d1a;
  }
  .footer-inner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
    font-size: 0.8rem;
    color: #6e7681;
  }

  /* ============================================================
     GRADE HELPERS (CSS custom properties)
  ============================================================ */
  .grade-A { --grade-color: #22c55e; --ring-color: #22c55e; --bar-color: #22c55e;
             --badge-color: #22c55e; --badge-bg: #0a2010; }
  .grade-B { --grade-color: #06b6d4; --ring-color: #06b6d4; --bar-color: #06b6d4;
             --badge-color: #06b6d4; --badge-bg: #001620; }
  .grade-C { --grade-color: #eab308; --ring-color: #eab308; --bar-color: #eab308;
             --badge-color: #eab308; --badge-bg: #1a1500; }
  .grade-D { --grade-color: #f97316; --ring-color: #f97316; --bar-color: #f97316;
             --badge-color: #f97316; --badge-bg: #1a0a00; }
  .grade-F { --grade-color: #ef4444; --ring-color: #ef4444; --bar-color: #ef4444;
             --badge-color: #ef4444; --badge-bg: #1a0000; }

  /* Utility */
  .text-muted { color: #8b949e; }
  .mt-1 { margin-top: 0.5rem; }
  .pb-section { padding-bottom: 2rem; }
</style>
</head>
<body>

<!-- ============================================================
     HEADER
============================================================ -->
<header class="site-header">
  <div class="container">
    <div class="header-inner">

      <!-- Inline SVG prism logo -->
      <svg class="logo-svg" width="56" height="56" viewBox="0 0 56 56" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <polygon points="28,4 52,46 4,46" fill="none" stroke="#e94560" stroke-width="2.5"/>
        <polygon points="28,4 52,46 4,46" fill="url(#prismGrad)" opacity="0.18"/>
        <line x1="28" y1="4" x2="28" y2="46" stroke="#0f3460" stroke-width="1.5" stroke-dasharray="3,3"/>
        <line x1="4" y1="46" x2="40" y2="22" stroke="#58a6ff" stroke-width="1.2" opacity="0.6"/>
        <line x1="52" y1="46" x2="16" y2="22" stroke="#22c55e" stroke-width="1.2" opacity="0.6"/>
        <circle cx="28" cy="4" r="3" fill="#e94560"/>
        <circle cx="52" cy="46" r="3" fill="#58a6ff"/>
        <circle cx="4" cy="46" r="3" fill="#22c55e"/>
        <defs>
          <linearGradient id="prismGrad" x1="4" y1="46" x2="52" y2="4" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#e94560"/>
            <stop offset="100%" stop-color="#58a6ff"/>
          </linearGradient>
        </defs>
      </svg>

      <div class="header-text">
        <h1>Prism Guard</h1>
        <div class="subtitle">
          Java &amp; Spring Boot Architecture Analyzer
          <#if projectName?has_content>
            &nbsp;·&nbsp; <strong style="color:#c9d1d9">${projectName}</strong>
          </#if>
        </div>
      </div>

      <div class="header-score-badge grade-${overallGrade}">
        <span class="score-num">${overallScorePercent}</span>
        <span class="score-grade">Grade ${overallGrade}</span>
      </div>

    </div>
  </div>
</header>

<!-- ============================================================
     MAIN
============================================================ -->
<main class="container">

  <!-- ── SUMMARY CARDS ─────────────────────────────────────── -->
  <section class="section">
    <h2 class="section-title"><span class="dot"></span>Summary</h2>
    <div class="grid-4">
      <div class="stat-card">
        <div class="stat-label">Total Files</div>
        <div class="stat-value">${totalFiles}</div>
        <div class="stat-sub">${totalClasses} classes &nbsp;·&nbsp; ${totalMethods} methods</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Total Findings</div>
        <div class="stat-value">${totalFindings}</div>
        <div class="stat-sub">${totalFindings} issues detected</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Critical Issues</div>
        <div class="stat-value" style="color:#ff4444">${criticalCount}</div>
        <div class="stat-sub">Require immediate attention</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Categories Analyzed</div>
        <div class="stat-value">${categoriesAnalyzed}</div>
        <div class="stat-sub">Architecture rule categories</div>
      </div>
    </div>
  </section>

  <!-- ── OVERALL SCORE ─────────────────────────────────────── -->
  <section class="section pb-section">
    <h2 class="section-title"><span class="dot"></span>Overall Score</h2>
    <div class="grid-2">
      <div class="stat-card" style="display:flex; align-items:center; gap:2rem;">
        <div class="score-ring-wrapper" style="padding:0;">
          <div class="score-ring grade-${overallGrade}" style="--pct:${overallScorePercent}">
            <div class="score-ring-inner">
              <div class="num">${overallScorePercent}</div>
              <div class="grade-lbl">Grade ${overallGrade}</div>
            </div>
          </div>
        </div>
        <div>
          <div style="font-size:1.4rem; font-weight:700; color:#e6edf3;">
            ${report.overallScore.label()}
          </div>
          <div style="font-size:0.875rem; color:#8b949e; margin-top:0.3rem;">
            Score: <strong style="color:#e6edf3">${overallScorePercent}</strong> / 100
          </div>
          <div style="font-size:0.875rem; color:#8b949e; margin-top:0.2rem;">
            <#if report.overallScore.isPassing(report.failOnScoreBelow)>
              <span style="color:#22c55e">&#10003; Passing threshold</span>
            <#else>
              <span style="color:#ef4444">&#10007; Below passing threshold</span>
            </#if>
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-label" style="margin-bottom:0.75rem;">Project Info</div>
        <table style="width:100%; font-size:0.875rem; border-collapse:collapse;">
          <#if projectName?has_content>
          <tr><td style="color:#8b949e; padding:0.2rem 0; width:120px;">Name</td><td>${projectName}</td></tr>
          </#if>
          <#if projectRoot?has_content>
          <tr><td style="color:#8b949e; padding:0.2rem 0;">Root</td><td style="font-family:monospace; font-size:0.78rem; word-break:break-all;">${projectRoot}</td></tr>
          </#if>
          <tr><td style="color:#8b949e; padding:0.2rem 0;">Lines</td><td>${totalLines}</td></tr>
          <#if frameworks?has_content>
          <tr><td style="color:#8b949e; padding:0.2rem 0;">Frameworks</td><td>${frameworks}</td></tr>
          </#if>
          <tr><td style="color:#8b949e; padding:0.2rem 0;">Multi-module</td>
              <td>${isMultiModule?string("Yes","No")}</td></tr>
        </table>
      </div>
    </div>
  </section>

  <!-- ── SEVERITY SUMMARY ──────────────────────────────────── -->
  <section class="section pb-section">
    <h2 class="section-title"><span class="dot"></span>Findings by Severity</h2>
    <div class="sev-summary-grid">
      <#list severitySummary as s>
      <div class="sev-card sev-${s.name}">
        <div class="sev-count">${s.count}</div>
        <div class="sev-name">${s.display}</div>
      </div>
      </#list>
    </div>
  </section>

  <!-- ── CATEGORY SCORES ───────────────────────────────────── -->
  <#if categoryScoreList?has_content>
  <section class="section pb-section">
    <h2 class="section-title"><span class="dot"></span>Score by Category</h2>
    <div class="stat-card">
      <#list categoryScoreList as cs>
      <div class="bar-row">
        <span class="bar-label">${cs.name}</span>
        <div class="bar-track">
          <div class="bar-fill grade-${cs.grade}" style="--bar-w:${cs.barPct}%;"></div>
        </div>
        <span class="bar-score">${cs.value?string["0.0"]}</span>
        <span class="grade-badge grade-${cs.grade}">${cs.grade}</span>
      </div>
      </#list>
    </div>
  </section>
  </#if>

  <!-- ── MODULE SCORES ─────────────────────────────────────── -->
  <#if moduleScoreList?has_content>
  <section class="section pb-section">
    <h2 class="section-title"><span class="dot"></span>Module Scores</h2>
    <div class="table-wrap">
      <table class="module-table">
        <thead>
          <tr>
            <th>Module</th>
            <th>Score</th>
            <th>Grade</th>
            <th>Label</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <#list moduleScoreList as m>
          <tr>
            <td>${m.name}</td>
            <td>${m.value?string["0.0"]}</td>
            <td><span class="grade-badge grade-${m.grade}">${m.grade}</span></td>
            <td>${m.label}</td>
            <td>
              <#if m.passing>
                <span style="color:#22c55e; font-size:0.8rem;">&#10003; Passing</span>
              <#else>
                <span style="color:#ef4444; font-size:0.8rem;">&#10007; Failing</span>
              </#if>
            </td>
          </tr>
          </#list>
        </tbody>
      </table>
    </div>
  </section>
  </#if>

  <!-- ── FINDINGS TABLE ────────────────────────────────────── -->
  <section class="section pb-section">
    <h2 class="section-title"><span class="dot"></span>All Findings</h2>

    <!-- Toolbar -->
    <div class="table-toolbar">
      <input
        type="search"
        id="searchBox"
        class="search-input"
        placeholder="Search rules, messages, files..."
        oninput="applyFilters()"
        aria-label="Search findings"
      />
      <select id="sevFilter" class="filter-select" onchange="applyFilters()" aria-label="Filter by severity">
        <option value="">All Severities</option>
        <option value="CRITICAL">Critical</option>
        <option value="HIGH">High</option>
        <option value="MEDIUM">Medium</option>
        <option value="LOW">Low</option>
        <option value="INFO">Info</option>
      </select>
      <select id="catFilter" class="filter-select" onchange="applyFilters()" aria-label="Filter by category">
        <option value="">All Categories</option>
        <#list categoryScoreList as cs>
        <option value="${cs.key}">${cs.name}</option>
        </#list>
      </select>
      <span class="findings-count" id="findingsCount"></span>
    </div>

    <div class="table-wrap">
      <table class="findings-tbl" id="findingsTable">
        <thead>
          <tr>
            <th onclick="sortTable(0)" data-col="0">Severity <span class="sort-icon">&#8597;</span></th>
            <th onclick="sortTable(1)" data-col="1">Rule ID <span class="sort-icon">&#8597;</span></th>
            <th onclick="sortTable(2)" data-col="2">Category <span class="sort-icon">&#8597;</span></th>
            <th onclick="sortTable(3)" data-col="3">Message <span class="sort-icon">&#8597;</span></th>
            <th onclick="sortTable(4)" data-col="4">Location <span class="sort-icon">&#8597;</span></th>
            <th>Recommendation</th>
          </tr>
        </thead>
        <tbody id="findingsTbody">
          <#if findingsList?has_content>
          <#list findingsList as f>
          <tr class="finding-row"
              data-severity="${f.severity}"
              data-category="${f.category}"
              data-search="${f.ruleId?lower_case} ${f.message?lower_case} ${f.location?lower_case} ${f.categoryDisplay?lower_case}">
            <td><span class="sev-badge sev-${f.severity}">${f.severityDisplay}</span></td>
            <td class="rule-id-cell">${f.ruleId}</td>
            <td><span class="cat-chip">${f.categoryDisplay}</span></td>
            <td class="msg-cell">${f.message}</td>
            <td class="file-cell">${f.location}</td>
            <td class="rec-cell">${f.recommendation}</td>
          </tr>
          </#list>
          <#else>
          <tr><td colspan="6" style="text-align:center; padding:2rem; color:#6e7681;">No findings detected. Great work!</td></tr>
          </#if>
        </tbody>
      </table>
      <div class="no-results" id="noResults">No findings match the current filters.</div>
    </div>
  </section>

</main>

<!-- ============================================================
     FOOTER
============================================================ -->
<footer class="site-footer">
  <div class="container">
    <div class="footer-inner">
      <div>
        <strong style="color:#c9d1d9;">Prism Guard</strong>
        <#if report.prismGuardVersion?has_content>
          v${report.prismGuardVersion}
        </#if>
        &nbsp;·&nbsp; Analyzed at: ${generatedAt}
        &nbsp;·&nbsp; Duration: ${durationDisplay}
      </div>
      <div>
        Generated by <a href="https://github.com/prismguard/prism-guard" target="_blank" rel="noopener">Prism Guard</a>
      </div>
    </div>
  </div>
</footer>

<!-- ============================================================
     JAVASCRIPT — vanilla, no external dependencies
============================================================ -->
<script>
(function () {
  'use strict';

  // ── Filtering ────────────────────────────────────────────────────────────
  window.applyFilters = function () {
    var query   = document.getElementById('searchBox').value.toLowerCase().trim();
    var sevF    = document.getElementById('sevFilter').value;
    var catF    = document.getElementById('catFilter').value;
    var rows    = document.querySelectorAll('#findingsTbody .finding-row');
    var visible = 0;

    rows.forEach(function (row) {
      var matchSev  = !sevF || row.dataset.severity === sevF;
      var matchCat  = !catF || row.dataset.category === catF;
      var matchText = !query || row.dataset.search.includes(query);
      var show = matchSev && matchCat && matchText;
      row.classList.toggle('hidden-row', !show);
      if (show) visible++;
    });

    document.getElementById('findingsCount').textContent = visible + ' of ' + rows.length + ' findings';
    document.getElementById('noResults').style.display = (visible === 0 && rows.length > 0) ? 'block' : 'none';
  };

  // ── Sorting ──────────────────────────────────────────────────────────────
  var sortState = { col: -1, asc: true };

  window.sortTable = function (colIndex) {
    var tbody = document.getElementById('findingsTbody');
    var rows  = Array.from(tbody.querySelectorAll('.finding-row'));

    if (sortState.col === colIndex) {
      sortState.asc = !sortState.asc;
    } else {
      sortState.col = colIndex;
      sortState.asc = true;
    }

    rows.sort(function (a, b) {
      var aVal = a.cells[colIndex] ? a.cells[colIndex].textContent.trim() : '';
      var bVal = b.cells[colIndex] ? b.cells[colIndex].textContent.trim() : '';
      return sortState.asc ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
    });

    rows.forEach(function (row) { tbody.appendChild(row); });

    // Update header icons
    document.querySelectorAll('.findings-tbl thead th').forEach(function (th, i) {
      th.classList.toggle('sorted', i === colIndex);
      var icon = th.querySelector('.sort-icon');
      if (icon) icon.textContent = i === colIndex ? (sortState.asc ? '\u25B2' : '\u25BC') : '\u2195';
    });

    applyFilters();
  };

  // ── Init ─────────────────────────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', function () {
    applyFilters();
  });

}());
</script>

</body>
</html>
