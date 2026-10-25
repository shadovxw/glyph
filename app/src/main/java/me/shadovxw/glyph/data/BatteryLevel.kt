package me.shadovxw.glyph.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Below this the Battery card's bar turns red (DESIGN.md 1). */
const val LowBatteryPercent = 20

private fun Intent.batteryPercent(): Int {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level < 0 || scale <= 0) -1 else (level * 100f / scale).toInt()
}

/** The phone's battery level in percent, live. The battery broadcast is sticky, so there is a value straight away. */
@Composable
fun rememberBatteryPercent(): State<Int> {
    val context: Context = LocalContext.current
    val percent = remember {
        mutableIntStateOf(
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.batteryPercent() ?: -1,
        )
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val value = intent.batteryPercent()
                if (value >= 0) percent.intValue = value
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return percent
}
