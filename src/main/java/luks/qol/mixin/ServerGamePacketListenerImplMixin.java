package luks.qol.mixin;

import luks.qol.LuksItems;
import luks.qol.ghast.GhastBoostSlot;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Слот гаста в творческом инвентаре.
 * В творческом режиме клиент не шлёт обычный пакет клика по инвентарю, а присылает серверу пакет
 * «положи предмет в слот №N». Ванильный сервер принимает только слоты 1..45, а наш слот гаста —
 * последний (№46), поэтому сервер молча его игнорировал: на клиенте снежок «лежал» в слоте,
 * а на сервере слот оставался пустым — ускорение не работало, а после пересинхронизации снежок пропадал.
 * Здесь этот пакет для слота гаста обрабатывается отдельно (только творческий режим, только снежок ускорения).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void luksqol$creativeGhastSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).getPlayer();
        AbstractContainerMenu menu = player.inventoryMenu;
        int last = menu.slots.size() - 1;
        if (packet.slotNum() != last || !(menu.slots.get(last) instanceof GhastBoostSlot)) {
            return; // обычный слот — дальше работает ванильный обработчик
        }
        ci.cancel();

        // Пакет приходит из сетевого потока, а менять слоты можно только в основном потоке сервера.
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ItemStack stack = packet.itemStack().copy();
        server.execute(() -> {
            Slot slot = menu.slots.get(last);
            boolean valid = player.isCreative()
                    && slot.isActive() // игрок сейчас на счастливом гасте
                    && (stack.isEmpty() || (stack.is(LuksItems.SPEED_SNOWBALL) && stack.getCount() <= stack.getMaxStackSize()));
            if (valid) {
                slot.setByPlayer(stack);
                menu.broadcastChanges();
            } else {
                menu.sendAllDataToRemote(); // вернуть клиенту настоящее содержимое слота
            }
        });
    }
}
