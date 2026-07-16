package dev.elv1n200.sessionlogin.mixin;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryEventSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Returns a disabled telemetry sender when the toggle is on, so events Mojang
 * would normally receive are simply dropped on the client. Same approach as
 * "No Chat Reports".
 */
@Mixin(ClientTelemetryManager.class)
public class TelemetryManagerMixin {

	@Inject(method = "getOutsideSessionSender", at = @At("HEAD"), cancellable = true)
	private void sessionlogin$blockTelemetry(
			CallbackInfoReturnable<TelemetryEventSender> cir) {
		if (SessionLogin.settings != null
				&& SessionLogin.settings.blockTelemetry()) {
			cir.setReturnValue(TelemetryEventSender.DISABLED);
		}
	}
}
