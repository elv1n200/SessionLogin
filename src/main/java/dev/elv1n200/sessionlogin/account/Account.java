package dev.elv1n200.sessionlogin.account;

/**
 * One saved account. Identified purely by its session token —
 * there is no email/password field anywhere by design.
 */
public final class Account {
	private final String label;
	private final String username;
	private final String uuid;
	private final String token;

	public Account(String label, String username, String uuid, String token) {
		this.label = label;
		this.username = username;
		this.uuid = uuid;
		this.token = token;
	}

	public String label() {
		return label;
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
}
