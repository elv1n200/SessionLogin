package dev.elv1n200.sessionlogin.util;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Minecraft-style toasts triggered by login or account switches.
 * Respects the showToasts setting and is safe to call from any thread.
 */
public final class Notifier {

	private static final SystemToast.Type SL_TOAST = new SystemToast.Type();

	private Notifier() {
	}

	public static void loggedIn(String username) {
		toast(Text.literal("SessionLogin").formatted(Formatting.GOLD),
				Text.literal("Logged in as " + username).formatted(Formatting.WHITE));
	}

	public static void info(String title, String message) {
		toast(Text.literal(title).formatted(Formatting.GOLD),
				Text.literal(message).formatted(Formatting.WHITE));
	}

	private static void toast(Text title, Text body) {
		if (SessionLogin.settings == null || !SessionLogin.settings.showToasts()) {
			return;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null) {
			return;
		}
		mc.execute(() ->
				SystemToast.show(mc.getToastManager(), SL_TOAST, title, body));
	}
}
