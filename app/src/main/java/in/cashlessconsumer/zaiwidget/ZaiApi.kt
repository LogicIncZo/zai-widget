package `in`.cashlessconsumer.zaiwidget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiException(message: String) : Exception(message)

object ZaiApi {
    private const val BASE = "https://api.z.ai/api"

    private fun get(path: String, key: String): JSONObject {
        val conn = URL("$BASE$path").openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        conn.setRequestProperty("Authorization", "Bearer $key")
        try {
            if (conn.responseCode !in 200..299) throw ApiException("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(body)
            if (!obj.optBoolean("success", false)) throw ApiException(obj.optString("msg", "API error"))
            return obj
        } finally {
            conn.disconnect()
        }
    }

    fun snapshot(key: String): Snapshot {
        val data = get("/monitor/usage/quota/limit", key).getJSONObject("data")
        val arr = data.optJSONArray("limits")
        val limits = if (arr == null) emptyList() else (0 until arr.length()).map { i ->
            val l = arr.getJSONObject(i)
            Limit(
                unit = l.optInt("unit"),
                percentage = l.optInt("percentage"),
                nextResetTime = if (l.has("nextResetTime")) l.optLong("nextResetTime") else null,
            )
        }
        val resets = try {
            val r = get("/biz/customer-package-reset/list?targetType=PERSONAL", key).getJSONObject("data")
            Resets(
                fiveHourResets = packs(r, "fiveHourResets"),
                weekResets = packs(r, "weekResets"),
            )
        } catch (e: Exception) {
            Resets(emptyList(), emptyList())
        }
        return Snapshot(Quota(limits), resets, System.currentTimeMillis())
    }

    private fun packs(data: JSONObject, field: String): List<ResetPack> {
        val arr = data.optJSONArray(field) ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ResetPack(available = o.optBoolean("available"), expireTime = Runway.parseZ8(o.optString("expireTime", "")))
        }
    }
}
