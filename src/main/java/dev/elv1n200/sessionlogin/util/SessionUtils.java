package dev.elv1n200.sessionlogin.util;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.util.Optional;
import java.util.UUID;

public final class SessionUtils {

	private SessionUtils() {
	}

	public static String getUsername() {
		return MinecraftClient.getInstance().getSession().getUsername();
	}

	public static Session getSession() {
		return MinecraftClient.getInstance().getSession();
	}

	/** Insert dashes into a 32-char Mojang UUID; pass through otherwise. */
	public static String dashUuid(String uuid) {
		if (uuid != null && uuid.length() == 32) {
			return uuid.substring(0, 8) + "-"
					+ uuid.substring(8, 12) + "-"
					+ uuid.substring(12, 16) + "-"
					+ uuid.substring(16, 20) + "-"
					+ uuid.substring(20);
		}
		return uuid;
	}

	public static Session createSession(String username, String uuid, String token) {
		return new Session(
				username,
				UUID.fromString(dashUuid(uuid)),
				token,
				Optional.empty(),
				Optional.empty());
	}

	public static Session createSession(String username, UUID uuid, String token) {
		return new Session(
				username,
				uuid,
				token,
				Optional.empty(),
				Optional.empty());
	}

	public static void setSession(Session session) {
		SessionLogin.currentSession = session;
	}

	public static void restoreSession() {
		SessionLogin.currentSession = SessionLogin.originalSession;
	}

	public static boolean isOriginalActive() {
		return SessionLogin.currentSession.equals(SessionLogin.originalSession);
	}
}
