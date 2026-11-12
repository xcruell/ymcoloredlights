package com.yesmenn.coloredlights.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fluorescent tube lamp. Works like the hanging lamp (wide cone, warm-up, warm-up color), but mounts on any block
 * face: {@link #FACING} is the side it was placed on, i.e. the direction it shines; the supporting block lies
 * opposite. {@link #AXIS} is the long axis of the housing, always perpendicular to FACING: on floors and ceilings
 * across the player's view, on walls horizontal; placing while sneaking turns it 90° (along the view / vertical). The model is two blocks long and
 * reaches half a block into both neighbours; the hit box stays inside its own block.
 */
public class FluorescentLampBlock extends HangingLampBlock {
    public static final MapCodec<FluorescentLampBlock> CODEC = simpleCodec(FluorescentLampBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /** Housing depth from the mounting face, and half width, in model pixels. */
    private static final double DEPTH = 4.6;
    private static final double HALF_WIDTH = 2.0;
    /** How often the lamp checks the supports beside its own for a redstone signal. */
    private static final int SIGNAL_CHECK_TICKS = 2;
    private static final VoxelShape[][] SHAPES = new VoxelShape[6][3];

    static {
        for (Direction facing : Direction.values()) {
            for (Direction.Axis axis : Direction.Axis.values()) {
                if (axis != facing.getAxis()) {
                    SHAPES[facing.ordinal()][axis.ordinal()] = shape(facing, axis);
                }
            }
        }
    }

    public FluorescentLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.DOWN)
                .setValue(BEAM_FACING, Direction.DOWN)
                .setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** Drawn by the block entity renderer (any orientation, blinking start); the model only supplies particles. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(BEAM_FACING, facing)
                .setValue(AXIS, placementAxis(facing, context));
        state = state.setValue(POWERED, isReceivingSignal(level, pos, state));
        return state.canSurvive(level, pos) ? state : null;
    }

    protected static Direction.Axis placementAxis(Direction facing, BlockPlaceContext context) {
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isShiftKeyDown();
        if (facing.getAxis() == Direction.Axis.Y) {
            // Floor and ceiling: across the view ("-"); sneaking places it along the view ("I").
            Direction.Axis view = context.getHorizontalDirection().getAxis();
            if (sneaking) {
                return view;
            }
            return view == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }
        // Walls: horizontal ("-"); sneaking places it vertically ("I").
        if (!sneaking) {
            return facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }
        return Direction.Axis.Y;
    }

    /**
     * The housing covers its supporting block and half of the two supports beside it, so a signal into any of these
     * three blocks switches the lamp, not only one into the lamp's own block.
     */
    @Override
    protected boolean isReceivingSignal(Level level, BlockPos pos, BlockState state) {
        if (level.hasNeighborSignal(pos)) {
            return true;
        }
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        Direction.Axis axis = state.getValue(AXIS);
        for (Direction.AxisDirection side : Direction.AxisDirection.values()) {
            BlockPos besideSupport = support.relative(Direction.fromAxisAndDirection(axis, side));
            if (level.hasNeighborSignal(besideSupport) || level.getSignal(besideSupport, facing) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * The supports beside the main one only touch the lamp at an edge and send it no block updates, so the lamp
     * looks at them itself every few ticks.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        scheduleSignalCheck(level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        updatePoweredState(level, pos, state);
        scheduleSignalCheck(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        // Lamps placed before the check existed get their schedule here.
        scheduleSignalCheck(level, pos);
    }

    private void scheduleSignalCheck(Level level, BlockPos pos) {
        if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, SIGNAL_CHECK_TICKS);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing, SupportType.CENTER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = rotation.rotate(state.getValue(FACING));
        Direction.Axis axis = state.getValue(AXIS);
        if (axis != Direction.Axis.Y
                && (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)) {
            axis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }
        return state.setValue(FACING, facing).setValue(BEAM_FACING, facing).setValue(AXIS, axis);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = mirror.mirror(state.getValue(FACING));
        return state.setValue(FACING, facing).setValue(BEAM_FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPES[state.getValue(FACING).ordinal()][state.getValue(AXIS).ordinal()];
        return shape != null ? shape : SHAPES[Direction.DOWN.ordinal()][Direction.Axis.Z.ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static VoxelShape shape(Direction facing, Direction.Axis longAxis) {
        double[] min = new double[3];
        double[] max = new double[3];
        for (Direction.Axis axis : Direction.Axis.values()) {
            int i = axis.ordinal();
            if (axis == facing.getAxis()) {
                // The housing sits against the supporting block, opposite the facing.
                boolean positive = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                min[i] = positive ? 0.0 : 16.0 - DEPTH;
                max[i] = positive ? DEPTH : 16.0;
            } else if (axis == longAxis) {
                min[i] = 0.0;
                max[i] = 16.0;
            } else {
                min[i] = 8.0 - HALF_WIDTH;
                max[i] = 8.0 + HALF_WIDTH;
            }
        }
        return Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }
}
