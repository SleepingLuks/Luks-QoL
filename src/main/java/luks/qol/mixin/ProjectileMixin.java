package luks.qol.mixin;

import luks.qol.shard.ReturnShardLogic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Осколок возвращения срабатывает только на поверхности: сквозь мобов и игроков он пролетает. */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void luksqol$shardIgnoresEntities(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ThrownEnderpearl pearl && ReturnShardLogic.isShard(pearl)) {
            cir.setReturnValue(false);
        }
    }
}
