package com.ruslan.growsseth.structure.pieces

import com.filloax.fxlib.api.EventUtil
import com.filloax.fxlib.api.ScheduledServerTask
import com.filloax.fxlib.api.enums.SetBlockFlag
import com.filloax.fxlib.api.iterBlocks
import com.filloax.fxlib.api.nbt.*
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.FabricEvents
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.ResearcherQuestComponent
import com.ruslan.growsseth.entity.researcher.ResearcherSavedData
import com.ruslan.growsseth.structure.GrowssethStructurePieceTypes
import com.ruslan.growsseth.utils.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.util.RandomSource
import net.minecraft.world.Clearable
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager
import java.util.UUID


/**
 * Tent structure piece, with structure data detection for donkey etc
 */
class ResearcherTent : GrTemplateStructurePiece {
    var jailPos: BlockPos? = null
    var initDonkeyUuid: UUID? = null
        private set
    var researcherPos: BlockPos? = null
    var cellarTrapdoorPos: BlockPos? = null
    var cellarBoundingBox: BoundingBox? = null
        private set
    lateinit var templatePath: ResourceLocation
        private set
    // Used during initialization
    private var cellarPos1: BlockPos? = null
    private var cellarPos2: BlockPos? = null

    constructor(structureManager: StructureTemplateManager, startPosition: BlockPos, rotation: Rotation = Rotation.NONE, templatePath: ResourceLocation = DEFAULT_ID)
        : super(
            GrowssethStructurePieceTypes.RESEARCHER_TENT, GEN_DEPTH, structureManager,
            templatePath, makeSettings(rotation), startPosition,
        ) {
        this.templatePath = templatePath
    }

    //(StructurePieceType type, int genDepth, StructureTemplateManager structureManager, ResourceLocation templateLocation, StructurePlaceSettings placeSettings, BlockPos startPosition)

    constructor(compoundTag: CompoundTag, ctx: StructurePieceSerializationContext)
        : super(GrowssethStructurePieceTypes.RESEARCHER_TENT, compoundTag, ctx, makeSettings(Rotation.valueOf(compoundTag.getString("Rot")))) {

        jailPos = compoundTag.loadField("JailPos", BlockPos.CODEC)
        researcherPos = compoundTag.loadField("ResearcherPos", BlockPos.CODEC)
        cellarTrapdoorPos = compoundTag.loadField("cellarTrapdoorPos", BlockPos.CODEC)
        initDonkeyUuid = compoundTag.loadField("DonkeyUUID", UUIDUtil.STRING_CODEC)
        cellarBoundingBox = compoundTag.loadField("cellarBoundingBox", BoundingBox.CODEC)
        templatePath = compoundTag.loadField("templatePath", ResourceLocation.CODEC) ?: DEFAULT_ID
    }

    companion object {
        val DEFAULT_ID = resLoc("misc/researcher_tent")
        val SIMPLE_ID = resLoc("misc/researcher_tent_simple")
        val CYDONIA_ID = resLoc("cydonia/misc/researcher_tent")
        const val GEN_DEPTH = 0

        val DEFAULT_FENCE: Block = Blocks.SPRUCE_FENCE

        fun makeSettings(rotation: Rotation): StructurePlaceSettings
            = defaultSettings().setRotation(rotation).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)

        private val removeReplaceBlocks = mutableMapOf<Block, Block>(
            Blocks.GREEN_WOOL to Blocks.COARSE_DIRT,
        )

