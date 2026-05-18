# SessionLogin

A client-side Fabric mod for **Minecraft 1.21.11** that lets you swap your
in-game session at runtime using a session token ("session ID"), edit the
logged-in account's name/skin, and keep a saved **token-only** multi-account
list for one-click switching.

## Features

- **Login by session ID** — paste a Minecraft bearer token; the mod swaps the
  active session in memory (original session is never touched and can be
  restored at any time).
- **Edit account** — change username / skin of the active account via the
  official Mojang API.
- **Account manager** — save accounts and switch between them with one click.
  Accounts are identified purely by **token** — there is **no email or
  password field anywhere**, by design.
- **Validity indicator** — the multiplayer screen shows whether the active
  session is currently valid.

## Privacy / safety

- The only network destination is the official `api.minecraftservices.com`.
  Nothing is sent anywhere else — no telemetry, no analytics, no third-party
  servers.
- Saved tokens are stored in `<.minecraft>/config/sessionlogin/accounts.json`
  encrypted (AES-GCM) with a key in `.../sessionlogin/.key`.

  **This is obfuscation-grade, not a vault.** The key sits next to the data,
  so anyone with read access to that folder can decrypt it. Its purpose is
  only to keep raw tokens out of plain text (screen-shares, backups, casual
  grep) — treat the config folder as sensitive and only ever use tokens for
  accounts you own.

## Build

Requires JDK 21.

```
./gradlew build
```

The built mod jar lands in `build/libs/`. Drop it into your `mods/` folder
alongside Fabric Loader and Fabric API for MC 1.21.11.

## License

CC0-1.0 — do whatever you want.
