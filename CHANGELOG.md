# Changelog

All notable changes to this project are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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

[1.2.0]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.2.0
[1.1.2]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.1.2
[1.1.1]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.1.1
[1.1.0]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.1.0
[1.0.0]: https://github.com/elv1n200/SessionLogin/releases/tag/v1.0.0
