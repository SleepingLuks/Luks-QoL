package luks.qol;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Вкладка Luks QoL в творческом инвентаре: все предметы мода можно взять без команд.
 * Все 14 ванильных ячеек вкладок заняты, поэтому позиция не задаётся (row = null, column = -1):
 * Fabric API сам ставит такую вкладку на отдельную страницу панели вкладок.
 */
public final class LuksTabs {
    private LuksTabs() {}

    public static final ResourceKey<CreativeModeTab> MAIN_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, "main"));

    public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_KEY,
            new CreativeModeTab.Builder(null, -1)
                    .title(Component.translatable("itemGroup.luks_qol.main"))
                    .icon(() -> new ItemStack(LuksItems.SPEED_SNOWBALL))
                    .displayItems((parameters, output) -> {
                        output.accept(LuksItems.ICE_BED);
                        output.accept(LuksItems.ICE_SEEDS);
                        output.accept(LuksItems.SPEED_SNOWBALL);
                        output.accept(LuksItems.RETURN_SHARD);
                        output.accept(LuksItems.VOID_EYE);
                        output.accept(LuksItems.RED_MAGMA);
                        output.accept(LuksItems.GLOWING_MAGMA_CREAM);
                        output.accept(LuksItems.FORTRESS_DRAGON);
                        output.accept(LuksItems.GLOWING_BLACKSTONE);
                        output.accept(LuksItems.GLOWING_BASALT);
                        output.accept(LuksItems.STURDY_BONE_BLOCK);
                        output.accept(LuksItems.FUSED_BONE_BLOCK);
                        output.accept(LuksItems.BASTION_DRAGON);
                        output.accept(LuksItems.RED_MARKER_BUSH);
                        output.accept(LuksItems.TEAL_MARKER_BUSH);
                    })
                    .build());

    public static void register() {}
}
