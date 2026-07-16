package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

public class SettingsScreen extends Screen {

	private final Screen parent;

	public SettingsScreen(Screen parent) {
		super(Component.literal(""));
		this.parent = parent;
	}

	public SettingsScreen() {
		this(null);
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int top = this.height / 2 - 80;

		// NOTE: the "isolate pack cache per account" toggle is omitted on 26.x —
		// its DownloaderMixin has no clean equivalent in the unobfuscated era.

		addToggle(top + 0,
				"Spoof brand as Vanilla",
				"Tell servers you're a vanilla client (instead of 'fabric').",
				SessionLogin.settings::spoofBrandAsVanilla,
				SessionLogin.settings::setSpoofBrandAsVanilla);

		addToggle(top + 28,
				"Block Mojang telemetry",
				"Drop telemetry events Mojang would otherwise receive.",
				SessionLogin.settings::blockTelemetry,
				SessionLogin.settings::setBlockTelemetry);

		addToggle(top + 56,
				"Show toasts on login/switch",
				"Pop a Minecraft toast in the corner when an account is swapped.",
				SessionLogin.settings::showToasts,
				SessionLogin.settings::setShowToasts);

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(parent != null ? parent : new AccountManagerScreen());
		}).bounds(cx - 100, top + 140, 200, 20).build());
	}

	private void addToggle(int y, String label, String tooltip,
						   BooleanSupplier get, Consumer<Boolean> set) {
		int cx = this.width / 2;
		boolean current = get.getAsBoolean();
		Button btn = Button.builder(
				Component.literal(label + ": ")
						.append(Component.literal(current ? "ON" : "OFF")
								.withStyle(current ? ChatFormatting.GREEN : ChatFormatting.RED)),
				b -> {
					set.accept(!get.getAsBoolean());
					this.rebuildWidgets();
				}
		).bounds(cx - 150, y, 300, 20)
				.tooltip(Tooltip.create(Component.literal(tooltip)))
				.build();
		this.addRenderableWidget(btn);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		extractor.centeredText(this.font,
				surroundWithObfuscated(Component.literal("Settings")
						.withStyle(ChatFormatting.AQUA), 4),
				this.width / 2, this.height / 2 - 100, 0xFFFFFF);
	}
}
