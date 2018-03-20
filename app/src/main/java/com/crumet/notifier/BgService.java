package com.crumet.notifier;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.content.ContentValues.TAG;

public class BgService extends Service {

    NotificationManagerCompat notificationManager;
    private Socket socket;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                notificationManager = NotificationManagerCompat.from(BgService.this);
                PowerManager mgr = (PowerManager)BgService.this.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
                wakeLock.acquire();
                connect();

            }
        }).start();
        return START_STICKY;
    }

    private void showNotification(String title, String content, boolean status, int id) {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(BgService.this, Helper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(content)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (status) {
            mBuilder.setOngoing(true);
            mBuilder.setAutoCancel(false);
        }



        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(id, mBuilder.build());
    }

    private void playAlarmWithNotification(String title, String content) {
        if (!Helper.isplaying){
            Intent playIntent = new Intent(Helper.PLAY_ACTION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(playIntent);
        }


        Intent snoozeIntent = new Intent(BgService.this, MyBroadcastReceiver.class);
        snoozeIntent.setAction(Helper.PAUSE_ACTION);
        PendingIntent snoozePendingIntent =
                PendingIntent.getBroadcast(BgService.this, 0, snoozeIntent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(BgService.this, Helper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(snoozePendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .addAction(R.drawable.ic_launcher_foreground, getString(R.string.snooze),
                        snoozePendingIntent);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, mBuilder.build());
    }

    private void connect() {
        try {
            socket = IO.socket(Helper.SOCKET_URL+":"+Helper.SOCKET_PORT);
            socket.connect();
            socket.emit("start", "android");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("SOCKET", "connected");
                    showNotification("Connected to server", "Waiting for email", true, 0);
                    notificationManager.cancel(2);
                }
            });
            socket.on("newemail", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject data = (JSONObject) args[0];
                    Log.d(TAG, data.toString());
                    Log.d("SOCKET", "received email");
                    try {
                        JSONObject mail = data.getJSONObject("mail");
                        String sender = mail.getString("from");
                        String subj = mail.getString("subject");
                        playAlarmWithNotification("new email from " + sender, subj);
                        Log.d(TAG, sender);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "Disconnected");
                    showNotification("Disconnected from server", "tap to reconnect", false, 2);
                    connect();
                    notificationManager.cancel(0);
                }

            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
