package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import dev.elv1n200.sessionlogin.util.TokenUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

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

	private Component status = Component.empty();
	private EditBox searchField;
	private int page = 0;

	private int titleY;
	private int statusY;
	private int emptyY;
	private int rowStart;
	private List<Account> visibleAccounts = java.util.Collections.emptyList();

	public AccountManagerScreen() {
		super(Component.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;

		if (SessionLogin.accountStore.isLocked()) {
			int ly = (this.height - 45) / 2;
			this.titleY = ly - 30;
			this.statusY = -100;
			this.emptyY = -100;
			this.addRenderableWidget(Button.builder(
					Component.literal("Unlock Vault"), b -> {
						assert this.minecraft != null;
						this.minecraft.setScreen(new UnlockScreen());
					}).bounds(cx - 100, ly, 200, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
				assert this.minecraft != null;
				this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
			}).bounds(cx - 100, ly + 25, 200, 20).build());
			return;
		}

		List<Account> all = filteredSorted();
		startValidation(all);

		int maxPage = Math.max(0, (all.size() - 1) / PER_PAGE);
		page = Math.min(page, maxPage);
		int start = page * PER_PAGE;
		int end = Math.min(all.size(), start + PER_PAGE);
		int visibleRows = Math.max(1, end - start);

		int contentH = 12 + 16 + 24 + visibleRows * 24 + 6 + 20 + (4 + 20) * 4;
		int top = Math.max(28, (this.height - contentH) / 2);

		this.titleY = top;
		this.statusY = top + 12;

		int searchY = top + 28;
		searchField = new EditBox(this.font,
				cx - 130, searchY + 1, 180, 18, Component.literal("Search"));
		searchField.setMaxLength(48);
		searchField.setValue(query);
		searchField.setResponder(s -> {
			query = s;
			page = 0;
			this.rebuildWidgets();
		});
		this.addRenderableWidget(searchField);

		this.addRenderableWidget(Button.builder(
				Component.literal("Sort: " + sort.name()), b -> {
					sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
					this.rebuildWidgets();
				}).bounds(cx + 55, searchY, 75, 20).build());

		this.rowStart = top + 52;
		this.emptyY = this.rowStart + 6;
		this.visibleAccounts = all.subList(start, end);

		for (int i = start; i < end; i++) {
			final Account acc = all.get(i);
			int rowY = this.rowStart + (i - start) * 24;

			this.addRenderableWidget(Button.builder(rowLabel(acc),
					b -> switchTo(acc)
			).bounds(cx - 130, rowY, 190, 20).build());

			this.addRenderableWidget(Button.builder(Component.literal("E"),
					b -> {
						assert this.minecraft != null;
						this.minecraft.setScreen(new EditEntryScreen(acc));
					}).bounds(cx + 62, rowY, 28, 20).build());

			this.addRenderableWidget(Button.builder(
					Component.literal("X").withStyle(ChatFormatting.RED),
					b -> {
						SessionLogin.accountStore.remove(acc);
						this.rebuildWidgets();
					}).bounds(cx + 92, rowY, 38, 20).build());
		}

		int navY = rowStart + visibleRows * 24 + 6;

		Button prev = Button.builder(Component.literal("< Prev"), b -> {
			page--;
			this.rebuildWidgets();
		}).bounds(cx - 130, navY, 60, 20).build();
		prev.active = page > 0;
		this.addRenderableWidget(prev);

		Button next = Button.builder(Component.literal("Next >"), b -> {
			page++;
			this.rebuildWidgets();
		}).bounds(cx - 65, navY, 60, 20).build();
		next.active = page < maxPage;
		this.addRenderableWidget(next);

		this.addRenderableWidget(Button.builder(
				Component.literal("Add by Token"), b -> {
					assert this.minecraft != null;
					this.minecraft.setScreen(new LoginScreen());
				}).bounds(cx + 5, navY, 125, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Save Current"), b -> {
					try {
						var s = SessionLogin.currentSession;
						Account a = new Account(s.getName(), s.getName(),
								String.valueOf(s.getProfileId()),
								s.getAccessToken());
						a.touch();
						SessionLogin.accountStore.add(a);
						status = ok("Saved " + s.getName());
						this.rebuildWidgets();
					} catch (Exception e) {
						status = bad("Could not save session");
					}
				}).bounds(cx - 130, navY + 24, 85, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Bulk Import"), b -> {
					assert this.minecraft != null;
					this.minecraft.setScreen(new BulkImportScreen());
				}).bounds(cx - 43, navY + 24, 85, 20).build());

		Button restore = Button.builder(
				Component.literal("Use Original"), b -> {
					SessionUtils.restoreSession();
					status = ok("Restored original session");
				}).bounds(cx + 44, navY + 24, 86, 20).build();
		restore.active = !SessionUtils.isOriginalActive();
		this.addRenderableWidget(restore);

		this.addRenderableWidget(Button.builder(
				Component.literal("Edit Account"), b -> {
					assert this.minecraft != null;
					this.minecraft.setScreen(new EditAccountScreen());
				}).bounds(cx - 130, navY + 48, 85, 20).build());

		String vaultLbl = SessionLogin.vault.isPasswordMode()
				? "Vault: PW" : "Vault: Local";
		this.addRenderableWidget(Button.builder(
				Component.literal(vaultLbl), b -> {
					assert this.minecraft != null;
					this.minecraft.setScreen(new SetPasswordScreen());
				}).bounds(cx - 43, navY + 48, 85, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Settings"), b -> {
					assert this.minecraft != null;
					this.minecraft.setScreen(new SettingsScreen(this));
				}).bounds(cx + 44, navY + 48, 86, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Add Offline"), b -> {
					assert this.minecraft != null;
					this.minecraft.setScreen(new AddOfflineAccountScreen());
				}).bounds(cx - 130, navY + 72, 85, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Import"), b -> {
					assert this.minecraft != null;
					this.minecraft.setScreen(new ImportExportScreen(true));
				}).bounds(cx - 43, navY + 72, 85, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Export"), b -> {
					assert this.minecraft != null;
					this.minecraft.setScreen(new ImportExportScreen(false));
				}).bounds(cx + 44, navY + 72, 86, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
		}).bounds(cx - 100, navY + 96, 200, 20).build());
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

	private Component rowLabel(Account acc) {
		// Leading spaces reserve room for the 16x16 avatar drawn in the extract pass.
		String pad = "    ";
		if (acc.isOffline()) {
			return Component.literal(pad + acc.label() + "  ")
					.append(Component.literal("[offline]").withStyle(ChatFormatting.DARK_GRAY));
		}
		String exp = acc.hasToken() ? TokenUtils.expiryLabel(acc.token())
				: "locked";
		Boolean valid = acc.hasToken() ? VALIDITY.get(acc.token()) : null;
		String mark = valid == null ? "?" : (valid ? "OK" : "X");
		ChatFormatting c = valid == null ? ChatFormatting.GRAY
				: (valid ? ChatFormatting.GREEN : ChatFormatting.RED);
		return Component.literal(pad + acc.label() + "  ")
				.append(Component.literal("[" + exp + "]")
						.withStyle(TokenUtils.isExpired(acc.token())
								? ChatFormatting.RED : ChatFormatting.DARK_GRAY))
				.append(Component.literal(" " + mark).withStyle(c));
	}

	private void startValidation(List<Account> list) {
		for (Account a : list) {
			if (!a.hasToken() || a.isOffline()) {
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
			if (acc.isOffline()) {
				SessionUtils.setSession(SessionUtils.createSession(
						acc.username(), acc.uuid(), ""));
				acc.touch();
				SessionLogin.accountStore.save();
				status = ok("Switched to " + acc.username() + " (offline)");
				dev.elv1n200.sessionlogin.util.Notifier.loggedIn(acc.username());
				return;
			}
			if (!acc.hasToken()) {
				status = bad("Vault locked");
				return;
			}
			SessionUtils.setSession(SessionUtils.createSession(
					acc.username(), acc.uuid(), acc.token()));
			acc.touch();
			SessionLogin.accountStore.save();
			if (TokenUtils.isExpired(acc.token())) {
				status = surroundWithObfuscated(Component.literal(
						"Switched (token expired!)")
						.withStyle(ChatFormatting.YELLOW), 3);
			} else {
				status = ok("Switched to " + acc.username());
			}
			dev.elv1n200.sessionlogin.util.Notifier.loggedIn(acc.username());
		} catch (Exception e) {
			status = bad("Failed to switch account");
		}
	}

	/**
	 * Row avatars. On 26.x {@link dev.elv1n200.sessionlogin.util.SkinCache}
	 * doesn't upload head textures (the texture pipeline changed), so every row
	 * gets the neutral placeholder square that keeps the label alignment.
	 */
	private void drawHeads(GuiGraphicsExtractor extractor) {
		if (visibleAccounts.isEmpty()) {
			return;
		}
		int cx = this.width / 2;
		int x = cx - 128;
		for (int i = 0; i < visibleAccounts.size(); i++) {
			int y = rowStart + i * 24 + 2;
			extractor.fill(x, y, x + 16, y + 16, 0x66202020);
		}
	}

	private static Component ok(String m) {
		return surroundWithObfuscated(
				Component.literal(m).withStyle(ChatFormatting.GREEN), 4);
	}

	private static Component bad(String m) {
		return surroundWithObfuscated(
				Component.literal(m).withStyle(ChatFormatting.RED), 4);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		drawHeads(extractor);
		extractor.centeredText(this.font,
				surroundWithObfuscated(Component.literal("Account Manager")
						.withStyle(ChatFormatting.AQUA), 4),
				this.width / 2, this.titleY, 0xFFFFFF);
		extractor.centeredText(this.font, this.status,
				this.width / 2, this.statusY, 0xFFFFFF);
		if (!SessionLogin.accountStore.isLocked() && filteredSorted().isEmpty()) {
			extractor.centeredText(this.font,
					Component.literal(query.isEmpty()
							? "No saved accounts yet" : "No matches")
							.withStyle(ChatFormatting.GRAY),
					this.width / 2, this.emptyY, 0xFFFFFF);
		}
	}
}
