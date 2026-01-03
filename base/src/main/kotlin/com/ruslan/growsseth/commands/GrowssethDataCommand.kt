package com.ruslan.growsseth.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.dialogues.DialogueEntryConversion
import com.ruslan.growsseth.worldgen.worldpreset.LocationEntryConversion
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.Resource
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.io.readText
import kotlin.jvm.optionals.getOrNull

object GrowssethDataCommand {
    const val DIALOGUES_ROOT = Constants.RESEARCHER_DIALOGUE_DATA_FOLDER
    const val PLACES_ROOT = Constants.PRESET_PLACES_FOLDER

    @OptIn(ExperimentalSerializationApi::class)
    private val JSON = Json {
        prettyPrint = true
        allowComments = true
    }

    private enum class DataType(
        val params: DataExtractionParams,
        val helpId: String
    ) {
        DIALOGUES(
            DataExtractionParams(
                DIALOGUES_ROOT,
                Constants.RESEARCHER_DIALOGUE_EXTRACTED_FOLDER,
                Constants.LANG_DIALOGUE_PREFIX,
                GrowssethDataCommand::tryWriteDialogueFiles
            ),
            "dialogue"
        ),
        PLACES(
            DataExtractionParams(
                PLACES_ROOT,
                Constants.PRESET_PLACES_EXTRACTED_FOLDER,
                Constants.LANG_PLACES_PREFIX,
                GrowssethDataCommand::tryWritePlacesFiles
            ),
            "places"
        )
    }

    private data class DataExtractionParams(
        val dataRoot: String,
        val extractedFolder: String,
        val langSubfolderPrefix: String,
        val tryWriteFiles: (Resource, String, CommandSourceStack, Path, String, Path) -> Boolean,
    )

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
                        .then(argument("filePath", IdentifierArgument.id())
                        .executes {
                            extractText(
                            it.source,
                            IdentifierArgument.getId(it, "filePath"),
                            StringArgumentType.getString(it, "prefix"),
                            StringArgumentType.getString(it, "lang"),
                            dataType
                        ) }
                    )))
                .executes { ctx -> showHelp(ctx.source, dataType) }
            )
    }

    private fun extractText(source: CommandSourceStack, filePath: Identifier, prefix: String, lang: String, dataType: DataType): Int {
        val params: DataExtractionParams = dataType.params
        val adjustedPath = filePath.withPath("${params.dataRoot}/${filePath.path}")

        val resource = source.server.resourceManager.getResource(adjustedPath).getOrNull() ?: run {
            RuinsOfGrowsseth.LOGGER.info(
                "Available files: {}",
                source.server.resourceManager.listResources(params.dataRoot) { true }.keys.joinToString("\n")
            )
            source.sendFailure(Component.translatable("growsseth.commands.gdata.extract.not-found", adjustedPath.path))
            return 0
        }

        val generated = source.server.getWorldPath(LevelResource.GENERATED_DIR).normalize()
        val convertedDir = generated.resolve(params.extractedFolder)
        val outputFile = convertedDir.resolve(adjustedPath.namespace).resolve(adjustedPath.path)
        outputFile.parent.toFile().mkdirs()

        // create lang files under dialogue/places subfolder
        val langPrefix = params.langSubfolderPrefix
        val langDir = convertedDir.resolve(adjustedPath.namespace).resolve("lang/${lang}/${langPrefix}")
        langDir.toFile().mkdirs()

        if (!params.tryWriteFiles(resource, prefix, source, outputFile, langPrefix, langDir)) return 0

        source.sendSuccess({
            Component.translatable("growsseth.commands.gdata.extract.success", convertedDir.toString(), langDir.toString())
        }, true)

        return 1
    }

    private fun tryWriteDialogueFiles(resource: Resource, prefix: String, source: CommandSourceStack, outputFile: Path, langPrefix: String, langDir: Path): Boolean {
        val (keyObj, languageStringObj) = try {
            resource.openAsReader()
                .readText()
                .let { JSON.decodeFromString(JsonObject.serializer(), it) }
                .let { DialogueEntryConversion.extractKeysFromDialogueFile(it, prefix) }
        } catch (e: Exception) {
            source.sendFailure(Component.translatable("growsseth.commands.gdata.extract.parse-failure"))
            return false
        }
        outputFile.writeText(JSON.encodeToString(JsonObject.serializer(), keyObj))

        if (languageStringObj.isEmpty()) {
            source.sendFailure(Component.translatable("growsseth.commands.gdata.extract.no-lang"))
            return false
        }

        languageStringObj[langPrefix]!!.jsonObject.forEach { (name, subObj) ->
            val out = langDir.resolve("${name}.json")
            var obj = subObj
            if (out.exists()) {
                val existingObj = JSON.decodeFromString<JsonObject>(out.readText())
                obj = mergeJsonObjects(existingObj, subObj.jsonObject)
            }
            out.writeText(JSON.encodeToString(JsonElement.serializer(), obj))
        }

        return true
    }

    private fun tryWritePlacesFiles(resource: Resource, prefix: String, source: CommandSourceStack, outputFile: Path, langPrefix: String, langDir: Path): Boolean {
        val (keyArray, languageStringObj) = try {
            resource.openAsReader()
                .readText()
                .let { JSON.decodeFromString(JsonArray.serializer(), it) }
                .let { LocationEntryConversion.extractKeysFromPlacesFile(it, prefix) }
        } catch (e: Exception) {
            source.sendFailure(Component.translatable("growsseth.commands.gdata.extract.parse-failure"))
            return false
        }
        outputFile.writeText(JSON.encodeToString(JsonArray.serializer(), keyArray))

        if (languageStringObj.isEmpty()) {
            source.sendFailure(Component.translatable("growsseth.commands.gdata.extract.no-lang"))
            return false
        }

        val outLang = langDir.resolve("${prefix}.json")
        var obj = languageStringObj
        if (outLang.exists()) {
            val existingObj = JSON.decodeFromString<JsonObject>(outLang.readText())
            obj = mergeJsonObjects(existingObj, languageStringObj)
        }
        outLang.writeText(JSON.encodeToString(JsonElement.serializer(), obj))

        return true
    }

    private fun showHelp(source: CommandSourceStack, dataType: DataType): Int {
        source.sendSuccess({
            Component.translatable("growsseth.commands.gdata.${dataType.helpId}.extract.help")
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