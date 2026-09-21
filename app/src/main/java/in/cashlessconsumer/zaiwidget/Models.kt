package `in`.cashlessconsumer.zaiwidget

data class Limit(val unit: Int, val percentage: Int, val nextResetTime: Long?)
data class Quota(val limits: List<Limit>)
data class ResetPack(val available: Boolean, val expireTime: Long?)
data class Resets(val fiveHourResets: List<ResetPack>, val weekResets: List<ResetPack>)
data class Snapshot(val quota: Quota, val resets: Resets, val fetchedAt: Long)
