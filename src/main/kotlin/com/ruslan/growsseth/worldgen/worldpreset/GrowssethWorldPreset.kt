package com.ruslan.growsseth.worldgen.worldpreset

import com.filloax.fxlib.api.FxLibServices
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.WorldPresetConfig
import com.ruslan.growsseth.networking.PlacesInfoPacket
import com.ruslan.growsseth.worldgen.GrowssethModBiomeSources
import com.ruslan.growsseth.worldgen.GrowssethModWorldPresets
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.biome.*
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.level.dimension.LevelStem
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.presets.WorldPreset
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureSet
import kotlin.jvm.optionals.getOrNull

// object as WorldPreset as a class doesn't seem to do much except hold data,
// we'll contain related functions, callbacks, etc here
object GrowssethWorldPreset {
    const val GROWSSETH_SEED = "MelminaVerde" // Cydonia's

    private val fixedStructureGeneration = FxLibServices.fixedStructureGeneration

    object Callbacks {
        fun onServerStarted(server: MinecraftServer) {
            if (isGrowssethPreset(server)) {
                WorldPresetConfig.getAll().forEach { cfg ->
                    fixedStructureGeneration.register(server.overworld(), cfg.structureId + "_grworldgen", BlockPos(cfg.x, cfg.y, cfg.z), cfg.structureId(), cfg.rotation)
                }
            }
        }

        fun onServerPlayerJoin(handler: ServerGamePacketListenerImpl, sender: PacketSender, server: MinecraftServer) {
            if (isGrowssethPreset(server)) {
                if (LocationNotifListener.loaded) {
                    sender.sendPacket(PlacesInfoPacket(LocationNotifListener.PLACES_DATA))
                } else {
                    LocationNotifListener.onNextReload {
                        sender.sendPacket(PlacesInfoPacket(it))
                    }
                }
            }
        }
    }

    @JvmStatic
    fun isGrowssethPreset(preset: Holder<WorldPreset>?): Boolean {
        return preset?.unwrapKey()?.getOrNull()?.equals(GrowssethModWorldPresets.GROWSSETH) ?: false
    }

    @JvmStatic
    fun isGrowssethPresetFromOverworldBiomeSource(biomeSource: BiomeSource): Boolean {
        if (biomeSource is MultiNoiseBiomeSource) {
            val parameters = biomeSource.parameters
            return parameters.map({
                false
            }, { parametersHolder ->
                parametersHolder.unwrapKey().getOrNull() == GrowssethModBiomeSources.GROWSSETH_OVERWORLD_SETTINGS
            })
        }
        return false
    }

    @JvmStatic
    fun isGrowssethPreset(server: MinecraftServer): Boolean {
        return isGrowssethPresetFromOverworldBiomeSource(server.overworld().chunkSource.generator.biomeSource)
    }

    @JvmStatic
    fun isGrowssethPreset(levelAcc: ServerLevelAccessor): Boolean {
        return isGrowssethPreset(levelAcc.level.server)
    }

    fun shouldDisableStructure(structure: Holder<Structure>, level: ServerLevel): Boolean {
        return isGrowssethPreset(level)
            && structure.unwrapKey().getOrNull()?.location()?.namespace == RuinsOfGrowsseth.MOD_ID
    }

    fun shouldDisableStructureSet(structureSet: Holder<StructureSet>, biomeSource: BiomeSource): Boolean {
        return isGrowssethPresetFromOverworldBiomeSource(biomeSource)
            && structureSet.unwrapKey().getOrNull()?.location()?.namespace == RuinsOfGrowsseth.MOD_ID
    }

    fun shouldDisableVillagePresets(server: MinecraftServer): Boolean {
        return isGrowssethPreset(server)
    }

    fun build(ctx: BootstrapContext<WorldPreset>): WorldPreset {
        val noiseSettings = ctx.lookup(Registries.NOISE_SETTINGS)
        val biomes = ctx.lookup(Registries.BIOME)
        val multiNoiseBiomeSourceParameterLists = ctx.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)

        val dimTypes =  ctx.lookup(Registries.DIMENSION_TYPE)
        val overworldType = dimTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD)
        val netherType = dimTypes.getOrThrow(BuiltinDimensionTypes.NETHER)
        val endType = dimTypes.getOrThrow(BuiltinDimensionTypes.END)

        val overworldBiomeParameters = multiNoiseBiomeSourceParameterLists.getOrThrow(
            GrowssethModBiomeSources.GROWSSETH_OVERWORLD_SETTINGS
        )
        val overworldNoiseSettings = noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD)
        val overworldStem = LevelStem(overworldType, NoiseBasedChunkGenerator(
            MultiNoiseBiomeSource.createFromPreset(overworldBiomeParameters), overworldNoiseSettings
        ))

        val netherBiomeParameters = multiNoiseBiomeSourceParameterLists.getOrThrow(
            MultiNoiseBiomeSourceParameterLists.NETHER
        )
        val netherNoiseSettings = noiseSettings.getOrThrow(NoiseGeneratorSettings.NETHER)
        val netherStem = LevelStem(netherType, NoiseBasedChunkGenerator(
            MultiNoiseBiomeSource.createFromPreset(netherBiomeParameters), netherNoiseSettings
        ))

        val endNoiseSettings = noiseSettings.getOrThrow(NoiseGeneratorSettings.END)
        val endStem = LevelStem(endType, NoiseBasedChunkGenerator(
            TheEndBiomeSource.create(biomes), endNoiseSettings
        ))

        return WorldPreset(mutableMapOf(
            LevelStem.OVERWORLD to overworldStem,
            LevelStem.NETHER to netherStem,
            LevelStem.END to endStem,
        ))
    }

    fun bootstrapNoiseBiomeSourcesSettings(ctx: BootstrapContext<MultiNoiseBiomeSourceParameterList>) {
        val biomes = ctx.lookup(Registries.BIOME)
        ctx.register(
            GrowssethModBiomeSources.GROWSSETH_OVERWORLD_SETTINGS,
            MultiNoiseBiomeSourceParameterList(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD, biomes)
        )
    }
}