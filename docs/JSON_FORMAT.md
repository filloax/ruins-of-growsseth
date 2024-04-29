[Torna a README](./README.md)

## Formato dati JSON

Le cose da inviare tramite sito stanno sul readme, questo file invece documenta il formato
dei dati JSON interni alle risorse della mod, per chi volesse espanderli.

### Formato DialogueEntry

I dialoghi sono rappresentati nei file di dialoghi e se inviati da EgoBalego da oggetti JSON, con questo formato.

```jsonc
{
    // Campi richiesti
    "content": list | string | object, // vedi sotto
    
    // Campi richiesti solo se si usa il campo useLimit, o per dialoghi shared
    "id": string,

    // Campi opzionali [e eventuali valori default]
    "immediate": true/false, // Attiva il dialogo immediatamente, saltando eventuali code
    "data": object[string, string], // Dati personalizzati utilizzabili in modo diverso da vari NPC (vedi sotto)

    // Campi opzionali, solo per file [no dialoghi remoti]
    "weight": float = 1.0, // Peso del dialogo nella scelta casuale
    "useLimit": int, // Limite di utilizzi per questo specifico dialogo
    "afterRepeatsMin": int, // Numero minimo di trigger dell'evento affinchè il dialogo possa essere riprodotto,
    "afterRepeatsMax": int, // Numero massimo di trigger dell'event affinchè il dialogo possa essere riprodotto,
    "afterCloseRepeatsMin": // Numero minimo di trigger event entro un periodo di tempo breve affinchè il dialogo possa essere riprodotto,
    "afterCloseRepeatsMax": // Numero massimo di trigger event entro un periodo di tempo breve affinchè il dialogo possa essere riprodotto,
    "requiresQuest": string, // Quest richiesta per attivare il dialogo (attualmente non necessario, avendo un npc al massimo una quest associata)
    "requiresQuestStage": string, // Nome dello stage della quest richiesta (per esempio, "home", "zombie", ecc per il Ricercatore)
    "requiresUntilQuestStage": string, // Come sopra, ma invece di richiedere lo stage disabilita il dialogo a partire da esso
    "requiresEventParam": string. // Alcuni eventi hanno un parametro associato (es. il nome in RENAME): attivati solo con questo parametro, se lasciato vuoto si attiverà con ogni parametro
    "priority": int, // Se ci sono più dialoghi attivabili, scegli solo tra quelli a priorità più alta 
}
```

#### DialogueEntry: Content

Il campo `content` può contenere una lista, una stringa, o un oggetto, in uno di questi tre formati:

```jsonc
"content": "LINES\nSEPARATED BY\nNEWLINES"
"content": {
    "content": "SINGLE LINE WITH PARAMETERS",
    "duration": 2.0
}
"content": [
    "SIMPLE LINE",
    {
        "content": "LINE WITH PARAMETERS, CAN MIX BOTH",
        "duration": 1.0
    }
]
```

#### DialogueEntry: Data

Il campo `data` è pensato nel caso in cui si aggiugneranno altri NPC con dialoghi, per essere usato per cose specifiche di quell'NPC. Al momento per il ricercatore può contenere:

**`data`: Ricercatore**
```jsonc
"data": {
    "sound": "angry"/"none", // Se impostato, usa il suono del villager arrabbiato o nessun suono rispettivamente invece del suono default quando parla
    "madeMess": true, // Se impostato, usa il dialogo solo se il ricercatore è arrabbiato per la rottura di blocchi della tenda
}
```

### File dei dialoghi

