package me.shadovxw.glyph.data.orders

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The four steps of a delivery, drawn as the four segments of the progress bar. */
enum class OrderStage { Placed, Preparing, OnTheWay, Arrived }

@Immutable
class Order(
    /** "Zomato" or "Blinkit". */
    val app: String,
    /** The restaurant, when the notification names one. */
    val place: String,
    val stage: OrderStage,
    /** Minutes to arrival, when the notification says. */
    val etaMinutes: Int?,
    val postedAt: Long,
)

@Immutable
data class OrdersState(
    val hasAccess: Boolean = false,
    val orders: List<Order> = emptyList(),
)

/**
 * Live food and grocery orders (DESIGN.md 3, screen 5), read from the Zomato and Blinkit notifications while an order
 * is active. Nothing is stored: when the notification goes away the order does too. The notification wording is not a
 * public format, so the stage and the ETA are worked out from keywords and "N min" in the text; an order that cannot
 * be understood is shown as "Placed" with no ETA rather than guessed at.
 */
object Orders {
    private val Apps = mapOf(
        "com.application.zomato" to "Zomato",
        "com.grofers.customerapp" to "Blinkit",
    )

    private val _state = MutableStateFlow(OrdersState())
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    fun setAccess(hasAccess: Boolean) = _state.update { it.copy(hasAccess = hasAccess) }

    fun onConnected(active: Array<StatusBarNotification>) {
        val found = active.mapNotNull(::parse)
        // The latest per app: an app can post several updates for one order.
        _state.update { it.copy(orders = found.groupBy { o -> o.app }.map { (_, list) -> list.maxBy { o -> o.postedAt } }) }
    }

    fun onPosted(sbn: StatusBarNotification) {
        val order = parse(sbn) ?: return
        _state.update { it.copy(orders = it.orders.filter { o -> o.app != order.app } + order) }
    }

    fun onRemoved(sbn: StatusBarNotification) {
        val app = Apps[sbn.packageName] ?: return
        _state.update { it.copy(orders = it.orders.filter { o -> o.app != app }) }
    }

    /** For checking the layout on a phone with no active orders; used by the debug build only. */
    fun debugReplace(orders: List<Order>) = _state.update { it.copy(orders = orders) }

    private fun parse(sbn: StatusBarNotification): Order? {
        val app = Apps[sbn.packageName] ?: return null
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString().orEmpty()
        val all = "$title $text".lowercase()
        // Only order updates: marketing and offers from the same apps are not orders.
        if (OrderWords.none { it in all }) return null

        val stage = when {
            listOf("delivered", "has arrived", "arrived", "reached you", "at your door").any { it in all } -> OrderStage.Arrived
            listOf("on the way", "out for delivery", "picked up", "is arriving", "will arrive", "arriving in", "heading").any { it in all } -> OrderStage.OnTheWay
            listOf("preparing", "being prepared", "cooking", "packing", "packed", "getting ready").any { it in all } -> OrderStage.Preparing
            else -> OrderStage.Placed
        }
        val eta = Regex("(\\d+)\\s*(?:-\\s*\\d+\\s*)?(?:min|mins|minutes)\\b").find(all)?.groupValues?.get(1)?.toIntOrNull()
        val place = Regex("from ([^.,!\\n]+?)(?: is | has | will | are |[.,!\\n]| and |$)", RegexOption.IGNORE_CASE)
            .find("$title $text")?.groupValues?.get(1)?.trim().orEmpty()
        return Order(app, if (app == "Blinkit") "" else place, stage, eta, sbn.postTime)
    }

    private val OrderWords = listOf("order", "delivery", "delivered", "rider", "arriv", "preparing", "on the way")
}
