package luks.qol;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Добыча спавнеров киркой с «Шёлковым касанием».
 * Тип моба (и остальные данные спавнера) сохраняется в предмете,
 * а в названии предмета пишется, какой моб внутри.
 * Опыт за спавнер выпадает только в первый раз: добытый спавнер помечается в предмете (и дальше переносится
 * в установленный блок), а при повторной добыче опыт не выпадает — иначе можно было бы фармить опыт,
 * ставя и ломая один и тот же спавнер.
 * Событие ломания блока выполняется только на сервере.
 */
public final class SpawnerSilkTouch {
    private static final Pattern ID_PATTERN = Pattern.compile("id:\\s*[\"']([a-z0-9_.-]+:[a-z0-9_./-]+)[\"']");

    /** Метка «опыт за этот спавнер уже получали» в пользовательских данных предмета/блока. */
    private static final String COLLECTED_KEY = "luks_qol_collected";

    /** Какой спавнер ломается прямо сейчас без опыта (только сервер, в пределах одного тика). */
    private static BlockPos suppressPos = null;
    private static long suppressTime = -1L;

    private SpawnerSilkTouch() {}

    /** Вызывается миксином SpawnerBlock: нужно ли пропустить выдачу опыта для этого блока. */
    public static boolean shouldSuppressXp(Level level, BlockPos pos) {
        boolean suppress = suppressPos != null && suppressPos.equals(pos) && suppressTime == level.getGameTime();
        if (suppress) {
            // Метка использована — сразу сбрасываем, чтобы она не задела другой спавнер.
            suppressPos = null;
            suppressTime = -1L;
        }
        return suppress;
    }

    private static boolean isCollected(BlockEntity blockEntity) {
        CustomData custom = blockEntity.components().get(DataComponents.CUSTOM_DATA);
        return custom != null && custom.copyTag().contains(COLLECTED_KEY);
    }

    public static void register() {
        // До ломания: если этот спавнер уже добывали раньше, опыт за него больше не выдаётся (любым инструментом).
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (state.is(Blocks.SPAWNER)) {
                if (blockEntity != null && isCollected(blockEntity)) {
                    suppressPos = pos.immutable();
                    suppressTime = level.getGameTime();
                } else {
                    suppressPos = null;
                    suppressTime = -1L;
                }
            }
            return true;
        });

        // ВАЖНО: метку подавления опыта здесь НЕ сбрасываем. Событие AFTER у Fabric срабатывает сразу после
        // удаления блока, а опыт выдаётся позже (в spawnAfterBreak) — сброс здесь делал подавление бесполезным.
        // Метка сбрасывается при использовании (shouldSuppressXp) и сама устаревает со сменой тика.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!state.is(Blocks.SPAWNER) || blockEntity == null) {
                return;
            }
            // В творческом режиме предметы не выпадают (иначе дубли).
            if (player.isCreative()) {
                return;
            }
            if (!hasSilkTouch(player.getMainHandItem())) {
                return;
            }

            CompoundTag data = blockEntity.saveWithoutMetadata(level.registryAccess());

            ItemStack drop = new ItemStack(Items.SPAWNER);
            // Метка «опыт уже получен»: переживает установку и повторную добычу спавнера.
            CompoundTag collected = new CompoundTag();
            collected.putBoolean(COLLECTED_KEY, true);
            drop.set(DataComponents.CUSTOM_DATA, CustomData.of(collected));
            drop.set(DataComponents.BLOCK_ENTITY_DATA,
                    TypedEntityData.<BlockEntityType<?>>of(blockEntity.getType(), data));

            // Подпись: «Рассадник: <моб>»
            String mobId = findMobId(data.toString());
            if (mobId != null) {
                Component name = Component.translatable("block.minecraft.spawner")
                        .append(Component.literal(": "))
                        .append(Component.translatable("entity." + mobId.replace(':', '.')));
                drop.set(DataComponents.ITEM_NAME, name);
                // Текстура предмета по мобу (выбирается в assets/minecraft/items/spawner.json)
                drop.set(DataComponents.CUSTOM_MODEL_DATA,
                        new CustomModelData(List.of(), List.of(), List.of(mobId), List.of()));
            }

            Block.popResource(level, pos, drop);
        });
    }

    /** Ищет в данных спавнера id существующего моба (например minecraft:zombie). */
    private static String findMobId(String snbt) {
        Matcher matcher = ID_PATTERN.matcher(snbt);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            boolean isEntity = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .anyMatch(key -> key.toString().equals(candidate));
            if (isEntity) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean hasSilkTouch(ItemStack tool) {
        for (Holder<Enchantment> enchantment : tool.getEnchantments().keySet()) {
            if (enchantment.is(Enchantments.SILK_TOUCH)) {
                return true;
            }
        }
        return false;
    }
}
