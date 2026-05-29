package dev.elv1n200.sessionlogin.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.elv1n200.sessionlogin.screen.SettingsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Lets ModMenu open the SessionLogin settings screen via its mod list.
 * This class is only loaded when ModMenu is actually installed.
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SettingsScreen::new;
	}
}
