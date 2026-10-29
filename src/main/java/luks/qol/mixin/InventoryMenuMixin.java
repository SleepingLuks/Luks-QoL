package luks.qol.mixin;

import luks.qol.LuksItems;
import luks.qol.ghast.GhastBoostSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Добавляет в обычный инвентарь игрока слот для гаста. Он последний по номеру,
 * поэтому номера ванильных слотов не меняются. Позиция — справа от окна инвентаря.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void luksqol$addGhastSlot(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        ((AbstractContainerMenuAccessor) this).luksqol$addSlot(new GhastBoostSlot(owner, GhastBoostSlot.SLOT_X, GhastBoostSlot.SLOT_Y));
    }

    /** Shift-клик: снежок ускорения из инвентаря уходит в слот гаста, а из слота гаста — обратно в инвентарь. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void luksqol$quickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        int last = menu.slots.size() - 1;
        if (index < 0 || index > last || !(menu.slots.get(last) instanceof GhastBoostSlot ghastSlot) || !ghastSlot.isActive()) {
            return;
        }
        Slot slot = menu.slots.get(index);
        ItemStack stack = slot.getItem();
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;

        if (index == last) {
            if (stack.isEmpty()) {
                return;
            }
            ItemStack copy = stack.copy();
            if (!accessor.luksqol$moveItemStackTo(stack, InventoryMenu.INV_SLOT_START, InventoryMenu.USE_ROW_SLOT_END, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            finish(slot, stack);
            cir.setReturnValue(copy);
        } else if (stack.is(LuksItems.SPEED_SNOWBALL)) {
            ItemStack copy = stack.copy();
            if (!accessor.luksqol$moveItemStackTo(stack, last, last + 1, false)) {
                return; // слот занят другим стеком — пусть сработает обычное поведение
            }
            finish(slot, stack);
            cir.setReturnValue(copy);
        }
    }

    private static void finish(Slot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
    }
}
