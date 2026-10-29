package luks.qol.ghast;

import luks.qol.LuksItems;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Дополнительный слот обычного инвентаря игрока (справа от окна).
 * Активен и работает только пока игрок едет/стоит на счастливом гасте;
 * хранит предметы в контейнере самого гаста, поэтому слот привязан к гасту, а не к игроку.
 */
public class GhastBoostSlot extends Slot {
    /** Позиция слота в окне инвентаря: справа от основной панели (176 пикселей шириной). */
    public static final int SLOT_X = 186;
    public static final int SLOT_Y = 24;

    private final Player player;

    public GhastBoostSlot(Player player, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.player = player;
    }

    private SimpleContainer boost() {
        if (this.player.getVehicle() instanceof HappyGhast ghast && ghast instanceof GhastBoostHolder holder) {
            return holder.luksqol$getBoostSlot();
        }
        return null;
    }

    @Override
    public boolean isActive() {
        return boost() != null;
    }

    @Override
    public ItemStack getItem() {
        SimpleContainer c = boost();
        return c == null ? ItemStack.EMPTY : c.getItem(0);
    }

    @Override
    public void set(ItemStack stack) {
        SimpleContainer c = boost();
        if (c != null) {
            c.setItem(0, stack);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        SimpleContainer c = boost();
        if (c != null) {
            c.setChanged();
        }
    }

    @Override
    public ItemStack remove(int amount) {
        SimpleContainer c = boost();
        return c == null ? ItemStack.EMPTY : c.removeItem(0, amount);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return boost() != null && stack.is(LuksItems.SPEED_SNOWBALL);
    }
}
