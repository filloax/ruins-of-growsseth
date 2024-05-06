<!-- TOC -->

- [Installazione](#installazione)
- [Informazioni formato JSON dialoghi e altri](#informazioni-formato-json-dialoghi-e-altri)
- [Aggiornamento dati da sito](#aggiornamento-dati-da-sito)
- [Formato dati da sito](#formato-dati-da-sito)
    - [Comandi quest finale [SPOILER]](#comandi-quest-finale-spoiler)
    - [Comandi di comunicazione](#comandi-di-comunicazione)
    - [Comandi di mercato](#comandi-di-mercato)
    - [Comandi vari](#comandi-vari)
    - [Comandi di emergenza](#comandi-di-emergenza)
- [Controllo mod tramite Socket](#controllo-mod-tramite-socket)

<!-- /TOC -->
# Ruins of Growsseth
<a id="markdown-ruins-of-growsseth" name="ruins-of-growsseth"></a>

<img src="./src/main/resources/assets/growsseth/icon.png" alt="alt text" width="250" height="250">

Lunga vita a Re Lucio.

## 1. Installazione
<a id="markdown-installazione" name="installazione"></a>

Versione del gioco: **1.20.2**, Mod loader: [**Fabric**](https://fabricmc.net/use/installer/) (almeno ver. 0.15.1)

Dipendenze:
- [**Fabric API**](https://modrinth.com/fabric-api), almeno ver. 0.91.1 (di fatto già inclusa nel 99% dei casi in cui avete Fabric);
- [**Fabric Language for Kotlin**](https://modrinth.com/fabric-language-kotlin), almeno ver. 1.10.16;
- Opzionale ma comodo: [*Mod Menu*](https://modrinth.com/modmenu) (permette la configurazione in game).

La mod si può scaricare da [***QUA***](https://github.com/filloax/ruins-of-growsseth/releases/latest).

## 2. Informazioni formato JSON dialoghi e altri
<a id="markdown-informazioni-formato-json-dialoghi-e-altri" name="informazioni-formato-json-dialoghi-e-altri"></a>

Si possono trovare [QUA](docs/JSON_FORMAT.md). Contengono info sul formato dei json stile datapack personalizzati usati dalla mod per dialoghi ecc.

## 3. Aggiornamento dati da sito
<a id="markdown-aggiornamento-dati-da-sito" name="aggiornamento-dati-da-sito"></a>

Per testare durante lo sviluppo è fornito un server che fa le veci del sito Egobalego. Per avviarlo, installate [Python](https://www.python.org/downloads/) (sta anche sul Microsoft Store volendo)e poi eseguite con esso lo script `egobalego-at-home.py`, che potete scaricare da [**qui**](https://github.com/filloax/ruins-of-growsseth/blob/master/server-rest-test/egobalego-at-home.py) con il pulsante di download in alto a destra. Se invece avete già scaricato tutto il repository lo trovate direttamente nella cartella "server-rest-test".

Lo script vi aprirà una pagina del browser dove potete impostare eventi e lo spawn delle strutture (nota: lo scopo finale della mod è cambiato, quindi alcune cose come le quest legate allo scantinato potrebbero non funzionare). Ricaricando il mondo o usando il comando ```/reloadremote``` le modifiche verranno applicate (altrimenti si aggiorna automaticamente ogni 30 minuti).

<details>
<summary>Vecchie istruzioni manuali</summary>

Eseguire con Python lo script ```samplerestapi.py```, che potete scaricare da [**qui**](https://github.com/filloax/ruins-of-growsseth/blob/master/server-rest-test/samplerestapi.py) con il pulsante di download in alto a destra. Se invece avete già scaricato tutto il repository lo trovate direttamente nella cartella "server-rest-test".

Da lì fa tutto lui, vi crea il file da modificare se non c'è già (chiamato ```website_mock.json```), da cui potete attivare gli eventi mettendo a true il parametro "active" in fondo ad ognuno. Ricaricando il mondo o usando il comando ```/reloadremote``` le modifiche verranno applicate (altrimenti si aggiorna automaticamente ogni 30 minuti).
</details>

## 4. Formato dati da sito
<a id="markdown-formato-dati-da-sito" name="formato-dati-da-sito"></a>

Di base si inviano alla mod dati formattati in JSON come da esempio fornito da Worgage per spawnare le strutture, per esempio:
```jsonc
{
    "id": 2,
    "structureID": "growsseth:beekeeper_house",
    "name": "beekeeper_house",
    "x": 1427,
    "y": 70,
    "z": 192,
    "active": false
}
```

Nota: per spawnare una struttura di nuovo è necessario cambiare il parametro "name". Inoltre le strutture spawnano dal loro centro (rispetto le coordinate x e z, eccetto la caverna che usa anche la y) e non dal punto di minor coordinate come per logica vanilla. Per cercare le coordinate migliori per lo spawn di una struttura la si può spawnare con il comando vanilla ```/place``` e vedere in che coordinate viene piazzata.

La mod permette di inviare anche comandi speciali tramite finte "strutture" che iniziano per *"event:"* invece che *"growsseth:"*. Generalmente hanno questo aspetto:

```Json
{
    "id": 3,
    "structureID": "event:researcher_end_quest_start",
    "name": "Inizio quest",
    "x": 0,
    "y": 0,
    "z": 0,
    "active": false
}
```

(x, y, z e name sono opzionali, usati da alcuni comandi ma non tutti).

Ecco quali sono, divisi per scopo pensato.

### Comandi quest finale [SPOILER]
<a id="markdown-comandi-quest-finale-%5Bspoiler%5D" name="comandi-quest-finale-%5Bspoiler%5D"></a>

La quest finale è stata modificata, a causa della scoperta della cura in modo indipendente; verrà quindi realizzata tramite comandi di dialogo (vedi `event:rdialogue` a [Comandi di comunicazione](#comandi-di-comunicazione)) e un comando speciale per attivare la scomparsa della tenda e del ricercatore lasciandosi dietro una chest con un diario e il corno come regalo, come nel finale precedente:

- `event:rmTentWithGift`: rimuove il ricercatore, la tenda, e spawna una chest con il diario d'addio e il corno del ricercatore. Richiede come `rmTent` un id per non venire ripetuto, ma non richiede la posizione (si triggera al primo Ricercatore che viene caricato);
- `event:enddiary/<TITOLO>`: sostituisce il testo del diario lasciato alla fine. Formato analogo a *rdiary*, inclusi **tutti** i campi requisiti, presente in [Comandi di comunicazione](#comandi-di-comunicazione).

NOTA: L'idea sarebbe attivare `event:event:sell_all_extras` prima di qualunque sia la fase finale, per attivare la vendita gratuita di tutti gli oggetti. Gli eventi sono comunque separati nel caso `event:sell_all_extras` possa servire in altri casi.

<details markdown="1">
<summary>Versione precedente (zombificazione)</summary>

I seguenti comandi attivano le varie fasi della quest finale. Non hanno parametri aggiuntivi oltre a id ed essere attivi o meno. Nota: gli id sono configurabili tramite file configurazione.

- `event:researcher_end_quest_start`: triggera il dialogo dei sintomi la volta successiva in cui si incontra il ricercatore;
- `event:researcher_end_quest_zombie`: zombifica il ricercatore e lo teletrasporta nella gabbia, oltre a far apparire il diario di addio dello scantinato;
- `event:researcher_end_quest_leave`: fa sparire la tenda, il ricercatore e l'asino se legato, e spawna la cassa con il regalo e il diario di addio (necessita l'aver curato il ricercatore).

NOTA: L'idea sarebbe di attivare `event:event:sell_all_extras` insieme a `event:researcher_end_quest_zombie`, così quando viene curato vende già tutto e scontato.
Gli eventi sono comunque separati in caso `event:sell_all_extras` possa servire per altre cose. Per curare istantaneamente il ricercatore zombie usare il comando vanilla
```/data modify entity @e[type=growsseth:zombie_researcher,limit=1,sort=nearest] ConversionTime set value 0b``` dopo avergli dato la pozione di debolezza e la mela d'oro.
</details>

### Comandi di comunicazione
<a id="markdown-comandi-di-comunicazione" name="comandi-di-comunicazione"></a>

Notifiche o dialoghi impostati in remoto. Sì, permettono anche di trollare Cydo, ma non li userei per quello scopo (o almeno non più di una volta). Sono pensati per far reagire il gioco/il ricercatore a eventi specifici se necessario; per esempio se Cydo si spawna il wither in casa e deve traslocare (e quindi va spostato anche il ricercatore) avrebbe senso farglielo commentare.

- `event:toast/<TITOLO>`: invia un "toast" (notifica stile achievement) al giocatore. Viene inviato solo una volta, per capire che è lo "stesso" al ricaricamento e non re-inviarlo la mod controlla x, y e z (stessi xyz = non re-invia). Il titolo del toast è impostato nell'id, per esempio `event:toast/Ciao mamma`, mentre opzionalmente il contenuto si può aggiungere dentro a name (per esempio `"name": "Sono in TV"`);
- `event:toast/<NAMESPACE>/<ID_OGGETTO>/<TITOLO>`: come sopra, ma include nella notifica l'icona dell'oggetto specificato. Principalmente pensato per notifiche degli ender eye, nel qual caso l'uso sarebbe `event:toast/minecraft/end_portal_frame/<TITOLO>`;
- `event:rdialogue/<ID>`: imposta un dialogo monouso che il ricercatore dirà la prossima volta che il giocatore (Cydo) vi si avvicina. L'Id (per esempio, `event:rdialogue/diag1`) viene usato per non ripeterlo. Il contenuto viene messo in "name" obbligatoriamente, separando più righe con "\n";
- `event:rdiary/<TITOLO>`: imposta un diario che il ricercatore scriverà la prossima volta che il giocatore ci si avvicina. Richiede di impostare sia *name* che posizione (*x*, *y*, *z*). Il titolo è la parte dopo la barra nell'evento, le pagine sono dentro al campo name, separate dalla sequenza %PAGEBREAK%. X, y e z sono usate per distinguere diari con lo stesso titolo: viene usato come id per tracciare i diari già scritti \<titolo\>-\<x\>-\<y\>-\<z\>;
- `event:structdiary/<NOMESTRUTTURA>/<TITOLO>`: come per rdiary, ma il diario invece di spawnare subito andrà a sostituire il diario che viene creato all'esplorazione della struttura in questione (se non è ancora stato creato). Il contenuto del diario va nel parametro "name", per il formato vedere [Libri e Diari](docs/JSON_FORMAT.md#libri-e-diari). Non richede pos non necessitando di id unici di conseguenza. Nota che NOMESTRUTTURA è il tag, non la singola struttura, e non include `growsseth:`: per esempio `event:structdiary/noteblock_lab/LOREM IPSUM`. Per nomi validi controllare [*src/main/resources/data/growsseth/growsseth_researcher_diary/it_it/structures*](./src/main/resources/data/growsseth/growsseth_researcher_diary/it_it/structures/) nella cartella del progetto e guardare i nomi dei file;

Esempio di rdiary:
```Json
{
    "id": 1,
    "structureID": "event:rdiary/Breve storia triste",
    "name": "Lorem ipsum.\n\nDolor sit amet.%PAGEBREAK%Davvero un bel casino zì\n\nasfdsfasdgdsfgsd\n\n§lAmogus§r\n\n§oCorsivo§r",
    "x": 0,
    "y": 0,
    "z": 1, 
    "active": true
}
```

- `event:structbook/<IDTEMPLATE>`: sostituisce i libri piazzati all'interno delle strutture. Vedere [Libri strutture](docs/JSON_FORMAT.md#libri-strutture) per altre informazioni. IDTEMPLATE deve essere l'id di un template libro usato nella mod (usare il comando custom `/booktemplate` per
avere un elenco, o guardare nella [loro cartella](./src/main/resources/data/growsseth/growsseth_structure_books/it_it/) dove
ogni file corrisponde a un template dallo stesso nome). Il contenuto dei libri è passato come stringa JSON dentro a "name", per il formato vedere [Libri e Diari](docs/JSON_FORMAT.md#libri-e-diari).

### Comandi di mercato
<a id="markdown-comandi-di-mercato" name="comandi-di-mercato"></a>

Sbloccano altri scambi:
- `event:sell_beginning`: attiva la vendita iniziale degli oggetti (pattern stendardo per 2 zuppe di funghi, la mappa è gestita dall'unlock della caverna);
- `event:sell_default`: attiva la vendita standard degli oggetti, quindi banner ecc. (disattivata di default per permettere la "fase iniziale" con solo sell_beginning attivo);
- `event:sell_sherd`: attiva vendita del coccio custom;
- `event:sell_trim_template`: attiva vendita dell'armor trim custom;
- `event:sell_enchant_dictionary`: attiva vendita del dizionario di "enchantese" per la struttura del conduit;
- `event:sell_giorgio_lofi`: vende il disco della sigla lofi (instrumental);
- `event:sell_giorgio_finding_home`: vende il disco della sigla acustica
- `event:sell_binobinooo`: vende il disco di Binobinoo (instrumental);
- `event:sell_all_extras`: sblocca la vendita di tutti gli oggetti speciali della mod non sbloccati da subito, pensato per essere usato a fine quest finale. Non mostra notifica di nuovo scambio. Ogni item sbloccato è "gratuito" (1 pezzo di carta);
- `event:sell_easter_egg_discs`: sblocca la vendita dei dischi "extra" (versioni vocal canzoni + alcune meme o tagliate), sempre aggratis;
- `event:customTrade/<ID>`: permette di vendere oggetti arbitrari, totalmente personalizzabili nei trade. Per scambi semplici si può usare anche `sell` spiegato sotto. L'id è arbitrario e usato solo per avere entry diverse, dentro a *name* va inserito del testo JSON che rappresenta una entry di trade analoga a quelle inseribili nei JSON dati della mod, formato spiegato [qua](docs/JSON_FORMAT.md#scambi). Un esempio:

    ```json
    {
        "id": 23,
        "structureID": "event:customTrade/test1",
        "name": "{\"gives\":{\"id\":\"minecraft:tnt\",\"amount\":15},\"wants\":[{\"id\":\"minecraft:stick\",\"amount\":3},{\"id\":\"minecraft:carrot\",\"amount\":10}],\"priority\":100}",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": true
    }
    ```

- `event:sell/<NAMESPACE>/<ID>/<PRICE>[/h]`: permette di vendere oggetti arbitrari, nel caso sia necessario (per esempio per la perdita di un oggetto unico della mod). Un esempio è `event:sell/minecraft/coal/1/h`, che aggiunge agli scambi minecraft:coal a prezzo 1 smeraldo. Se si aggiunge /h, lo scambio non avrà notifiche una volta sbloccato (altrimenti usare `event:sell/minecraft/coal/1`, per esempio).

### Comandi vari
<a id="markdown-comandi-vari" name="comandi-vari"></a>

- `event:cmd/<ID>`: esegue un comando appena i dati vengono sincronizzati. Eseguito una volta sola per ID, a prescindere che abbia successo o meno. Il comando (senza '/') va inserito nel parametro "name". Può essere anche fatto tramite [socket](#5-controllo-mod-tramite-socket). Se specificata una posizione, verrà usata come origine nel caso di posizioni relative (non testato). Nota: non funziona se l'opzione in config remoteCommandExecution (default true per Cydo) è disattivata.

### Comandi di emergenza
<a id="markdown-comandi-di-emergenza" name="comandi-di-emergenza"></a>

Pensati per sbloccare il ricercatore nel caso sparisca, si bugghi da qualche parte, muoia per errore, ecc. Permettono di rispawnarlo, teletrasportarlo, eliminare la tenda, e altro:

- `event:tpResearcher`: teletrasporta il ricercatore (o meglio, il primo che il gioco carica se ce ne sono di più) alla posizione in x/y/z, e reimposta la sua "posizione iniziale" (usata per rimanere vicino, rilevare la tenda, ecc.) al punto di teletrasporto;
- `event:rmResearcher`: rimuove TUTTI i ricercatori che sono "più vecchi" di quando il comando sia stato ricevuto, +1 minuto di grazia per evitare casini nel caso ne siano stati contemporaneamente spawnati altri (per esempio creando una nuova tenda). Per "vecchiaia" si intende tempo a cui sono stati spawnati la prima volta. Nota: la rimozione avviene quando il gioco li carica effettivamente, quindi per stare sul sicuro meglio lasciare un determinato "rmResearcher" attivo una volta creato. Una volta inserito il gioco ricorda l'età del comando rmResearcher con un certo `"name"`, quindi per rifarlo bisogna inserirne un altro con un `"name"` diverso;
- `event:rmTent`: rimuove la tenda a x/y/z quando caricata, e l'asino se non preso in prestito dal giocatore;
- `event:spawnResearcher`: spawna un ricercatore senza tenda a x/y/z (per spawnarne uno nuovo **E** la tenda usare il sistema delle strutture standard della mod).

L'idea di utilizzo di questi comandi è, per esempio: il ricercatore muore per bug o interazione imprevista, se ne spawna uno alla tenda; il ricercatore rimane bloccato in posto insensato, si teletrasporta alla tenda o si rimuove e se ne spawna un'altro (dati quest e dialoghi rimangono tanto); Cydo fa casini che necessitano un suo trasloco e conviene spostare la tenda per comodità, si eliminano tenda e ricercatore vecchi e se ne spawna una nuova vicino a base nuova.

Questi 4 comandi necessitano il campo `"name"` per identificarsi allo scopo di venire eseguiti una sola volta (così, per esempio, il gioco non continua a provare a eliminare la tenda una volta fatto). Quindi, per essere rieseguiti, vanno inseriti con un `"name"` diverso.

## 5. Controllo mod tramite Socket
<a id="markdown-controllo-mod-tramite-socket" name="controllo-mod-tramite-socket"></a>

La mod si connette al sito EgoBalego anche tramite Socket.io (URL e api Key configurabili). Al momento è possibile inviare i seguenti eventi: 

- `reload`: forza una ricarica dei dati del sito indicati a [Formato dati da Sito](#4-formato-dati-da-sito), senza necessitare la ricarica del mondo o di aspettare 10 minuti. Non richiede contenuti specifici.

- `rdialogue`: invia un dialogo che verrà immediatamente riprodotto dal Ricercatore se caricato e vicino al giocatore, o ignorato altrimenti. Nel contenuto va incluso un oggetto JSON che lo definisce; il formato è lo stesso dei file dei dialoghi, [specificato qua](docs/JSON_FORMAT.md#formato-dialogueentry). Si possono trascurare i campi indicati come non per messaggi remoti (includerli non porterà a errori, è semplicemente inutile).

- `toast`: invia una notifica, con o senza icona oggetto (sfondo nero con, blu senza). Il formato è JSON come segue:

```jsonc
{
    "title": "Titolo notifica",
    "item": "minecraft:ender_eye", // opzionale, id oggetto da wiki minecraft (o della mod nostra), viene mostrato a sinistra nella notifica
    "message": "Testo notifica!", // opzionale, può anche essere solo titolo
}
```

- `cmd`: esegue un comando. Il formato è JSON come segue:

```jsonc
{
    "command": "setblock X Y Z minecraft:dirt", // il comando senza '/'
}
```

La mod risponderà agli eventi con un oggetto JSON contenente i seguenti campi:

```jsonc
{
    "status": "failure"/"success",
    "reason": "<errore java>", // in caso di failure con eccezioni java
    // eventuali campi extra correlati in caso di successo
}
```

---

![](./src/main/resources/assets/growsseth/textures/gui/advancements/backgrounds/advancements_background.png)
