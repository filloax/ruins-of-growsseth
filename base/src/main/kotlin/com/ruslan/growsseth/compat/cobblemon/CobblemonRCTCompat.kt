package com.ruslan.growsseth.compat.cobblemon

import com.gitlab.srcmc.rctapi.api.RCTApi
import com.gitlab.srcmc.rctapi.api.battle.BattleFormat
import com.gitlab.srcmc.rctapi.api.battle.BattleRules
import com.gitlab.srcmc.rctapi.api.battle.BattleState
import com.gitlab.srcmc.rctapi.api.events.Events
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC
import com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.GrowssethLootTables
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ModCompatConfig
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.entity.researcher.ResearcherDialoguesComponent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets


/**
 * Optional compatibility with Cobblemon using the Radical Cobblemon Trainers API.
 */
object CobblemonRCTCompat {
    val RCT by lazy { RCTApi.initInstance(RuinsOfGrowsseth.MOD_ID) }
    var isServerActive = false
        private set

    val MAX_ITEM_USES = 3

    fun onInit() {
        ResearcherRCTBattleAIConfig.register()

        RCT.eventContext.register(Events.BATTLE_ENDED) { event ->
            onBattleEnd(event.value)
        }
    }

    fun rctApiOnServerStarted(server: MinecraftServer) {
        RCT.trainerRegistry.init(server)

        RuinsOfGrowsseth.LOGGER.info("Registering RCT trainer NPCs...")
        CobblemonRCTListener.TRAINER_DATA.forEach { (team, model) ->
            RCT.trainerRegistry.registerNPC(team.toString(), model)
        }

        isServerActive = true
    }

    fun rctApiOnServerStop(server: MinecraftServer) {
        isServerActive = false
    }

    fun rctApiOnPlayerJoin(player: ServerPlayer) {
        RCT.trainerRegistry.registerPlayer(player.name.string, player)
    }

    fun rctApiOnPlayerQuit(player: Player) {
        RCT.trainerRegistry.unregisterById(player.name.string)
    }

    /**
     * Attempts to start a trainer battle between the given player and the researcher.
     */
    fun tryStartTrainerBattle(player: ServerPlayer, researcher: Researcher): Boolean {
        val playerTrainer = RCT.trainerRegistry.getById(player.name.string, TrainerPlayer::class.java)

        if (playerTrainer == null) {
            RuinsOfGrowsseth.LOGGER.error("RCT trainer not found for player {}", player.name)
            startBattleErrorDialogue(researcher, player, "no-player")
            return false
        }

        val team = selectResearcherTeam(player, playerTrainer, researcher)

        val trainer = RCT.trainerRegistry.getById(team.toString(), TrainerNPC::class.java)

        if (trainer == null) {
            RuinsOfGrowsseth.LOGGER.error("RCT trainer not found for team {}", team)
            startBattleErrorDialogue(researcher, player, "no-team")
            return false
        }
        trainer.entity = researcher

        val battleId = RCT.battleManager.startBattle(listOf(playerTrainer), listOf(trainer), BattleFormat.GEN_9_SINGLES,  BattleRules.Builder()
            .withMaxItemUses(MAX_ITEM_USES)
            .build())

        if (battleId == null) {
            startBattleErrorDialogue(researcher, player, "no-battle")
            return false
        }

        var param: String? = null
        if (team == TrainerTeam.RESEARCHER_MAX_LEVEL) {
            param = if (researcher.compatData().cobblemonUsedMaxLevelOnce) {
                "maxLevel"
            } else {
                "forceMaxLevel"
            }
            researcher.compatData().cobblemonUsedMaxLevelOnce = true
        }

        researcher.dialogues!!.triggerDialogue(player, ResearcherDialoguesComponent.EV_COMPAT_COBBLEMON_BATTLE_START, eventParam = param)
        researcher.isInCobblemonBattle = true

        return true
    }

