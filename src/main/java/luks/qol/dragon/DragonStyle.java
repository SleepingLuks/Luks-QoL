package luks.qol.dragon;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Внешний вид дракона — только цвета и «искры»; движение и анимация у всех драконов одинаковые.
 * primary/secondary — две краски силуэта, number/word — число и слово в надписи (число ярче, чтобы читалось),
 * spark — искры у морды, trail — след за хвостом, burst — разлёт частиц при исчезновении.
 */
public record DragonStyle(ParticleOptions primary, ParticleOptions secondary, ParticleOptions number,
                          ParticleOptions word, ParticleOptions spark, ParticleOptions trail, ParticleOptions burst) {

    /** Дракон крепости: красные и оранжевые частицы. */
    public static final DragonStyle FORTRESS = new DragonStyle(
            new DustColorTransitionOptions(0xFF2A0C, 0xB3140A, 1.2F),
            new DustColorTransitionOptions(0xFF9A22, 0xFF5C12, 1.1F),
            new DustColorTransitionOptions(0xFFB02E, 0xFF6A14, 2.0F),
            new DustColorTransitionOptions(0xFF5A1A, 0xD8200C, 1.3F),
            ParticleTypes.SMALL_FLAME,
            ParticleTypes.FLAME,
            ParticleTypes.FLAME);

    /** Дракон бастиона: тёмные, золотые и фиолетовые частицы; надпись золотая — на тёмном фоне Незера читается лучше всего. */
    public static final DragonStyle BASTION = new DragonStyle(
            new DustColorTransitionOptions(0x2B0F3D, 0x7A35C9, 1.25F),
            new DustColorTransitionOptions(0xFFCC33, 0xE69B1C, 1.1F),
            new DustColorTransitionOptions(0xFFD84D, 0xFFB722, 2.0F),
            new DustColorTransitionOptions(0xC79BFF, 0x9A5CF0, 1.35F),
            ParticleTypes.WITCH,
            ParticleTypes.PORTAL,
            ParticleTypes.WITCH);
}
