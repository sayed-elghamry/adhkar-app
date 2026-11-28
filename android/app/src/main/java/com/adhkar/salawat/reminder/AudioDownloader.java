package com.adhkar.salawat.reminder;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** تنزيل تلاوة القراء (ملف لكل آية من everyayah.com) عند طلب المستخدم فقط، كما هي بدون تعديل. */
public final class AudioDownloader {
    private static volatile Thread worker;
    private static volatile boolean cancel;
    private static volatile int total, done;
    private static volatile String folderNow = "";

    private AudioDownloader() {}

    static String safe(String s) { return s == null ? "" : s.replaceAll("[^A-Za-z0-9_]", ""); }

    public static File dir(Context c, String folder) {
        File d = new File(new File(c.getFilesDir(), "recite"), safe(folder));
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static List<String> files(Context c, String folder) {
        List<String> out = new ArrayList<>();
        String[] n = dir(c, folder).list();
        if (n != null) for (String s : n) if (s.endsWith(".mp3")) out.add(s);
        return out;
    }

    public static boolean running() { return worker != null && worker.isAlive(); }
    public static int total() { return total; }
    public static int done() { return done; }
    public static String folder() { return folderNow; }

    public static synchronized void start(Context ctx, String folder, List<String> names) {
        if (running()) return;
        final Context c = ctx.getApplicationContext();
        final String f = safe(folder);
        cancel = false; total = names.size(); done = 0; folderNow = f;
        worker = new Thread(() -> {
            int fails = 0;
            for (String n : names) {
                if (cancel) break;
                String nm = n.replaceAll("[^0-9]", "");
                if (nm.length() != 6) { done++; continue; }
                File out = new File(dir(c, f), nm + ".mp3");
                boolean ok = out.exists() && out.length() > 1000;
                for (int a = 0; a < 3 && !ok && !cancel; a++) {
                    ok = fetch("https://everyayah.com/data/" + f + "/" + nm + ".mp3", out);
                    if (!ok) try { Thread.sleep(1500L * (a + 1)); } catch (InterruptedException ignored) {}
                }
                if (!ok && ++fails > 10) break;
                done++;
            }
        }, "recite-download");
        worker.start();
    }

    public static void stop() { cancel = true; }

    public static void delete(Context c, String folder, List<String> names) {
        File d = dir(c, folder);
        if (names == null || names.isEmpty()) {
            File[] fs = d.listFiles();
            if (fs != null) for (File x : fs) x.delete();
            return;
        }
        for (String n : names) new File(d, n.replaceAll("[^0-9]", "") + ".mp3").delete();
    }

    private static boolean fetch(String url, File out) {
        HttpURLConnection con = null;
        File tmp = new File(out.getPath() + ".part");
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setInstanceFollowRedirects(true);
            con.setConnectTimeout(20000);
            con.setReadTimeout(40000);
            if (con.getResponseCode() != 200) return false;
            try (InputStream in = con.getInputStream(); FileOutputStream fo = new FileOutputStream(tmp)) {
                byte[] buf = new byte[32768];
                int r;
                while ((r = in.read(buf)) > 0) {
                    if (cancel) return false;
                    fo.write(buf, 0, r);
                }
            }
            if (tmp.length() < 1000) { tmp.delete(); return false; }
            return tmp.renameTo(out);
        } catch (Exception e) {
            tmp.delete();
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }
}
