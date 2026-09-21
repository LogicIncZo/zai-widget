# zai-widget ⏱️

Android home-screen **clock widget** for your [Z.ai GLM Coding Plan](https://z.ai/subscribe?ic=5CA0GFZ4CO): glance at your phone, see whether quota is available, decide whether to send workloads.

Companion to the [zai-usage](https://github.com/LogicIncZo/zai-usage) CLI. The widget talks to the same two monitor endpoints directly from the phone — no server, no account, just your API key.

## The dial

It's drawn like a clock face, because the 5-hour quota window *is* a clock:

- **Ring** — % of the 5-hour token quota used (green < 50, amber < 80, red ≥ 80)
- **Needle** — time through the current 5-hour window (pointing up = reset imminent, like a minute hand at twelve)
- **Under the dial** — `5h 47% · resets in 2h 13m`, `week 12% · tools 38%`, pack note, last-updated time
- **Runway ETA** — ported straight from `zai-usage`'s `etaHours`: hours of run-left at your current burn rate

Tap anywhere on the widget to refresh instantly. Android refreshes widgets at most every 30 minutes (`updatePeriodMillis`) — tap is the real refresh.

## Install

Grab `zai-widget-<tag>.apk` from [Releases](../../releases) (or build it yourself: `make verify && make apk`), sideload it, long-press your launcher → Widgets → **zai-widget**.

First add opens a key prompt. Paste your **GLM Coding Plan API key** (the same `GLM_API_KEY` the CLI uses — from your [Z.ai API keys page](https://z.ai/manage-apikey/apikey-list)). The key is stored in Android `EncryptedSharedPreferences`, stays on the device, and is sent only to `api.z.ai`.

## Endpoints used

Same as the CLI:

- `GET /api/monitor/usage/quota/limit` — 5-hour / weekly / monthly / tool-call windows
- `GET /api/biz/customer-package-reset/list?targetType=PERSONAL` — reset packs + last reset time

## Development

Single-activity + single-widget Kotlin app. **Zero dependencies** — `HttpURLConnection` and `org.json` only, no OkHttp/Compose/AndroidX beyond the framework. AGP 8.7.3 · Kotlin 2.0.21 · compileSdk 35 · minSdk 26.

```bash
make verify   # unit tests + lint + debug build
make apk      # verify + copy APK to releases/
```

K2 gotcha (same as zo-voice): `in` is a keyword — every file uses `` package `in`.cashlessconsumer.zaiwidget ``.

CI (`.github/workflows/android.yml`) runs tests + lint + APK artifact on every push.

## License

MIT
