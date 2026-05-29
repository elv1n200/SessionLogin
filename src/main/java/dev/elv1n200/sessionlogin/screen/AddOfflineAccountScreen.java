package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

/**
 * Adds an offline ("cracked") account. No authentication, just a username
 * and the canonical OfflinePlayer:&lt;name&gt; UUID derivation. Useful for
 * offline-mode servers or LAN.
 */
public class AddOfflineAccountScreen extends Screen {

	private TextFieldWidget nameField;
	private Text status = Text.literal("Enter a username (3–16 chars, A–Z 0–9 _)")
			.formatted(Formatting.GRAY);

	public AddOfflineAccountScreen() {
		super(Text.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		nameField = new TextFieldWidget(this.textRenderer,
				cx - 100, cy, 200, 20, Text.literal("Username"));
		nameField.setMaxLength(16);
		nameField.setFocused(true);
		this.addSelectableChild(nameField);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> {
			String name = nameField.getText().trim();
			if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) {
				status = Text.literal("Invalid username").formatted(Formatting.RED);
				return;
			}
			Account acc = new Account(name, name,
					Account.offlineUuid(name).toString(), "",
					Account.Type.OFFLINE);
			acc.touch();
			SessionLogin.accountStore.add(acc);
			status = Text.literal("Saved offline account: " + name)
					.formatted(Formatting.GREEN);
		}).dimensions(cx - 100, cy + 25, 97, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
			assert this.client != null;
			this.client.setScreen(new AccountManagerScreen());
		}).dimensions(cx + 3, cy + 25, 97, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int cy = this.height / 2;
		context.drawCenteredTextWithShadow(this.textRenderer,
				surroundWithObfuscated(Text.literal("Add Offline Account")
						.formatted(Formatting.AQUA), 4),
				cx, cy - 40, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, status,
				cx, cy - 22, 0xFFFFFF);
		nameField.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		return nameField.keyPressed(input) || nameField.isActive()
				|| super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		return nameField.charTyped(input) || super.charTyped(input);
	}
}
