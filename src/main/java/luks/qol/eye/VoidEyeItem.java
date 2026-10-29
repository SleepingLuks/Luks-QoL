package luks.qol.eye;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Око Пустоты: бросается как Око Эндера и указывает на город Края (приоритет — ещё не посещённым).
 * Летящая сущность — обычное Око Эндера с этим предметом внутри, поэтому полёт, частицы и шанс
 * вернуть предмет (как у ванильного ока) работают так же, но выглядит оно как Око Пустоты.
 */
public class VoidEyeItem extends Item {
    /** Минимальная пауза между бросками одного игрока, чтобы не устраивать поиск каждый тик. */
    private static final long COOLDOWN_TICKS = 20L;
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    public VoidEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!Level.END.equals(level.dimension())) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("luks_qol.void_eye.only_end"));
            }
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer)) {
            return InteractionResult.CONSUME; // решение принимает сервер
        }

        long now = serverLevel.getGameTime();
        Long last = LAST_USE.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return InteractionResult.CONSUME;
        }
        LAST_USE.put(player.getUUID(), now);

        VisitedCities data = VisitedCities.get(serverLevel);
        CitySearch.City city = CitySearch.find(serverLevel, player.blockPosition(), data.citiesOf(player.getUUID()));
        if (city == null) {
            // Честно сообщаем, что цели нет; предмет не тратится и направление не выдумывается.
            player.sendOverlayMessage(Component.translatable("luks_qol.void_eye.no_city"));
            return InteractionResult.CONSUME;
        }
        if (city.visited()) {
            player.sendOverlayMessage(Component.translatable("luks_qol.void_eye.all_visited"));
        }

        ItemStack stack = player.getItemInHand(hand);
        EyeOfEnder eye = new EyeOfEnder(level, player.getX(), player.getY(0.5D), player.getZ());
        eye.setItem(stack.copyWithCount(1));
        eye.signalTo(new Vec3(city.center().getX() + 0.5D, city.center().getY() + 0.5D, city.center().getZ() + 0.5D));
        level.addFreshEntity(eye);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
