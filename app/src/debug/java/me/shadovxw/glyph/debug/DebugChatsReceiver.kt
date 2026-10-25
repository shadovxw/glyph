package me.shadovxw.glyph.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.shadovxw.glyph.data.chat.Chat
import me.shadovxw.glyph.data.chat.Chats
import me.shadovxw.glyph.data.github.AccountResult
import me.shadovxw.glyph.data.github.CiStatus
import me.shadovxw.glyph.data.github.GitHubData
import me.shadovxw.glyph.data.github.GitHubRepository
import me.shadovxw.glyph.data.github.PrState
import me.shadovxw.glyph.data.github.PullRequest
import me.shadovxw.glyph.data.orders.Order
import me.shadovxw.glyph.data.orders.OrderStage
import me.shadovxw.glyph.data.orders.Orders
import me.shadovxw.glyph.tiles.TileOffers

/**
 * Debug builds only (not in release). `adb shell am broadcast -n me.shadovxw.glyph/.debug.DebugChatsReceiver --es mode fill`
 * puts clearly fake chats on the WhatsApp page so its layout can be checked; `--es mode clear` empties the list.
 */
class DebugChatsReceiver : BroadcastReceiver() {
    private fun fakeWeeks(seed: Int): List<List<Int>> =
        List(26) { week -> List(7) { day -> if ((week * 7 + day + seed) % 5 < 2) 0 else (week * day + seed) % 9 } }

    override fun onReceive(context: Context, intent: Intent) {
        Chats.init(context)
        val now = System.currentTimeMillis()
        val minute = 60_000L
        when (intent.getStringExtra("mode")) {
            "fill" -> Chats.debugReplace(
                listOf(
                    Chat("test-1", "TEST Alex", "This is a test message", now - 3 * minute, 3, false),
                    Chat("test-2", "TEST Family group", "Sam: test photo", now - 40 * minute, 12, true),
                    Chat("test-3", "TEST Priya", "Test: see you at 6", now - 3 * 60 * minute, 0, false),
                    Chat("test-4", "TEST Project team", "Lee: test build is ready", now - 26 * 60 * minute, 1, true),
                    Chat("test-5", "TEST Mom", "Test: call me", now - 50 * 60 * minute, 0, false),
                    Chat("test-6", "TEST Gym buddies", "Test: leg day", now - 4 * 24 * 60 * minute, 0, true),
                ),
            )
            "clear" -> Chats.debugReplace(emptyList())
            // Two clearly fake orders (no page shows orders now that the World page is gone).
            "orders-fill" -> Orders.debugReplace(
                listOf(
                    Order("Zomato", "TEST Kitchen", OrderStage.OnTheWay, 12, now),
                    Order("Blinkit", "", OrderStage.Preparing, 9, now),
                ),
            )
            "orders-clear" -> Orders.debugReplace(emptyList())
            // Fake GitHub accounts, PRs and a contribution pattern, to check the GitHub page without a token.
            "github-fill" -> GitHubRepository.current?.debugReplace(
                listOf("Personal", "Work"),
                GitHubData(
                    listOf(
                        AccountResult(
                            "Personal",
                            listOf(
                                PullRequest("Personal", "test/glyph", "TEST Add GitHub page", PrState.Open, CiStatus.Running, "https://github.com", true),
                                PullRequest("Personal", "test/dotfiles", "TEST Tidy shell config", PrState.Draft, CiStatus.None, "https://github.com", true),
                            ),
                            needReview = 0, openPrs = 2, weeks = fakeWeeks(3), error = null,
                        ),
                        AccountResult(
                            "Work",
                            listOf(
                                PullRequest("Work", "test/api", "TEST Fix retry on timeout", PrState.ReviewRequested, CiStatus.Passing, "https://github.com", false),
                                PullRequest("Work", "test/web", "TEST Upgrade build tooling", PrState.ChangesRequested, CiStatus.Failing, "https://github.com", true),
                                PullRequest("Work", "test/api", "TEST Add pagination", PrState.Approved, CiStatus.Passing, "https://github.com", true),
                            ),
                            needReview = 1, openPrs = 2, weeks = fakeWeeks(5), error = null,
                        ),
                    ),
                    now,
                ),
            )
            // The next time Glyph comes forward it offers the Quick Settings tiles again.
            "tiles-reset" -> TileOffers.reset(context)
            "github-clear" -> GitHubRepository.current?.debugReplace(emptyList(), null)
        }
    }
}
