package dev.elv1n200.sessionlogin.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.elv1n200.sessionlogin.SessionLogin;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

/**
 * Trust-on-first-use jar tampering detector.
 *
 * <p>First run on a machine: record the SHA-256 of the running jar in
 * {@code config/sessionlogin/integrity.json}. Subsequent runs compare. A
 * mismatch likely means either:
 * <ul>
 *   <li>You legitimately swapped to a new build — accept the new hash; or</li>
 *   <li>Someone modified your jar (token-stealer wrapper, etc.).</li>
 * </ul>
 * The decision is left to the user via {@link
 * dev.elv1n200.sessionlogin.screen.TamperWarningScreen}.
 */
public final class IntegrityCheck {

	public enum Status {OK, FIRST_RUN, MISMATCH, UNAVAILABLE}

	private static String currentHash = null;
	private static String storedHash = null;
	private static Status status = Status.UNAVAILABLE;
	private static Path jarPath = null;
	private static Path file = null;

	private IntegrityCheck() {
	}

	public static void run(Path configDir) {
		file = configDir.resolve("integrity.json");
		jarPath = resolveJar();
		if (jarPath == null) {
			status = Status.UNAVAILABLE;
			return;
		}
		try {
			currentHash = sha256(jarPath);
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Integrity hash failed", e);
			status = Status.UNAVAILABLE;
			return;
		}
		try {
			if (!Files.exists(file)) {
				writeHash(currentHash);
				status = Status.FIRST_RUN;
				return;
			}
			JsonObject o = JsonParser.parseString(Files.readString(file))
					.getAsJsonObject();
			storedHash = o.has("sha256") ? o.get("sha256").getAsString() : null;
			if (storedHash == null) {
				writeHash(currentHash);
				status = Status.FIRST_RUN;
			} else if (storedHash.equals(currentHash)) {
				status = Status.OK;
			} else {
				status = Status.MISMATCH;
			}
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Integrity read failed", e);
			status = Status.UNAVAILABLE;
		}
	}

	/** User accepted a new jar — overwrite the recorded hash. */
	public static void acceptCurrent() {
		try {
			writeHash(currentHash);
			storedHash = currentHash;
			status = Status.OK;
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Could not update integrity.json", e);
		}
	}

	public static Status status() {
		return status;
	}

	public static String currentHash() {
		return currentHash;
	}

	public static String storedHash() {
		return storedHash;
	}

	public static Path jarPath() {
		return jarPath;
	}

	private static void writeHash(String hash) throws Exception {
		Files.createDirectories(file.getParent());
		JsonObject o = new JsonObject();
		o.addProperty("sha256", hash);
		o.addProperty("recordedAt", System.currentTimeMillis());
		Files.writeString(file, o.toString());
	}

	private static Path resolveJar() {
		try {
			URI src = SessionLogin.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI();
			Path p = Paths.get(src);
			return Files.isRegularFile(p) ? p : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String sha256(Path p) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] buf = new byte[8192];
		try (var in = Files.newInputStream(p)) {
			int n;
			while ((n = in.read(buf)) > 0) {
				md.update(buf, 0, n);
			}
		}
		byte[] digest = md.digest();
		StringBuilder sb = new StringBuilder(digest.length * 2);
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
