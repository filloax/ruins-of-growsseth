package com.ruslan.growsseth.maps

import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation

object MapIconTextures {
    //TODO: register

    // Client side class to load textures for custom map icons

    private val _customMapDecorationTextures = mutableMapOf<ResourceLocation, RenderType>()
    val customMapDecorationTextures: Map<ResourceLocation, RenderType>
        get() = _customMapDecorationTextures

    private val texturesByIconPath = mutableMapOf<ResourceLocation, RenderType>()

    private fun loadTextureForType(customMapDecorationType: CustomMapDecorationType) {
        val iconPath = customMapDecorationType.iconPath
        // Minor optimization
        _customMapDecorationTextures[customMapDecorationType.id] = texturesByIconPath.computeIfAbsent(iconPath) {
            RenderType.text(iconPath)
        }
    }


    fun init() {
        // register for new types added after, and load for types already loaded
        CustomMapData.onRegisterDecorationType { loadTextureForType(it) }
        CustomMapData.ALL_DECORATION_TYPES.values.forEach { loadTextureForType(it) }
    }
}