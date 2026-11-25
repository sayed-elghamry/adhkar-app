package com.adhkar.salawat.reminder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.adhkar.salawat.MainActivity;
import com.adhkar.salawat.R;

/**
 * يشغّل الأصوات الطويلة (الأذان، نغمتا الصباح والمساء) كخدمة أمامية حتى لا يقطعها النظام،
 * مع زر «إيقاف» في الإشعار. عند الانتهاء يبقى الإشعار ظاهرًا.
 */
public class PlaybackService extends Service {
    static final String ACTION_PLAY = "com.adhkar.salawat.action.PLAY";
    static final String ACTION_STOP = "com.adhkar.salawat.action.STOP_PLAY";

    private int notifId = 7401;
    private Notification last;

    /** يبدأ التشغيل؛ إن منع النظام الخدمة الأمامية يُظهر الإشعار ويشغّل الصوت مباشرة. */
    public static void start(Context c, int kind, int notifId, String title, String text, String open, boolean force) {
        Intent i = new Intent(c, PlaybackService.class).setAction(ACTION_PLAY)
            .putExtra("kind", kind).putExtra("id", notifId).putExtra("title", title)
            .putExtra("text", text).putExtra("open", open).putExtra("force", force);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Exception e) {
            try { NotificationManagerCompat.from(c).notify(notifId, kind == ReminderScheduler.K_ADHAN ? buildAdhan(c, title, text, true) : build(c, title, text, open, false)); } catch (SecurityException ignored) {}
            SoundPlayer.play(c, kind, force, null);
        }
    }

    static Notification build(Context c, String title, String text, String open, boolean playing) {
        ReminderScheduler.ensureChannel(c);
        Intent oi = new Intent(c, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (open != null && !open.isEmpty()) oi.putExtra(MainActivity.EXTRA_OPEN, open);
        PendingIntent content = PendingIntent.getActivity(c, 7402 + (open == null ? 0 : open.hashCode() & 0xff), oi,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(content);
        if (playing) {
            PendingIntent stop = PendingIntent.getService(c, 7403,
                new Intent(c, PlaybackService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(0, "إيقاف الصوت", stop).setDeleteIntent(stop);
        }
        return b.build();
    }

    /** إشعار الأذان: صورة مسجد، وشاشة كاملة تظهر حتى والهاتف مقفول، وزر جرس لإيقاف الصوت. */
    static Notification buildAdhan(Context c, String title, String text, boolean playing) {
        ReminderScheduler.ensureChannel(c);
        android.graphics.Bitmap pic = null;
        try {
            android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(c, R.drawable.mosque);
            if (d != null) {
                pic = android.graphics.Bitmap.createBitmap(720, 440, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas cv = new android.graphics.Canvas(pic);
                d.setBounds(0, 0, 720, 440); d.draw(cv);
            }
        } catch (Exception ignored) {}
        Intent ai = new Intent(c, AdhanActivity.class).putExtra("title", title)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent screen = PendingIntent.getActivity(c, 7404, ai, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("🕌 " + title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(screen);
        if (pic != null) b.setLargeIcon(pic).setStyle(new NotificationCompat.BigPictureStyle().bigPicture(pic).bigLargeIcon((android.graphics.Bitmap) null).setSummaryText(text));
        if (playing) {
            b.setFullScreenIntent(screen, true);
            PendingIntent stop = PendingIntent.getService(c, 7403,
                new Intent(c, PlaybackService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(0, "🔕 إيقاف صوت الأذان", stop).setDeleteIntent(stop);
        }
        return b.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            SoundPlayer.stop();
            finish();
            return START_NOT_STICKY;
        }
        int kind = intent.getIntExtra("kind", ReminderScheduler.K_SALAWAT);
        notifId = intent.getIntExtra("id", 7401);
        String title = intent.getStringExtra("title"), text = intent.getStringExtra("text"), open = intent.getStringExtra("open");
        boolean force = intent.getBooleanExtra("force", false);
        Notification n = kind == ReminderScheduler.K_ADHAN ? buildAdhan(this, title, text, true) : build(this, title, text, open, true);
        last = kind == ReminderScheduler.K_ADHAN ? buildAdhan(this, title, text, false) : build(this, title, text, open, false);
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(notifId, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(notifId, n);
        } catch (Exception e) {
            try { NotificationManagerCompat.from(this).notify(notifId, last); } catch (SecurityException ignored) {}
        }
        SoundPlayer.play(this, kind, force, this::finish);
        return START_NOT_STICKY;
    }

    private void finish() {
        try {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH); else stopForeground(false);
            // أبقِ الإشعار ظاهرًا لكن بدون زر الإيقاف
            if (last != null) NotificationManagerCompat.from(this).notify(notifId, last);
        } catch (Exception ignored) {}
        stopSelf();
    }

    @Override
    public void onDestroy() { SoundPlayer.stop(); super.onDestroy(); }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
