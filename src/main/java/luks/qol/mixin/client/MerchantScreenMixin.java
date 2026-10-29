package luks.qol.mixin.client;

import luks.qol.trade.TradeMenuData;
import luks.qol.trade.VillagerTradeData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Кнопки «Обновить предложения» и «Сбросить профессию» под окном торговли.
 * Клиент только показывает состояние (оно приходит в слотах данных меню) и отправляет нажатие;
 * разрешает действие и считает время сервер.
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Unique
    private static final int LUKSQOL$ROW_HEIGHT = 20;
    @Unique
    private static final int LUKSQOL$GAP = 4;

    @Unique
    private Button luksqol$refreshButton;
    @Unique
    private Button luksqol$resetButton;
    @Unique
    private Button luksqol$cancelButton;

    protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void luksqol$addTradeButtons(CallbackInfo ci) {
        int rowsHeight = LUKSQOL$ROW_HEIGHT * 2 + LUKSQOL$GAP;
        int x = this.leftPos;
        int y = this.topPos + this.imageHeight + LUKSQOL$GAP;
        if (y + rowsHeight > this.height) {
            // На крупном масштабе интерфейса под окном нет места — ставим кнопки над ним.
            y = Math.max(2, this.topPos - rowsHeight - LUKSQOL$GAP);
        }
        int half = (this.imageWidth - LUKSQOL$GAP) / 2;

        this.luksqol$refreshButton = this.addRenderableWidget(
                Button.builder(Component.translatable("luks_qol.trade.refresh"),
                                b -> this.luksqol$click(VillagerTradeData.BUTTON_REFRESH))
                        .bounds(x, y, half, LUKSQOL$ROW_HEIGHT).build());
        this.luksqol$resetButton = this.addRenderableWidget(
                Button.builder(Component.translatable("luks_qol.trade.reset"),
                                b -> this.luksqol$click(VillagerTradeData.BUTTON_RESET))
                        .bounds(x + half + LUKSQOL$GAP, y, this.imageWidth - half - LUKSQOL$GAP, LUKSQOL$ROW_HEIGHT)
                        .build());
        this.luksqol$cancelButton = this.addRenderableWidget(
                Button.builder(Component.translatable("luks_qol.trade.cancel_reset"),
                                b -> this.luksqol$click(VillagerTradeData.BUTTON_CANCEL_RESET))
                        .bounds(x, y + LUKSQOL$ROW_HEIGHT + LUKSQOL$GAP, this.imageWidth, LUKSQOL$ROW_HEIGHT)
                        .build());
        this.luksqol$cancelButton.visible = false;
    }

    /** Вызывается каждый кадр: подстраиваем кнопки под данные, пришедшие от сервера. */
    @Inject(method = "extractContents", at = @At("HEAD"))
    private void luksqol$updateTradeButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                            CallbackInfo ci) {
        if (this.luksqol$refreshButton == null) {
            return;
        }
        ContainerData data = ((TradeMenuData) (Object) this.menu).luksqol$tradeData();
        int flags = data == null ? 0 : data.get(VillagerTradeData.FLAGS);
        boolean supported = (flags & VillagerTradeData.FLAG_SUPPORTED) != 0;
        boolean traded = (flags & VillagerTradeData.FLAG_TRADED) != 0;

        if (!supported) {
            // Странствующий торговец и прочие торговцы — без кнопок.
            this.luksqol$refreshButton.visible = false;
            this.luksqol$resetButton.visible = false;
            this.luksqol$cancelButton.visible = false;
            return;
        }

        int refreshSeconds = data.get(VillagerTradeData.REFRESH_SECONDS);
        int resetSeconds = data.get(VillagerTradeData.RESET_SECONDS);
        boolean refreshing = refreshSeconds > 0;
        boolean resetting = resetSeconds > 0;
        int half = (this.imageWidth - LUKSQOL$GAP) / 2;

        this.luksqol$refreshButton.visible = !resetting;
        this.luksqol$resetButton.visible = true;
        this.luksqol$cancelButton.visible = resetting && !traded;

        if (traded) {
            Component locked = Component.translatable("luks_qol.trade.locked");
            this.luksqol$refreshButton.visible = true;
            this.luksqol$refreshButton.setMessage(Component.translatable("luks_qol.trade.refresh"));
            this.luksqol$refreshButton.active = false;
            this.luksqol$refreshButton.setTooltip(Tooltip.create(locked));
            this.luksqol$resetButton.setMessage(Component.translatable("luks_qol.trade.reset"));
            this.luksqol$resetButton.active = false;
            this.luksqol$resetButton.setTooltip(Tooltip.create(locked));
            this.luksqol$setResetWidth(half, false);
            return;
        }

        this.luksqol$refreshButton.setTooltip(null);
        this.luksqol$resetButton.setTooltip(null);

        if (resetting) {
            // Во время сброса вместо двух кнопок — строка с обратным отсчётом на всю ширину и кнопка отмены.
            this.luksqol$setResetWidth(half, true);
            this.luksqol$resetButton.active = false;
            this.luksqol$resetButton.setMessage(this.luksqol$resetText(resetSeconds));
            return;
        }

        this.luksqol$setResetWidth(half, false);
        this.luksqol$resetButton.setMessage(Component.translatable("luks_qol.trade.reset"));
        this.luksqol$resetButton.active = true;
        if (refreshing) {
            this.luksqol$refreshButton.active = false;
            this.luksqol$refreshButton.setMessage(Component.translatable("luks_qol.trade.refreshing", refreshSeconds));
        } else {
            this.luksqol$refreshButton.active = true;
            this.luksqol$refreshButton.setMessage(Component.translatable("luks_qol.trade.refresh"));
        }
    }

    /** Кнопка сброса занимает правую половину; во время отсчёта — всю строку. */
    @Unique
    private void luksqol$setResetWidth(int half, boolean full) {
        if (full) {
            this.luksqol$resetButton.setX(this.leftPos);
            this.luksqol$resetButton.setWidth(this.imageWidth);
        } else {
            this.luksqol$resetButton.setX(this.leftPos + half + LUKSQOL$GAP);
            this.luksqol$resetButton.setWidth(this.imageWidth - half - LUKSQOL$GAP);
        }
    }

    @Unique
    private Component luksqol$resetText(int seconds) {
        int minutes = seconds / 60;
        int rest = seconds % 60;
        if (minutes > 0) {
            return Component.translatable("luks_qol.trade.resetting", minutes, rest);
        }
        return Component.translatable("luks_qol.trade.resetting_seconds", rest);
    }

    @Unique
    private void luksqol$click(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }
}
