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
    private static AudioManager focusAm;
    private static Object focusReq;   // AudioFocusRequest (API 26+)

    /** يطلب من التطبيقات الأخرى (الموسيقى، الفيديو) أن تتوقف مؤقتًا أثناء الذكر أو الأذان، ثم تكمل بعده. */
    private static void requestFocus(Context c, int kind) {
        abandonFocus();
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        focusAm = am;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                // الأذان: إيقاف مؤقت للفيديو. الأذكار القصيرة: خفض صوته فقط (إلا لو اختار المستخدم الإيقاف)
                boolean duck = kind != ReminderScheduler.K_ADHAN && ReminderScheduler.duck(c);
                android.media.AudioFocusRequest r = new android.media.AudioFocusRequest.Builder(duck ? AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK : AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(duck ? AudioAttributes.USAGE_ASSISTANCE_SONIFICATION : AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(fc -> {})
                    .build();
                focusReq = r;
                am.requestAudioFocus(r);
            } else {
                am.requestAudioFocus(null, AudioManager.STREAM_ALARM, kind != ReminderScheduler.K_ADHAN && ReminderScheduler.duck(c) ? AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK : AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
        } catch (Exception ignored) {}
    }

    private static void abandonFocus() {
        if (focusAm == null) return;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26 && focusReq != null) focusAm.abandonAudioFocusRequest((android.media.AudioFocusRequest) focusReq);
            else focusAm.abandonAudioFocus(null);
        } catch (Exception ignored) {}
        focusAm = null; focusReq = null;
    }

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
    public static void play(Context ctx, boolean force, Runnable onDone) { play(ctx, ReminderScheduler.K_SALAWAT, force, onDone); }

    public static synchronized void play(Context ctx, int kind, boolean force, Runnable onDone) {
        final Context c = ctx.getApplicationContext();
        final Runnable done = onDone != null ? onDone : () -> {};
        if (!force && !shouldPlay(c)) { done.run(); return; }
        // اختيار «تنبيه بدون صوت» أثناء تشغيل فيديو/موسيقى (الأذان مستثنى)
        if (!force && kind != ReminderScheduler.K_ADHAN && ReminderScheduler.mediaMode(c) == 2) {
            AudioManager am0 = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            if (am0 != null && am0.isMusicActive()) { done.run(); return; }
        }
        stop();
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm == null ? null
            : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "adhkar:salawat");
        final long maxMs = kind == ReminderScheduler.K_ADHAN ? 240_000L : 45_000L;
        if (wl != null) wl.acquire(maxMs + 5_000L);
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
            int res = kind == ReminderScheduler.K_ISTIGHFAR ? R.raw.istighfar
                : kind == ReminderScheduler.K_HAWQALA ? R.raw.hawqala
                : kind == ReminderScheduler.K_MORNING ? R.raw.morning
                : kind == ReminderScheduler.K_EVENING ? R.raw.evening
                : kind == ReminderScheduler.K_ADHAN ? R.raw.adhan : R.raw.salawat;
            AssetFileDescriptor afd = c.getResources().openRawResourceFd(res);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setOnCompletionListener(p -> { release(p); finish.run(); });
            mp.setOnErrorListener((p, w, e) -> { release(p); finish.run(); return true; });
            mp.prepare();
            requestFocus(c, kind);
            mp.start();
            current = mp;
            // أمان: أنهِ بعد ٤٠ ثانية مهما حدث (نغمتا الصباح والمساء ~٣٠ ثانية)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (wl != null && wl.isHeld()) { stop(); finish.run(); }
            }, maxMs);
        } catch (Exception e) {
            finish.run();
        }
    }

    private static synchronized void release(MediaPlayer p) {
        try { p.release(); } catch (Exception ignored) {}
        if (current == p) { current = null; abandonFocus(); }
    }

    public static synchronized void stop() {
        if (current != null) {
            try { current.stop(); } catch (Exception ignored) {}
            release(current);
        }
    }
}
