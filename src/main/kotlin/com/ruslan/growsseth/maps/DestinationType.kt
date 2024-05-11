package com.ruslan.growsseth.maps

import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.BuiltinStructures
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecoration
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes

/**
 * Pre-1.20.5: wrapper for both vanilla and custom map icons
 * Post-1.20.5: wrapper above map icons, kept for easier backporting
 */
class DestinationType private constructor(val type: Holder<MapDecorationType>? = null, val auto: Boolean = false) {
    val isSet get() = type != null

    companion object {
        val EMPTY = DestinationType()
        val AUTO = DestinationType(auto = true)

        private val VANILLA_STRUCT_ICONS = mapOf<ResourceKey<Structure>, Holder<MapDecorationType>>(
            BuiltinStructures.WOODLAND_MANSION to MapDecorationTypes.WOODLAND_MANSION,
            BuiltinStructures.OCEAN_MONUMENT to MapDecorationTypes.OCEAN_MONUMENT,
            BuiltinStructures.SWAMP_HUT to MapDecorationTypes.SWAMP_HUT,
            BuiltinStructures.TRIAL_CHAMBERS to MapDecorationTypes.TRIAL_CHAMBERS,
            BuiltinStructures.VILLAGE_PLAINS to MapDecorationTypes.PLAINS_VILLAGE,
            BuiltinStructures.VILLAGE_SNOWY to MapDecorationTypes.SNOWY_VILLAGE,
            BuiltinStructures.VILLAGE_TAIGA to MapDecorationTypes.TAIGA_VILLAGE,
            BuiltinStructures.VILLAGE_DESERT to MapDecorationTypes.DESERT_VILLAGE,
            BuiltinStructures.VILLAGE_SAVANNA to MapDecorationTypes.SAVANNA_VILLAGE,
        )

        fun vanilla(type: Holder<MapDecorationType>) = DestinationType(type)

        fun auto(struct: Holder<Structure>): DestinationType {
            return auto(struct.unwrapKey().get())
        }
        fun auto(structKey: ResourceKey<Structure>): DestinationType {
            GrowssethMapDecorations.getForStructure(structKey)?.let { type ->
                return vanilla(type)
            }
            VANILLA_STRUCT_ICONS[structKey]?.let { type ->
                return vanilla(type)
            }
            return vanilla(MapDecorationTypes.RED_X)
        }
        fun auto(structTag: TagKey<Structure>, registryAccess: RegistryAccess): DestinationType {
            GrowssethMapDecorations.getForStructure(structTag)?.let { type ->
                return vanilla(type)
            }
            val tagHolders = registryAccess.registryOrThrow(Registries.STRUCTURE).getTag(structTag).orElseThrow()
            VANILLA_STRUCT_ICONS.forEach {
                tagHolders.forEach { holder ->
                    if (holder.unwrapKey().get().location() == it.key.location()) {
                        return vanilla(it.value)
                    }
                }
            }
            return vanilla(MapDecorationTypes.RED_X)
        }
    }
}