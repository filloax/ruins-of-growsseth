Questa è la sezione delle comunicazioni, le quali si dividono in tre categorie principali: **notifiche**, **dialoghi** del ricercatore e **diari / libri** del ricercatore / delle strutture.

Le **notifiche** sono le più semplici: hanno un **titolo**, un'**icona** (se si seleziona la tipologia corrispondente) e un **testo**. Titolo e testo sono autoesplicativi, mentre l'icona è indicata dall'id dell'item che si vuole usare preceduto dal suo namespace, quindi si possono utilizzare anche item di altre mod se si vuole.  
<u>Nota</u>: le notifiche con icona hanno uno sfondo nero come gli obiettivi, mentre quelle senza hanno uno sfondo blu come gli avvisi della chat non sicura.

Con i **dialoghi** si può decidere cosa si vuole che dica il ricercatore in chat quando ci si avvicina. Per un dialogo semplice basta scrivere il suo contenuto separandolo in più messaggi con gli **\\n** (il simbolo per le andate a capo), come in questo esempio:

    Salve collega, bla bla bla...\nA proposito, ha sentito di...

Il gioco decide automaticamente la durata per cui un messaggio blocca l'apparizione del successivo, ma è possibile deciderla manualmente per messaggi specifici se si ha una conoscenza di base della sintassi **JSON**.  
Per impostare una durata bisogna separare in una lista i messaggi, e inserire quello che si vuole controllare in un oggetto JSON con il contenuto e la durata in secondi, in questo modo:

    [
        "Salve collega, sa dirmi che ore sono?",
        {
            "content": "§lMmm...§r",
            "duration": 3.0
        }
        "Ah giusto, non posso leggere la chat.",
    ]

<u>Nota</u>: come hai visto sopra puoi usare i codici di formattazione di Minecraft per corsivo, grassetto ecc. (quelli che racchiudono il testo con §lettera). Inoltre ricorda che cambiare la durata di un messaggio non influisce su quanto rimane a schermo prima di sfumare, ma solo dopo quanto tempo può apparire il successivo!

Infine si arriva alla categoria dei **diari / libri**, che include tre diverse azioni:

*   Far scrivere al ricercatore un **nuovo diario** quando il giocatore si avvicina;
*   **Sostituire uno dei diari** che scrive il ricercatore quando ci si avvicina dopo aver esplorato una struttura (quelli sulle sue avventure nella struttura in questione);
*   **Sostituire uno dei libri** che si trovano nelle strutture.

Negli ultimi due casi, oltre a titolo, autore (solo per i libri delle strutture) e contenuto, bisognerà anche scegliere a quale struttura ci si riferisce.

Il contenuto è quello delle pagine, per le quali puoi usare **\\n** per andare a capo e **%PAGEBREAK%** per andare alla pagina successiva, come in questo esempio:

    Caro diario.\n\nOggi ho visto un creeper.%PAGEBREAK%Era molto §overde§r.",

<u>Nota</u>: Come per i dialoghi puoi usare le regole di formattazione del gioco.