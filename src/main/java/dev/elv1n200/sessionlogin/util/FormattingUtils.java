package dev.elv1n200.sessionlogin.util;

import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class FormattingUtils {

	private FormattingUtils() {
	}

	/** Wrap text in Minecraft's scrambling "magic text" effect for flair. */
	public static Text surroundWithObfuscated(Text baseText, int count) {
		Style baseStyle = baseText.getStyle().withObfuscated(false);
		Style obfStyle = baseStyle.withObfuscated(true);
		String padding = "@".repeat(Math.max(0, count));

		return Text.empty()
				.append(Text.literal(padding + " ").setStyle(obfStyle))
				.append(baseText.copy().setStyle(baseStyle))
				.append(Text.literal(" " + padding).setStyle(obfStyle));
	}
}
