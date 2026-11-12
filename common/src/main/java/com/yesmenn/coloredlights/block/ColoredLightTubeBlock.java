package com.yesmenn.coloredlights.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class ColoredLightTubeBlock extends ColoredLightBlock {
    public static final MapCodec<ColoredLightTubeBlock> CODEC = simpleCodec(ColoredLightTubeBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    private static final VoxelShape[] SHAPES = buildShapes();

    public ColoredLightTubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(NORTH, false)
                .setValue(SOUTH, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        int key = facing.ordinal() << 2;
        if (state.getValue(NORTH)) {
            key |= 1;
        }
        if (state.getValue(SOUTH)) {
            key |= 2;
        }
        return SHAPES[key];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (direction == facing) {
            state = state.setValue(NORTH, connectsTo(neighborState, facing));
        } else if (direction == facing.getOpposite()) {
            state = state.setValue(SOUTH, connectsTo(neighborState, facing));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getClickedFace().getOpposite();
        return defaultBlockState()
                .setValue(POWERED, level.hasNeighborSignal(pos))
                .setValue(FACING, facing)
                .setValue(NORTH, connectsTo(level.getBlockState(pos.relative(facing)), facing))
                .setValue(SOUTH, connectsTo(level.getBlockState(pos.relative(facing.getOpposite())), facing));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, NORTH, SOUTH);
    }

    /**
     * A row of connected tubes is one light, so it switches as one: a signal at any tube powers all of them. Called
     * for the tube whose neighbour changed; also re-evaluates the rows left behind when a tube is broken (their end
     * tubes get a neighbour update).
     */
    @Override
    protected void updatePoweredState(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> run = connectedRun(level, pos);
        if (run.isEmpty()) {
            return;
        }
        boolean powered = false;
        for (BlockPos tube : run) {
            if (level.hasNeighborSignal(tube)) {
                powered = true;
                break;
            }
        }
        for (BlockPos tube : run) {
            BlockState tubeState = level.getBlockState(tube);
            if (tubeState.getValue(POWERED) != powered) {
                level.setBlock(tube, tubeState.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Longest row of connected tubes handled as one; keeps the walk along a row bounded. */
    public static final int MAX_RUN = 256;

    /**
     * All tubes connected in a straight row with the one at {@code pos} (itself included), in order along the
     * positive axis direction. Empty when there is no tube at {@code pos}.
     */
    public static List<BlockPos> connectedRun(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ColoredLightTubeBlock)) {
            return List.of();
        }
        Direction.Axis axis = state.getValue(FACING).getAxis();
        Direction negative = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        BlockPos start = pos;
        for (int i = 0; i < MAX_RUN && isTubeOnAxis(level.getBlockState(start.relative(negative)), axis); i++) {
            start = start.relative(negative);
        }
        List<BlockPos> run = new ArrayList<>();
        BlockPos current = start;
        Direction positive = negative.getOpposite();
        do {
            run.add(current);
            current = current.relative(positive);
        } while (run.size() < MAX_RUN && isTubeOnAxis(level.getBlockState(current), axis));
        return run;
    }

    public static boolean isTubeOnAxis(BlockState state, Direction.Axis axis) {
        return state.getBlock() instanceof ColoredLightTubeBlock && state.getValue(FACING).getAxis() == axis;
    }

    private boolean connectsTo(BlockState neighborState, Direction facing) {
        return neighborState.is(this)
                && neighborState.getValue(FACING).getAxis() == facing.getAxis();
    }

    private static VoxelShape tubeShape(Direction.Axis axis) {
        return switch (axis) {
            case X -> box(0, 7, 7, 16, 9, 9);
            case Y -> box(7, 0, 7, 9, 16, 9);
            case Z -> box(7, 7, 0, 9, 9, 16);
        };
    }

    private static VoxelShape capShape(Direction direction) {
        return switch (direction) {
            case DOWN -> box(6, 0, 6, 10, 2, 10);
            case UP -> box(6, 14, 6, 10, 16, 10);
            case NORTH -> box(6, 6, 0, 10, 10, 2);
            case SOUTH -> box(6, 6, 14, 10, 10, 16);
            case WEST -> box(0, 6, 6, 2, 10, 10);
            case EAST -> box(14, 6, 6, 16, 10, 10);
        };
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[Direction.values().length * 4];
        for (Direction facing : Direction.values()) {
            for (int connected = 0; connected < 4; connected++) {
                boolean northConnected = (connected & 1) != 0;
                boolean southConnected = (connected & 2) != 0;
                VoxelShape shape = tubeShape(facing.getAxis());
                if (!northConnected) {
                    shape = Shapes.or(shape, capShape(facing));
                }
                if (!southConnected) {
                    shape = Shapes.or(shape, capShape(facing.getOpposite()));
                }
                shapes[(facing.ordinal() << 2) | connected] = shape.optimize();
            }
        }
        return shapes;
    }
}
