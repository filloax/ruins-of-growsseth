package com.ruslan.growsseth.structure

import com.filloax.fxlib.api.structure.ForcePosJigsawStructure
import com.ruslan.growsseth.GrowssethTags.StructTags
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import com.ruslan.growsseth.structure.structure.ResearcherTentStructure
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.HolderGetter
import net.minecraft.core.Vec3i
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep.Decoration
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.VerticalAnchor
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureSet
import net.minecraft.world.level.levelgen.structure.StructureType
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool

object GrowssethStructures {
    @JvmStatic
    val all = HashSet<ResourceKey<Structure>>()
    val allWithPlaceholders = HashSet<ResourceKey<Structure>>()
    val info = mutableMapOf<ResourceKey<Structure>, StructureInfo>()
    @JvmStatic
    val cydoniaToOriginal = mutableMapOf<ResourceKey<Structure>, ResourceKey<Structure>>()

    // Defined via data, here for reference
    @JvmField
    val RESEARCHER_TENT = make("researcher_tent", StructTags.RESEARCHER_TENT)
    @JvmField
    val RESEARCHER_TENT_SIMPLE = make("researcher_tent_simple", StructTags.RESEARCHER_TENT)
    @JvmField
    val BEEKEEPER_HOUSE = make("beekeeper_house", StructTags.BEEKEEPER_HOUSE)
    @JvmField
    val CAVE_CAMP = make("cave_camp", StructTags.CAVE_CAMP, emeraldCost = 2)
    @JvmField
    val MARKER = make("marker", StructTags.MARKER)
    // Not actual structure, placeholder used for mod features and legacy stuff
    // Real structures are either the variants found below meant for /place or gamemaster-mode,
    // or the templates added to village house pools
    @JvmField
    val GOLEM_HOUSE = make("golem_house", StructTags.GOLEM_HOUSE, placeholder = true)
    @JvmField
    val ENCHANT_TOWER = make("enchant_tower", StructTags.ENCHANT_TOWER, emeraldCost = 7)
    @JvmField
    val ABANDONED_FORGE = make("abandoned_forge", StructTags.ABANDONED_FORGE)
    @JvmField
    val CONDUIT_RUINS = make("conduit_ruins", StructTags.CONDUIT_RUINS)
    @JvmField
    val CONDUIT_CHURCH = make("conduit_church", StructTags.CONDUIT_CHURCH, emeraldCost = 7)
    @JvmField
    val NOTEBLOCK_LAB = make("noteblock_lab", StructTags.NOTEBLOCK_LAB)
    @JvmField
    val NOTEBLOCK_SHIP = make("noteblock_ship", StructTags.NOTEBLOCK_SHIP)

    // Structures findable by map, used for normal progress mode
    val PROGRESS_STRUCTURES = listOf(
        CAVE_CAMP,
        GOLEM_HOUSE,
        ENCHANT_TOWER,
        NOTEBLOCK_LAB,
        BEEKEEPER_HOUSE,
        CONDUIT_CHURCH,
        ABANDONED_FORGE,
    )
    // structures from the original live run, for growsseth progress mode
    val ORIGINAL_STRUCTURES = listOf(
        CAVE_CAMP,
        GOLEM_HOUSE,
        ENCHANT_TOWER,
        NOTEBLOCK_LAB,
        BEEKEEPER_HOUSE,
        CONDUIT_CHURCH,
    )

    // Structures with structure sets
    val SPAWNS_NATURALLY = listOf(
        CAVE_CAMP,
        ENCHANT_TOWER,
        NOTEBLOCK_LAB,
        BEEKEEPER_HOUSE,
        CONDUIT_CHURCH,
        RESEARCHER_TENT,
        RESEARCHER_TENT_SIMPLE,
        NOTEBLOCK_SHIP,
        ABANDONED_FORGE,
        CONDUIT_RUINS,
    )

    // Structures that represent village houses in various mod features
    // (for example, they are considered "found" if a matching village house is found),
    // to streamline handling in various parts of code
    val VILLAGE_HOUSE_STRUCTURES = mapOf(
        GOLEM_HOUSE to getHousesOfVillageCategory(VillageBuildings.CATEGORY_GOLEM_HOUSE)
    )


