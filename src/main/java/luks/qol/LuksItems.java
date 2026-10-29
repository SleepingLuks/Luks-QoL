package luks.qol;

import luks.qol.dragon.DragonStyle;
import luks.qol.dragon.StructureDragonItem;
import luks.qol.dragon.VisitedStructures;
import luks.qol.eye.VoidEyeItem;
import luks.qol.shard.ReturnShardItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

/** Предметы Luks QoL. */
public final class LuksItems {
    private LuksItems() {}

    private static final ResourceKey<Item> ICE_BED_KEY = key("ice_bed");
    private static final ResourceKey<Item> ICE_SEEDS_KEY = key("ice_seeds");

    public static final Item ICE_BED = Registry.register(BuiltInRegistries.ITEM, ICE_BED_KEY,
            new BlockItem(LuksBlocks.ICE_BED, new Item.Properties()
                    .setId(ICE_BED_KEY)
                    .useBlockDescriptionPrefix()));

    /** Ледяные семена: ставятся только на Ледяную грядку (проверка в IceSproutBlock.canSurvive). */
    public static final Item ICE_SEEDS = Registry.register(BuiltInRegistries.ITEM, ICE_SEEDS_KEY,
            new BlockItem(LuksBlocks.ICE_SPROUT, new Item.Properties()
                    .setId(ICE_SEEDS_KEY)
                    .overrideDescription("item.luks_qol.ice_seeds")));

    private static final ResourceKey<Item> SPEED_SNOWBALL_KEY = key("speed_snowball");

    /** Снежок ускорения: даёт счастливому гасту ускорение на 25 секунд (кормится через слот в инвентаре гаста). */
    public static final Item SPEED_SNOWBALL = Registry.register(BuiltInRegistries.ITEM, SPEED_SNOWBALL_KEY,
            new Item(new Item.Properties()
                    .setId(SPEED_SNOWBALL_KEY)
                    .stacksTo(16)
                    .rarity(Rarity.RARE)));

    private static final ResourceKey<Item> RETURN_SHARD_KEY = key("return_shard");

    /** Осколок возвращения: бросается как жемчуг (только в Краю), по столкновению возвращает на спавн-платформу Края. */
    public static final Item RETURN_SHARD = Registry.register(BuiltInRegistries.ITEM, RETURN_SHARD_KEY,
            new ReturnShardItem(new Item.Properties()
                    .setId(RETURN_SHARD_KEY)
                    .stacksTo(16)
                    .rarity(Rarity.RARE)));

    private static final ResourceKey<Item> VOID_EYE_KEY = key("void_eye");

    /** Око Пустоты: бросается как Око Эндера и указывает на город Края. */
    public static final Item VOID_EYE = Registry.register(BuiltInRegistries.ITEM, VOID_EYE_KEY,
            new VoidEyeItem(new Item.Properties()
                    .setId(VOID_EYE_KEY)
                    .stacksTo(16)
                    .rarity(Rarity.RARE)));

    private static final ResourceKey<Item> RED_MAGMA_KEY = key("red_magma");
    private static final ResourceKey<Item> GLOWING_MAGMA_CREAM_KEY = key("glowing_magma_cream");
    private static final ResourceKey<Item> FORTRESS_DRAGON_KEY = key("fortress_dragon");

    /** Красная магма: 9 обычных блоков магмы в верстаке. */
    public static final Item RED_MAGMA = Registry.register(BuiltInRegistries.ITEM, RED_MAGMA_KEY,
            new BlockItem(LuksBlocks.RED_MAGMA, new Item.Properties()
                    .setId(RED_MAGMA_KEY)
                    .useBlockDescriptionPrefix()));

    /** Светящийся сгусток магмы: обычный сгусток магмы в центре + 8 блоков светокамня. */
    public static final Item GLOWING_MAGMA_CREAM = Registry.register(BuiltInRegistries.ITEM, GLOWING_MAGMA_CREAM_KEY,
            new Item(new Item.Properties()
                    .setId(GLOWING_MAGMA_CREAM_KEY)
                    .rarity(Rarity.UNCOMMON)));

