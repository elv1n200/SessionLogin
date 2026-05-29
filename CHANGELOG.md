# Changelog

All notable changes to this project are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.3.3] — 2026-05-29

### Fixed
- **MC crash on launch when the integrity check triggered.**
  `TamperWarningScreen.render()` called both `renderBackground()` and
  `super.render()` (which also renders the background), so MC's
  one-blur-per-frame assertion fired the moment any overlay (e.g. the
  progress screen) sat in front of the warning, crashing the client
  with `IllegalStateException: Can only blur once per frame`. The
  screen now calls `super.render()` exactly once and draws its text on
  top.

## [1.3.2] — 2026-05-29

### Fixed
- **Duplicate accounts after re-importing from a launcher.** Modrinth
  App refreshes its `access_token` periodically, so the old
  token-equality dedup in `AccountStore.add()` thought every fresh
  import was a brand-new account and stacked duplicates — with most of
  them ending up with empty token strings because of a related
  write-path bug. `add()` now dedups by **UUID** (stable) and updates
  the token on the existing entry while preserving the user's label,
  notes, and `lastUsed`.
- **Self-healing of an already broken `accounts.json`.** `load()`
  collapses duplicate UUIDs and prefers the entry that still has a
  real encrypted token (or the most-recently-used one as a tie
  breaker), then re-saves. Existing broken installs clean themselves
  up on the next launch.

## [1.3.1] — 2026-05-29

### Added
- **Modrinth App account import.** Bundles `sqlite-jdbc` as a Jar-in-Jar
  and reads the `minecraft_users` table from
  `%APPDATA%/ModrinthApp/app.db` (and the macOS / Linux equivalents).
  The `Import from MC Launcher` button now tries the official Mojang
  launcher and the Modrinth App and merges the results.
- Jar grew from ~110 KB → ~14 MB because `sqlite-jdbc` ships native
  libs for every platform.

## [1.3.0] — 2026-05-29

### Added
- **Player-head preview** next to every row in the Account Manager,
  fetched lazily from Crafatar and registered as a dynamic texture.
  Fallback square is drawn until the image loads.
- **Import from Minecraft Launcher** — reads
  `launcher_accounts.json` from the standard per-OS locations and
  imports every account as a session-token entry. Added to the Bulk
  Import screen.
- **JAR integrity check (trust-on-first-use)** — at startup the running
  jar's SHA-256 is recorded to `config/sessionlogin/integrity.json`.
  Subsequent launches compare; mismatches open a red
  `TamperWarningScreen` on top of the title screen with options to
  trust the new build or quit.
- **GitHub Actions CI** — `.github/workflows/build.yml` builds every
  push and pull request and uploads the jar as an artifact. Pushing a
  `v*` tag also creates a GitHub release with the jar attached
  automatically.

## [1.2.0] — 2026-05-29

### Added — privacy
- **Per-account pack cache isolation** (default ON). `DownloaderMixin`
  redirects server-resource-pack downloads into a per-UUID subfolder so
  alts on the same machine can't be correlated via cached pack hashes.
  Idea inherited from LiquidBounce / OpSec.
- **Brand spoof as Vanilla** toggle. `ClientBrandRetrieverMixin` returns
  `vanilla` instead of `fabric` when on.
- **Telemetry blocking** toggle (default ON). `TelemetryManagerMixin`
  returns `TelemetrySender.NOOP` to drop Mojang telemetry on the client.
- New **Settings** screen with persistent toggles, saved to
  `config/sessionlogin/settings.json`.

### Added — account manager
- **Offline ("cracked") account type** with the canonical
  `OfflinePlayer:<name>` UUID derivation; new `Add Offline` button +
  screen; validation and JWT-expiry checks are skipped for these.
- **Import / Export** of the full vault (`meta.json` + `accounts.json`)
  to a portable JSON file at `<gameDir>/sessionlogin-export.json`. With
  master-password mode the export is genuinely portable across machines.
- Account-manager layout grew to four control rows to fit `Settings`,
  `Add Offline`, `Import`, `Export`.

### Added — QoL
- **`/sl` client command**: `list`, `current`, `switch <label>`,
  `restore` (label tab-completion).
- **SystemToast** on every successful login or switch.
- Optional **ModMenu integration** (`modCompileOnly`) — Settings screen
  opens from the ModMenu mod list.
- `en_us.json` so the keybind displays as "Open Account Manager" in the
  Controls menu.

### Changed
- Repo polish: structured README with badges + TOC, `.editorconfig`,
  `.gitattributes` normalised to LF for source.
- Build no longer depends on Apache HttpClient (kept from 1.1.2).

## [1.1.2] — 2026-05-21

### Fixed
- **Login was silently failing in production builds.** `ApiUtils` used
  Apache HttpClient added as `implementation`, which is not bundled into
  the mod jar — the background thread died with `NoClassDefFoundError`
  before re-enabling the Login button, leaving it grey and unresponsive.
  Rewrote `ApiUtils` on the JDK's built-in `java.net.http.HttpClient`
  and dropped the dependency. The login handler also catches `Throwable`
  now so the button can never get stuck again.

## [1.1.1] — 2026-05-21

### Added
- Async login (off the render thread) with immediate "Checking..."
  feedback and accurate error reasons ("Token invalid or expired" vs
  "network").
- Persistent "Active: &lt;name&gt;" line in the login screen.
- Reliable multiplayer-screen indicator (drawn via `addDrawable`
  instead of a render-method mixin that wouldn't remap).
- Enter-to-login.

### Fixed
- Removed the broken `render()` mixin on `MultiplayerScreen` that
  printed `Cannot remap render` and silently did nothing.

### Changed
- Account Manager: compact, vertically-centered panel layout.

## [1.1.0] — 2026-05-21

### Added
- `:UUID` / `Bearer` auto-strip on paste, plus a clipboard Paste button.
- JWT `exp` parsing + colour-coded token-expiry display.
- Background validity check per saved account (cached).
- Search and sort (recent / name / expiry) in the Account Manager.
- Custom label + notes per account.
- Bulk clipboard import.
- Open-anywhere keybind ("Open Account Manager").
- In-world HUD warning `⚠ Alt: &lt;name&gt;`.
- Master-password vault (PBKDF2, 210k iterations) alongside the local
  random-key mode; lock/unlock flow.

## [1.0.0] — 2026-05-18

### Added
- Initial release. Token-based session switcher with a multi-account
  manager, name / skin editing via the official Mojang API, and
  AES-GCM-at-rest encryption with a local key.

[1.3.3]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.3.3
[1.3.2]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.3.2
[1.3.1]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.3.1
[1.3.0]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.3.0
[1.2.0]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.2.0
[1.1.2]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.1.2
[1.1.1]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.1.1
[1.1.0]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.1.0
[1.0.0]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.0.0
