package dev.elv1n200.sessionlogin.account;

/**
 * One saved account, identified purely by its session token — there is no
 * email/password field anywhere by design.
 *
 * <p>{@code token} is the plaintext bearer token while the vault is unlocked,
 * or empty when locked. {@code encToken} always keeps the on-disk ciphertext
 * so a locked store can still be re-saved without data loss.
 */
public final class Account {
	private String label;
	private final String username;
	private final String uuid;
	private String token;
	private String notes;
	private long lastUsed;
	private String encToken;

	public Account(String label, String username, String uuid, String token) {
		this.label = label;
		this.username = username;
		this.uuid = uuid;
		this.token = token;
		this.notes = "";
		this.lastUsed = 0L;
		this.encToken = "";
	}

	public String label() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String username() {
		return username;
	}

	public String uuid() {
		return uuid;
	}

	public String token() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public boolean hasToken() {
		return token != null && !token.isEmpty();
	}

	public String notes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes == null ? "" : notes;
	}

	public long lastUsed() {
		return lastUsed;
	}

	public void setLastUsed(long lastUsed) {
		this.lastUsed = lastUsed;
	}

	public void touch() {
		this.lastUsed = System.currentTimeMillis();
	}

	public String encToken() {
		return encToken;
	}

	public void setEncToken(String encToken) {
		this.encToken = encToken == null ? "" : encToken;
	}
}