    init {
        // random statement to make sure the loader loads the sub-objects
        GolemStandaloneVariants.GOLEM_HOUSE_SNOWY
        CydoniaVersion.MARKER
    }

    // autogenerated this part via python lol
    object GolemStandaloneVariants {
        @JvmField
        val GOLEM_HOUSE_SNOWY = make("golem_variants/snowy_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_TAIGA = make("golem_variants/taiga_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_DESERT = make("golem_variants/desert_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_PLAINS = make("golem_variants/plains_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_SAVANNA = make("golem_variants/savanna_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_ZOMBIE_DESERT = make("golem_variants/zombie_desert_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_ZOMBIE_PLAINS = make("golem_variants/zombie_plains_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_ZOMBIE_SAVANNA = make("golem_variants/zombie_savanna_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_ZOMBIE_SNOWY = make("golem_variants/zombie_snowy_golem_house", StructTags.GOLEM_HOUSE)
        @JvmField
        val GOLEM_HOUSE_ZOMBIE_TAIGA = make("golem_variants/zombie_taiga_golem_house", StructTags.GOLEM_HOUSE)
    }

    object CydoniaVersion {
        @JvmField
        val RESEARCHER_TENT = makeCydonia("researcher_tent", GrowssethStructures.RESEARCHER_TENT)
        @JvmField
        val BEEKEEPER_HOUSE = makeCydonia("beekeeper_house", GrowssethStructures.BEEKEEPER_HOUSE)
        @JvmField
        val CAVE_CAMP = makeCydonia("cave_camp", GrowssethStructures.CAVE_CAMP)
        @JvmField
        val MARKER = makeCydonia("marker", GrowssethStructures.MARKER)
        @JvmField
        val GOLEM_HOUSE = makeCydonia("golem_house", GrowssethStructures.GOLEM_HOUSE)
        @JvmField
        val ENCHANT_TOWER = makeCydonia("enchant_tower", GrowssethStructures.ENCHANT_TOWER)
        @JvmField
        val CONDUIT_CHURCH = makeCydonia("conduit_church", GrowssethStructures.CONDUIT_CHURCH)
        @JvmField
        val NOTEBLOCK_LAB = makeCydonia("noteblock_lab", GrowssethStructures.NOTEBLOCK_LAB)
    }

    object Types {
        @JvmStatic
        val all = mutableMapOf<ResourceLocation, StructureType<*>>()

        val RESEARCHER_TENT = registerType("researcher_tent") { ResearcherTentStructure.CODEC }

        private fun <T : Structure> registerType(name: String, type: StructureType<T>): StructureType<T> {
            val id = resLoc(name)
            all[id] = type
            return type
        }
    }

    fun registerStructureTypes(registrator: (ResourceLocation, StructureType<*>) -> Unit) {
        Types.all.forEach{
            registrator(it.key, it.value)
        }
    }

    // Note: assumes that structures with a structures set have the same id
    fun getStructureSetId(structId: ResourceKey<Structure>): ResourceKey<StructureSet>? {
        return if (SPAWNS_NATURALLY.contains(structId)) {
            ResourceKey.create(Registries.STRUCTURE_SET, structId.location())
        } else {
            null
        }
    }

    private fun make(
        name: String, tag: TagKey<Structure>,
        emeraldCost: Int = 5,
        placeholder: Boolean = false
    ): ResourceKey<Structure> {
        val key = ResourceKey.create(Registries.STRUCTURE, resLoc(name))
        if (!placeholder)
            all.add(key)
        allWithPlaceholders.add(key)
        info[key] = StructureInfo(key, tag, emeraldCost)
        return key
    }

    private fun makeCydonia(name: String, base: ResourceKey<Structure>): ResourceKey<Structure> {
        val key = make("cydonia/$name", info[base]!!.tag)
        cydoniaToOriginal[key] = base
        return key
    }

    private fun getHousesOfVillageCategory(category: BuildingKey): Map<ResourceKey<Structure>, List<ResourceLocation>> {
        val entries = VillageBuildings.houseEntries[category] ?: throw IllegalArgumentException("Village category $category not found")
        return entries
            .groupBy{ it.kind }
            .mapKeys { ResourceKey.create(Registries.STRUCTURE, ResourceLocation("minecraft", "village_${it.key}")) }
            .mapValues { e -> e.value.flatMap { listOf(it.normalTemplate, it.zombieTemplate) } }
    }

    class Bootstrapper(private val ctx: BootstrapContext<Structure>) {
        private val templatePoolGetter: HolderGetter<StructureTemplatePool> = ctx.lookup(Registries.TEMPLATE_POOL)
        private val biomesGetter: HolderGetter<Biome> = ctx.lookup(Registries.BIOME)

        val NONE_BIOMES = TagKey.create(Registries.BIOME, resLoc("has_structure/none"))

        private fun registerJigsaws() {
            registerSimpleJigsaw(BEEKEEPER_HOUSE, "ruins/beekeeper_house/house",
                offset = Vec3i(-11, 0, -9),
            )
            registerSimpleJigsaw(CAVE_CAMP, "misc/cave_camp",
                startHeight = UniformHeight.of(VerticalAnchor.absolute(20), VerticalAnchor.absolute(50)),
                projectStartToHeightMap = null,
                offset = Vec3i(-10, -7, -10),
                step = Decoration.UNDERGROUND_STRUCTURES,
            )
            registerSimpleJigsaw(CONDUIT_CHURCH, "ruins/conduit_church/main",
                offset = Vec3i(-15, -2, -14),
                startHeight = ConstantHeight.of(VerticalAnchor.absolute(-2)),
                projectStartToHeightMap = Heightmap.Types.OCEAN_FLOOR,
            )
            registerSimpleJigsaw(ENCHANT_TOWER, "ruins/enchant_tower/base",
                offset = Vec3i(-5, 0, -4),
                size = 6,
            )
            registerSimpleJigsaw(MARKER, "misc/marker",
                biomesTag = NONE_BIOMES,
                size = 1,
            )
            registerSimpleJigsaw(ABANDONED_FORGE, "ruins/abandoned_forge/base",
                offset = Vec3i(-9, 0, -9),
            )
            registerSimpleJigsaw(NOTEBLOCK_LAB, "ruins/noteblock_lab/house",
                offset = Vec3i(-8, 0, -9),
            )
            registerSimpleJigsaw(NOTEBLOCK_SHIP, "ruins/noteblock_ship",
                startHeight = ConstantHeight.of(VerticalAnchor.absolute(-2)),
                offset = Vec3i(-19, -2, -15),
            )
            registerSimpleJigsaw(CONDUIT_RUINS, "ruins/conduit_ruins",
                offset = Vec3i(-16, 0, -16),
                projectStartToHeightMap = Heightmap.Types.OCEAN_FLOOR,
            )
        }

        private fun registerCydoniaVersions() {
            registerSimpleJigsaw(CydoniaVersion.BEEKEEPER_HOUSE, "cydonia/ruins/beekeeper_house/house",
                offset = Vec3i(-11, -2, -9),
                startHeight = ConstantHeight.of(VerticalAnchor.absolute(-2)),
            )
            registerSimpleJigsaw(CydoniaVersion.CAVE_CAMP, "cydonia/misc/cave_camp",
                biomesTag = BiomeTags.IS_OVERWORLD,
                projectStartToHeightMap = null,
                startHeight = UniformHeight.of(VerticalAnchor.absolute(20), VerticalAnchor.absolute(50)),
                offset = Vec3i(-10, -7, -10),
                step = Decoration.UNDERGROUND_STRUCTURES,
            )
            registerSimpleJigsaw(CydoniaVersion.CONDUIT_CHURCH, "cydonia/ruins/conduit_church/main",
                offset = Vec3i(-15, -2, -14),
                startHeight = ConstantHeight.of(VerticalAnchor.absolute(-2)),
                projectStartToHeightMap = Heightmap.Types.OCEAN_FLOOR,
            )
            registerSimpleJigsaw(CydoniaVersion.ENCHANT_TOWER, "cydonia/ruins/enchant_tower/base",
                offset = Vec3i(-5, 0, -4),
                size = 6,
            )
            registerSimpleJigsaw(CydoniaVersion.GOLEM_HOUSE, "cydonia/misc/golem_house",
                offset = Vec3i(-6, 0, -6),
                size = 6,
            )
            registerSimpleJigsaw(CydoniaVersion.MARKER, "cydonia/misc/marker",
                size = 1,
            )
            registerSimpleJigsaw(CydoniaVersion.NOTEBLOCK_LAB, "cydonia/ruins/noteblock_lab/house",
                offset = Vec3i(-8, 0, -9),
            )
        }

        private fun registerGolemVariants() {
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_TAIGA, "taiga", Vec3i(-7, 0, -8))
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_ZOMBIE_TAIGA, "taiga", Vec3i(-7, 0, -8), zombie = true)
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_DESERT, "desert", Vec3i(-6, 0, -8))
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_ZOMBIE_DESERT, "desert", Vec3i(-6, 0, -8), zombie = true)
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_PLAINS, "plains", Vec3i(-6, 0, -6))
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_ZOMBIE_PLAINS, "plains", Vec3i(-6, 0, -6), zombie = true)
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_SNOWY, "snowy", Vec3i(-6, 0, -6))
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_ZOMBIE_SNOWY, "snowy", Vec3i(-6, 0, -6), zombie = true)
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_SAVANNA, "savanna", Vec3i(-7, 0, -5))
            registerStandaloneGolemVariant(GolemStandaloneVariants.GOLEM_HOUSE_ZOMBIE_SAVANNA, "savanna", Vec3i(-7, 0, -5), zombie = true)
        }

        private fun registerTents() {
            ctx.register(RESEARCHER_TENT, ResearcherTentStructure.build(ctx))
            ctx.register(RESEARCHER_TENT_SIMPLE, ResearcherTentStructure.build(ctx, ResearcherTent.SIMPLE_ID, 0))
            ctx.register(CydoniaVersion.RESEARCHER_TENT, ResearcherTentStructure.build(ctx, ResearcherTent.CYDONIA_ID))
        }

        private fun registerSimpleJigsaw(
            key: ResourceKey<Structure>, startPool: String,
            biomesTag: TagKey<Biome>? = null,
            offset: Vec3i = Vec3i.ZERO,
            projectStartToHeightMap: Heightmap.Types? = Heightmap.Types.WORLD_SURFACE_WG,
            step: Decoration = Decoration.SURFACE_STRUCTURES,
            startHeight: HeightProvider = ConstantHeight.ZERO,
            size: Int = 7,
        ) {
            val startPoolHolder = templatePoolGetter.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, resLoc(startPool)))
            val name = key.location().path.split("/").last()
            val biomesHolder = biomesGetter.getOrThrow(biomesTag ?: TagKey.create(Registries.BIOME, resLoc("has_structure/$name")))
            ctx.register(key, ForcePosJigsawStructure.build(
                startPoolHolder, biomesHolder,
                forcePosOffset = offset, projectStartToHeightmap = projectStartToHeightMap,
                step = step, startHeight = startHeight,
                size = size,
            ))
        }

        // Standalone golem house variants, to be used with /place or gamemaster mode
        private fun registerStandaloneGolemVariant(key: ResourceKey<Structure>, name: String, offset: Vec3i, zombie: Boolean = false) {
            val villageBuildingEntry = VillageBuildings.houseEntries[VillageBuildings.CATEGORY_GOLEM_HOUSE]!!.find { it.kind == name }!!
            registerSimpleJigsaw(key, (if (zombie) villageBuildingEntry.zombiePool else villageBuildingEntry.normalPool).path,
                size = 6,
                offset = offset,
                biomesTag = NONE_BIOMES,
            )
        }

        // Used in data generation
        fun bootstrap() {
            registerJigsaws()
            registerCydoniaVersions()
            registerTents()
            registerGolemVariants()
        }
    }
    fun bootstrap(ctx: BootstrapContext<Structure>) = Bootstrapper(ctx).bootstrap()

    data class StructureInfo(
        val key: ResourceKey<Structure>,
        val tag: TagKey<Structure>,
        val emeraldCost: Int,
    )
}