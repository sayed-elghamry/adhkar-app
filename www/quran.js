// المصحف الشريف: نص مجمع الملك فهد (رواية حفص، ترقيم صفحات مصحف المدينة ٦٠٤ صفحة)
// يُحمَّل عند فتح تبويب المصحف فقط.
import { Mushaf } from './quran/quran-text.js';

const AR = (n) => String(n).replace(/\d/g, (d) => '٠١٢٣٤٥٦٧٨٩'[d]);
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const BASMALAH = 'بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ';

let M = null;
let loading = null;
function mushaf() {
  if (M) return Promise.resolve(M);
  if (!loading) {
    loading = Mushaf.hafs().then((m) => {
      M = m;
      const st = document.createElement('style');
      st.textContent = m.fontFace(m.font.url).replace('}', '; font-display: block; }');
      document.head.appendChild(st);
      return m;
    });
  }
  return loading;
}

const CSS = `
.q-top{position:sticky;top:0;z-index:3;background:var(--bg);padding:6px 0}
.q-bar{display:flex;align-items:center;justify-content:space-between;gap:8px}
.q-bar .ttl{font:700 15px 'Tajawal';text-align:center;flex:1;line-height:1.3}
.q-bar .ttl small{display:block;color:var(--muted);font-weight:400;font-size:12px}
.q-btn{border:1px solid var(--line);background:var(--card);color:var(--ink);border-radius:12px;padding:8px 12px;font:500 14px 'Tajawal';cursor:pointer;min-width:44px}
.q-btn.on{background:var(--gold);color:#fff;border-color:var(--gold)}
.q-page{background:#fffbef;color:#1b1b1b;border:1px solid #e6d9b5;border-radius:10px;padding:14px 10px 10px;margin:6px 0;box-shadow:0 2px 10px rgba(0,0,0,.06);touch-action:pan-y}
@media (prefers-color-scheme:dark){.q-page{background:#16130c;color:#efe7d2;border-color:#3a311d}}
.q-line{font-family:'KFGQPC HAFS Uthmanic Script',serif;font-size:var(--qfs,22px);line-height:1.95;text-align:justify;text-align-last:justify;white-space:nowrap;direction:rtl}
.q-line.c{text-align:center;text-align-last:center}
.q-sura{margin:8px 0 4px;border:2px solid var(--gold);border-radius:10px;padding:4px;text-align:center;font:700 17px 'Amiri',serif;color:var(--green);background:linear-gradient(90deg,transparent,rgba(184,137,43,.12),transparent)}
.q-bsm{font-family:'KFGQPC HAFS Uthmanic Script',serif;font-size:var(--qfs,22px);text-align:center;line-height:1.9}
.q-foot{display:flex;justify-content:space-between;color:var(--muted);font-size:12px;padding:6px 4px 0}
.q-list .it{display:flex;align-items:center;gap:12px;padding:11px 4px;border-bottom:1px solid var(--line);cursor:pointer}
.q-list .it:last-child{border:none}
.q-list .num{width:36px;height:36px;border-radius:50%;border:1.5px solid var(--gold);display:flex;align-items:center;justify-content:center;font:700 14px 'Tajawal';color:var(--green);flex:0 0 auto}
.q-list .nm{flex:1;font:700 18px 'Amiri',serif}
.q-list .nm small{display:block;font:400 12px 'Tajawal';color:var(--muted)}
.q-list .pg{color:var(--muted);font-size:13px}
.q-search{display:flex;gap:8px}
.q-search input{flex:1;border:1px solid var(--line);background:var(--card);color:var(--ink);border-radius:12px;padding:10px 12px;font:500 15px 'Tajawal'}
.q-res .r{padding:10px 4px;border-bottom:1px solid var(--line);cursor:pointer}
.q-res .r .t{font-family:'KFGQPC HAFS Uthmanic Script',serif;font-size:20px;line-height:1.9}
.q-res .r .m{font-size:12px;color:var(--muted)}
.q-tabs{display:flex;gap:5px;margin:6px 0;flex-wrap:wrap}
.q-tabs button{flex:1;padding:8px 4px;font-size:13px}
.q-juz{display:grid;grid-template-columns:repeat(5,1fr);gap:8px}
.q-juz button{padding:10px 0}
.q-credit{font-size:11px;color:var(--muted);text-align:center;margin-top:10px;line-height:1.7}
`;
let cssDone = false;
function ensureCss() {
  if (cssDone) return;
  const st = document.createElement('style');
  st.textContent = CSS;
  document.head.appendChild(st);
  cssDone = true;
}

