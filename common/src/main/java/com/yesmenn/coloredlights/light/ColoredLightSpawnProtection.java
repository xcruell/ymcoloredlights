package com.yesmenn.coloredlights.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Colored lights keep monsters from spawning like real light. NeoForge asks this from its spawn placement event,
 * Fabric from a mixin into {@code SpawnPlacements.checkSpawnRules}.
 */
public final class ColoredLightSpawnProtection {
    private ColoredLightSpawnProtection() {
    }

    public static boolean blocksSpawn(EntityType<?> type, ServerLevel level, BlockPos pos) {
        return type.getCategory() == MobCategory.MONSTER && FakeColoredLightManager.preventsMonsterSpawn(level, pos);
    }
}
