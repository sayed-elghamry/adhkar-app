package com.adhkar.salawat.reminder;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.adhkar.salawat.MainActivity;
import com.adhkar.salawat.R;

/** ودجت الشاشة الرئيسية: يعرض ذكرًا من اختيار المستخدم، يتغير كل ٣٠ دقيقة أو بلمسة. */
public class AdhkarWidget extends AppWidgetProvider {
    static final String ACTION_NEXT = "com.adhkar.salawat.action.WIDGET_NEXT";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        String text = AdhkarStore.next(c, "widgetIdx");
        for (int id : ids) m.updateAppWidget(id, views(c, text));
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (ACTION_NEXT.equals(intent.getAction())) {
            refreshAll(c, true);
            return;
        }
        super.onReceive(c, intent);
    }

    public static void refreshAll(Context c, boolean advance) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, AdhkarWidget.class));
        if (ids == null || ids.length == 0) return;
        String text = advance ? AdhkarStore.next(c, "widgetIdx") : AdhkarStore.current(c, "widgetIdx");
        for (int id : ids) m.updateAppWidget(id, views(c, text));
    }

    static RemoteViews views(Context c, String text) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_adhkar);
        v.setTextViewText(R.id.widget_text, text);
        PendingIntent next = PendingIntent.getBroadcast(c, 7201,
            new Intent(c, AdhkarWidget.class).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_text, next);
        PendingIntent open = PendingIntent.getActivity(c, 7202,
            new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_open, open);
        return v;
    }
}
