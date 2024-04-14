package com.ruslan.growsseth.utils;

import com.ruslan.growsseth.structure.GrowssethStructures;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class MixinHelpers {
    private MixinHelpers() {}

    public static boolean placingBlockEntityInStructure = false;

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
}
