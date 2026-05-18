package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

public class LoginScreen extends Screen {
	private TextFieldWidget sessionField;
	private ButtonWidget restoreButton;
	private ButtonWidget saveButton;
	private Text currentTitle;

	private String[] lastInfo;
	private String lastToken;

	public LoginScreen() {
		super(Text.literal(""));
		this.currentTitle = surroundWithObfuscated(
				Text.literal("Input Session ID").formatted(Formatting.GOLD), 5);
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		sessionField = new TextFieldWidget(this.textRenderer,
				cx - 100, cy, 200, 20, Text.literal("Session Input"));
		sessionField.setMaxLength(32767);
		sessionField.setFocused(true);
		this.addSelectableChild(sessionField);

		ButtonWidget loginButton = ButtonWidget.builder(Text.literal("Login"), b -> {
			String input = sessionField.getText().trim();
			if (input.isEmpty()) {
				currentTitle = surroundWithObfuscated(
						Text.literal("Session ID cannot be empty")
								.formatted(Formatting.RED), 5);
				return;
			}
			try {
				lastInfo = ApiUtils.getProfileInfo(input);
				lastToken = input;
				SessionUtils.setSession(SessionUtils.createSession(
						lastInfo[0], lastInfo[1], input));
				currentTitle = surroundWithObfuscated(
						Text.literal("Logged in as: " + lastInfo[0])
								.formatted(Formatting.GREEN), 5);
				restoreButton.active = true;
				saveButton.active = true;
			} catch (Exception e) {
				lastInfo = null;
				currentTitle = surroundWithObfuscated(
						Text.literal("Invalid Session ID")
								.formatted(Formatting.RED), 7);
			}
		}).dimensions(cx - 100, cy + 25, 97, 20).build();
		this.addDrawableChild(loginButton);

		restoreButton = ButtonWidget.builder(Text.literal("Restore"), b -> {
			SessionUtils.restoreSession();
			currentTitle = surroundWithObfuscated(
					Text.literal("Restored original session")
							.formatted(Formatting.GREEN), 7);
			restoreButton.active = false;
		}).dimensions(cx + 3, cy + 25, 97, 20).build();
		this.addDrawableChild(restoreButton);

		saveButton = ButtonWidget.builder(Text.literal("Save to accounts"), b -> {
			if (lastInfo == null || lastToken == null) {
				currentTitle = surroundWithObfuscated(
						Text.literal("Log in first").formatted(Formatting.RED), 5);
				return;
			}
			SessionLogin.accountStore.add(new Account(
					lastInfo[0], lastInfo[0],
					SessionUtils.dashUuid(lastInfo[1]), lastToken));
			currentTitle = surroundWithObfuscated(
					Text.literal("Saved " + lastInfo[0])
							.formatted(Formatting.GREEN), 4);
		}).dimensions(cx - 100, cy + 50, 200, 20).build();
		this.addDrawableChild(saveButton);

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Accounts"), b -> {
					assert this.client != null;
					this.client.setScreen(new AccountManagerScreen());
				}).dimensions(cx - 100, cy + 75, 97, 20).build());

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Back"), b -> {
					assert this.client != null;
					this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
				}).dimensions(cx + 3, cy + 75, 97, 20).build());

		restoreButton.active = !SessionUtils.isOriginalActive();
		saveButton.active = false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		sessionField.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.currentTitle,
				this.width / 2, this.height / 2 - 30, 0xFFFFFF);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		if (sessionField.keyPressed(input) || sessionField.isActive()) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		if (sessionField.charTyped(input)) {
			return true;
		}
		return super.charTyped(input);
	}
}
