package luks.qol.client;

import luks.qol.ghast.GhastBoostMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Окно слота ускорения гаста. Фон рисуется прямоугольниками (отдельная текстура не нужна). */
public class GhastBoostScreen extends AbstractContainerScreen<GhastBoostMenu> {
    public GhastBoostScreen(GhastBoostMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFFC6C6C6);
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            graphics.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }
    }
}
