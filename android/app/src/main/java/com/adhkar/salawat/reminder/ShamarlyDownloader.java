package com.adhkar.salawat.reminder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** يحمّل صفحات مصحف الشمرلي إلى ذاكرة الهاتف ليعمل بلا إنترنت. يستأنف من حيث توقف. */
public final class ShamarlyDownloader {
    public static final String ID = "20191116_20191116_1655";
    public static final int LEAVES = 522;
    public static final int WIDTH = 900;

    private static volatile Thread worker;
    private static volatile boolean cancel;
    private static volatile String lastError = "";

    private ShamarlyDownloader() {}

    public static File dir(Context c) {
        File d = new File(c.getFilesDir(), "shamarly");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** الصفحة تُحفظ مضغوطة (webp بعرض ٨٠٠) لتوفير المساحة: حوالي ٩٠ كيلو بدل ٣٥٠. */
    public static File file(Context c, int leaf) { return new File(dir(c), leaf + ".webp"); }
    private static File oldFile(Context c, int leaf) { return new File(dir(c), leaf + ".jpg"); }
    private static boolean has(Context c, int leaf) {
        File f = file(c, leaf), o = oldFile(c, leaf);
        return (f.exists() && f.length() > 1000) || (o.exists() && o.length() > 1000);
    }

    public static int countDone(Context c) {
        String[] names = dir(c).list();
        int n = 0;
        if (names != null) for (String s : names) if (s.endsWith(".jpg") || s.endsWith(".webp")) n++;
        return n;
    }

    /** أسماء الملفات المحفوظة (للواجهة). */
    public static List<String> files(Context c) {
        List<String> out = new ArrayList<>();
        String[] names = dir(c).list();
        if (names != null) for (String s : names) if (s.endsWith(".jpg") || s.endsWith(".webp")) out.add(s);
        return out;
    }

    private static final ExecutorService one = Executors.newSingleThreadExecutor();

    /** حفظ صفحة واحدة أثناء القراءة (حتى تفتح بعد ذلك بلا إنترنت). */
    public static void cacheOne(Context ctx, int leaf) {
        final Context c = ctx.getApplicationContext();
        if (leaf < 0 || leaf >= LEAVES || has(c, leaf)) return;
        one.execute(() -> {
            if (has(c, leaf)) return;
            fetchPage(c, leaf);
        });
    }

    private static boolean fetchPage(Context c, int leaf) {
        File raw = new File(dir(c), leaf + ".raw");
        boolean ok = fetch("https://archive.org/download/" + ID + "/page/n" + leaf + "_w" + WIDTH + ".jpg", raw);
        if (!ok) return false;
        try {
            Bitmap b = BitmapFactory.decodeFile(raw.getPath());
            if (b == null) { raw.delete(); return false; }
            int w = Math.min(800, b.getWidth());
            Bitmap s = w == b.getWidth() ? b : Bitmap.createScaledBitmap(b, w, Math.round(b.getHeight() * (w / (float) b.getWidth())), true);
            File tmp = new File(dir(c), leaf + ".tmp");
            try (FileOutputStream fo = new FileOutputStream(tmp)) {
                Bitmap.CompressFormat fmt = Build.VERSION.SDK_INT >= 30 ? Bitmap.CompressFormat.WEBP_LOSSY : Bitmap.CompressFormat.WEBP;
                s.compress(fmt, 55, fo);
            }
            if (s != b) s.recycle();
            b.recycle();
            raw.delete();
            return tmp.renameTo(file(c, leaf));
        } catch (Throwable e) {
            raw.delete();
            return false;
        }
    }

    public static boolean running() { return worker != null && worker.isAlive(); }
    public static String lastError() { return lastError; }

    public static synchronized void start(Context ctx) {
        if (running()) return;
        final Context c = ctx.getApplicationContext();
        cancel = false;
        lastError = "";
        worker = new Thread(() -> {
            int fails = 0;
            for (int leaf = 0; leaf < LEAVES && !cancel; leaf++) {
                if (has(c, leaf)) continue;
                boolean ok = false;
                for (int attempt = 0; attempt < 3 && !ok && !cancel; attempt++) {
                    ok = fetchPage(c, leaf);
                    if (!ok) try { Thread.sleep(1500L * (attempt + 1)); } catch (InterruptedException ignored) {}
                }
                if (!ok && ++fails > 15) { lastError = "network"; break; }
            }
        }, "shamarly-download");
        worker.start();
    }

    public static void stop() { cancel = true; }

    public static void deleteAll(Context c) {
        stop();
        File[] fs = dir(c).listFiles();
        if (fs != null) for (File f : fs) f.delete();
    }

    private static boolean fetch(String url, File out) {
        HttpURLConnection con = null;
        File tmp = new File(out.getPath() + ".part");
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setInstanceFollowRedirects(true);
            con.setConnectTimeout(20000);
            con.setReadTimeout(40000);
            int code = con.getResponseCode();
            // متابعة التحويل يدويًا إن لزم
            for (int i = 0; i < 5 && (code == 301 || code == 302 || code == 303 || code == 307 || code == 308); i++) {
                String loc = con.getHeaderField("Location");
                con.disconnect();
                con = (HttpURLConnection) new URL(new URL(url), loc).openConnection();
                con.setConnectTimeout(20000);
                con.setReadTimeout(40000);
                code = con.getResponseCode();
            }
            if (code != 200) return false;
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
