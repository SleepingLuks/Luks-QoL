package luks.qol.ghast;

import luks.qol.LuksItems;
import luks.qol.LuksMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Мини-инвентарь гаста: один слот только для Снежков ускорения + инвентарь игрока. */
public class GhastBoostMenu extends AbstractContainerMenu {
    private final Container container;

    /** Клиентская сторона: временный контейнер, содержимое придёт с сервера. */
    public GhastBoostMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(1));
    }

    public GhastBoostMenu(int containerId, Inventory playerInventory, Container container) {
        super(LuksMenus.GHAST_BOOST, containerId);
        this.container = container;
        addSlot(new Slot(container, 0, 80, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(LuksItems.SPEED_SNOWBALL);
            }
        });
        addStandardInventorySlots(playerInventory, 8, 51);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /** Меню действует, пока игрок едет/стоит на счастливом гасте. */
    @Override
    public boolean stillValid(Player player) {
        return player.getVehicle() instanceof HappyGhast ghast && ghast.isAlive();
    }
}