/* ---------------- الحالة ---------------- */
// ctx = { S, persist, view, render }  — من الصفحة الرئيسية
let ctx = null;
let mode = 'index';      // index | page
let idxTab = 'surah';    // surah | juz | search | marks
let lastQuery = '';
function Q() {
  ctx.S.quran = ctx.S.quran || { page: 1, marks: [], last: 0 };
  return ctx.S.quran;
}

function fitFont(el) {
  // قِس أطول سطر ثم صغّر الخط حتى يتسع كل سطر لعرض الصفحة
  const avail = el.clientWidth - 22;
  let fs = 30;
  el.style.setProperty('--qfs', fs + 'px');
  let widest = 1;
  el.querySelectorAll('.q-line > span').forEach((sp) => { widest = Math.max(widest, sp.getBoundingClientRect().width); });
  fs = Math.max(12, Math.min(34, fs * avail / widest * 0.985));
  el.style.setProperty('--qfs', fs.toFixed(2) + 'px');
}

/* ---------------- رسم صفحة ---------------- */
function pageHtml(m, n) {
  const page = m.page(n);
  const lines = page.lines;
  const surahStarts = new Map(m.surahs.map((s) => [s.start, s]));
  const center = n <= 2;
  let h = '';
  for (const ln of lines) {
    const s = surahStarts.get(ln.start);
    if (s) {
      h += '<div class="q-sura">سُورَةُ ' + esc(s.nameAr) + '</div>';
      if (s.number !== 1 && s.number !== 9) h += '<div class="q-bsm">' + BASMALAH + '</div>';
    }
    const txt = ln.render({ marks: true, ayahMarks: true });
    const short = ln.length < 4;
    h += '<div class="q-line' + (center || short ? ' c' : '') + '"><span>' + esc(txt) + '</span></div>';
  }
  return h;
}

function surahOfPage(m, n) {
  const p = m.page(n);
  const ss = p.surahs;
  return ss.map((s) => s.nameAr).join(' · ');
}

function renderPage(m) {
  const q = Q();
  const n = Math.max(1, Math.min(m.pageCount, q.page | 0 || 1));
  q.page = n; q.last = Date.now(); ctx.persist();
  const marked = q.marks.indexOf(n) >= 0;
  const juz = m.page(n).juz;
  ctx.view.innerHTML =
    '<div class="q-top"><div class="q-bar">' +
      '<button class="q-btn" id="qBack">☰</button>' +
      '<div class="ttl">' + esc(surahOfPage(m, n)) + '<small>الصفحة ' + AR(n) + ' · الجزء ' + AR(juz ? juz.number : '') + '</small></div>' +
      '<button class="q-btn' + (marked ? ' on' : '') + '" id="qMark" aria-label="علامة">🔖</button>' +
    '</div></div>' +
    '<div class="q-page" id="qPage">' + pageHtml(m, n) + '</div>' +
    '<div class="q-bar" style="margin-top:6px">' +
      '<button class="q-btn" id="qNext">→ التالية</button>' +
      '<button class="q-btn" id="qGo">انتقال</button>' +
      '<button class="q-btn" id="qPrev">السابقة ←</button>' +
    '</div>';
  const pg = document.getElementById('qPage');
  fitFont(pg);
  window.onresize = () => { const e = document.getElementById('qPage'); if (e) fitFont(e); };
  const go = (d) => { const t = n + d; if (t >= 1 && t <= m.pageCount) { q.page = t; renderPage(m); window.scrollTo(0, 0); } };
  document.getElementById('qNext').onclick = () => go(1);
  document.getElementById('qPrev').onclick = () => go(-1);
  document.getElementById('qBack').onclick = () => { mode = 'index'; render(); };
  document.getElementById('qMark').onclick = () => {
    const i = q.marks.indexOf(n);
    if (i >= 0) q.marks.splice(i, 1); else q.marks.unshift(n);
    ctx.persist(); renderPage(m);
  };
  document.getElementById('qGo').onclick = () => {
    const v = prompt('رقم الصفحة (١ – ٦٠٤)', String(n));
    if (!v) return;
    const t = parseInt(String(v).replace(/[٠-٩]/g, (d) => '٠١٢٣٤٥٦٧٨٩'.indexOf(d)), 10);
    if (t >= 1 && t <= m.pageCount) { q.page = t; renderPage(m); window.scrollTo(0, 0); }
  };
  // السحب: من اليسار إلى اليمين = الصفحة التالية (كما في المصحف الورقي)
  let x0 = null, y0 = null;
  pg.addEventListener('touchstart', (e) => { x0 = e.touches[0].clientX; y0 = e.touches[0].clientY; }, { passive: true });
  pg.addEventListener('touchend', (e) => {
    if (x0 === null) return;
    const dx = e.changedTouches[0].clientX - x0, dy = e.changedTouches[0].clientY - y0;
    x0 = null;
    if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) go(dx > 0 ? 1 : -1);
  });
}

