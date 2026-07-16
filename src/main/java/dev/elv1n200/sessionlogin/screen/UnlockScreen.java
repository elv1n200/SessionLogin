package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

/** Prompts for the master password and unlocks the vault. */
public class UnlockScreen extends Screen {

	private EditBox pwField;
	private Component status = Component.literal("Vault is locked")
			.withStyle(ChatFormatting.YELLOW);

	public UnlockScreen() {
		super(Component.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		pwField = new EditBox(this.font,
				cx - 100, cy, 200, 20, Component.literal("Master password"));
		pwField.setMaxLength(128);
		pwField.addFormatter((s, i) ->
				Component.literal("*".repeat(s.length())).getVisualOrderText());
		this.addRenderableWidget(pwField);
		this.setInitialFocus(pwField);

		this.addRenderableWidget(Button.builder(Component.literal("Unlock"), b -> {
			char[] pw = pwField.getValue().toCharArray();
			if (SessionLogin.vault.unlock(pw)) {
				SessionLogin.accountStore.decryptAll();
				assert this.minecraft != null;
				this.minecraft.setScreen(new AccountManagerScreen());
			} else {
				status = surroundWithObfuscated(Component.literal("Wrong password")
						.withStyle(ChatFormatting.RED), 4);
			}
		}).bounds(cx - 100, cy + 25, 97, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
		}).bounds(cx + 3, cy + 25, 97, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		extractor.centeredText(this.font, this.status,
				this.width / 2, this.height / 2 - 30, 0xFFFFFF);
	}
}
