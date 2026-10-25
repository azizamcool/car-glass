package my.carglass;

/** Offline prayer times (Fajr/Isha 18 deg, Asr shadow factor 1), after the praytimes.org formulas. */
final class PrayerTimes {
    static final String[] NAMES = {"Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak"};

    private PrayerTimes() {
    }

    /** Returns minutes after local midnight for each entry of NAMES. */
    static int[] compute(int year, int month, int day, double lat, double lng, double tz) {
        double jd = julian(year, month, day) - lng / (15 * 24);
        double[] t = {
                angleTime(jd, lat, 18, 5 / 24.0, true),
                angleTime(jd, lat, 0.833, 6 / 24.0, true),
                midDay(jd, 12 / 24.0),
                asrTime(jd, lat, 1, 13 / 24.0),
                angleTime(jd, lat, 0.833, 18 / 24.0, false),
                angleTime(jd, lat, 18, 18 / 24.0, false),
        };
        int[] out = new int[t.length];
        for (int i = 0; i < t.length; i++) {
            out[i] = (int) Math.round(fixHour(t[i] + tz - lng / 15) * 60);
        }
        return out;
    }

    private static double julian(int y, int m, int d) {
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    /** Returns {declination in degrees, equation of time in hours}. */
    private static double[] sun(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g)));
        double e = 23.439 - 0.00000036 * d;
        double ra = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(e)) * Math.sin(Math.toRadians(l)),
                Math.cos(Math.toRadians(l)))) / 15;
        double decl = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(e)) * Math.sin(Math.toRadians(l))));
        return new double[]{decl, q / 15 - fixHour(ra)};
    }

    private static double midDay(double jd, double t) {
        return fixHour(12 - sun(jd + t)[1]);
    }

    private static double angleTime(double jd, double lat, double angle, double t, boolean ccw) {
        double decl = sun(jd + t)[0];
        double noon = midDay(jd, t);
        double v = Math.toDegrees(Math.acos((-Math.sin(Math.toRadians(angle))
                - Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(decl)))
                / (Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(decl))))) / 15;
        return noon + (ccw ? -v : v);
    }

    private static double asrTime(double jd, double lat, double factor, double t) {
        double decl = sun(jd + t)[0];
        double angle = -Math.toDegrees(Math.atan(1 / (factor + Math.tan(Math.toRadians(Math.abs(lat - decl))))));
        return angleTime(jd, lat, angle, t, false);
    }

    private static double fixAngle(double a) {
        return a - 360 * Math.floor(a / 360);
    }

    private static double fixHour(double a) {
        return a - 24 * Math.floor(a / 24);
    }
}
