package com.example.take_homeactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BatteryReceiver : BroadcastReceiver() {

    companion object {
        var isPlaying = false
    }

    private var isFirstRun = true  // ✅ ignore initial trigger

    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {

            val level = intent.getIntExtra("level", -1)
            val scale = intent.getIntExtra("scale", -1)

            if (level == -1 || scale == -1) return

            val batteryPct = (level * 100) / scale

            // 🚨 Ignore first automatic broadcast
            if (isFirstRun) {
                isFirstRun = false
                return
            }

            if (batteryPct <= 20 && isPlaying) {

                val stopIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_STOP
                }
                context.startService(stopIntent)

                Toast.makeText(
                    context,
                    "Battery ≤20% – music stopped",
                    Toast.LENGTH_SHORT
                ).show()

                isPlaying = false

            } else if (batteryPct > 20 && !isPlaying) {

                val playIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY
                }
                context.startService(playIntent)

                Toast.makeText(
                    context,
                    "Battery >20% – music resumed",
                    Toast.LENGTH_SHORT
                ).show()

                isPlaying = true
            }
        }
    }
}