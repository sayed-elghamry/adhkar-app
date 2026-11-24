package com.adhkar.salawat.reminder;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "Reminder",
    permissions = { @Permission(strings = { Manifest.permission.POST_NOTIFICATIONS }, alias = "notifications") }
)
public class ReminderPlugin extends Plugin {

    @Override
    public void load() {
        ReminderScheduler.ensureChannel(getContext());
        ReminderScheduler.reschedule(getContext());
    }

    @PluginMethod
    public void configure(PluginCall call) {
        Context c = getContext();
        boolean enabled = call.getBoolean("enabled", ReminderScheduler.isEnabled(c));
        int from = call.getInt("from", ReminderScheduler.fromHour(c));
        int to = call.getInt("to", ReminderScheduler.toHour(c));
        ReminderScheduler.saveSettings(c, enabled, from, to);
        if (call.hasOption("playInSilent")) ReminderScheduler.setPlayInSilent(c, call.getBoolean("playInSilent", false));
        ReminderScheduler.reschedule(c);
        call.resolve(status());
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        ReminderScheduler.reschedule(getContext()); // يعيد الحساب (مثلًا بعد منح إذن المنبّهات)
        call.resolve(status());
    }

    @PluginMethod
    public void test(PluginCall call) {
        ReminderScheduler.showNow(getContext(), true, null);
        call.resolve(status());
    }

    @PluginMethod
    public void requestNotifications(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notifications") != PermissionState.GRANTED) {
            requestPermissionForAlias("notifications", call, "notifPermCallback");
        } else if (!NotificationManagerCompat.from(getContext()).areNotificationsEnabled()) {
            openNotificationSettingsInternal();
            call.resolve(status());
        } else {
            call.resolve(status());
        }
    }

    @PermissionCallback
    private void notifPermCallback(PluginCall call) {
        if (getPermissionState("notifications") != PermissionState.GRANTED) {
            // رُفض الطلب نهائيًا: افتح صفحة إشعارات التطبيق
            openNotificationSettingsInternal();
        }
        call.resolve(status());
    }

    @PluginMethod
    public void openExactAlarmSettings(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getContext().getPackageName()));
            if (!start(i)) start(appDetails());
        }
        call.resolve(status());
    }

    @PluginMethod
    public void requestIgnoreBattery(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:" + getContext().getPackageName()));
        if (!start(i) && !start(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) {
            start(appDetails());
        }
        call.resolve(status());
    }

    @PluginMethod
    public void openNotificationSettings(PluginCall call) {
        openNotificationSettingsInternal();
        call.resolve(status());
    }

    @PluginMethod
    public void openAppSettings(PluginCall call) {
        start(appDetails());
        call.resolve(status());
    }

    /** يفتح شاشة "التشغيل التلقائي" أو إدارة البطارية الخاصة بالشركة المصنّعة إن وُجدت. */
    @PluginMethod
    public void openAutostart(PluginCall call) {
        String[][] candidates = {
            {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
            {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
            {"com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"},
            {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
            {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
            {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
            {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
            {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"},
            {"com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"},
            {"com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"},
            {"com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"},
            {"com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"},
            {"com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"}
        };
        boolean opened = false;
        for (String[] cn : candidates) {
            Intent i = new Intent().setComponent(new ComponentName(cn[0], cn[1]));
            if (start(i)) { opened = true; break; }
        }
        if (!opened) start(appDetails());
        JSObject r = status();
        r.put("autostartScreenFound", opened);
        call.resolve(r);
    }

    @PluginMethod
    public void setPrayerAlarms(PluginCall call) {
        boolean enabled = call.getBoolean("enabled", false);
        org.json.JSONArray times = call.getArray("times", new com.getcapacitor.JSArray());
        PrayerScheduler.save(getContext(), enabled, times);
        JSObject r = new JSObject();
        r.put("remaining", PrayerScheduler.remaining(getContext()));
        call.resolve(r);
    }

    @PluginMethod
    public void setAdhkarDisplay(PluginCall call) {
        Context c = getContext();
        org.json.JSONArray items = call.getArray("items", new com.getcapacitor.JSArray());
        boolean bubble = call.getBoolean("bubble", false);
        int interval = call.getInt("interval", 60);
        AdhkarStore.save(c, items, bubble, interval);
        AdhkarWidget.refreshAll(c, false);
        BubbleService.apply(c);
        JSObject r = new JSObject();
        r.put("canDrawOverlays", BubbleService.canDraw(c));
        call.resolve(r);
    }

    @PluginMethod
    public void openOverlaySettings(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getContext().getPackageName()));
            if (!start(i)) start(appDetails());
        }
        call.resolve();
    }

    @PluginMethod
    public void shamarlyStatus(PluginCall call) {
        JSObject r = new JSObject();
        r.put("done", ShamarlyDownloader.countDone(getContext()));
        r.put("total", ShamarlyDownloader.LEAVES);
        r.put("running", ShamarlyDownloader.running());
        r.put("error", ShamarlyDownloader.lastError());
        r.put("dir", ShamarlyDownloader.dir(getContext()).getAbsolutePath());
        call.resolve(r);
    }

    @PluginMethod
    public void shamarlyDownload(PluginCall call) {
        ShamarlyDownloader.start(getContext());
        shamarlyStatus(call);
    }

    @PluginMethod
    public void shamarlyStop(PluginCall call) {
        ShamarlyDownloader.stop();
        shamarlyStatus(call);
    }

    @PluginMethod
    public void shamarlyDelete(PluginCall call) {
        ShamarlyDownloader.deleteAll(getContext());
        shamarlyStatus(call);
    }

    @PluginMethod
    public void openUrl(PluginCall call) {
        String url = call.getString("url", "");
        if (url.startsWith("https://")) start(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        call.resolve();
    }

    private void openNotificationSettingsInternal() {
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
        } else {
            i = appDetails();
        }
        if (!start(i)) start(appDetails());
    }

    private Intent appDetails() {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + getContext().getPackageName()));
    }

    private boolean start(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private JSObject status() {
        Context c = getContext();
        JSObject r = new JSObject();
        r.put("enabled", ReminderScheduler.isEnabled(c));
        r.put("from", ReminderScheduler.fromHour(c));
        r.put("to", ReminderScheduler.toHour(c));
        r.put("nextAt", ReminderScheduler.nextAt(c));
        r.put("playInSilent", ReminderScheduler.playInSilent(c));
        r.put("notifications", NotificationManagerCompat.from(c).areNotificationsEnabled());
        r.put("exactAlarms", ReminderScheduler.canScheduleExact(c));
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        r.put("ignoringBattery", pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName()));
        r.put("manufacturer", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase());
        r.put("sdk", Build.VERSION.SDK_INT);
        r.put("canDrawOverlays", BubbleService.canDraw(c));
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            r.put("versionCode", Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode);
            r.put("versionName", pi.versionName);
        } catch (Exception ignored) {}
        return r;
    }
}
