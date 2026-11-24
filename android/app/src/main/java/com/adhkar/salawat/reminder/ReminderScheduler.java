package com.adhkar.salawat.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
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
    public static final int SOUND_VERSION = 2;
    public static final String CHANNEL_ID = "salawat_reminder_v" + SOUND_VERSION;
    public static final String ACTION_FIRE = "com.adhkar.salawat.action.HOURLY_REMINDER";

    static final String PREFS = "hourly_reminder";
    static final String K_ENABLED = "enabled";
    static final String K_FROM = "from";
    static final String K_TO = "to";
    static final String K_NEXT = "nextAt";
    static final String K_LAST = "lastFiredAt";

    private static final int REQ_ALARM = 7001;
    private static final int NOTIF_ID = 7002;

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
    public static long nextAt(Context c) { return prefs(c).getLong(K_NEXT, 0L); }

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
        long at = computeNext(System.currentTimeMillis(), fromHour(c), toHour(c));
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
        AudioAttributes aa = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        ch.setSound(soundUri(c), aa);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 250, 150, 250});
        ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static Uri soundUri(Context c) {
        return Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.salawat);
    }

    public static void showNow(Context c) {
        ensureChannel(c);
        String text = SALAWAT[new Random().nextInt(SALAWAT.length)];
        Intent open = new Intent(c, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(c, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("صلِّ على النبي ﷺ")
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri(c))              // لأندرويد 7 وما قبله
            .setVibrate(new long[]{0, 250, 150, 250})
            .setAutoCancel(true)
            .setContentIntent(content);
        try {
            NotificationManagerCompat.from(c).notify(NOTIF_ID, b.build());
        } catch (SecurityException ignored) {
            // إذن الإشعارات غير ممنوح
        }
        prefs(c).edit().putLong(K_LAST, System.currentTimeMillis()).apply();
    }
}
