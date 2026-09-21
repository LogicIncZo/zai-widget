package `in`.cashlessconsumer.zaiwidget

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object Prefs {
    private const val FILE = "zai_widget_prefs"

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx, FILE,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun apiKey(ctx: Context): String? =
        prefs(ctx).getString("api_key", null)?.takeIf { it.isNotBlank() }

    fun saveKey(ctx: Context, key: String) {
        prefs(ctx).edit().putString("api_key", key.trim()).apply()
    }
}
