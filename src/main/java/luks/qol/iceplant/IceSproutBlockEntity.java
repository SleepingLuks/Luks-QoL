package luks.qol.iceplant;

import luks.qol.LuksBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Хранит игровое время следующего события растения (рост / таяние).
 * Запланированные тики отменить нельзя, поэтому устаревший тик
 * (например, «таяние» после сбора) просто игнорируется: событие
 * происходит только когда игровое время дошло до этого дедлайна.
 */
public class IceSproutBlockEntity extends BlockEntity {
    /** 0 = ещё не запущено. */
    long deadline = 0L;

    public IceSproutBlockEntity(BlockPos pos, BlockState state) {
        super(LuksBlocks.ICE_SPROUT_BE, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.deadline = input.getLongOr("deadline", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("deadline", this.deadline);
    }
}
