package luks.qol.mixin.client;

import luks.qol.client.GhastPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Рисует окошко слота ускорения справа от обычного инвентаря (только на гасте). */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void luksqol$drawGhastSlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                       CallbackInfo ci) {
        GhastPanel.draw(graphics, this.leftPos, this.topPos, 0, partialTick);
    }
}
