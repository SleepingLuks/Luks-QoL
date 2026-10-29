package luks.qol.client;

import luks.qol.LuksMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class LuksQoLClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(LuksMenus.GHAST_BOOST, GhastBoostScreen::new);
    }
}
