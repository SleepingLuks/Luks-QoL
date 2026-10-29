package luks.qol;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/**
 * Звуки Ледяной грядки: смесь снега, мягкой земли и тихого «ледяного» звона.
 * Сами сочетания (и вероятности) описаны в assets/luks_qol/sounds.json —
 * там каждое событие случайно выбирает один из нескольких ванильных звуков с подстроенной высотой и громкостью.
 */
public final class LuksSounds {
    private LuksSounds() {}

    public static final SoundEvent ICE_BED_BREAK = register("block.ice_bed.break");
    public static final SoundEvent ICE_BED_STEP = register("block.ice_bed.step");
    public static final SoundEvent ICE_BED_PLACE = register("block.ice_bed.place");
    public static final SoundEvent ICE_BED_HIT = register("block.ice_bed.hit");
    public static final SoundEvent ICE_BED_FALL = register("block.ice_bed.fall");

    public static final SoundType ICE_BED = new SoundType(0.9F, 1.0F,
            ICE_BED_BREAK, ICE_BED_STEP, ICE_BED_PLACE, ICE_BED_HIT, ICE_BED_FALL);

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void register() {}
}
