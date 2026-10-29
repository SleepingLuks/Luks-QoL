package luks.qol.eye;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import luks.qol.LuksQoL;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Города Края, в которые игрок реально заходил (по границам структуры). Хранится в данных мира Края:
 * для каждого игрока — набор «ключей» городов (упакованные координаты чанка, где начинается город).
 */
public class VisitedCities extends SavedData {
    private static final Codec<VisitedCities> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf())
                    .optionalFieldOf("players", Map.of())
                    .forGetter(VisitedCities::snapshot)
    ).apply(instance, VisitedCities::new));

    public static final SavedDataType<VisitedCities> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, "void_eye_visited_cities"),
            VisitedCities::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Set<Long>> visited = new HashMap<>();

    public VisitedCities() {}

    private VisitedCities(Map<String, List<Long>> stored) {
        stored.forEach((player, cities) -> this.visited.put(player, new HashSet<>(cities)));
    }

    public static VisitedCities get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Города, которые игрок уже посещал (только для чтения). */
    public Set<Long> citiesOf(UUID player) {
        return this.visited.getOrDefault(player.toString(), Set.of());
    }

    /** Отмечает город посещённым. Возвращает true, если раньше он не был отмечен. */
    public boolean markVisited(UUID player, long cityKey) {
        boolean added = this.visited.computeIfAbsent(player.toString(), id -> new HashSet<>()).add(cityKey);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    private Map<String, List<Long>> snapshot() {
        Map<String, List<Long>> copy = new HashMap<>();
        this.visited.forEach((player, cities) -> copy.put(player, new ArrayList<>(cities)));
        return copy;
    }
}
