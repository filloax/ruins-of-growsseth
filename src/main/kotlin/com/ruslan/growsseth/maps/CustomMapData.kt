package com.ruslan.growsseth.maps

import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.function.Consumer

object CustomMapData {
    const val DECORATIONS_CONTAINER_TAG = "${RuinsOfGrowsseth.MOD_ID}_CustomDecorations"
    private val decorationTypes = mutableMapOf<ResourceLocation, CustomMapDecorationType>()
    val ALL_DECORATION_TYPES: Map<ResourceLocation, CustomMapDecorationType>
        get() = decorationTypes
    private val registerDecorationCallbacks = mutableListOf<Consumer<CustomMapDecorationType>>()

    fun addTargetCustomDecoration(itemStack: ItemStack, blockPos: BlockPos, id: String?, type: CustomMapDecorationType) {
        val decorationTags: ListTag
        if (itemStack.hasTag() && itemStack.tag!!.contains(DECORATIONS_CONTAINER_TAG, Tag.TAG_LIST.toInt())) {
            decorationTags = itemStack.tag!!.getList(DECORATIONS_CONTAINER_TAG, Tag.TAG_COMPOUND.toInt())
        } else {
            decorationTags = ListTag()
            itemStack.addTagElement(DECORATIONS_CONTAINER_TAG, decorationTags)
        }

        val decorationTag = CompoundTag()
        decorationTag.putString("type", type.id.toString())
        id?.let { decorationTag.putString("id", it) }
        decorationTag.putDouble("x", blockPos.x.toDouble())
        decorationTag.putDouble("z", blockPos.z.toDouble())
        decorationTag.putDouble("rot", 180.0)
        decorationTags.add(decorationTag)

        if (type.hasMapColor()) {
            val compoundTag2 = itemStack.getOrCreateTagElement("display")
            compoundTag2.putInt("MapColor", type.mapColor)
        }

        RuinsOfGrowsseth.LOGGER.info("TAGS: ${itemStack.tag}")
    }

    fun loadCustomDecorationFromData(
        currentCustomDecorations: Map<String, CustomMapDecoration>, itemStack: ItemStack, player: Player,
        addCustomDecoration: (type: CustomMapDecorationType, levelAccessor: LevelAccessor?, id: String, x: Double, z: Double, rot: Double, name: Component?) -> Unit
    ) {
        val tags = itemStack.tag
        if (tags != null && tags.contains(DECORATIONS_CONTAINER_TAG, Tag.TAG_LIST.toInt())) {
            val decorations = tags.getList(DECORATIONS_CONTAINER_TAG, Tag.TAG_COMPOUND.toInt())
            for (j in decorations.indices) {
                val decorationTag = decorations.getCompound(j)
                val decorationType = getDecorationType(ResourceLocation(decorationTag.getString("type")))
                if (decorationType == null) {
                    RuinsOfGrowsseth.LOGGER.error("Decoration type ${decorationTag.getString("type")} not found during map data load for $itemStack")
                    continue
                }
                if (!currentCustomDecorations.containsKey(decorationTag.getString("id"))) {
                    addCustomDecoration(
                        decorationType,
                        player.level(),
                        decorationTag.getString("id"),
                        decorationTag.getDouble("x"),
                        decorationTag.getDouble("z"),
                        decorationTag.getDouble("rot"),
                        null as Component?
                    )
                }
            }
        }
    }

    // Since Java doesn't handle Kotlin unit/void lambdas well
    fun loadCustomDecorationFromDataJava(
        currentCustomDecorations: Map<String, CustomMapDecoration>, itemStack: ItemStack, player: Player,
        addCustomDecoration: JavaAddCustomDecoration
    ) {
        loadCustomDecorationFromData(currentCustomDecorations, itemStack, player) { type, levelAccessor, id, x, z, rot, name ->
            addCustomDecoration.run(type, levelAccessor, id, x, z, rot, name)
        }
    }

    fun registerDecorationType(decorationType: CustomMapDecorationType): CustomMapDecorationType {
        if (decorationTypes.containsKey(decorationType.id))
            throw Exception("Key ${decorationType.id} already present in custom decoration types!")

        decorationTypes[decorationType.id] = decorationType

        registerDecorationCallbacks.forEach { it.accept(decorationType) }

        return decorationType
    }

    fun getDecorationType(key: ResourceLocation): CustomMapDecorationType? {
        return decorationTypes[key]
    }

    fun decorationTypeByStructure(structure: ResourceKey<Structure>): CustomMapDecorationType? {
        return decorationTypes.values.find { it.structure?.location() == structure.location() }
    }

    fun decorationTypesByStructureTag(registryAccess: RegistryAccess, tag: TagKey<Structure>): List<CustomMapDecorationType> {
        val structureHolders = registryAccess.registryOrThrow(Registries.STRUCTURE).getTag(tag).orElseThrow()
        val resourceLocations: List<ResourceLocation> = structureHolders.map {
            it.unwrapKey().get().location()
        }
        return decorationTypes.values.filter { it.structure?.location() in resourceLocations }
    }

    fun onRegisterDecorationType(handler: Consumer<CustomMapDecorationType>) {
        registerDecorationCallbacks.add(handler)
    }

    // Since Java doesn't handle Kotlin unit/void lambdas well
    @FunctionalInterface
    interface JavaAddCustomDecoration {
        fun run(type: CustomMapDecorationType, levelAccessor: LevelAccessor?, id: String, x: Double, z: Double, rot: Double, name: Component?)
    }
}
