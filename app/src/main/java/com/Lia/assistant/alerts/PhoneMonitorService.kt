package com.Lia.assistant.alerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import com.Lia.assistant.data.Prefs
import java.util.Calendar
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Watches battery, headphones, network and downloads, and speaks or notifies when something changes.
 * Runs as a foreground service with a visible notification, so Android keeps it alive.
 */
class PhoneMonitorService : Service() {

    private var batteryReceiver: BroadcastReceiver? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val announcedLow = mutableSetOf<Int>()
    private var fullAnnounced = false
    private var networkLostBefore = false
    private var lastBriefingDay = -1
    private var lastStorageDay = -1
    private var lastHeatMs = 0L
    private val announcedEvents = mutableSetOf<String>()
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            runChecks()
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(NotificationChannel(CHANNEL, "Lia monitor", NotificationManager.IMPORTANCE_LOW))
        val note: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Lia")
            .setContentText("Phone alerts are on")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTE_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTE_ID, note)
        }
        registerBatteryHeadphoneDownload()
        registerNetwork()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        batteryReceiver?.let { runCatching { unregisterReceiver(it) } }
        networkCallback?.let {
            runCatching { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) }
        }
        super.onDestroy()
    }

    private fun registerBatteryHeadphoneDownload() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> battery(intent)
                    Intent.ACTION_HEADSET_PLUG ->
                        AlertDelivery.deliver(this@PhoneMonitorService, MonitorMessages.headphones(intent.getIntExtra("state", 0) == 1))
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE ->
                        AlertDelivery.deliver(this@PhoneMonitorService, MonitorMessages.downloadDone())
                }
            }
        }
        batteryReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    private fun battery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return
        val percent = level * 100 / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val full = status == BatteryManager.BATTERY_STATUS_FULL
        val charging = full || status == BatteryManager.BATTERY_STATUS_CHARGING

        if (full && !fullAnnounced) {
            fullAnnounced = true
            AlertDelivery.deliver(this, MonitorMessages.full())
        } else if (!full) {
            fullAnnounced = false
        }

        val tenthsC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        if (tenthsC >= 420 && System.currentTimeMillis() - lastHeatMs > 30 * 60_000L) {
            lastHeatMs = System.currentTimeMillis()
            AlertDelivery.deliver(this, MonitorMessages.hot(tenthsC / 10))
        }

        if (percent > MonitorMessages.RESET_ABOVE) announcedLow.clear()
        if (!charging) {
            MonitorMessages.lowThresholdToAnnounce(percent, announcedLow)?.let { threshold ->
                announcedLow += threshold
                AlertDelivery.deliver(this, MonitorMessages.low(percent))
            }
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Runs every minute while the service is alive. Network and calendar work happens on background threads. */
    private fun runChecks() {
        val now = System.currentTimeMillis()
        Reminders.takeDue(this, now).forEach { AlertDelivery.deliver(this, it.text) }

        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_YEAR)
        if (cal.get(Calendar.HOUR_OF_DAY) == Prefs.briefingHour(this) && day != lastBriefingDay) {
            lastBriefingDay = day
            val app = applicationContext
            val start = startOfToday()
            Thread {
                val loc = InfoSources.lastLocation(app)
                val weather = loc?.let { runCatching { InfoSources.weatherText(it.latitude, it.longitude) }.getOrNull() }
                val titles = InfoSources.eventsBetween(app, start, start + DAY_MS).map { it.first }
                AlertDelivery.deliver(app, Briefing.morning(weather, titles))
            }.start()
        }

        val app = applicationContext
        Thread {
            InfoSources.eventsBetween(app, now, now + 15 * 60_000L).forEach { (title, begin) ->
                if (announcedEvents.add("$title@$begin")) AlertDelivery.deliver(app, MonitorMessages.eventSoon(title))
            }
        }.start()

        if (day != lastStorageDay) {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val freeFraction = stat.availableBytes.toDouble() / stat.totalBytes.toDouble()
            if (freeFraction < 0.10) {
                lastStorageDay = day
                AlertDelivery.deliver(this, MonitorMessages.storageLow((freeFraction * 100).toInt()))
            }
        }
    }

    private fun registerNetwork() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                if (!networkLostBefore) {
                    networkLostBefore = true
                    AlertDelivery.deliver(this@PhoneMonitorService, MonitorMessages.networkLost())
                }
            }

            override fun onAvailable(network: Network) {
                if (networkLostBefore) {
                    networkLostBefore = false
                    AlertDelivery.deliver(this@PhoneMonitorService, MonitorMessages.networkBack())
                }
            }
        }
        networkCallback = callback
        runCatching { cm.registerDefaultNetworkCallback(callback) }
    }

    companion object {
        private const val CHANNEL = "lia_monitor"
        private const val NOTE_ID = 4411
        private const val DAY_MS = 24L * 60 * 60 * 1000

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, PhoneMonitorService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, PhoneMonitorService::class.java))
        }
    }
}
