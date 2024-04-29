package com.ruslan.growsseth.http

import com.mojang.datafixers.util.Either
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.Structure
import kotlin.concurrent.thread

interface GrowssethApi {
    val structureSpawns: List<ApiStructureSpawn>
    val quests: List<ApiQuestData>
    val events: List<ApiEvent>

    fun structById(id: Int): ApiStructureSpawn?
    fun structByName(name: String): ApiStructureSpawn?
    fun questById(id: Int): ApiQuestData?
    fun eventById(id: Int): ApiEvent?
    fun eventByName(name: String): ApiEvent?
    fun isEventActive(name: String): Boolean

    fun init()

    fun subscribe(callback: (GrowssethApi, MinecraftServer) -> Unit)
    fun unsubscribe(callback: (GrowssethApi, MinecraftServer) -> Unit)

    companion object {
        val current: GrowssethApi
            get() = GrowssethApiV2
    }
}

interface ApiStructureSpawn {
    val id: Int
    val structureId: String
    val name: String
    val startPos: BlockPos
    val active: Boolean
    val rotation: Rotation?
    val questId: Int?

    fun structureKey(): Either<TagKey<Structure>, ResourceKey<Structure>>
}

interface ApiEvent {
    val id: Int
    val name: String
    val active: Boolean
    val desc: String?
    val pos: BlockPos?
    val questId: Int?
    val rotation: Rotation?
}

interface ApiQuestData {
    val id: Int
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