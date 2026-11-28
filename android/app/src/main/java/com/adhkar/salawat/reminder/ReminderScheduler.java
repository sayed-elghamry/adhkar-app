package com.adhkar.salawat.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.adhkar.salawat.MainActivity;
import com.adhkar.salawat.R;

import java.util.Calendar;
import java.util.Random;

/**
 * يجدول تذكيرًا واحدًا في كل مرة (الساعة الكاملة التالية داخل النافذة الزمنية)،
 * وعند وصوله يُظهر الإشعار ثم يجدول التالي. هذا أدق من التكرار التلقائي
 * ويعمل في وضع Doze عبر setExactAndAllowWhileIdle.
 */
public final class ReminderScheduler {

    /** غيّر هذا الرقم عند استبدال ملف الصوت res/raw/salawat.mp3، لأن أندرويد يثبّت صوت القناة عند إنشائها. */
    public static final int SOUND_VERSION = 3;
    public static final String CHANNEL_ID = "salawat_reminder_v" + SOUND_VERSION;
    public static final String ACTION_FIRE = "com.adhkar.salawat.action.HOURLY_REMINDER";

    static final String PREFS = "hourly_reminder";
    static final String K_ENABLED = "enabled";
    static final String K_FROM = "from";
    static final String K_TO = "to";
    static final String K_NEXT = "nextAt";
    static final String K_LAST = "lastFiredAt";
    static final String K_SILENT = "playInSilent";
    static final String K_HALF = "halfHourIstighfar";

    private static final int REQ_ALARM = 7001;
    private static final int NOTIF_ID = 7002;

    private static final String[] ISTIGHFAR = {
        "أستغفر الله العظيم وأتوب إليه",
        "أستغفر الله",
        "أستغفر الله الذي لا إله إلا هو الحي القيوم وأتوب إليه",
        "رب اغفر لي وتب عليّ إنك أنت التواب الرحيم"
    };

    private static final String[] HAWQALA = {
        "لا حول ولا قوة إلا بالله",
        "لا حول ولا قوة إلا بالله العلي العظيم"
    };

    /** نوع التذكير: صلاة على النبي (رأس الساعة)، استغفار (:٢٠ و:٢١)، حوقلة (:٤٠ و:٤١). */
    public static final int K_SALAWAT = 0, K_ISTIGHFAR = 1, K_HAWQALA = 2, K_MORNING = 3, K_EVENING = 4, K_ADHAN = 5;
    /** دقائق التذكير داخل الساعة؛ كل ذكر من الاثنين يتكرر مرتين خلال دقيقتين. */
    private static final int[] THIRD_MINUTES = {0, 1, 20, 21, 40, 41};
    private static final int[] HOUR_MINUTES = {0, 1};
    /** وضع «بالتتابع كل ١٠ دقائق»: صلاة، استغفار، حوقلة، ثم من جديد (كل واحد مرتين). */
    private static final int[] TEN_MINUTES = {0, 1, 10, 11, 20, 21, 30, 31, 40, 41, 50, 51};
    static final String K_STEP = "step";
    public static int step(Context c) { return prefs(c).getInt(K_STEP, 20); }
    /** المدة بين كل تنبيه: ١٠ أو ٢٠ أو ٣٠ أو ٤٠ أو ٥٠ أو ٦٠ دقيقة. */
    static int clampStep(int v) { return v == 10 || v == 30 || v == 40 || v == 50 || v == 60 ? v : 20; }
    public static void setStep(Context c, int v) { prefs(c).edit().putInt(K_STEP, clampStep(v)).apply(); }

    static int dayMinute(long at) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(at);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    /** بالتتابع كل «مدة»: صلاة على النبي ← استغفار ← حوقلة، وإن أُطفئ التنويع فالصلاة على النبي فقط. */
    public static int kindAt(Context c, long at) {
        if (!halfHour(c)) return K_SALAWAT;
        int k = (dayMinute(at) / clampStep(step(c))) % 3;
        return k == 1 ? K_ISTIGHFAR : k == 2 ? K_HAWQALA : K_SALAWAT;
    }

