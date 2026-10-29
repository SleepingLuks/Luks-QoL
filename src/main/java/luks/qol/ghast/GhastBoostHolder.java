package luks.qol.ghast;

import net.minecraft.world.SimpleContainer;

/** Реализуется счастливым гастом (через миксин): отдаёт слот для Снежков ускорения. */
public interface GhastBoostHolder {
    SimpleContainer luksqol$getBoostSlot();
}
