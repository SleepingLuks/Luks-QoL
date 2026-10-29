package luks.qol.iceplant;

import luks.qol.LuksBlocks;
import luks.qol.LuksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Росток на Ледяной грядке.
 * age 0..2 — растёт, age 3 — созрел (можно собрать).
 * second = false — первый цикл, true — повторный рост.
 * Всё считает сервер; клиент только рисует состояние блока.
 */
public class IceSproutBlock extends Block implements EntityBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    public static final BooleanProperty SECOND = BooleanProperty.create("second");
    public static final int RIPE = 3;

    private static final VoxelShape[] SHAPES = {
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.25, 0.875),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.5, 0.875),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.75, 0.875),
            Shapes.box(0.125, 0.0, 0.125, 0.875, 0.9375, 0.875)
    };

    public IceSproutBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(SECOND, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SECOND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(LuksBlocks.ICE_BED);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IceSproutBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // Блок-сущность на этот момент ещё может не существовать — запускаем таймер следующим тиком.
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            serverLevel.scheduleTick(pos, this, 1);
        }
    }

    /** Ставит дедлайн следующего события и планирует тик на это время. */
    private void arm(ServerLevel level, BlockPos pos, BlockState state, IceSproutBlockEntity be) {
        int delay;
        if (state.getValue(AGE) >= RIPE) {
            delay = IcePlantTimings.MELT_DELAY;
        } else if (state.getValue(SECOND)) {
            delay = IcePlantTimings.REGROW_STEP;
        } else {
            delay = IcePlantTimings.FIRST_GROW_STEP;
        }
        be.deadline = level.getGameTime() + delay;
        be.setChanged();
        level.scheduleTick(pos, this, delay);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof IceSproutBlockEntity be)) {
            return;
        }
        if (be.deadline == 0L) {
            arm(level, pos, state, be);
            return;
        }
        if (level.getGameTime() < be.deadline) {
            return; // устаревший тик (после сбора) — игнорируем
        }

        int age = state.getValue(AGE);
        boolean second = state.getValue(SECOND);

        if (age < RIPE) {
            BlockState next = state.setValue(AGE, age + 1);
            level.setBlock(pos, next, 2);
            arm(level, pos, next, be);
        } else if (!second) {
            // Первая порция растаяла: без выдачи, растение продолжает цикл (повторный рост).
            snowflakes(level, pos);
            BlockState next = state.setValue(AGE, 0).setValue(SECOND, true);
            level.setBlock(pos, next, 2);
            arm(level, pos, next, be);
        } else {
            // Вторая порция растаяла: растение исчезает.
            snowflakes(level, pos);
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (state.getValue(AGE) < RIPE) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        // Состояние меняется сразу же на сервере, повторный клик увидит уже новое состояние (нет дублей).
        popResource(serverLevel, pos, new ItemStack(LuksItems.SPEED_SNOWBALL, IcePlantTimings.SNOWBALLS_PER_HARVEST));

        if (!state.getValue(SECOND)) {
            BlockState next = state.setValue(AGE, 0).setValue(SECOND, true);
            serverLevel.setBlock(pos, next, 2);
            if (serverLevel.getBlockEntity(pos) instanceof IceSproutBlockEntity be) {
                arm(serverLevel, pos, next, be);
            }
        } else {
            snowflakes(serverLevel, pos);
            serverLevel.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }

    /** Ручная поломка: эффект снежинок, дропа нет (noLootTable). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            snowflakes(serverLevel, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(LuksItems.ICE_SEEDS);
    }

    private static void snowflakes(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.02);
    }
}