Nel dubbio il javadoc sopra [DialogueEntry](Mod Fabric/src/main/kotlin/com/ruslan/growsseth/dialogues/DialogueEntries.kt) dovrebbe essere aggiornato anche
se il testo seguente non lo fosse, comunque potete inserire dialoghi (al momento solo ricercatore) nella cartella *resources/data/growsseth/growsseth_researcher_dialogue/lingua_dialoghi/*, e poi potete creare un file json o jsonc (*json with comments,* meno restrittivo e permette commenti) col nome che vi pare o creare gli esistenti.  
È anche possibile creare un datapack, in quel caso potete mettere in *data/<minecraft/growsseth/ecc>/growsseth_researcher_dialogue/lingua_dialoghi/* e stessa cosa.*

\* *Nota: in futuro potrebbe cambiare la cartella.*

I file hanno questo formato:

```json
{
    "shared": [
        dialoghi...
    ],
    "<evento 1>": [
        dialoghi...
    ],
    "<evento 2>": [
        dialoghi...
    ],
    ...
}
```

Le liste sotto i vari eventi contengono o dialoghi semplici rappresentati da una stringa, o oggetti JSON come indicato a [Formato DialogueEntry](#formato-dialogueentry).

Gli eventi possono essere uno tra i seguenti, al momento della scrittura (13 set 2023):

#### Eventi - Generali

| Evento                   | Descrizione                                                                            | Parametro       |
| :----------------------- | :------------------------------------------------------------------------------------- | --------------- |
| **playerArrive**         | Arrivo del giocatore entro range dell'NPC.                                             |                 |
| **playerArriveNight**    | Come **playerArrive**, ma di notte (prioritario).                                      |                 |
| **playerLeave**          | Uscita del giocatore in range e vista dell'NPC.                                        |                 |
| **playerLeaveNight**     | Come **playerLeave**, ma di notte (prioritario).                                       |                 |
| **playerArriveSoon**     | Arrivo del giocatore entro 10 secondi dall'ultimo saluto (prioritario).                |                 |
| **playerLeaveSoon**      | Uscita del giocatore dal range dopo un  **playerArriveSoon**.                          |                 |
| **playerArriveLongTime** | Arrivo del giocatore oltre 6 ore (in tempo di gioco) dall'ultimo saluto (prioritario). |                 |
| **tickNearPlayer**       | Triggera appena il giocatore sia vicino all'NPC, anche se già salutato.                |                 |
| **hitByPlayer**          | Dopo essere colpito dal giocatore.                                                     |                 |
| **lowHealth**            | Quando la vita scende sotto un tot.                                                    |                 |
| **death**                | Alla morte.                                                                            |                 |
| **rename**               | Dopo essere stato rinominato dal giocatore.                                            | Nome nuovo.     |
| **playerAdvancement**    | Un giocatore ottiene un advancement entro il range.                                    | Id advancement. |
| **global**               | Dialoghi che si attivano in *ogni* evento (usare in casi specifici).                   |                 |

#### Eventi - Ricercatore

| Evento                        | Descrizione                                                                      | Parametro |
| :---------------------------- | :------------------------------------------------------------------------------- | --------- |
| **makeMess**                  | Rottura blocchi "utili" per il ricercatore (lavoro, cartografia).                |           |
| **fixMess**                   | Riparati i blocchi rotti da **makeMess**                                         |           |
| **refuseTrade**               | Tentativo di scambio quando il ricercatore è arrabbiato e lo rifiuta.            |           |
| **breakTent**                 | Rottura di blocchi della tenda di poco valore.                                   |           |
| **borrowDonkey**              | Asino preso in prestito.                                                         |           |
| **returnDonkey**              | Asino restituito.                                                                |           |
| **exploreCellar**             | Giocatore entra nella zona segreta nello scantinato.                             |           |
| **exitCellar**                | Giocatore esce dallo scantinato.                                                 |           |
| **hitByPlayerImmortal**       | Come hitByPlayer, ma solo se il ricercatore è immortale.                         |           |
| **playerCheats**              | Quando il giocatore che sta combattendo triggera l'anticheat (se attivo).        |           |
| **killPlayer**                | Quando uccide il giocatore che lo ha aggredito.                                  |           |
| **playerArriveAfterKilled**   | Come playerArrive, ma solo una volta all'arrivo di un giocatore che ha ucciso.   |           |

#### Evento Shared

`shared` è speciale, e contiene dialoghi che possono essere riutilizzati nell'elenco dei dialoghi degli eventi in questo modo:
```json
"shared": [
    {
        "id": "shared-dialogue-id",
        "content": "Hello!"
    }
],
"<event>": [
    ...
    {
        "id": "shared-dialogue-id",
    },
    ...
]
```
Principalmente utile per dialoghi riutilizzati in più eventi per coprire più trigger.

### Libri e diari

I template di libri e i diari del ricercatore hanno un formato simile. Sono file JSON come segue:

```jsonc
{
    "pages": [
        "PAGINA 1",
        "PAGINA 2\nCon a capo",
        "",
        "Prima c'era una pagina vuota!"
    ],
    "name": "Titolo libro",

    // non presenti nei diari del ricercatore
    "author": "Nome autore", 
    "writable": true/false // opzionale, se true non sono necessari name e author
}
```

#### Diari

I diari del ricercatore stanno in `data/growsseth/growsseth_researcher_diary/it_it`. Quelli nella 
cartella principale sono usati in casi specifici deducibili dal nome, mentre quelli nella cartella
`structures` vengono creati dopo aver esplorato una struttura con un **tag** corrispondente al nome del file.
I tag sono presenti in `data/growsseth/tags/worldgen/structure`, e sono in generale un dato vanilla, [info su MC Wiki](https://minecraft.wiki/w/Tag).

#### Libri strutture

I libri delle strutture sono inseriti come template dentro a libri piazzati in chest o leggi dentro le strutture 
della mod. Per inserire un libro con template bisogna creare un libro (scrivibile o firmato) con una sola pagina, che
inizia con *"%%TEMPLATE%%"*, e il cui resto della pagina viene usato come id del template. Idealmente dovrebbe non contenere
spazi (a capo verranno semplicemente ignorati. Per esempio:)

```
%%PAGEBREAK%%
beekeper_
house_0
```

diventa il template `beekeeper_house_0`. I template vengono letti da `data/growsseth/growsseth_structure_books/it_it`, 
prendendo il file json con il nome corrispondente al nome del template del libro caricato.

Si può usare `/booktemplate <giocatore> <nometemplate>` per ottenere un libro con il template, e `/booktemplate` per
elencare i template disponibili.


### Scambi

Gli scambi del ricercatore sono anche essi definiti tramite JSON. Trovate gli esempi in [`growsseth_researcher_trades/`](./Mod%20Fabric/src/main/resources/data/growsseth/growsseth_researcher_trades/), in generale il formato è il seguente:

```jsonc
// Trade
{
    "gives": { //output
        //TradeItemEntry
    },
    "wants": [ //input, 1 o 2 entry
        {
            //TradeItemEntry
        }
    ],
    "priority": 50, // opzionale, default 0, più è alto e più va in alto
    "noNotification": false, // opzionale, default false, se true non notifica il nuovo scambio
    "replace": false, // opzionale, default false, se true sostituisce altri scambi a priorità più bassa che offrono lo stesso oggetto
    "maxUses": 1, // opzionale, default 1, massimo uso di usi per questo scambio, refreshati in base a impostazioni (ignorato e sempre infiniti per scambi fatti tramite game-master/web)
    "randomWeight": 1.0, // "peso" dello scambio se presente in una lista di scambi scelti casualmente (più alto = più probabile)
}
//TradeItemEntry
{
    "id": "minecraft:stick", // id oggetto dal gioco
    "amount": 5, // opzionale, default 1
    "map": { // opzionale, info mappa nel caso venda mappa del tesoro (vanilla o growssethiana)
        "structure": "minecraft:stronghold", // ID struttura
        "name": "Mappa per guida", // Nome visualizzato
        "description": [ // opzionale, può anche essere singola stringa invece di lista di stringhe
            "Riga 1",
            "Riga 2",
        ],
        "x": 535, // opzionale, coordinate a cui puntare (altrimenti cerca una struttura con l'ID corrispondente nella worldgen)
        "z": 535, // opzionale, coordinate a cui puntare (altrimenti cerca una struttura con l'ID corrispondente nella worldgen)
        "fixedStructureId": "growsseth:researcher_tent", // opzionale, ID dello spawn fisso della struttura a cui puntare (per esempio, le strutture prefissate tramite sito)
        "scale": 3, // opzionale, default 3, scala mappa
    },
    "diaryId": "enchantment_dictionary" // id diario se si vuole vendere uno dei diari configurati, l'oggetto deve essere un libro scrivibile o scritto
}
```