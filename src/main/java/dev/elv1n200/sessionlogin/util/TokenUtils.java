package dev.elv1n200.sessionlogin.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Helpers for the raw bearer token: cleanup + expiry parsing. */
public final class TokenUtils {

	private TokenUtils() {
	}

	/**
	 * Accepts what people usually copy and returns just the JWT:
	 * trims whitespace, drops a "Bearer " prefix, and strips a trailing
	 * ":&lt;32-hex-uuid&gt;" suffix (the common token:uuid dump format).
	 */
	public static String clean(String raw) {
		if (raw == null) {
			return "";
		}
		String s = raw.trim();
		if (s.regionMatches(true, 0, "Bearer ", 0, 7)) {
			s = s.substring(7).trim();
		}
		int colon = s.lastIndexOf(':');
		if (colon > 0) {
			String tail = s.substring(colon + 1);
			if (tail.matches("[0-9a-fA-F]{32}")
					|| tail.matches(
					"[0-9a-fA-F-]{36}")) {
				s = s.substring(0, colon);
			}
		}
		return s.trim();
	}

	/** Epoch seconds from the JWT {@code exp} claim, or -1 if unknown. */
	public static long expiryEpochSeconds(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length < 2) {
				return -1;
			}
			byte[] payload = Base64.getUrlDecoder().decode(pad(parts[1]));
			JsonObject json = JsonParser
					.parseString(new String(payload, StandardCharsets.UTF_8))
					.getAsJsonObject();
			return json.has("exp") ? json.get("exp").getAsLong() : -1;
		} catch (Exception e) {
			return -1;
		}
	}

	/** Human-readable remaining lifetime, e.g. "7h 12m" or "expired". */
	public static String expiryLabel(String token) {
		long exp = expiryEpochSeconds(token);
		if (exp < 0) {
			return "unknown";
		}
		long secs = exp - (System.currentTimeMillis() / 1000L);
		if (secs <= 0) {
			return "expired";
		}
		long h = secs / 3600;
		long m = (secs % 3600) / 60;
		if (h >= 24) {
			return (h / 24) + "d " + (h % 24) + "h";
		}
		return h + "h " + m + "m";
	}

	/** True if the token has a parseable exp that is already in the past. */
	public static boolean isExpired(String token) {
		long exp = expiryEpochSeconds(token);
		return exp >= 0 && exp <= System.currentTimeMillis() / 1000L;
	}

	private static String pad(String b64url) {
		int rem = b64url.length() % 4;
		if (rem == 0) {
			return b64url;
		}
		return b64url + "====".substring(rem);
	}
}