        fun removeTent(tent: ResearcherTent, level: ServerLevel, tag: TagKey<Block>, replaceUndergroundEntrance: Boolean = false) {
            var numRemoved = 0
            val blockCounts = tent.getBlockNum().toMutableMap()
            tent.boundingBox.iterBlocks { pos ->
                val blockState = level.getBlockState(pos)
                if (blockState.block in blockCounts && blockState.`is`(tag)) {
                    val blockEntity = level.getBlockEntity(pos)
                    if (blockEntity is RandomizableContainerBlockEntity) {
                        blockEntity.unpackLootTable(null)
                    }
                    Clearable.tryClear(blockEntity)
                    val newBlock = removeReplaceBlocks.getOrDefault(blockState.block, Blocks.AIR).defaultBlockState()
                    level.setBlock(pos, newBlock, SetBlockFlag.or(
                        SetBlockFlag.NOTIFY_CLIENTS,
                        SetBlockFlag.NO_NEIGHBOR_REACTIONS,
                        SetBlockFlag.NO_NEIGHBOR_REACTION_DROPS
                    ))
                    level.blockUpdated(pos, newBlock.block)
                    numRemoved++
                    blockCounts[blockState.block] = blockCounts[blockState.block]!! - 1
                    if (blockCounts[blockState.block]!! <= 0) {
                        blockCounts.remove(blockState.block)
                    }
                }
            }

            if (replaceUndergroundEntrance) {
                tent.cellarTrapdoorPos?.let { pos ->
                    level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), SetBlockFlag.or(
                        SetBlockFlag.NOTIFY_CLIENTS,
                        SetBlockFlag.NO_NEIGHBOR_REACTIONS,
                        SetBlockFlag.NO_NEIGHBOR_REACTION_DROPS
                    ))
                }
            }

            RuinsOfGrowsseth.LOGGER.info("Removed tent near ${tent.boundingBox.center} ($numRemoved blocks)")
        }
    }

    fun remove(level: ServerLevel, removeTag: TagKey<Block> = GrowssethTags.TENT_MATERIALS_WHITELIST, replaceUndergroundEntrance: Boolean = false) {
        removeTent(this, level, removeTag, replaceUndergroundEntrance)
    }

    fun getBlockNum(): Map<Block, Int> {
        if (template.palettes.size > 1)
            RuinsOfGrowsseth.LOGGER.warn("Tent piece has more than 1 template palette, might end up in wrong removal")

        val palette = template.palettes[0] ?: throw IllegalStateException("No palettes in structure template $template")
        val count = palette.blocks().groupingBy { it.state.block }.eachCount().toMutableMap()
        count[DEFAULT_FENCE] = count.getOrDefault(DEFAULT_FENCE, 0) + 1
        return count
    }

    override fun handleDataMarker(name: String, pos: BlockPos, level: ServerLevelAccessor, random: RandomSource, box: BoundingBox) {
        if (!boundingBox.isInside(pos)) return

        val server = level.level.server
        val researcherData = ResearcherSavedData.getPersistent(server)
        val spawnEntities = !(ResearcherConfig.singleResearcher && ResearcherQuestComponent.shouldRemoveTent(server)) && !(researcherData.isDead)

        when (name) {
            "researcher" -> {
                researcherPos = pos
                if (spawnEntities)
                    placeEntity(GrowssethEntities.RESEARCHER, pos, level) { }
                else
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), SetBlockFlag.NOTIFY_CLIENTS.flag)
            }
            "donkey" -> {
                level.setBlock(pos, DEFAULT_FENCE.defaultBlockState(), SetBlockFlag.NOTIFY_CLIENTS.flag)

                val donkeyPos = pos.relative(placeSettings().rotation.rotate(Direction.SOUTH))
                if (spawnEntities)
                    placeEntity(EntityType.DONKEY, donkeyPos, level) { donkey ->
                        manageDonkey(level.level, donkey.uuid, pos)     // the instance changes and gets reset otherwise
                    }
            }
            "jail" -> {
                jailPos = pos
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), SetBlockFlag.NOTIFY_CLIENTS.flag)
            }
            "cellarCorner1" -> {
                cellarPos1 = pos
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), SetBlockFlag.NOTIFY_CLIENTS.flag)
            }
            "cellarCorner2" -> {
                cellarPos2 = pos
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), SetBlockFlag.NOTIFY_CLIENTS.flag)
            }
        }
    }

    override fun postProcess(
        level: WorldGenLevel,
        structureManager: StructureManager,
        generator: ChunkGenerator,
        random: RandomSource,
        box: BoundingBox,
        chunkPos: ChunkPos,
        pos: BlockPos
    ) {
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos)

        // Find trapdoor above ladders position
        var ladderPos: BlockPos? = null
        boundingBox.iterBlocks { iPos ->
            val blockState = level.getBlockState(iPos)
            if (blockState.`is`(Blocks.LADDER)) {
                ladderPos = iPos
                return@iterBlocks
            }
        }

        cellarTrapdoorPos = ladderPos?.let { lPos ->
            val iPos = BlockPos.MutableBlockPos(lPos.x, lPos.y, lPos.z)
            var valid = false
            for (i in 1 .. 100) {
                if (level.getBlockState(iPos).`is`(Blocks.SPRUCE_TRAPDOOR)) {
                    valid = true
                    break
                }
                iPos.y += 1
            }
            if (valid) iPos else null
        } ?: run { // no ladder, tent variant with no basement? Return where trapdoor _would_ be to replace it
            var tpos: BlockPos? = null
            boundingBox.iterBlocks { iPos ->
                val blockState = level.getBlockState(iPos)
                if (blockState.`is`(Blocks.WHITE_CARPET)) {
                    tpos = iPos.below()
                    return@iterBlocks
                }
            }
            tpos
        }

        if (cellarPos1 != null && cellarPos2 != null) {
            cellarBoundingBox = BoundingBox.fromCorners(cellarPos1!!, cellarPos2!!)
        }
    }

    private fun manageDonkey(level: ServerLevel, uuid: UUID, pos: BlockPos) {
        EventUtil.runOnEntityWhenPossible(level, uuid) { donkey ->
            // DO NOT DO ON LOAD (which might be the case in runOnEntityWhenPossible),
            // as adding ents during load leads to ConcurrentModificationException
            EventUtil.runAtServerTickEnd {
                leashToBlock(level.level, donkey as Mob, pos)
            }
            donkey.getSlot(499)?.set(ItemStack(Items.CHEST))
            initDonkeyUuid = uuid
            donkey.addTag(Constants.TAG_RESEARCHER_DONKEY)
        }
    }

    private fun leashToBlock(level: ServerLevel, mob: Mob, pos: BlockPos) {
        val leashFenceKnotEntity = LeashFenceKnotEntity.getOrCreateKnot(level, pos)
        mob.setLeashedTo(leashFenceKnotEntity, true)
    }

    override fun addAdditionalSaveData(context: StructurePieceSerializationContext, tag: CompoundTag) {
        super.addAdditionalSaveData(context, tag)
        tag.putString("Rot", placeSettings.rotation.name)
        tag.saveField("JailPos", BlockPos.CODEC, ::jailPos)
        tag.saveField("ResearcherPos", BlockPos.CODEC, ::researcherPos)
        tag.saveField("cellarTrapdoorPos", BlockPos.CODEC, ::cellarTrapdoorPos)
        tag.saveField("DonkeyUUID", UUIDUtil.STRING_CODEC, ::initDonkeyUuid)
        tag.saveField("cellarBoundingBox", BoundingBox.CODEC, ::cellarBoundingBox)
        tag.saveField("templatePath", ResourceLocation.CODEC, ::templatePath)
    }
}