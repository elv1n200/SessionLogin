package dev.elv1n200.sessionlogin.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.elv1n200.sessionlogin.SessionLogin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;

import static dev.elv1n200.sessionlogin.util.FormattingUtils.surroundWithObfuscated;

/**
 * Export the entire vault (encrypted accounts + vault meta) to a portable JSON
 * file, or import one back. With master-password mode this is genuinely
 * portable — the recipient needs the password to unlock.
 */
public class ImportExportScreen extends Screen {

	private static final String FILE_NAME = "sessionlogin-export.json";

	private final boolean importMode;
	private Component status;

	public ImportExportScreen(boolean importMode) {
		super(Component.literal(""));
		this.importMode = importMode;
		this.status = Component.literal(importMode
				? "Place export file as <gameDir>/" + FILE_NAME
				: "Will write to <gameDir>/" + FILE_NAME)
				.withStyle(ChatFormatting.GRAY);
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		String action = importMode ? "Import" : "Export";
		this.addRenderableWidget(Button.builder(Component.literal(action), b -> {
			if (importMode) {
				doImport();
			} else {
				doExport();
			}
		}).bounds(cx - 100, cy, 200, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
			assert this.minecraft != null;
			this.minecraft.setScreen(new AccountManagerScreen());
		}).bounds(cx - 100, cy + 25, 200, 20).build());
	}

	private Path file() {
		return FabricLoader.getInstance().getGameDir().resolve(FILE_NAME);
	}

	private Path metaFile() {
		return FabricLoader.getInstance().getConfigDir()
				.resolve(SessionLogin.MOD_ID).resolve("meta.json");
	}

	private Path accountsFile() {
		return FabricLoader.getInstance().getConfigDir()
				.resolve(SessionLogin.MOD_ID).resolve("accounts.json");
	}

	private void doExport() {
		try {
			JsonObject out = new JsonObject();
			out.addProperty("version", 1);
			out.add("meta", Files.exists(metaFile())
					? JsonParser.parseString(Files.readString(metaFile()))
					: new JsonObject());
			out.add("accounts", Files.exists(accountsFile())
					? JsonParser.parseString(Files.readString(accountsFile()))
					: new JsonObject());
			Files.writeString(file(), out.toString());
			status = Component.literal("Exported to " + file())
					.withStyle(ChatFormatting.GREEN);
		} catch (Exception e) {
			status = Component.literal("Export failed: " + e.getMessage())
					.withStyle(ChatFormatting.RED);
		}
	}

	private void doImport() {
		try {
			if (!Files.exists(file())) {
				status = Component.literal("No file at " + file())
						.withStyle(ChatFormatting.RED);
				return;
			}
			JsonObject in = JsonParser.parseString(Files.readString(file()))
					.getAsJsonObject();
			JsonObject meta = in.has("meta")
					? in.getAsJsonObject("meta") : new JsonObject();
			JsonObject accs = in.has("accounts")
					? in.getAsJsonObject("accounts") : new JsonObject();
			Files.createDirectories(metaFile().getParent());
			Files.writeString(metaFile(), meta.toString());
			Files.writeString(accountsFile(), accs.toString());
			// Reinitialise vault + store from the new files
			SessionLogin.vault = new dev.elv1n200.sessionlogin.vault.VaultManager(
					metaFile().getParent());
			SessionLogin.vault.init();
			SessionLogin.accountStore = new dev.elv1n200.sessionlogin.account.AccountStore(
					metaFile().getParent(), SessionLogin.vault);
			SessionLogin.accountStore.load();
			status = Component.literal("Imported. " + (SessionLogin.vault.isPasswordMode()
					? "Vault locked — unlock to use." : "Done."))
					.withStyle(ChatFormatting.GREEN);
		} catch (Exception e) {
			status = Component.literal("Import failed: " + e.getMessage())
					.withStyle(ChatFormatting.RED);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int cy = this.height / 2;
		extractor.centeredText(this.font,
				surroundWithObfuscated(Component.literal(
						importMode ? "Import vault" : "Export vault")
						.withStyle(ChatFormatting.AQUA), 4),
				cx, cy - 40, 0xFFFFFF);
		extractor.centeredText(this.font, status, cx, cy - 22, 0xFFFFFF);
	}
}
