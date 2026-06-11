"use strict";

/* ============================================================
 * Estado y persistencia
 * ============================================================ */
const state = {
  token: null,
  userId: null,
  email: null,
  rol: null,
  nombre: null,
  tiendaId: null,
};

function saveSession() {
  localStorage.setItem("ecommerce_session", JSON.stringify(state));
}
function loadSession() {
  try {
    const raw = localStorage.getItem("ecommerce_session");
    if (!raw) return false;
    Object.assign(state, JSON.parse(raw));
    return !!state.token;
  } catch {
    return false;
  }
}
function clearSession() {
  localStorage.removeItem("ecommerce_session");
  state.token = state.userId = state.email = state.rol = state.nombre = state.tiendaId = null;
}
const isAdmin = () => state.rol === "ADMIN";

/* ============================================================
 * Helpers
 * ============================================================ */
function $(sel, root = document) { return root.querySelector(sel); }
function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === "class") node.className = v;
    else if (k === "html") node.innerHTML = v;
    else if (k.startsWith("on") && typeof v === "function") node.addEventListener(k.slice(2), v);
    else if (v !== null && v !== undefined && v !== false) node.setAttribute(k, v);
  }
  for (const c of children.flat()) {
    if (c === null || c === undefined || c === false) continue;
    node.appendChild(typeof c === "string" ? document.createTextNode(c) : c);
  }
  return node;
}
function escapeHtml(s) {
  if (s === null || s === undefined) return "";
  return String(s).replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
function money(v) {
  if (v === null || v === undefined || v === "") return "—";
  return Number(v).toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function decodeJwt(token) {
  const part = token.split(".")[1];
  const base = part.replace(/-/g, "+").replace(/_/g, "/");
  const json = decodeURIComponent(
    atob(base).split("").map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2)).join("")
  );
  return JSON.parse(json);
}

let toastTimer;
function toast(message, type = "success") {
  const t = $("#toast");
  t.textContent = message;
  t.className = "toast " + type;
  t.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (t.hidden = true), 3500);
}

/* ============================================================
 * Cliente HTTP
 * ============================================================ */
async function api(method, path, body) {
  const headers = {};
  if (state.token) headers["Authorization"] = "Bearer " + state.token;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  const res = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401) {
    handleLogout(false);
    throw new Error("Sesión expirada. Inicia sesión nuevamente.");
  }
  if (res.status === 403) throw new Error("No tienes permisos para esta acción (requiere ADMIN).");
  if (res.status === 204) return null;

  let data = null;
  const text = await res.text();
  if (text) { try { data = JSON.parse(text); } catch { data = text; } }

  if (!res.ok) {
    const msg = (data && (data.message || data.error || data.mensaje)) ||
      (typeof data === "string" ? data : null) || `Error ${res.status}`;
    throw new Error(msg);
  }
  return data;
}

/* ============================================================
 * Autenticación
 * ============================================================ */
async function populateTiendas() {
  const select = $("#login-tienda");
  try {
    const tiendas = await api("GET", "/api/tiendas");
    select.innerHTML = "";
    (tiendas || []).forEach((t) =>
      select.appendChild(el("option", { value: t.id }, `${t.nombre} (${t.slug})`)));
    if (!tiendas || tiendas.length === 0)
      select.appendChild(el("option", { value: "" }, "No hay tiendas"));
  } catch (e) {
    select.innerHTML = "";
    select.appendChild(el("option", { value: "1" }, "Tienda 1 (no se pudo cargar la lista)"));
  }
}

async function handleLogin(ev) {
  ev.preventDefault();
  const btn = $("#login-btn");
  const errorEl = $("#login-error");
  errorEl.hidden = true;
  btn.disabled = true;
  btn.textContent = "Ingresando…";
  try {
    const tiendaId = Number($("#login-tienda").value);
    const email = $("#login-email").value.trim();
    const password = $("#login-password").value;

    const resp = await api("POST", "/api/auth", { email, password, tiendaId });
    state.token = resp.access_token;

    const payload = decodeJwt(state.token);
    state.userId = Number(payload.jti);
    state.email = payload.sub;
    state.tiendaId = tiendaId;

    // El rol no viene en el token: lo obtenemos del perfil del usuario.
    try {
      const me = await api("GET", "/api/usuarios/" + state.userId);
      state.rol = me.rol;
      state.nombre = me.nombre;
      if (me.tiendaId) state.tiendaId = me.tiendaId;
    } catch {
      state.rol = "CLIENTE";
      state.nombre = state.email;
    }

    saveSession();
    showApp();
  } catch (e) {
    errorEl.textContent = e.message;
    errorEl.hidden = false;
  } finally {
    btn.disabled = false;
    btn.textContent = "Ingresar";
  }
}

