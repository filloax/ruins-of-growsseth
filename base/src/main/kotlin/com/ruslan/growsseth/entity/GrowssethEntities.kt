package com.ruslan.growsseth.entity

import com.filloax.fxlib.api.registration.RegistryDelegate
import com.filloax.fxlib.api.registration.registryDelegate
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ZombieResearcher
import com.ruslan.growsseth.platform.platform
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import org.w3c.dom.Attr

object GrowssethEntities {
    val all = mutableMapOf<ResourceLocation, () -> EntityType<*>>()

    val RESEARCHER by make(
        "researcher",
        EntityType.Builder.of(::Researcher, MobCategory.MISC) .sized(0.6f, 1.95f),
        Researcher.createAttributes(),
    )
    val ZOMBIE_RESEARCHER by make(
        "zombie_researcher",
        EntityType.Builder.of(::ZombieResearcher, MobCategory.MONSTER) .sized(0.6f, 1.95f),
        ZombieResearcher.createAttributes(),
    )

    private fun <T : LivingEntity> make(
        name: String,
        entityTypeBuilder: EntityType.Builder<T>,
        attributeSupplier: (() -> AttributeSupplier.Builder)? = null
    ) = registryDelegate(resLoc(name)) {
        all[id] = {
            val entityType = entityTypeBuilder.build(id.toString())
            init(entityType)
            attributeSupplier?.let { platform.registerEntDefaultAttribute(entityType, it) }

            entityType
        }
    }

    fun registerEntityTypes(registrator: (ResourceLocation, EntityType<*>) -> Unit) {
        all.forEach {
            registrator(it.key, it.value())
        }
    }
}