package com.lights.yesmenn.item;

import com.lights.yesmenn.block.ColoredLightTubeBlock;
import com.lights.yesmenn.block.entity.ColoredLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ColoredLightTubeItem extends BlockItem {
    public ColoredLightTubeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos sourcePos = placedPos.relative(clickedFace.getOpposite());
        BlockState sourceState = level.getBlockState(sourcePos);

        boolean copyConfig = sourceState.getBlock() instanceof ColoredLightTubeBlock
                && sourceState.getValue(ColoredLightTubeBlock.FACING).getAxis() == clickedFace.getAxis()
                && level.getBlockEntity(sourcePos) instanceof ColoredLightBlockEntity;
        InteractionResult result = super.place(context);
        if (copyConfig
                && result.consumesAction()
                && !level.isClientSide
                && level.getBlockEntity(sourcePos) instanceof ColoredLightBlockEntity source
                && level.getBlockEntity(placedPos) instanceof ColoredLightBlockEntity placed) {
            // Extending a row: the new tube takes over the row's settings (it becomes part of the same light).
            placed.copyLightSettingsFrom(source);
        }
        return result;
    }
}