async function handleLogout(callApi = true) {
  if (callApi && state.token) {
    try { await api("POST", "/api/auth/logout"); } catch { /* ignore */ }
  }
  clearSession();
  showLogin();
}

/* ============================================================
 * Recursos CRUD (config-driven)
 * ============================================================ */
const ROLES = [
  { value: "ADMIN", label: "ADMIN" },
  { value: "CLIENTE", label: "CLIENTE" },
];
const TIPOS_MOV = [
  { value: "ENTRADA", label: "ENTRADA" },
  { value: "SALIDA", label: "SALIDA" },
];

// Loaders de opciones dinámicas para los <select> de los formularios.
async function loadCategorias() {
  const list = await api("GET", `/api/categorias/tienda/${state.tiendaId}`);
  return (list || []).map((c) => ({ value: c.id, label: c.nombre }));
}
async function loadUnidades() {
  const list = await api("GET", `/api/unidades-medida/tienda/${state.tiendaId}`);
  return (list || []).map((u) => ({ value: u.id, label: u.abreviatura ? `${u.nombre} (${u.abreviatura})` : u.nombre }));
}
async function loadProductos() {
  const list = await api("GET", `/api/productos/tienda/${state.tiendaId}`);
  return (list || []).map((p) => ({ value: p.id, label: p.nombre }));
}

