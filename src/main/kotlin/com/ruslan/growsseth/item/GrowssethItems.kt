package com.ruslan.growsseth.item

import com.ruslan.growsseth.GrowssethBannerPatterns
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.sound.GrowssethSounds
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.*
import net.minecraft.world.item.armortrim.TrimPattern
import net.minecraft.world.item.component.ItemAttributeModifiers

object GrowssethItems {
	val all = mutableMapOf<ResourceLocation, Item>()
	val noAutogenerateItems = mutableSetOf<Item>()

	val RESEARCHER_SPAWN_EGG = make("researcher_spawn_egg", SpawnEggItem(GrowssethEntities.RESEARCHER, 16446952, 14524294, defaultBuilder()), autoGenerateJson = false)
	val ZOMBIE_RESEARCHER_SPAWN_EGG = make("zombie_researcher_spawn_egg", SpawnEggItem(GrowssethEntities.ZOMBIE_RESEARCHER, 16446952, 7115863, defaultBuilder()), autoGenerateJson = false)
	val RESEARCHER_HORN = make("researcher_horn", ResearcherHornItem(defaultBuilder().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), GrowssethTags.RESEARCHER_HORNS), autoGenerateJson = false)
	// 5 attack (2 less than diamond sword), 2,5 attack speed (sword speed + 0,9) (sword modifiers: 3 and -2.4):
	val RESEARCHER_DAGGER = make("researcher_dagger",
		ResearcherDaggerItem(Tiers.DIAMOND, Item.Properties()
			.rarity(Rarity.EPIC)
			.attributes(SwordItem.createAttributes(Tiers.DIAMOND, 1, -1.5F)),
		),
		autoGenerateJson = false,
	)

	val GROWSSETH_BANNER_PATTERN = make("growsseth_banner_pattern", AutoBannerItem(GrowssethBannerPatterns.GROWSSETH.tag, defaultBuilder().rarity(Rarity.RARE)))
	val GROWSSETH_ARMOR_TRIM = make("growsseth_trim_template", SmithingTemplateItem.createArmorTrimTemplate(TrimPatterns.GROWSSETH))
	val GROWSSETH_POTTERY_SHERD = make("growsseth_pottery_sherd", defaultItem())

	// Custom discs
	val DISC_SEGA_DI_NIENTE = make("disc_sega_di_niente",
		RecordItem(6, GrowssethSounds.DISC_SEGA_DI_NIENTE, Item.Properties().stacksTo(1), 119))
	val DISC_GIORGIO_CUBETTI = make("disc_giorgio_cubetti",
		RecordItem(6, GrowssethSounds.DISC_GIORGIO_CUBETTI, Item.Properties().stacksTo(1), 161))
	val DISC_GIORGIO_LOFI = make("disc_giorgio_lofi",
		RecordItem(6, GrowssethSounds.DISC_GIORGIO_LOFI, Item.Properties().stacksTo(1), 295))
	val DISC_GIORGIO_LOFI_INST = make("disc_giorgio_lofi_inst",
		RecordItem(6, GrowssethSounds.DISC_GIORGIO_LOFI_INST, Item.Properties().stacksTo(1), 295))
	val DISC_GIORGIO_FINDING_HOME = make("disc_giorgio_finding_home",
		RecordItem(6, GrowssethSounds.DISC_GIORGIO_FINDING_HOME, Item.Properties().stacksTo(1), 186))
	val DISC_BINOBINOOO = make("disc_binobinooo",
		RecordItem(6, GrowssethSounds.DISC_BINOBINOOO, Item.Properties().stacksTo(1), 98))
	val DISC_PADRE_MAMMONK = make("disc_padre_mammonk",
		RecordItem(6, GrowssethSounds.DISC_PADRE_MAMMONK, Item.Properties().stacksTo(1), 74))
	val DISC_ABBANDONATI = make("disc_abbandonati",
		RecordItem(6, GrowssethSounds.DISC_ABBANDONATI, Item.Properties().stacksTo(1), 145))
	val DISC_MISSIVA_NELL_OMBRA = make("disc_missiva_nell_ombra",
		RecordItem(6, GrowssethSounds.DISC_MISSIVA_NELL_OMBRA, Item.Properties().stacksTo(1), 329))

	val DISC_OURSTEPS = make("disc_oursteps",
		RecordItem(6, GrowssethSounds.DISC_OURSTEPS, Item.Properties().stacksTo(1), 154))
	val DISC_ODI_ET_AMO = make("disc_odi_et_amo",
		RecordItem(6, GrowssethSounds.DISC_ODI_ET_AMO, Item.Properties().stacksTo(1), 191))
	val DISC_ANCORA_QUI = make("disc_ancora_qui",
		RecordItem(6, GrowssethSounds.DISC_ANCORA_QUI, Item.Properties().stacksTo(1), 145))
	val DISC_BALLATA_DEL_RESPAWN = make("disc_ballata_del_respawn",
		RecordItem(6, GrowssethSounds.DISC_BALLATA_DEL_RESPAWN, Item.Properties().stacksTo(1), 266))
	val DISC_CACO_CACO = make("disc_caco_caco",
		RecordItem(6, GrowssethSounds.DISC_CACO_CACO, Item.Properties().stacksTo(1), 145))
	val DISC_PESCI_STRANI = make("disc_pesci_strani",
		RecordItem(6, GrowssethSounds.DISC_PESCI_STRANI, Item.Properties().stacksTo(1), 0))	// SISTEMARE DURATA!!!!!!


	val DISCS_TO_VOCALS = mutableMapOf(
		DISC_GIORGIO_LOFI_INST to DISC_GIORGIO_LOFI
	)

	val DISCS_WITH_VOCALS = listOf(
		DISC_ABBANDONATI,
		DISC_BINOBINOOO,
		DISC_GIORGIO_CUBETTI,
		DISC_GIORGIO_LOFI,
		DISC_MISSIVA_NELL_OMBRA,
		DISC_PADRE_MAMMONK,
		DISC_SEGA_DI_NIENTE,
		DISC_OURSTEPS,
		DISC_ODI_ET_AMO,
		DISC_ANCORA_QUI,
		DISC_BALLATA_DEL_RESPAWN,
		DISC_CACO_CACO,
		DISC_PESCI_STRANI
	)

	val DISCS_INSTRUMENTAL = listOf(
		DISC_GIORGIO_FINDING_HOME,
		DISC_GIORGIO_LOFI_INST
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