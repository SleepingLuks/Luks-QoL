package luks.qol.eye;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;
import java.util.Set;

/**
 * Поиск городов Края. Ванильный поиск «ближайшей структуры» нельзя попросить пропустить уже посещённые,
 * поэтому здесь свой обход: областей генерации (клеток размещения структуры) по кольцам вокруг игрока.
 * В каждой области у города ровно одна возможная точка, так что проверок немного.
 * Корабли Края — часть структуры города и отдельной целью не считаются.
 */
public final class CitySearch {
    private CitySearch() {}

    /** Сколько колец областей обходим (область ≈ 320 блоков, то есть до ≈ 3 000 блоков от игрока). */
    private static final int MAX_RINGS = 10;
    /** Сколько областей всего проверяем за один бросок. */
    private static final int MAX_CHECKS = 400;
    /** Сколько чанков разрешено подгрузить до стадии «начала структур» за один бросок. */
    private static final int MAX_CHUNK_LOADS = 24;

    public record City(BlockPos center, long key, boolean visited, double distanceSqr) {}

    /**
     * Ищет подходящий город. Сначала — ближайший непосещённый; если непосещённых в радиусе поиска нет,
     * возвращает ближайший посещённый (помечен {@code visited}); если городов нет совсем — null.
     */
    public static City find(ServerLevel level, BlockPos origin, Set<Long> visited) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = registry.getOrThrow(BuiltinStructures.END_CITY);
        Structure structure = holder.value();

        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        StructureManager manager = level.structureManager();

        RandomSpreadStructurePlacement placement = null;
        List<StructurePlacement> placements = state.getPlacementsForStructure(holder);
        for (StructurePlacement candidate : placements) {
            if (candidate instanceof RandomSpreadStructurePlacement spread) {
                placement = spread;
                break;
            }
        }
        if (placement == null) {
            return null;
        }

        int spacing = placement.spacing();
        long seed = state.getLevelSeed();
        // getPotentialStructureChunk принимает КООРДИНАТЫ ЧАНКА и сам делит их на размер области,
        // поэтому шагаем по чанкам кратно spacing (как это делает ванильный поиск структур).
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;

        int checks = 0;
        int loads = 0;
        City bestNew = null;
        City bestOld = null;

        search:
        for (int ring = 0; ring <= MAX_RINGS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue; // только внешний контур текущего кольца
                    }
                    ChunkPos chunk = placement.getPotentialStructureChunk(seed, originChunkX + spacing * dx, originChunkZ + spacing * dz);
                    if (!placement.isStructureChunk(state, chunk.x(), chunk.z())) {
                        continue;
                    }
                    if (++checks > MAX_CHECKS) {
                        break search;
                    }

                    StructureCheckResult presence = manager.checkStructurePresence(chunk, structure, placement, false);
                    if (presence == StructureCheckResult.START_NOT_PRESENT) {
                        continue;
                    }
                    if (loads >= MAX_CHUNK_LOADS) {
                        continue; // лимит нагрузки исчерпан — дальше только то, что уже проверено
                    }
                    loads++;
                    ChunkAccess loaded = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS);
                    StructureStart start = manager.getStartForStructure(structure, loaded);
                    if (start == null || !start.isValid()) {
                        continue; // в этой области город не сгенерировался
                    }

                    BlockPos center = start.getBoundingBox().getCenter();
                    long key = start.getChunkPos().pack();
                    double dxs = center.getX() - origin.getX();
                    double dzs = center.getZ() - origin.getZ();
                    double distance = dxs * dxs + dzs * dzs;
                    boolean seen = visited.contains(key);
                    City city = new City(center, key, seen, distance);
                    if (seen) {
                        if (bestOld == null || distance < bestOld.distanceSqr()) {
                            bestOld = city;
                        }
                    } else if (bestNew == null || distance < bestNew.distanceSqr()) {
                        bestNew = city;
                    }
                }
            }
            if (bestNew != null) {
                break; // непосещённый нашёлся в этом кольце — ближе искать не нужно
            }
        }
        return bestNew != null ? bestNew : bestOld;
    }
}
