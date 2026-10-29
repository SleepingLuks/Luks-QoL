package luks.qol;

import luks.qol.bush.MarkerBushBlock;
import luks.qol.dragon.VisitedStructures;
import luks.qol.iceplant.IceSproutBlock;
import luks.qol.iceplant.IceSproutBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Set;

/** Блоки Luks QoL. Item-версии регистрируются в LuksItems. */
public final class LuksBlocks {
    private LuksBlocks() {}

    public static final ResourceKey<Block> ICE_BED_KEY = key("ice_bed");
    public static final ResourceKey<Block> ICE_SPROUT_KEY = key("ice_sprout");
    public static final ResourceKey<Block> RED_MAGMA_KEY = key("red_magma");
    public static final ResourceKey<Block> RED_MARKER_BUSH_KEY = key("red_marker_bush");
    public static final ResourceKey<Block> TEAL_MARKER_BUSH_KEY = key("teal_marker_bush");
    public static final ResourceKey<Block> GLOWING_BLACKSTONE_KEY = key("glowing_blackstone");
    public static final ResourceKey<Block> GLOWING_BASALT_KEY = key("glowing_basalt");
    public static final ResourceKey<Block> STURDY_BONE_BLOCK_KEY = key("sturdy_bone_block");
    public static final ResourceKey<Block> FUSED_BONE_BLOCK_KEY = key("fused_bone_block");

    /** Ледяная грядка — блок, на который сажают ледяные семена. */
    public static final Block ICE_BED = Registry.register(BuiltInRegistries.BLOCK, ICE_BED_KEY,
            new Block(BlockBehaviour.Properties.of()
                    .setId(ICE_BED_KEY)
                    .strength(0.6F)
                    .sound(LuksSounds.ICE_BED)));

    /** Растение. Лута нет: при ручной поломке ничего не выпадает. */
    public static final Block ICE_SPROUT = Registry.register(BuiltInRegistries.BLOCK, ICE_SPROUT_KEY,
            new IceSproutBlock(BlockBehaviour.Properties.of()
                    .setId(ICE_SPROUT_KEY)
                    .noCollision()
                    .instabreak()
                    .noLootTable()
                    .sound(net.minecraft.world.level.block.SoundType.SNOW)
                    .pushReaction(PushReaction.POPPED)));

    /** Красная магма — ингредиент Дракона крепости. Светится, но (в отличие от обычной магмы) не обжигает. */
    public static final Block RED_MAGMA = Registry.register(BuiltInRegistries.BLOCK, RED_MAGMA_KEY,
            new Block(BlockBehaviour.Properties.of()
                    .setId(RED_MAGMA_KEY)
                    .mapColor(MapColor.COLOR_RED)
                    .requiresCorrectToolForDrops()
                    .strength(0.5F)
                    .lightLevel(state -> 4)
                    .sound(SoundType.STONE)));

    /** Красный куст: наклоном показывает направление к Багровому лесу. Генерируется в Незере. */
    public static final Block RED_MARKER_BUSH = Registry.register(BuiltInRegistries.BLOCK, RED_MARKER_BUSH_KEY,
            new MarkerBushBlock(BlockBehaviour.Properties.of()
                    .setId(RED_MARKER_BUSH_KEY)
                    .mapColor(MapColor.CRIMSON_NYLIUM)
                    .noCollision()
                    .instabreak()
                    .sound(SoundType.ROOTS)
                    .pushReaction(PushReaction.POPPED),
                    Biomes.CRIMSON_FOREST, VisitedStructures.CRIMSON_FOREST_CELLS));

    /** Бирюзовый куст: наклоном показывает направление к Искажённому лесу. Генерируется в Незере. */
    public static final Block TEAL_MARKER_BUSH = Registry.register(BuiltInRegistries.BLOCK, TEAL_MARKER_BUSH_KEY,
            new MarkerBushBlock(BlockBehaviour.Properties.of()
                    .setId(TEAL_MARKER_BUSH_KEY)
                    .mapColor(MapColor.WARPED_NYLIUM)
                    .noCollision()
                    .instabreak()
                    .sound(SoundType.ROOTS)
                    .pushReaction(PushReaction.POPPED),
                    Biomes.WARPED_FOREST, VisitedStructures.WARPED_FOREST_CELLS));

    /** Светящийся чернит (блок): ингредиент Дракона бастиона. */
    public static final Block GLOWING_BLACKSTONE = Registry.register(BuiltInRegistries.BLOCK, GLOWING_BLACKSTONE_KEY,
            new Block(BlockBehaviour.Properties.of()
                    .setId(GLOWING_BLACKSTONE_KEY)
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)
                    .lightLevel(state -> 7)
                    .sound(SoundType.STONE)));

    /** Светящийся базальт (блок): ингредиент Дракона бастиона. */
    public static final Block GLOWING_BASALT = Registry.register(BuiltInRegistries.BLOCK, GLOWING_BASALT_KEY,
            new Block(BlockBehaviour.Properties.of()
                    .setId(GLOWING_BASALT_KEY)
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(1.25F, 4.2F)
                    .lightLevel(state -> 7)
                    .sound(SoundType.BASALT)));

    /** Крепкий костяной блок: 9 костяных блоков; переплавляется в крепкий плавленый. */
    public static final Block STURDY_BONE_BLOCK = Registry.register(BuiltInRegistries.BLOCK, STURDY_BONE_BLOCK_KEY,
            new Block(BlockBehaviour.Properties.of()
                    .setId(STURDY_BONE_BLOCK_KEY)
                    .mapColor(MapColor.SAND)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.BONE_BLOCK)));

    /** Крепкий плавленый костяной блок: центр рецепта Дракона бастиона. */
    public static final Block FUSED_BONE_BLOCK = Registry.register(BuiltInRegistries.BLOCK, FUSED_BONE_BLOCK_KEY,
            new Block(BlockBehaviour.Properties.of()
                    .setId(FUSED_BONE_BLOCK_KEY)
                    .mapColor(MapColor.COLOR_BROWN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 12.0F)
                    .sound(SoundType.BONE_BLOCK)));

    public static final BlockEntityType<IceSproutBlockEntity> ICE_SPROUT_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, "ice_sprout"),
            new BlockEntityType<>(IceSproutBlockEntity::new, Set.of(ICE_SPROUT)));

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, name));
    }

    /** Вызов нужен, чтобы класс загрузился и всё зарегистрировалось. */
    public static void register() {}
}
