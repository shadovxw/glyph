package me.shadovxw.glyph.data.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GitHubAccount(val name: String, val token: String)

/**
 * Reads "name:token,name:token" (the `github.accounts` line of `local.properties`, which the build copies into
 * `BuildConfig`). The token is everything after the first colon, so it can contain colons.
 */
fun parseGitHubAccounts(raw: String): List<GitHubAccount> =
    raw.split(',').mapNotNull { entry ->
        val name = entry.substringBefore(':', "").trim()
        val token = entry.substringAfter(':', "").trim()
        if (name.isNotEmpty() && token.isNotEmpty()) GitHubAccount(name, token) else null
    }

/** GitHub's GraphQL API, one request per account: open pull requests, review requests, check results, contributions. */
class GitHubSource {
    suspend fun fetch(account: GitHubAccount): AccountResult = withContext(Dispatchers.IO) {
        try {
            parse(account.name, post(account.token))
        } catch (e: Exception) {
            AccountResult(account.name, emptyList(), 0, 0, emptyList(), e.message ?: "Could not reach GitHub")
        }
    }

    private fun post(token: String): String {
        val connection = URL("https://api.github.com/graphql").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Glyph")
            connection.outputStream.use { it.write(JSONObject().put("query", Query).toString().toByteArray()) }
            val code = connection.responseCode
            if (code == 401) error("GitHub rejected this token")
            check(code == 200) { "GitHub answered $code" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val PrFields = """
            title url isDraft reviewDecision
            repository { nameWithOwner }
            commits(last: 1) { nodes { commit { statusCheckRollup { state } } } }
        """

        private val Query = """
            query {
              viewer {
                contributionsCollection { contributionCalendar { weeks { contributionDays { contributionCount } } } }
              }
              authored: search(query: "is:pr is:open author:@me archived:false", type: ISSUE, first: 30) {
                issueCount
                nodes { ... on PullRequest { $PrFields } }
              }
              requested: search(query: "is:pr is:open review-requested:@me archived:false", type: ISSUE, first: 30) {
                issueCount
                nodes { ... on PullRequest { $PrFields } }
              }
            }
        """.trimIndent()

        fun parse(account: String, json: String): AccountResult {
            val root = JSONObject(json)
            root.optJSONArray("errors")?.let { errors ->
                if (errors.length() > 0) error(errors.getJSONObject(0).optString("message", "GitHub reported an error"))
            }
            val data = root.getJSONObject("data")
            val authored = data.getJSONObject("authored")
            val requested = data.getJSONObject("requested")

            val prs = pullRequests(account, authored.getJSONArray("nodes"), mine = true) +
                pullRequests(account, requested.getJSONArray("nodes"), mine = false)

            val weeks = data.getJSONObject("viewer").getJSONObject("contributionsCollection")
                .getJSONObject("contributionCalendar").getJSONArray("weeks")
                .let { array ->
                    (0 until array.length()).map { w ->
                        val days = array.getJSONObject(w).getJSONArray("contributionDays")
                        (0 until days.length()).map { d -> days.getJSONObject(d).getInt("contributionCount") }
                    }
                }
            return AccountResult(
                name = account,
                prs = prs,
                needReview = requested.getInt("issueCount"),
                openPrs = authored.getInt("issueCount"),
                weeks = weeks,
                error = null,
            )
        }

        private fun pullRequests(account: String, nodes: JSONArray, mine: Boolean): List<PullRequest> =
            (0 until nodes.length()).mapNotNull { i ->
                val node = nodes.optJSONObject(i)?.takeIf { it.has("title") } ?: return@mapNotNull null
                val rollup = node.optJSONObject("commits")?.optJSONArray("nodes")?.optJSONObject(0)
                    ?.optJSONObject("commit")?.optJSONObject("statusCheckRollup")?.optString("state")
                val state = when {
                    !mine -> PrState.ReviewRequested
                    node.optBoolean("isDraft") -> PrState.Draft
                    node.optString("reviewDecision") == "CHANGES_REQUESTED" -> PrState.ChangesRequested
                    node.optString("reviewDecision") == "APPROVED" -> PrState.Approved
                    else -> PrState.Open
                }
                val ci = when (rollup) {
                    "SUCCESS" -> CiStatus.Passing
                    "FAILURE", "ERROR" -> CiStatus.Failing
                    "PENDING", "EXPECTED" -> CiStatus.Running
                    else -> CiStatus.None
                }
                PullRequest(
                    account = account,
                    repo = node.getJSONObject("repository").getString("nameWithOwner"),
                    title = node.getString("title"),
                    state = state,
                    ci = ci,
                    url = node.getString("url"),
                    mine = mine,
                )
            }
    }
}
