package me.shadovxw.glyph.data.github

import androidx.compose.runtime.Immutable

enum class PrState(val label: String) {
    ReviewRequested("Review requested"),
    ChangesRequested("Changes requested"),
    Approved("Approved"),
    Draft("Draft"),
    Open("Open"),
}

/** The result of a pull request's checks: a tick, a cross, or an open arc while they run. */
enum class CiStatus { Passing, Failing, Running, None }

@Immutable
class PullRequest(
    /** The account name it came from, as written in `local.properties`. */
    val account: String,
    /** `owner/name`. */
    val repo: String,
    val title: String,
    val state: PrState,
    val ci: CiStatus,
    val url: String,
    /** True when you opened it; false when you were asked to review it. */
    val mine: Boolean,
)

/** What one account looked like on the last refresh. [error] is set when GitHub refused the token or was unreachable. */
@Immutable
class AccountResult(
    val name: String,
    val prs: List<PullRequest>,
    /** Pull requests waiting for your review (the true count, even when only some are listed). */
    val needReview: Int,
    /** Open pull requests you opened (the true count). */
    val openPrs: Int,
    /** Contribution counts, oldest week first, seven days per week (Sunday first). */
    val weeks: List<List<Int>>,
    val error: String?,
)

@Immutable
class GitHubData(val accounts: List<AccountResult>, val syncedAt: Long)

@Immutable
data class GitHubState(
    /** The account names configured in `local.properties`; empty when there is no token. */
    val configured: List<String> = emptyList(),
    val loading: Boolean = false,
    val data: GitHubData? = null,
)

@Immutable
class GitHubActions(val openUrl: (String) -> Unit, val refresh: () -> Unit) {
    companion object {
        val None = GitHubActions({}, {})
    }
}
