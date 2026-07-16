package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import dev.elv1n200.sessionlogin.util.TokenUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class LoginScreen extends Screen {

	private static final int KEY_ENTER = 257;
	private static final int KEY_KP_ENTER = 335;

	private EditBox sessionField;
	private Button loginButton;
	private Button restoreButton;
	private Button saveButton;

	private volatile Component status = Component.literal("Paste a session ID and press Login")
			.withStyle(ChatFormatting.GRAY);
	private volatile Component expiryText = Component.empty();
	private volatile boolean busy = false;

	private String[] lastInfo;
	private String lastToken;

	public LoginScreen() {
		super(Component.literal("Session Login"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		sessionField = new EditBox(this.font,
				cx - 100, cy, 200, 20, Component.literal("Session Input"));
		sessionField.setMaxLength(32767);
		this.addRenderableWidget(sessionField);
		this.setInitialFocus(sessionField);

		loginButton = Button.builder(Component.literal("Login"), b -> doLogin())
				.bounds(cx - 100, cy + 25, 64, 20).build();
		this.addRenderableWidget(loginButton);

		this.addRenderableWidget(Button.builder(Component.literal("Paste"), b -> {
			String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
			sessionField.setValue(TokenUtils.clean(clip));
			status = Component.literal("Pasted from clipboard").withStyle(ChatFormatting.GRAY);
		}).bounds(cx - 33, cy + 25, 64, 20).build());

		restoreButton = Button.builder(Component.literal("Restore"), b -> {
			SessionUtils.restoreSession();
			expiryText = Component.empty();
			lastInfo = null;
			lastToken = null;
			status = Component.literal("Restored your original session")
					.withStyle(ChatFormatting.GREEN);
			saveButton.active = false;
			restoreButton.active = false;
		}).bounds(cx + 34, cy + 25, 66, 20).build();
		this.addRenderableWidget(restoreButton);

		saveButton = Button.builder(Component.literal("Save to accounts"), b -> {
			if (lastInfo == null || lastToken == null) {
				status = Component.literal("Log in first").withStyle(ChatFormatting.RED);
				return;
			}
			Account acc = new Account(lastInfo[0], lastInfo[0],
					SessionUtils.dashUuid(lastInfo[1]), lastToken);
			acc.touch();
			SessionLogin.accountStore.add(acc);
			status = Component.literal("Saved " + lastInfo[0] + " to accounts")
					.withStyle(ChatFormatting.GREEN);
		}).bounds(cx - 100, cy + 50, 200, 20).build();
		this.addRenderableWidget(saveButton);

		this.addRenderableWidget(Button.builder(Component.literal("Accounts"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new AccountManagerScreen());
		}).bounds(cx - 100, cy + 75, 97, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
		}).bounds(cx + 3, cy + 75, 97, 20).build());

		restoreButton.active = !SessionUtils.isOriginalActive();
		saveButton.active = false;
	}

	private void doLogin() {
		if (busy) {
			return;
		}
		String input = TokenUtils.clean(sessionField.getValue());
		if (input.isEmpty()) {
			status = Component.literal("Session ID cannot be empty")
					.withStyle(ChatFormatting.RED);
			return;
		}

		busy = true;
		loginButton.active = false;
		status = Component.literal("Checking session ...").withStyle(ChatFormatting.YELLOW);
		expiryText = Component.empty();

		new Thread(() -> {
			try {
				String[] info = ApiUtils.getProfileInfo(input);
				Minecraft.getInstance().execute(() -> {
					lastInfo = info;
					lastToken = input;
					SessionUtils.setSession(SessionUtils.createSession(
							info[0], info[1], input));
					status = Component.literal("✔ Logged in as " + info[0])
							.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
					updateExpiry(input);
					restoreButton.active = true;
					saveButton.active = true;
					busy = false;
					loginButton.active = true;
					dev.elv1n200.sessionlogin.util.Notifier.loggedIn(info[0]);
				});
			} catch (Throwable t) {
				Minecraft.getInstance().execute(() -> {
					lastInfo = null;
					status = Component.literal("✘ " + reason(t))
							.withStyle(ChatFormatting.RED);
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
		ChatFormatting color = switch (label) {
			case "expired" -> ChatFormatting.RED;
			case "unknown" -> ChatFormatting.GRAY;
			default -> TokenUtils.expiryEpochSeconds(token)
					- System.currentTimeMillis() / 1000L < 3600
					? ChatFormatting.YELLOW : ChatFormatting.GREEN;
		};
		expiryText = Component.literal("Token expires: " + label).withStyle(color);
	}

	private Component activeLine() {
		String name = SessionUtils.getUsername();
		if (SessionUtils.isOriginalActive()) {
			return Component.literal("Active: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(name + " (original)")
							.withStyle(ChatFormatting.WHITE));
		}
		return Component.literal("Active: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(name).withStyle(ChatFormatting.GREEN))
				.append(Component.literal(" ✔").withStyle(ChatFormatting.GREEN));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int cy = this.height / 2;

		extractor.centeredText(this.font,
				Component.literal("Session Login").withStyle(ChatFormatting.GOLD),
				cx, cy - 60, 0xFFFFFF);
		extractor.centeredText(this.font, activeLine(), cx, cy - 46, 0xFFFFFF);
		extractor.centeredText(this.font, this.status, cx, cy - 28, 0xFFFFFF);
		extractor.centeredText(this.font, this.expiryText, cx, cy - 16, 0xFFFFFF);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		int key = input.key();
		if (key == KEY_ENTER || key == KEY_KP_ENTER) {
			doLogin();
			return true;
		}
		return super.keyPressed(input);
	}
}
