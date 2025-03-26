package com.ruslan.growsseth.utils;

import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.structure.GrowssethStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class MixinHelpers {
    private MixinHelpers() {}

    public static boolean placingBlockEntityInStructure = false;
    public static boolean savingPersistentEntities = false;

    @Nullable public static Structure researcherTent;

    public static void serverInit(MinecraftServer server) {
        Registry<Structure> registry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        researcherTent = registry.getOrThrow(GrowssethStructures.RESEARCHER_TENT);
    }

    public static final ResourceLoading RESOURCE_LOADING = new ResourceLoading();

    public static class ResourceLoading {
        @Nullable private static ResourceLocation currentlyDecoding;
        private static final Stack<ResourceLocation> parentDecoding = new Stack<>(); // just in case


        public void startResource(ResourceLocation id) {
            if (currentlyDecoding != null) parentDecoding.add(currentlyDecoding);
            currentlyDecoding = id;
        }

        public void endResource() {
            currentlyDecoding = parentDecoding.empty() ? null : parentDecoding.pop();
        }

        public @Nullable ResourceLocation getCurrent() { return currentlyDecoding; }
    }

    // Big thanks to Naz Ikhsan from https://bugs.mojang.com/browse/MC-102223 for the painting fix
    public static void fixPaintingPlacement(Entity entity) {
        if (entity instanceof Painting painting) {
            var pos = new BlockPos.MutableBlockPos();
            pos.set(painting.getPos());
            var variant = painting.getVariant().value();

            var width = variant.width() / 16;
            var height = variant.height() / 16;
            var direction = painting.getDirection();

            // paintings with an even height seem to always be moved upwards...
            if (height % 2 == 0) {
                pos.move(0, -1, 0);
            }

            // paintings with an even width seem to be moved in the clockwise direction of their facing direction,
            // if they're west or south.
            if (width % 2 == 0 && (direction == Direction.WEST || direction == Direction.SOUTH)) {
                var moveTo = direction.getClockWise().getNormal();
                pos.move(moveTo);
            }

            painting.setPos(pos.getCenter());
        }
        RuinsOfGrowsseth.LOGGER.info("Fixed painting placement in newly created structure");
    }
}
