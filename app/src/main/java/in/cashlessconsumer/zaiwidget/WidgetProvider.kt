package `in`.cashlessconsumer.zaiwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.RemoteViews
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) refresh(ctx, mgr, id)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) refreshAll(ctx)
    }

    private fun refreshAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        for (id in mgr.getAppWidgetIds(ComponentName(ctx, WidgetProvider::class.java))) refresh(ctx, mgr, id)
    }

    companion object {
        const val ACTION_REFRESH = "in.cashlessconsumer.zaiwidget.REFRESH"
        private const val CHECK_INTERVAL_MS = 24 * 3_600_000L

        fun refresh(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val key = Prefs.apiKey(ctx)
            if (key == null) {
                val cfg = Intent(ctx, SettingsActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                val v = placeholder(ctx, "tap to set key")
                v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(
                    ctx, id, cfg, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                mgr.updateAppWidget(id, v)
                return
            }
            mgr.updateAppWidget(id, placeholder(ctx, "refreshing…"))
            thread(name = "zai-widget-$id") {
                val now = System.currentTimeMillis()
                val cached = Prefs.loadView(ctx)
                if (!isOnline(ctx)) {
                    mgr.updateAppWidget(id, paint(ctx, cached, offline = true) ?: placeholder(ctx, "no network — tap to retry"))
                    return@thread
                }
                try {
                    val snap = ZaiApi.snapshot(key)
                    val v = Runway.view(snap)
                    Prefs.saveView(ctx, v, snap.fetchedAt)
                    var updateTag: String? = Prefs.updateTag(ctx)
                    if (now - Prefs.lastUpdateCheck(ctx) > CHECK_INTERVAL_MS) {
                        UpdateChecker.latest()?.let { (tag, url) -> Prefs.setUpdate(ctx, tag, url) }
                        Prefs.setLastUpdateCheck(ctx, now)
                        updateTag = Prefs.updateTag(ctx)
                    }
                    val current = versionName(ctx)
                    val newer = updateTag?.let { UpdateChecker.isNewer(current, it) } == true
                    mgr.updateAppWidget(id, paint(ctx, Pair(v, snap.fetchedAt), updateTag = if (newer) updateTag else null, updateUrl = Prefs.updateUrl(ctx)))
                } catch (e: UnknownHostException) {
                    mgr.updateAppWidget(id, paint(ctx, cached, offline = true) ?: placeholder(ctx, "offline: can't reach api.z.ai"))
                } catch (e: Exception) {
                    mgr.updateAppWidget(id, paint(ctx, cached, stale = true) ?: placeholder(ctx, "error: ${e.message?.take(60) ?: "unknown"}"))
                }
            }
        }

        /** Renders a (possibly stale) reading; offline/stale adds a marker instead of blanking the widget. */
        private fun paint(
            ctx: Context,
            cached: Pair<Runway.View, Long>?,
            offline: Boolean = false,
            stale: Boolean = false,
            updateTag: String? = null,
            updateUrl: String? = null,
        ): RemoteViews? {
            val (v, at) = cached ?: return null
            val views = RemoteViews(ZaiWidgetApp.PACKAGE, R.layout.widget_zai)
            val pct = v.fivePct ?: 0
            val now = System.currentTimeMillis()
            views.setImageViewBitmap(R.id.dial, Gauge.draw(pct, v.needleDegrees(now)))
            views.setTextViewText(R.id.big, "$pct%  ·  ${Runway.countDown(v.fiveResetAt, now) ?: "n/a"}")
            val eta = v.fiveEtaHours(now)
            val etaText = when {
                eta == null -> ""
                eta == Double.POSITIVE_INFINITY -> "no burn yet"
                eta <= 0.05 -> "dry — wait for reset"
                else -> "~${fmt(eta)} run-left at this pace"
            }
            val marker = when {
                offline -> " · offline"
                stale -> " · stale ${SimpleDateFormat("HH:mm", Locale.US).format(Date(at))}"
                else -> ""
            }
            views.setTextViewText(R.id.sub, (etaText + marker).trimStart(' ', '·'))
            val parts = mutableListOf<String>()
            v.weekPct?.let { parts.add("wk $it%") }
            v.monthlyPct?.let { parts.add("mo $it%") }
            v.toolsPct?.let { parts.add("tools $it%") }
            v.packsNote?.let { parts.add(it) }
            updateTag?.let { parts.add("⬆ $it — tap") }
            views.setTextViewText(R.id.row2, if (parts.isEmpty()) "5h window" else parts.joinToString(" · "))
            if (updateTag != null && updateUrl != null) {
                views.setOnClickPendingIntent(R.id.row2, ZaiWidgetApp.openUpdate(ctx, updateUrl))
            }
            return views
        }

        private fun placeholder(ctx: Context, msg: String): RemoteViews {
            val views = RemoteViews(ZaiWidgetApp.PACKAGE, R.layout.widget_zai)
            views.setTextViewText(R.id.big, "GLM")
            views.setTextViewText(R.id.sub, msg)
            views.setTextViewText(R.id.row2, "")
            views.setOnClickPendingIntent(R.id.root, pendingRefresh(ctx))
            return views
        }

        private fun versionName(ctx: Context): String = try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) { "0.0.0" }

        private fun isOnline(ctx: Context): Boolean {
            val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return true
            val net = cm.activeNetwork ?: return false
            return cm.getNetworkCapabilities(net)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }

        private fun fmt(h: Double): String {
            val totalMin = (h * 60).toInt()
            return if (totalMin / 60 > 0) "${totalMin / 60}h ${totalMin % 60}m" else "${totalMin}m"
        }

        private fun pendingRefresh(ctx: Context): PendingIntent =
            PendingIntent.getBroadcast(
                ctx, 0,
                Intent(ctx, WidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

object ZaiWidgetApp {
    const val PACKAGE = "in.cashlessconsumer.zaiwidget"
    fun openUpdate(ctx: Context, url: String): PendingIntent =
        PendingIntent.getActivity(
            ctx, 900_001,
            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