/* ---------------- الفهرس ---------------- */
function renderIndex(m) {
  const q = Q();
  let body = '';
  if (idxTab === 'surah') {
    body = '<div class="card q-list">' + m.surahs.map((s) =>
      '<div class="it" data-p="' + s.page.number + '"><span class="num">' + AR(s.number) + '</span>' +
      '<span class="nm">' + esc(s.nameAr) + '<small>' + (s.revelation === 'makki' ? 'مكية' : 'مدنية') + ' · ' + AR(s.ayahCount) + ' آية</small></span>' +
      '<span class="pg">ص ' + AR(s.page.number) + '</span></div>').join('') + '</div>';
  } else if (idxTab === 'juz') {
    let b = '';
    for (let j = 1; j <= m.juzCount; j++) b += '<button class="q-btn" data-p="' + m.juz(j).page.number + '">' + AR(j) + '</button>';
    body = '<div class="card"><div class="sub" style="margin-bottom:8px">اختر الجزء</div><div class="q-juz">' + b + '</div></div>';
  } else if (idxTab === 'sham') {
    const sh = SH();
    body = '<div class="card"><b>مصحف الشمرلي (صور الصفحات)</b>' +
      '<div class="sub" style="line-height:1.9;margin:6px 0">تُعرض الصفحات من نسخة على موقع archive.org، فتحتاج اتصالًا بالإنترنت عند فتح كل صفحة أول مرة، ثم تُحفظ مؤقتًا في الهاتف. للاستخدام الشخصي.</div>' +
      '<button class="pri" id="shOpen">' + (sh.leaf ? 'متابعة · ص ' + AR(sh.leaf - sh.off) : 'فتح المصحف') + '</button></div>' +
      (sh.marks.length ? '<div class="card q-list">' + sh.marks.map((l) => '<div class="it" data-l="' + l + '"><span class="num">🔖</span><span class="nm">صفحة ' + AR(l - sh.off) + '</span></div>').join('') + '</div>' : '');
  } else if (idxTab === 'marks') {
    body = '<div class="card q-list">' + (q.marks.length ? q.marks.map((p) =>
      '<div class="it" data-p="' + p + '"><span class="num">🔖</span><span class="nm">' + esc(surahOfPage(m, p)) + '<small>الجزء ' + AR(m.page(p).juz.number) + '</small></span><span class="pg">ص ' + AR(p) + '</span></div>').join('')
      : '<div class="sub">لا توجد علامات بعد. اضغط 🔖 أثناء القراءة لحفظ الصفحة.</div>') + '</div>';
  } else {
    body = '<div class="card"><div class="q-search"><input id="qQ" placeholder="ابحث بكلمات من الآية…" value="' + esc(lastQuery) + '"><button class="q-btn" id="qS">بحث</button></div><div class="q-res" id="qRes"></div></div>';
  }
  ctx.view.innerHTML =
    '<div class="card"><div class="row"><b style="font-size:18px">المصحف الشريف</b>' +
      '<button class="pri" id="qCont">متابعة القراءة · ص ' + AR(q.page || 1) + '</button></div>' +
    '<div class="q-tabs">' +
      [['surah', 'السور'], ['juz', 'الأجزاء'], ['search', 'بحث'], ['marks', 'العلامات'], ['sham', 'الشمرلي']].map(([k, t]) =>
        '<button class="q-btn' + (idxTab === k ? ' on' : '') + '" data-t="' + k + '">' + t + '</button>').join('') +
    '</div></div>' + body +
    '<div class="q-credit">نص المصحف برواية حفص عن عاصم من إصدار مجمع الملك فهد لطباعة المصحف الشريف (الخط العثماني، ترقيم مصحف المدينة)، عبر مكتبة quran-text (CC BY 4.0).</div>';
  ctx.view.querySelectorAll('.q-tabs button').forEach((b) => b.onclick = () => { idxTab = b.dataset.t; render(); });
  ctx.view.querySelectorAll('[data-p]').forEach((el) => el.onclick = () => { Q().page = +el.dataset.p; mode = 'page'; render(); window.scrollTo(0, 0); });
  if (idxTab === 'sham') {
    document.getElementById('shOpen').onclick = () => { mode = 'sham'; render(); window.scrollTo(0, 0); };
    ctx.view.querySelectorAll('[data-l]').forEach((el) => el.onclick = () => { SH().leaf = +el.dataset.l; mode = 'sham'; render(); });
  }
  document.getElementById('qCont').onclick = () => { mode = 'page'; render(); window.scrollTo(0, 0); };
  if (idxTab === 'search') {
    const inp = document.getElementById('qQ');
    const run = () => {
      lastQuery = inp.value.trim();
      const out = document.getElementById('qRes');
      if (lastQuery.length < 2) { out.innerHTML = ''; return; }
      const hits = m.search(lastQuery).slice(0, 60);
      out.innerHTML = hits.length ? '<div class="sub" style="margin:8px 0">' + AR(hits.length) + (hits.length === 60 ? '+' : '') + ' نتيجة</div>' + hits.map((sp) => {
        const a = m.ayahAt(sp.start);
        const s = m.surahAt(sp.start);
        const p = m.pageAt(sp.start).number;
        const txt = a ? a.render({ marks: true }) : sp.text;
        return '<div class="r" data-p="' + p + '"><div class="t">' + esc(txt.length > 220 ? txt.slice(0, 220) + '…' : txt) + '</div><div class="m">' + esc(s.nameAr) + (a ? ' · الآية ' + AR(a.number) : '') + ' · ص ' + AR(p) + '</div></div>';
      }).join('') : '<div class="sub" style="margin-top:8px">لا نتائج</div>';
      out.querySelectorAll('[data-p]').forEach((el) => el.onclick = () => { Q().page = +el.dataset.p; mode = 'page'; render(); window.scrollTo(0, 0); });
    };
    document.getElementById('qS').onclick = run;
    inp.addEventListener('keydown', (e) => { if (e.key === 'Enter') run(); });
    if (lastQuery) run();
  }
}

