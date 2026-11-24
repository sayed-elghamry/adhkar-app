package com.adhkar.salawat.reminder;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.adhkar.salawat.R;

/**
 * يشغّل صوت الصلاة على النبي ﷺ مباشرة (بدل صوت قناة الإشعار الذي تتجاهله بعض الهواتف
 * مثل شاومي وأوبو وريلمي). يعمل على مستوى صوت المنبّه حتى يُسمع بوضوح.
 * يحترم الوضع الصامت/الاهتزاز و«عدم الإزعاج» إلا إذا فعّل المستخدم «التشغيل حتى في الوضع الصامت».
 */
public final class SoundPlayer {
    private static MediaPlayer current;

    private SoundPlayer() {}

    public static boolean shouldPlay(Context c) {
        if (ReminderScheduler.playInSilent(c)) return true;
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am != null && am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) return false;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && nm.getCurrentInterruptionFilter() > NotificationManager.INTERRUPTION_FILTER_ALL) return false;
        return true;
    }

    /** @param force للتجربة من داخل التطبيق: يتجاهل الوضع الصامت */
    public static synchronized void play(Context ctx, boolean force, Runnable onDone) {
        final Context c = ctx.getApplicationContext();
        final Runnable done = onDone != null ? onDone : () -> {};
        if (!force && !shouldPlay(c)) { done.run(); return; }
        stop();
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm == null ? null
            : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "adhkar:salawat");
        if (wl != null) wl.acquire(20_000L);
        final Runnable finish = () -> {
            if (wl != null && wl.isHeld()) wl.release();
            done.run();
        };
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
            AssetFileDescriptor afd = c.getResources().openRawResourceFd(R.raw.salawat);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setOnCompletionListener(p -> { release(p); finish.run(); });
            mp.setOnErrorListener((p, w, e) -> { release(p); finish.run(); return true; });
            mp.prepare();
            mp.start();
            current = mp;
            // أمان: أنهِ بعد ١٨ ثانية مهما حدث
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (wl != null && wl.isHeld()) { stop(); finish.run(); }
            }, 18_000L);
        } catch (Exception e) {
            finish.run();
        }
    }

    private static synchronized void release(MediaPlayer p) {
        try { p.release(); } catch (Exception ignored) {}
        if (current == p) current = null;
    }

    public static synchronized void stop() {
        if (current != null) {
            try { current.stop(); } catch (Exception ignored) {}
            release(current);
        }
    }
}
