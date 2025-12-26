//package com.ruslan.growsseth.compat.cobblemon
//
//import com.gitlab.srcmc.rctapi.api.RCTApi
//import com.gitlab.srcmc.rctapi.api.battle.BattleFormat
//import com.gitlab.srcmc.rctapi.api.battle.BattleManager
//import com.gitlab.srcmc.rctapi.api.battle.BattleRules
//import com.gitlab.srcmc.rctapi.api.battle.BattleState
//import com.gitlab.srcmc.rctapi.api.events.Events
//import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC
//import com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer
//import com.ruslan.growsseth.RuinsOfGrowsseth
//import com.ruslan.growsseth.entity.researcher.Researcher
//import com.ruslan.growsseth.entity.researcher.ResearcherDialoguesComponent
//import net.minecraft.server.MinecraftServer
//import net.minecraft.server.level.ServerPlayer
//import net.minecraft.world.entity.player.Player
//
//
///**
// * Optional compatibility with Cobblemon using the Radical Cobblemon Trainers API.
// */
//object CobblemonRCTCompat {
//    val RCT by lazy { RCTApi.initInstance(RuinsOfGrowsseth.MOD_ID) }
//    var isServerActive = false
//        private set
//
//    val MAX_ITEM_USES = 3
//
//    fun onInit() {
//        ResearcherRCTBattleAIConfig.register()
//
//        RCT.eventContext.register(Events.BATTLE_ENDED) { event ->
//            onBattleEnd(event.value)
//        }
//    }
//
//    fun rctApiOnServerStarted(server: MinecraftServer) {
//        RCT.trainerRegistry.init(server)
//
//        RuinsOfGrowsseth.LOGGER.info("Registering RCT trainer NPCs...")
//        CobblemonRCTListener.TRAINER_DATA.forEach { (team, model) ->
//            RCT.trainerRegistry.registerNPC(team.toString(), model)
//        }
//
//        isServerActive = true
//    }
//
//    fun rctApiOnServerStop(server: MinecraftServer) {
//        isServerActive = false
//    }
//
//    fun rctApiOnPlayerJoin(player: ServerPlayer) {
//        RCT.trainerRegistry.registerPlayer(player.name.string, player)
//    }
//
//    fun rctApiOnPlayerQuit(player: Player) {
//        RCT.trainerRegistry.unregisterById(player.name.string)
//    }
//
//    /**
//     * Attempts to start a trainer battle between the given player and the researcher.
//     */
//    fun tryStartTrainerBattle(player: ServerPlayer, researcher: Researcher): Boolean {
//        val team = selectResearcherTeam(player, researcher)
//
//        val trainer = RCT.trainerRegistry.getById(team.toString(), TrainerNPC::class.java)
//
//        if (trainer == null) {
//            RuinsOfGrowsseth.LOGGER.error("RCT trainer not found for team {}", team)
//            startBattleErrorDialogue(researcher, player, "no-team")
//            return false
//        }
//
//        val playerTrainer = RCT.trainerRegistry.getById(player.name.string, TrainerPlayer::class.java)
//
//        if (playerTrainer == null) {
//            RuinsOfGrowsseth.LOGGER.error("RCT trainer not found for player {}", player.name)
//            startBattleErrorDialogue(researcher, player, "no-player")
//            return false
//        }
//
//        trainer.entity = researcher
//
//        val battleId = RCT.battleManager.startBattle(listOf(playerTrainer), listOf(trainer), BattleFormat.GEN_9_SINGLES,  BattleRules.Builder()
//            .withMaxItemUses(MAX_ITEM_USES)
//            .build())
//
//        if (battleId == null) {
//            startBattleErrorDialogue(researcher, player, "no-battle")
//            return false
//        }
//
//        researcher.dialogues!!.triggerDialogue(player, ResearcherDialoguesComponent.EV_COMPAT_COBBLEMON_BATTLE_START)
//        researcher.isInCobblemonBattle = true
//
//        return true
//    }
//
//    private fun onBattleEnd(battleState: BattleState) {
//        val researcher = (battleState.participants1 + battleState.participants2)
//            .firstNotNullOfOrNull { if (it.entity is Researcher) (it.entity as Researcher) else null }
//        val players = (battleState.participants1 + battleState.participants2)
//            .mapNotNull { if (it.entity is ServerPlayer) (it.entity as ServerPlayer) else null }
//
//        if (researcher != null) {
//            if (players.isEmpty()) {
//                RuinsOfGrowsseth.LOGGER.error("RCT battle ended without players!")
//                researcher.isInCobblemonBattle = false
//                return
//            }
//
//            val researcherWon = battleState.winners.any { it.entity == researcher }
//            players.forEach { player ->
//                if (researcherWon) {
//                    researcher.dialogues!!.triggerDialogue(player, ResearcherDialoguesComponent.EV_COMPAT_COBBLEMON_BATTLE_END_LOSE)
//                } else {
//                    researcher.dialogues!!.triggerDialogue(player, ResearcherDialoguesComponent.EV_COMPAT_COBBLEMON_BATTLE_END_WIN)
//                }
//            }
//            researcher.isInCobblemonBattle = false
//        }
//    }
//
//    private fun startBattleErrorDialogue(researcher: Researcher, player: ServerPlayer, errorId: String) {
//        researcher.dialogues!!.triggerDialogue(player, ResearcherDialoguesComponent.EV_COMPAT_COBBLEMON_ERROR, eventParam=errorId)
//    }
//
//    private fun selectResearcherTeam(playerTeam: ServerPlayer, researcher: Researcher): TrainerTeam {
//        // for now only this
//        return TrainerTeam.RESEARCHER_STANDARD
//    }
//}
