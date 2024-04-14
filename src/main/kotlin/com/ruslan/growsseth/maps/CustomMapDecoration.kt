package com.ruslan.growsseth.maps

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.structure.Structure


data class CustomMapDecoration(
    val type: CustomMapDecorationType,
    val x: Byte, val y: Byte, val rot: Byte,
    val name: Component?
) {
    fun renderOnFrame(): Boolean = type.renderedOnFrame
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomMapDecoration

        if (type.id != other.type.id) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (rot != other.rot) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + rot
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }
}

data class CustomMapDecorationType(
    val id: ResourceLocation,
    val iconPath: ResourceLocation,
    val iconNum: Int = 0,
    val iconsPerRow: Int = 1,
    val renderedOnFrame: Boolean = true,
    val mapColor: Int = -1,
    val trackCount: Boolean = true, // Unused
    val structure: ResourceKey<Structure>? = null,
) {
    init {
        assert(iconsPerRow > 0)
    }
    fun hasMapColor() = mapColor >= 0
    fun shouldTrackCount() = trackCount

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomMapDecorationType

        if (id != other.id) return false
        if (iconPath != other.iconPath) return false
        if (iconNum != other.iconNum) return false
        if (iconsPerRow != other.iconsPerRow) return false
        if (renderedOnFrame != other.renderedOnFrame) return false
        if (mapColor != other.mapColor) return false
        if (trackCount != other.trackCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + iconPath.hashCode()
        result = 31 * result + iconNum
        result = 31 * result + iconsPerRow
        result = 31 * result + renderedOnFrame.hashCode()
        result = 31 * result + mapColor
        result = 31 * result + trackCount.hashCode()
        result = 31 * result + (structure?.hashCode() ?: 0)
        return result
    }
}