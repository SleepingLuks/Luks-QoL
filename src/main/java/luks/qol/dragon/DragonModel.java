package luks.qol.dragon;

import java.util.ArrayList;
import java.util.List;

/**
 * «Модель» дракона — облако точек (без настоящей 3D-модели): тело, шея, голова с рогами, хвост с шипом и два крыла.
 * Локальные оси: x — вперёд, y — вверх, z — вбок. Крылья машут волной (кончики запаздывают), хвост извивается.
 * Размер задаётся SCALE: дракон небольшой, но достаточно крупный, чтобы заметить его с орбиты (около 3,5 блока в длину).
 */
final class DragonModel {
    private DragonModel() {}

    static final double SCALE = 1.1D;
    /** Период взмаха крыльев, тиков. */
    private static final double FLAP_PERIOD = 12.0D;

    static final int BODY = 0;
    static final int HEAD = 1;
    static final int TAIL = 2;
    static final int WING = 3;

    static final int RED = 0;
    static final int ORANGE = 1;

    private static final double WING_ROOT_Y = 0.14D;
    private static final double WING_ROOT_Z = 0.12D;

    static final double[][] BASE;
    static final int[] PART;
    static final int[] COLOR;
    /** Для хвоста: 0 у основания … 1 на конце (чем дальше, тем сильнее извивается). */
    private static final double[] PROGRESS;
    static final int COUNT;
    /** Точки, около которых рождаются искры: кончик морды и шип хвоста. */
    static final int HEAD_TIP;
    static final int TAIL_TIP;

    static {
        List<double[]> points = new ArrayList<>();
        List<int[]> meta = new ArrayList<>();
        List<Double> progress = new ArrayList<>();

        // Тело: шесть сечений веретена, в каждом верхняя точка и два бока (бока «горят» оранжевым).
        for (int i = 0; i < 6; i++) {
            double x = -0.5D + i * 0.2D;
            double r = 0.17D * Math.sqrt(Math.max(0.15D, 1.0D - Math.pow((x + 0.05D) / 0.62D, 2)));
            add(points, meta, progress, x, r, 0, BODY, RED, 0);
            add(points, meta, progress, x, -r * 0.35D, -r * 0.95D, BODY, ORANGE, 0);
            add(points, meta, progress, x, -r * 0.35D, r * 0.95D, BODY, ORANGE, 0);
        }
        // Шея.
        add(points, meta, progress, 0.62D, 0.10D, 0, BODY, RED, 0);
        add(points, meta, progress, 0.78D, 0.20D, 0, BODY, RED, 0);
        add(points, meta, progress, 0.92D, 0.30D, 0, BODY, RED, 0);
        // Голова: затылок, макушка, морда и пара рогов.
        add(points, meta, progress, 1.05D, 0.36D, 0, HEAD, ORANGE, 0);
        add(points, meta, progress, 1.17D, 0.36D, 0, HEAD, ORANGE, 0);
        HEAD_TIP = points.size();
        add(points, meta, progress, 1.30D, 0.32D, 0, HEAD, ORANGE, 0);
        for (int side = -1; side <= 1; side += 2) {
            add(points, meta, progress, 1.00D, 0.48D, side * 0.08D, HEAD, ORANGE, 0);
            add(points, meta, progress, 0.90D, 0.58D, side * 0.10D, HEAD, RED, 0);
        }
        // Хвост и шип на конце.
        for (int i = 0; i < 8; i++) {
            add(points, meta, progress, -0.70D - i * 0.14D, 0, 0, TAIL, RED, i / 8.0D);
        }
        TAIL_TIP = points.size();
        add(points, meta, progress, -1.80D, 0, 0, TAIL, ORANGE, 1.0D);
        add(points, meta, progress, -1.90D, 0, -0.10D, TAIL, ORANGE, 1.0D);
        add(points, meta, progress, -1.90D, 0, 0.10D, TAIL, ORANGE, 1.0D);

        // Крылья: кость от плеча к запястью, четыре «пальца» и перепонка между ними.
        double[][] tips = {{0.75D, 1.35D}, {0.10D, 1.55D}, {-0.50D, 1.25D}, {-0.80D, 0.65D}};
        double[] wrist = {0.15D, 0.70D};
        for (int side = -1; side <= 1; side += 2) {
            for (int s = 1; s <= 3; s++) {
                wingPoint(points, meta, progress, side, wrist[0] * s / 3.0D, wrist[1] * s / 3.0D, ORANGE);
            }
            for (double[] tip : tips) {
                for (double f : new double[]{0.4D, 0.7D, 1.0D}) {
                    wingPoint(points, meta, progress, side,
                            wrist[0] + (tip[0] - wrist[0]) * f, wrist[1] + (tip[1] - wrist[1]) * f, ORANGE);
                }
            }
            for (int t = 0; t < tips.length - 1; t++) {
                wingPoint(points, meta, progress, side,
                        (tips[t][0] + tips[t + 1][0]) / 2.0D, (tips[t][1] + tips[t + 1][1]) / 2.0D, ORANGE);
                for (double f : new double[]{0.6D, 0.85D}) {
                    double ax = wrist[0] + (tips[t][0] - wrist[0]) * f;
                    double az = wrist[1] + (tips[t][1] - wrist[1]) * f;
                    double bx = wrist[0] + (tips[t + 1][0] - wrist[0]) * f;
                    double bz = wrist[1] + (tips[t + 1][1] - wrist[1]) * f;
                    wingPoint(points, meta, progress, side, (ax + bx) / 2.0D, (az + bz) / 2.0D, RED);
                }
            }
            wingPoint(points, meta, progress, side, -0.50D, 0.30D, RED);
        }

        COUNT = points.size();
        BASE = points.toArray(new double[0][]);
        PART = new int[COUNT];
        COLOR = new int[COUNT];
        PROGRESS = new double[COUNT];
        for (int i = 0; i < COUNT; i++) {
            PART[i] = meta.get(i)[0];
            COLOR[i] = meta.get(i)[1];
            PROGRESS[i] = progress.get(i);
        }
    }

