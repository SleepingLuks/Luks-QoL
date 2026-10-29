package luks.qol.trade;

import net.minecraft.world.inventory.ContainerData;

/**
 * Серверная сторона данных меню торговли. Клиент получает значения штатным механизмом
 * слотов данных (как прогресс печи), поэтому свои сетевые пакеты не нужны.
 */
public final class VillagerTradeData implements ContainerData {
    public static final int FLAGS = 0;
    public static final int REFRESH_SECONDS = 1;
    public static final int RESET_SECONDS = 2;
    public static final int COUNT = 3;

    public static final int FLAG_SUPPORTED = 1;
    public static final int FLAG_TRADED = 2;

    /** Номера кнопок для ванильного пакета «нажата кнопка меню». */
    public static final int BUTTON_REFRESH = 0;
    public static final int BUTTON_RESET = 1;
    public static final int BUTTON_CANCEL_RESET = 2;

    private final VillagerTradeHolder villager;

    public VillagerTradeData(VillagerTradeHolder villager) {
        this.villager = villager;
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case FLAGS -> (villager.luksqol$isSupported() ? FLAG_SUPPORTED : 0)
                    | (villager.luksqol$hasTraded() ? FLAG_TRADED : 0);
            case REFRESH_SECONDS -> villager.luksqol$refreshSeconds();
            case RESET_SECONDS -> villager.luksqol$resetSeconds();
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        // Значения только читаются с жителя.
    }

    @Override
    public int getCount() {
        return COUNT;
    }
}
