package `in`.cashlessconsumer.zaiwidget

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

object Prefs {
    private const val FILE = "zai_widget_prefs"

    private fun secure(ctx: Context) = EncryptedSharedPreferences.create(
        ctx, FILE,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun apiKey(ctx: Context): String? =
        secure(ctx).getString("api_key", null)?.takeIf { it.isNotBlank() }

    fun saveKey(ctx: Context, key: String) {
        secure(ctx).edit().putString("api_key", key.trim()).apply()
    }

    private fun cache(ctx: Context) = ctx.getSharedPreferences("zai_widget_cache", Context.MODE_PRIVATE)

    /** Last successful reading, kept so transient failures show stale data instead of an error. */
    fun saveView(ctx: Context, v: Runway.View, fetchedAt: Long) {
        cache(ctx).edit().putString(
            "last_good",
            JSONObject()
                .put("fivePct", v.fivePct ?: -1)
                .put("fiveResetAt", v.fiveResetAt ?: -1)
                .put("weekPct", v.weekPct ?: -1)
                .put("monthlyPct", v.monthlyPct ?: -1)
                .put("toolsPct", v.toolsPct ?: -1)
                .put("packsNote", v.packsNote ?: "")
                .put("fetchedAt", fetchedAt)
                .toString(),
        ).apply()
    }

    fun loadView(ctx: Context): Pair<Runway.View, Long>? = try {
        val raw = cache(ctx).getString("last_good", null) ?: return null
        val o = JSONObject(raw)
        fun pct(k: String) = o.getInt(k).takeIf { it >= 0 }
        fun str(k: String) = o.optString(k, "").ifEmpty { null }
        Runway.View(
            fivePct = pct("fivePct"),
            fiveResetAt = o.getLong("fiveResetAt").takeIf { it >= 0 },
            weekPct = pct("weekPct"),
            monthlyPct = pct("monthlyPct"),
            toolsPct = pct("toolsPct"),
            packsNote = str("packsNote"),
        ) to o.getLong("fetchedAt")
    } catch (e: Exception) {
        null
    }

    fun lastUpdateCheck(ctx: Context): Long = cache(ctx).getLong("update_check_at", 0)
    fun setLastUpdateCheck(ctx: Context, at: Long) = cache(ctx).edit().putLong("update_check_at", at).apply()

    fun showClock(ctx: Context): Boolean = cache(ctx).getBoolean("show_clock", true)
    fun setShowClock(ctx: Context, v: Boolean) = cache(ctx).edit().putBoolean("show_clock", v).apply()

    fun showStats(ctx: Context): Boolean = cache(ctx).getBoolean("show_stats", true)
    fun setShowStats(ctx: Context, v: Boolean) = cache(ctx).edit().putBoolean("show_stats", v).apply()

    fun updateTag(ctx: Context): String? = cache(ctx).getString("update_tag", null)?.ifEmpty { null }
    fun updateUrl(ctx: Context): String = cache(ctx).getString("update_url", null) ?: UpdateChecker.RELEASES_URL
    fun setUpdate(ctx: Context, tag: String?, url: String) =
        cache(ctx).edit().putString("update_tag", tag ?: "").putString("update_url", url).apply()
}
