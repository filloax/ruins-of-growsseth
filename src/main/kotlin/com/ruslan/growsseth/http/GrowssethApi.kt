package com.ruslan.growsseth.http

import com.mojang.datafixers.util.Either
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

interface GrowssethApi {
    val structureSpawns: List<ApiStructureSpawn>
    // Currently unused
    val quests: List<ApiQuestData>
    val events: List<ApiEvent>

    fun structByName(name: String): ApiStructureSpawn?
    fun eventByName(name: String): ApiEvent?
    fun isEventActive(name: String): Boolean

    fun init()

    fun subscribe(callback: (GrowssethApi, MinecraftServer) -> Unit)
    fun unsubscribe(callback: (GrowssethApi, MinecraftServer) -> Unit)

    fun reload(): CompletableFuture<Boolean>

    companion object {
        val current: GrowssethApi
            get() = GrowssethApiV2
    }
}

interface ApiStructureSpawn {
    val structureId: String
    val name: String
    val startPos: BlockPos
    val active: Boolean
    val rotation: Rotation?

    fun structureKey(): Either<TagKey<Structure>, ResourceKey<Structure>>
}

interface ApiEvent {
    val name: String
    val active: Boolean
    val desc: String?
    val pos: BlockPos?
    val rotation: Rotation?
}

// Currently unused
interface ApiQuestData {
    val unlocked: Boolean
    val solved: Boolean
    val name: String
    val imgUnlocked: String
    val imgLocked: String
    val text: String
    val difficulty: String
}

abstract class AbstractGrowssethApi : GrowssethApi {
    private val subscribers = mutableSetOf<(GrowssethApi, MinecraftServer) -> Unit>()

    override fun subscribe(callback: (GrowssethApi, MinecraftServer) -> Unit) {
        subscribers.add(callback)
    }

    override fun unsubscribe(callback: (GrowssethApi, MinecraftServer) -> Unit) {
        subscribers.remove(callback)
    }

    fun triggerSubscriberUpdates(server: MinecraftServer) {
        thread {
            synchronized(subscribers) {
                subscribers.forEach { it(this, server) }
            }
        }
    }
}