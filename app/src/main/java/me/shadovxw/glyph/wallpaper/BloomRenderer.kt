package me.shadovxw.glyph.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.random.Random

/**
 * DESIGN.md 5B, "Bloom and wind". Everything still (dots, folds, lines, arc, thread, red dot) is
 * drawn once into [still]; each frame only blits it and draws the two moving layers (rain, dust).
 *
 * Positions in the spec are on a 390 x 844 frame and are scaled to the screen. All randomness is
 * seeded once, and nothing is allocated per frame. Call only from the main thread.
 */
class BloomRenderer(private val context: Context) {
    private class Drop(val x: Float, val length: Float, val durationMs: Float, val phase: Float, val shader: Shader)
    private class Mote(val x: Float, val radius: Float, val durationMs: Float, val phase: Float, val alpha: Int)

    private var width = 0
    private var height = 0
    private var dark = true
    private var still: Bitmap? = null
    private var drops: List<Drop> = emptyList()
    private var motes: List<Mote> = emptyList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun resize(width: Int, height: Int) {
        if (width == this.width && height == this.height) return
        this.width = width
        this.height = height
        rebuild()
    }

    fun setDark(dark: Boolean) {
        if (dark == this.dark && still != null) return
        this.dark = dark
        rebuild()
    }

    fun draw(canvas: Canvas, timeMs: Long) {
        val layer = still
        if (layer == null) {
            canvas.drawColor(background())
            return
        }
        canvas.drawBitmap(layer, 0f, 0f, null)
        drawRain(canvas, timeMs)
        drawDust(canvas, timeMs)
    }

    private fun background() = if (dark) 0xFF000000.toInt() else 0xFFF6F6F6.toInt()
    private fun ink() = if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    private fun inkAlpha(alpha: Float) = (ink() and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)

    private fun rebuild() {
        if (width <= 0 || height <= 0) return
        still?.recycle()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawStill(Canvas(bitmap))
        still = bitmap
        seedMovingLayers()
    }

    // ---- still layers ----------------------------------------------------------------------

    private fun drawStill(c: Canvas) {
        val s = width / FRAME_W   // size scale
        val sy = height / FRAME_H // vertical position scale
        c.drawColor(background())

        // Dot grid: r 0.8 on a 30 grid, ink @ 12%.
        paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.FILL; paint.color = inkAlpha(0.12f)
        var y = 0f
        while (y <= FRAME_H) {
            var x = 0f
            while (x <= FRAME_W) {
                c.drawCircle(x * s, y * sy, 0.8f * s, paint)
                x += 30f
            }
            y += 30f
        }

        // Paper folds: faint triangles, fill 4%, stroke 0.6 @ 25%.
        fold(c, s, sy, 390f, 110f, 248f, 300f, 390f, 520f)
        fold(c, s, sy, 0f, 470f, 170f, 640f, 0f, 800f)

        // Vertical line x = 232 from the top to y = 330, 0.7 @ 55%, with three dots on it.
        paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.7f * s; paint.color = inkAlpha(0.55f)
        c.drawLine(232f * s, 0f, 232f * s, 330f * sy, paint)
        paint.style = Paint.Style.FILL; paint.color = inkAlpha(0.8f)
        c.drawCircle(232f * s, 64f * sy, 2.6f * s, paint)
        c.drawCircle(232f * s, 170f * sy, 2f * s, paint)
        c.drawCircle(232f * s, 248f * sy, 1.4f * s, paint)

        // Dotted arc: quad from (40,205) via (160,170) to (232,300), dash 1/5 @ 60%.
        val arc = Path().apply {
            moveTo(40f * s, 205f * sy)
            quadTo(160f * s, 170f * sy, 232f * s, 300f * sy)
        }
        paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 1f * s; paint.color = inkAlpha(0.6f)
        paint.pathEffect = DashPathEffect(floatArrayOf(1f * s, 5f * s), 0f)
        c.drawPath(arc, paint)

        // Wind thread along the lower right edge, dash 3/5 @ 45%. (Exact curve is not in the spec.)
        val wind = Path().apply {
            moveTo(210f * s, 800f * sy)
            cubicTo(280f * s, 740f * sy, 330f * s, 790f * sy, 390f * s, 700f * sy)
        }
        paint.strokeWidth = 0.9f * s; paint.color = inkAlpha(0.45f)
        paint.pathEffect = DashPathEffect(floatArrayOf(3f * s, 5f * s), 0f)
        c.drawPath(wind, paint)

        // No lotus: it was removed from the wallpaper. The lotus_light / lotus_dark drawables are kept for later.

        // Red dot: 9 wide at (244, 380). The only colour in the design.
        paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.FILL; paint.color = 0xFFD7262E.toInt()
        c.drawCircle(244f * s, 380f * sy, 4.5f * s, paint)
    }

