package com.example.spoticaplite;

import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.exoplayer.ExoPlayer;

/**
 * Modern Media3 Service that provides system-level notification and lock screen controls.
 */
public class MediaService extends MediaSessionService {

    public static MediaService instance;
    private MediaSession mediaSession;
    private ExoPlayer basePlayer;

    @UnstableApi
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        basePlayer = new ExoPlayer.Builder(this).build();
        
        // Ensure the player has a dummy state so Bluetooth devices don't think it's idle
        basePlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        
        // We wrap ExoPlayer to intercept play/pause/skip commands 
        // and route them to Spotify in the WebView.
        ForwardingPlayer bridgePlayer = new ForwardingPlayer(basePlayer) {
            @Override
            public void play() {
                if (MainActivity.instance != null) MainActivity.instance.sendCommand("playpause");
            }
            @Override
            public void pause() {
                if (MainActivity.instance != null) MainActivity.instance.sendCommand("playpause");
            }
            @Override
            public void seekToNext() {
                if (MainActivity.instance != null) MainActivity.instance.sendCommand("next");
            }
            @Override
            public void seekToPrevious() {
                if (MainActivity.instance != null) MainActivity.instance.sendCommand("prev");
            }

            @Override
            public void setRepeatMode(@Player.RepeatMode int repeatMode) {
                if (MainActivity.instance != null) MainActivity.instance.sendCommand("repeat");
            }

            @Override
            public boolean isCommandAvailable(@Player.Command int command) {
                return command == Player.COMMAND_PLAY_PAUSE ||
                       command == Player.COMMAND_SEEK_TO_NEXT ||
                       command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                       command == Player.COMMAND_SET_REPEAT_MODE ||
                       super.isCommandAvailable(command);
            }

            @androidx.annotation.NonNull
            @Override
            public Commands getAvailableCommands() {
                return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SET_REPEAT_MODE)
                        .build();
            }
        };

        mediaSession = new MediaSession.Builder(this, bridgePlayer).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    /** Update system notification metadata */
    public void updateMetadata(String title, String artist, boolean isPlaying, String artUrl) {
        if (basePlayer != null) {
            MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist);
            
            if (artUrl != null && !artUrl.isEmpty()) {
                metaBuilder.setArtworkUri(android.net.Uri.parse(artUrl));
            }
            
            basePlayer.setPlaylistMetadata(metaBuilder.build());
            
            // Sync play/pause state with WebView
            // We use a small hack: if it's supposed to be playing, we ensure the player is in a playing state
            // even if it has no actual media, to keep the notification and service active.
            if (isPlaying) {
                if (!basePlayer.isPlaying()) {
                    basePlayer.setPlayWhenReady(true);
                    basePlayer.prepare();
                    basePlayer.play();
                }
            } else {
                if (basePlayer.isPlaying()) {
                    basePlayer.pause();
                }
            }
        }
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@androidx.annotation.NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        instance = null;
        if (mediaSession != null) {
            Player player = mediaSession.getPlayer();
            player.release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
