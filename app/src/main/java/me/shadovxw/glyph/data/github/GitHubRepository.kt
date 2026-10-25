package me.shadovxw.glyph.data.github

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The GitHub page's data (DESIGN.md 3, screen 4). The accounts and their tokens come from `local.properties` through
 * `BuildConfig`, so nothing secret is in the source. With no token it stays empty and the page says how to add one.
 */
class GitHubRepository(accountsConfig: String, private val scope: CoroutineScope) {
    private val accounts = parseGitHubAccounts(accountsConfig)
    private val source = GitHubSource()

    private val _state = MutableStateFlow(GitHubState(configured = accounts.map { it.name }))
    val state: StateFlow<GitHubState> = _state.asStateFlow()

    private var job: Job? = null
    private var lastRefresh = 0L

    /** At most every two minutes, unless [force]. */
    fun refresh(force: Boolean = false) {
        if (accounts.isEmpty()) return
        val now = SystemClock.elapsedRealtime()
        if (!force && (job?.isActive == true || now - lastRefresh < MinRefreshMillis)) return
        lastRefresh = now
        job?.cancel()
        job = scope.launch {
            _state.update { it.copy(loading = true) }
            val results = coroutineScope { accounts.map { async { source.fetch(it) } }.awaitAll() }
            _state.update { it.copy(loading = false, data = GitHubData(results, System.currentTimeMillis())) }
        }
    }

    /** For checking the layout with no token; used by the debug build only. */
    fun debugReplace(configured: List<String>, data: GitHubData?) = _state.update { it.copy(configured = configured, data = data) }

    init {
        current = this
    }

    companion object {
        private const val MinRefreshMillis = 120_000L

        /** The live instance, so the debug receiver can reach it. */
        @Volatile
        var current: GitHubRepository? = null
            private set
    }
}
