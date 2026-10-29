package luks.qol.trade;

import net.minecraft.world.inventory.ContainerData;

/** Доступ к данным, которые миксин добавил в меню торговли (MerchantMenu). */
public interface TradeMenuData {
    /** Данные, синхронизируемые с клиентом через обычные слоты данных меню. */
    ContainerData luksqol$tradeData();

    /** Житель на сервере; на клиенте всегда null. */
    VillagerTradeHolder luksqol$villager();
}
