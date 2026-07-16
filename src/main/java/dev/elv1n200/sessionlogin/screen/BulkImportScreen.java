package dev.elv1n200.sessionlogin.screen;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import dev.elv1n200.sessionlogin.util.TokenUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicInteger;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

/**
 * Imports many accounts at once from the clipboard — one token (or
 * token:uuid) per line. Each token is resolved against the Mojang API
 * on a background thread.
 */
public class BulkImportScreen extends Screen {

	private volatile String status = "Copy your token list to the clipboard, "
			+ "then click Import.";
	private volatile boolean running = false;

	public BulkImportScreen() {
		super(Component.literal(""));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		this.addRenderableWidget(Button.builder(
				Component.literal("Import from clipboard"), b -> startImport()
		).bounds(cx - 100, cy, 200, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Import from MC Launcher"), b -> startLauncherImport()
		).bounds(cx - 100, cy + 25, 200, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new AccountManagerScreen());
		}).bounds(cx - 100, cy + 50, 200, 20).build());
	}

	private void startLauncherImport() {
		if (running) {
			return;
		}
		java.util.List<dev.elv1n200.sessionlogin.account.Account> all =
				new java.util.ArrayList<>();
		java.util.List<String> sources = new java.util.ArrayList<>();

		java.nio.file.Path mojang =
				dev.elv1n200.sessionlogin.util.LauncherImport.findFile();
		if (mojang != null) {
			java.util.List<dev.elv1n200.sessionlogin.account.Account> a =
					dev.elv1n200.sessionlogin.util.LauncherImport.read(mojang);
			if (!a.isEmpty()) {
				all.addAll(a);
				sources.add("Mojang launcher (" + a.size() + ")");
			}
		}
		java.nio.file.Path modrinth =
				dev.elv1n200.sessionlogin.util.ModrinthImport.findFile();
		if (modrinth != null) {
			java.util.List<dev.elv1n200.sessionlogin.account.Account> a =
					dev.elv1n200.sessionlogin.util.ModrinthImport.read(modrinth);
			if (!a.isEmpty()) {
				all.addAll(a);
				sources.add("Modrinth App (" + a.size() + ")");
			}
		}

		if (all.isEmpty()) {
			status = "No accounts found in the Mojang launcher or Modrinth App.";
			return;
		}
		for (dev.elv1n200.sessionlogin.account.Account a : all) {
			SessionLogin.accountStore.add(a);
		}
		status = "Imported " + all.size() + " account(s) from "
				+ String.join(", ", sources) + ".";
	}

	private void startImport() {
		if (running) {
			return;
		}
		assert this.minecraft != null;
		String clip = this.minecraft.keyboardHandler.getClipboard();
		if (clip == null || clip.isBlank()) {
			status = "Clipboard is empty.";
			return;
		}
		String[] lines = clip.split("\\r?\\n");
		running = true;
		status = "Importing 0/" + lines.length + " ...";

		new Thread(() -> {
			AtomicInteger ok = new AtomicInteger();
			AtomicInteger fail = new AtomicInteger();
			int total = lines.length;
			for (int i = 0; i < total; i++) {
				String token = TokenUtils.clean(lines[i]);
				if (!token.isEmpty()) {
					try {
						String[] info = ApiUtils.getProfileInfo(token);
						Account acc = new Account(info[0], info[0],
								SessionUtils.dashUuid(info[1]), token);
						SessionLogin.accountStore.add(acc);
						ok.incrementAndGet();
					} catch (Exception e) {
						fail.incrementAndGet();
					}
				} else {
					fail.incrementAndGet();
				}
				status = "Importing " + (i + 1) + "/" + total
						+ " (ok " + ok.get() + ", failed " + fail.get() + ")";
			}
			status = "Done: " + ok.get() + " imported, " + fail.get()
					+ " failed/skipped.";
			running = false;
		}, "BulkImportThread").start();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		extractor.centeredText(this.font,
				surroundWithObfuscated(Component.literal("Bulk Import")
						.withStyle(ChatFormatting.AQUA), 4),
				this.width / 2, this.height / 2 - 40, 0xFFFFFF);
		extractor.centeredText(this.font,
				Component.literal(status).withStyle(ChatFormatting.GRAY),
				this.width / 2, this.height / 2 - 22, 0xFFFFFF);
	}
}
