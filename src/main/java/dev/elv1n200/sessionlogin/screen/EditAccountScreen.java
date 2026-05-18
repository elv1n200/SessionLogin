package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
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

public class EditAccountScreen extends Screen {
	private TextFieldWidget nameField;
	private TextFieldWidget skinUrlField;
	private ButtonWidget nameButton;
	private ButtonWidget skinButton;
	private Text currentTitle;

	public EditAccountScreen() {
		super(Text.literal(""));
		this.currentTitle = surroundWithObfuscated(
				Text.literal("Edit Account").formatted(Formatting.AQUA), 5);
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		nameField = new TextFieldWidget(this.textRenderer,
				cx - 100, cy - 40, 200, 20, Text.literal("New Username"));
		nameField.setMaxLength(16);
		nameField.setFocused(true);
		this.addSelectableChild(nameField);

		skinUrlField = new TextFieldWidget(this.textRenderer,
				cx - 100, cy, 200, 20, Text.literal("Skin URL"));
		skinUrlField.setMaxLength(2048);
		this.addSelectableChild(skinUrlField);

		nameButton = ButtonWidget.builder(Text.literal("Change Name"), b -> {
			String newName = nameField.getText().trim();
			if (newName.isEmpty()) {
				currentTitle = surroundWithObfuscated(
						Text.literal("Please input a name")
								.formatted(Formatting.RED), 5);
				return;
			}
			if (!newName.matches("^[a-zA-Z0-9_]{3,16}$")) {
				currentTitle = surroundWithObfuscated(
						Text.literal("Invalid name").formatted(Formatting.RED), 7);
				return;
			}
			int code = ApiUtils.changeName(newName,
					SessionLogin.currentSession.getAccessToken());
			currentTitle = switch (code) {
				case 200 -> {
					SessionLogin.currentSession = SessionUtils.createSession(
							newName,
							SessionLogin.currentSession.getUuidOrNull(),
							SessionLogin.currentSession.getAccessToken());
					yield surroundWithObfuscated(Text.literal(
							"Successfully changed name").formatted(Formatting.GREEN), 4);
				}
				case 429 -> surroundWithObfuscated(Text.literal(
						"Too many requests").formatted(Formatting.RED), 5);
				case 400 -> surroundWithObfuscated(Text.literal(
						"Invalid name").formatted(Formatting.RED), 7);
				case 401 -> surroundWithObfuscated(Text.literal(
						"Invalid token").formatted(Formatting.RED), 7);
				case 403 -> surroundWithObfuscated(Text.literal(
						"Name unavailable or changed in the last 35 days")
						.formatted(Formatting.RED), 2);
				default -> surroundWithObfuscated(Text.literal(
						"Unknown error").formatted(Formatting.RED), 2);
			};
		}).dimensions(cx - 100, cy + 25, 97, 20).build();
		this.addDrawableChild(nameButton);

		skinButton = ButtonWidget.builder(Text.literal("Change Skin"), b -> {
			String skinUrl = skinUrlField.getText().trim();
			if (skinUrl.isEmpty()) {
				currentTitle = surroundWithObfuscated(
						Text.literal("Please input an URL")
								.formatted(Formatting.RED), 5);
				return;
			}
			int code = ApiUtils.changeSkin(skinUrl,
					SessionLogin.currentSession.getAccessToken());
			currentTitle = switch (code) {
				case 200 -> surroundWithObfuscated(Text.literal(
						"Successfully changed skin").formatted(Formatting.GREEN), 4);
				case 429 -> surroundWithObfuscated(Text.literal(
						"Too many requests").formatted(Formatting.RED), 5);
				case 401 -> surroundWithObfuscated(Text.literal(
						"Invalid token").formatted(Formatting.RED), 7);
				case -1 -> surroundWithObfuscated(Text.literal(
						"Unknown error").formatted(Formatting.RED), 7);
				default -> surroundWithObfuscated(Text.literal(
						"Invalid Skin").formatted(Formatting.RED), 7);
			};
		}).dimensions(cx + 3, cy + 25, 97, 20).build();
		this.addDrawableChild(skinButton);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
			assert this.client != null;
			this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
		}).dimensions(cx - 100, cy + 50, 200, 20).build());

		if (SessionUtils.isOriginalActive()) {
			nameButton.active = false;
			skinButton.active = false;
			currentTitle = surroundWithObfuscated(Text.literal(
					"Cannot modify original session")
					.formatted(Formatting.YELLOW), 4);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawTextWithShadow(this.textRenderer, Text.literal("Username:"),
				this.width / 2 - 100, this.height / 2 - 52, 0xA0A0A0);
		nameField.render(context, mouseX, mouseY, delta);
		context.drawTextWithShadow(this.textRenderer, Text.literal("Skin URL:"),
				this.width / 2 - 100, this.height / 2 - 10, 0xA0A0A0);
		skinUrlField.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.currentTitle,
				this.width / 2, this.height / 2 - 75, 0xFFFFFF);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		return nameField.keyPressed(input)
				|| skinUrlField.keyPressed(input)
				|| super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		return nameField.charTyped(input)
				|| skinUrlField.charTyped(input)
				|| super.charTyped(input);
	}
}
