package dev.elv1n200.sessionlogin.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Local-only token-at-rest protection.
 *
 * <p>This is obfuscation-grade, NOT a vault: the AES key lives in a file next
 * to the data, so anyone with read access to your config dir can decrypt it.
 * Its only job is to keep raw bearer tokens from sitting in plain text where a
 * stray screen-share, backup, or grep would expose them. It is never sent
 * anywhere.
 */
public final class CryptoUtils {

	private static final int GCM_TAG_BITS = 128;
	private static final int IV_BYTES = 12;
	private static final SecureRandom RNG = new SecureRandom();

	private final SecretKey key;

	public CryptoUtils(Path keyFile) {
		this.key = loadOrCreateKey(keyFile);
	}

	private static SecretKey loadOrCreateKey(Path keyFile) {
		try {
			if (Files.exists(keyFile)) {
				byte[] raw = Base64.getDecoder().decode(
						Files.readString(keyFile).trim());
				return new SecretKeySpec(raw, "AES");
			}
			KeyGenerator gen = KeyGenerator.getInstance("AES");
			gen.init(256);
			SecretKey k = gen.generateKey();
			Files.createDirectories(keyFile.getParent());
			Files.writeString(keyFile,
					Base64.getEncoder().encodeToString(k.getEncoded()));
			return k;
		} catch (Exception e) {
			throw new IllegalStateException("Could not init local key", e);
		}
	}

	public String encrypt(String plain) {
		try {
			byte[] iv = new byte[IV_BYTES];
			RNG.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key,
					new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] ct = cipher.doFinal(plain.getBytes("UTF-8"));
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
			return new String(pt, "UTF-8");
		} catch (Exception e) {
			throw new IllegalStateException("decrypt failed", e);
		}
	}
}
