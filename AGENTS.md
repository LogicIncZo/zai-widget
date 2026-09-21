# zai-widget — project notes

Android home-screen widget showing GLM Coding Plan runway/usage as a clock-style dial.
Repo: `LogicIncZo/zai-widget`. Companion of `Projects/zai-usage` (same endpoints, Kotlin port of its runway math).

## Facts

- Package `in.cashlessconsumer.zaiwidget`; minSdk 26, target/compile 35, AGP 8.7.3, Gradle 8.10, JDK 17, buildTools 34.0.0 (same pins as zo-voice).
- **Zero dependencies**: plain Views + RemoteViews (no Compose/AndroidX), `HttpURLConnection` for API calls. Only test dep is JUnit 4.
- Endpoints (GET, `Authorization: Bearer <GLM key>`): `/api/monitor/usage/quota/limit` and `/api/biz/customer-package-reset/list?targetType=PERSONAL`. Response gate: `success:true` else error string.
- Limit `unit` mapping (verified against zai-usage): 3 = 5-hour tokens, 6 = weekly, 4 = monthly, 5 = tool calls. `percentage` 0–100, `nextResetTime` epoch-ms = end of window.
- Runway ETA ported from zai-usage `etaHours`: `rate = pct/hoursInto; (100-pct)/rate`, capped at window remainder; ∞ before any usage; 0 when ≥100% or <15 min left.
- Dial is a Canvas-drawn bitmap set into an `ImageView` via RemoteViews (512px, regenerated every refresh). Needle = fraction of 5h window elapsed; ring = % used; color thresholds 50/80%.
- Updates: `updatePeriodMillis` 30 min (Android minimum) + tap-to-refresh broadcast (`in.cashlessconsumer.zaiwidget.REFRESH`). Key in `EncryptedSharedPreferences` via androidx security-crypto (only dependency).
- Config activity doubles as the `APPWIDGET_CONFIGURE` target — launcher opens it on widget add; saving triggers an immediate refresh.

## Gotchas (first build)

- K2 rejects `in` in package paths — backtick-escape: ``package `in`.cashlessconsumer.zaiwidget`` (same as zo-voice).
- JUnit `assertEquals(0.0, etaHours(...), 1e-9)` fails to resolve when the fn returns `Double?` — unbox with `!!`.
- `etaHours(50, 1h, 5h)` = 1.0 (rate 50%/h → 1h left), NOT (100-pct) hours — the burn rate matters; test it that way.
- packsNote reads "1 reset pack" (singular/plural handled).

## Verify

`make verify` = `testDebugUnitTest` + `lintDebug` + `assembleDebug`. Local SDK at `/opt/android-sdk` (`local.properties`, gitignored).
CI (`.github/workflows/android.yml`): tests + lint + debug APK artifact on every push to main.
