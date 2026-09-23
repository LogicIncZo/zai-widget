# zai-widget

Android home-screen widget for your **Z.ai GLM Coding Plan** — glance at your phone, see if quota is available, decide whether to send workloads.

Clock-style dial:

- **Ring** = % of the 5-hour token window used (green → amber → red)
- **Needle** = time elapsed in the current 5-hour window (points up when a reset just happened, sweeps a full circle until the next reset)
- **Center** = % used + runway ETA ("2h 41m left" at the current burn rate — ported from the zai-usage CLI's `etaHours`)
- Below the dial: reset countdown, weekly %, tool-call %, available reset packs
- Wall-clock row with time, date, and day
- In-app configuration toggles for the wall-clock row and quota statistics

Data comes straight from the same undocumented Z.ai monitor endpoints the [zai-usage](https://github.com/LogicIncZo/zai-usage) CLI uses (`/api/monitor/usage/quota/limit` + `/api/biz/customer-package-reset/list`) — **no backend**, the phone talks to api.z.ai with your GLM key.

## Install

Grab `zai-widget-v*.apk` from [Releases](https://github.com/LogicIncZo/zai-widget/releases), sideload it, long-press your launcher → Widgets → **zai-widget**, drop it on the home screen, paste your GLM Coding Plan API key once (stored with `EncryptedSharedPreferences`). The setup screen also lets you show or hide the wall-clock row and the week/month/tool-call statistics. Tap the ⟳ button (or the widget itself) any time to force a refresh — Android's minimum widget auto-refresh interval is 30 minutes, which is what `updatePeriodMillis` is set to; the button covers everything faster.

Android refreshes home-screen widgets at most every 30 minutes (`updatePeriodMillis`); tap for instant. That cadence is deliberate — battery-kind and fine for "do I have quota?" decisions.

## Privacy

- The API key never leaves the device (except to api.z.ai).
- The app makes exactly two GET requests per refresh and stores nothing else. No analytics, no trackers.

## Build

```bash
make verify   # unit tests + lint + assembleDebug (JDK 17, Android SDK 35)
make apk      # copy APK to releases/
```

Zero runtime dependencies — plain Kotlin + framework widgets (no Compose, no OkHttp; `HttpURLConnection` only), so builds are fast.

## Repo family

- [`zai-usage`](https://github.com/LogicIncZo/zai-usage) — the terminal CLI (quota, billing ledger, behavioral reviews, runway)
- `zai-widget` — this widget (this repo)

MIT