const RESOURCES = {
  categorias: {
    label: "Categorías",
    icon: "🏷️",
    list: () => `/api/categorias/tienda/${state.tiendaId}`,
    create: () => `/api/categorias`,
    update: (id) => `/api/categorias/${id}`,
    remove: (id) => `/api/categorias/${id}`,
    columns: [
      { key: "id", label: "ID" },
      { key: "nombre", label: "Nombre" },
    ],
    fields: [
      { name: "nombre", label: "Nombre", type: "text", required: true },
    ],
    toBody: (f) => ({ tiendaId: state.tiendaId, nombre: f.nombre }),
  },

  "unidades-medida": {
    label: "Unidades de medida",
    icon: "📏",
    list: () => `/api/unidades-medida/tienda/${state.tiendaId}`,
    create: () => `/api/unidades-medida`,
    update: (id) => `/api/unidades-medida/${id}`,
    remove: (id) => `/api/unidades-medida/tienda/${state.tiendaId}/${id}`,
    columns: [
      { key: "id", label: "ID" },
      { key: "nombre", label: "Nombre" },
      { key: "abreviatura", label: "Abreviatura" },
    ],
    fields: [
      { name: "nombre", label: "Nombre", type: "text", required: true },
      { name: "abreviatura", label: "Abreviatura", type: "text" },
    ],
    toBody: (f) => ({ tiendaId: state.tiendaId, nombre: f.nombre, abreviatura: f.abreviatura || null }),
  },

  productos: {
    label: "Productos",
    icon: "📦",
    list: () => `/api/productos/tienda/${state.tiendaId}`,
    create: () => `/api/productos`,
    update: (id) => `/api/productos/${id}`,
    remove: (id) => `/api/productos/tienda/${state.tiendaId}/${id}`,
    columns: [
      { key: "id", label: "ID" },
      { key: "nombre", label: "Nombre" },
      { key: "categoriaNombre", label: "Categoría" },
      { key: "precio", label: "Precio", render: (v) => money(v) },
      { key: "stock", label: "Stock" },
      { key: "stockMinimo", label: "Mín." },
      { key: "unidadMedidaNombre", label: "Unidad" },
    ],
    fields: [
      { name: "nombre", label: "Nombre", type: "text", required: true },
      { name: "slugProducto", label: "Slug", type: "text", required: true },
      { name: "categoriaId", label: "Categoría", type: "select", optionsLoader: loadCategorias, allowEmpty: true },
      { name: "unidadMedidaId", label: "Unidad de medida", type: "select", optionsLoader: loadUnidades, allowEmpty: true },
      { name: "precio", label: "Precio de venta", type: "number", step: "0.01", required: true },
      { name: "precioCosto", label: "Precio de costo", type: "number", step: "0.01" },
      { name: "stock", label: "Stock", type: "number", required: true },
      { name: "stockMinimo", label: "Stock mínimo", type: "number" },
      { name: "imagenUrl", label: "URL de imagen", type: "text", full: true },
      { name: "descripcionLarga", label: "Descripción", type: "textarea", full: true },
    ],
    fillForm: (item) => ({
      nombre: item.nombre,
      slugProducto: item.slugProducto,
      categoriaId: item.categoriaId,
      unidadMedidaId: item.unidadMedidaId,
      precio: item.precio,
      precioCosto: item.precioCosto,
      stock: item.stock,
      stockMinimo: item.stockMinimo,
      imagenUrl: item.imagenUrl,
      descripcionLarga: item.descripcionLarga,
    }),
    toBody: (f) => ({
      tiendaId: state.tiendaId,
      nombre: f.nombre,
      slugProducto: f.slugProducto,
      categoriaId: f.categoriaId ? Number(f.categoriaId) : null,
      unidadMedidaId: f.unidadMedidaId ? Number(f.unidadMedidaId) : null,
      precio: Number(f.precio),
      precioCosto: f.precioCosto ? Number(f.precioCosto) : null,
      stock: Number(f.stock),
      stockMinimo: f.stockMinimo ? Number(f.stockMinimo) : null,
      imagenUrl: f.imagenUrl || null,
      descripcionLarga: f.descripcionLarga || null,
    }),
  },

  usuarios: {
    label: "Usuarios",
    icon: "👤",
    adminOnly: true,
    list: () => `/api/usuarios/tienda/${state.tiendaId}`,
    create: () => `/api/usuarios`,
    update: (id) => `/api/usuarios/${id}`,
    remove: (id) => `/api/usuarios/${id}`,
    columns: [
      { key: "id", label: "ID" },
      { key: "nombre", label: "Nombre" },
      { key: "email", label: "Email" },
      { key: "rol", label: "Rol", render: (v) => badge(v, v === "ADMIN" ? "admin" : "cliente") },
      { key: "numeroWhatsapp", label: "WhatsApp" },
      { key: "visibleCatalogo", label: "Catálogo", render: (v) => v ? badge("Sí", "on") : badge("No", "off") },
      { key: "estado", label: "Estado", render: (v) => v ? badge("Activo", "on") : badge("Inactivo", "off") },
    ],
    fields: [
      { name: "nombre", label: "Nombre", type: "text", required: true },
      { name: "email", label: "Email", type: "email", required: true, lockOnEdit: true },
      { name: "password", label: "Contraseña", type: "password", requiredOnCreate: true, hint: "En edición, déjala vacía para no cambiarla." },
      { name: "rol", label: "Rol", type: "select", options: ROLES, required: true, lockOnEdit: true },
      { name: "numeroWhatsapp", label: "WhatsApp", type: "text" },
      { name: "visibleCatalogo", label: "Visible en catálogo público", type: "checkbox", full: true },
    ],
    fillForm: (item) => ({
      nombre: item.nombre,
      email: item.email,
      password: "",
      rol: item.rol,
      numeroWhatsapp: item.numeroWhatsapp,
      visibleCatalogo: item.visibleCatalogo,
    }),
    toBody: (f) => ({
      tiendaId: state.tiendaId,
      nombre: f.nombre,
      email: f.email,
      password: f.password,
      rol: f.rol,
      numeroWhatsapp: f.numeroWhatsapp || null,
      visibleCatalogo: !!f.visibleCatalogo,
    }),
  },

  inventario: {
    label: "Inventario",
    icon: "🔄",
    adminOnly: true,
    canEdit: false,
    canDelete: false,
    createLabel: "Registrar movimiento",
    list: () => `/api/inventario/tienda/${state.tiendaId}`,
    create: () => `/api/inventario`,
    columns: [
      { key: "id", label: "ID" },
      { key: "fecha", label: "Fecha", render: (v) => v ? new Date(v).toLocaleString("es-BO") : "—" },
      { key: "productoNombre", label: "Producto" },
      { key: "tipo", label: "Tipo", render: (v) => badge(v, v === "ENTRADA" ? "on" : "off") },
      { key: "cantidad", label: "Cantidad" },
      { key: "referencia", label: "Referencia" },
    ],
    fields: [
      { name: "productoId", label: "Producto", type: "select", optionsLoader: loadProductos, required: true },
      { name: "tipo", label: "Tipo", type: "select", options: TIPOS_MOV, required: true },
      { name: "cantidad", label: "Cantidad", type: "number", required: true },
      { name: "referencia", label: "Referencia", type: "text", full: true },
    ],
    toBody: (f) => ({
      tiendaId: state.tiendaId,
      productoId: Number(f.productoId),
      usuarioId: state.userId,
      tipo: f.tipo,
      cantidad: Number(f.cantidad),
      referencia: f.referencia || null,
    }),
  },
};

