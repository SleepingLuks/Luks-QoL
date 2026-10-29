package luks.qol.eye;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Отмечает город посещённым, когда игрок действительно находится внутри границ структуры. */
public final class CityVisits {
    private CityVisits() {}

    /** Вызывается каждый тик игрока; проверка раз в секунду только в Краю. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0 || !player.isAlive() || player.isSpectator()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !Level.END.equals(level.dimension())) {
            return;
        }
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Structure city = registry.getOrThrow(BuiltinStructures.END_CITY).value();

        // Внутри ограничивающего объёма города (а не просто рядом).
        StructureStart start = level.structureManager().getStructureAt(player.blockPosition(), city);
        if (start == null || !start.isValid()) {
            return;
        }
        VisitedCities.get(level).markVisited(player.getUUID(), start.getChunkPos().pack());
    }
}
