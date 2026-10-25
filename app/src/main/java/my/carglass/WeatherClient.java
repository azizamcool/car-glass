package my.carglass;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/** Open-Meteo (weather) and OpenStreetMap Nominatim (place name). Call off the main thread. */
final class WeatherClient {
    static final class Result {
        int temp;
        int code;
        String place;
    }

    private WeatherClient() {
    }

    static Result fetch(double lat, double lng) throws Exception {
        Result r = new Result();
        String w = get(String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,weather_code&timezone=auto", lat, lng));
        JSONObject cur = new JSONObject(w).getJSONObject("current");
        r.temp = (int) Math.round(cur.getDouble("temperature_2m"));
        r.code = cur.getInt("weather_code");
        try {
            String g = get(String.format(Locale.US,
                    "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=14"
                            + "&accept-language=ms&lat=%.4f&lon=%.4f", lat, lng));
            JSONObject a = new JSONObject(g).getJSONObject("address");
            String place = first(a, "village", "hamlet", "suburb", "neighbourhood", "town", "city", "city_district",
                    "municipality", "county", "state_district");
            String state = first(a, "state");
            r.place = place == null ? state : state == null ? place : place + ", " + state;
        } catch (Exception ignored) {
        }
        return r;
    }

    private static String first(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.optString(k, "");
            if (!v.isEmpty()) return v;
        }
        return null;
    }

    private static String get(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setRequestProperty("User-Agent", "CarGlass/1.0 (personal car launcher)");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    static boolean isRain(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82) || code >= 95;
    }

    static String describe(int code) {
        if (code == 0) return "Cerah";
        if (code <= 2) return "Cerah berawan";
        if (code == 3) return "Mendung";
        if (code == 45 || code == 48) return "Berkabus";
        if (code >= 51 && code <= 57) return "Gerimis";
        if (code >= 61 && code <= 67) return "Hujan";
        if (code >= 80 && code <= 82) return "Hujan lebat";
        if (code >= 95) return "Ribut petir";
        return "Berawan";
    }

    static String icon(int code) {
        if (code == 0) return "☀️";
        if (code <= 2) return "⛅";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 95) return "⛈️";
        if (isRain(code)) return "🌧️";
        return "⛅";
    }
}
