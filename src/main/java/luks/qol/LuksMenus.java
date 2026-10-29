package luks.qol;

import luks.qol.ghast.GhastBoostMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class LuksMenus {
    private LuksMenus() {}

    public static final MenuType<GhastBoostMenu> GHAST_BOOST = Registry.register(BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, "ghast_boost"),
            new MenuType<>(GhastBoostMenu::new, FeatureFlags.VANILLA_SET));

    public static void register() {}
}
