package me.shadovxw.glyph.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.shadovxw.glyph.data.github.AccountResult
import me.shadovxw.glyph.data.github.CiStatus
import me.shadovxw.glyph.data.github.GitHubActions
import me.shadovxw.glyph.data.github.GitHubState
import me.shadovxw.glyph.data.github.PullRequest
import me.shadovxw.glyph.launcher.glyphCard
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.theme.GlyphIcons
import me.shadovxw.glyph.theme.GlyphType
import me.shadovxw.glyph.theme.bottomFade
import kotlin.math.ceil

/**
 * Screen 4 (DESIGN.md 3): GitHub across your accounts. A header, account chips (All / each account), three stats,
 * the open and review-requested pull requests with their check results, and the contribution grid. The chips filter
 * everything below them. Real data from GitHub's API, one token per account in `local.properties`.
 */
@Composable
fun GitHubScreen(state: GitHubState, actions: GitHubActions) {
    var selected by remember { mutableStateOf<String?>(null) } // null = All
    val results = state.data?.accounts.orEmpty().filter { selected == null || it.name == selected }
    val prs = results.flatMap { it.prs }.sortedBy { it.state.ordinal }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .bottomFade()
            .verticalScroll(rememberScrollState())
            .padding(
                start = GlyphDimens.screenSide,
                end = GlyphDimens.screenSide,
                top = GlyphDimens.screenTop,
                bottom = GlyphDimens.homeBottomClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(GlyphDimens.cardGap * 2),
    ) {
        Header(state, Modifier.glyphCard(0))

        if (state.configured.isEmpty()) {
            Notice(
                "Add a GitHub token",
                "In local.properties add  github.accounts=Personal:TOKEN,Work:TOKEN  and rebuild.",
                Modifier.glyphCard(1),
            )
            return@Column
        }

        Chips(listOf<String?>(null) + state.configured, selected, { selected = it }, Modifier.glyphCard(1))

        results.filter { it.error != null }.forEach { account ->
            BasicText(
                "${account.name}: ${account.error}",
                style = GlyphType.rowSecondary.copy(color = Glyph.colors.inkDim),
            )
        }

        Stats(results, Modifier.glyphCard(2))

        Column(Modifier.glyphCard(3)) {
            BasicText("Pull requests", style = GlyphType.label.copy(color = Glyph.colors.inkDim), modifier = Modifier.padding(bottom = 6.dp))
            when {
                state.data == null -> BasicText(if (state.loading) "Loading…" else "Nothing yet", style = GlyphType.rowSecondary.copy(color = Glyph.colors.inkDim))
                prs.isEmpty() -> BasicText("No open pull requests", style = GlyphType.rowSecondary.copy(color = Glyph.colors.inkDim))
                else -> prs.forEach { PullRequestRow(it) { actions.openUrl(it.url) } }
            }
        }

        if (results.any { it.weeks.isNotEmpty() }) {
            Column(Modifier.glyphCard(4)) {
                BasicText("Contributions", style = GlyphType.label.copy(color = Glyph.colors.inkDim), modifier = Modifier.padding(bottom = 8.dp))
                ContributionGrid(results)
            }
        }
    }
}

@Composable
private fun Header(state: GitHubState, modifier: Modifier) {
    val c = Glyph.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(GlyphIcons.Branch, contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(c.ink))
        Spacer(Modifier.size(8.dp))
        BasicText("GitHub", style = GlyphType.screenTitle.copy(color = c.ink), modifier = Modifier.weight(1f))
        val synced = state.data?.syncedAt
        BasicText(
            when {
                state.loading -> "syncing…"
                synced != null -> "synced ${ago(synced)}"
                else -> ""
            },
            style = GlyphType.meta.copy(color = c.inkDim),
        )
    }
}

private fun ago(millis: Long): String {
    val minutes = ((System.currentTimeMillis() - millis) / 60_000).toInt()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60}h ago"
    }
}

