package com.lights.yesmenn.block;

import com.lights.yesmenn.compat.veil.ColoredLightCompatRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Fluorescent lamp hanging on two chains. Works like the {@link FluorescentLampBlock} (start blinking, glowing tubes,
 * a signal into the block above it or beside that switches it), but always hangs, shining down. {@link #AXIS} is the
 * long axis: across the player's view, along the view while sneaking.
 * <p>
 * The housing sits at the top of the block, its top plate reaching 2.25 px into the block above, so a block placed on
 * it (e.g. a Create redstone link) sinks a little into its top. The model is three blocks long; its two chains rise
 * from the ends into the blocks above the two neighbours. Placing the lamp puts {@link HangingLampChainBlock chain
 * parts} there (where free), which can be extended with ordinary chains; breaking a chain part breaks the lamp and the
 * other way round. It needs nothing to hold it and never drops on its own, so chains can be added afterwards.
 */
public class HangingFluorescentLampBlock extends FluorescentLampBlock {
    public static final MapCodec<HangingFluorescentLampBlock> CODEC = simpleCodec(HangingFluorescentLampBlock::new);
    /**
     * Housing (top plate up to y 18.25, into the block above, where a redstone link placed on the lamp sinks into it)
     * and the tubes with their glow below it, as in the model.
     */
    private static final VoxelShape SHAPE_Z = Shapes.or(
            Block.box(0.0, 15.25, 0.0, 16.0, 18.25, 16.0),
            Block.box(5.5, 10.25, 0.0, 10.5, 15.25, 16.0));
    private static final VoxelShape SHAPE_X = Shapes.or(
            Block.box(0.0, 15.25, 0.0, 16.0, 18.25, 16.0),
            Block.box(0.0, 10.25, 5.5, 16.0, 15.25, 10.5));

    public HangingFluorescentLampBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState()
                .setValue(FACING, Direction.DOWN)
                .setValue(BEAM_FACING, Direction.DOWN)
                .setValue(AXIS, placementAxis(Direction.DOWN, context));
        state = state.setValue(POWERED, isReceivingSignal(level, pos, state));
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        BlockState chain = ColoredLightCompatRegistry.FLUORESCENT_LAMP_HANGING_CHAIN.get().defaultBlockState();
        for (BlockPos chainPos : chainPositions(pos, state.getValue(AXIS))) {
            if (level.getBlockState(chainPos).canBeReplaced()) {
                level.setBlock(chainPos, chain, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this)) {
            for (BlockPos chainPos : chainPositions(pos, state.getValue(AXIS))) {
                // A chain part between two lamps in a row belongs to both; it stays while the other one hangs on it.
                if (level.getBlockState(chainPos).getBlock() instanceof HangingLampChainBlock
                        && HangingLampChainBlock.owners(level, chainPos).isEmpty()) {
                    level.removeBlock(chainPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Stays where it is placed, also without anything holding it: chains can be added afterwards. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    /** Where the two chains of a lamp at {@code pos} rise: above its neighbours along the long axis. */
    public static BlockPos[] chainPositions(BlockPos pos, Direction.Axis axis) {
        BlockPos above = pos.above();
        return new BlockPos[]{
                above.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)),
                above.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE))};
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction.Axis axis = state.getValue(AXIS);
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            axis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }
        return state.setValue(AXIS, axis);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
