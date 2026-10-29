package luks.qol.mixin;

import luks.qol.LuksItems;
import luks.qol.ghast.GhastBoost;
import luks.qol.ghast.GhastBoostData;
import luks.qol.ghast.GhastBoostHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Слот для Снежков ускорения у счастливого гаста (показывается в обычном инвентаре игрока,
 * пока он едет/стоит на гасте).
 * Слот, расход предметов и таймер ускорения работают только на сервере.
 */
@Mixin(HappyGhast.class)
public abstract class HappyGhastMixin implements GhastBoostHolder {
    @Unique
    private final SimpleContainer luksqol$boostSlot = new SimpleContainer(1);
    @Unique
    private int luksqol$boostTicks = 0;

    @Override
    public SimpleContainer luksqol$getBoostSlot() {
        return this.luksqol$boostSlot;
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void luksqol$defineBoostData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(GhastBoostData.END_TIME, 0L);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void luksqol$tickBoost(CallbackInfo ci) {
        HappyGhast self = (HappyGhast) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }

        // Ускорение имеет смысл, только пока гастом реально управляет игрок (на гасте без упряжи или когда он
        // «замер», управления нет). Пока управления нет — снежки не тратятся и таймер стоит, чтобы они не пропадали зря.
        boolean ridden = self.getControllingPassenger() instanceof Player;

        if (this.luksqol$boostTicks > 0 && ridden) {
            GhastBoost.apply(self); // после перезагрузки мира или повторной посадки модификатор нужно вернуть
            if (--this.luksqol$boostTicks == 0) {
                GhastBoost.remove(self);
            }
        } else if (!ridden) {
            GhastBoost.remove(self); // без наездника бонус к скорости не нужен
        }

        // Ускорение закончилось — берём следующий снежок из слота (только если гастом управляют).
        if (ridden && this.luksqol$boostTicks <= 0) {
            ItemStack stack = this.luksqol$boostSlot.getItem(0);
            if (!stack.isEmpty() && stack.is(LuksItems.SPEED_SNOWBALL)) {
                stack.shrink(1);
                this.luksqol$boostSlot.setChanged();
                this.luksqol$boostTicks = GhastBoost.DURATION_TICKS;
                GhastBoost.apply(self);
            }
        }

        // Для шкалы таймера на клиенте: игровое время окончания ускорения (константа, пока ускорение идёт).
        long end = this.luksqol$boostTicks > 0 ? self.level().getGameTime() + this.luksqol$boostTicks : 0L;
        long current = self.getEntityData().get(GhastBoostData.END_TIME);
        if (Math.abs(current - end) > 2L || (end == 0L && current != 0L)) {
            self.getEntityData().set(GhastBoostData.END_TIME, end);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void luksqol$save(ValueOutput output, CallbackInfo ci) {
        this.luksqol$boostSlot.storeAsItemList(output.list("LuksBoostItems", ItemStack.CODEC));
        output.putInt("LuksBoostTicks", this.luksqol$boostTicks);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void luksqol$load(ValueInput input, CallbackInfo ci) {
        this.luksqol$boostSlot.fromItemList(input.listOrEmpty("LuksBoostItems", ItemStack.CODEC));
        this.luksqol$boostTicks = input.getIntOr("LuksBoostTicks", 0);
    }
}
