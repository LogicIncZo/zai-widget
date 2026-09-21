package `in`.cashlessconsumer.zaiwidget

import android.content.Context

object Prefs {
    private const val FILE = "zai_widget"

    fun apiKey(ctx: Context): String? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("api_key", null)?.takeIf { it.isNotBlank() }

    fun setApiKey(ctx: Context, key: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("api_key", key.trim()).apply()
    }
}
