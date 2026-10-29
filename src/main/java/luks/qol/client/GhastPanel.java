package luks.qol.client;

import luks.qol.LuksItems;
import luks.qol.ghast.GhastBoost;
import luks.qol.ghast.GhastBoostData;
import luks.qol.ghast.GhastBoostHolder;
import luks.qol.ghast.GhastBoostSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;

/**
 * Окошко слота ускорения гаста: рисуется справа от окна инвентаря (в стиле ванильных панелей,
 * с холодной голубой рамкой вокруг слота). Общий код для обычного и творческого инвентаря.
 */
public final class GhastPanel {
    private GhastPanel() {}

    /** Сдвиг вправо в творческом инвентаре (его окно шире на 19 пикселей). */
    public static final int CREATIVE_DX = 19;

    private static final int W = 38;
    private static final int H = 46;
    private static final ItemStack HINT = new ItemStack(LuksItems.SPEED_SNOWBALL);

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int SHADOW = 0xFF555555;
    private static final int FROST = 0xFFB9D3EA;
    private static final int FROST_DARK = 0xFF86A9CE;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT = 0xFF8B8B8B;

    public static HappyGhast ghast() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getVehicle() instanceof HappyGhast g ? g : null;
    }

    private static int x0(int left, int dx) { return left + GhastBoostSlot.SLOT_X + dx - 10; }
    private static int y0(int top) { return top + GhastBoostSlot.SLOT_Y - 10; }

    /** Клик внутри окошка не должен считаться кликом «за пределами окна» (иначе предметы выбрасываются). */
    public static boolean contains(double mouseX, double mouseY, int left, int top, int dx) {
        if (ghast() == null) {
            return false;
        }
        int x = x0(left, dx);
        int y = y0(top);
        return mouseX >= x && mouseX < x + W && mouseY >= y && mouseY < y + H;
    }

    public static void draw(GuiGraphicsExtractor g, int left, int top, int dx, float partialTick) {
        HappyGhast ghast = ghast();
        if (ghast == null) {
            return;
        }
        int x0 = x0(left, dx);
        int y0 = y0(top);
        int x1 = x0 + W;
        int y1 = y0 + H;

        // Чёрный контур со срезанными углами, как у ванильных окон.
        g.fill(x0 + 1, y0, x1 - 1, y1, BLACK);
        g.fill(x0, y0 + 1, x1, y1 - 1, BLACK);
        // Фаски: светлая сверху-слева, тёмная снизу-справа.
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, PANEL);
        g.fill(x0 + 1, y0 + 1, x1 - 2, y0 + 3, WHITE);
        g.fill(x0 + 1, y0 + 1, x0 + 3, y1 - 2, WHITE);
        g.fill(x0 + 2, y1 - 3, x1 - 1, y1 - 1, SHADOW);
        g.fill(x1 - 3, y0 + 2, x1 - 1, y1 - 1, SHADOW);
        g.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, PANEL);

        // Холодная рамка вокруг слота.
        int fx = x0 + 5;
        int fy = y0 + 5;
        g.fill(fx, fy, fx + 26, fy + 26, FROST_DARK);
        g.fill(fx + 1, fy + 1, fx + 25, fy + 25, FROST);

        // Сам слот.
        int sx = left + GhastBoostSlot.SLOT_X + dx;
        int sy = top + GhastBoostSlot.SLOT_Y;
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_DARK);
        g.fill(sx, sy, sx + 17, sy + 17, WHITE);
        g.fill(sx, sy, sx + 16, sy + 16, SLOT);

        // Подсказка: бледный Снежок ускорения в пустом слоте.
        if (ghast instanceof GhastBoostHolder holder && holder.luksqol$getBoostSlot().getItem(0).isEmpty()) {
            g.item(HINT, sx, sy);
            g.fill(sx, sy, sx + 16, sy + 16, 0xB08B8B8B);
        }

        drawTimer(g, ghast, x0 + 6, y0 + 35, partialTick);
    }

    /** Шкала оставшегося действия снежка: убывает плавно, по заливке бежит мягкий блик. */
    private static void drawTimer(GuiGraphicsExtractor g, HappyGhast ghast, int x, int y, float partialTick) {
        final int w = 26;
        g.fill(x, y, x + w, y + 6, SLOT_DARK);
        g.fill(x + 1, y + 1, x + w, y + 6, WHITE);
        g.fill(x + 1, y + 1, x + w - 1, y + 5, 0xFF2A3440);

        long end = ghast.getEntityData().get(GhastBoostData.END_TIME);
        if (end <= 0L) {
            return;
        }
        float now = ghast.level().getGameTime() + partialTick;
        float fraction = Math.max(0F, Math.min(1F, (end - now) / GhastBoost.DURATION_TICKS));
        int fill = Math.round((w - 2) * fraction);
        if (fill <= 0) {
            return;
        }
        g.fillGradient(x + 1, y + 1, x + 1 + fill, y + 5, 0xFFC9F1FF, 0xFF5FB8EA);
        // Бегущий блик.
        int shine = (int) ((now * 0.8F) % (w + 6)) - 4;
        int sx0 = Math.max(x + 1, x + 1 + shine);
        int sx1 = Math.min(x + 1 + fill, x + 1 + shine + 4);
        if (sx1 > sx0) {
            g.fillGradient(sx0, y + 1, sx1, y + 5, 0x80FFFFFF, 0x30FFFFFF);
        }
    }
}
