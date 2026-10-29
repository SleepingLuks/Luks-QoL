package luks.qol.mixin;

import luks.qol.LuksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Когда Око Эндера лопается, ваниль шлёт клиентам событие 2003, а клиент рисует осколки с ЖЁСТКО зашитой текстурой
 * обычного Ока Эндера. Для Ока Пустоты это событие заменяется точно таким же эффектом, но с осколками его текстуры.
 * Остальные глаза (в том числе обычные Очи Эндера) не затрагиваются.
 */
@Mixin(EyeOfEnder.class)
public abstract class EyeOfEnderMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void luksqol$breakEffect(Level level, int type, BlockPos pos, int data) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        if (type == 2003 && level instanceof ServerLevel serverLevel && self.getItem().is(LuksItems.VOID_EYE)) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY();
            double z = pos.getZ() + 0.5D;
            RandomSource random = serverLevel.getRandom();
            ItemParticleOption shards = new ItemParticleOption(ParticleTypes.ITEM, LuksItems.VOID_EYE);
            // Осколки разлетаются (count = 0: dx/dy/dz — скорость частицы).
            for (int i = 0; i < 8; i++) {
                serverLevel.sendParticles(shards, false, false, x, y, z, 0,
                        random.nextGaussian() * 0.15D, random.nextDouble() * 0.2D, random.nextGaussian() * 0.15D, 1.0D);
            }
            // Кольцо частиц портала, как у ванильного ока.
            for (double a = 0.0D; a < Math.PI * 2.0D; a += 0.15707963267948966D) {
                double cos = Math.cos(a);
                double sin = Math.sin(a);
                serverLevel.sendParticles(ParticleTypes.PORTAL, false, false, x + cos * 5.0D, y - 0.4D, z + sin * 5.0D,
                        0, cos * -5.0D, 0.0D, sin * -5.0D, 1.0D);
                serverLevel.sendParticles(ParticleTypes.PORTAL, false, false, x + cos * 5.0D, y - 0.4D, z + sin * 5.0D,
                        0, cos * -7.0D, 0.0D, sin * -7.0D, 1.0D);
            }
            return;
        }
        level.levelEvent(type, pos, data);
    }
}
