package com.ruslan.growsseth.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface StructureManagerExtension {
    StructureStart getStructureAtExpanded(BlockPos pos, Structure structure, int expandBy);
}
