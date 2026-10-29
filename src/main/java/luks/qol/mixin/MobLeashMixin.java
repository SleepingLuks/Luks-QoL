package luks.qol.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Разрешает привязывать жителей на поводок.
 *
 * В 26.x класс жителя и константа типа сущности переехали, поэтому жителя
 * определяем по простому имени класса ("Villager"). ZombieVillager и
 * WanderingTrader под это условие не попадают.
 * Остальная ванильная логика поводка не меняется.
 */
@Mixin(Mob.class)
public abstract class MobLeashMixin {

    @Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
    private void luksqol$allowVillagerLeash(CallbackInfoReturnable<Boolean> cir) {
        if ("Villager".equals(this.getClass().getSimpleName())) {
            cir.setReturnValue(true);
        }
    }
}
