package com.ruslan.growsseth.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class DisableStructuresEvents {
    private DisableStructuresEvents() {}

    public static Event<StructureGenerate> STRUCTURE_GENERATE = EventFactory.createArrayBacked(StructureGenerate.class,
            (listeners) -> (level, structure, structureManager, registryAccess, randomState, structureTemplateManager, seed, chunkAccess, chunkPos, sectionPos) -> {
                for (StructureGenerate event : listeners) {
                    boolean result = event.shouldCancel(level, structure, structureManager, registryAccess, randomState, structureTemplateManager, seed, chunkAccess, chunkPos, sectionPos);

                    if (result) {
                        return true;
                    }
                }

                return false;
            }
    );

    public interface StructureGenerate {
        boolean shouldCancel(ServerLevel level,
                             Holder<Structure> structure,
                             StructureManager structureManager,
                             RegistryAccess registryAccess,
                             RandomState randomState,
                             StructureTemplateManager structureTemplateManager,
                             long seed,
                             ChunkAccess chunkAccess,
                             ChunkPos chunkPos,
                             SectionPos sectionPos);
    }
}
