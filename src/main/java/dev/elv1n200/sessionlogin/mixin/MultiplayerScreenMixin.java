package dev.elv1n200.sessionlogin.mixin;

import dev.elv1n200.sessionlogin.screen.AccountManagerScreen;
import dev.elv1n200.sessionlogin.screen.EditAccountScreen;
import dev.elv1n200.sessionlogin.screen.LoginScreen;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

	@Unique
	private static volatile Boolean sessionlogin$valid = null;

	@Unique
	private static volatile String sessionlogin$validatedToken = null;

	protected MultiplayerScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void sessionlogin$onInit(CallbackInfo ci) {
		int y = 5;
		int w = 80;
		int h = 20;

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Login"),
				b -> MinecraftClient.getInstance().setScreen(new LoginScreen())
		).dimensions(this.width - 90, y, w, h).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Accounts"),
				b -> MinecraftClient.getInstance().setScreen(new AccountManagerScreen())
		).dimensions(this.width - 180, y, w, h).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"),
				b -> MinecraftClient.getInstance().setScreen(new EditAccountScreen())
		).dimensions(this.width - 270, y, w, h).build());

		// Persistent logged-in indicator, drawn every frame. Uses addDrawable
		// (reliable) instead of injecting render(), which doesn't remap here.
		this.addDrawable((context, mouseX, mouseY, delta) -> {
			String token = MinecraftClient.getInstance()
					.getSession().getAccessToken();
			sessionlogin$revalidateIfNeeded(token);

			Text statusPart;
			if (!token.equals(sessionlogin$validatedToken)
					|| sessionlogin$valid == null) {
				statusPart = Text.literal("[checking...]").formatted(Formatting.GRAY);
			} else if (sessionlogin$valid) {
				statusPart = Text.literal("[✔ valid]").formatted(Formatting.GREEN);
			} else {
				statusPart = Text.literal("[✘ invalid]").formatted(Formatting.RED);
			}

			boolean swapped = !SessionUtils.isOriginalActive();
			Text display = Text.literal(swapped ? "Logged in: " : "Account: ")
					.formatted(swapped ? Formatting.GOLD : Formatting.GRAY)
					.append(Text.literal(SessionUtils.getUsername())
							.formatted(Formatting.WHITE))
					.append(Text.literal(" "))
					.append(statusPart);

			context.drawTextWithShadow(this.textRenderer, display, 5, 6, 0xFFFFFF);
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
