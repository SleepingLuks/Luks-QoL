package luks.qol.mixin;

import luks.qol.trade.TradeMenuData;
import luks.qol.trade.VillagerTradeData;
import luks.qol.trade.VillagerTradeHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Добавляет в меню торговли слоты данных (флаги и два таймера). На сервере они читаются с жителя,
 * на клиенте это обычный буфер, который игра сама заполняет пакетами синхронизации.
 * Конструктор клиентской версии меню вызывает этот же конструктор, так что слоты совпадают на обеих сторонах.
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin implements TradeMenuData {
    @Unique
    private ContainerData luksqol$tradeData;
    @Unique
    private VillagerTradeHolder luksqol$villager;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",
            at = @At("TAIL"))
    private void luksqol$addTradeData(int containerId, Inventory inventory, Merchant merchant, CallbackInfo ci) {
        ContainerData data;
        if (!merchant.isClientSide() && merchant instanceof VillagerTradeHolder holder) {
            this.luksqol$villager = holder;
            data = new VillagerTradeData(holder);
        } else {
            data = new SimpleContainerData(VillagerTradeData.COUNT);
        }
        this.luksqol$tradeData = data;
        ((AbstractContainerMenuAccessor) (Object) this).luksqol$addDataSlots(data);
    }

    @Override
    public ContainerData luksqol$tradeData() {
        return this.luksqol$tradeData;
    }

    @Override
    public VillagerTradeHolder luksqol$villager() {
        return this.luksqol$villager;
    }
}
