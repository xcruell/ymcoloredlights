package com.yesmenn.coloredlights.mixin.fabric;

import com.yesmenn.coloredlights.light.ColoredLightSpawnProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric stand-in for NeoForge's {@code MobSpawnEvent.SpawnPlacementCheck}, which fires from the same method. */
@Mixin(SpawnPlacements.class)
public abstract class MixinSpawnPlacementsFabric {
    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void ymcoloredlights$coloredLightSpawnProtection(EntityType<?> type, ServerLevelAccessor level,
                                                                  MobSpawnType spawnType, BlockPos pos,
                                                                  RandomSource random,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        if (ColoredLightSpawnProtection.blocksSpawn(type, level.getLevel(), pos)) {
            cir.setReturnValue(false);
        }
    }
}
