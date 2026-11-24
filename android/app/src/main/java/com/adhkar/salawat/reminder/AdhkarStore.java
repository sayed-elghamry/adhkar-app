package com.adhkar.salawat.reminder;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

/** الأذكار التي اختارها المستخدم للودجت والفقاعة العائمة. */
public final class AdhkarStore {
    static final String PREFS = "adhkar_display";
    private static final String[] DEFAULTS = {
        "اللهم صلِّ وسلِّم على نبينا محمد",
        "سبحان الله وبحمده، سبحان الله العظيم",
        "لا إله إلا الله وحده لا شريك له",
        "أستغفر الله العظيم وأتوب إليه",
        "لا حول ولا قوة إلا بالله"
    };

    private AdhkarStore() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static String[] items(Context c) {
        try {
            JSONArray a = new JSONArray(prefs(c).getString("items", "[]"));
            if (a.length() == 0) return DEFAULTS;
            String[] out = new String[a.length()];
            for (int i = 0; i < a.length(); i++) out[i] = a.getString(i);
            return out;
        } catch (Exception e) { return DEFAULTS; }
    }

    public static void save(Context c, JSONArray items, boolean bubble, int intervalSec) {
        prefs(c).edit()
            .putString("items", items == null ? "[]" : items.toString())
            .putBoolean("bubble", bubble)
            .putInt("interval", Math.max(15, intervalSec))
            .apply();
    }

    public static boolean bubble(Context c) { return prefs(c).getBoolean("bubble", false); }
    public static int interval(Context c) { return prefs(c).getInt("interval", 60); }

    /** يعيد الذكر التالي ويقدّم المؤشر. */
    public static String next(Context c, String key) {
        String[] it = items(c);
        int i = prefs(c).getInt(key, -1) + 1;
        if (i >= it.length) i = 0;
        prefs(c).edit().putInt(key, i).apply();
        return it[i];
    }

    public static String current(Context c, String key) {
        String[] it = items(c);
        int i = prefs(c).getInt(key, 0);
        return it[Math.max(0, Math.min(it.length - 1, i))];
    }
}
