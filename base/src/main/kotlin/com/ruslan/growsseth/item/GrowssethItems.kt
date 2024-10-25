package com.ruslan.growsseth.item

import com.filloax.fxlib.api.registration.registryDelegate
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.sound.GrowssethSounds
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.armortrim.TrimPattern
import net.minecraft.world.level.block.entity.DecoratedPotPattern

object GrowssethItems {
	val all = mutableMapOf<ResourceLocation, Item>()
	private val allInitializers = mutableMapOf<ResourceLocation, () -> Item>()
	val noAutogenerateItems = mutableSetOf<Item>()

	val RESEARCHER_SPAWN_EGG by make("researcher_spawn_egg",
		{ SpawnEggItem(GrowssethEntities.RESEARCHER, 16446952, 14524294, defaultBuilder()) }, autoGenerateJson = false)
	val ZOMBIE_RESEARCHER_SPAWN_EGG by make("zombie_researcher_spawn_egg",
		{ SpawnEggItem(GrowssethEntities.ZOMBIE_RESEARCHER, 16446952, 7115863, defaultBuilder()) }, autoGenerateJson = false)
	val RESEARCHER_HORN by make("researcher_horn", {
		ResearcherHornItem(
			defaultBuilder().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), GrowssethTags.RESEARCHER_HORNS
		)
	}, autoGenerateJson = false)
	// 5 attack (2 less than diamond sword), 2,5 attack speed (sword speed + 0,9) (sword modifiers: 3 and -2.4):
	val RESEARCHER_DAGGER by make(
		"researcher_dagger",
		{ AbstractResearcherDaggerItem.create() },
		autoGenerateJson = false,
	)
	val RUINS_MAP by make("ruins_map", { MapItem(defaultBuilder().stacksTo(1)) }, autoGenerateJson = false)

	val GROWSSETH_BANNER_PATTERN by make("growsseth_banner_pattern", {
		AutoBannerItem(
			com.ruslan.growsseth.GrowssethBannerPatterns.GROWSSETH.tag, defaultBuilder().rarity(Rarity.RARE)
		)
	})
	val GROWSSETH_ARMOR_TRIM by make("growsseth_trim_template",
		{ SmithingTemplateItem.createArmorTrimTemplate(TrimPatterns.GROWSSETH) })
	val GROWSSETH_POTTERY_SHERD by make("growsseth_pottery_sherd", { defaultItem() })
	val FRAGMENT_BALLATA_DEL_RESPAWN by make("fragment_ballata_del_respawn", { DiscFragmentItem(Properties()) })

	// Custom discs
	val DISC_SEGA_DI_NIENTE 		by makeDisc("disc_sega_di_niente", GrowssethJukeboxSongs.SEGA_DI_NIENTE)
	val DISC_GIORGIO_CUBETTI 		by makeDisc("disc_giorgio_cubetti", GrowssethJukeboxSongs.GIORGIO_CUBETTI)
	val DISC_GIORGIO_LOFI 			by makeDisc("disc_giorgio_lofi", GrowssethJukeboxSongs.GIORGIO_LOFI)
	val DISC_GIORGIO_LOFI_INST 		by makeDisc("disc_giorgio_lofi_inst", GrowssethJukeboxSongs.GIORGIO_LOFI_INST)
	val DISC_GIORGIO_FINDING_HOME 	by makeDisc("disc_giorgio_finding_home", GrowssethJukeboxSongs.GIORGIO_FINDING_HOME)
	val DISC_GIORGIO_8BIT		 	by makeDisc("disc_giorgio_8bit", GrowssethJukeboxSongs.GIORGIO_8BIT)
	val DISC_BINOBINOOO 			by makeDisc("disc_binobinooo", GrowssethJukeboxSongs.BINOBINOOO)
	val DISC_PADRE_MAMMONK 			by makeDisc("disc_padre_mammonk", GrowssethJukeboxSongs.PADRE_MAMMONK)
	val DISC_ABBANDONATI 			by makeDisc("disc_abbandonati", GrowssethJukeboxSongs.ABBANDONATI)
	val DISC_MISSIVA_NELL_OMBRA 	by makeDisc("disc_missiva_nell_ombra", GrowssethJukeboxSongs.MISSIVA_NELL_OMBRA)
	val DISC_OURSTEPS 				by makeDisc("disc_oursteps", GrowssethJukeboxSongs.OURSTEPS)
	val DISC_ODI_ET_AMO 			by makeDisc("disc_odi_et_amo", GrowssethJukeboxSongs.ODI_ET_AMO)
	val DISC_ANCORA_QUI 			by makeDisc("disc_ancora_qui", GrowssethJukeboxSongs.ANCORA_QUI)
	val DISC_BALLATA_DEL_RESPAWN 	by makeDisc("disc_ballata_del_respawn", GrowssethJukeboxSongs.BALLATA_DEL_RESPAWN)
	val DISC_CACO_CACO 				by makeDisc("disc_caco_caco", GrowssethJukeboxSongs.CACO_CACO)
	val DISC_PESCI_STRANI 			by makeDisc("disc_pesci_strani", GrowssethJukeboxSongs.PESCI_STRANI)


	val DISCS_TO_VOCALS by lazy { mutableMapOf(
		Items.MUSIC_DISC_PIGSTEP to DISC_OURSTEPS,
		DISC_GIORGIO_LOFI_INST to DISC_GIORGIO_LOFI
	) }

	val FRAGMENTS_TO_DISCS by lazy { mutableMapOf(
		FRAGMENT_BALLATA_DEL_RESPAWN to DISC_BALLATA_DEL_RESPAWN
	) }

	val DISCS_ORDERED by lazy { listOf(
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
	) }

	private fun <T : Item> make(hashName: String, supplier: () -> T, autoGenerateJson: Boolean = true) = registryDelegate {
		val resourceLocation = resLoc(hashName)
		if (allInitializers.containsKey(resourceLocation)) {
			throw IllegalArgumentException("Item $hashName already registered!")
		}

		allInitializers[resourceLocation] = {
			val item = supplier()
			all[resourceLocation] = item
			if (!autoGenerateJson) noAutogenerateItems.add(item)
			init(item)
			item
		}
	}

	private fun makeDisc(
		name: String, jukeboxSong: ResourceKey<JukeboxSong>,
		properties: Properties = Properties(),
	) = make(name, {
			Item(
				properties.stacksTo(1).jukeboxPlayable(jukeboxSong),
			)
		})

	fun registerItems(registrator: (ResourceLocation, Item) -> Unit) {
		allInitializers.forEach{
			registrator(it.key, it.value())
		}
	}

	private fun defaultItem(): Item = Item(defaultBuilder())

	private fun defaultBuilder() = Item.Properties()

	object TrimPatterns {
		val GROWSSETH: ResourceKey<TrimPattern> = ResourceKey.create(Registries.TRIM_PATTERN, resLoc("growsseth"))
	}

	object SherdPatterns {
		val GROWSSETH = create("growsseth_pottery_pattern")

		val sherdToPattern by lazy { mapOf(
			GROWSSETH_POTTERY_SHERD to GROWSSETH
		) }

		private fun create(name: String) =
			Pair(ResourceKey.create(Registries.DECORATED_POT_PATTERN, resLoc(name)), DecoratedPotPattern(resLoc(name)))


		fun registerPotPatterns(registrator: (ResourceLocation, DecoratedPotPattern) -> Unit) {
			registrator(GROWSSETH.first.location(), GROWSSETH.second)
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