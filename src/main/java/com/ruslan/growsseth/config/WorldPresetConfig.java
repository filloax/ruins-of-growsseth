package com.ruslan.growsseth.config;

import com.ruslan.growsseth.structure.GrowssethStructures;
import com.teamresourceful.resourcefulconfig.api.annotations.*;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;
import com.teamresourceful.resourcefulconfig.api.types.options.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Map;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("worldpreset")
public class WorldPresetConfig {
    public static final Map<ResourceKey<Structure>, PosPreset> PRESET_ORIGINAL = Map.of(
            GrowssethStructures.CydoniaVersion.RESEARCHER_TENT, new PosPreset(1374, 74, 162, Rotation.CLOCKWISE_180),
            GrowssethStructures.CydoniaVersion.CAVE_CAMP,       new PosPreset(934, 38, 340, Rotation.NONE),
            GrowssethStructures.CydoniaVersion.MARKER,          new PosPreset(940, 68, 347, Rotation.NONE),
            GrowssethStructures.CydoniaVersion.ENCHANT_TOWER,   new PosPreset(-1276, 123, 743, Rotation.CLOCKWISE_180),
            GrowssethStructures.CydoniaVersion.GOLEM_HOUSE,     new PosPreset(171, 144, 861, Rotation.COUNTERCLOCKWISE_90),
            GrowssethStructures.CydoniaVersion.BEEKEEPER_HOUSE, new PosPreset(3086, 72, 989, Rotation.COUNTERCLOCKWISE_90),
            GrowssethStructures.CydoniaVersion.NOTEBLOCK_LAB,   new PosPreset(2260, 69, -1674, Rotation.NONE),
            GrowssethStructures.CydoniaVersion.CONDUIT_CHURCH,  new PosPreset(-1553, 39, 2527, Rotation.CLOCKWISE_180)
    );
    public static final Map<ResourceKey<Structure>, PosPreset> PRESET_UPDATED = Map.of(
        GrowssethStructures.CydoniaVersion.RESEARCHER_TENT, new PosPreset(1374, 74, 162, Rotation.CLOCKWISE_180),
        GrowssethStructures.CydoniaVersion.CAVE_CAMP,       new PosPreset(934, 38, 340, Rotation.NONE),
        GrowssethStructures.CydoniaVersion.MARKER,          new PosPreset(940, 68, 347, Rotation.NONE),
        GrowssethStructures.CydoniaVersion.ENCHANT_TOWER,   new PosPreset(-1276, 123, 743, Rotation.NONE), // tower changes
        GrowssethStructures.CydoniaVersion.GOLEM_HOUSE,     new PosPreset(171, 144, 861, Rotation.COUNTERCLOCKWISE_90),
        GrowssethStructures.CydoniaVersion.BEEKEEPER_HOUSE, new PosPreset(3086, 72, 989, Rotation.COUNTERCLOCKWISE_90),
        GrowssethStructures.CydoniaVersion.NOTEBLOCK_LAB,   new PosPreset(2260, 69, -1674, Rotation.NONE),
        GrowssethStructures.CydoniaVersion.CONDUIT_CHURCH,  new PosPreset(-1553, 39, 2527, Rotation.CLOCKWISE_180)
    );

    @ConfigButton(text = "Apply updated preset", target = "researcherTent", translation = T_PREF + "buttons.updatedPreset", position = Position.BEFORE)
    public static void useUpdatedPreset() {
        usePreset(PRESET_UPDATED);
    }

    @ConfigButton(text = "Apply original preset", target = "researcherTent", translation = T_PREF + "buttons.originalPreset", position = Position.BEFORE)
    public static void useOriginalPreset() {
        usePreset(PRESET_ORIGINAL);
    }

