package com.ruslan.growsseth.client.render

import com.ruslan.growsseth.entity.researcher.ZombieResearcher
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.client.model.ZombieVillagerModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.ArmorModelSet
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
import net.minecraft.resources.ResourceLocation

class ZombieResearcherRenderer(context: EntityRendererProvider.Context) :
    HumanoidMobRenderer<ZombieResearcher, ZombieResearcherRendererState, ZombieVillagerModel<ZombieResearcherRendererState>>
        (context, ZombieVillagerModel(context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER)), 0.5f)
{
    init {
        addLayer(HumanoidArmorLayer(
            this,
            ArmorModelSet.bake(ModelLayers.ZOMBIE_VILLAGER_ARMOR, context.modelSet, ::ZombieVillagerModel),
            context.equipmentRenderer,
        ))
        addLayer(ResearcherProfessionLayer(
            this,
            RESEARCHER_TYPE_SKIN,
            RESEARCHER_CLOTHES,
            RESEARCHER_CLOTHES_UNSHEATED_DAGGER
        ))

        //addLayer(CustomHeadLayer(this, context.modelSet, context.itemInHandRenderer))
        //addLayer(SimpleVillagerProfessionLayer(this, RESEARCHER_TYPE_SKIN, RESEARCHER_CLOTHES))
    }

    companion object {
        private val RESEARCHER_BASE_SKIN = resLoc("textures/entity/zombie_villager/researcher_zombie.png")
        private val RESEARCHER_TYPE_SKIN = resLoc("textures/entity/zombie_villager/type/researcher_zombie.png")
        // different texture from researcher because elbows mess with zombie hands
        private val RESEARCHER_CLOTHES = resLoc("textures/entity/zombie_villager/profession/researcher_zombie.png")
        private val RESEARCHER_CLOTHES_UNSHEATED_DAGGER = resLoc("textures/entity/villager/profession/researcher_unsheated.png")
    }

    override fun getTextureLocation(renderState: ZombieResearcherRendererState): ResourceLocation {
        return RESEARCHER_BASE_SKIN
    }

    override fun isShaking(renderState: ZombieResearcherRendererState): Boolean {
        return super.isShaking(renderState) || renderState.entity!!.isConverting
    }

    override fun createRenderState(): ZombieResearcherRendererState {
        return ZombieResearcherRendererState()
    }
}