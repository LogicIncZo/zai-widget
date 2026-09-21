package `in`.cashlessconsumer.zaiwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlin.concurrent.thread

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) refresh(ctx, mgr, id)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, WidgetProvider::class.java))
            for (id in ids) refresh(ctx, mgr, id)
        }
    }

    private fun refresh(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = placeholder(ctx, "…")
        mgr.updateAppWidget(id, views)
        val key = Prefs.apiKey(ctx)
        if (key == null) {
            mgr.updateAppWidget(id, placeholder(ctx, "tap to set key"))
            return
        }
        thread(name = "zai-widget-$id") {
            val now = System.currentTimeMillis()
            try {
                val v = Runway.view(ZaiApi.snapshot(key))
                mgr.updateAppWidget(id, render(ctx, v, now))
            } catch (e: Exception) {
                mgr.updateAppWidget(id, placeholder(ctx, "error: ${e.message?.take(60) ?: "unknown"}"))
            }
        }
    }

    private fun render(ctx: Context, v: Runway.View, now: Long): RemoteViews {
        val views = RemoteViews(ctx.packageName, R.layout.widget_zai)
        val pct = v.fivePct ?: 0
        views.setImageViewBitmap(R.id.dial, Gauge.draw(pct, v.needleDegrees(now)))
        views.setTextViewText(R.id.big, "$pct%  ·  ${Runway.countDown(v.fiveResetAt, now) ?: "n/a"}")
        val eta = v.fiveEtaHours(now)
        val etaText = when {
            eta == null -> ""
            eta == Double.POSITIVE_INFINITY -> "no burn yet"
            eta <= 0.05 -> "dry — wait for reset"
            else -> "~${formatHours(eta)} run-left at this pace"
        }
        views.setTextViewText(R.id.sub, etaText)
        val parts = mutableListOf<String>()
        v.weekPct?.let { parts.add("week $it%") }
        v.monthlyPct?.let { parts.add("month $it%") }
        v.toolsPct?.let { parts.add("tools $it%") }
        v.packsNote?.let { parts.add(it) }
        views.setTextViewText(R.id.row2, if (parts.isEmpty()) "5h window" else parts.joinToString(" · "))
        views.setOnClickPendingIntent(R.id.root, pendingRefresh(ctx))
        return views
    }

    private fun placeholder(ctx: Context, msg: String): RemoteViews {
        val views = RemoteViews(ctx.packageName, R.layout.widget_zai)
        views.setTextViewText(R.id.big, "GLM")
        views.setTextViewText(R.id.sub, msg)
        views.setTextViewText(R.id.row2, "")
        views.setOnClickPendingIntent(R.id.root, pendingRefresh(ctx))
        return views
    }

    private fun formatHours(h: Double): String {
        val totalMin = (h * 60).toInt()
        val hh = totalMin / 60
        val mm = totalMin % 60
        return if (hh > 0) "${hh}h ${mm}m" else "${mm}m"
    }

    private fun pendingRefresh(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 0,
            Intent(ctx, WidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_REFRESH = "in.cashlessconsumer.zaiwidget.REFRESH"
    }
}
