package luks.qol.ghast;

import luks.qol.LuksQoL;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;

import java.util.List;

/** Ускорение счастливого гаста от Снежка ускорения. Всё считает сервер. */
public final class GhastBoost {
    private GhastBoost() {}

    /** Длительность ускорения от одного снежка: 25 секунд. */
    public static final int DURATION_TICKS = 25 * 20;
    /** Величина ускорения: +35% к базовой скорости (в ТЗ не определено — легко менять). */
    public static final double BONUS = 0.35;

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(LuksQoL.MOD_ID, "ghast_speed_boost");
    // Какой именно атрибут использует управляемый гаст — ставим на оба.
    private static final List<Holder<Attribute>> ATTRIBUTES = List.of(Attributes.FLYING_SPEED, Attributes.MOVEMENT_SPEED);

    public static void apply(HappyGhast ghast) {
        for (Holder<Attribute> attribute : ATTRIBUTES) {
            AttributeInstance instance = ghast.getAttribute(attribute);
            if (instance != null && !instance.hasModifier(MODIFIER_ID)) {
                instance.addTransientModifier(new AttributeModifier(MODIFIER_ID, BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    public static void remove(HappyGhast ghast) {
        for (Holder<Attribute> attribute : ATTRIBUTES) {
            AttributeInstance instance = ghast.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(MODIFIER_ID);
            }
        }
    }
}
