package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

/**
 * Adds an offline ("cracked") account. No authentication, just a username
 * and the canonical OfflinePlayer:&lt;name&gt; UUID derivation. Useful for
 * offline-mode servers or LAN.
 */
public class AddOfflineAccountScreen extends Screen {

	private EditBox nameField;
	private Component status = Component.literal("Enter a username (3–16 chars, A–Z 0–9 _)")
			.withStyle(ChatFormatting.GRAY);

	public AddOfflineAccountScreen() {
		super(Component.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		nameField = new EditBox(this.font,
				cx - 100, cy, 200, 20, Component.literal("Username"));
		nameField.setMaxLength(16);
		this.addRenderableWidget(nameField);
		this.setInitialFocus(nameField);

		this.addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
			String name = nameField.getValue().trim();
			if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) {
				status = Component.literal("Invalid username").withStyle(ChatFormatting.RED);
				return;
			}
			Account acc = new Account(name, name,
					Account.offlineUuid(name).toString(), "",
					Account.Type.OFFLINE);
			acc.touch();
			SessionLogin.accountStore.add(acc);
			status = Component.literal("Saved offline account: " + name)
					.withStyle(ChatFormatting.GREEN);
		}).bounds(cx - 100, cy + 25, 97, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new AccountManagerScreen());
		}).bounds(cx + 3, cy + 25, 97, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int cy = this.height / 2;
		extractor.centeredText(this.font,
				surroundWithObfuscated(Component.literal("Add Offline Account")
						.withStyle(ChatFormatting.AQUA), 4),
				cx, cy - 40, 0xFFFFFF);
		extractor.centeredText(this.font, status, cx, cy - 22, 0xFFFFFF);
	}
}
