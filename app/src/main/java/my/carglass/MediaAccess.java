package my.carglass;

import android.service.notification.NotificationListenerService;

/**
 * Does nothing itself. Android only lets an app read the active media sessions (what is playing, the play/pause
 * buttons) when it holds notification access, and that access is granted to a NotificationListenerService.
 */
public class MediaAccess extends NotificationListenerService {
}
