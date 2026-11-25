package com.adhkar.salawat;

import android.content.Intent;
import android.os.Bundle;

import com.adhkar.salawat.reminder.ReminderPlugin;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    public static final String EXTRA_OPEN = "openSection";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ReminderPlugin.class);
        super.onCreate(savedInstanceState);
        remember(getIntent());
        // زر الرجوع: نسأل الواجهة أولاً (المصحف ← الفهرس ← الرئيسية)، وفي الرئيسية نخفي التطبيق بدل إغلاقه
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                android.webkit.WebView wv = getBridge() == null ? null : getBridge().getWebView();
                if (wv == null) { moveTaskToBack(true); return; }
                wv.evaluateJavascript("(window.__onBack&&window.__onBack())?'1':'0'", v -> {
                    if (v == null || !v.contains("1")) moveTaskToBack(true);
                });
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        remember(intent);
    }

    /** يحفظ القسم المطلوب فتحه (من إشعار أذكار الصباح/المساء) لتقرأه الواجهة. */
    private void remember(Intent i) {
        if (i == null) return;
        String s = i.getStringExtra(EXTRA_OPEN);
        if (s == null) return;
        i.removeExtra(EXTRA_OPEN);
        getSharedPreferences("nav", MODE_PRIVATE).edit().putString("open", s).apply();
    }
}
