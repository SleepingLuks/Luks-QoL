package luks.qol.dragon;

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
 * Структуры Незера, в которых игрок реально побывал. Хранится в данных мира Незера (общие для всех игроков
 * сервера): для каждого игрока — набор «ключей» структур (упакованные координаты чанка, где структура начинается).
 * Для каждого вида структуры свой тип данных, чтобы будущий Дракон бастиона не смешивался с Драконом крепости.
 */
public class VisitedStructures extends SavedData {
    private static final Codec<VisitedStructures> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf())
                    .optionalFieldOf("players", Map.of())
                    .forGetter(VisitedStructures::snapshot)
    ).apply(instance, VisitedStructures::new));

    /** Посещённые крепости Незера. */
    public static final SavedDataType<VisitedStructures> FORTRESSES = type("fortress_dragon_visited_fortresses");
    /** Посещённые бастионы Незера. */
    public static final SavedDataType<VisitedStructures> BASTIONS = type("bastion_dragon_visited_bastions");
    /** Посещённые ячейки (128×128 блоков) Багрового леса — для красных кустов-указателей. */
    public static final SavedDataType<VisitedStructures> CRIMSON_FOREST_CELLS = type("marker_bush_visited_crimson_forest");
    /** Посещённые ячейки Искажённого леса — для бирюзовых кустов-указателей. */
    public static final SavedDataType<VisitedStructures> WARPED_FOREST_CELLS = type("marker_bush_visited_warped_forest");

    private static SavedDataType<VisitedStructures> type(String name) {
        return new SavedDataType<>(
                Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, name),
                VisitedStructures::new,
                CODEC,
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    private final Map<String, Set<Long>> visited = new HashMap<>();

    public VisitedStructures() {}

    private VisitedStructures(Map<String, List<Long>> stored) {
        stored.forEach((player, keys) -> this.visited.put(player, new HashSet<>(keys)));
    }

    public static VisitedStructures get(ServerLevel level, SavedDataType<VisitedStructures> type) {
        return level.getDataStorage().computeIfAbsent(type);
    }

    /** Структуры, которые игрок уже посещал (только для чтения). */
    public Set<Long> visitedBy(UUID player) {
        return this.visited.getOrDefault(player.toString(), Set.of());
    }

    /** Отмечает структуру посещённой. Возвращает true, если раньше она не была отмечена. */
    public boolean markVisited(UUID player, long structureKey) {
        boolean added = this.visited.computeIfAbsent(player.toString(), id -> new HashSet<>()).add(structureKey);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    private Map<String, List<Long>> snapshot() {
        Map<String, List<Long>> copy = new HashMap<>();
        this.visited.forEach((player, keys) -> copy.put(player, new ArrayList<>(keys)));
        return copy;
    }
}
