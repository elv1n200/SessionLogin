package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import dev.elv1n200.sessionlogin.util.TokenUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LoginScreen extends Screen {

	private static final int KEY_ENTER = 257;
	private static final int KEY_KP_ENTER = 335;

	private TextFieldWidget sessionField;
	private ButtonWidget loginButton;
	private ButtonWidget restoreButton;
	private ButtonWidget saveButton;

	private volatile Text status = Text.literal("Paste a session ID and press Login")
			.formatted(Formatting.GRAY);
	private volatile Text expiryText = Text.empty();
	private volatile boolean busy = false;

	private String[] lastInfo;
	private String lastToken;

	public LoginScreen() {
		super(Text.literal("Session Login"));
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

		loginButton = ButtonWidget.builder(Text.literal("Login"), b -> doLogin())
				.dimensions(cx - 100, cy + 25, 64, 20).build();
		this.addDrawableChild(loginButton);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Paste"), b -> {
			String clip = MinecraftClient.getInstance().keyboard.getClipboard();
			sessionField.setText(TokenUtils.clean(clip));
			status = Text.literal("Pasted from clipboard").formatted(Formatting.GRAY);
		}).dimensions(cx - 33, cy + 25, 64, 20).build());

		restoreButton = ButtonWidget.builder(Text.literal("Restore"), b -> {
			SessionUtils.restoreSession();
			expiryText = Text.empty();
			lastInfo = null;
			lastToken = null;
			status = Text.literal("Restored your original session")
					.formatted(Formatting.GREEN);
			saveButton.active = false;
			restoreButton.active = false;
		}).dimensions(cx + 34, cy + 25, 66, 20).build();
		this.addDrawableChild(restoreButton);

		saveButton = ButtonWidget.builder(Text.literal("Save to accounts"), b -> {
			if (lastInfo == null || lastToken == null) {
				status = Text.literal("Log in first").formatted(Formatting.RED);
				return;
			}
			Account acc = new Account(lastInfo[0], lastInfo[0],
					SessionUtils.dashUuid(lastInfo[1]), lastToken);
			acc.touch();
			SessionLogin.accountStore.add(acc);
			status = Text.literal("Saved " + lastInfo[0] + " to accounts")
					.formatted(Formatting.GREEN);
		}).dimensions(cx - 100, cy + 50, 200, 20).build();
		this.addDrawableChild(saveButton);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Accounts"), b -> {
			assert this.client != null;
			this.client.setScreen(new AccountManagerScreen());
		}).dimensions(cx - 100, cy + 75, 97, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
			assert this.client != null;
			this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
		}).dimensions(cx + 3, cy + 75, 97, 20).build());

		restoreButton.active = !SessionUtils.isOriginalActive();
		saveButton.active = false;
	}

	private void doLogin() {
		if (busy) {
			return;
		}
		String input = TokenUtils.clean(sessionField.getText());
		if (input.isEmpty()) {
			status = Text.literal("Session ID cannot be empty")
					.formatted(Formatting.RED);
			return;
		}

		busy = true;
		loginButton.active = false;
		status = Text.literal("Checking session ...").formatted(Formatting.YELLOW);
		expiryText = Text.empty();

		new Thread(() -> {
			try {
				String[] info = ApiUtils.getProfileInfo(input);
				MinecraftClient.getInstance().execute(() -> {
					lastInfo = info;
					lastToken = input;
					SessionUtils.setSession(SessionUtils.createSession(
							info[0], info[1], input));
					status = Text.literal("✔ Logged in as " + info[0])
							.formatted(Formatting.GREEN, Formatting.BOLD);
					updateExpiry(input);
					restoreButton.active = true;
					saveButton.active = true;
					busy = false;
					loginButton.active = true;
					dev.elv1n200.sessionlogin.util.Notifier.loggedIn(info[0]);
				});
			} catch (Throwable t) {
				MinecraftClient.getInstance().execute(() -> {
					lastInfo = null;
					status = Text.literal("✘ " + reason(t))
							.formatted(Formatting.RED);
					busy = false;
					loginButton.active = true;
				});
			}
		}, "SessionLoginThread").start();
	}

	private static String reason(Throwable e) {
		String m = e.getMessage();
		if (m != null && m.toLowerCase().contains("401")) {
			return "Token invalid or expired";
		}
		if (m != null && m.toLowerCase().contains("rejected")) {
			return "Token invalid or expired";
		}
		return "Login failed (network or invalid token)";
	}

	private void updateExpiry(String token) {
		String label = TokenUtils.expiryLabel(token);
		Formatting color = switch (label) {
			case "expired" -> Formatting.RED;
			case "unknown" -> Formatting.GRAY;
			default -> TokenUtils.expiryEpochSeconds(token)
					- System.currentTimeMillis() / 1000L < 3600
					? Formatting.YELLOW : Formatting.GREEN;
		};
		expiryText = Text.literal("Token expires: " + label).formatted(color);
	}

	private Text activeLine() {
		String name = SessionUtils.getUsername();
		if (SessionUtils.isOriginalActive()) {
			return Text.literal("Active: ").formatted(Formatting.GRAY)
					.append(Text.literal(name + " (original)")
							.formatted(Formatting.WHITE));
		}
		return Text.literal("Active: ").formatted(Formatting.GRAY)
				.append(Text.literal(name).formatted(Formatting.GREEN))
				.append(Text.literal(" ✔").formatted(Formatting.GREEN));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int cy = this.height / 2;

		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Session Login").formatted(Formatting.GOLD),
				cx, cy - 60, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, activeLine(),
				cx, cy - 46, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, this.status,
				cx, cy - 28, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, this.expiryText,
				cx, cy - 16, 0xFFFFFF);

		sessionField.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		int key = input.getKeycode();
		if (key == KEY_ENTER || key == KEY_KP_ENTER) {
			doLogin();
			return true;
		}
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
