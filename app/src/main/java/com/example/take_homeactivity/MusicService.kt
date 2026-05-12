package com.example.take_homeactivity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var lastSongUri: String? = null   // ✅ ADDED

    companion object {
        const val CHANNEL_ID = "music_service_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PLAY = "ACTION_PLAY"   // ✅ ADDED
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            ACTION_STOP -> {
                stopMusic()
                stopForeground(true)
                stopSelf()
                BatteryReceiver.isPlaying = false   // ✅ ADDED
                return START_NOT_STICKY
            }

            ACTION_PLAY -> {
                lastSongUri?.let {
                    startForeground(NOTIFICATION_ID, createNotification())
                    playMusic(it)
                    BatteryReceiver.isPlaying = true   // ✅ ADDED
                }
                return START_STICKY
            }
        }

        val songUri = intent?.getStringExtra("SONG_URI")
        if (songUri != null) {
            lastSongUri = songUri   // ✅ ADDED
            startForeground(NOTIFICATION_ID, createNotification())
            playMusic(songUri)
            BatteryReceiver.isPlaying = true   // ✅ ADDED
        }

        return START_STICKY
    }

    private fun playMusic(songUri: String) {
        try {
            if (!requestAudioFocus()) {
                stopSelf()
                return
            }

            mediaPlayer?.release()

            val uri = Uri.parse(songUri)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MusicService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    stopForeground(true)
                    stopSelf()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MusicService", "MediaPlayer error: what=$what extra=$extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error playing music", e)
            stopSelf()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        abandonAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { }
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Music Player")
            .setContentText("Playing music...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(activityPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}