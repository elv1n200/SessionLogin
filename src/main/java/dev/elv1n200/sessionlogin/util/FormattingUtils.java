package dev.elv1n200.sessionlogin.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class FormattingUtils {

	private FormattingUtils() {
	}

	/** Wrap text in Minecraft's scrambling "magic text" effect for flair. */
	public static Component surroundWithObfuscated(Component baseText, int count) {
		Style baseStyle = baseText.getStyle().withObfuscated(false);
		Style obfStyle = baseStyle.withObfuscated(true);
		String padding = "@".repeat(Math.max(0, count));

		return Component.empty()
				.append(Component.literal(padding + " ").setStyle(obfStyle))
				.append(baseText.copy().setStyle(baseStyle))
				.append(Component.literal(" " + padding).setStyle(obfStyle));
	}
}
