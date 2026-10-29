package luks.qol;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuksQoL implements ModInitializer {
    public static final String MOD_ID = "luks_qol";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        luks.qol.ghast.GhastBoostData.register();
        LuksSounds.register();
        LuksBlocks.register();
        LuksItems.register();
        LuksTabs.register();
        luks.qol.dragon.DragonFlights.register();
        registerMarkerBushes();
        registerVillagerLeash();
        SpawnerSilkTouch.register();
        LOGGER.info("Luks QoL loaded");
    }

    /** Красные и бирюзовые кусты-указатели: естественная генерация во всех биомах Незера (сами кусты описаны в data/.../worldgen). */
    private static void registerMarkerBushes() {
        for (String name : new String[]{"red_marker_bush", "teal_marker_bush"}) {
            BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(),
                    GenerationStep.Decoration.VEGETAL_DECORATION,
                    ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(MOD_ID, name)));
        }
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> luks.qol.bush.MarkerBushBlock.clearCaches());
    }

    // Поводок в руке + правый клик по жителю = ТОЛЬКО привязка.
    private static void registerVillagerLeash() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.LEAD)) {
                return InteractionResult.PASS;
            }

            if (!"Villager".equals(entity.getClass().getSimpleName())) {
                return InteractionResult.PASS;
            }

            if (!(entity instanceof Leashable leashable)) {
                return InteractionResult.PASS;
            }

            if (level instanceof ServerLevel) {
                if (!leashable.isLeashed()) {
                    leashable.setLeashedTo(player, true);
                    if (!player.hasInfiniteMaterials()) {
                        stack.shrink(1);
                    }
                }
            }

            return InteractionResult.SUCCESS;
        });
    }
}
