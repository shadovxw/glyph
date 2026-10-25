package me.shadovxw.glyph.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hosts one other app's home-screen widget inside Glyph. Glyph is a launcher, so Android lets it do what the stock
 * launcher does: bind a widget (after the user agrees once), show it, and keep it updated. It is used for WhatsApp,
 * whose own chats widget is the only way to see the full chat list.
 *
 * Adding it takes up to two system screens, both answered by the user: "allow Glyph to create widgets" (bind), and
 * the widget's own set-up screen if it has one (configure).
 */
class WidgetHostController(
    private val context: Context,
    /** The widget this controller hosts, for example WhatsApp's chats widget. */
    val provider: ComponentName,
    private val prefsKey: String,
) {
    private val manager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context, HostId)
    private val prefs = context.getSharedPreferences("glyph_widgets", Context.MODE_PRIVATE)

    private val _boundId = MutableStateFlow(savedValidId())
    /** The bound widget's id, or null when none is added (or it was removed from outside). */
    val boundId: StateFlow<Int?> = _boundId.asStateFlow()

    /** An id that was reserved and is waiting for the user's answer. */
    private var pendingId: Int? = null

    fun isAvailable(): Boolean = manager.installedProviders.any { it.provider == provider }

    fun startListening() { runCatching { host.startListening() } }

    fun stopListening() { runCatching { host.stopListening() } }

    /** The widget as a View, drawn by its own app. Null when it is no longer bound. */
    fun createView(id: Int): AppWidgetHostView? {
        val info = manager.getAppWidgetInfo(id) ?: return null
        return host.createView(context, id, info).also { it.setAppWidget(id, info) }
    }

    // ---- adding it ----

    sealed interface Step {
        /** Bound already; carry on to [configure]. */
        data class Bound(val id: Int) : Step
        /** The system needs the user to agree first; start [intent] for a result. */
        data class NeedsConsent(val intent: Intent) : Step
        data object Unavailable : Step
    }

    fun begin(): Step {
        if (!isAvailable()) return Step.Unavailable
        val id = host.allocateAppWidgetId()
        pendingId = id
        return if (manager.bindAppWidgetIdIfAllowed(id, provider)) {
            Step.Bound(id)
        } else {
            Step.NeedsConsent(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider),
            )
        }
    }

    /** After the bind screen: carries on to the widget's set-up screen if it has one, otherwise finishes. */
    fun onBindResult(activity: Activity, granted: Boolean, configureRequestCode: Int) {
        val id = pendingId ?: return
        if (!granted) return cancelPending()
        configureOrFinish(activity, id, configureRequestCode)
    }

    fun configureOrFinish(activity: Activity, id: Int, configureRequestCode: Int) {
        val info = manager.getAppWidgetInfo(id)
        if (info?.configure != null) {
            host.startAppWidgetConfigureActivityForResult(activity, id, 0, configureRequestCode, null)
        } else {
            finish(id)
        }
    }

    fun onConfigureResult(ok: Boolean) {
        val id = pendingId ?: return
        if (ok) finish(id) else cancelPending()
    }

    private fun finish(id: Int) {
        // Replace any older instance so widgets are not left behind.
        _boundId.value?.takeIf { it != id }?.let { runCatching { host.deleteAppWidgetId(it) } }
        prefs.edit().putInt(prefsKey, id).apply()
        pendingId = null
        _boundId.value = id
    }

    private fun cancelPending() {
        pendingId?.let { runCatching { host.deleteAppWidgetId(it) } }
        pendingId = null
    }

    /** Removes the widget from Glyph. */
    fun remove() {
        _boundId.value?.let { runCatching { host.deleteAppWidgetId(it) } }
        prefs.edit().remove(prefsKey).apply()
        _boundId.value = null
    }

    private fun savedValidId(): Int? {
        val id = prefs.getInt(prefsKey, -1)
        return if (id != -1 && manager.getAppWidgetInfo(id) != null) id else null
    }

    private companion object {
        const val HostId = 2048
    }
}
