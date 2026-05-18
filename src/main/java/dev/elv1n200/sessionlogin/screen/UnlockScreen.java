package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

/** Prompts for the master password and unlocks the vault. */
public class UnlockScreen extends Screen {

	private TextFieldWidget pwField;
	private Text status = Text.literal("Vault is locked")
			.formatted(Formatting.YELLOW);

	public UnlockScreen() {
		super(Text.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		pwField = new TextFieldWidget(this.textRenderer,
				cx - 100, cy, 200, 20, Text.literal("Master password"));
		pwField.setMaxLength(128);
		pwField.addFormatter((s, i) ->
				Text.literal("*".repeat(s.length())).asOrderedText());
		pwField.setFocused(true);
		this.addSelectableChild(pwField);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Unlock"), b -> {
			char[] pw = pwField.getText().toCharArray();
			if (SessionLogin.vault.unlock(pw)) {
				SessionLogin.accountStore.decryptAll();
				assert this.client != null;
				this.client.setScreen(new AccountManagerScreen());
			} else {
				status = surroundWithObfuscated(Text.literal("Wrong password")
						.formatted(Formatting.RED), 4);
			}
		}).dimensions(cx - 100, cy + 25, 97, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
			assert this.client != null;
			this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
		}).dimensions(cx + 3, cy + 25, 97, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.status,
				this.width / 2, this.height / 2 - 30, 0xFFFFFF);
		pwField.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		return pwField.keyPressed(input) || pwField.isActive()
				|| super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		return pwField.charTyped(input) || super.charTyped(input);
	}
}
