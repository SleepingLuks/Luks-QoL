package luks.qol.mixin;

import luks.qol.dragon.NetherVisits;
import luks.qol.eye.CityVisits;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Раз в секунду проверяет, не вошёл ли игрок в город Края (Око Пустоты) или в крепость или бастион Незера (драконы). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void luksqol$trackEndCityVisits(CallbackInfo ci) {
        CityVisits.tick((ServerPlayer) (Object) this);
        NetherVisits.tick((ServerPlayer) (Object) this);
    }
}