function badge(text, kind) {
  const span = el("span", { class: "badge badge-" + kind }, String(text));
  return span;
}

/* ============================================================
 * Navegación
 * ============================================================ */
const NAV = [
  { route: "dashboard", label: "Dashboard", icon: "📊", adminOnly: true },
  { route: "productos", label: "Productos", icon: "📦" },
  { route: "categorias", label: "Categorías", icon: "🏷️" },
  { route: "unidades-medida", label: "Unidades", icon: "📏" },
  { route: "inventario", label: "Inventario", icon: "🔄", adminOnly: true },
  { route: "usuarios", label: "Usuarios", icon: "👤", adminOnly: true },
];

function renderNav() {
  const nav = $("#nav");
  nav.innerHTML = "";
  const current = location.hash.slice(2) || defaultRoute();
  NAV.filter((n) => !n.adminOnly || isAdmin()).forEach((n) => {
    const link = el("a", {
      class: "nav-link" + (n.route === current ? " active" : ""),
      href: "#/" + n.route,
    }, `${n.icon}  ${n.label}`);
    nav.appendChild(link);
  });
}

function defaultRoute() {
  return isAdmin() ? "dashboard" : "productos";
}

/* ============================================================
 * Router
 * ============================================================ */
async function router() {
  if (!state.token) return;
  let route = location.hash.slice(2) || defaultRoute();

  const navItem = NAV.find((n) => n.route === route);
  if (navItem && navItem.adminOnly && !isAdmin()) route = defaultRoute();

  renderNav();
  $("#sidebar").classList.remove("open");

  if (route === "dashboard") return renderDashboard();
  if (RESOURCES[route]) return renderResource(route);
  // Ruta desconocida → default
  location.hash = "#/" + defaultRoute();
}

/* ============================================================
 * Vista: Dashboard
 * ============================================================ */
async function renderDashboard() {
  const content = $("#content");
  content.innerHTML = "";
  content.appendChild(el("div", { class: "view-header" },
    el("div", {}, el("h1", {}, "Dashboard"),
      el("p", {}, "Métricas rápidas de la tienda"))));
  const loading = el("div", { class: "loading" }, "Cargando métricas…");
  content.appendChild(loading);

  try {
    const d = await api("GET", `/api/dashboard/tienda/${state.tiendaId}`);
    loading.remove();

    const grid = el("div", { class: "cards-grid" },
      metric("Productos", d.totalProductos),
      metric("Movimientos", d.totalMovimientos),
      metric("Agotados", d.productosAgotados, d.productosAgotados > 0 ? "danger" : ""),
      metric("Stock bajo", d.productosStockBajo, d.productosStockBajo > 0 ? "warn" : ""),
      metric("Ventas hoy", d.ventasHoy),
      metric("Ingresos hoy", money(d.ingresosHoy)));
    content.appendChild(grid);

    // Alertas de stock
    content.appendChild(el("h2", { class: "section-title" }, "Alertas de stock"));
    content.appendChild(simpleTable(
      ["Producto", "Stock", "Mínimo"],
      (d.alertasStock || []).map((p) => [p.nombre, p.stock, p.stockMinimo]),
      "Sin alertas de stock 🎉"));

    // Últimas ventas
    content.appendChild(el("h2", { class: "section-title" }, "Últimas ventas"));
    content.appendChild(simpleTable(
      ["Código", "Total", "Estado"],
      (d.ultimasVentas || []).map((v) => [v.codigoSeguimiento, money(v.total), v.estadoPedido]),
      "Aún no hay ventas completadas."));
  } catch (e) {
    loading.remove();
    content.appendChild(el("div", { class: "card empty" }, e.message));
  }
}

function metric(label, value, kind = "") {
  return el("div", { class: "metric " + kind },
    el("div", { class: "label" }, label),
    el("div", { class: "value" }, String(value ?? "—")));
}

