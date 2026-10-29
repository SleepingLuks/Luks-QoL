package luks.qol.iceplant;

/** Все времена Ледяной грядки в игровых тиках (20 тиков = 1 секунда). */
public final class IcePlantTimings {
    private IcePlantTimings() {}

    /** Первое созревание: 20 минут, разбито на 3 стадии роста. */
    public static final int FIRST_GROW_STEP = 20 * 60 * 20 / 3;
    /** Повторный рост: 15 минут, разбит на 3 стадии роста. */
    public static final int REGROW_STEP = 15 * 60 * 20 / 3;
    /** Время на сбор созревшей порции: 8 минут, потом тает. */
    public static final int MELT_DELAY = 8 * 60 * 20;
    /** Снежков за один сбор. */
    public static final int SNOWBALLS_PER_HARVEST = 2;
}
