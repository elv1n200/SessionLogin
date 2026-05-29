package dev.elv1n200.sessionlogin.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.vault.VaultManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persists the account list to {@code <config>/sessionlogin/accounts.json}.
 * Token ciphertext is produced by the {@link VaultManager}; everything else
 * (label, username, uuid, notes, lastUsed) is plaintext metadata.
 */
public final class AccountStore {

	private final Path dir;
	private final Path file;
	private final VaultManager vault;
	private final List<Account> accounts = new ArrayList<>();

	public AccountStore(Path dir, VaultManager vault) {
		this.dir = dir;
		this.file = dir.resolve("accounts.json");
		this.vault = vault;
	}

	public List<Account> accounts() {
		return Collections.unmodifiableList(accounts);
	}

	public boolean isLocked() {
		return !vault.isUnlocked();
	}

	public void add(Account account) {
		accounts.removeIf(a -> a.encToken().equals(account.encToken())
				&& !account.encToken().isEmpty());
		accounts.removeIf(a -> a.hasToken()
				&& a.token().equals(account.token()));
		accounts.add(account);
		save();
	}

	public void remove(Account account) {
		accounts.remove(account);
		save();
	}

	public void load() {
		accounts.clear();
		try {
			if (!Files.exists(file)) {
				return;
			}
			JsonObject root = JsonParser.parseString(Files.readString(file))
					.getAsJsonObject();
			JsonArray arr = root.getAsJsonArray("accounts");
			if (arr == null) {
				return;
			}
			for (var el : arr) {
				JsonObject o = el.getAsJsonObject();
				Account.Type type = Account.Type.SESSION;
				if (o.has("type")) {
					try {
						type = Account.Type.valueOf(
								o.get("type").getAsString().toUpperCase());
					} catch (Exception ignored) {
					}
				}
				Account a = new Account(
						o.get("label").getAsString(),
						o.get("username").getAsString(),
						o.get("uuid").getAsString(),
						"", type);
				a.setEncToken(o.has("token") ? o.get("token").getAsString() : "");
				if (o.has("notes")) {
					a.setNotes(o.get("notes").getAsString());
				}
				if (o.has("lastUsed")) {
					a.setLastUsed(o.get("lastUsed").getAsLong());
				}
				if (vault.isUnlocked()) {
					try {
						a.setToken(vault.crypto().decrypt(a.encToken()));
					} catch (Exception e) {
						a.setToken("");
					}
				}
				accounts.add(a);
			}
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Could not read accounts.json", e);
		}
	}

	/** After a successful vault unlock: decrypt every stored token. */
	public void decryptAll() {
		if (!vault.isUnlocked()) {
			return;
		}
		for (Account a : accounts) {
			if (!a.hasToken() && !a.encToken().isEmpty()) {
				try {
					a.setToken(vault.crypto().decrypt(a.encToken()));
				} catch (Exception e) {
					a.setToken("");
				}
			}
		}
	}

	/** Re-encrypt all tokens with the current vault key, then persist. */
	public void reencryptAll() {
		if (!vault.isUnlocked()) {
			return;
		}
		for (Account a : accounts) {
			if (a.hasToken()) {
				a.setEncToken(vault.crypto().encrypt(a.token()));
			}
		}
		save();
	}

	public void save() {
		try {
			Files.createDirectories(dir);
			JsonArray arr = new JsonArray();
			for (Account a : accounts) {
				String enc = a.encToken();
				if (a.hasToken() && vault.isUnlocked()) {
					enc = vault.crypto().encrypt(a.token());
					a.setEncToken(enc);
				}
				JsonObject o = new JsonObject();
				o.addProperty("label", a.label());
				o.addProperty("username", a.username());
				o.addProperty("uuid", a.uuid());
				o.addProperty("token", enc);
				o.addProperty("notes", a.notes());
				o.addProperty("lastUsed", a.lastUsed());
				o.addProperty("type", a.type().name());
				arr.add(o);
			}
			JsonObject root = new JsonObject();
			root.add("accounts", arr);
			Files.writeString(file, root.toString());
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Could not write accounts.json", e);
		}
	}
}