function simpleTable(headers, rows, emptyMsg) {
  const card = el("div", { class: "card" });
  if (!rows || rows.length === 0) {
    card.appendChild(el("div", { class: "empty" }, emptyMsg));
    return card;
  }
  const thead = el("tr", {}, headers.map((h) => el("th", {}, h)));
  const tbody = rows.map((r) => el("tr", {}, r.map((c) => {
    const td = el("td", {});
    if (c instanceof Node) td.appendChild(c); else td.textContent = c ?? "—";
    return td;
  })));
  card.appendChild(el("div", { class: "table-wrap" },
    el("table", {}, el("thead", {}, thead), el("tbody", {}, tbody))));
  return card;
}

/* ============================================================
 * Vista genérica: Recurso CRUD
 * ============================================================ */
async function renderResource(key) {
  const cfg = RESOURCES[key];
  const content = $("#content");
  content.innerHTML = "";

  const canMutate = isAdmin();
  const canCreate = canMutate && cfg.create;

  const header = el("div", { class: "view-header" },
    el("div", {}, el("h1", {}, cfg.label)));
  if (canCreate) {
    header.appendChild(el("button", {
      class: "btn btn-primary",
      onclick: () => openForm(key, null),
    }, "+ " + (cfg.createLabel || "Nuevo")));
  }
  content.appendChild(header);

  const loading = el("div", { class: "loading" }, "Cargando…");
  content.appendChild(loading);

  try {
    const items = await api("GET", cfg.list());
    loading.remove();
    content.appendChild(buildTable(key, cfg, items || [], canMutate));
  } catch (e) {
    loading.remove();
    content.appendChild(el("div", { class: "card empty" }, e.message));
  }
}

function buildTable(key, cfg, items, canMutate) {
  const card = el("div", { class: "card" });
  if (items.length === 0) {
    card.appendChild(el("div", { class: "empty" }, "No hay registros."));
    return card;
  }
  const showEdit = canMutate && cfg.canEdit !== false && cfg.update;
  const showDelete = canMutate && cfg.canDelete !== false && cfg.remove;

  const headRow = el("tr", {}, [
    ...cfg.columns.map((c) => el("th", {}, c.label)),
    (showEdit || showDelete) ? el("th", { class: "actions" }, "Acciones") : null,
  ]);

  const body = items.map((item) => {
    const cells = cfg.columns.map((col) => {
      const td = el("td", {});
      const raw = item[col.key];
      const rendered = col.render ? col.render(raw, item) : raw;
      if (rendered instanceof Node) td.appendChild(rendered);
      else td.textContent = (rendered === null || rendered === undefined || rendered === "") ? "—" : rendered;
      return td;
    });
    if (showEdit || showDelete) {
      const actions = el("td", { class: "actions" });
      if (showEdit) actions.appendChild(el("button", {
        class: "btn btn-ghost btn-sm",
        onclick: () => openForm(key, item),
      }, "Editar"));
      if (showDelete) actions.appendChild(el("button", {
        class: "btn btn-danger btn-sm",
        onclick: () => confirmDelete(key, item),
      }, "Eliminar"));
      cells.push(actions);
    }
    return el("tr", {}, cells);
  });

  card.appendChild(el("div", { class: "table-wrap" },
    el("table", {}, el("thead", {}, headRow), el("tbody", {}, body))));
  return card;
}

async function confirmDelete(key, item) {
  const cfg = RESOURCES[key];
  const labelField = cfg.columns.find((c) => c.key === "nombre");
  const name = labelField ? item.nombre : "#" + item.id;
  if (!confirm(`¿Eliminar "${name}"? Esta acción no se puede deshacer.`)) return;
  try {
    await api("DELETE", cfg.remove(item.id));
    toast("Eliminado correctamente");
    renderResource(key);
  } catch (e) {
    toast(e.message, "error");
  }
}

/* ============================================================
 * Modal de formulario (crear / editar)
 * ============================================================ */
let modalSubmit = null;

