package com.ruslan.growsseth.mixin.event;

import com.filloax.fxlib.api.FxUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.structure.StructureDisabler;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

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

            if (StructureDisabler.Mixins.shouldDisableStructure(structureSetEntry.structure(), level)) {
                if (FxUtils.isDevEnvironment()) {
                    RuinsOfGrowsseth.getLOGGER().info("Disabled spawn for structure " + structureSetEntry.structure());
                }
                cir.setReturnValue(false);
            }
        }
    }

    @Mixin(ChunkGeneratorStructureState.class)
    public static class ChunkGeneratorStructureStateMixin {
        @Shadow @Final private List<Holder<StructureSet>> possibleStructureSets;

        @ModifyVariable(
            method = "<init>(Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeSource;JJLjava/util/List;)V",
            at = @At("HEAD"),
            argsOnly = true
        )
        private static List<Holder<StructureSet>> modifyPossibleStructureSets(List<Holder<StructureSet>> possibleStructureSets, @Local(argsOnly = true) BiomeSource biomeSource) {
            return StructureDisabler.Mixins.filterStructureSets(possibleStructureSets, biomeSource);
        }
    }
}