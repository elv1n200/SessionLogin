package dev.elv1n200.sessionlogin.mixin;

import dev.elv1n200.sessionlogin.screen.AccountManagerScreen;
import dev.elv1n200.sessionlogin.screen.EditAccountScreen;
import dev.elv1n200.sessionlogin.screen.LoginScreen;
import dev.elv1n200.sessionlogin.util.ApiUtils;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
	private static Boolean sessionlogin$valid = null;

	@Unique
	private static boolean sessionlogin$started = false;

	protected MultiplayerScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void sessionlogin$onInit(CallbackInfo ci) {
		sessionlogin$valid = null;
		sessionlogin$started = false;

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
	}

	@Inject(method = "render", at = @At("TAIL"), require = 0)
	private void sessionlogin$onRender(DrawContext context, int mouseX,
									   int mouseY, float delta, CallbackInfo ci) {
		String username = SessionUtils.getUsername();

		if (sessionlogin$valid == null && !sessionlogin$started) {
			sessionlogin$started = true;
			new Thread(() -> sessionlogin$valid = ApiUtils.validateSession(
					MinecraftClient.getInstance().getSession().getAccessToken()),
					"SessionValidationThread").start();
		}

		Text status;
		if (sessionlogin$valid == null) {
			status = Text.literal("[... Validating]").formatted(Formatting.GRAY);
		} else if (sessionlogin$valid) {
			status = Text.literal("[OK] Valid").formatted(Formatting.GREEN);
		} else {
			status = Text.literal("[X] Invalid").formatted(Formatting.RED);
		}

		Text display = Text.literal("User: ")
				.append(Text.literal(username).formatted(Formatting.WHITE))
				.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
				.append(status);

		context.drawText(this.textRenderer, display, 5, 10, 0xFFFFFF, false);
	}
}
