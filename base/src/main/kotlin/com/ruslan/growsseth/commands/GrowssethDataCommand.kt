package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.dialogues.DialogueEntryConversion
import com.ruslan.growsseth.worldgen.worldpreset.LocationEntryConversion
import kotlinx.serialization.json.*
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.storage.LevelResource
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.jvm.optionals.getOrNull

object GrowssethDataCommand {
    const val DIALOGUES_ROOT = Constants.RESEARCHER_DIALOGUE_DATA_FOLDER
    const val PLACES_ROOT = Constants.PRESET_PLACES_FOLDER
    private val JSON = Json {
        prettyPrint = true
    }

    private enum class DataType(
        val params: DataExtractionParams,
        val id: String
    ) {
        DIALOGUES(
            DataExtractionParams(
                DIALOGUES_ROOT,
                DialogueEntryConversion::extractKeysFromDialogueFile,
                Constants.RESEARCHER_DIALOGUE_EXTRACTED_FOLDER,
                Constants.LANG_DIALOGUE_PREFIX
            ),
            "dialogues"
        ),
        PLACES(
            DataExtractionParams(
                PLACES_ROOT,
                LocationEntryConversion::extractKeysFromPlacesFile,
                Constants.PRESET_PLACES_EXTRACTED_FOLDER,
                Constants.LANG_PLACES_PREFIX
            ),
            "places"
        )
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(
            literal("gdata").requires{ it.hasPermission(2) }
                .then(
                    literal("dialogue").also { arg -> registerDataArgs(arg, DataType.DIALOGUES) }
                )
                .then(
                    literal("places").also { arg -> registerDataArgs(arg, DataType.PLACES) }
                )
        )
    }

    private fun <T : ArgumentBuilder<CommandSourceStack, T>> registerDataArgs(builder: ArgumentBuilder<CommandSourceStack, T>, dataType: DataType) {
        builder
            .then(literal("extract")
                .then(argument("lang", StringArgumentType.word())
                    .then(argument("prefix", StringArgumentType.word())
                        .then(argument("filePath", ResourceLocationArgument.id())
                        .executes {
                            extractText(
                            it.source,
                            ResourceLocationArgument.getId(it, "filePath"),
                            StringArgumentType.getString(it, "prefix"),
                            StringArgumentType.getString(it, "lang"),
                            dataType
                        ) }
                    )))
                .executes { ctx -> showHelp(ctx.source, dataType) }
            )
    }

    private data class DataExtractionParams(
        val dataRoot: String,
        val extractKeysFromFile: (root: JsonObject, langPrefix: String) -> Pair<JsonObject, JsonObject>,
        val extractedFolder: String,
        val langPrefix: String
    )

    private fun extractText(source: CommandSourceStack, filePath: ResourceLocation, prefix: String, lang: String, dataType: DataType): Int {
        val params: DataExtractionParams = dataType.params
        val adjustedPath = filePath.withPath("${params.dataRoot}/${filePath.path}")

        val resource = source.server.resourceManager.getResource(adjustedPath).getOrNull() ?: run {
            RuinsOfGrowsseth.LOGGER.info(
                "Available files: {}",
                source.server.resourceManager.listResources(params.dataRoot) { true }
            )
            source.sendFailure(Component.translatable("growsseth.commands.gdata.extract.not-found", adjustedPath.path))
            return 0
        }

        val (keyObj, languageStringObj) = try {
            resource.openAsReader()
                .readText()
                .let { JSON.decodeFromString(JsonObject.serializer(), it) }
                .let { params.extractKeysFromFile(it, prefix) }
        } catch (e: Exception) {
            source.sendFailure(Component.translatable("growsseth.commands.gdata.extract.parse-failure"))
            return 0
        }

        val generated = source.server.getWorldPath(LevelResource.GENERATED_DIR).normalize()
        val convertedDir = generated.resolve(params.extractedFolder)
        val outputFile = convertedDir.resolve(adjustedPath.namespace).resolve(adjustedPath.path)

        outputFile.parent.toFile().mkdirs()
        outputFile.writeText(JSON.encodeToString(JsonObject.serializer(), keyObj))

        // create lang files under dialogue/places subfolder

        val langDir = convertedDir.resolve(adjustedPath.namespace).resolve("lang/${lang}/${params.langPrefix}")
        langDir.toFile().mkdirs()

        languageStringObj[params.langPrefix]!!.jsonObject.forEach { (name, subObj) ->
            val out = langDir.resolve("${name}.json")

            var obj = subObj
            if (out.exists()) {
                val existingObj = JSON.decodeFromString<JsonObject>(out.readText())
                obj = mergeJsonObjects(existingObj, subObj.jsonObject)
            }

            out.writeText(JSON.encodeToString(JsonElement.serializer(), obj))
        }

        source.sendSuccess({
                Component.translatable("growsseth.commands.gdata.extract.success", convertedDir.toString(), langDir.toString())
            }, true)

        return 1
    }

    private fun showHelp(source: CommandSourceStack, dataType: DataType): Int {
        val type = dataType.id
        source.sendSuccess({
            Component.translatable("growsseth.commands.gdata.$type.extract.help")
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