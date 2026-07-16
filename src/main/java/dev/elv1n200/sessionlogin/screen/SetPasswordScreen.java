package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

/**
 * Enable / change / remove the master password. Re-encrypts every saved
 * token with the new key. The vault must be unlocked first.
 */
public class SetPasswordScreen extends Screen {

	private EditBox pwField;
	private EditBox confirmField;
	private Component status = Component.empty();

	public SetPasswordScreen() {
		super(Component.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		pwField = passwordField(cx - 100, cy - 20, "New password");
		this.addRenderableWidget(pwField);
		this.setInitialFocus(pwField);
		confirmField = passwordField(cx - 100, cy + 10, "Confirm password");
		this.addRenderableWidget(confirmField);

		boolean unlocked = SessionLogin.vault.isUnlocked();
		boolean pwMode = SessionLogin.vault.isPasswordMode();

		Button setBtn = Button.builder(
				Component.literal(pwMode ? "Change" : "Enable"), b -> {
					String p = pwField.getValue();
					if (p.length() < 4) {
						status = err("Password too short (min 4)");
						return;
					}
					if (!p.equals(confirmField.getValue())) {
						status = err("Passwords do not match");
						return;
					}
					if (SessionLogin.vault.enablePassword(p.toCharArray())) {
						SessionLogin.accountStore.reencryptAll();
						status = surroundWithObfuscated(Component.literal(
								"Master password set")
								.withStyle(ChatFormatting.GREEN), 4);
					} else {
						status = err("Unlock the vault first");
					}
				}).bounds(cx - 100, cy + 40, 97, 20).build();
		setBtn.active = unlocked;
		this.addRenderableWidget(setBtn);

		Button removeBtn = Button.builder(
				Component.literal("Remove"), b -> {
					if (SessionLogin.vault.disablePassword()) {
						SessionLogin.accountStore.reencryptAll();
						status = surroundWithObfuscated(Component.literal(
								"Switched to local mode")
								.withStyle(ChatFormatting.GREEN), 4);
					} else {
						status = err("Unlock the vault first");
					}
				}).bounds(cx + 3, cy + 40, 97, 20).build();
		removeBtn.active = unlocked && pwMode;
		this.addRenderableWidget(removeBtn);

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new AccountManagerScreen());
		}).bounds(cx - 100, cy + 64, 200, 20).build());

		if (!unlocked) {
			status = err("Vault locked — unlock before changing password");
		}
	}

	private EditBox passwordField(int x, int y, String hint) {
		EditBox f = new EditBox(this.font, x, y, 200, 20, Component.literal(hint));
		f.setMaxLength(128);
		f.addFormatter((s, i) ->
				Component.literal("*".repeat(s.length())).getVisualOrderText());
		return f;
	}

	private static Component err(String m) {
		return surroundWithObfuscated(
				Component.literal(m).withStyle(ChatFormatting.RED), 4);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		extractor.centeredText(this.font,
				surroundWithObfuscated(Component.literal("Master Password")
						.withStyle(ChatFormatting.AQUA), 4),
				this.width / 2, this.height / 2 - 46, 0xFFFFFF);
		extractor.centeredText(this.font, this.status,
				this.width / 2, this.height / 2 + 90, 0xFFFFFF);
	}
}
