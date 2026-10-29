package luks.qol.bush;

import luks.qol.dragon.VisitedStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Куст-указатель Незера. Наклон куста (lean 0..15, шаг 22,5°) показывает направление к ближайшему подходящему лесу;
 * lean = 16 — куст стоит прямо (цели нет или ещё не определена — ложного направления не бывает).
 * Всё считает сервер: когда рядом игрок, куст раз в несколько секунд спрашивает {@link BiomeCompass}
 * (с учётом лесов, где этот игрок уже бывал) и плавно доворачивается к цели не больше чем на один шаг за раз.
 */
public class MarkerBushBlock extends Block {
    /** 0..15 — направление наклона (0 = на юг, дальше по часовой стрелке, как угол поворота игрока), 16 — прямо. */
    public static final IntegerProperty LEAN = IntegerProperty.create("lean", 0, 16);
    public static final int UPRIGHT = 16;

    private static final VoxelShape SHAPE = Shapes.box(0.125, 0.0, 0.125, 0.875, 0.875, 0.875);

    /** Куст оживает, только когда игрок ближе этого расстояния (блоков). */
    private static final double PLAYER_RANGE = 48.0D;
    /** Как часто пересчитывается цель (тиков). */
    private static final long REFRESH_TICKS = 300L;
    /** Пауза между проверками: пока доворачивается / когда уже смотрит куда надо / когда игрока нет рядом. */
    private static final int TURN_DELAY = 30;
    private static final int IDLE_DELAY = 100;

    private final ResourceKey<Biome> targetBiome;
    private final SavedDataType<VisitedStructures> visitedType;

    public MarkerBushBlock(Properties properties, ResourceKey<Biome> targetBiome, SavedDataType<VisitedStructures> visitedType) {
        super(properties);
        this.targetBiome = targetBiome;
        this.visitedType = visitedType;
        registerDefaultState(stateDefinition.any().setValue(LEAN, UPRIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEAN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            serverLevel.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!Level.NETHER.equals(level.dimension())) {
            return; // вне Незера куст просто растение: направление там ничего не значит
        }
        ServerPlayer player = nearestPlayer(level, pos);
        if (player == null) {
            level.scheduleTick(pos, this, IDLE_DELAY + random.nextInt(60));
            return;
        }

        long now = level.getGameTime();
        long key = pos.asLong();
        Cached cached = CACHE.get(key);
        if (cached == null || now - cached.time() > REFRESH_TICKS || !cached.player().equals(player.getUUID())) {
            if (!takeSearchBudget(now)) {
                level.scheduleTick(pos, this, 6); // за этот тик уже были поиски — подождём
                return;
            }
            Set<Long> visited = VisitedStructures.get(level, this.visitedType).visitedBy(player.getUUID());
            BiomeCompass.Target target = BiomeCompass.find(level, pos, this.targetBiome, visited);
            if (CACHE.size() > 2048) {
                CACHE.clear();
            }
            cached = new Cached(now, player.getUUID(), target == null ? null : target.pos());
            CACHE.put(key, cached);
        }

        int current = state.getValue(LEAN);
        int wanted = cached.target() == null ? UPRIGHT : leanToward(pos, cached.target());
        int next = stepToward(current, wanted);
        if (next != current) {
            level.setBlock(pos, state.setValue(LEAN, next), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, next == wanted ? IDLE_DELAY : TURN_DELAY);
    }

    /** Подстраховка: если тик по какой-то причине не был запланирован, куст запланирует его случайным тиком. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    /** Индекс наклона (0..15) в сторону точки: шаг 22,5°, 0 = юг, как угол поворота игрока. */
    private static int leanToward(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        int index = (int) Math.round(yaw / 22.5D);
        return ((index % 16) + 16) % 16;
    }

    /** Один шаг по кратчайшему пути по кругу; из «прямо» сразу в нужное направление. */
    private static int stepToward(int current, int wanted) {
        if (current == wanted) {
            return current;
        }
        if (current == UPRIGHT || wanted == UPRIGHT) {
            return wanted;
        }
        int diff = ((wanted - current + 8) % 16 + 16) % 16 - 8;
        int step = diff > 0 ? 1 : -1;
        return (current + step + 16) % 16;
    }

    private static ServerPlayer nearestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer best = null;
        double bestDistance = PLAYER_RANGE * PLAYER_RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double distance = player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    // ---- общий кэш целей и «бюджет» поисков на тик (чтобы много кустов не устроили лаг одновременно) ----
    private record Cached(long time, UUID player, BlockPos target) {}

    private static final Map<Long, Cached> CACHE = new HashMap<>();
    private static long budgetTick = -1L;
    private static int budgetUsed = 0;
    private static final int SEARCHES_PER_TICK = 1;

    private static boolean takeSearchBudget(long now) {
        if (budgetTick != now) {
            budgetTick = now;
            budgetUsed = 0;
        }
        if (budgetUsed >= SEARCHES_PER_TICK) {
            return false;
        }
        budgetUsed++;
        return true;
    }

    public static void clearCaches() {
        CACHE.clear();
        budgetTick = -1L;
        budgetUsed = 0;
    }
}
