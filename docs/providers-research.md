# Multi-provider support — research (2026-09-23)

Question: can zai-widget show runway/usage for coding plans other than Z.ai GLM?

Ground rule: the phone must be able to fetch the numbers directly with a credential
the user can obtain without running a desktop agent. Window math (5h ring, needle,
ETA) is provider-independent already.

## Feasibility matrix

| Provider | Data source | Auth | On-device? | Stability | Verdict |
| --- | --- | --- | --- | --- | --- |
| Z.ai GLM (shipped) | `GET /api/monitor/usage/quota/limit` + `customer-package-reset/list` | plan API key (Bearer) | ✅ | Undocumented but stable since 2025; already in prod for us | done |
| OpenRouter | `GET /api/v1/credits` (management key) → total_credits/total_usage; `GET /api/v1/auth/key` → per-key limit/usage | paste API key | ✅ | **Documented official API** | easiest add |
| OpenAI Codex (ChatGPT plan) | `GET https://chatgpt.com/backend-api/wham/usage` → primary/secondary windows (`limit_window_seconds` 18000 = 5h, 604800 = 7d), used_percent, reset_at, limit_reached; companion `wham/rate-limit-reset-credits` (banked resets) | ChatGPT OAuth access token from `codex login` (`~/.codex/auth.json`); refresh via `POST auth.openai.com/oauth/token` (grant_type=refresh_token, client_id `app_EMoamEEZ73f0CkXaXp7hrann`) | ✅ with caveats | Undocumented; community-maintained reference (monperrus gist) + ≥4 OSS consumers (codex-usage, codex-reset-tracker, codexbar, Codex-Usage skill) | add in v0.5 with "paste refresh token" onboarding |
| GitHub Copilot | `GET api.github.com/copilot_internal/v2/token` (token exchange) → `quota_snapshots` (e.g. `premium_interactions`: quota_remaining, percent_remaining, entitlement), `quota_reset_date_utc`, `copilot_plan` | GitHub OAuth device flow (RFC 8628) with VS Code client_id `Iv1.b507a08c87ecfe98`, scope `read:user` → `gho_` token; exchange → ~30-min Copilot session token (re-exchange each refresh). **PATs rejected** ("Personal Access Tokens are not supported") | ✅ — device flow is phone-friendly (open github.com/login/device, enter code) | Undocumented but the whole Copilot ecosystem (gh-copilot, litellm, RewriteBar, ai-provider-kit) rides on it | v0.6; device flow = most UX work, nicest login |
| Anthropic Claude Code | No API. `/usage` is in-CLI only; claude.ai Settings→Usage is HTML behind session cookies; `ccusage` parses **local** JSONL transcripts and shows consumption, **never remaining allowance** | — | ❌ on-device; ⚠️ via bridge | n/a | skip on-device; optional generic-JSON bridge |
| Cursor | No individual API (dashboard HTML); admin API is teams-only | — | ❌ | n/a | skip |
| Gemini Code Assist | No individual quota API (Cloud Monitoring = enterprise only) | — | ❌ | n/a | skip |
| Any/other (escape hatch) | "Custom JSON URL" provider: widget GETs any HTTPS JSON matching a ~6-field schema (windows[{label,pct,resetsAt}]) | user-supplied URL | ✅ | user-owned | cheap, unlocks Claude/Codex-desktop/self-hosted via cron + gist |

## Key technical facts per provider

### Codex (wham/usage)
- Identify window type by `limit_window_seconds`, **not** position: 18000 = 5h, 604800 = 7d.
- `secondary_window` can be `null` on some plans; `plan_type` has ≥14 enum values and the
  official CLI crashed on unknown variants (`prolite`) — our parser must ignore unknowns.
- 7-day `reset_at` is not guaranteed to be a fixed weekly schedule; treat as advisory.
- Auth: access token expires (~days); refresh-token flow exists but refresh-token
  rotation may conflict with the desktop CLI (both hold refresh tokens). Onboarding
  choice: paste access token (simple, expires) vs paste refresh token (durable, may
  deauth desktop CLI on rotation). Document; default to access token + manual renew.
- ToS-gray: same class of risk we already accept for z.ai monitor endpoints.

### Copilot
- Token exchange returns a session token (~30 min TTL) — every widget refresh must
  re-exchange using the stored `gho_` token. Two calls per refresh (exchange + read).