/* ---------------- مصحف الشمرلي (صور من archive.org) ---------------- */
const SH_ID = '20191116_20191116_1655';
const SH_LEAVES = 522;
function SH() {
  const q = Q();
  q.sham = q.sham || { leaf: 0, off: 5, marks: [] };   // off: رقم الورقة قبل الصفحة ١ (قابل للضبط)
  return q.sham;
}
function shUrl(leaf, w) { return 'https://archive.org/download/' + SH_ID + '/page/n' + leaf + (w ? '_w' + w : '') + '.jpg'; }
function renderSham() {
  const sh = SH();
  if (!sh.leaf) sh.leaf = sh.off + 1;
  const leaf = Math.max(0, Math.min(SH_LEAVES - 1, sh.leaf));
  sh.leaf = leaf; ctx.persist();
  const pageNo = leaf - sh.off;
  const marked = sh.marks.indexOf(leaf) >= 0;
  const w = Math.min(1600, Math.round(window.innerWidth * (window.devicePixelRatio || 2)));
  ctx.view.innerHTML =
    '<div class="q-top"><div class="q-bar">' +
      '<button class="q-btn" id="qBack">☰</button>' +
      '<div class="ttl">مصحف الشمرلي<small>الصفحة ' + AR(pageNo) + '</small></div>' +
      '<button class="q-btn' + (marked ? ' on' : '') + '" id="qMark">🔖</button>' +
    '</div></div>' +
    '<div class="q-page" id="qPage" style="position:relative;padding:4px;min-height:60vh;display:flex;align-items:center;justify-content:center">' +
      '<img id="shImg" alt="" style="width:100%;height:auto;display:block;border-radius:6px" src="' + shUrl(leaf, w) + '">' +
      '<div id="shMsg" class="sub" style="position:absolute">جارٍ تحميل الصفحة…</div>' +
    '</div>' +
    '<div class="q-bar" style="margin-top:6px">' +
      '<button class="q-btn" id="qNext">→ التالية</button>' +
      '<button class="q-btn" id="qGo">انتقال</button>' +
      '<button class="q-btn" id="shFix">ضبط الترقيم</button>' +
      '<button class="q-btn" id="qPrev">السابقة ←</button>' +
    '</div>';
  const img = document.getElementById('shImg'), msg = document.getElementById('shMsg');
  img.onload = () => { msg.remove(); };
  img.onerror = () => {
    if (!img.dataset.retry) { img.dataset.retry = '1'; img.src = shUrl(leaf, 0); return; }
    msg.innerHTML = 'تعذّر تحميل الصفحة. تأكد من الاتصال بالإنترنت.<br><button class="q-btn" id="shRetry" style="margin-top:8px">إعادة المحاولة</button> <button class="q-btn" id="shWeb" style="margin-top:8px">فتح في المتصفح</button>';
    document.getElementById('shRetry').onclick = () => renderSham();
    document.getElementById('shWeb').onclick = () => {
      const u = 'https://archive.org/details/' + SH_ID + '/page/n' + leaf + '/mode/1up';
      const R = window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Reminder;
      if (R && R.openUrl) R.openUrl({ url: u }); else window.open(u, '_blank');
    };
  };
  // تحميل مسبق للصفحتين التاليتين
  [1, 2].forEach((d) => { if (leaf + d < SH_LEAVES) { const p = new Image(); p.src = shUrl(leaf + d, w); } });
  const go = (d) => { const t = leaf + d; if (t >= 0 && t < SH_LEAVES) { sh.leaf = t; renderSham(); window.scrollTo(0, 0); } };
  document.getElementById('qNext').onclick = () => go(1);
  document.getElementById('qPrev').onclick = () => go(-1);
  document.getElementById('qBack').onclick = () => { mode = 'index'; idxTab = 'sham'; render(); };
  document.getElementById('qMark').onclick = () => {
    const i = sh.marks.indexOf(leaf);
    if (i >= 0) sh.marks.splice(i, 1); else sh.marks.unshift(leaf);
    ctx.persist(); renderSham();
  };
  const num = (v) => parseInt(String(v).replace(/[٠-٩]/g, (d) => '٠١٢٣٤٥٦٧٨٩'.indexOf(d)), 10);
  document.getElementById('qGo').onclick = () => {
    const t = num(prompt('رقم الصفحة', String(pageNo)) || '');
    if (t >= 1 - sh.off && t + sh.off < SH_LEAVES) { sh.leaf = t + sh.off; renderSham(); window.scrollTo(0, 0); }
  };
  document.getElementById('shFix').onclick = () => {
    const t = num(prompt('ما رقم هذه الصفحة المطبوع في المصحف؟', String(pageNo)) || '');
    if (t >= 0 && t < SH_LEAVES) { sh.off = leaf - t; ctx.persist(); renderSham(); }
  };
  const pg = document.getElementById('qPage');
  let x0 = null, y0 = null;
  pg.addEventListener('touchstart', (e) => { x0 = e.touches[0].clientX; y0 = e.touches[0].clientY; }, { passive: true });
  pg.addEventListener('touchend', (e) => {
    if (x0 === null) return;
    const dx = e.changedTouches[0].clientX - x0, dy = e.changedTouches[0].clientY - y0;
    x0 = null;
    if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) go(dx > 0 ? 1 : -1);
  });
}

function render() {
  if (!M) return;
  if (mode === 'page') renderPage(M);
  else if (mode === 'sham') renderSham();
  else renderIndex(M);
}

export function show(c) {
  ctx = c;
  ensureCss();
  if (!M) ctx.view.innerHTML = '<div class="card sub" style="text-align:center;padding:30px">جارٍ تحميل المصحف…</div>';
  return mushaf().then(() => {
    if (document.fonts && document.fonts.load) return document.fonts.load('22px "KFGQPC HAFS Uthmanic Script"').catch(() => {});
  }).then(render).catch((e) => {
    ctx.view.innerHTML = '<div class="card sub">تعذّر تحميل المصحف: ' + esc(e && e.message || e) + '</div>';
  });
}
