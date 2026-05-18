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

public class EditEntryScreen extends Screen {
	private final Account account;
	private TextFieldWidget labelField;
	private TextFieldWidget notesField;

	public EditEntryScreen(Account account) {
		super(Text.literal(""));
		this.account = account;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		labelField = new TextFieldWidget(this.textRenderer,
				cx - 100, cy - 30, 200, 20, Text.literal("Label"));
		labelField.setMaxLength(48);
		labelField.setText(account.label());
		this.addSelectableChild(labelField);

		notesField = new TextFieldWidget(this.textRenderer,
				cx - 100, cy + 10, 200, 20, Text.literal("Notes"));
		notesField.setMaxLength(256);
		notesField.setText(account.notes());
		this.addSelectableChild(notesField);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
			String lbl = labelField.getText().trim();
			account.setLabel(lbl.isEmpty() ? account.username() : lbl);
			account.setNotes(notesField.getText().trim());
			SessionLogin.accountStore.save();
			assert this.client != null;
			this.client.setScreen(new AccountManagerScreen());
		}).dimensions(cx - 100, cy + 40, 97, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> {
			assert this.client != null;
			this.client.setScreen(new AccountManagerScreen());
		}).dimensions(cx + 3, cy + 40, 97, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer,
				surroundWithObfuscated(Text.literal(
						"Edit: " + account.username()).formatted(Formatting.AQUA), 4),
				this.width / 2, this.height / 2 - 60, 0xFFFFFF);
		context.drawTextWithShadow(this.textRenderer, Text.literal("Label:"),
				this.width / 2 - 100, this.height / 2 - 42, 0xA0A0A0);
		labelField.render(context, mouseX, mouseY, delta);
		context.drawTextWithShadow(this.textRenderer, Text.literal("Notes:"),
				this.width / 2 - 100, this.height / 2 - 2, 0xA0A0A0);
		notesField.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		return labelField.keyPressed(input)
				|| notesField.keyPressed(input)
				|| super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		return labelField.charTyped(input)
				|| notesField.charTyped(input)
				|| super.charTyped(input);
	}
}
