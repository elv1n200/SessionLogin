package dev.elv1n200.sessionlogin.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads accounts directly from the official Minecraft Launcher's
 * {@code launcher_accounts.json}. Each entry has the access token + profile
 * already, so no Mojang round-trip is needed.
 */
public final class LauncherImport {

	private LauncherImport() {
	}

	/** Searches the well-known per-OS locations, returns the first match. */
	public static Path findFile() {
		List<Path> candidates = new ArrayList<>();
		String appData = System.getenv("APPDATA");
		String home = System.getProperty("user.home");

		if (appData != null) {
			candidates.add(Paths.get(appData, ".minecraft", "launcher_accounts.json"));
		}
		if (home != null) {
			candidates.add(Paths.get(home, "Library", "Application Support",
					"minecraft", "launcher_accounts.json"));
			candidates.add(Paths.get(home, ".minecraft", "launcher_accounts.json"));
		}
		for (Path p : candidates) {
			if (Files.isRegularFile(p)) {
				return p;
			}
		}
		return null;
	}

	/** Parses every Xbox/MSA account in the file into a fresh Account list. */
	public static List<Account> read(Path file) {
		List<Account> out = new ArrayList<>();
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file))
					.getAsJsonObject();
			JsonObject accounts = root.getAsJsonObject("accounts");
			if (accounts == null) {
				return out;
			}
			for (var entry : accounts.entrySet()) {
				JsonObject acc = entry.getValue().getAsJsonObject();
				if (!acc.has("accessToken") || !acc.has("minecraftProfile")) {
					continue;
				}
				String token = acc.get("accessToken").getAsString();
				JsonObject profile = acc.getAsJsonObject("minecraftProfile");
				if (!profile.has("id") || !profile.has("name")) {
					continue;
				}
				String name = profile.get("name").getAsString();
				String uuid = SessionUtils.dashUuid(profile.get("id").getAsString());
				Account a = new Account(name, name, uuid, token);
				a.touch();
				out.add(a);
			}
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Could not parse launcher_accounts.json", e);
		}
		return out;
	}
}
