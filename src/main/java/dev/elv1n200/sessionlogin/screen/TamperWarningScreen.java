package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.util.IntegrityCheck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Shown once on game start if the running jar's SHA-256 doesn't match the
 * hash recorded on the previous launch. User chooses to trust the new build
 * or quit.
 */
public class TamperWarningScreen extends Screen {

	public TamperWarningScreen() {
		super(Text.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Trust this build (update recorded hash)"), b -> {
					IntegrityCheck.acceptCurrent();
					assert this.client != null;
					this.client.setScreen(new TitleScreen());
				}).dimensions(cx - 160, cy + 50, 320, 20).build());

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Quit game"), b -> MinecraftClient.getInstance().scheduleStop()
		).dimensions(cx - 160, cy + 75, 320, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int cy = this.height / 2;

		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("⚠ SessionLogin: jar tampering detected")
						.formatted(Formatting.RED, Formatting.BOLD),
				cx, cy - 80, 0xFFFFFF);

		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("The mod jar's hash changed since the last launch.")
						.formatted(Formatting.YELLOW),
				cx, cy - 60, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("If you just updated the mod, choose Trust.")
						.formatted(Formatting.GRAY),
				cx, cy - 48, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("If you did NOT, quit and reinstall from the official source.")
						.formatted(Formatting.GRAY),
				cx, cy - 36, 0xFFFFFF);

		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Recorded: " + shortHash(IntegrityCheck.storedHash()))
						.formatted(Formatting.DARK_GRAY),
				cx, cy - 12, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Current:  " + shortHash(IntegrityCheck.currentHash()))
						.formatted(Formatting.DARK_GRAY),
				cx, cy, 0xFFFFFF);

		super.render(context, mouseX, mouseY, delta);
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