/** Account chips, 32 dp tall; the active one is filled. */
@Composable
private fun Chips(options: List<String?>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier) {
    val c = Glyph.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .height(32.dp)
                    .background(if (on) c.chipBg else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(option ?: "All", style = GlyphType.rowPrimary.copy(color = if (on) c.chipText else c.ink))
            }
        }
    }
}

/** Open PRs, PRs that need your review, and open PRs of yours whose checks are failing. */
@Composable
private fun Stats(results: List<AccountResult>, modifier: Modifier) {
    val open = results.sumOf { it.openPrs }
    val review = results.sumOf { it.needReview }
    val failing = results.sumOf { account -> account.prs.count { it.mine && it.ci == CiStatus.Failing } }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GlyphDimens.cardGap)) {
        Stat("Open PRs", open, Modifier.weight(1f))
        Stat("Need your review", review, Modifier.weight(1f))
        Stat("Failing checks", failing, Modifier.weight(1f))
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier) {
    val c = Glyph.colors
    Column(modifier) {
        BasicText(value.toString(), style = GlyphType.mediumStat.copy(color = c.ink))
        BasicText(label, style = GlyphType.label.copy(color = c.inkDim))
    }
}

/** 52 dp: account tag square, the repository in mono, the title, the state, and the checks icon. */
@Composable
private fun PullRequestRow(pr: PullRequest, onClick: () -> Unit) {
    val c = Glyph.colors
    Row(
        Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp).background(c.chipBg), contentAlignment = Alignment.Center) {
            BasicText(pr.account.first().uppercaseChar().toString(), style = GlyphType.rowPrimary.copy(color = c.chipText))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(pr.repo, style = GlyphType.meta.copy(color = c.inkDim), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(pr.title, style = GlyphType.rowPrimary.copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(pr.state.label, style = GlyphType.rowSecondary.copy(color = c.inkDim))
        }
        val icon = when (pr.ci) {
            CiStatus.Passing -> GlyphIcons.Check
            CiStatus.Failing -> GlyphIcons.Cross
            CiStatus.Running -> GlyphIcons.Running
            CiStatus.None -> null
        }
        if (icon != null) {
            Image(icon, contentDescription = pr.ci.name, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(c.ink))
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.inkLine))
}

/** The last 22 weeks, 7 days each, 10 dp cells with a 3 dp gap, five ink levels. Several accounts are added together. */
@Composable
private fun ContributionGrid(results: List<AccountResult>) {
    val c = Glyph.colors
    val weekCount = 22
    val grid: List<List<Int>> = remember(results) {
        val longest = results.maxOf { it.weeks.size }
        ((longest - weekCount).coerceAtLeast(0) until longest).map { w ->
            (0 until 7).map { d -> results.sumOf { it.weeks.getOrNull(w)?.getOrNull(d) ?: 0 } }
        }
    }
    val max = (grid.flatten().maxOrNull() ?: 0).coerceAtLeast(1)
    val levels = listOf(0.10f, 0.30f, 0.50f, 0.72f, 1f)
    Canvas(Modifier.fillMaxWidth().height((7 * 10 + 6 * 3).dp)) {
        val cell = 10.dp.toPx()
        val gap = 3.dp.toPx()
        grid.forEachIndexed { week, days ->
            days.forEachIndexed { day, count ->
                val level = if (count == 0) 0 else ceil(4f * count / max).toInt().coerceIn(1, 4)
                drawRect(
                    color = c.ink.copy(alpha = levels[level]),
                    topLeft = Offset(week * (cell + gap), day * (cell + gap)),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

@Composable
private fun Notice(title: String, detail: String, modifier: Modifier) {
    val c = Glyph.colors
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        BasicText(title, style = GlyphType.screenTitle.copy(color = c.ink))
        BasicText(detail, style = GlyphType.rowSecondary.copy(color = c.inkDim))
    }
}
