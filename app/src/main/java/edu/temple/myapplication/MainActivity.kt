package edu.temple.myapplication

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_START = 100
        const val PREFS_NAME = "timer_prefs"
        const val KEY_PAUSED_VALUE = "paused_countdown_value"
    }

    lateinit var timerBinder: TimerService.TimerBinder
    var isBound = false
    var currentCountdownValue = 0

    private val timerHandler = Handler(Looper.getMainLooper()) { msg ->
        currentCountdownValue = msg.what
        findViewById<TextView>(R.id.textView).text = msg.what.toString()
        true
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as TimerService.TimerBinder
            timerBinder.setHandler(timerHandler)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Show saved value on startup if one exists
        val savedValue = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_PAUSED_VALUE, -1)
        if (savedValue > 0) {
            findViewById<TextView>(R.id.textView).text = savedValue.toString()
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (isBound) handleStart()
        }

        findViewById<Button>(R.id.pauseButton).setOnClickListener {
            if (isBound) handlePause()
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            if (isBound) handleStop()
        }
    }

    private fun handleStart() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedValue = prefs.getInt(KEY_PAUSED_VALUE, -1)

        if (timerBinder.paused) {
            timerBinder.pause()
            prefs.edit().remove(KEY_PAUSED_VALUE).apply()
        } else if (savedValue > 0) {
            timerBinder.start(savedValue)
            prefs.edit().remove(KEY_PAUSED_VALUE).apply()
        } else {
            timerBinder.start(DEFAULT_START)
        }
    }

    private fun handlePause() {
        if (timerBinder.isRunning) {
            timerBinder.pause()
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_PAUSED_VALUE, currentCountdownValue)
                .apply()
        }
    }

    private fun handleStop() {
        timerBinder.stop()
        findViewById<TextView>(R.id.textView).text = "0"
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .remove(KEY_PAUSED_VALUE)
            .apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_start -> {
                if (isBound) handleStart()
                true
            }
            R.id.action_pause -> {
                if (isBound) handlePause()
                true
            }
            R.id.action_stop -> {
                if (isBound) handleStop()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, TimerService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}
