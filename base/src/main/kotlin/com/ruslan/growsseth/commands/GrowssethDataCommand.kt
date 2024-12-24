package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.dialogues.DialogueEntryConversion
import kotlinx.serialization.json.*
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.world.level.storage.LevelResource
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.jvm.optionals.getOrNull

object GrowssethDataCommand {
    const val DIALOGUES_ROOT = "growsseth_researcher_dialogue"
    private val JSON = Json {
        prettyPrint = true
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(
            literal("gdata").requires{ it.hasPermission(2) }
                .then(literal("dialogue")
                    .then(literal("extract")
                        .then(argument("lang", StringArgumentType.word())
                        .then(argument("prefix", StringArgumentType.word())
                        .then(argument("filePath", ResourceLocationArgument.id())
                        .executes { extractDialogueText(
                            it.source,
                            ResourceLocationArgument.getId(it, "filePath"),
                            StringArgumentType.getString(it, "prefix"),
                            StringArgumentType.getString(it, "lang"),
                        ) }
                        )))
                        .executes { ctx -> showHelp(ctx.source, "dialogue.extract") }
                    )
                )
        )
    }

    private fun extractDialogueText(source: CommandSourceStack, filePath: ResourceLocation, prefix: String, lang: String): Int {
        val adjustedPath = filePath.withPath("${DIALOGUES_ROOT}/${filePath.path}")

        val resource = source.server.resourceManager.getResource(adjustedPath).getOrNull() ?: run {
            RuinsOfGrowsseth.LOGGER.info(
                "Available files: {}",
                source.server.resourceManager.listResources(DIALOGUES_ROOT) { true }
            )
            source.sendFailure(Component.translatable("growsseth.commands.gdata.dialogue.extract.not-found", adjustedPath.path))
            return 0
        }

        val (dialogueKeyObj, languageStringObj) = try {
            resource.openAsReader()
                .readText()
                .let { JSON.decodeFromString(JsonObject.serializer(), it) }
                .let { DialogueEntryConversion.extractKeysFromDialogueFile(it, prefix) }
        } catch (e: Exception) {
            source.sendFailure(Component.translatable("growsseth.commands.gdata.dialogue.extract.parse-failure"))
            return 0
        }

        val generated = source.server.getWorldPath(LevelResource.GENERATED_DIR).normalize()
        val convertedDir = generated.resolve(Constants.RESEARCHER_DIALOGUE_EXTRACTED_FOLDER)
        val outputFile = convertedDir.resolve(adjustedPath.namespace).resolve(adjustedPath.path)

        outputFile.parent.toFile().mkdirs()
        outputFile.writeText(JSON.encodeToString(JsonObject.serializer(), dialogueKeyObj))

        // create lang files under dialogue subfolder

        val langDir = generated.resolve("lang/${lang}/${Constants.LANG_DIALOGUE_PREFIX}")
        langDir.toFile().mkdirs()

        languageStringObj[Constants.LANG_DIALOGUE_PREFIX]!!.jsonObject.forEach { (name, subObj) ->
            val out = langDir.resolve("${name}.json")

            var obj = subObj
            if (out.exists()) {
                val existingObj = JSON.decodeFromString<JsonObject>(out.readText())
                obj = mergeJsonObjects(existingObj, subObj.jsonObject)
            }

            out.writeText(JSON.encodeToString(JsonElement.serializer(), obj))
        }

        source.sendSuccess({
                Component.translatable("growsseth.commands.gdata.dialogue.extract.success", convertedDir.toString(), langDir.toString())
            }, true)

        return 1
    }

    private fun showHelp(source: CommandSourceStack, what: String): Int {
        source.sendSuccess({
            Component.translatable("growsseth.commands.gdata.$what.help")
        }, true)

        return 1
    }

    private fun mergeJsonObjects(obj1: JsonObject, obj2: JsonObject): JsonObject {
        val mergedContent = mutableMapOf<String, JsonElement>()

        // Add all keys from the first object
        for ((key, value) in obj1) {
            mergedContent[key] = value
        }

        // Merge keys from the second object
        for ((key, value) in obj2) {
            mergedContent[key] = when {
                key in obj1 && value is JsonObject && obj1[key] is JsonObject ->
                    mergeJsonObjects(obj1[key] as JsonObject, value) // Deep merge
                else -> value
            }
        }

        return JsonObject(mergedContent)
    }

}