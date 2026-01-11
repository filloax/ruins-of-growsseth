package com.ruslan.growsseth.item

import com.filloax.fxlib.api.registration.RegistryDelegate
import com.filloax.fxlib.api.registration.registryDelegate
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.compat.ModCompatChecker
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.utils.resLoc
import com.teamremastered.endrem.item.EREnderEye
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.equipment.trim.TrimPattern
import net.minecraft.world.level.block.entity.DecoratedPotPattern

object GrowssethItems {
	val all = mutableMapOf<Identifier, Item>()
	private val allInitializers = mutableMapOf<Identifier, () -> Item>()
	val noAutogenerateItems = mutableSetOf<Item>()

    // TODO: Improve item creation to avoid duplicating the ids

	val RESEARCHER_SPAWN_EGG by make("researcher_spawn_egg",
		{ SpawnEggItem(defaultBuilder("researcher_spawn_egg").spawnEgg(GrowssethEntities.RESEARCHER)) }, autoGenerateJson = false)
	val ZOMBIE_RESEARCHER_SPAWN_EGG by make("zombie_researcher_spawn_egg",
		{ SpawnEggItem(defaultBuilder("zombie_researcher_spawn_egg").spawnEgg(GrowssethEntities.ZOMBIE_RESEARCHER)) }, autoGenerateJson = false)
	val RESEARCHER_HORN by make("researcher_horn", {
		ResearcherHornItem(
			defaultBuilder("researcher_horn").rarity(Rarity.EPIC).stacksTo(1).fireResistant()
		)
	}, autoGenerateJson = false)
	// 5 attack (2 less than diamond sword), 2,5 attack speed (sword speed + 0,9) (sword modifiers: 3 and -2.4):
	val RESEARCHER_DAGGER by make(
		"researcher_dagger",
		{ AbstractResearcherDaggerItem.create() },  // id set inside
		autoGenerateJson = false,
	)
	val RUINS_MAP by make("ruins_map", { MapItem(defaultBuilder("ruins_map").stacksTo(1)) }, autoGenerateJson = false)

	val GROWSSETH_BANNER_PATTERN by make("growsseth_banner_pattern", {
		AutoBannerItem(defaultBuilder("growsseth_banner_pattern").rarity(Rarity.RARE))
	})
	val GROWSSETH_ARMOR_TRIM: SmithingTemplateItem by make("growsseth_trim_template",
		{ SmithingTemplateItem.createArmorTrimTemplate(defaultBuilder("growsseth_trim_template")) })
	val GROWSSETH_POTTERY_SHERD by make("growsseth_pottery_sherd", { defaultItem("growsseth_pottery_sherd") })
	val FRAGMENT_BALLATA_DEL_RESPAWN by make("fragment_ballata_del_respawn", { DiscFragmentItem(defaultBuilder("growsseth_pottery_sherd")) })

    val ENDREM_GROWSSETH_EYE by makeEndRemasteredEye("growsseth_eye", defaultBuilder("growsseth_eye").rarity(Rarity.EPIC))

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

	private inline fun <reified T : Item> make(name: String, noinline supplier: () -> T, autoGenerateJson: Boolean = true) = registryDelegate(resLoc(name)) {
		if (allInitializers.containsKey(id)) {
			throw IllegalArgumentException("Item $id already registered!")
		}

		allInitializers[id] = {
			val item = supplier()
			all[id] = item
			if (!autoGenerateJson) noAutogenerateItems.add(item)
			init(item)
			item
		}
	}

	private fun makeDisc(
		name: String, jukeboxSong: ResourceKey<JukeboxSong>,
		properties: Properties = defaultBuilder(name),
	) = make(name, {
			Item(
				properties.stacksTo(1).jukeboxPlayable(jukeboxSong),
			)
		})

    private fun makeEndRemasteredEye(
        name: String, properties: Properties = defaultBuilder(name),
    ): RegistryDelegate<Item> {
        if (RuinsOfGrowsseth.modCompat.isEndRemasteredLoaded) {
            try {
                RuinsOfGrowsseth.LOGGER.info("Registering End Remastered eye item $name")
                return make(name, { EREnderEye(properties) })
            } catch (e: Throwable) {
                RuinsOfGrowsseth.LOGGER.error("Couldn't register End Remastered eye item $name, falling back to generic item that won't work with portal", e)
            }
        }
        // Blank item so it doesn't get deleted if you already have it
        return make(name, { Item(properties) })
    }

	fun registerItems(registrator: (Identifier, Item) -> Unit) {
		allInitializers.forEach{
			registrator(it.key, it.value())
		}
	}

	private fun defaultItem(itemId: String): Item = Item(defaultBuilder(itemId))

	private fun defaultBuilder(itemId: String) =
        Properties().setId(ResourceKey.create<Item>(Registries.ITEM, resLoc(itemId)))

	object TrimPatterns {
		val GROWSSETH_TRIM_PATTERN: ResourceKey<TrimPattern> = ResourceKey.create(Registries.TRIM_PATTERN, resLoc("growsseth_trim_pattern"))

		fun bootstrap(ctx: BootstrapContext<TrimPattern>) {
			ctx.register(GROWSSETH_TRIM_PATTERN,
				TrimPattern(GROWSSETH_TRIM_PATTERN.identifier(), Component.translatable("aaaaa"), false)
			)
		}
	}

	object SherdPatterns {
		val GROWSSETH = create("growsseth_pottery_pattern")

		val sherdToPattern by lazy { mapOf(
			GROWSSETH_POTTERY_SHERD to GROWSSETH
		) }

		private fun create(name: String) =
			Pair(ResourceKey.create(Registries.DECORATED_POT_PATTERN, resLoc(name)), DecoratedPotPattern(resLoc(name)))


		fun registerPotPatterns(registrator: (Identifier, DecoratedPotPattern) -> Unit) {
			registrator(GROWSSETH.first.identifier(), GROWSSETH.second)
		}
	}
}