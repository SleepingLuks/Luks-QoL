package luks.qol.dragon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;
import java.util.Set;

/**
 * Поиск структур Незера (крепость, позже — бастион) для драконов. Устроен так же, как CitySearch у Ока Пустоты:
 * ванильный поиск «ближайшей структуры» нельзя попросить пропустить уже посещённые, поэтому здесь свой обход
 * областей генерации по кольцам вокруг игрока. Крепость и бастион делят одну сетку размещения
 * («nether_complexes»), поэтому в части областей стоит другая структура — такие области просто пропускаются.
 */
public final class NetherStructureSearch {
    private NetherStructureSearch() {}

    /** Сколько колец областей обходим (область ≈ 432 блока, то есть до ≈ 4 300 блоков от игрока). */
    private static final int MAX_RINGS = 10;
    /** Сколько областей всего проверяем за один бросок. */
    private static final int MAX_CHECKS = 400;
    /** Сколько чанков разрешено подгрузить до стадии «начала структур» за один бросок (у Ока — 24;
     *  здесь чуть больше, потому что часть областей занята бастионами и отсеивается только после загрузки). */
    private static final int MAX_CHUNK_LOADS = 32;

    /** point — ближайшая к игроку часть структуры (центр ближайшего её элемента), а не центр всей огромной рамки. */
    public record Target(BlockPos point, long key, boolean visited, double distanceSqr) {}

    /**
     * Ближайшая непосещённая структура; если непосещённых в радиусе поиска нет — ближайшая посещённая
     * (помечена {@code visited}); если структур нет совсем — null.
     */
    public static Target find(ServerLevel level, BlockPos origin, ResourceKey<Structure> key, Set<Long> visited) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = registry.getOrThrow(key);
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
        Target bestNew = null;
        Target bestOld = null;

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
                        continue; // в этой области другая структура (бастион) или ничего не сгенерировалось
                    }

                    BlockPos point = nearestPoint(start, origin);
                    long startKey = start.getChunkPos().pack();
                    double dxs = point.getX() - origin.getX();
                    double dzs = point.getZ() - origin.getZ();
                    double distance = dxs * dxs + dzs * dzs;
                    boolean seen = visited.contains(startKey);
                    Target target = new Target(point, startKey, seen, distance);
                    if (seen) {
                        if (bestOld == null || distance < bestOld.distanceSqr()) {
                            bestOld = target;
                        }
                    } else if (bestNew == null || distance < bestNew.distanceSqr()) {
                        bestNew = target;
                    }
                }
            }
            if (bestNew != null) {
                break; // непосещённая нашлась в этом кольце — ближе искать не нужно
            }
        }
        return bestNew != null ? bestNew : bestOld;
    }

    /** Центр ближайшего к игроку элемента структуры (по горизонтали); если элементов нет — центр рамки. */
    private static BlockPos nearestPoint(StructureStart start, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (StructurePiece piece : start.getPieces()) {
            BlockPos center = piece.getBoundingBox().getCenter();
            double dx = center.getX() - origin.getX();
            double dz = center.getZ() - origin.getZ();
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = center;
            }
        }
        return best != null ? best : start.getBoundingBox().getCenter();
    }
}
