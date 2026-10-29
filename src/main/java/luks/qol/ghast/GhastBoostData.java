package luks.qol.ghast;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;

/**
 * Синхронизируемое поле гаста: игровое время (тики), когда закончится текущий Снежок ускорения (0 = нет ускорения).
 * Клиент по нему рисует шкалу таймера. Значение меняется только при старте/продлении ускорения,
 * поэтому сетевого трафика почти нет.
 */
public final class GhastBoostData {
    private GhastBoostData() {}

    public static final EntityDataAccessor<Long> END_TIME;

    static {
        // defineId считает номера полей по цепочке родителей, поэтому сначала гарантируем,
        // что HappyGhast (и его родители) уже инициализированы и заняли свои номера.
        try {
            Class.forName(HappyGhast.class.getName(), true, HappyGhast.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        END_TIME = SynchedEntityData.defineId(HappyGhast.class, EntityDataSerializers.LONG);
    }

    /** Вызов нужен, чтобы класс загрузился в момент запуска мода (до появления сущностей). */
    public static void register() {}
}
