## Live Update Server

Server per inviare un segnale in live alla partita di Cydo per aggiornare i dati con il sito anche oltre i 10 minuti, appena riceve un comando manuale.
Inoltre come funzionalità secondaria ricezione dei log da remoto per debuggare in diretta.

Si connette con l'URL privato a casa di Filloax, indi per cui lato client deve essere 100% robusto in caso di assenza di connessione, per evitare una dipendenza del funzionamento della run di Cydo dal funzionamento del PC di un privato.

[Separato da server test vari in altra cartella perchè questo include cose di produzione, quindi tengo tutti gli script correlati qua]

---

Il server nel progetto kotlin *remove-commands-server* viene eseguito nel mio Raspberry Pi (-Filloax), voi dovete lanciare **remote-commands-client.py** per connettervi al mio server. L'unico comando che funziona è `reload;`, che porta il client minecraft attualmente connesso (se presente) a ricaricare i dati di Project EgoBalego immediatamente