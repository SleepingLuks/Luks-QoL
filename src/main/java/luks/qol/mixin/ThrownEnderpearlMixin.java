package luks.qol.mixin;

import luks.qol.shard.ReturnShardLogic;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Если внутри «жемчуга» лежит Осколок возвращения — вместо ванильной телепортации (и урона от падения)
 * работает логика осколка. Обычные эндер-жемчуга не затрагиваются.
 */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {
    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void luksqol$shardHit(HitResult hit, CallbackInfo ci) {
        ThrownEnderpearl self = (ThrownEnderpearl) (Object) this;
        if (ReturnShardLogic.isShard(self)) {
            ci.cancel();
            ReturnShardLogic.onHit(self, hit);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void luksqol$shardTick(CallbackInfo ci) {
        ThrownEnderpearl self = (ThrownEnderpearl) (Object) this;
        if (ReturnShardLogic.isShard(self)) {
            ReturnShardLogic.tick(self);
        }
    }
}
