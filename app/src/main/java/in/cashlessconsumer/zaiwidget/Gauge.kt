package `in`.cashlessconsumer.zaiwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** Clock-face gauge: ring = % of 5h quota used, needle = time through the window. */
object Gauge {
    private const val TRACK = 0xFF21262D.toInt()
    private const val NEEDLE = 0xFFE6EDF3.toInt()
    private const val HUB = 0xFF8B949E.toInt()
    private const val GREEN = 0xFF3FB950.toInt()
    private const val AMBER = 0xFFD29922.toInt()
    private const val RED = 0xFFF85149.toInt()

    fun colorFor(pct: Int): Int = when {
        pct >= 80 -> RED
        pct >= 50 -> AMBER
        else -> GREEN
    }

    fun draw(pct: Int, needleDegrees: Float, sizePx: Int = 512): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val r = sizePx * 0.42f
        val stroke = sizePx * 0.075f

        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = stroke; color = TRACK; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(cx, cx, r, track)

        val filled = pct.coerceIn(0, 100)
        if (filled > 0) {
            val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = stroke; color = colorFor(filled); strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(RectF(cx - r, cx - r, cx + r, cx + r), -90f, filled * 3.6f, false, arc)
        }

        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = sizePx * 0.012f; color = HUB
        }
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30).toDouble())
            val inner = r - stroke * 1.6f
            val outer = r - stroke * 1.1f
            canvas.drawLine(
                cx + (inner * Math.sin(a)).toFloat(), cx - (inner * Math.cos(a)).toFloat(),
                cx + (outer * Math.sin(a)).toFloat(), cx - (outer * Math.cos(a)).toFloat(), tick,
            )
        }

        val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = sizePx * 0.02f; color = NEEDLE; strokeCap = Paint.Cap.ROUND
        }
        val na = Math.toRadians(needleDegrees.toDouble())
        val needleR = r - stroke * 2.2f
        canvas.drawLine(cx, cx, cx + (needleR * Math.sin(na)).toFloat(), cx - (needleR * Math.cos(na)).toFloat(), needle)

        val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = HUB }
        canvas.drawCircle(cx, cx, sizePx * 0.025f, hub)
        return bmp
    }
}
