package dev.elv1n200.sessionlogin.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.util.CryptoUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persists the saved-account list to {@code <config>/sessionlogin/accounts.json}.
 * Tokens are encrypted at rest with {@link CryptoUtils}.
 */
public final class AccountStore {

	private final Path dir;
	private final Path file;
	private final CryptoUtils crypto;
	private final List<Account> accounts = new ArrayList<>();

	public AccountStore(Path dir) {
		this.dir = dir;
		this.file = dir.resolve("accounts.json");
		this.crypto = new CryptoUtils(dir.resolve(".key"));
	}

	public List<Account> accounts() {
		return Collections.unmodifiableList(accounts);
	}

	public void add(Account account) {
		accounts.removeIf(a -> a.token().equals(account.token()));
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
				accounts.add(new Account(
						o.get("label").getAsString(),
						o.get("username").getAsString(),
						o.get("uuid").getAsString(),
						crypto.decrypt(o.get("token").getAsString())));
			}
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Could not read accounts.json", e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(dir);
			JsonArray arr = new JsonArray();
			for (Account a : accounts) {
				JsonObject o = new JsonObject();
				o.addProperty("label", a.label());
				o.addProperty("username", a.username());
				o.addProperty("uuid", a.uuid());
				o.addProperty("token", crypto.encrypt(a.token()));
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
