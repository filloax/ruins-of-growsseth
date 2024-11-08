Questa è la sezione per il controllo della mod tramite WebSocket, un sistema tramite il quale (se l'impostazione del servizio "Live Update" è attiva nella mod) Egobalego at Home e la mod possono comunicare in tempo reale, a differenza delle altre sezioni che richiedono aspettare che la mod chieda i dati aggiornati.

È possibile inviare quattro eventi distinti:
*   Trigger istantaneo di un **dialogo** per tutti i ricercatori con un giocatore vicino;
*   Popup di una **notifica**, da impostare come spiegato nella sezione "Comunicazioni";
*   Esecuzione di un **comando** qualsiasi nel gioco (come i comandi manuali della sezione "Comandi" richiedono di attivare l'opzione _"remoteCommandExecution"_);
*   **Ricarica dei dati** impostati tramite le altre sezioni (quest'ultimo disponibile anche nelle altre pagine se il WebSocket è connesso).

Se il WebSocket non è collegato non sarà possibile inviare nessun evento e i pulsanti saranno disattivati. Se sono attivi cliccarci sopra manderà l'evento corrispondente alla mod, e si coloreranno di verde in caso di successo, di rosso in caso di fallimento.

In alto (se c'è connessione) si può visualizzare l'ultima risposta della mod con il timestamp corrispondente, ed eventuali dettagli vengono mostrati a destra.
