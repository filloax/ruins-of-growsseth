package com.ruslan.growsseth.mixin.event;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.util.Pair;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.events.DisableStructuresEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

public class StructureSpawnMixins {
    @Mixin(ChunkGenerator.class)
    public static class ChunkGeneratorMixin {
        @Inject(method = "tryGenerateStructure", at = @At(value = "HEAD"), cancellable = true)
        void disableStructures(
                StructureSet.StructureSelectionEntry structureSetEntry,
                StructureManager structureManager,
                RegistryAccess registryAccess,
                RandomState randomState,
                StructureTemplateManager structureTemplateManager,
                long seed,
                ChunkAccess chunkAccess,
                ChunkPos chunkPos,
                SectionPos sectionPos,
                CallbackInfoReturnable<Boolean> cir
        ) {
            ServerLevel level = ((ServerLevelAccessor) structureManager).getLevel();

            if (DisableStructuresEvents.STRUCTURE_GENERATE.invoker().shouldCancel(level, structureSetEntry.structure(), structureManager, registryAccess, randomState, structureTemplateManager, seed, chunkAccess, chunkPos, sectionPos)) {
                RuinsOfGrowsseth.getLOGGER().info("Disabled spawn for structure " + structureSetEntry.structure());
                cir.setReturnValue(false);
            }
        }
    }
}