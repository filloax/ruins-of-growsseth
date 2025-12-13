package com.ruslan.growsseth.mixin;

import com.ruslan.growsseth.interfaces.StructureManagerExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StructureManager.class)
public abstract class StructureManagerMixin implements StructureManagerExtension {
    /** Works exactly like getStructureAt, but inflates the bounding box of the structure before the check */
    public StructureStart getStructureAtExpanded(BlockPos pos, Structure structure, int expandBy) {
        StructureManager th1s = (StructureManager)(Object)this;
        for (StructureStart structurestart : th1s.startsForStructure(SectionPos.of(pos), structure)) {
            if (structurestart.getBoundingBox().inflatedBy(expandBy).isInside(pos)) {
                return structurestart;
            }
        }
        return StructureStart.INVALID_START;
    }
}
