package luks.qol.dragon;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Одна анимация Дракона крепости. Всё считает сервер и рассылает частицы — на клиенте нет ни модели, ни кода.
 * Этапы: сборка силуэта из частиц → кружение вокруг игрока → полёт в сторону крепости → частицы складываются в
 * надпись «850 блоков» → надпись висит, пока её читают → дракон рассыпается искрами.
 * Сам дракон виден всем рядом, а цифры (расстояние) и их рассыпание — только владельцу предмета.
 */
public final class DragonFlight {
    private enum Phase { FORM, ORBIT, FLIGHT, MORPH, READ, VANISH }

    // ---- Тайминги (тики, 20 в секунду) и размеры — всё настраивается здесь ----
    /** Сборка силуэта из облака частиц. */
    private static final int FORM_TICKS = 40;
    /** Радиус кружения: дракон держится далеко от игрока, чтобы его было легко поймать взглядом. */
    private static final double ORBIT_RADIUS = 4.5D;
    /** Скорость дракона, блоков в тик (≈ 5 блоков в секунду). */
    private static final double ORBIT_SPEED = 0.25D;
    /** Минимальный угол кружения (1,5 оборота…3,5 оборота зависит от направления на цель). */
    private static final double MIN_SWEEP = 1.5D * Math.PI;
    /** Сколько блоков пролетает дракон в сторону цели (если путь не перегорожен стеной). */
    private static final double FLIGHT_MAX_BLOCKS = 32.0D;
    /** Торможение в конце полёта. */
    private static final int DECEL_TICKS = 14;
    /** Складывание частиц в надпись. */
    private static final int MORPH_TICKS = 40;
    /** Сколько надпись висит на экране (6 секунд). */
    private static final int READ_TICKS = 120;
    /** Рассыпание. */
    private static final int VANISH_TICKS = 10;
    /** Размер надписи относительно базового шрифта (дальше от игрока — крупнее). */
    private static final double TEXT_SCALE = 1.4D;
    /** Высота центра надписи над уровнем полёта. */
    private static final double TEXT_LIFT = 1.2D;

    /** Скорость полёта равна скорости на орбите, чтобы переход был плавным. */
    private static final double FLIGHT_SPEED = ORBIT_SPEED;

    private final DragonStyle style;
    private final ServerLevel level;
    private final ServerPlayer owner;
    private final boolean english;
    private final double targetX;
    private final double targetZ;

    private final List<PixelFont.Dot> text;
    private final int particleCount;
    private final int[] dragonOrder;
    private final int[] textOrder;
    private final double[][] jitter;
    private final double[][] localPose = new double[DragonModel.COUNT][3];
    private final double[][] dragonWorld = new double[DragonModel.COUNT][3];
    private final double[][] textWorld;
    private final Random random = new Random();

    private Phase phase = Phase.FORM;
    private int phaseTick = 0;
    private int age = 0;
    private final double theta0;
    private final double thetaEnd;
    private final double sweep;
    private final int orbitTicks;

    // положение и курс дракона
    private double px;
    private double py;
    private double pz;
    private double heading;

    // полёт
    private double startX;
    private double startY;
    private double startZ;
    private double dirX;
    private double dirZ;
    private double headingAtOrbitEnd;
    private double headingFlight;
    private double length;
    private int flightTicks;
    private double endX;
    private double endY;
    private double endZ;

