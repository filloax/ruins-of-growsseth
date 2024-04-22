Questa è la sezione degli scambi, i quali si dividono in due categorie: **preimpostati** e **personalizzati**.

Gli scambi **preimpostati** sono scambi preconfezionati presenti all'interno dei dati della mod, già pronti per l'attivazione senza che serva impostare niente. Per vedere cosa fanno di preciso o modificarli li puoi trovare nella cartella degli scambi del ricercatore del datapack (data/growsseth/growsseth\_researcher\_trades).

Gli scambi **personalizzati** sono scambi liberamente impostabili. Danno molta più libertà di quelli preimpostati, ma richiedono di conoscere la sintassi JSON.  
Seguono questo template:

    {
        "gives": //TradeItemEntry,
        "wants": [
            //TradeItemEntry1,
            //TradeItemEntry2 (optional)
        ],
        // OPTIONALS:
        "priority": 0,
        "noNotification": false,
        "replace": false
    }

In **"gives"** va l'item che si vuole dare con lo scambio (vedere sotto per il formato degli item), mentre in **"wants"** vanno gli item che si vuole che il ricercatore chieda in cambio (massimo due, come per i villici vanilla). I seguenti campi sono opzionali:

*   **"priority"** indica quanto il nuovo scambio deve andare in alto nel menu del ricercatore (tutti gli scambi sono ordinati tramite questo valore, è consigliato studiarsi il datapack per capirlo);
*   **"noNotification"** (se impostato a _true_) non mostra la notifica in gioco quando lo scambio viene creato;
*   **"replace"** (se impostato a _true_) sostituisce altri scambi a priorità più bassa che offrono lo stesso oggetto.

Al posto dei commenti con "<u>TradeItemEntry</u>" bisogna inserire un oggetto JSON con questo formato:

    {
        "id": "minecraft:stick",
        "amount": 5,
    }

In **"id"** va l'id dell'item che si vuole dare con lo scambio, mentre in **"amount"** va la quantità offerta (se l'item sta in "gives") o richiesta (se l'item sta in "wants").

<u>Nota 1</u>: se l'oggetto venduto è una mappa del tesoro (per strutture vanilla o della mod), bisogna aggiungere ai valori dell'item un elemento "map" con questo formato:

        "map": {
            "name": "Mappa per la fortezza",
            // CHOOSE:
            "structure": "minecraft:stronghold",
            "fixedStructure": "growsseth:cave_camp",
            "x": 0,
            "z": 0,
            // OPTIONALS:
            "description": [
                "Riga 1",
                "Riga 2",
            ],
            "scale": 3,
        },

In **"name"** va il titolo della mappa, poi bisogna lasciare uno (due per le coordinate) tra i seguenti elementi (e **rimuovere gli altri**):

*   **"structure"** fa puntare la mappa alla struttura spawnata **naturalmente** con l'id indicato (viene presa la più vicina);
*   **"fixedStructure"** funziona come structure ma per strutture spawnate **tramite sito**;
*   **"x"** e **"z"** fanno puntare la mappa a delle **coordinate specifiche**.

I seguenti campi sono invece opzionali:

*   **"description"** è la descrizione dell'oggetto (quello che appare sotto il nome quando si passa il mouse sopra) e può essere composta da più righe (se si vuole usare una riga sola si può anche usare una stringa al posto della lista);
*   **"scale"** è lo zoom della mappa, più il valore è alto e più ampia sarà l'area mostrata. Va da 1 a 4 come per le mappe vanilla.

<u>Nota 2</u>: se si vuole vendere uno dei **diari del ricercatore** presenti nella mod si deve aggiungere ai valore dell'item questo elemento:

        "diaryId": "enchantment_dictionary"

In **"diaryID"** va semplicemente l'id del diario nei dati della mod. Per vedere quali sono disponibili o aggiungerne di tuoi puoi guardare il datapack (data/growsseth/growsseth\_researcher\_diary/it\_it).

Per concludere, ecco un esempio di scambio in cui si offrono 15 TNT per 3 bastoni e 10 carote:

    {
        "gives":{
            "id":"minecraft:tnt",
            "amount":15
        },
        "wants":[
            {
                "id":"minecraft:stick",
                "amount":3
            },
            {
                "id":"minecraft:carrot",
                "amount":10
            }
        ],
        "priority":100
    }

<u>Nota</u>: gli scambi del ricercatore (a differenza di altri eventi che vengono attivati una volta sola) vengono caricati nel gioco ogni volta che la mod riceve i dati dal sito, quindi una volta attivati non andrebbero toccati finché non si decide di modificarli o disattivarli.