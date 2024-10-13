package com.ruslan.growsseth.entity

import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ZombieResearcher
import com.ruslan.growsseth.platform.platform
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.ai.attributes.AttributeSupplier

object GrowssethEntities {
    val all = mutableMapOf<ResourceLocation, EntityType<*>>()

    val RESEARCHER = make(
        "researcher",
        EntityType.Builder.of(::Researcher, MobCategory.MISC) .sized(0.6f, 1.95f),
        Researcher.createAttributes(),
    )
    val ZOMBIE_RESEARCHER = make(
        "zombie_researcher",
        EntityType.Builder.of(::ZombieResearcher, MobCategory.MONSTER) .sized(0.6f, 1.95f),
        ZombieResearcher.createAttributes(),
    )

    private fun <T : LivingEntity> make(name: String, entityTypeBuilder: EntityType.Builder<T>, attributeBuilder: AttributeSupplier.Builder? = null) : EntityType<T> {
        val id = resLoc(name)
        val entityType = entityTypeBuilder.build(id.toString())
        attributeBuilder?.let { platform.registerEntDefaultAttribute(entityType, it) }
        all[id] = entityType
        return entityType
    }

    fun registerEntityTypes(registrator: (ResourceLocation, EntityType<*>) -> Unit) {
        all.forEach {
            registrator(it.key, it.value)
        }
    }
}