package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

/**
 * Enable / change / remove the master password. Re-encrypts every saved
 * token with the new key. The vault must be unlocked first.
 */
public class SetPasswordScreen extends Screen {

	private TextFieldWidget pwField;
	private TextFieldWidget confirmField;
	private Text status = Text.empty();

	public SetPasswordScreen() {
		super(Text.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		pwField = passwordField(cx - 100, cy - 20, "New password");
		this.addSelectableChild(pwField);
		confirmField = passwordField(cx - 100, cy + 10, "Confirm password");
		this.addSelectableChild(confirmField);

		boolean unlocked = SessionLogin.vault.isUnlocked();
		boolean pwMode = SessionLogin.vault.isPasswordMode();

		ButtonWidget setBtn = ButtonWidget.builder(
				Text.literal(pwMode ? "Change" : "Enable"), b -> {
					String p = pwField.getText();
					if (p.length() < 4) {
						status = err("Password too short (min 4)");
						return;
					}
					if (!p.equals(confirmField.getText())) {
						status = err("Passwords do not match");
						return;
					}
					if (SessionLogin.vault.enablePassword(p.toCharArray())) {
						SessionLogin.accountStore.reencryptAll();
						status = surroundWithObfuscated(Text.literal(
								"Master password set")
								.formatted(Formatting.GREEN), 4);
					} else {
						status = err("Unlock the vault first");
					}
				}).dimensions(cx - 100, cy + 40, 97, 20).build();
		setBtn.active = unlocked;
		this.addDrawableChild(setBtn);

		ButtonWidget removeBtn = ButtonWidget.builder(
				Text.literal("Remove"), b -> {
					if (SessionLogin.vault.disablePassword()) {
						SessionLogin.accountStore.reencryptAll();
						status = surroundWithObfuscated(Text.literal(
								"Switched to local mode")
								.formatted(Formatting.GREEN), 4);
					} else {
						status = err("Unlock the vault first");
					}
				}).dimensions(cx + 3, cy + 40, 97, 20).build();
		removeBtn.active = unlocked && pwMode;
		this.addDrawableChild(removeBtn);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
			assert this.client != null;
			this.client.setScreen(new AccountManagerScreen());
		}).dimensions(cx - 100, cy + 64, 200, 20).build());

		if (!unlocked) {
			status = err("Vault locked — unlock before changing password");
		}
	}

	private TextFieldWidget passwordField(int x, int y, String hint) {
		TextFieldWidget f = new TextFieldWidget(this.textRenderer,
				x, y, 200, 20, Text.literal(hint));
		f.setMaxLength(128);
		f.addFormatter((s, i) ->
				Text.literal("*".repeat(s.length())).asOrderedText());
		return f;
	}

	private static Text err(String m) {
		return surroundWithObfuscated(
				Text.literal(m).formatted(Formatting.RED), 4);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer,
				surroundWithObfuscated(Text.literal("Master Password")
						.formatted(Formatting.AQUA), 4),
				this.width / 2, this.height / 2 - 46, 0xFFFFFF);
		pwField.render(context, mouseX, mouseY, delta);
		confirmField.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.status,
				this.width / 2, this.height / 2 + 90, 0xFFFFFF);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		return pwField.keyPressed(input)
				|| confirmField.keyPressed(input)
				|| super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		return pwField.charTyped(input)
				|| confirmField.charTyped(input)
				|| super.charTyped(input);
	}
}
