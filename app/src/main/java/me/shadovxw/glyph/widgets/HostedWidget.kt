package me.shadovxw.glyph.widgets

import android.appwidget.AppWidgetHostView
import android.view.View
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Shows a hosted widget filling the space it is given, and tells it that size so it can lay itself out. */
@Composable
fun HostedWidget(controller: WidgetHostController, id: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val widthDp = maxWidth.value.toInt()
        val heightDp = maxHeight.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            // A fresh view each time the page comes into composition: a View can only have one parent.
            factory = { context -> controller.createView(id) ?: View(context) },
            update = { (it as? AppWidgetHostView)?.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp) },
        )
    }
}
