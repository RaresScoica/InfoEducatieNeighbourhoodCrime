package ro.infoeducatie.neighbourhoodcrime.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import ro.infoeducatie.neighbourhoodcrime.R;

public class NotificationHelper extends ContextWrapper {
    private static final String NOTIFICATION_CHANNEL_ID = "ro.infoeducatie.neighbourhoodcrime.Notification";
    private static final String NOTIFICATION_CHANNEL_NAME = "Notification Neighbourhood";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super(base);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        NotificationChannel notChannels = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        notChannels.enableLights(true);
        notChannels.setDescription("Problem Channel");
        notChannels.enableLights(true);
        notChannels.setLightColor(Color.BLUE);
        notChannels.setVibrationPattern(new long[]{0, 1000, 500, 1000});

        getManager().createNotificationChannel(notChannels);
    }

    public NotificationManager getManager() {
        if(manager == null)
            manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            return manager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getNotification(String title, String content, PendingIntent contentIntent, Uri soundUri) {
        return new Notification.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setContentText(content)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_logo);
    }
}
