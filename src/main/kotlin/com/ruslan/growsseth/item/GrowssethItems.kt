package com.ruslan.growsseth.item

import com.ruslan.growsseth.GrowssethBannerPatterns
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.sound.GrowssethSounds
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.armortrim.TrimPattern

object GrowssethItems {
	val all = mutableMapOf<ResourceLocation, Item>()
	val noAutogenerateItems = mutableSetOf<Item>()

	val RESEARCHER_SPAWN_EGG = make("researcher_spawn_egg", SpawnEggItem(GrowssethEntities.RESEARCHER, 16446952, 14524294, defaultBuilder()), autoGenerateJson = false)
	val ZOMBIE_RESEARCHER_SPAWN_EGG = make("zombie_researcher_spawn_egg", SpawnEggItem(GrowssethEntities.ZOMBIE_RESEARCHER, 16446952, 7115863, defaultBuilder()), autoGenerateJson = false)
	val RESEARCHER_HORN = make("researcher_horn", ResearcherHornItem(defaultBuilder().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), GrowssethTags.RESEARCHER_HORNS), autoGenerateJson = false)
	// 5 attack (2 less than diamond sword), 2,5 attack speed (sword speed + 0,9) (sword modifiers: 3 and -2.4):
	val RESEARCHER_DAGGER = make("researcher_dagger",
		ResearcherDaggerItem(Tiers.DIAMOND, Properties()
			.rarity(Rarity.EPIC)
			.attributes(SwordItem.createAttributes(Tiers.DIAMOND, 1, -1.5F)),
		),
		autoGenerateJson = false,
	)

	val GROWSSETH_BANNER_PATTERN = make("growsseth_banner_pattern", AutoBannerItem(GrowssethBannerPatterns.GROWSSETH.tag, defaultBuilder().rarity(Rarity.RARE)))
	val GROWSSETH_ARMOR_TRIM = make("growsseth_trim_template", SmithingTemplateItem.createArmorTrimTemplate(TrimPatterns.GROWSSETH))
	val GROWSSETH_POTTERY_SHERD = make("growsseth_pottery_sherd", defaultItem())
	val FRAGMENT_BALLATA_DEL_RESPAWN = make("fragment_ballata_del_respawn", DiscFragmentItem(Properties()))

	// Custom discs
	val DISC_SEGA_DI_NIENTE 		= makeDisc("disc_sega_di_niente", GrowssethSounds.DISC_SEGA_DI_NIENTE, 119, listOf(
		"Vocals & Lyrics - Cydonia",
		"Mixing - Guber",
		"Cover of 'Labyrinthine', by Lena Raine"
	))
	val DISC_GIORGIO_CUBETTI 		= makeDisc("disc_giorgio_cubetti", GrowssethSounds.DISC_GIORGIO_CUBETTI, 161, listOf(
		"Vocals, Mix & Master - Singalek",
		"Lyrics - Singalek, Cydonia",
		"Cover of 'If I Could, I Would' - MINDME"
	),
		ytChannel = "SingalekSMW"
	)
	val DISC_GIORGIO_LOFI 			= makeDisc("disc_giorgio_lofi", GrowssethSounds.DISC_GIORGIO_LOFI, 295, listOf(
		"Instrumental, Vocals, Mix, Master - Singalek",
		"Lyrics - Singalek, Cydonia"
	),
		ytChannel = "SingalekSMW"
	)
	val DISC_GIORGIO_LOFI_INST 		= makeDisc("disc_giorgio_lofi_inst", GrowssethSounds.DISC_GIORGIO_LOFI_INST, 295,
		ytChannel = "SingalekSMW"
	)
	val DISC_GIORGIO_FINDING_HOME 	= makeDisc("disc_giorgio_finding_home", GrowssethSounds.DISC_GIORGIO_FINDING_HOME, 186,
		ytChannel = "HunterProduction"
	)
	val DISC_GIORGIO_8BIT		 	= makeDisc("disc_giorgio_8bit", GrowssethSounds.DISC_GIORGIO_8BIT, 152, listOf(
		"Music - Hawkelele",
		"Based on 'Giorgio Cubetti' by Singalek",
		"Cover of 'If I Could, I Would' - MINDME"
	), ytChannel = "IlCoroDiMammonk")
	val DISC_BINOBINOOO 			= makeDisc("disc_binobinooo", GrowssethSounds.DISC_BINOBINOOO, 98,
		ytChannel = "SingalekSMW"
	)
	val DISC_PADRE_MAMMONK 			= makeDisc("disc_padre_mammonk", GrowssethSounds.DISC_PADRE_MAMMONK, 74, listOf(
		"Vocals - Emoon LeStrange, HunterProduction, KalarFenrir, Singalek",
		"Chorus composed by Emoon LeStrange",
		"Cover of 'All is Found' - Evan Rachel Wood"
	), ytChannel = "IlCoroDiMammonk")
	val DISC_ABBANDONATI 			= makeDisc("disc_abbandonati", GrowssethSounds.DISC_ABBANDONATI, 145, listOf(
		"Lyrics - Cydonia",
		"Vocals - Ako",
		"Music, Mix & Master - R-E-M"
	), ytChannel = "rem.producer")
	val DISC_MISSIVA_NELL_OMBRA 	= makeDisc("disc_missiva_nell_ombra", GrowssethSounds.DISC_MISSIVA_NELL_OMBRA, 329, listOf(
		"Vocals - Singalek",
		"Music - Hawkelele, Monnui, AndreWharn, HunterProduction, 't Hooft",
		"Cover of 'Sleepsong' - Secret Garden"
	), ytChannel = "IlCoroDiMammonk")
	val DISC_OURSTEPS 				= makeDisc("disc_oursteps", GrowssethSounds.DISC_OURSTEPS, 154, listOf(
		"Vocals - Singalek, Blessing Of No One",
		"Music - Lena Raine, IridioSound, Avage",
		"Cover of 'Pigstep' - Lena Raine"
	), ytChannel = "IlCoroDiMammonk")
	val DISC_ODI_ET_AMO 			= makeDisc("disc_odi_et_amo", GrowssethSounds.DISC_ODI_ET_AMO, 191, listOf(
		"Vocals - Jova117, Emoon LeStrange",
		"Music - Hawkelele, Jova117"
	), ytChannel = "IlCoroDiMammonk")
	val DISC_ANCORA_QUI 			= makeDisc("disc_ancora_qui", GrowssethSounds.DISC_ANCORA_QUI, 145, listOf(
		"Vocals - Avage, Emoon LeStrange, Singalek",
		"Music - Avage"
	), ytChannel = "IlCoroDiMammonk")
	val DISC_BALLATA_DEL_RESPAWN 	= makeDisc("disc_ballata_del_respawn", GrowssethSounds.DISC_BALLATA_DEL_RESPAWN, 266, listOf(
		"Vocals - Monnui",
		"Music - Monnui, Singalek, IridioSound"
	), ytChannel = "IlCoroDiMammonk")
	val DISC_CACO_CACO 				= makeDisc("disc_caco_caco", GrowssethSounds.DISC_CACO_CACO, 145, listOf(
		"Vocals - Blessing Of No One, HunterProduction, KalarFenrir, Singalek",
		"Music - HunterProduction"
	), ytChannel = "IlCoroDiMammonk")
	val DISC_PESCI_STRANI 			= makeDisc("disc_pesci_strani", GrowssethSounds.DISC_PESCI_STRANI, 157, listOf(
		"Vocals - Emoon LeStrange, Singalek",
		"Music - Hawkelele, Emoon LeStrange, Monnui, Singalek"
	), ytChannel = "IlCoroDiMammonk")


	val DISCS_TO_VOCALS = mutableMapOf(
		Items.MUSIC_DISC_PIGSTEP to DISC_OURSTEPS,
		DISC_GIORGIO_LOFI_INST to DISC_GIORGIO_LOFI
	)

	val FRAGMENTS_TO_DISCS = mutableMapOf(
		FRAGMENT_BALLATA_DEL_RESPAWN to DISC_BALLATA_DEL_RESPAWN
	)

	val DISCS_ORDERED = listOf(
		DISC_OURSTEPS,
		DISC_SEGA_DI_NIENTE,
		DISC_GIORGIO_CUBETTI,
		DISC_GIORGIO_LOFI_INST,
		DISC_GIORGIO_LOFI,
		DISC_GIORGIO_FINDING_HOME,
		DISC_GIORGIO_8BIT,
		DISC_ANCORA_QUI,
		DISC_BINOBINOOO,
		DISC_PADRE_MAMMONK,
		DISC_ABBANDONATI,
		DISC_MISSIVA_NELL_OMBRA,
		DISC_CACO_CACO,
		DISC_PESCI_STRANI,
		DISC_ODI_ET_AMO,
		DISC_BALLATA_DEL_RESPAWN
	)

	val RUINS_MAP = make("ruins_map", MapItem(defaultBuilder().stacksTo(1)), autoGenerateJson = false)

	private fun <T:Item> make(hashName: String, item: T, autoGenerateJson: Boolean = true): T {
		val resourceLocation = resLoc(hashName)
		if (all.containsKey(resourceLocation)) {
			throw IllegalArgumentException("Item $hashName already registered!")
		}

		if (!autoGenerateJson) noAutogenerateItems.add(item)

		all[resourceLocation] = item
		return item
	}

	private fun makeDisc(
		name: String, sound: SoundEvent, lengthInSeconds: Int,
		authors: List<String> = listOf(),
		ytChannel: String? = null,
		properties: Properties = Properties(),
	) =
		make(name, RecordCreditsItem(
			6, sound, properties.stacksTo(1), lengthInSeconds,
			authors, listOfNotNull(ytChannel?.let { Component.literal("YT: @$it").withStyle(ChatFormatting.RED) })
		))

	fun registerItems(registrator: (ResourceLocation, Item) -> Unit) {
		all.forEach{
			registrator(it.key, it.value)
		}
		SherdPatterns.registerSherds()
	}

	private fun defaultItem(): Item = Item(defaultBuilder())

	private fun defaultBuilder() = Item.Properties()

	object TrimPatterns {
		val GROWSSETH: ResourceKey<TrimPattern> = ResourceKey.create(Registries.TRIM_PATTERN, resLoc("growsseth"))
	}

	object SherdPatterns {
		val GROWSSETH = resLoc("growsseth_pottery_pattern")

		fun registerSherds() {
			// Use the library, pattern pngs must be located in assets/<modid>/textures/entity/decorated_pot/<id>.png
			PotPatternRegistry.register(GROWSSETH_POTTERY_SHERD, GROWSSETH)
		}
	}

	object Instruments {
		val all = mutableMapOf<ResourceLocation, Instrument>()

		val RESEARCHER_HORN = make("researcher_horn", GrowssethSounds.RESEARCHER_HORN_SOUND)

		fun make(name: String, sound: Holder<SoundEvent>): Pair<ResourceLocation, Instrument> {
			val loc = resLoc(name)
			val item = Instrument(sound, 140, 256.0f)
			all[loc] = item
			return Pair(loc, item)
		}

		fun registerInstruments(registrator: (ResourceLocation, Instrument) -> Unit) {
			all.forEach{
				registrator(it.key, it.value)
			}
		}
	}
}