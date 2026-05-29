package dev.elv1n200.sessionlogin.mixin;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional brand spoofing — when the toggle is on, report "vanilla" to the
 * server instead of "fabric". Useful while playing on a swapped session if
 * the server filters modded clients.
 */
@Mixin(ClientBrandRetriever.class)
public class ClientBrandRetrieverMixin {

	@Inject(method = "getClientModName", at = @At("HEAD"),
			cancellable = true, remap = false)
	private static void sessionlogin$spoofBrand(CallbackInfoReturnable<String> cir) {
		if (SessionLogin.settings != null
				&& SessionLogin.settings.spoofBrandAsVanilla()) {
			cir.setReturnValue(ClientBrandRetriever.VANILLA);
		}
	}
}
