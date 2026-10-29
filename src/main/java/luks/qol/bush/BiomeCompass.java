package luks.qol.bush;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;

/**
 * Серверный поиск ближайшего подходящего биома для кустов-указателей. Ничего не угадывает: опрашивает настоящий
 * источник биомов мира (тот же, по которому генерируется местность) по расходящимся квадратным кольцам вокруг
 * куста и возвращает точку, где биом действительно есть. Если биома в радиусе поиска нет — возвращает null,
 * и куст остаётся прямым (никакого выдуманного направления).
 * Непосещённым биомам приоритет: «посещённость» хранится ячейками 128×128 блоков отдельно для каждого игрока.
 */
public final class BiomeCompass {
    private BiomeCompass() {}

    /** Шаг опроса, блоков (лес — крупные области, более частый шаг не нужен). */
    private static final int STEP = 32;
    /** Сколько колец обходим: 28 × 32 ≈ 900 блоков от куста. */
    private static final int MAX_RINGS = 28;
    /** Ячейка учёта посещений: 1 << 7 = 128 блоков. */
    private static final int CELL_SHIFT = 7;

    public record Target(BlockPos pos, boolean visited) {}

    /** Ключ ячейки 128×128, в которой лежит точка (x, z). */
    public static long cellKey(int x, int z) {
        return ((long) (x >> CELL_SHIFT) << 32) | ((z >> CELL_SHIFT) & 0xFFFFFFFFL);
    }

    /** Ближайшая непосещённая точка биома; если непосещённых нет — ближайшая посещённая; если биома нет вовсе — null. */
    public static Target find(ServerLevel level, BlockPos origin, ResourceKey<Biome> biome, Set<Long> visitedCells) {
        BlockPos nearestVisited = null;
        long nearestVisitedDistance = Long.MAX_VALUE;
        // Лес в Незере объёмный по высоте, поэтому опрашиваем три высоты: на уровне куста и по 20 блоков выше и ниже.
        int[] heights = {origin.getY(), origin.getY() + 20, origin.getY() - 20};

        for (int ring = 0; ring <= MAX_RINGS; ring++) {
            BlockPos bestNew = null;
            long bestNewDistance = Long.MAX_VALUE;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue; // только внешний контур кольца
                    }
                    int x = origin.getX() + dx * STEP;
                    int z = origin.getZ() + dz * STEP;
                    for (int y : heights) {
                        Holder<Biome> holder = level.getUncachedNoiseBiome(x >> 2, y >> 2, z >> 2);
                        if (!holder.is(biome)) {
                            continue;
                        }
                        long distance = (long) dx * dx + (long) dz * dz;
                        BlockPos point = new BlockPos(x, y, z);
                        if (visitedCells.contains(cellKey(x, z))) {
                            if (distance < nearestVisitedDistance) {
                                nearestVisitedDistance = distance;
                                nearestVisited = point;
                            }
                        } else if (distance < bestNewDistance) {
                            bestNewDistance = distance;
                            bestNew = point;
                        }
                        break; // на этой точке биом найден — другие высоты не нужны
                    }
                }
            }
            if (bestNew != null) {
                return new Target(bestNew, false); // непосещённый нашёлся в этом кольце — ближе уже не будет
            }
        }
        return nearestVisited != null ? new Target(nearestVisited, true) : null;
    }
}
