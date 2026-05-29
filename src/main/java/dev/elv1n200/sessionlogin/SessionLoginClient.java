package dev.elv1n200.sessionlogin;

import dev.elv1n200.sessionlogin.command.SessionLoginCommand;
import dev.elv1n200.sessionlogin.screen.AccountManagerScreen;
import dev.elv1n200.sessionlogin.screen.UnlockScreen;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class SessionLoginClient implements ClientModInitializer {

	private KeyBinding openManagerKey;

	@Override
	public void onInitializeClient() {
		SessionLoginCommand.register();

		openManagerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.sessionlogin.manager",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openManagerKey.wasPressed()) {
				if (SessionLogin.accountStore.isLocked()) {
					client.setScreen(new UnlockScreen());
				} else {
					client.setScreen(new AccountManagerScreen());
				}
			}
		});

		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.world == null || mc.currentScreen != null) {
				return;
			}
			if (SessionUtils.isOriginalActive()) {
				return;
			}
			Text label = Text.literal("⚠ Alt: ")
					.formatted(Formatting.GOLD)
					.append(Text.literal(SessionUtils.getUsername())
							.formatted(Formatting.WHITE));
			context.drawTextWithShadow(mc.textRenderer, label, 4, 4, 0xFFFFFF);
		});
	}
}