async function openForm(key, item) {
  const cfg = RESOURCES[key];
  const isEdit = !!item;
  const root = $("#modal-root");
  const form = $("#modal-form");
  $("#modal-title").textContent = (isEdit ? "Editar " : "Nuevo ") + cfg.label.toLowerCase().replace(/s$/, "");
  form.innerHTML = "";

  const values = isEdit ? (cfg.fillForm ? cfg.fillForm(item) : { ...item }) : {};

  // Cargar opciones dinámicas de los selects en paralelo.
  await Promise.all(cfg.fields
    .filter((f) => f.optionsLoader)
    .map(async (f) => { f._options = await f.optionsLoader().catch(() => []); }));

  const grid = el("div", { class: "form-grid" });
  cfg.fields.forEach((f) => grid.appendChild(buildField(f, values[f.name], isEdit)));
  form.appendChild(grid);

  modalSubmit = async (ev) => {
    ev.preventDefault();
    const saveBtn = $("#modal-save");
    saveBtn.disabled = true;
    saveBtn.textContent = "Guardando…";
    try {
      const formData = collectForm(cfg, form);
      const body = cfg.toBody(formData);
      // En edición de usuario, no enviar password vacía.
      if (isEdit && key === "usuarios" && !formData.password) delete body.password;

      if (isEdit) await api("PUT", cfg.update(item.id), body);
      else await api("POST", cfg.create(), body);

      toast(isEdit ? "Cambios guardados" : "Creado correctamente");
      closeModal();
      renderResource(key);
    } catch (e) {
      toast(e.message, "error");
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = "Guardar";
    }
  };
  form.addEventListener("submit", modalSubmit);
  root.hidden = false;
  const first = form.querySelector("input, select, textarea");
  if (first) first.focus();
}

function buildField(f, value, isEdit) {
  const wrap = el("div", { class: "field" + (f.type === "checkbox" ? " field-check" : "") + (f.full ? " full" : "") });
  const id = "f_" + f.name;
  const locked = isEdit && f.lockOnEdit;

  if (f.type === "checkbox") {
    const input = el("input", { type: "checkbox", id, "data-name": f.name });
    if (value) input.checked = true;
    wrap.appendChild(input);
    wrap.appendChild(el("label", { for: id }, f.label));
    return wrap;
  }

  wrap.appendChild(el("label", { for: id }, f.label + (f.required || f.requiredOnCreate && !isEdit ? " *" : "")));

  let input;
  if (f.type === "select") {
    input = el("select", { id, "data-name": f.name });
    if (f.allowEmpty) input.appendChild(el("option", { value: "" }, "— Ninguno —"));
    const opts = f.options || f._options || [];
    opts.forEach((o) => input.appendChild(el("option", { value: o.value }, o.label)));
    input.value = value !== null && value !== undefined ? String(value) : "";
  } else if (f.type === "textarea") {
    input = el("textarea", { id, "data-name": f.name });
    input.value = value ?? "";
  } else {
    input = el("input", { id, type: f.type || "text", "data-name": f.name });
    if (f.step) input.setAttribute("step", f.step);
    input.value = value ?? "";
  }
  if ((f.required || (f.requiredOnCreate && !isEdit)) && !locked) input.required = true;
  if (locked) input.setAttribute("disabled", "disabled");
  wrap.appendChild(input);
  if (f.hint) wrap.appendChild(el("small", { style: "color:var(--text-soft);margin-top:.25rem;font-size:.75rem" }, f.hint));
  return wrap;
}

function collectForm(cfg, form) {
  const data = {};
  form.querySelectorAll("[data-name]").forEach((node) => {
    const name = node.getAttribute("data-name");
    if (node.type === "checkbox") data[name] = node.checked;
    else data[name] = node.value;
  });
  // Los campos bloqueados (disabled) igual deben enviarse: ya están en data si tienen data-name.
  return data;
}

function closeModal() {
  const root = $("#modal-root");
  root.hidden = true;
  const form = $("#modal-form");
  if (modalSubmit) form.removeEventListener("submit", modalSubmit);
  modalSubmit = null;
  form.innerHTML = "";
}

/* ============================================================
 * Mostrar login / app
 * ============================================================ */
function showLogin() {
  $("#app-view").hidden = true;
  $("#login-view").hidden = false;
  $("#login-password").value = "";
  populateTiendas();
}

function showApp() {
  $("#login-view").hidden = true;
  $("#app-view").hidden = false;
  $("#user-info").textContent = `${state.nombre || state.email} · ${state.rol}`;
  if (!location.hash) location.hash = "#/" + defaultRoute();
  else router();
  renderNav();
}

/* ============================================================
 * Init
 * ============================================================ */
function init() {
  $("#login-form").addEventListener("submit", handleLogin);
  $("#logout-btn").addEventListener("click", () => handleLogout(true));
  $("#menu-toggle").addEventListener("click", () => $("#sidebar").classList.toggle("open"));
  window.addEventListener("hashchange", router);

  // Cerrar modal
  $("#modal-root").addEventListener("click", (e) => {
    if (e.target.hasAttribute("data-close")) closeModal();
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && !$("#modal-root").hidden) closeModal();
  });

  if (loadSession()) showApp();
  else showLogin();
}

document.addEventListener("DOMContentLoaded", init);