    private fun onBattleEnd(battleState: BattleState) {
        val researcher = (battleState.participants1 + battleState.participants2)
            .firstNotNullOfOrNull { if (it.entity is Researcher) (it.entity as Researcher) else null }
        val players = (battleState.participants1 + battleState.participants2)
            .mapNotNull { if (it.entity is ServerPlayer) (it.entity as ServerPlayer) else null }

        if (researcher != null) {
            researcher.isInCobblemonBattle = false

            if (players.isEmpty()) {
                RuinsOfGrowsseth.LOGGER.error("RCT battle ended without players!")
                return
            }

            val researcherWon = battleState.winners.any { it.entity == researcher }
            players.forEach { player ->
                if (researcherWon) {
                    researcher.dialogues!!.triggerDialogue(player, ResearcherDialoguesComponent.EV_COMPAT_COBBLEMON_BATTLE_END_LOSE)
                } else {
                    researcher.dialogues!!.triggerDialogue(player, ResearcherDialoguesComponent.EV_COMPAT_COBBLEMON_BATTLE_END_WIN)
                }
            }

            if (!researcherWon) {
                researcher.compatData().cobblemonDefeatedOnce = true
                spawnBattleReward(researcher)
            }
        }
    }

    private fun spawnBattleReward(researcher: Researcher) {
        if (!ModCompatConfig.cobblemonResearcherRewardEnabled) return

        val compatData = researcher.compatData()

        val timeSinceLast = researcher.level().gameTime - compatData.cobblemonLastDefeatedDate
        val minimumTime = ModCompatConfig.cobblemonResearcherRewardCooldownDays * Constants.DAY_TICKS_DURATION

        if (compatData.cobblemonLastDefeatedDate == -1L || timeSinceLast >= minimumTime) {
            compatData.cobblemonLastDefeatedDate = researcher.level().gameTime

            val serverLevel = researcher.level() as ServerLevel
            val lootParams = LootParams.Builder(serverLevel).create(LootContextParamSets.EMPTY)
            // intentionally do not affect with luck
            val table = serverLevel.server.reloadableRegistries().getLootTable(GrowssethLootTables.COBBLEMON_DEFEAT_RESEARCHER)
            val list = table.getRandomItems(lootParams)

            list.forEach { item ->
                val pos = researcher.position()
                val x = pos.x + 0.5
                val y = pos.y + 0.5
                val z = pos.z + 0.5

                val itemEntity = ItemEntity(serverLevel, x, y, z, item)

                itemEntity.setDeltaMovement(
                    (serverLevel.random.nextDouble() - 0.5) * 0.1,
                    serverLevel.random.nextDouble() * 0.1 + 0.1,
                    (serverLevel.random.nextDouble() - 0.5) * 0.1
                )

                serverLevel.addFreshEntity(itemEntity)
            }
        } else {
            RuinsOfGrowsseth.LOGGER.info("Researcher defeated in cobblemon battle, but not enough time has passed since last defeat (is $timeSinceLast). Skipping reward spawn.")
        }
    }

    private fun startBattleErrorDialogue(researcher: Researcher, player: ServerPlayer, errorId: String) {
        researcher.dialogues!!.triggerDialogue(player, ResearcherDialoguesComponent.EV_COMPAT_COBBLEMON_ERROR, eventParam=errorId)
    }

    private fun selectResearcherTeam(playerTeam: ServerPlayer, playerTrainer: TrainerPlayer, researcher: Researcher): TrainerTeam {
        // if defeated once and player has pokemon above lvl 81
        if (researcher.compatData().cobblemonDefeatedOnce) {
            val playerLevelMax = playerTrainer.team.maxOf { pkmn -> pkmn.level }
            if (playerLevelMax > 81) {
                return TrainerTeam.RESEARCHER_MAX_LEVEL
            }
        }

        return TrainerTeam.RESEARCHER_STANDARD
    }
}
