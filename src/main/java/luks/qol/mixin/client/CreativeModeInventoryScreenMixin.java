package luks.qol.mixin.client;

import luks.qol.client.GhastPanel;
import luks.qol.ghast.GhastBoostSlot;
import luks.qol.mixin.SlotAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Слот гаста в творческом инвентаре (вкладка «Инвентарь выживания»).
 * Творческий экран показывает слоты настоящего инвентаря игрока через обёртки; клики идут
 * в настоящее меню, поэтому достаточно поставить обёртку нашего слота на нужное место.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    @Shadow
    private static CreativeModeTab selectedTab;

    protected CreativeModeInventoryScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void luksqol$placeGhastSlot(CreativeModeTab tab, CallbackInfo ci) {
        if (tab.getType() != CreativeModeTab.Type.INVENTORY) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int last = player.inventoryMenu.slots.size() - 1;
        if (!(player.inventoryMenu.slots.get(last) instanceof GhastBoostSlot) || this.menu.slots.size() <= last) {
            return;
        }
        // Обёртки добавляются в том же порядке, что и слоты инвентаря игрока: нужная — на том же номере.
        Slot wrapper = this.menu.slots.get(last);
        ((SlotAccessor) wrapper).luksqol$setX(GhastBoostSlot.SLOT_X + GhastPanel.CREATIVE_DX);
        ((SlotAccessor) wrapper).luksqol$setY(GhastBoostSlot.SLOT_Y);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void luksqol$drawGhastSlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                       CallbackInfo ci) {
        if (selectedTab != null && selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            GhastPanel.draw(graphics, this.leftPos, this.topPos, GhastPanel.CREATIVE_DX, partialTick);
        }
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void luksqol$insideGhastPanel(double mouseX, double mouseY, int left, int top,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab != null && selectedTab.getType() == CreativeModeTab.Type.INVENTORY
                && GhastPanel.contains(mouseX, mouseY, left, top, GhastPanel.CREATIVE_DX)) {
            cir.setReturnValue(false);
        }
    }
}
