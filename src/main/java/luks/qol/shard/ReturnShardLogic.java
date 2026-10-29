package luks.qol.shard;

import luks.qol.LuksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Серверная логика Осколка возвращения и визуальные эффекты полёта. */
public final class ReturnShardLogic {
    private ReturnShardLogic() {}

    public static boolean isShard(ThrownEnderpearl entity) {
        return entity.getItem().is(LuksItems.RETURN_SHARD);
    }

    // ---- столкновение ----

    /** Вызывается вместо ванильной логики жемчуга. Телепорт — только при ударе о блок (поверхность). */
    public static void onHit(ThrownEnderpearl shard, HitResult hit) {
        if (!(shard.level() instanceof ServerLevel level)) {
            return;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return; // через сущности осколок пролетает (см. ProjectileMixin), сюда они не должны попадать
        }

        Vec3 at = hit.getLocation();
        flash(level, at.x, at.y, at.z);

        boolean teleported = false;
        if (shard.getOwner() instanceof ServerPlayer player && player.isAlive()
                && player.level() == level && Level.END.equals(level.dimension())) {
            teleported = teleportToPlatform(level, player);
        }
        if (!teleported) {
            refund(shard, level);
        }
        shard.discard();
    }

    // ---- спасательная платформа ----

    /** Телепортирует игрока на ванильную спавн-обсидиановую платформу Края. */
    private static boolean teleportToPlatform(ServerLevel level, ServerPlayer player) {
        BlockPos spawn = ServerLevel.END_SPAWN_POINT;
        // Ванильная платформа: обсидиан 5×5 на высоте (точка появления − 2), игрок стоит на ней.
        BlockPos floorCenter = spawn.below(2);

        if (!level.getBlockState(floorCenter).is(Blocks.OBSIDIAN)) {
            EndPlatformFeature.createEndPlatform(level, spawn.below(), true);
        }

        BlockPos feet = findStandingSpot(level, floorCenter);
        if (feet == null) {
            // Платформа застроена или разрушена — пересоздаём её так же, как это делает игра при входе в Край.
            EndPlatformFeature.createEndPlatform(level, spawn.below(), true);
            feet = floorCenter.above();
        }

        double x = feet.getX() + 0.5D;
        double y = feet.getY();
        double z = feet.getZ() + 0.5D;

        flash(level, player.getX(), player.getY() + 1.0D, player.getZ()); // вспышка там, откуда ушёл игрок
        // Поворот головы сохраняем (Relative.ROTATION с нулевым смещением).
        boolean ok = player.teleportTo(level, x, y, z, Relative.ROTATION, 0.0F, 0.0F, true);
        if (!ok) {
            return false;
        }
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        flash(level, x, y + 1.0D, z); // и там, куда пришёл
        return true;
    }

    /** Ищет на платформе свободное место: твёрдый пол и два свободных блока над ним, центр — в приоритете. */
    private static BlockPos findStandingSpot(ServerLevel level, BlockPos floorCenter) {
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floor = floorCenter.offset(dx, 0, dz);
                BlockPos feet = floor.above();
                BlockPos head = feet.above();
                boolean solidFloor = level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
                boolean freeFeet = level.getBlockState(feet).getCollisionShape(level, feet).isEmpty();
                boolean freeHead = level.getBlockState(head).getCollisionShape(level, head).isEmpty();
                int distance = dx * dx + dz * dz;
                if (solidFloor && freeFeet && freeHead && distance < bestDistance) {
                    best = feet;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    // ---- возврат предмета, если осколок не сработал ----

    /** Осколок расходуется только при успешном срабатывании: если он не сработал, предмет возвращается. */
    public static void refund(ThrownEnderpearl shard, ServerLevel level) {
        if (shard.getOwner() instanceof ServerPlayer player && player.hasInfiniteMaterials()) {
            return; // в творческом режиме при броске ничего не списывалось
        }
        ItemStack stack = shard.getItem().copyWithCount(1);
        if (shard.getOwner() instanceof ServerPlayer player && player.isAlive()) {
            player.getInventory().add(stack);
        }
        if (!stack.isEmpty()) {
            shard.spawnAtLocation(level, stack);
        }
    }

    // ---- каждый тик ----

    public static void tick(ThrownEnderpearl shard) {
        if (shard.level() instanceof ServerLevel level) {
            // Упал в бездну, так и не встретив поверхность: предмет возвращается владельцу.
            if (!shard.isRemoved() && shard.getY() < level.getMinY() - 4) {
                refund(shard, level);
                shard.discard();
            }
        } else {
            spawnTrail(shard);
        }
    }

    // ---- эффекты (только частицы, на траекторию не влияют) ----

    /** Фиолетовая «молниевидная» энергия, закручивающаяся спиралью вокруг летящего осколка. */
    private static void spawnTrail(ThrownEnderpearl shard) {
        Level level = shard.level();
        Vec3 velocity = shard.getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-4D) {
            return;
        }
        Vec3 dir = velocity.normalize();
        Vec3 side = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-4D) {
            side = dir.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 up = dir.cross(side).normalize();

        // За тик осколок пролетает несколько блоков — рисуем спираль вдоль всего пройденного отрезка.
        final int steps = 6;
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double px = shard.xo + (shard.getX() - shard.xo) * t;
            double py = shard.yo + (shard.getY() - shard.yo) * t;
            double pz = shard.zo + (shard.getZ() - shard.zo) * t;
            double angle = (shard.tickCount + t) * 1.1D;
            for (int strand = 0; strand < 2; strand++) {
                double a = angle + strand * Math.PI;
                double radius = 0.30D + level.getRandom().nextDouble() * 0.12D; // рваный, «электрический» радиус
                double ox = side.x * Math.cos(a) * radius + up.x * Math.sin(a) * radius;
                double oy = side.y * Math.cos(a) * radius + up.y * Math.sin(a) * radius;
                double oz = side.z * Math.cos(a) * radius + up.z * Math.sin(a) * radius;
                level.addParticle(ParticleTypes.WITCH, px + ox, py + oy, pz + oz, 0.0D, 0.0D, 0.0D);
                if (level.getRandom().nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, px + ox, py + oy, pz + oz, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    /** Фиолетовая вспышка: при разбивании осколка и при прибытии игрока. */
    private static void flash(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.WITCH, x, y, z, 40, 0.4D, 0.4D, 0.4D, 0.15D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 30, 0.4D, 0.4D, 0.4D, 0.2D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 20, 0.5D, 0.5D, 0.5D, 0.3D);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