    private static void usePreset(Map<ResourceKey<Structure>, PosPreset> preset) {
        researcherTent.assign(new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.RESEARCHER_TENT, preset));
        caveCamp.assign(new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.CAVE_CAMP, preset));
        caveCampMarker.assign(new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.MARKER, preset));
        enchantTower.assign(new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.ENCHANT_TOWER, preset));
        golemHouse.assign(new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.GOLEM_HOUSE, preset));
        beekeeperHouse.assign(new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.BEEKEEPER_HOUSE, preset));
        noteblockLab.assign(new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.NOTEBLOCK_LAB, preset));
        conduitChurch.assign(new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.CONDUIT_CHURCH, preset));
    }

    @ConfigEntry(id = "researcherTent", type = EntryType.OBJECT, translation = T_PREF + "researcherTent.name")
    public static final StructureInWorldConfig researcherTent = new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.RESEARCHER_TENT);

    @ConfigEntry(id = "caveCamp", type = EntryType.OBJECT, translation = T_PREF + "caveCamp.name")
    public static final StructureInWorldConfig caveCamp = new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.CAVE_CAMP);

    @ConfigEntry(id = "caveCampMarker", type = EntryType.OBJECT, translation = T_PREF + "caveCampMarker.name")
    public static final StructureInWorldConfig caveCampMarker = new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.MARKER);

    @ConfigEntry(id = "enchantTower", type = EntryType.OBJECT, translation = T_PREF + "enchantTower.name")
    public static final StructureInWorldConfig enchantTower = new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.ENCHANT_TOWER);

    @ConfigEntry(id = "golemHouse", type = EntryType.OBJECT, translation = T_PREF + "golemHouse.name")
    public static final StructureInWorldConfig golemHouse = new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.GOLEM_HOUSE);

    @ConfigEntry(id = "beekeeperHouse", type = EntryType.OBJECT, translation = T_PREF + "beekeeperHouse.name")
    public static final StructureInWorldConfig beekeeperHouse = new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.BEEKEEPER_HOUSE);

    @ConfigEntry(id = "noteblockLab", type = EntryType.OBJECT, translation = T_PREF + "noteblockLab.name")
    public static final StructureInWorldConfig noteblockLab = new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.NOTEBLOCK_LAB);

    @ConfigEntry(id = "conduitChurch", type = EntryType.OBJECT, translation = T_PREF + "conduitChurch.name")
    public static final StructureInWorldConfig conduitChurch = new StructureInWorldConfig(GrowssethStructures.CydoniaVersion.CONDUIT_CHURCH);

    public static List<StructureInWorldConfig> getAll() {
        return List.of(
            researcherTent,
            caveCamp,
            caveCampMarker,
            enchantTower,
            golemHouse,
            beekeeperHouse,
            noteblockLab,
            conduitChurch
        );
    }

    @ConfigObject
    public static class StructureInWorldConfig {
        @ConfigEntry(id = "structureId", type = EntryType.STRING, translation = T_PREF + "structId.name")
        public String structureId;
        @ConfigEntry(id = "enabled", type = EntryType.BOOLEAN, translation = T_PREF + "enabled.name")
        public boolean enabled;
        @ConfigEntry(id = "x", type = EntryType.INTEGER, translation = T_PREF + "structX.name")
        public int x;
        @ConfigEntry(id = "y", type = EntryType.INTEGER, translation = T_PREF + "structY.name")
        public int y;
        @ConfigEntry(id = "z", type = EntryType.INTEGER, translation = T_PREF + "structZ.name")
        public int z;
        @ConfigEntry(id = "rotation", type = EntryType.ENUM, translation = T_PREF + "rotation.name")
        public Rotation rotation;

        public ResourceLocation structureId() {
            return new ResourceLocation(structureId);
        }
        public ResourceKey<Structure> structureKey() {
            return ResourceKey.create(Registries.STRUCTURE, structureId());
        }

        public void assign(StructureInWorldConfig other) {
            this.structureId = other.structureId;
            this.enabled = other.enabled;
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.rotation = other.rotation;
        }

        public StructureInWorldConfig(ResourceKey<Structure> structureId, boolean enabled, int x, int y, int z, Rotation rotation) {
            this.structureId = structureId.location().toString();
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotation = rotation;
        }

        public StructureInWorldConfig(ResourceKey<Structure> structureId, boolean enabled, PosPreset posRot) {
            this(structureId, enabled, posRot.x, posRot.y, posRot.z, posRot.rotation);
        }

        public StructureInWorldConfig(ResourceKey<Structure> structureId, boolean enabled, Map<ResourceKey<Structure>, PosPreset> preset) {
            this(structureId, enabled, preset.get(structureId));
        }

        public StructureInWorldConfig(ResourceKey<Structure> structureId, Map<ResourceKey<Structure>, PosPreset> preset) {
            // default to enabled
            this(structureId, true, preset.get(structureId));
        }

        public StructureInWorldConfig(ResourceKey<Structure> structureId) {
            // default to updated preset and enabled
            this(structureId, PRESET_UPDATED);
        }
    }

    public record PosPreset(int x, int y, int z, Rotation rotation) { }
}
