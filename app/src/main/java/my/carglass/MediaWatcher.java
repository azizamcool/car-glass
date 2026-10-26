package my.carglass;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.List;

/** Follows the media session that is playing (YouTube Music, YouTube, Bluetooth music, ...). */
final class MediaWatcher {
    static final class State {
        String source, title, artist;
        Bitmap art;
        boolean playing;
        long position, duration; // ms; duration <= 0 when unknown
    }

    private final Context ctx;
    private final Handler handler;
    private final Runnable onChange;
    private final ComponentName listener;
    private MediaSessionManager msm;
    private MediaController current;
    private boolean started;

    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState s) {
            pick();
        }

        @Override public void onMetadataChanged(MediaMetadata m) {
            onChange.run();
        }

        @Override public void onSessionDestroyed() {
            pick();
        }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener sessions = list -> pick();

    MediaWatcher(Context ctx, Handler handler, Runnable onChange) {
        this.ctx = ctx;
        this.handler = handler;
        this.onChange = onChange;
        this.listener = new ComponentName(ctx, MediaAccess.class);
    }

    boolean hasAccess() {
        String s = Settings.Secure.getString(ctx.getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(listener.flattenToString());
    }

    void start() {
        if (started || !hasAccess()) return;
        try {
            msm = (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
            msm.addOnActiveSessionsChangedListener(sessions, listener, handler);
            started = true;
            pick();
        } catch (SecurityException | NullPointerException ignored) {
        }
    }

    void stop() {
        if (!started) return;
        try {
            msm.removeOnActiveSessionsChangedListener(sessions);
        } catch (Exception ignored) {
        }
        if (current != null) current.unregisterCallback(callback);
        current = null;
        started = false;
    }

    /** Prefer a session that is playing; otherwise keep the current one, then any that has something to show. */
    private void pick() {
        MediaController best = null;
        try {
            List<MediaController> all = msm.getActiveSessions(listener);
            for (MediaController c : all) if (isPlaying(c)) { best = c; break; }
            if (best == null && current != null) {
                for (MediaController c : all) if (c.getSessionToken().equals(current.getSessionToken())) best = c;
            }
            if (best == null) for (MediaController c : all) if (c.getMetadata() != null) { best = c; break; }
        } catch (SecurityException ignored) {
        }
        if (current == null || best == null || !current.getSessionToken().equals(best.getSessionToken())) {
            if (current != null) current.unregisterCallback(callback);
            current = best;
            if (current != null) current.registerCallback(callback, handler);
        }
        onChange.run();
    }

    private static boolean isPlaying(MediaController c) {
        PlaybackState s = c.getPlaybackState();
        return s != null && (s.getState() == PlaybackState.STATE_PLAYING || s.getState() == PlaybackState.STATE_BUFFERING);
    }

    /** Null when nothing is worth showing (no session, or it has no title). */
    State get() {
        if (current == null) return null;
        MediaMetadata m = current.getMetadata();
        PlaybackState ps = current.getPlaybackState();
        if (m == null || ps == null) return null;
        CharSequence title = m.getText(MediaMetadata.METADATA_KEY_TITLE);
        if (title == null || title.length() == 0) title = m.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (title == null || title.length() == 0) return null;
        if (ps.getState() == PlaybackState.STATE_STOPPED || ps.getState() == PlaybackState.STATE_NONE) return null;
        State s = new State();
        s.title = title.toString();
        CharSequence a = m.getText(MediaMetadata.METADATA_KEY_ARTIST);
        if (a == null || a.length() == 0) a = m.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        if (a == null || a.length() == 0) a = m.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
        s.artist = a == null ? "" : a.toString();
        Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        s.art = b;
        s.playing = isPlaying(current);
        s.duration = m.getLong(MediaMetadata.METADATA_KEY_DURATION);
        long pos = ps.getPosition();
        if (s.playing && ps.getLastPositionUpdateTime() > 0) {
            pos += (long) ((SystemClock.elapsedRealtime() - ps.getLastPositionUpdateTime()) * ps.getPlaybackSpeed());
        }
        s.position = Math.max(0, s.duration > 0 ? Math.min(pos, s.duration) : pos);
        s.source = sourceName(current.getPackageName());
        return s;
    }

    private String sourceName(String pkg) {
        if (pkg.contains("youtube.music")) return "YouTube Music";
        if (pkg.equals("com.google.android.youtube")) return "YouTube";
        if (pkg.contains("bluetooth") || pkg.contains("btmusic")) return "BT Muzik";
        try {
            PackageManager pm = ctx.getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    void previous() {
        if (current != null) current.getTransportControls().skipToPrevious();
    }

    void toggle() {
        if (current == null) return;
        if (isPlaying(current)) current.getTransportControls().pause();
        else current.getTransportControls().play();
    }

    void next() {
        if (current != null) current.getTransportControls().skipToNext();
    }
}
