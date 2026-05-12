package com.example.take_homeactivity

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val songs = mutableListOf<Song>()
    private lateinit var adapter: MusicAdapter

    private val AUDIO_PERMISSION_CODE = 100
    private val NOTIFICATION_PERMISSION_CODE = 101

    // Battery Receiver
    private lateinit var batteryReceiver: BatteryReceiver

    private val AUDIO_PERMISSION: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()

        // Register battery receiver dynamically
        batteryReceiver = BatteryReceiver()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)

        val btnLoad: Button = findViewById(R.id.btnLoad)
        val btnStop: Button = findViewById(R.id.btnStop)
        val listView: ListView = findViewById(R.id.listViewSongs)

        adapter = MusicAdapter(this, songs)
        listView.adapter = adapter

        btnLoad.setOnClickListener {
            checkPermissionAndLoad()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val song = songs[position]
            playMusic(song.uri)
        }

        btnStop.setOnClickListener {
            stopMusic()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, AUDIO_PERMISSION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(AUDIO_PERMISSION),
                AUDIO_PERMISSION_CODE
            )
        } else {
            loadMusic()
        }
    }

    private fun loadMusic() {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        val cursor = contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        songs.clear()

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "Unknown Title"
                val artist = it.getString(artistColumn) ?: "Unknown Artist"

                val contentUri: Uri =
                    ContentUris.withAppendedId(collection, id)

                songs.add(Song(title, artist, contentUri.toString()))
            }
        }

        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Loaded ${songs.size} songs", Toast.LENGTH_SHORT).show()
    }

    private fun playMusic(songUri: String) {
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra("SONG_URI", songUri)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Track playing state
        BatteryReceiver.isPlaying = true

        Toast.makeText(this, "Playing music", Toast.LENGTH_SHORT).show()
    }

    private fun stopMusic() {
        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_STOP
        }
        startService(intent)

        // IMPORTANT
        BatteryReceiver.isPlaying = false

        Toast.makeText(this, "Music stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        // unregister receiver
        unregisterReceiver(batteryReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    loadMusic()
                } else {
                    Toast.makeText(this, "Audio permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}