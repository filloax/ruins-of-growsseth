package com.ruslan.growsseth.item

import com.ruslan.growsseth.GrowssethBannerPatterns
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.sound.GrowssethSounds
import com.ruslan.growsseth.utils.resLoc
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.*
import net.minecraft.world.item.armortrim.TrimPattern

object GrowssethItems {
	val all = mutableMapOf<ResourceLocation, Item>()
	val noAutogenerateItems = mutableSetOf<Item>()

	val RESEARCHER_SPAWN_EGG = make("researcher_spawn_egg", SpawnEggItem(GrowssethEntities.RESEARCHER, 16446952, 14524294, defaultBuilder()), autoGenerateJson = false)
	val ZOMBIE_RESEARCHER_SPAWN_EGG = make("zombie_researcher_spawn_egg", SpawnEggItem(GrowssethEntities.ZOMBIE_RESEARCHER, 16446952, 7115863, defaultBuilder()), autoGenerateJson = false)
	val RESEARCHER_HORN = make("researcher_horn", ResearcherHornItem(defaultBuilder().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), GrowssethTags.RESEARCHER_HORNS), autoGenerateJson = false)
	// 5 attack (2 less than diamond sword), 2,5 attack speed (sword speed + 0,9) (sword modifiers: 3 and -2.4):
	val RESEARCHER_DAGGER = make("researcher_dagger",
		ResearcherDaggerItem(Tiers.DIAMOND, 1, -1.5F, Item.Properties().rarity(Rarity.EPIC)),
		autoGenerateJson = false,
	)

	val GROWSSETH_BANNER_PATTERN = make("growsseth_banner_pattern", AutoBannerItem(GrowssethBannerPatterns.GROWSSETH.tag, defaultBuilder().rarity(Rarity.RARE)))
	val GROWSSETH_ARMOR_TRIM = make("growsseth_trim_template", SmithingTemplateItem.createArmorTrimTemplate(TrimPatterns.GROWSSETH))
	val GROWSSETH_POTTERY_SHERD = make("growsseth_pottery_sherd", defaultItem())

	// Custom discs
	val DISC_SEGA_DI_NIENTE = make("disc_sega_di_niente",
		RecordItem(6, GrowssethSounds.DISC_SEGA_DI_NIENTE, FabricItemSettings().maxCount(1), 119))
	val DISC_GIORGIO_CUBETTI = make("disc_giorgio_cubetti",
		RecordItem(6, GrowssethSounds.DISC_GIORGIO_CUBETTI, FabricItemSettings().maxCount(1), 161))
	val DISC_GIORGIO_LOFI = make("disc_giorgio_lofi",
		RecordItem(6, GrowssethSounds.DISC_GIORGIO_LOFI, FabricItemSettings().maxCount(1), 295))
	val DISC_GIORGIO_LOFI_INST = make("disc_giorgio_lofi_inst",
		RecordItem(6, GrowssethSounds.DISC_GIORGIO_LOFI_INST, FabricItemSettings().maxCount(1), 295))
	val DISC_GIORGIO_FINDING_HOME = make("disc_giorgio_finding_home",
		RecordItem(6, GrowssethSounds.DISC_GIORGIO_FINDING_HOME, FabricItemSettings().maxCount(1), 186))
	val DISC_BINOBINOOO = make("disc_binobinooo",
		RecordItem(6, GrowssethSounds.DISC_BINOBINOOO, FabricItemSettings().maxCount(1), 98))
	val DISC_BINOBINOOO_INST = make("disc_binobinooo_inst",
		RecordItem(6, GrowssethSounds.DISC_BINOBINOOO_INST, FabricItemSettings().maxCount(1), 98))
	val DISC_PADRE_MAMMONK = make("disc_padre_mammonk",
		RecordItem(6, GrowssethSounds.DISC_PADRE_MAMMONK, FabricItemSettings().maxCount(1), 74))
	val DISC_ABBANDONATI = make("disc_abbandonati",
		RecordItem(6, GrowssethSounds.DISC_ABBANDONATI, FabricItemSettings().maxCount(1), 145))
	val DISC_MISSIVA_NELL_OMBRA = make("disc_missiva_nell_ombra",
		RecordItem(6, GrowssethSounds.DISC_MISSIVA_NELL_OMBRA, FabricItemSettings().maxCount(1), 329))

	// Vanilla ambience discs
	val DISC_MICE_ON_VENUS = make("disc_mice_on_venus",
		RecordItem(6, GrowssethSounds.MUSIC_MICE_ON_VENUS, FabricItemSettings().maxCount(1), 280))
	val DISC_INFINITE_AMETHYST = make("disc_infinite_amethyst",
		RecordItem(6, GrowssethSounds.MUSIC_INFINITE_AMETHYST, FabricItemSettings().maxCount(1), 271))
	val DISC_LABYRINTHINE = make("disc_labyrinthine",
		RecordItem(6, GrowssethSounds.MUSIC_LABYRINTHINE, FabricItemSettings().maxCount(1), 324))

	val DISCS_TO_VOCALS = mutableMapOf(
		DISC_GIORGIO_LOFI_INST to DISC_GIORGIO_LOFI,
		DISC_BINOBINOOO_INST to DISC_BINOBINOOO,
	)

	val DISCS_WITH_VOCALS = listOf(
		DISC_ABBANDONATI,
		DISC_BINOBINOOO,
		DISC_GIORGIO_CUBETTI,
		DISC_GIORGIO_LOFI,
		DISC_MISSIVA_NELL_OMBRA,
		DISC_PADRE_MAMMONK,
		DISC_SEGA_DI_NIENTE
	)

	val DISCS_INSTRUMENTAL = listOf(
		DISC_BINOBINOOO_INST,
		DISC_GIORGIO_FINDING_HOME,
		DISC_GIORGIO_LOFI_INST
	)

	val DISCS_VANILLA = listOf(
		DISC_MICE_ON_VENUS,
		DISC_INFINITE_AMETHYST,
		DISC_LABYRINTHINE
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