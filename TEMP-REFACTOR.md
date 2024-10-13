# Info sul refactor/conversione a neoforge

Cose più complesse da ricontrollare in prossimi commit che al momento usano fabric e non compilano
- Networking in generale (invio/ricezione pacchetti sia client che server)
    - Sorprendentemente Packets.kt è a posto
    - Classi colpite
        - Basic/ResearcherDialogueComponent
        - GlobalResearcherTradesProvider
        - GrowssethExtraEvents
        - LiveUpdatesConnection
- net.fabricmc.fabric.api.loot.v3.LootTableSource in VanillaStructureLoot e datagen
- FabricItemGroup in CreativeModeTabs
- Metodi aggiunti da fabric per enchant in ResearcherDaggerItem.kt
- Eventi (per ora patchato male) che usano classi fabric
    - onLootTableModify
    - onPlayerServerJoin
- Gestione registri