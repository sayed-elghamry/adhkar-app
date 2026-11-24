package com.adhkar.salawat.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** يعيد الجدولة بعد إعادة التشغيل، تحديث التطبيق، تغيير الوقت/المنطقة الزمنية، أو منح إذن المنبّهات الدقيقة. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderScheduler.ensureChannel(context);
        ReminderScheduler.reschedule(context);
    }
}