    /** Дракон крепости: одноразовый навигатор к крепости Незера (только в Незере). */
    public static final Item FORTRESS_DRAGON = Registry.register(BuiltInRegistries.ITEM, FORTRESS_DRAGON_KEY,
            new StructureDragonItem(new Item.Properties()
                    .setId(FORTRESS_DRAGON_KEY)
                    .stacksTo(16)
                    .rarity(Rarity.RARE),
                    BuiltinStructures.FORTRESS, VisitedStructures.FORTRESSES, DragonStyle.FORTRESS,
                    "luks_qol.fortress_dragon"));

    private static final ResourceKey<Item> GLOWING_BLACKSTONE_KEY = key("glowing_blackstone");
    private static final ResourceKey<Item> GLOWING_BASALT_KEY = key("glowing_basalt");
    private static final ResourceKey<Item> STURDY_BONE_BLOCK_KEY = key("sturdy_bone_block");
    private static final ResourceKey<Item> FUSED_BONE_BLOCK_KEY = key("fused_bone_block");
    private static final ResourceKey<Item> BASTION_DRAGON_KEY = key("bastion_dragon");

    /** Светящийся чернит: чернит в центре + 8 блоков магмы. */
    public static final Item GLOWING_BLACKSTONE = Registry.register(BuiltInRegistries.ITEM, GLOWING_BLACKSTONE_KEY,
            new BlockItem(LuksBlocks.GLOWING_BLACKSTONE, new Item.Properties()
                    .setId(GLOWING_BLACKSTONE_KEY).useBlockDescriptionPrefix()));

    /** Светящийся базальт: базальт в центре + 8 блоков магмы. */
    public static final Item GLOWING_BASALT = Registry.register(BuiltInRegistries.ITEM, GLOWING_BASALT_KEY,
            new BlockItem(LuksBlocks.GLOWING_BASALT, new Item.Properties()
                    .setId(GLOWING_BASALT_KEY).useBlockDescriptionPrefix()));

    /** Крепкий костяной блок: 9 костяных блоков. */
    public static final Item STURDY_BONE_BLOCK = Registry.register(BuiltInRegistries.ITEM, STURDY_BONE_BLOCK_KEY,
            new BlockItem(LuksBlocks.STURDY_BONE_BLOCK, new Item.Properties()
                    .setId(STURDY_BONE_BLOCK_KEY).useBlockDescriptionPrefix()));

    /** Крепкий плавленый костяной блок: крепкий костяной блок, переплавленный в печи. */
    public static final Item FUSED_BONE_BLOCK = Registry.register(BuiltInRegistries.ITEM, FUSED_BONE_BLOCK_KEY,
            new BlockItem(LuksBlocks.FUSED_BONE_BLOCK, new Item.Properties()
                    .setId(FUSED_BONE_BLOCK_KEY).useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON)));

    /** Дракон бастиона: одноразовый навигатор к бастиону Незера (только в Незере). */
    public static final Item BASTION_DRAGON = Registry.register(BuiltInRegistries.ITEM, BASTION_DRAGON_KEY,
            new StructureDragonItem(new Item.Properties()
                    .setId(BASTION_DRAGON_KEY)
                    .stacksTo(16)
                    .rarity(Rarity.RARE),
                    BuiltinStructures.BASTION_REMNANT, VisitedStructures.BASTIONS, DragonStyle.BASTION,
                    "luks_qol.bastion_dragon"));

    private static final ResourceKey<Item> RED_MARKER_BUSH_KEY = key("red_marker_bush");
    private static final ResourceKey<Item> TEAL_MARKER_BUSH_KEY = key("teal_marker_bush");

    /** Красный куст (указывает на Багровый лес). */
    public static final Item RED_MARKER_BUSH = Registry.register(BuiltInRegistries.ITEM, RED_MARKER_BUSH_KEY,
            new BlockItem(LuksBlocks.RED_MARKER_BUSH, new Item.Properties()
                    .setId(RED_MARKER_BUSH_KEY).useBlockDescriptionPrefix()));

    /** Бирюзовый куст (указывает на Искажённый лес). */
    public static final Item TEAL_MARKER_BUSH = Registry.register(BuiltInRegistries.ITEM, TEAL_MARKER_BUSH_KEY,
            new BlockItem(LuksBlocks.TEAL_MARKER_BUSH, new Item.Properties()
                    .setId(TEAL_MARKER_BUSH_KEY).useBlockDescriptionPrefix()));

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, name));
    }

    public static void register() {}
}
