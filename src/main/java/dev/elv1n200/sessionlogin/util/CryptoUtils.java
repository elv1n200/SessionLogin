package dev.elv1n200.sessionlogin.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM string encryption.
 *
 * <p>Two ways to obtain an instance:
 * <ul>
 *   <li>{@link #localKey(Path)} — random key stored next to the data
 *       (obfuscation-grade: deters casual snooping, not an attacker with
 *       file access).</li>
 *   <li>{@link #fromPassword(char[], byte[])} — key derived from a master
 *       password via PBKDF2. Nothing secret is stored on disk, so the
 *       vault is genuinely protected and portable.</li>
 * </ul>
 */
public final class CryptoUtils {

	private static final int GCM_TAG_BITS = 128;
	private static final int IV_BYTES = 12;
	private static final int PBKDF2_ITERATIONS = 210_000;
	private static final SecureRandom RNG = new SecureRandom();

	private final SecretKey key;

	private CryptoUtils(SecretKey key) {
		this.key = key;
	}

	public static CryptoUtils localKey(Path keyFile) {
		try {
			if (Files.exists(keyFile)) {
				byte[] raw = Base64.getDecoder()
						.decode(Files.readString(keyFile).trim());
				return new CryptoUtils(new SecretKeySpec(raw, "AES"));
			}
			KeyGenerator gen = KeyGenerator.getInstance("AES");
			gen.init(256);
			SecretKey k = gen.generateKey();
			Files.createDirectories(keyFile.getParent());
			Files.writeString(keyFile,
					Base64.getEncoder().encodeToString(k.getEncoded()));
			return new CryptoUtils(k);
		} catch (Exception e) {
			throw new IllegalStateException("Could not init local key", e);
		}
	}

	public static CryptoUtils fromPassword(char[] password, byte[] salt) {
		try {
			SecretKeyFactory f =
					SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			PBEKeySpec spec =
					new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, 256);
			byte[] derived = f.generateSecret(spec).getEncoded();
			return new CryptoUtils(new SecretKeySpec(derived, "AES"));
		} catch (Exception e) {
			throw new IllegalStateException("Could not derive key", e);
		}
	}

	public static byte[] newSalt() {
		byte[] salt = new byte[16];
		RNG.nextBytes(salt);
		return salt;
	}

	public String encrypt(String plain) {
		try {
			byte[] iv = new byte[IV_BYTES];
			RNG.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key,
					new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			byte[] out = new byte[iv.length + ct.length];
			System.arraycopy(iv, 0, out, 0, iv.length);
			System.arraycopy(ct, 0, out, iv.length, ct.length);
			return Base64.getEncoder().encodeToString(out);
		} catch (Exception e) {
			throw new IllegalStateException("encrypt failed", e);
		}
	}

	public String decrypt(String encoded) {
		try {
			byte[] in = Base64.getDecoder().decode(encoded);
			byte[] iv = new byte[IV_BYTES];
			System.arraycopy(in, 0, iv, 0, IV_BYTES);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key,
					new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] pt = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
			return new String(pt, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("decrypt failed", e);
		}
	}
}
