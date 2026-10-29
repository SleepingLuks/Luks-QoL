package luks.qol.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * В ванили данные блока из предмета применяются при установке только у операторов
 * (для спавнеров, командных блоков и табличек), а на предмете показывается
 * предупреждение «Внимание». Для спавнера снимаем это ограничение:
 * обычный игрок ставит добытый спавнер с тем же мобом, предупреждения нет.
 * Тип определяется по id в реестре, чтобы не зависеть от имён констант.
 * require = 0: если метод в этой версии называется иначе, игра не упадёт.
 */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {

    @Inject(method = "onlyOpCanSetNbt", at = @At("HEAD"), cancellable = true, require = 0)
    private void luksqol$allowSpawner(CallbackInfoReturnable<Boolean> cir) {
        String id = String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>) (Object) this));
        if (id.contains("spawner")) {
            cir.setReturnValue(false);
        }
    }
}
