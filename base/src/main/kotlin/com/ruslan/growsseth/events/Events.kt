package com.ruslan.growsseth.events
import com.filloax.fxlib.api.EventWithTristate
import com.filloax.fxlib.api.TriState
import com.ruslan.growsseth.entity.researcher.trades.TradesListener
import kotlinx.event.event
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Leashable
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

object Events {
    @JvmField val FENCE_LEASH = event<FenceLeashEvent.Leash>()
    @JvmField val FENCE_LEASH_PRE = event<FenceLeashEvent.PreLeash>()
    @JvmField val FENCE_UNLEASH = event<FenceLeashEvent.Unleash>()

    @JvmField val NAMETAG_PRE = event<NameTagEvent.Pre>()
    @JvmField val NAMETAG_POST = event<NameTagEvent.Post>()

    @JvmField val PLACE_BLOCK = event<PlaceBlockEvent.Post>()

    @JvmField val PLAYER_ADVANCEMENT = event<PlayerAdvancementEvent.Post>()

    @JvmField val SERVER_ENTITY_DESTROYED = event<ServerEntityLifecycleEvent.Destroyed>()
}

object FenceLeashEvent {
    data class PreLeash(val mob: Leashable, val pos: BlockPos, val player: ServerPlayer)
        : EventWithTristate()
    data class Leash(val mob: Leashable, val pos: BlockPos, val player: ServerPlayer)
    data class Unleash(val mob: Leashable, val pos: BlockPos)
}

object NameTagEvent {
    data class Pre(
        val target: LivingEntity, val name: Component,
        val player: ServerPlayer, val stack: ItemStack,
        val usedHand: InteractionHand,
    ): EventWithTristate()
    data class Post(
        val target: LivingEntity, val name: Component,
        val player: ServerPlayer, val stack: ItemStack,
        val usedHand: InteractionHand,
    )
}

object PlaceBlockEvent {
    data class Post(
        val player: Player, val world: Level,
        val pos: BlockPos, val placeContext: BlockPlaceContext,
        val blockState: BlockState, val item: BlockItem
    )
}

object PlayerAdvancementEvent {
    data class Post(val player : ServerPlayer, val advancement: AdvancementHolder, val criterionKey: String)
}

object ServerEntityLifecycleEvent {
    data class Destroyed(val entity: Entity, val level: ServerLevel)
}