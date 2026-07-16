package dev.elv1n200.sessionlogin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.elv1n200.sessionlogin.command.SessionLoginCommand;
import dev.elv1n200.sessionlogin.screen.AccountManagerScreen;
import dev.elv1n200.sessionlogin.screen.TamperWarningScreen;
import dev.elv1n200.sessionlogin.screen.UnlockScreen;
import dev.elv1n200.sessionlogin.util.IntegrityCheck;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class SessionLoginClient implements ClientModInitializer {

	private KeyMapping openManagerKey;

	@Override
	public void onInitializeClient() {
		SessionLoginCommand.register();

		openManagerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.sessionlogin.manager",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				KeyMapping.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
			private boolean tamperShown = false;

			@Override
			public void onEndTick(Minecraft client) {
				while (openManagerKey.consumeClick()) {
					if (SessionLogin.accountStore.isLocked()) {
						client.setScreen(new UnlockScreen());
					} else {
						client.setScreen(new AccountManagerScreen());
					}
				}
				if (!tamperShown
						&& IntegrityCheck.status() == IntegrityCheck.Status.MISMATCH
						&& client.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
					tamperShown = true;
					client.setScreen(new TamperWarningScreen());
				}
			}
		});

		// NOTE: the in-world "⚠ Alt: <name>" HUD overlay is omitted on 26.x —
		// Fabric's HUD callback moved to the new render-state pipeline. The
		// swapped-account indicator on the multiplayer screen still shows it.
	}
}
