package com.ruslan.growsseth.events;

import com.ruslan.growsseth.config.GrowssethConfig;
import com.ruslan.growsseth.item.GrowssethItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;

public class ModifyLootTableEvents {
    private static final ResourceLocation OCEAN_RUIN_COLD_LOOT = BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY;
    private static final ResourceLocation OCEAN_RUIN_WARM_LOOT = BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY;
    private static final ResourceLocation RUINED_PORTAL_LOOT = BuiltInLootTables.RUINED_PORTAL;
    private static final ResourceLocation ANCIENT_CITY_LOOT = BuiltInLootTables.ANCIENT_CITY;
    private static final ResourceLocation STRONGHOLD_LOOT = BuiltInLootTables.STRONGHOLD_CORRIDOR;
    private static final ResourceLocation DUNGEON_LOOT = BuiltInLootTables.SIMPLE_DUNGEON;
    private static final ResourceLocation MANSION_LOOT = BuiltInLootTables.WOODLAND_MANSION;

    public static void init() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (GrowssethConfig.modLootInVanillaStructures && source.isBuiltin()) {
                LootPool.Builder poolBuilder = LootPool.lootPool();
                if (STRONGHOLD_LOOT.equals(id)) {
                    poolBuilder
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_BINOBINOOO()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_BINOBINOOO_INST()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_GIORGIO_CUBETTI()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_GIORGIO_FINDING_HOME()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_GIORGIO_LOFI()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_GIORGIO_LOFI_INST()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_INFINITE_AMETHYST()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_LABYRINTHINE()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_MICE_ON_VENUS()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_PADRE_MAMMONK()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_SEGA_DI_NIENTE()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getRESEARCHER_DAGGER()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getRESEARCHER_HORN()));
                }
                else if (RUINED_PORTAL_LOOT.equals(id)) {
                    poolBuilder
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getGROWSSETH_BANNER_PATTERN()));
                }
                else if (ANCIENT_CITY_LOOT.equals(id)) {
                    poolBuilder
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_ABBANDONATI()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_MISSIVA_NELL_OMBRA()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getGROWSSETH_ARMOR_TRIM()));
                }
                else if (DUNGEON_LOOT.equals(id) || MANSION_LOOT.equals(id)) {
                    poolBuilder
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_BINOBINOOO()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_BINOBINOOO_INST()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_GIORGIO_CUBETTI()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_GIORGIO_FINDING_HOME()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_GIORGIO_LOFI()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_GIORGIO_LOFI_INST()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_INFINITE_AMETHYST()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_LABYRINTHINE()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_MICE_ON_VENUS()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_PADRE_MAMMONK()))
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getDISC_SEGA_DI_NIENTE()));
                }
                else if (OCEAN_RUIN_COLD_LOOT.equals(id) || OCEAN_RUIN_WARM_LOOT.equals(id)) {
                    poolBuilder
                        .add(LootItem.lootTableItem(GrowssethItems.INSTANCE.getGROWSSETH_POTTERY_SHERD()));
                }
                tableBuilder.pool(poolBuilder.build());
            }
        });
    }
}
