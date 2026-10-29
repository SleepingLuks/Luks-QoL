package luks.qol.mixin.client;

import luks.qol.client.GhastPanel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Окошко слота гаста находится вне основной панели инвентаря, и игра считала клик по нему кликом
 * «за окном» — предмет из слота выбрасывался. Здесь клик по окошку перестаёт считаться внешним.
 * Окно инвентаря выживания идёт через AbstractRecipeBookScreen (у него свой hasClickedOutside), поэтому цели две.
 */
@Mixin({AbstractContainerScreen.class, AbstractRecipeBookScreen.class})
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void luksqol$insideGhastPanel(double mouseX, double mouseY, int left, int top,
                                          CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen && GhastPanel.contains(mouseX, mouseY, left, top, 0)) {
            cir.setReturnValue(false);
        }
    }
}
