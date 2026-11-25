package com.adhkar.salawat.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.SystemClock;
import android.widget.RemoteViews;

import com.adhkar.salawat.MainActivity;
import com.adhkar.salawat.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/** ودجت مواقيت الصلاة: التاريخ الهجري، الصلاة القادمة مع عدّ تنازلي وشريط، ومواقيت اليوم الخمس. */
public class PrayerWidget extends AppWidgetProvider {
    static final String ACTION_BELL = "com.adhkar.salawat.action.PRAYER_WIDGET_BELL";
    static final String ACTION_TICK = "com.adhkar.salawat.action.PRAYER_WIDGET_TICK";
    static final String[] NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
    static final int[] COL = {R.id.pw_c0, R.id.pw_c1, R.id.pw_c2, R.id.pw_c3, R.id.pw_c4};
    static final int[] TIM = {R.id.pw_t0, R.id.pw_t1, R.id.pw_t2, R.id.pw_t3, R.id.pw_t4};
    static final int[] NAM = {R.id.pw_n0, R.id.pw_n1, R.id.pw_n2, R.id.pw_n3, R.id.pw_n4};

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("prayer_widget", Context.MODE_PRIVATE); }

    public static void save(Context c, String place, JSONArray days) {
        prefs(c).edit().putString("place", place == null ? "" : place).putString("days", days == null ? "[]" : days.toString()).apply();
        refreshAll(c);
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { refreshAll(c); }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (ACTION_TICK.equals(intent.getAction())) { refreshAll(c); return; }
        if (ACTION_BELL.equals(intent.getAction())) {
            SharedPreferences p = PrayerScheduler.prefs(c);
            boolean on = !p.getBoolean("adhan", true);
            p.edit().putBoolean("adhan", on).putBoolean("adhanChanged", true).apply();
            if (!on) SoundPlayer.stop();
            android.widget.Toast.makeText(c, on ? "🔔 صوت الأذان يعمل" : "🔕 تم إيقاف صوت الأذان (يظهر إشعار فقط)", android.widget.Toast.LENGTH_SHORT).show();
            refreshAll(c); return;
        }
        super.onReceive(c, intent);
    }

    @Override
    public void onDisabled(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(tickIntent(c));
    }

    static PendingIntent tickIntent(Context c) {
        return PendingIntent.getBroadcast(c, 7301, new Intent(c, PrayerWidget.class).setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static String key(Calendar k) {
        return String.format(Locale.US, "%04d-%02d-%02d", k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
    }

    static JSONObject day(JSONArray days, String k) {
        for (int i = 0; i < days.length(); i++) { JSONObject o = days.optJSONObject(i); if (o != null && k.equals(o.optString("d"))) return o; }
        return null;
    }

    static String hm(long t) {
        Calendar k = Calendar.getInstance(); k.setTimeInMillis(t);
        int h = k.get(Calendar.HOUR); if (h == 0) h = 12;
        return String.format(Locale.US, "%02d:%02d", h, k.get(Calendar.MINUTE));
    }

    public static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, PrayerWidget.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_prayer);
        long now = System.currentTimeMillis();
        long nextUpdate = now + 10 * 60000L;
        try {
            JSONArray days = new JSONArray(prefs(c).getString("days", "[]"));
            Calendar today = Calendar.getInstance();
            Calendar yest = (Calendar) today.clone(); yest.add(Calendar.DAY_OF_MONTH, -1);
            Calendar tom = (Calendar) today.clone(); tom.add(Calendar.DAY_OF_MONTH, 1);
            JSONObject d0 = day(days, key(today));
            if (d0 == null) throw new Exception("no data");
            JSONObject dy = day(days, key(yest)), dt = day(days, key(tom));
            JSONArray t = d0.getJSONArray("t");
            v.setTextViewText(R.id.pw_place, prefs(c).getString("place", ""));
            v.setTextViewText(R.id.pw_hijri, d0.optString("h", ""));
            int nx = -1;
            for (int i = 0; i < 5; i++) if (t.getLong(i) > now) { nx = i; break; }
            long next, prev; String name; JSONArray shown = t;
            if (nx >= 0) {
                next = t.getLong(nx); name = NAMES[nx];
                prev = nx > 0 ? t.getLong(nx - 1) : (dy != null ? dy.getJSONArray("t").getLong(4) : next - 9 * 3600000L);
            } else {
                // بعد العشاء: القادمة فجر الغد
                prev = t.getLong(4); name = NAMES[0];
                if (dt != null) { shown = dt.getJSONArray("t"); next = shown.getLong(0); v.setTextViewText(R.id.pw_hijri, dt.optString("h", "")); }
                else next = prev + 9 * 3600000L;
                nx = 0;
            }
            for (int i = 0; i < 5; i++) {
                v.setTextViewText(TIM[i], hm(shown.getLong(i)));
                boolean on = i == nx;
                v.setInt(TIM[i], "setBackgroundResource", on ? R.drawable.pw_hl : 0);
                v.setTextColor(TIM[i], on ? Color.WHITE : Color.WHITE);
                v.setTextColor(NAM[i], on ? Color.parseColor("#E9B23C") : Color.parseColor("#E6E6E6"));
            }
            v.setTextViewText(R.id.pw_next, name);
            long left = next - now;
            v.setChronometer(R.id.pw_count, SystemClock.elapsedRealtime() + left, "بعد %s", true);
            v.setChronometerCountDown(R.id.pw_count, true);
            int pct = (int) Math.max(0, Math.min(1000, (now - prev) * 1000 / Math.max(1, next - prev)));
            v.setProgressBar(R.id.pw_bar, 1000, pct, false);
            nextUpdate = Math.min(nextUpdate, next + 1000);
        } catch (Exception e) {
            v.setTextViewText(R.id.pw_next, "افتح التطبيق مرة لتحميل المواقيت");
            v.setTextViewText(R.id.pw_count, "");
        }
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(MainActivity.EXTRA_OPEN, "times");
        v.setOnClickPendingIntent(R.id.pw_root, PendingIntent.getActivity(c, 7302, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        boolean adhanOn = PrayerScheduler.prefs(c).getBoolean("adhan", true);
        v.setTextViewText(R.id.pw_bell, adhanOn ? "🔔" : "🔕");
        v.setOnClickPendingIntent(R.id.pw_bell, PendingIntent.getBroadcast(c, 7303, new Intent(c, PrayerWidget.class).setAction(ACTION_BELL),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        for (int id : ids) m.updateAppWidget(id, v);
        // تحديث خفيف (بدون إيقاظ الهاتف) كل ١٠ دقائق وعند دخول وقت الصلاة
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.set(AlarmManager.RTC, nextUpdate, tickIntent(c));
    }
}
