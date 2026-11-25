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
.sh-head{display:flex;justify-content:space-between;align-items:center;padding:calc(env(safe-area-inset-top) + 8px) 16px 8px;font:500 16px 'Tajawal';color:#fff;background:linear-gradient(#1d3a2c,#3d5a48)}
.sh-head span{flex:1;text-align:center}.sh-head span:first-child{text-align:right}.sh-head span:last-child{text-align:left}
.sh-ctl{position:fixed;inset:auto 0 0 0;z-index:6;background:var(--card);border-top:1px solid var(--line);padding:10px 12px calc(env(safe-area-inset-bottom) + 10px);box-shadow:0 -4px 16px rgba(0,0,0,.15);transform:translateY(110%);transition:transform .2s}
.sh-ctl.on{transform:none}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]) #shImg.qa{filter:invert(.9) hue-rotate(180deg)}}
:root[data-theme=dark] #shImg.qa{filter:invert(.9) hue-rotate(180deg)}
body.reading{padding:0;background:var(--shbg,var(--bg))}
body.reading .wrap{max-width:none}
body.reading header,body.reading nav{display:none}
.q-top{position:sticky;top:0;z-index:3;background:var(--bg);padding:6px 0}
.q-bar{display:flex;align-items:center;justify-content:space-between;gap:8px}
.q-bar .ttl{font:700 15px 'Tajawal';text-align:center;flex:1;line-height:1.3}
.q-bar .ttl small{display:block;color:var(--muted);font-weight:400;font-size:12px}
.q-btn{border:1px solid var(--line);background:var(--card);color:var(--ink);border-radius:12px;padding:8px 12px;font:500 14px 'Tajawal';cursor:pointer;min-width:44px}
.q-btn.on{background:var(--gold);color:#fff;border-color:var(--gold)}
.q-page{background:#fffbef;color:#1b1b1b;border:1px solid #e6d9b5;border-radius:10px;padding:14px 10px 10px;margin:6px 0;box-shadow:0 2px 10px rgba(0,0,0,.06);touch-action:pan-y}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]) .q-page{background:#16130c;color:#efe7d2;border-color:#3a311d}}
:root[data-theme=dark] .q-page{background:#16130c;color:#efe7d2;border-color:#3a311d}
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
.sh-sheet{position:fixed;inset:0;z-index:30;background:rgba(0,0,0,.45);display:flex;align-items:flex-end}
.sh-sheet>div{background:var(--card);color:var(--ink);width:100%;max-height:85vh;overflow:auto;border-radius:18px 18px 0 0;padding:16px 14px calc(env(safe-area-inset-bottom) + 16px)}
.sh-sheet h3{margin:0 0 4px;font:700 17px 'Tajawal';text-align:center}
.bm-h{text-align:center;margin:4px 0 10px}.bm-h b{font:700 18px 'Tajawal';display:block}.bm-h small{color:var(--muted);font-size:12.5px;line-height:1.7;display:block}
.bm-sec{font:700 14px 'Tajawal';color:var(--gold);margin:4px 2px 8px}
.bm{display:flex;align-items:center;gap:10px;background:var(--card);border:1px solid var(--line);border-right:6px solid var(--c);border-radius:14px;padding:12px;margin-bottom:10px;cursor:pointer;-webkit-user-select:none;user-select:none;-webkit-touch-callout:none}
.bm .dot{width:14px;height:14px;border-radius:50%;background:var(--c);flex:0 0 auto}
.bm .bi{flex:1;min-width:0}.bm .bi b{display:block;font:700 16px 'Tajawal';color:var(--c)}
.bm .bi span{display:block;font-size:13px;margin-top:3px}.bm .bi small{display:block;color:var(--muted);font-size:12px;margin-top:2px}
.bm .go{background:var(--c);color:#fff;border:0;border-radius:10px;padding:8px 10px;font:600 13px 'Tajawal';white-space:nowrap}
.bm.empty .bi span{color:var(--muted)}
.th{border:1px solid var(--line);border-radius:14px;margin-bottom:10px;overflow:hidden;background:var(--card)}
.th.on{border:2px solid var(--gold)}
.th .pv{padding:16px 10px;text-align:center;font:400 24px 'Amiri',serif}
.th .ft{display:flex;align-items:center;gap:8px;padding:10px 12px}
.th .ft div{flex:1}.th .ft b{display:block;font:700 15px 'Tajawal'}.th .ft small{color:var(--muted);font-size:12px}
.th .ft button{border:0;border-radius:10px;padding:8px 12px;font:600 13px 'Tajawal';background:var(--green);color:#fff}
.th.on .ft button{background:transparent;color:var(--ok);font-weight:700}
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
// الورد اليومي والختمة: تُحسب الصفحة مقروءة إذا بقيت معروضة ٨ ثوانٍ
let readTimer = null;
function trackRead(key) {
  clearTimeout(readTimer);
  readTimer = setTimeout(() => {
    const S = ctx.S;
    const w = S.wird = S.wird || { target: 20, days: {}, kh: { pages: {}, n: 0 } };
    const d0 = new Date(), d = d0.getFullYear() + '-' + String(d0.getMonth() + 1).padStart(2, '0') + '-' + String(d0.getDate()).padStart(2, '0');
    w.days[d] = w.days[d] || [];
    if (w.days[d].indexOf(key) < 0) w.days[d].push(key);
    w.kh.pages[key] = 1;
    const ks = Object.keys(w.days).sort(); while (ks.length > 30) delete w.days[ks.shift()];
    ctx.persist();
  }, 8000);
}
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
  q.page = n; q.last = Date.now(); ctx.persist(); trackRead('m' + n);
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
function edSwitch(ed) {
  return '<div class="q-tabs" style="margin:0 0 4px">' +
    '<button class="q-btn' + (ed === 'madina' ? ' on' : '') + '" data-ed="madina">مصحف المدينة</button>' +
    '<button class="q-btn' + (ed === 'sham' ? ' on' : '') + '" data-ed="sham">مصحف الشمرلي</button></div>';
}
function bindEd() {
  ctx.view.querySelectorAll('[data-ed]').forEach((b) => b.onclick = () => { Q().ed = b.dataset.ed; idxTab = 'surah'; render(); });
}
function renderIndex(m) {
  const q = Q();
  if (q.ed === 'sham') { renderShamIndex(m); return; }
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
  } else if (idxTab === 'marks') {
    body = '<div class="card q-list">' + (q.marks.length ? q.marks.map((p) =>
      '<div class="it" data-p="' + p + '"><span class="num">🔖</span><span class="nm">' + esc(surahOfPage(m, p)) + '<small>الجزء ' + AR(m.page(p).juz.number) + '</small></span><span class="pg">ص ' + AR(p) + '</span></div>').join('')
      : '<div class="sub">لا توجد علامات بعد. اضغط 🔖 أثناء القراءة لحفظ الصفحة.</div>') + '</div>';
  } else {
    body = '<div class="card"><div class="q-search"><input id="qQ" placeholder="ابحث بكلمات من الآية…" value="' + esc(lastQuery) + '"><button class="q-btn" id="qS">بحث</button></div><div class="q-res" id="qRes"></div></div>';
  }
  ctx.view.innerHTML =
    '<div class="card">' + edSwitch('madina') + '<div class="row"><b style="font-size:18px">المصحف الشريف</b>' +
      '<button class="pri" id="qCont">متابعة القراءة · ص ' + AR(q.page || 1) + '</button></div>' +
    '<div class="q-tabs">' +
      [['surah', 'السور'], ['juz', 'الأجزاء'], ['search', 'بحث'], ['marks', 'العلامات']].map(([k, t]) =>
        '<button class="q-btn' + (idxTab === k ? ' on' : '') + '" data-t="' + k + '">' + t + '</button>').join('') +
    '</div></div>' + body +
    '<div class="q-credit">نص المصحف برواية حفص عن عاصم من إصدار مجمع الملك فهد لطباعة المصحف الشريف (الخط العثماني، ترقيم مصحف المدينة)، عبر مكتبة quran-text (CC BY 4.0).</div>';
  ctx.view.querySelectorAll('.q-tabs button').forEach((b) => b.onclick = () => { idxTab = b.dataset.t; render(); });
  ctx.view.querySelectorAll('[data-p]').forEach((el) => el.onclick = () => { Q().page = +el.dataset.p; mode = 'page'; render(); window.scrollTo(0, 0); });
  bindEd();
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
  // الترقيم: الصورة n1 = الفاتحة (ص ٢ المطبوعة)، فرقم الصفحة = رقم الصورة + ١
  q.sham = q.sham || { leaf: 1, off: -1, marks: [] };
  if (!q.sham.v2) { q.sham.off = -1; q.sham.v2 = 1; }
  return q.sham;
}
// بداية كل سورة وكل جزء في مصحف الشمرلي (٥٢١ صفحة)، من بيانات مشروع Quran for Android (GPL-3)
const SH_SURA = [1, 2, 41, 63, 86, 104, 122, 144, 152, 168, 180, 192, 204, 209, 215, 220, 232, 242, 252, 259, 267, 275, 283, 290, 299, 305, 314, 322, 331, 337, 343, 346, 349, 357, 363, 368, 373, 379, 384, 391, 399, 404, 410, 416, 418, 422, 426, 429, 433, 436, 438, 441, 443, 446, 448, 451, 454, 458, 461, 464, 467, 468, 470, 471, 473, 475, 477, 479, 481, 483, 485, 487, 489, 490, 492, 494, 496, 497, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 511, 512, 513, 513, 513, 514, 515, 515, 516, 516, 517, 517, 518, 518, 519, 519, 519, 520, 520, 520, 521, 521, 521];
const SH_JUZ = [1, 19, 35, 51, 67, 83, 98, 115, 131, 147, 163, 180, 198, 215, 232, 249, 267, 283, 301, 319, 335, 352, 369, 387, 404, 422, 439, 458, 477, 497];
// الصورة رقم n في archive.org = الصفحة n بهذا الترقيم، والرقم المطبوع أسفل الصفحة = n + ١
const shPrinted = (leaf) => leaf + 1;
function shSurahOf(leaf) { let i = 0; while (i + 1 < SH_SURA.length && SH_SURA[i + 1] <= leaf) i++; return i + 1; }
// بداية كل ربع حزب (٢٤٠ ربعًا) في طبعة الشمرلي، من مشروع quran_android (GPL-3)
const SH_Q = [1,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,38,40,43,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75,77,79,81,83,84,86,89,91,92,94,96,98,100,102,105,107,109,111,114,115,117,119,120,122,125,127,129,131,134,136,138,140,142,144,146,147,150,152,154,156,157,159,161,163,165,167,169,171,174,176,178,180,182,184,186,188,190,193,195,198,200,202,204,206,208,210,212,215,217,220,222,224,226,228,230,232,234,236,238,241,243,246,247,249,251,254,256,259,261,263,265,267,269,271,273,275,277,279,281,283,286,288,290,292,294,296,299,301,303,305,307,309,312,314,316,319,320,323,324,327,329,331,333,335,337,340,342,344,347,349,350,352,354,356,358,360,362,365,367,369,371,373,376,378,380,382,385,387,389,391,393,395,397,400,401,403,405,407,409,411,414,416,419,422,424,426,429,431,433,435,437,439,442,444,446,448,451,454,456,458,460,463,465,468,470,473,475,477,479,481,484,487,490,492,495,497,500,502,504,507,510,513,516];
function shHizbOf(leaf) { let i = 0; while (i + 1 < SH_Q.length && SH_Q[i + 1] <= leaf) i++; return Math.floor(i / 4) + 1; }
function shJuzOf(leaf) { let i = 0; while (i + 1 < SH_JUZ.length && SH_JUZ[i + 1] <= leaf) i++; return i + 1; }
// العلامات الخمس الملونة
const BM_DEF = [['الورد اليومي', '#d64545'], ['الحفظ والمراجعة', '#2e9b57'], ['قراءة التدبر', '#2f6fd6'], ['قيام الليل', '#c29b1f'], ['علامة مرجعية ٥', '#8a4fd0']];
function BM() {
  const sh = SH();
  if (!sh.bm) {
    const old = sh.marks || [];
    sh.bm = BM_DEF.map(([n], i) => ({ name: n, leaf: old[i] != null ? old[i] : null, at: old[i] != null ? Date.now() : 0 }));
  }
  return sh.bm;
}
const p2 = (n) => String(n).padStart(2, '0');
function bmDate(t) {
  const d = new Date(t); let h = d.getHours(); const pm = h >= 12; h = h % 12 || 12;
  return d.getFullYear() + '/' + p2(d.getMonth() + 1) + '/' + p2(d.getDate()) + ' - ' + p2(h) + ':' + p2(d.getMinutes()) + (pm ? ' م' : ' ص');
}
function bmCard(i, withGo) {
  const b = BM()[i], c = BM_DEF[i][1], has = b.leaf != null;
  const nm = has && M ? M.surahs[shSurahOf(b.leaf) - 1].nameAr : '';
  return '<div class="bm' + (has ? '' : ' empty') + '" data-bm="' + i + '" style="--c:' + c + '"><span class="dot"></span><div class="bi"><b>' + esc(b.name) + '</b>' +
    (has ? '<span>سورة ' + esc(nm) + ' • صفحة ' + AR(shPrinted(b.leaf)) + ' • الجزء ' + AR(shJuzOf(b.leaf)) + '</span><small>' + bmDate(b.at) + '</small>'
      : '<span>فارغة، احفظ فيها صفحة من زر 🔖 أثناء القراءة</span>') + '</div>' +
    (has && withGo ? '<button class="go" data-go="' + i + '">انتقال ص ' + AR(shPrinted(b.leaf)) + '</button>' : '') + '</div>';
}
function sheet(html) {
  const el = document.createElement('div'); el.className = 'sh-sheet';
  el.innerHTML = '<div>' + html + '</div>';
  el.onclick = (e) => { if (e.target === el) el.remove(); };
  document.body.appendChild(el);
  return el;
}
function bmEdit(i, after) {
  const b = BM()[i];
  const el = sheet('<h3 style="color:' + BM_DEF[i][1] + '">' + esc(b.name) + '</h3>' +
    '<div class="sub" style="margin:8px 0 4px">اسم العلامة</div><input id="bmName" value="' + esc(b.name) + '" style="width:100%;box-sizing:border-box;border:1px solid var(--line);background:var(--bg);color:var(--ink);border-radius:12px;padding:10px;font:500 15px Tajawal">' +
    '<div class="row" style="margin-top:12px;gap:8px;justify-content:flex-start;flex-wrap:wrap"><button class="pri" id="bmSave">حفظ الاسم</button>' +
    (b.leaf != null ? '<button class="ghost" id="bmClr">مسح العلامة</button>' : '') +
    '<button class="ghost" id="bmDef">الاسم الأصلي</button><button class="ghost" id="bmX">إلغاء</button></div>');
  const done = () => { ctx.persist(); el.remove(); after(); };
  el.querySelector('#bmSave').onclick = () => { const v = el.querySelector('#bmName').value.trim(); if (v) b.name = v.slice(0, 30); done(); };
  el.querySelector('#bmDef').onclick = () => { b.name = BM_DEF[i][0]; done(); };
  el.querySelector('#bmX').onclick = () => el.remove();
  const clr = el.querySelector('#bmClr'); if (clr) clr.onclick = () => { b.leaf = null; b.at = 0; done(); };
}
// ضغط مطوّل على العلامة = تسمية أو مسح
function bindBm(root, onTap, after) {
  root.querySelectorAll('.bm').forEach((el) => {
    const i = +el.dataset.bm; let t = null, long = false, sx = 0, sy = 0;
    const cancel = () => { clearTimeout(t); t = null; };
    el.addEventListener('touchstart', (e) => { long = false; sx = e.touches[0].clientX; sy = e.touches[0].clientY; cancel(); t = setTimeout(() => { long = true; t = null; bmEdit(i, after); }, 550); }, { passive: true });
    el.addEventListener('touchmove', (e) => { if (Math.abs(e.touches[0].clientX - sx) > 10 || Math.abs(e.touches[0].clientY - sy) > 10) cancel(); }, { passive: true });
    el.addEventListener('touchend', cancel);
    el.addEventListener('contextmenu', (e) => { e.preventDefault(); if (!long) { cancel(); long = true; bmEdit(i, after); } });
    el.onclick = (e) => { if (long) { long = false; return; } onTap(i, e); };
  });
}
// مظهر القراءة: فلاتر عرض فقط، صور المصحف نفسها لا تتغير أبدًا
const SH_TH = [
  ['orig', '📜 المظهر الأصلي (كريمي الشمرلي)', 'ألوان المصحف الأصلية', '#fffbef', '#1b1b1b', 'none', '', 'linear-gradient(#1d3a2c,#3d5a48)'],
  ['warm', '☀️ ورقي دافئ (حماية للعين)', 'تقليل الأشعة الزرقاء للقراءة الطويلة', '#f4e3c1', '#3b2a14', 'sepia(.35) saturate(1.1)', 'multiply', 'linear-gradient(#5a3e1c,#7a5a30)'],
  ['green', '🌿 أخضر هادئ', 'خلفية خضراء لطيفة ومريحة', '#dcebd3', '#1c2b1a', 'none', 'multiply', 'linear-gradient(#2f5a3a,#4a7a55)'],
  ['bright', '💡 نهاري ساطع (عالي التباين)', 'تباين عالٍ للإضاءة النهارية', '#ffffff', '#000000', 'contrast(1.5) brightness(1.02)', '', 'linear-gradient(#0f2a1e,#1d3a2c)'],
  ['dark', '🌙 وضع ليلي مريح (داكن)', 'خط كريمي على خلفية داكنة للظلام', '#1f1c17', '#eadfc4', 'invert(.92) hue-rotate(180deg) sepia(.35)', '', 'linear-gradient(#141210,#24201a)'],
  ['amoled', '🖤 وضع ليلي أسود (AMOLED)', 'توفير أقصى للبطارية في الإضاءة المنخفضة', '#000000', '#cfcfcf', 'invert(1) hue-rotate(180deg) brightness(.82)', '', '#000']
];
function appDark() {
  const t = document.documentElement.dataset.theme;
  return t === 'dark' || (t !== 'light' && window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches);
}
function shTheme() { const k = Q().shTheme || (appDark() ? 'dark' : 'orig'); return SH_TH.find((t) => t[0] === k) || SH_TH[0]; }
function themeCards() {
  const cur = shTheme()[0];
  return SH_TH.map((t) => '<div class="th' + (t[0] === cur ? ' on' : '') + '" data-th="' + t[0] + '"><div class="pv" style="background:' + t[3] + ';color:' + t[4] + '">بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</div>' +
    '<div class="ft"><div><b>' + t[1] + '</b><small>' + t[2] + '</small></div><button>' + (t[0] === cur ? 'مفعل حالياً ✓' : 'اختيار') + '</button></div></div>').join('');
}
function bindThemes(root, after) {
  root.querySelectorAll('.th[data-th]').forEach((el) => el.onclick = () => { Q().shTheme = el.dataset.th; ctx.persist(); after(); });
}
let shSt = null;   // حالة التحميل من الجهاز
function R() { return ctx && ctx.R; }
function refreshShStatus() {
  if (!R() || !R().shamarlyStatus) return Promise.resolve(null);
  return R().shamarlyStatus({ src: shSrc() }).then((st) => {
    shSt = st; shFiles = {};
    (st.files || []).forEach((n) => { const l = parseInt(n, 10); if (!isNaN(l) && (!shFiles[l] || /webp$/.test(n))) shFiles[l] = n; });
    return st;
  }).catch(() => null);
}
// من حمّل المصحف بالنسخة المصوّرة قبل إضافة النسخة الأخف: نبقيه عليها حتى لا يحمّل من جديد
function keepOldDownload() {
  if (Q().shSrc || !R() || !R().shamarlyStatus) return Promise.resolve();
  return R().shamarlyStatus({ src: 'archive' }).then((st) => {
    if (st && st.done >= 50) { Q().shSrc = 'archive'; ctx.persist(); }
  }).catch(() => {});
}
let shFiles = {};   // الصفحات المحفوظة على الهاتف: رقم ← اسم الملف
// حفظ الصفحة على الهاتف بعد قراءتها (مرة واحدة) حتى تفتح بعد ذلك بلا إنترنت
function cachePage(leaf) {
  if (leaf < 0 || leaf >= SH_LEAVES || shFiles[leaf] || !R() || !R().shamarlyCache) return;
  shFiles[leaf] = leaf + '.jpg';   // يُفترض النجاح؛ إن فشل يعود العرض للإنترنت تلقائيًا
  R().shamarlyCache({ leaf, src: shSrc() }).catch(() => {});
}
// صفحات مدمجة داخل التطبيق (تُضاف وقت البناء)؛ إن لم توجد نعود للمحفوظ أو للإنترنت
let shBundled = null;
function loadBundled() {
  if (shBundled) return Promise.resolve();
  return fetch('shamarly/index.json').then((r) => r.ok ? r.json() : null).then((j) => {
    shBundled = new Set(j && j.leaves ? j.leaves : []);
  }).catch(() => { shBundled = new Set(); });
}
function localSrc(leaf) {
  if (shBundled && shBundled.has(leaf)) return 'shamarly/' + leaf + '.webp';
  if (!shSt || !shFiles[leaf] || !window.Capacitor || !window.Capacitor.convertFileSrc) return null;
  return window.Capacitor.convertFileSrc(shSt.dir + '/' + shFiles[leaf]);
}
let shPoll = null;
function renderShamIndex(m) {
  const sh = SH();
  let body = '';
  if (idxTab === 'juz') {
    body = '<div class="card"><div class="sub" style="margin-bottom:8px">اختر الجزء</div><div class="q-juz">' +
      SH_JUZ.map((p, i) => '<button class="q-btn" data-l="' + p + '">' + AR(i + 1) + '</button>').join('') + '</div></div>';
  } else if (idxTab === 'marks') {
    body = '<div class="card"><div class="bm-h"><b>متابعة القراءة والعلامات المرجعية</b><small>اختر علامة للانتقال إليها أو اضغط مطولاً لتسميتها أو مسحها</small></div>' +
      '<div class="bm-sec">الفواصل والعلامات الملونة الخمس</div>' + BM().map((_, i) => bmCard(i, true)).join('') + '</div>';
  } else if (idxTab === 'look') {
    body = '<div class="card"><div class="bm-h"><b>مظهر القراءة والوضع الليلي</b><small>فلاتر لونية متقدمة تحافظ على دقة خط مصحف الشمرلي وتريح العين</small></div>' + themeCards() +
      '<div class="sub" style="text-align:center;line-height:1.8">الألوان تتغير على الشاشة فقط، وصفحات المصحف نفسها محفوظة كما هي بدون أي تعديل.</div></div>';
  } else if (idxTab === 'dl') {
    const st = shSt || { done: 0, total: SH_LEAVES, running: false };
    const pct = Math.round(st.done / st.total * 100);
    body = '<div class="card"><b>نسخة الصفحات</b>' +
      '<div class="q-tabs" style="margin-top:8px">' +
        '<button class="q-btn' + (shSrc() === 'qa' ? ' on' : '') + '" data-src="qa">الأخف (نص فقط)</button>' +
        '<button class="q-btn' + (shSrc() !== 'qa' ? ' on' : '') + '" data-src="archive">بالإطار والزخارف</button>' +
      '</div><div class="sub" style="margin-top:6px">النص واحد في النسختين. الأخف حجمها حوالي ١٢٠ ميجا كاملة، والتي بالإطار حوالي ١٨٠ ميجا.</div></div>' +
      '<div class="card"><b>تحميل المصحف للقراءة بلا إنترنت</b>' +
      '<div class="sub" style="line-height:1.9;margin:6px 0">كل صفحة تقرأها تُحفظ تلقائيًا على الهاتف كما هي من المصدر بدون أي ضغط. ولتحميل المصحف كله مرة واحدة (حوالي ' + (shSrc() === 'qa' ? '١٢٠' : '١٨٠') + ' ميجا) اضغط الزر. اترك التطبيق مفتوحًا على شبكة واي فاي حتى يكتمل، وإن توقف يكمل من حيث انتهى.</div>' +
      '<div class="prog"><i style="width:' + pct + '%"></i></div><div class="sub" id="shDlTxt">' + AR(st.done) + ' / ' + AR(st.total) + ' صفحة' + (st.running ? ' · جارٍ التحميل…' : '') + (st.error ? ' · توقف بسبب الاتصال' : '') + '</div>' +
      '<div class="row" style="margin-top:10px;justify-content:flex-start;gap:8px">' +
        (st.running ? '<button class="ghost" id="shStop">إيقاف</button>' : (st.done < st.total ? '<button class="pri" id="shDl">' + (st.done ? 'استكمال التحميل' : 'تحميل الآن') + '</button>' : '<span style="color:var(--ok)">✓ المصحف محفوظ في الهاتف</span>')) +
        (st.done && !st.running ? '<button class="ghost" id="shDel">حذف من الهاتف</button>' : '') +
      '</div>' + (R() ? '' : '<div class="sub" style="margin-top:8px">التحميل متاح في تطبيق أندرويد فقط.</div>') + '</div>';
  } else {
    body = '<div class="card q-list">' + m.surahs.map((s) =>
      '<div class="it" data-l="' + SH_SURA[s.number - 1] + '"><span class="num">' + AR(s.number) + '</span>' +
      '<span class="nm">' + esc(s.nameAr) + '<small>' + (s.revelation === 'makki' ? 'مكية' : 'مدنية') + ' · ' + AR(s.ayahCount) + ' آية</small></span>' +
      '<span class="pg">ص ' + AR(shPrinted(SH_SURA[s.number - 1])) + '</span></div>').join('') + '</div>';
  }
  ctx.view.innerHTML =
    '<div class="card">' + edSwitch('sham') + '<div class="row"><b style="font-size:18px">مصحف الشمرلي</b>' +
      '<button class="pri" id="shOpen">متابعة القراءة · ص ' + AR(shPrinted(sh.leaf || 1)) + '</button></div>' +
    '<div class="q-tabs">' +
      [['surah', 'السور'], ['juz', 'الأجزاء'], ['marks', 'العلامات'], ['look', 'المظهر'], ['dl', 'بلا إنترنت']].filter(([k]) => k !== 'dl' || !(shBundled && shBundled.size >= 500) || (shSt && shSt.done > 0)).map(([k, t]) =>
        '<button class="q-btn' + (idxTab === k ? ' on' : '') + '" data-t="' + k + '">' + t + '</button>').join('') +
    '</div></div>' + body +
    '<div class="q-credit">مصحف الشمرلي بخط محمد سعد إبراهيم «حداد»، نسخة مجانية يجوز نسخها وتداولها لغير الأغراض التجارية. النسخة المصوّرة بالإطار من archive.org، والنسخة الأخف من مشروع Quran Android (files.quran.app).</div>';
  bindEd();
  ctx.view.querySelectorAll('.q-tabs button[data-t]').forEach((b) => b.onclick = () => { idxTab = b.dataset.t; render(); });
  ctx.view.querySelectorAll('[data-l]').forEach((el) => el.onclick = () => { SH().leaf = +el.dataset.l; mode = 'sham'; render(); window.scrollTo(0, 0); });
  bindBm(ctx.view, (i) => { const b = BM()[i]; if (b.leaf == null) { bmEdit(i, render); return; } SH().leaf = b.leaf; mode = 'sham'; render(); window.scrollTo(0, 0); }, render);
  bindThemes(ctx.view, render);
  document.getElementById('shOpen').onclick = () => { mode = 'sham'; render(); window.scrollTo(0, 0); };
  ctx.view.querySelectorAll('[data-src]').forEach((b) => b.onclick = () => {
    if (shSrc() === b.dataset.src) return;
    if (R() && R().shamarlyStop) R().shamarlyStop({ src: shSrc() }).catch(() => {});
    Q().shSrc = b.dataset.src; ctx.persist(); shFiles = {}; shSt = null;
    Object.keys(shLoaded).forEach((k) => delete shLoaded[k]); Object.keys(shCrop).forEach((k) => delete shCrop[k]); shLastCrop = null;
    refreshShStatus().then(render);
  });
  const act = (fn) => () => { if (!R()) return; R()[fn]({ src: shSrc() }).then((st) => { shSt = st; refreshShStatus().then(render); }).catch(() => {}); };
  if (document.getElementById('shDl')) document.getElementById('shDl').onclick = act('shamarlyDownload');
  if (document.getElementById('shStop')) document.getElementById('shStop').onclick = act('shamarlyStop');
  if (document.getElementById('shDel')) document.getElementById('shDel').onclick = () => { if (confirm('حذف صفحات المصحف المحفوظة؟')) act('shamarlyDelete')(); };
  clearInterval(shPoll);
  if (idxTab === 'dl' && shSt && shSt.running) {
    shPoll = setInterval(() => {
      if (mode !== 'index' || idxTab !== 'dl' || Q().ed !== 'sham') { clearInterval(shPoll); return; }
      refreshShStatus().then(() => render());
    }, 1500);
  }
}
const shLoaded = {};
let shZoom = 1;
let shCtl = false;
let shHinted = false;   // أول دخول: تظهر الأزرار ثانيتين ثم تختفي
const shCrop = {};
let shLastCrop = null;
// مصدر الصور: 'qa' = نسخة Quran Android (أخف، نص فقط بخلفية شفافة)، 'archive' = النسخة المصوّرة بالإطار
function shSrc() { return Q().shSrc || 'qa'; }
function useQa(leaf) { return shSrc() === 'qa' && leaf >= 1; }
const QA_URL = 'https://files.quran.app/hafs/shemerly/width_1200/page';
function archUrl(leaf, w) { return 'https://archive.org/download/' + SH_ID + '/page/n' + leaf + (w ? '_w' + w : '') + '.jpg'; }
function shUrl(leaf, w) { return useQa(leaf) ? QA_URL + String(leaf).padStart(3, '0') + '.png' : archUrl(leaf, w); }
const isRemote = (u) => u.indexOf('archive.org') >= 0 || u.indexOf('quran.app') >= 0;
function renderSham() {
  const sh = SH();
  if (!sh.leaf) sh.leaf = 1;
  const leaf = Math.max(0, Math.min(SH_LEAVES - 1, sh.leaf));
  sh.leaf = leaf; ctx.persist(); trackRead('s' + leaf);
  const pageNo = shPrinted(leaf);
  const surahName = M ? M.surahs[shSurahOf(leaf) - 1].nameAr : '';
  const bmi = BM().findIndex((b) => b.leaf === leaf);
  const th = shTheme();
  const w = 900;   // دقة كافية للقراءة وأسرع في التحميل
  const wLow = 360;
  document.body.classList.add('reading');
  document.body.style.setProperty('--shbg', th[3]);
  ctx.view.innerHTML =
    '<div class="sh-head" style="background:' + th[7] + '"><span>سورة ' + esc(surahName) + '</span><span>ص ' + AR(pageNo) + '</span><span>جزء ' + AR(shJuzOf(leaf)) + ' | حزب ' + AR(shHizbOf(leaf)) + '</span></div>' +
    '<div class="q-page" id="qPage" style="background:' + th[3] + ';border-color:' + th[3] + ';position:relative;padding:0;margin:0;border-radius:0;border-left:0;border-right:0;min-height:60vh;display:flex;align-items:flex-start;justify-content:center;overflow:hidden;touch-action:pan-y">' +
      '<div id="shBox" style="position:relative;width:100%;overflow:hidden;transform-origin:center center;will-change:transform">' +
      '<img id="shImg" class="' + (useQa(leaf) ? 'qa' : '') + '" alt="" draggable="false" style="width:100%;height:auto;display:block;filter:' + th[5] + (th[6] ? ';mix-blend-mode:' + th[6] : '') + '" src="' + (localSrc(leaf) || (shLoaded[leaf] ? shUrl(leaf, w) : shUrl(leaf, wLow))) + '">' +
      '</div>' +
      '<div id="shMsg" class="sub" style="position:absolute;top:40%">جارٍ تحميل الصفحة…</div>' +
    '</div>' +
    '<div class="sh-ctl' + (shCtl ? ' on' : '') + '" id="shCtl">' +
      '<div class="q-bar">' +
        '<button class="q-btn" id="qBack">☰ الفهرس</button>' +
        '<button class="q-btn" id="qHome">⌂ الرئيسية</button>' +
        '<button class="q-btn" id="qLook">🎨 المظهر</button>' +
        '<button class="q-btn" id="qMark"' + (bmi >= 0 ? ' style="background:' + BM_DEF[bmi][1] + ';color:#fff;border-color:' + BM_DEF[bmi][1] + '"' : '') + '>🔖</button>' +
      '</div>' +
      '<div class="q-bar" style="margin-top:8px">' +
        '<button class="q-btn" id="qNext">→ التالية</button>' +
        '<button class="q-btn" id="qZout" aria-label="تصغير">－</button>' +
        '<button class="q-btn" id="qGo">انتقال</button>' +
        '<button class="q-btn" id="qZin" aria-label="تكبير">＋</button>' +
        '<button class="q-btn" id="qPrev">السابقة ←</button>' +
      '</div>' +
      '<div class="sub" style="text-align:center;margin-top:6px">المس الصفحة مرة لإظهار/إخفاء الأزرار، ومرتين للتكبير، واسحب يمينًا أو يسارًا للتقليب</div>' +
    '</div>';
  const img = document.getElementById('shImg'), msg = document.getElementById('shMsg');
  const box = document.getElementById('shBox');
  // قص الهوامش البيضاء حتى تملأ الصفحة عرض الشاشة مثل مصحف المدينة
  const applyCrop = (cr) => {
    if (!cr || !img.naturalWidth) return;
    box.style.width = '100%';
    let W = box.clientWidth || 360;
    let dw = W / cr.w, dh = dw * img.naturalHeight / img.naturalWidth;   // مقاس الصورة كاملة بعد التكبير
    // لو الصفحة أطول من الشاشة: صغّرها لتظهر كاملة بدون تمرير
    const head = document.querySelector('.sh-head');
    const avail = window.innerHeight - (head ? head.getBoundingClientRect().bottom : 0) - 4;
    if (cr.h * dh > avail && avail > 300) { const f = avail / (cr.h * dh); W = W * f; dw *= f; dh *= f; box.style.width = Math.round(W) + 'px'; }
    box.style.height = Math.round(cr.h * dh) + 'px';
    img.style.position = 'absolute';
    img.style.width = Math.round(dw) + 'px';
    img.style.maxWidth = 'none';
    img.style.left = Math.round(-cr.x * dw) + 'px';
    img.style.top = Math.round(-cr.y * dh) + 'px';
  };
  const detectCrop = (src) => {
    if (shCrop[leaf]) return;
    const t = new Image();
    t.crossOrigin = 'anonymous';
    t.onload = () => {
      try {
        const cw = 240, ch = Math.round(cw * t.naturalHeight / t.naturalWidth);
        const cv = document.createElement('canvas'); cv.width = cw; cv.height = ch;
        const g = cv.getContext('2d'); g.drawImage(t, 0, 0, cw, ch);
        const d = g.getImageData(0, 0, cw, ch).data;
        const lum = (i) => (d[i] * 3 + d[i + 1] * 6 + d[i + 2]) / 10;
        let bg = 0, al = 0; [0, cw - 1, (ch - 1) * cw, ch * cw - 1].forEach((p) => { bg += lum(p * 4); al += d[p * 4 + 3]; }); bg /= 4;
        const clear = al < 40;   // خلفية شفافة (نسخة Quran Android): الحبر = ما ليس شفافًا
        const ink = clear ? (x, y) => d[(y * cw + x) * 4 + 3] > 60 : (x, y) => Math.abs(lum((y * cw + x) * 4) - bg) > 45;
        const rowHas = (y) => { let n = 0; for (let x = 0; x < cw; x++) if (ink(x, y)) n++; return n > cw * 0.02; };
        const colHas = (x) => { let n = 0; for (let y = 0; y < ch; y++) if (ink(x, y)) n++; return n > ch * 0.02; };
        let y0 = 0, y1 = ch - 1, x0 = 0, x1 = cw - 1;
        while (y0 < ch / 3 && !rowHas(y0)) y0++;
        while (y1 > ch * 2 / 3 && !rowHas(y1)) y1--;
        while (x0 < cw / 3 && !colHas(x0)) x0++;
        while (x1 > cw * 2 / 3 && !colHas(x1)) x1--;
        const pad = 0.012;
        const cr = { x: Math.max(0, x0 / cw - pad), y: Math.max(0, y0 / ch - pad) };
        cr.w = Math.min(1, x1 / cw + pad) - cr.x; cr.h = Math.min(1, y1 / ch + pad) - cr.y;
        if (cr.w > 0.5 && cr.h > 0.5) { shCrop[leaf] = cr; shLastCrop = cr; if (SH().leaf === leaf) applyCrop(cr); }
      } catch (e) { /* الصورة لا تسمح بالقراءة: نعرضها كما هي */ }
    };
    t.src = src;
  };
  img.onload = () => {
    if (msg.parentNode) msg.remove();
    if (isRemote(img.src)) { cachePage(leaf); cachePage(leaf + 1); }
    applyCrop(shCrop[leaf] || shLastCrop);
    detectCrop(img.src);
    // بعد ظهور النسخة السريعة، حمّل النسخة الواضحة واستبدلها
    if (img.src.indexOf('_w' + wLow) > 0) {
      const hi = new Image();
      hi.onload = () => { shLoaded[leaf] = 1; if (SH().leaf === leaf) img.src = hi.src; };
      hi.src = shUrl(leaf, w);
    }
  };
  img.onerror = () => {
    const stage = +(img.dataset.stage || 0);
    img.dataset.stage = stage + 1;
    if (stage === 0 && !isRemote(img.src)) { img.src = shUrl(leaf, wLow); return; }   // الصفحة غير محفوظة بعد
    if (stage <= 1) { img.src = shUrl(leaf, 0); return; }
    if (stage === 2 && useQa(leaf)) { img.src = archUrl(leaf, 900); return; }   // احتياطي: النسخة الأخرى
    msg.innerHTML = 'تعذّر تحميل الصفحة. تأكد من الاتصال بالإنترنت.<br><button class="q-btn" id="shRetry" style="margin-top:8px">إعادة المحاولة</button> <button class="q-btn" id="shWeb" style="margin-top:8px">فتح في المتصفح</button>';
    document.getElementById('shRetry').onclick = () => renderSham();
    document.getElementById('shWeb').onclick = () => {
      const u = 'https://archive.org/details/' + SH_ID + '/page/n' + leaf + '/mode/1up';
      const R = window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.Reminder;
      if (R && R.openUrl) R.openUrl({ url: u }); else window.open(u, '_blank');
    };
  };
  // تحميل مسبق للصفحات المجاورة حتى يكون التقليب فوريًا
  [1, 2, 3, -1].forEach((d) => {
    const t = leaf + d;
    if (t >= 0 && t < SH_LEAVES && !shLoaded[t] && !(shSt && shSt.done >= shSt.total) && !(shBundled && shBundled.has(t)) && !shFiles[t]) { const p = new Image(); p.onload = () => { shLoaded[t] = 1; }; p.src = shUrl(t, w); }
  });
  const go = (d) => { const t = leaf + d; if (t >= 0 && t < SH_LEAVES) { sh.leaf = t; renderSham(); window.scrollTo(0, 0); } };
  document.getElementById('qNext').onclick = () => go(1);
  document.getElementById('qPrev').onclick = () => go(-1);
  document.getElementById('qBack').onclick = () => { mode = 'index'; Q().ed = 'sham'; render(); };
  if (!shHinted) {
    shHinted = true;
    const el = document.getElementById('shCtl'); el.classList.add('on');
    setTimeout(() => { if (!shCtl) { const e2 = document.getElementById('shCtl'); if (e2) e2.classList.remove('on'); } }, 2500);
  }
  document.getElementById('qHome').onclick = () => { document.body.classList.remove('reading'); mode = 'index'; if (ctx.go) ctx.go('home'); };
  document.getElementById('qMark').onclick = () => {
    const el = sheet('<h3>حفظ ص ' + AR(pageNo) + ' في علامة</h3><div class="sub" style="text-align:center;margin-bottom:10px">اختر لون العلامة، أو اضغط مطولاً لتسميتها أو مسحها</div>' + BM().map((_, i) => bmCard(i, false)).join(''));
    bindBm(el, (i) => { const b = BM()[i]; b.leaf = leaf; b.at = Date.now(); ctx.persist(); el.remove(); renderSham(); }, () => { el.remove(); renderSham(); document.getElementById('qMark').click(); });
  };
  document.getElementById('qLook').onclick = () => {
    const el = sheet('<h3>مظهر القراءة</h3><div class="sub" style="text-align:center;margin-bottom:10px">الألوان على الشاشة فقط، وصفحات المصحف كما هي</div>' + themeCards());
    bindThemes(el, () => { el.remove(); renderSham(); });
  };
  // أول مرة: اعرض تحميل المصحف كله ليعمل بلا إنترنت
  if (R() && R().shamarlyDownload && !Q().shAsked && shSt && shSt.done < shSt.total && !shSt.running) {
    Q().shAsked = 1; ctx.persist();
    const el = sheet('<h3>تحميل المصحف للقراءة بلا إنترنت</h3><div class="sub" style="line-height:1.9;margin:8px 0 12px;text-align:center">تحب تحمّل مصحف الشمرلي كامل مرة واحدة (حوالي ' + (shSrc() === 'qa' ? '١٢٠' : '١٨٠') + ' ميجا) عشان يشتغل بعد كده من غير نت؟<br>يفضل تكون على واي فاي. ولو اخترت «بعدين» الصفحات اللي تقراها هتتحفظ لوحدها، وتقدر تحمّل الكل من الفهرس ← بلا إنترنت.</div>' +
      '<div class="row" style="gap:8px;justify-content:center"><button class="pri" id="shAskY">تحميل الآن</button><button class="ghost" id="shAskN">بعدين</button></div>');
    el.querySelector('#shAskN').onclick = () => el.remove();
    el.querySelector('#shAskY').onclick = () => {
      R().shamarlyDownload({ src: shSrc() }).then((st) => { shSt = st; }).catch(() => {});
      el.firstChild.innerHTML = '<h3>✓ بدأ التحميل</h3><div class="sub" style="text-align:center;line-height:1.9;margin:8px 0">سيب التطبيق مفتوح لحد ما يخلص. تقدر تتابع التقدم من الفهرس ← بلا إنترنت.</div><div class="row" style="justify-content:center"><button class="pri" id="shAskOk">تمام</button></div>';
      el.querySelector('#shAskOk').onclick = () => el.remove();
    };
  }
  const num = (v) => parseInt(String(v).replace(/[٠-٩]/g, (d) => '٠١٢٣٤٥٦٧٨٩'.indexOf(d)), 10);
  document.getElementById('qGo').onclick = () => {
    const t = num(prompt('رقم الصفحة', String(pageNo)) || '');
    if (t >= 1 && t - 1 < SH_LEAVES) { sh.leaf = t - 1; renderSham(); window.scrollTo(0, 0); }
  };
  const pg = document.getElementById('qPage');
  // ---- التكبير والتحريك ----
  let tx = 0, ty = 0, hiReq = false;
  const lim = () => ({ x: (shZoom - 1) * box.clientWidth / 2, y: (shZoom - 1) * box.clientHeight / 2 });
  const apply = () => {
    const L = lim();
    tx = Math.max(-L.x, Math.min(L.x, tx)); ty = Math.max(-L.y, Math.min(L.y, ty));
    box.style.transform = shZoom > 1 ? 'translate(' + tx + 'px,' + ty + 'px) scale(' + shZoom + ')' : '';
    pg.style.touchAction = shZoom > 1 ? 'none' : 'pan-y';
    // عند التكبير حمّل النسخة الأصلية الأوضح (إن توفر الإنترنت)
    if (shZoom > 1.4 && !hiReq) {
      hiReq = true;
      const full = new Image();
      full.onload = () => { if (SH().leaf === leaf) img.src = full.src; };
      full.src = shUrl(leaf, 0);
    }
  };
  const setZoom = (z, top) => {
    shZoom = Math.max(1, Math.min(4, z));
    if (top) { const L = lim(); tx = L.x; ty = L.y; }   // ابدأ من أعلى يمين الصفحة
    apply();
  };
  if (shZoom > 1) { if (img.complete && img.clientWidth) setZoom(shZoom, true); else img.addEventListener('load', () => setZoom(shZoom, true), { once: true }); }
  document.getElementById('qZin').onclick = () => setZoom(shZoom + 0.5, shZoom === 1);
  document.getElementById('qZout').onclick = () => setZoom(shZoom - 0.5);
  let x0 = null, y0 = null, px = 0, py = 0, d0 = 0, z0 = 1, pinch = false, lastTap = 0, tapT = null;
  const dist = (t) => Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);
  pg.addEventListener('touchstart', (e) => {
    if (e.touches.length === 2) { pinch = true; d0 = dist(e.touches) || 1; z0 = shZoom; x0 = null; return; }
    x0 = px = e.touches[0].clientX; y0 = py = e.touches[0].clientY;
  }, { passive: true });
  pg.addEventListener('touchmove', (e) => {
    if (pinch && e.touches.length === 2) { e.preventDefault(); shZoom = Math.max(1, Math.min(4, z0 * dist(e.touches) / d0)); apply(); return; }
    if (shZoom > 1 && e.touches.length === 1 && x0 !== null) {
      e.preventDefault();
      const x = e.touches[0].clientX, y = e.touches[0].clientY;
      tx += x - px; ty += y - py; px = x; py = y; apply();
    }
  }, { passive: false });
  pg.addEventListener('touchend', (e) => {
    if (pinch) { if (e.touches.length === 0) pinch = false; x0 = null; return; }
    if (x0 === null) return;
    const dx = e.changedTouches[0].clientX - x0, dy = e.changedTouches[0].clientY - y0;
    x0 = null;
    if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {   // لمستان سريعتان = تكبير/تصغير
      const now = Date.now();
      if (now - lastTap < 320) { lastTap = 0; clearTimeout(tapT); setZoom(shZoom > 1 ? 1 : 2.2, true); }
      else { lastTap = now; clearTimeout(tapT); tapT = setTimeout(() => { shCtl = !shCtl; const el = document.getElementById('shCtl'); if (el) el.classList.toggle('on', shCtl); }, 330); }
      return;
    }
    if (shZoom === 1 && Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) go(dx > 0 ? 1 : -1);
  });
}

function render() {
  if (!M) return;
  if (mode !== 'sham') document.body.classList.remove('reading');
  document.querySelectorAll('.sh-sheet').forEach((e) => e.remove());
  if (mode === 'page') renderPage(M);
  else if (mode === 'sham') renderSham();
  else renderIndex(M);
}

export function show(c) {
  ctx = c;
  ensureCss();
  if (!M) ctx.view.innerHTML = '<div class="card sub" style="text-align:center;padding:30px">جارٍ تحميل المصحف…</div>';
  return Promise.all([mushaf(), keepOldDownload().then(refreshShStatus), loadBundled()]).then(() => {
    if (document.fonts && document.fonts.load) return document.fonts.load('22px "KFGQPC HAFS Uthmanic Script"').catch(() => {});
  }).then(render).catch((e) => {
    ctx.view.innerHTML = '<div class="card sub">تعذّر تحميل المصحف: ' + esc(e && e.message || e) + '</div>';
  });
}

/** فتح سورة مباشرة (مثل الكهف يوم الجمعة) في الطبعة المختارة. */
export function openSurah(c, n) {
  ctx = c;
  ensureCss();
  return mushaf().then(() => {
    if (Q().ed === 'sham') { SH().leaf = SH_SURA[n - 1]; mode = 'sham'; }
    else { Q().page = M.surahs[n - 1].page.number; mode = 'page'; }
    ctx.persist();
    return show(c);
  });
}
