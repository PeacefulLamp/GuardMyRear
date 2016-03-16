package com.pekka.guardmyrear;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

/**
 * Created by havard on 16.03.16.
 */
public class NotificationCenter {
    /**
     * Send typical notification to user when something is close
     */
    public static Uri GetRingtone()
    {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    public static void PingNotification(
            NotificationManager man,Context context,
            Uri rt, String title,String msg) {
        NotificationCompat.Builder bld = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(msg)
                .setSound(rt)
                .setSmallIcon(R.drawable.ic_menu_camera)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        man.notify(0,bld.build());
    }
}
