package luks.qol.mixin;

import luks.qol.LuksItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Шанс 10% получить ледяное семя при переплавке снега.
 * Рецепт печи (snow -> ice_seeds) в данных обычный и детерминированный;
 * здесь в 90% случаев результат просто не выдаётся, а снег всё равно расходуется.
 * Какой из двух ItemStack-аргументов результат, определяем по предмету,
 * а расходуем всегда входной слот печи (индекс 0).
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceSeedChanceMixin {
    private static final double SEED_CHANCE = 0.10;

    @Inject(method = "burn", at = @At("HEAD"), cancellable = true)
    private static void luksqol$seedChance(NonNullList<ItemStack> items, ItemStack a, ItemStack b, CallbackInfo ci) {
        if (!a.is(LuksItems.ICE_SEEDS) && !b.is(LuksItems.ICE_SEEDS)) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= SEED_CHANCE) {
            items.get(0).shrink(1);
            ci.cancel();
        }
    }
}
