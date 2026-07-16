package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.util.IntegrityCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Shown once on game start if the running jar's SHA-256 doesn't match the
 * hash recorded on the previous launch. User chooses to trust the new build
 * or quit.
 */
public class TamperWarningScreen extends Screen {

	private static final int COLOR_RED   = 0xFFFF5555;
	private static final int COLOR_YELLOW = 0xFFFFFF55;
	private static final int COLOR_WHITE = 0xFFFFFFFF;
	private static final int COLOR_GREY  = 0xFFAAAAAA;

	public TamperWarningScreen() {
		super(Component.literal("SessionLogin tamper warning"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		this.addRenderableWidget(Button.builder(
				Component.literal("Trust this build (update recorded hash)"), b -> {
					IntegrityCheck.acceptCurrent();
					assert this.minecraft != null;
					this.minecraft.setScreen(new TitleScreen());
				}).bounds(cx - 160, cy + 50, 320, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Quit game"), b -> Minecraft.getInstance().stop()
		).bounds(cx - 160, cy + 75, 320, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);

		int cx = this.width / 2;
		int cy = this.height / 2;

		extractor.centeredText(this.font,
				"SessionLogin: jar tampering detected", cx, cy - 80, COLOR_RED);
		extractor.centeredText(this.font,
				"The mod jar's hash changed since the last launch.", cx, cy - 60, COLOR_YELLOW);
		extractor.centeredText(this.font,
				"If you just updated the mod, choose Trust.", cx, cy - 46, COLOR_WHITE);
		extractor.centeredText(this.font,
				"If you did NOT, quit and reinstall from the official source.", cx, cy - 32, COLOR_WHITE);

		extractor.centeredText(this.font,
				"Recorded: " + shortHash(IntegrityCheck.storedHash()), cx, cy - 10, COLOR_GREY);
		extractor.centeredText(this.font,
				"Current:  " + shortHash(IntegrityCheck.currentHash()), cx, cy + 4, COLOR_GREY);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private static String shortHash(String hash) {
		if (hash == null) {
			return "<none>";
		}
		return hash.length() > 16 ? hash.substring(0, 16) + "..." : hash;
	}
}
