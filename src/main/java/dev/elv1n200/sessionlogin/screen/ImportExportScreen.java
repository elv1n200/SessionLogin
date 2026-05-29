package dev.elv1n200.sessionlogin.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.elv1n200.sessionlogin.SessionLogin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
	private Text status;

	public ImportExportScreen(boolean importMode) {
		super(Text.literal(""));
		this.importMode = importMode;
		this.status = Text.literal(importMode
				? "Place export file as <gameDir>/" + FILE_NAME
				: "Will write to <gameDir>/" + FILE_NAME)
				.formatted(Formatting.GRAY);
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		String action = importMode ? "Import" : "Export";
		this.addDrawableChild(ButtonWidget.builder(Text.literal(action), b -> {
			if (importMode) {
				doImport();
			} else {
				doExport();
			}
		}).dimensions(cx - 100, cy, 200, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
			assert this.client != null;
			this.client.setScreen(new AccountManagerScreen());
		}).dimensions(cx - 100, cy + 25, 200, 20).build());
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
			status = Text.literal("Exported to " + file())
					.formatted(Formatting.GREEN);
		} catch (Exception e) {
			status = Text.literal("Export failed: " + e.getMessage())
					.formatted(Formatting.RED);
		}
	}

	private void doImport() {
		try {
			if (!Files.exists(file())) {
				status = Text.literal("No file at " + file())
						.formatted(Formatting.RED);
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
			status = Text.literal("Imported. " + (SessionLogin.vault.isPasswordMode()
					? "Vault locked — unlock to use." : "Done."))
					.formatted(Formatting.GREEN);
		} catch (Exception e) {
			status = Text.literal("Import failed: " + e.getMessage())
					.formatted(Formatting.RED);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int cy = this.height / 2;
		context.drawCenteredTextWithShadow(this.textRenderer,
				surroundWithObfuscated(Text.literal(
						importMode ? "Import vault" : "Export vault")
						.formatted(Formatting.AQUA), 4),
				cx, cy - 40, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, status,
				cx, cy - 22, 0xFFFFFF);
	}
}
