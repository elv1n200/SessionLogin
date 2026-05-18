# SessionLogin

A client-side Fabric mod for **Minecraft 1.21.11** that swaps your in-game
session at runtime using a session token ("session ID"), edits the
logged-in account's name/skin, and keeps a saved **token-only** multi-account
manager for one-click switching.

## Features

### Login & account editing
- **Login by session ID** — paste a Minecraft bearer token; the mod swaps the
  active session in memory. The original session is never touched and can be
  restored anytime.
- Pasting is forgiving: a `Bearer ` prefix and a trailing `:UUID` suffix
  (the common `token:uuid` dump format) are stripped automatically, plus a
  one-click **Paste from clipboard** button.
- **Edit account** — change username / skin via the official Mojang API.

### Account manager
- Save accounts and switch with one click. Accounts are identified purely by
  **token** — there is **no email or password field anywhere**, by design.
- **Token expiry** shown per account (parsed from the JWT `exp` claim),
  colour-coded; expired tokens are flagged.
- **Background validity check** — each saved token is verified against Mojang
  and shows OK / X, cached to avoid API spam.
- **Search** (label / username / notes) and **sort** (recent / name / expiry).
- **Custom label + notes** per account.
- **Bulk import** — paste a list of tokens (one per line, `token` or
  `token:uuid`) and import them all at once.
- **Open-anywhere keybind** — bind a key (Controls → Misc →
  "Open Account Manager") to open the manager even while in a world.
- **HUD warning** — a small `⚠ Alt: <name>` indicator while you are on a
  swapped (non-original) session, so you never forget which account is live.

### Security
- The only network destination is the official `api.minecraftservices.com`.
  No telemetry, no analytics, no third-party servers.
- Saved tokens are encrypted at rest. Two vault modes:
  - **Local** (default) — random key in `config/sessionlogin/.key`.
    Obfuscation-grade: keeps tokens out of plain text, but anyone with file
    access can decrypt (the key sits next to the data).
  - **Master password** — key derived via PBKDF2 (210k iterations) from a
    password you set. **Nothing secret is stored on disk**, so the
    `accounts.json` is genuinely protected and portable (back up / sync it,
    unlock on another PC with the password). The vault is locked on startup
    until you enter the password.

> If you forget the master password there is no recovery — the tokens cannot
> be decrypted. Switch back to local mode (in the vault screen) any time
> while unlocked.

## Build

Requires JDK 21.

```
./gradlew build
```

The mod jar lands in `build/libs/`. Drop it into your `mods/` folder with
Fabric Loader + Fabric API for MC 1.21.11.

## License

CC0-1.0 — do whatever you want.