    private fun fold(c: Canvas, s: Float, sy: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val path = Path().apply {
            moveTo(x1 * s, y1 * sy); lineTo(x2 * s, y2 * sy); lineTo(x3 * s, y3 * sy); close()
        }
        paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.FILL; paint.color = inkAlpha(0.04f)
        c.drawPath(path, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.6f * s; paint.color = inkAlpha(0.25f)
        c.drawPath(path, paint)
    }

    // ---- moving layers ---------------------------------------------------------------------

    private fun seedMovingLayers() {
        val s = width / FRAME_W
        val random = Random(SEED)
        // Rain: 12 lines, 1 wide, 50-180 tall, transparent -> ink, 2.5-7.5 s per fall.
        drops = List(12) {
            val length = (50f + random.nextFloat() * 130f) * s
            Drop(
                x = random.nextFloat() * width,
                length = length,
                durationMs = 2_500f + random.nextFloat() * 5_000f,
                phase = random.nextFloat(),
                shader = LinearGradient(0f, 0f, 0f, length, 0x00000000, ink(), Shader.TileMode.CLAMP),
            )
        }
        // Dust: 28 dots, 1.5-4.5 across, rising the full height in 12-34 s, opacity 0.2-0.8 by size.
        motes = List(28) {
            val size = 1.5f + random.nextFloat() * 3f
            val opacity = 0.2f + 0.6f * (size - 1.5f) / 3f
            Mote(
                x = random.nextFloat() * width,
                radius = size / 2f * s,
                durationMs = 12_000f + random.nextFloat() * 22_000f,
                phase = random.nextFloat(),
                alpha = (opacity * DUST_LAYER_ALPHA * 255).toInt(),
            )
        }
    }

    private fun drawRain(canvas: Canvas, timeMs: Long) {
        val lineWidth = width / FRAME_W
        paint.reset(); paint.alpha = (RAIN_LAYER_ALPHA * 255).toInt()
        for (drop in drops) {
            val progress = (timeMs / drop.durationMs + drop.phase) % 1f
            val top = -drop.length + progress * (height + drop.length)
            canvas.save()
            canvas.translate(drop.x, top)
            paint.shader = drop.shader
            canvas.drawRect(0f, 0f, lineWidth, drop.length, paint)
            canvas.restore()
        }
        paint.shader = null
    }

    private fun drawDust(canvas: Canvas, timeMs: Long) {
        paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.FILL; paint.color = ink()
        for (mote in motes) {
            val progress = (timeMs / mote.durationMs + mote.phase) % 1f
            val y = height + mote.radius - progress * (height + 2f * mote.radius)
            paint.alpha = mote.alpha
            canvas.drawCircle(mote.x, y, mote.radius, paint)
        }
    }

    private companion object {
        const val FRAME_W = 390f
        const val FRAME_H = 844f
        const val SEED = 20260825
        const val RAIN_LAYER_ALPHA = 0.30f
        const val DUST_LAYER_ALPHA = 0.70f
    }
}
