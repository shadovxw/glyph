package me.shadovxw.glyph.data

import android.content.pm.ApplicationInfo

/** Drawer "Categories" sections (DESIGN.md 6), in display order. */
enum class DrawerCategory(val title: String) {
    Social("Social"),
    Ai("AI"),
    Work("Work"),
    Shopping("Shopping & food"),
    Games("Games"),
    Travel("Travel & local"),
    Media("Media"),
    Tools("Tools"),
}

private val Overrides: Map<String, DrawerCategory> = buildMap {
    listOf(
        "com.anthropic.claude", "com.openai.chatgpt", "com.google.android.apps.bard", "com.microsoft.copilot",
    ).forEach { put(it, DrawerCategory.Ai) }
    listOf(
        "in.swiggy.android", "com.application.zomato", "com.grofers.customerapp", "in.amazon.mShop.android.shopping",
        "com.amazon.mShop.android.shopping", "com.flipkart.android",
    ).forEach { put(it, DrawerCategory.Shopping) }
}

fun categoryOf(app: AppEntry): DrawerCategory {
    Overrides[app.packageName]?.let { return it }
    return when (app.androidCategory) {
        ApplicationInfo.CATEGORY_SOCIAL -> DrawerCategory.Social
        ApplicationInfo.CATEGORY_GAME -> DrawerCategory.Games
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> DrawerCategory.Work
        ApplicationInfo.CATEGORY_MAPS -> DrawerCategory.Travel
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE,
        ApplicationInfo.CATEGORY_NEWS -> DrawerCategory.Media
        else -> DrawerCategory.Tools
    }
}

/** Common apps shown as suggestions until there is launch history. */
private val DefaultSuggested = listOf(
    "com.android.chrome",
    "com.google.android.youtube",
    "com.whatsapp",
    "com.instagram.android",
    "com.google.android.apps.maps",
    "com.google.android.gm",
    "com.google.android.apps.nbu.paisa.user",
    "com.microsoft.teams",
    "com.microsoft.office.outlook",
    "com.google.android.apps.authenticator2",
    "com.google.android.apps.photos",
)

/** The 10 most launched apps, topped up with [DefaultSuggested] that are installed. */
fun suggestedApps(apps: List<AppEntry>, launchCounts: Map<String, Int>): List<AppEntry> {
    val byCount = apps
        .filter { (launchCounts[it.packageName] ?: 0) > 0 }
        .sortedByDescending { launchCounts[it.packageName] }
    val fallback = DefaultSuggested.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    return (byCount + fallback).distinctBy { it.packageName }.take(10)
}
