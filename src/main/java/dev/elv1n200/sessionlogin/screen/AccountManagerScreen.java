package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import dev.elv1n200.sessionlogin.util.TokenUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

public class AccountManagerScreen extends Screen {

	private static final int PER_PAGE = 5;

	/** token -> valid? cached across screen opens to avoid API spam. */
	private static final Map<String, Boolean> VALIDITY =
			new ConcurrentHashMap<>();

	/** tokens whose validation thread is currently running. */
	private static final java.util.Set<String> CHECKING =
			ConcurrentHashMap.newKeySet();

	private enum Sort {RECENT, NAME, EXPIRY}

	private static Sort sort = Sort.RECENT;
	private static String query = "";

	private Text status = Text.empty();
	private TextFieldWidget searchField;
	private int page = 0;

	private int titleY;
	private int statusY;
	private int emptyY;

	public AccountManagerScreen() {
		super(Text.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;

		if (SessionLogin.accountStore.isLocked()) {
			int ly = (this.height - 45) / 2;
			this.titleY = ly - 30;
			this.statusY = -100;
			this.emptyY = -100;
			this.addDrawableChild(ButtonWidget.builder(
					Text.literal("Unlock Vault"), b -> {
						assert this.client != null;
						this.client.setScreen(new UnlockScreen());
					}).dimensions(cx - 100, ly, 200, 20).build());
			this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
				assert this.client != null;
				this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
			}).dimensions(cx - 100, ly + 25, 200, 20).build());
			return;
		}

		List<Account> all = filteredSorted();
		startValidation(all);

		int maxPage = Math.max(0, (all.size() - 1) / PER_PAGE);
		page = Math.min(page, maxPage);
		int start = page * PER_PAGE;
		int end = Math.min(all.size(), start + PER_PAGE);
		int visibleRows = Math.max(1, end - start);

		int contentH = 12 + 16 + 24 + visibleRows * 24 + 6 + 20 + 4 + 20 + 4 + 20;
		int top = Math.max(28, (this.height - contentH) / 2);

		this.titleY = top;
		this.statusY = top + 12;

		int searchY = top + 28;
		searchField = new TextFieldWidget(this.textRenderer,
				cx - 130, searchY + 1, 180, 18, Text.literal("Search"));
		searchField.setMaxLength(48);
		searchField.setText(query);
		searchField.setChangedListener(s -> {
			query = s;
			page = 0;
			this.clearAndInit();
		});
		this.addSelectableChild(searchField);

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Sort: " + sort.name()), b -> {
					sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
					this.clearAndInit();
				}).dimensions(cx + 55, searchY, 75, 20).build());

		int rowStart = top + 52;
		this.emptyY = rowStart + 6;

		for (int i = start; i < end; i++) {
			final Account acc = all.get(i);
			int rowY = rowStart + (i - start) * 24;

			this.addDrawableChild(ButtonWidget.builder(rowLabel(acc),
					b -> switchTo(acc)
			).dimensions(cx - 130, rowY, 190, 20).build());

			this.addDrawableChild(ButtonWidget.builder(Text.literal("E"),
					b -> {
						assert this.client != null;
						this.client.setScreen(new EditEntryScreen(acc));
					}).dimensions(cx + 62, rowY, 28, 20).build());

			this.addDrawableChild(ButtonWidget.builder(
					Text.literal("X").formatted(Formatting.RED),
					b -> {
						SessionLogin.accountStore.remove(acc);
						this.clearAndInit();
					}).dimensions(cx + 92, rowY, 38, 20).build());
		}

		int navY = rowStart + visibleRows * 24 + 6;

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
				Text.literal("Save Current"), b -> {
					try {
						var s = SessionLogin.currentSession;
						Account a = new Account(s.getUsername(), s.getUsername(),
								String.valueOf(s.getUuidOrNull()),
								s.getAccessToken());
						a.touch();
						SessionLogin.accountStore.add(a);
						status = ok("Saved " + s.getUsername());
						this.clearAndInit();
					} catch (Exception e) {
						status = bad("Could not save session");
					}
				}).dimensions(cx - 130, navY + 24, 85, 20).build());

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Bulk Import"), b -> {
					assert this.client != null;
					this.client.setScreen(new BulkImportScreen());
				}).dimensions(cx - 43, navY + 24, 85, 20).build());

		ButtonWidget restore = ButtonWidget.builder(
				Text.literal("Use Original"), b -> {
					SessionUtils.restoreSession();
					status = ok("Restored original session");
				}).dimensions(cx + 44, navY + 24, 86, 20).build();
		restore.active = !SessionUtils.isOriginalActive();
		this.addDrawableChild(restore);

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Edit Account"), b -> {
					assert this.client != null;
					this.client.setScreen(new EditAccountScreen());
				}).dimensions(cx - 130, navY + 48, 85, 20).build());

		String vaultLbl = SessionLogin.vault.isPasswordMode()
				? "Vault: PW" : "Vault: Local";
		this.addDrawableChild(ButtonWidget.builder(
				Text.literal(vaultLbl), b -> {
					assert this.client != null;
					this.client.setScreen(new SetPasswordScreen());
				}).dimensions(cx - 43, navY + 48, 85, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
			assert this.client != null;
			this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
		}).dimensions(cx + 44, navY + 48, 86, 20).build());
	}

	private List<Account> filteredSorted() {
		List<Account> list = new ArrayList<>();
		String q = query.toLowerCase(Locale.ROOT).trim();
		for (Account a : SessionLogin.accountStore.accounts()) {
			if (q.isEmpty()
					|| a.label().toLowerCase(Locale.ROOT).contains(q)
					|| a.username().toLowerCase(Locale.ROOT).contains(q)
					|| a.notes().toLowerCase(Locale.ROOT).contains(q)) {
				list.add(a);
			}
		}
		switch (sort) {
			case NAME -> list.sort(Comparator.comparing(
					a -> a.username().toLowerCase(Locale.ROOT)));
			case RECENT -> list.sort(Comparator.comparingLong(
					Account::lastUsed).reversed());
			case EXPIRY -> list.sort(Comparator.comparingLong(
					a -> TokenUtils.expiryEpochSeconds(a.token())));
		}
		return list;
	}

	private Text rowLabel(Account acc) {
		String exp = acc.hasToken() ? TokenUtils.expiryLabel(acc.token())
				: "locked";
		Boolean valid = acc.hasToken() ? VALIDITY.get(acc.token()) : null;
		String mark = valid == null ? "?" : (valid ? "OK" : "X");
		Formatting c = valid == null ? Formatting.GRAY
				: (valid ? Formatting.GREEN : Formatting.RED);
		return Text.literal(acc.label() + "  ")
				.append(Text.literal("[" + exp + "]")
						.formatted(TokenUtils.isExpired(acc.token())
								? Formatting.RED : Formatting.DARK_GRAY))
				.append(Text.literal(" " + mark).formatted(c));
	}

	private void startValidation(List<Account> list) {
		for (Account a : list) {
			if (!a.hasToken()) {
				continue;
			}
			String t = a.token();
			if (VALIDITY.containsKey(t) || !CHECKING.add(t)) {
				continue;
			}
			new Thread(() -> {
				try {
					VALIDITY.put(t, ApiUtils.tokenLooksValid(t));
				} finally {
					CHECKING.remove(t);
				}
			}, "AccValidate").start();
		}
	}

	private void switchTo(Account acc) {
		try {
			if (!acc.hasToken()) {
				status = bad("Vault locked");
				return;
			}
			SessionUtils.setSession(SessionUtils.createSession(
					acc.username(), acc.uuid(), acc.token()));
			acc.touch();
			SessionLogin.accountStore.save();
			if (TokenUtils.isExpired(acc.token())) {
				status = surroundWithObfuscated(Text.literal(
						"Switched (token expired!)")
						.formatted(Formatting.YELLOW), 3);
			} else {
				status = ok("Switched to " + acc.username());
			}
		} catch (Exception e) {
			status = bad("Failed to switch account");
		}
	}

	private static Text ok(String m) {
		return surroundWithObfuscated(
				Text.literal(m).formatted(Formatting.GREEN), 4);
	}

	private static Text bad(String m) {
		return surroundWithObfuscated(
				Text.literal(m).formatted(Formatting.RED), 4);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer,
				surroundWithObfuscated(Text.literal("Account Manager")
						.formatted(Formatting.AQUA), 4),
				this.width / 2, this.titleY, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, this.status,
				this.width / 2, this.statusY, 0xFFFFFF);
		if (!SessionLogin.accountStore.isLocked()) {
			searchField.render(context, mouseX, mouseY, delta);
			if (filteredSorted().isEmpty()) {
				context.drawCenteredTextWithShadow(this.textRenderer,
						Text.literal(query.isEmpty()
								? "No saved accounts yet" : "No matches")
								.formatted(Formatting.GRAY),
						this.width / 2, this.emptyY, 0xFFFFFF);
			}
		}
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		if (searchField != null && searchField.keyPressed(input)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		if (searchField != null && searchField.charTyped(input)) {
			return true;
		}
		return super.charTyped(input);
	}
}
