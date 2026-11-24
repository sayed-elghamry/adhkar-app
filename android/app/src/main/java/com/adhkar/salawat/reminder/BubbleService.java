package com.adhkar.salawat.reminder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.adhkar.salawat.R;

/** فقاعة عائمة على جانب الشاشة تعرض الأذكار المختارة فوق كل التطبيقات. */
public class BubbleService extends Service {
    static final String CHANNEL = "adhkar_bubble";
    static final String ACTION_STOP = "com.adhkar.salawat.action.BUBBLE_STOP";
    private WindowManager wm;
    private TextView view;
    private WindowManager.LayoutParams lp;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean expanded = true;
    private final Runnable rotate = new Runnable() {
        @Override public void run() {
            if (view != null) view.setText(AdhkarStore.next(BubbleService.this, "bubbleIdx"));
            h.postDelayed(this, AdhkarStore.interval(BubbleService.this) * 1000L);
        }
    };

    public static boolean canDraw(Context c) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
    }

    public static void apply(Context c) {
        Intent i = new Intent(c, BubbleService.class);
        if (AdhkarStore.bubble(c) && canDraw(c)) {
            try {
                if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
            } catch (Exception ignored) {}
        } else {
            c.stopService(i);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            AdhkarStore.prefs(this).edit().putBoolean("bubble", false).apply();
            stopSelf();
            return START_NOT_STICKY;
        }
        startFg();
        if (!canDraw(this)) { stopSelf(); return START_NOT_STICKY; }
        if (view == null) addBubble();
        return START_STICKY;
    }

    private void startFg() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "الأذكار العائمة", NotificationManager.IMPORTANCE_MIN);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
        PendingIntent stop = PendingIntent.getService(this, 7301,
            new Intent(this, BubbleService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("الأذكار العائمة تعمل")
            .setContentText("المس الفقاعة للذكر التالي، واسحبها لتحريكها")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .addAction(0, "إيقاف", stop)
            .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(7302, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(7302, n);
        }
    }

    private int dp(float v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }

    @SuppressLint("ClickableViewAccessibility")
    private void addBubble() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        view = new TextView(this);
        view.setTextColor(Color.WHITE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setMaxWidth(dp(220));
        view.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E61F6B4F"));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.parseColor("#D9AC4E"));
        view.setBackground(bg);
        view.setText(AdhkarStore.current(this, "bubbleIdx"));

        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                               : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = 0;
        lp.y = AdhkarStore.prefs(this).getInt("bubbleY", dp(160));

        view.setOnTouchListener(new View.OnTouchListener() {
            float sy, sx; int oy, ox; long t0; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sy = e.getRawY(); sx = e.getRawX(); oy = lp.y; ox = lp.x; t0 = System.currentTimeMillis(); moved = false; return true;
                    case MotionEvent.ACTION_MOVE:
                        float dy = e.getRawY() - sy, dx = e.getRawX() - sx;
                        if (Math.abs(dy) > dp(6) || Math.abs(dx) > dp(6)) moved = true;
                        lp.y = Math.max(0, oy + (int) dy);
                        lp.x = Math.max(0, ox - (int) dx);
                        wm.updateViewLayout(view, lp);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            long held = System.currentTimeMillis() - t0;
                            if (held > 600) toggleSize();
                            else view.setText(AdhkarStore.next(BubbleService.this, "bubbleIdx"));
                        } else {
                            AdhkarStore.prefs(BubbleService.this).edit().putInt("bubbleY", lp.y).apply();
                        }
                        return true;
                }
                return false;
            }
        });
        try { wm.addView(view, lp); } catch (Exception e) { view = null; stopSelf(); return; }
        h.postDelayed(rotate, AdhkarStore.interval(this) * 1000L);
    }

    /** ضغطة مطوّلة: تصغير الفقاعة إلى نقطة صغيرة أو إعادتها */
    private void toggleSize() {
        expanded = !expanded;
        if (expanded) {
            view.setText(AdhkarStore.current(this, "bubbleIdx"));
            view.setPadding(dp(14), dp(10), dp(14), dp(10));
        } else {
            view.setText("ﷺ");
            view.setPadding(dp(10), dp(8), dp(10), dp(8));
        }
    }

    @Override
    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (view != null && wm != null) { try { wm.removeView(view); } catch (Exception ignored) {} }
        view = null;
        super.onDestroy();
    }
}
