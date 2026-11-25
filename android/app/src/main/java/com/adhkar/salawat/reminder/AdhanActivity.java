package com.adhkar.salawat.reminder;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adhkar.salawat.MainActivity;
import com.adhkar.salawat.R;

/** شاشة الأذان: صورة مسجد، اسم الصلاة، وجرس لإيقاف الصوت. */
public class AdhanActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String title = getIntent().getStringExtra("title");
        if (title == null) title = "حان الآن موعد الصلاة";
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#14281F"));
        root.setPadding((int) (20 * dp), (int) (30 * dp), (int) (20 * dp), (int) (30 * dp));

        ImageView img = new ImageView(this);
        img.setImageResource(R.drawable.mosque);
        img.setAdjustViewBounds(true);
        root.addView(img, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(26);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (24 * dp), 0, (int) (6 * dp));
        root.addView(t);

        TextView s = new TextView(this);
        s.setText("حيّ على الصلاة، حيّ على الفلاح");
        s.setTextColor(Color.parseColor("#E9B23C"));
        s.setTextSize(18);
        s.setGravity(Gravity.CENTER);
        root.addView(s);

        final TextView bell = new TextView(this);
        bell.setText("🔔");
        bell.setTextSize(40);
        bell.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#2A4A3A"));
        circle.setStroke((int) (2 * dp), Color.parseColor("#E9B23C"));
        bell.setBackground(circle);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams((int) (96 * dp), (int) (96 * dp));
        bp.topMargin = (int) (36 * dp);
        root.addView(bell, bp);

        final TextView hint = new TextView(this);
        hint.setText("اضغط الجرس لإيقاف صوت الأذان");
        hint.setTextColor(Color.parseColor("#CFCFCF"));
        hint.setTextSize(14);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, (int) (10 * dp), 0, 0);
        root.addView(hint);

        TextView close = new TextView(this);
        close.setText("إغلاق");
        close.setTextColor(Color.WHITE);
        close.setTextSize(16);
        close.setGravity(Gravity.CENTER);
        close.setPadding((int) (30 * dp), (int) (10 * dp), (int) (30 * dp), (int) (10 * dp));
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(24 * dp);
        pill.setStroke((int) (1 * dp), Color.parseColor("#88FFFFFF"));
        close.setBackground(pill);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.topMargin = (int) (28 * dp);
        root.addView(close, cp);

        bell.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                stopSound();
                bell.setText("🔕");
                hint.setText("تم إيقاف صوت الأذان");
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                startActivity(new Intent(AdhanActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish(); return true;
            }
        });
        setContentView(root);
    }

    private void stopSound() {
        try { startService(new Intent(this, PlaybackService.class).setAction(PlaybackService.ACTION_STOP)); }
        catch (Exception e) { SoundPlayer.stop(); }
    }
}
