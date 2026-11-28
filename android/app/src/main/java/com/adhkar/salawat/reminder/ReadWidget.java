package com.adhkar.salawat.reminder;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.adhkar.salawat.MainActivity;
import com.adhkar.salawat.R;

/** ودجت «متابعة القراءة»: يفتح المصحف على آخر صفحة قرأها المستخدم. */
public class ReadWidget extends AppWidgetProvider {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("read_widget", Context.MODE_PRIVATE); }

    public static void save(Context c, String title, String sub) {
        prefs(c).edit().putString("title", title).putString("sub", sub).apply();
        refreshAll(c);
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { refreshAll(c); }

    public static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, ReadWidget.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_read);
        v.setTextViewText(R.id.rw_title, prefs(c).getString("title", "📖 المصحف الشريف"));
        v.setTextViewText(R.id.rw_sub, prefs(c).getString("sub", "اضغط لبدء القراءة"));
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, "quran");
        v.setOnClickPendingIntent(R.id.rw_root, PendingIntent.getActivity(c, 7310, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        for (int id : ids) m.updateAppWidget(id, v);
    }
}
