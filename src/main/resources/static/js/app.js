/* ---------- Configuración ---------- */
const API = "";
const PAGE = 12;
const TABS = {
  platforms:  { label: "Plataformas",     url: "/api/admin/platforms",  extra: "manufacturer", extraLabel: "Fabricante" },
  genres:     { label: "Géneros",         url: "/api/admin/genres",     extra: "description",  extraLabel: "Descripción" },
  developers: { label: "Desarrolladores", url: "/api/admin/developers", extra: "country",      extraLabel: "País" },
};
const SECTIONS = {
  home:     { title: "Nuevos y en tendencia", sub: "Los juegos del catálogo, del más reciente al más antiguo", order: "added" },
  recent:   { title: "Últimos agregados", sub: "Los 10 juegos agregados más recientemente", order: "added", limit: 10 },
  byyear:   { title: "Lanzamientos recientes", sub: "Ordenados por año de lanzamiento", order: "year-desc" },
  all:      { title: "Todos los juegos", sub: "Todo el catálogo de GameLog", order: "name" },
  classics: { title: "Clásicos", sub: "Juegos lanzados en 2015 o antes", order: "year-asc", filter: g => g.year && g.year <= 2015 },
};
const SORTERS = {
  added: (a, b) => b.id - a.id,
  name: (a, b) => a.title.localeCompare(b.title),
  "year-desc": (a, b) => (b.year || 0) - (a.year || 0),
  "year-asc": (a, b) => (a.year || 9999) - (b.year || 9999),
};
const state = {
  token: localStorage.getItem("token"), email: localStorage.getItem("email"), rol: localStorage.getItem("rol"),
  view: "discover", sec: "home", secValue: "", search: "", order: "relevance", plFilter: "",
  layout: "grid", shown: PAGE, tab: "platforms", games: [],
}; state.closed = JSON.parse(localStorage.getItem("closedGroups") || "{}");

/* ---------- Utilidades ---------- */
const $ = (s) => document.querySelector(s);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, c =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const list = (d) => (Array.isArray(d) ? d : d?.content ?? []);
const isAdmin = () => state.rol === "ADMIN";
const initials = (t) => t.split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0]).join("").toUpperCase();
const svg = (p) => `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${p}</svg>`;
const ICON = {
  home: svg('<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/>'),
  star: svg('<path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"/>'),
  calendar: svg('<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>'),
  game: svg('<rect x="2" y="7" width="20" height="11" rx="5"/><path d="M7 10v5M4.5 12.5h5"/><circle cx="16" cy="11" r=".6"/><circle cx="18" cy="14" r=".6"/>'),
  trophy: svg('<path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0z"/><path d="M7 6H4a3 3 0 0 0 3 4M17 6h3a3 3 0 0 1-3 4"/>'),
  chart: svg('<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>'),
  gear: svg('<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1"/>'),
  grid: svg('<rect x="4" y="4" width="6" height="6" rx="1"/><rect x="14" y="4" width="6" height="6" rx="1"/><rect x="4" y="14" width="6" height="6" rx="1"/><rect x="14" y="14" width="6" height="6" rx="1"/>'),
  list: svg('<path d="M4 6h16M4 12h16M4 18h16"/>'),
};
ICON.chev = svg('<path d="M6 9l6 6 6-6"/>');
const SUN = svg('<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>');
const MOON = svg('<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>');

function grad(s) {
  let h = 0;
  for (const c of String(s)) h = (h * 31 + c.charCodeAt(0)) % 360;
  return `linear-gradient(135deg,hsl(${h},70%,82%),hsl(${(h + 50) % 360},70%,76%))`;
}

function toast(msg, err = false) {
  const t = $("#toast");
  t.textContent = msg; t.className = "toast" + (err ? " err" : "");
  setTimeout(() => t.classList.add("hidden"), 3000);
}