    public DragonFlight(DragonStyle style, ServerLevel level, ServerPlayer owner, double targetX, double targetZ, int distance) {
        this.style = style;
        this.level = level;
        this.owner = owner;
        this.targetX = targetX;
        this.targetZ = targetZ;
        // Слово пишем по-английски только тем, у кого язык клиента английский; по умолчанию — по-русски.
        String language = owner.clientInformation().language();
        this.english = language != null && language.toLowerCase().startsWith("en");

        double phi = Math.atan2(targetZ - owner.getZ(), targetX - owner.getX());
        // Орбита заканчивается в точке, где касательная смотрит на цель, — дракон улетает без резкого поворота.
        this.thetaEnd = phi - Math.PI / 2.0D;
        // Начинается она там, куда игрок смотрит, чтобы сборку дракона было видно сразу.
        double yaw = Math.toRadians(owner.getYRot());
        this.theta0 = Math.atan2(Math.cos(yaw), -Math.sin(yaw));
        double base = (this.thetaEnd - this.theta0) % (2.0D * Math.PI);
        if (base < 0.0D) {
            base += 2.0D * Math.PI;
        }
        if (base < MIN_SWEEP) {
            base += 2.0D * Math.PI;
        }
        this.sweep = base;
        this.orbitTicks = (int) Math.ceil(this.sweep * ORBIT_RADIUS / ORBIT_SPEED);

        this.text = PixelFont.layout(distance, this.english, TEXT_SCALE);
        this.textWorld = new double[this.text.size()][3];
        this.particleCount = Math.max(DragonModel.COUNT, this.text.size());

        // Соответствие «точка дракона → точка надписи»: от хвоста к голове ↔ слева направо, чтобы получался плавный поток.
        Integer[] dragonIdx = new Integer[DragonModel.COUNT];
        for (int i = 0; i < dragonIdx.length; i++) {
            dragonIdx[i] = i;
        }
        Arrays.sort(dragonIdx, Comparator.<Integer>comparingDouble(i -> DragonModel.BASE[i][0]));
        this.dragonOrder = new int[dragonIdx.length];
        for (int i = 0; i < dragonIdx.length; i++) {
            this.dragonOrder[i] = dragonIdx[i];
        }
        Integer[] textIdx = new Integer[this.text.size()];
        for (int i = 0; i < textIdx.length; i++) {
            textIdx[i] = i;
        }
        Arrays.sort(textIdx, Comparator.<Integer>comparingDouble(i -> this.text.get(i).u())
                .thenComparingDouble(i -> -this.text.get(i).v()));
        this.textOrder = new int[textIdx.length];
        for (int i = 0; i < textIdx.length; i++) {
            this.textOrder[i] = textIdx[i];
        }

        // Случайные смещения для «облака» при сборке и для завихрений при складывании в цифры.
        this.jitter = new double[this.particleCount][3];
        for (double[] vector : this.jitter) {
            double x;
            double y;
            double z;
            do {
                x = this.random.nextDouble() * 2.0D - 1.0D;
                y = this.random.nextDouble() * 2.0D - 1.0D;
                z = this.random.nextDouble() * 2.0D - 1.0D;
            } while (x * x + y * y + z * z > 1.0D);
            vector[0] = x;
            vector[1] = y;
            vector[2] = z;
        }
    }

    public ServerPlayer owner() {
        return this.owner;
    }

