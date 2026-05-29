# SessionLogin

> A client-side Fabric mod that swaps your Minecraft session at runtime — with
> a token-only multi-account manager and privacy toggles that keep your alts
> from being correlated to the same machine.

<p>
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.21.11-62B132?logo=minecraft&logoColor=white">
  <img alt="Loader"    src="https://img.shields.io/badge/Loader-Fabric-DBB69B">
  <img alt="Java"      src="https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white">
  <img alt="License"   src="https://img.shields.io/badge/License-CC0--1.0-lightgrey">
  <img alt="Version"   src="https://img.shields.io/badge/Version-1.2.0-blue">
</p>

---

## Table of contents

- [What it is](#what-it-is)
- [Features](#features)
- [Install](#install)
- [Quick start](#quick-start)
- [Slash commands](#slash-commands)
- [Settings](#settings)
- [Vault &amp; security](#vault--security)
- [Build from source](#build-from-source)
- [Project layout](#project-layout)
- [Credits](#credits)
- [License](#license)

---

## What it is

SessionLogin lets you paste a Minecraft session token (a "session ID") and
instantly swap the active session in memory — no relaunch, no Microsoft
re-login. On top of the basic switcher it adds a saved multi-account
manager, an encrypted token vault, and a small set of privacy toggles so
two accounts on the same machine can't be linked by their cached resource
packs, client brand, or telemetry.

Every network call goes to the official `api.minecraftservices.com` and
nowhere else. There is **no email/password field anywhere** — accounts are
keyed purely by token.

---

## Features

### 🔑 Login &amp; account editing
- Paste a session token → session is swapped in memory; the original is
  preserved and can be restored anytime.
- Tolerant pasting: strips `Bearer ` prefix and trailing `:UUID` suffix,
  plus a one-click *Paste from clipboard* button and Enter-to-login.
- Login runs off the render thread — no UI freeze, clear green ✔
  confirmation and a Minecraft toast.
- Edit account: change username / skin via the official Mojang API.

### 👥 Account manager
- **Two account types**: session-token (online play) and **offline** /
  cracked (username + canonical OfflinePlayer UUID, for LAN / offline
  servers).
- **Token expiry** parsed from the JWT `exp` claim, colour-coded.
- **Background validity check** (✔ / ✘) cached so Mojang isn't spammed.
- **Search** by label / username / notes; **sort** by recent / name /
  expiry.
- **Custom label + notes** per account.
- **Bulk import** from the clipboard (one token per line, `token` or
  `token:uuid`).
- **Import / Export** the whole encrypted vault as a single portable JSON
  file — with master-password mode, genuinely portable across machines.

### 🛡 Privacy toggles
- **Isolate pack cache per account** *(default ON)* — each session UUID
  gets its own `server-resource-packs/<uuid>/` subfolder, so a server
  can't fingerprint your machine by correlating cached packs between
  alts. Idea from LiquidBounce / OpSec.
- **Spoof brand as Vanilla** *(default OFF)* — report `vanilla` to
  servers instead of `fabric`.
- **Block Mojang telemetry** *(default ON)* — drop telemetry events
  before they're sent. Same idea as No Chat Reports.

> For deeper anti-tracking (sign-translation, channel spoofing, known-pack
> filtering, key resolution probing, chat-signature stripping), stack
> SessionLogin with [OpSec](https://github.com/aurickk/OpSec) or
> [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) —
> they're complementary, not competing.

### 🎮 Quality of life
- `/sl` command — `list`, `current`, `switch <label>`, `restore`.
- Open-anywhere **keybind** (Controls → Misc → *Open Account Manager*).
- In-world **HUD warning** `⚠ Alt: <name>` while you're on a swapped
  session.
- **SystemToast** on every successful login / switch.
- Optional **ModMenu** integration — opens the settings screen from the
  ModMenu mod list (soft dep, nothing breaks if ModMenu is missing).

---

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for **Minecraft 1.21.11**.
2. Install matching [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download `sessionlogin-1.2.0.jar` from the
   [Releases](https://github.com/elv1n200/SessionLogin/releases) page
   (or build from source, see below) and drop it into your `mods/`
   folder.
4. *(Optional)* Install [ModMenu](https://modrinth.com/mod/modmenu) for
   in-game access to the settings screen.
5. Launch the game.

---

## Quick start

1. Multiplayer screen → top-right **Login** button.
2. Paste a session token (or use **Paste** to take it from the clipboard;
   `:UUID` suffixes are trimmed automatically).
3. Click **Login** (or hit Enter) → you'll see ✔ *Logged in as &lt;name&gt;*
   and a toast.
4. *Optional:* **Save to accounts** to keep it for one-click switching
   later.
5. **Accounts** button opens the manager. Click a row to switch, click
   again on **Use Original** to restore.

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

Open the **Settings** screen from the Account Manager or via ModMenu.
Saved to `config/sessionlogin/settings.json`.

| Setting | Default | Description |
|---|:--:|---|
| Isolate pack cache per account | ON | Per-UUID subdirectory for server resource-pack downloads. |
| Spoof brand as Vanilla | OFF | Report `vanilla` instead of `fabric`. |
| Block Mojang telemetry | ON | Replace the telemetry sender with NOOP. |
| Show toasts on login / switch | ON | Pop a Minecraft-style toast on switch. |

---

## Vault &amp; security

Saved tokens are encrypted at rest. Pick a mode in the **Vault** screen:

| Mode | Key location | Portability | Protection level |
|---|---|---|---|
| **Local** *(default)* | `config/sessionlogin/.key` (random) | Local only | Obfuscation — anyone with file access can decrypt. |
| **Master password** | Derived in memory via PBKDF2 (210k iters) from your password + salt | Portable — `accounts.json` + `meta.json` can be moved to another PC and unlocked with the password | Genuine encryption — no secret on disk. |

> If you forget the master password, there's **no recovery** — tokens
> can't be decrypted. Switch back to local mode any time while unlocked.

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

Concepts and approaches inspired by:

- [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) — per-account
  pack cache isolation.
- [OpSec](https://github.com/aurickk/OpSec) — synthesises and extends the
  privacy techniques used here.
- [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) —
  original sink for the local-URL / fingerprinting research.
- [No Chat Reports](https://modrinth.com/mod/no-chat-reports) —
  telemetry-blocking approach.
- [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) —
  session-token login flow.

This mod is an independent implementation, not a fork.

---

## License

[CC0-1.0](LICENSE) — public domain. Do whatever you want.
