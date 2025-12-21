package com.ruslan.growsseth.compat.cobblemon

import com.cobblemon.mod.common.api.battles.interpreter.BattleContext
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.battles.*
import com.gitlab.srcmc.rctapi.api.RCTApi
import com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI
import com.gitlab.srcmc.rctapi.api.ai.utils.ResponseBuilder
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.compat.ModCompatChecker
import net.minecraft.resources.ResourceLocation
import java.util.*

class ResearcherRCTBattleAI : RCTBattleAI {
    private val rng = Random()
    private val maxSelectMargin: Double

    constructor() : super() {
        maxSelectMargin = ResearcherRCTBattleAIConfig().maxSelectMargin
    }

    constructor(config: ResearcherRCTBattleAIConfig) : super(config.toRCTConfig()) {
        maxSelectMargin = config.maxSelectMargin
    }

    val researcherMonAiRegistry = mapOf<String, (ActiveBattlePokemon, PokemonBattle, BattleSide, ShowdownMoveset) -> ShowdownActionResponse?>(
        "vespiquen" to ai@{ pkmn, battle, aiSide, moveset ->
            moveset.canDynamax = false
            moveset.canTerastallize = null

            val moves = when (getToxicSpikesNum(aiSide.getOppositeSide())) {
                0 -> listOf("toxicspikes")
                1 -> listOf("toxicspikes", "protection")
                else -> return@ai null
            }
            simpleMoveResponse(pkmn, moveset, moves)
        }
    ).mapKeys { ResourceLocation.fromNamespaceAndPath(ModCompatChecker.ID_COBBLEMON, it.key) }

    override fun choose(
        pkmn: ActiveBattlePokemon,
        battle: PokemonBattle,
        aiSide: BattleSide,
        moveset: ShowdownMoveset?,
        forceSwitch: Boolean
    ): ShowdownActionResponse {
        if (pkmn.hasPokemon() && moveset != null && !forceSwitch) {
//            val registryTrainer = RCTApi.getInstances()
//                .map { (key, rct) ->
//                    rct.trainerRegistry.getByOT(pkmn.battlePokemon!!.effectedPokemon)
//                }
//                .filter { t -> t != null && t is TrainerNPC }
//                .findFirst()
//                .orElse(null)

            val species = pkmn.battlePokemon!!.effectedPokemon.species
            val customAi = researcherMonAiRegistry[species.resourceIdentifier]

            if (customAi != null) {
                RuinsOfGrowsseth.LOGGER.info("Pokemon is {} with custom AI for Researcher", pkmn.battlePokemon!!.effectedPokemon.species)

                val response = customAi(pkmn, battle, aiSide, moveset)
                if (response != null) {
                    return response
                }
            }
        }

        return super.choose(pkmn, battle, aiSide, moveset, forceSwitch)
    }

    private fun getToxicSpikesNum(aiSide: BattleSide): Int {
        val hazards = aiSide.contextManager.get(BattleContext.Type.HAZARD)?.map { it.id } ?: emptyList()
        return hazards.count { it == "toxicspikes" }
    }

    private fun simpleMoveResponse(pkmn: ActiveBattlePokemon, moveset: ShowdownMoveset, allowedMoves: List<String>): ShowdownActionResponse {
        return ResponseBuilder
            .create(pkmn, moveset, false)
            .margin(this.rng.nextDouble(this.maxSelectMargin))
            .random(this.rng)
            .suggestMoves{ candidates ->
                candidates
                    .filter { pair -> pair!!.second !is ActiveBattlePokemon && allowedMoves.contains(pair.first!!.id) }
                    .map { pair ->
                        ResponseBuilder.Choice (
                            String.format(
                                "MOVE %s -> <multi/none>",
                                pair.first!!.id
                            ), pair, 1.0
                        )
                    }
            }
            .response()
    }
}