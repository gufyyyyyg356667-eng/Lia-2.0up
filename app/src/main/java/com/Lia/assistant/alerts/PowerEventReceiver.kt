package com.Lia.assistant.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.Lia.assistant.data.Prefs

/** Speaks when the charger is connected or removed. */
class PowerEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.alertsOn(context)) return
        val percent = batteryPercent(context)
        val text = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> PhoneEvents.charging(percent)
            Intent.ACTION_POWER_DISCONNECTED -> PhoneEvents.unplugged(percent)
            else -> return
        }
        AlertDelivery.deliver(context, text)
    }

    private fun batteryPercent(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 0..100) level else -1
    }
}
