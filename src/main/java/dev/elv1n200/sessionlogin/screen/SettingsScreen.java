package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

public class SettingsScreen extends Screen {

	private final Screen parent;

	public SettingsScreen(Screen parent) {
		super(Text.literal(""));
		this.parent = parent;
	}

	public SettingsScreen() {
		this(null);
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int top = this.height / 2 - 80;

		addToggle(top + 0,
				"Isolate pack cache per account",
				"Servers can fingerprint your machine across alts via cached resource\n"
						+ "packs. When ON, each UUID gets its own cache folder.",
				SessionLogin.settings::isolatePackCache,
				SessionLogin.settings::setIsolatePackCache);

		addToggle(top + 28,
				"Spoof brand as Vanilla",
				"Tell servers you're a vanilla client (instead of 'fabric').",
				SessionLogin.settings::spoofBrandAsVanilla,
				SessionLogin.settings::setSpoofBrandAsVanilla);

		addToggle(top + 56,
				"Block Mojang telemetry",
				"Drop telemetry events Mojang would otherwise receive.",
				SessionLogin.settings::blockTelemetry,
				SessionLogin.settings::setBlockTelemetry);

		addToggle(top + 84,
				"Show toasts on login/switch",
				"Pop a Minecraft toast in the corner when an account is swapped.",
				SessionLogin.settings::showToasts,
				SessionLogin.settings::setShowToasts);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
			assert this.client != null;
			this.client.setScreen(parent != null ? parent : new AccountManagerScreen());
		}).dimensions(cx - 100, top + 140, 200, 20).build());
	}

	private void addToggle(int y, String label, String tooltip,
						   BooleanSupplier get, Consumer<Boolean> set) {
		int cx = this.width / 2;
		boolean current = get.getAsBoolean();
		ButtonWidget btn = ButtonWidget.builder(
				Text.literal(label + ": ")
						.append(Text.literal(current ? "ON" : "OFF")
								.formatted(current ? Formatting.GREEN : Formatting.RED)),
				b -> {
					set.accept(!get.getAsBoolean());
					this.clearAndInit();
				}
		).dimensions(cx - 150, y, 300, 20)
				.tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(tooltip)))
				.build();
		this.addDrawableChild(btn);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer,
				surroundWithObfuscated(Text.literal("Settings")
						.formatted(Formatting.AQUA), 4),
				this.width / 2, this.height / 2 - 100, 0xFFFFFF);
	}
}
