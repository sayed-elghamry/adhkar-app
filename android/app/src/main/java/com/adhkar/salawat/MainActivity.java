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
