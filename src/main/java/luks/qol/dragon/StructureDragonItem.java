package luks.qol.dragon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Одноразовый предмет-навигатор «дракон» к структуре Незера (крепость или бастион); работает только в Незере.
 * Сервер ищет цель (непосещённым — приоритет), считает расстояние и запускает анимацию из частиц;
 * предмет тратится только если цель найдена. Фиксированы только параметры: что искать, где хранить посещения,
 * как выглядит дракон и какие сообщения показывать.
 */
public class StructureDragonItem extends Item {
    /** Минимальная пауза между использованиями одного игрока, чтобы не устраивать поиск каждый тик. */
    private static final long COOLDOWN_TICKS = 20L;
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    private final ResourceKey<Structure> structure;
    private final SavedDataType<VisitedStructures> visitedType;
    private final DragonStyle style;
    private final String messageKey;

    /** messageKey — префикс переводов, например «luks_qol.fortress_dragon» (+ .only_nether / .no_target / .all_visited / .busy). */
    public StructureDragonItem(Properties properties, ResourceKey<Structure> structure,
                               SavedDataType<VisitedStructures> visitedType, DragonStyle style, String messageKey) {
        super(properties);
        this.structure = structure;
        this.visitedType = visitedType;
        this.style = style;
        this.messageKey = messageKey;
    }

    public static void clearCooldowns() {
        LAST_USE.clear();
    }

    private Component message(String suffix) {
        return Component.translatable(this.messageKey + "." + suffix);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!Level.NETHER.equals(level.dimension())) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(message("only_nether"));
            }
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME; // решение принимает сервер
        }
        if (DragonFlights.isActive(serverPlayer)) {
            player.sendOverlayMessage(message("busy"));
            return InteractionResult.CONSUME;
        }

        long now = serverLevel.getGameTime();
        Long last = LAST_USE.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return InteractionResult.CONSUME;
        }
        LAST_USE.put(player.getUUID(), now);

        VisitedStructures data = VisitedStructures.get(serverLevel, this.visitedType);
        NetherStructureSearch.Target target = NetherStructureSearch.find(
                serverLevel, player.blockPosition(), this.structure, data.visitedBy(player.getUUID()));
        if (target == null) {
            // Честно сообщаем, что цели нет: дракон не взлетает, направление не выдумывается, предмет не тратится.
            player.sendOverlayMessage(message("no_target"));
            return InteractionResult.CONSUME;
        }
        if (target.visited()) {
            player.sendOverlayMessage(message("all_visited"));
        }

        BlockPos point = target.point();
        double dx = point.getX() + 0.5D - player.getX();
        double dz = point.getZ() + 0.5D - player.getZ();
        int distance = (int) Math.round(Math.hypot(dx, dz));

        DragonFlights.start(new DragonFlight(this.style, serverLevel, serverPlayer,
                point.getX() + 0.5D, point.getZ() + 0.5D, distance));

        ItemStack stack = player.getItemInHand(hand);
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
