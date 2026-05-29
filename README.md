# SessionLogin

A client-side Fabric mod for **Minecraft 1.21.11** that swaps your in-game
session at runtime using a session token ("session ID"), edits the
logged-in account's name/skin, and keeps a saved **token-only** multi-account
manager for one-click switching — with privacy toggles so your alts can't be
correlated to the same machine.

## Features

### Login & account editing
- **Login by session ID** — paste a Minecraft bearer token; the mod swaps the
  active session in memory. The original session is never touched and can be
  restored anytime.
- Pasting is forgiving: `Bearer ` prefix and trailing `:UUID` suffix (the
  common `token:uuid` dump format) are stripped automatically, plus a
  **Paste from clipboard** button and Enter-to-login.
- Login runs on a background thread so the click never freezes the UI; clear
  green "✔ Logged in as <name>" confirmation, plus a Minecraft toast.
- **Edit account** — change username / skin via the official Mojang API.

### Account manager
- Save accounts and switch with one click. Accounts are identified purely by
  **token** — there is **no email or password field anywhere**, by design.
- **Two account types**: session-token accounts (online play) and
  **offline accounts** (username + canonical OfflinePlayer UUID, for LAN /
  offline-mode servers).
- **Token expiry** parsed from the JWT `exp` claim, colour-coded; expired
  tokens are flagged. Background validity check (✔ / ✘) cached to avoid
  hammering Mojang.
- **Search** (label / username / notes) and **sort** (recent / name / expiry).
- **Custom label + notes** per account.
- **Bulk import** — paste a list of tokens (one per line, `token` or
  `token:uuid`) and import them all at once.
- **Import / Export** the whole vault to a single portable JSON file
  (`<gameDir>/sessionlogin-export.json`); with master-password mode the
  export is genuinely portable across machines.
- **`/sl` command** — `/sl list`, `/sl current`, `/sl switch <label>`,
  `/sl restore`. Quick switching without opening the GUI.
- **Open-anywhere keybind** — bind a key (Controls → Misc → "Open Account
  Manager") to open the manager even while in a world.
- **HUD warning** — small `⚠ Alt: <name>` indicator while you are on a
  swapped (non-original) session, so you never forget which account is live.
- **System toast** on every login / switch.

### Privacy toggles (Settings screen)
- **Isolate pack cache per account** (default ON) — server-required resource
  packs are cached in a per-UUID subfolder so two alts can't be linked by the
  hash of the cached pack files. Idea from LiquidBounce / OpSec.
- **Spoof brand as Vanilla** (default OFF) — report `vanilla` to the server
  instead of `fabric`.
- **Block Mojang telemetry** (default ON) — drop telemetry events Mojang
  would otherwise receive. Same approach as No Chat Reports.
- **Show toasts on login / switch** (default ON).

> For deeper anti-tracking (sign-translation, channel spoofing, known-pack
> filtering, key resolution probing, chat-signature stripping) stack
> SessionLogin with [OpSec](https://github.com/aurickk/OpSec) or
> [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) —
> they're complementary, not competing.

### ModMenu integration
If [ModMenu](https://modrinth.com/mod/modmenu) is installed, the Settings
screen opens from the ModMenu mod list. Optional dependency.

### Security
- Only network destination is the official `api.minecraftservices.com`. No
  telemetry, no analytics, no third-party servers.
- Network calls use the JDK's built-in `java.net.http.HttpClient` — no
  external HTTP library bundled.
- Saved tokens are encrypted at rest. Two vault modes:
  - **Local** (default) — random key in `config/sessionlogin/.key`.
    Obfuscation-grade: keeps tokens out of plain text, but anyone with file
    access can decrypt (the key sits next to the data).
  - **Master password** — key derived via PBKDF2 (210k iterations) from a
    password you set. **Nothing secret is stored on disk**, so the
    `accounts.json` is genuinely protected and portable. Vault locked on
    startup until you enter the password.

> If you forget the master password there is no recovery — tokens cannot be
> decrypted. Switch back to local mode (in the vault screen) any time while
> unlocked.

## Build

Requires JDK 21.

```
./gradlew build
```

Mod jar lands in `build/libs/`. Drop it into your `mods/` folder with Fabric
Loader + Fabric API for MC 1.21.11.

## License

CC0-1.0 — do whatever you want.

## Credits & inspiration

- [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) — per-account pack
  cache isolation idea.
- [OpSec](https://github.com/aurickk/OpSec) — synthesises and extends the
  privacy techniques used here.
- [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) —
  original sink for the local-URL/fingerprinting research.
- [No Chat Reports](https://modrinth.com/mod/no-chat-reports) — telemetry
  blocking approach.
