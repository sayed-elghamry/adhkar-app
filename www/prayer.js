// مواقيت الصلاة + منبّه لكل صلاة + اتجاه القبلة. يعتمد على مكتبة adhan (تُحمَّل في index.html).
(function () {
  'use strict';
  var A = window.adhan;
  var AR = function (n) { return String(n).replace(/\d/g, function (d) { return '٠١٢٣٤٥٦٧٨٩'[d]; }); };
  var esc = function (s) { return String(s).replace(/[&<>"]/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]; }); };

  var NAMES = { fajr: 'الفجر', sunrise: 'الشروق', dhuhr: 'الظهر', asr: 'العصر', maghrib: 'المغرب', isha: 'العشاء' };
  var ORDER = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];
  var ALARMABLE = ['fajr', 'dhuhr', 'asr', 'maghrib', 'isha'];
  var METHODS = [
    ['Egyptian', 'الهيئة المصرية العامة للمساحة'],
    ['UmmAlQura', 'أم القرى (مكة المكرمة)'],
    ['MuslimWorldLeague', 'رابطة العالم الإسلامي'],
    ['Karachi', 'جامعة العلوم الإسلامية بكراتشي'],
    ['Dubai', 'دبي'],
    ['Kuwait', 'الكويت'],
    ['Qatar', 'قطر'],
    ['MoonsightingCommittee', 'لجنة رؤية الهلال'],
    ['NorthAmerica', 'أمريكا الشمالية (ISNA)'],
    ['Turkey', 'تركيا']
  ];
  var CITIES = [
    ['القاهرة', 30.0444, 31.2357], ['الجيزة', 30.0131, 31.2089], ['الإسكندرية', 31.2001, 29.9187],
    ['المنصورة', 31.0409, 31.3785], ['طنطا', 30.7865, 31.0004], ['الزقازيق', 30.5877, 31.502],
    ['شبين الكوم', 30.5503, 31.0106], ['بنها', 30.4659, 31.1848], ['دمنهور', 31.0341, 30.4682],
    ['كفر الشيخ', 31.1107, 30.9388], ['دمياط', 31.4165, 31.8133], ['بورسعيد', 31.2653, 32.3019],
    ['الإسماعيلية', 30.5965, 32.2715], ['السويس', 29.9668, 32.5498], ['الفيوم', 29.3084, 30.8428],
    ['بني سويف', 29.0661, 31.0994], ['المنيا', 28.0871, 30.7618], ['أسيوط', 27.1783, 31.1859],
    ['سوهاج', 26.5591, 31.6957], ['قنا', 26.1551, 32.716], ['الأقصر', 25.6872, 32.6396],
    ['أسوان', 24.0889, 32.8998], ['الغردقة', 27.2579, 33.8116], ['مرسى مطروح', 31.3543, 27.2373],
    ['مكة المكرمة', 21.3891, 39.8579], ['المدينة المنورة', 24.5247, 39.5692], ['الرياض', 24.7136, 46.6753],
    ['جدة', 21.4858, 39.1925], ['الدمام', 26.4207, 50.0888], ['دبي', 25.2048, 55.2708],
    ['أبوظبي', 24.4539, 54.3773], ['الدوحة', 25.2854, 51.531], ['الكويت', 29.3759, 47.9774],
    ['المنامة', 26.2285, 50.586], ['مسقط', 23.588, 58.3829], ['عمّان', 31.9454, 35.9284],
    ['القدس', 31.7683, 35.2137], ['بيروت', 33.8938, 35.5018], ['دمشق', 33.5138, 36.2765],
    ['بغداد', 33.3152, 44.3661], ['الخرطوم', 15.5007, 32.5599], ['طرابلس', 32.8872, 13.1913],
    ['تونس', 36.8065, 10.1815], ['الجزائر', 36.7538, 3.0588], ['الرباط', 34.0209, -6.8416]
  ];

  var ctx = null;
  function P() {
    var S = ctx.S;
    S.prayer = S.prayer || { lat: null, lng: null, place: '', method: 'Egyptian', madhab: 'shafi', alarms: { fajr: true, dhuhr: true, asr: true, maghrib: true, isha: true }, on: false };
    return S.prayer;
  }

  function params(p) {
    var m = (A.CalculationMethod[p.method] || A.CalculationMethod.Egyptian)();
    m.madhab = p.madhab === 'hanafi' ? A.Madhab.Hanafi : A.Madhab.Shafi;
    return m;
  }
  function timesFor(p, date) {
    return new A.PrayerTimes(new A.Coordinates(p.lat, p.lng), date, params(p));
  }
  function fmt(d) {
    var h = d.getHours(), m = d.getMinutes();
    return AR((h % 12 || 12) + ':' + String(m).padStart(2, '0')) + ' ' + (h >= 12 ? 'م' : 'ص');
  }
  function hijriDay(d) {
    try { return parseInt(new Intl.DateTimeFormat('en-u-ca-islamic-umalqura-nu-latn', { day: 'numeric' }).format(d), 10) || 0; } catch (e) { return 0; }
  }
  function hijri(d) {
    try { return new Intl.DateTimeFormat('ar-EG-u-ca-islamic-umalqura', { day: 'numeric', month: 'long', year: 'numeric' }).format(d); } catch (e) { return ''; }
  }

  /** الصلاة القادمة: {key, name, at} */
  function nextPrayer(p) {
    if (p.lat == null) return null;
    var now = new Date();
    var t = timesFor(p, now);
    for (var i = 0; i < ORDER.length; i++) {
      var k = ORDER[i];
      if (k !== 'sunrise' && t[k] > now) return { key: k, name: NAMES[k], at: t[k] };
    }
    var tm = new Date(now); tm.setDate(tm.getDate() + 1);
    var t2 = timesFor(p, tm);
    return { key: 'fajr', name: NAMES.fajr, at: t2.fajr };
  }

  /** أرسل مواقيت ٦٠ يومًا لمنبّه أندرويد */
  function syncAlarms() {
    var p = P(), R = ctx.R;
    if (!R || !R.setPrayerAlarms) return Promise.resolve();
    var st = ctx.S.settings || {};
    var mOn = st.mAlarm !== false, eOn = st.eAlarm !== false, mAt = st.mAt == null ? 360 : st.mAt;
    // بدون موقع: نحسب المغرب على القاهرة حتى يعمل تنبيه أذكار المساء
    var loc = p.lat == null ? { lat: 30.0444, lng: 31.2357, method: p.method, madhab: p.madhab } : p;
    var list = [], d = new Date(), now = Date.now();
    for (var i = 0; i < 60; i++) {
      var day = new Date(d.getFullYear(), d.getMonth(), d.getDate() + i, 12);
      var t = timesFor(loc, day);
      if (p.on && p.lat != null) ALARMABLE.forEach(function (k) {
        if (p.alarms[k] && t[k].getTime() > now) list.push({ t: t[k].getTime(), name: NAMES[k] });
      });
      var mt = new Date(d.getFullYear(), d.getMonth(), d.getDate() + i, Math.floor(mAt / 60), mAt % 60).getTime();
      if (mOn && mt > now) list.push({ t: mt, name: 'أذكار الصباح', kind: 'morning' });
      var et = t.maghrib.getTime() - 15 * 60000;
      if (eOn && et > now) list.push({ t: et, name: 'أذكار المساء', kind: 'evening' });
      // صلاة الضحى: بعد الشروق بعدد دقائق يختاره المستخدم
      var dt = t.sunrise.getTime() + (st.duhaMin == null ? 20 : st.duhaMin) * 60000;
      if (st.duha !== false && dt > now) list.push({ t: dt, kind: 'note', id: 1, name: 'صلاة الضحى ☀', text: 'دخل وقت صلاة الضحى. «ويُجزئ من ذلك ركعتان يركعهما من الضحى» (رواه مسلم)' });
      // منبّه قبل الفجر (اختياري): ٥ أو ١٠ أو ١٥ دقيقة
      if (st.preFajr) { var pf = t.fajr.getTime() - st.preFajr * 60000; if (pf > now) list.push({ t: pf, kind: 'prefajr', name: 'استيقظ لصلاة الفجر', text: 'باقي ' + AR(st.preFajr) + ' دقائق على أذان الفجر. «الصلاة خير من النوم»' }); }
      var dow = day.getDay();
      // الجمعة: سورة الكهف والصلاة على النبي
      var ft = new Date(day.getFullYear(), day.getMonth(), day.getDate(), 9, 0).getTime();
      if (st.friday !== false && dow === 5 && ft > now) list.push({ t: ft, kind: 'note', id: 2, name: 'يوم الجمعة 🕌', text: 'لا تنسَ قراءة سورة الكهف، وأكثِر من الصلاة على النبي ﷺ', open: 'kahf' });
      // الصيام: ليلة الاثنين والخميس، وليلة الأيام البيض (١٣ و١٤ و١٥ هجري)
      if (st.fast !== false) {
        var tom = new Date(day.getFullYear(), day.getMonth(), day.getDate() + 1, 12);
        var hd = hijriDay(tom), why = [];
        if (tom.getDay() === 1) why.push('صيام يوم الاثنين');
        if (tom.getDay() === 4) why.push('صيام يوم الخميس');
        if (hd >= 13 && hd <= 15) why.push('صيام الأيام البيض (' + AR(hd) + ' من الشهر الهجري)');
        var nt = new Date(day.getFullYear(), day.getMonth(), day.getDate(), 21, 0).getTime();
        if (why.length && nt > now) list.push({ t: nt, kind: 'note', id: 3, name: 'تذكير بالصيام غدًا', text: 'غدًا: ' + why.join('، و') + '. لا تنسَ نية الصيام والسحور، فإن في السحور بركة.' });
      }
    }
    return R.setPrayerAlarms({ enabled: list.length > 0, times: list, adhan: st.adhan !== false }).catch(function () {});
  }

  function locate() {
    var p = P();
    return new Promise(function (res, rej) {
      if (!navigator.geolocation) { rej(new Error('no geo')); return; }
      navigator.geolocation.getCurrentPosition(function (pos) {
        p.lat = +pos.coords.latitude.toFixed(4); p.lng = +pos.coords.longitude.toFixed(4); p.place = 'موقعي الحالي';
        ctx.persist(); res();
      }, function (e) { rej(e); }, { enableHighAccuracy: false, timeout: 20000, maximumAge: 3600e3 });
    });
  }

  /* ---------- البوصلة ---------- */
  var heading = null, compassOn = false;
  function onOrient(e) {
    var h = null;
    if (typeof e.webkitCompassHeading === 'number') h = e.webkitCompassHeading;
    else if (e.absolute && typeof e.alpha === 'number') h = 360 - e.alpha;
    if (h == null) return;
    // تصحيح دوران الشاشة
    var so = (screen.orientation && screen.orientation.angle) || 0;
    heading = (h + so + 360) % 360;
    var arrow = document.getElementById('qbArrow'), dial = document.getElementById('qbDial'), txt = document.getElementById('qbTxt');
    if (!arrow) return;
    var q = A.Qibla(new A.Coordinates(P().lat, P().lng));
    dial.style.transform = 'rotate(' + (-heading) + 'deg)';
    var diff = ((q - heading) % 360 + 540) % 360 - 180;
    arrow.style.transform = 'rotate(' + (q - heading) + 'deg)';
    var ok = Math.abs(diff) < 4;
    arrow.style.color = ok ? 'var(--ok)' : 'var(--gold)';
    txt.textContent = ok ? '✓ أنت في اتجاه القبلة' : ('أدر الهاتف ' + (diff > 0 ? 'يمينًا' : 'يسارًا') + ' ' + AR(Math.round(Math.abs(diff))) + '°');
  }
  function startCompass() {
    if (compassOn) return;
    compassOn = true;
    if ('ondeviceorientationabsolute' in window) window.addEventListener('deviceorientationabsolute', onOrient);
    else window.addEventListener('deviceorientation', onOrient);
  }
  function stopCompass() {
    compassOn = false;
    window.removeEventListener('deviceorientationabsolute', onOrient);
    window.removeEventListener('deviceorientation', onOrient);
  }

  /* ---------- الشاشة ---------- */
  var tick = null;
  function render() { render0(); if (ctx.after) ctx.after(); }
  function render0() {
    var p = P(), v = ctx.view;
    clearInterval(tick);
    if (p.lat == null) {
      v.innerHTML =
        '<div class="card"><b style="font-size:18px">مواقيت الصلاة والقبلة</b>' +
        '<div class="sub" style="line-height:1.9;margin:8px 0">حدّد موقعك مرة واحدة لحساب المواقيت واتجاه القبلة. الحساب يتم على الهاتف بلا إنترنت.</div>' +
        '<button class="pri" id="pLoc">📍 استخدم موقعي الحالي</button>' +
        '<div class="sub" id="pErr" style="margin-top:8px;color:var(--warn)"></div>' +
        '<div class="sw" style="margin-top:8px"><span>أو اختر مدينتك</span><select id="pCity"><option value="">—</option>' +
        CITIES.map(function (c, i) { return '<option value="' + i + '">' + c[0] + '</option>'; }).join('') + '</select></div></div>';
      bindLoc();
      return;
    }
    var now = new Date(), t = timesFor(p, now), np = nextPrayer(p);
    var q = A.Qibla(new A.Coordinates(p.lat, p.lng));
    var rows = ORDER.map(function (k) {
      var isNext = np && np.key === k && t[k] > now;
      return '<div class="sw" style="' + (isNext ? 'background:rgba(31,107,79,.08);border-radius:10px;padding-inline:8px' : '') + '"><span>' + (isNext ? '<b>' : '') + NAMES[k] + (isNext ? '</b>' : '') + '</span>' +
        '<span style="display:flex;gap:12px;align-items:center"><b>' + fmt(t[k]) + '</b>' +
        (ALARMABLE.indexOf(k) >= 0 ? '<input type="checkbox" class="pAl" data-k="' + k + '" ' + (p.alarms[k] ? 'checked' : '') + ' aria-label="منبّه">' : '<span style="width:22px"></span>') +
        '</span></div>';
    }).join('');
    v.innerHTML =
      '<div class="card" style="text-align:center"><div class="sub">' + esc(hijri(now)) + '</div>' +
        '<div style="font:700 15px Tajawal;margin-top:6px">الصلاة القادمة: ' + (np ? np.name : '') + '</div>' +
        '<div id="pCount" style="font:700 34px Tajawal;color:var(--green);margin:4px 0"></div>' +
        '<div class="sub">📍 ' + esc(p.place || '') + ' · <a href="#" id="pChange" style="color:var(--green)">تغيير</a></div></div>' +
      '<div class="card"><div class="row"><b style="font-size:17px">مواقيت اليوم</b><span class="sub">المنبّه</span></div>' + rows +
        '<div class="sw"><span>تشغيل منبّه الصلوات</span><input type="checkbox" id="pOn" ' + (p.on ? 'checked' : '') + '></div>' +
        '<div class="sub" style="line-height:1.8">يعمل المنبّه والهاتف مقفل لمدة شهرين من آخر فتح للتطبيق، ويتجدد تلقائيًا كلما فتحته.</div></div>' +
      '<div class="card" style="text-align:center"><b style="font-size:17px">اتجاه القبلة</b>' +
        '<div class="sub" style="margin:4px 0">' + AR(Math.round(q)) + '° من الشمال</div>' +
        '<div style="position:relative;width:240px;height:240px;margin:10px auto">' +
          '<div id="qbDial" style="position:absolute;inset:0;border-radius:50%;border:2px solid var(--line);transition:transform .15s linear">' +
            '<span style="position:absolute;top:6px;left:50%;transform:translateX(-50%);font:700 14px Tajawal;color:var(--warn)">ش</span>' +
            '<span style="position:absolute;bottom:6px;left:50%;transform:translateX(-50%);font:500 13px Tajawal;color:var(--muted)">ج</span>' +
            '<span style="position:absolute;right:8px;top:50%;transform:translateY(-50%);font:500 13px Tajawal;color:var(--muted)">ق</span>' +
            '<span style="position:absolute;left:8px;top:50%;transform:translateY(-50%);font:500 13px Tajawal;color:var(--muted)">غ</span>' +
          '</div>' +
          '<div id="qbArrow" style="position:absolute;inset:0;display:flex;justify-content:center;color:var(--gold);transition:transform .15s linear;transform:rotate(' + q + 'deg)">' +
            '<div style="display:flex;flex-direction:column;align-items:center;margin-top:18px"><span style="font-size:30px;line-height:1">🕋</span><span style="width:4px;height:78px;background:currentColor;border-radius:2px"></span></div>' +
          '</div>' +
        '</div>' +
        '<div id="qbTxt" class="sub">ضع الهاتف أفقيًا بعيدًا عن المعادن. إن لم يتحرك السهم فاعتمد على الزاوية المكتوبة أعلاه.</div></div>' +
      '<div class="card"><b>إعدادات الحساب</b>' +
        '<div class="sw"><span>طريقة الحساب</span><select id="pMethod">' + METHODS.map(function (m) { return '<option value="' + m[0] + '"' + (p.method === m[0] ? ' selected' : '') + '>' + m[1] + '</option>'; }).join('') + '</select></div>' +
        '<div class="sw"><span>وقت العصر</span><select id="pMadhab"><option value="shafi"' + (p.madhab !== 'hanafi' ? ' selected' : '') + '>الجمهور</option><option value="hanafi"' + (p.madhab === 'hanafi' ? ' selected' : '') + '>الحنفي</option></select></div></div>';

    var cd = document.getElementById('pCount');
    var upd = function () {
      var n = nextPrayer(p); if (!n) return;
      var s = Math.max(0, Math.floor((n.at - new Date()) / 1000));
      cd.textContent = AR(String(Math.floor(s / 3600)).padStart(2, '0') + ':' + String(Math.floor(s / 60) % 60).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0'));
      if (s === 0) setTimeout(render, 1500);
    };
    upd(); tick = setInterval(function () { if (!document.getElementById('pCount')) { clearInterval(tick); stopCompass(); return; } upd(); }, 1000);
    startCompass();

    document.getElementById('pChange').onclick = function (e) { e.preventDefault(); p.lat = null; ctx.persist(); render(); };
    document.getElementById('pOn').onchange = function () {
      p.on = this.checked; ctx.persist(); syncAlarms();
      if (p.on && ctx.R && ctx.R.requestNotifications) ctx.R.requestNotifications().catch(function () {});
    };
    Array.prototype.forEach.call(v.querySelectorAll('.pAl'), function (b) {
      b.onchange = function () { p.alarms[b.dataset.k] = b.checked; ctx.persist(); syncAlarms(); };
    });
    document.getElementById('pMethod').onchange = function () { p.method = this.value; ctx.persist(); syncAlarms(); render(); };
    document.getElementById('pMadhab').onchange = function () { p.madhab = this.value; ctx.persist(); syncAlarms(); render(); };
  }

  function bindLoc() {
    var p = P();
    document.getElementById('pLoc').onclick = function () {
      var err = document.getElementById('pErr'); err.textContent = 'جارٍ تحديد الموقع…';
      locate().then(function () { p.on = true; ctx.persist(); syncAlarms(); render(); })
        .catch(function () { err.textContent = 'تعذّر تحديد الموقع. اسمح للتطبيق بالوصول للموقع، أو اختر مدينتك من القائمة.'; });
    };
    document.getElementById('pCity').onchange = function () {
      var c = CITIES[+this.value]; if (!c) return;
      p.lat = c[1]; p.lng = c[2]; p.place = c[0]; p.on = true; ctx.persist(); syncAlarms(); render();
    };
  }

  window.Prayer = {
    show: function (c) { ctx = c; render(); },
    init: function (c) { ctx = c; syncAlarms(); },
    sync: function (c) { ctx = c; return syncAlarms(); },
    hijri: function () { return hijri(new Date()); },
    eveningAt: function (c) { ctx = c; var p = P(); var loc = p.lat == null ? { lat: 30.0444, lng: 31.2357, method: p.method, madhab: p.madhab } : p; var now = new Date(); var t = timesFor(loc, now); var e = new Date(t.maghrib.getTime() - 15 * 60000); if (e < now) { var tm = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 12); e = new Date(timesFor(loc, tm).maghrib.getTime() - 15 * 60000); } return fmt(e) + (p.lat == null ? ' (حسب توقيت القاهرة، اختر مدينتك من «المواقيت»)' : ''); },   // عند بدء التطبيق: جدّد المواقيت للمنبّه
    next: function (c) { ctx = c; var p = P(); var n = nextPrayer(p); return n ? { name: n.name, time: fmt(n.at) } : null; },
    leave: function () { stopCompass(); clearInterval(tick); }
  };
})();
