package luks.qol.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Invoker("addSlot")
    Slot luksqol$addSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean luksqol$moveItemStackTo(ItemStack stack, int start, int end, boolean reverse);

    @Invoker("addDataSlots")
    void luksqol$addDataSlots(ContainerData data);
}