- quota_snapshots observed: `chat`, `premium_interactions` (entitlement, quota_remaining,
  percent_remaining, unlimited); `quota_reset_date_utc` = monthly reset.
- GitHub blog confirms Copilot now ALSO has session + weekly token-based limits distinct
  from premium requests — not yet visible in quota_snapshots; render what exists.
- Device flow on Android: open `github.com/login/device?user_code=XXXX` in browser,
  poll `github.com/login/oauth/access_token` (client_id above) until authorized.
  Store `gho_` token in EncryptedSharedPreferences.

### OpenRouter
- `/api/v1/credits` needs a **management key** (regular keys get 403). Onboarding copy
  must say "create a management key". Response: `{data: {total_credits, total_usage}}`.
- Not a window model — render as remaining-credits dial (credits-left %, no needle).

## Proposed architecture (implementation sketch, not built)

```kotlin
interface UsageProvider {
    val id: String
    val displayName: String
    fun fetch(credential: String): ProviderStatus   // network on bg thread
}
data class ProviderStatus(
    val dialPct: Int?,          // primary dial
    val dialLabel: String,      // "5h window" / "credits"
    val windows: List<Window>,  // secondary rows
    val notes: List<String>,    // packs / credits / warnings
)
data class Window(val label: String, val pctUsed: Int?, val resetsAt: Long?)
```

- WidgetProvider gains a provider registry `mapOf("zai" to ZaiProvider, "openrouter" to
  OpenRouterProvider, "codex" to CodexProvider, "copilot" to CopilotProvider, "json" to
  BridgeProvider)`; per-widget binding (widgetId → provider id) stored alongside the
  existing per-widget key. Multiple widgets on one home screen = multiple providers.
- Config UI gains a provider picker (radio list) before the credential field, with
  per-provider help text (where to get the key/token).
- `Runway.kt` window math unchanged — it consumes pct + resetAt, which every provider
  normalizes to.
- Z.ai stays the default provider; existing installs migrate silently.

## Recommended phasing

1. **v0.5 — abstraction + OpenRouter + Codex.** OpenRouter proves the abstraction
   against a documented API; Codex is the highest-demand add (identical 5h/7d window
   shape to Z.ai). Label both z.ai + codex endpoints "undocumented" in UI help.
2. **v0.6 — Copilot device flow.** Biggest UX lift (in-app browser auth), biggest user
   base. Needs a `provide-google-free` style polish pass on error states (403 =
   no Copilot subscription; 401 = re-run device flow).
3. **v0.7 (optional) — Custom JSON URL bridge.** Unlocks Claude Code and anything else
   via a desktop cron exporting `{windows:[...]}` to a private gist. No Claude scraping,
   no ToS risk in the app itself.

## Sources

- Codex wham/usage unofficial reference: https://gist.github.com/monperrus/21dc7d85ea518dc2a66606006d356b5b
- davidfindlay/codex-usage (auth.json discovery, API-key limitation): https://github.com/davidfindlay/codex-usage
- AyalX/codex-reset-tracker (reset credits, windows): https://github.com/AyalX/codex-reset-tracker
- codex-cli plan_type decode failure (unknown-variant hardening): https://github.com/openai/codex/issues/17353
- litellm issue #18242 with raw copilot_internal quota_snapshots JSON: https://github.com/BerriAI/litellm/issues/18242
- RewriteBar Copilot setup (device flow client_id, token exchange): https://rewritebar.com/docs/ai-providers/github
- Copilot PAT rejection reports: https://github.com/RightNow-AI/openfang/issues/1014
- byterover-cli #322 (device flow + 30-min session token re-exchange): https://github.com/campfirein/byterover-cli/issues/322
- OpenRouter credits endpoint (documented): https://openrouter.ai/docs/api/api-reference/credits/get-remaining-credits
- OpenRouter key limits (management keys, credit limits): https://openrouter.ai/docs/api_reference/authentication
- Claude Code `/usage` in-CLI only; ccusage shows consumption not remaining: https://continuumcode.ai/guides/how-to-check-claude-usage-limit
- Copilot usage limits now session+weekly, separate from premium requests: https://github.blog/news-insights/company-news/changes-to-github-copilot-individual-plans
