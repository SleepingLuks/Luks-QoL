package luks.qol.mixin;

import luks.qol.SpawnerSilkTouch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Спавнер, за который опыт уже выдавали (добыт шёлковым касанием раньше), при ломании опыта не даёт. */
@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin {
    @Inject(method = "spawnAfterBreak", at = @At("HEAD"), cancellable = true)
    private void luksqol$noRepeatExperience(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool,
                                            boolean dropExperience, CallbackInfo ci) {
        if (SpawnerSilkTouch.shouldSuppressXp(level, pos)) {
            ci.cancel();
        }
    }
}