async function api(path, opts = {}) {
  const headers = { "Content-Type": "application/json", ...(opts.headers || {}) };
  if (state.token) headers.Authorization = "Bearer " + state.token;
  const res = await fetch(API + path, { ...opts, headers });
  if (!res.ok) {
    let msg = res.status + " " + res.statusText;
    try {
      const j = await res.json();
      msg = j.detail || j.error || j.message || msg;
      if (j.errors) msg += ": " + Object.entries(j.errors).map(([k, v]) => k + " " + v).join(", ");
    } catch {}
    if (res.status === 401 || res.status === 403)
      msg = state.token ? "Sin permiso o sesión expirada. Vuelve a iniciar sesión." : "Inicia sesión para hacer esto.";
    throw new Error(msg);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

const fetchAll = async (url) => list(await api(url + "?size=200"));

/* Game del backend: id, titulo, plataformas (texto), anio, genre{}, platforms[] */
function norm(g) {
  return {
    id: g.id,
    title: g.titulo ?? "Sin título",
    year: g.anio ?? "",
    genre: g.genre?.name ?? "",
    platforms: g.platforms ?? [],
    legacy: g.plataformas ?? "",
  };
}
const allGames = async () => list(await api("/api/games")).map(norm);

async function load() {
  state.games = await allGames();
  $("#q").placeholder = `Buscar entre ${state.games.length} juegos...`;
  renderSide();
}

/* ---------- Barra lateral ---------- */
function renderSide() {
  const disc = state.view === "discover";
  const on = (c) => (c ? " on" : "");
  const sec = (k) => disc && !state.search && state.sec === k;
  const plats = [...new Set(state.games.flatMap(g => g.platforms.map(p => p.name)))].sort();
  const genres = [...new Set(state.games.map(g => g.genre).filter(Boolean))].sort();

  const group = (id, title, body) => `
    <button class="grp${state.closed[id] ? " closed" : ""}" data-grp="${id}">
      <span>${title}</span><span class="chev">${ICON.chev}</span>
    </button>
    <div class="grp-body${state.closed[id] ? " closed" : ""}" data-body="${id}"><div class="grp-in">${body}</div></div>`;

  $("#side").innerHTML = `
    <div class="brand">GAMELOG</div>
    <button class="item big${on(sec("home"))}" data-sec="home"><span class="ico">${ICON.home}</span>Inicio</button>

    ${group("new", "Nuevos lanzamientos", `
      <button class="item${on(sec("recent"))}" data-sec="recent"><span class="ico">${ICON.star}</span>Últimos agregados</button>
      <button class="item${on(sec("byyear"))}" data-sec="byyear"><span class="ico">${ICON.calendar}</span>Por año de lanzamiento</button>`)}

    ${group("explore", "Explorar", `
      <button class="item${on(sec("all"))}" data-sec="all"><span class="ico">${ICON.game}</span>Todos los juegos</button>
      <button class="item${on(sec("classics"))}" data-sec="classics"><span class="ico">${ICON.trophy}</span>Clásicos</button>
      <button class="item${on(state.view === "dashboard")}" data-view="dashboard"><span class="ico">${ICON.chart}</span>Estadísticas</button>`)}

    ${group("plats", "Plataformas",
      plats.map(n => `<button class="item${on(sec("platform") && state.secValue === n)}" data-plat="${esc(n)}"><span class="ico">${esc(n[0])}</span>${esc(n)}</button>`).join("")
        || `<small class="muted pad">Sin datos</small>`)}

    ${group("genres", "Géneros",
      genres.map(n => `<button class="item${on(sec("genre") && state.secValue === n)}" data-genre="${esc(n)}"><span class="ico">${esc(n[0])}</span>${esc(n)}</button>`).join("")
        || `<small class="muted pad">Sin datos</small>`)}

    ${isAdmin() ? group("admin", "Administrar", `
      <button class="item${on(state.view === "admin")}" data-view="admin"><span class="ico">${ICON.gear}</span>Administración</button>`) : ""}`;
}

$("#side").addEventListener("click", e => {
  const b = e.target.closest("button");
  if (!b) return;
  if (b.dataset.sec) openSection(b.dataset.sec);
  else if (b.dataset.plat) openSection("platform", b.dataset.plat);
  else if (b.dataset.genre) openSection("genre", b.dataset.genre);
  else if (b.dataset.view) go(b.dataset.view);
});

$("#side").addEventListener("click", e => {
  const h = e.target.closest(".grp");
  if (!h) return;
  const id = h.dataset.grp;
  const closed = !h.classList.contains("closed");
  h.classList.toggle("closed", closed);
  $(`[data-body="${id}"]`).classList.toggle("closed", closed);
  state.closed[id] = closed;
  localStorage.setItem("closedGroups", JSON.stringify(state.closed));
});

/* ---------- Tema claro/oscuro ---------- */
function applyTheme(t) {
  document.documentElement.dataset.theme = t;
  localStorage.setItem("theme", t);
  $("#themeBtn").innerHTML = t === "dark" ? SUN : MOON;
  $("#themeBtn").title = t === "dark" ? "Cambiar a modo claro" : "Cambiar a modo oscuro";
}
$("#themeBtn").onclick = () =>
  applyTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark");
applyTheme(document.documentElement.dataset.theme);

function openSection(sec, value = "") {
  Object.assign(state, { sec, secValue: value, search: "", order: "relevance", plFilter: "", shown: PAGE });
  $("#q").value = "";
  go("discover");
  window.scrollTo(0, 0);
}

function go(view) {
  state.view = view;
  renderSide();
  ({ discover, dashboard, admin })[view]();
}

let timer;
$("#q").addEventListener("input", e => {
  clearTimeout(timer);
  timer = setTimeout(() => {
    state.search = e.target.value.trim();
    if (state.search) { state.sec = "all"; state.secValue = ""; }
    state.shown = PAGE;
    go("discover");
  }, 300);
});

/* ---------- Inicio / descubrir (estilo RAWG) ---------- */
const card = (g) => `
  <article class="g-card" data-id="${g.id}">
    <div class="media" style="background:${grad(g.title)}">${esc(initials(g.title))}</div>
    <div class="g-body">
      <div class="pfs">${g.platforms.map(p => `<span class="pf">${esc(p.name)}</span>`).join("") || `<span class="pf">—</span>`}</div>
      <h3 class="g-title">${esc(g.title)}</h3>
      <div class="extra">
        <div class="xrow"><span>Año de lanzamiento</span><b>${esc(g.year || "—")}</b></div>
        <div class="xrow"><span>Género</span><b>${esc(g.genre || "—")}</b></div>
      </div>
    </div>
  </article>`;

function discover() {
  const v = $("#view");
  const sec = SECTIONS[state.sec] || {};
  let title = sec.title, sub = sec.sub;
  if (state.search) { title = `Resultados para «${state.search}»`; sub = "Búsqueda en el catálogo"; }
  else if (state.sec === "platform") { title = "Plataforma: " + state.secValue; sub = "Juegos disponibles en esta plataforma"; }
  else if (state.sec === "genre") { title = "Género: " + state.secValue; sub = "Juegos de este género"; }

  let items = [...state.games];
  const q = state.search.toLowerCase();
  if (q) items = items.filter(g => g.title.toLowerCase().includes(q));
  if (state.sec === "platform") items = items.filter(g => g.platforms.some(p => p.name === state.secValue));
  if (state.sec === "genre") items = items.filter(g => g.genre === state.secValue);
  if (sec.filter) items = items.filter(sec.filter);
  if (state.plFilter) items = items.filter(g => g.platforms.some(p => p.name === state.plFilter));
  items.sort(SORTERS[state.order === "relevance" ? (sec.order || "added") : state.order]);
  if (sec.limit && !q) items = items.slice(0, sec.limit);

  const plats = [...new Set(state.games.flatMap(g => g.platforms.map(p => p.name)))].sort();
  const shown = items.slice(0, state.shown);
  const orders = [["relevance", "Relevancia"], ["added", "Agregado recientemente"], ["name", "Nombre"],
                  ["year-desc", "Año (reciente primero)"], ["year-asc", "Año (antiguo primero)"]];

  v.innerHTML = `
    <h1 class="page-title">${esc(title)}</h1>
    <p class="muted">${esc(sub)}</p>
    <div class="controls">
      <label class="sel">Ordenar por:
        <select id="ord">${orders.map(([k, l]) => `<option value="${k}"${state.order === k ? " selected" : ""}>${l}</option>`).join("")}</select>
      </label>
      <label class="sel">
        <select id="plt"><option value="">Todas las plataformas</option>${plats.map(n => `<option${state.plFilter === n ? " selected" : ""}>${esc(n)}</option>`).join("")}</select>
      </label>
      <span class="muted">${items.length} juegos</span>
      <div class="disp"><span class="muted">Vista:</span>
        <button data-l="grid" class="${state.layout === "grid" ? "on" : ""}" title="Cuadrícula">${ICON.grid}</button>
        <button data-l="list" class="${state.layout === "list" ? "on" : ""}" title="Lista">${ICON.list}</button>
      </div>
    </div>
    <div class="games ${state.layout}">${shown.map(card).join("") || "<p class='muted'>No hay juegos que coincidan.</p>"}</div>
    ${items.length > shown.length ? `<div class="more"><button class="btn" id="more">Cargar más</button></div>` : ""}`;

  $("#ord").onchange = e => { state.order = e.target.value; state.shown = PAGE; discover(); };
  $("#plt").onchange = e => { state.plFilter = e.target.value; state.shown = PAGE; discover(); };
  v.querySelectorAll("[data-l]").forEach(b => b.onclick = () => { state.layout = b.dataset.l; discover(); });
  if ($("#more")) $("#more").onclick = () => { state.shown += PAGE; discover(); };
  v.querySelectorAll(".g-card").forEach(el => el.onclick = () => openGame(el.dataset.id));
}

/* ---------- Estadísticas ---------- */
function countBy(items, fn) {
  const m = {};
  items.forEach(i => fn(i).forEach(k => k && (m[k] = (m[k] || 0) + 1)));
  return Object.entries(m).sort((a, b) => b[1] - a[1]);
}

async function dashboard() {
  const v = $("#view");
  v.innerHTML = `<h1>Estadísticas</h1><p>Cargando...</p>`;
  try {
    await load();
    const games = state.games;
    const byGenre = countBy(games, g => [g.genre]);
    const byPlat = countBy(games, g => g.platforms.map(p => p.name));
    const byYear = countBy(games, g => [String(g.year)]).sort((a, b) => a[0] - b[0]);
    let nPl = byPlat.length, nGe = byGenre.length, nDe = "—";
    if (isAdmin()) {
      const [pl, ge, de] = await Promise.allSettled(Object.values(TABS).map(t => fetchAll(t.url)));
      if (pl.status === "fulfilled") nPl = pl.value.length;
      if (ge.status === "fulfilled") nGe = ge.value.length;
      if (de.status === "fulfilled") nDe = de.value.length;
    }
    const max = Math.max(1, ...byGenre.map(x => x[1]));

    v.innerHTML = `
      <h1 class="page-title">Estadísticas</h1><p class="muted">Resumen del catálogo de GameLog</p>
      <div class="stats" style="margin-top:14px">
        <div class="stat c0"><small>Juegos</small><b>${games.length}</b></div>
        <div class="stat c1"><small>Plataformas</small><b>${nPl}</b></div>
        <div class="stat c2"><small>Géneros</small><b>${nGe}</b></div>
        <div class="stat c0"><small>Desarrolladores</small><b>${nDe}</b></div>
      </div>
      <div class="grid-dash">
        <section class="card span-8 chart">
          <h3>Juegos por año de lanzamiento</h3><small>Cantidad en el catálogo</small>
          ${lineChart(byYear.map(x => x[1]))}
          <small>${byYear.map(x => x[0]).join(" · ")}</small>
        </section>
        <section class="card span-4">
          <h3>Recientes</h3><small>Últimos agregados</small>
          <div class="course-list">
            ${games.slice(-3).reverse().map((g, i) => `
              <div class="course c${i}" data-id="${g.id}">
                <div class="th" style="background:${grad(g.title)}"></div>
                <div><b>${esc(g.title)}</b><br><small>${esc(g.year)} · ${esc(g.genre)}</small></div>
              </div>`).join("") || "<p class='muted'>Aún no hay juegos.</p>"}
          </div>
        </section>
        <section class="card span-7">
          <h3>Juegos por género</h3>
          <div class="bars">
            ${byGenre.slice(0, 5).map(([k, c]) => `
              <div class="bar-row"><div>${esc(k)}<div class="bar"><i style="width:${(c / max) * 100}%"></i></div></div><b>${c}</b></div>`).join("")
              || "<p class='muted'>Sin datos</p>"}
          </div>
        </section>
        <section class="card span-5">
          <h3>Plataformas</h3><small>Juegos disponibles</small>
          <div class="rank">
            ${byPlat.slice(0, 5).map(([k, c]) => `<div class="rank-item"><span class="dot">${esc(k[0])}</span>${esc(k)}<b>${c}</b></div>`).join("")
              || "<p class='muted'>Sin datos</p>"}
          </div>
        </section>
      </div>`;
    v.querySelectorAll(".course").forEach(el => el.addEventListener("click", () => openGame(el.dataset.id)));
  } catch (e) { v.innerHTML = `<h1>Estadísticas</h1><p>No se pudo cargar: ${esc(e.message)}</p>`; }
}

function lineChart(vals) {
  if (vals.length < 2) return `<p class="muted">Se necesitan juegos de al menos 2 años distintos.</p>`;
  const W = 600, H = 160, max = Math.max(...vals);
  const pts = vals.map((v, i) => [(i / (vals.length - 1)) * W, H - (v / max) * (H - 24) - 10]);
  const line = pts.map((p, i) => (i ? "L" : "M") + p[0].toFixed(1) + " " + p[1].toFixed(1)).join(" ");
  return `<svg viewBox="0 0 ${W} ${H}" preserveAspectRatio="none">
    <defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#6c5ce7" stop-opacity=".25"/><stop offset="1" stop-color="#6c5ce7" stop-opacity="0"/>
    </linearGradient></defs>
    <path d="${line} L${W} ${H} L0 ${H} Z" fill="url(#g)"/>
    <path d="${line}" fill="none" stroke="#6c5ce7" stroke-width="3" stroke-linejoin="round" stroke-linecap="round"/>
  </svg>`;
}

/* ---------- Detalle de juego ---------- */
async function refresh() { await load(); go(state.view); }

async function openGame(id) {
  showModal(`<p>Cargando...</p>`);
  try {
    const g = (await allGames()).find(x => String(x.id) === String(id));
    if (!g) throw new Error("Juego no encontrado");
    const allPlats = isAdmin() ? await fetchAll(TABS.platforms.url) : [];
    const mine = new Set(g.platforms.map(p => p.id));
    const free = allPlats.filter(p => !mine.has(p.id));

    showModal(`
      <div class="cover" style="background:${grad(g.title)}">${esc(initials(g.title))}</div>
      <h2 style="margin-top:12px">${esc(g.title)}</h2>
      <p class="muted">${esc(g.year)} · ${esc(g.genre || "Sin género")}</p>
      <h3 style="margin-top:12px">Plataformas</h3>
      <div>${g.platforms.map(p => `<span class="chip">${esc(p.name)}${isAdmin() ? `<button data-rm="${p.id}">×</button>` : ""}</span>`).join("")
        || `<small class="muted">${esc(g.legacy) || "Sin plataformas asociadas"}</small>`}</div>
      ${isAdmin() && free.length ? `
        <div class="row">
          <select id="pSel">${free.map(p => `<option value="${p.id}">${esc(p.name)}</option>`).join("")}</select>
          <button class="btn light" id="pAdd">Asociar plataforma</button>
        </div>` : ""}
      <div class="row">
        ${isAdmin() ? `<button class="btn light" id="gDel" style="color:var(--danger)">Eliminar juego</button>` : ""}
        <button class="btn" id="closeBtn">Cerrar</button>
      </div>`);
    $("#closeBtn").onclick = hideModal;
    document.querySelectorAll("[data-rm]").forEach(b => b.onclick = () =>
      act(() => api(`/api/games/${id}/platforms/${b.dataset.rm}`, { method: "DELETE" }), "Plataforma quitada")
        .then(async () => { await refresh(); openGame(id); }));
    if ($("#pAdd")) $("#pAdd").onclick = () =>
      act(() => api(`/api/games/${id}/platforms/${$("#pSel").value}`, { method: "POST" }), "Plataforma asociada")
        .then(async () => { await refresh(); openGame(id); });
    if ($("#gDel")) $("#gDel").onclick = () => {
      if (confirm("¿Eliminar este juego?"))
        act(() => api("/api/games/" + id, { method: "DELETE" }), "Juego eliminado")
          .then(async () => { hideModal(); await refresh(); });
    };
  } catch (e) { showModal(`<p>Error: ${esc(e.message)}</p><button class="btn" onclick="hideModal()">Cerrar</button>`); }
}

async function act(fn, okMsg) {
  if (!state.token) { toast("Inicia sesión primero", true); authDialog(); return; }
  try { await fn(); toast(okMsg); } catch (e) { toast(e.message, true); }
}

/* ---------- Administración ---------- */
async function admin() {
  const v = $("#view");
  if (!isAdmin()) {
    v.innerHTML = `<h1>Administración</h1><p class="muted">Inicia sesión con una cuenta ADMIN para ver esta sección.</p>`;
    return;
  }
  const tab = TABS[state.tab];
  v.innerHTML = `<h1>Administración</h1><p>Cargando...</p>`;
  let genres = [], items = [];
  try { genres = await fetchAll(TABS.genres.url); } catch {}
  try { items = await fetchAll(tab.url); } catch (e) { toast(e.message, true); }

  v.innerHTML = `
    <h1 class="page-title">Administración</h1>
    <div class="grid-dash" style="margin-top:14px">
      <section class="card span-5">
        <h3>Nuevo juego</h3>
        <div class="row"><input id="gt" placeholder="Título"></div>
        <div class="row"><input id="gy" type="number" placeholder="Año (ej. 2020)">
          <select id="gg"><option value="">Sin género</option>${genres.map(g => `<option value="${g.id}">${esc(g.name)}</option>`).join("")}</select></div>
        <button class="btn" id="gAdd">Crear juego</button>
      </section>
      <section class="card span-7">
        <div class="tabs" style="display:inline-flex;margin-bottom:12px">
          ${Object.entries(TABS).map(([k, t]) => `<button class="pill ${k === state.tab ? "active" : ""}" data-tab="${k}">${t.label}</button>`).join("")}
        </div>
        <div class="row"><input id="nn" placeholder="Nombre"><input id="ne" placeholder="${tab.extraLabel} (opcional)"><button class="btn" id="nAdd" style="flex:none">Agregar</button></div>
        ${items.map(i => `<div class="list-item"><span>${esc(i.name)} <small>${esc(i[tab.extra] || "")}</small></span><button class="del" data-del="${i.id}">Eliminar</button></div>`).join("")
          || "<p class='muted'>Sin registros.</p>"}
      </section>
    </div>`;

  const reload = async () => { await load(); admin(); };
  document.querySelectorAll("[data-tab]").forEach(b => b.onclick = () => { state.tab = b.dataset.tab; admin(); });
  $("#nAdd").onclick = () => act(() => api(tab.url, { method: "POST",
    body: JSON.stringify({ name: $("#nn").value, [tab.extra]: $("#ne").value || null }) }), "Agregado").then(reload);
  document.querySelectorAll("[data-del]").forEach(b => b.onclick = () => {
    if (confirm("¿Eliminar?")) act(() => api(`${tab.url}/${b.dataset.del}`, { method: "DELETE" }), "Eliminado").then(reload);
  });
  $("#gAdd").onclick = () => {
    const body = { titulo: $("#gt").value };
    if ($("#gy").value) body.anio = Number($("#gy").value);
    if ($("#gg").value) body.genre = { id: Number($("#gg").value) };
    act(() => api("/api/games", { method: "POST", body: JSON.stringify(body) }), "Juego creado").then(reload);
  };
}

/* ---------- Modal ---------- */
function showModal(html) { $("#modalBox").innerHTML = html; $("#modal").classList.remove("hidden"); }
function hideModal() { $("#modal").classList.add("hidden"); }
$("#modal").addEventListener("click", e => { if (e.target.id === "modal") hideModal(); });

/* ---------- Login / Registro ---------- */
function authDialog() {
  if (state.token) {
    showModal(`<h3>Sesión: ${esc(state.email)}</h3><p class="muted">Rol: ${esc(state.rol)}</p><div class="row"><button class="btn" id="lo">Cerrar sesión</button></div>`);
    $("#lo").onclick = () => {
      localStorage.clear(); state.token = state.email = state.rol = null;
      hideModal(); updateUser(); openSection("home");
    };
    return;
  }
  showModal(`
    <h3>Iniciar sesión / Registrarse</h3>
    <div class="row"><input id="au" placeholder="Usuario (solo para registro)"></div>
    <div class="row"><input id="ae" type="email" placeholder="Correo"></div>
    <div class="row"><input id="ap" type="password" placeholder="Contraseña (mínimo 6)"></div>
    <div class="row"><button class="btn" id="li">Entrar</button><button class="btn light" id="re">Registrarme</button></div>`);
  $("#li").onclick = async () => {
    try {
      const d = await api("/api/auth/login", { method: "POST", body: JSON.stringify({ email: $("#ae").value, password: $("#ap").value }) });
      state.token = d.token; state.email = d.email; state.rol = d.rol;
      localStorage.setItem("token", d.token); localStorage.setItem("email", d.email); localStorage.setItem("rol", d.rol);
      hideModal(); updateUser(); toast("Bienvenido"); go(state.view);
    } catch (e) { toast(e.message, true); }
  };
  $("#re").onclick = async () => {
    try {
      await api("/api/auth/register", { method: "POST", body: JSON.stringify({ username: $("#au").value, email: $("#ae").value, password: $("#ap").value }) });
      toast("Registrado. Ahora presiona Entrar");
    } catch (e) { toast(e.message, true); }
  };
}

function updateUser() {
  $("#auth").innerHTML = state.token
    ? `<a href="/swagger-ui/index.html" target="_blank" rel="noopener">API</a>
       <button class="avatar" id="userBtn" title="${esc(state.email)}">${esc((state.email || "?")[0].toUpperCase())}</button>`
    : `<button id="userBtn">Iniciar sesión</button><button id="regBtn">Registrarse</button>
       <a href="/swagger-ui/index.html" target="_blank" rel="noopener">API</a>`;
  $("#userBtn").onclick = authDialog;
  if ($("#regBtn")) $("#regBtn").onclick = authDialog;
  renderSide();
}

/* ---------- Inicio ---------- */
(async () => {
  updateUser();
  try { await load(); } catch (e) { toast(e.message, true); }
  openSection("home");
})();