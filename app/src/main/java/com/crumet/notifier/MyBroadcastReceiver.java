package com.crumet.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by ravi on 2/28/2018.
 */

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static MediaPlayer player;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Helper.PLAY_ACTION)) {
            Log.d("broadcastsomething", "playing music"+ Helper.isPlaying());
            play(context, getAlarmSound());
            Helper.setPlaying(true);
        }
        if (intent.getAction().equals(Helper.PAUSE_ACTION)) {
            Log.d("broadcastsomething", "pausing music"+Helper.isPlaying());
            player.stop();
            Helper.setPlaying(false);
        }

    }

    private void play(Context context, Uri alert) {
        if (Helper.isPlaying())
            return;
        player = new MediaPlayer();
        try {
            player.setDataSource(context, alert);
            final AudioManager audio = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                player.setAudioStreamType(AudioManager.STREAM_ALARM);
                player.prepare();
                player.start();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms
                        player.stop();
                        Helper.setPlaying(false);
                    }
                }, 60000);
            }
        } catch (IOException e) {
            Log.e("Error....", "Check code...");
        }
    }

    private Uri getAlarmSound() {
        Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        return alertSound;
    }
}
