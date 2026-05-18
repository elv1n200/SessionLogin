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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

public class AccountManagerScreen extends Screen {

	private static final int PER_PAGE = 6;

	private Text currentTitle;
	private int page = 0;

	public AccountManagerScreen() {
		super(Text.literal(""));
		this.currentTitle = surroundWithObfuscated(
				Text.literal("Account Manager").formatted(Formatting.AQUA), 5);
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int top = 50;

		List<Account> all = SessionLogin.accountStore.accounts();
		int maxPage = Math.max(0, (all.size() - 1) / PER_PAGE);
		page = Math.min(page, maxPage);

		int start = page * PER_PAGE;
		int end = Math.min(all.size(), start + PER_PAGE);

		for (int i = start; i < end; i++) {
			final Account acc = all.get(i);
			int rowY = top + (i - start) * 24;

			this.addDrawableChild(ButtonWidget.builder(
					Text.literal(acc.label() + " (" + acc.username() + ")"),
					b -> switchTo(acc)
			).dimensions(cx - 130, rowY, 220, 20).build());

			this.addDrawableChild(ButtonWidget.builder(
					Text.literal("X").formatted(Formatting.RED),
					b -> {
						SessionLogin.accountStore.remove(acc);
						this.clearAndInit();
					}
			).dimensions(cx + 95, rowY, 35, 20).build());
		}

		int navY = top + PER_PAGE * 24 + 8;

		ButtonWidget prev = ButtonWidget.builder(Text.literal("< Prev"), b -> {
			page--;
			this.clearAndInit();
		}).dimensions(cx - 130, navY, 60, 20).build();
		prev.active = page > 0;
		this.addDrawableChild(prev);

		ButtonWidget next = ButtonWidget.builder(Text.literal("Next >"), b -> {
			page++;
			this.clearAndInit();
		}).dimensions(cx - 65, navY, 60, 20).build();
		next.active = page < maxPage;
		this.addDrawableChild(next);

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Add by Token"), b -> {
					assert this.client != null;
					this.client.setScreen(new LoginScreen());
				}).dimensions(cx + 5, navY, 125, 20).build());

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Save Current Session"), b -> {
					try {
						var s = SessionLogin.currentSession;
						SessionLogin.accountStore.add(new Account(
								s.getUsername(), s.getUsername(),
								String.valueOf(s.getUuidOrNull()),
								s.getAccessToken()));
						currentTitle = surroundWithObfuscated(Text.literal(
								"Saved " + s.getUsername())
								.formatted(Formatting.GREEN), 4);
						this.clearAndInit();
					} catch (Exception e) {
						currentTitle = surroundWithObfuscated(Text.literal(
								"Could not save session")
								.formatted(Formatting.RED), 5);
					}
				}).dimensions(cx - 130, navY + 24, 130, 20).build());

		ButtonWidget restore = ButtonWidget.builder(
				Text.literal("Use Original"), b -> {
					SessionUtils.restoreSession();
					currentTitle = surroundWithObfuscated(Text.literal(
							"Restored original session")
							.formatted(Formatting.GREEN), 5);
				}).dimensions(cx, navY + 24, 130, 20).build();
		restore.active = !SessionUtils.isOriginalActive();
		this.addDrawableChild(restore);

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Edit Account"), b -> {
					assert this.client != null;
					this.client.setScreen(new EditAccountScreen());
				}).dimensions(cx - 130, navY + 48, 130, 20).build());

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Back"), b -> {
					assert this.client != null;
					this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
				}).dimensions(cx, navY + 48, 130, 20).build());
	}

	private void switchTo(Account acc) {
		try {
			SessionUtils.setSession(SessionUtils.createSession(
					acc.username(), acc.uuid(), acc.token()));
			currentTitle = surroundWithObfuscated(Text.literal(
					"Switched to " + acc.username())
					.formatted(Formatting.GREEN), 4);
			if (!ApiUtils.validateSession(acc.token())) {
				currentTitle = surroundWithObfuscated(Text.literal(
						"Switched (token may be expired)")
						.formatted(Formatting.YELLOW), 3);
			}
		} catch (Exception e) {
			currentTitle = surroundWithObfuscated(Text.literal(
					"Failed to switch account")
					.formatted(Formatting.RED), 5);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.currentTitle,
				this.width / 2, 24, 0xFFFFFF);
		if (SessionLogin.accountStore.accounts().isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("No saved accounts yet")
							.formatted(Formatting.GRAY),
					this.width / 2, 60, 0xFFFFFF);
		}
	}
}