    private static void add(List<double[]> points, List<int[]> meta, List<Double> progress,
                            double x, double y, double z, int part, int color, double prog) {
        points.add(new double[]{x, y, z});
        meta.add(new int[]{part, color});
        progress.add(prog);
    }

    /** Точка крыла: x вперёд, zOut — вылет от плеча наружу; side = -1 (левое) или +1 (правое). */
    private static void wingPoint(List<double[]> points, List<int[]> meta, List<Double> progress,
                                  int side, double x, double zOut, int color) {
        add(points, meta, progress, x, WING_ROOT_Y, side * (WING_ROOT_Z + zOut), WING, color, 0);
    }

    /** Позы всех точек в локальных осях для данного тика (взмах крыльев, покачивание, извивающийся хвост). */
    static void pose(int tick, double[][] out) {
        double phase = tick * 2.0D * Math.PI / FLAP_PERIOD;
        double bob = 0.05D * Math.sin(phase + 1.0D);
        for (int i = 0; i < COUNT; i++) {
            double x = BASE[i][0];
            double y = BASE[i][1] + bob;
            double z = BASE[i][2];
            if (PART[i] == TAIL) {
                double p = PROGRESS[i];
                y += 0.14D * p * Math.sin(tick * 0.35D - 3.0D * p);
                z += 0.07D * p * Math.sin(tick * 0.25D - 2.0D * p + 1.0D);
            } else if (PART[i] == WING) {
                double sign = z >= 0 ? 1.0D : -1.0D;
                double out0 = Math.abs(z) - WING_ROOT_Z;
                double angle = 0.22D + 0.62D * Math.sin(phase - 1.1D * out0);
                y = WING_ROOT_Y + bob + out0 * Math.sin(angle);
                z = sign * (WING_ROOT_Z + out0 * Math.cos(angle));
            }
            out[i][0] = x;
            out[i][1] = y;
            out[i][2] = z;
        }
    }
}
