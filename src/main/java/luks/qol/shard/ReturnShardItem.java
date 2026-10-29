package luks.qol.shard;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Осколок возвращения. Бросается как эндер-жемчуг, но летит дальше. Работает только в Краю.
 * Летящий осколок — обычная сущность жемчуга с этим предметом внутри (поэтому в полёте выглядит как сам осколок);
 * всё остальное поведение подменяет ThrownEnderpearlMixin через {@link ReturnShardLogic}.
 */
public class ReturnShardItem extends Item {
    /** У эндер-жемчуга 1,5 — осколок летит заметно быстрее и дальше. */
    private static final float SHOOT_POWER = 2.4F;

    public ReturnShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Только Край: ни в Верхнем мире, ни в Незере предмет не бросается и не расходуется.
        if (!Level.END.equals(level.dimension())) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("luks_qol.return_shard.only_end"));
            }
            return InteractionResult.FAIL;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_PEARL_THROW,
                    SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            Projectile.spawnProjectileFromRotation(
                    (lvl, owner, projectileStack) -> new ThrownEnderpearl(lvl, owner, projectileStack.copyWithCount(1)),
                    serverLevel, stack, player, 0.0F, SHOOT_POWER, 1.0F);
            // Предмет уходит при броске; если осколок так и не врежется в поверхность, он вернётся (см. ReturnShardLogic).
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
