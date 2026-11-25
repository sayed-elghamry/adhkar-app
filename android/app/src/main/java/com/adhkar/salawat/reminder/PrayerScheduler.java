package com.adhkar.salawat.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.adhkar.salawat.MainActivity;
import com.adhkar.salawat.R;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * منبّه الصلوات: الواجهة تحسب مواقيت ~٦٠ يومًا (مكتبة adhan) وترسلها هنا،
 * وهنا نجدول أقرب موعد فقط، وعند وصوله نُظهر الإشعار ونجدول التالي.
 */
public final class PrayerScheduler {
    static final String PREFS = "prayer_alarms";
    static final String ACTION = "com.adhkar.salawat.action.PRAYER";
    static final String CHANNEL = "prayer_times_v1";
    private static final int REQ = 7101;
    private static final int NOTIF = 7102;

    private PrayerScheduler() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** times: [{t: epochMillis, name: "الفجر"}...] */
    public static void save(Context c, boolean enabled, JSONArray times) {
        prefs(c).edit().putBoolean("enabled", enabled).putString("times", times == null ? "[]" : times.toString()).apply();
        reschedule(c);
    }

    public static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", false); }

    private static JSONObject nextEntry(Context c, long after) {
        try {
            JSONArray a = new JSONArray(prefs(c).getString("times", "[]"));
            JSONObject best = null;
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                long t = o.getLong("t");
                if (t > after && (best == null || t < best.getLong("t"))) best = o;
            }
            return best;
        } catch (Exception e) { return null; }
    }

    public static int remaining(Context c) {
        try {
            JSONArray a = new JSONArray(prefs(c).getString("times", "[]"));
            long now = System.currentTimeMillis();
            int n = 0;
            for (int i = 0; i < a.length(); i++) if (a.getJSONObject(i).getLong("t") > now) n++;
            return n;
        } catch (Exception e) { return 0; }
    }

    private static PendingIntent pi(Context c) {
        Intent i = new Intent(c, Receiver.class).setAction(ACTION);
        return PendingIntent.getBroadcast(c, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static long reschedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return 0;
        PendingIntent p = pi(c);
        am.cancel(p);
        if (!enabled(c)) return 0;
        JSONObject n = nextEntry(c, System.currentTimeMillis() + 2000);
        if (n == null) return 0;
        try {
            long t = n.getLong("t");
            prefs(c).edit().putString("pending", n.toString()).apply();
            if (ReminderScheduler.canScheduleExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p);
            return t;
        } catch (Exception e) { return 0; }
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "مواقيت الصلاة", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("تنبيه عند دخول وقت كل صلاة");
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        ch.enableVibration(true);
        ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static void showAdhkar(Context c, boolean morning, Runnable onDone) { showAdhkar(c, morning, false, onDone); }

    static void showAdhkar(Context c, boolean morning, boolean force, Runnable onDone) {
        PlaybackService.start(c, morning ? ReminderScheduler.K_MORNING : ReminderScheduler.K_EVENING, morning ? 7201 : 7202,
            morning ? "أذكار الصباح ☀" : "أذكار المساء ☾",
            morning ? "أصبحنا وأصبح الملك لله، المس هنا لقراءة أذكار الصباح" : "أمسينا وأمسى الملك لله، المس هنا لقراءة أذكار المساء",
            morning ? "morning" : "evening", force);
        if (onDone != null) onDone.run();
    }

    static final String CH_GENERAL = "general_reminders_v1";

    /** تذكير عام (الضحى، الجمعة، الصيام) بصوت الإشعار الافتراضي. */
    static void showNote(Context c, int id, String title, String text, String open) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CH_GENERAL) == null) {
                NotificationChannel ch = new NotificationChannel(CH_GENERAL, "تذكيرات الضحى والجمعة والصيام", NotificationManager.IMPORTANCE_HIGH);
                ch.enableVibration(true);
                ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(ch);
            }
        }
        Intent oi = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (open != null && !open.isEmpty()) oi.putExtra(MainActivity.EXTRA_OPEN, open);
        PendingIntent content = PendingIntent.getActivity(c, 7500 + (id & 0xff), oi, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CH_GENERAL)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(content);
        try { NotificationManagerCompat.from(c).notify(id, b.build()); } catch (SecurityException ignored) {}
    }

    /** منبّه قبل الفجر: بنغمة المنبّه في الهاتف (قناة المواقيت). */
    static void showWake(Context c, String title, String text) {
        ensureChannel(c);
        PendingIntent content = PendingIntent.getActivity(c, 2, new Intent(c, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(content);
        try { NotificationManagerCompat.from(c).notify(7103, b.build()); } catch (SecurityException ignored) {}
    }

    static void show(Context c, String name) {
        if (prefs(c).getBoolean("adhan", true)) {
            PlaybackService.start(c, ReminderScheduler.K_ADHAN, NOTIF, "حان الآن موعد صلاة " + name, "حيّ على الصلاة، حيّ على الفلاح", "", false);
            return;
        }
        ensureChannel(c);
        PendingIntent content = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("حان الآن موعد صلاة " + name)
            .setContentText("حيّ على الصلاة، حيّ على الفلاح")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(content);
        try { NotificationManagerCompat.from(c).notify(NOTIF, b.build()); } catch (SecurityException ignored) {}
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (!ACTION.equals(intent.getAction())) return;
            final PendingResult pr = goAsync();
            boolean async = false;
            try {
                JSONObject p = new JSONObject(prefs(c).getString("pending", "{}"));
                long late = System.currentTimeMillis() - p.optLong("t", 0);
                reschedule(c);
                if (enabled(c) && late < 20 * 60_000L) {
                    String kind = p.optString("kind", "");
                    if ("morning".equals(kind) || "evening".equals(kind)) {
                        async = true;
                        showAdhkar(c, "morning".equals(kind), pr::finish);
                    } else if ("prefajr".equals(kind)) {
                        showWake(c, p.optString("name", ""), p.optString("text", ""));
                    } else if ("note".equals(kind)) {
                        showNote(c, 7500 + p.optInt("id", 0), p.optString("name", ""), p.optString("text", ""), p.optString("open", ""));
                    } else {
                        show(c, p.optString("name", ""));
                    }
                }
            } catch (Exception ignored) {
                reschedule(c);
            }
            if (!async) pr.finish();
        }
    }
}