    /** Один тик анимации. Возвращает false, когда анимация закончена (или прервана: игрок ушёл/умер/сменил мир). */
    public boolean tick() {
        if (this.owner.isRemoved() || !this.owner.isAlive() || this.owner.level() != this.level) {
            return false;
        }
        this.age++;
        switch (this.phase) {
            case FORM -> tickForm();
            case ORBIT -> tickOrbit();
            case FLIGHT -> tickFlight();
            case MORPH -> tickMorph();
            case READ -> tickRead();
            case VANISH -> {
                return tickVanish();
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ этапы

    private void tickForm() {
        if (this.phaseTick == 0) {
            this.level.playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
        placeOnOrbit(this.theta0);
        if (this.age % 2 == 0) {
            emitDragon(smooth(this.phaseTick / (double) FORM_TICKS));
        }
        if (++this.phaseTick >= FORM_TICKS) {
            nextPhase(Phase.ORBIT);
        }
    }

    private void tickOrbit() {
        placeOnOrbit(this.theta0 + this.sweep * (this.phaseTick / (double) this.orbitTicks));
        if (this.age % 2 == 0) {
            emitDragon(1.0D);
        }
        flapSound();
        if (++this.phaseTick >= this.orbitTicks) {
            beginFlight();
            nextPhase(Phase.FLIGHT);
        }
    }

    private void tickFlight() {
        double travelled = this.length * distanceAt(this.phaseTick) / distanceAt(this.flightTicks);
        this.px = this.startX + this.dirX * travelled;
        this.pz = this.startZ + this.dirZ * travelled;
        this.py = this.startY + 0.1D * Math.sin(this.age * 0.3D);
        this.heading = lerpAngle(this.headingAtOrbitEnd, this.headingFlight, Math.min(1.0D, this.phaseTick / 8.0D));
        if (this.age % 2 == 0) {
            emitDragon(1.0D);
        }
        flapSound();
        if (++this.phaseTick >= this.flightTicks) {
            nextPhase(Phase.MORPH);
        }
    }

    private void tickMorph() {
        hoverAtEnd();
        double u = this.phaseTick / (double) MORPH_TICKS;
        if (this.age % 2 == 0) {
            double eased = smooth(u);
            double swirl = Math.sin(Math.PI * u) * 0.9D;
            boolean toEveryone = u < 0.5D; // вторая половина превращения — уже цифры, их видит только владелец
            DragonModel.pose(this.age, this.localPose);
            for (int i = 0; i < DragonModel.COUNT; i++) {
                toWorld(this.localPose[i], this.dragonWorld[i]);
            }
            updateTextWorld();
            int dragonCount = DragonModel.COUNT;
            int textCount = this.text.size();
            for (int i = 0; i < this.particleCount; i++) {
                int di = this.dragonOrder[(int) ((long) i * dragonCount / this.particleCount)];
                int ti = this.textOrder[(int) ((long) i * textCount / this.particleCount)];
                double[] from = this.dragonWorld[di];
                double[] to = this.textWorld[ti];
                double[] j = this.jitter[i];
                double x = from[0] + (to[0] - from[0]) * eased + j[0] * swirl;
                double y = from[1] + (to[1] - from[1]) * eased + j[1] * swirl;
                double z = from[2] + (to[2] - from[2]) * eased + j[2] * swirl;
                ParticleOptions options = u < 0.5D
                        ? (DragonModel.COLOR[di] == DragonModel.RED ? this.style.primary() : this.style.secondary())
                        : (this.text.get(ti).number() ? this.style.number() : this.style.word());
                if (toEveryone) {
                    sendToAll(options, x, y, z);
                } else {
                    sendToOwner(options, x, y, z);
                }
            }
        }
        if (++this.phaseTick >= MORPH_TICKS) {
            nextPhase(Phase.READ);
        }
    }

    private void tickRead() {
        if (this.phaseTick == 0) {
            this.level.playSound(this.owner, this.endX, this.endY + TEXT_LIFT, this.endZ,
                    SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.4F, 1.3F);
        }
        if (this.age % 3 == 0) {
            updateTextWorld();
            for (int i = 0; i < this.textWorld.length; i++) {
                double[] p = this.textWorld[i];
                sendToOwner(this.text.get(i).number() ? this.style.number() : this.style.word(), p[0], p[1], p[2]);
            }
            // Пара искр, чтобы надпись «тлела».
            for (int s = 0; s < 2 && this.textWorld.length > 0; s++) {
                double[] p = this.textWorld[this.random.nextInt(this.textWorld.length)];
                this.level.sendParticles(this.owner, this.style.spark(), false, true,
                        p[0], p[1], p[2], 1, 0.02D, 0.02D, 0.02D, 0.01D);
            }
        }
        if (++this.phaseTick >= READ_TICKS) {
            nextPhase(Phase.VANISH);
        }
    }

    /** Рассыпание: каждая точка надписи вспыхивает и разлетается искрами и дымом. Владельцу — цифры, всем рядом — вспышка. */
    private boolean tickVanish() {
        if (this.phaseTick == 0) {
            updateTextWorld();
            double cx = this.endX;
            double cy = this.endY + TEXT_LIFT;
            double cz = this.endZ;
            for (int i = 0; i < this.textWorld.length; i++) {
                double[] p = this.textWorld[i];
                double ox = p[0] - cx;
                double oy = p[1] - cy;
                double oz = p[2] - cz;
                double len = Math.max(0.1D, Math.sqrt(ox * ox + oy * oy + oz * oz));
                double vx = ox / len * 0.10D + (this.random.nextDouble() - 0.5D) * 0.05D;
                double vy = oy / len * 0.06D + 0.07D + this.random.nextDouble() * 0.05D;
                double vz = oz / len * 0.10D + (this.random.nextDouble() - 0.5D) * 0.05D;
                // count = 0: dx/dy/dz — это скорость частицы.
                this.level.sendParticles(this.owner, this.style.burst(), false, true, p[0], p[1], p[2], 0, vx, vy, vz, 1.0D);
                if (i % 2 == 0) {
                    this.level.sendParticles(this.owner, ParticleTypes.SMOKE, false, true, p[0], p[1], p[2], 0, vx * 0.6D, vy * 0.6D, vz * 0.6D, 1.0D);
                }
            }
            this.level.sendParticles(this.style.burst(), false, true, cx, cy, cz, 40, 1.2D, 0.7D, 1.2D, 0.04D);
            this.level.sendParticles(ParticleTypes.LARGE_SMOKE, false, true, cx, cy, cz, 14, 1.0D, 0.6D, 1.0D, 0.02D);
            this.level.playSound(null, cx, cy, cz, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F, 0.8F);
            this.level.playSound(null, cx, cy, cz, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.35F, 1.7F);
        }
        return ++this.phaseTick < VANISH_TICKS;
    }

    private void nextPhase(Phase next) {
        this.phase = next;
        this.phaseTick = 0;
    }

    // ------------------------------------------------------------------ геометрия

    private void placeOnOrbit(double theta) {
        this.px = this.owner.getX() + ORBIT_RADIUS * Math.cos(theta);
        this.pz = this.owner.getZ() + ORBIT_RADIUS * Math.sin(theta);
        this.py = this.owner.getEyeY() + 0.5D + 0.15D * Math.sin(this.age * 0.3D);
        this.heading = theta + Math.PI / 2.0D;
    }

    /** Начало полёта: направление на крепость и длина пути (укорачивается, если впереди стена — чтобы дракона было видно). */
    private void beginFlight() {
        placeOnOrbit(this.thetaEnd);
        this.startX = this.px;
        this.startY = this.py;
        this.startZ = this.pz;
        this.headingAtOrbitEnd = this.heading;

        double dx = this.targetX - this.startX;
        double dz = this.targetZ - this.startZ;
        double distance = Math.hypot(dx, dz);
        if (distance < 1.0E-3D) {
            dx = Math.cos(this.heading);
            dz = Math.sin(this.heading);
            distance = 1.0D;
        }
        this.dirX = dx / distance;
        this.dirZ = dz / distance;
        this.headingFlight = Math.atan2(dz, dx);

        double len = Math.min(FLIGHT_MAX_BLOCKS, Math.max(2.0D, distance * 0.85D));
        Vec3 from = new Vec3(this.startX, this.startY, this.startZ);
        Vec3 to = new Vec3(this.startX + this.dirX * (len + 2.5D), this.startY, this.startZ + this.dirZ * (len + 2.5D));
        BlockHitResult hit = this.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.owner));
        if (hit.getType() != HitResult.Type.MISS) {
            double hitDistance = hit.getLocation().distanceTo(from);
            len = Math.min(len, Math.max(1.0D, hitDistance - 2.5D));
        }
        this.length = len;
        this.flightTicks = Math.max(DECEL_TICKS, (int) Math.ceil(len / FLIGHT_SPEED + DECEL_TICKS / 2.0D));
        this.endX = this.startX + this.dirX * len;
        this.endY = this.startY;
        this.endZ = this.startZ + this.dirZ * len;
    }

    /** Пройденный путь по профилю скорости: ровно, а в последние DECEL_TICKS — плавное торможение до нуля. */
    private double distanceAt(int t) {
        int total = this.flightTicks;
        int decel = Math.min(DECEL_TICKS, total);
        int cruise = total - decel;
        if (t <= cruise) {
            return FLIGHT_SPEED * t;
        }
        double tau = t - cruise;
        return FLIGHT_SPEED * (cruise + tau - tau * tau / (2.0D * decel));
    }

    private void hoverAtEnd() {
        this.px = this.endX;
        this.py = this.endY + 0.1D * Math.sin(this.age * 0.3D);
        this.pz = this.endZ;
        this.heading = this.headingFlight;
    }

    /** Локальная точка дракона → мировая (x — вперёд по курсу, z — вбок). */
    private void toWorld(double[] local, double[] out) {
        double cos = Math.cos(this.heading);
        double sin = Math.sin(this.heading);
        double x = local[0] * DragonModel.SCALE;
        double y = local[1] * DragonModel.SCALE;
        double z = local[2] * DragonModel.SCALE;
        out[0] = this.px + cos * x - sin * z;
        out[1] = this.py + y;
        out[2] = this.pz + sin * x + cos * z;
    }

    /** Надпись всегда повёрнута лицом к игроку (по горизонтали), чтобы её было удобно читать. */
    private void updateTextWorld() {
        double ax = this.endX;
        double ay = this.endY + TEXT_LIFT;
        double az = this.endZ;
        double fx = ax - this.owner.getX();
        double fz = az - this.owner.getZ();
        double len = Math.hypot(fx, fz);
        if (len < 1.0E-3D) {
            fx = 0.0D;
            fz = 1.0D;
        } else {
            fx /= len;
            fz /= len;
        }
        // Правая рука смотрящего: (-fz, fx).
        double rx = -fz;
        double rz = fx;
        for (int i = 0; i < this.textWorld.length; i++) {
            PixelFont.Dot dot = this.text.get(i);
            this.textWorld[i][0] = ax + rx * dot.u();
            this.textWorld[i][1] = ay + dot.v();
            this.textWorld[i][2] = az + rz * dot.u();
        }
    }

    // ------------------------------------------------------------------ частицы и звук

    /** Рисует дракона. assemble < 1 — идёт сборка: точки ещё слетаются к силуэту из закрученного облака. */
    private void emitDragon(double assemble) {
        DragonModel.pose(this.age, this.localPose);
        double inverse = 1.0D - assemble;
        double swirlAngle = inverse * 2.2D;
        double cos = Math.cos(swirlAngle);
        double sin = Math.sin(swirlAngle);
        for (int i = 0; i < DragonModel.COUNT; i++) {
            toWorld(this.localPose[i], this.dragonWorld[i]);
            double[] w = this.dragonWorld[i];
            double x = w[0];
            double y = w[1];
            double z = w[2];
            if (inverse > 0.0D) {
                double[] j = this.jitter[i];
                double jx = (j[0] * cos - j[2] * sin) * inverse * 1.8D;
                double jz = (j[0] * sin + j[2] * cos) * inverse * 1.8D;
                x = this.px + (x - this.px) * assemble + jx;
                y = this.py + (y - this.py) * assemble + j[1] * inverse * 1.8D;
                z = this.pz + (z - this.pz) * assemble + jz;
            }
            sendToAll(DragonModel.COLOR[i] == DragonModel.RED ? this.style.primary() : this.style.secondary(), x, y, z);
        }
        // Искры у морды и огненный след за хвостом.
        double[] head = this.dragonWorld[DragonModel.HEAD_TIP];
        double[] tail = this.dragonWorld[DragonModel.TAIL_TIP];
        this.level.sendParticles(this.style.spark(), false, true, head[0], head[1], head[2], 1, 0.03D, 0.03D, 0.03D, 0.01D);
        this.level.sendParticles(this.style.trail(), false, true, tail[0], tail[1], tail[2], 1, 0.05D, 0.05D, 0.05D, 0.005D);
    }

    private void sendToAll(ParticleOptions options, double x, double y, double z) {
        this.level.sendParticles(options, false, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void sendToOwner(ParticleOptions options, double x, double y, double z) {
        this.level.sendParticles(this.owner, options, false, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void flapSound() {
        if (this.age % 12 == 0) {
            this.level.playSound(null, this.px, this.py, this.pz, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 0.35F, 1.6F);
        }
    }

    private static double smooth(double u) {
        double c = Math.max(0.0D, Math.min(1.0D, u));
        return c * c * (3.0D - 2.0D * c);
    }

    private static double lerpAngle(double from, double to, double f) {
        double delta = to - from;
        while (delta > Math.PI) {
            delta -= 2.0D * Math.PI;
        }
        while (delta < -Math.PI) {
            delta += 2.0D * Math.PI;
        }
        return from + delta * f;
    }
}
