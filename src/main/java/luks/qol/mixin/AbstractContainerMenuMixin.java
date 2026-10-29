package luks.qol.mixin;

import luks.qol.trade.TradeMenuData;
import luks.qol.trade.VillagerTradeData;
import luks.qol.trade.VillagerTradeHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Принимает нажатия кнопок окна торговли. Клиент шлёт ванильный пакет «кнопка меню»,
 * решение (можно ли запускать таймер) принимает только сервер.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void luksqol$tradeButtons(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof TradeMenuData data)) {
            return;
        }
        VillagerTradeHolder villager = data.luksqol$villager();
        if (villager == null || !(player instanceof ServerPlayer)) {
            return;
        }
        boolean changed = switch (id) {
            case VillagerTradeData.BUTTON_REFRESH -> villager.luksqol$startRefresh();
            case VillagerTradeData.BUTTON_RESET -> villager.luksqol$startReset();
            case VillagerTradeData.BUTTON_CANCEL_RESET -> villager.luksqol$cancelReset();
            default -> false;
        };
        cir.setReturnValue(changed);
    }
}
