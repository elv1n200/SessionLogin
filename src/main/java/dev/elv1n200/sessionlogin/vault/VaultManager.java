package dev.elv1n200.sessionlogin.vault;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.util.CryptoUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Owns the encryption mode for the account store.
 *
 * <p>"local"   – random key in {@code .key}, always unlocked (obfuscation).<br>
 * "password" – PBKDF2-derived key, locked until the master password is
 * entered. Nothing secret is on disk, so it is safe to back up / sync.
 */
public final class VaultManager {

	public static final String MODE_LOCAL = "local";
	public static final String MODE_PASSWORD = "password";

	private static final String VERIFIER_PLAINTEXT = "SESSIONLOGIN_OK";

	private final Path dir;
	private final Path metaFile;
	private final Path keyFile;

	private String mode = MODE_LOCAL;
	private byte[] salt;
	private String verifier;
	private CryptoUtils crypto;

	public VaultManager(Path dir) {
		this.dir = dir;
		this.metaFile = dir.resolve("meta.json");
		this.keyFile = dir.resolve(".key");
	}

	public void init() {
		try {
			if (Files.exists(metaFile)) {
				JsonObject m = JsonParser.parseString(
						Files.readString(metaFile)).getAsJsonObject();
				mode = m.get("mode").getAsString();
				if (m.has("salt")) {
					salt = Base64.getDecoder().decode(m.get("salt").getAsString());
				}
				if (m.has("verifier")) {
					verifier = m.get("verifier").getAsString();
				}
			} else {
				mode = MODE_LOCAL;
			}
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Vault meta unreadable, using local", e);
			mode = MODE_LOCAL;
		}

		if (MODE_LOCAL.equals(mode)) {
			crypto = CryptoUtils.localKey(keyFile);
			if (verifier == null) {
				verifier = crypto.encrypt(VERIFIER_PLAINTEXT);
				writeMeta();
			}
		}
		// password mode: stays locked until unlock() succeeds
	}

	public String mode() {
		return mode;
	}

	public boolean isPasswordMode() {
		return MODE_PASSWORD.equals(mode);
	}

	public boolean isUnlocked() {
		return crypto != null;
	}

	public CryptoUtils crypto() {
		if (crypto == null) {
			throw new IllegalStateException("Vault is locked");
		}
		return crypto;
	}

	/** @return true if the password was correct and the vault is now open. */
	public boolean unlock(char[] password) {
		if (!isPasswordMode() || salt == null || verifier == null) {
			return false;
		}
		try {
			CryptoUtils c = CryptoUtils.fromPassword(password, salt);
			if (VERIFIER_PLAINTEXT.equals(c.decrypt(verifier))) {
				crypto = c;
				return true;
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	/**
	 * Switch to password mode. {@code reencrypt} is invoked while both the
	 * old (still set) and new crypto are available so the store can rewrite
	 * tokens; here we simply swap then let the caller re-save.
	 *
	 * @return true on success (vault must be unlocked first).
	 */
	public boolean enablePassword(char[] newPassword) {
		if (!isUnlocked()) {
			return false;
		}
		byte[] newSalt = CryptoUtils.newSalt();
		CryptoUtils c = CryptoUtils.fromPassword(newPassword, newSalt);
		this.salt = newSalt;
		this.verifier = c.encrypt(VERIFIER_PLAINTEXT);
		this.crypto = c;
		this.mode = MODE_PASSWORD;
		try {
			Files.deleteIfExists(keyFile);
		} catch (Exception ignored) {
		}
		writeMeta();
		return true;
	}

	/** Switch back to local (no password) mode. Must be unlocked. */
	public boolean disablePassword() {
		if (!isUnlocked()) {
			return false;
		}
		try {
			Files.deleteIfExists(keyFile);
		} catch (Exception ignored) {
		}
		CryptoUtils c = CryptoUtils.localKey(keyFile);
		this.crypto = c;
		this.mode = MODE_LOCAL;
		this.salt = null;
		this.verifier = c.encrypt(VERIFIER_PLAINTEXT);
		writeMeta();
		return true;
	}

	private void writeMeta() {
		try {
			Files.createDirectories(dir);
			JsonObject m = new JsonObject();
			m.addProperty("mode", mode);
			if (salt != null) {
				m.addProperty("salt", Base64.getEncoder().encodeToString(salt));
			}
			if (verifier != null) {
				m.addProperty("verifier", verifier);
			}
			Files.writeString(metaFile, m.toString());
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Could not write vault meta", e);
		}
	}
}
