package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.util.IntegrityCheck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

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
		super(Text.literal("SessionLogin tamper warning"));
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
		// Screen.renderWithTooltip already called renderBackground (blur) for
		// us this frame, so we must NOT call it again here or we trigger the
		// "Can only blur once per frame" assertion. super.render only draws
		// the registered widgets (our two buttons); we layer the warning text
		// on top using plain String overloads with explicit ARGB colors so
		// no MutableText/formatting weirdness can swallow the draw call.
		super.render(context, mouseX, mouseY, delta);

		int cx = this.width / 2;
		int cy = this.height / 2;

		context.drawCenteredTextWithShadow(this.textRenderer,
				"SessionLogin: jar tampering detected",
				cx, cy - 80, COLOR_RED);
		context.drawCenteredTextWithShadow(this.textRenderer,
				"The mod jar's hash changed since the last launch.",
				cx, cy - 60, COLOR_YELLOW);
		context.drawCenteredTextWithShadow(this.textRenderer,
				"If you just updated the mod, choose Trust.",
				cx, cy - 46, COLOR_WHITE);
		context.drawCenteredTextWithShadow(this.textRenderer,
				"If you did NOT, quit and reinstall from the official source.",
				cx, cy - 32, COLOR_WHITE);

		context.drawCenteredTextWithShadow(this.textRenderer,
				"Recorded: " + shortHash(IntegrityCheck.storedHash()),
				cx, cy - 10, COLOR_GREY);
		context.drawCenteredTextWithShadow(this.textRenderer,
				"Current:  " + shortHash(IntegrityCheck.currentHash()),
				cx, cy + 4, COLOR_GREY);
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
