package dev.elv1n200.sessionlogin;

import dev.elv1n200.sessionlogin.account.AccountStore;
import dev.elv1n200.sessionlogin.config.Settings;
import dev.elv1n200.sessionlogin.util.IntegrityCheck;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import dev.elv1n200.sessionlogin.vault.VaultManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class SessionLogin implements ModInitializer {
	public static final String MOD_ID = "sessionlogin";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** The real session Minecraft started with. Never modified. */
	public static Session originalSession;

	/** The session currently exposed to the game (may be swapped). */
	public static Session currentSession;

	/** When false, the mixin returns the vanilla session untouched. */
	public static boolean overrideSession = false;

	/** Persistent, token-only account list. */
	public static AccountStore accountStore;

	/** Encryption mode owner for the account store. */
	public static VaultManager vault;

	/** Privacy/QoL toggles persisted in config. */
	public static Settings settings;

	@Override
	public void onInitialize() {
		originalSession = SessionUtils.getSession();
		currentSession = originalSession;
		overrideSession = true;

		Path configDir =
				FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
		IntegrityCheck.run(configDir);
		settings = new Settings(configDir);
		settings.load();
		vault = new VaultManager(configDir);
		vault.init();
		accountStore = new AccountStore(configDir, vault);
		accountStore.load();

		String modVersion = FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("1.?.?");
		String modAuthor = FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getAuthors().isEmpty()
						? "Unknown"
						: c.getMetadata().getAuthors().iterator().next().getName())
				.orElse("Unknown");

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.getWindow() != null) {
				client.getWindow().setTitle(
						"SessionID Login • v" + modVersion + " • by " + modAuthor);
			}
		});

		LOGGER.info("SessionLogin initialized (token-only, no telemetry).");
	}
}
