<div align="center">

<img src="src/main/resources/assets/sessionlogin/icon.png" width="140" alt="SessionLogin">

# SessionLogin

<hr>

**Token-basierter Minecraft-Session-Switcher für Fabric 1.21.11.**
Multi-Account-Manager, verschlüsselter Vault und Privacy-Toggles, damit deine
Alts nicht über denselben Rechner korreliert werden können.

<p>
  <img alt="release"   src="https://img.shields.io/badge/release-v1.2.0-blue">
  <img alt="license"   src="https://img.shields.io/badge/license-CC0--1.0-lightgrey">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.21.11-62B132?logo=minecraft&logoColor=white">
  <img alt="Loader"    src="https://img.shields.io/badge/Loader-Fabric-DBB69B">
  <img alt="Java"      src="https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white">
</p>

[Installation](#installation) · [Features](#features) · [Commands](#slash-commands) · [Settings](#settings) · [Vault](#vault--security) · [Build](#build-from-source) · [Changelog](CHANGELOG.md)

</div>

---

## What it is

SessionLogin lets you paste a Minecraft session token ("session ID") and
instantly swap the active session in memory — no relaunch, no Microsoft
re-login. On top of the basic switcher it adds a saved multi-account
manager, an encrypted token vault, and a set of privacy toggles so two
accounts on the same machine can't be linked by their cached resource
packs, client brand, or telemetry.

Every network call goes to the official `api.minecraftservices.com` and
nowhere else. **No email/password field anywhere** — accounts are keyed
purely by token.

---

## Features

### 🔑 Login &amp; account editing
- Paste a session token → session swapped in memory; original is
  preserved and restorable.
- Tolerant pasting: strips `Bearer ` prefix and trailing `:UUID` suffix,
  one-click *Paste from clipboard*, Enter-to-login.
- Login runs off the render thread — no UI freeze, clear green ✔
  confirmation, Minecraft toast.
- Edit account: change username / skin via official Mojang API.

### 👥 Account manager
- **Two account types**: session-token (online) and **offline** /
  cracked (username + canonical OfflinePlayer UUID).
- **Token expiry** parsed from the JWT `exp` claim, colour-coded.
- **Background validity check** (✔ / ✘) cached so Mojang isn't spammed.
- **Search** (label / username / notes) and **sort** (recent / name /
  expiry).
- **Custom label + notes** per account.
- **Bulk import** from the clipboard (one token per line).
- **Import / Export** the whole encrypted vault as a single portable
  JSON file.

### 🛡 Privacy toggles
- **Isolate pack cache per account** *(default ON)* — each session UUID
  gets its own `server-resource-packs/<uuid>/` subfolder. Idea from
  LiquidBounce / OpSec.
- **Spoof brand as Vanilla** *(default OFF)* — report `vanilla` instead
  of `fabric`.
- **Block Mojang telemetry** *(default ON)* — drop telemetry events on
  the client.

> For deeper anti-tracking (sign translation, channel spoofing,
> known-pack filtering, key resolution, chat-signature stripping) stack
> SessionLogin with [OpSec](https://github.com/aurickk/OpSec) or
> [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) —
> complementary, not competing.

### 🎮 Quality of life
- `/sl` command — `list`, `current`, `switch <label>`, `restore`.
- Open-anywhere **keybind** (Controls → Misc → *Open Account Manager*).
- In-world **HUD warning** `⚠ Alt: <name>` on a swapped session.
- **SystemToast** on every login / switch.
- Optional **ModMenu** integration (soft dep).

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for **Minecraft 1.21.11**.
2. Install matching [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download `sessionlogin-1.2.0.jar` from the
   [Releases](https://github.com/elv1n200/SessionLogin/releases) page
   and drop it into your `mods/` folder.
4. *(Optional)* Install [ModMenu](https://modrinth.com/mod/modmenu).
5. Launch the game.

---

## Quick start

1. Multiplayer screen → top-right **Login** button.
2. Paste a session token (or click **Paste** for the clipboard).
3. Press **Login** (or Enter) → ✔ *Logged in as &lt;name&gt;* + toast.
4. *Optional:* **Save to accounts** for one-click switching later.
5. **Accounts** opens the manager. Click a row to switch, **Use
   Original** to restore.

---

## Slash commands

| Command | Description |
|---|---|
| `/sl` | Print the available subcommands. |
| `/sl list` | List every saved account with `[expiry]` and badge. |
| `/sl current` | Show which account is currently active. |
| `/sl switch <label>` | Switch by label or username (tab-complete). |
| `/sl restore` | Restore the original session. |

---

## Settings

Open via **Settings** in the Account Manager or via ModMenu.
Persisted to `config/sessionlogin/settings.json`.

| Setting | Default | Description |
|---|:--:|---|
| Isolate pack cache per account | ON | Per-UUID subdirectory for server resource-pack downloads. |
| Spoof brand as Vanilla | OFF | Report `vanilla` instead of `fabric`. |
| Block Mojang telemetry | ON | Replace the telemetry sender with NOOP. |
| Show toasts on login / switch | ON | Pop a Minecraft-style toast. |

---

## Vault &amp; security

Saved tokens are encrypted at rest. Pick a mode in the **Vault** screen:

| Mode | Key location | Portability | Protection level |
|---|---|---|---|
| **Local** *(default)* | `config/sessionlogin/.key` (random) | Local only | Obfuscation — anyone with file access can decrypt. |
| **Master password** | Derived in memory via PBKDF2 (210k iters) from your password + salt | Portable — `accounts.json` + `meta.json` can be moved to another PC and unlocked with the password | Genuine encryption — no secret on disk. |

> Forget the master password → **no recovery**. Switch back to local
> mode any time while unlocked.

All HTTP calls go to `api.minecraftservices.com` only, via the JDK's
built-in `java.net.http.HttpClient` (no external HTTP library bundled).

---

## Build from source

Requires JDK 21.

```bash
./gradlew build
```

Output jar: `build/libs/sessionlogin-<version>.jar`.

---

## Project layout

```
src/main/java/dev/elv1n200/sessionlogin/
├── SessionLogin.java          ─ main entrypoint (vault, store, settings)
├── SessionLoginClient.java    ─ client entrypoint (keybind, HUD, command)
├── account/                   ─ Account model, AccountStore (encrypted)
├── command/                   ─ /sl client command
├── config/                    ─ Settings (persisted toggles)
├── mixin/                     ─ MinecraftClient (session swap),
│                                Downloader (per-UUID pack cache),
│                                ClientBrandRetriever (vanilla spoof),
│                                TelemetryManager (NOOP sender),
│                                MultiplayerScreen (buttons + indicator)
├── modmenu/                   ─ optional ModMenu integration
├── screen/                    ─ all GUI screens
├── util/                      ─ ApiUtils, TokenUtils, CryptoUtils,
│                                SessionUtils, Notifier, FormattingUtils
└── vault/                     ─ VaultManager (local / master-password)
```

---

## Credits

Concepts inspired by:

- [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) — per-account
  pack cache isolation.
- [OpSec](https://github.com/aurickk/OpSec) — synthesises and extends
  the privacy techniques used here.
- [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) —
  local-URL / fingerprinting research.
- [No Chat Reports](https://modrinth.com/mod/no-chat-reports) —
  telemetry-blocking approach.
- [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) —
  session-token login flow.

Independent implementation, not a fork.

---

## License

[CC0-1.0](LICENSE) — public domain. Do whatever you want.
