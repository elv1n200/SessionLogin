package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

public class EditEntryScreen extends Screen {
	private final Account account;
	private EditBox labelField;
	private EditBox notesField;

	public EditEntryScreen(Account account) {
		super(Component.literal(""));
		this.account = account;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		labelField = new EditBox(this.font,
				cx - 100, cy - 30, 200, 20, Component.literal("Label"));
		labelField.setMaxLength(48);
		labelField.setValue(account.label());
		this.addRenderableWidget(labelField);
		this.setInitialFocus(labelField);

		notesField = new EditBox(this.font,
				cx - 100, cy + 10, 200, 20, Component.literal("Notes"));
		notesField.setMaxLength(256);
		notesField.setValue(account.notes());
		this.addRenderableWidget(notesField);

		this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
			String lbl = labelField.getValue().trim();
			account.setLabel(lbl.isEmpty() ? account.username() : lbl);
			account.setNotes(notesField.getValue().trim());
			SessionLogin.accountStore.save();
			assert this.minecraft != null;
			this.minecraft.setScreen(new AccountManagerScreen());
		}).bounds(cx - 100, cy + 40, 97, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new AccountManagerScreen());
		}).bounds(cx + 3, cy + 40, 97, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		extractor.centeredText(this.font,
				surroundWithObfuscated(Component.literal(
						"Edit: " + account.username()).withStyle(ChatFormatting.AQUA), 4),
				this.width / 2, this.height / 2 - 60, 0xFFFFFF);
		extractor.text(this.font, Component.literal("Label:"),
				this.width / 2 - 100, this.height / 2 - 42, 0xA0A0A0);
		extractor.text(this.font, Component.literal("Notes:"),
				this.width / 2 - 100, this.height / 2 - 2, 0xA0A0A0);
	}
}
