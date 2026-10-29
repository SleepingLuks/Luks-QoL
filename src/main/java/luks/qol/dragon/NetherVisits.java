package luks.qol.dragon;

import luks.qol.bush.BiomeCompass;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Отмечает посещённые структуры (крепость, бастион) и леса Незера. Крепость или бастион считаются посещёнными, когда игрок находится внутри их построек (а не просто
 * в большой пустой рамке вокруг: она огромная, поэтому проверяем именно элементы структуры).
 */
public final class NetherVisits {
    private NetherVisits() {}

    /** Вызывается каждый тик игрока; проверка раз в секунду и только в Незере. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0 || !player.isAlive() || player.isSpectator()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !Level.NETHER.equals(level.dimension())) {
            return;
        }
        check(level, player, BuiltinStructures.FORTRESS, VisitedStructures.FORTRESSES);
        check(level, player, BuiltinStructures.BASTION_REMNANT, VisitedStructures.BASTIONS);

        // Леса: игрок «посетил» лес, когда стоит в его биоме; запоминаем ячейку 128×128 блоков (для кустов-указателей).
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        if (biome.is(Biomes.CRIMSON_FOREST)) {
            markCell(level, player, VisitedStructures.CRIMSON_FOREST_CELLS);
        } else if (biome.is(Biomes.WARPED_FOREST)) {
            markCell(level, player, VisitedStructures.WARPED_FOREST_CELLS);
        }
    }

    private static void markCell(ServerLevel level, ServerPlayer player, SavedDataType<VisitedStructures> type) {
        VisitedStructures.get(level, type).markVisited(player.getUUID(), BiomeCompass.cellKey(player.getBlockX(), player.getBlockZ()));
    }

    private static void check(ServerLevel level, ServerPlayer player, ResourceKey<Structure> key,
                              SavedDataType<VisitedStructures> type) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Structure structure = registry.getOrThrow(key).value();
        StructureStart start = level.structureManager()
                .getStructureWithPieceAt(player.getBlockX(), player.getBlockY(), player.getBlockZ(), structure);
        if (start == null || !start.isValid()) {
            return;
        }
        VisitedStructures.get(level, type).markVisited(player.getUUID(), start.getChunkPos().pack());
    }
}
