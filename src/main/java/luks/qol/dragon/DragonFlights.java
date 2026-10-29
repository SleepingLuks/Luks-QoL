package luks.qol.dragon;

import luks.qol.LuksQoL;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Список идущих анимаций драконов: тикаются в конце каждого тика сервера. Только серверная логика. */
public final class DragonFlights {
    private DragonFlights() {}

    private static final List<DragonFlight> ACTIVE = new ArrayList<>();

    public static boolean isActive(ServerPlayer player) {
        for (DragonFlight flight : ACTIVE) {
            if (flight.owner() == player) {
                return true;
            }
        }
        return false;
    }

    public static void start(DragonFlight flight) {
        ACTIVE.add(flight);
    }

    private static void tickAll() {
        Iterator<DragonFlight> it = ACTIVE.iterator();
        while (it.hasNext()) {
            DragonFlight flight = it.next();
            boolean alive;
            try {
                alive = flight.tick();
            } catch (RuntimeException e) {
                // Ошибка в анимации не должна ронять сервер — просто убираем дракона.
                LuksQoL.LOGGER.error("Fortress Dragon animation failed", e);
                alive = false;
            }
            if (!alive) {
                it.remove();
            }
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tickAll());
        // Мир закрылся (в одиночной игре JVM живёт дальше) — забываем всё, что осталось от прошлого мира.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE.clear();
            StructureDragonItem.clearCooldowns();
        });
    }
}
