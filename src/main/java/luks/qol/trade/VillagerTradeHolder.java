package luks.qol.trade;

/**
 * Серверное состояние «обновления торговли» и «сброса профессии» жителя.
 * Реализуется миксином на Villager. Все таймеры считаются по игровому времени мира,
 * поэтому переживают закрытие окна, перезаход и выгрузку чанка.
 */
public interface VillagerTradeHolder {
    /** Это житель с настоящей профессией (не безработный и не бездельник). */
    boolean luksqol$isSupported();

    /** С жителем уже была хотя бы одна успешная сделка. */
    boolean luksqol$hasTraded();

    /** Сколько секунд осталось до обновления предложений (0 — таймер не идёт). */
    int luksqol$refreshSeconds();

    /** Сколько секунд осталось до сброса профессии (0 — таймер не идёт). */
    int luksqol$resetSeconds();

    boolean luksqol$startRefresh();

    boolean luksqol$startReset();

    boolean luksqol$cancelReset();
}
