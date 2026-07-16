package dev.elv1n200.sessionlogin.mixin;

import dev.elv1n200.sessionlogin.screen.AccountManagerScreen;
import dev.elv1n200.sessionlogin.screen.EditAccountScreen;
import dev.elv1n200.sessionlogin.screen.LoginScreen;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

	@Unique
	private static volatile Boolean sessionlogin$valid = null;

	@Unique
	private static volatile String sessionlogin$validatedToken = null;

	protected MultiplayerScreenMixin(Minecraft minecraft, Font font, Component title) {
		super(minecraft, font, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void sessionlogin$onInit(CallbackInfo ci) {
		int y = 5;
		int w = 80;
		int h = 20;

		this.addRenderableWidget(Button.builder(Component.literal("Login"),
				b -> Minecraft.getInstance().setScreen(new LoginScreen())
		).bounds(this.width - 90, y, w, h).build());

		this.addRenderableWidget(Button.builder(Component.literal("Accounts"),
				b -> Minecraft.getInstance().setScreen(new AccountManagerScreen())
		).bounds(this.width - 180, y, w, h).build());

		this.addRenderableWidget(Button.builder(Component.literal("Edit"),
				b -> Minecraft.getInstance().setScreen(new EditAccountScreen())
		).bounds(this.width - 270, y, w, h).build());

		// Persistent logged-in indicator, extracted every frame. Uses
		// addRenderableOnly (reliable) instead of injecting the render pass.
		this.addRenderableOnly((extractor, mouseX, mouseY, delta) -> {
			String token = Minecraft.getInstance().getUser().getAccessToken();
			sessionlogin$revalidateIfNeeded(token);

			Component statusPart;
			if (!token.equals(sessionlogin$validatedToken)
					|| sessionlogin$valid == null) {
				statusPart = Component.literal("[checking...]").withStyle(ChatFormatting.GRAY);
			} else if (sessionlogin$valid) {
				statusPart = Component.literal("[✔ valid]").withStyle(ChatFormatting.GREEN);
			} else {
				statusPart = Component.literal("[✘ invalid]").withStyle(ChatFormatting.RED);
			}

			boolean swapped = !SessionUtils.isOriginalActive();
			Component display = Component.literal(swapped ? "Logged in: " : "Account: ")
					.withStyle(swapped ? ChatFormatting.GOLD : ChatFormatting.GRAY)
					.append(Component.literal(SessionUtils.getUsername())
							.withStyle(ChatFormatting.WHITE))
					.append(Component.literal(" "))
					.append(statusPart);

			extractor.text(this.font, display, 5, 6, 0xFFFFFF, true);
		});
	}

	@Unique
	private static void sessionlogin$revalidateIfNeeded(String token) {
		if (token.equals(sessionlogin$validatedToken)
				&& sessionlogin$valid != null) {
			return;
		}
		// kick off one validation per distinct token
		synchronized (MultiplayerScreenMixin.class) {
			if (token.equals(sessionlogin$validatedToken)) {
				return;
			}
			sessionlogin$validatedToken = token;
			sessionlogin$valid = null;
			new Thread(() -> {
				boolean v = ApiUtils.tokenLooksValid(token);
				if (token.equals(sessionlogin$validatedToken)) {
					sessionlogin$valid = v;
				}
			}, "SessionValidationThread").start();
		}
	}
}
