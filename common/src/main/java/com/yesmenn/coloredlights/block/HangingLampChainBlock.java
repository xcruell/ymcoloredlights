package com.yesmenn.coloredlights.block;

import com.yesmenn.coloredlights.compat.veil.ColoredLightCompatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain of a {@link HangingFluorescentLampBlock}, placed with the lamp above its two neighbours. Looks and hits like a
 * chain, so ordinary chains can be stacked on it; it has no item and drops nothing. Breaking it breaks the lamp(s)
 * hanging on it.
 */
public class HangingLampChainBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5);

    public HangingLampChainBlock(Properties properties) {
        super(properties);
    }

    /** Hanging lamps whose chain rises at {@code chainPos}. */
    public static List<BlockPos> owners(LevelReader level, BlockPos chainPos) {
        List<BlockPos> owners = new ArrayList<>();
        BlockPos below = chainPos.below();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos lampPos = below.relative(side);
            BlockState lamp = level.getBlockState(lampPos);
            if (lamp.getBlock() instanceof HangingFluorescentLampBlock
                    && lamp.getValue(FluorescentLampBlock.AXIS) == side.getAxis()) {
                owners.add(lampPos);
            }
        }
        return owners;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !newState.is(this)) {
            for (BlockPos lampPos : owners(level, pos)) {
                level.destroyBlock(lampPos, true);
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ColoredLightCompatRegistry.CHAIN_ITEM.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
