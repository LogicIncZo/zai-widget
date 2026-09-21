# zai-widget — project notes

Android clock-style home-screen widget for Z.ai GLM Coding Plan quota/runway. Companion to `Projects/zai-usage/` (CLI). Package `in.cashlessconsumer.zaiwidget` — K2 requires backticked `` `in` `` in package/import paths (same gotcha as zo-voice; applicationId/namespace stay plain strings).

## Ground-truth API facts (verified 2026-09-21 against live API via zai-usage)

- Auth: `Authorization: Bearer <GLM coding-plan key>` on `https://api.z.ai`.
- `GET /api/monitor/usage/quota/limit` → `data.limits[]` with `unit` (3 = 5-hour tokens, 6 = weekly tokens, 4 = monthly tokens, 5 = tool calls), `percentage`, `nextResetTime` (epoch ms = end of current 5h window).
- `GET /api/biz/customer-package-reset/list?targetType=PERSONAL` → `data.lastFiveHourResetTime` ("yyyy-MM-dd HH:mm:ss" UTC+8), `fiveHourResets`/`weekResets` arrays (`available`, `expireTime`).
- Phone calls these directly — no proxy, key stays on device in EncryptedSharedPreferences.

## Widget model

- Ring = % quota used; needle = time through the 5h window (up = reset imminent). Texts: 5h %, reset countdown (Z8 → remaining), week %, tools %, packs note, runway ETA.
- Runway math ported from zai-usage `etaHours` (burn-rate extrapolation, capped at remaining window). Tests in `RunwayTest` guard: infinite before usage, 0 when exhausted/near-end, rate math (50% in 1h → 1h left), needle full-circle sweep.
- Refresh: `updatePeriodMillis=1800000` (Android minimum) + tap-to-refresh broadcast. Live runs at build time are manual (no network in unit tests).

## Build

AGP 8.7.3 + Gradle 8.10 + JDK 17 + compileSdk 35, buildTools 34.0.0, SDK at `/opt/android-sdk` (local.properties). `make verify` = test + lint + assembleDebug. Zero runtime deps (HttpURLConnection + org.json only — no Compose/OkHttp/AndroidX libs). JUnit 4 test-only dep.

CI: `.github/workflows/android.yml` — tests + lint + APK artifact on push. Releases: tag `v*` → release workflow attaches APK.
