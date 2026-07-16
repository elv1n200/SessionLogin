package dev.elv1n200.sessionlogin.util;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * Minecraft-style toasts triggered by login or account switches.
 * Respects the showToasts setting and is safe to call from any thread.
 */
public final class Notifier {

	private static final SystemToast.SystemToastId SL_TOAST =
			new SystemToast.SystemToastId();

	private Notifier() {
	}

	public static void loggedIn(String username) {
		toast(Component.literal("SessionLogin").withStyle(ChatFormatting.GOLD),
				Component.literal("Logged in as " + username).withStyle(ChatFormatting.WHITE));
	}

	public static void info(String title, String message) {
		toast(Component.literal(title).withStyle(ChatFormatting.GOLD),
				Component.literal(message).withStyle(ChatFormatting.WHITE));
	}

	private static void toast(Component title, Component body) {
		if (SessionLogin.settings == null || !SessionLogin.settings.showToasts()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) {
			return;
		}
		mc.execute(() ->
				SystemToast.add(mc.getToastManager(), SL_TOAST, title, body));
	}
}
