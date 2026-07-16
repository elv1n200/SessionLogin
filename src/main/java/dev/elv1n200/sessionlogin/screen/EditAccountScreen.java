package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

public class EditAccountScreen extends Screen {
	private EditBox nameField;
	private EditBox skinUrlField;
	private Button nameButton;
	private Button skinButton;
	private Component currentTitle;

	public EditAccountScreen() {
		super(Component.literal(""));
		this.currentTitle = surroundWithObfuscated(
				Component.literal("Edit Account").withStyle(ChatFormatting.AQUA), 5);
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		nameField = new EditBox(this.font,
				cx - 100, cy - 40, 200, 20, Component.literal("New Username"));
		nameField.setMaxLength(16);
		this.addRenderableWidget(nameField);
		this.setInitialFocus(nameField);

		skinUrlField = new EditBox(this.font,
				cx - 100, cy, 200, 20, Component.literal("Skin URL"));
		skinUrlField.setMaxLength(2048);
		this.addRenderableWidget(skinUrlField);

		nameButton = Button.builder(Component.literal("Change Name"), b -> {
			String newName = nameField.getValue().trim();
			if (newName.isEmpty()) {
				currentTitle = surroundWithObfuscated(
						Component.literal("Please input a name")
								.withStyle(ChatFormatting.RED), 5);
				return;
			}
			if (!newName.matches("^[a-zA-Z0-9_]{3,16}$")) {
				currentTitle = surroundWithObfuscated(
						Component.literal("Invalid name").withStyle(ChatFormatting.RED), 7);
				return;
			}
			int code = ApiUtils.changeName(newName,
					SessionLogin.currentSession.getAccessToken());
			currentTitle = switch (code) {
				case 200 -> {
					SessionLogin.currentSession = SessionUtils.createSession(
							newName,
							SessionLogin.currentSession.getProfileId(),
							SessionLogin.currentSession.getAccessToken());
					yield surroundWithObfuscated(Component.literal(
							"Successfully changed name").withStyle(ChatFormatting.GREEN), 4);
				}
				case 429 -> surroundWithObfuscated(Component.literal(
						"Too many requests").withStyle(ChatFormatting.RED), 5);
				case 400 -> surroundWithObfuscated(Component.literal(
						"Invalid name").withStyle(ChatFormatting.RED), 7);
				case 401 -> surroundWithObfuscated(Component.literal(
						"Invalid token").withStyle(ChatFormatting.RED), 7);
				case 403 -> surroundWithObfuscated(Component.literal(
						"Name unavailable or changed in the last 35 days")
						.withStyle(ChatFormatting.RED), 2);
				default -> surroundWithObfuscated(Component.literal(
						"Unknown error").withStyle(ChatFormatting.RED), 2);
			};
		}).bounds(cx - 100, cy + 25, 97, 20).build();
		this.addRenderableWidget(nameButton);

		skinButton = Button.builder(Component.literal("Change Skin"), b -> {
			String skinUrl = skinUrlField.getValue().trim();
			if (skinUrl.isEmpty()) {
				currentTitle = surroundWithObfuscated(
						Component.literal("Please input an URL")
								.withStyle(ChatFormatting.RED), 5);
				return;
			}
			int code = ApiUtils.changeSkin(skinUrl,
					SessionLogin.currentSession.getAccessToken());
			currentTitle = switch (code) {
				case 200 -> surroundWithObfuscated(Component.literal(
						"Successfully changed skin").withStyle(ChatFormatting.GREEN), 4);
				case 429 -> surroundWithObfuscated(Component.literal(
						"Too many requests").withStyle(ChatFormatting.RED), 5);
				case 401 -> surroundWithObfuscated(Component.literal(
						"Invalid token").withStyle(ChatFormatting.RED), 7);
				case -1 -> surroundWithObfuscated(Component.literal(
						"Unknown error").withStyle(ChatFormatting.RED), 7);
				default -> surroundWithObfuscated(Component.literal(
						"Invalid Skin").withStyle(ChatFormatting.RED), 7);
			};
		}).bounds(cx + 3, cy + 25, 97, 20).build();
		this.addRenderableWidget(skinButton);

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
		}).bounds(cx - 100, cy + 50, 200, 20).build());

		if (SessionUtils.isOriginalActive()) {
			nameButton.active = false;
			skinButton.active = false;
			currentTitle = surroundWithObfuscated(Component.literal(
					"Cannot modify original session")
					.withStyle(ChatFormatting.YELLOW), 4);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		extractor.text(this.font, Component.literal("Username:"),
				this.width / 2 - 100, this.height / 2 - 52, 0xA0A0A0);
		extractor.text(this.font, Component.literal("Skin URL:"),
				this.width / 2 - 100, this.height / 2 - 10, 0xA0A0A0);
		extractor.centeredText(this.font, this.currentTitle,
				this.width / 2, this.height / 2 - 75, 0xFFFFFF);
	}
}
