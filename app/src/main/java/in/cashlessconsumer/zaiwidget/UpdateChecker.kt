package `in`.cashlessconsumer.zaiwidget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    const val REPO = "LogicIncZo/zai-widget"
    const val RELEASES_URL = "https://github.com/$REPO/releases"

    /** Semver-ish compare of "0.1.1" vs "v0.2.0" — true when latest is strictly newer. */
    fun isNewer(current: String, latestTag: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").removePrefix("V").split(".").map { it.trim().toIntOrNull() ?: 0 }
        val c = parts(current)
        val l = parts(latestTag)
        for (i in 0 until maxOf(c.size, l.size)) {
            val cv = c.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    /** Latest GitHub release as (tag, html_url); null when none or unreachable. */
    fun latest(): Pair<String, String>? {
        val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout = 6_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "zai-widget")
        try {
            if (conn.responseCode !in 200..299) return null
            val o = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = o.optString("tag_name", "").ifEmpty { return null }
            val url = o.optString("html_url", RELEASES_URL)
            return tag to url
        } catch (e: Exception) {
            return null
        } finally {
            conn.disconnect()
        }
    }
}
