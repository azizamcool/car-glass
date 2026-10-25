package my.carglass;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Small CSV of GPS fixes to find out where "GPS delay" comes from.
 * File: /sdcard/Android/data/my.carglass/files/gps-log.csv (pull it with adb after a drive).
 * Columns: wall_ms, age_ms (how old the fix already is on arrival), interval_ms (gap since the previous fix),
 * speed_kmh (-1 = none), accuracy_m, satellites_used (-1 = unknown).
 * No coordinates are written, only timing and speed. Capped at about 400 KB (one .old file is kept).
 */
final class GpsLog {
    private static final long MAX_BYTES = 400_000;
    private final File file;
    private long lastFixNanos;

    GpsLog(Context c) {
        File dir = c.getExternalFilesDir(null);
        file = dir == null ? null : new File(dir, "gps-log.csv");
    }

    synchronized void record(Location l, int sats) {
        if (file == null) return;
        long fixNanos = l.getElapsedRealtimeNanos();
        long age = (SystemClock.elapsedRealtimeNanos() - fixNanos) / 1_000_000;
        long gap = lastFixNanos == 0 ? 0 : (fixNanos - lastFixNanos) / 1_000_000;
        lastFixNanos = fixNanos;
        try {
            if (file.length() > MAX_BYTES) {
                File old = new File(file.getParentFile(), "gps-log.old.csv");
                //noinspection ResultOfMethodCallIgnored
                old.delete();
                //noinspection ResultOfMethodCallIgnored
                file.renameTo(old);
            }
            boolean fresh = !file.exists();
            try (FileWriter w = new FileWriter(file, true)) {
                if (fresh) w.write("wall_ms,age_ms,interval_ms,speed_kmh,accuracy_m,satellites_used\n");
                w.write(String.format(Locale.US, "%d,%d,%d,%.1f,%.1f,%d\n", System.currentTimeMillis(), age, gap,
                        l.hasSpeed() ? l.getSpeed() * 3.6f : -1f, l.hasAccuracy() ? l.getAccuracy() : -1f, sats));
            }
        } catch (IOException ignored) {
        }
    }
}