    public static int kindAt(long at) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(at);
        int m = cal.get(Calendar.MINUTE);
        if (m >= 15 && m < 35) return K_ISTIGHFAR;
        if (m >= 35 && m < 55) return K_HAWQALA;
        return K_SALAWAT;
    }

    private static final String[] SALAWAT = {
        "اللهم صلِّ وسلِّم على نبينا محمد",
        "اللهم صلِّ على محمد صلى الله عليه وسلم",
        "اللهم صلِّ على محمد وعلى آل محمد كما صليت على إبراهيم وعلى آل إبراهيم إنك حميد مجيد",
        "اللهم صلِّ وسلِّم وبارك على سيدنا محمد وعلى آله وصحبه أجمعين",
        "صلى الله عليه وعلى آله وسلم تسليمًا كثيرًا"
    };

    private ReminderScheduler() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c) { return prefs(c).getBoolean(K_ENABLED, true); }
    public static int fromHour(Context c) { return prefs(c).getInt(K_FROM, 5); }
    public static int toHour(Context c) { return prefs(c).getInt(K_TO, 23); }
    public static boolean playInSilent(Context c) { return prefs(c).getBoolean(K_SILENT, false); }
    public static void setPlayInSilent(Context c, boolean v) { prefs(c).edit().putBoolean(K_SILENT, v).apply(); }
    public static long nextAt(Context c) { return prefs(c).getLong(K_NEXT, 0L); }
    /** وقت التذكير: true = خفض صوت الفيديو/الموسيقى فقط، false = إيقافه مؤقتًا. */
    public static boolean duck(Context c) { return prefs(c).getBoolean("duck", true); }
    public static void setDuck(Context c, boolean v) { prefs(c).edit().putBoolean("duck", v).apply(); }
    public static boolean halfHour(Context c) { return prefs(c).getBoolean(K_HALF, true); }
    public static void setHalfHour(Context c, boolean v) { prefs(c).edit().putBoolean(K_HALF, v).apply(); }


    public static void saveSettings(Context c, boolean enabled, int from, int to) {
        prefs(c).edit()
            .putBoolean(K_ENABLED, enabled)
            .putInt(K_FROM, clampHour(from))
            .putInt(K_TO, clampHour(to))
            .apply();
    }

    private static int clampHour(int h) { return Math.max(0, Math.min(23, h)); }

    static boolean inWindow(int hour, int from, int to) {
        return from <= to ? (hour >= from && hour <= to) : (hour >= from || hour <= to);
    }

    /** الموعد التالي: الساعة الكاملة (صلاة على النبي) أو نصفها (استغفار) داخل النافذة. */
    static long computeNext(long now, int from, int to, boolean half) { return computeNext(now, from, to, half, 20); }

    static long computeNext(long now, int from, int to, boolean half, int step) {
        // كل ذكر يتكرر مرتين خلال دقيقتين، كل «step» دقيقة من بداية اليوم
        step = clampStep(step);
        Calendar m0 = Calendar.getInstance();
        m0.setTimeInMillis(now + 5_001);
        if (m0.get(Calendar.SECOND) > 0 || m0.get(Calendar.MILLISECOND) > 0) { m0.set(Calendar.SECOND, 0); m0.set(Calendar.MILLISECOND, 0); m0.add(Calendar.MINUTE, 1); }
        for (int i = 0; i < 49 * 60; i++) {
            int h = m0.get(Calendar.HOUR_OF_DAY), mi = m0.get(Calendar.MINUTE);
            int dm = h * 60 + mi;
            // في ساعة آخر تذكير: أول الساعة فقط
            if (dm % step < 2 && inWindow(h, from, to) && (h != to || mi < 2)) return m0.getTimeInMillis();
            m0.add(Calendar.MINUTE, 1);
        }
        return now + 1_200_000L;
    }

    static long computeNextOld(long now, int from, int to, boolean half, int step) {
        Calendar base = Calendar.getInstance();
        base.setTimeInMillis(now);
        base.set(Calendar.MINUTE, 0);
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);
        for (int hr = 0; hr < 49; hr++) {
            int h = base.get(Calendar.HOUR_OF_DAY);
            for (int m : (!half ? HOUR_MINUTES : step == 10 ? TEN_MINUTES : THIRD_MINUTES)) {
                // أذكار الثلث داخل النافذة فقط إذا لم تكن ساعة آخر تذكير
                boolean ok = m < 5 ? inWindow(h, from, to) : (inWindow(h, from, to) && h != to);
                long t = base.getTimeInMillis() + m * 60_000L;
                if (ok && t > now + 5_000) return t;
            }
            base.add(Calendar.HOUR_OF_DAY, 1);
        }
        return now + 1_200_000L;
    }

    /** أول ساعة كاملة بعد "الآن" تقع داخل النافذة. */
    static long computeNext(long now, int from, int to) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < 48; i++) {
            cal.add(Calendar.HOUR_OF_DAY, 1);
            if (cal.getTimeInMillis() > now + 5_000 && inWindow(cal.get(Calendar.HOUR_OF_DAY), from, to)) {
                return cal.getTimeInMillis();
            }
        }
        return now + 3_600_000L;
    }

    public static boolean canScheduleExact(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_FIRE);
        return PendingIntent.getBroadcast(c, REQ_ALARM, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** يلغي أي تذكير سابق ويجدول التالي حسب الإعدادات. */
    public static long reschedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(c);
        if (am == null) return 0L;
        am.cancel(pi);
        if (!isEnabled(c)) {
            prefs(c).edit().putLong(K_NEXT, 0L).apply();
            return 0L;
        }
        long at = computeNext(System.currentTimeMillis(), fromHour(c), toHour(c), halfHour(c), step(c));
        if (canScheduleExact(c)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            // بدون إذن "المنبّهات والتذكيرات" قد يتأخر التذكير بضع دقائق.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        prefs(c).edit().putLong(K_NEXT, at).apply();
        return at;
    }

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        // احذف القنوات القديمة (بأصوات سابقة)
        for (NotificationChannel ch : nm.getNotificationChannels()) {
            if (ch.getId().startsWith("salawat_reminder_v") && !ch.getId().equals(CHANNEL_ID)) {
                nm.deleteNotificationChannel(ch.getId());
            }
        }
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
            "التذكير بالصلاة على النبي ﷺ", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("تذكير كل ساعة بصوت الصلاة على النبي ﷺ");
        // القناة بلا صوت: الصوت يشغّله SoundPlayer مباشرة حتى لا تكتمه إعدادات الهاتف
        ch.setSound(null, null);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 250, 150, 250});
        ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static Uri soundUri(Context c) {
        return Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.salawat);
    }

    public static void showNow(Context c) { showNow(c, false, null); }

    public static void showNow(Context c, boolean forceSound, Runnable onDone) { showNow(c, K_SALAWAT, forceSound, onDone); }

    public static void showNow(Context c, int kind, boolean forceSound, Runnable onDone) {
        ensureChannel(c);
        String[] list = kind == K_ISTIGHFAR ? ISTIGHFAR : kind == K_HAWQALA ? HAWQALA : SALAWAT;
        String text = list[new Random().nextInt(list.length)];
        Intent open = new Intent(c, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(c, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(kind == K_ISTIGHFAR ? "أستغفر الله" : kind == K_HAWQALA ? "لا حول ولا قوة إلا بالله" : "صلِّ على النبي ﷺ")
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(false)
            .setVibrate(new long[]{0, 250, 150, 250})
            .setAutoCancel(true)
            .setContentIntent(content);
        try {
            NotificationManagerCompat.from(c).notify(NOTIF_ID, b.build());
        } catch (SecurityException ignored) {
            // إذن الإشعارات غير ممنوح
        }
        prefs(c).edit().putLong(K_LAST, System.currentTimeMillis()).apply();
        SoundPlayer.play(c, kind, forceSound, onDone);
    }
}
