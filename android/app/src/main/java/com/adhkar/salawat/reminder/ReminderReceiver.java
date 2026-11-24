package com.adhkar.salawat.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** يستقبل المنبّه، يُظهر الإشعار، ثم يجدول الساعة التالية. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ReminderScheduler.ACTION_FIRE.equals(intent.getAction())) return;
        final PendingResult pr = goAsync();
        boolean shown = false;
        if (ReminderScheduler.isEnabled(context)) {
            long scheduled = ReminderScheduler.nextAt(context);
            long late = System.currentTimeMillis() - scheduled;
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            boolean inWin = ReminderScheduler.inWindow(hour,
                ReminderScheduler.fromHour(context), ReminderScheduler.toHour(context));
            // لا نُظهر تذكيرًا متأخرًا جدًا (مثلًا بعد خروج الهاتف من سبات طويل)
            if (scheduled == 0L || (late < 30 * 60_000L && (inWin || late < 10 * 60_000L))) {
                shown = true;
                ReminderScheduler.reschedule(context);
                ReminderScheduler.showNow(context, false, pr::finish);
            }
        }
        if (!shown) {
            ReminderScheduler.reschedule(context);
            pr.finish();
        }
    }
}
