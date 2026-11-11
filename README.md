# PageFlow — Notion-clone locale per Android

(fino alla v0.152 si chiamava **notE**; il pacchetto interno resta
`com.gabriele.notionlocal`, invariato, perché cambiarlo farebbe un'app
nuova, senza i dati e senza gli aggiornamenti)

App di note/database stile Notion, con **tutti i dati salvati sul
dispositivo** (SQLite via Room). Nessuna rete, nessun account, nessun
cloud.

---

## ⚠️ LEGGIMI PRIMA — contesto per chi riprende il progetto

Questa sezione esiste perché il progetto è stato sviluppato in una
lunga conversazione, e senza questo contesto è facile "correggere"
cose che sono scelte deliberate, o ritentare strade già rivelatesi
senza uscita. Ambiente di test: **Samsung Galaxy S25 Ultra, tastiera
Samsung** (rilevante: vedi sotto).

### Dove vive il progetto (dal 24/09/2026)

**La copia di riferimento è il repository GitHub
`AbernathyXVII/MyNotesApp`**, non più la cartella
`Downloads\NotionLocal` sul PC dell'utente. Quella cartella non è
collegata a GitHub e da quel giorno può essere più vecchia del
repository: per riprendere il lavoro sul PC **si clona il repository**
(vedi "Come aprirla") e si lavora lì.

Il progetto ora si sviluppa in **due modi alternati**:

- **Claude Code sul PC**, col telefono collegato via adb: si compila,
  si installa, si legge `logcat`, si prova tutto sul dispositivo. È il
  modo in cui è nato tutto quello che c'è in "Cronologia".
- **Sessioni cloud** (claude.ai/code), quando il limite settimanale di
  Code sul PC è esaurito. Girano su un server remoto che **non vede né
  il PC né il telefono**: si può scrivere il codice, non installarlo né
  provarlo. Compilarlo si può **solo se l'ambiente cloud lascia passare
  `dl.google.com`** (SDK e librerie AndroidX arrivano da lì): il
  24/09/2026 all'inizio era bloccato, poi l'utente l'ha aggiunto ai
  domini consentiti e da lì si è compilato. Come si fa: "Compilare in
  una sessione cloud", qui sotto.

### Compilare in una sessione cloud

Il server cloud riparte da zero a ogni sessione: SDK e Gradle vanno
rimessi ogni volta (circa cinque minuti). Serve che l'ambiente lasci
passare `dl.google.com` e `maven.google.com` (menu della sessione →
"Edit cloud environment" → Network access). Poi:

```
# SDK: cmdline-tools dall'indirizzo che dà
# https://dl.google.com/android/repository/repository2-3.xml
# (voce "cmdline-tools;latest", controllare lo sha1), scompattati in
# /opt/android-sdk/cmdline-tools/latest, quindi:
yes | sdkmanager --sdk_root=/opt/android-sdk --licenses
sdkmanager --sdk_root=/opt/android-sdk "platforms;android-35" "build-tools;34.0.0" "platform-tools"

# Gradle: la versione del progetto (gradle/wrapper/gradle-wrapper.properties,
# oggi 8.7) da https://services.gradle.org/distributions/, con lo sha256.
# Il progetto non ha gradlew: si usa quello scaricato.
ANDROID_HOME=/opt/android-sdk gradle --no-daemon --max-workers=2 \
  -Dorg.gradle.internal.repository.max.retries=8 \
  -Dorg.gradle.internal.repository.initial.backoff=3000 assembleDebug
```

**Trappola di Maven Central**: dal server cloud `repo.maven.apache.org`
risponde spesso **429 (troppe richieste)**, e dal 24/09/2026
pomeriggio anche `repo1.maven.org`. Si aggira con uno script in
`~/.gradle/init.d/` (fuori dal progetto, non va nel repository) che
sostituisce quegli indirizzi con il **mirror di Maven Central su Google
Cloud Storage**, `https://maven-central.storage-download.googleapis.com/maven2/`
(stessi file, e non ha mai risposto 429), e che passa lo stesso
indirizzo ai test (`systemProperty 'robolectric.dependency.repo.url'`
per ogni task `Test`: Robolectric scarica da sé il suo Android); e con i
parametri qui sopra (meno download in parallelo, più tentativi con
attesa). Ogni giro tiene in cache quello che ha già scaricato.

**I test** (`testDebugUnitTest`) girano anche lì, compresi quelli del
repository su un database vero (Robolectric, `PageRepositoryTest`): sono
l'unico modo di provare in cloud le operazioni che toccano i dati. La
prima volta Robolectric scarica circa 150 MB.

**Guardare una schermata senza telefono** *(dal 24/09/2026)*:
`WidgetsRenderTest` disegna i widget della barra laterale con
Robolectric e salva le immagini in `app/build/widget-screenshots/`, che
poi si aprono e si guardano. Lo stesso schema (`@GraphicsMode(NATIVE)`,
la finestra disegnata a mano su una bitmap, perché `captureToImage` lì
non finisce mai) serve per qualsiasi altro pezzo di interfaccia. Ha già
scovato un errore di impaginazione prima del telefono.

**L'APK che esce da lì non va installato sopra l'app del telefono**:
è firmato con una chiave di debug nata sul server, diversa da quella
del PC, e Android costringerebbe a disinstallare l'app — note comprese. Ogni modifica fatta lì è segnata **"(da
  verificare sul telefono)"** e il **"Diario delle sessioni cloud"**
  più in basso elenca, sessione per sessione, le richieste dell'utente
  parola per parola, cosa è stato fatto e cosa va provato. **Chi
  riprende sul PC dopo una sessione cloud parte da lì**: prima si
  provano sul telefono le voci ancora da verificare, poi si va avanti.

### ⛔ Il confine: da qui comincia il lavoro fatto in cloud

**Ultimo stato provato sul telefono** = il codice come stava nella
cartella `Downloads\NotionLocal` del PC il **24/09/2026**, caricato su
GitHub così com'era:

- archivio originale: commit `83f7b61` ("Add files via upload") sul
  ramo `main` — non cambia più, è la prova di com'era;
- stesso codice estratto nel repository: commit `4a35c64` ("Importa il
  progetto notE (NotionLocal) dall'archivio caricato"), identico
  all'archivio salvo gli a-capo.

**Tutto quello che viene dopo `4a35c64` è lavoro cloud: scritto e
compilato (per la sessione del 24/09/2026: vedi Diario), MAI installato
né provato sul telefono.** Va
trattato come una bozza finché non passa la procedura qui sotto. Per
vedere in un colpo solo tutte le modifiche al codice fatte in cloud:
`git diff 4a35c64 HEAD -- app/` (se quel commit non si trova più,
per esempio dopo un "Squash and merge", si confronta col contenuto
dell'archivio in `83f7b61`).

**Aggiornamento del 24/09/2026 sera — ripresa sul PC.** Il lavoro cloud
è stato **provato sul telefono** con la build del PC: le migrazioni
24→29 su un database vero, e le voci "Da verificare" in buona parte
(esito voce per voce nel Diario, sopra l'elenco; i tre bug trovati e
corretti in Cronologia, "Ripresa sul PC del 24/09/2026"). Sul telefono
ora c'è la build **firmata con la chiave di debug del PC** (non più
quella del cloud: notE non era installata), con note di prova: le
prossime installazioni dal PC la aggiornano senza chiedere di
disinstallare. Restano da provare le voci segnate 🟡 e ⏸️.

**Dal 24/09/2026 (richiesta 38) sul telefono può esserci l'APK fatto in
cloud** (commit `0557c35`), installato dall'utente dopo aver
disinstallato notE, con le sole note di prova. È firmato con una chiave
di debug nata sul server cloud (e ogni sessione cloud ne crea una nuova):
**la prossima installazione da Android Studio sul PC chiederà di nuovo
di disinstallare**, e le note sul telefono andranno perse — prima si
copia il database (vedi "Nota tecnica") se dentro c'è qualcosa che
serve. Da lì in poi la chiave torna quella del PC e le installazioni
successive dal PC tengono le note.

### Ripresa sul PC dopo il cloud — procedura obbligatoria

Richiesta esplicita dell'utente: il lavoro cloud **va ricontrollato
alla perfezione sul telefono** prima di costruirci sopra.

1. **Clonare il repository** in una cartella nuova (vedi "Come
   aprirla") e sincronizzare Gradle con la JDK 21.
2. **Collegare il Galaxy S25 Ultra** col debug USB o il debug wireless
   e controllare che `adb devices` lo veda.
3. **Copiare il database dal telefono PRIMA di installare** (comandi
   e trappole in "Nota tecnica": su Windows passa da `cmd /c`, servono
   anche `-wal` e `-shm`). Obbligatorio se il lavoro cloud ha cambiato
   lo schema; conviene sempre, perché lì dentro ci sono note vere.
4. **Installare da Android Studio (▶ Run)**. La build firmata con la
   chiave di debug del PC si installa sopra l'app esistente e **le note
   restano**. Un APK compilato altrove (per esempio in cloud) ha una
   firma diversa, Android costringe a disinstallare e **le note si
   perdono**: non farlo.
5. **Se lo schema è cambiato**, verificare che la migrazione sia
   avvenuta davvero (`user_version` e colonna in fondo al
   `CREATE TABLE`, vedi "Nota tecnica").
6. **Provare una per una, sul telefono e con `logcat` aperto, tutte le
   voci "Da verificare sul telefono"** del Diario delle sessioni cloud,
   dalla più vecchia alla più recente. Rileggere anche il diff del
   punto sopra con occhio critico: chi l'ha scritto non ha potuto
   vederlo girare. Massima attenzione a `MergedTextRunField`,
   `updateRun`, alla mappatura delle posizioni e alle migrazioni —
   sono i punti dove un errore arriva ai dati (vedi "Strade già
   tentate").
7. **Aggiornare il README con l'esito**: accanto a ogni voce provata
   "(verificato sul telefono il gg/mm/aaaa)", e ogni problema trovato
   diventa un `[Bug fix]` in Cronologia.
8. Solo a quel punto si riprende con lavoro nuovo.

### Decisioni di design deliberate (non sono bug)

**I "blocchi di testo scorrevole" sono uniti in un campo condiviso.**
Paragrafi, titoli e elementi di lista consecutivi vivono in UN SOLO
`BasicTextField` (vedi `MergedTextRunField`), non uno per blocco. Sono
righe separate nel database, ma un unico campo nell'editor. Questa è
la scelta architetturale più importante del progetto ed è stata presa
DOPO aver provato l'alternativa: con un campo per blocco, selezionare
testo a cavallo di due blocchi e usare backspace per unirli sono
impossibili, perché sono due campi che non si parlano. Non è un
problema risolvibile con una correzione mirata — ci sono stati diversi
tentativi falliti prima di capirlo.

**Checkbox, toggle, tabelle, divisori, link sono "isole".** Restano
campi separati, fuori dal flusso di testo continuo, gestiti da
`BlockRow`. È deliberato: un elemento interattivo (una casella
cliccabile, un toggle comprimibile) non si "scrive" dentro un flusso
di testo con la stessa naturalezza. La scelta è stata discussa
esplicitamente con l'utente, che ha preferito questa via alla
complessità di integrarli nel testo continuo.

**Il campo di testo unito comincia con un a-capo in più, nascosto, e
serve.** `RUN_LEAD`: il testo di `MergedTextRunField` è sempre `"\n"` +
una riga per blocco. Quell'a-capo non appartiene a nessun blocco, non
viene mostrato (la mappatura delle posizioni lo salta) e non viene
salvato. Esiste per un solo motivo: dare al backspace premuto a inizio
della PRIMA riga un carattere da cancellare, così quel gesto produce un
cambiamento di testo. Senza, il testo resterebbe identico e non ci
sarebbe nulla da osservare, dato che gli eventi-tasto non arrivano.
**Non rimuoverlo pensando che sia un residuo**, e se lo si sostituisce,
sostituirlo solo con un altro separatore di parole — un carattere che
non spezza le parole viene inghiottito dalla tastiera (vedi "Strade già
tentate"). Conseguenza pratica: il cursore a posizione zero è
impossibile per qualsiasi altra via, ed è proprio ciò che rende il
riconoscimento del gesto inequivocabile.

**Il rientro (Tab) del testo scorrevole è un livello visivo, non
nidificazione vera.** `BlockEntity.indentLevel` è un numero, come Tab
in Word. Per le isole (toggle) resta invece la nidificazione vera via
`parentBlockId`. Semplificazione deliberata, conseguenza del punto
sopra.

### Strade già tentate che NON funzionano

**Dare spazio a una riga sola dentro il campo di testo condiviso,
toccando la trasformazione visiva.** Provate due strade: uno
`ParagraphStyle(lineHeight = ...)` sulle righe con la casella, e un
carattere invisibile ingrandito in fondo al marcatore. In tutti e due i
casi il campo ha **smesso di disegnare**: righe vuote sullo schermo,
nessun errore in `logcat`, nessun crash. E il guaio vero viene dopo —
quella vuotezza è stata presa per una modifica dell'utente e **scritta
nel database**, cancellando il testo dei blocchi e trasformando le
caselle in paragrafi. Lo spazio si mette invece come `lineHeight` nel
`textStyle` **dell'intero campo** (vedi `CHECKBOX_LINE_HEIGHT`), che non
tocca né il testo trasformato né la traduzione delle posizioni.

Lezione più larga: il campo condiviso **riscrive i blocchi a partire da
quello che ha in mano**. Se la trasformazione sbaglia, il danno non
resta sullo schermo, arriva ai dati. Qualsiasi modifica lì dentro va
provata con il database sotto controllo, non solo guardando la pagina.

**Avviare l'app con `monkey` per provarla via adb.**
`adb shell monkey -p com.gabriele.notionlocal -c android.intent.category.LAUNCHER 1`
è l'idioma diffuso per "apri l'app", ma quel numero finale è il
**conteggio di eventi casuali da iniettare**, non un numero di
ripetizioni: ogni avvio spara un tocco o un tasto a caso dentro l'app.
Il 22/09/2026 quattro avvii così hanno fuso cinque caselle della pagina
radice in una sola e l'hanno svuotata — perdita di dati vera, che
sembrava un difetto dell'app. Si avvia con
`adb shell am start -n com.gabriele.notionlocal/.MainActivity`.
Vale anche la regola generale: **guidare l'app con tocchi alla cieca a
coordinate fisse**, senza uno screenshot di controllo fra un passaggio e
l'altro, fa danni sui dati veri. Prima si copia il database.

**Intercettare il tasto Backspace via `onPreviewKeyEvent` o
`onKeyEvent`.** Provato due volte, in entrambe le fasi (tunnel e
bubble). Sulla tastiera Samsung non arriva alcun evento rilevabile per
"backspace su campo vuoto" — la tastiera comunica in un modo che salta
del tutto il sistema di eventi-tasto di Compose. Peggio: la semplice
presenza di quell'intercettazione sembrava disturbare l'input normale.
Rimosso del tutto. Oggi backspace-tra-blocchi funziona perché è una
CONSEGUENZA del testo che cambia nel campo unito (vedi
`PageEditorViewModel.updateRun`, ramo "meno righe che blocchi"), non
perché qualcuno intercetta un tasto.

**Usare uno ZERO-WIDTH SPACE come carattere nascosto in testa al campo
di testo unito.** Tentato il 2026-09-18 per risolvere "backspace non
toglie il marcatore al primo elemento". L'idea di fondo era giusta e
oggi è in uso (vedi `RUN_LEAD` sopra), ma **il carattere scelto era
sbagliato**: lo zero-width space non spezza le parole, quindi la
tastiera Samsung lo ingloba nella parola che sta componendo e lo
sostituisce insieme al resto ad ogni tasto. Il codice se ne accorgeva e
rimetteva il testo a posto, cioè litigava con la tastiera ad ogni
carattere, azzerando la regione di composizione. Sintomi osservati sul
dispositivo: primo Invio ignorato (il secondo funzionava), elenchi che
non continuavano, backspace che cancellava molto più del dovuto —
esattamente il disturbo all'input già visto con l'intercettazione dei
tasti. Sostituito con un a-capo, che le parole le spezza sempre.
**Morale: qualunque carattere nascosto si usi lì dentro deve essere un
separatore di parole, altrimenti l'IME se lo mangia.**

**Capire "quale riga è cambiata" dal solo confronto fra prima e dopo.**
Non basta, e il caso che lo dimostra è banale: **una riga vuota
inserita in mezzo ad altre righe vuote è identica a tutte le sue
vicine.** Il confronto allora attribuiva l'inserimento all'ultima riga
del gruppo, e la riga nuova ereditava il tipo dal blocco lì in fondo
invece che da quello su cui si stava scrivendo. Sintomo: `0.` (o
qualsiasi numero) creava la lista, ma l'Invio dopo dava un paragrafo e
la lista si fermava — e solo su pagine che sotto avevano righe vuote,
che è la ragione per cui a volte si presentava e a volte no. Ora il
campo passa anche **dov'è il cursore** (`caretLine`): la modifica non
può essere dopo di lui, e l'ambiguità sparisce.

**Attenzione alla diagnosi comoda.** Lo stesso sintomo era stato
attribuito prima al ritardo del salvataggio (il database indietro di
qualche decina di millisecondi rispetto al campo). Era una spiegazione
plausibile, coerente col fatto che pilotando l'app da adb non si
riproduceva — e sbagliata: la correzione fatta su quella base non ha
cambiato niente, e solo il log ha mostrato la vera causa (l'Invio
finiva `mid=25` quando il cursore era alla riga 20). Il confronto con
il testo del campo (`linesBefore`) è rimasto perché è comunque più
corretto del confronto col testo salvato, ma **non era quello il
problema**.

**Fidarsi della lista di blocchi che la UI passa al ViewModel.** La
UI può essere indietro di un istante (es. si scrive subito dopo un
Invio, prima della ricomposizione). Il codice originale scartava la
modifica in silenzio in quel caso — causa profonda di una serie di
sintomi apparentemente scollegati (Invio da premere due volte, liste
che non continuavano, tipo di blocco ereditato a caso). `updateRun`
ora ricostruisce il gruppo reale dallo stato corrente. **Se si
toccano `updateRun` o `MergedTextRunField`, non reintrodurre
l'assunzione che la lista ricevuta sia aggiornata.**

**Risincronizzare il campo col database ad ogni emissione.** Faceva
sovrascrivere l'a capo appena digitato quando il salvataggio era
ancora in viaggio. Ora `MergedTextRunField` tiene traccia di
`pendingText` per distinguere "database indietro" da "contenuto
cambiato davvero da fuori" (Annulla/Ripristina, segnalato da
`externalChangeTick`).

**Impostare `pendingText` quando non è partito nessun salvataggio.**
`onValueChange` scatta anche quando si sposta solo il cursore o si
seleziona. Se lì si mette `pendingText`, dal database non torna mai
niente che lo azzeri, e da quel momento il campo ignora ogni
cambiamento fatto **da fuori** (menu del blocco: Duplicate, Turn into):
mostra le righe vecchie, e alla prima lettera `updateRun` confronta
righe e blocchi che non combaciano e **cancella o sposta blocchi**
(trovato sul telefono il 24/09/2026: sparita la copia appena
duplicata). `pendingText` si mette **solo se il testo è cambiato**. Chi
aggiunge un'azione che cambia i blocchi di un gruppo da fuori deve
chiedersi che cosa vedrà il campo quando arriva il nuovo stato.

**Rimettere una foto dei blocchi nell'ordine di `orderIndex`.**
`orderIndex` riparte da 0 dentro ogni toggle, quindi in una lista
ordinata solo per lui un figlio può venire prima del suo genitore; se
la si reinserisce così, la chiave esterna verso il genitore la rifiuta
e l'app si chiude (Annulla, fino al 24/09/2026). Per reinserire blocchi
si usa `inDocumentOrder`: genitori prima, figli subito dopo.

**Salvare i blocchi uno alla volta quando la modifica ne tocca
tanti.** Room avvisa chi osserva la tabella **ad ogni scrittura**:
rinumerando i venti blocchi sotto a quello diviso, un solo Invio
faceva ridisegnare la pagina venti volte di fila. Dentro quella
tempesta il campo su cui si sta scrivendo viene ricomposto di
continuo, il fuoco non arriva al blocco nuovo e le lettere successive
finiscono in quello vecchio. Le modifiche che toccano più blocchi
vanno in **una transazione sola** (`PageRepository.applyBlockChanges`),
e la rinumerazione è **una query sola**
(`BlockDao.shiftOrderIndexes`), non una riga alla volta: così l'avviso
è uno, e i numeri li calcola il database invece della copia in memoria
della UI — che due Invii ravvicinati rendevano vecchia, lasciando due
blocchi con lo stesso numero.

**Dare il fuoco a una riga che non si vede.** `LazyColumn` costruisce
solo le righe dentro lo schermo: il blocco nato premendo Invio in
fondo alla parte visibile spuntava dietro la tastiera, non veniva mai
costruito, e una riga che non esiste non può prendersi il cursore.
Verificato col log: la richiesta di fuoco partiva, e fra le righe
composte quella nuova non c'era. Prima del fuoco bisogna **portare la
riga dentro lo schermo** (`animateScrollToItem`); costruita quella, il
fuoco la trova.

**Tenere la pagina in una variabile letta una volta sola, e salvarla
intera ad ogni modifica.** È come stavano `DatabaseViewModel` e
`PageEditorViewModel`, ed è una perdita di dati silenziosa: la stessa
pagina può stare aperta in due schermate insieme (un database dentro
una pagina e lo stesso database a schermo intero, o la stessa pagina
due volte nella pila di navigazione), ognuna col suo ViewModel. Quella
rimasta indietro non mostra solo un titolo vecchio: alla prima
modifica — anche di tutt'altro campo, perché si scrive la riga intera
— lo rimette nel database, e il nome nuovo sparisce. **Se si aggiunge
un ViewModel che tiene una pagina, la si segue con
`PageRepository.observePage`, non con `getPage`.** E il campo di testo
del titolo tiene una copia locale finché ha il fuoco: il valore che
torna dal database arriva un attimo dopo e, se nel frattempo è stato
battuto un altro tasto, rimetterebbe il testo indietro mangiandosi il
carattere (è la stessa trappola del `pendingText` qui sopra).

**Intercettare il tasto indietro con un `BackHandler` mentre la
tastiera è aperta.** Non arriva. Con la tastiera a schermo il tasto (o
il gesto) indietro lo riceve **per prima la tastiera**, che se lo mangia
per chiudersi, e all'app non arriva niente: il `BackHandler` non viene
mai chiamato. Verificato col log — l'evento di fuoco arrivava
(`hasFocus=true`), il gesto no. Serve solo per il caso in cui il cursore
è acceso ma la tastiera è già chiusa. Se quello che si vuole è reagire
al "l'utente ha finito di scrivere", il segnale giusto è **la tastiera
che si chiude** (`WindowInsets.isImeVisible`), non il gesto.

### Misurare la fluidità, non guardarla

Lo scatto nello scorrimento della settimana è costato **tre correzioni
sbagliate di fila**, tutte plausibili e tutte inutili, perché il modo di
verificare era sbagliato: `adb shell input swipe` produce un
trascinamento perfettamente lineare e senza spinta, molto più gentile di
un dito vero, e le misure sui fotogrammi risultavano pulite mentre
l'utente continuava a vedere lo scatto.

Gli strumenti che invece dicono la verità:

```
adb shell dumpsys gfxinfo com.gabriele.notionlocal reset
(fai i gesti)
adb shell dumpsys gfxinfo com.gabriele.notionlocal
```

Conta i fotogrammi persi davvero. Sul calendario a settembre 2026 dava
**3,6% di fotogrammi persi e 99° percentile a 23 ms**: su uno schermo a
120 Hz il budget è 8 ms, quindi lo "scatto" non era un errore di logica
ma lavoro di troppo per fotogramma.

Per vedere *dove* si muove il contenuto, la correlazione incrociata fra
fotogrammi successivi (non il centroide dei pixel chiari, che si sposta
da solo appena entra contenuto nuovo):

```
adb shell screenrecord --time-limit 4 /sdcard/r.mp4
ffmpeg -i r.mp4 -vsync 0 -vf "crop=1440:46:0:1488,format=gray" -f rawvideo strip.raw
```

e poi si cerca lo spostamento che massimizza la sovrapposizione dei
profili di colonna. Uno scorrimento sano dà una serie monotona che
decelera; uno scatto dà un fotogramma fermo seguito da un passo doppio.

**Attenzione a due trappole**: `screenrecord` chiude il file riemettendo
un fotogramma vecchio, quindi l'ultimo fotogramma mostra sempre la
schermata di partenza (artefatto, non l'app); e una build di debug di
Compose perde più fotogrammi di una release — qui 6,2% contro 3,2%,
misurato firmando la release con la chiave di debug.

### Peculiarità dell'ambiente di test

- La **tastiera Samsung** ha un comportamento documentato e
  problematico con Compose: caratteri che si duplicano quando il campo
  attivo cambia troppo a ridosso di un tasto premuto. Mitigato con
  `IME_SETTLE_DELAY_MS` (60ms) prima di ogni spostamento di focus in
  `PageEditorViewModel`. Non è garantito che risolva in ogni caso — la
  causa vive nella tastiera, non nel codice.
- `keyboardController?.show()` va chiamato **solo** se la tastiera non
  è già visibile: chiamarlo a tastiera aperta disturba l'input.
- **`Modifier.clickable` non risponde solo al tocco**: si attiva anche
  col tasto Invio quando un elemento interno ha il focus. Su un
  contenitore che racchiude campi di testo è una trappola — usare
  `detectTapGestures`. Ha già causato un bug (vedi cronologia)
- Il cursore di `BasicTextField` è **nero di default** — invisibile su
  sfondo scuro finché non si imposta `cursorBrush` esplicitamente.

### Cosa l'utente ha chiesto di tenere per ultimo

- **Selezione di testo che scavalca le isole** (es. dal testo dentro
  un toggle al testo fuori). Discusso: richiederebbe di unire anche i
  contenuti dei toggle nel campo condiviso. L'utente ha accettato di
  rimandarlo a fine progetto.
- **Evidenziazione di selezione stretta sul testo** invece che allargata
  a tutta la larghezza (Notion fa così, Android di default no).
  Verificato fattibile (disegnando l'evidenziazione a mano da
  `TextLayoutResult`), ma è puramente estetico e l'utente ha chiesto
  di metterlo in fondo alla lista.

### Regola di lavoro concordata

**Aggiornare questo README ad ogni modifica dell'app**, anche se non
verificata sul dispositivo, etichettando ogni voce come
`[Nuova funzionalità]` o `[Bug fix]`.

Ribadita ed estesa dall'utente il 24/09/2026: **va scritta filo per
filo e per segno QUALSIASI cosa** — ogni modifica, ogni aggiunta, ogni
cosa tolta, ogni bug trovato (anche se non ancora corretto), e anche i
cambiamenti che non toccano l'app (configurazione, repository, questo
stesso README). Per questi ultimi si usano due etichette in più:
`[Rimosso]` per ciò che viene tolto dall'app e `[Progetto]` per ciò che
sta attorno al codice. Dal 19/10/2026 (chiesto dall'utente) c'è anche
**🔴 [Da rivedere]**, rossa: una scelta fatta fra due strade, che l'utente
vuole riguardare più avanti per decidere se tenerla, con l'altra strada
scritta accanto. Nelle Patch Notes dell'app la scritta è rossa. Nelle sessioni cloud si trascrivono anche **le
richieste dell'utente** (vedi "Diario delle sessioni cloud"). Lo scopo
dichiarato: tornando su Claude Code non si deve ricominciare niente da
capo.

**Dal 30/09/2026 ogni voce della Cronologia è una versione** (chiesto
dall'utente, vedi v0.84 e v0.85.1): il titolo è `### 🆕 v0.N —
gg/mm/aaaa · hh:mm — Titolo`. **Dal 21/10/2026 il numero dice che cosa
porta la versione** (chiesto dall'utente, che ha fatto rileggere e
rinumerare tutta la Cronologia):
- 🆕 **v0.N**, uno in più al secondo numero: **un'aggiunta**, qualcosa che
  l'app prima non faceva;
- ✏️ **v0.N.M**, uno in più al terzo: **una modifica** di qualcosa che
  c'era già (come si vede, come si comporta, dove sta, toglierlo);
- 🔧 **v0.N.M.K**, uno in più al quarto: **solo correzioni di bug**.

Una versione con più cose prende il numero di quella più grande (con
un'aggiunta è 🆕 anche se corregge dei bug). Ogni voce di una versione
ha la sua etichetta (`[Nuova funzionalità]`, `[Modifica]`, `[Rimosso]`,
`[Bug fix]`; `[Progetto]` per come è fatta) e, **dal 21/10/2026, la
piattaforma** a cui si riferisce: `[📱 Mobile]` l'app per il telefono,
`[💻 PC]`, `[🍎 Mac]`, `[🌐 Web App]`, più d'una insieme o `[🌍 Tutte]`.
Per ora c'è l'app per il telefono; quella per il PC (Windows e Mac) è in
costruzione dal 04/11/2026 (v0.153). Il giorno e l'ora sono quelli
(italiani) del commit che chiude la voce; la voce nuova va in cima. La
Cronologia è anche **Settings → Patch Notes** nell'app, che la copia a
ogni compilazione, e `PatchNotesTest` controlla che numeri, date e
ordine tornino. I commit prima del 21/10/2026 hanno il numero vecchio:
vedi "Vecchia numerazione", dopo la Cronologia.

---

## Come aprirla

1. Installa **Android Studio**: https://developer.android.com/studio
2. Prendi il progetto da GitHub: `File → New → Project from Version
   Control`, URL `https://github.com/AbernathyXVII/MyNotesApp`, e
   scegli una cartella **nuova** (non la vecchia
   `Downloads\NotionLocal`, che non è collegata al repository). Se il
   progetto è già stato clonato, basta aprirne la cartella con
   `File → Open` e aggiornarla con `Git → Pull`.
   **Il ramo giusto**: il lavoro cloud sta su
   `claude/funny-ramanujan-uuwahs` finché la sua pull request non è
   unita in `main`. Se non lo è, dopo aver clonato si passa a quel ramo
   (`git checkout claude/funny-ramanujan-uuwahs`, o in basso a destra in
   Android Studio) — su `main` ci sarebbe il codice di prima del cloud
3. Se richiesto, seleziona una JDK 21 per Gradle (Gradle 8.14, dal
   03/11/2026). Va rifatto ad ogni estrazione fresca del progetto:
   l'impostazione vive in `.idea/`
4. Aspetta la sincronizzazione Gradle (la prima volta scarica le
   dipendenze — serve connessione internet)
5. Collega il telefono con debug USB attivo, premi ▶ Run
6. **L'app per il PC** (dal 04/11/2026, in costruzione) si prova con
   `gradle :desktop:run`, o in Android Studio dal pannello Gradle:
   `desktop → Tasks → compose desktop → run`

**Le versioni** (dal 03/11/2026, v0.152.3.1): Kotlin 2.4.21, Compose
1.11 (BOM 2026.06.01), plugin Android 8.13.2, Gradle 8.14.3. **Compose
resta alla 1.11** finché non si passa al plugin Android 9.1 e a
compileSdk 37, che la 1.12 richiede, e deve andare d'accordo con Compose
Multiplatform (la 1.11.x), con cui si farà l'app per Windows e Mac.

## Cosa fa l'app oggi

- **Un'unica pagina radice** ("My Space", titolo modificabile) fa da
  Home — non esiste una schermata Home separata: si comporta come
  qualsiasi altra pagina, con lo stesso editor
- **Testo scorrevole unificato**: paragrafi, elenchi puntati e
  numerati e caselle consecutivi vivono in un unico campo di testo
  condiviso — selezione e backspace-tra-blocchi funzionano in modo
  nativo, come in un editor di testo normale
- **Isole separate**: checkbox, toggle annidabili, tabelle semplici
  ridimensionabili, divisori, blocchi Page/Database (creano una
  sottopagina o un database e ci navigano dentro subito, come in
  Notion)
- **Backspace a inizio riga** smonta la riga un gradino alla volta:
  prima toglie il marcatore di elenco, poi i rientri uno per uno, e
  solo quando non resta niente fonde con la riga sopra — come in
  Notion, anche sul primo elemento in cima alla pagina
- **Scrivere `1.` a inizio blocco** crea un elenco numerato; scrivendo
  un altro numero (`5.`, e anche `0.`) l'elenco parte da lì e prosegue
  normalmente. Solo se il numero è l'unica cosa scritta nel blocco
- **Invio** crea un nuovo blocco ereditando il tipo se è un elenco;
  **incollare testo multi-riga** separa correttamente in più blocchi
- **Rientro (Tab)** per il testo scorrevole è un livello visivo sul
  paragrafo; per le isole (toggle) è nidificazione vera. Sugli elenchi
  numerati il livello di rientro decide anche la numerazione: `1.`,
  `1.1`, `1.1.1`
- **Formattazione inline ("Aa")**: grassetto, corsivo, sottolineato,
  barrato, spoiler e *(sessione cloud del 24/09/2026, provato sul
  telefono il 24/09/2026)* pedice e apice — funziona sia nel testo unito (la selezione, anche se
  attraversa più blocchi, viene tradotta per ciascuno) sia nelle isole
- **Undo/Redo** per testo e struttura dei blocchi (non celle di
  tabelle/database), con la digitazione continua raggruppata in un
  unico passo
- **Numerazione delle liste** su più livelli (`1.`, `1.1`, `1.1.1`)
  guidata dal rientro; **tenendo premuto sul numero** si apre il menu
  della lista — far ripartire il conteggio da lì, rimetterlo in fila,
  togliere il numero, passare al pallino — come su OneNote
- **Indenta/disindenta, sposta su/giù, Undo/Redo, menu "+", Aa** dalla
  barra sopra la tastiera (scorrevole orizzontalmente)
- **Il menu del blocco** *(sessione cloud del 24/09/2026, provato in
  parte sul telefono il 24/09/2026)*: i **sei puntini** all'inizio della barra aprono il menu
  della riga in cui sta il cursore — Turn into (le voci del "/"), Color
  su tutta la riga, Duplicate. **Tenendo premuto il collegamento a una
  pagina** si apre quello della pagina: Color, Edit icon, preferiti,
  Rename, Duplicate, Move to, Move to trash. Per un database (sei puntini
  nella sua barra, o collegamento tenuto premuto) in cima anche Turn into
  page, Turn into simple / complex database, Lock database, Open as page
- **Tocco ovunque sotto il titolo** per iniziare/continuare a scrivere
- **Database semplice** *(sessione cloud del 24/09/2026, provato in
  parte sul telefono il 24/09/2026)*: "Simple database" nei menu "/" e "+" crea un database
  con tutto quello che hanno gli altri — viste, filtri, ordinamento,
  raggruppamento, proprietà — ma **le cui righe sono solo testo e non
  diventano mai pagine**: niente OPEN, niente icone o copertine di riga;
  toccando una riga si apre la sua scheda, col nome da scrivere e tutte
  le proprietà. Una cosa che Notion non ha
- **Ricerca dentro un database** *(sessione cloud del 24/09/2026,
  provata in parte sul telefono il 24/09/2026)*: la lente accanto a Sort apre lì una barra;
  scrivendo restano le righe che hanno la parola nel nome, in una
  proprietà o dentro la loro pagina, con il nome illuminato di giallo se
  è lì, o tutta la riga se la parola è dentro. In tutte e sei le viste
- **Viste collegate** *(sessione cloud del 24/09/2026, provate in parte
  sul telefono il 24/09/2026)*: "Linked view of data source" nei menu "/" e "+" mette in
  una pagina **lo stesso database** che sta in un'altra — righe,
  proprietà, pagine e quello che c'è scritto dentro sono gli stessi — con
  filtro, ordinamento, raggruppamento, vista e proprietà nascoste suoi.
  Il titolo "↗ Nome" porta al database principale
- **Widget in cima alla barra laterale** *(sessione cloud del
  24/09/2026, provati in parte sul telefono il 24/09/2026)*: fusi orari (orologi
  analogici o digitali, e un elenco sempre da ovest a est), avanzamento
  di anno, mese, settimana, giorno e date proprie, contatore, pomodoro
  con sessione e pausa (e dal 25/09/2026 il suo suono, scelto fra le
  suonerie del telefono), e dal 25/09/2026 **cronometro** coi giri e
  **timer** (anche più d'uno insieme), come quelli dell'orologio Samsung;
  i timer aggiunti col "+", dal secondo in poi, si eliminano col cestino
  in alto a destra, il primo resta sempre.
  Una freccetta nasconde tutta la sezione, e la scelta resta
- **Database dentro le pagine**: creandone uno dal menu "+" compare
  nella pagina e si modifica lì; l'icona con le frecce lo apre a
  schermo intero, e dalle impostazioni lo si elimina
- **Database** con la schermata modellata sulla UI mobile di Notion:
  titolo modificabile, tabella scorrevole, `+ Add property` in coda
  alle colonne, `OPEN` per aprire una riga come pagina vera,
  `+ New page` in fondo. Toccando un'intestazione si apre la finestra
  della proprietà (nome, tipo con ricerca, elimina; dal 24/09/2026 non
  più "sposta", che sta nel menu dell'intestazione tenuta premuta). Dodici
  tipi: Text, Number, Select, Multi-select, Date, Checkbox, **Color cell**
  (dal 26/09/2026: la cella piena di un colore, con sopra del testo se si
  vuole), URL, Email, Phone, Created time, Last edited time. Dal
  21/10/2026 (v0.125) anche **Relation**, che collega le righe a quelle di
  un altro database (o dello stesso), anche nei due sensi, e **Rollup**,
  che fa un calcolo sulle righe collegate
- **Sei viste per i database** — Table, List, Board, **Calendar**,
  **Timeline** e **Gallery**, una per volta, scelte da Settings → Layout
  e ricordate per ogni database
- **Gallery** *(sessione cloud del 24/09/2026, provata in parte sul
  telefono il 24/09/2026: mancano le copertine)*: le pagine come schede in griglia, con in alto la
  **copertina** della pagina (con l'inquadratura scelta in
  "Reposition") oppure **l'inizio del suo testo**, scritto come nella
  pagina. Sotto, nome e proprietà. Dalle impostazioni della vista si
  sceglie **Card preview** (None / Page cover / Page content) e **Card
  size** (Small / Medium / Large: tre, due o una scheda per riga su un
  telefono in verticale). Si crea anche dal menu "/" con "Gallery view".
  Dal 25/09/2026, tenendo premuta una scheda, **"Reposition cover"**
  sposta e ingrandisce la copertina nella sola scheda
- **Data con ora facoltativa**: l'interruttore "Include time" nella
  finestra della data aggiunge l'orario all'inizio e alla fine. Spento,
  la data vale il giorno intero. **Selettori come quelli di Samsung**
  (dal 25/09/2026): il mese a griglia con le frecce e lo scorrimento
  orizzontale, toccando il mese le rotelle di giorno, mese e anno; l'ora a
  rotelle, un minuto alla volta. La data di fine si chiede con
  l'interruttore "End date"
- **Disegno a mano** (dal 25/09/2026): "Draw" nei tre puntini di una
  pagina. Penna, matita, pennarello ed evidenziatore, con spessore e
  colore; gomma per tratti o a punto; Annulla e Ripeti. La S Pen (con la
  pressione, e il tasto laterale per cancellare) e un dito disegnano, due
  dita scorrono; il palmo appoggiato mentre si usa la penna non lascia
  segni. I disegni seguono le righe, il database o il titolo su cui li si è
  fatti
- **Notifiche** (dal 25/09/2026): la riga **`/notification 22.10.2026
  19:41 Buttare l'immondizia`** (Invio, e diventa una riga con la
  campanella; senza ora alle 9:00; senza testo mostra il nome della
  pagina), le date dei database con **"Include notification"** (col nome
  della pagina della riga, all'inizio e alla fine), e la fine di timer e
  fasi del pomodoro. Arrivano anche ad app chiusa, e toccandole si apre la
  pagina
- **Quaderni come in OneNote** (dal 26/09/2026): "Notebooks" nella barra
  laterale, con quaderni, gruppi di sezioni, sezioni colorate e pagine che
  si allungano man mano che si scrive, col giorno e l'ora sotto il titolo.
  Un mondo a parte dal menu principale: non si cercano da lì e non si
  collegano alle sue pagine
- **Pagine e database bloccati** (dal 26/09/2026): "Lock with password"
  nei tre puntini; si aprono con l'impronta o con la password, scelta la
  prima volta e cambiata o tolta da Settings. Chiudono anche quello che
  hanno dentro; un database bloccato dentro una pagina diventa un
  collegamento, e nella galleria la loro scheda mostra un lucchetto bianco
  al posto della copertina. Aperte, hanno un lucchetto chiuso accanto ai
  tre puntini
- **Timeline**: il calendario srotolato. Un asse del tempo che scorre
  in orizzontale, una riga per pagina con la barra intera (non spezzata
  a fine settimana), i nomi in una colonna ferma a sinistra, i fine
  settimana in ombra e una riga verticale sull'ora di adesso. Si apre
  già posizionata su adesso, e il pulsante **Today** ci riporta
- **Sette ingrandimenti della Timeline** — Hours, Day, Week, Bi-week,
  Month, Quarter, Year — scelti dalla stessa finestra di Notion
  ("Select a zoom level") e ricordati per ogni database
- **Calendario ad anno, mese o settimana**, con le pagine disegnate come
  **barre lunghe quanto i giorni che coprono**: una proprietà data può
  avere inizio e fine ("Date started"/"Date finished"), e la barra le
  rispetta, spezzandosi a fine settimana e restando squadrata dal lato
  dove prosegue. Toccando un giorno vuoto si crea lì una pagina, nella
  settimana si passa da una all'altra **trascinando col dito**, e
  l'anno mostra dodici mesi in miniatura con i giorni occupati cerchiati
- **Icona e copertina per ogni pagina**, radice compresa: si toccano
  per cambiarle, e l'immagine arriva dalla **galleria del telefono**
  ("Upload"), di cui l'app tiene una copia. Un PNG trasparente resta
  trasparente. (Fino al 25/09/2026 anche da un link: tolto su
  richiesta, e con lui il permesso di rete. La rete è tornata il
  06/10/2026, v0.91, solo per i video presi da un link e per le
  anteprime dei segnalibri)
- **La copertina si inquadra**: da "Reposition" la si sposta col dito
  in tutte le direzioni e la si ingrandisce pizzicando, fino a
  quattro volte. Il file non viene mai ritagliato né ridotto — nel
  database finiscono solo tre numeri, e l'immagine resta intera
- **Ordinamento delle righe** per una qualsiasi proprietà, crescente o
  decrescente, dall'icona Sort: alfabetico per le parole, numerico per i
  numeri, cronologico per le date
- **Raggruppamento della tabella** per una qualsiasi proprietà: ogni
  gruppo ha la sua intestazione col valore, il conteggio e il suo
  "+ New page", che crea la pagina già dentro quel gruppo
- **Date che si ripetono**, con le scorciatoie di Google Calendar e una
  schermata per le regole su misura. La pagina resta una sola: compare
  su tutte le date della regola e modificarla le cambia tutte
- **Filtri** sulle proprietà: un intervallo di numeri (da x a y, con un
  estremo facoltativo), i giorni di una data (uno solo o da un giorno a
  un altro), i tag di un Select o MultiSelect, e le caselle spuntate o
  non spuntate. Si aprono dall'icona a sinistra di Sort o dalla voce
  "Filter" nelle impostazioni — è la stessa finestra — e si buttano col
  cestino in alto a destra
- **Colonne nascondibili**: tenendo premuta l'intestazione di una
  proprietà compare "Hide", e in "Property visibility" (impostazioni
  della vista) ci sono le due liste *Shown in table* e *Hidden in
  table* da cui rimetterle a posto, una per volta o tutte insieme
- **Colonne che si spostano dal menu dell'intestazione**: tenendola
  premuta, **in cima al menu**, sopra "Edit property", ci sono **"Move
  to left"** e **"Move to right"**. La prima colonna dopo Name ha "Move to left" grigia —
  Name resta sempre la prima — e l'ultima ha grigia "Move to right"
  *(documentata il 24/09/2026: era nel codice ma non qui; provata sul
  telefono il 24/09/2026, vedi Cronologia)*
- **Titolo del database nascondibile** quando sta dentro una pagina,
  dalle impostazioni della vista. A schermo intero resta sempre visibile
- **Ricerca full-text** tra titoli e contenuto dei blocchi
- **Font e dimensione del testo selezionato** *(sessione cloud del
  24/09/2026 per tutta la pagina; **dal 25/09/2026 solo sul testo
  selezionato**, provato sul telefono lo stesso giorno)*: nella barra Aa,
  prima di B, la voce del font e quella della dimensione, come su
  OneNote; mostrano quelli del testo dove sta il cursore. Si seleziona un
  pezzo di testo e si sceglie: cambia **solo quel pezzo**, come il colore
  o il grassetto, e un altro pezzo può avere un altro font. Senza niente
  di selezionato dicono "Select some text first". Sedici font in tre
  gruppi — occidentali, cinesi, giapponesi — che sono **sosia liberi**
  dei font commerciali chiesti; dimensione da 5 a 72, di partenza 16,
  **scritta a mano** in una finestrella. Nelle impostazioni **non** ci
  sono più "Font" e "Font size" per tutto il Workspace (tolte il
  25/09/2026, su richiesta)
- **Niente titoli (Heading)** dal 24/09/2026: tolti dall'app su
  richiesta dell'utente, ora che la dimensione del testo si sceglie
  dalla barra Aa. Quelli che c'erano sono diventati testo normale
- **Conteggio del testo** *(sessione cloud del 24/09/2026, provato sul
  telefono il 24/09/2026)*: in fondo al menu dei tre puntini di una pagina c'è
  **"X words"**; toccandola si apre lì sotto l'elenco come nella
  finestra "Word Count" di Word — parole, lettere, numeri, lettere e
  numeri insieme, caratteri (tutti i segni, simboli compresi) senza e
  con spazi, righe (blocchi con del testo), caratteri
  giapponesi, cinesi e lettere coreane. Conta solo il testo scritto
  nella pagina: niente database, niente titolo
- **Move to** *(sessione cloud del 24/09/2026, provato in parte sul
  telefono il 24/09/2026)* dai tre puntini di una pagina o di un database, e dal menu
  del blocco: l'albero della barra laterale con la ricerca in cima, da
  cui si sceglie qualsiasi pagina, comprese quelle dei database; la
  pagina finisce **in fondo**. Un database arriva sempre come
  collegamento a pagina. Sulla pagina principale la voce è grigia
- **Duplicate** dal menu dei tre puntini, con la scelta di dove mettere
  la copia: accanto all'originale (per una riga di database, una riga
  nuova), in fondo al menu principale o dentro una pagina qualsiasi,
  comprese quelle delle righe di tutti i database. Si copia tutto,
  sottopagine e immagini comprese
- ~~**Tema scuro fisso in tutta l'app**, coi grigi di Notion: `#191919`
  per le pagine, `#202020` per le finestre dal basso, `#252525` per i
  riquadri delle opzioni. Niente tema chiaro e niente colori dinamici~~
  **Superato** (corretto il 24/09/2026): in Settings → Theme c'è
  **Dark / Light / System** (vedi la voce "Theme" in Cronologia). I
  grigi di Notion qui sopra restano quelli del tema scuro, che è ancora
  quello di partenza; niente colori dinamici
- **Il correttore** (dal 08/10/2026), come in OneNote: le parole che il
  dizionario non conosce hanno sotto una riga rossa ondulata; toccandone
  una, fino a cinque parole al suo posto, "Add to dictionary" e
  "Ignore". Si accende da Settings → Proofing per tutte le pagine
  (Workspace e Notebooks) o dai tre puntini di una pagina sola, che può
  avere anche la sua lingua: inglese americano e britannico, italiano,
  tedesco, francese, spagnolo. Spento di partenza. I dizionari sono
  quelli di Hunspell (LibreOffice), letti da un motore scritto apposta
  (`data/proofing/`), senza internet

## Limiti noti

- **Quaderni** (dal 26/09/2026):
  - eliminare una pagina, una sezione o un quaderno è per sempre: il
    cestino è del menu principale, e un cestino dei quaderni non c'è
  - nei quaderni non si cerca (la ricerca è quella del menu principale,
    che le loro pagine non le vede)
  - dal menu di OneNote mancano Export, Move or Copy, Merge e Copy Link;
    le pagine non si ordinano per data o nome e non hanno sottopagine
  - la pagina è quella dell'editor, che si scrive dall'alto in basso: non
    si scrive in un punto qualsiasi del foglio come in OneNote
- **Pagine bloccate con la password** (dal 26/09/2026):
  - l'impronta c'è solo da Android 11 in su; prima, solo la password
  - una notifica ("/notification" o una data con "Include notification")
    mostra il suo testo anche se sta in una pagina bloccata
  - dimenticata la password, la si rifà col blocco schermo del telefono:
    se il telefono non ne ha uno, le pagine bloccate restano chiuse
- **Un pezzo di testo più grande allunga tutte le righe del suo
  gruppo**, non solo la sua (dal 25/09/2026, con la dimensione sul testo
  selezionato). Le righe di testo scorrevole consecutive stanno in un
  campo solo con **un'altezza di riga unica** — è quella che tiene i
  segni degli elenchi tutti allo stesso passo — e per darla riga per
  riga servirebbe uno `ParagraphStyle` per riga dentro la
  trasformazione, la strada che ha già fatto smettere di disegnare il
  campo e perdere del testo (vedi "Strade già tentate"). Così l'altezza
  del gruppo si allunga quanto serve al pezzo più grande (`lineScaleFor`),
  e torna com'era appena quel pezzo torna a 16. Un gruppo finisce dove
  c'è un blocco che non è testo scorrevole (tabella, toggle, collegamento
  a pagina, divisore…)
- **Copiare con "Seleziona tutto" da un blocco di testo unito porta con
  sé un a-capo in più** in testa (il `RUN_LEAD`, vedi sopra). Toglierlo
  richiederebbe di intercettare copia/taglia
- **Un numero decimale a inizio blocco diventa un elenco**: scrivendo
  `1.5` la conversione scatta già sul punto, prima che il `5` arrivi.
  Conseguenza diretta di aver scelto il punto come innesco invece dello
  spazio (`1. `, come fanno Notion e Word). Scelta confermata
  dall'utente: i sottolivelli si fanno col rientro, non scrivendo
  `1.1` a mano
- **Una riga rientrata che va a capo da sola** riparte dal margine
  invece di restare allineata sotto al testo. Conseguenza del rientro
  fatto con spazi nel prefisso invece che con `TextIndent`, che invece
  sapeva rientrare anche le righe di continuazione
- **Fondere due elementi di elenco richiede due backspace**: il
  primo toglie il marcatore, il secondo fonde. È il comportamento di
  Notion ed è la conseguenza diretta della scelta sopra, non una svista
- **Selezione e backspace-tra-blocchi non funzionano dentro un
  toggle**: i suoi contenuti restano sul vecchio modello a campo
  singolo per ciascuno
- ~~La selezione non attraversa il confine di una casella da
  spuntare.~~ **Risolto**: le caselle sono entrate nel campo di testo
  condiviso, quindi selezionare fra una casella e l'altra è selezionare
  del testo come in qualsiasi altro punto. Resta il limite dentro un
  *toggle*, dove le caselle figlie sono ancora righe a sé
- ~~**Colore del testo** non disponibile da nessuna parte.~~
  **Superato**: c'è il pennello accanto ad Aa (vedi "Colore del testo
  e colore dietro al testo" in Cronologia). Resta vero per i campi
  della voce qui sotto
- **Titolo pagina, celle delle tabelle semplici e celle dei database**
  sono testo completamente piatto, senza formattazione né colore:
  portarceli richiede di costruire per ciascuno l'infrastruttura già
  fatta per i blocchi
- ~~Nei menu "+" e "/" (dal 24/09/2026 hanno le stesse voci), **Callout,
  Quote, Link to page (come voce separata da "Page"), Image, Video,
  Audio, Code, File, Web bookmark e Database - Full page** ci sono ma
  sono grigie: non sono state
  costruite — alcune richiedono capacità nuove (gestione file,
  embedding media).~~ **Superato** (06–07/10/2026, dalla v0.91 alla
  v0.96): ci sono tutte. ~~Anche Linked view of data source.~~
  **Superato** (24/09/2026, richiesta 36): la vista collegata c'è
- **Database: "Property visibility" non riordina e non cerca.** Su
  Notion le due liste hanno le maniglie per trascinare le colonne e un
  campo di ricerca in cima; qui l'ordine si cambia solo tenendo
  premuta l'intestazione della colonna (Move to left / Move to right),
  e la ricerca non c'è, perché con
  una manciata di proprietà sarebbe un campo di testo da riempire per
  scorrere una lista che si vede già tutta
- **Database semplice: si sceglie quando lo si crea**, e non si può
  trasformare un database normale in semplice o viceversa. Un database
  normale ha righe che magari sono già pagine piene di testo, e farle
  diventare "solo testo" vorrebbe dire decidere cosa farne
- **Database semplice: dalla ricerca si arriva al database, non alla
  riga.** Il risultato dice il nome della riga e in quale database sta,
  e aprendolo si apre il database intero: la riga va cercata lì. Nella
  barra laterale il database semplice non ha figli, perché le sue righe
  non sono pagine
- **Database: una colonna nascosta non si sposta** *(dal 24/09/2026)*.
  Spostare si fa tenendo premuta l'intestazione, e una colonna nascosta
  un'intestazione non ce l'ha; il suo "Move left / Move right" dentro la
  finestra della proprietà, che era l'altro modo, è stato tolto su
  richiesta dell'utente. Per spostarla la si rimostra (Property
  visibility), la si sposta e la si nasconde di nuovo. Lo stesso vale
  per le viste senza intestazioni (bacheca, elenco, galleria...): lì
  l'ordine delle proprietà sulle schede si cambia dalla tabella
- **Database: la visibilità è una sola per tutto il database**, non una
  per vista come su Notion: nascondendo una colonna nella tabella
  sparisce anche dalle schede della bacheca e dell'elenco. Qui un
  database ha una vista sola per volta, quindi tenerne una copia per
  ognuna vorrebbe dire salvare impostazioni che nessuno ha mai scelto
- **Database: ordinamento solo su una proprietà per volta**. Notion permette di concatenare più ordinamenti; qui il secondo criterio a parità di valore è fisso
- **Database: l'ordinamento non tocca calendario e linea del tempo**, dove le pagine stanno già in ordine di data — che è l'ordine che serve lì
- ~~**Database: la ricerca non esiste.** L'icona di Notion che sta lì
  accanto alla vista è stata deliberatamente omessa finché non
  funziona, perché un'icona che non fa niente è peggio di un'icona
  assente.~~ **Superato** (24/09/2026, richiesta 35): la lente accanto a
  Sort c'è. I filtri ci sono, su una proprietà per volta
- **Database: manca la vista Chart.** Ci sono Table, Board, List,
  Calendar, Timeline e (dal 24/09/2026) Gallery; Chart si aggiungerà
  quando disegnerà davvero qualcosa, come le altre
- **Gallery: le schede si costruiscono tutte insieme**, immagini
  comprese, come nelle altre viste: non è una griglia "pigra", perché
  il database dentro una pagina sta in una lista che scorre già e una
  griglia che scorre per conto suo lì dentro non si può mettere. Con
  qualche decina di pagine non si nota; con centinaia di copertine
  l'apertura rallenta e la memoria cresce. Ogni immagine comunque si
  legge già rimpicciolita alla misura della scheda
- **Gallery: delle impostazioni di Notion ci sono solo Card preview e
  Card size.** Mancano "Fit image" (mostrare l'immagine intera invece
  di riempire il riquadro — qui vale sempre l'inquadratura scelta nella
  pagina), "Show page icon", "Wrap all properties", "Open pages in", il
  raggruppamento e l'anteprima da una proprietà "Files & media", che
  l'app non ha. Le schede non si trascinano per riordinarle
- **Gallery: le proprietà sulle schede sono testo semplice**, una per
  riga e tagliata se lunga, come sulle schede della bacheca: i tag non
  sono colorati. "Created time" e "Last edited time" non compaiono,
  come nella bacheca
- **Gallery: l'anteprima "Page content" mostra solo il testo**
  (paragrafi, titoli, elenchi, caselle, toggle chiusi con la freccia):
  niente tabelle, divisori, collegamenti a pagine o database, né il
  contenuto dentro i toggle. Il testo è senza formattazione (niente
  grassetto, corsivo o colori); **gli spoiler restano coperti**, ogni
  lettera diventa un quadratino `▒`. Una pagina senza copertina, con
  "Page cover", mostra il riquadro vuoto
- **Timeline: manca l'ingrandimento "5 anni"** che Notion ha. Escluso
  apposta: a quella scala un mese è largo due millimetri e non si
  distingue più niente
- **Date: niente formato della data e fuso orario** nella finestra della
  data, che Notion ha: qui seguono le impostazioni dell'app. Il
  promemoria c'è dal 25/09/2026 ("Include notification"), ma a un'ora
  fissa — quella della data, o le 9:00 — e non "un giorno prima" o "un'ora
  prima" come su Notion
- **Date: l'ora vale per l'inizio e per la fine insieme**, non si può
  accenderla solo su uno dei due
- **Timeline: l'asse copre al massimo quattrocento caselle** attorno
  alle date che ci sono, e se le date sono più larghe la finestra si
  stringe attorno a oggi. Le caselle non sono pigre, vengono composte
  tutte insieme, quindi senza tetto una data sbagliata nell'anno
  tremila ne costruirebbe un milione
- **Timeline: le barre non si trascinano** per spostare o allungare
  una pagina, come nel calendario. La data si cambia dalla cella
- **Timeline: scorrere partendo dalla colonna dei nomi non fa
  niente**, perché quella colonna è ferma apposta. Il gesto va
  iniziato sull'asse
- **Calendario: le barre non si trascinano** per spostare o allungare
  una pagina. La data si cambia dalla cella, come nelle altre viste
- **Calendario: oltre venti pagine datate nel database le altre
  diventano un "+N"** che non si può aprire, e si guardano dalla
  tabella. Ogni pagina si tiene la sua riga per sempre, anche nelle
  settimane in cui non compare, quindi le righe sono tante quante le
  pagine datate: senza un tetto un database pieno renderebbe altissima
  ogni settimana. È il prezzo delle righe fisse per sempre, scelto
  sapendolo
- **Calendario: ogni settimana è alta quanto tutta la griglia delle
  righe**, anche se è vuota. Discende dalla stessa scelta: se una
  settimana si stringesse sulle pagine che contiene, la stessa pagina
  non starebbe più alla stessa altezza in tutte
- **Calendario: nella vista ad anno non ci sono barre né titoli**, solo
  i giorni occupati cerchiati. A dodici mesi per schermata non ci sarebbe
  spazio per scriverci sopra niente di leggibile. Un giorno lì non si
  tocca per creare una pagina: si tocca il mese e lo si apre da vicino
- **Calendario: solo la settimana si trascina col dito.** Il mese e
  l'anno si cambiano con le frecce
- **Database: le schede della bacheca non si trascinano** da una
  colonna all'altra. Per spostare una riga si cambia il suo tag
- **Database: tipi di proprietà non realizzabili in locale.** Person,
  Created by, Last edited by (non esistono account né utenti), Files &
  media, Button, Place, e tutte le Connections (Google Drive, Figma,
  GitHub, Zendesk) e l'AI Autofill, che richiedono rete e servizi
  esterni — contro il principio dell'app. **Relation e Rollup** ci sono
  dal 21/10/2026 (v0.125); **Formula sarebbe invece possibile** ed è il
  candidato sensato da aggiungere: un progetto a sé
- **Database: Relation e Rollup non si filtrano e non raggruppano la
  bacheca** (v0.125): il filtro sceglie fra tag, date, numeri e caselle,
  e il raggruppamento fra le proprietà che hanno un valore solo per riga
- **Database: aprendo una riga come pagina non si vedono le sue
  proprietà.** In Notion stanno in cima alla pagina e si modificano da
  lì; qui i valori vivono solo nella tabella
- ~~**Copertina: non si può spostare né ritagliare.**~~ **Superato**:
  "Reposition" la sposta e la ingrandisce (vedi "Copertina —
  inquadratura" in Cronologia)
- **Immagini: non c'è una raccolta da cui scegliere**, come le copertine pronte di Notion. Si mette la propria
- **Un'immagine mandata con Telegram come "foto" perde la trasparenza.**
  Telegram la comprime in JPEG, che la trasparenza non ce l'ha: l'icona
  arriva con lo sfondo bianco già dentro, e l'app non può toglierlo senza
  rovinare le icone che hanno parti bianche. Va mandata come **file**
  (su Telegram per PC: togliere "Comprimi immagini"), o scaricata
  direttamente sul telefono. Verificato il 25/09/2026 su un'icona di
  Flaticon: il file ricevuto era `IMG_…jpg`, e mandato come file è
  rimasto trasparente
- **Nelle finestrelle che salgono dal basso la barrettina Copia / Incolla
  è dell'app**, non del telefono (vedi Cronologia del 25/09/2026, "quattro
  bug"): ha Cut, Copy, Paste e Select all con le scritte di Android, ma
  non le voci in più che Samsung aggiunge altrove
- **Font: nessuno è quello vero.** Helvetica, Arial, Calibri, Cambria,
  Consolas, Georgia, Verdana, Times New Roman, Futura, Garamond e SimSun
  (dal 25/09/2026 al posto di MS YaHei, che col suo sosia era identico
  al font di sistema) sono commerciali: il telefono non li ha e l'app non li può
  distribuire. Li disegnano dei sosia liberi (tabella in Cronologia,
  "Pagine — font e dimensione del testo"); cinque hanno le stesse
  identiche misure dell'originale, gli altri somigliano. **Verdana** non
  ha un sosia fedele (Noto Sans è il più vicino), **Helvetica** usa
  Inter per non essere identico ad Arial, **Kaiti** è un kaishu a
  pennello (Ma Shan Zheng). Il **Lishu** non c'è: un Lishu libero non
  esiste, ed è stato tolto su scelta dell'utente
- **Font cinesi e giapponesi: la prima volta serve internet** e Google
  Play Services. Pesano 10-20 MB l'uno e si scaricano da Google quando
  una pagina li usa per la prima volta; poi restano sul telefono. Finché
  non arrivano — o se la prima volta si è offline — il testo si vede col
  font di sistema e cambia da solo quando il font c'è. Nel menu di
  scelta i loro nomi non sono scritti col loro font, per non scaricarli
  tutti solo aprendo il menu
- **Font e dimensione valgono per tutta la pagina**, non per un pezzo
  di testo selezionato: è quello che ha chiesto l'utente ("selezionando
  automaticamente tutto quanto"). Il **titolo della pagina** prende il
  font ma non la dimensione (a 72 non ci starebbe nello schermo); i
  **database dentro la pagina** restano come sono; i **quadratini delle
  caselle** e le frecce dei toggle restano della loro misura
- **La dimensione è in sp**, l'unità di Android, non in punti
  tipografici come Word e OneNote: il 16 di partenza dell'app non
  corrisponde all'11 di OneNote
- **Conteggio del testo: le parole si contano come in Word**, cioè
  qualunque cosa fra due spazi: anche "=" o "[]" da soli sono una
  parola. Scelto per dare **gli stessi numeri di Word** sugli stessi
  appunti, che è il confronto che l'utente ha fatto vedere
- **Conteggio del testo: una riga tutta in kanji, senza nemmeno un
  hiragana o un katakana, finisce fra i caratteri cinesi.** Da un
  ideogramma solo non si capisce la lingua, e si decide riga per riga:
  basta un kana nella riga perché i suoi ideogrammi contino come
  giapponesi (vedi `textStatsOf`)
- **Conteggio del testo: il titolo della pagina non conta**, e nemmeno
  i database dentro la pagina né i nomi delle pagine collegate. Il
  testo dentro i toggle invece conta anche quando sono chiusi, e gli
  spoiler contano come testo normale. Il conteggio si fa quando si apre
  il menu: non si aggiorna mentre il menu resta aperto (che è anche
  l'unico momento in cui non si può scrivere)
- **Menu del blocco: "Turn into" di una pagina o di un database non è
  costruito** (la voce c'è, grigia), e nemmeno "Color" di un database
  dentro la pagina. Il colore di un collegamento vale solo per quel
  collegamento, non per la pagina. ~~**"Edit icon" cambia l'immagine
  dell'icona**, come ovunque nell'app: l'emoji di una pagina non si
  sceglie ancora da nessuna parte~~ **Superato** (08/10/2026, v0.97):
  "Edit icon", come ogni icona, apre il selettore delle emoji di Notion
- ~~**Annulla dopo un "Duplicate" di una pagina** (o di un toggle con
  dentro una sottopagina) toglie il collegamento alla copia, ma la copia
  resta: la si ritrova dalla ricerca. Annulla segue i blocchi della
  pagina, non le pagine~~ **Superato per le pagine** (richiesta 34):
  Annulla manda la copia nel Cestino. Resta vero per il **Duplicate di
  un toggle** dal menu del blocco che ha dentro una sottopagina: Annulla
  toglie la copia del toggle, e la copia della sottopagina resta, da
  ritrovare con la ricerca
- **Move to fuori da un database perde le proprietà della riga**: la
  pagina esce dal database, come su Notion, e i valori delle colonne
  restano con la riga, che si toglie. Lo dice la riga di spiegazione
  quando si sposta la pagina di una riga. Nome e contenuto restano
- ~~**Annulla non riporta indietro un "Move to" né un "Move to
  trash"**~~ **Superato** (richiesta 34): li riporta indietro, e anche
  "Duplicate". Quando si fanno dai tre puntini, l'Annulla va alla
  **prossima pagina che si apre** (si torna indietro, o si entra nella
  copia): se da lì si va da un'altra parte senza annullare, lo si trova
  nella prima pagina che si apre dopo — è "Annulla l'ultima cosa fatta".
  Dal database a schermo intero non si annulla niente lì (non ha la
  barra): lo si fa dalla pagina in cui si torna
- **Annulla non vive oltre la pagina aperta**: chiusa l'app, o lasciata
  la pagina abbastanza a lungo perché Android la dimentichi, la sua
  cronologia se ne va, come prima
- **"Turn into simple database" non si annulla**: le pagine delle righe
  se ne vanno per sempre, come chiesto (richiesta 31). Per questo, se
  hanno del contenuto, prima c'è una conferma col loro numero
- **Ricerca dentro un database**: le **date e le caselle** non si
  cercano, e nemmeno le date automatiche di creazione e modifica (il
  valore salvato non è quello che si legge). Nelle proprietà e nelle
  pagine si illumina la riga intera, non la parola: dentro la cella o
  la pagina la parola non è segnata. Il testo delle pagine si legge
  quando si apre la ricerca o si torna sulla schermata: se la pagina di
  una riga cambia mentre la ricerca è aperta su un'altra schermata (la
  stessa pagina aperta due volte), lo si vede al ritorno. Le sottopagine
  delle pagine delle righe contano solo per il nome, non per quello che
  hanno dentro
- **Viste collegate**: **l'ordine delle proprietà**, il loro nome, tipo
  e opzioni e "Center" sono del database, quindi valgono anche per tutte
  le sue viste (Notion l'ordine lo tiene per vista); per vista sono
  layout, filtro, ordinamento, raggruppamento, galleria, calendario,
  linea del tempo e quali proprietà si vedono. Il **filtro è uno solo**
  per vista (una proprietà, uno o più valori), come nei database. Una
  vista non ha un nome suo: mostra sempre quello del database. Non si
  può fare una vista di una vista (si collega il database vero)
- **Disegno: dentro un gruppo di righe il disegno non segue la singola
  riga.** È attaccato al gruppo intero (le righe fra due isole, come i
  divisori): scrivendo righe nuove *dentro* il gruppo, sopra al disegno,
  il testo scende e il disegno no. Per questo l'utente mette i divisori
  come confini. Niente selezione a lazo né zoom, e sui database a schermo
  intero non si disegna
- **Notifiche: la riga "/notification" legge la data col giorno
  prima** (`22.10` è il 22 ottobre), qualunque sia il formato della data
  scelto nelle impostazioni. E `9.05` è il 9 maggio, non le 9:05: per
  l'ora, meglio i due punti (`9:05`)
- **Widget: i nomi delle città sono quelli inglesi del sistema** ("Rome",
  "Beijing", "Kolkata"), gli stessi in tutte le lingue: tradurli
  vorrebbe un elenco di centinaia di città in otto lingue. Anche la sigla
  del fuso è quella inglese ("CEST", "PDT"), l'unica che esiste per
  tutti
- **Widget: niente "Quarter" e "Life" della foto** fra le barre fisse:
  non erano nella richiesta; si fanno con "Aggiungi una data" (per
  "Life", da una data di nascita a una di arrivo)
- **Widget: niente Annulla.** Un widget tolto (dopo la conferma) o un
  contatore azzerato non tornano indietro
- **Widget: si spostano coi tre puntini**, Sposta su e Sposta giù, non
  trascinandoli
- **Correttore** (dal 08/10/2026): niente cinese, coreano e giapponese
  (si vedono grigi, "Not available yet"); non controlla le celle dei
  database e delle tabelle, il codice, gli indirizzi, le sigle tutte
  maiuscole e le parole con dei numeri. È solo ortografia: la grammatica
  ("le casa") non la guarda
- **Export/backup** (Markdown+CSV, PDF) progettato ma non implementato.
  Decisione già presa con l'utente: **Markdown+CSV come formato
  principale** (il PDF non conserva la struttura, quindi è inadatto al
  recupero dei contenuti), **PDF come seconda funzione separata**, non
  due rami della stessa
- **App Windows + sincronizzazione**: discusso, non iniziato.
  **Decisione presa (2026-09-18): "forse un giorno"** — non si tocca lo
  schema esistente adesso, ma ogni entità nuova va progettata già
  pronta per la sincronizzazione (identificatore stabile, momento
  dell'ultima modifica). Costo quasi nullo ora, evita una migrazione
  dolorosa se l'obiettivo diventa reale

## Diario delle sessioni cloud

Il diario della sessione cloud del 24/09/2026 riportava i messaggi
dell'utente: non è pubblico.

## Cronologia degli aggiornamenti

**Le patch notes dell'app** (dal 30/09/2026, chiesto dall'utente). Ogni
richiesta dell'utente è una **versione**, dalla prima, v0.1, a oggi, e il
numero dice che cosa porta (dal 21/10/2026, chiesto dall'utente: tutta la
Cronologia è stata riletta e rinumerata così):
- 🆕 **un'aggiunta**, qualcosa che l'app prima non faceva: uno in più al
  secondo numero (da v0.1 a v0.2, v0.3…);
- ✏️ **una modifica** di qualcosa che c'era già, come si vede o come si
  comporta, o toglierla: uno in più al terzo (da v0.8 a v0.8.1, v0.8.2…);
- 🔧 **solo correzioni di bug**: uno in più al quarto (da v0.8 a v0.8.0.1,
  da v0.8.1 a v0.8.1.1…).

Una versione con più cose prende il numero della più grande: con
un'aggiunta è 🆕 anche se corregge dei bug. La più recente sta in cima;
sotto il titolo, come sempre, la richiesta parola per parola e le voci,
ognuna con la sua etichetta (`[Nuova funzionalità]`, `[Modifica]`,
`[Rimosso]`, `[Bug fix]`; `[Progetto]` per come è fatta) e con **la
piattaforma** a cui si riferisce: **[📱 Mobile]** l'app per il telefono,
**[💻 PC]**, **[🍎 Mac]**, **[🌐 Web App]**, più d'una insieme o
**[🌍 Tutte]**. Per ora c'è l'app per il telefono; quella per il PC è in
costruzione dal 04/11/2026 (v0.153) e non si scarica ancora.

Accanto al numero, il giorno e l'ora (italiana) in cui la versione è
stata chiusa: quella del commit, o, per il lavoro che commit non ne ha
(fino al 23/09/2026), quella dell'ultima risposta di Claude prima della
richiesta dopo, letta dalle conversazioni salvate sul PC. Delle prime
versioni, fatte in un'altra chat fra l'8 e il 18/09/2026, non restano né
l'ora né l'ordine, e lo dicono; di due si sa il momento dai file del
progetto.

Lo stesso testo, identico, è in **Settings → Patch Notes** dell'app, che
lo copia da qui a ogni compilazione (vedi "Nota tecnica"); il numero
della versione più recente è anche quello dell'app.

Le voci nate nelle sessioni cloud (quelle del 24/09/2026, da "Database —
vista Gallery" a "Pedice e apice nella barra Aa") portano scritto che
**vanno ancora verificate sul telefono**: quando lo sono, si aggiunge "(verificato sul telefono il
gg/mm/aaaa)" accanto al titolo.

### ✏️ v0.153.42 — 11/11/2026 · 09:20 — Sul PC il pallino della puntina si vede solo col mouse sopra
*(Claude Code sul PC; non ancora provata dall'utente; installata sul telefono; 1162 test passati)*

- **[Progetto]** **[💻 PC]** **Il pallino grigio dietro il pulsante per
  fissare o nascondere la barra laterale compare solo col mouse sopra**;
  senza, si vede solo l'icona. Non ancora provato dall'utente
- **[Progetto]** `SidebarPinButton` guarda il mouse (`hoverable`). Test: in
  `DesktopEditorTest`, dietro la puntina c'è il colore della barra, col
  mouse sopra il pallino, e dopo di nuovo la barra (col pallino sempre
  acceso fallisce)

### ✏️ v0.153.41 — 11/11/2026 · 09:06 — Un divider fra "Favorite pages" e le pagine
*(Claude Code sul PC; non ancora provata dall'utente; installata sul telefono; 1161 test passati)*

- **[Progetto]** **[📱 Mobile]** **[💻 PC]** **Nella barra laterale un
  divider separa "Favorite pages" da "Pages"**, l'albero delle pagine,
  come quello sotto "Main menu". Non ancora provato dall'utente

### ✏️ v0.153.40 — 11/11/2026 · 08:55 — Preferiti e pagine subito sotto "Main menu"
*(Claude Code sul PC; non ancora provata dall'utente; installata sul telefono; 1161 test passati)*

- **[Progetto]** **[📱 Mobile]** **[💻 PC]** **La barra laterale ora è:
  "Main menu", "Favorite pages", l'albero delle pagine, poi "Notebooks", i
  widget e le altre voci** (Search, Templates, Backup, Trash, Connections,
  Settings). Prima preferiti e pagine stavano sotto i widget
- **[Progetto]** **[💻 PC]** **Niente fondo grigio dietro la pagina
  principale nell'albero anche sul PC**, come sul telefono dalla
  0.153.39. Non ancora provato dall'utente
- **[Progetto]** `SidebarContent` nel nuovo ordine, senza più
  `rootBackground`. La foto della barra sul PC (`app_pinned.png`) lo mostra

### ✏️ v0.153.39 — 11/11/2026 · 08:41 — "Main menu" in cima alla barra laterale, la puntina del PC in un pallino
*(Claude Code sul PC; non ancora provata dall'utente; installata sul telefono; 1161 test passati)*

- **[Progetto]** **[📱 Mobile]** **[💻 PC]** **"Main menu" è la prima voce
  della barra laterale**, sopra "Notebooks"; prima stava sotto i widget
- **[Progetto]** **[💻 PC]** **Il pulsante per fissare o nascondere la
  barra sta a destra di "Main menu", in un pallino**, così non si confonde
  con le icone delle voci; prima era da solo, in cima a destra
- **[Progetto]** **[📱 Mobile]** **Nell'albero delle pagine, niente più
  fondo grigio dietro la pagina principale** (la prima in cima, "My
  Personal Workspace") quando è quella aperta; le altre pagine aperte lo
  tengono, e sul PC resta anche lì. Non ancora provato dall'utente
- **[Progetto]** `SidebarContent` con `rootBackground`; il colore del
  pallino è `SidebarPinCircle` (anche col tema chiaro). Le foto della barra
  sul PC (`app_pinned.png`) lo mostrano

### ✏️ v0.153.38 — 11/11/2026 · 08:25 — Sul PC Ctrl+Z e Ctrl+Y sono l'Annulla e il Ripristina della pagina
*(Claude Code sul PC; non ancora provata dall'utente; non installata sul telefono (non collegato); 1161 test passati)*

- **[Bug fix]** **[💻 PC]** **Ctrl+Z annulla come il pulsante Annulla della
  pagina**, e Ctrl+Y (o Ctrl+Maiusc+Z) ripristina. Prima, dentro una riga,
  Ctrl+Z usava l'annulla del campo di testo, che conosce solo il testo del
  campo: rimetteva un testo vecchio del gruppo di righe, che la pagina poi
  riscriveva sui blocchi (righe unite o spezzate), e non sapeva niente di
  divider, tabelle o blocchi eliminati. Ora vale nelle righe, nei blocchi
  di codice e negli altri campi della pagina, e anche dopo aver eliminato
  dei blocchi scelti col mouse. Nel titolo, nelle celle delle tabelle e in
  quelle dei database resta l'annulla del campo. Non ancora provato
  dall'utente
- **[Progetto]** `undoKeyOf` in `BlockSelection.kt`, preso dai campi della
  pagina prima del campo di testo. Test: in `DesktopBlockSelectionTest`,
  tolto un divider e messo il cursore in una riga, Ctrl+Z lo rimette e
  Ctrl+Y lo toglie di nuovo (senza la correzione fallisce)

### ✏️ v0.153.37 — 10/11/2026 · 22:33 — Tolto un divider le righe restano strette, e la selezione col mouse fa scorrere la pagina
*(Claude Code sul PC; non ancora provata dall'utente; installata sul telefono; 1160 test passati)*

- **[Bug fix]** **[📱 Mobile]** **[💻 PC]** **Cancellato un divider, le
  righe sotto non prendono più lo spazio di un titolo.** Le righe prima e
  dopo un divider sono due gruppi; tolto lui diventano uno, e il primo
  pezzo del gruppo sotto non aveva un confine suo: finiva nello stesso
  campo del titolo grande sopra il divider, con la sua altezza di riga, e
  così restava finché si scriveva. Ora un gruppo che comincia diverso dalla
  riga sopra il divider ha già il suo confine. Vale anche per il secondo
  Backspace sotto un divider sul telefono. Non ancora provato dall'utente
- **[Bug fix]** **[💻 PC]** **Trascinando la selezione verso il bordo in
  alto o in basso, la pagina scorre da sola**, più veloce quanto più si è
  vicini al bordo o oltre; mentre scorre, la selezione segue la pagina a
  blocchi interi (come su Notion). La rotella, tenendo premuto, non viene
  più presa dalla selezione. Non ancora provato dall'utente
- **[Progetto]** `rechunk` dà un pezzo al primo di un gruppo che appare
  diverso dalla riga sopra l'isola (`lookAboveIsland`);
  `mouseBlockSelection` scorre con `scrollBy` vicino ai bordi, e il campo
  lascia il testo selezionato quando si passa ai blocchi. Test:
  `DividerRemovedChunksTest` (tolto il divisore sotto un titolo grande, le
  righe sotto restano in un campo loro: senza la correzione fallisce),
  `DesktopBlockSelectionTest` (+2: la rotella mentre si seleziona, e il
  trascinamento fino al bordo che fa scorrere la pagina e porta la selezione
  fin lì)

### ✏️ v0.153.36 — 10/11/2026 · 22:10 — Sul PC più blocchi col mouse, Ctrl+A, i divider e le celle delle tabelle
*(Claude Code sul PC; non ancora provata dall'utente; installata sul telefono; 1157 test passati)*

- **[Progetto]** **Più blocchi col mouse, come su Notion**: trascinando
  dentro un campo si seleziona il testo come sempre; uscendo verso altri
  blocchi (oltre un titolo, un'immagine, una tabella) si scelgono i blocchi
  interi, velati di blu, con in basso Copy, Cut e Delete. Canc o Backspace
  li eliminano (si può annullare), Ctrl+C e Ctrl+X li copiano e tagliano,
  Esc o un clic altrove li lasciano. Sottopagine e database restano, come
  quando si svuota la pagina intera
- **[Progetto]** **Ctrl+A seleziona tutta la pagina**, non solo il pezzo
  di testo col cursore: lo stesso velo di "Select all" del telefono, e i
  tasti di sopra (Canc chiede prima di svuotare la pagina). Nel titolo e
  nelle celle dei database Ctrl+A resta del campo
- **[Progetto]** **Un clic su un divider lo sceglie**, e Canc o Backspace
  lo eliminano
- **[Progetto]** **Più celle di una tabella semplice**: trascinando da una
  cella all'altra si sceglie il rettangolo di celle; Canc le svuota, e
  scelte tutte se ne va la tabella intera. Ctrl+C le copia (una riga per
  riga, tabulazioni fra le colonne), Ctrl+A le sceglie tutte. Dentro una
  cella il testo si seleziona come prima. Non ancora provato dall'utente
- **[Progetto]** Solo col mouse: col dito sul telefono non cambia niente.
  `BlockSelection.kt` (chi sta dove, il gesto, i tasti), il velo riga per
  riga nei campi di più righe, `pageCopyLines` e `deletePageContent` con i
  blocchi scelti. Test: `DesktopBlockSelectionTest` (una pagina vera
  disegnata e guidata col mouse: trascinando oltre il titolo grande e Canc
  se ne vanno tre blocchi, clic sul divider e Backspace, Ctrl+A vela la
  pagina ed Esc la lascia, due celle svuotate, tutte le celle e la tabella
  se ne va)

### ✏️ v0.153.35 — 10/11/2026 · 22:08 — Selezionando un titolo grande le righe sotto non si allargano più
*(Claude Code sul PC; non ancora provata dall'utente; installata sul telefono; 1152 test passati)*

- **[Bug fix]** **[📱 Mobile]** **[💻 PC]** **Le righe sotto un titolo
  grande non prendono più il suo spazio.** Selezionando la fine di un testo
  più grande (il titolo di una pagina presa dal Web Clipper, un titolo col
  corpo scelto con la barra Aa), tutte le righe normali sotto si
  allontanavano come se fossero grandi anche loro, finché la selezione non
  spariva. Succedeva perché la selezione arrivata in fondo a un campo si
  porta dietro quello dopo (dal 19/10/2026), e un campo ha un'altezza di
  riga sola, quella del testo più grande. Ora si porta dietro solo un campo
  con righe che appaiono uguali, come per lo scorrimento (v0.131.0.2). Per
  selezionare oltre un titolo, sul PC ci sono i blocchi interi col mouse
  (v0.153.36). Non ancora provato dall'utente
- **[Progetto]** `PageEditorViewModel.joinChunkAfter` guarda `lineLookOf`.
  Test: `LineLookChunksTest` (+1: la selezione in fondo al titolo grande non
  unisce le righe sotto, che restano alte come le altre)

### ✏️ v0.153.34 — 10/11/2026 · 21:22 — Sul PC il tasto "Sincronizza col telefono"
*(Claude Code sul PC; installata sul telefono dopo la pubblicazione; provata dall'utente col telefono e col PC il 10/11/2026; 1151 test passati)*

- **[Progetto]** **"Sincronizza col telefono" anche sul PC**, in
  Connections, sotto "Aspetta il telefono": il PC lo chiede al telefono
  abbinato (all'indirizzo dell'ultima volta e a tutta la rete di casa), e
  il telefono, con PageFlow aperto, parte da solo come col suo tasto —
  decide sempre lui cosa va dove. Sul PC una rotellina finché non ha finito
  e poi la notifica; sul telefono, su qualunque schermata, com'è andata in
  basso. Se il telefono non risponde il PC lo dice. Prima si poteva
  sincronizzare solo dal telefono. Provato dall'utente il 10/11/2026
- **[Progetto]** Il telefono ascolta sulla porta 47391 finché PageFlow è
  aperto, solo se è abbinato a un PC, e una richiesta da un PC sconosciuto
  non fa niente (`WifiSync.listenForPc`, `SyncServer.askPhones`). Test:
  `DesktopSyncTest` (il PC chiede, il telefono parte e porta a casa la
  pagina nuova del PC)

### ✏️ v0.153.33 — 10/11/2026 · 19:47 — Sincronizzazione: una pagina cambiata da tutte e due le parti si unisce riga per riga
*(Claude Code sul PC; installata sul telefono; provata dall'utente col telefono e col PC il 10/11/2026; 1150 test passati)*

- **[Progetto]** **La stessa pagina cambiata sul telefono e sul PC non
  sceglie più la pagina intera**: si guarda riga per riga (e il titolo, i
  disegni, le colonne di un database). Una riga cambiata da una parte sola
  vince quella, una cancellata da una parte e uguale dall'altra se ne va,
  una nuova resta; due righe nuove nello stesso posto vanno una dopo
  l'altra. Solo una riga cambiata da tutte e due è un conflitto: vince
  l'ultima modifica, e la versione di prima resta nella cronologia della
  pagina ("Before syncing"). Così anche due pagine nuove fatte di qua e di
  là nello stesso menu restano tutte e due. La prima volta, senza un "prima"
  da confrontare, niente si cancella. Provato dall'utente il 10/11/2026
  («Funziona, ottimo»)
- **[Progetto]** `SyncMerge`; il telefono si ricorda le impronte di ogni
  riga dopo la sincronizzazione (`SyncPeer.elements`, in `sync.json`).
  Test in `DesktopSyncTest`: la lista della spesa cambiata dalle due parti
  (una riga cambiata e una cancellata di qua, un'altra cambiata e una nuova
  di là), e i due menu con una pagina nuova ciascuno

### ✏️ v0.153.32 — 10/11/2026 · 17:53 — La sincronizzazione Wi‑Fi fra telefono e PC
*(Claude Code sul PC; provata dall'utente col telefono e col PC il 10/11/2026; 1149 test passati)*

- **[Progetto]** **Telefono e PC si sincronizzano sulla rete di casa**, senza
  cloud: nella barra laterale "Connections" (la voce che c'era già, grigia).
  Sul PC si accende "Aspetta il telefono"; sul telefono "Sincronizza col PC"
  trova il PC da solo. La prima volta i due schermi mostrano lo stesso
  codice di sei cifre da confermare (l'abbinamento), e tutti e due fanno un
  backup. Vanno nei due sensi pagine, database (riga per riga), quaderni,
  file, cronologia, "Updates", il testo delle foto e le parole del
  correttore; impostazioni e widget restano di ogni dispositivo. Solo quello
  che è cambiato: cambiato da una parte va dall'altra; cancellato da una
  parte va nel cestino dall'altra; cambiato da tutte e due vince l'ultima
  modifica, e l'altra resta nella cronologia della pagina ("Before
  syncing"). Le pagine nuove fatte dalle due parti nella stessa pagina non
  perdono il loro posto. Provato dall'utente il 10/11/2026: «quattro»
  scritto nella pagina House M.D. sul PC è arrivato sul telefono, e
  «quattordici» scritto poi sul telefono è arrivato sul PC
- **[Progetto]** Tutto cifrato: l'abbinamento con ECDH (P-256) e il codice
  da confrontare, poi AES-GCM con chiavi nuove a ogni sincronizzazione.
  Le righe si copiano tali e quali con l'SQL (`SyncDatabase`), per nome di
  colonna; il piano (`SyncPlan`) confronta le impronte di qua, di là e
  dell'ultima volta. Sul PC, Windows chiede la prima volta se PageFlow può
  usare la rete. Test: `DesktopSyncTest` (due PageFlow con la loro
  cartella, su una vera connessione: abbinamento, pagine e file nuovi,
  modifiche, conflitto, cestino, la ricerca del PC in rete, le pagine nuove
  nello stesso menu), `SyncPhoneTest` (le stesse righe col database del
  telefono), la foto di "Connections" con la domanda del codice

### ✏️ v0.153.31 — 10/11/2026 · 17:30 — Sul PC Ctrl+N fa una pagina nuova
*(Claude Code sul PC; non ancora provata dall'utente; non installata sul telefono (non collegato); 1144 test passati)*

- **[Progetto]** **Ctrl+N sul PC**: una pagina nuova, col collegamento in
  fondo alla pagina aperta (dal menu principale, o da una schermata che non
  è una pagina, in fondo al menu principale), e ci si entra col cursore
  sulla prima riga. Come la nota al volo del telefono: lasciata vuota se ne
  va, e senza titolo prende la sua prima riga. Funziona anche mentre si
  scrive in un'altra pagina. Non ancora provato dall'utente
- **[Progetto]** La scorciatoia la riceve la finestra (`AppShortcuts`,
  come Ctrl+F) e la pagina nasce con `createQuickNote`. Test: in
  `DesktopEditorTest`, Ctrl+N dal menu principale lascia il collegamento in
  fondo al menu ed entra nella pagina nuova

### ✏️ v0.153.30 — 10/11/2026 · 17:25 — Sul PC le pagine bloccate si aprono con Windows Hello
*(Claude Code sul PC; non ancora provata dall'utente; non installata sul telefono (non collegato); 1143 test passati)*

- **[Progetto]** **Windows Hello per le pagine bloccate**, come l'impronta
  sul telefono: aprendo una pagina bloccata la finestra di Windows Hello
  (volto, impronta o PIN di Windows) si apre davanti a PageFlow, e se si
  annulla resta la password. Con "Password dimenticata?" Windows Hello
  conferma che sei tu e poi si sceglie una password nuova, come col blocco
  schermo del telefono. Sul PC le scritte dicono "Windows Hello" invece di
  "impronta"; l'interruttore "Sblocca anche con Windows Hello" è nella
  password delle pagine bloccate, nelle Impostazioni. Prima, sul PC, c'era
  solo la password. Non ancora provato dall'utente
- **[Progetto]** Lo chiede uno script PowerShell
  (`IUserConsentVerifierInterop`, il modo delle app per il desktop, con la
  finestra di PageFlow), lo stesso aiuto del testo delle foto
  (`WinRtScript`). Provato il 10/11 col PC bloccato: la finestra di
  Windows parte; annullarla da PageFlow non la chiude, quindi PageFlow
  aspetta la risposta. Test: `DesktopHelloTest` (lo script si compila e
  Windows risponde se Windows Hello è pronto)

### ✏️ v0.153.29 — 10/11/2026 · 17:20 — Sul PC si incolla coi link (e con la formattazione), come sul telefono
*(Claude Code sul PC; non ancora provata dall'utente; non installata sul telefono (non collegato); 1142 test passati)*

- **[Progetto]** **Incollare coi link anche sul PC**: il testo copiato da
  un browser, da Word o da PageFlow arriva coi suoi link sulle parole
  giuste; con "Keep Source Formatting" anche grassetto, corsivo, grandezze,
  elenchi e tabelle, e con "Keep Text Only" niente, come sul telefono.
  Copiando da PageFlow, negli appunti di Windows va anche l'HTML con lo
  stile e il tipo delle righe, così incollando in un'altra pagina (o in
  Word) la formattazione resta. Prima, sul PC, si incollava solo il testo.
  Non ancora provato dall'utente
- **[Progetto]** Sul PC gli appunti si leggono dall'HTML di Windows con le
  stesse funzioni del telefono (`parseClipHtml`, `matchAnchors`,
  `htmlStyleRuns`). Test: `DesktopPasteTest` (un link da una pagina web
  arriva sulla parola giusta; copiando del grassetto negli appunti va anche
  l'HTML)

### ✏️ v0.153.28 — 10/11/2026 · 10:26 — Sul PC le Notifiche nelle Impostazioni: la prova, spegnerle, spegnerle per un po'
*(Claude Code sul PC; non ancora provata dall'utente; non installata sul telefono (non collegato); 1140 test passati)*

- **[Progetto]** **La sezione "Notifications" delle Impostazioni anche sul
  PC**: "Send a test notification", "Turn off notifications" e "Turn off
  for a while" (un'ora, otto ore, un giorno, una settimana, e "Turn back
  on"), come sul telefono, con una riga che dice come arrivano sul PC (con
  PageFlow aperta, col suono di Windows; spente, tacciono anche timer e
  pomodoro). Suono, vibrazione, priorità e pallino restano del telefono:
  sul PC le notifiche sono quelle di Windows. Prima, sul PC, la sezione non
  c'era e le notifiche non si potevano spegnere. Non ancora provato
  dall'utente
- **[Progetto]** Via `hasNotificationSettings`: la sezione c'è su tutti e due.
  La pagina delle Impostazioni disegnata sul PC (`settings.png`) la mostra

### ✏️ v0.153.27 — 10/11/2026 · 10:18 — Sul PC si legge il testo delle foto, per ritrovarle cercando
*(Claude Code sul PC; non ancora provata dall'utente; non installata sul telefono (non collegato); 1140 test passati)*

- **[Progetto]** **Il testo delle foto anche sul PC**: le foto dei blocchi
  aggiunte sul PC (immagini, scansioni, catture del Web Clipper) si leggono
  in sottofondo, una alla volta, col lettore di testo di Windows (quello
  dello Strumento di cattura, chiamato da PowerShell), e la ricerca trova
  quello che c'è scritto, come sul telefono. Legge le lingue di Windows e
  giapponese, coreano e cinese (di queste si tengono solo le righe coi loro
  caratteri, senza gli spazi che Windows mette fra gli ideogrammi); le foto
  molto alte si leggono a pezzi, rimpicciolite come sul telefono. Quelle
  arrivate dal telefono avevano già il loro testo. Non ancora provato
  dall'utente
- **[Progetto]** Scegliere le foto da leggere e salvarne il testo
  (`ImageTextScan`) nel codice comune: il telefono lo usa come prima col
  lettore di Google. Test: `DesktopImageTextsTest` (una scritta disegnata
  lì letta dal vero lettore di Windows e salvata, le righe ripulite, una
  foto alta 20000 pixel letta in tre pezzi)

### ✏️ v0.153.26 — 10/11/2026 · 10:11 — I suoni anche sul PC: il contatore, la sveglia di timer e pomodoro
*(Claude Code sul PC; non ancora provata dall'utente; non installata sul telefono (non collegato); 1137 test passati)*

- **[Progetto]** **I suoni sul PC**, con Java Sound: i clic del widget
  contatore (gli stessi suoni del telefono, col suo volume) e la **sveglia**
  quando un timer finisce o il pomodoro cambia fase: il suono scelto per
  quella fase o, se non c'è, una suoneria di PageFlow (tre note di
  campanello, due volte). Con le notifiche spente la sveglia tace, come sul
  telefono. Nelle impostazioni del pomodoro, sul PC, "suono" apre "Apri"
  di Windows per un file .wav (anche .aiff e .au; gli mp3 Java non li
  suona); un suono scelto sul telefono, sul PC, diventa la suoneria di
  PageFlow. I promemoria non hanno un suono in più: suonano già col suono
  delle notifiche di Windows. Prima, sul PC, era tutto muto. Non ancora
  provato dall'utente
- **[Progetto]** Le onde dei suoni del contatore (`CounterSoundWaves`) nel
  codice comune: il telefono le suona come prima, il PC con
  `DesktopSounds`. Test: `DesktopSoundsTest` (la sveglia dura due secondi e
  mezzo, si sente e fra i due giri tace; i WAV del contatore si aprono con
  Java Sound); `CounterSoundsTest` del telefono sulle onde comuni

### ✏️ v0.153.25 — 10/11/2026 · 10:03 — Il Web Clipper anche sul PC, con Edge nascosto
*(Claude Code sul PC; provata sul PC dall'utente (installatore 0.153.2400), la nitidezza finale ancora no; non installata sul telefono (non collegato); 1135 test passati)*

- **[Progetto]** **Il Web Clipper sul PC**, dai tre puntini di una pagina: la
  stessa finestra del telefono (link, Full Page, Region, Bookmark, Article,
  Text and Images, Dark mode, titolo, nota). Al posto del browser dentro
  l'app, PageFlow usa **Microsoft Edge nascosto** (o Chrome), comandato col
  protocollo dei suoi strumenti per sviluppatori: apre il sito come un
  portatile (largo 1440), toglie i banner e fa arrivare le immagini con gli
  stessi script del telefono, poi fotografa la pagina a strisce o ne legge
  l'articolo. "Region": prima la foto della pagina intera, poi col mouse si
  trascina il rettangolo da tenere. Edge ha un profilo suo, nella cartella
  dei dati: non vede la cronologia né gli accessi dell'Edge di tutti i
  giorni. Senza Edge né Chrome lo dice. Provato dall'utente sul PC
- **[Progetto]** **Foto più nitide sul PC**: la pagina si disegna davvero a 2
  pixel per pixel CSS (prima il browser la scattava a 1 e la ingrandiva) e
  il JPEG ha qualità 94; nelle pagine del PC le immagini lunghe si leggono
  grandi quanto il loro riquadro sullo schermo (prima sempre 1200 pixel) e
  tutte, rimpicciolite, col filtro di qualità (prima i bordi a scalette).
  La nitidezza finale non è ancora stata provata dall'utente
- **[Progetto]** La finestra del Web Clipper, il suo collegamento con la
  pagina, il velo della selezione, lo scaricamento delle immagini di "Text
  and Images" e le costanti (larghezza, user agent, script) nel codice
  comune; al telefono restano le sue finestre di cattura col WebView. Nell'installatore
  c'è `java.net.http` (serve per parlare con Edge); `-PinstallerVersion` dà
  un numero agli installatori di prova (con lo stesso numero di quello
  installato, quello di Windows non partiva)
- **[Progetto]** `DesktopWebClipTest`, con Edge vero su una pagina scritta lì:
  la foto della pagina intera (larga 2880, alta tutta, blu in fondo), il
  ritaglio di "Region", l'articolo letto da "Text and Images"

### ✏️ v0.153.24 — 10/11/2026 · 09:21 — Sul PC si registra l'audio dal microfono, nei blocchi audio
*(Claude Code sul PC; provata sul PC dall'utente (installatore 0.153.2301) e installata sul telefono; 1134 test passati)*

- **[Progetto]** **Registrare sul PC**, da un blocco audio vuoto come sul
  telefono: "Record" accende il microfono di Windows, la barra della
  registrazione mostra il tempo e quanto è forte la voce, "Stop" mette il
  file nel blocco ("Registrazione…"), "Scarta" lo butta. Il file è un .wav
  (circa 2,6 MB al minuto, a 22 kHz): Java non sa scrivere l'.m4a del
  telefono; lo suonano sia il telefono sia Windows. Chiudendo PageFlow
  mentre registra, la registrazione si ferma e va nel suo blocco prima di
  uscire. Se Windows nega il microfono alle app del desktop, la
  registrazione resta muta. Prima, sul PC, "Record" diceva che il microfono
  non si poteva usare. Provato dall'utente sul PC
- **[Progetto]** Mettere la registrazione finita nel suo blocco
  (`placeRecording`) è nel codice comune: lo usano il telefono, come prima,
  e il PC. Test: `DesktopRecordingTest` (quanto è forte il suono, il .wav
  scritto senza sapere quanto sarà lungo che alla fine si apre intero, il
  file che va nel blocco o, scartato, se ne va); via il vecchio test che
  voleva il registratore del PC spento

### 🔧 v0.153.23.1 — 10/11/2026 · 09:05 — Sul PC la barra appena fissata risponde al primo clic
*(Claude Code sul PC; provata sul PC dall'utente (installatore 0.153.2300) e installata sul telefono; 1132 test passati)*

- **[Bug fix]** **[💻 PC]** **La barra laterale appena fissata rispondeva solo
  dal secondo clic**: per scorrerla o richiuderla con le frecce bisognava
  prima cliccare una volta a vuoto. Fissandola, il cassetto da cui la barra
  si apre restava vuoto e largo zero, e allora "chiuso" e "aperto" erano lo
  stesso punto: il cassetto si credeva ancora aperto, e il suo velo
  invisibile sopra la finestra si prendeva il primo clic e la rotella. Ora
  il cassetto si chiude prima di fissare la barra e, vuoto, resta largo come
  lei. Il `DesktopEditorTest` fa la stessa cosa (apre la barra, la fissa,
  un clic solo sulle frecce la richiude): prima della correzione falliva.
  Provato dall'utente sul PC

### ✏️ v0.153.23 — 10/11/2026 · 07:44 — Sul PC la finestra si riapre com'era, l'app usa tutta la finestra e la barra laterale si fissa e si allarga, come in Notion
*(Claude Code sul PC; provata coi test del PC (installatore 0.153.2200) e sul telefono (la barra laterale di sempre); 1131 test passati)*

- **[Progetto]** **La finestra del PC si riapre com'era**: grandezza,
  posizione e se era ingrandita (file "window" nella cartella dei dati).
  La prima volta 1200 × 800, al centro; prima si apriva sempre stretta
  come un telefono. Se lo schermo dov'era non c'è più, si apre al centro
- **[Progetto]** **L'app usa tutta la finestra**: niente più colonna di 900
  punti al centro; la barra laterale si apre dal bordo sinistro della
  finestra e la pagina è larga quanto lo spazio che resta (scelto
  dall'utente: "Tutta la larghezza")
- **[Progetto]** **La barra laterale fissata, come in Notion**: la puntina in
  cima alla barra la fissa accanto alla pagina, dove resta finché non la si
  richiude con le due frecce («); trascinandone il bordo destro si allarga
  o si stringe (da 220 a 480). Fissata e larghezza si ricordano anche alla
  riapertura. Fissata, le pagine non mostrano il pulsante per aprirla, e
  l'albero delle pagine si aggiorna da solo quando cambiano pagine e righe.
  Sul telefono non cambia niente
- **[Progetto]** Nel `DesktopEditorTest` l'app intera disegnata in una
  finestra di 1200 × 800 con la barra fissata (foto in `app_pinned.png`)

### ✏️ v0.153.22 — 09/11/2026 · 23:07 — Il PDF anche sul PC, col testo vero, e "Print"
*(Claude Code sul PC; provata sul PC dall'utente (installatore 0.153.2100) e sul telefono (il PDF si prepara e si apre la stampa; l'anteprima no, la stampante era spenta); 1130 test passati)*

- **[Progetto]** **"Export" in PDF sul PC**, come sul telefono: il testo è
  testo vero (si seleziona e si cerca), con grassetto, corsivo, colori,
  evidenziato, sottolineato, barrato, apici e pedici, le liste, le caselle,
  citazioni, riquadri, codice, tabelle, database, bottoni, immagini, i
  disegni a mano ("Include drawings") e "1 / 5" in fondo ai fogli. "Print"
  lo manda alla stampa di Windows (se non si può, lo apre nel programma dei
  PDF). I caratteri sono quelli di Windows dentro il file: Segoe UI, Consolas
  per il codice e, per le lettere che mancano, Segoe UI Symbol, Segoe UI
  Emoji (gli emoji vengono in bianco e nero), Malgun Gothic per il coreano,
  Microsoft YaHei per cinese e giapponese. Le righe vanno a capo un po'
  diversamente dal telefono (Segoe UI non è Roboto). Provato dall'utente sul PC
- **[Progetto]** **L'impaginazione del PDF nel codice comune** (`PdfExport`):
  è la stessa su telefono e PC; il testo lo misurano e i fogli li disegnano
  gli attrezzi di ogni sistema (`PdfBackend`): sul telefono quelli di Android
  di sempre (`StaticLayout`, `PdfDocument`), sul PC PDFBox 3.0.5, una
  libreria nuova (l'installatore cresce di circa 4 MB)
- **[Progetto]** `DesktopPdfTest`: una pagina con un po' di tutto diventa
  sul PC un PDF di più fogli in cui si ritrova il testo, anche il coreano
  (foto del primo foglio in `pdf_page1.png`); i test del PDF del telefono
  disegnano i fogli come prima

### ✏️ v0.153.21 — 09/11/2026 · 22:45 — "Export" anche sul PC, in Markdown
*(Claude Code sul PC; provata sul PC dall'utente (installatore 0.153.2000) e sul telefono; 1129 test passati)*

- **[Progetto]** **"Export" sul PC**, dai tre puntini di una pagina o di un
  database: lo stesso Markdown del telefono (una pagina da sola è un .md;
  con immagini, sottopagine o database uno .zip con la cartella accanto).
  "Save" apre la finestra "Salva con nome" di Windows col nome già scritto.
  Sul PC "Share" non c'è (non c'è la scelta delle app a cui mandarlo) e il
  PDF non ancora: il telefono lo disegna con gli strumenti di Android, e
  sul PC va rifatto. Prima, sul PC, la finestra si chiudeva subito.
  Provato dall'utente sul PC
- **[Progetto]** `PageExporter` (preparare i file) e la finestra "Export"
  nel codice comune; per sistema `pdfWriter` (sul PC null per ora),
  "Save", "Share" e "Print" (`ExportActions` sul telefono, coi gesti di
  sempre). `exportToFile` è una funzione comune, non più una per sistema
- **[Progetto]** `DesktopExportTest`: sul PC una pagina con un'immagine e
  una sottopagina diventa lo stesso .zip del telefono, e il PDF non si fa;
  la finestra "Export" disegnata sul PC (`export.png`)

### ✏️ v0.153.20 — 09/11/2026 · 22:25 — Il Backup completo sul PC, e un backup del PC si apre anche sul telefono
*(Claude Code sul PC; provata sul PC dall'utente (backup e caricamento) e sul telefono (un backup del PC ripristinato); il ripristino sul PC non ancora; 1127 test passati)*

- **[Progetto]** **La pagina Backup del telefono anche sul PC**, la stessa
  (passata nel codice comune): "Fai un backup adesso", "Carica su Proton
  Drive" (sul PC una finestra "Salva come", da puntare la prima volta sulla
  cartella di Proton Drive o su una chiavetta: la volta dopo si apre lì),
  il backup di ogni giorno, il promemoria per caricarne uno (una notifica
  di Windows che apre la pagina Backup), l'elenco dei backup col menu
  carica / ripristina / elimina, e "Ripristina da un file…". Sul PC i
  backup stanno in Documenti/PageFlow backup e se ne tengono gli ultimi 7;
  quello di ogni giorno si fa un minuto dopo l'apertura, e poi ogni ora si
  guarda se è già stato fatto, finché PageFlow è aperta
- **[Progetto]** **Il ripristino sul PC come sul telefono**: prima un backup
  di com'era (nell'elenco, "Prima di un ripristino"), poi PageFlow si chiude
  e si riapre da sola col backup al suo posto. Prima si chiudeva e
  andava riaperta a mano. Anche dopo "Delete all content" ora si riapre da sola
- **[Bug fix]** **[📱 Mobile]** **Un backup fatto sul PC si apre sul telefono**: il
  PC tiene le impostazioni tutte come testo, e il telefono, trovando
  "true" scritto come testo dove si aspettava un sì/no, si chiudeva a ogni
  avvio. Ora le legge lo stesso. Provato dall'utente: sul telefono c'è
  tutto, anche un evento creato sul PC
- **[Progetto]** La navigazione apre direttamente Impostazioni e Backup, le
  stesse sui due sistemi (via `PlatformRoutes`); i backup del PC stanno in
  `Backups` (al posto di `DesktopBackups`). Test: `DesktopBackupTest` (un
  backup del PC torna com'era: pagine, immagini, impostazioni), la pagina
  Backup disegnata sul PC (`backup.png`), `BackupTest` (un backup con le
  impostazioni come testo si legge sul telefono)

### ✏️ v0.153.19 — 09/11/2026 · 21:58 — Le notifiche di Windows: promemoria, timer, pomodoro e pulsanti anche sul PC
*(Claude Code sul PC; provata sul PC dall'utente e sul telefono; 1124 test passati)*

- **[Progetto]** **Le notifiche sul PC**, come notifiche di Windows: i
  promemoria delle pagine ("/notification") e delle date dei database con
  "Include notification", la fine di un timer, le fasi del pomodoro e
  "Send notification" dei pulsanti, con gli stessi testi del telefono.
  Cliccandone una si apre la pagina, la riga o la barra laterale dei
  widget, e la finestra torna davanti anche se era ridotta a icona. Alla
  prima notifica l'icona di PageFlow compare accanto all'orologio; chiusa
  la finestra, sparisce. Arrivano solo con PageFlow aperta: una persa a PC
  spento arriva alla riapertura, se non è passata da più di 12 ore, e
  nessuna arriva due volte. Provate dall'utente sul PC
- **[Progetto]** Sul PC la sezione "Notifications" delle Impostazioni non
  c'è ancora: suono, priorità e "spegnerle per un po'" sono del telefono
- **[Progetto]** `DesktopRemindersTest`: un promemoria arriva una volta
  sola e quando è l'ora, alla prima apertura il passato non arriva tutto
  insieme, uno perso da più di 12 ore non arriva più

### ✏️ v0.153.18 — 09/11/2026 · 21:36 — Le Impostazioni anche sul PC: tema, lingua, formati, correttore, password
*(Claude Code sul PC; provata sul PC dall'utente e sul telefono; 1122 test passati)*

- **[Progetto]** **Le Impostazioni nel codice comune**: sul PC sono le stesse
  del telefono — tema, finestra di avvio, titolo, riga intera, paragrafo,
  animazioni, formati di numeri, date e ore, lingua, correttore, fuso
  orario, password delle pagine bloccate, cache, Patch Notes — tranne la
  sezione "Notifications", che sul PC non c'è finché non ci sono le
  notifiche di Windows. Prima, sul PC, erano una pagina vuota. Provate
  dall'utente sul PC
- **[Progetto]** Quello che era solo del telefono passa da funzioni di ogni
  sistema: la versione dell'app (sul PC quella in cima alle Patch Notes),
  il riavvio dopo "Delete all content" (sul PC PageFlow si chiude) e lo
  svuotamento delle tabelle (sul PC una per una, in una transazione).
  `AppCleanup` (svuotare la cache, cancellare tutto) è nel codice comune;
  il peso della cache si scrive come quello dei file
- **[Progetto]** Nel `DesktopEditorTest`: le Impostazioni si disegnano sul
  PC (foto in `settings.png`), e "Delete all content" lascia solo una My
  Space vuota

### ✏️ v0.153.17 — 09/11/2026 · 18:28 — Sul PC il clic destro apre i menu, come tenere premuto sul telefono
*(Claude Code sul PC; provata sul PC dall'utente e sul telefono; 1120 test passati)*

- **[Progetto]** **Il clic destro come la pressione lunga** (`onRightClick`):
  dove tenendo premuto col dito si apre un menu, sul PC lo apre il clic
  destro — le righe e le schede dei database (elenco, tabella, bacheca,
  galleria, calendario, sequenza temporale), i quaderni, i blocchi
  immagine, video, audio, file, segnalibro e bottone, le menzioni di
  pagina, i numeri degli elenchi, i widget della barra laterale. Il clic
  destro si ferma lì, così il clic normale (aprire la riga) non parte
  insieme. Dentro il testo resta il menu di sempre (taglia, copia,
  incolla). Col dito non cambia niente; provato dall'utente sul PC
- **[Progetto]** `DesktopRightClickTest` (il clic destro apre il menu e non
  la riga, il sinistro resta il clic di sempre); `SubGroupTest` aspetta
  tutte e due le scelte di un raggruppamento prima di controllarle (fra le
  due scritture c'è un istante con la prima sola, e a volte falliva)

### ✏️ v0.153.16 — 09/11/2026 · 18:14 — Sul PC: la pagina al centro, Esc per tornare indietro, Ctrl+F per cercare
*(Claude Code sul PC; provata sul PC dall'utente e sul telefono; 1118 test passati)*

- **[Progetto]** **La finestra del PC a schermo largo**: l'app sta al centro,
  larga al massimo come una pagina (900 punti), e ai lati resta lo sfondo;
  prima, a finestra ingrandita, la pagina restava incollata a sinistra
- **[Progetto]** **Esc è il gesto "indietro" del PC**: chiude la barra
  laterale, toglie il cursore dalla riga in cui si scrive, e poi torna alla
  schermata di prima, come il gesto del telefono (`BackHandler` del PC, che
  passa da quello di Compose)
- **[Progetto]** **Ctrl+F apre la ricerca** da qualsiasi schermata, anche
  senza un cursore in un campo (`AppShortcuts`: la finestra riceve il tasto,
  la navigazione apre "Search")
- **[Progetto]** Provati dall'utente sul PC con l'installatore aggiornato
  sopra quello di prima: l'aggiornamento tiene le note

### ✏️ v0.153.15 — 09/11/2026 · 18:01 — PageFlow si installa su Windows: menu Start, icona, collegamento sul desktop
*(Claude Code sul PC; installata su Windows dall'utente; sul telefono installata, il suo codice non cambia; 1118 test passati)*

- **[Progetto]** **L'installatore per Windows** (`gradle :desktop:packageExe`,
  `PageFlow-<versione>.exe`): si installa per l'utente, senza permessi da
  amministratore, con la voce nel menu Start, il collegamento sul desktop e
  l'icona di PageFlow (`branding/pageflow.ico`). Dentro c'è anche Java,
  quindi non serve installare altro. I dati restano in `%APPDATA%\PageFlow`:
  installandolo si ritrovano le note già aperte sul PC. Installato e provato
  dall'utente
- **[Progetto]** Il numero di versione per Windows viene dall'ultima voce del
  README, coi tre numeri che Windows vuole: 0.153.14 diventa 0.153.1400,
  0.153.11.1 diventa 0.153.1101, così cresce sempre e un nuovo installatore
  aggiorna quello vecchio. Fra i moduli di Java portati nel programma ci
  sono anche le date nelle otto lingue, le connessioni sicure (meteo,
  sport, azioni) e le codifiche delle pagine web

### ✏️ v0.153.14 — 09/11/2026 · 17:50 — Sul PC si apre un backup del telefono: le tue note su Windows
*(Claude Code sul PC; provata sul PC con un backup vero e sul telefono; 1118 test passati)*

- **[Progetto]** **Le tue note sul PC**: in "Backup" della finestra del PC
  c'è "Restore from a file…", che apre un backup fatto dal telefono (lo
  stesso file zip, letto con lo stesso codice). Come sul telefono va in due
  tempi: prima si toglie dallo zip e si controlla, poi PageFlow si chiude e,
  riaprendola, il database, le immagini e le impostazioni (tema, widget,
  disegno, la password delle pagine bloccate) prendono il loro posto.
  **Niente si perde**: quello che il PC aveva va nella cartella
  `before-restore-<ora>` dei suoi dati. Provato con un backup vero: sul PC
  c'è tutto quello che c'è sul telefono. Per ora è una copia: quello che si
  scrive dopo su uno non arriva sull'altro (per quello ci sarà la
  sincronizzazione)
- **[Progetto]** Il PC apre solo backup della sua stessa versione del
  database (`DATABASE_VERSION`, 70): le migrazioni dei database vecchi
  stanno nel codice del telefono, e un database più vecchio non si
  aprirebbe più. Un backup più vecchio lo dice ("aggiorna l'app sul
  telefono, fai un nuovo backup"), uno più nuovo pure. Le impostazioni che
  vanno nel backup (`PREF_NAMES`) sono adesso le stesse per telefono e PC
- **[Progetto]** `DesktopBackupTest`: un backup fatto col codice del
  telefono prende il posto dei dati del PC, e quelli di prima restano da
  parte; un backup più vecchio e un file che non è un backup non toccano
  niente

### ✏️ v0.153.13 — 09/11/2026 · 17:21 — La navigazione nel codice comune: sul PC si apre l'app, dalla home alle pagine
*(Claude Code sul PC; provata sul telefono; 1115 test passati)*

- **[Progetto]** **La navigazione fra le schermate nel codice comune**
  (`AppNavHost`): al posto della navigazione di Android (navigation-compose
  2.7.7) c'è quella multipiattaforma di JetBrains (2.9.1), con le stesse
  rotte e gli stessi comandi; sul telefono è la navigation-compose 2.9 di
  Google. Gli argomenti delle rotte si leggono alla maniera comune
  (`read { getStringOrNull(…) }`). "Settings" e "Backup", ancora solo del
  telefono, passano da `SettingsRoute` e `BackupRoute`: sul PC per ora sono
  una pagina vuota con la freccia per tornare
- **[Progetto]** **La finestra del PC apre l'app**: la home, la barra
  laterale, le pagine e i database, con lo stesso codice del telefono, al
  posto delle sole Patch Notes. All'avvio fa quello che fa il telefono per
  le parti comuni (impostazioni, widget, disegno, password, formati). Il
  database del PC è ancora suo e vuoto: i dati del telefono arriveranno
  col backup e poi con la sincronizzazione
- **[Progetto]** `DesktopEditorTest`: sul PC tutta l'app arriva alla home
  (disegnata sul filo della finestra, come la finestra vera); la foto resta
  in `build/desktop-screenshots/app_home.png`

### ✏️ v0.153.12 — 09/11/2026 · 17:05 — L'editor delle pagine e i database nel codice comune: la prima pagina disegnata sul PC
*(Claude Code sul PC; provata sul telefono; 1114 test passati)*

- **[Progetto]** **L'editor delle pagine e i database sono nel codice
  comune** (`PageEditorScreen`, `DatabaseViewScreen`, i bottoni e il
  calendario del giorno): sono gli stessi file per telefono e PC. Quello
  che resta solo del telefono passa da funzioni di ogni sistema: il Web
  Clipper (`WebClipperFlow`, tutto il giro dalla scelta del sito al
  risultato nella pagina), lo scanner dei documenti, la finestra
  "Export" e il registro di prova dell'editor. Sul PC, per ora, Web
  Clipper ed esportazione si chiudono subito e lo scanner non fa niente
- **[Progetto]** Adesso sono 276 file comuni (90.800 righe) e 60 solo del
  telefono (10.900): notifiche, sveglie, backup, impostazioni, Web
  Clipper, lettori, esportazione e la navigazione fra le schermate
- **[Progetto]** **Più memoria per compilare**: l'editor nel codice comune
  non si compilava per il telefono con 2 GB ("Java heap space"); adesso
  Gradle e il compilatore di Kotlin ne hanno 4 (`gradle.properties`)
- **[Progetto]** `DesktopEditorTest`: sul PC l'editor disegna una pagina
  vera — icona, titolo, righe — e carica le sue righe dal database. È la
  prima pagina di PageFlow disegnata su Windows

### 🔧 v0.153.11.1 — 09/11/2026 · 16:41 — Il nome delle immagini scaricate sul PC, e "Salva una copia" controllato dai test
*(Claude Code sul PC; installata sul telefono, il codice del telefono non cambia; 1113 test passati)*

- **[Bug fix]** **[💻 PC]** **Il nome di un'immagine scaricata sul PC**
  (trovato rivedendo la v0.153.11): veniva dal nome del blocco, che per le
  immagini del Web Clipper è il testo descrittivo della pagina web. Uno che
  finiva col punto ("Un grafico delle emissioni.") dava un file senza
  estensione, uno con `:` o `?` non si salvava, e uno con `..\` poteva
  finire fuori da Immagini/PageFlow. Adesso si tiene solo l'ultimo pezzo
  del nome, senza i caratteri che Windows non vuole né i punti finali, e
  con l'estensione vera dell'immagine
- **[Progetto]** `SaveCopyTest`: "Salva una copia" di un blocco-file passa
  il nome proposto al "Salva in…" di sistema e copia il file dove l'utente
  ha scelto (il "Salva in…" del test è finto e risponde subito). Nel
  `DesktopFilesTest` del PC, i nomi delle immagini scaricate

### ✏️ v0.153.11 — 04/11/2026 · 15:07 — Immagini, video, audio e file dei blocchi nel codice comune
*(Claude Code sul PC; provata sul telefono; 1111 test passati)*

- **[Progetto]** **I blocchi Image, Video, Audio, File e Web bookmark nel
  codice comune.** Scegliere un'immagine o un file, copiarlo nell'app e
  salvarne una copia passa da azioni di ogni sistema
  (`rememberImageImporter`, `rememberFileImporter`, `rememberSaveCopy`): sul
  telefono i selettori di sempre, sul PC la finestra di Windows o del Mac.
  `BlockFiles` è diviso allo stesso modo: sul PC un file si apre col
  programma del computer e un'immagine scaricata va in Immagini/PageFlow
- **[Progetto]** **I lettori** dei video e degli audio (`MediaSource`: un
  file dell'app o un link), YouTube e Vimeo dentro la pagina, le immagini
  molto alte e il registratore dei blocchi audio sono azioni di ogni
  sistema: sul telefono quelli di sempre, sul PC per ora una riga che apre
  il video o l'audio col programma del computer, l'immagine intera
  rimpicciolita, e niente registrazione. La barra di registrazione è nel
  codice comune
- **[Progetto]** Nel codice comune anche la finestra di icona e copertina,
  le celle immagine dei database e il menu delle azioni sui blocchi: adesso
  sono 268 file (70.000 righe) e 63 solo del telefono (31.600). Dall'editor
  mancano ancora il Web Clipper, lo scanner, l'esportazione e le notifiche
- **[Progetto]** `DesktopFilesTest`: sul PC il tipo di un'immagine si legge
  dai suoi primi byte, i link di YouTube e Vimeo si riconoscono come sul
  telefono, e il registratore dice di no

### ✏️ v0.153.10 — 04/11/2026 · 14:30 — Appunti, caratteri, correttore e Web Clipper verso il codice comune
*(Claude Code sul PC; provata sul telefono; 1107 test passati)*

- **[Progetto]** **Gli appunti dell'editor nel codice comune**: i modi di
  incollare, i segni dei link e degli stili incollati, il tipo delle righe
  copiate e l'HTML della formattazione (`PasteLinks.kt`). `LinkClipboard` è
  una classe per sistema: sul telefono quella di sempre, che legge gli
  appunti di Android col loro HTML; sul PC, per ora, gli appunti normali.
  Anche i pulsanti copia, incolla e taglia della barra sopra la tastiera
  passano da un'azione comune (`rememberEditShortcuts`), e "Copy" della
  pagina selezionata da `copyRichText` (sul PC il testo e l'HTML negli
  appunti di Windows o del Mac)
- **[Progetto]** **I caratteri delle pagine**: nomi, sosia e gruppi nel
  codice comune (`PageFontNames.kt`); la famiglia da disegnare è per
  sistema: sul telefono i font inclusi e quelli di Google come prima, sul PC
  per ora il carattere di sistema più simile
- **[Progetto]** Il correttore legge i dizionari con `openAppAsset` (sul PC
  dalle risorse del programma), e il contenuto delle pagine salvate col Web
  Clipper ("Text and Images") si legge con la libreria JSON di Kotlin invece
  di quella di Android. Sei file in più nel codice comune: adesso sono 254
  (66.000 righe) e 67 quelli solo del telefono (35.000)
- **[Progetto]** `DesktopTextTest`: sul PC il dizionario italiano si legge
  dalle risorse e riconosce le parole, il JSON del Web Clipper si legge, e
  ogni carattere ha nome e famiglia

### ✏️ v0.153.9 — 04/11/2026 · 14:11 — La barra laterale coi widget, i quaderni, le icone e il blocco con password nel codice comune
*(Claude Code sul PC; provata sul telefono; 1104 test passati)*

- **[Progetto]** **I widget della barra laterale nel codice comune**, con
  `WidgetStore`: salva con `SettingsStore` (sul telefono le stesse
  preferenze "widgets" di sempre, sul PC il file `widgets.properties` nella
  cartella dei dati). Le notifiche dei timer, l'avviso di fine e la sveglia
  passano da `WidgetEffects`: sul telefono sono `TimerNotifications`,
  `Notifier` e `NoteAlarms` come prima, sul PC per ora niente. Il suono
  "una volta sola" di pomodori e notifiche è `RingtonePlayer`, sul telefono
- **[Progetto]** **Altre azioni di ogni sistema**: la tastiera aperta e dove
  sta arrivando (`isKeyboardVisible`, `keyboardTargetInsets`), la finestra
  dei selettori di data e ora in fondo allo schermo (`PlaceDialogAtBottom`),
  la cartella della cache e le immagini dai byte (`cacheDirOf`,
  `decodeImageBitmap`, per gli stemmi), i file che viaggiano con l'app
  (`readAppAsset`: sul PC gli asset del telefono sono fra le risorse del
  programma) e le emoji che il sistema sa disegnare, la scelta dei suoni del
  pomodoro (`rememberSoundPicker`), i suoni del contatore e l'impronta
  (`Biometrics`: sul PC c'è solo la password). L'immagine come icona di una
  pagina o di un quaderno si sceglie con `rememberImagePicker`
- **[Progetto]** **44 file in più nel codice comune**: la barra laterale con
  tutti i widget, i quaderni, il selettore delle icone e le emoji, il blocco
  con password, i selettori di data e ora, il correttore, molte celle e
  menu dei database. Adesso sono 247 file comuni (65.000 righe) e 70 solo
  del telefono (36.000), quasi tutti dell'editor e del database. Tolti
  dall'editor e dal database gli import rimasti senza uso
- **[Progetto]** `DesktopWidgetsTest`: sul PC un contatore si salva nel file dei
  widget, le emoji del selettore si leggono dalle risorse del programma, e
  la cartella della cache e le immagini dai byte funzionano

### ✏️ v0.153.8 — 04/11/2026 · 13:26 — Altri nodi sciolti verso l'editor: "indietro", foto, esportazione e notifiche per piattaforma
*(Claude Code sul PC; provata sul telefono; 1101 test passati)*

- **[Progetto]** **Le cose di ogni sistema che l'editor e il database usano**
  sono dichiarate nel codice comune: il gesto "indietro" (`BackHandler`, sul
  telefono quello di Android), la scelta di un'immagine per icona e copertina
  (`rememberImagePicker`: sul telefono il selettore di foto di sistema, sul
  PC la scelta di un file), l'esportazione in un file (`exportToFile`: sul
  telefono `PageExporter`, sul PC arriverà), la notifica di un bottone
  (`postButtonNotification`), il sito e lo schema di un link (`urlHost`,
  `urlScheme`: sul telefono l'`Uri` di Android), l'altezza della vista
  (`rememberRootHeight`, per il calendario del giorno). `LocalContext` è
  diventato `LocalPlatformContext` in 28 file (sul telefono è lo stesso
  `Context`); `ExportedFile` ed `ExportOptions` sono nel codice comune
- **[Progetto]** Sei file in più nel codice comune (il codice dei bottoni,
  i blocchi di codice, le menzioni di pagina, l'esportazione dalla pagina,
  le richieste delle notifiche, i siti dei link); dall'editor sono usciti in
  `EditorParts.kt` la copertina, la scelta di icona e copertina, il catalogo
  del menu "/" e i numeri degli elenchi, e dalle notifiche la lettura
  dell'`Intent` (`NotificationIntents.kt`). Il colore scritto come testo
  passa da `parseColorString` anche nei bottoni, nel calendario e nei
  numeri. Restano da sciogliere i widget (`WidgetStore`), i selettori di
  data e ora di Samsung e le misure della tastiera di Android

### ✏️ v0.153.7 — 04/11/2026 · 10:10 — I primi nodi sciolti: ricerca, preferiti, cestino e modelli nel codice comune
*(Claude Code sul PC; provata sul telefono; 1101 test passati)*

- **[Progetto]** **30 file in più nel codice comune** (ora 184, 43.500 righe;
  dal lato del telefono ne restano 105, 57.000 righe, quasi tutte l'editor e
  il database): le schermate della ricerca, dei preferiti, del cestino, dei
  modelli e dell'avvio, i pezzi delle viste dei database (`DatabaseViews`,
  "Group", "Date by", le relazioni, l'ordine dei tag, le frecce fra le
  righe), le menzioni, l'inchiostro (`InkLayer`, `InkTools`), i segnalibri
  della pagina, il "torna su", le parti comuni delle schermate
  (`ScreenParts`) e l'immagine di una pagina (`PageImageView`)
- **[Progetto]** **I nodi sciolti**: i pezzi che tutte le schermate usano e
  che stavano dentro `DatabaseViewScreen` (`SheetGroup`, `SettingsRow`,
  `SheetTextField`, `TagChip`, i gruppi delle righe, i nomi delle date…)
  ora sono in `DatabaseParts.kt`; dall'editor sono usciti `PageImage` e
  `LinePrefix`, da altri file la scelta di come si vede un link, il nome
  della colonna "Name", i suoni del contatore, i modi del Web Clipper, le
  richieste di permesso delle notifiche. I 50 `Toast` passano da
  `showMessage` (sul telefono sono sempre `Toast`, anche quelli lunghi), le
  misure dello schermo da `screenWidthDp`/`screenHeightDp` (sul telefono
  quelle di prima), il colore dei tag da `parseColorString` (gli stessi
  colori di `Color.parseColor` di Android, controllato da `ColorParseTest`
  sui colori dei tag), l'immagine da disegnare da
  `toImageBitmap`. La chiusura del cursore quando scende la tastiera è del
  telefono (sul PC non c'è tastiera sullo schermo), e gli strumenti
  dell'inchiostro si ricordano con le impostazioni comuni (sul telefono nel
  file `ink` di sempre)
- **[Progetto]** Come si fa: uno strumento toglie da un file le
  dichiarazioni che servono altrove (con quelle che usano), un altro ripulisce
  gli import, e il solito giro sposta nel codice comune tutto quello che
  compila sia per il telefono che per il PC. Una classe "sigillata" del codice
  comune non può avere eredi nel codice del telefono: per questo `Widgets.kt`
  resta con i widget, finché non passano tutti insieme

### ✏️ v0.153.6 — 04/11/2026 · 09:44 — Tutta l'app del telefono nel modulo comune, accanto al codice del PC
*(Claude Code sul PC; provata sul telefono; 1099 test passati)*

- **[Progetto]** **Il modulo `app` ora ha solo `MainActivity`,
  `ShareActivity` e il widget della nota al volo** (che usa il layout e i
  testi dell'app). Tutto il resto, 115 file (l'editor, i database, le altre
  schermate, le notifiche, i backup, il Web Clipper, la registrazione…), è
  in `shared/src/androidMain`, con le stesse librerie di prima: sul
  telefono il codice è lo stesso. I font delle pagine e l'icona delle
  notifiche sono risorse di `shared` (`com.gabriele.notionlocal.shared.R`);
  le notifiche aprono `MainActivity` per nome (`mainActivityIntent`), perché
  da `shared` la classe non si vede
- **[Progetto]** **Perché**: le schermate si usano una con l'altra, e da
  sole non passavano al codice comune. Ora stanno nello stesso modulo e ci
  possono passare un pezzo per volta, con lo stesso giro di prima (si
  spostano tutte, quelle che non compilano per il PC tornano indietro):
  `UpdatesScreen` è la prima. Un conteggio dice dove sono i nodi: i pezzi
  che usano tutte le schermate e che stanno dentro `DatabaseViewScreen`
  (`SheetGroup`, `SettingsRow`, `SheetTextField`, `TagChip`…), i widget
  (`WidgetStore`), gli strumenti dell'inchiostro (`InkTools`) e alcune
  funzioni senza Android chiuse in file Android. Nel codice comune ci sono
  anche le librerie multipiattaforma di `collectAsStateWithLifecycle` e di
  `viewModel(...)` (sul telefono la 2.9.4 di sempre); `AutoCapsTest` cerca i
  campi di testo anche nei sorgenti di `shared`

### ✏️ v0.153.5 — 04/11/2026 · 09:23 — Le prime schermate nel codice comune, e le azioni di ogni sistema
*(Claude Code sul PC; provata sul telefono; 1099 test passati)*

- **[Progetto]** **25 file in `shared`**, fra cui le prime schermate: la
  scelta dei colori (`ColorPickerSheet`), i promemoria delle date, "Full
  line view", il ridimensionare le immagini, "Move to", la cronologia della
  pagina, la barra di scorrimento della pagina, la barra di testo dei
  pannelli (`SheetTextToolbar`), i colori del codice, le icone di Material
  (`MaterialIconSet`), i blocchi sincronizzati (`SyncedBlockUi`), le tabelle
  ferme, e poi il correttore (dizionari e suggerimenti), l'esportazione in
  Markdown, l'archivio dei backup, le rotte della navigazione e altri
  pezzi. Quali file passano lo decide il compilatore: un giro sposta tutti
  quelli senza Android, quelli che non compilano tornano indietro così
  com'erano, e si ripete finché il codice comune compila
- **[Progetto]** **Le azioni di ogni sistema** (`platform/PlatformActions.kt`):
  un avviso breve, aprire un link, copiare e leggere gli appunti, i nomi di
  Taglia/Copia/Incolla/Seleziona tutto. Sul telefono sono quelle di Android
  di prima (il `Toast`, l'`Intent` per il browser, gli appunti di Android,
  i nomi dei comandi nella lingua del telefono); sul PC il browser e gli
  appunti di Windows e Mac, e gli avvisi in basso nella finestra
  (`DesktopMessages`). Le usano già `BlockFiles.openUrl`, i link delle celle
  (`CellText`) e la barra di testo dei pannelli; `ScreenParts` prende il
  contesto comune. `AutoCaps` (la maiuscola a inizio frase della tastiera
  del telefono) è dichiarato nel codice comune: sul telefono è quello di
  prima, sul PC lascia il campo com'è. `ViewModelFactory` è in `shared`, e
  chiede i ViewModel per classe Kotlin (`KClass`), come fa la libreria
- **[Progetto]** **Cosa resta**: le schermate si usano una con l'altra, e
  molti pezzi che tutte usano (`SheetGroup`, `SettingsRow`, `PageImage`,
  `NoteBottomSheet`…) stanno dentro i due file più grandi, l'editor
  (`PageEditorScreen`) e il database (`DatabaseViewScreen`). Il prossimo
  passo li sposta insieme, con le parti di Android (i video, il Web Clipper,
  la fotocamera, la scelta dei file) separate per piattaforma

### ✏️ v0.153.4 — 04/11/2026 · 09:06 — I ViewModel nel codice comune: le schermate si muovono allo stesso modo sul PC
*(Claude Code sul PC; provata sul telefono; 1099 test passati)*

- **[Progetto]** **I ViewModel sono in `shared`**: quello dell'editor,
  dei database, della ricerca, della barra laterale, dei quaderni,
  dell'inchiostro e delle altre schermate (la libreria dei ViewModel
  multipiattaforma 2.9.5, che sul telefono è la 2.9.4 di Google, la stessa
  di prima). `ViewModelFactory` resta nell'app: il PC avrà il suo. Con loro
  sono passati `LinkPreviews` e `NaturalOrder`, e alcuni pezzi tolti da
  file Android: `EraserMode` (da `InkTools`), `linkUri` (da `CellText`),
  `runGroupKey` e i segni degli elenchi puntati (da `PageEditorScreen`,
  ora `RunGroups.kt` e `BulletMarkers.kt`), il corpo del testo predefinito,
  minimo e massimo (da `PageFonts`, ora `PageFontSizes.kt`) e `QuickNotes`
  (leggere la richiesta dall'`Intent`, `takeFrom`, resta in `Incoming.kt`)
- **[Progetto]** Quello che nell'editor era di Android: l'esportazione in
  un file è un'estensione nell'app (`PageEditorExport.kt`, chiamata come
  prima); il testo delle foto si chiede a `PhotoTextRequests`, dove sul
  telefono si registra `ImageTexts`; le pagine di un documento scansionato
  arrivano con la funzione che le salva (sul telefono, dall'`Uri` dello
  scanner); l'ora del blocco spostato è quella di Java (serviva solo a
  distinguere uno spostamento dal successivo). Sul PC il "filo principale"
  dei ViewModel è quello della finestra (`kotlinx-coroutines-swing`): senza,
  il primo test sul PC si fermava subito. Test: `DesktopViewModelTest`
  (l'editor apre una pagina e la ricerca la trova, sul database del PC)

### ✏️ v0.153.3 — 04/11/2026 · 08:54 — Le immagini delle pagine nel codice comune, lette anche sul PC
*(Claude Code sul PC; provata sul telefono; 1097 test passati)*

- **[Progetto]** **`PageImageStore` è in `shared`**: icone, copertine e
  file dei blocchi, con le regole di sempre (nel database il nome del file,
  una copia per ogni pagina duplicata, quello che non è un'immagine non
  resta, al più sei megapixel in memoria). Quello che cambia fra telefono e
  PC è in fondo al file: `PlatformBitmap` (la `Bitmap` di Android, quella di
  Skia sul PC), la cartella, le misure, l'orientamento della foto (EXIF) e
  la lettura rimpicciolita. Sul telefono è il codice di prima, in
  `PageImageStore.android.kt`; prendere un file dalla galleria
  (`saveFromGallery`, `saveFile`, con l'`Uri` di Android) è un'estensione
  lì, sopra il comune `saveFromStream`. Sul PC le immagini stanno in
  `page-images` nella cartella dei dati, e Skia le legge, le gira come va
  guardata una foto in verticale (`encodedOrigin`) e le rimpicciolisce in
  un solo disegno. `toMatrix` di Skia vuole le misure della foto già
  girata: con quelle del file la foto finiva fuori dal quadro, e il test
  l'ha trovato
- **[Progetto]** `BlockFiles` resta nell'app: è tutto Android (aprire un
  file con un'altra app, salvarlo nella galleria, il nome che gli dà il
  telefono); quello del PC arriverà con le sue schermate. Test:
  `DesktopImageStoreTest` fa una foto rossa e blu con l'etichetta EXIF "girala
  di 90 gradi" e controlla che sul PC si misuri e si veda dritta, che quello
  che non è un'immagine non resti, e che una copia abbia il suo file

### ✏️ v0.153.2 — 04/11/2026 · 08:29 — Le repository nel codice comune: pagine e database si leggono e si scrivono allo stesso modo sul PC
*(Claude Code sul PC; provata sul telefono; 1094 test passati)*

- **[Progetto]** **`PageRepository`, `DatabaseRepository` e
  `NotebookRepository` sono in `shared`**, con quello che usano e che non
  ha bisogno di Android: `TextStats`, `DatabaseChanges` (le automazioni),
  `PageExport`, la cronologia delle pagine (`data/history`),
  `InkGeometry`, `SyncedBlocks` e `AppLock`, che ora salva la password da
  `SettingsStore` come `AppSettings` (sul telefono nello stesso file
  `app_lock` di sempre; `SettingsStore` sa anche togliere una chiave)
- **[Progetto]** **Le transazioni**: le repository usano `withTransaction`
  di `com.gabriele.notionlocal.data` (`Transactions.kt`). Sul telefono è
  proprio quello di Room per Android, come prima; sul PC, dove non c'è, la
  connessione di scrittura di Room (`useWriterConnection`) con una
  transazione `IMMEDIATE`. **Le sveglie dei promemoria**: dopo una
  scrittura le repository chiamano `ReminderAlarms.reschedule()`, e sul
  telefono a rispondere è `NoteAlarms`, che si registra all'avvio (prima,
  come prima, non succede niente); sul PC per ora nessuno. Ancora un punto
  dove Kotlin non si fida del controllo su una proprietà che viene da un
  altro modulo (`DuplicateSheet`). Test: `DesktopRepositoryTest` duplica
  una pagina con Annulla e Ripristina, la butta nel cestino e la
  ripristina, e cerca nel testo, sul database del PC

### ✏️ v0.153.1 — 04/11/2026 · 00:38 — Il database nel codice comune: lo stesso sul telefono e sul PC
*(Claude Code sul PC; provata sul telefono; 1091 test passati)*

- **[Progetto]** **Il database è in `shared`**: i DAO e `AppDatabase` (le
  14 tabelle, versione 70), con Room che ne scrive il codice per telefono e
  PC (`kspAndroid` e `kspDesktop` in `shared/build.gradle.kts`; l'app non
  ha più KSP). `@ConstructedBy(AppDatabaseConstructor::class)`: un
  `expect object` che Room completa da sé. Il file lo apre
  `buildAppDatabase`, diverso per ognuno: sul telefono `notion_local.db`
  come sempre, con le 65 migrazioni dalla 5 alla 70 (passate dal
  `companion object` a `PhoneMigrations`, in `AppDatabase.android.kt`:
  sono scritte con `SupportSQLiteDatabase`, che c'è solo su Android); sul
  PC `notion_local.db` nella cartella dei dati, con l'SQLite dentro l'app
  (`androidx.sqlite:sqlite-bundled` 2.6.2), dove nasce già alla versione
  70. L'impronta dello schema che Room salva nel file è la stessa di prima
  (`13d967df…`), sul telefono e sul PC: i dati del telefono si aprono come
  sempre, e il file del telefono e quello del PC sono uguali. **Le
  migrazioni da qui in avanti** vanno scritte col `SQLiteConnection` di
  Room, che vale per tutti e due
- **[Progetto]** Con i DAO sono passati in `shared` anche
  `data/notify/Reminders.kt` e `NotificationCommand.kt` (le righe dei
  promemoria che due DAO restituiscono). Le repository restano nell'app,
  per ora: usano `withTransaction`, che c'è solo su Android (sul PC c'è
  `useWriterConnection`). Test: `DesktopDatabaseTest` scrive una pagina sul
  PC in una cartella temporanea, chiude il database, lo riapre e la ritrova

### 🆕 v0.153 — 04/11/2026 · 00:20 — PageFlow comincia sul PC: il codice comune a telefono e Windows
*(Claude Code sul PC; provata sul telefono; 1090 test passati)*

- **[Nuova funzionalità]** **[💻 PC]** **La prima finestra di PageFlow sul
  PC**: per ora mostra le Patch Notes, l'elenco delle versioni e la pagina
  di ognuna, con lo stesso codice e gli stessi colori del telefono. Non si
  scarica ancora: si prova dal progetto con `gradle :desktop:run`. Le
  impostazioni del PC stanno in `%APPDATA%\PageFlow` (sul Mac in
  `~/Library/Application Support/PageFlow`)
- **[Progetto]** **Tre moduli**: `shared` (Kotlin Multiplatform, il codice
  comune a telefono e PC), `app` (l'app Android) e `desktop` (Compose
  Multiplatform 1.11.1, per Windows e Mac). In `shared`, con gli stessi
  pacchetti di prima: le entità del database (`data/entity`), le formule,
  i testi in 8 lingue (tranne quelli dei widget), il tema, i formati,
  `AppSettings`, i bottoni, le Patch Notes con la loro schermata e la
  barra in alto delle schermate (`WorkspaceTopBar`). Il database (DAO e
  `AppDatabase`) resta in `app`, e Room legge le entità dall'altro modulo.
  Visto che telefono e PC sono tutti e due JVM, il codice comune usa
  `java.time` e il resto di Java come prima
- **[Progetto]** Quello che cambia fra telefono e PC sta in `androidMain` e
  `desktopMain` (`expect`/`actual`): `PlatformContext` (sul telefono è
  proprio il `Context` di Android, quindi chi lo passava non è cambiato),
  `LocalPlatformContext`, dove si salvano le impostazioni (`SettingsStore`:
  sul telefono le `SharedPreferences` di sempre, sul PC un file
  `.properties`) e da dove si leggono le Patch Notes (gli asset del
  telefono, le risorse del PC: il task `patchNotes` di
  `desktop/build.gradle.kts` le copia dal README come quello dell'app).
  Divisi in due: da `EmojiCatalog.kt` le categorie, da `ButtonRunner.kt`
  `ButtonProblem`, da `ProofingLanguage.kt` i dizionari
  (`SpellDictionaries.kt`), da `ScreenParts.kt` la barra in alto. Le
  dichiarazioni `internal` dei file spostati sono diventate pubbliche
  (l'app è un altro modulo), e in cinque punti un valore va messo in una
  variabile prima di usarlo: Kotlin non si fida del controllo fatto su una
  proprietà che viene da un altro modulo. Test: `DesktopScreensTest`
  disegna le schermate del PC in `desktop/build/desktop-screenshots`, senza
  aprire finestre e senza toccare la cartella vera delle impostazioni

### 🔧 v0.152.3.1 — 04/11/2026 · 00:01 — Kotlin 2 e Compose 1.11: il primo passo verso Windows e Mac
*(Claude Code sul PC; provata sul telefono; 1088 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Il cursore non si spegne più da solo
  quando la tastiera sullo schermo non si apre**, per esempio con una
  tastiera vera collegata al telefono: dopo mezzo secondo l'app credeva
  che la tastiera fosse stata chiusa, e toglieva il cursore. Adesso lo
  toglie solo quando si chiude una tastiera che si era aperta. Servirà
  anche su Windows e Mac, dove la tastiera sullo schermo non c'è
- **[Progetto]** **Gli strumenti aggiornati, primo passo verso Windows e
  Mac**: l'app per il PC si farà con Compose Multiplatform, che vuole
  Kotlin 2 e un Compose recente. Kotlin 1.9.24 → 2.4.21 (il compilatore
  di Compose è il plugin `org.jetbrains.kotlin.plugin.compose`, non più
  `kotlinCompilerExtensionVersion`), KSP 2.3.13, Compose 1.6.8 → 1.11.4
  (BOM 2026.06.01; material3 1.2.1 → 1.4.0), Room 2.6.1 → 2.8.5,
  kotlinx.serialization 1.6.3 → 1.9.0, plugin Android 8.5.2 → 8.13.2 con
  Gradle 8.14.3 (la 8.7 non basta più), Robolectric 4.13 → 4.17. Compose
  1.12 chiede il plugin Android 9.1 e compileSdk 37: si passerà dopo
- **[Progetto]** Cosa è cambiato nel codice con Compose 1.11:
  `beyondBoundsPageCount` si chiama `beyondViewportPageCount`;
  `CaretInView.kt` dice dov'è il cursore con `focusProperties { focusRect }`
  (l'aggancio interno di Compose 1.6 che usava non c'è più); i campi
  copiano e incollano dai nuovi appunti (`LocalClipboard`), che
  `LinkClipboard` ora implementa: l'incolla arriva come testo semplice e
  `formatPaste` ci rimette link, stili e tipo delle righe;
  `ClearFocusWhenKeyboardCloses` spegne il cursore solo se la tastiera si
  era vista (il bug fix qui sopra). Nei test: `enterTouchMode` (`TouchMode.kt`)
  mette Robolectric in modalità tocco come il telefono (senza, tolto il
  cursore, Android lo ridava al primo campo), e la finestra dal basso di
  material3 1.4 è un dialog, non più un popup

### ✏️ v0.152.3 — 03/11/2026 · 20:51 — "Sort groups" anche nella pagina "Group"
*(Claude Code sul PC; provata sul telefono; 1088 test passati)*

- **[Modifica]** **[📱 Mobile]** **"Sort groups" anche nella pagina
  "Group"** di un database, sotto l'elenco dei raggruppamenti: Manual,
  Ascending, Descending e i gruppi da trascinare con ≡, senza dover prima
  toccare il nome della proprietà. Con più raggruppamenti ce n'è uno per
  ognuno, col nome della sua proprietà ("Sort groups · Done"). Le date
  hanno il loro ordine in "Date by" ("Newest first"), nella loro pagina
- **[Progetto]** `GroupSortSection` in `GroupRulesUi.kt`, usata dalla
  pagina della regola e da `GroupRulesPage` (che riceve i gruppi di ogni
  regola da `DatabaseViewScreen`). Test: `GroupRulesScreenTest` (la pagina
  "Group" ha "Sort groups" e Descending si salva; con due regole, il nome
  della proprietà)

### ✏️ v0.152.2 — 03/11/2026 · 20:04 — Le richieste dell'utente restano sul suo telefono
*(Claude Code sul PC; provata sul telefono; 1086 test passati)*

- **[Modifica]** **[📱 Mobile]** Le **Patch Notes** dell'app pubblicata
  su GitHub non hanno più le richieste dell'utente parola per parola: solo
  cosa è cambiato, versione per versione. L'app sul telefono dell'utente le
  ha ancora tutte
- **[Progetto]** Il README su GitHub è senza le richieste dell'utente: tolte
  le citazioni (con la frase che le introduceva), il diario della sessione
  cloud del 24/09/2026 e le frasi dell'utente dentro le voci. La copia
  completa è `private/README.md`, che resta sul PC (`.gitignore`). In
  `app/build.gradle.kts` `-Ppersonal` fa prendere le Patch Notes da lì: con
  quello si compila l'app per il telefono dell'utente, senza quello l'APK
  della Release. Ogni voce nuova va in tutti e due i README: con la richiesta
  in quello privato, senza in quello pubblico

### ✏️ v0.152.1 — 03/11/2026 · 18:33 — L'app si chiama PageFlow, con l'icona nuova
*(Claude Code sul PC; provata sul telefono; 1086 test passati)*

- **[Modifica]** **[📱 Mobile]** **L'app si chiama PageFlow**: sotto
  l'icona, in "Condividi", nelle notifiche e in tutte le scritte dell'app,
  nelle 8 lingue
- **[Modifica]** **[📱 Mobile]** **L'icona nuova**: i fogli blu e verdi
  con la linea che li attraversa, su fondo scuro. È ridotta quanto basta
  perché nessuna forma di icona (cerchio, squircle) tagli la freccia o gli
  angoli dei fogli
- **[Modifica]** **[📱 Mobile]** I backup nuovi si chiamano
  "PageFlow-backup-…" e stanno in **Download/PageFlow backup**; quelli in
  "Download/notE backup" che l'app ha creato ci si spostano da soli, col nome
  nuovo, gli altri restano dove sono. Tutti si vedono e si ripristinano come
  prima. Le immagini salvate vanno in Immagini/PageFlow
- **[Nota]** Il pacchetto interno (`com.gabriele.notionlocal`) e il nome
  del database non cambiano: cambiandoli il telefono vedrebbe un'altra app,
  senza i dati e senza gli aggiornamenti. Un'app desktop o web ancora non
  esiste: l'icona per loro è pronta in `branding/` (1024, 512, 192 e 180 px e
  le favicon)
- **[Progetto]** Il repository su GitHub si chiama **PageFlow**
  (`AbernathyXVII/PageFlow`; i link vecchi portano lì). `app_name`, le
  scritte di `BackupStrings`, `BlockStrings`, `HistoryStrings`,
  `NotifyStrings`, `ShareStrings`; `Backups.FOLDER` e `OLD_FOLDERS`,
  `BACKUP_PREFIX` e `OLD_BACKUP_PREFIX`, `BackupArchive.MANIFEST` e
  `OLD_MANIFEST` (si leggono anche i backup di prima); il segno delle righe
  copiate negli appunti (anche quello vecchio); `ic_launcher_foreground.png`
  rifatta dall'immagine; `rootProject.name`. Test: `BackupTest` (un backup
  di quando l'app si chiamava notE si vede, si sposta e si ripristina)

### 🆕 v0.152 — 03/11/2026 · 14:14 — Registrare l'audio, scansionare i documenti e cercare nel testo delle foto
*(Claude Code sul PC; provata sul telefono: una scansione e una parola trovata dentro una foto; la registrazione fino al bottone; 1085 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Record audio"** nei menu "/" e
  "+" (e "Record audio" sotto "Choose file" in un blocco audio vuoto): il
  blocco comincia subito a registrare, chiedendo il microfono la prima
  volta. Mentre registra mostra il pallino rosso, il tempo e quanto è forte
  il suono; **Stop** e la registrazione diventa il file del blocco, da
  ascoltare lì; la **crocetta** la butta via
- **[Nuova funzionalità]** **[📱 Mobile]** **La registrazione continua a
  schermo spento** o uscendo dalla pagina o dall'app: c'è la notifica
  "Recording audio" col tempo che passa e **Stop**. Fermata da lì, il file va
  comunque nel suo blocco (o, se intanto è sparito, in un blocco audio nuovo
  in fondo alla pagina). Una registrazione alla volta
- **[Nuova funzionalità]** **[📱 Mobile]** **"Scan document"** nei menu "/"
  e "+": lo scanner di Google — trova da sé i bordi del foglio, lo
  raddrizza, ha i filtri e fa più pagine di fila (fino a 50), anche da foto
  già fatte. Le pagine diventano foto nella pagina, al posto della riga se
  è vuota, se no sotto di lei; Annulla le toglie in un passo
- **[Nuova funzionalità]** **[📱 Mobile]** **La ricerca trova il testo
  delle foto**: quello che c'è scritto nelle immagini delle pagine (le
  scansioni, gli scontrini, le foto della lavagna…) si legge da sé, in
  sottofondo, una foto alla volta: quelle nuove subito, quelle che c'erano
  già dopo l'avvio. Nei risultati il pezzo trovato comincia con 🖼. Si
  leggono i caratteri latini, cinesi, giapponesi e coreani
- **[Nota]** Lo scanner e il lettore del testo sono di Google (ML Kit) e
  stanno in Google Play Services, che scarica i modelli la prima volta: l'app
  resta leggera, e le foto non escono dal telefono. I modelli di cinese,
  giapponese e coreano arrivano qualche secondo dopo l'installazione: una
  foto letta prima si rilegge all'avvio dopo. Una foto si legge una volta
  sola; cancellando per sempre la sua pagina, il suo testo se ne va con lei.
  Anche le foto arrivate con "Condividi" si leggono subito
- **[Progetto]** Database alla versione 70 (`image_texts`, migrazione
  69→70); `ImageTextEntity`, `ImageTextDao`; `data/ocr/ImageTexts.kt`
  (`TextReader`, il lettore di ML Kit, `readPending`); la ricerca in
  `PageRepository.search`; `data/record/AudioRecorder.kt` (`AudioRecorder`
  e `RecordingService`, servizio in primo piano col microfono);
  `PageRepository.putRecording` e `insertScannedPages`; `CaptureUi.kt`
  (`RecordingBar`, `rememberRecordStarter`, `rememberDocumentScanner`);
  `SlashAction.Record` e `Scan`; `MediaRequestKind.RECORD`;
  `CaptureStrings`; i permessi `RECORD_AUDIO`, `FOREGROUND_SERVICE` e
  `FOREGROUND_SERVICE_MICROPHONE`; le librerie di ML Kit. Test:
  `CaptureTest` (dove va una registrazione, le pagine scansionate, una foto
  letta una volta e trovata dalla ricerca, una che non si riesce ancora a
  leggere, il testo che se ne va con la pagina, il tempo), `MigrationTest`
  dalla 69

### 🆕 v0.151 — 03/11/2026 · 13:49 — I blocchi sincronizzati: le stesse righe in più pagine, che cambiano insieme
*(Claude Code sul PC; provata sul telefono con due copie su una pagina di prova; 1078 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Synced block"** nei menu "/"
  e "+" e in "Turn into": la riga diventa un blocco sincronizzato, una
  cornice arancione con dentro le sue righe (quello che c'era scritto
  diventa la prima), e il cursore ci va dentro. Dentro si scrive come in un
  toggle: testo, elenchi, caselle, tabelle, immagini
- **[Nuova funzionalità]** **[📱 Mobile]** **"Sync to another page"** nel
  menu del blocco (toccando la scritta "Synced block" in cima, o tenendo
  premuto): si sceglie una pagina e una copia va in fondo a lei. In cima
  alla cornice si legge in quante pagine sta ("Synced block · 2 pages")
- **[Nuova funzionalità]** **[📱 Mobile]** **Scrivi in una copia e cambiano
  tutte**: una lettera, una riga nuova, una casella spuntata, una cella di
  una tabella. Nessuna copia è l'originale: si può scrivere in qualunque
  delle pagine, anche in due copie nella stessa pagina
- **[Nuova funzionalità]** **[📱 Mobile]** **"Unsync"**: questa copia torna
  righe normali, al suo posto; le altre restano sincronizzate fra loro.
  Eliminare una copia non tocca le altre
- **[Nota]** Una sottopagina o un database creati dentro il blocco vivono
  nella pagina dove sono nati; nelle altre copie sono un "Link to page" a
  loro. Le copie usano gli stessi file (le immagini): cancellando per sempre
  una pagina, un file ancora usato da un'altra copia resta. Un blocco
  sincronizzato non va dentro un altro. L'export in PDF e Markdown e la
  cronologia della pagina mostrano le righe del blocco come le altre
- **[Progetto]** `BlockType.SYNCED` con `BlockAttrs.syncId` (niente
  migrazione: sta nel JSON delle voci in più); `data/sync/SyncedBlocks.kt`
  (`SyncedLine`, la firma del contenuto, la pausa e le riscritture attese,
  e `SyncedWatcher`, che nella pagina aperta vede cosa cambia); in
  `PageRepository` `makeSynced`, `addSyncedCopy`, `pushSynced` (al loro
  posto se la forma è la stessa, se no da capo), `unsync`,
  `syncedPageCount`, e i file condivisi in `deletePermanently`;
  `SyncedBlockUi.kt` (la cornice); `SlashAction.Synced`; il menu in
  `BlockActionsSheet`; `SyncStrings`. Test: `SyncedBlocksTest` (la prima
  riga, la copia, una riga riscritta al suo posto, una riga nuova e le
  tabelle, la sottopagina che resta a casa, "Unsync", i file),
  `SyncedBlockTest` (il cursore dentro, una modifica che arriva all'altra
  pagina, due copie nella stessa pagina che non si rimbalzano)

### ✏️ v0.150.1 — 03/11/2026 · 13:22 — Niente "Turn into" e "Color" grigi nel menu di un database
*(Claude Code sul PC; provata sul telefono; 1067 test passati)*

- **[Rimosso]** **[📱 Mobile]** Nel menu di un database dentro una pagina
  (quello che si apre dal blocco) non ci sono più **"Turn into"** e
  **"Color"**: erano tutte e due grigie, senza niente da fare. Per il
  collegamento a una pagina restano come prima
- **[Progetto]** In `BlockActionsSheet.kt` (`PageBlockMenu`) il gruppo si
  mostra solo se non è un database

### 🆕 v0.150 — 03/11/2026 · 13:09 — Le automazioni dei database: quando cambia una proprietà, quando arriva una pagina, a orario
*(Claude Code sul PC; provata sul telefono su un database di prova; 1067 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Automations"** nei tre
  puntini di un database a schermo intero: l'elenco delle sue automazioni,
  ognuna col suo quando a parole ("Fatto diventa Spuntata", "Ogni settimana
  (lunedì) alle 09:00"), quante azioni fa, l'ultima volta che è partita e
  l'interruttore per accenderla e spegnerla. "New automation" ne fa una
- **[Nuova funzionalità]** **[📱 Mobile]** **Il quando**: **Property
  edited** — una proprietà scelta che cambia, o che diventa un valore
  (Spuntata o Non spuntata per una casella, una o più scelte per una
  Select); **Page added** — una pagina nuova nel database, comunque nata; o
  **ogni giorno**, **ogni settimana** nei giorni scelti, **ogni mese** in un
  giorno (l'ultimo, nei mesi più corti), all'ora scelta col calendario a
  rotelle
- **[Nuova funzionalità]** **[📱 Mobile]** **Le azioni** sono quelle dei
  bottoni, con la stessa finestra: "Edit pages in" (anche **This page**, la
  pagina che ha fatto partire l'automazione: "quando spunto Fatto, metti la
  data di oggi"), "Add page to" (di suo in questo database), "Insert
  blocks" e "Send notification". Niente conferme né pagine da aprire:
  un'automazione lavora da sola
- **[Nuova funzionalità]** **[📱 Mobile]** **"Add page to" col template**,
  anche nei bottoni: la pagina nuova nasce col contenuto del template
  scelto. E nel nome **{date}** diventa la data di oggi e **{week}** il
  numero della settimana: "Settimana {week}" ogni lunedì diventa
  "Settimana 45", "Settimana 46"…
- **[Nota]** Una cella di testo si salva a ogni lettera: l'automazione parte
  un secondo dopo l'ultima, e con una condizione solo se vale ancora
  (spuntata e subito tolta, niente). Le modifiche fatte da un'automazione non
  ne fanno partire altre, come su Notion, così non girano all'infinito.
  Quelle a orario partono con la sveglia delle notifiche, anche ad app
  chiusa; se il telefono era spento, alla prima occasione, una volta sola
- **[Progetto]** `AutomationEntity` (tabella `automations`, migrazione
  68→69) con `AutomationConfig` e `AutomationTrigger` (`PageAdded`,
  `PropertyEdited`, `Schedule`); `AutomationDao`; `data/automations`:
  `DatabaseChanges` (avvisato da `DatabaseRepository.addRow` e
  `setCellValue`, zitto dentro `AutomationRun`) e `Automations` (il motore,
  che usa `ButtonRunner` con un `ButtonHost` senza finestre; le automazioni
  a orario nella sveglia di `NoteAlarms`); `ButtonStep.AddPage.templateId`
  e i segnaposto del nome in `ButtonRunner`; `ButtonEditorContent` con
  intestazione, azioni e titolo a scelta; `AutomationsUi.kt`;
  `AutomationStrings`. Test: `AutomationsTest` (i giorni e le ore, quando
  una modifica la fa partire, "spunto Fatto → data di oggi", la pagina
  nuova e due automazioni che non si rincorrono, la pagina della settimana
  dal template una volta sola), `AutomationsScreenTest`, `MigrationTest`
  dalla 68

### ✏️ v0.149.2 — 03/11/2026 · 09:08 — "Startup window" in Settings, sopra "Title position"
*(Claude Code sul PC; provata sul telefono; 1060 test passati)*

- **[Modifica]** **[📱 Mobile]** **"Startup window"** non sta più nella
  barra laterale: è in **Settings**, subito sopra "Title position", con
  accanto la scelta di adesso ("Last visited page", "Main menu" o "Specific
  page"). Toccandola si apre la sua schermata di sempre, e indietro si torna
  a Settings
- **[Progetto]** Tolta da `Sidebar.kt` (e `SidebarActions.onStartup`); in
  `SettingsScreen` la voce e `onOpenStartup`, che `AppNavHost` collega alla
  schermata `Routes.STARTUP`

### ✏️ v0.149.1 — 03/11/2026 · 09:01 — Niente più "Import" ed "Export" grigi nella barra laterale
*(Claude Code sul PC; provata sul telefono; 1060 test passati)*

- **[Rimosso]** **[📱 Mobile]** Le voci **"Import"** ed **"Export"** della
  barra laterale, grigie e senza funzione: l'export si fa dai tre puntini di
  una pagina o di un database (vedi v0.147 e v0.149)
- **[Progetto]** Tolte da `Sidebar.kt`, con le loro icone

### 🆕 v0.149 — 03/11/2026 · 08:49 — L'export completo: i database come Notion, i bottoni con le loro azioni, i disegni a scelta
*(Claude Code sul PC; provata sul telefono su “Paragraph – prova” e su un database; 1060 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **I database nell'export**, come
  li mostra la loro vista: le colonne che si vedono (Name al suo posto), le
  righe filtrate e ordinate, e i valori scritti come nelle celle — date col
  formato di Settings, numeri, ✓ per le caselle, i nomi delle righe
  collegate dalle Relation, i risultati di formule e Rollup. Nel **PDF** una
  tabella, con le colonne larghe secondo quanto c'è scritto; oltre cinque
  colonne non ci starebbe, e ogni riga diventa una scheda col nome e sotto
  le proprietà. Nel **Markdown** una tabella e, accanto, il **CSV** del
  database con tutte le colonne, anche quelle nascoste, come fa Notion
- **[Nuova funzionalità]** **[📱 Mobile]** **Le pagine delle righe** che
  hanno qualcosa di scritto (o un'immagine, un file, un altro database…)
  diventano file a sé: nel PDF uno .zip con "Pagina.pdf" e la cartella
  "Pagina/Database/Riga.pdf" — e il 📄 davanti al nome della riga nella
  tabella; nel Markdown "Pagina/Database/Riga.md", a cui porta il nome della
  riga. Anche i database dentro quelle pagine, e così via
- **[Nuova funzionalità]** **[📱 Mobile]** **I bottoni** si esportano col
  loro nome e "Quando lo tocchi:", poi le azioni numerate con le parole
  della finestra in cui si scelgono: "Chiedi conferma: «…»", "Inserisci
  blocchi (sotto il pulsante): Casella «Bere acqua»", "Aggiungi pagina a
  «Tasks»: «Diario» — Status: Aggiungi «Casa, Urgente»", "Modifica pagine in
  «Tasks» — solo le pagine in cui Done è «Non spuntata»: …", "Apri pagina o
  link", "Manda una notifica". Una copia collegata dice le azioni del
  bottone vero
- **[Nuova funzionalità]** **[📱 Mobile]** **"Include drawings"** nella
  finestra "Export page", per il PDF e solo se la pagina ha disegni a mano:
  i tratti escono come nell'app (penna, matita, pennarello, evidenziatore,
  coi loro colori e la pressione della S Pen). Perché restino al loro posto
  la pagina tiene le proporzioni del telefono, su fogli A5: il testo e le
  righe alti come nell'editor. Un tratto che scende oltre il foglio
  continua in cima al foglio dopo
- **[Nuova funzionalità]** **[📱 Mobile]** **"Export" anche per un database
  a schermo intero**, dai suoi tre puntini: la sua tabella e le pagine delle
  righe
- **[Modifica]** **[📱 Mobile]** **"Print"** stampa sempre un PDF solo:
  senza i PDF delle righe, che farebbero uno .zip
- **[Nota]** I disegni nel PDF cadono dove sono stati fatti rispetto alla
  riga, al gruppo di righe o al blocco su cui li si è disegnati; sopra
  un'immagine, una tabella o un database che nel PDF è alto diverso
  dall'app possono spostarsi un po'. Nel Markdown i disegni non ci sono
- **[Progetto]** `data/export`: `DatabaseExport.kt` (`DatabaseReader`, che
  legge il database col ViewModel della schermata e aspetta che abbia
  finito di calcolare; `databaseRead`, `exportCellText`, `csvOf`),
  `ButtonExport.kt` (`exportButton`, `buttonReferences`), `PdfLayout`
  (documento A4 o proporzioni del telefono) e i disegni in `PdfExport`,
  `ExportOptions` e l'arricchimento delle pagine in `PageExporter` (database,
  bottoni, tratti, PDF delle righe nello .zip); in `PageRepository`
  `viewsOf`, `strokesOf`, `columnsOf`, `blockOf`; "Include drawings" in
  `PageExportSheet`; "Export" in `DatabaseViewScreen`. Test:
  `ExportExtrasTest` (database e bottone nel Markdown, il CSV, il bottone a
  parole, la vista del database con colonne nascoste e Relation, il tratto
  rosso nel PDF dove era stato fatto, le schede di un database largo, lo
  .zip con il CSV e solo le pagine delle righe con qualcosa) e
  `PageExportScreenTest` ("Include drawings" solo per il PDF)

### 🆕 v0.148 — 03/11/2026 · 08:19 — Le menzioni "@" di pagine e date, e i backlink in fondo alla pagina
*(Claude Code sul PC; provata sul telefono su “Paragraph – prova”; 1052 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"@" nel testo**: scritta la
  chiocciola a inizio riga o dopo uno spazio (in un indirizzo email no), si
  apre un menu con le **pagine** — le toccate per ultime, e poi quelle col
  nome che somiglia a quello che si scrive dopo la chiocciola, anche con gli
  spazi — e le **date**: oggi, domani, ieri e "Pick a date…" col calendario.
  Scelta una voce, al posto di "@…" resta il titolo della pagina (o la data
  scritta per intero, "3 novembre 2026") col suo collegamento, e uno spazio
  per continuare a scrivere
- **[Nuova funzionalità]** **[📱 Mobile]** **Toccare una menzione**: quella
  di una pagina la apre (un database nella sua vista); quella di una data
  apre il calendario per cambiarla. Una pagina menzionata e poi rinominata
  si vede col nome nuovo appena si riapre la pagina che la cita
- **[Nuova funzionalità]** **[📱 Mobile]** **I backlink in fondo alla
  pagina**: sotto l'ultima riga, la sezione **"Backlinks"** con quante e
  quali pagine richiamano questa — con una menzione "@" o con un "Link to
  page" — e per ognuna la riga in cui lo fa. Un tocco apre la pagina; il
  titolo della sezione la chiude e la riapre. Non c'è quando nessuna pagina
  la richiama, e si aggiorna tornando sulla pagina
- **[Modifica]** **[📱 Mobile]** Nell'export in Markdown le menzioni e i
  collegamenti alle righe della pagina restano testo semplice: fuori
  dall'app non porterebbero da nessuna parte
- **[Nota]** Il collegamento di una sottopagina nella pagina che la
  contiene non conta come backlink: è dove sta, non un richiamo. Le pagine
  nel cestino non compaiono né nel menu né fra i backlink. Il menu "@" c'è
  nelle righe di testo, negli elenchi e nelle caselle, come il menu "/"
- **[Progetto]** `data/entity/PageMentions.kt` (i collegamenti
  `note://page/…` e `note://date/…?m=…`, `replaceLinkedText`,
  `refreshPageMentions`); in `PageRepository` `backlinksOf`,
  `mentionCandidates` e `refreshMentionTitles`; `BlockDao.getBlocksMentioning`
  e `PageDao.recentlyUpdated`; `ui/screen/Mentions.kt` (`MentionMenu`,
  `BacklinksSection`, `LocalPageMentionOpen`, `LocalDateMentionTap`); il
  menu "@" nel campo di testo dell'editor accanto a quello "/"; le scritte
  in `MentionStrings`. Test: `MentionsTest` (i collegamenti, i titoli
  rimessi in pari, quando si apre il menu, chi richiama chi, la ricerca) e
  `MentionScreenTest` ("@Hot" e la pagina scelta dal menu; i backlink in
  fondo alla pagina citata, che la aprono)

### 🆕 v0.147 — 02/11/2026 · 23:40 — "Export": la pagina in PDF o in Markdown, da mandare, salvare o stampare
*(Claude Code sul PC; provata sul telefono su “Paragraph – prova”; 1045 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Export" nel menu "..." di una
  pagina** non è più grigia: apre la finestra **"Export page"**, dove si
  sceglie il formato (**PDF** o **Markdown**) e cosa farne: **Share** lo
  manda con un'altra app (WhatsApp, la posta, Drive…), **Save** lo salva in
  una cartella del telefono (o su Drive) con la finestra di sistema,
  **Print** (solo il PDF) apre la stampa di sistema, che sa anche salvarlo
  come PDF
- **[Nuova funzionalità]** **[📱 Mobile]** **Il PDF**: fogli A4 col titolo
  della pagina e la sua icona, il testo vero (si seleziona e si cerca) con
  grassetto, corsivo, sottolineato, barrato, pedice, apice, misure e colori,
  gli elenchi col pallino o il numero, le caselle (quelle spuntate barrate,
  come nell'app), le citazioni, i callout e il codice nel loro riquadro, le
  linee, le tabelle, le immagini larghe come nella pagina, i file, i video e
  i segnalibri col loro nome e indirizzo, e il numero di pagina in fondo ("2 / 5"). I colori
  pensati per il tema scuro che sul bianco della carta non si leggerebbero
  si scuriscono o si schiariscono
- **[Nuova funzionalità]** **[📱 Mobile]** **Il Markdown**: testo semplice
  coi segni che leggono Obsidian, GitHub e le altre app di note (`**`, `-`,
  `- [ ]`, `>`, le tabelle, i blocchi di codice col linguaggio), per non
  restare chiusi dentro l'app. Le righe scritte grandi (come i titoli presi
  dal web) diventano titoli. Una pagina con immagini o file diventa uno
  **.zip**, come fa Notion: "Pagina.md" e accanto la cartella "Pagina" coi
  file, a cui il testo porta con dei link
- **[Nuova funzionalità]** **[📱 Mobile]** **"Include subpages"**, se la
  pagina ha sottopagine: nel PDF ognuna comincia su un foglio nuovo dopo la
  pagina; nel Markdown vanno nello .zip, nella cartella della pagina, e il
  link alla sottopagina le apre. Solo le pagine figlie: un link a una pagina
  che sta altrove resta il suo titolo
- **[Nota]** Non si esportano (per ora) i disegni a mano sopra la pagina, i
  bottoni e il contenuto dei database: di un database resta il nome. I font
  scelti per il testo nel PDF diventano quello di sistema. Il database a
  schermo intero ha ancora "Export" grigio, come "Import" e le due voci
  della barra laterale
- **[Progetto]** `data/export`: `PageExport` (la pagina e le sue righe in
  ordine di lettura, `exportFileName`), `MarkdownExport`, `PdfExport`
  (impagina in due passate e scrive con `PdfDocument`), `PageExporter` (il
  file nella cache, lo .zip, `share`, `copyTo`, `print` con un
  `PrintDocumentAdapter`) e `SaveExport`; `PageRepository.exportPage`; la
  finestra `PageExportSheet`; `res/xml/shared_files.xml` passa alle altre
  app anche la cartella "exports"; le scritte in `ExportStrings`. Test:
  `PageExportTest` (una pagina intera in Markdown, i segni uno dentro
  l'altro, i nomi dei file, i fogli del PDF disegnati su un'immagine, lo
  .zip con i file e solo le sottopagine vere) e `PageExportScreenTest`

### 🆕 v0.146 — 02/11/2026 · 22:58 — "Condividi con notE" e la nota al volo: tasto rapido, widget e scorciatoia
*(Claude Code sul PC; provata sul telefono su una pagina di prova; 1038 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"notE" nel menu Condividi**
  delle altre app (Chrome, la Galleria, WhatsApp…): sopra l'app da cui si
  condivide sale la finestra **"Save to notE"**, col titolo da cambiare
  (quello della pagina web, o la prima riga del messaggio), quello che arriva
  (il link col suo sito, le prime righe del testo, quante immagini e file),
  **"Save to"** con la pagina in cui va, e se diventa **una pagina nuova**
  (il collegamento in fondo a quella pagina) o va **in fondo alla pagina**.
  **Save** salva e si torna all'app di prima; **Save and open** apre notE
  sulla pagina
- **[Nuova funzionalità]** **[📱 Mobile]** **Cosa diventa**: il testo righe
  di testo; un link un segnalibro, che la prima volta che si apre la pagina
  prende titolo, descrizione e immagine del sito; le foto immagini; video e
  audio i loro blocchi; il resto blocchi file. Una o tante cose insieme
- **[Nuova funzionalità]** **[📱 Mobile]** **La nota al volo**, da tre
  posti: il tasto **"Quick note" nelle impostazioni rapide** (si aggiunge
  dalla matita della tendina), il **widget "Quick note"** per la schermata
  Home e la **scorciatoia** che compare tenendo premuta l'icona di notE. Si
  entra in una pagina nuova col cursore già sulla prima riga. Lasciata vuota
  se ne va da sola; scritta senza titolo, prende per titolo la sua prima
  riga
- **[Nuova funzionalità]** **[📱 Mobile]** **Dove finiscono**: nella pagina
  scelta in Settings → **"Shared items and quick notes"** o condividendo;
  finché non se ne sceglie una, in **Inbox** (📥), che nasce in fondo al
  menu principale la prima volta che serve
- **[Nota]** Le immagini e i file si copiano dentro l'app salvando, come
  quelli scelti dal telefono. Le pagine bloccate con la password non si
  aprono: si vedono solo i titoli, come nella barra laterale. Le scritte del
  tasto rapido, del widget e della scorciatoia seguono la lingua del
  telefono
- **[Progetto]** `ShareActivity` (trasparente, coi filtri SEND e
  SEND_MULTIPLE) e `ShareSheet`; `Incoming` e `splitSharedText` per leggere
  quello che arriva; `QuickNotes`, `QuickNoteTile` (TileService),
  `QuickNoteWidget` e `res/xml/shortcuts.xml`; in `PageRepository`
  `inboxPageId`, `saveIncoming`, `createQuickNote` e `finishQuickNote`;
  `AppSettings.sharePageId`; le scritte in `ShareStrings`. Test:
  `IncomingTest`, `ShareRepositoryTest`, `ShareScreenTest` (un link da
  Chrome nell'Inbox, un messaggio in fondo alla pagina scelta, il titolo
  cambiato), `QuickNoteScreenTest` (la nota al volo che, lasciata vuota, se
  ne va)

### 🆕 v0.145 — 02/11/2026 · 16:59 — "Page history": le versioni di una pagina, da rileggere e rimettere
*(Claude Code sul PC; provata sul telefono su una pagina di prova; 1022 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Page history"** nei tre puntini
  di una pagina, sotto "Updates": le versioni salvate della pagina, divise per
  giorno (Today, Yesterday, poi la data), con l'ora, il titolo di allora e
  quante righe e parole aveva. Toccandone una si legge com'era la pagina
  (titolo, righe rientrate come nella pagina, caselle, elenchi numerati,
  toggle, tabelle, nomi di immagini, file e sottopagine). **"Restore this
  version"**, dopo la domanda, la rimette: tornano titolo, testo, tabelle e
  disegni
- **[Nuova funzionalità]** **[📱 Mobile]** **Quando nasce una versione**:
  aprendo la pagina, ogni 10 minuti mentre è aperta, uscendo dalla pagina e
  quando l'app va in secondo piano, ma solo se qualcosa è cambiato (aprire o
  chiudere un toggle non conta). **Quante se ne tengono**: tutte quelle delle
  ultime 24 ore; più indietro una al giorno (l'ultima di quel giorno) per 30
  giorni; l'ultima versione di una pagina resta sempre
- **[Nota]** Il ripristino si può disfare: prima di rimettere una versione,
  com'è la pagina in quel momento finisce nella cronologia, segnata "Before
  restoring". Non tornano l'icona, la copertina e quello che c'è dentro
  database e sottopagine (sono pagine a sé); le sottopagine e i database nati
  dopo la versione non si perdono: il loro collegamento resta, in fondo alla
  pagina. I collegamenti a pagine nel cestino o cancellate non tornano. Con la
  pagina bloccata le versioni si leggono ma non si rimettono. I database a
  pagina intera non hanno versioni (le loro righe non sono blocchi); le pagine
  delle righe sì. Le versioni stanno nel database dell'app, quindi anche nei
  backup
- **[Progetto]** Database alla versione 68: tabella `page_versions`
  (`PageVersionEntity`, `PageVersionDao`, migrazione 67→68), il contenuto in
  JSON compresso con la sua impronta (`VersionContent`, `VersionCodec`), le
  regole di quante tenerne (`VersionRetention`). `saveVersion` e
  `restoreVersion` in `PageRepository`; i salvataggi e il ripristino in
  `PageEditorViewModel`; `InkViewModel.reload` per i disegni rimessi;
  `PageHistorySheet` e `versionOutline` in `PageHistoryUi.kt`; le scritte in
  `HistoryStrings`. Test: `VersionContentTest` (nessun campo dei blocchi resta
  fuori, l'impronta, quante versioni si tengono), `PageVersionsTest` (una
  versione solo se cambia, il ripristino di toggle, tabelle, disegni e titolo,
  "Before restoring", le sottopagine nate dopo), `PageHistoryScreenTest` (la
  prima versione aprendo la pagina, l'anteprima e il ripristino, la pagina
  bloccata, l'anteprima delle righe), `MigrationTest` (dalla 67)

### 🆕 v0.144 — 02/11/2026 · 16:24 — "Sort" nel menu di una proprietà ordina solo per lei: A → Z, o dal più grande al più piccolo
*(Claude Code sul PC; provata sul telefono su una pagina di prova; 1006 test passati)*

- **[Modifica]** **[📱 Mobile]** **"Sort" nel menu di una proprietà** (si
  apre toccando la sua intestazione, anche quella di Name) non apre più la
  finestra di tutte le regole: si apre lì sotto, con due scelte per quella
  proprietà. Le parole **A → Z** e **Z → A**; i numeri **Largest →
  smallest** (per prima) e **Smallest → largest**. Una proprietà di testo o
  di tag in cui ogni valore comincia con una cifra ("2017", "3 km") conta come
  numeri. Le date hanno **Newest first** e **Oldest first**, le caselle
  **Checked first** e **Unchecked first**. Scelto un verso, la tabella si
  ordina e il menu si chiude; riaprendo, la spunta dice il verso di adesso, e
  **Remove sort** toglie l'ordinamento di quella proprietà
- **[Nota]** La proprietà scelta diventa la prima regola: le regole che
  c'erano restano sotto (contano solo fra le righe pari) e si vedono, tutte,
  nella finestra dell'icona "Sort" in cima al database, che resta com'era.
  Con "Lock view" la voce è grigia, come l'icona
- **[Progetto]** `SortKind` e `sortKindOfTexts` in `SortRules.kt`;
  `sortFirstBy` e `sortKindOf` nel `DatabaseViewModel` (le formule e i Rollup
  per quello che danno); `propertySortRows` e `sortChoices` in
  `PropertyMenu.kt`, al posto di `PropertyMenuActions.sort`. Test:
  `PropertySortTest` (i due versi nel menu, le parole, i numeri dal più
  grande, un testo di soli numeri, Name, le caselle e le date, le altre regole
  che restano sotto e "Remove sort"); `PropertyMenuTest` aggiornato

### 🆕 v0.143 — 02/11/2026 · 16:05 — "Sort groups": i gruppi nell'ordine che vuoi, anche Unchecked sopra Checked
*(Claude Code sul PC; provata sul telefono in "Paragraph – prova": in un database di prova raggruppato per una casella "Done", di suo Checked sopra Unchecked; nella regola "Sort groups" con Manual, Ascending e Descending e i due gruppi con le tre lineette; trascinato Unchecked sopra Checked, e nella tabella Unchecked è salito sopra; poi il database di prova nel cestino; 999 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Sort groups" in ogni regola di
  "Group"** (e di "Group by" nella bacheca): toccando la regola, sotto "Hide
  empty groups", tre scelte. **Manual**: l'elenco dei gruppi con le tre
  lineette, da trascinare nell'ordine che si vuole (per una casella, Unchecked
  sopra Checked); **Ascending** e **Descending**: per nome, i numeri come
  numeri. Vale per quella vista e quel livello: la tabella, le colonne e le
  fasce della bacheca hanno ognuna il suo
- **[Nota]** Senza averli mai trascinati, i gruppi restano nell'ordine di
  sempre (i tag come nel loro menu, prima Checked, i testi e i numeri dal più
  piccolo). Un gruppo nuovo (un tag aggiunto dopo) va in fondo a quelli
  messi a mano; quello senza valore resta sempre l'ultimo. Le date hanno già
  "Newest first" in "Date by". Cambiando la proprietà di una regola, l'ordine
  riparte da capo. Anche le frecce "pagina prima" e "pagina dopo" seguono
  l'ordine scelto
- **[Progetto]** `GroupRuleSpec` con `order` (`GroupOrder`: MANUAL,
  ASCENDING, DESCENDING) e `manual` (le chiavi dei gruppi); `orderGroups` e
  `buildLevelGroups` accanto a `buildRowGroups`; `setGroupRuleOrder` e
  `setGroupRuleManual` nel `DatabaseViewModel`; `ReorderList` (l'elenco a tre
  lineette dei tag, ora per qualunque elenco). Test: `GroupOrderTest` (la
  casella a mano e al contrario, i tag, i numeri, i nuovi in fondo, "Hide
  empty" dopo l'ordine, le frecce delle pagine), `SubGroupTest` (salvato, e da
  capo con un'altra proprietà), `GroupRulesScreenTest` (la pagina della
  regola, e Unchecked che sale sopra Checked)

### 🔧 v0.142.0.1 — 28/10/2026 · 14:38 — Scrivendo in fretta in una cella vuota non nasce più una cella doppia
*(Claude Code sul PC; provata sul telefono in "Paragraph – prova": in un database di prova "Cibo" scritto velocissimo in due celle vuote, poi la tabella raggruppata per quella proprietà: un gruppo solo, "Cibo" con 2 righe (con la v0.142 erano "Ci" e "Cib"); l'app si apre coi dati dell'utente dopo la migrazione; poi il database di prova nel cestino; 991 test passati)*

Provando la v0.142 sul telefono, nel database di prova "Cibo" scritto molto in
fretta in due celle vuote era stato salvato come "Ci" e "Cib", anche se a
schermo si leggeva "Cibo": si è visto quando i gruppi hanno ridisegnato la
tabella. Nei database dell'utente (backup di stamattina, solo conteggi: 20.725
celle) non c'era nessuna cella doppia.

- **[Bug fix]** **[📱 Mobile]** **Una cella sola per riga e proprietà**: ogni
  lettera scritta salva la cella, e due salvataggi vicini su una cella ancora
  vuota la trovavano vuota tutti e due e ne creavano una ciascuno; le lettere
  dopo finivano in una, e la tabella mostrava l'altra, rimasta indietro. Ora
  il salvataggio legge e scrive in un colpo solo, e il database non accetta
  più due celle per la stessa riga e la stessa proprietà
- **[Nota]** Le celle doppie che ci fossero se ne vanno da sole all'apertura:
  resta la prima, quella su cui finivano le modifiche
- **[Progetto]** `DatabaseRepository.setCellValue` e `setCellLinkLabel` in una
  transazione; `DatabaseCellEntity` con l'indice unico su (`rowId`,
  `columnId`); migrazione 66→67 (via le doppie, poi l'indice). Test:
  `CellWriteTest` (le lettere salvate una dopo l'altra senza aspettarsi, trenta
  salvataggi da più thread, la seconda cella che prende il posto della prima,
  la scritta del link), `MigrationTest` (dalla 66, con una cella doppia)

### 🆕 v0.142 — 28/10/2026 · 14:16 — I gruppi quanti se ne vogliono: "Add a group" dentro Group, e niente più "Sub groups"
*(Claude Code sul PC; provata sul telefono: la bacheca "Weekly" di KPOP è rimasta com'era (colonne per programma, fasce per settimana dalla più recente) e in "Group by" ha 1 Music show (Columns) e 2 Win date (Bands, con "Week" e "Newest first"), poi la vista rimessa su "Songs"; in "Paragraph – prova" un database di prova con Tipo, Preso e Bio: "Add a group" tre volte, la tabella a tre livelli ognuno più a destra, "Add a group" grigio quando le proprietà sono finite; nella bacheca le colonne per Tipo e le fasce per Preso; poi il database di prova nel cestino; 986 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **I gruppi della tabella, quanti se
  ne vogliono**: dentro "Group" c'è in cima "Add a group" e sotto le regole,
  numerate nell'ordine in cui contano: la prima fa i gruppi, la seconda
  divide ogni gruppo, la terza ogni sottogruppo, e così via. Nella tabella
  ogni livello sta un po' più a destra del suo gruppo e si chiude toccandolo;
  "New page" dentro un gruppo crea la pagina coi valori di tutti i livelli,
  e "Create sub row" li copia tutti
- **[Nuova funzionalità]** **[📱 Mobile]** **Anche la bacheca** ("Group by"):
  la prima regola fa le colonne ("Columns"), le altre le fasce ("Bands"), una
  dentro l'altra, ognuna col suo nome fermo sul bordo sinistro
- **[Modifica]** **[📱 Mobile]** **Ogni regola ha la sua pagina**, toccandola:
  la proprietà (scegliendo quella di un'altra regola, le due si scambiano),
  "Hide empty groups", per una data "Date by", e "Remove group". Il menu ⋮
  accanto sposta la regola su o giù o la toglie; in fondo "Remove all groups"
- **[Rimosso]** **[📱 Mobile]** **"Sub groups" e "Hide empty sub groups"**
  dalle impostazioni del database, nella tabella e nella bacheca: i gruppi
  dentro i gruppi sono le regole dopo la prima. Anche "Hide empty groups" non
  sta più nella prima pagina: ogni regola ha il suo
- **[Nota]** I raggruppamenti che c'erano restano com'erano: il gruppo e il
  sottogruppo di ogni vista sono diventati le sue prime due regole, coi loro
  "Hide empty" e "Date by"; una bacheca con le fasce ma senza una proprietà
  scelta per le colonne ha come prima regola la prima a selezione singola,
  quella che già si vedeva
- **[Progetto]** `data/entity/GroupRules.kt` (`GroupRuleSpec`, `GroupLevel`,
  `levelsIn`); migrazione 65→66 (`pages.tableGroupRulesJson` e
  `boardGroupRulesJson`, coi campi di gruppo e sottogruppo spostati lì e
  svuotati); nel `DatabaseViewModel` `tableGroupLevels`, `boardGroupLevels`,
  `addGroupRule`, `moveGroupRule`, `setGroupRuleColumn`, `addRowInGroups`…;
  `TableLayout` e `BoardLayout` disegnano un livello dentro l'altro;
  `ui/screen/GroupRulesUi.kt` (l'elenco, "Add a group", la pagina della
  regola); `RowNavigation` segue tutti i livelli. Test: `SubGroupTest` e
  `BoardSubGroupTest` riscritti per le regole, `GroupRulesScreenTest` (la
  pagina "Group", tre livelli nella tabella, la pagina della regola, la
  bacheca), `BoardLayoutTest` (fasce dentro le fasce), `RowOrderTest` (tre
  livelli), `MigrationTest` (dalla 65)

### 🆕 v0.141 — 28/10/2026 · 10:51 — Le formule nei database, come la proprietà Formula di Notion
*(Claude Code sul PC; provata sul telefono in "Paragraph – prova": in un database di prova la proprietà "Totale" scelta in "New property" e scritta con "Edit formula" toccando le proprietà e gli operatori (`prop("Prezzo") * prop("Quanti")` fa 6 con 1,5 × 4); toccando la cella, la finestra con l'anteprima su quella riga; l'esempio "Days left" (3 giorni al 31/10), l'errore "The formula stops halfway", un "se… allora…" (`… > 5 ? "caro" : "ok"` dà "caro"), un sì/no che nella cella è una casella spuntata; il menu dell'intestazione con "Edit formula" e senza "Filter"; "Prezzo" rinominata "Costo" e la formula riscritta da sola; a campo vuoto la tastiera resta in minuscolo; poi il database di prova nel cestino; 976 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La proprietà "Formula"** (icona
  Σ): un valore calcolato riga per riga dalle altre proprietà, che si
  aggiorna da solo quando cambiano. Si scrive come su Notion:
  `prop("Prezzo") * prop("Quantità")`, `dateBetween(prop("Scadenza"),
  today(), "days")`, `if(prop("Fatto"), "✓ Finito", "Da fare")`. Si sceglie
  fra i tipi in "Change type" o creando una proprietà; sotto il tipo c'è
  "Edit formula"
- **[Nuova funzionalità]** **[📱 Mobile]** **La finestra della formula**: si
  apre toccando una cella della formula, o da "Edit formula" nel menu
  dell'intestazione. In cima la formula; sotto, mentre si scrive, il
  risultato su quella riga, o che cosa non va ("Missing “)”", "There's no
  property “Boh”"…). Più giù, da toccare: le proprietà del database (scrivono
  `prop("…")` dove sta il cursore), gli operatori, tre esempi già con le
  proprietà del database e le funzioni, ognuna con cosa fa. Chiudendola, la
  formula si salva
- **[Nuova funzionalità]** **[📱 Mobile]** **Il linguaggio**: `+ - * / % ^`,
  `== != > < >= <=` (anche `× ÷ ≥ ≤ ≠` e un `=` solo), `and or not` (o
  `&& || !`), `condizione ? sì : no`, testi fra virgolette (anche quelle
  curve della tastiera) e una sessantina di funzioni: logica (`if`, `ifs`,
  `empty`…), numeri (`round`, `sum`, `min`, `max`…), testo (`concat`,
  `contains`, `replaceAll`, `slice`…) e date (`now`, `today`, `dateAdd`,
  `dateBetween`, `formatDate`, `year`, `day`…). Le unità delle date anche in
  italiano ("giorni", "mesi"); in `formatDate` il testo fra `[ ]` resta com'è
- **[Nuova funzionalità]** **[📱 Mobile]** **Che cosa dà**: un numero, un
  testo, una data o un sì/no, che nella cella è una casella spuntata o vuota.
  Una formula sbagliata scrive "Error" nella sua cella e non ferma le altre.
  Una formula legge anche un'altra formula, un Rollup, i nomi di una
  Relation, la data di creazione…; due formule che si leggono a vicenda lo
  dicono invece di girare per sempre
- **[Nuova funzionalità]** **[📱 Mobile]** **Ordinare, cercare, calcolare**:
  una formula si ordina per il suo numero o la sua data (non come testo: 6
  viene prima di 32), si trova con la ricerca, in fondo alla colonna ha anche
  Sum, Average, Min, Max e le date, e fa i gruppi dei grafici. Si vede anche
  nella pagina della riga, nelle schede e nella galleria. Non si filtra e non
  raggruppa la tabella, come i Rollup
- **[Nuova funzionalità]** **[📱 Mobile]** **Rinominando una proprietà**, le
  formule che la leggono si riscrivono col nome nuovo
- **[Progetto]** `data/formula/FormulaSyntax.kt` (come si legge una formula,
  e gli errori), `FormulaEngine.kt` (il calcolo, le funzioni, le proprietà
  di ogni riga, `renameProperty`); `ColumnType.FORMULA`; nel
  `DatabaseViewModel` le formule si calcolano dopo i Rollup
  (`DatabaseTableState.formulas`, `previewFormula`, `setFormula`);
  `ui/screen/FormulaUi.kt` (la cella, la finestra); `FormulaStrings` nelle
  otto lingue; `RollupFormatter.dateTime`. Test: `FormulaEngineTest` (gli
  esempi dell'utente, operatori, funzioni, date, proprietà, errori, formule
  che si leggono a vicenda, il rinominare), `FormulaViewModelTest` (col
  database vero: i risultati, l'ordine, la ricerca, il rinominare,
  l'anteprima), `FormulaScreenTest` (la cella, la finestra con l'anteprima,
  il menu dell'intestazione, "Change type")

### 🔧 v0.140.0.1 — 28/10/2026 · 09:54 — La domanda del ripristino scrive la data del backup
*(Claude Code sul PC; provata sul telefono: la domanda ora dice «replaced by the backup of 28.10.2026 09:53», poi annullata; 939 test passati)*

Provandolo sul telefono (fatto un backup, ripristinato, e il backup dopo
identico a quello ripristinato: le 13 tabelle con le loro 29.619 righe, i 55
file delle pagine e le preferenze), la domanda prima del ripristino usava la
scritta dell'elenco dei backup.

- **[Bug fix]** **[📱 Mobile]** **"Ripristinare questo backup?" dice la data
  vera del backup**: «sostituito dal backup del 28/10/2026 09:51», invece di
  «sostituito dal backup del Oggi alle 09:51», che in italiano non si legge.
  L'elenco dei backup resta con "Oggi alle…" e "Ieri alle…"
- **[Progetto]** In `BackupScreen.kt` la domanda prende `Formats.dateTime`.
  Test: `BackupScreenTest` (+1 controllo: la data nella domanda)

### 🆕 v0.140 — 28/10/2026 · 08:31 — Il backup ogni giorno, "Carica su Proton Drive" e il ripristino
*(Claude Code sul PC; provata sul telefono: "Fai un backup adesso" fa in 6 secondi un backup di 27 MB in Download/notE backup, con dentro il database sano (versione 65), i 55 file delle pagine e le preferenze; "Carica su Proton Drive" lo ha copiato in Proton Drive/Documents/notE backup, identico byte per byte all'originale, e la volta dopo "Salva come" si apre già lì; il ripristino provato solo coi test; 939 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Backup" nella barra laterale**
  (prima era grigio): in cima l'ultimo backup e l'ultimo caricamento su
  Proton Drive, "Carica su Proton Drive" e "Fai un backup adesso"; sotto il
  backup di ogni giorno, il promemoria, i backup sul telefono e "Ripristina
  da un file…"
- **[Nuova funzionalità]** **[📱 Mobile]** **Il backup ogni giorno**: una
  volta al giorno, uscendo dall'app, se oggi non ce n'è ancora uno. È un file
  zip con tutto notE: il database (copiato tutto in un istante, anche mentre
  l'app scrive), le immagini e i file delle pagine, le impostazioni, i
  widget, gli strumenti della penna e la password delle pagine bloccate. Sta
  in **Download/notE backup**, dove resta anche se notE viene disinstallata;
  se ne tengono gli ultimi 7. Si spegne dalla pagina Backup
- **[Nuova funzionalità]** **[📱 Mobile]** **"Carica su Proton Drive"**: apre
  "Salva come" col nome del backup già scritto; la prima volta si sceglie la
  cartella di Proton Drive, dalla seconda la finestra si apre già lì e basta
  toccare "Salva". Senza backup, prima se ne fa uno. Ogni backup
  dell'elenco ha anche lui "Carica su Proton Drive"
- **[Nuova funzionalità]** **[📱 Mobile]** **Il promemoria**: passati 7
  giorni dall'ultimo caricamento (si sceglie 3, 7, 14, 30 o mai), uscendo
  dall'app arriva una notifica, al massimo una al giorno, finché non se ne
  carica uno. Toccata, apre la pagina Backup
- **[Nuova funzionalità]** **[📱 Mobile]** **Il ripristino**, da un backup
  dell'elenco o da un file (anche scaricato da Proton Drive): dopo la
  domanda, prima si fa un backup di com'è adesso (per tornare indietro), poi
  notE si riavvia e alla riapertura, prima di leggere qualsiasi cosa, mette
  al loro posto database, file e impostazioni del backup. Un file che non è
  un backup di notE, o uno di una notE più nuova, non tocca niente e lo dice
- **[Progetto]** `data/backup/BackupArchive.kt` (com'è fatto lo zip, e
  nessun file dello zip può uscire dalla sua cartella), `BackupStorage.kt`
  (Download/notE backup con `MediaStore`, una cartella per i test e i
  telefoni prima di Android 10), `Backups.kt` (il backup con `VACUUM INTO`,
  i 7 tenuti, il caricamento, il promemoria, `prepareRestore` e
  `applyPendingRestore` chiamato per primo in `MainActivity.onCreate`);
  `ui/screen/BackupScreen.kt`; `BackupStrings` nelle otto lingue;
  `Notifier.postBackupReminder`; `AppDatabase.closeInstance`. Test:
  `BackupTest` (dentro c'è tutto, uno al giorno, i 7 tenuti, il ripristino
  completo, i file sbagliati, il caricamento, il promemoria, con la SQLite
  vera), `BackupScreenTest` (la pagina, "Salva come" col nome e poi dove si
  è salvato, il ripristino dal menu)

### 🆕 v0.139 — 27/10/2026 · 23:31 — I bottoni, come su Notion: un tocco e partono le azioni scelte
*(Claude Code sul PC; provata sul telefono in "Paragraph – prova": "/button" apre la finestra, "Show confirmation" e "Insert blocks" con una casella, "Cancel" non fa niente e "Continue" mette la casella sotto il bottone, "Edit button" ritrova le azioni; in un database di prova la proprietà Button con "Edit pages in" → "This page" → "Toggle" spunta e toglie la spunta alla sua riga; poi tutto tolto, il database di prova nel cestino; 924 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Il blocco "Button"**, dai menu
  "/" e "+" (o "Turn into", e il testo della riga diventa il nome): una
  pastiglia grigia con l'icona e il nome, come su Notion. Al primo tocco,
  quando non sa ancora cosa fare, apre la finestra delle azioni; dopo, ogni
  tocco le fa tutte, in fila. Tenendolo premuto c'è il menu del blocco, con
  "Edit button" per cambiarle, "Link in another page", "Move to",
  "Duplicate" (una copia con le sue azioni, che non dipende dall'altra) e
  "Delete"
- **[Nuova funzionalità]** **[📱 Mobile]** **La finestra delle azioni**: in
  cima il nome e l'icona (emoji, icone o un'immagine, dallo stesso selettore
  delle pagine), sotto "When tapped" con le azioni, ognuna nel suo riquadro,
  e "New action" per aggiungerne. Le azioni, come su Notion:
  - **"Insert blocks"**: dei blocchi già scritti (testo, elenchi, caselle,
    toggle, citazione, riquadro, divisore), sopra o sotto il bottone, o in
    cima o in fondo alla pagina. Sotto il bottone, i blocchi di un tocco
    nuovo vanno prima di quelli del tocco di prima;
  - **"Add page to"**: una riga nuova in un database, col nome e i valori
    delle proprietà, e se si vuole la si apre alla fine;
  - **"Edit pages in"**: cambia le proprietà di "This page" (la riga del
    bottone, o quella di cui è la pagina che contiene il bottone), oppure
    delle righe di un database, tutte o solo quelle in cui una proprietà ha
    un certo valore. Si può spuntare, togliere la spunta o invertire una
    casella, aumentare o diminuire un numero, mettere la data di adesso o di
    oggi, aggiungere o togliere un tag, scrivere un testo o il nome, svuotare;
  - **"Show confirmation"**: una domanda; "Cancel" ferma le azioni dopo di lei;
  - **"Open page or URL"**: alla fine si apre una pagina dell'app o un link;
  - **"Send notification"**: una notifica sul telefono, subito.
  Con "…" un'azione si sposta su o giù o si toglie. Come su Notion un'azione
  a metà non si salva: "Done" dice cosa manca (il database, la pagina, il
  valore). Le pagine e i link si aprono solo alla fine, così uscire dalla
  pagina non lascia a metà le azioni dopo
- **[Nuova funzionalità]** **[📱 Mobile]** **La proprietà "Button" dei
  database** (fra i tipi, anche in "Change type"): un bottone su ogni riga,
  con le stesse azioni per tutte, che lavorano su "This page", cioè la riga
  del bottone toccato. Le azioni si scelgono sotto il tipo ("Edit actions"),
  dal menu dell'intestazione, o toccando un bottone che non ne ha ancora. Sul
  bottone c'è il nome della proprietà, o la scritta scelta nella finestra.
  Un bottone non ha un valore: niente filtro, ordinamento, calcolo e grafico
  su di lui, e "Fit to content" misura la sua scritta
- **[Nuova funzionalità]** **[📱 Mobile]** **I collegamenti ai bottoni**: dal
  menu di un bottone, "Link in another page" mette una copia collegata in
  fondo alla pagina scelta; dal menu "/", "Link to button" fa della riga la
  copia collegata di un bottone scelto da un elenco di tutti. La copia ha la
  freccetta, mostra nome e icona del bottone vero e fa le sue azioni (i
  blocchi vanno accanto a lei), e cambiandole da lei cambiano per tutte.
  "Go to original" porta al bottone vero; se lui viene cancellato, la copia
  si legge "Deleted button" e non fa niente
- **[Nuova funzionalità]** **[📱 Mobile]** Dopo un tocco un avviso dice quello
  che non si vede: le righe aggiunte o cambiate in un altro database, e
  quello che non si è potuto fare (il database cancellato o protetto da
  password, una proprietà sparita, la pagina bloccata, le notifiche spente),
  mentre le altre azioni vanno avanti. Un filtro su una proprietà cancellata
  non cambia niente, invece di cambiare tutte le righe
- **[Nota]** Rispetto a Notion mancano le azioni che parlano con fuori o con
  altre persone (Gmail, webhook, Slack, le notifiche agli altri) e le
  variabili con le formule: notE vive sul telefono. "Annulla" toglie i
  blocchi messi nella pagina aperta; le righe dei database cambiate da un
  bottone si rimettono a mano
- **[Progetto]** `data/buttons/Buttons.kt` (`ButtonConfig`, `ButtonStep`,
  `PropertyEdit.applyTo`, `ButtonFilter.matches`), `ButtonRunner.kt` (il
  motore, `ButtonHost`, `ButtonProblem`); `BlockType.BUTTON` con
  `BlockAttrs.button` e `buttonLink`; `ColumnType.BUTTON` con le azioni
  nell'`optionsJson`; `PageRepository.insertBlocksAt`, `addLinkedButton`,
  `buttonChoices`; `ui/screen/ButtonEditor.kt` (la finestra), `ButtonUi.kt`
  (`ButtonRunController`, `ButtonPill`, `ButtonBlockContent`,
  `ButtonEditHost`, `ButtonCell`, `LinkToButtonSheet`); `ButtonStrings`
  nelle otto lingue; `Notifier.postButton`. Nessuna migrazione: il database
  resta alla versione 65. Test: `ButtonModelTest` (i conti di ogni tipo, il
  filtro, il salvataggio), `ButtonRunnerTest` (ogni azione sul database vero,
  la conferma, l'ordine, i problemi), `ButtonBlockTest` (menu "/", "Turn
  into", Annulla, le copie collegate), `ButtonScreenTest` (l'editor vero:
  tocco, domanda, finestra, menu del blocco, copia collegata, la proprietà
  dei database), `ButtonsRenderTest` (l'immagine dei bottoni e della
  finestra)

### 🆕 v0.138 — 27/10/2026 · 20:04 — Il widget "Countdown", coi foglietti che si sfogliano
*(Claude Code sul PC; provata sul telefono aggiungendo il widget dalla barra laterale: Natale col conto giusto, i secondi che si sfogliano (visti in una registrazione dello schermo), il tocco lungo che apre la finestra, "Add countdown" che ne mette uno sotto e "Delete"; poi il widget di prova è stato tolto; 865 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Countdown" fra i widget** (in
  fondo alla lista di "Add widget"). Nasce col prossimo Natale a mezzanotte,
  come nella foto. Sotto il titolo, sei foglietti chiari con le cifre scure:
  mesi, settimane, giorni, ore, minuti e secondi che mancano, coi nomi in
  azzurro sotto. I mesi sono quelli del calendario (dal 31 gennaio "un mese"
  arriva al 28 febbraio) e possono essere più di dodici, come su Indify; quello
  che resta va in settimane, giorni, ore, minuti e secondi. Una data già
  passata conta il tempo trascorso da allora
- **[Nuova funzionalità]** **[📱 Mobile]** **Ogni numero che cambia si
  sfoglia** come i fogli di un calendario: la metà di sopra col numero vecchio
  si piega in avanti attorno alla cerniera e scende, e dall'altra parte arriva
  la metà di sotto del numero nuovo, in mezzo secondo. I secondi si sfogliano
  ogni secondo, le altre unità quando cambiano. Come gli altri widget, si
  muovono solo con la barra laterale aperta
- **[Nuova funzionalità]** **[📱 Mobile]** **Tenendo premuto un countdown si
  cambia**: il nome e, uno per campo, anno, mese, giorno, ora, minuto e
  secondo, sia nel futuro sia nel passato. Una data che non esiste (il 31
  aprile) non si salva, e lo dice. Dalla stessa finestra il countdown si può
  anche togliere ("Delete")
- **[Nuova funzionalità]** **[📱 Mobile]** **"Add countdown" nei tre puntini**
  del widget: un countdown in più, che va sotto gli altri, uno sotto l'altro e
  non a scorrimento orizzontale, così si vedono tutti. Quello nuovo parte da
  domani a mezzanotte, da cambiare nella stessa finestra
- **[Progetto]** `Countdown.kt` (`CountdownWidget` col nome `"countdown"` nei
  widget salvati, `CountdownEntry`, `countdownParts`, `christmasCountdown`);
  `WidgetStore.add(kind, firstCountdown)`, `addCountdown`, `updateCountdown`,
  `removeCountdown`, `WidgetKind.COUNTDOWN`; `CountdownWidgetUi.kt`
  (`CountdownView`, `CountdownBlock`, `FlipTile` con le due metà ferme e quella
  che gira, `CountdownEditorDialog`); le scritte in `WidgetStrings`, nelle otto
  lingue. Test: `CountdownTest` (i conti, Natale, il salvataggio),
  `CountdownUiTest` (il tocco lungo che cambia l'anno, "Add countdown" che lo
  mette sotto, "Delete"; il foglietto che a metà corsa mostra il numero vecchio
  e il nuovo, alla fine solo il nuovo); in `WidgetsRenderTest` due countdown
  nell'immagine dei widget

### 🔧 v0.137.0.1 — 27/10/2026 · 19:42 — In "Change type" le righe dei tipi non si spostano più scegliendone uno
*(Claude Code sul PC; provata coi test, che senza la correzione falliscono; sul telefono solo installata; 857 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Scegliendo un tipo, le righe sotto restano
  ferme.** Sotto il tipo scelto c'è il riquadro delle sue impostazioni (il
  formato dei numeri, la durata, la Relation, il Rollup); per i tipi che non
  ne hanno (Text, Date, Select…) restava vuoto ma col suo margine di 8 punti
  sotto. Così la riga scelta si allungava un poco e quelle sotto scendevano,
  per risalire scegliendone un'altra. Ora il riquadro c'è solo per i tipi con
  le impostazioni, e la spunta del tipo scelto è grande come la sua icona, così
  la riga non si alza nemmeno per lei. Vale anche nella finestra della
  proprietà dalle impostazioni del database
- **[Progetto]** `TYPES_WITH_SETTINGS` in `DatabaseViewScreen.kt`. Test:
  `PropertyMenuTest` (+1: scelto "Date", fra la sua riga e quella sotto la
  stessa distanza che fra le altre)

### 🆕 v0.137 — 27/10/2026 · 19:32 — Il menu di una proprietà si apre col tocco, a gruppi come su Notion
*(Claude Code sul PC; provata sul telefono aprendo il menu di una proprietà del Main calendar e chiudendolo senza toccare niente; 856 test passati)*

- **[Modifica]** **[📱 Mobile]** **Toccando l'intestazione di una proprietà
  si apre il suo menu; tenerla premuta non fa più niente.** Il menu non è più
  la tendina di prima ma una finestra che sale dal basso, a gruppi di voci con
  la freccetta su quelle che aprono un'altra finestra, come quella di Notion.
  Le voci sono quelle di prima, nello stesso ordine: "Move to left" e "Move to
  right"; "Activate all" e "Deactivate all" (solo le caselle); "Align left",
  "Center" e "Align right"; "Resize column" e "Fit to content"; "Hide", "Hide
  icon" e "Calculate". Lo stesso per la proprietà principale (Name): "Rename",
  gli spostamenti, "Calculate", "Hide icon" e la larghezza
- **[Nuova funzionalità]** **[📱 Mobile]** Nel menu, dopo gli spostamenti:
  **"Change type"**, coi tipi che prima stavano nella finestra della proprietà
  (sotto quello di adesso, le sue impostazioni: il formato dei numeri, la
  durata, la Relation, il Rollup); **"Filter"**, la finestra del filtro delle
  icone in cima, che parte già dai valori di questa proprietà; **"Sort"**, la
  finestra dell'ordinamento delle icone in cima. Sono le stesse finestre, quindi
  il filtro e le regole di ordinamento sono gli stessi da tutte e due le parti.
  Con "Lock view" sono grigie, come le icone. Per Name c'è "Sort" (il filtro in
  cima non lavora sul nome)
- **[Rimosso]** **[📱 Mobile]** **"Edit property" non c'è più nel menu.** Il
  nome si scrive direttamente nella riga in cima al menu, accanto all'icona,
  come su Notion: chiudendo il menu la proprietà lo prende. Le impostazioni del
  tipo stanno in "Change type"
- **[Nuova funzionalità]** **[📱 Mobile]** **"Delete property" in fondo al
  menu**, in rosso come su Notion (prima stava dentro "Edit property"). Prima
  di cancellare chiede, perché se ne vanno anche i valori di tutte le righe.
  Con il database bloccato è grigio, e il nome non si cambia
- **[Modifica]** **[📱 Mobile]** Dalle impostazioni del database (Properties),
  dove un'intestazione da toccare non c'è (la bacheca, il calendario, la
  galleria), toccando una proprietà si apre come prima la sua finestra con nome
  e tipi
- **[Progetto]** `PropertyMenu.kt` (`PropertyMenuSheet` con la riga del nome,
  `PropertyMenuGroup`, `PropertyMenuChevronRow`, `PropertyMenuActions` in
  `LocalPropertyMenuActions`); in `DatabaseViewScreen.kt` `PropertyHeaderCell` e
  `NameHeaderCell` con `combinedClickable` (il tocco lungo non fa niente),
  `columnWidthRows`, `PropertyMode` (`FULL`, `TYPE`), `FilterSheet(startColumnId)`;
  `DatabaseViewModel.renameColumn`; `DbStrings.deletePropertyTitle`,
  `deletePropertyNote`. Test: `PropertyMenuTest` (l'ordine delle voci, il tocco
  lungo, "Change type", il nome nella riga in cima, "Delete property" con la
  domanda, "Filter" e "Sort"), `HideIconTest` col tocco

### 🆕 v0.136 — 27/10/2026 · 19:10 — "Back" per tornare in cima quando si è scesi molto in una pagina
*(Claude Code sul PC; provata sul telefono sulla pagina principale, scorrendo dal margine sinistro: scendendo compare, salendo se ne va, toccato riporta in cima; 850 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Back" in basso al centro quando
  si è scesi molto**, in ogni pagina: lo stesso pulsantino dell'indice, con la
  freccia in su. Compare scendendo col dito oltre una schermata e mezza dalla
  cima; toccandolo la pagina torna in cima e il pulsante se ne va. **Salendo a
  mano non compare**, e se c'era se ne va: conta l'ultimo verso in cui il dito
  ha fatto scorrere la pagina, non gli spostamenti che fa l'app da sé (il
  cursore portato in vista, il salto di un collegamento). Non c'è mentre si
  scrive (tastiera aperta, coprirebbe il testo), con tutta la pagina
  selezionata, mentre si disegna, e quando c'è già il "Back" di un
  collegamento dell'indice
- **[Progetto]** `BackToTop.kt` (`BackToTop` col verso del dito da una
  `NestedScrollConnection` sulla lista della pagina e la distanza dalla cima
  stimata come la barretta di scorrimento, `ItemSizes`; `BackToTopButton`);
  `BackPill` in `PageAnchors.kt`, la pillola comune ai due "Back". Test:
  `BackToTopTest` (giù compare, su se ne va, giù torna, toccato si va in cima)

### 🔧 v0.135.0.1 — 27/10/2026 · 18:58 — "---" fa il divisore senza lasciare i trattini né righe doppie
*(Claude Code sul PC; provata coi test sul PC; sul telefono la prova la fa l'utente, che stava usando la pagina di prova; 849 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Scrivendo "---" la riga diventa un divisore,
  e il campo di testo lo mostra subito come sta davvero.** Nel database era
  già tutto giusto: la riga diventava il divisore, senza trattini, e le righe
  sotto andavano sotto di lui. Il campo in cui si scriveva invece restava
  com'era prima, coi trattini, col numero dell'elenco e con le righe che ormai
  stavano sotto il divisore — che così si vedevano due volte, sopra nel campo
  vecchio e sotto il divisore. Il campo aspettava indietro dal database il
  testo senza i trattini; sul telefono il database salta quel passaggio e
  arriva già diviso, e il campo, convinto di avere ancora una modifica in
  viaggio, non si aggiornava più. Ora, fatto il divisore, il campo prende
  quello che c'è nel database, come dopo Annulla. Una riga vuota in più sopra
  il divisore (la prima volta, in fondo alla pagina) veniva da lì: il campo
  rimasto indietro la riscriveva
- **[Progetto]** `PageEditorViewModel.convertToDividerAndFocusNext` fa scattare
  `externalChangeTick` dopo il cambio. Test: `DividerShortcutUiTest` (Invio e
  "---" in fondo alla pagina, e "---" in mezzo a un elenco numerato: nel
  database e nei campi ogni riga una volta sola, senza trattini). Sul PC il
  guasto non si ripete (il database lì risponde in tempo): la prova vera è
  sul telefono

### 🆕 v0.135 — 27/10/2026 · 17:55 — "Hide icon": l'icona di una proprietà si può nascondere
*(Claude Code sul PC; provata sul telefono solo aprendo il menu dell'intestazione nel Main calendar, senza toccare la voce: nascondere e rimostrare l'icona è provato dai test sul PC; 847 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Hide icon" tenendo premuta
  l'intestazione di una proprietà**, sotto "Hide", con accanto l'icona stessa:
  l'icona del tipo (il calendario di una data, la catena di un link…) se ne va
  e il nome parte dal bordo della colonna. Tenendo premuto di nuovo la voce è
  diventata **"Show icon"**, e l'icona torna. Vale anche per il **"Aa" della
  proprietà principale** (Name). Di suo ogni icona si vede: le proprietà che
  c'erano restano come prima, e si sceglie una per una, database per database
- **[Modifica]** **[📱 Mobile]** Una proprietà con l'icona nascosta non la
  mostra nemmeno fra le proprietà di una riga (in cima alla sua pagina e nella
  finestra della riga): lì resta il suo posto vuoto, così i nomi stanno in
  colonna. Con "Fit to content" la colonna non conta più lo spazio
  dell'icona. Nei menu per scegliere una proprietà (filtri, ordinamento,
  impostazioni) le icone restano, per riconoscere il tipo
- **[Progetto]** `DatabaseColumnEntity.hideIcon` e `PageEntity.hideNameIcon`
  (database v65, migrazione 64→65: tutte le icone si vedono);
  `DatabaseViewModel.setPropertyIconHidden`, `PageDao.setHideNameIcon`;
  `LocalPropertyIcons` (`PropertyIcons`) per le intestazioni;
  `nameFitWidth(iconHidden)`. Test: `HideIconTest` (in un database dentro una
  pagina: "Hide icon", "Show icon" e il "Aa"), `MigrationTest` (+1, dalla v64
  con `schema/v64.sql`)

### 🔧 v0.134.0.2 — 27/10/2026 · 14:45 — "Fit to content" misura la scritta di una cella URL, non il link intero
*(Claude Code sul PC; provata sul telefono guardando la vista tabella del Main calendar, senza toccare niente: la colonna URL ora è larga quanto "Meeting Teams"; 845 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Con "Fit to content", una colonna URL è
  larga quanto quello che mostra.** Nella vista tabella del Main calendar la
  cella URL mostrava "Meeting Teams" ("Show as…", v0.133), ma la colonna si
  misurava sul link di Teams intero, 70 lettere. Guasto della v0.133: la
  misura di "Fit to content" non sapeva della scritta. Ora misura la scritta
  quando c'è, il link quando non c'è
- **[Progetto]** `cellFitWidth(linkLabel)`, che `fitWidths` prende da
  `DatabaseTableState.linkLabels`. Test: `PageLinkTest` (+1)

### 🔧 v0.134.0.1 — 27/10/2026 · 14:35 — Il + e il − del Counter sono istantanei
*(Claude Code sul PC; misurata sul telefono su una release provvisoria, tre volte + e − sul contatore dell'utente, che è tornato a 0; 844 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Il numero del Counter cambia appena si
  tocca + o −.** Aveva ragione l'utente: il contatore è nato il 24/09 senza
  suoni, e i suoni sono arrivati con la v0.179 (18/10). Misurato sul
  telefono: a ogni tocco far partire il click con `AudioTrack` (stop, ricarica
  e play) teneva fermo l'app 65-74 ms, e solo dopo cambiava il numero, circa
  80 ms dopo il tocco. Ora prima cambia il numero e poi parte il suono, da un
  `SoundPool` (il lettore di Android per i suoni brevi) che non fa aspettare
  nessuno: misurato di nuovo, il numero cambia 2-9 ms dopo il tocco e il
  tocco stesso dura meno di 1 ms. I suoni sono gli stessi di prima, col loro
  volume
- **[Bug fix]** **[📱 Mobile]** Lo stato dei widget si scrive in memoria da un
  filo a parte, uno dopo l'altro: un tocco su un widget non aspetta più il
  JSON di tutti i widget (3-10 ms a tocco)
- **[Progetto]** `CounterSounds`: `prepare` (un WAV per suono nella cache,
  caricato nel `SoundPool`; lo chiama `CounterView`), `play` che non aspetta
  (finché il `SoundPool` non è pronto, il modo di prima su un filo a parte),
  `wav`; `WidgetStore.update` salva con `saver`. Test: `CounterSoundsTest`
  (+1, il file WAV)

### 🆕 v0.134 — 27/10/2026 · 14:22 — "Full or page line": ogni pagina sceglie se le righe lunghe restano intere o vanno a capo
*(Claude Code sul PC; provata sul telefono in "Paragraph – prova", con "Full line view" spento in Settings: una riga lunga scritta in fondo, "Full or page line" nei tre puntini con "Default (Page line)", poi "Full line" (la riga non va più a capo) e "Page line" (di nuovo a capo); la pagina rimessa su "Default" e la riga tolta, confrontata col database (migrazione 63→64 compresa); 843 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Full or page line" nei tre
  puntini di ogni pagina**, sotto "Paragraph", con scritto accanto com'è
  adesso. Si apre e si sceglie fra tre opzioni: **"Default"** fa come
  "Full line view" in Settings, e tra parentesi dice cosa vuol dire adesso
  ("Default (Full line)" o "Default (Page line)"); **"Full line"** lascia le
  righe lunghe intere, da scorrere di lato; **"Page line"** le manda a capo,
  così tutta la riga sta nello schermo. La scelta vale subito, anche a
  pagina aperta
- **[Modifica]** **[📱 Mobile]** Settings resta com'era e vale per tutte le
  pagine lasciate su "Default": tutte quelle che c'erano, e quelle nuove.
  Cambiandolo, quelle pagine lo seguono subito
- **[Progetto]** `PageEntity.fullLineMode` (`FULL_LINE_DEFAULT`,
  `FULL_LINE_ON`, `FULL_LINE_OFF`; database v64, migrazione 63→64: tutte le
  pagine su "Default"); `FullLineChoice.kt` (`fullLinesOf`,
  `fullLineSummary`, `PageFullLineSheet`), che `PageEditorContent` usa al
  posto di `AppSettings.fullLineView`; `PageEditorViewModel.setFullLineMode`,
  `PageDao.setFullLineMode` (non cambia la data della pagina, come il
  correttore). Test: `FullLineChoiceTest` (una riga lunga nella pagina vera:
  a capo o intera secondo la pagina e Settings), `MigrationTest` (+1, dalla
  v63 con `schema/v63.sql`)

### ✏️ v0.133.1 — 27/10/2026 · 14:05 — La spunta della riga principale scende a tutte le sue sotto-righe
*(Claude Code sul PC; provata coi test, sul telefono solo installata: le caselle di "Food" sono dell'utente e non le ho toccate; 838 test passati)*

- **[Modifica]** **[📱 Mobile]** **Spuntando la riga principale si spuntano
  tutte le sue sotto-righe**, e quelle sotto di loro, fino in fondo;
  togliendole la spunta si toglie a tutte. Insieme a quello che c'era già, la
  riga principale resta spuntata solo quando lo sono tutte le sue sotto-righe.
  Spuntando una sotto-riga che ha sotto-righe sue, la spunta scende a loro e
  poi risale come prima. Le righe che erano già com'è giusto non cambiano, e
  non risultano modificate adesso ("Last edited time")
- **[Progetto]** `DatabaseRepository.setCheckbox`: prima di risalire, la
  casella scende a tutte le sotto-righe (con `getSubRows`, nella stessa
  transazione). Test: `SubRowCheckboxTest` (+2)

### 🆕 v0.133 — 27/10/2026 · 13:44 — Cosa si vede di un link: l'indirizzo, il titolo della pagina o una scritta tua
*(Claude Code sul PC; provata sul telefono in "Paragraph – prova": un sito scritto in fondo a una riga, l'icona del link col solo cursore, "Page title" ("Example Domain", letto dal sito), "Your text", di nuovo "Full link", e una parola tenuta premuta che prende tutto il link; la riga poi rimessa com'era, confrontata col database (migrazione 62→63 compresa). "Show as…" delle celle URL provato solo nei test sul PC: sul telefono non c'è un database di prova; 836 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Show" nella finestra del link
  del testo** (l'icona del link nella barra sopra la tastiera): sotto
  l'indirizzo, tre scelte per quello che si legge nella pagina. **"Full
  link"** è l'indirizzo per intero; **"Page title"** è il titolo della pagina
  web, letto dal sito quando lo si sceglie (serve la rete: senza, la finestra
  scrive "No title found"); **"Your text"** è una scritta scritta a mano. Il
  testo del link cambia e tiene la sua formattazione (grassetto, colore…), il
  link resta lo stesso e il cursore va in fondo al link. Si cambia di nuovo
  dalla stessa finestra, o con Annulla
- **[Nuova funzionalità]** **[📱 Mobile]** **Un sito scritto come testo
  semplice** ("https://…" o "www.…", senza link): col solo cursore dentro
  compare l'icona del link, e la finestra parte con quell'indirizzo già
  scritto. Con "Done" diventa un link, e "Show" vale anche qui. Le parentesi,
  le virgolette e il punto attaccati intorno non fanno parte dell'indirizzo
- **[Modifica]** **[📱 Mobile]** Tenendo premuto su un link si seleziona una
  parola sola: l'icona del link ora prende **tutto il link**. Cambiarlo,
  toglierlo o cambiarne la scritta vale per tutto il link, non più solo per
  quella parola
- **[Nuova funzionalità]** **[📱 Mobile]** **"Show as…" nelle celle URL dei
  database** (tocco lungo sulla cella, con "Edit link", "Copy link" e "Paste
  link"): le stesse tre scelte. La cella mostra il titolo o la scritta al
  posto dell'indirizzo, e così fanno le schede della bacheca, della galleria e
  dell'elenco e le proprietà in cima alla pagina di una riga. Il tocco apre
  sempre il link, e la lente trova anche la scritta. Se il link della cella
  cambia e lei mostrava il titolo, il titolo si rilegge per il link nuovo
  (senza rete torna l'indirizzo per intero); una scritta tua resta. Svuotata
  la cella, la scritta se ne va
- **[Progetto]** `DatabaseCellEntity.linkLabel` e `linkLabelIsTitle`
  (database v63; con la migrazione 62→63 le celle di prima mostrano il link
  come sempre); `LinkTitles` (il titolo dell'anteprima dei segnalibri,
  `LinkPreviews`); in `TextLinks.kt` `LinkShowChoices`, `LinkShowState`,
  `LinkShowDialog` e `LinkDialog(shown, onSave(url, testo))`; `bareLinkAt` in
  `RichText.kt`. In `PageEditorViewModel`, `linkSelection` prende il link
  intero e il sito scritto come testo, e ci sono `capturedLinkText`,
  `replaceLinkText` e `linkTextChanges`: i campi prendono subito il testo
  nuovo, col cursore in fondo al link. In `DatabaseViewModel`,
  `setCellLinkLabel`, `rereadLinkTitle` e `DatabaseTableState.linkLabels`, che
  arrivano alle celle con `LocalCellLinkLabels`. Test: `BareLinkTest`,
  `LinkShowTest`, `CellLinkLabelTest`, `LinkShowDialogTest`, e `MigrationTest`
  (+1, dalla v62 con `schema/v62.sql`)

### 🆕 v0.132 — 27/10/2026 · 09:42 — "Default" per il corpo, e sotto un titolo grande si scrive a 16
*(Claude Code sul PC; provata sul telefono in "Paragraph – prova": "Default" nella finestra, un titolo a 50, Invio e una riga scritta sotto, che dopo la pausa torna alla sua altezza col cursore dove si scriveva; la pagina poi rimessa com'era, confrontata col database; 813 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Default" nella finestra del
  corpo** (barra Aa → il numero), accanto alla casella: rimette il corpo
  normale, 16, al testo selezionato e chiude la finestra. In italiano
  "Predefinito", nelle altre lingue dell'app la parola loro
- **[Modifica]** **[📱 Mobile]** **Sotto un titolo grande si scrive a 16, e
  lo si vede subito.** Nel database le righe nuove nascevano già senza
  corpo, cioè a 16; quello che sembrava 50 era il campo di testo: la riga
  nuova stava nello stesso campo del titolo, e un campo ha un'altezza di
  riga sola, quella che serve al carattere più grande. Le righe scritte sotto
  restavano così alte come il titolo, **col cursore alto come il titolo**,
  finché la tastiera era aperta (i campi non si ridividono mentre il cursore è
  nella pagina). Ora, appena si smette di scrivere per due secondi, le righe
  che appaiono diverse da quella col cursore escono dal suo campo: il titolo
  va in un campo suo e le righe nuove tornano alla loro altezza. Il campo col
  cursore resta lui, quindi la tastiera non si chiude e il cursore resta dove
  si scriveva. Vale anche al contrario (scrivendo nel titolo, le righe normali
  escono) e per le caselle da spuntare fra righe normali
- **[Bug fix]** **[📱 Mobile]** Trovato provandolo sul telefono: uscito il
  titolo, nello stesso giro il campo col cursore si univa anche alle righe di
  sotto, e cambiando sopra e sotto insieme non sapeva dove rimettere il
  cursore, che finiva qualche riga più giù (le lettere scritte dopo finivano in
  "1. Primo numero"). Ora il campo col cursore cambia da un lato solo per
  volta, e il resto al giro dopo; vale anche per le unioni dei pezzi
  sullo schermo, che potevano fare lo stesso
- **[Modifica]** **[📱 Mobile]** Scrivendo subito dopo l'Invio, prima che la
  riga nuova fosse nel database, per un istante la si vedeva col corpo del
  titolo: ora anche quella previsione la mostra a 16. Il resto della
  formattazione (grassetto, colore…) continua come prima, e un Invio in mezzo
  a un titolo lascia alla metà che scende il suo corpo
- **[Progetto]** `EditorStrings.defaultFontSize`, `FontSizeDialog`;
  `insertedSpans` in `spliceSpans` (il testo dopo il primo a-capo di un pezzo
  inserito non eredita `size`); in `PageEditorViewModel.keepVisibleTogether` il
  campo col cursore lascia uscire le righe di aspetto diverso, un lato per
  volta, tenendo la sua chiave (per esteso con `runGroupKey` se era quella del
  gruppo; `rechunk` poi la rifà), e `MergedTextRunField` rimette il cursore
  quando delle righe escono. Test: `NewLineFontSizeTest` (+5),
  `NewLineSizeUiTest` (Invio e lettere nel campo vero: nel database a 16, poi
  il titolo in un campo suo e il cursore in fondo ad "Altro"),
  `LineLookChunksTest` (+1: la chiave del campo col cursore resta, e dopo
  `rechunk` le chiavi sono ancora tutte diverse)

### 🔧 v0.131.0.2 — 27/10/2026 · 09:17 — Lo spazio fra le righe non cambia più da solo
*(Claude Code sul PC; provata sul telefono in Fast notes: righe a 44 punti in cima, sotto Anki field codes e più giù, e la pagina fermata in quattro punti fotografata subito e dopo 3 secondi, identica a parte la barretta di scorrimento; 806 test passati)*

Le righe di testo di fila stanno in campi, pezzi da 50 righe. Scorrendo, i
pezzi che si vedono si uniscono in un campo solo e poi si ridividono (v0.113.1
e successive). Tre cose di un campo dipendevano da quali righe ci sono
dentro, e quindi cambiavano quando le righe passavano da un campo all'altro:

- **[Bug fix]** **[📱 Mobile]** **L'altezza delle righe.** Un campo ha
  un'altezza di riga sola: quella che serve al carattere più grande che
  contiene, più larga se c'è una casella da spuntare. In "Fast notes" i
  titoli col corpo a 20 ("Anki field codes", "Creepy things", "Emergency")
  allungavano così tutte le righe del loro pezzo: sotto "Anki field codes"
  distavano 54 punti invece di 44. Ora una riga che appare diversa da quella
  sopra (corpo più grande, o casella fra righe normali, o il contrario)
  comincia un campo suo, e campi diversi non si uniscono
- **[Bug fix]** **[📱 Mobile]** **Dove sta il testo dentro le righe.**
  Compose lo decide guardando **solo la prima riga** del campo: se lì c'è un
  pallino (scritto a 20, più grande del testo), il testo di tutto il campo
  scende di 11 pixel. Misurato: la riga di base a 60 pixel dalla cima della
  riga nel pezzo che cominciava con "◦ BACK", a 49 in quello sopra; unendoli
  il testo risaliva. Ora un pezzo comincia solo con una riga che ha lo stesso
  segno della prima del pezzo sopra (se non arriva, al doppio della misura lo
  si taglia lo stesso), e due pezzi che cominciano diversi non si uniscono
- **[Bug fix]** **[📱 Mobile]** **Lo spazio ai capi del campo.** Quando un
  segno della prima o dell'ultima riga sporge (il pallino, l'accento di "É"),
  Compose allunga il testo di quel tanto, ma solo ai due capi: fra due pezzi
  c'erano 5 pixel in più che sparivano unendoli. Ora fra due pezzi di fila
  quell'aggiunta si toglie, e si toccano come le righe di un campo solo.
  Vale in tutte le pagine
- **[Nota]** Mentre si scrive i campi non si ridividono (v0.113.1): una riga
  nuova scritta subito sotto un titolo grande ne prende lo spazio finché non
  si smette di scrivere. E la selezione col dito che arriva in fondo a un
  campo si porta dietro quello dopo (dal 19/10/2026): in quel momento lo
  spazio può ancora cambiare. Nei due punti in cui due pezzi ora non si
  uniscono, la selezione che li attraversa li unisce così
- **[Progetto]** `PageEditorViewModel.lineLookOf` (casella, corpo più grande)
  e `markerSizeOf` (il corpo del pallino, anche nascosto): `rechunk` comincia
  un pezzo dove cambia il primo (anche fra righe sullo schermo) e taglia i 50
  solo dove il secondo è quello della prima riga del pezzo;
  `keepVisibleTogether` non unisce pezzi che non vanno. `TextEdges` e
  `trimTextEdges` in `MergedTextRunField`, con l'altezza di riga del campo in
  un posto solo (`lineHeight`). Trovato con un registro provvisorio sul
  telefono (altezza delle righe e riga di base di ogni campo), poi tolto.
  Test: `LineLookChunksTest` (il titolo e le caselle in campi loro; la pagina
  che scorre a finestre di 20 righe non cambia mai l'altezza di riga di una
  riga normale; in un elenco puntato con righe normali ogni pezzo comincia
  come il primo, anche scorrendo; le righe uguali si uniscono ancora), che
  senza le correzioni falliscono

### 🔧 v0.131.0.1 — 27/10/2026 · 08:12 — La tastiera nella home non lagga più
*(Claude Code sul PC; misurata sul telefono nella home, prima e dopo, aprendo la tastiera nel titolo, in una cella e in una riga vuota; 802 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Aprendo e chiudendo la tastiera i database
  della pagina non si rifanno più.** Quando la tastiera si apre o si chiude
  (e quando il cursore entra o esce) la pagina si aggiorna un paio di volte,
  ed è giusto; ma con lei si rifacevano da capo tutti i database che ha
  dentro. Nella home sono dieci, tenuti pronti anche fuori dallo schermo,
  con la tabella di "Habit tracker 2026" e le sue migliaia di caselle: fino
  a 60–117 ms per fotogramma, su 8. Misurato sul telefono aprendo e chiudendo
  la tastiera cinque volte (release, `dumpsys gfxinfo`), fotogrammi in
  ritardo e i più lenti (99%):
  - nel titolo della home: **13,9% → 7,2%**, 69 → 18–20 ms
  - in una cella di "Habit tracker 2026": **15% → 6,4%**, 73–85 → 16 ms
  - in una riga vuota in fondo, con la barra sopra la tastiera: **18% →
    8–10%**, 97–117 → 19–20 ms
  Quelli che restano lenti sono uno o due all'inizio e alla fine: compare o
  sparisce la goccia del cursore (una finestrella a sé) e la barra
- **[Progetto]** Tre cose, trovate con una traccia di sistema e i nomi delle
  funzioni di Compose accesi solo per la prova (`Composer.setTracer`):
  `openBlockMenu` rifatto a ogni giro della pagina, che le isole leggono
  (`PageItemEnv.onOpenBlockMenu`) e che quindi le faceva ridisegnare tutte
  (ora `remember`); il contenuto della `LazyColumn` della pagina rifatto a
  ogni giro, che faceva ripartire tutte le voci tenute pronte (ora
  `listContent`, rifatto solo se cambia quello che mostra); e l'altezza
  della tastiera letta per la barra mentre la si costruiva, che a barra
  aperta rifaceva la pagina a ogni fotogramma della corsa (ora la si legge
  in `offset { }`). Test: `PageFocusRecomposeTest` (il cursore che entra e
  esce dal titolo non fa ripartire il database della pagina: senza la
  correzione ripartiva 24 volte)
- **[Progetto]** Perché non riaccada, un secondo test, `theKeyboardMovingLeavesThePageAlone`, manda
  alla pagina le altezze della tastiera una dopo l'altra, come il telefono a
  ogni fotogramma, su e giù: la barra sale con lei, e dell'app si rifanno
  solo il cursore che si spegne a tastiera chiusa e la barretta di
  scorrimento. Qualunque altra cosa che un giorno leggesse la tastiera
  mentre si costruisce lo schermo lo fa fallire; con la lettura di prima
  nella barra la pagina si rifaceva 18 volte. Nei test, finché nessuno gliela
  manda, per Compose la tastiera è "visibile": il test prima la chiude

### 🆕 v0.131 — 26/10/2026 · 23:43 — "Hide “New”" nella bacheca
*(Claude Code sul PC; provata sul telefono su "KPOP Music Shows", dove è rimasta accesa; 800 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Hide “New”"** nelle
  impostazioni della bacheca (le tre lineette), sotto "Hide empty groups":
  acceso, sotto le schede di ogni colonna non c'è più "+ New", né nelle
  colonne né nelle fasce, con o senza "Color columns". Nelle fasce una
  colonna senza schede non lascia una striscia colorata vuota, solo il suo
  posto. Vale per la vista in cui lo si accende, come le altre impostazioni
  della bacheca; di suo è spento
- **[Progetto]** `PageEntity.boardHideNew` (migrazione 61→62, spenta per
  tutte le viste); in `BoardColumnCards` `onAdd` null vuol dire niente
  "New", e nelle fasce la cella vuota resta senza colore. Test:
  `BoardLayoutTest` (+2), `MigrationTest.from61EveryBoardStillShowsNew`
  (schema `v61.sql`)

### 🔧 v0.130.0.1 — 26/10/2026 · 23:35 — Le righe non si duplicano più quando la pagina ridivide i suoi pezzi
*(Claude Code sul PC; sul telefono controllata "Fast notes" dopo aver tolto la seconda copia, la correzione provata coi test; 797 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Righe doppie, tutte come punti elenco,
  sotto la cinquantesima riga di una pagina.** Le righe sullo schermo stanno
  in un campo solo e, quando non si scrive, la pagina le ridivide in pezzi
  da 50: se in quel momento il campo riceveva un tocco o una modifica (come
  il cambio di corpo di "Anki field codes"), le sue righe oltre la
  cinquantesima sembravano appena scritte e diventavano righe nuove col tipo
  della cinquantesima. In "Fast notes" la cinquantesima era "English
  meaning", un punto elenco di secondo livello: 34 righe della sezione Anki
  erano ricomparse lì sotto, tutte punti elenco, con le originali più giù. Al
  contrario, un campo più corto del suo pezzo poteva far sparire le righe che
  non aveva. Ora la modifica va alle righe che il campo ha davvero
- **[Nota]** Dalle copie del database fatte sul PC: alle 20:01 del 20/10
  la sezione Anki era in ordine, la notte dopo era già doppia (con le copie
  delle 13:55 del 21/10 uguali). È successo quindi il 20/10 dopo le 20:01, non
  il 21/10. La prima copia, quella sistemata dall'utente il 21/10, resta; la
  seconda (60 righe, da `<h2 class="english">{{English meaning}}</h1>` al
  secondo "Simplified radical", prima di "Creepy things") è stata tolta.
  Fatto il 26/10 alle 23:33 sul telefono: "Fast notes" da 323 a 263 righe,
  controllo del database ok, la copia di prima tenuta sul PC
- **[Progetto]** `PageEditorViewModel.runOfField`: se le righe che il campo
  manda a `updateRun` ci sono ancora, una dopo l'altra e con lo stesso testo
  di prima, il gruppo è il loro e non quello che `resolveRun` ricostruisce coi
  confini dei pezzi di adesso. Test: `ChunkSelectionTest` (+2: il campo
  rimasto con 100 righe e quello rimasto con 50), che senza la correzione
  falliscono tutti e due

### 🆕 v0.130 — 26/10/2026 · 17:44 — Più notifiche per una data, come su Google Calendar
*(Claude Code sul PC; provata sul telefono aprendo la finestra della data e chiudendola senza salvare; 795 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Più notifiche per una data**,
  come su Google Calendar: con "Include notification" acceso, sotto c'è una
  riga per ogni notifica, con la X per toglierla, e "Add notification" per
  aggiungerne un'altra. Con un tocco: 10 minuti, 30 minuti, un'ora, un
  giorno o una settimana prima (e all'inizio, se la si era tolta); da
  "Custom…" un numero qualsiasi di minuti, ore, giorni o settimane prima,
  fino a un anno: "10 ore prima", "3 giorni prima", "2 settimane prima".
  C'è sia nella finestra della proprietà Date sia in "More options" degli
  eventi del calendario. Tolta l'ultima, la notifica si spegne
- **[Nuova funzionalità]** **[📱 Mobile]** La notifica in anticipo dice
  quanto manca: "Concerti · Inizia tra 3 giorni". Quella all'inizio resta
  com'era, e suona anche alla fine se la data ce l'ha; senza di lei, alla
  fine non suona niente. Una data che si ripete suona prima di ogni volta.
  "Un giorno prima" è alla stessa ora del giorno prima anche quando in
  mezzo cambia l'ora legale
- **[Progetto]** `DateRange.reminders`: i minuti prima dell'inizio. Nel
  valore della cella stanno fra la ripetizione e la "n", che resta l'ultimo
  campo (`start|end|t|regola|0,600,4320|n`): la sveglia le trova con la
  stessa ricerca di prima, e le date salvate prima (solo la "n") valgono
  "all'inizio". `dateReminderTimes` dà `DateReminderTime` (quando, se è la
  fine, quanti minuti prima) e conta le ripetizioni anche dopo la finestra,
  per quelle in anticipo. Le parole in `ReminderTexts` (8 lingue),
  l'elenco in `ReminderList` (`DateRemindersUi.kt`). Test:
  `DateRemindersTest` (10), `ReminderListTest` (3), `NotificationsTest`
  aggiornato

### 🔧 v0.129.0.1 — 26/10/2026 · 17:29 — La freccia dei collegamenti dentro la pagina non copre più l'ultima lettera
*(Claude Code sul PC; provata sul telefono su "Fast notes"; 782 test passati)*

- **[Bug fix]** **[📱 Mobile]** **La freccia dei collegamenti a una riga
  della pagina** (l'indice navigabile, dalla v0.118) **copriva l'ultima
  lettera**, e quella lettera stava staccata dalle altre con uno spazio
  davanti: "Emergenc y", "Creepy thing s", "Anki field code s" nel sommario
  di "Fast notes". Ora l'ultima lettera resta attaccata alle altre e la
  freccia viene subito dopo; se dopo il collegamento la riga continua, è la
  parola dopo a spostarsi un po' per farle posto
- **[Progetto]** Per far posto alla freccia si allargava l'ultima lettera
  (`letterSpacing`), ma Android divide lo spazio in più fra prima e dopo la
  lettera: metà le finiva davanti, e a fine riga (sul telefono, Android 17)
  la metà dietro non c'era, così la freccia le cadeva sopra. Ora
  `markBlockLinkEnd` (in `PageAnchors.kt`) allarga il carattere dopo il
  collegamento, di solito lo spazio, e niente se il collegamento chiude la
  riga; `drawBlockLinkArrows` disegna la freccia dopo la lettera
  (`blockLinkArrowSpan`). Test: `BlockLinkArrowTest` (3), che misura dove
  cadono le lettere

### 🆕 v0.129 — 26/10/2026 · 17:07 — Le zone della classifica come su Sofascore: Champions, Europa, Conference e retrocessione
*(Claude Code sul PC; provata sul telefono sulla classifica della Serie A; 779 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Le zone nella classifica dei
  campionati**, come su Sofascore: sopra la prima squadra di ogni zona il
  suo nome in piccolo ("Champions League", "Europa League", "Conference
  League Qualification", "Relegation"), e sul margine sinistro una
  parentesi colorata dal nome all'ultima squadra della zona: verde la
  Champions, blu l'Europa League, azzurro la Conference, rosso la
  retrocessione. Ci sono anche le altre zone che ESPN segna: verde chiaro i
  preliminari di Champions e i playoff per la promozione (Serie B),
  arancione lo spareggio per non retrocedere (Bundesliga, Ligue 1); nelle
  coppe europee, le fasi a eliminazione diretta col colore di ESPN. Vale
  anche per le stagioni passate
- **[Nota]** Le zone sono quelle di ESPN, squadra per squadra, e possono
  essere diverse da quelle di Sofascore: oggi in Serie A ESPN dà l'Europa
  League alla 5ª e alla 6ª e la Conference alla 7ª, Sofascore l'Europa alla
  5ª e la Conference alla 6ª. Una squadra che va in Europa con la coppa
  nazionale ha la sua zona anche a metà classifica (in Liga il Villarreal,
  10°, con l'Europa League). NBA, NFL e MLB: ESPN non dà zone e la
  classifica resta com'era. Una classifica già scaricata prende le zone
  quando si aggiorna (al massimo mezz'ora, o subito con la freccia)
- **[Progetto]** `StandingRow` ha `zone` e `zoneColor` (la "note" di ESPN;
  il colore "##B5E7CE", che ESPN a volte scrive così, si ripulisce);
  `standingZoneOf` e `standingZoneLabel` in `Sports.kt`; la parentesi è
  `Modifier.zoneBracket` in `SidebarSports.kt`, e ogni riga della classifica
  ha a sinistra 12 dp per lei. Test: `SportsTest.standingZonesFromTheNotesOfEspn`,
  `SportsWidgetTest.standingsWithTheZonesLikeSofascore`

### ✏️ v0.128.2 — 26/10/2026 · 16:49 — La riga principale è spuntata solo quando lo sono tutte le sue sotto-righe
*(Claude Code sul PC; provata coi test, sul telefono solo installata: le caselle di "Food" non le ho toccate; 777 test passati)*

- **[Modifica]** **[📱 Mobile]** **Togliendo la spunta a una sotto-riga si
  toglie anche alla riga principale**, e su fino in cima. Una riga
  principale spuntata a mano con sotto qualcosa ancora da fare (come
  "Rosticceria" in "Food") si sistema appena si tocca una delle sue
  sotto-righe: resta spuntata solo se lo sono tutte
- **[Progetto]** `DatabaseRepository.setCheckbox`: a ogni casella di una
  sotto-riga, la riga principale prende "tutte spuntate sì/no", su finché
  non c'è niente da cambiare. Test: `SubRowCheckboxTest` (6), che ora fa
  andare avanti anche l'orologio dell'app (le righe restano ferme
  `CHECKBOX_SETTLE_MS` dopo una spunta)

### ✏️ v0.128.1 — 26/10/2026 · 16:43 — Le sotto-righe: la riga principale si spunta da sola, e si ordinano a blocchi
*(Claude Code sul PC; provata sul telefono su "Food", guardando l'ordine senza toccare le caselle; la spunta che risale provata coi test; 775 test passati)*

- **[Modifica]** **[📱 Mobile]** **Spuntate tutte le sotto-righe, si spunta
  da sola la riga principale**, nella stessa colonna Checkbox; e se anche
  lei è una sotto-riga, risale fino in cima. Togliendo la spunta a una
  sotto-riga, la riga principale resta com'è
- **[Modifica]** **[📱 Mobile]** **Una riga con le sue sotto-righe si
  ordina come un blocco**: sta al posto del primo dei suoi elementi (lei o
  una sotto-riga), e dentro il blocco le sotto-righe seguono le stesse
  regole. In "Food", ordinata con le non spuntate in cima, "Rosticceria"
  era spuntata e scendeva fra le spuntate portandosi dietro "Patate al
  forno", "Bocconcini di pollo" e "Fettine di pollo panate", ancora da
  prendere: ora il blocco sta in alto con le non spuntate, e quando le tre
  sono prese va giù
- **[Progetto]** `DatabaseRepository.setCheckbox` (in una transazione, con
  `DatabaseDao.getSubRows`), `sortedAsBlocks` in `SubRows.kt`, usata da
  `DatabaseViewModel.sortRows`. Test: `SubRowCheckboxTest`

### 🔧 v0.128.0.1 — 26/10/2026 · 16:30 — L'app non si chiude più con una bacheca raggruppata per data giorno per giorno
*(Claude Code sul PC; provata sul telefono sulla vista "Weekly" di "KPOP Music Shows", che la faceva chiudere, col database confrontato prima e dopo; 771 test passati)*

- **[Bug fix]** **[📱 Mobile]** **L'app si chiudeva aprendo "KPOP"** (21 e
  24/10, memoria esaurita): la vista "Weekly" di "KPOP Music Shows" aveva
  "Group by" su "Win date", che senza "Date by" raggruppa giorno per giorno,
  e la bacheca disegnava insieme una colonna per ogni giorno con una
  vittoria, più di 4.000. Ora **le colonne della bacheca si vedono 12 alla
  volta**, con "**Load more**" dopo l'ultima (sulla vista "Weekly": "Load
  more 4256"); la colonna senza valore resta sempre, perché lì nascono le
  schede nuove. Lo stesso per **i gruppi e i sottogruppi di una tabella
  dentro una pagina**, che si disegnavano tutti insieme (a schermo intero
  si disegnano già solo quelli sullo schermo). Non era un errore
  dell'utente: la bacheca deve reggere qualunque raggruppamento
- **[Nota]** Per installare questa versione l'app sul telefono è stata
  **disinstallata e reinstallata coi dati rimessi dentro**: la chiave con
  cui il PC firma l'app (`.android\debug.keystore`, usata anche per la
  release) non c'era più, la cartella era stata svuotata fra il 21 e il
  26/10, e il telefono rifiuta un aggiornamento firmato con un'altra
  chiave. Prima: copia di tutti i dati dell'app sul PC (132 file, controllati
  uno per uno con l'MD5, database integro), due copie dell'archivio; poi
  disinstallazione, installazione con la chiave nuova, dati rimessi (di
  nuovo 132 file uguali, stessi conteggi e stessa impronta di tutti i
  blocchi). La password delle pagine bloccate è nelle impostazioni dell'app,
  ed è tornata con loro
- **[Progetto]** `DATABASE_GROUP_LIMIT` = 12 (`RowLimits.kt`). Test:
  `BoardLayoutTest`, una bacheca con 30 giorni: 12 colonne, "Load more 18" e
  quella senza data

### 🆕 v0.128 — 26/10/2026 · 16:14 — Il tipo dentro "Edit property", impostazioni più corte e "Hide empty groups" nella bacheca
*(Claude Code sul PC; "Edit property" e le impostazioni provate sul telefono sul Pokédex; "Hide empty groups" provato coi test, sul telefono con la v0.128.0.1; 770 test passati)*

- **[Modifica]** **[📱 Mobile]** **Il tipo si cambia dentro "Edit
  property"**: toccando una proprietà si vede tutto, il nome in alto e
  sotto tutti i tipi; sotto il tipo scelto le sue impostazioni (Number:
  formato, cifre, "Show as"; Duration; Relation; Rollup). Il tipo che la
  proprietà ha adesso sta in cima alla lista, con le sue impostazioni, gli
  altri dopo nell'ordine di sempre. "Change type", nel menu che si apre
  tenendo premuta l'intestazione, non c'è più
- **[Rimosso]** **[📱 Mobile]** Dalle impostazioni del database (l'icona
  con le tre lineette e i pallini) via **Favorite**, **Turn into page** e
  **Delete**: stanno nei sei puntini e, a schermo intero, nei tre puntini
  in alto. **Icon** resta solo a schermo intero, dove i sei puntini non ci
  sono e altrimenti un database senza icona non avrebbe modo di averne una
- **[Nuova funzionalità]** **[📱 Mobile]** **"Hide empty groups" anche
  nella bacheca**, sotto "Group by": le colonne senza nemmeno una scheda non
  si vedono, compresa quella senza valore ("No Music show"). Di suo spento,
  in ogni bacheca, nuova o di prima: le colonne si vedono tutte, com'era. Se
  una scheda nasce senza valore, la sua colonna torna
- **[Nota]** Sulla domanda 3: nei sei puntini di un database "**Turn
  into**" è la voce che sulle righe di testo le trasforma in un altro tipo
  di blocco (titolo, elenco, casella…); per una pagina o un database non è
  mai stata costruita, e c'è grigia come nei menu "/" e "+". "**Color**"
  colora il nome di una pagina nel suo collegamento; un database dentro la
  pagina non ha quel nome da colorare. Su un database tutte e due non fanno
  niente. In "KPOP Music Shows" la colonna "No Music show" non era vuota: ci
  sta la riga senza nome che c'era prima delle vittorie, e "Hide empty
  groups" la lascia finché c'è
- **[Progetto]** `PageEntity.boardHideEmptyGroups` (migrazione 60→61,
  schema di prova `v60.sql`), copiato anche togliendo la vista di partenza.
  Test: `BoardLayoutTest` (di suo tutte le colonne; con l'interruttore solo
  quelle con schede), `MigrationTest` dalla 60

### 🆕 v0.127 — 21/10/2026 · 13:35 — "Data source": una vista con le righe di un altro database, e i tre database K-pop in uno
*(Claude Code sul PC; provata sul telefono su "KPOP Music Shows", col database confrontato prima e dopo; 767 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Data source" nella finestra
  "New view"**, come le sorgenti dei database di Notion: di suo la vista
  mostra le righe del suo database; toccando la voce si sceglie un altro
  database dall'elenco (col nome della pagina in cui sta), e la vista, che
  resta in questo, mostra le righe, le proprietà, i Rollup e le Relation di
  quello. Senza un nome suo prende quello dell'altro database. Una riga
  creata da lì nasce in quel database
- **[Modifica]** **[📱 Mobile]** La vista di partenza si toglie solo se ne
  resta un'altra con le sue stesse righe, che prende il suo posto: una vista
  che mostra un altro database non può diventare il database
- **[Bug fix]** **[📱 Mobile]** Togliendo la vista di partenza, il database
  prendeva le impostazioni della vista dopo ma non "Date by" (settimane,
  mesi o anni, dalla v0.126)
- **[Nota]** **"KPOP Music Shows" ora è un database solo con tre viste**:
  "**Board**" (le vittorie, colonne per programma e fasce per settimana; le
  colonne colorate, come le aveva messe l'utente), "**KPOP Songs**" e
  "**KPOP Groups**" (Table, dal più premiato). I due collegamenti in fondo
  alla pagina "KPOP" non ci sono più; "KPOP Songs" e "KPOP Groups" restano
  come database, sono le sorgenti delle due viste. La tabella delle vittorie
  che era la vista di partenza se n'è andata: la bacheca ha preso il suo
  posto. Confrontato il database del telefono prima e dopo: righe e celle
  uguali, fuori dalla pagina "KPOP" niente di cambiato
- **[Progetto]** `PageRepository.createView(…, sourceId)`;
  `deleteStartingView` cerca la prima vista con la stessa sorgente; ogni
  vista mette le sue Relation e i suoi "Page link" (`LocalRelated`,
  `LocalLinkedPages`) invece di quelli dell'ospite; `databaseViews` dice
  se la vista di partenza si può togliere (`deletable`). Solo debug,
  `KpopMergeActivity`, una volta sola. Test: `ViewSourceTest`,
  `KpopImportTest` (l'unione, sulla copia con gli stessi id)

### ✏️ v0.126.2 — 21/10/2026 · 13:15 — Le vittorie K-pop dal 2006 in "KPOP Music Shows", e i ritocchi visti sul telefono
*(Claude Code sul PC; provata sul telefono su "KPOP Music Shows", "KPOP Groups" e "KPOP Songs", col database confrontato prima e dopo; 763 test passati)*

- **[Nota]** **Le vittorie sono nel database dell'utente**: 4.268, dall'8
  gennaio 2006 al 20 ottobre 2026, dal database che Soridata offre da
  scaricare ("Database Download": `dbdownload.zip`, 6,3 MB, con dentro
  `mainbackup_2026-10-21.sql`, tabelle `app_kpop_ms` e `app_kpop_group`),
  messe con `KpopImportActivity` (v0.126.1). In "KPOP Music Shows" ogni
  vittoria ha la canzone come nome, il gruppo fra i tag (i suoi restano
  suoi: "aespa"), il programma, la data e la Relation "Song"; nella pagina
  "KPOP", sotto la bacheca, "**KPOP Songs**" (1.288 canzoni: gruppo,
  vittorie, prima e ultima, programmi) e "**KPOP Groups**" (374 gruppi:
  canzoni, "Winning songs", "Number of wins"), dal più premiato: BTS 179,
  Twice 123, EXO 119, IU 100. La bacheca ha le colonne per programma e le
  fasce per settimana, dalla più recente. "Show KPOP (Monday)" resta vuota:
  Soridata non ha il lunedì. Confrontato il database del telefono prima e
  dopo: fuori dalla pagina "KPOP" non è cambiato niente
- **[Nota]** Sui dati: 29 vittorie cadono in un giorno diverso dal solito
  del loro programma (quasi tutte Show Champion del 2012, che allora andava
  di martedì) e restano come le dà Soridata; una, Inkigayo del 24/08/2025
  (ALLDAY PROJECT), non ha il nome della canzone. Prima del database
  scaricato provavo a leggere le pagine anno per anno: i tentativi ripetuti
  dal PC hanno fatto bloccare dal sito il collegamento dell'utente, che l'ha
  sbloccato col captcha. Da allora nessuna richiesta automatica a Soridata
- **[Modifica]** **[📱 Mobile]** **Le settimane di "Date by" vanno sempre
  dal lunedì alla domenica**, come su Soridata e nelle classifiche K-pop.
  Seguivano il telefono, e su quello dell'utente, in inglese americano,
  cominciavano di domenica: l'Inkigayo della domenica finiva nella
  settimana dopo. Il calendario segue ancora il telefono
- **[Modifica]** **[📱 Mobile]** **La cella di una Relation su una riga
  sola**: le righe collegate che ci stanno intere, poi "**+N**" per le
  altre ("I need U +17"). Prima l'ultima restava tagliata a metà, una
  freccia e due puntini. Un nome che da solo non ci sta si accorcia coi tre
  puntini
- **[Modifica]** **[📱 Mobile]** In "Related to", "**Show on “…”**" e
  "Name there" stanno **subito sotto il database scelto**: in fondo
  all'elenco di tutti i database finivano fuori dallo schermo
- **[Modifica]** **[📱 Mobile]** Nella finestra di un Rollup, le icone delle
  proprietà sono grigie come altrove (quella di una Relation era blu)
- **[Progetto]** `RelatedRowsLine` misura i nomi col misuratore del testo
  e disegna solo quelli che si vedono: misurarli componendoli li lasciava
  nell'albero dell'accessibilità anche nascosti. Test: `RelationCellTest`;
  `DateGroupingTest` col telefono in inglese americano

### ✏️ v0.126.1 — 21/10/2026 · 09:17 — Le fasce della bacheca: le prime 12 e "Load more"
*(Claude Code sul PC; provata sul telefono il 21/10/2026 sulla bacheca di "KPOP Music Shows": "Load more 1059" in fondo alle fasce; l'importazione fatta sul telefono, vedi v0.126.2; 760 test passati)*

Dalla stessa richiesta della v0.125 e della v0.126 (le vittorie K-pop di
Soridata in una bacheca a fasce settimanali).

- **[Modifica]** **[📱 Mobile]** **Le fasce della bacheca** (i
  sottogruppi) si vedono **12 alla volta**, e "**Load more**", in fondo a
  sinistra, ne aggiunge altre 12, come già le schede di ogni colonna (50
  alla volta). Ogni fascia ha tutte le colonne, e con le settimane di
  vent'anni di music show sarebbero state mille fasce da disegnare
  insieme: la bacheca ci avrebbe messo secondi ad aprirsi. Con "Newest
  first" le prime 12 sono le ultime dodici settimane, quasi tre mesi
- **[Progetto]** `RowLimits.take` e `limit` hanno `first` (quante subito),
  `BOARD_BAND_LIMIT` = 12. Test: `DatabaseRowLimitsTest`, mille settimane
- **[Progetto]** Solo nella build di debug, **`KpopImportActivity`**: legge
  le vittorie preparate sul PC dalle pagine di soridata.com (un file JSON:
  data della puntata, programma, artista, canzone) e le mette nel
  database "KPOP Music Shows" dell'utente, col tag del gruppo (quelli che
  l'utente ha già restano suoi: "aespa", non "Aespa"), il programma fra le
  sue opzioni e la data; crea "**KPOP Songs**" e "**KPOP Groups**" in fondo
  alla pagina "KPOP", collegati nei due sensi (vittoria ↔ canzone ↔
  gruppo), coi Rollup "Number of wins", "First win", "Last win", "Music
  shows", "Winning songs", ordinati dal più premiato; la bacheca prende le
  fasce per settimana dalla più recente. Una volta sola. Test:
  `KpopImportTest`, su una copia del database dell'utente con gli stessi id
- **[Progetto]** `ManySortRulesTest` cadeva a volte nella suite intera:
  dopo un salvataggio il ViewModel rilegge la pagina, e col PC carico una
  rilettura partita prima arrivava dopo, rimettendo per un attimo le regole
  di prima proprio mentre il test aggiungeva quella dopo. Ora, dopo ogni
  passo, aspetta che la pagina resti ferma

### 🆕 v0.126 — 21/10/2026 · 09:06 — "Date by": i gruppi per data diventano settimane, mesi o anni, anche dai più recenti
*(Claude Code sul PC; provata sul telefono il 21/10/2026 sulla bacheca di "KPOP Music Shows": colonne per programma, fasce per settimana dalla più recente; le settimane cominciavano di domenica, corretto nella v0.126.2; 757 test passati)*

Su Soridata ogni riga è una settimana e ogni colonna un programma. In notE
è una bacheca con le colonne per "Music show" e **le fasce per settimana**:
per le fasce serviva raggruppare le date per settimana, e questa versione lo
fa (i dati arrivano nella prossima).

- **[Nuova funzionalità]** **[📱 Mobile]** **"Date by"**, come su Notion:
  raggruppando per una data — "Group" della tabella, "Group by" della
  bacheca, "Sub groups" di tutte e due — sotto l'elenco delle proprietà si
  sceglie se i gruppi sono **giorni** (com'era), **settimane**, **mesi** o
  **anni**. Una settimana si chiama col primo e l'ultimo giorno
  ("12/10/2026 – 18/10/2026") e comincia dove la fa cominciare il calendario
  (il lunedì, su un telefono in italiano); un mese col suo nome ("Ottobre
  2026"); un anno col numero. Ogni raggruppamento della vista ha il suo
- **[Nuova funzionalità]** **[📱 Mobile]** **"Newest first"**, sotto: i
  gruppi dal più recente al più vecchio, come le settimane di Soridata, con
  quella di adesso in cima. Le righe senza data restano in fondo
- **[Nuova funzionalità]** **[📱 Mobile]** Una riga creata dentro una
  settimana, un mese o un anno prende il primo giorno del gruppo, e ci resta
- **[Progetto]** La scelta sta in `PageEntity.dateGroupingJson`, un JSON
  coi quattro raggruppamenti della vista (null = tutto per giorno, dal più
  vecchio): migrazione 59→60, schema di prova `v59.sql`. File:
  `DateGrouping.kt` (`DateUnit`, `DateGroupBy`, `DateGroupings`, inizio e
  fine di un gruppo), `DateGroupingUi.kt`, `DateGroupStrings.kt` (otto
  lingue); `buildRowGroups` riceve `dateBy`. Test: `DateGroupingTest` (le
  vittorie di quattro giorni in tre settimane, due mesi e due anni) e
  `MigrationTest` dalla 59

### 🆕 v0.125 — 21/10/2026 · 08:53 — "Relation" e "Rollup": le righe di un database collegate a quelle di un altro, e i calcoli su quelle collegate
*(Claude Code sul PC; provata sul telefono il 21/10/2026 su "KPOP Groups" e "KPOP Songs", guardando senza cambiare niente: celle, Rollup, finestre "Edit property", scelta delle righe; una proprietà nuova provata fino alla scelta del database e annullata; ritocchi nella v0.126.2; 750 test passati)*

Questa versione è la prima metà: **Relation** e **Rollup**. Il database
delle vittorie, coi dati di Soridata, viene nelle prossime.

- **[Nuova funzionalità]** **[📱 Mobile]** Fra i tipi di proprietà c'è
  "**Relation**" (icona ↗): collega le righe di questo database a quelle di
  un altro, o dello stesso. Scegliendola nella finestra della proprietà,
  subito sotto compare "**Related to**", l'elenco dei database col nome
  della pagina in cui stanno; senza averne scelto uno non si aggiunge. Nella
  cella ci sono i nomi delle righe collegate, sottolineati con la freccia
  come in "Page link": toccandone uno si apre la sua pagina (in un database
  semplice, dove le righe non hanno pagina, si apre la scelta). Toccando il
  resto della cella si sceglie: in cima le righe già collegate, sotto le
  altre del database, con la ricerca; un tocco collega o scollega, e la
  cella cambia subito. Una Relation già fatta tiene il suo database: le sue
  celle sono righe di quello
- **[Nuova funzionalità]** **[📱 Mobile]** **"Show on “…”": la Relation
  nei due sensi**, come su Notion. Accesa, nell'altro database nasce una
  Relation gemella, col nome scritto sotto ("Name there"; di suo il nome di
  questo database), e le due restano allineate: collegando la canzone "I
  AM" a "IVE", nella riga di IVE compare I AM, e viceversa. Si accende e si
  spegne anche dopo, da "Edit property": accesa, la gemella nasce già con
  i collegamenti che ci sono; spenta, se ne va (le celle di qua restano);
  cambiando il nome, la gemella si rinomina. Eliminando la Relation se ne
  va anche la gemella
- **[Nuova funzionalità]** **[📱 Mobile]** Fra i tipi di proprietà c'è
  "**Rollup**" (icona della lente): legge le righe collegate da una
  Relation di questo database, una loro proprietà (o il nome) e ci fa un
  calcolo. Scegliendolo, sotto si scelgono la Relation, la proprietà e il
  calcolo: **Show original**, **Show unique values**, i conteggi (Count
  all, values, unique values, empty, not empty) e le percentuali; per i
  numeri Sum, Average, Median, Min, Max e Range; per le date Earliest date,
  Latest date e i giorni fra la prima e l'ultima; per le caselle Checked,
  Unchecked e le loro percentuali. La proprietà letta può essere anche lei
  una Relation: da una vittoria si legge **il gruppo della canzone**, due
  database più in là. Non si scrive: si aggiorna da sé appena cambia
  qualcosa nelle righe collegate, anche nell'altro database. Senza una
  Relation nel database, la finestra dice di aggiungerne prima una
- **[Nuova funzionalità]** **[📱 Mobile]** Dove si vedono: nella tabella,
  nelle schede di bacheca, galleria ed elenco, nella scheda della riga e in
  cima alla sua pagina. **Si ordinano** (la Relation per i nomi collegati,
  il Rollup per il suo numero o la sua data), **la lente li cerca** (i nomi
  delle righe collegate, il testo del Rollup), "**Calculate**" in fondo
  alla colonna conta ogni riga collegata come un valore, "Fit to content"
  misura i nomi, e **un grafico si può raggruppare per una Relation**: una
  barra per ogni riga collegata, col suo nome. Non ancora nel filtro e nel
  raggruppamento della bacheca (vedi "Limiti noti")
- **[Nuova funzionalità]** **[📱 Mobile]** **Le copie.** Duplicando la
  pagina di una riga, la copia ha gli stessi collegamenti e, nei due sensi,
  anche le righe di là la hanno fra le loro. Copiando un database, le
  Relation con sé stesso collegano le righe della copia fra loro; quelle
  verso un altro database restano collegate a quello, in un senso solo (la
  gemella di là è dell'originale). Anche i Rollup della copia leggono le
  Relation della copia
- **[Progetto]** Nessuna colonna nuova nel database dell'app (resta la
  versione 59): la cella di una Relation tiene gli id delle righe
  collegate, separati come i tag, e la configurazione di Relation e Rollup
  sta nelle opzioni della colonna, come quelle dei tag. I Rollup si
  calcolano nel `DatabaseViewModel`, prima di filtrare e ordinare, dai
  database collegati seguiti nel tempo (`related`, a uno e a due passi).
  File: `Relations.kt` (i conti, le due metà, le copie), `DatabaseRepository`
  (`setRelation`, `addRelationColumn`, `updateRelationColumn`,
  `deleteColumnWithTwin`, in una transazione ciascuno), `RelationCell.kt`,
  `RelationSettings.kt`, `RelationStrings.kt` (otto lingue). Test:
  `RelationsTest` (i conti, coi gruppi, le canzoni e le vittorie
  dell'utente) e `RelationTest` (col database: le due metà allineate, la
  gemella accesa, rinominata e spenta, il Rollup a due passi che segue i
  cambiamenti, ordinamento, ricerca, riga duplicata, database copiato)

### ✏️ v0.124.1 — 21/10/2026 · 01:14 — La Cronologia riletta e rinumerata: aggiunte, modifiche e correzioni, e la piattaforma di ogni voce
*(Claude Code sul PC; Cronologia riletta versione per versione; provata sul telefono in Settings → Patch Notes: "v0.124.1" sotto la voce, l'elenco con ✏️ v0.124.1 e 🆕 v0.124, le etichette delle piattaforme nella pagina della versione; 729 test passati)*

- **[Modifica]** **[📱 Mobile]** **Tutte le 246 versioni rinumerate**,
  rileggendole una per una dalla prima: 🆕 **v0.N** per un'aggiunta
  (qualcosa che l'app prima non faceva), ✏️ **v0.N.M** per una modifica di
  qualcosa che c'era già (com'è fatto, come si comporta, dove sta,
  toglierlo), 🔧 **v0.N.M.K** per una versione di sole correzioni. Una
  versione con più cose prende il numero di quella più grande. Sono venute
  **124 aggiunte, 77 modifiche e 45 versioni di sole correzioni**: la
  versione di prima, "Paragraph", che era la v0.196, ora è la **v0.124**.
  Anche l'app ha il numero nuovo (Settings → Patch Notes e la versione
  sotto)
- **[Modifica]** **[📱 Mobile]** **La piattaforma di ogni voce**:
  **[📱 Mobile]** l'app per il telefono, **[💻 PC]**, **[🍎 Mac]**,
  **[🌐 Web App]**, più d'una insieme o **[🌍 Tutte]**. Messa a tutte le
  voci, anche a quelle di prima (731), che sono tutte dell'app per il
  telefono; le voci `[Progetto]`, su come è fatta, non ce l'hanno
- **[Modifica]** **[📱 Mobile]** Le etichette sono quattro: `[Nuova
  funzionalità]`, `[Modifica]` (prima anche `[Modificato]` e
  `[Cambiamento]`), `[Rimosso]` e `[Bug fix]` (prima anche `[Bug]`).
  Nelle versioni che ora sono modifiche, le voci che dicevano "[Nuova
  funzionalità]" dicono "[Modifica]"
- **[Progetto]** Come ho deciso: un'aggiunta è una cosa che prima non si
  poteva fare (una vista, un tipo di proprietà, un'opzione, un menu, un
  widget); allargare o spostare una cosa che c'era (un'altra voce dove
  c'era già, colori, misure, un comportamento diverso, toglierla) è una
  modifica; una versione di soli `[Bug fix]` è una correzione. Le voci
  dentro le aggiunte tengono la loro etichetta. I rimandi nel testo ("vedi
  v0.N") hanno il numero nuovo, tranne dentro le parole dell'utente e nei
  racconti delle numerazioni di allora (le vecchie v0.131, v0.132 e
  v0.134). La tabella vecchio → nuovo, per i commit di prima, è in
  "Vecchia numerazione", dopo la Cronologia, fuori dalle Patch Notes
- **[Progetto]** App: `PatchKind` ha tre tipi (🆕, ✏️, 🔧) e si legge dal
  numero (`PatchKind.of`); il titolo di una versione può avere un numero
  di quattro parti, nel build e nelle Patch Notes. `versionCode` resta
  quante versioni ci sono, quindi il telefono accetta l'aggiornamento
  anche col numero più basso. Test: `PatchNotesTest`, coi tre tipi, i
  numeri che seguono la regola nuova, e ogni voce con la sua piattaforma e
  un'etichetta adatta alla sua versione

### 🆕 v0.124 — 21/10/2026 · 00:59 — "Paragraph": lo spazio fra i blocchi e dopo numeri, pallini e caselle, come su Word
*(Claude Code sul PC; provata sul telefono su una pagina di prova, "Paragraph – prova", col database confrontato prima e dopo; 728 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Settings → "Paragraph"**, sotto "Full line
  view", con le due misure in punti, scelte con "−" e "+":
  - **"Space between blocks"**: lo spazio in più fra un blocco e l'altro
    (da 0 a 32 pt; 0 è com'erano sempre le pagine), metà sopra e metà sotto
    ogni blocco, anche dentro i toggle;
  - **"Space after number, bullet or checkbox"**: lo spazio fra il numero,
    il pallino o la casella di un elenco e il primo carattere ("Default",
    com'era sempre, poi da 0 a 32 pt). Con **"Same for numbered, bulleted
    and to-do lists"** acceso è uno solo per tutti e tre gli elenchi; spento,
    ognuno ha il suo
- **[Nuova funzionalità]** **[📱 Mobile]** **"Paragraph" nei tre puntini di ogni pagina**,
  sotto "Proofing": per ognuna delle due misure "As in Settings" oppure
  "Only this page", con le stesse scelte di Settings
- **[Nuova funzionalità]** **[📱 Mobile]** **"Paragraph" per un gruppo di righe**: dal nuovo
  pulsante ¶ della barra sopra la tastiera (le righe toccate dal testo
  selezionato, o quella del cursore) e dalla voce "Paragraph" del menu del
  blocco (sei puntini). "As in the page" oppure "Only these lines"; con più
  tipi di elenco fra le righe c'è anche lì "uguale per tutti". Come su Word,
  le righe nuove fatte con Invio dentro il gruppo ne tengono le misure.
  Ogni scelta si vede subito nella pagina e si annulla con Annulla
- **[Nota]** "Uguale per numbered, bulleted e to-do" l'ho letto come lo
  spazio dopo il marcatore, l'unica delle due misure che riguarda solo gli
  elenchi. Con uno spazio grande fra i blocchi il cursore diventa alto
  quanto la riga: è lo stesso limite delle caselle da spuntare (vedi
  `CHECKBOX_LINE_HEIGHT`)
- **[Progetto]** `data/entity/Paragraph.kt`: `ListGaps`, `PageParagraph`
  (colonna `pages.paragraphJson`, migrazione 58→59), `ParagraphLook`
  (vince la riga, poi la pagina, poi Settings) e `spacingStartsOf`;
  `BlockAttrs.spacing` e `BlockAttrs.markerGap`, che `attrsForNewLine`
  passa alla riga nuova; `AppSettings.blockSpacing` e `listGaps`. Nel campo
  di testo condiviso lo spazio fra i blocchi è **l'altezza di riga
  dell'intero campo**, la strada che "Strade già tentate" dice sicura: un
  gruppo con uno spazio suo diventa un campo a sé, e `resolveRun` si ferma
  agli stessi confini della schermata. Lo spazio dopo il marcatore allarga
  o stringe **solo in larghezza** gli spazi già scritti nel marcatore
  (`MarkerGapScale`): nessun carattere in più, la traduzione delle
  posizioni resta quella. Finestre in `ParagraphUi.kt`, scritte in
  `ParagraphStrings`. Nella build di debug `ParagraphProbeActivity` fa la
  pagina di prova. Test: `ParagraphTest`, `ParagraphLinesTest`,
  `ParagraphSettingsTest`, `MigrationTest` (con lo schema `v58.sql`)

### 🔧 v0.123.0.1 — 21/10/2026 · 00:17 — "+ Row", "+ Column" e il cestino solo mentre si scrive nella tabella
*(Claude Code sul PC; provata sul telefono nella pagina presa da game8; 719 test passati)*

Nella pagina presa da game8 con "Text and Images" ci sono sei tabelle
("Previous / Next", le debolezze del boss, gli oggetti di ogni piano), e
sotto ognuna la barra per modificarla restava sempre accesa, in mezzo
all'articolo.

- **[Bug fix]** **[📱 Mobile]** **La barra sotto una tabella** ("+ Row", "+ Column" e il
  cestino) **si vede solo mentre si scrive** in una casella di quella
  tabella: si tocca una casella e compare, si chiude la tastiera e va via.
  Vale per tutte le tabelle, non solo per quelle del Web Clipper
- **[Progetto]** `TableBlockContent` guarda se una sua casella ha il cursore
  (`onFocusChanged` e `focusGroup`). Test: `TableBarTest`

### 🆕 v0.123 — 20/10/2026 · 22:59 — Il Web Clipper legge le tabelle per righe, toglie i buchi della pubblicità e ha "Dark mode"
*(Claude Code sul PC; provato sul telefono sulla pagina di game8: "Text and Images", Full Page chiara e scura, Article scuro; 718 test passati)*

Visto sul telefono (schermate e struttura delle due pagine, chieste
dall'utente): in "Untitled 1" i pezzi erano nello stesso ordine di game8 (anche
la versione per telefono del sito ha le tre mappe nella sezione "Maps", dopo il
boss), ma **le tabelle erano srotolate**: le righe dei passi incollate in un
blocco solo, il numero di un passo in coda alla frase prima ("Exit the
Station. 2"), e nelle griglie di schede il testo di una scheda sopra
l'immagine della scheda dopo ("Recommended level: 20" sopra "Second Kalpa").
Per questo tutto sembrava sparso. In più le quattro tabelle degli oggetti
erano vuote, e "Untitled 2" (la foto Full Page, alta 29222 pixel) aveva un
buco bianco al posto di ogni riquadro pubblicitario.

- **[Bug fix]** **[📱 Mobile]** **"Text and Images" legge le tabelle per righe**, come la
  pagina:
  - le **tabelle dei passi** (numero e cosa fare): una riga per passo, col
    numero in grassetto davanti, "**1.** Exit the Station."; le note sotto un
    passo ("You can talk to the spirit…") restano sotto di lui; con
    un'immagine in cima al passo, prima il numero ("**2.**"), poi l'immagine,
    poi il testo; "**Boss Fight**" e le etichette come "**1F**" davanti al
    loro contenuto;
  - le **griglie di schede** (le guide dei capitoli, le aree Kalpa): una
    scheda alla volta, separate da una riga sottile, così ogni testo resta
    con la sua immagine;
  - la riga d'intestazione ("Objectives") in grassetto;
  - le righe orizzontali dentro una cella non diventano più divisori
- **[Bug fix]** **[📱 Mobile]** Le **tabelle di solo testo** (gli oggetti: "Obtained from",
  "Items", "Cache Cube"…) arrivano col loro testo: le celle si leggono dal
  testo scritto, non da come si vedono, che sul telefono a volte risultava
  vuoto
- **[Bug fix]** **[📱 Mobile]** Il titolo di un indice lasciato fuori ("List of Contents")
  non resta più da solo
- **[Bug fix]** **[📱 Mobile]** **I buchi bianchi della foto Full Page**: notE toglie i
  riquadri pubblicitari dei circuiti più diffusi (AdThrive/Raptive, Google,
  Taboola, Outbrain, Ezoic…) e il lettore video pubblicitario, e dopo lo
  scorrimento chiude ogni riquadro alto rimasto vuoto (né testo né immagini,
  mai dentro una tabella). Su game8: la pagina passa da 17553 a 13625 pixel, il
  buco sotto la copertina da 498 a 20, e restano tutte le 41 immagini e le 12
  tabelle. Le classi "ad" si riconoscono come parole intere ("head-container"
  non è pubblicità) e la pagina stessa non si nasconde mai (AdThrive mette una
  sua classe anche lì)
- **[Nuova funzionalità]** **[📱 Mobile]** **"Dark mode"** nel pannello del Web Clipper, per
  Full Page, Region e Article: il sito si apre col tema scuro (quelli che ne
  hanno uno usano il loro) e Chrome scurisce da sé gli altri; Article si
  impagina chiaro su scuro con i colori del tema scuro di Notion. Prima di
  Android 13, dove Chrome non sa scurire, i colori si rovesciano (le immagini
  no). La scelta si ricorda
- **[Progetto]** `web_clipper_reader.js`: `richTable`, `cell`, `TOC_TITLE`,
  `render(dark)`; `web_clipper_cleanup.js`: la pubblicità (`adBox`, la regola
  `[data-note-hidden]`) e `__noteCollapseEmpty`; `WebClipEngine` con `dark` e
  `cleanUp(collapseEmpty)`; `WebClipRequest.dark`, `WebClipMode.isPicture`,
  `AppSettings.webClipDark`. Test: `WebClipperSheetTest`; i calcoli del
  ridimensionamento delle immagini spostati in `ImageFrameTest`

### 🆕 v0.122 — 20/10/2026 · 19:45 — Le maniglie delle immagini solo su richiesta, e "Full resize" come una finestra
*(Claude Code sul PC; non ancora provata sul telefono; 717 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Le maniglie delle immagini non si vedono più**
  (c'erano dal 06/10/2026, sempre, su tutte). Tenendo premuta l'immagine, in
  cima al menu, sopra "View", c'è **"Resize"**: compaiono le due maniglie ai
  lati di sempre, che allargano e stringono l'immagine tenendola al centro e
  con la sua forma
- **[Nuova funzionalità]** **[📱 Mobile]** **"Full resize"**, subito sotto: le maniglie su
  **tutti e quattro i lati e sui quattro angoli**, come una finestra di
  Windows. Si muove solo il bordo afferrato: il destro allarga a destra, il
  sinistro sposta l'inizio dell'immagine, sopra e sotto cambiano l'altezza, gli
  angoli due bordi insieme. L'immagine riempie il riquadro (come in OneNote);
  non esce dalla pagina e non diventa più piccola di un dito
- **[Nuova funzionalità]** **[📱 Mobile]** Mentre si ridimensiona, sull'immagine ci sono
  **"Original size"** (la larghezza della pagina, al centro, con la sua forma)
  e **"Done"**. Finiscono anche un tocco sull'immagine e il gesto indietro. Si
  annulla come ogni modifica; con la pagina bloccata le maniglie non vengono
- **[Bug fix]** **[📱 Mobile]** Le **foto della fotocamera grandi** (da 6 megapixel in su)
  dalla v0.120 passavano alle strisce delle immagini lunghe, che non sanno
  girare una foto scattata in verticale. Ora le strisce sono solo per le
  immagini davvero lunghe e strette (più alte di 4096 pixel e di due volte e
  mezza la larghezza), come le foto delle pagine web; le altre si leggono come
  prima. E la forma di una foto in verticale si legge già girata
- **[Progetto]** `ImageResize.kt` (`ImageFrame`, `frameOf`, `widened`,
  `dragged`, `withFrame`, `withOriginalSize`, `ResizeHandle`,
  `ResizeToolbar`), `BlockAttrs.left` e `aspect`, `MediaRequestKind.RESIZE` e
  `FULL_RESIZE`, `PageImageStore.imageSize` con l'orientamento, `StripImage`
  alta quanto il suo riquadro. Test: `ImageResizeTest`

### 🆕 v0.121 — 20/10/2026 · 19:24 — "Text and Images": l'articolo di un sito come testo e immagini, come OneNote
*(Claude Code sul PC; provato sul telefono con l'attività di prova, non ancora dall'utente; 713 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Text and Images"**, la quinta scelta del Web
  Clipper: non una foto ma **l'articolo come righe di notE**, nell'ordine
  della pagina, al posto della prima riga vuota (o in fondo). In cima, come
  OneNote, il **titolo grande** (24 punti, in grassetto: è anche una voce
  dell'indice), la nota e "Clipped from" col link; poi l'articolo:
  - i **paragrafi** coi loro **grassetti**, corsivi, sottolineati, barrati e
    **link** che si toccano;
  - i **titoletti** in grassetto e più grandi (24 e 20 punti: anche loro voci
    dell'indice, v0.118);
  - gli **elenchi puntati e numerati** col loro rientro, citazioni, codice,
    tabelle semplici con le celle, divisori, e le didascalie delle immagini in
    corsivo;
  - le **immagini**, scaricate nell'app, larghe come nella pagina (quelle
    larghe quasi tutta la colonna a tutta larghezza)
- **[Nuova funzionalità]** **[📱 Mobile]** **Cosa resta fuori**, come fa OneNote: notE trova
  l'articolo come il "Reader" dei browser (il blocco con più paragrafi di testo
  vero e pochi link, più i fratelli che ne hanno abbastanza), quindi
  **copertina, colonne di lato, barre in fondo, menu, commenti e pubblicità**
  non ci sono. In più di OneNote lascia fuori anche i riquadri "Advertisement.
  Keep scrolling for more" (OneNote li aveva tenuti) e gli **indici fatti solo
  di link** ai capitoli della pagina (come OneNote). Il titolo dell'articolo
  non si ripete. Sulla guida di rpgsite: 96 blocchi e 7 immagini su 7, in 11
  secondi, dal primo paragrafo "Persona 5 Royal features…" all'ultimo
- **[Nuova funzionalità]** **[📱 Mobile]** Le immagini che la pagina carica solo scorrendo (su
  rpgsite un segnaposto "image-loading") arrivano col loro indirizzo vero: notE
  fa scorrere la pagina e legge anche gli indirizzi nascosti delle immagini
  (`data-src`, le varianti del tag `<picture>`). Si scaricano come le scarica
  il browser, coi cookie del sito: un sito in cui si è entrati dà anche le sue
  immagini. Mentre si scaricano si vede "Downloading the images (3 of 7)…";
  fermato, quelle già scaricate non restano
- **[Bug fix]** **[📱 Mobile]** Il banner dei cookie di Google ("Do not consent", come su
  rpgsite) si rifiuta da solo come gli altri; prima si nascondeva soltanto
- **[Nuova funzionalità]** **[📱 Mobile]** Anche **"Article"** usa lo stesso modo di trovare
  l'articolo: la copertina non c'è più neanche lì
- **[Progetto]** `assets/web_clipper_reader.js` (`noteReader`: `render` per
  "Article", `serialize` per "Text and Images"; prende il posto di
  `web_clipper_article.js`), `WebClipEngine.readContent`, `WebClipContent.kt`
  (`parseClipContent`, `ClipPiece`, `toNewBlocks`, `headingSize`),
  `WebClipDownloads`, `NewBlock`, `PageRepository.insertAtFirstEmptyLine` (più
  blocchi in fila, con le celle delle tabelle; ci passa anche la foto) e
  `PageEditorViewModel.insertWebClipBlocks`. Test: `WebClipContentTest`, e in
  `WebClipPlacementTest` l'articolo inserito con elenco, immagine e tabella

### 🆕 v0.120 — 20/10/2026 · 18:59 — Il Web Clipper, come OneNote: la foto di un sito nella pagina
*(Claude Code sul PC; provato sul telefono con le attività di prova, non ancora dall'utente; 709 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Web Clipper"** nel menu dei tre puntini di ogni
  pagina (non nei database; spento con la pagina bloccata). Si apre il
  pannello come quello di OneNote Web Clipper: il **link** (incollato col
  pulsante accanto o scritto, anche senza "https://"), **"What do you want to
  capture?"** con **Full Page**, **Region**, **Bookmark** e **Article**, poi
  **Title** (si riempie da solo col titolo del sito), **Note** e **Source**.
  **Niente "Location"**: tolta, come chiesto. "Clip" si accende solo con un
  link vero
- **[Nuova funzionalità]** **[📱 Mobile]** **Full Page**: notE apre il sito **come sul PC**
  (largo 1440 pixel, come un portatile: è la larghezza a cui game8 viene come
  nella foto di OneNote, colonna di sinistra compresa; in chiaro), lo fa
  scorrere fino in fondo perché arrivino le immagini che si caricano solo
  scorrendo, e lo fotografa dall'alto in fondo. Mentre lo fa si vede la
  pagina che scorre, coi passi scritti e la barra che avanza; "Cancel" ferma
  tutto. La pagina di game8: 2160 × 19023 pixel in 13 secondi, 4 MB
- **[Nuova funzionalità]** **[📱 Mobile]** **Le pagine vengono intere** (la domanda 3): la
  foto si scrive nel file **a strisce** mentre la pagina scorre, senza mai
  stare tutta in memoria (intera, quella di game8 sarebbero 160 MB), e si
  mostra **a pezzi**, leggendo dal file solo quelli sullo schermo. Solo una
  pagina più alta di 65535 pixel (il limite del formato JPEG, più di quaranta
  schermate di PC) si fotografa un po' più piccola, ma sempre intera
- **[Nuova funzionalità]** **[📱 Mobile]** **I banner dei cookie** (la domanda 2): prima dello
  scatto notE tocca da sé **"Reject all"** / "Rifiuta" (la scelta che
  protegge di più), poi nasconde i banner che restano (anche quelli chiusi in
  un pezzo di pagina a parte, come quello di game8), il velo scuro dietro, le
  barre fisse in basso (pubblicità, chat) che nella foto finirebbero in mezzo
  alla pagina, e sblocca lo scorrimento. La barra del sito in alto resta, in
  cima. Provato sulla pagina di game8 dell'utente: banner e barra
  pubblicitaria via
- **[Nuova funzionalità]** **[📱 Mobile]** **Region**: il sito a tutto schermo come sul PC, da
  scorrere e ingrandire come in un browser; "Select" ferma la pagina e col
  dito si disegna il rettangolo (fuori si scurisce), "Clip" lo fotografa
  nitido come la pagina intera. "Scroll" torna a scorrere
- **[Nuova funzionalità]** **[📱 Mobile]** **Article**: notE tiene solo l'articolo (titolo,
  testo, immagini, tabelle), senza menu, pubblicità, colonne e commenti,
  impaginato pulito come un libro in una colonna da leggere, e lo fotografa
  intero
- **[Nuova funzionalità]** **[📱 Mobile]** **Bookmark**: non una foto ma il "Web bookmark" di
  notE, con titolo, descrizione e immagine del sito
- **[Nuova funzionalità]** **[📱 Mobile]** **Dove va**: al posto della **prima riga vuota
  della pagina, dall'alto** (anche dentro un toggle aperto); se non ce n'è,
  in fondo. Sotto la foto: il titolo in grassetto, la nota (una riga per ogni
  sua riga) e, piccolo e grigio, "Clipped from" col link che si tocca. La
  pagina scorre fino alla foto, che si illumina un attimo. Si annulla come
  ogni modifica
- **[Nuova funzionalità]** **[📱 Mobile]** **La foto nella pagina** è larga quanto la pagina e
  alta quanto vuole, nitida (a strisce, vedi sopra); le maniglie ai lati la
  stringono come le altre immagini. **Toccandola** si apre a tutto schermo
  larga quanto lo schermo, in cima: si scorre col dito **con lo slancio**,
  si ingrandisce con due dita fino a otto volte, con due tocchi si ingrandisce
  lì o si torna com'era; la X e lo scarica sono su un tondo scuro, che sopra
  una pagina bianca si vedono. Vale per tutte le immagini molto alte
- **[Nuova funzionalità]** **[📱 Mobile]** **"Move to line…"** (la domanda 1): nel menu di
  un'immagine (e di segnalibri, file, divisori, sottopagine e database),
  accanto a "Move up" e "Move down", l'elenco delle righe della pagina
  rientrate come lì, più "Top of the page": toccandone una il blocco va
  subito sotto di lei, anche dentro un toggle. Prima c'erano solo "Move up" e
  "Move down", un posto alla volta, e "Move to" verso un'altra pagina
- I **paywall** (la domanda 4): aggirarli no, vorrebbe dire scavalcare
  l'abbonamento di un sito. Se l'abbonamento c'è, si entra col proprio
  account in "Region" (il sito si usa come in un browser) e notE se lo
  ricorda: anche le foto "Full Page" e "Article" dopo vedono la pagina come
  la vede l'utente
- **[Progetto]** `data/webclip`: `WebClipEngine` (il WebView "da PC", le
  sue misure in pixel veri, `prepare`, `captureFullPage` che scorre una
  finestra alla volta e disegna con `WebView.draw`, `captureRegion`,
  `keepOnlyArticle`), `JpegStripWriter` (JPEG baseline 4:4:4 scritto a
  strisce, con le tabelle standard), `WebClips.kt` (`webClipUrl`,
  `webClipCaptions`). Gli script in `assets/web_clipper_cleanup.js` e
  `web_clipper_article.js`. `PageRepository.insertWebClip` e
  `moveBlockAfter`, `PageLines.kt` (`inReadingOrder`, `firstEmptyLine`,
  `moveTargetsFor`), `PageImageStore.newFile`/`imageSize`, `TallImage.kt`
  (`StripImage`, `TallImageViewer` con `BitmapRegionDecoder`), `WebClipper.kt`
  (il pannello, `WebClipCaptureDialog`, `WebClipRegionDialog`),
  `MoveToLineSheet`, `WebClipStrings` nelle 8 lingue. Scoperto sul telefono:
  `WebView.enableSlowWholeDocumentDraw()` faceva uscire le foto bianche, e
  `WebView.getScale()` restava fermo a 3,5: non si usano. Nella build di debug
  `WebClipProbeActivity` e `WebClipUiProbeActivity` provano tutto da adb
  senza toccare le pagine. Test: `JpegStripWriterTest` (il JPEG riletto dal
  decodificatore di Java), `WebClipsTest`, `PageLinesTest`,
  `WebClipPlacementTest`, `WebClipperSheetTest`

### 🆕 v0.119 — 20/10/2026 · 17:55 — I colori preferiti, sotto gli ultimi colori usati
*(Claude Code sul PC; non ancora provata sul telefono; 693 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La stella** nel selettore di colore, a destra
  dell'anteprima ("Aa" col nome e il codice del colore): toccata, il colore
  scelto va fra i **preferiti** e la stella si colora di giallo; toccata di
  nuovo (sul colore di un preferito) lo toglie. Il selettore è lo stesso in
  tutta l'app: testo, celle, tag, penne, barre
- **[Nuova funzionalità]** **[📱 Mobile]** **"Favorite colors"**, la fila dei preferiti
  **sotto "Recent colors"**: i tondi sono grandi come quelli dei recenti e
  stanno in colonna con loro, nell'ordine in cui li si è aggiunti (l'ultimo
  in fondo); oltre i dieci la fila si scorre di lato. Toccandone uno il
  selettore va su quel colore, e si applica con "Apply" come i recenti.
  Restano finché non li si toglie (fino a 40); senza preferiti la fila dice
  di toccare la stella
- **[Progetto]** `AppSettings.favoriteColors` e `toggleFavoriteColor`
  (salvati in `favorite_colors`), `withFavoriteColorToggled`; nel
  `ColorPickerSheet` la stella, `FavoriteColorsRow` e il tondo `ColorSwatch`
  in comune coi recenti; i testi nelle 8 lingue. Test: `FavoriteColorsTest`

### 🔧 v0.118.2.1 — 20/10/2026 · 15:17 — Una colonna stretta a mano resta stretta anche con "Widen the column"
*(Claude Code sul PC; non ancora provata sul telefono; 691 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Stringere una colonna col segnalino** ("Resize column") in
  una tabella con "Long text" su **"Widen the column"**: mentre si trascinava
  la colonna si stringeva, ma al rilascio tornava larga quanto il suo
  contenuto, perché "Widen the column" allargava ogni colonna fin lì. Ora
  allarga solo le colonne senza una misura scelta a mano: quella stretta col
  segnalino resta com'è, e il testo che non ci sta finisce coi tre puntini.
  Le altre colonne si allargano come prima
- **[Bug fix]** **[📱 Mobile]** Sempre con "Widen the column", riportata alla larghezza di
  sempre (dove il segnalino si aggancia) la colonna resta lì, invece di
  riallargarsi al contenuto
- **[Progetto]** `tableColumnWidth` in `CellText.kt` decide quale misura
  vince (il dito, "Fit to content", la misura scelta, quella di sempre, e
  quando allargare); `DatabaseViewModel.setColumnWidth` con "Widen the column"
  ricorda anche la larghezza di sempre. Test: `WrapCellsTest`

### ✏️ v0.118.2 — 20/10/2026 · 14:33 — "Select all" seleziona tutta la pagina, come su Notion
*(Claude Code sul PC; non ancora provata sul telefono; 689 test passati)*

- **[Modifica]** **[📱 Mobile]** **"Select all" vela di blu tutta la pagina**: dalla
  barra sopra la tastiera e dal menu del testo (quello che compare tenendo
  premuto). Prima arrivava solo ai bordi del pezzo di testo da 50 righe (dalla
  v0.113.1), e una selezione di testo comunque non passa sopra un database o
  un'immagine. La tastiera si chiude, e in basso c'è una barra con **Copy**,
  **Cut** e **Delete** (in una pagina bloccata solo Copy) e la ✕. Un tocco sulla
  pagina, la ✕ o Indietro tolgono la selezione; la pagina scorre come sempre
- **[Modifica]** **[📱 Mobile]** **Copy** mette negli appunti tutta la pagina,
  nell'ordine in cui si legge: grassetti, corsivi, colori e link restano, e
  incollata in notE ogni riga torna col suo tipo (testo, elenco puntato o
  numerato, casella spuntata o no) e il suo rientro. Riquadri, citazioni e
  codice danno una riga per ognuna delle loro; le tabelle una riga per riga,
  con le celle separate da una tabulazione (come le copia un foglio di
  calcolo); i divisori "---"; immagini e file il loro indirizzo o nome
- **[Modifica]** **[📱 Mobile]** **Cut** copia e poi toglie tutto; **Delete** toglie
  tutto, dopo una domanda. Le **sottopagine e i database restano** (e i toggle
  che li contengono): vivono anche altrove, e si tolgono uno alla volta dal
  loro menu. Tutti e due si possono annullare
- **[Modifica]** **[📱 Mobile]** Nel titolo della pagina e nelle celle dei database
  dentro la pagina "Select all" del menu del testo prende solo quel campo,
  come prima. Ctrl+A su una tastiera vera seleziona il testo del campo
- I database da migliaia di righe (la domanda sul dizionario): **a pagina
  intera le righe scorrono già tutte**, senza "Load more", dalla v0.113.2; solo
  la bacheca ha 50 schede per colonna anche lì. Dentro una pagina restano le
  prime 50 e "Load more". Nessuna delle due cose fa pesare l'app di più
- **[Progetto]** `PageSelection.kt` (`WholePageTextToolbar`,
  `PlainTextToolbar`, `pageVeil`, `tapToEndPageSelection`,
  `copyPageToClipboard`, `PageSelectionBar`), `PageEditorViewModel.
  pageCopyLines` e `deletePageContent`, `ClipboardBarButtons(onSelectAll)`.
  Test: `PageSelectAllTest`, `PageSelectionTest`

### ✏️ v0.118.1 — 20/10/2026 · 13:59 — "Keep Source Formatting" porta grandezze, elenchi e tabelle da OneNote e dal web
*(Claude Code sul PC; non ancora provata sul telefono; 685 test passati)*

- **[Modifica]** **[📱 Mobile]** **La grandezza del testo**, con "Keep Source
  Formatting": il corpo più usato nel testo copiato diventa quello normale di
  notE, e il resto in proporzione. Il testo normale di OneNote (11 punti)
  resta normale; un titolo a 20 punti diventa 29, grande quanto lo era lì
  rispetto al resto. Dal web, il testo che non dice la sua grandezza vale
  quella di sempre dei browser, e i titoli (H1–H6) arrivano grandi e in
  grassetto come sulla pagina. Una riga che arriva tutta da 20 punti in su fa
  da punto di arrivo dell'indice (v0.118)
- **[Modifica]** **[📱 Mobile]** **Gli elenchi puntati e numerati** arrivano come
  elenchi di notE, col loro rientro (un elenco dentro l'altro), senza i
  pallini o i numeri scritti come testo
- **[Modifica]** **[📱 Mobile]** **Le tabelle** arrivano come tabelle semplici di
  notE, con le righe, le colonne e il testo diviso cella per cella come nella
  tabella copiata (da OneNote o da una pagina web, intestazioni comprese). Una
  "tabella" di una cella sola, che sui siti serve solo a impaginare, arriva
  come testo. Le celle sono di solo testo, come sempre nelle tabelle semplici
- **[Modifica]** **[📱 Mobile]** Le righe, i paragrafi vuoti messi apposta e i
  collegamenti arrivano come erano nella fonte. "Paste", "Match Destination
  Formatting" e "Keep Text Only" restano come prima, e da notE a notE il tipo
  delle righe arriva come prima
- **[Progetto]** `ClipHtml.kt`: `parseClipHtml` (righe, elenchi, tabelle,
  stili e grandezze dall'HTML degli appunti; il carattere scritto sul
  `<body>` da OneNote vale per tutto), `noteSizes`. `LinkClipboard.
  sourceFormatted` con l'etichetta `pasted-table` per le celle,
  `StyleRange.size`, `PastedLine.table`, e in `PageEditorViewModel.updateRun`
  la riga diventa una tabella (`writePastedCells`). Test: `ClipHtmlTest`,
  `PasteSourceStructureTest`

### 🔧 v0.118.0.2 — 20/10/2026 · 13:46 — La riga alla fine del database torna sotto il database
*(Claude Code sul PC; non ancora provata sul telefono; 678 test passati)*

- **[Bug fix]** **[📱 Mobile]** **La riga sottile alla fine di un database dentro una pagina**
  (v0.112, 18/10/2026) stava **in cima** al database invece che sotto:
  in una pagina di riga si vedevano due linee in alto, quella delle proprietà
  e questa. Ora è di nuovo sotto, a separare il database dal testo che segue.
  Il guasto era della v0.114 (19/10, "Full line view"): ogni blocco a sé stava in un
  riquadro che mette i suoi pezzi uno **sopra** l'altro, e il database e la
  sua riga finivano nello stesso punto. Ora i pezzi stanno uno **sotto**
  l'altro, come prima, per tutti i blocchi a sé
- **[Progetto]** In `PageEditorScreen` il contenitore delle isole è una
  `Column`, non un `Box`

### 🔧 v0.118.0.1 — 20/10/2026 · 13:39 — I numeri nel testo si ordinano come numeri: 2 prima di 10
*(Claude Code sul PC; non ancora provata sul telefono; 678 test passati)*

- **[Bug fix]** **[📱 Mobile]** **Ordinando per un testo** (il nome delle righe, una proprietà
  di testo, il titolo di una pagina collegata…) i numeri che ci sono dentro
  valgono per quello che sono: 1, 2, 3… 9, 10, 11, e non più 1, 10, 11… 19, 2,
  20. Vale anche dentro il testo: "Giorno 9" prima di "Giorno 10", "v0.9"
  prima di "v0.10". Maiuscole e minuscole continuano a non contare, e i numeri
  vengono prima delle lettere come prima
- **[Bug fix]** **[📱 Mobile]** Lo stesso ordine per i **gruppi** di una tabella o di una
  bacheca raggruppata per una proprietà di testo
- **[Progetto]** `NaturalOrder` in `data/NaturalOrder.kt` (a parità, come
  "01" e "1", decide il confronto di prima, così l'ordine resta fisso), usato
  da `DatabaseViewModel.compareSortValues` e da `buildRowGroups`. Test:
  `NaturalOrderTest`

### 🆕 v0.118 — 20/10/2026 · 13:01 — L'indice navigabile: collegamenti a una riga della stessa pagina
*(Claude Code sul PC; non ancora provata sul telefono; 674 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Il pin**: nel menu dei sei puntini di una riga di
  testo (anche riquadro, citazione, toggle, codice), **"Pin as link target"**
  ("Fissa come destinazione"). La riga ha un piccolo segnalibro blu nel
  margine a sinistra; dallo stesso menu, "Unpin link target" lo toglie. Si
  può annullare
- **[Nuova funzionalità]** **[📱 Mobile]** **Le righe in grande fanno da titoli da sole**: una
  riga scritta tutta da 20 punti in su (barra Aa) è già un punto di arrivo,
  senza pin. I titoli H1, H2 e H3 non ci sono più dal 24/09/2026, quindi il
  "titolo" è il testo grande
- **[Nuova funzionalità]** **[📱 Mobile]** **"In this page"** nella finestra del
  collegamento, accanto a "Web": l'elenco delle righe col pin e di quelle in
  grande, nell'ordine della pagina e rientrate come lì (segnalibro per quelle
  col pin, "T" per quelle in grande). Si seleziona la voce dell'indice, si
  tocca l'icona del collegamento, "In this page", e un tocco sulla riga fa il
  collegamento. Senza righe a cui collegarsi, la scheda dice come farne una
- **[Nuova funzionalità]** **[📱 Mobile]** **Il salto**: toccando la voce collegata la pagina
  va **subito** a quella riga, in cima sotto i pulsanti sospesi, e la riga si
  illumina un attimo come una riga spostata. In basso compare **"Back"** (con
  la freccia verso dove si torna), che riporta la pagina dov'era; se ne va da
  solo dopo dieci secondi. Una riga dentro un toggle porta al toggle
- **[Nuova funzionalità]** **[📱 Mobile]** Il collegamento è **blu e sottolineato come gli
  altri, con una piccola freccia dopo l'ultima lettera**, così si distingue da
  quello a un sito. Segue la riga e non il testo: rinominata o spostata la si
  ritrova; cancellata, toccando la voce un avviso dice che non c'è più
- **[Nuova funzionalità]** **[📱 Mobile]** I blocchi a sé (riquadro, toggle, citazione,
  codice) ora si illuminano anche quando li si sposta con "Move up" e "Move
  down", come le righe di testo
- **[Progetto]** `BlockLinks.kt` (`note://block/<id>`, `blockLink`,
  `blockLinkTarget`, `LinkTarget`, `isLargeLine`, `linkTargetsOf` in ordine di
  lettura), `BlockAttrs.linkTarget`, `PageAnchors.kt` (`LocalBlockLinkJump`,
  `lineStartIn`/`lineTopIn`, `drawPinRibbon`, `drawBlockLinkArrows` con lo
  spazio `markBlockLinkEnd`, `anchorDecor`, `LinkBackButton`),
  `VisibleRunLines.lineTops` (dove sta una riga dentro un gruppo, per il
  salto), `PageEditorViewModel.setLinkTarget`/`linkTargets`/`highlightBlock`,
  `rememberLinkOpener` che salta invece di aprire il browser, `LinkDialog` con
  le schede e `LinkTargetList`; i testi nelle 8 lingue. Test: `BlockLinksTest`,
  `PageLinkTargetTest`, `LinkDialogTargetsTest`

### 🆕 v0.117 — 20/10/2026 · 10:24 — La barretta di scorrimento a destra delle pagine
*(Claude Code sul PC; non ancora provata sul telefono; 664 test passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La barretta di scorrimento** sul bordo destro
  delle pagine: sottile, chiara (scura col tema chiaro), coi bordi tondi, come
  nelle impostazioni del telefono. Compare mentre la pagina scorre e svanisce
  poco meno di un secondo dopo che si è fermata. È alta quanto la parte di
  pagina che si vede (mai meno di un dito di pagina), in cima e in fondo tocca
  il bordo, e non prende i tocchi: sotto si continua a scrivere e a toccare
  come prima. Non c'è se la pagina sta tutta nello schermo
- **[Nuova funzionalità]** **[📱 Mobile]** Le pagine sono fatte di voci di altezze diverse
  (pezzi di testo, immagini, database) che si misurano solo quando si vedono:
  la barretta si ricorda l'altezza di quelle già viste e dà la media alle
  altre, quindi è esatta dove si è già passati e si assesta scorrendo
- **[Progetto]** `PageScrollIndicator.kt`: `PageScrollIndicator` (sopra la
  lista della pagina in `PageEditorScreen`, accanto alla barretta di lato di
  "Full line view"), `ItemSizes`, `estimateExtent`, `scrollThumb`. Test:
  `PageScrollIndicatorTest` (la media per le voci mai viste; altezza, minimo,
  cima e fondo; compare scorrendo e svanisce dopo)

### 🔧 v0.116.0.2 — 20/10/2026 · 08:58 — "Today" passa alla giornata dopo a mezzanotte
*(Claude Code sul PC; non ancora provata sul telefono; 661 test passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** **"Today" non tiene più la giornata finita il giorno dopo**:
  fino alle 23:59 del giorno dell'ultima partita resta quella giornata, da
  mezzanotte va alla successiva. Nell'esempio: lunedì 19/10 la settima della
  Serie A, da martedì 20 l'ottava. Aprendo il widget, invece, il martedì si
  vedono ancora i risultati della giornata finita il lunedì, come prima
- **[Progetto]** `todayRound` in `Sports.kt`, come `currentRound` ma senza il
  giorno dopo; lo usa "Today" in `CompetitionRounds`. Test: `SportsTest` (le
  date dell'esempio)

### 🔧 v0.116.0.1 — 20/10/2026 · 08:57 — La selezione non si ferma più fra un pezzo e l'altro delle pagine lunghe
*(Claude Code sul PC; verificata sul telefono dall'utente il 20/10/2026; 661 test passati; commit solo in locale)*

La richiesta, riassunta (la conversazione in cui era scritta conteneva dati
privati e non viene riletta): tenendo premuto e trascinando, la selezione si
fermava sempre alla stessa riga di una pagina lunga.

- **[Bug fix]** **[📱 Mobile]** Dalla v0.113.1 le pagine lunghe sono divise in pezzi da 50
  righe, e ogni pezzo è un campo di testo a sé: un campo non sa selezionare
  oltre le sue righe, quindi la selezione si fermava al confine fra due pezzi,
  circa ogni 50 righe. Ora, quando la pagina è ferma e nessun dito la tocca, le
  righe sullo schermo stanno in un campo solo, e la selezione va su e giù per
  tutto lo schermo. Il campo in cui si scrive non si tocca mentre si scrive: le
  righe si uniscono due secondi dopo l'ultima lettera. Lasciato il campo, la
  pagina torna divisa in pezzi piccoli, così la scrittura resta veloce
- **[Bug fix]** **[📱 Mobile]** Scartata la prima strada, unire i pezzi durante la selezione:
  trascinando la maniglia blu, la maniglia si fermava al confine e la lente
  restava bloccata sullo schermo
- **[Progetto]** `VisibleChunks.kt`: `VisibleRunLines`, `LocalVisibleRunLines`,
  `LocalPageFingerDown`, `watchPageFingers`, `KeepVisibleLinesTogether`.
  `PageEditorViewModel`: `joinChunkAfter`, `keepVisibleTogether` (il campo col
  fuoco resta e prende le righe di sopra; `TYPING_QUIET_MS`, 2 secondi;
  `FOCUSED_CHUNK_CAP`, 200 righe), `rechunk` non mette confini fra le righe
  sullo schermo, `lastTypedAt`. `PageEditorScreen`: le righe arrivate in fondo
  o in cima si uniscono subito, `joinsAbove`/`joinsBelow`. Test:
  `ChunkSelectionTest`
- Provata dall'utente sul telefono, con la release v0.116: funziona

### 🆕 v0.116 — 20/10/2026 · 08:29 — "Freeze Properties Row", "Today" nel widget dello sport e i tag a due colori
*(Claude Code sul PC; verificata sul telefono dall'utente il 20/10/2026; 654 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** "Freeze first row" ora si chiama **"Freeze
  Properties Row"** ("Blocca la riga delle proprietà" in italiano, e così
  nelle altre lingue): è la riga coi nomi delle proprietà, non una riga del
  database. Fa la stessa cosa di prima
- **[Nuova funzionalità]** **[📱 Mobile]** **"Today" nel widget dello sport**: un piccolo
  calendario a sinistra della freccia che aggiorna. Toccandolo, la competizione
  che si vede torna alla giornata di oggi (o alla prossima, se oggi non si
  gioca), scorrendo fin lì; se si stava guardando una stagione passata, torna
  anche alla stagione in corso. Se le partite sono vecchie, si riscaricano
- **[Nuova funzionalità]** **[📱 Mobile]** **I tag a due colori**: nella modifica di un tag,
  sotto "Color", **"Add a second color"** apre lo stesso spettro del primo. La
  pastiglia ha il primo colore nella metà di sinistra e il secondo in quella di
  destra, ovunque si vede il tag: nelle celle, nella finestra dei tag, nei
  filtri, nei gruppi della tabella e della bacheca. Poi, sotto: "Second color"
  per cambiarlo, **"Line between colors"** (spenta di partenza, i colori stanno
  attaccati; accesa, una linea verticale del colore della pagina li separa) e
  "Remove second color". La scritta è bianca o nera secondo la media dei due
  colori. Nella bacheca con "Color columns" la colonna prende il velo del primo
  colore
- **[Progetto]** `DbStrings.freezeFirstRow` cambia solo il testo (il campo
  `freezeFirstRow` del database resta). `SportsUpdates.goToToday` e
  `todayRequests` (i tocchi su "Today", per widget), `SportsTodayButton`, e in
  `CompetitionRounds` lo scorrimento alla giornata di `currentRound` a ogni
  tocco. `SelectOption.secondColor` e `colorLine` (i tag salvati prima si
  leggono con un colore solo), `DatabaseViewModel.setSelectOptionSecondColor`,
  `onSetOptionSecondColor` per la stessa strada di `onMoveOptions`, `TagChip`
  con le due metà disegnate dietro alla scritta, `RowGroup.secondColor`,
  `TagColorRow`, `darkTextOn(first, second)`; `DbStrings.addSecondColor`,
  `secondColor`, `lineBetweenColors`, `removeSecondColor`. Test:
  `TagSecondColorTest` (il secondo colore dallo spettro; la linea e il
  togliere; la scritta sulla media; i tag di prima)
- Provata dall'utente sul telefono, con la release v0.116: funziona

### 🆕 v0.115 — 20/10/2026 · 08:17 — I tag si riordinano con le maniglie
*(Claude Code sul PC; verificata sul telefono dall'utente il 20/10/2026; 650 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Reorder"** ("Riordina" in italiano) in alto a
  destra nella finestra dei tag, accanto al titolo "Select" o "Multi-select",
  quando i tag sono almeno due. Toccandolo, i tag compaiono tutti in fila,
  ognuno con la maniglia a tre lineette a destra: la si tiene e la si
  trascina, e gli altri tag fanno posto appena il tag arriva a metà di uno di
  loro. Il tag trascinato si stacca dagli altri con un'ombra. "Done" torna
  all'elenco di sempre
- **[Nuova funzionalità]** **[📱 Mobile]** L'ordine si salva a ogni rilascio: chiudere la
  finestra non perde niente. Con tanti tag, portando quello trascinato vicino
  al bordo di sopra o di sotto della finestra, l'elenco scorre da solo
- **[Nuova funzionalità]** **[📱 Mobile]** L'ordine vale ovunque si vedono i tag in elenco:
  nella finestra dei tag, nei filtri, e nei gruppi della tabella e della
  bacheca raggruppate per quella proprietà, che seguono l'ordine dei tag come
  su Notion. Le celle non cambiano
- **[Progetto]** `TagReorderList.kt`: `TagReorderList` (l'ordine sullo
  schermo vale più di quello del database che arriva un attimo dopo il
  rilascio, così il tag non torna per un attimo dov'era; lo scorrimento da
  solo vicino ai bordi) e `moveWhileDragging` (oltre quali vicini è passato il
  tag). `SelectOption.reorderedAs` in `DatabaseColumnEntity.kt` (un tag che
  manca dall'ordine va in fondo, uno eliminato non rinasce),
  `DatabaseViewModel.moveSelectOptions`, e `onMoveOptions` che scende fino a
  `TagPickerSheet` per la stessa strada di `onDeleteOption`.
  `DbStrings.reorder` e `dragToReorder`. Test: `TagReorderTest` (l'ordine
  nuovo; oltre la metà del vicino e non prima; un trascinamento corto non
  salva niente; il secondo parte dall'ordine salvato; "Done" torna
  all'elenco)
- Provata dall'utente sul telefono, con la release v0.116: funziona

### 🆕 v0.114 — 19/10/2026 · 23:20 — Le righe lunghe non vanno più a capo
*(Claude Code sul PC; verificata sul telefono; 643 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Settings → "Full line view"** ("Visualizzazione
  totale" in italiano), sotto "Title position", accesa di partenza: le righe
  lunghe delle pagine non vanno più a capo, e col dito si scorrono di lato
  fino alla loro fine. Le righe scorrono **tutte insieme**, come su un foglio
  largo, e restano allineate fra loro. Il titolo, le proprietà, i database, le
  immagini e gli altri blocchi che non sono testo restano fermi. Vale per il
  testo della pagina (paragrafi, elenchi, caselle) e per quello di toggle,
  riquadri, citazioni, notifiche e codice, che scorre dentro il suo riquadro.
  Spenta, tutto è come prima
- **[Nuova funzionalità]** **[📱 Mobile]** **Fatta per non avere i difetti di OneNote**:
  - il dito va in verticale **oppure** di lato, mai di sbieco: il primo verso
    in cui si muove decide per tutto il gesto;
  - di lato si va solo fin dove finisce la riga più lunga fra quelle sullo
    schermo, più un po' d'aria, mai oltre;
  - scrivendo, il cursore resta sempre in vista anche di lato; andando a capo
    si torna all'inizio delle righe;
  - mentre si scorre di lato compare in fondo una barretta blu che dice dove
    si è, e svanisce poco dopo;
  - una tabella di un database arrivata al suo bordo non si trascina dietro il
    testo intorno;
  - i tratti a penna sul testo si spostano col testo; mentre si disegna le
    righe tornano all'inizio
- **🔴 [Da rivedere]** **Scorrendo in giù verso righe più corte, la pagina
  torna di lato da sola**, quanto basta per vedere la fine della riga più
  lunga sullo schermo, invece di lasciare lo schermo vuoto. **L'alternativa**:
  restare dove si è finché non la si sposta col dito, come OneNote e gli
  editor di codice. Risalendo si ritroverebbe la stessa posizione, ma si può
  finire su uno schermo vuoto
- **[Progetto]** `FullLineView.kt`: `SideScroll` (lo spostamento comune, in
  px, e `reach`, fin dove si può andare, contato sulle voci della lista che si
  vedono), `SideLane` (ogni campo di testo è una corsia: si misura senza
  limite di larghezza e si posa spostato; lo spostamento si legge solo nel
  momento di posare, quindi scorrere di lato non rifà né il testo né la sua
  misura), `SideResponder` (il cursore in vista anche di lato; un rettangolo
  più largo dello spazio, cioè tutto il campo, non sposta niente),
  `KeepSidewaysInside` sulle isole, `SideScrollEffects`,
  `SideScrollIndicator`. `AppSettings.fullLineView` (`full_line_view`),
  `Strings.fullLineView`/`fullLineViewNote`, `InkPageState.slidingItems` e
  `sideOffset`. Test: `FullLineViewTest` (le righe non vanno a capo; di lato
  fino alla fine della riga e non oltre, col riquadro fermo che resta fermo; il
  primo verso decide il gesto; scesi fra righe corte si torna all'inizio; il
  cursore resta in vista e a capo si torna all'inizio; l'impostazione)
- Provata sul telefono. In Permanent notes, solo guardata e scorsa: le righe
  lunghe non vanno più a capo, si scorrono di lato fino alla fine della riga
  più lunga sullo schermo, in verticale la pagina non si sposta di lato, e
  risalendo in cima torna di lato quanto basta. In una riga di prova del Main
  calendar: una riga lunga scritta con la tastiera, col cursore sempre in
  vista; a capo si torna all'inizio; trascinata di lato con la tastiera
  aperta; un riquadro con un testo lungo scorre dentro il suo riquadro,
  allineato al testo sopra. La prima barretta, grigia e appena sopra la barra
  di sistema, sembrava una seconda barra di sistema: ora è blu, su una
  traccia, più in alto. Sulla release, dieci trascinate di lato in Permanent
  notes: 295 fotogrammi, metà sotto i 6 ms, il 95% sotto i 9 ms, nessuno
  lento per colpa dell'app. Riga di prova eliminata: database, impostazioni e
  widget identici a prima
### ✏️ v0.113.3 — 19/10/2026 · 22:44 — Le caselle copiate si incollano come caselle
*(Claude Code sul PC; verificata sul telefono; 637 test passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **Copiando delle righe da notE, ognuna porta con
  sé il suo tipo**: le caselle da spuntare (spuntate o no), gli elenchi
  puntati e numerati e il rientro. Incollandole in notE tornano com'erano: le
  righe incollate che erano state copiate intere riprendono il loro tipo, e
  il rientro si conta da quello della riga in cui si incolla. Incollando in
  una riga vuota anche lei diventa quello che era; incollando dopo del
  testo, la prima riga copiata si unisce a quel testo e resta com'è. Una riga
  copiata solo in parte (la selezione che comincia o finisce a metà) è testo
  come prima. Vale col copia della barra sopra la tastiera, col "Copy" del
  menu e col taglia, e per l'incolla in tutti i modi tranne "Keep Text Only".
  Anche dentro un toggle
- **[Progetto]** Il tipo delle righe va negli appunti in un commento dell'HTML
  (`<!--notE-lines:CHECKBOX,1,0;…-->`, `x` per una riga presa in parte), che
  le altre app non mostrano: per loro il testo copiato è quello di sempre.
  `PageEditorViewModel.selectedLines` (il tipo delle righe della selezione),
  `CopiedLine` e `PastedLine` in `RichText.kt`, `PasteFormat.lines`;
  `LinkClipboard` lo scrive e lo rilegge (`PASTED_LINES_TAG`).
  `writeRunChange` dà il tipo alle righe nuove e a quella in cui si incolla
  quando è tutta e solo una riga copiata; `splitBlockAtMultiple` lo stesso
  dentro i toggle (non al titolo di un toggle). Test: `PasteLineTypesTest`,
  e in `ClipboardBarTest` gli appunti andata e ritorno e le righe di solo
  testo, che si copiano come prima
- Provata sul telefono in una riga di prova del Main calendar: tre caselle
  latte (spuntata), pane e uova, copiate e incollate in una riga vuota più
  sotto, sono tornate tre caselle con latte spuntata; nel database le righe
  nuove erano CHECKBOX con la spunta giusta. Riga di prova eliminata:
  database, impostazioni e widget identici a prima
### 🔧 v0.113.2.1 — 19/10/2026 · 17:53 — La pagina segue il cursore e non salta più
*(Claude Code sul PC; verificata sul telefono; 630 test passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** **La pagina si sposta solo se la tastiera coprirebbe il
  cursore, e solo di quanto basta.** Toccando una riga a metà pagina, la
  pagina saliva fino a portare in cima l'inizio del suo pezzo di testo;
  unendo col backspace una riga a quella del pezzo sopra, scendeva fino a
  mettere la fine di quel pezzo appena sopra la tastiera. Colpa della v0.113.1:
  le pagine lunghe sono divise in pezzi di 50 righe, e quando un campo di
  testo prende il cursore (o la tastiera accorcia lo schermo) Compose porta
  in vista **tutto il campo**, cioè tutto il pezzo. Prima, con un campo solo
  alto quanto la pagina, non si notava. Ora chi fa scorrere la pagina sente
  dove sta il **cursore**, non il pezzo: se il cursore si vede, la pagina sta
  ferma; se la tastiera lo coprirebbe, sale finché il cursore è appena sopra
  la barra; andando a capo in fondo lo segue
- **[Progetto]** `CaretInView.kt`: `followCaretOnly` sul riquadro del campo
  (un `BringIntoViewResponder` che a chiunque chieda di portare in vista
  qualcosa del campo risponde col rettangolo del cursore) e `CaretAnchor`, un
  segnaposto grande quanto il cursore che la pagina tiene in vista quando la
  tastiera sale, al posto di tutto il campo. Per questo secondo usa
  `ModifierLocalFocusedBoundsObserver`, interno a Compose: un aggiornamento
  di Compose potrebbe romperlo, e lo direbbe la compilazione. Il cursore ha
  12 dp di spazio sotto (`CARET_KEYBOARD_MARGIN`) e sopra quello dei
  pulsanti sospesi. Test: `CaretInViewTest` (senza: la pagina saltava di
  oltre 250 dp; con: un tocco in vista non muove niente, la tastiera che non
  copre il cursore nemmeno, quella che lo copre fa salire di quanto basta)
- Provata sul telefono in una riga di prova del Main calendar con 120 righe
  (tre pezzi): toccata riga63, a metà schermo, la pagina resta ferma (prima
  saliva fino a riga51); backspace a inizio di riga51 la unisce a riga50 e
  la pagina resta ferma; toccata riga84, che la tastiera avrebbe coperto, la
  pagina sale e la mette appena sopra la barra; tre "a capo" scrivendo, la
  pagina segue il cursore. Riga di prova eliminata: database, impostazioni e
  widget identici a prima. In Permanent notes solo toccate due righe, senza
  scrivere
### ✏️ v0.113.2 — 19/10/2026 · 17:28 — I database da diecimila righe
*(Claude Code sul PC; verificata sul telefono con un database di prova da 10.000 righe; 627 test passati; commit solo in locale)*

Dopo la v0.113.1 l'utente aveva chiesto che l'app reggesse "tante scritte e
database", pensando al suo dizionario di parole studiate con Anki ("arriveranno
anche a 8k, se non più di 10k parole, persino in un database semplice").
Un database disegnava tutte le righe in una volta: con diecimila voleva dire
secondi per aprirlo e rifarle tutte a ogni modifica. Gli sono state proposte
due strade, e ha scelto la prima.

- **🔴 [Da rivedere]** **I database come Notion: dentro una pagina le prime 50
  righe e "Load more"**, che ne aggiunge altre 50, col numero di quelle che
  restano fuori accanto. Vale per la tabella (e per ogni gruppo e
  sottogruppo, ognuno per conto suo), l'elenco e la galleria. Una riga creata
  con "New page" si vede sempre, anche oltre il limite. Tornando sulla pagina
  si riparte dalle prime 50. Calendario, timeline e grafico restano come
  prima. **A pagina intera scorrono tutte**, senza limite: tabella, elenco e
  galleria sono una lista che disegna solo le righe sullo schermo, e "New
  page" è in fondo alla lista ("Freeze first row" tiene ferma l'intestazione,
  una per gruppo). **La bacheca** ha le prime 50 schede per colonna e "Load
  more" anche a pagina intera, come su Notion: non è una lista che disegna
  solo quello che si vede.
  **L'alternativa, se questa non va bene**: tutte le righe scorrevoli anche
  dentro la pagina, senza "Load more", disegnando solo quelle sullo schermo
  come a pagina intera. Da fuori non cambierebbe niente, ma è molto più
  lavoro: dentro una pagina il database è un pezzo della lista della pagina,
  e le sue righe dovrebbero diventare pezzi di quella lista, con gruppi,
  intestazioni ferme, calcoli e lo scorrimento orizzontale delle colonne da
  rifare
- **[Modifica]** **[📱 Mobile]** **Scrivere in un database grande non ferma più lo
  schermo**: il filtro, la ricerca e l'ordinamento di tutte le righe, che si
  rifanno a ogni lettera scritta in una cella, ora girano in background e
  non sul thread che disegna; e l'albero delle sotto-righe, quando nessuna
  riga ne ha, non si costruisce più
- **[Modifica]** **[📱 Mobile]** **L'etichetta rossa "🔴 [Da rivedere]"** nella
  Cronologia, chiesta dall'utente: segna una scelta fatta fra due, da
  riguardare più avanti, con l'altra strada scritta accanto. Nelle Patch
  Notes dell'app la scritta è rossa; su GitHub il colore lo dà 🔴
- **[Progetto]** `RowLimits.kt` (`RowLimits`, `LoadMoreButton`, `LazyBody`,
  `LAZY_FULL_PAGE_LAYOUTS`), `DatabaseViewModel.createdHere`; in
  `DatabaseViewScreen.kt` il ramo `lazy` di `TableLayout` (`LazyColumn`
  dentro lo scorrimento orizzontale, `stickyHeader` per "Freeze first
  row"), `ListRow`, `GalleryRow`, `GroupNewRowButton`, e il limite in
  `BoardColumnCards`. Il calcolo della tabella con `flowOn(Dispatchers.Default)`;
  `rowTree` e `hasSubRows` tornano subito senza sotto-righe. `REVIEW_TAG`
  in `PatchNotesScreen.kt`. Test: `DatabaseRowLimitsTest` (1.200 righe in un
  database vero: le prime 50 e "Load more"; a pagina intera fatte solo
  quelle sullo schermo, anche nell'elenco; le righe create sempre visibili),
  `PatchNotesTest.theReviewTagIsRed`. Per la prova sul telefono, un comando
  della sola versione di prova riempiva di righe un database chiamato
  "Prova velocita" (nessun altro): tolto dopo la prova
- Provata sul telefono, in un database semplice "Prova velocita" da 10.000
  righe dentro una riga di prova del Main calendar: nella pagina le prime 50
  e "Load more 9950", che porta la 51, la 52…; a pagina intera scorrendo in
  fretta solo lo 0,8% dei fotogrammi in ritardo (versione di prova); scrivendo
  e cancellando 30 lettere nel nome di una riga (release), a pagina intera
  5 ms di mediana e 17–25 al massimo (prima di spostare il calcolo in
  background 8 e 18), dentro la pagina 7 e 18. Poi la riga di prova
  eliminata (con la copia di Permanent notes della v0.113.1 e il database):
  database identico a com'era prima delle prove, a parte quella riga, la
  sua pagina, le sue 689 righe e tre voci della sua cronologia; impostazioni
  e widget identici
### ✏️ v0.113.1 — 18/10/2026 · 18:45 — Le pagine lunghe veloci
*(Claude Code sul PC; installata sul telefono e misurata, in parte verificata: il telefono si è scollegato a metà; 622 test passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **Le pagine lunghe si dividono in pezzi**: Permanent
  notes ha 689 righe e 31.600 lettere, ed era tutta un campo di testo solo,
  che a ogni tasto si rifaceva per intero (e la tastiera Samsung se lo
  faceva rimandare tutto). Ora ogni pezzo di 50 righe (o 3.500 lettere) è un
  campo a sé, e scrivendo si rifà solo quello in cui si scrive. Da fuori non
  si vede: i pezzi si toccano senza spazio in mezzo. Il backspace a inizio
  pezzo unisce la riga a quella sopra come prima, Invio e le righe nuove
  restano nel pezzo in cui si scrive, e i pezzi si rifanno solo un attimo
  dopo che si è smesso di scrivere, mai sotto le dita. **"Select all" (e la
  selezione col dito) arriva fino ai bordi del pezzo**, non a tutta la
  pagina (detto all'utente prima di cominciare)
- **[Modifica]** **[📱 Mobile]** **A ogni tasto si ridisegna solo quello che cambia**:
  prima ogni pezzo della pagina già visto, i database dentro la pagina e la
  testata (copertina, icona, titolo, proprietà) si rifacevano a ogni
  lettera; ora ognuno riceve solo quello che mostra e si salta se non è
  cambiato. I numeri degli elenchi si contano una volta per tutta la pagina
  (prima riga per riga, rileggendo la pagina per ogni numero), e il testo di
  ogni riga si legge dal database una volta sola
- **[Modifica]** **[📱 Mobile]** **Sul telefono c'è la versione "release"**, quella che
  si usa davvero: la versione di prova, che c'era finora, è più lenta di
  circa tre volte per come è costruita (misurato). Ha la stessa firma, si
  installa sopra e i dati restano
- **[Progetto]** Le misure, sul telefono (Galaxy S25 Ultra, schermo a 120
  Hz, 8 ms per fotogramma), in una copia di Permanent notes nel Main
  calendar: **scrivere e cancellare 30 lettere**, prima 150 ms per
  fotogramma (99% dei fotogrammi in ritardo), dopo 9 ms di mediana e 19–23
  al massimo; **scorrere a tastiera aperta**, prima 6,8% di fotogrammi in
  ritardo e punte di 48 ms, dopo 3,8% e il 95% entro 9 ms (come a tastiera
  chiusa); la home scorreva già bene (2,6%). Dove andava il tempo l'ha detto
  una traccia di sistema (`atrace`, coi nomi delle funzioni di Compose
  accesi solo per la prova): i profilatori normali sul telefono non
  registrano niente. Codice: `PageEditorViewModel.chunkMembers`,
  `chunkStartsOf`, `rechunk`, `setEditing`, `noteNewChunkLines`,
  `numberedOrdinals`, la cache di `spansOf`; `resolveRun` si ferma ai
  bordi dei pezzi e `mergeIntoIslandAbove` unisce anche all'ultima riga del
  pezzo sopra. Nella pagina: `groupIntoRenderItems(…, chunkStarts)`,
  `RunContent`/`IslandContent`, `PageItemEnv`, `RunItem`/`IslandItem`,
  `pageHeader`, `joinsAbove`/`joinsBelow`. Release firmata con la chiave di
  prova in `build.gradle.kts`. Test: `PageChunksTest` (i pezzi, la
  scrittura in un pezzo, Invio e backspace ai bordi, i pezzi rifatti solo
  quando non si scrive, i numeri in un giro)
- Provata sul telefono nella copia di Permanent notes: i pezzi non si
  vedono, il backspace a inizio del secondo pezzo unisce "32." alla riga
  "31." col cursore nel punto giusto e la tastiera aperta, Invio la ridivide
  e "xyz" finisce davanti a "32.". Poi il telefono si è scollegato, e la
  riga di prova "Untitled" del Main calendar (creata alle 14:28) è stata
  eliminata il giorno dopo, con le prove della v0.113.2 (vedi lì). Prima di
  cominciare Permanent notes era identico a com'era
### 🆕 v0.113 — 18/10/2026 · 14:00 — Il volume dei click del Counter, il backspace negli elenchi come su Word
*(Claude Code sul PC; verificata sul telefono; 616 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La barra del volume nei suoni del Counter**, fra
  l'interruttore e la scelta dei suoni: da 0 a 100%, col numero accanto e le
  icone dell'altoparlante ai capi. Mentre la si trascina cambia il numero;
  lasciandola si salva e si sente il suono del + a quel volume. Il 100% è il
  volume "Sistema" del telefono (sul telefono dell'utente è un volume a sé,
  nelle impostazioni dei suoni), e la nota del pannello ora lo dice. La
  barra segue l'orecchio: a metà si sente circa a metà. Coi suoni spenti è
  grigia. Ogni contatore ha il suo; quelli di prima partono da 100%
- **[Modifica]** **[📱 Mobile]** **Il backspace a inizio di un elemento di elenco, come su
  Word**: il primo toglie il numero o il pallino ma **il testo resta dov'era**,
  allineato agli altri elementi; il secondo lo porta a sinistra (nel punto
  in cui c'era il numero, e con altri backspace si toglie un rientro alla
  volta, poi la riga si unisce a quella sopra, come prima). La numerazione
  dopo continua: tolto il numero al quinto, quello che segue resta 5, come
  nelle foto. Andando a capo da una riga senza numero, anche la nuova resta
  allineata, come su Word. Vale anche negli elenchi dentro un toggle. La
  casella da spuntare resta com'era: il primo backspace la fa diventare
  testo
- **[Progetto]** Counter: `CounterWidget.volume`, `WidgetStore.setCounterVolume`,
  `CounterSounds.gain` (il quadrato della barra) e `play(sound, volume)` con
  `AudioTrack.setVolume`; `CounterVolume` nel pannello. Elenchi:
  `BlockAttrs.listIndent` (l'elenco di cui il paragrafo tiene il posto, nelle
  voci del blocco: nessun cambio al database), `heldListType`,
  `attrsForNewLine`; `PageRepository.makeListText` e `releaseListIndent`, e
  `setBlockType` lo toglie quando il blocco cambia tipo. Nel testo unito il
  marcatore si scrive trasparente (`LinePrefix.hidden`), così il testo resta
  dove sarebbe; dentro un toggle è un `Text` trasparente. Test:
  `ListIndentBackspaceTest` (i due passi, i pallini, la riga nuova, il
  cambio di tipo, il rientro, il toggle, la casella), `CounterVolumeTest`
  (la barra nel pannello), `CounterSoundsTest` (volume e guadagno)
- Prima di installarla l'utente l'ha provata sul telefono, dove c'era ancora
  la 0.112 (il PC aspettava il permesso per il debug USB): il testo andava
  subito a sinistra, com'era prima ("Non funziona"). Riavviato adb il
  permesso è arrivato, e con la 0.113 installata: in una riga di prova del
  Main calendar, un elenco 1–6 di "A"; col cursore fra "5." e la "A" il
  primo backspace toglie il numero e la "A" resta allineata alle altre, col
  cursore davanti, e sotto c'è il 5; il secondo la porta a sinistra, sotto
  sempre il 5 — come nelle foto di Word. Il pannello "Sounds" del Counter
  mostra la barra al 100% con la nota sul volume "Sistema" (solo guardato:
  i suoni scelti dall'utente restano Soft click e Drop). Riga di prova
  eliminata; database identico a prima, nei widget solo i dati scaricati e
  il volume nuovo del Counter a 100
### 🆕 v0.112 — 18/10/2026 · 09:21 — Marcatori e stadio, i suoni del Counter, colonne libere, la riga dopo i database, copia e incolla nella barra
*(Claude Code sul PC; verificata sul telefono; 607 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Nella finestra della partita, sotto il
  risultato, i marcatori come su Sofascore**: quelli della casa a sinistra,
  quelli degli ospiti a destra, col pallone in mezzo; nome intero e minuti
  ("Antonio Raimondo 26', 30', 57'"), rigori e autogol segnati (l'autogol va
  alla squadra che ne approfitta). Sotto, **data e ora, competizione (con la
  bandiera) e stadio**, ognuno con la sua icona; la piattaforma no. Vale per
  le partite in corso, finite e da giocare (che hanno solo la seconda riga).
  La riga in cima con la competizione è sparita: ora sta qui
- **[Nuova funzionalità]** **[📱 Mobile]** **I suoni del Counter**: un click su + e uno più
  lieve su −. L'app li disegna da sé (nessun file da scaricare) e li suona
  come i suoni dei tasti: col telefono in silenzioso non si sentono. Nei tre
  puntini del widget c'è "Sounds": un interruttore per spegnerli e, per + e
  per −, la scelta fra Click, Soft click, Tick, Pop, Drop, Wood, Beep e Coin
  (toccandone uno lo si sente). Ogni contatore ha i suoi
- **[Modifica]** **[📱 Mobile]** **Le colonne dei database si stringono quanto si vuole**,
  trascinando il segnalino, fino a sparire del tutto. Prima avevano un minimo
  (Name 120, quelle in cui si scrive 80, le altre 48); resta solo per "Fit
  to content". Il contenuto di una colonna stretta non sborda più in quella
  accanto
- **[Nuova funzionalità]** **[📱 Mobile]** **Una riga sottile alla fine di ogni database
  dentro una pagina** (semplice o no, anche le viste collegate), come il
  divisore: il testo che segue non sembra più attaccato alla tabella
- **[Nuova funzionalità]** **[📱 Mobile]** **Copia, incolla, taglia e seleziona tutto nella
  barra sopra la tastiera**, fra i sei puntini e la freccia del rientro.
  Fanno quello che fanno Ctrl+C, Ctrl+V, Ctrl+X e Ctrl+A su una tastiera
  vera, sul testo dove sta il cursore. Copiando del testo formattato, negli
  appunti va anche la sua formattazione (grassetto, corsivo, sottolineato,
  barrato, link), anche col "Copy" del menu che si apre tenendo premuto
- **[Nuova funzionalità]** **[📱 Mobile]** **Tenendo premuto "incolla", i tre modi di Word**:
  "Keep Source Formatting" (anche il tocco semplice) toglie la formattazione
  del punto in cui si incolla e mette quella di chi ha copiato — grassetto,
  corsivo, sottolineato, barrato e link, da notE o da un'altra app (OneNote,
  Word, Google Docs, un browser); "Match Destination Formatting" incolla il
  testo con la formattazione del punto in cui arriva; "Keep Text Only" solo
  il testo, senza formattazione. Il "Paste" del menu che si apre tenendo
  premuto resta com'era (i link sì)
- **[Bug fix]** **[📱 Mobile]** **"Select all" prendeva anche l'a-capo nascosto** che ogni
  riga di testo ha in testa (serve a sentire il backspace a inizio riga):
  copiato e incollato in fondo, il testo finiva in una riga nuova invece di
  attaccarsi; tagliato, valeva un backspace a inizio riga. Visto provando la
  barra sul telefono; succedeva anche col "Select all" del menu che si apre
  tenendo premuto. Ora la selezione si ferma al testo vero
- **[Progetto]** Sport: `SportMatch.venue` dal tabellone di ESPN,
  `scorersOf` e `MatchEvent.playerFull` in `SportsMatch.kt`, `ScorersRow` e
  `MatchInfoRow` in `SportsMatchSheet.kt`. Counter: `CounterSounds.kt`
  (onde a 44,1 kHz in `AudioTrack`, uso "sonification"),
  `CounterWidget.soundsOn/plusSound/minusSound`, `CounterSoundsSheet`.
  Colonne: `minColumnWidth` è 0, i minimi di prima in `fitMinColumnWidth`;
  `clipToBounds` sulle celle. La riga dopo il database in
  `PageEditorScreen`. Barra: `ClipboardBarButtons`,
  `View.sendCtrlShortcut` (un tasto senza carattere, così non si scrive la
  lettera), `PasteMode` e `LinkClipboard.nextPasteMode`, `PasteFormat`,
  `applyPaste` e `sliceSpans` in `RichText.kt`, `htmlStyleRuns` (gli stili
  dell'HTML, anche quelli negli `span` come li scrivono OneNote, Word e
  Google Docs). `TextFieldValue.leadOutOfSelection` nei due campi di testo
  della pagina. Test: `ClipboardBarTest` (i quattro pulsanti su un campo
  vero, i tre modi, lo stile copiato in HTML, l'a-capo nascosto fuori dalla
  selezione), `CounterSoundsTest`, `SportsMatchTest` (marcatori, stadio),
  `WrapCellsTest` e `ColumnWidthTest` aggiornati
- Provata sul telefono: Bologna-Inter 1-3 mostra Pobega 10' a sinistra e
  Thuram 44', Carlos Augusto 69', Bonny 83' a destra, poi "17.10.2026 •
  18:00", la bandiera con Serie A e "Renato Dall'Ara". Il pannello "Sounds"
  del Counter si apre con l'interruttore acceso, Click per + e Soft click
  per − (solo guardato: il contatore è rimasto a 0). La riga dopo Notebook,
  Main calendar e 家計簿 nella home. In una riga di prova del Main calendar:
  seleziona tutto, copia, incolla, taglia, e tenendo premuto incolla il menu
  dei tre modi (la tastiera resta aperta), con "Keep Text Only" che attacca
  il testo in fondo alla riga; la riga di prova poi eliminata. Database
  identico a prima delle prove; nei widget cambiano solo meteo e partite
  scaricati e i tre campi nuovi del Counter coi valori di partenza
### 🆕 v0.111 — 18/10/2026 · 08:51 — Il nome della proprietà principale
*(Claude Code sul PC; installata insieme alla v0.112 e verificata sul telefono; 596 test passati; database alla
versione 58; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La proprietà principale ("Aa Name") si può
  rinominare**, database per database: un tocco sull'intestazione della
  colonna, o "Rename" in cima al menu che si apre tenendola premuta. La
  finestrella mostra il nome di adesso; lasciandola vuota torna "Name" (nella
  lingua dell'app). Il nome nuovo compare dappertutto: nell'intestazione
  della tabella, nella finestra della riga di un database semplice, in
  "Sort" e nelle sue regole, nei calcoli in fondo alla colonna, nel grafico
  ("What to show", "Sort by"); e "Fit to content" misura la colonna col nome
  nuovo. Con il database bloccato non si rinomina
- **[Progetto]** `PageEntity.nameColumnTitle`, nel database e non nella
  vista: le viste collegate mostrano (e cambiano) quello della loro origine.
  Database alla versione 58 (`MIGRATION_57_58`: per tutti resta "Name").
  `DbStrings.nameColumn`, `LocalNameColumnTitle` e `nameColumnLabel()` in
  `DatabaseViewScreen.kt`; `DatabaseViewModel.renameNameColumn`. Test:
  `MainPropertyNameTest` (il nome nel database giusto, anche da una vista
  collegata; vuoto torna "Name"; tradotto solo quello dell'app) e
  `MigrationTest.from57TheMainPropertyIsStillName`, con il nuovo
  `schema/v57.sql`
- Provata sul telefono: prima la copia del database (versione 57: 78
  pagine, 152 righe, 798 blocchi), poi l'installazione. Il database passa
  alla 58 con gli stessi numeri, e per tutti resta "Name". In Food un tocco
  su "Aa Name" apre "Rename the main property"; con "Prodotto" cambia
  l'intestazione di entrambi i gruppi (ALDI e IPERPOLI), e svuotato il
  campo torna "Name" (rimesso com'era)
### ✏️ v0.110.2 — 18/10/2026 · 08:40 — I link si aprono in Firefox
*(Claude Code sul PC; verificato sul telefono; 592 test passati; commit solo
in locale)*

- **[Modifica]** **[📱 Mobile]** **Le pagine web si aprono nel browser predefinito del
  telefono** (per l'utente Firefox), con la sua barra degli indirizzi e le
  sue schede, e non più nel riquadro del browser sopra l'app (la "Custom
  Tab" della v0.105). Vale per i link sul testo, i segnalibri, i video e gli
  audio con un indirizzo e le celle URL dei database. Email e numeri di
  telefono si aprono come prima con la posta e il telefono, e un indirizzo
  che ha un'app sua (un video di YouTube) si apre ancora in quella
- **[Progetto]** `BlockFiles.openUrl`: niente più la richiesta di Custom Tab,
  solo l'indirizzo "da aprire nel browser" (`CATEGORY_BROWSABLE`). Test:
  `OpenUrlTest` aggiornato (nessuna Custom Tab)
- Verificato sul telefono, in una riga di prova di Main calendar poi
  cancellata: un segnalibro di Wikipedia si apre in Firefox, e "indietro" da
  Firefox torna alla pagina di notE (nella v0.105, con la scheda normale,
  aveva mostrato un'altra app: oggi no). Alla fine il database è uguale a
  prima della prova
### ✏️ v0.110.1 — 17/10/2026 · 23:45 — "Turn into page" e si resta dove si è
*(Claude Code sul PC; verificato nei test; 592 test passati; commit solo in
locale)*

- **[Modifica]** **[📱 Mobile]** **"Turn into page"** su un database dentro una pagina (dalle
  impostazioni del database o dal menu del blocco) lo trasforma in un
  collegamento alla pagina del database **e basta**: si resta nella pagina in
  cui si era, col collegamento al posto del database. Prima il database si
  apriva anche a schermo intero. Per aprirlo basta toccare il collegamento;
  per rimetterlo dentro la pagina c'è sempre "Turn into database"
- **[Progetto]** `turnDatabaseIntoPage` non prende più l'azione da fare dopo;
  le due voci (`PageEditorScreen`, `BlockActionsSheet`) non aprono più il
  database. Test: `TurnDatabaseIntoPageTest` (il blocco diventa un
  collegamento allo stesso database, le altre righe e il database non
  cambiano)
- Non provato sul telefono: nelle pagine dove si possono fare prove (Food,
  Main calendar) non c'è un database dentro la pagina, e crearne uno di prova
  l'avrebbe lasciato nel Cestino. Installata la mattina dopo, a telefono
  ricollegato: l'app si apre normalmente
### 🆕 v0.110 — 17/10/2026 · 19:49 — Lo sport: la freccia, il risultato in rosso, le stagioni
*(Claude Code sul PC; verificato sul telefono; 591 test passati; commit solo
in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La freccia che aggiorna**, a sinistra dei tre
  puntini del widget dello sport: riscarica subito la competizione che si
  vede (nella stagione scelta) e le giornate guardate scorrendo. Gira mentre
  scarica
- **[Modifica]** **[📱 Mobile]** **Il risultato di una partita in corso è rosso**, come il
  minuto: nel widget, nella finestra della giornata e in quella della
  partita. Quelli delle partite finite restano bianchi
- **[Nuova funzionalità]** **[📱 Mobile]** **La stagione a sinistra del nome**
  ("2026-2027 Serie A"; per NFL, MLB e tennis un anno solo, "2026"). **Un
  tocco** sulla stagione o sul nome apre la scelta: quella in corso e le
  nove prima. Scelta un'altra stagione, il widget mostra le sue giornate
  dall'ultima (scorrendo, tutte le altre), con l'anno in arancione; la
  finestra della giornata ha le partite, la classifica di quell'anno e, nel
  calcio, la scheda nuova **"Players"**: i primi dieci per gol, assist,
  ammonizioni ed espulsioni, con lo stemma della squadra (anche per la
  stagione in corso). Ogni partita si apre come sempre, con la cronaca e le
  statistiche. Riaprendo la barra si torna alla stagione in corso
- **[Progetto]** La stagione la dà ESPN con le partite (`SeasonInfo`,
  `seasonLabel`); un'altra stagione si chiede con un giorno di quella
  stagione (`seasonProbeDay`), la classifica con l'anno; i migliori
  giocatori dall'API "core" di ESPN, coi nomi chiesti a parte, tutti insieme
  (`SportsApi.leaders`). Le altre stagioni restano in memoria finché l'app è
  aperta: le passate non cambiano. I dati salvati prima si riscaricano subito
  per avere la stagione (`SPORT_REPORT_VERSION` 3). Test: `SportsTest`
  (stagioni di Serie A, NBA e NFL; i migliori giocatori della Serie A
  2025-26 veri) e `SportsWidgetTest` (la stagione, la freccia, la scelta)
- Limite: ESPN non dice il numero della giornata. Nelle stagioni già giocate
  ci sono i recuperi a metà settimana (nella Serie A 2025-26 per esempio
  mercoledì 18 febbraio), che fanno un gruppo di date in più: lì "Matchday"
  non si scrive e restano le date; può succedere anche alla stagione in corso
  quando arriverà un recupero
- Verificato sul telefono, sulla Serie A dell'utente: "2026-2027 Serie A" e
  la freccia accanto ai tre puntini (gira toccandola); Bologna-Inter in corso
  con l'1-3 rosso e il minuto rosso; scelta la 2025-2026 (in arancione),
  l'ultima giornata coi risultati, la classifica finale (Inter 87 punti) e i
  giocatori (Lautaro Martínez 17 gol, Dimarco 17 assist, i gialli, i rossi);
  poi di nuovo la stagione in corso. Widget, impostazioni e database come
  prima
### 🆕 v0.109 — 17/10/2026 · 19:33 — Una partita da vicino, come Sofascore
*(Claude Code sul PC; verificato sul telefono; 589 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Un tocco su una partita** (nel widget o nella
  finestra della giornata) apre la partita: in alto le due squadre con lo
  stemma e il risultato (o l'ora d'inizio), il minuto in rosso se è in
  corso. Nel calcio due schede:
  - **"Events"**, la cronaca come Sofascore, dall'ultimo evento al primo, la
    squadra di casa a sinistra e gli ospiti a destra: i gol col minuto, il
    punteggio, il marcatore e l'assist (autogol e rigori segnati a parte);
    i rigori sbagliati; le sostituzioni, chi entra e chi esce; i cartellini
    gialli, rossi e il secondo giallo, col motivo ("Foul", "Handball",
    "Violent conduct"…, tradotti); i gol annullati dal VAR; la fine di ogni
    tempo col punteggio ("HT 3 - 2", "FT 4 - 2") e il recupero annunciato
    ("Additional time 4")
  - **"Statistics"**: possesso palla, tiri, tiri in porta, calci d'angolo,
    fuorigioco, falli, cartellini, parate, passaggi e passaggi riusciti (con
    la percentuale), contrasti, intercetti, con la barra delle due squadre
- **[Nuova funzionalità]** **[📱 Mobile]** Negli altri sport: i punti per quarto o inning
  (e le azioni da punti nella NFL) e le statistiche (NBA: tiri, tiri da
  tre, liberi, rimbalzi, assist, palle rubate, stoppate, palle perse; NFL:
  first down, yard, penalità, possesso). Nel tennis i set dei due giocatori.
  Una partita in corso si aggiorna ogni minuto finché è aperta
- **[Modifica]** **[📱 Mobile]** Il tocco sul titolo della giornata apre ancora la
  finestra con tutte le partite e la classifica; tenendo premuto si
  scelgono le competizioni, come prima
- **[Progetto]** Dal "summary" di ESPN: gli eventi principali e, per il
  recupero annunciato e il VAR, la cronaca. `SportsMatch.kt` (`MatchDetail`,
  `parseMatchSummary`, `MatchDetails` che le tiene in memoria),
  `SportsMatchSheet.kt` (la finestra). I nomi corti dei giocatori ("D.
  Berardi") li fa l'app: ESPN dà quelli interi. Test: `SportsMatchTest`
  (Frosinone-Sassuolo 4-2 vero: gol, assist, punteggi, VAR, recuperi, cambi,
  ammonizione col motivo, statistiche; NBA e NFL per periodo) e
  `SportsWidgetTest.aMatchLikeSofascore` (la partita aperta col tocco)
- Verificato sul telefono, sulla Serie A dell'utente (solo guardando):
  Bologna-Inter in corso all'86', 1-3, con i gol e gli assist (Bonny su
  assist di Augusto, Augusto su assist di Zielinski), i cambi, i gialli col
  motivo, e le statistiche (possesso 33%-67%, passaggi riusciti 244 (80%) e
  586 (92%)). Installata insieme alla v0.110
### 🆕 v0.108 — 17/10/2026 · 19:19 — Incollare coi link; Inter e Roma; stemmi più svelti
*(Claude Code sul PC; nomi e stemmi verificati sul telefono, l'incolla nei
test; 582 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Il testo incollato da un'altra app tiene i suoi
  link**, sulle stesse parole: OneNote, un browser, qualsiasi app che metta
  negli appunti, accanto al testo, l'HTML o un testo coi link. Vale nei
  paragrafi, negli elenchi, nelle caselle, nei riquadri e nelle citazioni,
  anche quando l'incolla fa più righe (i link delle righe nuove restano
  sulle loro righe). Il testo incollato è lo stesso di prima: cambiano solo
  i link. Funziona con "Paste" del menu che si apre tenendo premuto (o
  Ctrl+V); quello che la tastiera incolla dal suo pannello degli appunti
  arriva senza link
- **[Modifica]** **[📱 Mobile]** Nel widget dello sport **"Inter" e "Roma"** invece di
  "Inter Milan" e "AS Roma", nelle partite (anche di Champions) e in
  classifica
- **[Bug fix]** **[📱 Mobile]** **Gli stemmi arrivano prima.** Erano lenti perché ogni
  immagine apriva una connessione nuova verso il server delle immagini di
  ESPN (con la ricerca dell'indirizzo, che col DNS privato del telefono a
  volte è lenta) e la chiudeva subito; e uno stemma non arrivato non si
  riprovava per dieci minuti. Adesso le connessioni si riusano (vale per
  tutti i widget: meteo, sport, borsa), gli stemmi di tutta la competizione
  si scaricano appena la si guarda, e uno mancato si riprova dopo un minuto,
  da solo. Una volta scaricati restano sul telefono e compaiono subito
- **[Progetto]** L'incolla in `PasteLinks.kt`: `LinkClipboard` dà
  all'incolla di Compose il testo con i link segnati sopra; i due campi
  dell'editor li raccolgono (`pastedLinks`) e li mettono nel testo ricco con
  `applyLinks` (`RichText.kt`), anche in `updateRun`, `splitBlockAt` e
  `splitBlockAtMultiple`. I nomi corti in `teamDisplayName` (`Sports.kt`).
  Test: `PasteLinksTest` (appunti con l'HTML come OneNote, testo coi link,
  testo semplice, parole ripetute, link sul testo ricco) e
  `PasteLinksSaveTest` (i link salvati nel testo condiviso su due righe e
  in tre caselle)
- Verificato sul telefono: sulla Serie A dell'utente "Inter" e "Roma", e gli
  stemmi subito, già salvati; con un widget di prova sulla Bundesliga, mai
  vista, quasi tutti gli stemmi in 3 secondi e gli ultimi cinque in 8 (le
  stesse immagini dal PC rispondono subito: il ritardo è del server delle
  immagini di ESPN). Il widget di prova è stato tolto: widget, impostazioni
  e database come prima. L'incolla coi link non è stato provato sul
  telefono: avrebbe voluto dire sovrascrivere gli appunti dell'utente
### ✏️ v0.107.1 — 17/10/2026 · 19:00 — Lo sport a giornate, con stemmi e bandiere
*(Claude Code sul PC; verificato sul telefono; 574 test passati; commit solo
in locale)*

- **[Modifica]** **[📱 Mobile]** **Gli stemmi** a sinistra di ogni squadra, nelle
  partite e nella classifica; nel tennis la bandiera del giocatore. Sono
  quelli di ESPN nella versione per i fondi scuri (la J della Juventus è
  bianca), scaricati piccoli (64×64, circa 4 KB) e tenuti nella cache
  dell'app: ognuno si scarica una volta sola
- **[Modifica]** **[📱 Mobile]** **Il widget mostra una giornata intera** di una
  competizione alla volta, con i giorni separati ("Friday 16", "Today",
  "Tomorrow"), e **scorrendo** si vedono le giornate prima e dopo, tutta la
  stagione. Si apre sulla giornata di adesso (il giorno dopo l'ultima
  partita si vedono ancora i risultati). Le giornate: nel calcio i giorni di
  partita vicini, nella NFL e nel college la settimana ("Week 6"), nel
  basket, nel baseball e nel tennis il giorno (il tennis diviso per torneo).
  Dove il conto torna (Serie A, Serie B, Premier, Bundesliga, Ligue 1)
  "Matchday 7"; altrove (LaLiga con recuperi e anticipi, le coppe) le date,
  col nome della fase se ESPN lo dà ("League Phase")
- **[Modifica]** **[📱 Mobile]** **Le frecce** ai lati del nome cambiano
  competizione: grigia la sinistra sulla prima, la destra sull'ultima,
  tutte e due se ce n'è una sola. Aprendo la barra si vede la prima
- **[Modifica]** **[📱 Mobile]** **La bandiera** a sinistra del nome di ogni
  competizione (nel widget, nella finestra, nell'elenco e nel catalogo):
  🇮🇹 Serie A, Serie B, Coppa Italia, la croce di San Giorgio per la
  Premier, 🇪🇸 🇩🇪 🇫🇷, 🇺🇸 per le leghe americane. Per le competizioni
  continentali il globo col continente: 🌍 Europa e Africa (non c'è
  un'emoji della sola Europa), 🌎 le Americhe, 🌏 l'Asia; per Mondiali, ATP
  e WTA il globo intero 🌐
- **[Modifica]** **[📱 Mobile]** Un tocco su una giornata apre la sua finestra: le partite
  e la classifica, ora con gli stemmi. Si scarica solo la competizione che
  si vede: le giornate due volte al giorno, la giornata di adesso ogni
  cinque minuti (ogni minuto con una partita in corso), le altre quando ci
  si arriva scorrendo; "Refresh" riscarica tutto
- **[Progetto]** ESPN non dice il numero della giornata: le giornate si
  ricavano dal suo calendario (`buildRounds`, `soccerRounds`,
  `currentRound` in `Sports.kt`). Il baseball elenca i giorni senza
  partite; per le coppe, che hanno solo le fasi, i giorni si cercano mese
  per mese. Gli stemmi in `TeamLogos.kt`. I dati salvati dalla versione di
  prima si riscaricano subito (`SPORT_REPORT_VERSION`). Test: `SportsTest`
  (Serie A a 38 giornate, LaLiga, NBA, NFL a settimane, baseball,
  Champions dai mesi, tennis con le bandiere, stemmi e bandiere delle
  competizioni) e `SportsWidgetTest` (la giornata disegnata, le frecce),
  con risposte nuove di ESPN in `resources/sports`
- Verificato sul telefono, sul widget Sports dell'utente (Serie A, solo
  guardando): Matchday 7 da venerdì 16 a lunedì 19 con tutte le partite,
  i risultati, l'intervallo in rosso e gli stemmi; le giornate 6 e 8
  scorrendo; la classifica con gli stemmi; tutte e due le frecce grigie.
  Con un widget di prova (Champions League, ATP, NFL): la League Phase di
  martedì 20 e mercoledì 21, l'ATP di oggi coi tornei e le bandiere, la Week
  6 della NFL, le frecce grigie alla prima e all'ultima. Poi il widget di
  prova è stato tolto: widget, impostazioni e database come prima
### 🆕 v0.107 — 17/10/2026 · 14:50 — I widget dello sport e della borsa
*(Claude Code sul PC; provato sul telefono con internet; 569 test passati;
commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Sports" fra i widget della barra laterale**.
  Una pagina per competizione, fino a dieci, che si sfogliano col dito:
  calcio (Serie A, Serie B, Coppa Italia, Champions, Europa e Conference
  League, Premier League, LaLiga, Bundesliga, Ligue 1, Nations League,
  Mondiali e qualificazioni UEFA), tennis (ATP, WTA), basket (NBA, WNBA,
  NCAA), baseball (MLB) e football americano (NFL, college). Sulla pagina le
  ultime partite finite, quelle in corso col minuto in rosso e le prossime.
  **Un tocco** apre la finestra con due schede: "Matches", tutte le partite
  divise per giorno, e "Standings" (nel tennis "Ranking"), la classifica.
  **Tenendo premuto** si apre l'elenco delle competizioni: "Add a
  competition" in cima, e per ognuna, dal menu ⋮, Show first, Move up,
  Move down e Remove
- **[Nuova funzionalità]** **[📱 Mobile]** **"Stocks" fra i widget**: la lista dei titoli
  come nella foto dell'utente, con sigla, nome, prezzo e variazione del
  giorno in verde o in rosso, fino a trenta titoli. Si cercano per nome o
  sigla (azioni, indici, cambi, fondi), con borsa e tipo nella lingua
  dell'app ("Milan · Equity"). **Un tocco** su un titolo apre i dettagli:
  prezzo e valuta, chiusura precedente, minimo e massimo del giorno e
  dell'anno, ultimo scambio (con data e ora come in Settings). **Tenendo
  premuto** si apre la lista per aggiungere, spostare e togliere i titoli
- **[Nuova funzionalità]** **[📱 Mobile]** Come il meteo, sport e borsa si scaricano **solo
  con la barra aperta**: lo sport ogni cinque minuti (ogni minuto con una
  partita in corso, la classifica ogni mezz'ora), la borsa ogni cinque
  minuti, o subito con "Refresh" nei tre puntini. Senza rete resta l'ultimo
  dato salvato, con "No connection" accanto all'ora
- **[Modifica]** **[📱 Mobile]** **Le città si cercano nella lingua dell'app e in quella
  del telefono**, come ha deciso l'utente: con tutti e due in inglese si
  cerca "Milan"; con uno dei due in italiano anche "Milano" trova Milano,
  Lombardia. Le città trovate vengono **dalla più abitata alla meno
  abitata**, e la stessa città una volta sola
- **[Bug fix]** **[📱 Mobile]** **Lo sport non fa più aspettare minuti**. Sul telefono il
  DNS privato (AdGuard) ogni tanto impiega 16 secondi a trovare un sito, e
  il widget faceva sei domande a ESPN una dopo l'altra: la Serie A ci ha
  messo più di due minuti. Ora le domande partono tutte insieme, e la prima
  volta le partite compaiono appena arriva la prima risposta: la Premier
  League si è caricata in pochi secondi. Anche le ricerche e i prezzi dei
  titoli chiedono tutto insieme
- **[Modifica]** **[📱 Mobile]** Le ricerche di città e titoli mostrano **"Loading…"**
  mentre aspettano la risposta, invece di una finestra vuota
- **[Modifica]** **[📱 Mobile]** Nella finestra delle partite il giorno sta solo nel
  titolo sopra le partite, e la scheda non scelta è grigia. L'avviso "Remove
  this widget?" non elenca più orologi e contatori, che non c'entrano coi
  widget nuovi
- **[Progetto]** Le partite e le classifiche da **ESPN**, i prezzi e la
  ricerca da **Yahoo Finance**: gratuiti, senza account né chiave, ma non
  ufficiali (possono cambiare senza preavviso), e i prezzi possono essere
  in ritardo fino a quindici minuti. `SportsWidget` e la lettura delle
  risposte in `Sports.kt`, le domande in `SportsApi.kt`, il widget in
  `SidebarSports.kt`; la borsa in `Stocks.kt` e `SidebarStocks.kt`; la
  richiesta comune in `WidgetHttp.kt`. Test: `SportsTest`, `StocksTest`,
  `SportsWidgetTest`, `StocksWidgetTest` e un test nuovo in `WeatherTest`,
  con risposte vere salvate in `resources/sports`, `resources/stocks` e
  `resources/weather`
- Provato sul telefono, con internet: il meteo di Milano (cercando "Milan",
  Milano, Lombardia era la prima) e il tempo ora per ora; la Serie A con
  partite e classifica e la Premier League con una partita in corso; la
  ricerca "leonardo", Leonardo (LDO.MI) a +4,73% con i dettagli, e la
  ricerca "enel" in inglese con "Loading…". Poi i tre widget di prova sono
  stati tolti, e i widget, le impostazioni e il database sono tornati
  identici a prima
### 🆕 v0.106 — 17/10/2026 · 14:01 — Il widget del meteo
*(Claude Code sul PC; provato sul telefono senza rete, con la rete dal PC e
nei test; 554 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Weather" fra i widget della barra laterale**.
  Una pagina per città: la principale e fino ad altre nove, che si
  sfogliano col dito. Ogni pagina ha temperatura, cielo, massima e minima
  di oggi, i prossimi cinque giorni e l'ora dell'ultimo aggiornamento. **Un
  tocco** apre il tempo ora per ora: adesso (percepita, umidità, vento),
  poi le prossime 48 ore divise per giorno, con la probabilità di pioggia,
  e i prossimi sette giorni. **Tenendo premuto** si apre la finestra delle
  città: "Add a city" in cima, e per ognuna, dal menu ⋮, Change city, Make
  main city, Move up, Move down e Remove city. Le città si cercano per nome,
  con regione e paese per distinguerle. Le stesse voci, "Cities" e
  "Refresh", stanno nei tre puntini del widget
- **[Nuova funzionalità]** **[📱 Mobile]** Il meteo si scarica **solo con la barra aperta**,
  e per ogni città al massimo ogni mezz'ora (o subito, con "Refresh").
  Senza rete il widget mostra l'ultimo meteo salvato con "No connection"
  accanto all'ora, e la ricerca dice "No connection"
- **[Progetto]** I dati da **Open-Meteo**, gratuito e senza account né
  chiave: le previsioni e la ricerca delle città, coi nomi nella lingua
  dell'app. `WeatherWidget` e la lettura delle risposte in `Weather.kt`, le
  domande in `WeatherApi.kt`, quando riscaricare in `WeatherUpdates.kt`, il
  widget in `SidebarWeather.kt`. Le città e l'ultimo meteo stanno coi
  widget, nelle preferenze del telefono. Il cielo viene dal codice WMO, con
  un'emoji per il giorno e una per la notte e il nome tradotto. Test:
  `WeatherTest`, con due risposte vere di Open-Meteo salvate in
  `resources/weather`, e `WeatherWidgetTest` (città, limite di dieci, e il
  disegno della pagina di Bergamo)
- Provato sul telefono: il widget si aggiunge da "Add widget", e i tre
  puntini hanno "Cities" e "Refresh". Il telefono non aveva internet (Wi-Fi
  spento, nessuna rete attiva): cercando "Bergamo" la ricerca ha detto "No
  connection". Poi il widget è stato tolto, e i widget e il database sono
  tornati identici a prima. Con la rete il meteo è stato provato dal PC
  (Open-Meteo risponde, e le risposte si leggono giuste nei test); sul
  telefono va riprovato quando c'è internet
### ✏️ v0.105.2 — 17/10/2026 · 10:09 — Regole di ordinamento quante se ne vogliono, in "Sort rules"
*(Claude Code sul PC; verificato sul telefono; 548 test passati; database alla
versione 57; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **"Sort rules" in cima alla finestra Sort**, col
  numero di regole. Apre una finestra a parte con tutte le regole, dalla
  prima all'ultima, numerate nell'ordine in cui contano: ognuna ordina solo
  le righe pari per quelle sopra. "Add a sort rule" sta sempre in alto,
  sopra l'elenco, e propone le proprietà non ancora usate; la regola nuova
  va in fondo, in verso crescente, e l'elenco scende fino a lei. Un tocco su
  una regola ne rovescia il verso. Il menu ⋮ ha Move up, Move down e Remove
  sort. In fondo c'è "Remove all sorts". Quando tutte le proprietà sono già
  in una regola, "Add a sort rule" si spegne
- **[Modifica]** **[📱 Mobile]** Sotto "Sort rules", "Sort by" con le proprietà come
  prima: toccandone una diventa la prima regola, al posto di quella che
  c'era; toccando quella che è già la prima se ne rovescia il verso. Accanto
  alle proprietà già usate, il loro posto e verso ("1 ↓"). "Then sort by" e
  "Add a second sort" non ci sono più: la seconda regola è una regola come
  le altre
- **[Progetto]** Tutte le regole in un elenco solo, `pages.sortRulesJson`
  (`SortRuleSpec`, `SortRules.kt`). La migrazione 56→57 sposta la prima e la
  seconda regola di ogni pagina nell'elenco, nello stesso ordine e verso, e
  svuota i campi di prima (`sortColumnId`, `thenSortColumnId`): restano nella
  tabella, ma non li legge più nessuno. Non è servito cancellare nessuna
  regola: le cinque che c'erano (Videogames, Food e la sua vista nella
  pagina iniziale, Pokédex, Habit tracker 2026) sono passate tutte. Nel
  ViewModel `addSortRule`, `toggleSortRule`, `moveSortRule`,
  `removeSortRule` e `clearSortRules`; `setSort` e `setThenSort` lavorano
  sull'elenco; le regole su proprietà cancellate cadono alla prima modifica.
  Test: `SortRulesTest`, `ManySortRulesTest` (con quattro regole conta anche
  la quarta), `MigrationTest.from56TheTwoSortRulesMoveIntoTheList` con
  `schema/v56.sql`, e `SecondSortTest` aggiornato
- Verificato sul telefono, con una copia di sicurezza del database fatta
  prima. Dopo l'aggiornamento i cinque ordinamenti sono nell'elenco, con lo
  stesso ordine e verso. In Food "Sort rules" diceva "2 rules"; aggiunte
  Name, Supermercato e Tipologia prodotto (cinque regole, e "Add a sort
  rule" spento), Name rovesciata, Tipologia prodotto spostata su: salvate in
  quell'ordine. Tolte le tre di prova, Food è tornato com'era. Il tasto
  indietro chiude tutta la finestra, come le altre finestre che salgono dal
  basso; per tornare alla pagina prima c'è la freccia ← in alto. A fine
  prova il telefono si era scollegato: questa versione è stata installata
  quando l'utente l'ha ricollegato. Rispetto a quella provata cambiano solo
  due righe tolte (un `BackHandler` che nella finestra non poteva
  funzionare) e il numero di versione
### ✏️ v0.105.1 — 17/10/2026 · 09:46 — Anche segnalibri, video e celle URL si aprono sopra l'app
*(Claude Code sul PC; verificato sul telefono; 539 test passati; commit solo
in locale)*

- **[Modifica]** **[📱 Mobile]** **Segnalibri web, video e audio con un indirizzo e le
  celle URL, email e telefono dei database si aprono come i link sul
  testo**: le pagine web nel riquadro del browser sopra l'app, e "indietro"
  torna a notE. Prima si aprivano come scheda normale del browser, e
  "indietro" poteva mostrare un'altra app. Un indirizzo che ha un'app sua
  (un video di YouTube con l'app YouTube) si apre ancora in quella. Anche i
  file (video, PDF…) aperti "con un'altra app" partono ora da notE, così
  "indietro" dal lettore torna qui
- **[Progetto]** Un modo solo di aprire gli indirizzi: `BlockFiles.openUrl`,
  che apre come Custom Tab (fino alla v0.105 stava in `TextLinks.kt`). Lo
  usano `rememberLinkOpener` dei link sul testo e `openLink` delle celle.
  Senza un'attività, che nell'app non succede, chiede un'attività nuova come
  prima. Test: `OpenUrlTest`
- Verificato sul telefono in una riga di prova di Main calendar, poi
  cancellata: un segnalibro di example.com si apre nel riquadro di Firefox,
  e "indietro" torna alla pagina. Alla fine il database del telefono è
  uguale a prima della prova. Video e celle URL passano dalla stessa
  funzione, provata dai test; sul telefono non li ho aperti, perché un video
  di YouTube avrebbe aperto l'app YouTube
### 🆕 v0.105 — 17/10/2026 · 09:39 — I link sul testo selezionato
*(Claude Code sul PC; verificato sul telefono; 536 test passati; commit solo
in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **L'icona del collegamento nella barra sopra la
  tastiera**, accanto al pennello. Compare quando c'è del testo selezionato
  (tenendo premuta una parola) o quando il cursore sta dentro un link, e
  apre la finestrella "Link" dove incollare o scrivere l'indirizzo:
  "google.com" basta (diventa https://google.com), un indirizzo email
  diventa un link per scrivere una mail. Se il testo ha già un link, la
  finestrella lo mostra e ha "Remove link". Col solo cursore dentro un link
  si cambia o si toglie il link intero
- **[Nuova funzionalità]** **[📱 Mobile]** **Il testo con un link è blu e sottolineato, e
  toccandolo si apre**, senza mettere il cursore né aprire la tastiera.
  Tenendolo premuto, invece, si seleziona come sempre, per cambiare o
  togliere il link. Le pagine web si aprono nel riquadro del browser sopra
  l'app (Custom Tab), e "indietro" torna alla pagina di notE. Scrivendo
  subito prima o subito dopo un link, il testo nuovo non entra nel link;
  scrivendo dentro sì. Un link dentro uno spoiler coperto non si apre: il
  tocco lo scopre, come sempre
- **[Progetto]** `RichTextSpan.link`: il testo salvato prima non ha la
  chiave e vale null, niente migrazione. In `RichText.kt` `setLinkInRange`,
  `linkInRange` e `linkRunAt`; il nuovo `TextLinks.kt` ha il colore, il
  segno sul testo mostrato, `linkTaps` (prende il tocco prima del campo),
  `LinkDialog` e l'apertura come Custom Tab, senza aggiungere la libreria
  androidx.browser. Nel ViewModel `selectionLink`, `capturedLink` e
  `applyLinkToCapturedSelection`, sulla selezione messa da parte come per il
  pennello. Vale nei due campi dell'editor: le righe di testo, elenchi e
  caselle, e i blocchi a sé come citazioni, toggle e callout. Test:
  `RichTextLinkTest`, `TextLinkTest` e `TextLinkTapTest` (fallisce se il
  tocco sul link arriva al campo)
- Verificato sul telefono in una riga di prova di Main calendar, poi
  cancellata. Tenendo premuto "Google" compare l'icona; con "example.com"
  "Google" diventa blu e sottolineato e resta così dopo aver riaperto l'app.
  Toccandolo si apre la pagina, e "indietro" torna a notE senza tastiera;
  "Remove link" lo toglie; lo stesso in una citazione. Il primo tentativo
  apriva il link come scheda normale di Firefox, e "indietro" mostrava
  l'app usata prima (YouTube) invece di notE: da qui la Custom Tab. Alla
  fine il database del telefono è uguale a prima della prova
### 🔧 v0.104.1.1 — 17/10/2026 · 09:14 — Il punto in alto sta proprio in alto: 2˙026
*(Claude Code sul PC; verificato sul telefono; 526 test passati; commit solo
in locale)*

- **[Bug fix]** **[📱 Mobile]** **La quarta scelta di "Number format" usava "·", il punto a
  metà altezza**: ora è "˙", in cima alle cifre. 2˙026, 1˙000˙000 e coi
  decimali 1˙234,56
- **[Progetto]** `NumberStyle.MIDDLE_DOT_GROUPS` diventa `HIGH_DOT_GROUPS`
  (U+02D9). Nessuno l'aveva scelta, quindi il nome nuovo non cambia
  niente nelle impostazioni salvate. `NumberStyleTest` aggiornato
- Verificato sul telefono: nel menu e nella colonna Year di "Habit tracker
  2026" il punto sta all'altezza della cima delle cifre. Poi rimessa la
  scelta dell'utente, 2026 senza separatore
### ✏️ v0.104.1 — 17/10/2026 · 09:05 — I numeri con più di tre cifre: 2 026, 2026, 2.026 o 2·026
*(Claude Code sul PC; verificato sul telefono; 526 test passati; commit solo
in locale)*

- **[Modifica]** **[📱 Mobile]** **"Number format" in Settings ha cinque scelte
  invece di due**: 1 000 000, 1000000, 1.000.000, 1·000·000 e, come prima,
  1,000,000. Sotto ognuna lo stesso numero coi decimali: con le prime
  quattro i decimali vanno dopo la virgola (1 234,56), con l'ultima dopo il
  punto (1,234.56). Vale in tutta l'app: celle Number, calcoli in fondo
  alle colonne, grafici, filtri, conteggio delle parole, widget. Lo spazio
  non va a capo. Il numero salvato non cambia: cambia solo come si legge
- **[Progetto]** `NumberStyle` dice il segno fra i gruppi di tre cifre
  (`grouping`, null = nessuno) e quello dei decimali; `Formats.number(raw,
  style)` scrive anche gli esempi del menu. Test: `NumberStyleTest`
- Verificato sul telefono: il menu mostra le cinque scritture; in "Habit
  tracker 2026" la colonna Year si legge "2·026", "2 026" e "2026" a
  seconda della scelta. Poi rimesso 1.000.000, com'era
### 🆕 v0.104 — 17/10/2026 · 08:50 — Nella pagina di una riga, le frecce per la pagina prima e quella dopo
*(Claude Code sul PC; verificato sul telefono; 523 test passati; commit solo
in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **˄ e ˅ in alto a destra nella pagina di una
  riga**, accanto ai tre puntini, come le frecce di Notion: la pagina prima
  e quella dopo dello stesso database. L'ordine è quello della vista da cui
  la si è aperta: filtro, ordinamento, ricerca, gruppi e sottogruppi della
  tabella (con le sotto-righe aperte sotto la loro riga), colonne e fasce
  della bacheca, date del calendario e della timeline (e in fondo le righe
  senza data). In cima ˄ è spenta, in fondo ˅. La pagina nuova prende il
  posto di quella di prima: "indietro" torna al database, non a tutte le
  pagine scorse
- **[Progetto]** `RowNavigation.kt`: l'ordine si fotografa quando si apre
  la riga dalla vista (`displayedRowIds`, una regola per vista, che usa
  `buildRowGroups` e `rowTree` come le viste stesse) e resta quello mentre
  si scorre. Aprendo la pagina da altrove (ricerca, collegamenti, dopo aver
  chiuso l'app) vale il filtro e l'ordinamento del database. Una riga che
  compare in più gruppi (selezione multipla) conta la prima volta. Test:
  `RowOrderTest` (tabella semplice, con gruppi e sottogruppi, bacheca con
  colonne e fasce, calendario)
- Verificato sul telefono il 16 e il 17/10, in Main calendar con tre righe di
  prova "Prova 1–3" (poi tolte): con la ricerca "Prova", da "Prova 2" ˅ →
  "Prova 3" con ˅ spenta; ˄ due volte → "Prova 1" con ˄ spenta; "indietro"
  torna subito alla tabella. Con l'ordinamento per nome decrescente, da
  "Prova 2" ˅ → "Prova 1". Tolti l'ordinamento e le righe di prova; nel
  database del telefono restano solo le modifiche fatte nel frattempo
  dall'utente
### ✏️ v0.103.3 — 16/10/2026 · 17:40 — Anche "Time format" in Settings, uno solo per tutta l'app
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 519 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **"Time format" in Settings**, subito sotto
  "Date format": **Hidden**, **12 hour**, **24 hour**, con l'ora di adesso
  scritta così. 12 hour vale in tutta l'app: le date dei database, la
  cronologia, "Created" e "Last edited", preferiti, cestino, quaderni, le
  righe "/notification", gli eventi e le ore del giorno nel calendario
  ("3 PM"), le durate, la fine dei timer. **Hidden** toglie l'ora dalle date
  dei database; dove l'ora è quello che si vuole leggere (cronologia,
  "Created", timer…) resta a 24 ore. Le rotelle di "Set time" restano a 24
  ore. Di partenza 24 hour, come avevano tutte le colonne
- **[Rimosso]** **[📱 Mobile]** **"Time format" da "Edit property" delle colonne Date**: lì
  ora c'è solo il nome
- **[Progetto]** `AppSettings.timeFormat` (`TimeFormatKind`);
  `Formats.timeFormatter`, `hourLabelFormatter` e
  `databaseDateTimeFormatter` al posto dei formati scritti a mano nei vari
  file. Via `DateSettings` e `DateSettingsSection`;
  `database_columns.timeFormat`, come `dateFormat`, resta nel database ma
  non si legge più. Test: `GlobalTimeFormatTest` (24 ore, 12 ore e "3 PM",
  Hidden solo nei database). Il test cambiava le impostazioni fuori da
  un'istantanea di Compose, e i test grafici dopo di lui restavano ad
  aspettare: ora le cambia dentro, e il giro completo torna a 44 secondi
- Verificato sul telefono: Settings → "Time format — 24 hour", la lista
  Hidden ("No time in database dates"), 12 hour (5:37 PM), 24 hour (17:37);
  con 12 hour Main calendar → "2026年11月15日 3:00 PM", con Hidden
  "2026年11月15日" con la campanella; "Edit property" della colonna Date ha
  solo il nome. Rimesso 24 hour: database identico a prima delle prove
### ✏️ v0.103.2 — 16/10/2026 · 17:15 — "Date format" torna in Settings, uno solo per tutta l'app
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 518 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **"Date format" in Settings**, dopo "Number
  format", come prima della v0.64: i nove formati (Full date, Short date,
  Month.Day.Year, Month/Day/Year, Day.Month.Year, Day/Month/Year,
  Year.Month.Day, Year/Month/Day, Chinese date), ognuno con la data di oggi.
  Vale **ovunque**: celle, liste, calcoli (Earliest, Latest…), riassunto del
  filtro, grafici, cronologia, preferiti, cestino, quaderni. Quello scelto
  resta quello di prima: sul telefono "Chinese date"
- **[Rimosso]** **[📱 Mobile]** **"Date format" da "Edit property" delle colonne Date**.
  Resta "Time format" (Hidden, 12 hour, 24 hour), che vale per la colonna.
  L'unica colonna che se n'era scelto uno suo, "Release date" di
  Videogames ("Full date"), ora segue quello di Settings
- **[Progetto]** `DateSettings` ha solo l'ora; `database_columns.dateFormat`
  resta nel database ma non si legge più, e salvando la colonna si svuota.
  Test: `GlobalDateFormatTest`
- Verificato sul telefono: Settings → "Date format — Chinese date
  2026年10月16日", la lista dei nove con la spunta su Chinese date; scelto per
  prova Day/Month/Year, le date di Main calendar sono diventate
  "15/11/2026 15:00"; in "Edit property" della colonna Date c'è solo "Time
  format". Rimesso Chinese date: database e impostazioni del telefono sono
  identici a prima delle prove
### ✏️ v0.103.1 — 16/10/2026 · 17:04 — L'ora della data resta quella scelta, e la notifica non va più alle 9:00
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 516 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** **"Set time" teneva l'ora di prima** se si premeva "Done" mentre
  la rotella si stava ancora fermando: dopo un colpo veloce sui minuti
  restavano quelli di partenza (le 9:00, o le 15:00). Ora "Done" prende i
  numeri che si vedono in mezzo. Vale per ogni "Set time" dell'app: date
  dei database, eventi del calendario, durate, righe "/notification"
- **[Modifica]** **[📱 Mobile]** **Niente più 9:00 da sole**: accendendo "Include
  notification" si accende anche "Include time", e spegnendo l'ora si spegne
  la notifica, così la notifica suona sempre all'ora scelta. L'ora proposta,
  quando non ce n'è una, è quella di adesso e non più le 9:00 (la fine un'ora
  dopo). Le date salvate prima con la notifica e senza ora restano com'erano
  (alle 9:00) finché non le si cambia: nel database del telefono è una sola,
  "Picross S Climax" in Videogames
- **[Progetto]** `WheelPicker` ha `onLiveValue`, il numero in mezzo a ogni
  momento (`snapshotFlow`), e `SamsungTimeDialog` lo usa per ore e minuti.
  Test: `TimeDialogTest` (colpo sui minuti, "Done" a metà corsa: i minuti non
  sono più quelli di partenza), che senza la correzione fallisce
- Verificato sul telefono su una riga di prova in Main calendar (poi tolta):
  spenta l'ora si spegne la notifica, accesa la notifica si accende l'ora;
  colpo veloce sui minuti e "Done" subito, con la rotella su 15:07 → "Start
  time 15:07", e nel database 20/10/2026 15:07 con ora e notifica. Prima
  della correzione lo stesso gesto aveva dato 09:05. Il database del telefono
  è identico a prima delle prove
### 🆕 v0.103 — 14/10/2026 · 11:35 — "---" fa un divisore, e "Move up"/"Move down" per immagini, file e divisori
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 515 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"---" scritto da solo in una riga di testo la
  fa diventare un divisore**, come su Notion: i trattini spariscono e il
  cursore va sulla riga nuova sotto. Vale anche con la lineetta lunga al
  posto dei primi due trattini, e nelle righe dentro un toggle. Su elenchi
  e caselle, o in mezzo ad altro testo, i trattini restano scritti.
  Annulla toglie il divisore e rimette "--"
- **[Nuova funzionalità]** **[📱 Mobile]** **"Move up" e "Move down" anche per immagini,
  video, audio, file, segnalibri, "Link to page" e divisori**, sopra "Add
  above", tenendoli premuti; grigi in cima e in fondo
- **[Bug fix]** **[📱 Mobile]** Un divisore teneva nel database il testo che aveva prima
  (invisibile, ma contato nella ricerca): ora diventando divisore il blocco
  si svuota sempre
- **[Progetto]** `dividerShortcutJustTyped` (come `numberedListPrefixJustTyped`
  per "5."), `PageEditorViewModel.dividerShortcutBlock`, poi la stessa voce
  "Divider" del menu "/"; `MoveGroup` in `ObjectBlockMenu` e
  `DividerBlockMenu`; `turnIntoDividerWithLineBelow` svuota sempre il testo.
  Test: `DividerShortcutTest`
- Verificato sul telefono in una pagina di prova in Main calendar (poi
  tolta): "Uno", a capo, tre trattini → divisore, e "Due" scritto sotto;
  tenuto premuto il divisore, "Move down" → sotto "Due"; un secondo "---"
  ha il testo vuoto nel database; Annulla → "--". Il database del telefono
  è identico a prima delle prove
### ✏️ v0.102.3 — 14/10/2026 · 11:22 — "Move up" e "Move down" anche per database e pagine
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 512 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **"Move up" e "Move down"** in cima al menu che
  si apre dai sei puntini di un database dentro una pagina e tenendo
  premuto il collegamento a una pagina, sopra "Add above": il blocco
  scambia il posto con quello sopra o sotto, come fanno le frecce della
  barra per le righe di testo. In cima "Move up" è grigio, in fondo "Move
  down". Il menu si chiude, e la pagina porta in vista il blocco spostato
- **[Progetto]** `MoveGroup` in `BlockActionsSheet.kt`, nel menu di pagine e
  database (`PageBlockMenu`); usa `PageEditorViewModel.moveBlockUp` e
  `moveBlockDown`, gli stessi delle frecce. Test: in `MoveBlockTest` il
  database che sale in cima e riscende fino in fondo
- Verificato sul telefono, in una pagina di prova in Main calendar (poi
  tolta, con le pagine che conteneva): "Sopra", un database, "Sotto". Dai sei
  puntini del database "Move up" → il database sopra "Sopra", e riaprendo il
  menu "Move up" è grigio; due volte "Move down" → in fondo, e "Move down"
  grigio. Una sottopagina tenuta premuta → "Move up" → sale sopra "Sotto".
  Il database del telefono è identico a prima delle prove
### ✏️ v0.102.2 — 14/10/2026 · 11:07 — Il "+" cambia tutte le righe selezionate, non solo quella col cursore
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 511 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **Con del testo selezionato, il "+" della barra
  cambia tutte le righe che la selezione tocca**, nell'ordine in cui sono:
  tre paragrafi selezionati e "Numbered list" fanno 1, 2, 3; "To-do list"
  fa tre caselle. Vale qualunque tipo avessero (testo, elenco puntato o
  numerato, casella) e qualunque font o corpo abbia il testo, che resta
  com'era. Le voci sono Text, Bulleted list, Numbered list, To-do list,
  Toggle list, Callout, Quote e Code: ogni riga diventa la sua. Notification,
  Table e le altre restano della riga col cursore, come prima. Senza
  selezione, o dentro una riga sola, cambia la riga col cursore. **Annulla**
  le rimette tutte com'erano in un passo solo
- **[Progetto]** Quali righe tocca la selezione: quelle di cui è selezionato
  almeno un carattere, e quelle vuote che le stanno dentro; una selezione che
  finisce proprio all'inizio di una riga non la prende (si è selezionato fino
  all'a-capo prima). Il "+" mette da parte la selezione quando si apre, come
  il pennello (`captureSelection`); `capturedSelectionBlocks` e
  `updateBlockTypes` nel ViewModel, `PageRepository.setBlockTypes` cambia le
  righe in una transazione sola (si vedono cambiare tutte insieme). Test:
  `TurnIntoManyTest` (quali righe, numerate in ordine col testo intatto e
  Annulla in un passo, quattro caselle)
- **[Progetto]** Sul menu della lista numerata: **c'è ancora**, tenendo
  premuto **sul numero** ("Begin a new list here", "Renumber as continuation
  of previous list", "Remove number", "Change to bullet"). Provato sul
  telefono in una pagina nuova e in "Permanent notes" (sul "3.", sul "iv."
  rientrato, e con la tastiera aperta): si apre ogni volta. Riprovato
  anche dall'utente: funziona
- Verificato sul telefono in una pagina di prova in Main calendar (poi
  tolta): "Uno, Due, Tre, Quattro", selezionato da "Uno" all'inizio di "Tre"
  e scelto Numbered list → "1. Uno, 2. Due", Tre e Quattro restano testo;
  poi "Select all" e To-do list → quattro caselle, anche le due numerate;
  Annulla → di nuovo "1. Uno, 2. Due, Tre, Quattro". Il database del telefono
  è identico a prima delle prove
### ✏️ v0.102.1 — 14/10/2026 · 10:51 — La cella su cui si scrive sale sopra la tastiera, con un movimento solo
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 508 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **La cella su cui si scrive resta sempre sopra
  la tastiera**, intera, col suo bordo e un po' di spazio: mentre la
  tastiera sale, la tabella sale con lei, in un movimento solo che
  accelera e rallenta e finisce insieme alla tastiera. Vale per ogni campo
  di un database: le celle, i nomi delle righe, il titolo, nella tabella a
  pagina intera, nel database dentro una pagina e nelle proprietà di una
  riga aperta come pagina. Chiudendo la tastiera tutto torna giù al suo
  posto
- **[Bug fix]** **[📱 Mobile]** **A pagina intera la tastiera passava sopra le righe**: la
  parte che scorre restava alta come lo schermo, e le celle in fondo si
  scrivevano alla cieca (le foto dell'utente). Ora si accorcia insieme alla
  tastiera
- **[Bug fix]** **[📱 Mobile]** **Dentro una pagina la cella restava mezza coperta**: la pagina
  saliva, ma solo fino a far vedere il testo, e il bordo della cella
  restava dietro la tastiera
- **[Progetto]** `keepFocusedAboveKeyboard` (`KeepAboveKeyboard.kt`), messo
  dentro quello che scorre. Il riquadro che scorre si accorcia con la
  tastiera (`imePadding` a pagina intera, come già l'editor delle pagine), e
  Compose insegue da sé il campo col fuoco; in più, a ogni fotogramma della
  salita, si chiede di vedere il campo con 24 dp sopra e sotto, e Compose
  unisce la richiesta all'animazione già in corso. La tastiera si legge
  fuori dal disegno (`snapshotFlow`), per non ridisegnare tutto il database
  a ogni fotogramma. Il margine non supera il contenuto che c'è sotto e
  sopra il campo (chiedere di scorrere oltre la fine fa interrompere a
  Compose l'animazione); i campi più alti di un terzo dello schermo si
  lasciano stare. La richiesta riparte anche quando il fuoco passa a
  un'altra cella con la tastiera aperta (questo non provato sul telefono).
  Una prima versione chiedeva il margine solo a tastiera ferma: si vedevano
  due movimenti, con due fotogrammi di pausa in mezzo
- Verificato sul telefono registrando lo schermo a 120 Hz e misurando di
  quanto si sposta la tabella a ogni fotogramma. **A pagina intera**, Food,
  cella "Order" di "Capsule lavastoviglie" (l'ultima): la tastiera sale in
  0,23 s e la tabella con lei, 4 → 12 → 16 → 24 → 28 px per fotogramma, poi
  giù fino a 4 e 0, senza salti né pause; alla fine la riga è tutta sopra la
  tastiera, col bordo e lo spazio. Prima la tastiera la copriva del tutto.
  **Nella pagina** (My Personal Workspace, cella di "Fette di pane"): stesso
  movimento unico, e la cella intera in vista invece che tagliata a metà.
  Chiudendo la tastiera la tabella torna giù. Nessun valore scritto: il
  database del telefono è identico a prima delle prove
### 🆕 v0.102 — 14/10/2026 · 10:28 — I sottogruppi anche nella Board, e "Group by" per quasi ogni proprietà
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 508 test
passati; database alla versione 56, copia del database del telefono fatta
prima di installare; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Sub groups" nella Board**, sotto "Group by",
  come su Notion: la bacheca si divide in **fasce orizzontali**, una per
  valore della seconda proprietà, e ogni fascia ha tutte le colonne della
  bacheca con le sue schede. I nomi delle colonne stanno una volta sola, in
  cima; il nome della fascia, col numero delle sue schede, resta sul bordo
  sinistro mentre le colonne scorrono, e toccandolo la fascia si chiude e si
  riapre. "New" dentro una fascia crea una scheda già col valore della
  colonna **e** quello della fascia. Sulle schede non si ripetono né il
  valore della colonna né quello della fascia
- **[Nuova funzionalità]** **[📱 Mobile]** **"Hide empty sub groups"** per le fasce della
  Board, acceso di partenza, a sé da quello della tabella
- **[Modifica]** **[📱 Mobile]** **"Group by" della Board offre le stesse proprietà del
  raggruppamento della tabella**: anche casella, data, numero, testo,
  selezione multipla, URL, email, telefono — non più solo quelle a
  selezione singola. Una casella fa due colonne, "Checked" e "Unchecked";
  con la selezione multipla una scheda sta nella colonna di ognuno dei suoi
  tag. Non ci sono colore, immagine, collegamento a pagina, durata, "Created
  time" e "Last edited time", che non danno nomi leggibili o fanno un gruppo
  per riga. Senza scelta resta la prima a selezione singola, come prima
- **[Bug fix]** **[📱 Mobile]** Nella Board una scheda con un tag poi cancellato dalla
  proprietà spariva: ora ha la sua colonna, dopo quelle dei tag, come nella
  tabella
- **[Bug fix]** **[📱 Mobile]** Nella tabella raggruppata per una data, "New page" dentro un
  gruppo creava la riga al **1° gennaio 1970**, in un gruppo tutto suo: ora
  nasce nel giorno del gruppo. Lo stesso vale per la Board
- **[Progetto]** Sulla domanda: **non era un bug ma un limite voluto**. La
  Board accettava apposta solo le proprietà a selezione singola, e in Food
  dopo il 09/10 "Tipologia prodotto" è diventata a selezione multipla: con
  "Order" (numero) e "Checkbox" restava fuori, e rimaneva solo
  "Supermercato"
- **[Progetto]** Database alla **versione 56**: `pages.boardSubGroupColumnId`
  e `boardHideEmptySubGroups` (`MIGRATION_55_56`), copiati anche duplicando
  una pagina e togliendo la prima vista. `ColumnType.canGroup` dice per
  quali proprietà si raggruppa, per tabella, sottogruppi e Board. La Board
  prende colonne e fasce da `buildRowGroups`, lo stesso dei gruppi della
  tabella; la pagina "Sub groups" delle impostazioni è una sola per tabella
  e Board. Test: `BoardSubGroupTest` (ViewModel), `BoardLayoutTest` (la
  bacheca disegnata: schede nella colonna e nella fascia giuste, fasce che
  si chiudono, "New" coi due valori, colonne per casella), e la migrazione
  dalla 55 in `MigrationTest` (`schema/v55.sql`)
- Verificato sul telefono, nella vista Board di Food: "Group by" offre
  Supermercato, Tipologia prodotto, Order e Checkbox; "Sub groups" le stesse
  tranne Supermercato. Con le fasce per "Tipologia prodotto" si vedono
  Bagno (13), Cucina (4), Bevande (1), Colazione (6)…, ognuna con ALDI,
  IPERPOLI e "No Supermercato"; scorrendo a destra il nome della fascia
  resta a sinistra; toccandolo "Bagno" si chiude. "New" in Cucina sotto ALDI
  ha creato una riga con ALDI e Cucina (poi tolta). Raggruppata per
  Checkbox: "Checked" 44 e "Unchecked" 0. Rimesse le impostazioni di prima
  e la vista Table: il database del telefono è com'era (cambia solo l'ora di
  modifica della vista Board)
### ✏️ v0.101.3 — 09/10/2026 · 09:03 — Food: la riga spuntata scende dopo 0,2 secondi
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 495 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** La riga appena spuntata (o tolta) va al suo posto **0,2
  secondi** dopo che la sua casella si è riempita, non più un secondo dopo.
  Spuntando più cose di fila restano ferme finché si continua, e poi
  scendono insieme
- **[Progetto]** `CHECKBOX_SETTLE_MS` a 200. Con un attimo così corto, un
  salvataggio lento avrebbe fatto partire la riga prima che la casella si
  riempisse, e lo scatto della v0.101.1 sarebbe tornato: per questo gli 0,2
  secondi ora si contano **da quando il valore nuovo arriva dal database**
  (`holdAfterCheckbox` aspetta di vederlo nella tabella, al più 2 secondi),
  non dal tocco. `CheckboxHoldTest` usa la costante
- Verificato sul telefono registrando lo schermo, su "Dentifricio" in
  Food: la casella si riempie 0,15 s dopo il tocco, e a 0,28 s la riga è
  già scesa fra le prese; nella colonna delle caselle nessun altro
  movimento. Tolta la spunta, risale dopo "Shampoo". Il database del
  telefono è tornato com'era (cambia solo l'ora di modifica della riga)
### ✏️ v0.101.2 — 09/10/2026 · 08:56 — Food: la riga spuntata scende al suo posto dopo un secondo
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 495 test
passati; commit solo in locale)*

La v0.101.1 teneva ferme le righe finché si restava sulla pagina: niente
più caselle che scattavano, ma nemmeno la presa che scende in fondo.

- **[Modifica]** **[📱 Mobile]** **Un secondo dopo l'ultima spunta** le righe vanno al
  loro posto: la casella si riempie dove la si è toccata, e poco dopo la
  riga scende fra le prese (o risale, togliendo la spunta). Spuntando più
  cose di fila, a meno di un secondo l'una dall'altra, restano tutte ferme
  e poi scendono insieme: sotto il dito c'è sempre la riga che si vede.
  Quando le righe si spostano ognuna porta con sé la sua casella, quindi
  non si vede nessuna casella svuotarsi e riempirsi da sola. Uscendo dalla
  pagina, o cambiando ordine e filtro, vanno subito al loro posto
- **[Progetto]** `DatabaseViewModel.holdAfterCheckbox` fa partire un conto
  alla rovescia (`CHECKBOX_SETTLE_MS`, un secondo) che ricomincia a ogni
  spunta e alla fine chiama `releaseCheckboxHold`. Test: un caso in più in
  `CheckboxHoldTest` (spunta, 0,6 s ancora ferma, un'altra spunta, altri
  0,6 s ancora ferme, poi giù insieme)
- Verificato sul telefono registrando lo schermo, su "Dentifricio" in Food:
  toccata, la casella si riempie in 0,1 s dov'è; 1,1 s dopo il tocco i nomi
  cambiano posto e "Dentifricio" è fra le prese, dopo "Bagnoschiuma";
  nella colonna delle caselle nessun altro movimento. Tolta la spunta
  laggiù, si svuota lì e 1,1 s dopo risale dopo "Shampoo". Il database del
  telefono è tornato com'era (cambia solo l'ora di modifica della riga)
### ✏️ v0.101.1 — 09/10/2026 · 07:44 — Le caselle di Food non scattano più: la riga spuntata resta sotto il dito
*(Claude Code sul PC; provato sul telefono l'08 e il 09/10/2026,
registrando lo schermo; 494 test passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** **Food è ordinato per la sua casella** (prima le cose da
  prendere, poi le prese). Spuntandone una, la riga saltava **subito** fra
  le prese: le righe in mezzo scalavano di un posto, e sotto il dito
  arrivava un'altra riga, con la sua casella che si ridisegnava. Si vedeva
  scattare una casella mai toccata, sopra o sotto; e toccando di nuovo lo
  stesso punto si spuntava **un'altra riga** (successo anche durante le
  prove: "Spazzolino" al posto di "Dentifricio")
- **[Modifica]** **[📱 Mobile]** Ora, spuntata (o tolta) una casella su cui la vista
  **ordina o filtra**, le righe **restano dove sono** finché si resta
  sulla pagina: si possono spuntare più cose di fila, come in una lista
  della spesa, e sotto il dito c'è sempre la riga che si vede. Vanno al
  loro posto **uscendo dalla pagina** (anche solo aprendone un'altra e
  tornando indietro), o cambiando ordine e filtro. Scorrere la pagina non
  basta: una tabella appena fuori dallo schermo l'app la tiene pronta così
  com'è. Con un filtro sulla casella ("solo da prendere") la riga appena
  presa resta visibile fino ad allora, invece di sparire
- **[Progetto]** `DatabaseViewModel.checkboxHold`: `setCellValue` su una
  casella che decide l'ordine o il filtro ferma l'ordine (lo stesso
  `_frozenOrder` di quando si scrive un nome) e le righe visibili
  (`_heldVisible`); `releaseCheckboxHold` le libera, chiamato dalla tabella
  quando esce dalla composizione (`DisposableEffect` in `DatabaseViewBody`) e da
  `setSort`, `setThenSort`, `setFilter`. Test: `CheckboxHoldTest` (4: la
  riga resta e un secondo tocco tocca lei, il filtro, niente fermo senza
  ordine né filtro sulla casella, cambiare ordine libera le righe)
- Provato sul telefono registrando lo schermo e misurando le caselle
  fotogramma per fotogramma. Prima: spuntando "Dentifricio" la riga
  scendeva e la casella del posto sotto ripartiva vuota e si riempiva; il
  secondo tocco nello stesso punto spuntava "Spazzolino". Dopo: la casella
  toccata si riempie e resta lì, il secondo tocco la toglie, le altre non
  si muovono; sei tocchi di fila, tutti sulla stessa riga. Il ritardo fra
  tocco e casella piena è quello di prima (fra 17 e 156 ms, misurati).
  Il 09/10, col telefono di nuovo collegato: tolta la spunta a
  "Dentifricio" (lasciata spuntata dalla prova della sera prima), la riga
  resta fra le prese; portata la pagina in cima e tornati giù è ancora lì
  (la tabella era appena sotto lo schermo); aperta la pagina "Food" e
  tornati indietro, è risalita fra le cose da prendere. Poi tolte le due
  righe di prova di Main calendar: il database del telefono ha di diverso
  solo quello che l'utente ha cambiato nel frattempo
### 🆕 v0.101 — 08/10/2026 · 22:37 — Il correttore, come in OneNote: riga rossa sotto le parole sbagliate
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 490 test passati; database alla
versione 55, copia del database del telefono fatta prima di installare; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La riga rossa ondulata** sotto le parole che
  il dizionario non conosce, nelle pagine del Workspace e dei Notebooks:
  nel testo, negli elenchi, nelle caselle, nei toggle, nei riquadri, nelle
  citazioni, nelle notifiche e nel titolo. La parola che si sta scrivendo
  non si segna finché non la si finisce, e le altre si ricontrollano
  appena si smette di scrivere
- **[Nuova funzionalità]** **[📱 Mobile]** **Toccando una parola sottolineata** si apre il
  suo menu: fino a **cinque parole al suo posto** (quattro con la
  tastiera aperta, perché il menu stia sopra o sotto la parola senza
  coprirla), la migliore per prima ("perche" → "perché", "squola" →
  "scuola", "dellacasa" → "della casa"), **"Add to dictionary"** e
  **"Ignore"**. La tastiera resta aperta; "Indietro" o un tocco fuori lo
  chiudono
- **[Nuova funzionalità]** **[📱 Mobile]** **Settings → Proofing**, sotto Language:
  "Check spelling" per tutte le pagine, Workspace e Notebooks insieme;
  **la lingua**; **"My dictionary"**, le parole aggiunte, che da lì si
  tolgono. **Spento di partenza**; la lingua di partenza è quella
  dell'app
- **[Nuova funzionalità]** **[📱 Mobile]** **"Proofing" nei tre puntini di una pagina**:
  "Default" (come dice Settings), "On" o "Off" solo per lei, e la sua
  lingua — una pagina in tedesco in un'app in italiano. Sotto, le parole
  ignorate in quella pagina, con "Check again" per rimetterle
- **[Nuova funzionalità]** **[📱 Mobile]** **Le lingue** sono quelle dell'app: inglese
  americano e britannico, italiano, tedesco, francese, spagnolo. Cinese,
  coreano e giapponese ci sono ma grigie, "Not available yet". Le parole
  aggiunte al dizionario valgono in tutte
- **[Nuova funzionalità]** **[📱 Mobile]** **Non si controllano**, come in OneNote e Word:
  indirizzi, email e nomi di file ("notes.txt"), le parole con dei numeri,
  le sigle tutte maiuscole, le parole di una lettera, quelle in un altro
  alfabeto, il testo sotto uno spoiler, il codice, le celle dei database
- **[Progetto]** **I dizionari sono quelli di Hunspell** — il correttore
  di LibreOffice, Firefox e Chrome — dentro l'app (circa 6 MB in tutto, 2
  compressi nell'APK), con le loro licenze; niente da scaricare a parte,
  niente internet. `tools/proofing/get_dictionaries.py` li riscarica alle
  versioni provate
- **[Progetto]** **Un motore scritto apposta in Kotlin** (`data/proofing/`),
  senza librerie native: `SpellDictionary` legge i file di Hunspell e
  controlla una parola come fa Hunspell — prefissi e desinenze, anche due
  desinenze una sull'altra ("dámelo"), l'elisione ("dell'anno",
  "l'homme"), le parole composte del tedesco ("Haustür",
  "Donaudampfschifffahrt"), maiuscole e "ß"; `SpellSuggestions.kt` trova
  le parole al suo posto (gli errori tipici prima — accenti, doppie,
  lettere scambiate — e se non basta le parole che si somigliano);
  `ProofingText` divide il testo in parole; `SpellChecker` aggiunge le
  parole dell'utente e quelle ignorate. Nella pagina: `ProofingUi.kt` (la
  riga e il menu, attaccati ai campi di testo senza cambiarne il
  funzionamento) e `ProofingSettings.kt` (le scelte)
- **[Progetto]** Database alla **versione 55**: `pages.proofingMode`,
  `proofingLanguage`, `proofingIgnored` (tutte le pagine "come dice
  Settings") e la tabella `personal_words`. Test: `SpellDictionaryTest`
  (frasi vere nelle sei lingue: le parole giuste passano, le sbagliate no,
  il primo suggerimento è quello giusto, e in fretta), `ProofingTextTest`,
  `SpellCheckerTest`, `ProofingMarksTest`, `ProofingRenderTest` (la riga
  disegnata e il tocco che apre il menu), la migrazione 54→55 in
  `MigrationTest` e due prove in `PageRepositoryTest`
- Verificato sul telefono, dopo aver copiato il database (l'app si apre
  col database alla 55, niente perso). Settings → Proofing acceso, in
  inglese: la pagina principale ha la riga rossa sotto le parole italiane
  delle liste, e non sotto "lo", "WC" e "Shampoo". Poi su una riga di prova
  di Main calendar, coi tre puntini → "Proofing" e l'italiano solo per
  lei, scritto "Domani vado a squola perche ce il compito di Mario. Parola
  zorblat e blablabla.": segnate "squola", "perche", "zorblat",
  "blablabla" (e il titolo "Untitled"). Toccando "squola" il menu dice
  scuola, suola, squalo, stuola, con la tastiera ancora aperta, e "scuola"
  prende il suo posto; "perche" diventa "perché"; "zorblat" con "Add to
  dictionary" perde la riga e compare in Settings → My dictionary, da cui
  "Remove" lo toglie; "blablabla" con "Ignore" finisce fra le parole
  ignorate della pagina, e "Check again" lo rimette. Lo stesso in una
  citazione. **Il telefono ha trovato tre cose**, sistemate prima del
  commit: il menu copriva la parola (con la tastiera aperta non ci stava né
  sopra né sotto), "Indietro" chiudeva la tastiera ma non il menu, e in
  Settings l'interruttore spento quasi non si vedeva e la spiegazione sotto
  era tagliata. Alla fine riga di prova tolta, parola tolta dal dizionario
  e correttore rispento: il database del telefono è tornato com'era
### 🆕 v0.100 — 08/10/2026 · 15:57 — "Move as inline": un database spostato può arrivare aperto nella pagina
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 461 test
passati; commit solo in locale)*

Arrivava come collegamento a pagina per una scelta del 24/09/2026 (chiesta
allora dall'utente): lo spostamento rifaceva sempre un collegamento.

- **[Nuova funzionalità]** **[📱 Mobile]** **"Move as inline"**, in fondo alla finestra di
  "Move to" quando si sposta **un database** (dal menu del blocco o dai tre
  puntini del database a pagina intera): acceso, il database arriva **aperto
  dentro la pagina** scelta; spento, come **collegamento a pagina**, come
  prima. Per le pagine non c'è
- **[Nuova funzionalità]** **[📱 Mobile]** **Parte com'è il database adesso**: acceso se si
  vede aperto dentro una pagina, spento se è un collegamento. Così, se non
  lo si tocca, il database arriva com'era
- **[Modifica]** **[📱 Mobile]** La spiegazione in cima alla finestra dice le due strade
  (prima rimandava a "Turn into database" da fare dopo)
- **[Progetto]** `PageRepository.movePageTo(…, asInline)` crea il blocco
  database invece del collegamento (solo per un database);
  `MoveExclusions.shownInline` dice com'è adesso; `MovePagePicker` ha
  l'interruttore, nella parte in fondo che `DestinationPickerSheet` ora sa
  avere (`footer`). Annulla rimette tutto com'era, come prima. Test: uno in
  `PageRepositoryTest`
- Verificato sul telefono, con due righe di prova di Main calendar (poi
  tolte; il database del telefono è tornato com'era): un database aperto
  nella prima, "Move to" dal suo menu → in fondo "Move as inline", già
  acceso; scelta la seconda riga ("Destinazione"), il database ci arriva
  aperto. Di nuovo "Move to" con l'interruttore spento → arriva come
  collegamento a pagina; e da collegamento, "Move to" mostra l'interruttore
  spento. Prima delle prove il confronto del database mostrava che l'utente
  aveva appena spostato "Main calendar" in fondo alla pagina principale: è
  rimasto dove l'aveva messo

### ✏️ v0.99.1 — 08/10/2026 · 15:39 — "Turn into database inline"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 460 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** La voce che rimette un database a pagina intera dentro
  la pagina che lo richiama ora si chiama **"Turn into database inline"**:
  sia nel menu del blocco (tenendo premuto il collegamento, come nella foto)
  sia nei tre puntini del database aperto a pagina intera, dove fa la stessa
  cosa. Nelle altre lingue come "Database - Inline" del menu "/": "Trasforma
  in database in linea", "In Inline-Datenbank umwandeln", "Convertir en base
  de données intégrée", "Convertir en base de datos en línea", "转换为内嵌数据库",
  "인라인 데이터베이스로 전환", "インラインデータベースに変換"
- **[Progetto]** `Strings.turnIntoDatabase`, la scritta delle due voci
- Installata sul telefono alle 15:37, ancora col numero 0.99 (la versione
  si prende dal README, e questa voce non c'era ancora): la scritta nuova
  c'è già. Il menu non è stato aperto per vederla: dal confronto del
  database risultava che l'utente stava usando l'app (alle 15:34–15:35 aveva
  tolto la spunta a otto righe di "Food" e aggiunto una riga in 家計簿), e da
  lì Claude non ha più toccato il telefono. Avuto il via dall'utente,
  installata la 0.99.1:
  tenendo premuto il collegamento "Main calendar" nella pagina principale il
  menu dice "Turn into database inline". Il menu è stato solo aperto e
  chiuso, e il database è rimasto com'era

### 🆕 v0.99 — 08/10/2026 · 15:32 — "Freeze first column" e "Freeze first row": prima colonna e titoli sempre in vista
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 460 test
passati; database alla versione 54, copia del database del telefono fatta
prima di installare; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Freeze first column"**, nelle impostazioni
  della tabella (sotto "Long text"): scorrendo la tabella a destra **la
  prima colonna resta al bordo sinistro** — Name, o quella che c'è per
  prima se Name è stata spostata — e le altre le passano sotto. Si sa
  sempre di che riga è la casella che si spunta
- **[Nuova funzionalità]** **[📱 Mobile]** **"Freeze first row"**: scorrendo in giù **la
  riga dei titoli delle colonne resta in cima**, e le righe le passano
  sotto. La "prima riga" è quella coi nomi delle proprietà: è lei che dice
  di quale proprietà è la casella. Dentro una pagina si ferma sotto i
  pulsanti sospesi (indietro, barra laterale, tre puntini); a pagina intera
  sotto la barra della vista. Quando la tabella finisce se ne va con lei. In
  una tabella a gruppi ogni gruppo ha la sua, ferma finché si è nel gruppo
- **[Nuova funzionalità]** **[📱 Mobile]** Si accendono **tutte e due insieme** o una sola;
  insieme, anche l'angolo (il titolo della prima colonna) resta fermo. Sono
  impostazioni **della vista**, come "Long text": ogni vista ha le sue, e
  con "Lock view" restano grigie
- **[Progetto]** Database alla **versione 54**: `pages.freezeFirstColumn` e
  `freezeFirstRow`, spente. `FrozenTable.kt`: la prima cella di ogni riga
  è spostata di quanto la tabella è scorsa (`frozenFirstCell`, in
  `TableSlots`); l'intestazione scende nel disegno (`PinnedHeaderSection`),
  così resta ferma nello stesso fotogramma dello scorrimento, senza
  inseguirlo. Il contenitore che scorre lo dice alla tabella
  (`StickyViewport`): la pagina (`forList`, con il margine dei pulsanti) e
  il database a pagina intera (`forScroll`). "Delete view" sulla prima vista
  (v0.98.1) si porta dietro anche queste due. `SettingsToggleRow` sa essere
  spento. Test: `FrozenTableTest` (5: l'intestazione in cima, sotto il
  margine, spenta, dentro una pagina; la prima cella a sinistra), la
  migrazione 53→54 in `MigrationTest`, e `DatabaseViewsTest`
- Verificato sul telefono, dopo aver copiato il database (l'app si apre col
  database alla 54), su un database di prova con una colonna di testo, una
  Checkbox e una ventina di righe, dentro una riga di prova di Main calendar
  (poi tolta, e con lei il database; il database del telefono è tornato
  com'era): le due voci sotto "Long text", accese; con la tabella scorsa a
  destra Name resta a sinistra e Text le passa sotto; scorrendo la pagina
  la riga "Name | Checkbox" resta sotto i pulsanti in alto; una casella
  spuntata lì sotto si spunta nella riga giusta, e toccando il titolo
  "Checkbox" fermo si apre "Edit property" (il tocco arriva al titolo, non
  alla riga nascosta sotto). A pagina intera la riga dei titoli resta sotto
  la barra della vista, e scorrendo a destra Name e l'angolo restano fermi.
  **Le prime prove del test hanno trovato due ritardi**, prima di arrivare
  al telefono: a pagina intera l'intestazione si calcolava con lo
  scorrimento del fotogramma prima, e dentro la pagina restava indietro di
  un passo; ora si rimette a posto ogni volta che la tabella si sposta

### ✏️ v0.98.1 — 08/10/2026 · 09:57 — Le viste: "Edit view" dai tre puntini, e anche la prima si toglie
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 454 test
passati; commit solo in locale)*

La Table non si toglieva perché era **la vista di partenza**: nell'app è il
database stesso (le viste aggiunte col "+" sono a parte), e il database non
può andarsene. Su Notion invece tutte le viste sono uguali.

- **[Modifica]** **[📱 Mobile]** **"Edit view"** in cima ai tre puntini di ogni
  vista, nell'elenco delle viste: la mette davanti e apre le sue
  impostazioni (Layout, Filter, Group, Long text, proprietà…), come l'icona
  dei cursori
- **[Modifica]** **[📱 Mobile]** **"Delete view" c'è anche per la vista di partenza**
  (la Table di "Foreign Languages"). Il database prende le impostazioni
  della vista che viene dopo — layout, nome, filtro, ordinamento, gruppi,
  proprietà nascoste, larghezze delle colonne, calcoli, grafico — e quella
  se ne va: per chi guarda, la prima vista è sparita e le altre sono
  salite, come su Notion. Le pagine e le proprietà restano tutte. **L'ultima
  vista rimasta non si toglie**: "Delete view" sparisce dai suoi tre puntini
- **[Progetto]** `PageRepository.deleteStartingView` (le colonne nascoste di
  una vista sono una lista sua, quelle del database sono le colonne stesse:
  si travasano), `DatabaseViewModel.deleteStartingView`; nei tre puntini
  (`ViewsSheet`) "Edit view" e `canDelete`; `DatabaseViewBody` apre le
  impostazioni appena la vista si vede (`startWithSettings`, come già la
  finestra dei grafici). Test: due in `DatabaseViewsTest`
- Verificato sul telefono su un database di prova, dentro una riga di prova
  di Main calendar (poi tolta col database; "Foreign Languages" non è stato
  toccato): aggiunta una vista List accanto alla Table; i tre puntini della
  Table hanno "Edit view", "Rename" e "Delete view"; "Edit view" mette
  davanti la Table e apre le sue impostazioni ("Layout: Table"); "Delete
  view" sulla Table, confermato, lascia solo la List, davanti; i tre puntini
  della List, ora l'unica, hanno "Edit view" e "Rename" ma non "Delete
  view". Alla fine il database del telefono è identico a prima della prova

### 🔧 v0.98.0.1 — 08/10/2026 · 09:48 — Cancellando una riga se ne va anche la sua pagina
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 452 test
passati; copia del database del telefono fatta prima; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** **Cancellando una riga** ("Delete this page? The row and its
  values will be deleted. This can't be undone.") **se ne va anche la sua
  pagina**, con quello che c'è dentro: i blocchi, la cronologia delle
  modifiche, i file (icone, copertine, immagini, video, audio, file), e le
  sottopagine che non sono richiamate da nessun'altra parte, come quando si
  cancella per sempre una pagina dal cestino. È quello che la conferma
  diceva già. Prima se ne andava solo la riga
- **[Bug fix]** **[📱 Mobile]** **Le pagine rimaste da righe cancellate prima** non si
  cancellano: all'avvio **vanno nel cestino**, dove si possono guardare e
  ripristinare, e da dove se ne vanno con la regola del cestino (30 giorni).
  Ripristinata, una di queste torna **una pagina normale, in fondo al menu
  principale**: la sua riga non c'è più, e lasciarla "pagina di una riga"
  l'avrebbe fatta sparire di nuovo. Se un'altra pagina la richiamava
  ancora, resta lì e diventa una pagina normale
- **[Progetto]** `DatabaseRepository.deleteRow` restituisce la pagina della
  riga (letta dal database, non dalla riga in mano alla schermata);
  `PageRepository.deleteRowPage` la cancella con `deletePermanently`, a
  meno che un'altra riga la apra ancora; `PageRepository.tidyOrphanRowPages`
  (all'avvio, dopo la pulizia del cestino) e `PageDao.orphanRowPages`;
  `restoreFromTrash` sa delle pagine senza più riga. `ensureRowPage` crea
  pagina e collegamento della riga insieme, così la pulizia non vede mai
  una pagina a metà. Test: `RowPageDeleteTest` (2)
- Verificato sul telefono, dopo aver copiato il database: all'avvio sono
  andate nel cestino esattamente le 18 pagine rimaste (create fra il 05/10
  e oggi, quasi tutte dalle prove di Claude: "Contenitore", "Sotto",
  "Prova", "Archivio", "Divisore", "ProvaDest", diverse "Untitled"), e
  nient'altro nel database è cambiato; il Cestino le mostra con "30 days
  left". Una riga di prova in Main calendar (vista calendario, 9 ottobre),
  aperta e con "Prova cancellazione" scritto dentro, poi cancellata: nel
  database non resta niente, né la riga né la pagina né il testo

### 🆕 v0.98 — 08/10/2026 · 09:39 — La scheda "Icons": le icone di Material, bianche o del colore che si sceglie
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 450 test
passati; database alla versione 53, copia del database del telefono fatta
prima di installare; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La scheda "Icons"** del selettore delle icone,
  fra "Emoji" e "Upload": le **2132 icone piene di Material**, di partenza
  **bianche** (il colore del testo: sul tema chiaro scure), in una griglia
  come quella di Notion. "Filter…" le cerca per le parole del nome inglese
  ("home", "alar acc"), "Random" ne sceglie una a caso
- **[Nuova funzionalità]** **[📱 Mobile]** **Il pallino accanto a "Random"** apre lo
  spettro dei colori di tutta l'app ("Icon color"): il colore scelto colora
  tutte le icone della griglia e va con l'icona che si sceglie; "No color"
  le rimette bianche. Il colore resta per la prossima volta, come su Notion
- **[Nuova funzionalità]** **[📱 Mobile]** **L'icona scelta vale ovunque** come le emoji:
  in cima alla pagina, nell'albero, nella ricerca, nei preferiti, nel
  cestino, nei collegamenti e nei rimandi, davanti al nome di un database,
  accanto alle righe in tutte le viste del database, nei riquadri
- **[Progetto]** Database alla **versione 53**: `pages.iconSymbol`, il nome
  dell'icona e il colore in una stringa ("Home" o "Home|#040369",
  `IconSymbol`); nei riquadri la voce `symbol` di `BlockAttrs`. L'ordine:
  immagine, poi icona di Material, poi emoji. `MaterialIconSet.kt` è
  generato da `tools/icons/make_material_icons.py`, che legge le due
  librerie di Material dell'app (quella di base, con le icone più comuni
  come Home e Star, e quella estesa) e prende le icone che si specchiano
  nelle lingue da destra a sinistra dalla loro versione nuova. Il disegno
  comune è `SymbolIcon`; il colore scelto sta in `AppSettings.iconColor`.
  Test: `MaterialIconsTest` (3: ogni nome dà la sua icona, le parole e la
  ricerca, il colore), due in `PageIconTest`, uno in `IconPickerTest`, e la
  migrazione 52→53 in `MigrationTest`
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta), dopo aver copiato il database del telefono: l'app si apre col
  database alla 53; "Add icon" → "Icons" mostra la griglia bianca; il
  pallino apre lo spettro con "Icon color"; scelto il primo dei colori
  recenti (per non cambiarne l'ordine) e "Apply", tutta la griglia diventa
  blu; "home" → la casa diventa l'icona della pagina, blu, e compare blu
  accanto al nome della riga nel calendario; "No color" rimette la griglia
  bianca. **Prima di installare, il test che controlla ogni nome ha
  trovato che mancavano le icone più comuni** (Home, Star, Settings…):
  stanno nella libreria di base di Material, e il generatore leggeva solo
  quella estesa

### 🆕 v0.97 — 08/10/2026 · 09:17 — Le emoji di Notion per le icone: categorie, ricerca e colore della pelle
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 443 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Il selettore delle icone**, lo stesso
  dappertutto si tocca un'icona per cambiarla: l'icona di una pagina (anche
  "Add icon" delle pagine delle righe), "Icon" delle impostazioni di un
  database e delle azioni di una riga, "Edit icon" dei collegamenti, l'icona
  dei riquadri (Callout). In cima due schede, **"Emoji"** e **"Upload"**
  (l'immagine dal telefono, come prima), e **"Remove"** quando un'icona c'è
- **[Nuova funzionalità]** **[📱 Mobile]** **Tutte le emoji, nelle categorie di Notion**:
  Recent, People, Animals and nature, Food and drink, Activity, Travel and
  places, Objects, Symbols, Flags (1906 emoji di Unicode 16; quelle che il
  telefono non sa ancora disegnare restano fuori, invece di comparire come
  quadratini). In fondo **"Custom"**: le immagini già caricate come icona di
  pagine e riquadri, una volta sola anche se copiate più volte; toccandone
  una la pagina ne prende una copia sua
- **[Nuova funzionalità]** **[📱 Mobile]** **La barra in basso della foto**: orologio,
  faccina, foglia, carota, pallone, aereo, lampadina, spunta, bandiera,
  quadretti e "+". Ogni voce porta alla sua categoria e resta accesa mentre
  la si guarda; "+" carica un'immagine nuova
- **[Nuova funzionalità]** **[📱 Mobile]** **"Filter…"**: si cerca per nome **in tutte e
  otto le lingue dell'app insieme**, senza badare a maiuscole e accenti —
  "gatto", "cat", "Katze" e "猫" trovano il gatto anche con l'app in
  inglese. Ogni parola scritta è l'inizio di una parola del nome ("pizz"
  trova la pizza, "cat" non trova la "catena" prima dei gatti). Un'emoji
  scritta con la tastiera si usa così com'è ("Use this emoji")
- **[Nuova funzionalità]** **[📱 Mobile]** **"Random"** sceglie un'emoji a caso; **la
  manina** accanto apre i sei colori della pelle, che valgono per tutte le
  emoji di persone e mani e restano scelti
- **[Nuova funzionalità]** **[📱 Mobile]** **Anche i database e le pagine delle righe hanno
  l'emoji**: davanti al nome del database, nell'albero, nella ricerca, nei
  collegamenti e accanto al nome della riga in tutte le viste (tabella,
  bacheca, calendario…). Prima per loro contava solo un'immagine
- **[Modifica]** **[📱 Mobile]** **Il selettore dei riquadri** è questo, con in cima la
  famiglia "Callout" della v0.91; il campo "Other emoji" non c'è più, la
  ricerca fa lo stesso
- **[Modifica]** **[📱 Mobile]** README: nei limiti noti, la frase "l'emoji di una pagina
  non si sceglie ancora da nessuna parte" è superata
- **[Progetto]** `IconPicker.kt` (`IconPickerSheet`), `EmojiCatalog.kt`
  (`EmojiCatalog`, `withSkinTone`, `EmojiCatalogs`, `IconLibrary`),
  `PageIcons.kt` (`IconChoice`: un'emoji, un'immagine o niente; il 📄 di
  riserva nel campo `icon` vuol dire "niente scelto"), `EmojiStrings`. Le
  emoji recenti e il colore della pelle stanno in `AppSettings`. Nessun
  cambio al database: `PageDao.setIcon` scrive emoji e immagine insieme, e
  le icone delle righe (`DatabaseDao.observeRowIcons`) portano anche
  l'emoji. Gli asset sono in `assets/emoji` (le emoji per categoria e i nomi
  del CLDR nelle otto lingue) e si rigenerano con
  `tools/emoji/make_emoji_assets.py`, che scarica da sé i file di Unicode.
  Test: `EmojiCatalogTest` (6), `PageIconTest` (2), `IconPickerTest` (4)
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta), creata toccando il 9 ottobre nella vista calendario: "Add icon"
  apre il selettore con le schede, la ricerca, la manina, le People e la
  barra della foto; la carota porta a "Food and drink", la bandiera alle
  Flags, i quadretti alle Custom (il cappello, le icone delle pagine, i
  Pokémon). **"gatto" dava "No results"**: l'app è in inglese, e la ricerca
  conosceva solo la lingua dell'app e l'inglese; ora conosce tutte le
  lingue e trova i gatti. 🐈 diventa l'icona della pagina e compare
  accanto al nome della riga nel calendario. "Icon" delle impostazioni di
  Main calendar: in Recent c'è 🐈, scelto compare davanti a "Main
  calendar", "Remove" lo toglie (nel database torna com'era: 📄 e nessuna
  immagine). Nel riquadro: in cima la famiglia "Callout", la manina coi sei
  colori, "pollice" dà le mani colorate, 👍🏽 diventa l'icona. **In fondo
  alla griglia la barra restava sulla bandiera** anche toccando i
  quadretti: le Custom non arrivano in cima; ora vale l'ultima sezione che
  si vede. Il colore della pelle è stato rimesso giallo. Le Recent del
  telefono hanno 🐈 e 👍🏽 delle prove

### 🆕 v0.96 — 07/10/2026 · 21:19 — "File" e "Web bookmark": il file da aprire o scaricare e il link con l'anteprima
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, anche con
la rete; 431 test passati; commit solo in locale)*

Con questa, **tutte le voci di quella richiesta ci sono**.

- **[Nuova funzionalità]** **[📱 Mobile]** **"File"** (dal menu "/" e dal "+"): il riquadro
  "Upload or embed a file" e la finestrella Upload/Link della v0.95:
  "Choose a file" apre i file del telefono, **di qualsiasi tipo**; "Link"
  ("Embed link") tiene l'indirizzo di un file in rete
- **[Nuova funzionalità]** **[📱 Mobile]** Il blocco è la riga della nona foto: **l'icona
  del tipo** (PDF, immagine, video, audio, documento, archivio), **il nome
  e il peso** ("215 KB", "1,4 MB"). **Toccandolo si apre** con l'app che lo
  sa aprire (Android chiede quale); **la freccia a destra lo scarica dove si
  sceglie** — il telefono, la scheda SD, Drive —: è il "scaricarlo ovunque
  io sia". Tenendolo premuto: "Open with…", "Download", "Replace",
  "Duplicate", "Delete". Un file da link si apre toccandolo
- **[Nuova funzionalità]** **[📱 Mobile]** **"Web bookmark"**: la finestra "Create
  bookmark", dove si incolla il link. Il segnalibro è la scheda della decima
  foto: **il titolo del sito, la descrizione e la sua immagine**, e sotto
  l'indirizzo. **Toccandolo si apre nel browser predefinito** (o nell'app
  del sito, se c'è). Tenendolo premuto: "Open in browser or app", "Replace",
  "Duplicate", "Delete"
- **[Nuova funzionalità]** **[📱 Mobile]** **L'anteprima si legge dal sito una volta sola**,
  appena nasce il segnalibro: sono i dati che il sito stesso dà per le
  anteprime (gli stessi che usano Notion, WhatsApp e Telegram). L'immagine
  si salva nell'app, quindi dopo si vede anche senza rete. Senza rete il
  segnalibro mostra il nome del sito e l'indirizzo, e ci riprova alla
  prossima apertura della pagina
- **[Modifica]** **[📱 Mobile]** README: tolte dai limiti noti le voci grigie dei menu
  "+" e "/" (ora ci sono tutte), e nelle funzionalità la frase "l'app non
  si collega a niente", vera fino alla v0.91
- **[Progetto]** `FileBookmarkBlocks.kt` (`FileBlockContent`,
  `BookmarkBlockContent`, `BookmarkSheet`, `formatFileSize`),
  `LinkPreviews.kt` (legge al massimo 512 KB della pagina, presentandosi
  come un browser, con 8 secondi di pazienza; l'immagine al massimo 5 MB).
  Il "Download" passa dal "Salva in…" di Android, che non chiede permessi.
  Test: `LinkPreviewTest` (4: i tag "og:" in qualsiasi ordine, il
  `<title>` quando mancano, una pagina senza niente, il peso dei file)
- Verificato sul telefono, nella stessa riga di prova di Main calendar
  (poi tolta), con un file audio di prova: il blocco File mostra l'icona
  dell'audio, il nome e "215 KB"; toccandolo Android chiede con che app
  aprirlo; la freccia lo salva dove si sceglie, e il file salvato è
  identico all'originale (stessa impronta md5). Il segnalibro, a rete
  spenta, mostra "wikipedia.org" e l'indirizzo. **Con la rete accesa
  l'anteprima non arrivava mai**: la lettura del sito ripartiva da capo
  appena cominciata, e così si interrompeva da sola; ora parte una volta
  per link. Poi: "Wikipedia, the free encyclopedia", la descrizione e il
  globo; toccandolo si apre Firefox, il browser predefinito, su
  wikipedia.org, e Indietro torna a notE

### 🆕 v0.95 — 07/10/2026 · 21:18 — "Video" e "Audio": guardarli e ascoltarli dentro la pagina
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, anche con
la rete; 431 test passati insieme alla v0.96; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Video"** (dal menu "/" e dal "+"): la riga
  diventa il riquadro "Embed or upload a video" e si apre subito la
  finestrella di Notion con due schede: **"Upload"** ("Choose a video", la
  galleria del telefono con i soli video) e **"Link"** (si incolla
  l'indirizzo, "Embed video"). Un indirizzo senza "https://" lo prende; una
  parola che non è un link no ("That doesn't look like a link")
- **[Nuova funzionalità]** **[📱 Mobile]** **Un video caricato, o il link diretto a un
  video** (un .mp4…), si guarda **nel lettore dell'app**: toccando il video
  parte o si ferma, e sotto ci sono **play/pausa, il tempo e la barra col
  pallino** — lo si trascina per andare avanti o indietro, e il video ci
  salta quando lo si lascia. Prima di partire mostra il primo fotogramma
  invece del nero; arrivato in fondo, play ricomincia
- **[Nuova funzionalità]** **[📱 Mobile]** **YouTube e Vimeo** si guardano **nel loro
  lettore, dentro la pagina** (play, pausa, la barra col pallino rosso):
  è l'unico modo in cui quei siti lasciano vedere i loro video fuori da sé.
  Il tasto **schermo intero** del lettore lo allarga a tutto il telefono,
  barre di sistema comprese; Indietro lo riporta nella pagina. Sotto, il
  nome del sito e i tre puntini del menu del blocco
- **[Nuova funzionalità]** **[📱 Mobile]** **Il menu del video** (tenendolo premuto, o dai
  tre puntini): per un link **"Open in browser or app"** — Android chiede
  se aprirlo nell'app di YouTube o nel browser —; per un file **"Open
  with…"** (un altro lettore), **"Download"** (lo si salva dove si sceglie)
  e "Replace"; per tutti "Duplicate" e "Delete"
- **[Nuova funzionalità]** **[📱 Mobile]** **"Audio"**: il riquadro "Add an audio file", poi
  la stessa finestrella ("Choose a file" apre i file del telefono, solo
  audio; "Link" per l'indirizzo di un file audio). Il blocco è la scheda
  della settima foto: **il nome e, sotto, play/pausa, il tempo e la barra
  col pallino**. Tenendolo premuto, lo stesso menu del video
- **[Nuova funzionalità]** **[📱 Mobile]** **Uscendo dall'app o dalla pagina il lettore si
  ferma** (Home, un'altra app, lo schermo spento, un'altra pagina): niente
  suono da una pagina che non si vede. Tornando non riparte da solo, e play
  **riprende dal punto in cui si era fermato**
- **[Progetto]** `MediaPlayers.kt`: `Playback` (il `MediaPlayer` di
  Android su una `TextureView`), `PlaybackControls`, `InlineVideoPlayer`,
  `InlineAudioPlayer`, e `EmbeddedVideoPlayer` — un `WebView` con l'iframe
  di `youtube-nocookie.com` o di Vimeo, la cui pagina ha come indirizzo
  "di casa" `https://com.gabriele.notionlocal` (YouTube vuole sapere chi
  lo incorpora). `VideoAudioBlocks.kt`: la finestrella Upload/Link e i
  blocchi. I file caricati vivono nella cartella delle immagini dei blocchi
  e si aprono alle altre app dal `FileProvider` della v0.94. Test:
  `MediaLinksTest` (4: YouTube in tutte le forme del suo indirizzo, Vimeo,
  i link senza "https://", il tempo come un orologio)
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta), con un video e un audio di prova messi apposta nel telefono (non
  i file dell'utente): il video caricato parte, si ferma, "Open with…" lo
  apre in un altro lettore; l'audio parte, la barra salta avanti e
  indietro, riprende. **Il video di YouTube restava nero**: con
  l'ispettore del `WebView` si è visto che l'iframe era alto zero — in
  Compose il `WebView` nasce "alto quanto il contenuto", e così la pagina
  dentro si crede alta zero. Ora è alto quanto il riquadro: si vede, parte,
  si ferma, va a schermo intero (prima quel tasto non faceva niente) e
  torna. **Uscendo con Home l'audio diventava "This audio can't be played
  here"**: fermare un lettore mai partito per Android è un errore; ora si
  ferma solo quello che suona. Il link diretto a un .mp4 di prova si
  carica, mostra il primo fotogramma e parte (0:02 di 0:10). "Open in
  browser or app" su YouTube: Android offrirebbe l'app di YouTube e
  Firefox (controllato senza aprirlo, per non toccare la cronologia di
  YouTube dell'utente)

### 🆕 v0.94 — 07/10/2026 · 20:39 — "Image": l'immagine da ridimensionare, ingrandire, scaricare e spostare
*(Claude Code sul PC; verificato sul telefono il 06/10/2026; 423 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Image"** (dal menu "/" e dal "+"): la riga
  diventa il riquadro grigio "Add an image" e **si apre subito la galleria**
  del telefono, come su Notion. Si carica **solo dal telefono**, come
  chiesto (niente link). Annullando la galleria il riquadro resta, e
  toccandolo la si riapre
- **[Nuova funzionalità]** **[📱 Mobile]** L'immagine si vede **intera**, larga quanto la
  pagina e centrata. **Due maniglie ai lati** la stringono (fino a un
  quarto della pagina) o la allargano trascinandole; la larghezza resta
- **[Nuova funzionalità]** **[📱 Mobile]** **Toccandola si apre a tutto schermo**, sul nero:
  si ingrandisce e si sposta con due dita, due tocchi la rimettono com'era.
  Lì c'è anche il pulsante per **scaricarla**, nella Galleria del telefono
  ("Pictures/notE")
- **[Nuova funzionalità]** **[📱 Mobile]** **Tenendola premuta**: "View", "Download",
  "Replace" (un'altra dalla galleria), **"Move to"** — si sceglie la pagina,
  dall'albero o cercandola, e l'immagine **va in fondo a quella pagina** —,
  "Duplicate" (con una copia sua del file) e "Delete". "Move to" non si
  annulla con la freccia indietro: l'avviso dice dov'è finita
- **[Modifica]** **[📱 Mobile]** **Backspace a inizio della riga sotto un riquadro o una
  citazione** unisce la riga al loro testo, come già sotto un toggle o una
  casella. Sotto un'immagine (o un altro blocco senza testo) non fa niente
- **[Progetto]** `MediaBlocks.kt` (`ImageBlockContent`, `MediaPlaceholder`),
  `BlockFiles` (nome e tipo del file, Galleria, apertura con altre app),
  `PageImageStore.saveFile`, il `FileProvider` (`res/xml/shared_files.xml`),
  `PageRepository.moveBlockToPage`. Il menu dei sei puntini chiede e il
  blocco fa (`PageEditorViewModel.mediaRequest`): il menu si chiude prima che
  la galleria risponda. L'immagine a tutto schermo è quella della proprietà
  "Image" dei database, con in più "Scarica". Test: `ImageBlockTest` (4)
- Verificato sul telefono, in due righe di prova di Main calendar (poi
  tolte), con un'immagine di prova messa apposta nella Galleria (non le foto
  dell'utente): "/image" apre la galleria; scelta, l'immagine compare a tutta
  larghezza; le maniglie la stringono e poi la allargano, due volte di fila;
  a tutto schermo "Scarica" la salva in Pictures/notE ("Saved in
  Pictures/notE"); tenendola premuta, il menu con le sei voci; "Move to" →
  "ProvaDest" la porta in fondo a quella pagina ("Moved to "ProvaDest""),
  con la sua larghezza. Le righe di Main calendar sono come prima della prova

### 🆕 v0.93 — 06/10/2026 · 10:15 — "Link to page" e "Database - Full page"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 419 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Link to page"** (dal menu "/" e dal "+"): si
  sceglie una pagina dall'albero del Workspace, o cercandola per nome (lo
  stesso di "Move to"; anche le pagine delle righe dei database), e la riga
  diventa un **rimando** a lei: la sua icona con una **freccetta** e il nome
  sottolineato, sempre quelli di adesso. Toccandolo si apre la pagina (un
  database si apre a pagina intera). **La pagina non si sposta**: resta dov'è
  nell'albero, e questa non diventa sua madre
- **[Nuova funzionalità]** **[📱 Mobile]** Tenendo premuto il rimando: **"Open"**, **"Change
  page"** (si sceglie un'altra pagina), "Duplicate" e "Delete" (toglie il
  rimando, non la pagina). Se la pagina finisce nel cestino il rimando lo
  dice ("In the trash") e non la apre; se viene eliminata per sempre diventa
  "Deleted page"
- **[Nuova funzionalità]** **[📱 Mobile]** **"Database - Full page"**: nasce un database che
  **si apre subito a pagina intera**, e nella pagina resta il suo
  collegamento (l'icona della tabella e il nome), come un database dopo
  "Turn into page". Da "Turn into" il testo della riga diventa il nome del
  database
- **[Modifica]** **[📱 Mobile]** "Link to page" non è in "Turn into": il testo della riga
  andrebbe perso
- **[Progetto]** `BlockType.PAGE_MENTION`, con la pagina in
  `BlockAttrs.pageId` e non in `linkedPageId` (che costruisce l'albero);
  `PageMentionBlock.kt`; `PageRepository.makeFullPageDatabase`. Il menu dei
  sei puntini dei blocchi che non sono testo è `ObjectBlockMenu`, che
  useranno anche immagini, video, audio e file. Test: `LinkToPageTest` (4)
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta): "/link" → "Link to page" → "Food" fa il rimando con la freccetta;
  toccandolo si apre Food (solo guardata, niente toccato), e tenendolo
  premuto c'è il suo menu; "/full" → "Database - Full page" apre il database
  nuovo a pagina intera, e tornando indietro resta il collegamento
  "Untitled" con la tabella. Le pagine dell'utente e le righe di Main
  calendar sono come prima della prova

### 🆕 v0.92 — 06/10/2026 · 10:08 — "Code": il codice col suo linguaggio e i suoi colori
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 415 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Code"** (dal menu "/" e dal "+"): un
  **riquadro** col carattere a larghezza fissa. In alto a sinistra il
  **linguaggio** (di partenza JavaScript, come su Notion): toccandolo si
  apre l'elenco dei sessanta più usati, con la ricerca in cima e il segno
  accanto a quello scelto. In alto a destra il pulsante che **copia tutto il
  codice** ("Code copied")
- **[Nuova funzionalità]** **[📱 Mobile]** **Il codice si colora** secondo il linguaggio,
  coi colori dei temi "One" (scuro e chiaro): parole chiave viola, stringhe
  verdi, commenti grigi in corsivo, numeri arancioni, funzioni azzurre, tipi
  gialli. "Plain Text" e "Diff" restano senza colori
- **[Nuova funzionalità]** **[📱 Mobile]** Nel codice **l'Invio va sempre a capo dentro**, e
  la tastiera non mette maiuscole e non corregge le parole. Mentre si scrive
  codice la barra non mostra "Aa" né il pennello, e i sei puntini non hanno
  "Color": i colori sono quelli del linguaggio. Hanno invece "Delete"
- **[Nuova funzionalità]** **[📱 Mobile]** **Backspace in un codice vuoto** lo fa tornare una
  riga di testo; **"Turn into"** verso il testo fa di ogni riga di codice una
  riga di testo
- **[Progetto]** `CodeBlock.kt` (il blocco e la scelta del linguaggio),
  `CodeHighlight.kt` (`CODE_LANGUAGES` e `highlightCode`); il linguaggio sta
  in `BlockAttrs.language`. Test: `CodeHighlightTest` (5)
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta): "/code" fa il riquadro "JavaScript"; `const x = 1`, `// nota`,
  `alert(x)` su tre righe, colorati; "Python" dalla ricerca "pyth" cambia i
  colori (lì `const` e `//` non vogliono dire niente); il pulsante copia
  ("Code copied"); "Turn into → Text" fa tre righe di testo; backspace in
  un codice vuoto lo fa tornare testo. Le righe di Main calendar sono come
  prima della prova

### 🆕 v0.91 — 06/10/2026 · 09:58 — "Callout" e "Quote": il riquadro con l'icona e la citazione
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 410 test
passati; commit solo in locale)*

Alla domanda di Claude (l'app dal 25/09/2026 non usa Internet: per i video
da link e l'anteprima dei segnalibri serve di nuovo) la risposta è stata
**"Sì, rimettilo"**.

Le voci si costruiscono a gruppi, ognuno con la sua versione: questa è la
prima, **Callout** e **Quote**.

- **[Nuova funzionalità]** **[📱 Mobile]** **"Callout"** (dal menu "/" e dal "+"): un
  **riquadro** con lo sfondo grigio più chiaro della pagina, l'icona a
  sinistra (di partenza la lampadina 💡, come su Notion) e il testo accanto.
  **L'Invio va a capo dentro il riquadro**, che si allunga; un Invio su una
  riga vuota in fondo esce: la riga vuota se ne va e sotto nasce una riga di
  testo. Il testo si formatta come quello delle altre righe (grassetto,
  colori, corpo)
- **[Nuova funzionalità]** **[📱 Mobile]** **L'icona del riquadro**, toccandola (o dai sei
  puntini, "Edit icon"): le emoji della famiglia "Callout" del selettore di
  Notion (💡 👉 ☝️ 👌 🔑 🚧 ⚠️ 🔥 📌 ✂️ ❓ 🚫 ⛔ ⏰ ☎️ 🚨 ♻️ ✅ 🔒 📎 📖 🗣️ ➡️ 📢
  🛠️ ⚙️), "Other emoji" per scriverne una qualsiasi con la tastiera,
  **"Upload"** per un'immagine dal telefono come per le icone delle pagine, e
  "Remove" per lasciarlo senza icona
- **[Nuova funzionalità]** **[📱 Mobile]** **Lo sfondo del riquadro**, dai sei puntini
  ("Background color"): lo stesso selettore dei colori di tutta l'app; "No
  color" rimette il grigio
- **[Nuova funzionalità]** **[📱 Mobile]** **"Quote"**: la **riga verticale spessa** a
  sinistra, alta quanto il testo, e il suggerimento grigio "Empty quote"
  finché è vuota. Anche qui l'Invio va a capo dentro e quello su una riga
  vuota in fondo esce
- **[Nuova funzionalità]** **[📱 Mobile]** **Backspace a inizio riquadro o citazione** li fa
  tornare testo normale, come su Notion, **con tutte le loro righe**: ogni
  riga diventa una riga di testo (lo stesso con "Turn into" verso un altro
  tipo). Un altro backspace la unisce alla riga sopra
- **[Nuova funzionalità]** **[📱 Mobile]** Nei **sei puntini** di riquadro e citazione, oltre
  alle voci delle righe di testo, **"Delete"**: sono scatole a sé
- **[Modifica]** **[📱 Mobile]** Toccando sotto un riquadro o una citazione in fondo alla
  pagina si scrive su una riga nuova, invece di rientrarci dentro
- **[Modifica]** **[📱 Mobile]** **L'app può di nuovo usare Internet** (permesso tolto il
  25/09/2026): servirà ai video presi da un link e alle anteprime dei
  segnalibri delle prossime versioni. Le note restano sul telefono
- **[Progetto]** Database alla **versione 52**: `blocks.attrsJson`, le voci
  in più dei blocchi nuovi in JSON (`BlockAttrs`: file, indirizzo, nome,
  larghezza, lingua, icona, colore, pagina…). I tipi nuovi sono già tutti
  nell'enum (`CALLOUT`, `QUOTE`, `CODE`, `IMAGE`, `VIDEO`, `AUDIO`, `FILE`,
  `BOOKMARK`, `PAGE_MENTION`). I file dei blocchi si copiano con la pagina o
  il blocco duplicati e coi template, e se ne vanno con la pagina cancellata
  per sempre. `PageRepository.convertMultiline` divide le righe;
  `CalloutParts.kt` ha l'icona e la sua finestra. Test: `CalloutQuoteTest`
  (5) e la migrazione 51→52 in `MigrationTest`
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta): "/callout" fa il riquadro con la lampadina; "Prima", Invio,
  "Seconda riga" restano dentro, su due righe; due Invio in fondo escono;
  "/quote" fa la riga spessa con "Empty quote", poi due righe; 🔥 dalla
  finestra dell'icona; lo sfondo blu da "Background color"; backspace a
  inizio citazione la fa tornare due righe di testo. **Alla prima prova
  l'Invio dentro il riquadro lo spezzava in due**: quando la tastiera
  rimanda lo stesso testo per spostare il cursore, il suo a-capo veniva
  preso per un Invio. Corretto. Il database del telefono è passato alla 52
  senza perdere niente

### 🔧 v0.90.0.1 — 06/10/2026 · 09:26 — "/database" sotto un divisore non chiude più l'app
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 404 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Sì, era il crash del 03/10/2026 alle 19:31: il database
  "Foreign languages" di never_underestimate_feelings è nato in quello
  stesso secondo. Una riga sola sotto un divisore (o sotto un database) è
  un gruppo di testo tutto suo; scegliendo **"Database - Inline"** (o
  "Simple database", o una delle viste) dal menu **"/"** o dal **"+"**, la
  riga diventava un database e il suo campo di scrittura spariva **col
  cursore dentro**: la tastiera continuava a chiedergli dov'era il cursore
  e l'app si chiudeva (`LayoutCoordinate operations are only valid when
  isAttached is true`, lo stesso errore già risolto per toggle e divisore).
  Ora il cursore si spegne prima, e la tastiera si chiude: nel database non
  c'è niente da scrivere. Lo stesso per **"Table"**, che aveva lo stesso
  difetto. "Turn into" dai sei puntini non lo aveva: lì il cursore si
  spegne già prima di aprire il menu
- **[Progetto]** `applyInsertAction`: `focusManager.clearFocus()` prima di
  `convertToDatabaseLink` e prima di trasformare la riga in tabella, come
  già faceva "Linked view of data source". Il crash dipende dalla tastiera
  vera e non si riproduce nei test sul PC
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta): con la v0.90 i passi dell'utente (database, "Add below",
  "/divider", "/database" sotto il divisore, "Database - Inline") hanno
  chiuso l'app con lo stesso errore del 03/10; con la correzione, gli
  stessi passi creano il database senza crash. Provati anche "/table" e
  "/page" su una riga sola fra due isole: niente crash. Le righe di Main
  calendar sono come prima della prova

### 🆕 v0.90 — 06/10/2026 · 09:13 — "Title position": il titolo sotto l'icona, accanto o nascosto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 404 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** In **Settings**, sopra "Animations", la voce
  **"Title position"**, che vale per tutte le pagine insieme, anche quella
  del menu principale: **Below the icon** (come prima, ed è la scelta di
  partenza), **Next to the icon** (il titolo a destra dell'icona, sulla
  stessa riga: tutto il resto della pagina sale) e **Hidden** (il titolo
  non si vede; la pagina si rinomina con "Rename" dal menu del suo blocco
  nella pagina madre o, per una riga, dalla cella del database). Le
  pagine dei quaderni, che non hanno icona, tengono il titolo dov'è
- **[Modifica]** **[📱 Mobile]** **"Add cover"** non sta più accanto all'icona: è la
  prima voce dei **tre puntini** di ogni pagina, sempre, qualunque sia la
  posizione del titolo. Non c'è quando la pagina ha già una copertina (si
  cambia toccandola), nel cestino e nelle pagine dei quaderni
- **[Progetto]** `AppSettings.titlePosition` (`TitlePosition`: `BELOW_ICON`,
  `BESIDE_ICON`, `HIDDEN`, salvata in "title_position"); nell'editor il
  campo del titolo è `TitleField`, messo nella riga dell'icona o sotto;
  `PageOptionsSheet(onAddCover)`. `TitlePositionTest`: la voce sta sopra
  "Animations" e la scelta si salva
- Verificato sul telefono: in Settings "Title position" sopra "Animations",
  con le tre scelte; con "Next to the icon" "My Personal Workspace" accanto
  al cappello, e in una riga di prova di Main calendar "Untitled" accanto ad
  "Add icon"; nei tre puntini di quella riga "Add cover" in cima, che apre
  la scelta della copertina; con "Hidden" il titolo sparisce dal menu e
  dalla pagina della riga. Poi rimesso "Below the icon" e tolta la riga di
  prova: le righe di Main calendar sono come prima della prova

### ✏️ v0.89.1 — 05/10/2026 · 22:58 — Il divisore tenuto premuto: "Add above", "Add below" e "Delete"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 403 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Tenendo premuto un **divisore** si apre il menu
  dei blocchi (quello dei sei puntini), col titolo "Divider" e solo tre
  voci: **Add above** e **Add below** (una riga vuota sopra o sotto, col
  cursore dentro) e **Delete** (lo toglie subito; Annulla lo rimette).
  Prima il tocco lungo chiedeva soltanto "Delete this divider?". In una
  pagina bloccata il tocco lungo non fa niente, come per gli altri
  blocchi
- **[Rimosso]** **[📱 Mobile]** La domanda "Delete this divider?"
- **[Progetto]** `DividerBlockContent(onLongPress)` apre `BlockActionsHost`,
  che per un divisore mostra `DividerBlockMenu`. `AddLineTest`: un test in
  più (una riga sopra e una sotto il divisore, e "Delete" che toglie solo
  lui)
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta) con "Sopra" e un divisore: tenendolo premuto, il menu "Divider"
  con "Add above", "Add below" e "Delete"; "Add above" ha messo una riga
  fra "Sopra" e il divisore col cursore dentro, "Add below" una subito
  sotto, "Delete" ha tolto il divisore lasciando le righe. Alla prima
  prova il titolo diceva "Text": corretto. Le righe di Main calendar sono
  come prima della prova

### 🆕 v0.89 — 05/10/2026 · 22:48 — Gli ultimi dieci colori usati, sotto il campo Hex
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 402 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nel selettore del colore, sotto il campo
  **Hex**, ci sono i **"Recent colors"**: gli ultimi dieci colori
  applicati, il più recente per primo, in una fila di tondi. Toccandone
  uno il selettore va su quel colore (si applica con "Apply", come gli
  altri); quello uguale al colore scelto ha l'anello bianco
  - **ovunque**: il selettore è lo stesso per il colore del testo e dello
    sfondo, delle celle colorate, dei tag, delle penne del disegno, delle
    barre e degli anelli dei numeri, delle barre dell'avanzamento — è
    l'unico campo Hex dell'app
  - un colore si ricorda quando lo si applica; uno già nella fila sale in
    cima senza doppioni; restano anche chiudendo l'app. Finché non se ne è
    applicato nessuno, la fila non c'è
- **[Progetto]** `AppSettings.recentColors` e `rememberColor` (chiave
  `recent_colors`), `withRecentColor` (in cima, senza doppioni, al massimo
  `RECENT_COLORS_MAX` = 10); `RecentColorsRow` in `ColorPickerSheet`, che
  ricorda il colore su "Apply". `RecentColorsTest`: 2 test (la lista dei
  dieci, senza doppioni e solo colori veri; "Apply" lo ricorda e il tondo
  lo rimette)
- Verificato sul telefono, col colore di un tag "Prova" su una proprietà
  di Main calendar aggiunta per la prova e poi tolta: prima nessuna fila;
  applicati un blu (#040369) e un marrone (#562316), riaprendo c'erano
  "Recent colors" coi due tondi, il marrone per primo con l'anello;
  toccando il blu il campo Hex è passato a #040369. I due colori della
  prova restano fra i recenti. Nel database è cambiata solo l'ora di
  ultima modifica della riga di prova

### ✏️ v0.88.1 — 05/10/2026 · 22:27 — "Move to database": prima di spostare, il nome della pagina nuova
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 400 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** In "Move to database", scelto il database,
  compare **"Name of the new page"**: il nome della pagina nuova, già
  scritto (quello di adesso) e tutto selezionato, con sopra cosa
  succederà. "Move" sposta, "Cancel" torna alla scelta del database
  - **una pagina** diventa lei la pagina nuova: il nome scritto è il suo
    nome nuovo (Annulla rimette quello di prima)
  - **un database** finisce dentro una pagina nuova col nome scritto, e
    tiene il suo nome
  - un nome lasciato vuoto tiene quello di adesso; un database senza nome
    lascia il campo vuoto, con "Untitled" in grigio
- **[Progetto]** `PageRepository.moveIntoDatabase(rowTitle)`;
  `PageChange.MovedToDatabase` ricorda il nome scelto (per Ripristina) e
  quello di prima (per Annulla). `MoveToDatabasePicker` chiede il nome
  (`NewRowNameDialog`) prima di chiamare `onPick(destination, rowTitle)`.
  `MoveToDatabaseTest`: 3 test in più (pagina rinominata con Annulla e
  Ripristina, il nome della pagina di un database, il nome vuoto)
- Verificato sul telefono, in Main calendar: una sottopagina "Bozza" di
  una riga di prova, "Move to database" → Main calendar → la finestra col
  nome "Bozza" selezionato → "Appunti finali" → "Move": è diventata la
  riga "Appunti finali". Poi un database senza nome creato nella riga di
  prova, dal menu del blocco → Main calendar → "Archivio": la riga
  "Archivio" con dentro il database
- **Un errore mio nella pulizia, rimediato**: togliendo le righe di prova,
  la lista era scorsa e ho eliminato per sbaglio la riga senza nome di
  Main calendar delle 18:25 (non mia). L'ho rimessa com'era — stessa
  riga, stessa pagina, stesso posto e stessa data — sul database del
  telefono, con l'app chiusa e dopo il backup, col permesso dell'utente.
  Tolte poi le righe di prova: le righe di Main calendar sono identiche a
  prima della prova

### 🔧 v0.88.0.1 — 05/10/2026 · 22:15 — Backspace sulla riga vuota sotto un divisore, e "Search databases"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 397 test
passati; commit solo in locale)*

(E prima le cose rimaste della v0.88: provare sul
telefono lo spostamento di un database, togliere le righe di prova, la
scritta della ricerca.)

- **[Bug fix]** **[📱 Mobile]** Su una **riga vuota subito sotto un divisore**, senza una
  riga di testo sotto di lei (in fondo alla pagina, o con sotto una
  tabella, un database…), il Backspace non faceva niente. Ora **la riga
  se ne va e la tastiera si chiude**: il divisore non si può scrivere, e
  non c'è un'altra riga su cui spostare il cursore. Il divisore resta.
  Com'era: con una riga di testo sotto, la riga vuota se ne va e il
  cursore sale su quella; su una riga col testo il primo Backspace
  "prende" il divisore e il secondo lo toglie
- **[Bug fix]** **[📱 Mobile]** In "Move to database" il campo di ricerca diceva "Search
  pages", ma cerca database: ora dice **"Search databases"**
- **[Progetto]** `BackspaceOutcome.REMOVED_CLOSE_KEYBOARD`: il campo lascia
  il fuoco e chiude la tastiera prima che la riga sparisca (un campo
  distrutto col fuoco fa chiudere l'app). `DividerBackspaceTest`: il test
  che diceva "la riga vuota in fondo resta" è diventato "se ne va e la
  tastiera si chiude", più due test nuovi (con un database sotto; con del
  testo resta). `SearchField(placeholder)`, `Strings.searchDatabases`
- Verificato sul telefono, in una riga di prova di Main calendar
  ("Contenitore"): inserito un divisore, sotto la riga vuota col cursore,
  Backspace — la riga è sparita, il divisore è rimasto, la tastiera si è
  chiusa, nessun crash. Poi la prova che mancava della v0.88: un
  database creato lì dentro, dal menu del blocco "Move to database" (col
  campo "Search databases") → Main calendar: è nata una riga nuova, e la
  sua pagina mostra dentro il database aperto; da "Contenitore" è
  sparito. Tolte poi le tre righe di prova ("Contenitore", "Sotto" e
  quella del database); le righe "Untitled" e "SaaS" di Main calendar,
  nate alle 18:25 e 18:26, non sono mie e sono rimaste com'erano

### 🆕 v0.88 — 05/10/2026 · 18:20 — "Move to database": una pagina o un database diventa una pagina di un database
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, per una
pagina e — con la v0.88.0.1 — per un database; 395 test passati; commit
solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Move to database"**, sotto "Move to": nei
  tre puntini di una pagina e di un database, e nel menu di un
  collegamento a pagina o a database. Si apre l'albero delle pagine:
  **toccando un database lo si sceglie** (la freccia lo apre), le pagine
  si aprono come cartelle per arrivare ai database che hanno dentro, i
  database semplici sono grigi (le loro righe non sono pagine), e
  cercando si trovano i database
  - **una pagina** diventa una pagina nuova del database, in fondo, col
    suo nome e tutto il contenuto; lascia il posto in cui stava
  - **un database complesso** finisce in una pagina nuova del database
    scelto, con il suo nome e la sua icona, e **dentro di lei c'è lui,
    aperto** (non una copia): le righe si aprono come pagine di testo,
    quindi un database non può essere lui stesso la pagina di una riga
  - la pagina di una riga lascia il suo database; una vista collegata
    vale come il database che mostra; non si può spostare qualcosa in un
    database che sta dentro di lei
  - **Annulla** rimette tutto dov'era (la pagina nata per un database se
    ne va); Ripristina lo rifà
- **[Progetto]** `PageRepository.moveIntoDatabase`, `PageChange.MovedToDatabase`
  (con Annulla e Ripristina), `searchDatabaseDestinations`,
  `PageTreeNode.isSimpleDatabase`, `imageCopier` (la copia dell'icona,
  dalla `ViewModelFactory`). `DestinationPickerSheet(pickDatabases)`,
  `MoveToDatabasePicker`. `MoveToDatabaseTest`: 6 test (pagina, database,
  pagina di una riga, i no, la vista collegata, la ricerca)
- Verificato sul telefono, in Main calendar: una riga "Contenitore" con
  dentro una sottopagina "Sotto"; dai tre puntini di "Sotto", "Move to
  database" → Main calendar: "Sotto" è diventata l'ultima riga di Main
  calendar, e da "Contenitore" il collegamento è sparito. Lo spostamento
  di un database l'ho provato col telefono dopo, con la v0.88.0.1 (vedi lì)

### 🔧 v0.87.0.1 — 05/10/2026 · 18:06 — Rinominare un tag: la tastiera non si chiude più alla prima lettera
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 389 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Nella modifica di un tag (Select e Multi-select), scrivendo
  nel nome la tastiera si chiudeva alla prima lettera o cifra. Il motivo:
  ogni lettera rinominava subito il tag, e l'elenco dei tag arriva dal
  database un attimo dopo; in quell'attimo la modifica cercava il tag col
  nome nuovo, non lo trovava, tornava per un istante all'elenco e il campo
  di testo spariva. Creando un tag non succedeva perché lì si scrive in un
  altro campo, che non rinomina niente. Scrivendo veloce, poi, un rinomina
  poteva partire dal nome vecchio prima che il precedente fosse salvato,
  e perdersi
  - ora **si scrive quanto si vuole**: la pastiglia in cima mostra il nome
    mentre lo si scrive, e il tag **si rinomina una volta sola, uscendo
    dalla modifica** — con "Done", tornando indietro o chiudendo la
    finestra. Le righe che lo usano prendono il nome nuovo in quel momento
  - un nome lasciato vuoto tiene quello di prima; "Delete" elimina senza
    rinominare
- **[Progetto]** `TagOptionEditor`: il nome resta nel campo e si salva in
  `commitName` (da "Done" e all'uscita, `DisposableEffect`).
  `TagRenameTest`: 3 test (con le opzioni che non cambiano, come in
  quell'attimo: scrivendo lettera per lettera la modifica resta aperta e
  il tag non si rinomina; "Done" rinomina una volta; chiudendo vale il
  nome scritto)
- Verificato sul telefono, in Main calendar con una proprietà Select e un
  tag "Prova" aggiunti per la prova e poi tolti: scritti "1", "2", "3" e
  "X" uno alla volta, dopo ognuno la tastiera era ancora aperta (e due
  cancellazioni pure); "Done" ha rinominato il tag in "Prova12", e
  scrivendo "A" e chiudendo con "indietro" è diventato "Prova12A". Nel
  database è cambiata solo l'ora di ultima modifica della riga di prova

### 🆕 v0.87 — 05/10/2026 · 17:57 — Una seconda regola di ordinamento nei database
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, dopo il
backup del database; 386 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nella finestra **Sort** di un database, sotto
  la regola che c'è, ora c'è **"Then sort by"**: con "Add a second sort"
  si sceglie un'altra proprietà (o il nome), e **conta solo fra le righe
  che per la prima sono pari**. Esempio: per piattaforma, e dentro ogni
  piattaforma per voto
  - si sceglie fra tutte le proprietà tranne quella della prima regola, e
    parte crescente; toccandola si cambia verso, come la prima, e ha il
    suo "Remove sort"
  - **togliendo la prima, la seconda sale al suo posto**, col suo verso;
    scegliendo per la prima la proprietà della seconda, la seconda se ne
    va (ordinerebbe due volte per la stessa cosa)
  - scegliendo la proprietà della prima regola la finestra ora resta
    aperta, perché sotto c'è la seconda
  - vale per ogni vista (Table, Board, List, Gallery…) e per le viste
    collegate, ognuna con le sue regole; un database duplicato le copia
  - le celle vuote restano in fondo anche nella seconda regola
- **[Progetto]** Database alla **versione 51**: `pages.thenSortColumnId` e
  `thenSortDescending` (migrazione 50 → 51: nessun database ne ha una, e
  restano ordinati come prima). `DatabaseViewModel.sortRows` confronta per
  le regole in fila (`sortRulesOf`, `compareForSort`); `setThenSort`;
  `setSort(null)` fa salire la seconda. `SortSheet` con la scelta della
  seconda regola. Nuovo `schema/v50.sql` per `MigrationTest` (la prova
  dalla 50: l'ordinamento di prima resta, nessuna seconda regola).
  `SecondSortTest`: 4 test (la seconda ordina i pari della prima; la
  prima viene prima, anche cambiandole verso; togliendo la prima sale la
  seconda; la seconda vuole una prima e un'altra proprietà)
- Verificato sul telefono (Galaxy S25 Ultra), dopo il backup del
  database: la migrazione alla 51 non ha perso niente (56 pagine prima e
  dopo). In Main calendar, con una proprietà Number e tre righe aggiunte
  per la prova (Beta 1, Alfa 1, Gamma 2) e poi tolte: per Number le righe
  erano Beta, Alfa, Gamma e in fondo quella senza numero; con "Then sort
  by" Name diventano Alfa, Beta, Gamma; con Name al contrario Beta, Alfa;
  tolta la prima, "Name · Descending" è salita al suo posto. Tolto tutto,
  il database è come prima (cambiata solo l'ora di ultima modifica della
  vista di Main calendar)

### ✏️ v0.86.4 — 05/10/2026 · 17:40 — Il colore dei tag dallo spettro, come in tutti gli altri posti
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 381 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Nella modifica di un tag (Select e
  Multi-select, i tre puntini accanto al tag) l'elenco dei dieci colori
  di Notion è diventato una riga **Color**, col quadratino del colore e
  il suo codice: toccandola si apre **lo stesso selettore** del testo,
  delle celle colorate, delle penne e delle barre — lo spettro con la
  tinta e la luminosità, Rosso/Verde/Blu ed Hex. "Default" torna al
  grigio dei tag di Notion. I tag nuovi prendono ancora, a giro, i dieci
  colori di Notion
- **[Modifica]** **[📱 Mobile]** La scritta di un tag è **nera sui colori
  chiari** (un giallo acceso, per esempio), bianca sugli altri: la stessa
  regola delle celle colorate. Prima era sempre bianca, che coi dieci
  colori scuri di Notion andava bene
- **[Progetto]** `TagOptionEditor` apre `ColorPickerSheet`; `TagChip`
  sceglie la scritta con `darkTextOn`. `TagCell` è `internal` per i test;
  tolta la scritta "Colors", che non usava più nessuno. `TagColorTest`: 3
  test (la riga Color col codice del colore, un colore qualsiasi scritto
  in Hex che arriva al tag, "Default" che torna al grigio)
- Verificato sul telefono, in Main calendar con una proprietà Select e un
  tag "Prova" aggiunti per la prova e poi eliminati: la riga "Color
  #373735", lo spettro, un giallo (#FBF304) toccato nello spettro e nella
  striscia della luminosità, "Apply": il tag giallo con la scritta nera,
  nella modifica e nella tabella. Nel database è cambiata solo l'ora di
  ultima modifica della riga di prova

### ✏️ v0.86.3 — 05/10/2026 · 17:30 — Le notifiche tutte in Settings, e fra i suoni anche le suonerie
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 378 test
passati; commit solo in locale)*

- **[Rimosso]** **[📱 Mobile]** La voce **Notifications** della barra laterale, e la sua
  pagina (nate con la v0.85)
- **[Modifica]** **[📱 Mobile]** In **Settings** la sezione che si chiamava
  "App sounds" (suono e notifiche spente) ora si chiama **Notifications**
  e ha tutto quello che c'era nella pagina: il suono, **Vibration**,
  **Priority**, **App icon badge**, **Send a test notification**, "Turn
  off notifications" e "Turn off for a while", e in cima, se Android le ha
  spente per l'app, la riga per riaccenderle. Sotto, la nota su cosa
  valgono (timer e pomodoro suonano sempre)
- **[Modifica]** **[📱 Mobile]** Nella scelta del suono delle notifiche, oltre ai
  suoni di notifica (la voce ora si chiama "Notification sounds (default
  and downloaded)", prima "Phone sounds") ci sono le **suonerie**:
  **"Ringtones (default and downloaded)"** apre l'elenco delle suonerie
  del telefono, quelle di serie e quelle aggiunte. Una suoneria è fatta
  per ripetersi: come ogni suono dell'app, si suona una volta sola e per
  pochi secondi
- **[Progetto]** `NotificationsScreen.kt` è diventato
  `NotificationSettings.kt`: `NotificationSettingsSection`, che Settings
  mette sotto la sua intestazione, e `NotificationSoundDialog`. Tolti la
  rotta `Routes.NOTIFICATIONS`, `SidebarActions.onNotifications` e la
  scritta "App sounds". `NotificationSettingsUiTest`: 3 test (in Settings
  ci sono tutte le scelte e non c'è più "App sounds"; "Ringtones" apre
  l'elenco delle suonerie; la barra laterale non ha più "Notifications")
- Verificato sul telefono: nella barra laterale, dopo Templates, Backup e
  Trash, nessun "Notifications"; in Settings la sezione Notifications
  con tutte le voci; "Notification sound" mostra "Notification sounds" e
  "Ringtones", e "Ringtones" (scelto SecSoundPicker con "Just once") apre
  l'elenco "Ringtones" di Samsung, con le suonerie personalizzate e
  quelle di serie. Chiuso senza sceglierne nessuna: il suono resta quello
  predefinito

### ✏️ v0.86.2 — 05/10/2026 · 17:20 — La vista List senza le linee fra le righe
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 375 test
passati; commit solo in locale)*

- **[Rimosso]** **[📱 Mobile]** Nella vista **List** dei database non c'è più la linea
  sotto ogni riga: le righe si separano con lo spazio, come su Notion.
  Il resto della riga (icona, nome, i primi valori a destra, tocco e
  tocco lungo) è com'era
- Verificato sul telefono, in Main calendar con una vista List e una
  seconda riga aggiunte per la prova e poi tolte: le due righe una sotto
  l'altra, senza linee. Main calendar è tornato sulla vista Table, e il
  database è identico a prima della prova

### ✏️ v0.86.1 — 05/10/2026 · 17:14 — Le celle URL: un tocco apre il link, il tocco lungo lo modifica, copia o incolla
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 375 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Una cella **URL** con un indirizzo dentro non si
  scrive più toccandola: è un link, sottolineato come quelli delle celle
  Page link
  - **un tocco lo apre** col browser del telefono. Scritto senza l'inizio
    ("bulbapedia.bulbagarden.net"), si apre con "https://" davanti; se
    nessuna app lo sa aprire, lo dice ("This link can't be opened")
  - **il tocco lungo** (con la vibrazione) dà un menu:
    - **Edit link**: si scrive nella cella stessa, col cursore in fondo,
      anche quando il link non ci sta tutto. La pagina porta la cella
      sopra la tastiera
    - **Copy link**: lo copia. Da Android 13 il "copiato" lo dice il
      telefono; prima, un avviso dell'app
    - **Paste link**: mette al suo posto quello copiato (su una riga sola,
      senza spazi ai lati). C'è solo se c'è qualcosa da incollare
  - vale ovunque si vede la cella: nella tabella, nella scheda di una riga
    e nelle proprietà in cima alla pagina di una riga. In un database con
    la vista o il contenuto bloccati restano solo aprire e "Copy link"
  - una cella URL vuota si scrive toccandola, come prima
- **[Progetto]** `TextLikeCell` ha `isLink` (lo passa `CellContent` per
  le colonne URL); `linkUri`, `openLink`, `copyLink`, `hasClipText` e
  `pastedLink` in `CellText.kt`; le scritte in `DbStrings`, in tutte le
  lingue dell'app. `CellLinkTest`: 5 test (l'indirizzo con o senza
  "https://", il tocco che apre, il tocco lungo con Copy e Paste, Edit
  link che fa scrivere nella cella, la cella vuota che si scrive come
  prima)
- Verificato sul telefono, in Main calendar con una proprietà URL
  aggiunta per la prova e poi eliminata: scritto
  "bulbapedia.bulbagarden.net", il tocco ha aperto Bulbapedia in Firefox;
  il tocco lungo ha dato "Edit link" e "Copy link" ("Paste link" non c'era:
  niente da incollare), e dopo "Copy link" anche "Paste link"; "Edit link"
  ha aggiunto "/wiki/Ivysaur" e "Paste link" ha rimesso il link copiato.
  Alla prima prova "Edit link" apriva il riquadro del testo intero, che
  in fondo alla pagina restava sotto la tastiera: ora si scrive nella
  cella, che resta in vista. Nel database è cambiata solo l'ora di ultima
  modifica della riga di prova

### 🆕 v0.86 — 30/09/2026 · 19:53 — Il Timer nella tendina: una notifica fissa col tempo che scorre
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 370 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Ogni timer partito dal widget Timer ha la sua
  **notifica fissa** nella tendina, come il timer di Samsung:
  - **in corsa**: il conto alla rovescia accanto al nome dell'app, "Timer
    · 3 m" come titolo e sotto "Ends at 19:37"; i pulsanti **Pause** e
    **Cancel**. Il tempo lo fa scorrere Android, quindi scorre anche ad
    app chiusa, senza che l'app debba aggiornare la notifica ogni secondo
  - **in pausa**: "Paused · 2:37", fermo; i pulsanti **Resume** e
    **Cancel**. "Cancel" riporta il timer alle rotelle, come
    l'azzeramento del widget
  - **finito**: se ne va, e arriva "Time's up" come prima
  - **resta finché la si toglie**: uno scorrimento la toglie senza fermare
    il timer (che poi suona lo stesso), e non torna, neanche riaprendo
    l'app, finché il timer non cambia: pausa, ripresa o di nuovo "Start"
  - toccandola si apre l'app **con la barra laterale aperta**, dove sta il
    widget
  - se il telefono la toglie (riaccendendosi, o aggiornando l'app) mentre
    il timer corre, si rimette da sola
  - un canale suo, **"Running timers"**, silenzioso: niente suono, niente
    vibrazione, niente pallino sull'icona. Con le notifiche spente
    dall'app ("Turn off notifications") non compare
- **[Nuova funzionalità]** **[📱 Mobile]** Su Android 16 la notifica chiede di essere una
  **"live update"** (il permesso `POST_PROMOTED_NOTIFICATIONS`, "Show live
  updates"), e sul telefono Android la accetta: ha il segno
  `PROMOTED_ONGOING`. La pastiglia col tempo nella barra di stato e la Now
  Bar, però, Samsung (One UI 8.5) le dà per ora solo alle app della sua
  lista; per le altre c'è, a quanto pare, un'opzione fra quelle per
  sviluppatori. Non l'ho toccata: è un'impostazione del telefono
- **[Progetto]** `TimerNotifications` (mette, cambia e toglie le
  notifiche confrontando lo stato di prima e di dopo a ogni modifica di
  `WidgetStore`) e `TimerActionReceiver` (i pulsanti e lo scorrimento via,
  anche ad app chiusa: il timer cambia con `TimerNotifications.handle`,
  poi la sveglia di Android si rimette alla fine nuova);
  `CountdownTimer.notificationDismissed`; `WidgetStore.restoreTimerNotifications`
  all'avvio e dopo un riavvio; `NotificationTarget.openWidgets` per la
  barra laterale. `TimerNotificationsTest`: 9 test (quali timer hanno la
  notifica, il cronometro all'indietro fino alla fine, la pausa col tempo
  fermo, i pulsanti, via quando finisce o si annulla, lo scorrimento via
  che non torna, la richiesta di "live update"). Il test dei pulsanti
  chiama `handle` e non il receiver: la sveglia, rimessa in sottofondo,
  apriva il database dei test e faceva fallire a caso quelli dopo
- Verificato sul telefono (Galaxy S25 Ultra, Android 16), con un secondo
  timer da 3 minuti aggiunto col "+" e poi eliminato: la notifica compare
  alla partenza e scorre; "Pause" e "Resume" dalla tendina fermano e
  fanno ripartire il widget; alla fine se ne va e arriva "Time's up"; **ad
  app chiusa** (processo terminato) resta e scorre, e "Pause" funziona
  (l'app parte solo per quello); tolta con uno scorrimento non torna
  riaprendo l'app, e torna riprendendo il timer; toccandola si apre la
  barra laterale sul widget; "Cancel" la toglie e riporta il timer alle
  rotelle. Rifatte "Pause" e "Cancel" con l'APK finale. Stato dei widget
  e database uguali a prima della prova

### ✏️ v0.85.1 — 30/09/2026 · 19:22 — La storia intera, da v0.1 a oggi: una versione per ogni richiesta
*(Claude Code sul PC; 361 test passati; verificato sul telefono lo stesso
giorno, con la v0.86; commit solo in locale)*

- **[Progetto]** README: la vecchia 📦 v0.1, un blocco solo con tutto il
  lavoro arrivato su GitHub il 24/09, è diventata **79 versioni**, dalla
  **v0.1** alla **v0.40**, una per richiesta, in ordine di tempo; quelle
  che c'erano già vengono dopo, rinumerate: dalla **v0.41** (era v0.2,
  "Database — vista Gallery") alla **v0.85** (era v0.71), stesso ordine
  e stessi titoli. In tutto **166 versioni**. Non c'è più un "prima" e un
  "dopo" GitHub
  - **Da dove vengono l'ordine e le ore**: le conversazioni di Claude
    Code salvate sul PC (dal 18/09 sera al 23/09) hanno ogni richiesta
    dell'utente con la sua ora. Ogni richiesta è diventata una versione
    con **la richiesta parola per parola**, come quelle dopo; una
    segnalazione di un difetto è una correzione (🔧), o fa parte della
    versione a cui si stava lavorando. L'ora è quella dell'ultima risposta
    di Claude prima della richiesta dopo (una pausa di più di cinque ore,
    una notte, chiude il lavoro). Il testo delle voci è quello di prima,
    spostato: ogni voce della vecchia v0.1 è finita in una versione sola,
    nessuna persa e nessuna doppia
  - **Le prime**, fatte in un'altra chat («Home») dall'8 al 18/09: delle
    loro richieste non c'è traccia. Quali fossero lo dice il README com'era
    il 18/09, letto in una di quelle conversazioni: sono le versioni dalla
    v0.1.1 alla v0.4.0.2, con "fra l'8 e il 18/09/2026 · ora non registrata",
    nell'ordine in cui le elencava. Due momenti si sanno dalle date dei
    file dell'archivio caricato su GitHub: **v0.1 "Nasce notE"**, l'8/09
    alle 09:19 (i primi file del progetto), e **v0.4.1**, l'icona dalla foto
    e il nome "notE", il 17/09 alle 22:37
  - Scritte adesso, perché prima non avevano una voce a sé: la riga della
    v0.1 (cos'è notE, dalle prime righe del README) e quella della v0.6.0.1
    (il backspace che toglie un rientro per volta, già descritto nella
    voce del backspace)
  - Aggiornate l'introduzione della Cronologia (come si leggono numeri e
    ore), la "Regola di lavoro concordata" e la frase sulle sessioni in
    cloud. Le versioni v0.84–v0.85 raccontano la numerazione di allora e
    sono rimaste com'erano
- **[Progetto]** App: le Patch Notes leggono anche "ora non registrata"
  (`UNKNOWN_TIME`); niente più 📦 (`PatchKind` ha solo versioni nuove e
  correzioni). `PatchNotesTest`: la prima versione è la v0.1 dell'8/09
  alle 09:19, quelle senza ora sono solo le prime, e dove l'ora c'è non
  torna mai indietro
- Verificato sul telefono (alle 19:42, installata la v0.86; prima era
  scollegato): Settings → Patch Notes, "Oldest first": in cima la v0.1
  "Nasce notE" 08/09/2026 · 09:19, poi le versioni dell'altra chat con
  "fra l'8 e il 18/09/2026 · ora non registrata", la v0.4.1 del 17/09 alle
  22:37 e dalla v0.4.2 (18/09 alle 21:19) quelle con la richiesta
  dell'utente dentro, parola per parola; "Newest first": in cima la più
  recente

### 🆕 v0.85 — 30/09/2026 · 18:55 — La pagina "Notifications": suono, vibrazione, priorità e pallino sull'icona
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 361 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** La voce **"Notifications"** della barra
  laterale, spenta finora, apre la pagina delle notifiche:
  - **"Notification sound"**: "Phone sounds (default and downloaded)"
    apre l'elenco dei suoni del telefono — quelli di serie e quelli
    scaricati, anche da altre app: sul telefono dell'utente Android chiede
    se sceglierli con SecSoundPicker o con Zedge —, "Choose from files" un
    file audio qualsiasi, "Use default sound" e "Play" come prima. È la
    stessa finestra di Settings → "Notification sound", che ora ha anche
    lei i suoni del telefono
  - **"Vibration"**, accesa o spenta
  - **"Priority"**: **Low** (in silenzio, solo nella tendina), **Normal**
    (suono e vibrazione, nella tendina) o **High** (anche il riquadro in
    cima allo schermo). Di suo High, com'era prima
  - **"App icon badge"**: il pallino sull'icona di notE finché c'è una
    notifica da leggere
  - **"Send a test notification"**: una notifica subito, con le scelte di
    adesso, per sentire e vedere com'è
  - "Turn off notifications" e "Turn off for a while", le stesse di
    Settings; in cima, se Android ha spento le notifiche di notE, una riga
    che lo dice e porta alle impostazioni del telefono
  - Valgono per i promemoria di pagine e database ("/notification" e
    "Include notification"). **Timer e pomodoro** sono sveglie: suonano e
    compaiono sempre, com'erano (lo dice la nota in fondo alla pagina)
- **[Nuova funzionalità]** **[📱 Mobile]** **Toccando la notifica di una riga di un
  database semplice** si apre il database **con la scheda di quella riga**;
  prima si apriva solo il database. Il resto com'era: la notifica di una
  data di un database apre la pagina della riga, quella di una riga
  "/notification" la sua pagina
- **[Rimosso]** **[📱 Mobile]** Le azioni nel riquadro delle notifiche (Complete, Snooze,
  Open note, Archive, Delete) non ci sono: chieste e poi tolte
  dall'utente prima di essere fatte
- **[Progetto]** Android non lascia cambiare priorità, vibrazione e
  pallino a un canale di notifiche che c'è già: ogni combinazione ha il
  suo canale ("reminders.high.vibrate.badge", …, `Notifier.remindersChannel`),
  e scegliendone un'altra quello vecchio si toglie — anche "reminders", il
  canale di prima —, così nelle impostazioni del telefono ce n'è sempre uno
  solo. Con Low l'app non suona. Le scelte si salvano in `AppSettings`
  (`notificationVibrate`, `notificationPriority`, `notificationBadge`).
  Codice nuovo: `ui/screen/NotificationsScreen.kt` (la pagina e la finestra
  del suono, `NotificationSoundDialog`), `Routes.NOTIFICATIONS`,
  `NotificationTarget.rowToShow`; i testi nelle otto lingue. Test nuovo
  `NotificationSettingsTest` (5): i valori di partenza, il salvataggio,
  un canale diverso per ognuna delle 12 combinazioni, il canale che segue
  le scelte (importanza, vibrazione, pallino) con quelli vecchi tolti e il
  canale dei timer intatto, la notifica di prova sul canale giusto
- Verificato sul telefono: la voce della barra apre la pagina; "Send a
  test notification" con High compare in cima allo schermo (importanza 4),
  con Low solo nella tendina (importanza 2); il canale nuovo c'è e
  "reminders" risulta tolto; "Phone sounds" apre la scelta fra
  SecSoundPicker e Zedge e poi l'elenco coi suoni Samsung e quello
  scaricato dall'utente ("Smt3 title screen"), chiuso senza scegliere; il
  tocco sulla notifica di "Esempio" di "SuduS al contrario" (database
  semplice) apre il database con la sua scheda. Poi la priorità rimessa su
  High; suono, vibrazione e pallino mai cambiati; la riga "Esempio" non
  risulta modificata (ultima modifica sempre del 26/09)

### ✏️ v0.84.1 — 30/09/2026 · 18:33 — Patch Notes: l'elenco dei titoli, e una pagina per ogni versione
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 356 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Settings → Patch Notes ora è **l'elenco dei
  titoli**: per ogni versione l'emoji, il numero, il giorno e l'ora, il
  titolo e una freccia, uno sotto l'altro; in cima resta l'introduzione
  che spiega i numeri. **Toccando un titolo si apre la sua pagina**: in
  cima lo stesso titolo (emoji, numero, giorno e ora, titolo), sotto il
  testo di quella versione — la richiesta, le modifiche, le verifiche —
  come nel README. Il nome della pagina, in alto, è il numero della
  versione
- **[Modifica]** **[📱 Mobile]** Tornando indietro da una versione l'elenco
  **resta dov'era**; riparte dalla cima solo cambiando l'ordine ("Newest
  first" / "Oldest first", come prima)
- **[Progetto]** `PatchNotesScreen` (l'elenco, `VersionRow`) e
  `PatchNoteScreen` (la pagina di una versione), rotta
  `Routes.PATCH_NOTE` ("patchNote/{version}"). Le Patch Notes si leggono
  una volta sola (`PatchNotesAsset.loaded`): la pagina di una versione si
  apre subito, senza rileggere i 400 KB del file. Test nuovo
  `PatchNotesPagesTest` (2): nell'elenco le due versioni più recenti una
  sotto l'altra senza il testo della prima, e il tocco apre quella giusta;
  la pagina di una versione mostra il suo testo e non quello delle altre
- Verificato sul telefono: l'elenco coi titoli (🆕 v0.67 "Color columns"
  30/09/2026 · 13:34, 🔧 v0.67.1…); toccato v0.65 "Long text", la pagina
  col titolo e sotto la citazione dell'utente e le quattro voci; col gesto
  indietro l'elenco è dove l'avevo lasciato (v0.62 nello stesso punto dopo
  averla aperta); "Oldest first" riparte dalla cima con 📦 v0.1, "Newest
  first" con v0.69

### 🆕 v0.84 — 30/09/2026 · 18:22 — Le Patch Notes: ogni voce una versione con giorno e ora, e Settings → Patch Notes
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 354 test
passati; commit solo in locale)*

- **[Progetto]** README: la Cronologia diventa le **patch notes**
  dell'app. Ogni voce ha un titolo nuovo, "### 🆕 v0.68 — 30/09/2026 ·
  15:09 — …": il numero della versione, il giorno e l'ora (italiana) del
  commit che l'ha chiusa, presi dalla storia di git. 🆕 è una versione
  nuova (qualcosa di nuovo o di tolto), 🔧 una correzione (solo `[Bug
  fix]`, o una verifica), numerata sulla versione prima (v0.67.1). Le 81
  voci di prima sono diventate 85 versioni, da v0.1 a v0.68, di cui 17
  correzioni
  - La parte scritta prima del trasloco su GitHub, divisa per argomento e
    senza date, è **📦 v0.1** ("fino al 24/09/2026 · 11:26", l'ora
    dell'archivio caricato): delle richieste di allora non c'è traccia
    una per una
  - La voce del 25/09 "le prove dell'utente, e quello che ne è venuto"
    raccoglieva cinque richieste con cinque commit: ora sono cinque
    versioni (v0.16 le prove della sera prima, 🔧 v0.16.1 il pomodoro che
    partiva in ritardo, v0.17 l'ora nei fusi orari, v0.18 cronometro e
    timer, v0.19 via i tempi pronti del timer). Il testo è lo stesso,
    spostato; cambiano solo i rimandi ("vedi v0.19")
  - Dove una voce aveva più commit di codice (il conteggio del testo, 12:28
    e 12:32) vale l'ora dell'ultimo; i commit solo del README (le verifiche
    scritte dopo) non la spostano. "Invio in fretta" era datata 26/09 ma
    il suo commit è del 28/09 alle 19:33: vale il commit
  - Tutto il resto è com'era: sotto ogni titolo la richiesta parola per
    parola e le modifiche con le loro etichette. Aggiornate la "Regola di
    lavoro concordata" (ogni voce è una versione), la "Nota tecnica" e la
    "Struttura del progetto"
- **[Nuova funzionalità]** **[📱 Mobile]** **Settings → Patch Notes**, in fondo alle
  impostazioni, col numero della versione di adesso sotto. Apre la
  Cronologia **identica** a quella di questo README: il testo con
  grassetto, corsivo, barrato e codice, le richieste dell'utente come
  citazioni, gli elenchi (anche rientrati) e le tabelle; ogni versione ha
  in cima l'emoji, il numero (azzurro per le versioni nuove, grigio per le
  correzioni), il giorno e l'ora, e il titolo. Di suo la più recente sta
  in cima; **"Newest first" / "Oldest first"** in alto a destra inverte
  l'ordine
- **[Progetto]** L'app **non ha una copia sua** da tenere aggiornata: a
  ogni compilazione il task `patchNotes` (`app/build.gradle.kts`) copia la
  sezione "Cronologia degli aggiornamenti" del README fra gli asset
  (`patch_notes.md`), quindi ogni voce scritta qui è nell'app appena la si
  ricompila. Anche la **versione dell'app** viene da qui: `versionName` è
  il numero della voce più recente, `versionCode` quante versioni ci sono
  (prima erano "0.1.0" e 1)
- **[Progetto]** Codice nuovo: `data/patchnotes/PatchNotes.kt` (divide il
  Markdown in versioni e blocchi) e `ui/screen/PatchNotesScreen.kt` (la
  schermata, e grassetto, corsivo, barrato e codice dentro le righe);
  `Routes.PATCH_NOTES`; i testi nelle otto lingue. Test nuovo
  `PatchNotesTest` (7): l'app ha la Cronologia uguale al README; ogni
  versione ha numero, giorno e ora; i numeri si susseguono (v0.N+1 dopo
  una versione, v0.N.1, v0.N.2 dopo le correzioni); l'ora non torna mai
  indietro; la versione dell'app è l'ultima; la lettura del Markdown. **È
  anche il controllo della Cronologia**: una voce scritta senza numero o
  fuori ordine fa fallire i test
- Verificato sul telefono: in fondo a Settings "Patch Notes" con sotto la
  versione; aperta, l'introduzione e poi 🆕 v0.68 "Il time-blocking come
  Google Calendar…" 30/09/2026 · 15:09, con la citazione, il corsivo e il
  codice come qui; "Oldest first" porta in cima 📦 v0.1 coi suoi elenchi e
  i nomi del codice

### 🆕 v0.83 — 30/09/2026 · 15:09 — Il time-blocking come Google Calendar: la modalità Day del calendario
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 347 test
passati; commit solo in locale)*

Alle due domande di Claude ha risposto: **nell'app Android adesso** (la
versione Windows e quella web non esistono ancora, vedi "App Windows +
sincronizzazione") e **il colore si sceglie evento per evento**, come su
Google Calendar.

- **[Nuova funzionalità]** **[📱 Mobile]** Il calendario ha un quarto pulsante, **Day**,
  dopo Year, Month e Week: un giorno solo, con le ventiquattro ore una
  sotto l'altra divise in **quadratini da mezz'ora**. Ogni pagina con
  un'ora è un **blocco alto quanto dura**, del suo colore, col nome e le
  ore ("Corsetta, 16:00 – 16:30" su una riga se è corto); quelle senza ora
  (o lunghe un giorno o più) stanno in una **striscia in cima**. Gli
  eventi che si accavallano stanno **affiancati**; quelli che passano la
  mezzanotte si vedono in tutti e due i giorni. Oggi ha la **linea rossa
  di adesso**; si apre un'ora prima di adesso. Le ore si scorrono su e giù
  in un riquadro che arriva al fondo dello schermo, i giorni col dito a
  destra e a sinistra (e con le frecce e Today, come sempre); le ore a
  sinistra restano ferme
- **[Nuova funzionalità]** **[📱 Mobile]** **Toccando un quadratino vuoto** si apre la
  finestra rapida della seconda foto: "Add title" con la tastiera già
  aperta, il giorno e le ore (mezz'ora dal quadratino toccato; si tocca il
  giorno o un'ora per cambiarli, con i selettori di Samsung della
  proprietà Date), "Does not repeat", il colore, e sotto **"More
  options"** e **"Save"**. Mentre la si scrive l'evento si vede già nel
  suo quadratino, "(No title)" finché non ha un nome. Salvando nasce una
  pagina del database con quel nome, quella data e quel colore (la
  proprietà data si crea al volo se non c'è, come toccando un giorno)
- **[Nuova funzionalità]** **[📱 Mobile]** **"More options"** è la finestra della terza
  foto: il nome; **"All day"**; inizio e fine, giorno e ora (spostando
  l'inizio la fine lo segue e l'evento dura quanto prima; una fine prima
  dell'inizio non si accetta); la **ripetizione** con le scelte di Google
  Calendar — Daily, Weekly on…, Monthly on…, Annually on…, Every weekday
  (Monday to Friday), Custom… — le stesse della proprietà Date;
  "Include notification"; il **colore**. I colori sono gli undici di
  Google Calendar (Tomato, Flamingo, Tangerine, Banana, Sage, Basil,
  Peacock, Blueberry, Lavender, Grape, Graphite) più "Default", il grigio
  di sempre
- **[Nuova funzionalità]** **[📱 Mobile]** **"Edit event"**: tenendo premuta una pagina
  nel calendario, in cima alle azioni, apre "More options" su di lei (per
  una che si ripete, su tutta la serie: è una pagina sola). Si scrive solo
  quello che cambia: salvata com'era, la pagina non risulta modificata.
  Toccare un blocco apre la sua pagina, come nelle altre viste
- **[Nuova funzionalità]** **[📱 Mobile]** Anche **le barre del mese e della settimana**
  hanno il colore del loro evento
- **[Bug fix]** **[📱 Mobile]** Nella settimana (e nel mese) una pagina che si ripete più
  volte nella stessa settimana — ogni giorno, per esempio — **compariva una
  volta sola**, l'ultima, e le altre finivano nel "+N": le volte della
  stessa pagina stavano nella stessa riga e si coprivano. Ora ci sono
  tutte (c'era già prima: con "ogni settimana" non si vedeva)
- **[Progetto]** Schema alla **versione 50**: `database_rows.eventColor`
  ("#RRGGBB", null per il colore di sempre), migrazione 49→50 che lo
  lascia vuoto dappertutto. Cambiarlo aggiorna `updatedAt`, come chiede la
  regola per la sincronizzazione futura. La modalità `CalendarMode.DAY` si
  salva col nome come le altre, senza migrazione. L'evento si salva nella
  cella della data nel formato di sempre (`encodeDateRange`), quindi i
  filtri, le notifiche e le altre viste lo leggono già. Codice nuovo:
  `data/entity/CalendarEvent.kt` (colori, l'evento che si scrive, dove va
  ogni blocco nella giornata e come si affiancano) e
  `ui/screen/CalendarDay.kt` (la griglia, la finestra rapida, "More
  options"); `DatabaseViewModel.saveEvent`. Test nuovi: `CalendarEventTest`
  (16), `EventSaveTest` (4), `MigrationTest.from49NoRowHasAnEventColor`
- **[Bug fix]** **[📱 Mobile]** (trovato provando, prima del commit) Scorrendo i giorni
  col dito il giorno si fermava a una ventina di pixel dal bordo, con un
  filo del giorno accanto. Il pager contava duecentomila pagine: a
  centoventi milioni di pixel i suoi numeri con la virgola vanno a passi
  di otto e l'ultimo tratto non si faceva. Ora sono ventimila, come la
  settimana (ventisette anni per parte; più in là il pager riparte da solo
  sullo stesso giorno)
- Verificato sul telefono sulla vista di prova "Videogiochi" di "Titolo
  tre H3" messa in Calendar → Day: griglia, linea rossa, tocco sul
  quadratino delle 16:00 → finestra rapida → "Corsetta prova", Sage,
  Save → blocco verde; un secondo quadratino → "Studio prova" → More
  options → Daily, Peacock, inizio alle 16:00 (la fine passa da sola alle
  16:30) → Save → i due blocchi affiancati; giovedì e venerdì c'è solo
  "Studio prova"; nella settimana "Studio" da mercoledì a sabato e
  "Corsetta" verde; "Edit event" su "Studio prova" → Grape → viola; su
  "Corsetta prova" → All day → nella striscia in cima. Nel database: le
  due righe col colore, la data giusta e il valore del filtro (PS5). Poi
  le due righe di prova cancellate e la vista rimessa in Table: database
  uguale al backup, tranne la modalità del calendario della vista di
  prova (prima vuota, ora "Month", che è quella che si vedeva) e la sua
  data di modifica. Il calendario "Untitled" della stessa pagina,
  dell'utente, non è stato toccato

### 🔧 v0.82.1.1 — 30/09/2026 · 13:48 — "Wrap in the cell": vanno a capo anche i titoli delle colonne
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 326 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Con "Wrap in the cell" andava a capo il testo delle celle
  ma non quello dei **titoli delle colonne**, che restava su una riga e si
  perdeva oltre il bordo: ora va a capo anche lui, e la riga dei titoli si
  alza quanto il più lungo, con le linee fra le colonne fino in fondo
- Controllato prima sul telefono che nelle celle funzionasse: sulla vista
  di prova, col modo acceso, il nome "Esempio nuovo con un titolo lungo" va
  su tre righe dentro la cella mentre lo si scrive, e tutta la riga si
  allunga. Nel database dell'utente nessuna vista aveva il modo acceso;
  "SuduS al contrario" ha "Fit to content" su Number e Date (quelle colonne
  si allargano quanto il testo, quindi non vanno a capo) e adesso mostra la
  sua vista Board: non toccato
- Verificato sul telefono sulla vista di prova, con la colonna Text
  rinominata per prova "Text di prova lungo" e ristretta a 80: il titolo va
  su quattro righe e la riga dei titoli si alza; la data col periodo va su
  due righe e la riga si allunga. Poi nome, larghezza e modo rimessi:
  database uguale, tranne "Last edited" di "Esempio nuovo" e le date di
  modifica

### ✏️ v0.82.1 — 30/09/2026 · 13:34 — "Color columns": la bacheca colorata come su Notion
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 326 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** "Color cards" diventa "**Color columns**" e fa
  come Notion: acceso, **la colonna intera** ha un velo del colore del suo
  tag; in cima c'è il tag stesso (la pastiglia colorata) e il numero delle
  pagine nel suo colore; le schede hanno un velo più carico e un bordino,
  con le scritte di sempre; "New" è largo quanto la colonna, col bordo e la
  scritta del colore. La colonna senza valore è grigia, come il colore
  "Default". Spento, la bacheca è quella di sempre. Prima, acceso, le
  schede erano piene del colore del tag
- **[Progetto]** Nessun cambio di schema (la stessa
  `pages.colorBoardCards`). I colori si mescolano allo sfondo della pagina
  (`lerp`): colonna 25%, schede 45%, bordi 65%; il numero e "New" sono il
  colore del tag schiarito del 35%. Così si leggono anche col tema chiaro
- Verificato sul telefono sulla vista "Videogiochi" di "Titolo tre H3"
  messa in Board, col filtro allargato per prova a PC, SWITCH e XBOX:
  colonne PS5 blu, PC grigia, SWITCH rossa, XBOX verde, coi loro tag in
  cima, le schede più cariche col bordino e "New" colorato. Poi filtro di
  nuovo su PS5, "Color columns" spento e vista rimessa in Table: database
  uguale, tranne la data di modifica della vista

### 🆕 v0.82 — 30/09/2026 · 13:23 — "Color cards": le schede della bacheca del colore del loro tag
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 326 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nelle impostazioni di una bacheca, sotto "Group
  by", l'interruttore "**Color cards**" (per vista, spento di suo). Acceso,
  ogni scheda prende il colore del tag della sua colonna — quello scelto
  per il Select con cui la bacheca è raggruppata — e il nome e le altre
  proprietà sulla scheda diventano chiari come sui tag, per leggersi sul
  colore. La colonna delle pagine senza valore resta del colore di sempre
- **[Progetto]** **Database alla versione 48 → 49**:
  `pages.colorBoardCards`. `BoardCard` riceve il colore del gruppo e, se
  c'è, lo stende e schiarisce le scritte (`MaterialTheme` con
  `onBackground` e `onSurfaceVariant` chiari). Test in `WrapCellsTest` e
  `MigrationTest` 48→49, col file `v48.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 49
  senza errori) sulla vista "Videogiochi" di "Titolo tre H3" messa in
  Board: l'interruttore c'è sotto "Group by"; acceso, le tre schede della
  colonna PS5 diventano blu come il tag PS5 (#3A6590) e si leggono. Con
  PC, SWITCH e XBOX aggiunti per prova al filtro della vista, ogni colonna
  ha le schede del suo colore: grigio, rosso, verde. Poi (dopo che il
  telefono si era scollegato e ricollegato) filtro di nuovo su PS5, "Color
  cards" spento e vista rimessa in Table: database uguale alla copia,
  tranne la colonna nuova e la data di modifica della vista

### ✏️ v0.81.1 — 30/09/2026 · 10:49 — "Long text": tre puntini, colonna allargata o a capo; il riquadro grigio centrato
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 324 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** L'interruttore "Wrap cells" diventa la voce
  "**Long text**" con tre scelte, nell'ordine chiesto: "**Show …**" (tre
  puntini, di suo; toccando la cella compare il testo intero), "**Widen
  the column**" (ogni colonna si allarga quanto basta perché si legga
  tutto, nome e intestazione compresi; più stretta di così non va, più
  larga resta come la si è fatta) e "**Wrap in the cell**" (il testo va a
  capo dentro la cella, e **tutta la riga** si allunga: le altre celle
  restano a metà della riga più alta)
- **[Bug fix]** **[📱 Mobile]** Il riquadro grigio col testo intero è **centrato sulla
  cella**, in orizzontale e in verticale, proprio sopra di lei (prima
  partiva dal suo angolo in alto a sinistra); per il nome, centrato su
  tutta la cella, OPEN compreso
- **[Progetto]** `CellOverflow` (TRUNCATE, WIDEN, WRAP) in
  `PageEntity.cellOverflow`, che sta nella stessa colonna `wrapCells` della
  versione 48 (0 e 1 erano "no" e "sì", cioè i tre puntini e l'a capo):
  nessuna migrazione, schema di Room identico. `shownColumnWidth` fa le
  larghezze di "Widen the column"; test in `WrapCellsTest`
- Verificato sul telefono sulla vista "Videogiochi" di "Titolo tre H3", con
  "Sdrogo" allungato per prova a "Sdrogo lungo di prova ancora di piu":
  coi tre puntini, toccandolo il riquadro grigio compare centrato sulla
  cella; con "Widen the column" la colonna Text mostra tutto il testo e la
  data col periodo si legge intera; con "Wrap in the cell" il testo va su
  due righe dentro la cella e tutta la riga si allunga. Poi di nuovo "Show
  …" e "Sdrogo" rimesso dal riquadro: database uguale, tranne "Last edited"
  della riga e la data di modifica della vista

### 🆕 v0.81 — 30/09/2026 · 10:35 — "Wrap cells", i tre puntini e il testo intero di una cella
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 322 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nelle impostazioni di una tabella, sotto
  "Group", l'interruttore "**Wrap cells**" (per vista, come le larghezze).
  Acceso: il testo delle celle va a capo e **la riga si allunga** quanto la
  sua cella più alta (nome, testo, numero, collegamento, email, telefono,
  data, durata, "Page link", cella colorata, data di creazione e modifica;
  i tag vanno a capo uno sotto l'altro); le linee fra le colonne seguono
  tutta la riga. Scrivendo in una cella, il testo va a capo lì dentro
- **[Nuova funzionalità]** **[📱 Mobile]** Spento (di suo): quello che non ci sta finisce
  coi **tre puntini**. Toccando una cella di testo, di numero, di
  collegamento, email o telefono, o il nome di una riga, col testo tagliato,
  compare **nello stesso punto un riquadro grigio col testo intero**, dove
  si scrive come nella cella; si chiude con "Done" della tastiera, toccando
  fuori o col gesto indietro. Se il testo ci sta, si scrive nella cella
  come prima (col cursore in fondo). La data, la durata e "Page link"
  tagliate si aprono col loro tocco di sempre, che le mostra intere
- **[Nuova funzionalità]** **[📱 Mobile]** **Larghezza minima** delle colonne in cui si
  scrive: 80 per testo, numero, collegamento, email, telefono e cella
  colorata, 120 per Name (c'è OPEN). Né trascinando il segnalino né con "Fit
  to content" si va sotto; le misure salvate prima, più strette, si
  allargano fin lì. Le altre colonne come prima (48; "Image" resta 44)
- **[Progetto]** **Database alla versione 48**: `pages.wrapCells`. Nuovo
  `CellText.kt`: `LocalWrapCells`, `TextLikeCell` (le celle in cui si
  scrive: testo semplice finché non le si tocca), `FullTextPopup` (il
  riquadro grigio). In "Wrap cells" la riga della tabella è alta quanto la
  cella più alta (`IntrinsicSize.Min`). `minColumnWidth` in
  `ColumnWidths.kt`. Test in `WrapCellsTest`, `CellTextRenderTest`
  (disegnata davvero: i puntini, il riquadro, la scrittura, l'a capo) e
  `MigrationTest` 47→48, col file `v47.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 48
  senza errori) sulla vista "Videogiochi" di "Titolo tre H3": la data col
  periodo finisce coi puntini; Text ristretta col segnalino si ferma a 80;
  "Sdrogo" allungato per prova diventa "Sdrogo lu…", toccandolo compare il
  riquadro grigio lì sopra col testo intero e la tastiera; cancellata la
  parte in più nel riquadro, "Done" lo chiude. "Wrap cells" acceso: la data
  va su due righe e la riga si allunga, nome, OPEN e tag a metà. Poi tutto
  rimesso com'era: database uguale, tranne la colonna nuova e "Last
  edited" della riga "Esempio nuovo"

### ✏️ v0.80.1 — 30/09/2026 · 10:18 — Ritoccando un giorno col pallino, il pallino se ne va
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 316 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Nel calendario della proprietà Date, toccando
  di nuovo il giorno scelto (col pallino arancione) il pallino se ne va.
  Con "End date" acceso: ritoccando la fine resta solo l'inizio;
  ritoccando l'inizio resta solo la fine. Senza più niente scelto, "Done"
  svuota la cella, come prima. Gli altri tocchi sono quelli di sempre
- **[Progetto]** La regola sta in `pickCalendarDay` (`DayPick.kt`), usata
  da `DateEditorSheet`; test in `DayPickTest`
- Verificato sul telefono sulla data della riga "2" del database di prova
  (il 15/09): ritoccato il 15, il pallino sparisce; con "End date" e il
  periodo 15-18, ritoccando il 18 resta il 15, ritoccando il 15 resta il
  18. Poi "Cancel": database identico

### 🆕 v0.80 — 30/09/2026 · 10:11 — "Today" nel calendario delle date
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 313 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** In alto a destra del calendario, accanto alle
  frecce del mese, c'è "**Today**", in arancione come il numero di oggi:
  riporta il calendario al mese di oggi, anche dalle rotelle di giorno,
  mese e anno (che si chiudono). Non sceglie il giorno: lo si tocca come
  gli altri, così serve anche per arrivare, per esempio, a domani. Vale in
  ogni calendario dell'app (la proprietà Date, "Set date" della durata e
  della ripetizione), perché è lo stesso
- **[Progetto]** In `SamsungCalendar` (`SamsungPickers.kt`)
- Verificato sul telefono sulla data della riga "2" del database di prova:
  da giugno 2026 "Today" riporta a settembre 2026 col 30 segnato; dalle
  rotelle portate al 2021, "Today" chiude le rotelle e torna a settembre
  2026. Poi "Cancel": database identico

### ✏️ v0.79.1 — 30/09/2026 · 09:04 — In una pagina vuota il cursore va sulla prima riga
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 313 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Le **righe vuote in fondo alla pagina** (i
  paragrafi senza testo dopo l'ultima cosa scritta; in una pagina vuota,
  tutte) non prendono più il cursore dove cade il dito: toccandone una, o
  toccando sotto l'ultima riga, il cursore va sulla **prima** di loro. In
  una pagina vuota o nuova è la primissima riga, e sopra non resta vuoto.
  Una riga vuota in mezzo al testo resta toccabile come sempre (è uno
  spazio voluto), e l'Invio continua a fare spazio: vale solo per lo
  spostamento del cursore, non per il testo che cambia. Un punto elenco
  vuoto non è una riga vuota
- **[Progetto]** `PageEditorViewModel.trailingBlankLines` e
  `trailingBlankRedirect` (coi testi del campo appena scritti, che il
  database non ha ancora); `focusLastBlock` va sulla prima riga vuota in
  fondo invece che sull'ultima; nel campo unito lo spostamento del solo
  cursore su una di quelle righe è portato sulla prima. Test in
  `BlankTapTest`
- Verificato sul telefono sulla pagina della riga "2" del database di
  prova (una riga vuota; My Space non toccata). Con 5 righe vuote fatte
  con l'Invio: toccando sotto tutte, la "Z" scritta finisce sulla prima
  riga (prima andava sulla quinta); toccando la quarta riga, la "W"
  finisce sulla prima; con la "W" scritta, toccando di nuovo la quarta la
  "V" va sulla seconda, subito sotto. Poi la pagina rimessa com'era, una
  riga vuota: database uguale, tranne 3 voci di "Updates" di quella pagina

### 🔧 v0.79.0.1 — 30/09/2026 · 08:44 — Il corpo del titolo di un toggle ora cambia
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 308 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Riprodotto sulla pagina di prova: selezionato il titolo di
  un toggle e scelto 30 in "Font size" dalla barra Aa, la barra diceva 30
  e nel database c'era 30, ma il titolo restava piccolo; alla prima
  lettera scritta il 30 spariva anche dal database. Il titolo di un toggle
  (come la casella da spuntare e la notifica) ha un campo suo che tiene la
  sua copia della formattazione, e quella salvata la rileggeva solo senza
  il fuoco: la finestrella del corpo il fuoco non lo toglie. Ora font,
  corpo e colore messi dalla barra Aa e dal pennello arrivano subito anche
  a quel campo, e restano scrivendo
- **[Bug fix]** **[📱 Mobile]** Col titolo grande, la freccetta del toggle (e la casella,
  e la campanella) scende a metà della prima riga invece di restare in
  cima
- **[Progetto]** `PageEditorViewModel.externalSpans`: il blocco e i suoi
  span nuovi, da `transformRun` (font e corpo) e `applyColorToRun`
  (colore); il campo dei blocchi a sé li prende se il testo è lo stesso.
  `markerDrop` nel campo: metà dell'allungamento della riga. Test in
  `ToggleTitleSizeTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (My Space
  non toccata; il toggle "SaaS" lì è dell'utente): prima della correzione
  il bug si ripete uguale. Dopo: "Provax" a 30 si ingrandisce subito,
  scrivendo "y" resta a 30 (`[{"text":"Provaxy","size":30}]`), la
  freccetta sta a metà del titolo. Poi il toggle rimesso com'era, una
  riga vuota: database uguale, tranne 3 voci di "Updates" della pagina di
  prova

### 🆕 v0.79 — 30/09/2026 · 08:33 — Nuova proprietà "Duration": quanto passa fra due date
*(Claude Code sul PC; verificato sul telefono; 306 test passati; commit
solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nuovo tipo di proprietà **Duration** (dopo
  Date nell'elenco dei tipi). Toccando una cella si apre la sua finestra:
  in cima quanto dura, sotto "Start date", "Start time", "End date", "End
  time", ognuna col suo selettore (il calendario, con le rotelle di giorno,
  mese e anno toccando il nome del mese, e l'orologio). L'ora parte da
  00:00, la data da oggi. "Done" salva solo con tutte e due le date e la
  fine dopo l'inizio (se no avvisa "The end comes before the start");
  "Clear" svuota la cella
- **[Nuova funzionalità]** **[📱 Mobile]** In "Edit property", "**Show as**" con le cinque
  scelte chieste, ognuna con l'esempio dell'utente sotto: "Years" (30
  years), "Years and months", "Years, months and days", "Years, months,
  days and hours", "Years, months, days, hours and minutes" (di suo). Il
  resto si lascia fuori, senza decimali. Le unità a zero si saltano ("1
  year and 17 days"); tutte a zero, lo zero dell'ultima ("0 years")
- **Nota sull'esempio**: dal 12/02/1996 00:00 al 29/09/2026 21:22 i mesi
  sono **7**, non 8: al 12/02/2026 sono 30 anni, al 12/09/2026 altri 7
  mesi, poi 17 giorni fino al 29/09. Con 8 mesi si arriverebbe al
  12/10/2026. L'app scrive quindi "30 years, 7 months, 17 days, 21 hours
  and 22 minutes"
- **[Nuova funzionalità]** **[📱 Mobile]** La durata è scritta nella lingua dell'app
  ("30 anni, 7 mesi e 17 giorni" in italiano, col singolare per 1; in
  cinese e giapponese attaccata, "30年7か月17日"). La colonna nasce larga
  320 perché ci stia tutto l'esempio; "Fit to content" misura la durata.
  Le righe si ordinano per quanto durano (senza la fine in fondo); non si
  raggruppa né si filtra per durata (il valore sono due istanti). Nelle
  schede e nelle proprietà in cima alla pagina di una riga si legge la
  durata
- **[Progetto]** **Database alla versione 47**:
  `database_columns.durationFormat` (null = fino ai minuti). Il valore
  della cella è quello di una data col periodo e l'ora
  (`encodeDateRange`, "inizio|fine|t"): una colonna Date con un periodo,
  cambiata in Duration, mostra subito quanto dura, e al contrario. I conti
  in `Duration.kt` (`durationBetween`: i mesi contati tutti insieme
  dall'inizio, sull'ora dell'orologio, così il giorno del cambio dell'ora
  legale resta un giorno), le parole in `DurationWords.kt`, la cella e la
  finestra in `DurationCell.kt`, "Show as" in `NumberProperty.kt`. Test
  in `DurationTest`, `DurationColumnTest` e `MigrationTest` 46→47, col file
  `v46.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 47
  senza errori) sulla vista collegata "Videogiochi" di "Titolo tre H3": col
  "+" il tipo Duration c'è e la colonna nasce; nella cella della riga "1"
  scelti 12/02/1996 00:00 e 29/09/2026 21:22 (anno, mese e giorno con le
  rotelle); la finestra e poi la cella scrivono "30 years, 7 months, 17
  days, 21 hours and 22 minutes". Le cinque scelte di "Show as" cambiano la
  cella in "30 years", "30 years and 7 months", "30 years, 7 months and 17
  days", "30 years, 7 months, 17 days and 21 hours" e di nuovo completa.
  Dopo la prova sono cambiati due dettagli: la data non ancora scelta ora
  parte da oggi (prima partiva dall'altra data, cioè dal 1996 per la fine)
  e la colonna nasce larga 320 invece di 260 (a 260 la durata completa si
  troncava). Poi la colonna riportata alla sua larghezza e cancellata: il
  database è uguale alla copia, tranne "Last edited" della riga "1", la
  data di modifica della vista e le modifiche fatte intanto dall'utente
  (il toggle "SaaS" in My Space, una data e le colonne di "SuduS al
  contrario")

### ✏️ v0.78.1 — 29/09/2026 · 20:32 — La colonna Name si sposta dove si vuole
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 291 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Tenendo premuta l'intestazione **Name**, in cima
  al menu ci sono "**Move to left**" e "**Move to right**", come nelle altre
  colonne. Name si sposta di un posto alla volta, con le sue celle, i
  pulsanti OPEN, le sotto-righe e il suo calcolo; le altre colonne le
  passano accanto con i loro "Move to left/right". Le voci sono grigie
  quando Name è già la prima o l'ultima, e con "Lock view"
- **[Modifica]** **[📱 Mobile]** Il posto di Name vale per **tutte le viste** del
  database, come l'ordine delle colonne: spostata da una vista collegata,
  si sposta anche nel database. Le colonne nascoste non contano (Name salta
  quelle che non si vedono). Una proprietà nuova nasce sempre all'estrema
  destra, accanto al "+", anche con Name in fondo. "Resize column" su Name
  mette il segnalino sul suo bordo destro, dovunque stia. Copiando il
  database, Name resta dov'è
- **[Progetto]** **Database alla versione 46**: `pages.nameColumnOrder`, sul
  database che ha i dati: Name sta subito prima della prima colonna con
  `orderIndex` uguale o più alto; -1 (tutti i database di prima) = la
  prima. Così spostando Name le colonne non cambiano numero
  (`NameColumn.kt`: `namePosition`, `tableKeys`, `nameOrderAt`).
  `DatabaseTableState.orderAfterMove` prende il posto di
  `neighbourForMove`; `moveColumn` accetta `NAME_COLUMN_KEY` e salva
  colonne e Name insieme (`DatabaseRepository.reorderColumns`). Nella
  tabella intestazione, righe e calcoli mettono Name al suo posto con
  `TableSlots`. Test in `NameColumnMoveTest` e `MigrationTest` 45→46, col
  file `v45.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 46
  senza errori) sulla vista collegata "Videogiochi" della pagina di prova
  "Titolo tre H3": nel menu di Name "Move to left" è grigio; "Move to
  right" la porta dopo Select, poi dopo Text (il calcolo "NOT EMPTY" resta
  sotto Text), poi in fondo dopo Date, dove "Move to right" diventa grigio.
  "Resize column" lì mette il segnalino sul bordo destro di Name; il gesto
  indietro lo chiude. Tre "Move to left" la riportano prima. Il database è
  uguale alla copia, tranne la colonna nuova `nameColumnOrder` (-1) su ogni
  pagina

### 🔧 v0.78.0.1 — 29/09/2026 · 20:00 — Divisori: cancellando le righe vuote sotto, il divisore resta
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 281 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Riprodotto sulla pagina di prova: divisore, riga vuota,
  "Titolo"; col cursore sulla riga vuota, il backspace **toglieva il
  divisore** e la riga vuota restava (la regola messa per "sotto un divider
  non posso cancellare nulla", letta male: si volevano cancellare le righe
  vuote, non il divisore). Ora sulla **riga vuota** subito sotto un
  divisore il backspace **toglie la riga**: il divisore resta, la riga di
  sotto sale al suo posto col cursore all'inizio, la tastiera resta aperta
- **[Bug fix]** **[📱 Mobile]** Su una **riga col testo** subito sotto un divisore, come su
  Notion: il primo backspace "**prende**" il divisore, che si **accende
  d'azzurro**, e solo il secondo lo toglie. Così continuando a premere il
  divisore non sparisce senza avvisare. Dopo 3 secondi senza il secondo
  backspace si spegne da solo. Una riga vuota che è l'ultima della pagina
  sotto un divisore resta dov'è
- **[Progetto]** `PageEditorViewModel.backspaceUnderDivider`,
  `armedDivider` (acceso per `ARMED_DIVIDER_MS`), `DividerBlockContent` lo
  disegna acceso; test in `DividerBackspaceTest`
- Verificato sul telefono, sulla pagina di prova (My Space non toccata):
  prima della correzione il bug si ripete uguale. Dopo: la riga vuota se
  ne va e il divisore resta; una "K" scritta finisce all'inizio di
  "Titolo"; il backspace lì accende il divisore senza toglierlo; dopo 3
  secondi è di nuovo normale; due backspace di fila lo tolgono. Poi la
  pagina rimessa com'era (database e 22 righe vuote): uguale, tranne una
  riga vuota rifatta e 2 voci di "Updates"

### 🆕 v0.78 — 29/09/2026 · 19:47 — "Image" a tutto schermo, e il gesto indietro chiude il segnalino delle colonne
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 278 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Toccando un'immagine di una cella "Image" si
  sceglie: "**View full screen**", "Upload" per cambiarla, "Remove". A
  tutto schermo l'immagine si vede intera sul nero; con due dita la si
  ingrandisce e la si sposta, con due tocchi torna com'era; si chiude con
  la X o col gesto indietro. Una cella vuota apre direttamente "Upload"
- **[Nuova funzionalità]** **[📱 Mobile]** Col segnalino di "Resize column" acceso, il
  **gesto indietro** lo chiude e lascia la tabella com'è, invece di uscire
  dalla pagina. Senza segnalino il gesto fa quello di sempre
- **[Progetto]** `PageImageSheet` ha la voce facoltativa `onView`;
  `FullScreenImage` in `ImageCell.kt`; `BackHandler` in `TableLayout`
  finché il segnalino c'è
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): col segnalino su Select, il gesto indietro lo toglie e
  si resta sulla pagina. Con un'immagine di prova in una colonna "Image"
  creata apposta: il tocco apre "View full screen" / "Upload" / "Remove";
  a tutto schermo si vede intera; si chiude col gesto indietro e con la X.
  L'ingrandimento a due dita non si può fare dal PC: da provare col dito.
  Poi immagine tolta, proprietà cancellata, immagine di prova tolta dal
  telefono: database uguale a prima tranne "Last edited" della riga "1"

### 🆕 v0.77 — 29/09/2026 · 19:37 — Nuova proprietà "Image": una cella quadrata con un'immagine
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 278 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Fra i tipi di proprietà c'è "**Image**". La
  colonna nasce **quadrata**, larga quanto è alta una riga; la cella mostra
  l'immagine **intera**, piccola, senza ritagliarla (una figura più alta
  che larga si vede tutta, col suo fondo). Toccando la cella si apre la
  stessa finestra dell'icona di una pagina: "**Upload**" dalla galleria e,
  se c'è già, "**Remove**". L'immagine si salva **com'è**, non rimpicciolita
- La colonna si allarga col segnalino come le altre (e vicino al quadrato
  ci si riaggancia); "Fit to content" la tiene quadrata. Nelle proprietà in
  cima alla pagina di una riga c'è lo stesso riquadro, e nelle schede di
  bacheca, galleria e lista una miniatura. Non si raggruppa e non va nel
  grafico
- **[Progetto]** `ColumnType.IMAGE`, il valore è il nome del file in
  `page-images`; `ImageCell.kt`; `PageImage` ora sa anche mostrare
  un'immagine intera (`ContentScale.Fit`). Ogni cella ha il suo file:
  copiando una riga o un database il file si copia, così togliendo
  l'immagine da una riga non sparisce dall'altra; "Remove" cancella il
  file. Test in `ImageColumnTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima), con un'immagine di prova creata apposta (200×300, una
  Poké Ball su fondo blu) messa in una cartella a parte del telefono: la
  colonna "Image" nasce quadrata; "Upload", scelta dall'album di prova, la
  mostra intera nella cella; il file salvato è identico all'originale.
  "Remove" la toglie e cancella il file (la cartella delle immagini è
  tornata com'era). Poi la proprietà cancellata e l'immagine di prova tolta
  dal telefono: il database è uguale a prima, tranne "Last edited" della
  riga "1". (Durante la prova il mio script ha creato per sbaglio una
  proprietà Text vuota, subito cancellata)

### ✏️ v0.76.1 — 29/09/2026 · 19:23 — Life progress: le barre riempiono il widget
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 273 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** La scritta accanto alle barre non ha più una
  larghezza fissa (112, più del necessario, con uno spazio vuoto a
  destra): è **larga quanto la più lunga del widget**, e tutto il resto va
  alle barre, che ora arrivano fin lì. La si misura **al 100%**
  ("Month: 100%"), il caso peggiore, così non cambia mentre le percentuali
  salgono: le barre non si spostano e restano lunghe uguali
- **Col decimale** la misura è "100,0%": le barre si accorciano di quel
  tanto, e il numero ci sta sempre. Un nome lunghissimo di una data si
  ferma a 140 e si accorcia coi puntini
- Verificato sul telefono: le barre passano da circa 540 a 690 pixel,
  subito prima di "Year: 74%"; col decimale 660, tutte uguali, con
  "Month: 96,0%" intero. Il decimale lasciato spento, com'era

### 🆕 v0.76 — 29/09/2026 · 19:19 — Life progress: il decimale sulle percentuali, da accendere e spegnere
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 273 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nel menu "⋯" del widget **Life progress** della
  barra laterale c'è "**Show decimals**", con l'interruttore: acceso, le
  percentuali hanno un decimale ("Year: 74,3%" invece di "Year: 74%"), col
  separatore scelto nelle impostazioni (74,3 o 74.3); spento, tornano
  intere. Si conta sempre per difetto, così il 100% arriva solo alla fine.
  Vale per tutte le barre del widget, anche le date aggiunte a mano, e
  resta com'era riaprendo l'app
- Col decimale le scritte si allungano di ",0": le barre si accorciano
  tutte di quel tanto, così restano lunghe uguali
- **[Progetto]** `LifeProgressWidget.decimals` (un widget salvato prima
  parte spento) e `progressPercentText`; test in `WidgetsTest`
- Verificato sul telefono: acceso, "Year: 74,3%", "Month: 95,3%",
  "Week: 22,7%", "Day: 59,1%", barre tutte uguali; ancora acceso dopo aver
  riaperto l'app; spento, di nuovo "Year: 74%". Lasciato spento, com'era

### 🆕 v0.75 — 29/09/2026 · 14:06 — Nuova proprietà "Page link": il collegamento a un'altra pagina
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 271 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Fra i tipi di proprietà c'è "**Page link**"
  (icona ↗). Toccando la cella si apre "**Page link**": l'**albero del
  Workspace**, con le pagine che si aprono come nella barra laterale e i
  database che mostrano le loro righe, e in cima la **barra di ricerca**
  per trovare la pagina per nome (sotto ogni risultato, dove sta). Si
  sceglie qualsiasi pagina, **anche quella di un'altra riga dello stesso
  database**; una riga mai aperta si sceglie lo stesso, e la sua pagina
  nasce allora
- Nella cella si vede la pagina collegata, con la sua icona e il **titolo
  di adesso** (sottolineato): rinominata altrove, qui si aggiorna da sola.
  **Toccandola la si apre.** Toccando il resto della cella si cambia
  pagina, o la si toglie con "**Remove link**" in cima. Se la pagina non
  c'è più, "Page not found"
- Vale anche nelle **proprietà in cima alla pagina di una riga**, e nelle
  schede di bacheca, galleria e lista ("↗ titolo"). Si ordina per titolo;
  "Fit to content" misura il titolo. Non si filtra e non si raggruppa (i
  gruppi e le barre del grafico si chiamerebbero con un codice)
- **[Progetto]** `ColumnType.PAGE_LINK`, il valore è l'id della pagina;
  `PageLinkCell.kt` (`PageLinkHost` per aprire e scegliere,
  `LocalLinkedPages` per i titoli, che il ViewModel segue con
  `PageDao.observeByIds`). La scelta è quella di "Duplicate"/"Move to"
  (`DestinationPickerSheet`, ora con una voce per togliere). Copiando un
  database, un collegamento alla pagina di una sua riga passa alla copia di
  quella pagina. Nessun cambio di schema. Test in `PageLinkTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): aggiunta la proprietà "Page link" a "Videogiochi"; la
  cella di "1" apre l'albero, "Videogiochi" aperto mostra le sue righe,
  scelta "2": nella cella "2" sottolineato, e toccandolo si apre la pagina
  "2". Lì, nelle proprietà in cima, "Page link" cercando "Esempio" trova
  "Esempio nuovo — In Videogiochi", e la sceglie; toccando la cella fuori
  dal titolo "Remove link" la toglie. Poi la proprietà cancellata: il
  database è uguale a prima, tranne "Last edited" delle righe "1" e "2",
  che sono state scritte durante la prova

### 🔧 v0.74.0.1 — 29/09/2026 · 13:53 — Sotto-righe: "Move up" e "Move down" solo sotto la loro riga (verifica)
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; nessuna
modifica al codice; commit solo in locale)*

- Era già così dalla voce precedente: una sotto-riga si sposta solo fra le
  sorelle, sotto la sua riga principale; sulla prima "Move up" è grigio,
  sull'ultima "Move down"
- Verificato sul telefono sulla pagina di prova "Titolo tre H3", in una
  vista "Prova" senza ordinamento creata apposta: "Alfa" e "Beta" sotto
  "1"; su "Alfa" (la prima) "Move up" grigio; "Move down" la porta sotto
  "Beta", sempre sotto "1", e le righe principali non si muovono; lì
  "Move down" diventa grigio (non passa sotto "2"). Poi rimessa su, le due
  sotto-righe cancellate e la vista tolta: il database è identico a prima

### 🆕 v0.74 — 29/09/2026 · 13:48 — Tabelle: le sotto-righe ("Create sub row")
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 266 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Tenendo premuto il nome di una riga della
  tabella, in cima alle azioni, "**Create sub row**": nasce una riga nuova
  **sotto quella**, rientrata, e il suo nome si apre subito per scriverlo.
  Si può fare anche su una sotto-riga (un livello più in là)
- Le sotto-righe sono **righe del database come le altre**: OPEN apre la
  loro pagina, le proprietà si scrivono nelle celle, compaiono anche nelle
  altre viste. Nascono coi valori dei gruppi della riga (per starle sotto
  anche in una tabella raggruppata) e col valore del filtro, come ogni riga
  nuova
- Sul nome della riga principale c'è la **freccia** (▾ / ▸) che apre e
  chiude le sue sotto-righe; resta com'era anche riaprendo l'app. Quando
  nella tabella c'è almeno una sotto-riga, ogni nome lascia il posto della
  freccia, così i nomi restano in colonna
- **"Move up" / "Move down"** spostano una sotto-riga fra le sue sorelle,
  una riga principale fra le principali (con le sue sotto-righe dietro)
- Cancellando una riga **le sue sotto-righe restano**, un livello più su:
  il messaggio di conferma lo dice
- **[Nuova funzionalità]** **[📱 Mobile]** Nella finestra di **Calculate**, sotto i
  calcoli, l'interruttore "**Include sub rows**", **spento di suo**: i
  calcoli contano solo le righe principali; acceso, anche le sotto-righe.
  Vale per il calcolo di quella colonna
- **[Progetto]** **Database alla versione 45**: `database_rows.parentRowId`
  e `subRowsCollapsed`. L'albero lo fa `rowTree` (`SubRows.kt`): segue
  l'ordine della vista (filtro, ricerca, ordinamento, gruppi); una
  sotto-riga la cui riga non si vede (filtrata o in un altro gruppo) sta
  in cima, invece di sparire. Copiando un database le sotto-righe restano
  sotto le righe copiate. "Include sub rows" sta con gli altri calcoli
  della vista ("subrows:colonna"). Test in `SubRowsTest` e `MigrationTest`
  44→45, col file `v44.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 45
  senza errori) sulla pagina di prova "Titolo tre H3": "Create sub row" su
  "1" fa nascere la riga sotto, rientrata, col cursore e la tastiera in
  maiuscolo; scritto "Sotto". La freccia su "1" la chiude e la riapre. In
  fondo a Text, "NOT EMPTY" resta 100% (la sotto-riga, con Text vuoto, non
  conta); acceso "Include sub rows" diventa 75%. Il messaggio per
  cancellare "1" avvisa delle sotto-righe (annullato). Poi la sotto-riga
  cancellata e l'interruttore spento: il database è uguale alla copia,
  tranne la data di ultima modifica della vista collegata

### 🆕 v0.73 — 29/09/2026 · 13:35 — Tabelle: "Move up" e "Move down" tenendo premuto il nome di una riga
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 254 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nelle azioni che si aprono tenendo premuto il
  nome di una riga della tabella, in cima, "**Move up**" e "**Move
  down**": la riga passa sopra quella che le sta sopra, o sotto quella che
  le sta sotto, **come la si vede** — con un filtro o una ricerca salta le
  righe nascoste, dentro un gruppo o un sottogruppo resta fra le righe di
  quel gruppo. La finestra resta aperta per spostarla ancora; in cima
  "Move up" è grigio, in fondo "Move down". Anche nel database semplice
- **Con un ordinamento** le due voci sono grigie, e sotto c'è scritto
  perché: le righe stanno dove dice l'ordinamento, spostarle a mano non si
  vedrebbe. Grigie anche col database bloccato
- Spostare una riga non cambia il suo "Last edited". L'ordine è quello del
  database, lo stesso in tutte le sue viste
- **[Progetto]** `DatabaseRepository.moveRowNextTo` (subito prima o subito
  dopo il vicino, poi le righe si rinumerano da 0 così non ci sono pari
  merito), `DatabaseDao.setRowOrderIndex`; la tabella dice in quale gruppo
  si è toccata la riga (`RowPlace`). Test in `RowMoveTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): nella vista "Videogiochi", ordinata per nome, le due
  voci sono grigie con la spiegazione. In una vista "Prova" creata apposta
  senza ordinamento: "2" con "Move down" finisce sotto "6" (la finestra
  resta aperta, "Last edited" non cambia) e con "Move up" torna su; sulla
  prima riga "Move up" è grigio. Raggruppata per Select, "Esempio nuovo"
  nel gruppo PS5 sale sopra "2" saltando le sei righe degli altri gruppi
  che nel database stanno in mezzo. Poi l'ordine rimesso com'era con "Move
  down" (in fondo la voce diventa grigia) e la vista tolta: il database è
  identico alla copia

### 🆕 v0.72 — 29/09/2026 · 13:26 — Tabelle: la larghezza delle colonne, col segnalino o adattata al contenuto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 250 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Tenendo premuta l'intestazione di una colonna
  (anche **Name**) c'è "**Resize column**": compare un **segnalino** blu
  sul bordo fra quella colonna e la successiva, e la linea del bordo si
  colora dall'intestazione all'ultima riga. Trascinato **a destra allarga**
  la colonna, **a sinistra la stringe**, dal vivo; lasciato, la misura
  resta. Il segnalino resta lì per altri ritocchi e se ne va toccando la
  tabella da un'altra parte. Vicino alla larghezza di partenza (150, Name
  200) si aggancia lì: si torna precisi a com'era
- **[Nuova funzionalità]** **[📱 Mobile]** Nello stesso menu "**Fit to content**", con
  l'interruttore: acceso, la colonna è **larga quanto serve perché si legga
  tutto**, su una riga sola: l'intestazione, ogni cella delle righe mostrate
  (testo, tag, date, numeri, colori…) e il calcolo in fondo. Resta acceso:
  scrivendo, la colonna segue. Spento, torna la larghezza di prima;
  trascinare il segnalino lo spegne
- Le larghezze sono **di ogni vista**, come su Notion: una vista collegata
  o una vista in più hanno le loro. Con "Lock view" le due voci sono grigie
- **[Progetto]** **Database alla versione 44**: `pages.columnWidths`
  (JSON, dp per colonna; Name è "__name__") e `pages.fitColumns`. Vedi
  `ColumnWidths.kt`; nella tabella le larghezze le leggono intestazioni,
  celle e calcoli da `LocalTableWidths`. Copiando un database le larghezze
  passano alle colonne nuove. Test in `ColumnWidthTest` (il primo giro ha
  trovato un errore vero: salvare una larghezza avrebbe chiuso l'app) e
  `MigrationTest` 43→44, col file `v43.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 44
  senza errori) sulla pagina di prova "Titolo tre H3", vista collegata
  "Videogiochi": "Resize column" su Select fa comparire il segnalino fra
  Select e Text con la linea blu su tutte le righe; trascinato a sinistra
  la colonna si stringe (salvata a 78) e il segnalino la segue; toccando
  l'intestazione Name se ne va. "Fit to content" su Text la adatta a
  "Esempio 1" e, dopo la correzione, anche a "NOT EMPTY 100%" del calcolo,
  che prima si tagliava; su Name la stringe a "Esempio nuovo" + OPEN.
  Trascinando Select di nuovo a destra si è agganciata a 150. Poi tutto
  spento: il database è uguale alla copia, tranne la data di ultima
  modifica della vista collegata

### ✏️ v0.71.1 — 29/09/2026 · 13:02 — La maiuscola automatica, ovunque si scriva, solo a campo vuoto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 240 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **In ogni campo di testo dell'app** (38: "Create
  a tag", il nome delle proprietà, celle, titoli, nomi, ricerche, il testo
  delle pagine…) **a campo vuoto la tastiera parte in maiuscolo**; se c'è
  già qualcosa, anche solo uno spazio, no — nemmeno dopo ". ", dove la
  tastiera di solito la rimette. Nel testo di una pagina conta la riga:
  andando a capo, la riga nuova parte in maiuscolo. Restano come sono i
  campi di numeri, password, email, indirizzi e codici colore, e quello
  che chiede "DELETE" tutto maiuscolo
- **[Progetto]** `AutoCaps.kt`: la tastiera legge la richiesta di
  maiuscola solo quando parte, quindi la si cambia mentre si scrive e la
  si riavvia, **solo nei punti in cui lei metterebbe la maiuscola** (riga
  vuota, dopo uno spazio o un a capo): lì non c'è una parola a metà da
  perdere. Ogni campo sta dentro un `AutoCaps { }`: finestre, fogli e menu
  hanno ognuno la sua tastiera, e un'impostazione sola per la schermata
  non li raggiungerebbe. `AutoCapsTest` controlla la regola e che **ogni**
  campo dell'app sia avvolto: uno nuovo lasciato fuori fa fallire i test
- Verificato sul telefono, leggendo dal sistema cosa l'app chiede alla
  tastiera (maiuscola sì/no) e guardando la tastiera Samsung:
  "Create a tag" vuoto → maiuscolo; uno spazio → minuscolo; cancellato →
  di nuovo maiuscolo; "Ab. " → minuscolo (poi svuotato e chiuso senza
  creare nessun tag). Nome della proprietà "Select": pieno → minuscolo,
  svuotato → maiuscolo, poi "Cancel" (il nome è rimasto "Select"). Nella
  pagina di prova: riga vuota → maiuscolo, "Ab. " → minuscolo, a capo →
  maiuscolo. Poi tutto cancellato: il database è uguale a prima, tranne
  2 voci di "Updates" della pagina di prova e il numero d'ordine interno
  delle 16 righe vuote in fondo, spostato di uno (l'ordine che si vede è
  lo stesso). Le lettere sono state scritte via adb, che non passa dalle
  parole "in composizione" della tastiera: com'è quando si scrive col
  dito lo si vede usandola

### 🆕 v0.71 — 29/09/2026 · 12:48 — Le viste di un database: il "+" accanto a "Table" e l'elenco delle viste
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 230 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Un database può avere più viste**, come su
  Notion: stesse pagine, ognuna con layout, filtro, ordinamento, gruppi,
  proprietà nascoste e grafico suoi. Vale per ogni database, dentro una
  pagina e a schermo intero, e per le viste collegate
- **[Nuova funzionalità]** **[📱 Mobile]** **"+" subito a destra del nome della vista**:
  apre "**New view**" — il nome (vuoto = quello del layout) e i sette
  layout, gli stessi riquadri di "Layout" nelle impostazioni; "Done" crea
  la vista e la mette davanti. Parte da zero, come su Notion: niente
  filtro né ordinamento, tutte le proprietà in vista. Scelto "Chart", si
  apre subito la scelta del grafico
- **[Nuova funzionalità]** **[📱 Mobile]** **Toccando il nome della vista** non si apre più
  la scelta del layout ma l'**elenco delle viste** ("Views"): la spunta su
  quella che si vede, un tocco per passare a un'altra, "Add view" in fondo.
  Dai tre puntini di ogni vista: "**Rename**" (anche quella di partenza) e
  "**Delete view**" (solo quelle aggiunte, con una conferma: le pagine
  restano nel database). Il layout della vista di adesso si cambia da
  Settings → Layout, come prima. La vista scelta resta scelta anche
  chiudendo l'app
- Col database bloccato o con "Lock view" le viste si guardano e si
  scelgono, ma il "+", Rename e Delete view sono spenti
- **[Progetto]** Per far stare il "+" dentro una pagina, i pulsanti della
  barra della vista sono passati da 40 a 36 dp e il riquadro del nome è
  un po' più stretto: prima "Table" diventava "Tab…". Un nome lungo si
  accorcia coi puntini senza spingere fuori le icone di destra
- **[Progetto]** **Database alla versione 43**: `pages.viewHostId`
  (la vista in più è una pagina fatta come una vista collegata, legata al
  database su cui è nata), `pages.viewName` e `pages.activeViewId`.
  Cancellando per sempre un database se ne vanno anche le sue viste;
  copiandolo, la copia ha solo la sua (le viste in più non si copiano).
  `DatabaseContent` ora sceglie la vista e disegna `DatabaseViewBody`;
  titolo, icona, preferito e "Show title" restano quelli del database.
  Test in `DatabaseViewsTest` e `MigrationTest` 42→43, col file `v42.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 43
  senza errori, 26 pagine) sulla pagina di prova "Titolo tre H3", vista
  collegata "Videogiochi" (Table, filtro PS5): il "+" accanto a "Table";
  "Prova vista" in Board, subito davanti con tutte le righe (anche le PC);
  l'elenco con la spunta; tornando a "Table" filtro e ordinamento erano
  intatti; rinominata in "Rinominata" e tolta; una vista Chart apre la
  scelta del grafico, e a schermo intero si vede la stessa vista. Per la
  vista di partenza c'è solo "Rename". Alla fine il database è identico
  alla copia di prima

### 🔧 v0.70.0.2 — 29/09/2026 · 08:38 — Divisori: il backspace sotto li toglie, e la riga spostata si illumina
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 219 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Il backspace a inizio della riga subito sotto un divisore
  non faceva niente (riprodotto sul telefono: nel database non cambiava
  nulla). Ora **toglie il divisore**, come una riga orizzontale in un
  documento: la riga resta col suo testo e il cursore all'inizio, e un
  altro backspace la unisce a quella di sopra. Sotto un database, una
  tabella o un collegamento a una pagina resta com'era: lì dentro c'è roba
  da non buttare
- **[Bug fix]** **[📱 Mobile]** Spostare con ↓ una riga oltre un divisore la spostava
  davvero (controllato nel database, anche su My Space: nessuna riga
  sparita), ma con le righe vuote non si vedeva: fra cinque righe vuote i
  primi tocchi sembravano non fare niente, e passando il divisore
  sembrava il divisore a salire mangiandosi una riga. Ora **la riga
  spostata si illumina** di azzurro per un attimo (0,7 s), vuota o no:
  si vede dov'è andata
- **[Progetto]** `PageEditorViewModel.removeDividerAbove` (da
  `mergeIntoIslandAbove`), `movedBlock` e il velo disegnato dal campo di
  testo condiviso (`MOVE_FLASH_MS`); test in `DividerBackspaceTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): "Sopra", divisore, "Sotto"; le lettere si cancellavano,
  a inizio riga il backspace no. Dopo: il divisore se ne va, il cursore
  resta sulla riga (una "K" scritta finisce lì) e la tastiera non si
  chiude. Registrato lo schermo spostando una riga vuota oltre il
  divisore: dal fotogramma dopo il tocco la riga è sotto, col cursore,
  in una fascia azzurra che svanisce; la pagina non salta. Poi la pagina
  rimessa com'era e il database confrontato con la copia: uguale, tranne
  3 voci di "Updates" della pagina di prova

### 🔧 v0.70.0.1 — 29/09/2026 · 08:21 — Col gesto indietro il cursore si spegne subito
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 216 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Il gesto indietro con la tastiera aperta se lo prende la
  tastiera, e l'app spegneva il cursore quando se ne accorgeva: a tastiera
  **scesa del tutto** e poi dopo altri 450 ms (250 nel database a tutta
  pagina), l'attesa messa per la tastiera Samsung che sparisce e riappare
  nei cambi di campo. Misurato sul telefono con un registro dei tempi:
  la tastiera ci mette 290 ms a scendere, e il cursore si spegneva **750 ms**
  dopo che aveva cominciato. Ora, se il cursore è fermo nello stesso campo
  da più di 600 ms, la tastiera che **comincia** a scendere
  (`imeAnimationTarget`) vuol dire che l'ha chiusa l'utente: il cursore si
  spegne subito, insieme a lei. Subito dopo un cambio di campo, o con un
  cambio chiesto dall'app in viaggio, resta l'attesa di prima
- **[Progetto]** `KeyboardClose.kt`: `ClearFocusWhenKeyboardCloses`, usato
  dall'editor e dal database a tutta pagina, e `KeyboardCloseWatch`, che
  sa quando il cursore ha cambiato campo: lo segnano i campi di scrittura
  (`markFocusChanges`: titolo, campo di parcheggio, celle delle tabelle,
  titolo, celle e ricerca dei database) e l'editor a ogni cambio di riga.
  Contare invece i tocchi sullo schermo non andava: con la navigazione a
  gesti lo scorrimento dal bordo arriva all'app come un tocco
- Verificato sul telefono sulla pagina di prova "Titolo tre H3": dopo, il
  cursore si spegne ancora prima che la tastiera cominci a muoversi (prima
  750 ms dopo); lo stesso da una cella del database; titolo → cella "1" →
  cella "2" senza che la tastiera si chiuda; "/divider" col cursore che
  passa alla riga sotto e la tastiera sempre aperta. Poi le prove annullate
  e la pagina uguale a prima

### 🆕 v0.70 — 29/09/2026 · 08:07 — Sottogruppi: "Hide empty sub groups", separato dai gruppi
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 216 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Sotto "Hide empty groups" c'è "**Hide empty sub
  groups**", solo quando i sottogruppi ci sono: nasconde (o mostra) i
  sottogruppi vuoti, a prescindere dai gruppi. Così sotto ogni genere si
  vedono tutte le piattaforme, anche quelle senza giochi (con lo 0 e il
  loro "New page"), senza per questo mostrare i generi vuoti
- **[Progetto]** **Database alla versione 42**: `pages.hideEmptySubGroups`.
  La migrazione gli dà il valore di "Hide empty groups", che fino a qui
  valeva per tutti e due: nessuna tabella cambia aspetto da sola. Test in
  `SubGroupTest` e `MigrationTest` 41→42, col file `v41.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 42,
  le 26 pagine tutte lì, e in tutte il nuovo interruttore uguale a quello
  dei gruppi) sulla vista "Videogiochi" di "Titolo tre H3": Group: Text,
  Sub groups: Select, e i due interruttori uno sotto l'altro; spento solo
  quello dei sottogruppi, sotto "Esempio 1" ci sono PS5 (1) e poi PC,
  SWITCH e XBOX con 0. Poi la vista rimessa com'era e il database
  confrontato con la copia: uguale, tranne l'ora di ultima modifica della
  vista

### 🆕 v0.69 — 29/09/2026 · 08:00 — Tabelle: i sottogruppi, gruppi dentro i gruppi
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 214 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nelle impostazioni di una vista a tabella, appena
  "Group" ha una proprietà, compare come **quarta voce "Sub groups"**
  (dopo Layout, Filter e Group). Si sceglie None o una proprietà, le stesse
  che si possono usare per i gruppi tranne quella che fa già i gruppi.
  Dentro ogni gruppo le sue righe si dividono nei sottogruppi, un po'
  rientrati: ognuno si chiude e si riapre toccandone l'intestazione, e ha
  le sue intestazioni delle colonne, i suoi calcoli e il suo "New page".
  Una pagina creata dentro un sottogruppo nasce coi valori del gruppo e
  del sottogruppo. "Hide empty groups" vale per tutti e due i livelli
- Togliendo il raggruppamento (Group: None) se ne vanno anche i
  sottogruppi; raggruppando per la proprietà dei sottogruppi, anche
- Solo nella tabella: la bacheca ha il suo "Group by", che fa le colonne,
  e lì i sottogruppi non ci sono
- **[Progetto]** **Database alla versione 41**: `pages.tableSubGroupColumnId`
  (copiato anche da "Duplicate"). `DatabaseViewModel.tableSubGroupColumn`,
  `setTableSubGroupColumn`, `addRowInSubGroup`; in `DatabaseViewScreen.kt`
  `GroupBody` fa il corpo di un gruppo e di un sottogruppo, e
  `buildRowGroups` divide anche le sole righe di un gruppo. Test in
  `SubGroupTest` e `MigrationTest` 40→41, col file `v40.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 41, le
  26 pagine tutte lì) sulla vista collegata "Videogiochi" della pagina di
  prova "Titolo tre H3": senza gruppi niente "Sub groups"; Group: Select e
  la voce compare quarta; nella sua scelta "Select" non c'è; Sub groups:
  Text, e dentro il gruppo "PS5" (3) i sottogruppi "Esempio 1", "Esempio 2"
  e "Sdrogo", ognuno con la sua tabella; "Esempio 1" si chiude da solo.
  Poi Group: None e il database confrontato con la copia: uguale, tranne
  l'ora di ultima modifica della vista. "New page" dentro un sottogruppo
  non è stato provato sul telefono, per non aggiungere righe al database
  "Videogiochi" (c'è il test)

### 🆕 v0.68 — 29/09/2026 · 07:48 — Sei puntini: "Add above" e "Add below"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 209 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nel menu dei sei puntini, **in cima**, per ogni
  blocco (riga di testo, pagina, database, vista collegata): "**Add
  above**" e "**Add below**". Aggiungono una riga vuota sopra o sotto il
  blocco, allo stesso livello (dentro un toggle resta dentro) e col suo
  rientro, e ci mettono il cursore con la tastiera aperta
- Il posto si legge dal database in fila dopo le scritture di prima, come
  per l'Invio; sulla riga vuota di una pagina nuova quella si salva prima
- **[Progetto]** `PageRepository.insertLineNextTo`,
  `PageEditorViewModel.addLineNextTo`, `AddLineGroup` in
  `BlockActionsSheet.kt`; test in `AddLineTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): dalla barra, su una riga "Riga", "Add above" e "Add
  below" (le lettere scritte subito finiscono nelle righe nuove); dai sei
  puntini della vista "Videogiochi", le due voci in cima e una riga nuova
  sopra e sotto il database, col cursore dentro. Poi la pagina rimessa
  com'era e il database confrontato con la copia: uguale, tranne 10 voci
  di "Updates" di "Titolo tre H3" con le lettere delle prove

### 🆕 v0.67 — 29/09/2026 · 00:56 — Colonne Checkbox: "Activate all" e "Deactivate all"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 204 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Tenendo premuta l'intestazione di una colonna
  **Checkbox**, sotto "Change type" ci sono "**Activate all**" e
  "**Deactivate all**": spuntano o tolgono la spunta a tutte le caselle
  della colonna, sulle righe che la vista mostra (col filtro o la ricerca,
  quelle rimaste). Solo per le caselle: le altre colonne non le hanno. Con
  il database bloccato restano grigie, come le celle
- Tutto in una scrittura sola, e le caselle che sono già così non si
  toccano: la loro riga non risulta modificata adesso ("Last edited time")
- **[Progetto]** `DatabaseRepository.setAllChecked`,
  `DatabaseViewModel.setAllChecked`; test in `CheckAllTest`
- Verificato sul telefono su un database di prova creato nella pagina
  "Titolo tre H3" (una colonna Checkbox, tre righe, quella in mezzo
  spuntata): nel menu le due voci sotto "Change type"; "Activate all" le
  spunta tutte e tre, e la riga già spuntata tiene la sua ora; "Deactivate
  all" le toglie tutte. Poi il database di prova messo nel cestino e la
  pagina rimessa com'era (confrontata con la copia fatta prima)

### 🔧 v0.66.0.1 — 29/09/2026 · 00:45 — Le frecce della barra spostano davvero la riga, e il cursore la segue
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 201 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Le frecce su e giù scambiavano due righe **intere**, prese
  dalla copia della schermata, con due salvataggi separati: se il testo
  appena scritto o la riga appena nata con l'Invio non erano ancora nel
  database, lo scambio rimetteva il testo e le posizioni di prima. E il
  campo di testo, se aveva una sua scrittura in viaggio, ignorava il nuovo
  ordine: restava con quello vecchio, e la lettera dopo finiva nella riga
  sbagliata, cancellandone il testo. Ora lo spostamento aspetta in fila le
  scritture di prima e cambia nel database **solo la posizione**
  (`PageRepository.moveBlock`, `BlockDao.setOrderIndex`); il campo prende
  sempre il nuovo ordine, come per Annulla
- **[Bug fix]** **[📱 Mobile]** **Il cursore segue la riga spostata**, nello stesso punto.
  Prima restava alla stessa altezza, cioè sulla riga scambiata: su righe
  vuote non si vedeva muoversi niente, e sopra un database la riga saliva
  ma il cursore restava sotto, così il tocco seguente spostava un'altra
  riga. Se la riga finisce in cima allo schermo, la pagina lascia sopra lo
  spazio dei pulsanti sospesi: prima ci finiva dietro
- **[Progetto]** Test in `MoveBlockTest`; due di loro (il cursore sopra il
  database, due righe con la stessa posizione) falliscono col codice di
  prima
- Verificato sul telefono (copia del database prima) sulla pagina di prova
  "Titolo tre H3", col database "Videogiochi" in cima: scritte "Uno",
  "Due", "Tre"; ↑ porta "Tre" sopra "Due" col cursore; altri due ↑ la
  portano sopra il database, e una lettera scritta subito finisce in lei;
  ↓ la riporta sotto; una lettera e subito ↑, un Invio e subito ↑, tre ↓ di
  fila veloci: nessuna riga persa, nessuna posizione doppia, il testo
  sempre nella riga giusta; Annulla rimette la riga dov'era. Poi la pagina
  rimessa com'era (il database e 22 righe vuote) e il database confrontato
  con la copia: uguale, tranne 11 voci di "Updates" di "Titolo tre H3" con
  le lettere delle prove

### 🆕 v0.66 — 28/09/2026 · 20:48 — Database: la vista Chart, con barre, linea e ciambella
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 196 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Chart" fra i layout dei database**, dopo
  Gallery (e "Chart view" nel menu "/"). Scelto, si apre la finestrella
  "**Chart type**" con i quattro grafici: **Vertical**, **Horizontal**,
  **Line** e **Donut**; di base sono le barre verticali, una per pagina,
  alte quanto il conteggio. Nelle impostazioni, sotto "Layout: Chart", la
  voce "Chart type" riapre la stessa finestrella
- **[Nuova funzionalità]** **[📱 Mobile]** **Le voci delle foto**, sotto Layout:
  - **X axis**: *What to show* (Name, cioè una barra per pagina, oppure una
    proprietà: i tag contano una volta per tag, le caselle Checked e
    Unchecked, le date un giorno per gruppo nel formato della colonna, i
    vuoti tutti insieme in "Empty"); *Sort by* (Name A → Z, Z → A, e il
    valore dal più basso o dal più alto); *Omit zero values*
  - **Y axis**: *What to show* (Count, oppure una colonna Number con
    Values, Sum, Average, Min, Max, Median o Distinct values); *Decimal
    places* (Default, 1, 1.0, 1.00…); *Cumulative*; *Range* (minimo e
    massimo dell'asse, vuoti = li sceglie il grafico); *Reference line*
    (linee arancioni tratteggiate, col loro nome)
  - con **Horizontal** gli assi si scambiano; con **Donut** c'è "**Data**"
    (What to show, Slice by, Sort by), come nella foto: al centro il
    totale ("186 Total Number"), accanto alle fette "81 (44%)", sotto la
    legenda
  - **Style**: *Color* (Auto, cioè un colore per gruppo, o uno dei dieci);
    *More style options*: Height, Grid lines, Data labels, Smooth line,
    Show legend, Show total, ognuna solo dove serve
- Come su Notion, **ogni gruppo tiene il suo colore** anche cambiando
  l'ordine o togliendo gli zeri (nella foto la "3" resta verde anche per
  prima); la ciambella con un colore solo usa sue sfumature, se no le
  fette non si distinguerebbero. L'asse lascia un po' d'aria sopra (400
  arriva a 480, come nella foto). Con tanti gruppi le barre scorrono di
  lato
- **Non fatte**, delle voci nelle foto: il quinto tipo "#" (la richiesta
  era di quattro), l'ordine "Advanced", e *Source*, *Save chart as…*,
  *Copy link to view*, *Manage data sources*, *Learn about charts*, che su
  Notion servono al sito e ai link; Filter e il blocco della vista ci
  sono già, uguali per tutti i layout
- **[Progetto]** **Database alla versione 40**: `pages.chartSettings`, le
  impostazioni in JSON; tutte di base non si salvano (`null`). I conti in
  `data/entity/Chart.kt` (`chartPoints`, `chartAxis`, `chartTicks`,
  `compactNumber`), il disegno in `DatabaseChart.kt`, le impostazioni in
  `ChartSettingsUi.kt`, le scritte in `ChartStrings`. Test in `ChartTest`
  e `MigrationTest` 39→40, col file `v39.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 40,
  le 19 pagine tutte lì) su "SuduS al contrario": Layout → Chart apre la
  finestrella; barre da 1; col Number 81, 25, 9, 71 sull'asse 0–100;
  Horizontal, Line con l'area sotto e la legenda "Number", Donut; ordine
  dal più alto; colore Green; senza totale e senza etichette, Large;
  Cumulative (81, 152, 177, 186, asse 0–240); due decimali ("0,00");
  Range fino a 200; linea "Media" a 100; asse X per Date (tre gruppi con
  "Empty"); Smooth line; anche aperto a tutta pagina. Corretti durante la
  prova: i numeri tagliati dalla griglia e dalla linea (ora hanno attorno
  un bordino del colore dello sfondo), i colori che seguivano il posto,
  l'asse fino a 300 per un massimo di 186, "Decimal places" che diceva
  "2" invece di "1.00". Poi tutto rimesso com'era dall'app (Table,
  impostazioni del grafico di nuovo `null`): il database è uguale alla
  copia, tranne l'ora di ultima modifica della pagina "SuduS al
  contrario". "1 line" al singolare non è stato riprovato sul telefono

### 🔧 v0.65.3.2 — 28/09/2026 · 20:05 — Lo scorrimento non si ferma più un istante prima di partire
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 185 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Risalendo dal fondo della pagina, il primo fotogramma
  dello scorrimento durava 29 millisecondi invece di 8: la lista della
  pagina rifaceva da capo il database di sopra ("SuduS al contrario"),
  che aveva smontato quando era uscito dallo schermo. Poco dopo c'era un
  altro fotogramma da 35 millisecondi, per le righe di testo sopra. Ora
  ogni voce della pagina, una volta fatta, resta pronta anche fuori dallo
  schermo finché si resta sulla pagina (`KeepComposed`, con
  `LocalPinnableContainer`, lo stesso modo in cui la lista tiene vivo un
  campo col cursore); quelle mai viste si fanno quando arrivano, come
  prima. Una finestra per preparare in anticipo le voci vicine ci sarebbe
  solo in una versione più nuova di Compose (qui la 1.6.8), da aggiornare
  a parte
- Verificato sul telefono misurando i fotogrammi (`dumpsys gfxinfo`) con
  lo stesso scorrimento dal fondo della pagina principale, partito da una
  cella di "Videogiochi". **Prima**: il primo fotogramma 29 ms (27 di
  preparazione), uno da 35 ms a metà corsa. **Dopo**: il primo 14 ms (6
  di preparazione), nessuno sopra i 16 ms su 84. La scrittura funziona
  come prima: toccata "Primo punto", cursore in fondo e tastiera aperta

### 🔧 v0.65.3.1 — 28/09/2026 · 19:57 — Risalendo una pagina, i database non fanno più saltare tutto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 185 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Risalendo una pagina con dei database dentro, quando il
  database di sopra rientrava nello schermo la pagina **saltava in alto**
  di colpo e lo scavalcava tutto. Non dipendeva dalle celle: capitava anche
  partendo dal testo sotto. Ogni database dentro una pagina passa dal
  cancello della password (`LockGate`), che finché non aveva chiesto al
  database se la pagina è bloccata **non disegnava niente**; un database
  uscito dallo schermo veniva smontato, e al rientro per un attimo era
  alto zero, poi alto quanto la sua tabella. La pagina tiene fermo il suo
  bordo di sopra, e l'allungamento spingeva giù tutto il resto. Ora il
  cancello si ricorda l'ultima risposta per pagina, finché le protezioni
  non cambiano (`AppLock.protectionVersion`), e il database torna subito
  alto com'è; la domanda si rifà comunque ogni volta
- Verificato sul telefono registrando lo schermo sulla pagina principale,
  in fondo, con uno scorrimento verso il basso di mezzo schermo partito da
  una cella Select di "Videogiochi". **Prima**: per quattro fotogrammi
  quasi fermo, poi un salto di circa il doppio della strada del dito, fino
  a "SuduS al contrario" e oltre; lo stesso partendo dal testo sotto la
  tabella. **Dopo**: la tabella scende piano col dito, e da sopra compare
  un po' alla volta il fondo di "SuduS al contrario"

### ✏️ v0.65.3 — 28/09/2026 · 19:49 — Pomodoro: la durata dello studio dal riquadro grande, "Study sound" e "Break sound"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 185 test
passati; commit solo in locale)*

- **[Rimosso]** **[📱 Mobile]** Dall'ingranaggio del Pomodoro la durata della sessione di
  studio. Restano la durata della pausa e i suoni
- **[Modifica]** **[📱 Mobile]** **Il tempo scritto nel riquadro grande durante
  lo studio è la durata della sessione**, anche per le sessioni dopo e per
  "da capo", secondi compresi. Senza, tolta la sua voce dall'ingranaggio,
  le sessioni seguenti sarebbero tornate alla durata vecchia. Durante la
  pausa il tempo scritto vale solo per quella pausa
- **[Modifica]** **[📱 Mobile]** Due suoni al posto di uno: **"Study sound"**,
  quando ricomincia la sessione di studio, e **"Break sound"**, quando
  comincia la pausa, ognuno scelto fra le suonerie del telefono e ognuno
  col suo "usa il suono predefinito". Il suono unico di prima diventa il
  "Break sound" (si salva ancora col suo nome di allora, `soundUri`)
- **[Progetto]** `PomodoroWidget`: `sessionMs`, `breakSoundUri` /
  `breakSoundName` (salvati come `soundUri` / `soundName`),
  `studySoundUri` / `studySoundName`, `soundFor`, `withTimeWritten`.
  `WidgetStore.pomodoroSetBreakLength` e `pomodoroSetSounds`. Test in
  `WidgetsTest`, anche un pomodoro salvato prima che si rilegge col suo
  suono come "Break sound"
- Verificato sul telefono: l'ingranaggio ha "Break length", "Study sound"
  e "Break sound" (tutti e due "Default"), niente durata dello studio;
  "Study sound" apre l'elenco delle suonerie col suo nome in cima (scelto
  il selettore di Samsung con "Just once"), e tornando indietro non è
  cambiato niente. Chiuso con Cancel: il pomodoro è rimasto com'era (1
  minuto di studio, 5 di pausa, nessun suono scelto)

### ✏️ v0.65.2 — 28/09/2026 · 19:40 — Nei calendari per scegliere la data, solo i giorni del mese
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, appena
ricollegato; 182 test passati; commit solo in locale)*

- **[Rimosso]** **[📱 Mobile]** Nel calendario che si apre per scegliere una data (le
  date dei database, con Repeat, e le notifiche: è lo stesso,
  `SamsungCalendar`) **i giorni del mese prima e del mese dopo non si
  vedono più**: al loro posto le caselle restano vuote, senza numero, senza
  pallino della ripetizione e senza tocco. Le settimane restano sei, così
  il calendario non cambia altezza passando da un mese all'altro
- Verificato sul telefono: aperta la data "2025年12月08日" della riga
  "Esempio" di "SuduS al contrario". Dicembre 2025 comincia lunedì 1 con
  la domenica prima vuota, e dopo il 31 le caselle sono vuote; col ">"
  gennaio 2026 comincia giovedì 1, prima niente. Chiuso con Cancel: le date
  nel database sono rimaste quelle di prima

### ✏️ v0.65.1 — 28/09/2026 · 19:38 — Notebooks in cima alla barra laterale
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 182 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Il riquadro **Notebooks** coi suoi quaderni
  sta ora **in cima alla barra laterale**, sopra i widget; sotto di lui
  resta la linea chiesta il 26/09. Fra i widget e "Main menu" c'è adesso
  la stessa linea: prima a separarli c'erano i quaderni
- Verificato sul telefono: aperta la barra, in cima Notebooks, la linea,
  poi "Widgets" coi fusi orari e gli altri; in fondo ai widget "Add
  widget", la linea, "Main menu"

### 🔧 v0.65.0.2 — 28/09/2026 · 19:33 — Invio in fretta: niente righe in più, e i font non scattano
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 182 test
passati; commit solo in locale il 28/09/2026)*

- **[Bug fix]** **[📱 Mobile]** **Invio premuti in fretta creavano una riga in più.** Il
  secondo Invio arrivava prima che il database avesse scritto il primo:
  il campo aveva una riga più dei blocchi, `updateRun` tornava a
  confrontare coi testi salvati e da lì sembravano nate due righe. Tre
  Invio facevano quattro righe, e da quel momento campo e blocchi non si
  corrispondevano più: "Paragrafo di prova…" restava senza i suoi font e
  "acqua H₂O energia mc²" senza pedice e apice, finché non si riapriva la
  pagina. Ora ogni modifica aspetta la precedente e rilegge i blocchi dal
  database (`structuralEdits`), dove la precedente c'è già; delle righe
  che c'erano si riscrive solo il testo, così un cambio di tipo arrivato
  un istante prima (il menu "/") non viene riportato indietro
- **[Bug fix]** **[📱 Mobile]** **Lo "scatto"** anche con un Invio solo: per un
  fotogramma, finché il database non scriveva, ogni riga prendeva font,
  colori, pedici e apici dal blocco della riga accanto, e le righe
  formattate si vedevano cambiare e tornare. Ora righe e blocchi si
  abbinano contando dal fondo e dall'inizio (`blocksOfLines`), come già
  facevano i pallini degli elenchi — e ora anche col backspace e con più
  righe di differenza; la riga divisa in due o le due righe unite tengono
  la formattazione prevista
- **[Progetto]** `updateRun` → `runDiff` e `writeRunChange`;
  `PageRepository.getBlocksOnce`, `applyBlockChanges` con `ensured` e
  `texts`. Test in `FastEnterTest` (con la versione di prima il test dei
  tre Invio falliva: quattro righe) e `LineBlocksTest`
- Verificato sul telefono registrando lo schermo sulla pagina principale,
  cursore sulla prima riga vuota e tre Invio di fila col tasto della
  tastiera. **Prima**: nei fotogrammi dopo il terzo Invio "Paragrafo di
  prova…" senza font e "acqua H2O energia mc2" senza pedice e apice fino
  alla fine della registrazione, e nel database quattro righe nuove invece
  di tre. **Dopo**: in tutti i 23 fotogrammi le due righe restano
  formattate, e nel database le righe nuove sono esattamente tre. Le
  dieci righe vuote aggiunte dalle prove in cima alla pagina principale
  non ho fatto in tempo a toglierle (il telefono si è scollegato); al
  28/09/2026, ricollegato, non c'erano più: in cima ci sono quattro righe
  vuote, e il resto della pagina, formattazione compresa, è intatto

### 🔧 v0.65.0.1 — 26/09/2026 · 18:31 — "To-do list" dal "+" sulla prima riga di una pagina nuova
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 174 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** In un template appena creato — e in ogni pagina nuova,
  comprese quelle delle righe dei database — **scegliere un tipo dal "+"
  sulla prima riga, prima di scriverci, non faceva niente**: "To-do list"
  non metteva la casella, e quello che si scriveva dopo restava testo
  normale. La prima riga di una pagina senza righe si vede ma nel database
  non c'è finché non ci si scrive (vedi `placeholder`), e il cambio di tipo
  scrive **una colonna sola** di una riga che deve già esserci: non la
  trovava e si perdeva. Con "/" funzionava, perché la barra scritta salvava
  prima la riga. Ora le modifiche di una colonna sola (tipo, testo,
  rientro, numero di partenza, notifica, tabella, vista collegata)
  passano da `blocksDb`, che prima salva quella riga **solo se manca**:
  se intanto è stata scritta, resta com'è
- **[Progetto]** `BlockDao.insertIfAbsent`, `PageRepository.saveBlockIfAbsent`.
  Test in `EmptyPageTest`
- Verificato sul telefono: riprodotto prima della correzione (template
  nuovo, "+" → To-do list, "ciao": nel database un paragrafo "ciao").
  Dopo: la casella compare subito, "ciao", Invio, "due" → due caselle nel
  database. I template di prova sono nel cestino; "SaaS" e "SuduS"
  dell'utente non sono stati toccati

### 🆕 v0.65 — 26/09/2026 · 18:21 — I template delle pagine, e le proprietà in cima alla pagina di una riga
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 173 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Templates" nella barra laterale**, sotto
  "Startup window": l'elenco dei template, "New template" in cima per
  crearne uno, e i tre puntini di ciascuno per mandarlo nel cestino (da cui
  torna fra i template, non nel menu principale). Un template **si scrive
  come una pagina qualsiasi** — caselle, elenchi, titoli, toggle, tabelle,
  sottopagine — con in cima, in grigio, "You're editing a template"; senza
  nome si chiama "Untitled template". Nei tre puntini di un template non ci
  sono "Duplicate" e "Move to", che lo farebbero diventare una pagina del
  menu principale
- **[Nuova funzionalità]** **[📱 Mobile]** **Le voci grigie di una pagina nuova di un
  database**: aprendo la pagina di una riga ancora vuota, sotto le
  proprietà compaiono "Tap anywhere to continue with an empty page, or pick
  a template" e le due voci **"+ New template"** (crea un template e lo
  apre, da scrivere) e **"Add existing template"** (l'elenco dei template:
  toccato uno, tutto quello che c'è scritto dentro finisce nella pagina e
  le voci se ne vanno). Sono grigie come il resto dei suggerimenti
  dell'app. **Un tocco in un punto qualsiasi della pagina** le fa sparire;
  riaprendo la pagina ancora vuota tornano, come su Notion. Il template
  prende il posto delle righe vuote della pagina (in una pagina già scritta
  andrebbe in fondo); titolo, proprietà, icona e copertina della pagina non
  cambiano; Annulla lo toglie. I template sono **gli stessi per tutti i
  database**, sia quelli creati dalla barra laterale sia quelli creati da
  una pagina
- **[Nuova funzionalità]** **[📱 Mobile]** **Le proprietà in cima alla pagina di una
  riga**, come nella terza foto: fra il titolo e il testo, una per riga,
  con l'icona del tipo e il nome, **ciascuna modificabile lì** come nella
  sua cella (tag, date con ripetizioni e notifiche, numeri, caselle,
  colori, testo…); sotto, "**Add a property**" per aggiungerne una al
  database, e una linea che le separa dal testo. **Ci sono anche quelle
  nascoste nella tabella**, con accanto l'occhio sbarrato. Tutto
  sincronizzato: cambiata qui, la proprietà cambia nella tabella e nelle
  altre viste, e al contrario. Con la pagina o il database bloccati si
  leggono soltanto. I database semplici non c'entrano: le loro righe non
  hanno pagine
- **[Progetto]** **Database alla versione 39**: `pages.isTemplate`. I
  template non compaiono nell'albero, nella ricerca, fra le pagine da cui
  partire e fra le destinazioni di "Move to". `PageRepository`:
  `observeTemplates`, `createTemplate`, `applyTemplate` (copia blocchi,
  figli, celle delle tabelle e sottopagine, come "Duplicate"),
  `observeRowOfPage`. `RowPageProperties` (in `DatabaseViewScreen.kt`, con
  le celle della tabella) usa un `DatabaseViewModel` caricato
  `light`, senza le anteprime della galleria che cambierebbero a ogni
  tasto. `TemplatesScreen.kt` ha l'elenco, `TemplatePicker` e
  `TemplateChooserSheet`; le scritte stanno in `TemplateStrings`. Test in
  `TemplateTest` e `MigrationTest` 38→39, col file `v38.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 39, le
  10 pagine tutte lì): Templates sotto Startup window, vuoto; "New
  template" → "SaaS" con le caselle 1, 2, 3; nei suoi tre puntini niente
  Duplicate e Move to. Nel database "Untitled", pagina "2": Select PS5,
  Text, "Add a property", la linea, e le voci grigie; "Add existing
  template" → SaaS → le tre caselle nella pagina, le voci sparite. Select
  cambiato in PC dalla pagina → PC nella tabella; rimesso PS5 dalla
  tabella → PS5 nella pagina. "Text" nascosta nella tabella → nella pagina
  c'è, con l'occhio sbarrato. Un tocco nel vuoto fa sparire le voci; "Add a
  property" apre "New property" (annullato); "+ New template" dalla pagina
  "4" apre un template nuovo, e tornando le voci sono ancora lì con due
  template nell'elenco; la pagina "1", che ha del testo, non le mostra.
  Alla fine rimesso tutto com'era: Annulla ha tolto le caselle dalla pagina
  "2" (stesso blocco di prima), "Text" di nuovo visibile, PS5 al suo posto,
  e i due template di prova nel cestino. Il confronto col database di prima
  ha trovato **un mio errore**, corretto: un tocco finito su una cella di
  "SuduS al contrario" e uno scorrimento sopra la tastiera avevano
  aggiunto "⁵" al nome "Riga semplice"; tolto, e al secondo confronto i
  dati erano identici a prima (a parte i due template di prova)

### 🔧 v0.64.0.1 — 26/09/2026 · 17:49 — L'Invio a inizio della prima riga non chiude più la tastiera
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 168 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** **Invio col cursore all'inizio della prima riga** di un
  gruppo di righe di testo (in cima alla pagina, o subito sotto un
  toggle, un divisore, un link…): la tastiera spariva. Il campo che
  tiene insieme le righe era riconosciuto dal suo primo blocco, e la
  riga vuota nuova nasce proprio sopra di lui: con un primo blocco
  diverso il campo veniva rifatto da capo e perdeva il cursore. Solo
  sulla prima riga e solo con del testo perché su una riga vuota, o su
  una riga più sotto, la riga nuova non diventa la prima del gruppo.
  Ora il campo è riconosciuto da quello che gli sta sopra (l'isola che
  lo precede, o l'inizio della pagina), che andando a capo non cambia
- **[Bug fix]** **[📱 Mobile]** Nello stesso caso, sotto un'isola **la riga vuota finiva
  in cima alla pagina**, sopra l'isola, invece che subito sopra la riga:
  le si dava il posto zero della pagina invece di quello della riga.
  Non si vedeva ancora perché il campo si chiudeva prima
- **[Progetto]** `groupIntoRenderItems` dà la chiave ai gruppi di righe
  (`RenderItem.Run.key`), e `MergedTextRunField` la riceve invece di
  ricavarla dal primo blocco; in `updateRun` le righe nuove senza un
  blocco prima vanno al posto della prima riga. Test in `RunKeyTest` e
  `EnterAtFirstLineTest`
- Verificato sul telefono (Galaxy S25 Ultra, collegato col debug senza
  fili), sulla pagina "Titolo tre H3", che ha solo righe vuote: scritto
  "prova" sulla prima riga, cursore all'inizio, Invio col tasto della
  tastiera. **Prima** la tastiera si chiudeva (riprodotto), **dopo**
  resta aperta, "prova" scende di una riga e la lettera scritta subito
  dopo va davanti a "prova". Lo stesso con un divisore sopra: la riga
  vuota nasce fra il divisore e "prova", la tastiera resta. (In fondo
  alla riga l'Invio andava già bene: provato prima della correzione,
  non di nuovo dopo.) Alla fine
  tolti il testo e il divisore di prova: la pagina ha di nuovo le sue
  23 righe vuote

### 🆕 v0.64 — 26/09/2026 · 13:53 — "Date format" e "Time format" nella proprietà Date, e via da Settings
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 164 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Date format** in Edit property di una
  colonna Date: gli stessi nove formati che c'erano in Settings (Full
  date, Short date, Month.Day.Year, Month/Day/Year, Day.Month.Year,
  Day/Month/Year, Year.Month.Day, Year/Month/Day, Chinese date), ognuno
  con la data di oggi scritta così e la spunta su quello attuale. "Relative"
  della foto di Notion non c'è: la richiesta era la lista di Settings
- **[Nuova funzionalità]** **[📱 Mobile]** **Time format** accanto: **Hidden**, **12
  hour**, **24 hour**, come nella seconda foto. 12 hour scrive "7:43 PM",
  24 hour "19:43", Hidden toglie l'ora dalla cella (resta salvata, e la
  notifica pure). Vale per le celle, le liste, i calcoli (Earliest,
  Latest…) e il riassunto del filtro, in tutte le viste della colonna;
  ogni colonna Date ha il suo
- **[Rimosso]** **[📱 Mobile]** **Date format da Settings**. Le colonne Date che non ne
  hanno ancora scelto uno (e quelle nuove) continuano col formato scelto
  lì l'ultima volta, e così la cronologia, i preferiti, il cestino e la
  data dei quaderni, che non hanno una proprietà da cui prenderlo
- **[Progetto]** **Database alla versione 38**:
  `database_columns.dateFormat` (il nome di `DateStyle`; vuoto = quello
  di prima) e `database_columns.timeFormat` (H12, HIDDEN; vuoto = 24
  ore), letti con `dateSettings()`. `Formats.dateTimeFormatter(style,
  time)` e `Formats.dateStyleName`. Test in `MigrationTest` 37→38 (le
  date restano come prima, ora a 24 ore), col file `v37.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 38,
  le 10 pagine tutte lì), su "SuduS al contrario": Edit property di Date
  → Date format "Chinese date", Time format "24 hour"; la lista dei nove
  formati con gli esempi di oggi e la spunta su Chinese date; Time format
  con Hidden, 12 hour, 24 hour. Day/Month/Year e 12 hour → "25/09/2026
  7:43 PM", "08/12/2025", e il calcolo Earliest "08/12/2025"; Hidden →
  "25/09/2026". In Settings, dopo Number format viene subito Language.
  Alla fine rimesso Chinese date e 24 hour: le celle sono tornate
  "2026年09月25日 19:43" e "2025年12月08日" come prima

### 🆕 v0.63 — 26/09/2026 · 13:40 — A sinistra, al centro o a destra: le celle e i calcoli
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 163 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Le celle di ogni colonna a sinistra, al centro
  o a destra**: tenendo premuta l'intestazione, tre voci — **Align left**,
  **Center**, **Align right** — con la spunta su quella attuale, per
  **ogni tipo** (prima si centravano solo le caselle). Si spostano il
  testo, i numeri (anche barra e anello: l'anello al centro o a destra
  segue la colonna, a sinistra comanda la sua "Position"), i tag, le date,
  il testo delle celle colorate, le caselle, le date di creazione e
  modifica. **Le intestazioni restano sempre a sinistra.** Vale in tutte
  le viste che mostrano la colonna; la colonna del nome non cambia
- **[Nuova funzionalità]** **[📱 Mobile]** **Il risultato di ogni calcolo** sotto la
  tabella a sinistra, al centro o a destra, **tenendolo premuto** (toccato
  si cambia il calcolo, come prima). Di suo a destra, come su Notion. Vale
  per quella vista, come i calcoli
- **[Progetto]** **Database alla versione 37**: `database_columns.contentAlignment`
  (LEFT, CENTER, RIGHT; vuoto = come prima, al centro solo le caselle
  centrate col vecchio interruttore, vedi `cellAlignment`). Gli
  allineamenti dei calcoli stanno nella stessa stringa dei calcoli della
  vista, con le chiavi "align:colonna" (`parseCalculationAlignments`),
  senza colonne nuove. Il contenuto della cella sa dove stare da
  `LocalCellAlignment`; `setColumnCentered` non serve più. Test in
  `NumberFormatTest` e `MigrationTest` 36→37 (una casella centrata prima
  resta al centro), col file `v36.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 37, le
  10 pagine tutte lì), su "SuduS al contrario", dove intanto l'utente aveva
  scritto dei numeri (25, 81, 9, 71) e messo "Sum": tenuta premuta
  l'intestazione di Number → Align left spuntato, Center, Align right →
  Align right → i numeri a destra; Date → Center → i trattini al centro (la
  data della prima riga ha l'ora e riempie la cella, quindi non ha spazio
  per spostarsi); "SUM 186" tenuto premuto → Align left → a sinistra. Alla
  fine rimesso com'era: Number e Date a sinistra, "SUM 186" a destra, il
  calcolo dell'utente al suo posto (nel database la stringa dei calcoli è
  tornata identica)

### 🆕 v0.62 — 26/09/2026 · 13:24 — La proprietà Number: formato, cifre, barra e anello; "Calculate" sotto la tabella
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne quello
elencato in fondo; 161 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Change type"** nel menu che si apre tenendo
  premuta l'intestazione di una colonna, di **qualunque** tipo: lì c'è la
  lista dei tipi con la ricerca. **"Edit property"** (e il tocco
  sull'intestazione) ha il nome e, per una colonna Number, le impostazioni
  qui sotto; il tipo non c'è più
- **[Nuova funzionalità]** **[📱 Mobile]** **Edit property di una colonna Number**, come
  nella prima foto: **Number format**, **Decimal places**, una riga, "Show
  as" e i tre riquadri **Number** (42), **Bar** (piena a metà) e **Ring**
  (pieno al 40%); quello scelto è bordato di blu. Si salva con "Save"; vale
  in tutte le viste che mostrano la colonna
- **[Nuova funzionalità]** **[📱 Mobile]** **Number format**: Number e Percent, una riga,
  poi le venti valute **nell'ordine chiesto**, ognuna col suo segno ($, €,
  £, CHF, ¥, ₩, A$, CA$, S$, ₽, ₹, HK$, NZ$, kr, MX$, NT$, kr., zł), con un
  campo per cercarle. Il segno va davanti ("€25,00") o dopo per ₽, kr e zł
  ("100 kr"). "Pound" è **GBP**: nella richiesta c'era "GBR", che è il
  codice del paese. **Percent** aggiunge il segno % al numero scritto: 45
  si legge 45% (non si moltiplica per 100)
- **[Nuova funzionalità]** **[📱 Mobile]** **Decimal places**: Default (come l'ha scritto
  l'utente) e da 0 a 5. Le cifre in più **si nascondono senza
  arrotondare** e senza essere cancellate: 45.76 con una cifra si legge
  45.7, con tre 45.760; il valore salvato resta 45.76. Scrivendo nella
  cella si vede il numero com'è, senza formato
- **[Nuova funzionalità]** **[📱 Mobile]** **Bar e Ring nelle celle**: una barra orizzontale
  o un anello pieni per il numero diviso 100 (72 → quasi tre quarti; "Divide
  by" non c'è, come chiesto), col numero accanto se **Show number** è
  acceso. **Color** apre la tavolozza di sempre (spettro, grigi, RGB, Hex);
  di suo è verde. Per l'anello, **Position**: a sinistra o al centro della
  cella. Nell'elenco e nelle schede il numero si legge col suo formato
- **[Nuova funzionalità]** **[📱 Mobile]** **"Calculate"**, tenendo premuta l'intestazione di
  qualunque colonna **e del nome**: None, **Count** (Count all, Count
  values, Count unique values, Count empty, Count not empty; per le caselle
  Count all, Checked, Unchecked), **Percent** (Percent empty, Percent not
  empty), per le date **Date** (Earliest date, Latest date, Date range),
  per i numeri **More options** (Sum, Average, Median, Min, Max, Range). I
  gruppi si aprono toccandoli. Il calcolo compare in una **riga sotto la
  tabella**, sotto la sua colonna ("SUM €25,00", "COUNT 4"), sulle righe
  mostrate; in una tabella a gruppi, sotto ogni gruppo. Toccandolo si
  cambia; con None su tutte le colonne la riga sparisce. I calcoli sono
  **della vista**, come su Notion: una vista collegata ha i suoi
- **[Progetto]** **Database alla versione 36**: in `database_columns`
  `numberFormat`, `decimalPlaces`, `numberDisplay`, `numberColor`,
  `showNumber`, `ringCentered`; in `pages` `viewCalculations`
  ("colonna=CALCOLO;…"). Formati e calcoli in `NumberFormat.kt` (letti per
  nome: uno sconosciuto torna al valore di sempre invece di far chiudere
  l'app), la parte grafica in `NumberProperty.kt`. Test `NumberFormatTest`
  (8, anche coi numeri delle foto: somma 309, media 38,63, mediana 30,5) e
  `MigrationTest` 35→36 col file `v35.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 36, le
  10 pagine tutte lì), sulla colonna "Number" di "SuduS al contrario" (25
  in "Riga semplice"): tenuta premuta → Move, Edit property, **Change
  type**, Hide, **Calculate**; Edit property come nella foto; Number format
  → la lista con i segni, Number e Percent sopra la riga → Euro; Decimal
  places → 2; Bar → Color e Show number → "Save" → **"€25,00" e la barra
  verde piena a un quarto**; Ring + Center → "€25,00" e l'anello in mezzo
  alla cella; Calculate → More options → Sum → **"SUM €25,00"** sotto la
  colonna; sul nome Count all → **"COUNT 4"**; toccando la cella si scrive
  "25", con "Done" torna "€25,00"; Change type → solo i tipi. Alla fine
  tutto rimesso com'era (Number, Default, Number, a sinistra, nessun
  calcolo: la riga dei calcoli sparisce), controllato nel database
- **Non provato sul telefono**: le cifre che si nascondono (45.76 → 45.7;
  lo prova il test, sul telefono avrebbe voluto dire cambiare un numero
  dell'utente), il cambio di colore della barra, i calcoli delle date e
  delle caselle e quelli sotto i gruppi

### 🔧 v0.61.0.1 — 26/09/2026 · 12:51 — Il nome di una riga: il cursore in fondo, e la tastiera che resta con l'ordinamento
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 152 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** **Il cursore in fondo al nome.** Il campo del nome si
  apriva col cursore all'inizio (un campo di solo testo parte da lì); ora
  parte in fondo, pronto per continuare o cancellare
- **[Bug fix]** **[📱 Mobile]** **Con un ordinamento, cancellando il nome la tastiera
  spariva.** Le righe della tabella non avevano il loro id: con l'ordine
  per nome, un nome vuoto va in fondo, la riga cambiava posto e il campo
  in cui si scriveva finiva su un'altra riga, che lo chiudeva. Ora ogni
  riga ha il suo id (`key(row.id)`), anche nei gruppi. In più, **mentre si
  scrive il nome l'ordine resta fermo** (`DatabaseViewModel.startTitleEdit`
  / `endTitleEdit`): la riga non salta a ogni lettera, e va al suo posto
  quando si preme "Done" o si esce dal campo
- **[Bug fix]** **[📱 Mobile]** (trovato provando) **Il nome di una riga si riscriveva una
  volta sola**: la seconda volta la cella si apriva e si richiudeva
  nell'istante, perché si ricordava di aver già avuto il fuoco la volta
  prima. Ora si riparte da capo a ogni tocco
- Verificato sul telefono, sul database ordinato per nome dell'utente
  (righe "1", "2", "4", "5" e una senza nome), e su "Esempio" dell'altro:
  toccando "Esempio" il cursore sta dopo la "o"; "2" → cancellato → la
  tastiera resta, la riga resta seconda (vuota); "2" riscritto e "Done";
  di nuovo "2" (la seconda volta si apre) → "9" → resta seconda mentre si
  scrive → "Done" → va dopo "5"; poi "9" → "2" → resta quarta mentre si
  scrive → "Done" → torna seconda. I nomi sono di nuovo quelli di prima
- Notato: in un tentativo le coordinate erano vecchie (la pagina si era
  spostata chiudendo la tastiera) e un tocco è finito sulla riga "Riga
  dentro il toggle"; controllato col confronto sulla copia di stamattina:
  i 28 blocchi di My Space sono identici, niente cambiato

### 🆕 v0.61 — 26/09/2026 · 12:39 — La pagina di un quaderno, e un divisore sotto i quaderni
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 152 test
passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Un divisore sotto i quaderni**, come quello
  sotto "Main menu": l'ultima riga dei quaderni non si confonde più con
  "Main menu"
- **[Nuova funzionalità]** **[📱 Mobile]** **Nella barra, il nome di un quaderno apre la sua
  pagina, la freccetta a sinistra apre e chiude l'albero** lì dov'è. La
  freccetta ora è un pulsante a sé, un po' più largo, per il dito. I gruppi
  si aprono e chiudono come prima
- **[Nuova funzionalità]** **[📱 Mobile]** **La pagina di un quaderno**: in cima il quaderno
  (icona grande e nome), sotto **tutte** le sezioni e i gruppi, rientrati
  come nell'albero, ogni sezione con la sua linguetta, il lucchetto se è
  protetta e **quante pagine ha**, in fila sul bordo destro. Toccando una
  sezione la si apre (e "indietro" torna qui); tenendola premuta, il suo
  menu; la freccetta di un gruppo lo chiude **solo in questa pagina** (la
  barra resta com'è), e lo ricorda anche tornando da una sezione. In fondo
  "+ New section" e "+ New section group"; i tre puntini aprono il menu del
  quaderno, ed eliminandolo si torna indietro. Nella barra, il quaderno
  aperto è evidenziato
- **[Progetto]** La rotta `notebook/{notebookId}` e `NotebookScreen`; il
  conteggio delle pagine per sezione (`NotebookDao.observePageCounts`) si
  legge solo mentre la pagina è aperta, perché la tabella delle pagine
  cambia a ogni tasto
- **[Bug fix]** **[📱 Mobile]** (trovato provando) I numeri delle pagine si spostavano a
  destra a ogni rientro invece di stare in colonna
- Verificato sul telefono, coi quaderni dell'utente ("My Notebook", con
  due gruppi uno dentro l'altro, e "My Notebook 2"): il divisore sopra
  "Main menu"; la freccetta di "My Notebook" lo chiude e lo riapre (lasciato
  aperto com'era); il nome apre la pagina con tutte e cinque le sezioni e i
  due gruppi, "0 pages" in colonna; "New Section Group 2" chiuso nella
  pagina e ancora aperto nel database; "New Section 3" → la sezione, con
  "My Notebook › New Section Group 1" → indietro → di nuovo la pagina del
  quaderno, col gruppo ancora chiuso. Nessun dato dell'utente toccato

### 🆕 v0.60 — 26/09/2026 · 09:06 — Le ripetizioni: senza toccare il giorno, i pallini arancioni, fino al 2100
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 152 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** **La ripetizione senza un giorno toccato andava persa.**
  Scegliendo solo "Repeat" senza toccare un giorno sul calendario, "Done"
  trovava la data vuota e **svuotava la cella**: la regola non arrivava
  mai al database, e per questo non si vedeva né sul calendario né nella
  vista Calendar (che le ripetizioni le sa già disegnare). Sul telefono la
  riga "Esempio" dell'utente aveva infatti la data vuota. Ora, con una
  ripetizione, **il giorno non va toccato**: si parte da oggi
- **[Nuova funzionalità]** **[📱 Mobile]** **I pallini arancioni della ripetizione** sul
  calendario della data, gli stessi del giorno scelto: con "ogni sabato",
  tutti i sabati **da quel giorno in avanti** (quelli prima no). Il giorno
  da cui parte, se non è uno di quelli, ha solo il contorno arancione
- **[Nuova funzionalità]** **[📱 Mobile]** **Da quando**: sotto "Repeat", quando c'è una
  ripetizione, la riga **"Starts"** ("Today" o la data). Toccandola:
  **"From today until 2100"** oppure **"From a day I choose until 2100"**
  (col selettore della data). Tutte e due fanno finire la ripetizione il
  31/12/2100, che si legge nel riassunto ("Weekly on Saturday, until
  2100年12月31日")
- **[Nuova funzionalità]** **[📱 Mobile]** **"L'ultima domenica del mese" senza partire da
  un'ultima domenica.** In "Custom…" con "month": "On [giorno]" col numero
  da scrivere, oppure "The first / second / third / fourth / last
  [giorno]" da un menu, col giorno della settimana scelto dai pallini sotto.
  Prima si poteva solo "come il giorno toccato sul calendario"
- **[Nuova funzionalità]** **[📱 Mobile]** Nella cella, accanto alla data, le **frecce in
  cerchio** di una data che si ripete (come la campanella della notifica)
- **[Progetto]** `Recurrence` ha tre campi in più, `monthDay`, `nthWeek`
  (1–4 o `LAST_WEEK`) e `nthWeekday`, salvati nella regola (`MDAY`,
  `NTH`, `NWD`): null vuol dire "dalla data di partenza", quindi le regole
  scritte prima si leggono come prima. Niente migrazione: la regola sta
  già nel testo della cella. Anche il modo rapido "Monthly on the last
  Saturday" scrive settimana e giorno nella regola, così cambiando poi il
  giorno di partenza resta l'ultimo sabato. "Ogni giorno" salta dritto al
  periodo mostrato: dal 2026 al 2100 sono ventisettemila giorni, oltre il
  tetto di sicurezza del calcolo. Test `RecurrenceTest` (5)
- Verificato sul telefono, sulla riga "Esempio" del primo database di My
  Space (data vuota): Repeat → "Weekly on Saturday" senza toccare giorni →
  pallini su 26 settembre, 3 e 10 ottobre, non sui sabati passati; "Starts
  Today". "Starts" → "From a day I choose until 2100" → 5 ottobre →
  contorno sul 5, pallini da sabato 10; il riassunto "…, until
  2100年12月31日". "Custom…" → month → "The last Sunday" (menu e pallino
  della domenica) → pallino sul 25 ottobre. "Done" → salvato
  `FREQ=MONTHLY;…;NTH=5;NWD=SUNDAY;UNTIL=…`, la cella con la data e le
  frecce. Vista Calendar (messa per la prova e poi rimessa Table):
  "Esempio" il 25 ottobre. Alla fine la data di "Esempio" è stata
  cancellata, com'era prima
- Da sapere: la cella mostra il giorno da cui la ripetizione parte, non la
  prossima volta in cui capita

### 🆕 v0.59 — 26/09/2026 · 08:53 — I quaderni, come in OneNote
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne
quello che è elencato in fondo; 147 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La voce "Notebooks" della barra laterale è
  accesa**: è la fascia di prima (un altro colore, sotto i widget), con la
  freccetta per aprirla e chiuderla — la scelta si ricorda — e il **+** per
  un quaderno nuovo. Sotto, **l'albero di OneNote**: i quaderni col loro
  quaderno disegnato e la freccetta; dentro, le **sezioni** con la
  linguetta colorata e i **gruppi di sezioni**, che si aprono e si
  chiudono come i toggle e possono contenere altre sezioni e altri gruppi
  (come "Languages" → "Deutsch" → "General" nella foto). In fondo a ogni
  quaderno aperto, "+ New section". La sezione aperta è evidenziata
- **[Nuova funzionalità]** **[📱 Mobile]** **Un quaderno nuovo**: nome e colore (i dodici
  di OneNote), e nasce con la sua prima sezione "New Section 1", come in
  OneNote. **L'icona**: il quaderno disegnato nel colore scelto, oppure
  **un'immagine a scelta dalla galleria** — per esempio un'icona scaricata
  da Flaticon — da "Notebook icon" nel suo menu. Scaricarle da Flaticon
  non lo posso fare io: l'app non va in rete (e le icone hanno la loro
  licenza); se l'utente me le manda, le metto fra quelle pronte
- **[Nuova funzionalità]** **[📱 Mobile]** **Tenendo premuta una voce** si apre il suo menu,
  le voci del tasto destro di OneNote che hanno senso qui:
  - quaderno: Rename, Notebook color, Notebook icon, New section, New
    section group, Move up, Move down, Delete
  - sezione: Rename, Section color, **Password protect this section**
    (con la password dell'app o l'impronta, come le pagine), New section,
    New section group (nascono accanto a lei), Move up, Move down, Delete
  - gruppo: Rename, New section e New section group (nascono dentro),
    Move up, Move down, Delete
  Il nome nuovo si propone già scritto e selezionato ("New Section 2",
  "New Section Group 1"…). Una sezione nuova prende il colore dopo quello
  dell'ultima, e si apre subito
- **[Nuova funzionalità]** **[📱 Mobile]** **Una sezione aperta**: in alto dove sta
  ("Prova quaderni › Languages"), il nome con la linguetta, **"Add page"**
  come in OneNote, e le pagine nel loro ordine, col giorno in cui sono
  nate. Toccando una pagina la si apre; tenendola premuta, Move up, Move
  down, Delete page. I tre puntini aprono il menu della sezione. Su un
  telefono le tre colonne di OneNote non ci stanno: la colonna delle
  sezioni è la barra laterale, quella delle pagine è questa schermata
- **[Nuova funzionalità]** **[📱 Mobile]** **Le pagine dei quaderni** usano lo stesso
  editor delle altre, quindi **scorrono senza fine**: all'inizio c'è solo
  quello che si vede, e si allungano man mano che si scrive. In cima,
  **come in OneNote**: il titolo ("Untitled page" in grigio finché è
  vuoto), una riga sottile e sotto **il giorno, la data e l'ora** in cui
  la pagina è nata (nel formato di data scelto nelle impostazioni). Niente
  icona né copertina. Si disegna, si blocca, si protegge con la password,
  si guarda la cronologia come nelle altre pagine
- **[Nuova funzionalità]** **[📱 Mobile]** **Due mondi separati**, come chiesto. Le pagine
  dei quaderni non compaiono mai nel menu principale: né nell'albero delle
  pagine, né nella ricerca, né fra le pagine da cui far partire l'app, né
  fra i database da mostrare in una vista collegata. E dal quaderno non si
  arriva al menu principale: nei menu "/" e "+" delle pagine dei quaderni
  non ci sono Page, Link to page e i database; nei tre puntini non ci sono
  preferiti, ricerca, Duplicate e Move to. **Eliminare** (una pagina, una
  sezione, un gruppo, un quaderno) è **per sempre**, perché il cestino è
  del menu principale: prima lo chiede, dicendo quante pagine se ne vanno.
  Una sezione protetta, o un gruppo o un quaderno che ne contiene una,
  chiede prima impronta o password
- **[Progetto]** **Database alla versione 35**: le tabelle `notebooks` e
  `notebook_sections` (sezioni e gruppi insieme, perché formano un unico
  albero con un ordine solo), e `pages.notebookSectionId`. Le pagine sono
  pagine vere, così hanno tutto l'editor gratis; `orderIndex` è il loro
  ordine nella sezione. `NotebookRepository`, `NotebooksViewModel`,
  `NotebookSectionViewModel`, `Notebooks.kt` (barra, menu, schermata della
  sezione), `NotebookStrings` nelle otto lingue. `lockingPageOf` ora sa
  delle sezioni protette; "Remove password" nelle impostazioni conta anche
  quelle. Test `NotebookRepositoryTest` (5) e `MigrationTest` 34→35, col
  file `v34.sql`
- **Fuori, per ora** (dal menu di OneNote nella foto): Export, Move or
  Copy, Merge into Another Section, Copy Link to Section (e Copilot, che
  non interessa). Neanche l'ordinamento delle pagine, le sottopagine
  rientrate e un cestino dei quaderni. Si aggiungono se servono
- Verificato sul telefono (con un quaderno di prova, poi eliminato): prima
  di installare, copia del database del telefono; migrazione alla 35 sul
  posto (la colonna nuova in fondo a `pages`), le 10 pagine dell'utente
  tutte lì. "Notebooks" acceso nella barra, "+ New notebook" con nome
  selezionato e colori → "Prova quaderni" col quaderno blu e "New Section
  1"; tenuto premuto → il menu del quaderno → New section group
  "Languages" → dentro, New section "Deutsch" (rossa) → **si apre da
  sola**, con "Prova quaderni › Languages"; "Add page" → la pagina con
  "Untitled page", la riga e "Saturday, 2026年09月26日 08:47"; titolo
  "Esempio" e una riga di testo; nel menu "/" niente Page, e "/data" →
  "Nothing with that name"; i tre puntini con Draw, Delete page, Lock page,
  Lock with password, Updates, 2 words. Indietro → "Esempio" nell'elenco.
  Nella barra l'albero come nella foto, "Deutsch" evidenziata. "Password
  protect this section" → la sezione si chiude subito (lucchetto accanto
  al nome, "This section is locked", l'impronta chiesta da sola; annullata,
  non l'ho aperta). Delete del quaderno → "Everything inside goes with it,
  1 page included" → eliminato: nel database zero quaderni, zero sezioni,
  zero pagine di quaderni, le 10 pagine dell'utente intatte
- **[Bug fix]** **[📱 Mobile]** (trovato provando) Il blu del quaderno nuovo non era uno
  dei dodici colori, e nella scelta nessuno aveva la spunta: ora è il
  primo. Verificato: la spunta sul blu
- **Non provato sul telefono**: l'impronta chiesta prima di eliminare una
  sezione protetta (per chiudere la prova sarebbe servita l'impronta
  dell'utente), l'immagine come icona di un quaderno, i colori cambiati
  dopo, Rename e Move up/down dai menu (su e giù li prova il test)

### 🔧 v0.58.0.1 — 26/09/2026 · 08:27 — La freccetta del toggle, un pelino più su
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; commit solo
in locale)*

- **[Bug fix]** **[📱 Mobile]** **Il triangolino del toggle stava un po' basso** rispetto al
  titolo. Il triangolo è in mezzo al suo quadrato di 24dp e la riga del
  titolo è in mezzo ai suoi 24, ma le lettere non stanno in mezzo alla loro
  riga: stanno più su, perché sotto c'è il posto delle gambe di g, p e q.
  Misurato sul telefono (in pixel, sulla schermata): il triangolo aveva il
  centro 5 pixel sotto il centro delle minuscole e 9 sotto quello delle
  maiuscole. Ora sale di 2dp (`TOGGLE_ARROW_LIFT`) e il suo centro sta fra
  i due, 2 pixel sopra le minuscole e 2 sotto le maiuscole. Vale per ogni
  toggle, chiuso e aperto
- Verificato sul telefono, sul toggle "Riga dentro il toggle" in fondo a My
  Space: prima 2422–2455 (in pixel), dopo 2415–2448, con la "R" del titolo
  a 2414–2445

### 🆕 v0.58 — 26/09/2026 · 08:20 — Il lucchetto accanto ai tre puntini
*(Claude Code sul PC; verificato sul telefono lo stesso giorno
dall'utente; 141 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** Su una pagina bloccata con la password, **alla
  sinistra dei tre puntini** c'è un secondo pulsante **identico** — lo
  stesso cerchio grigio semitrasparente, la stessa grandezza — con **un
  lucchetto chiuso**. Toccato, dice "Protected by a password"
  ("Protezione con password attiva"). Anche sui database aperti a schermo
  intero, accanto ai loro tre puntini (lì senza cerchio, come i tre
  puntini). Compare solo sulla pagina bloccata lei stessa, come
  l'interruttore "Lock with password" nel menu: le sue sottopagine no
- **Verificato sul telefono, dalla voce di prima**: il database che
  l'utente ha bloccato ("Untitled", dentro "Titolo tre H3") **è diventato
  un collegamento** col lucchetto accanto al nome — la conversione, fin qui
  provata solo dal test, funziona anche sul telefono
- Il lucchetto accanto ai tre puntini si vede solo con la pagina aperta,
  e le pagine bloccate si aprono con l'impronta o la password dell'utente:
  **l'ha guardato l'utente**, aprendo "Untitled" in "Titolo tre H3"

### 🔧 v0.57.0.1 — 26/09/2026 · 08:16 — La scheda della galleria di una pagina bloccata
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 141 test
passati; commit solo in locale)*

- **[Bug fix]** **[📱 Mobile]** Nella vista Gallery **di qualunque database** (anche dentro
  una pagina, anche una vista collegata), la scheda di una pagina bloccata
  con la password mostrava la sua copertina. Ora al posto dell'anteprima
  c'è **uno sfondo grigio scuro sfumato, sempre uguale, con un lucchetto
  bianco** in un cerchio; il lucchetto cresce con la dimensione della
  scheda. Vale per "Page cover" e per "Page content"; con "None" la scheda
  non ha anteprima e resta com'è. Tenendo premuta la scheda non c'è più
  "Reposition cover", che avrebbe fatto rivedere la copertina
- **[Bug fix]** **[📱 Mobile]** (trovato lavorandoci) La lente accanto a Sort cercava anche
  dentro il testo delle pagine bloccate, e trovandolo illuminava la riga:
  ora le salta, come la ricerca generale
- **[Progetto]** La copertina e il testo di una pagina bloccata non escono
  nemmeno dal database (`observeRowCovers`, e le tre query della lente);
  le righe bloccate arrivano alla galleria da `observeLockedRowIds`, seguite
  solo mentre la galleria ha un'anteprima. Lo sfondo è `LockedCardPreview`,
  scuro anche col tema chiaro perché il lucchetto resti bianco. Test in
  `PageLockTest`
- Verificato sul telefono, nel database a galleria di My Space: la scheda
  "UntitledZeta" (bloccata dall'utente, con una copertina) mostra lo sfondo
  col lucchetto, "Gioco uno" accanto la sua copertina; tenuta premuta,
  "UntitledZeta" ha Icon, Edit property e Delete, senza "Reposition cover",
  che invece "Gioco uno" ha ancora

### 🆕 v0.57 — 26/09/2026 · 08:04 — Bloccare pagine e database con la password o l'impronta
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne il
database che diventa un collegamento, vedi in fondo; 140 test passati;
commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Lock with password"** nei tre puntini di ogni
  pagina e di ogni database, sotto "Lock page", con l'icona dell'impronta e
  l'interruttore. Non c'è in My Space: bloccarla chiuderebbe l'app intera.
  **La prima volta si sceglie la password** (almeno 4 caratteri, scritta
  due volte) e se aprire anche con l'impronta; dopo, bloccare è immediato.
  Bloccata, la pagina si richiude subito: al suo posto un lucchetto, e
  **la richiesta dell'impronta parte da sola**; "Use password" passa alla
  password. Con la pagina si chiude **tutto quello che ha dentro**:
  sottopagine, database, pagine delle righe. Nella barra laterale non
  mostra più cosa contiene, la ricerca ne trova solo il titolo (senza il
  testo), e il suo collegamento ha accanto un piccolo lucchetto. Aperta,
  resta aperta finché ci si muove al suo interno; andando altrove, o
  uscendo dall'app per più di 30 secondi, si richiude. Togliere il blocco
  (l'interruttore spento) chiede di nuovo impronta o password
- **[Nuova funzionalità]** **[📱 Mobile]** **Un database bloccato, dentro una pagina,
  diventa un collegamento**, come con "Turn into page", e aprirlo chiede
  impronta o password. Si chiudono anche le sue righe e le sue viste
  collegate: una vista collegata dentro una pagina aperta mostra, al posto
  della tabella, la richiesta di sbloccarlo
- **[Nuova funzionalità]** **[📱 Mobile]** In Settings, sotto "Time zone", **"Password for
  locked pages"** (Set / Not set): l'impronta sì o no, **"Change
  password"** (prima si conferma che si è noi) e **"Remove password"**,
  che funziona solo quando nessuna pagina è più bloccata — altrimenti
  resterebbero chiuse per sempre. Dalla finestra della password,
  **"Forgot the password?"** chiede il blocco schermo del telefono (PIN,
  sequenza o impronta) e poi una password nuova
- **[Progetto]** **Database alla versione 34**: `pages.isProtected`. La
  password non si salva: si tiene solo un hash PBKDF2 col suo sale, nelle
  preferenze dell'app (`AppLock`, `PasswordHash`). Il cancello `LockGate`
  sta davanti a ogni pagina e database a schermo intero (`AppNavHost`) e
  davanti ai database dentro le pagine; `lockingPageOf` risale
  collegamenti, righe e viste collegate per trovare chi blocca cosa. La
  ricerca e la barra laterale filtrano in `PageRepository`; le anteprime
  delle schede della galleria saltano le pagine bloccate. L'impronta è il
  `BiometricPrompt` di sistema (permesso `USE_BIOMETRIC`). Test
  `PageLockTest` e `MigrationTest` (33→34, col file `v33.sql`)
- **[Bug fix]** **[📱 Mobile]** (trovato provando) Tornando indietro da una pagina bloccata
  appena aperta, la richiesta dell'impronta ripartiva dalla schermata che
  se ne stava andando: ora parte solo quando la schermata è davvero
  davanti
- Verificato sul telefono: la prima volta la finestra per scegliere la
  password → pagina bloccata, col lucchetto; **l'impronta l'ha provata
  l'utente**, e la pagina si è sbloccata.
  Password sbagliata → "Wrong password"; quella giusta → la pagina si
  apre; indietro → nessuna richiesta; riaperta → la richiesta torna; il
  lucchetto accanto al collegamento in My Space; togliere il blocco dal
  menu chiede di confermare. Alla fine Settings → "Remove password" →
  "Password removed", "Not set": **la password di prova non c'è più**, e
  la prima volta che l'utente blocca qualcosa sceglie la sua. **Il
  database che diventa un collegamento non è stato provato sul
  telefono**: lo prova il test
- Notato: i disegni della pagina "Titolo tre H3" erano spariti già prima
  dell'installazione; li aveva cancellati l'utente, non
  è un errore

### 🆕 v0.56 — 26/09/2026 · 00:23 — Svuotare la cache, cancellare tutto il contenuto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne la
cancellazione vera, vedi in fondo; 133 test passati; commit solo in
locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** In Settings, **sotto "Time zone"** e separate da
  una riga, due voci:
  - **"Clear cache"**, con accanto quanto occupa: toglie i soli file
    temporanei, che l'app rifà quando servono. Chiede conferma, poi dice
    "Cache cleared". Pagine, database, disegni e immagini non si toccano
  - **"Delete all content"** ("Pagine, database, disegni, immagini e
    cestino"): cancella ogni pagina, database, riga, disegno, la
    cronologia delle modifiche, il cestino e le immagini delle pagine.
    Restano le impostazioni e i widget della barra. **Non si può
    annullare**, quindi il pulsante rosso "Delete everything" si accende
    solo dopo aver **scritto la parola** che la finestra chiede ("DELETE"
    in inglese, "ELIMINA" in italiano). Fatto, My Space rinasce vuota e
    **l'app riparte da capo**: le schermate aperte avevano ancora in mano
    il contenuto di prima, e un loro Annulla l'avrebbe rimesso
- **[Progetto]** `AppCleanup`: la cache è `cacheDir` (e quella esterna);
  il contenuto si cancella con `clearAllTables()` di Room, poi la pagina
  principale si ricrea subito (il resto dell'app dà per scontato che ci
  sia), si svuota la cartella delle immagini (`PageImageStore.deleteAll`)
  e si rimette la sveglia delle notifiche. Test `AppCleanupTest`, con
  Robolectric, su un database e dei file di prova
- Verificato sul telefono: le due voci sotto "Time zone"; "Clear cache" (la
  cache era già vuota, 0 B) → conferma → "Cache cleared". "Delete all
  content" → la finestra con l'avviso e la parola da scrivere, "Delete
  everything" spento finché la parola non c'è → annullata: le 9 pagine
  dell'utente ci sono tutte. **La cancellazione vera non è stata fatta sul
  telefono** (avrebbe cancellato le note dell'utente): la prova il test
- Notato: in un primo tentativo di arrivare alle impostazioni il menu
  laterale non si era aperto, e un tocco è finito sull'icona delle
  impostazioni del database a galleria di My Space; nessuna scelta toccata

### 🆕 v0.55 — 26/09/2026 · 00:14 — La proprietà "Color cell"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne le
targhette della bacheca e della galleria; 131 test passati; commit solo in
locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Color cell"** fra i tipi di proprietà (col
  secchiello), nei database normali e in quelli semplici. Nella tabella il
  colore **riempie tutta la cella**; il testo, se c'è, sta sopra, **nero o
  bianco** a seconda di cosa si legge meglio su quel colore. Toccando la
  cella si apre la sua finestra: com'è, il **testo (facoltativo)** e il
  **colore**, che si sceglie col selettore del pennello (spettro, grigi,
  RGB, Hex; "No color" lo toglie). Si salva con "Done". Nella scheda di una
  riga è un riquadro colorato; nelle schede della bacheca e della galleria
  e nell'elenco, una targhetta del suo colore. Col database bloccato si
  guarda e basta
- **[Progetto]** Il valore sta nella cella come gli altri, in una stringa:
  `"#RRGGBB|testo"`, solo il colore o solo il testo (`ColorCellValue.kt`).
  Nessuna migrazione: il tipo di una colonna è già salvato come testo.
  L'ordinamento e la ricerca guardano il testo, non il codice del colore;
  la tabella non si raggruppa per una cella colorata (il gruppo si
  chiamerebbe "#E03E3E"). Cambiando il tipo della colonna i valori restano
  quelli, come per gli altri tipi. Test `ColorCellValueTest`
- **[Bug fix]** **[📱 Mobile]** (trovato provando) Sul verde acceso scelto nella prova
  (#12F20D) il testo veniva bianco e non si leggeva: la soglia fra nero e
  bianco è scesa a metà
- Verificato sul telefono, nella tabella "Untitled" di My Space: "Color
  cell" nell'elenco dei tipi → colonna nuova, cella "—"; toccata → la
  finestra; colore dal selettore (#12F20D) → anteprima verde; testo
  "Fatto" → "Done" → la cella tutta verde con "Fatto" (salvato
  "#12F20D|Fatto"); la scheda della riga → riquadro verde con "Fatto". Poi
  la proprietà di prova tolta ("Delete property"): la tabella è com'era

### ✏️ v0.54.3 — 26/09/2026 · 00:03 — Via il cestino accanto ai collegamenti alle pagine
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 126 test
passati; commit solo in locale)*

- **[Rimosso]** **[📱 Mobile]** Il cestino a destra di ogni collegamento a una pagina
  dentro il testo (e la sua scritta "Remove link"). Per eliminare si tiene
  premuto il collegamento: nel menu del blocco, in fondo, c'è "Move to
  trash". Su una pagina bloccata il menu non si apre, come prima
- Verificato sul telefono, su "Titolo tre H3" in My Space: il cestino non
  c'è più; tenendo premuto si apre il menu con "Move to trash" (richiuso
  senza toccare niente)

### ✏️ v0.54.2 — 25/09/2026 · 23:57 — Il dito disegna, due dita scorrono, il palmo non conta
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, le due dita
dall'utente il 26/09/2026, vedi in fondo; 126 test passati; commit solo in
locale)*

- **[Modifica]** **[📱 Mobile]** **Mentre si disegna, un dito disegna e la pagina
  scorre solo con due dita**, sempre. Con la mano spenta un dito non fa
  niente (non scorre più la pagina)
- **[Modifica]** **[📱 Mobile]** **La mano sta accanto a ✓, Annulla e Ripeti**,
  sempre in vista, blu quando il dito disegna
- **[Rimosso]** **[📱 Mobile]** Lo spegnimento automatico del dito al primo uso della S Pen,
  e il suo avviso. La scelta salvata ha un nome nuovo (`fingerDrawing`):
  quella spenta da sola sul telefono dell'utente si è dimenticata, e il dito
  è tornato a disegnare
- **[Modifica]** **[📱 Mobile]** **Il palmo non conta**, al posto dello
  spegnimento: mentre la S Pen è vicina allo schermo (anche solo sospesa) o
  lo tocca, e per 0,6 secondi dopo, i tocchi della mano si ignorano; e se la
  mano ha cominciato un segno prima che la penna tocchi, quel segno si butta
  e disegna la penna
- Verificato sul telefono, su "Titolo tre H3" (dove ci sono i disegni
  dell'utente, lasciati com'erano): la mano accanto a Ripeti, blu; un dito
  → disegna; S Pen (simulata) e subito dopo un dito → la linea del dito non
  c'è; un dito un secondo dopo → disegna. Le tre linee di prova tolte con
  Annulla (restano i 7 tratti dell'utente). **Lo scorrimento con due dita
  l'ha provato l'utente il 26/09/2026: "funziona"**. Da provare a mano
  resta la penna sospesa col palmo appoggiato
- Notato: la S Pen simulata da adb aveva lasciato accesa l'icona mobile
  della S Pen di Samsung, come se la penna fosse fuori dal foro. L'utente
  l'ha estratta e rimessa, e l'icona è sparita. Da qui in poi le prove di
  disegno si fanno col dito, e quello che vuole la penna vera lo prova
  l'utente

### ✏️ v0.54.1 — 25/09/2026 · 23:49 — Il colore delle penne col selettore del pennello
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 126 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** Nella finestra di una penna, sotto "Colors", c'è
  il colore in uso (il tondo e il codice, "#2383E2"); toccandolo si apre
  **il selettore del pennello**, lo stesso della barra del testo: il
  riquadro di tinta e saturazione, la striscia dei grigi dal bianco al
  nero, Rosso/Verde/Blu ed Hex. Si apre sul colore della penna. Al posto di
  "No color" c'è **"Text color"**, che rimette il colore del testo (quello
  che segue il tema); sotto l'anteprima c'è il nome della penna invece di
  "Text color"
- **[Rimosso]** **[📱 Mobile]** I venti tondi dei colori fissi della finestra della penna
- **[Progetto]** `ColorPickerSheet` prende due scritte da fuori
  (`previewLabel`, `noColorLabel`); il pennello e le barre dei widget non
  cambiano. Il colore della penna passa al selettore come "#RRGGBB" e torna
  pieno (`inkHex`, `hexToInkColor`, test `InkColorTest`)
- Verificato sul telefono: penna blu → "Colors" → il selettore del
  pennello su #2383E2 (35, 131, 226), con "Text color" e "Apply"; "Apply"
  sullo stesso colore → la penna resta #2383E2. Le altre penne sono come le
  aveva lasciate l'utente
- Notato: l'utente aveva già disegnato su "Titolo tre H3" con la S Pen vera
  (scarabocchi bianchi e rosa, con la prima penna fatta rosa e più
  spessa): **la pressione funziona**, il tratto si allarga e si stringe.
  Lasciati com'erano. Nella prova è rimasta in mano la penna blu

### 🆕 v0.54 — 25/09/2026 · 23:42 — Disegnare a mano sulle pagine, anche con la S Pen
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, con la S Pen
simulata da adb, vedi in fondo; 123 test passati; commit solo in locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Draw"**, con la matita, nei tre puntini di
  ogni pagina di testo (anche le pagine delle righe; non nei database a
  schermo intero). Si disegna **su tutta la pagina dalla fine della
  copertina in giù**, copertina esclusa: sopra il testo, il titolo, un
  database. In fondo c'è la barra del disegno al posto di quella del testo:
  **✓** per finire (anche il gesto indietro), **Annulla** e **Ripeti** (solo
  dei disegni, separati da quelli del testo), la **gomma** e sei penne come
  nella foto, e la **mano** per disegnare col dito. Mentre si disegna il
  testo si legge e basta; finito, i disegni restano e si vedono sempre
- **[Nuova funzionalità]** **[📱 Mobile]** **Le penne**: penna, matita, pennarello ed
  evidenziatore. Di partenza: penna del colore del testo (bianca sul tema
  scuro, nera sul chiaro), penna blu, penna rossa, matita grigia,
  pennarello verde, evidenziatore giallo. Toccando lo strumento già in mano
  (o tenendolo premuto) si apre la sua finestra, come nella foto:
  **Thickness** (cinque spessori, e "−" "+" per i passi in mezzo), **Types**
  (quale delle quattro è) e **Colors** (dal 25/09/2026 col selettore del
  pennello; prima venti colori fissi). La penna si allarga premendo di più con la S Pen; la matita
  è un po' trasparente; l'evidenziatore è trasparente, a punta piatta, e
  sta sotto gli altri tratti. Le scelte si ricordano
- **[Nuova funzionalità]** **[📱 Mobile]** **La gomma**, con **Thickness** e **Types**:
  **Stroke Eraser** toglie tutto il tratto toccato, **Point Eraser** solo
  quello che passa sotto la gomma (il tratto si spezza). Mentre si cancella
  si vede il cerchio della gomma. Una passata è un Annulla solo
- **[Nuova funzionalità]** **[📱 Mobile]** **La S Pen.** Sì, funziona: il telefono è un
  Galaxy S25 Ultra (SM-S938B) e la S Pen arriva all'app come penna vera, con
  la **pressione**. La prima volta che la si usa il dito smette di
  disegnare e **scorre la pagina** (un avviso lo dice): così il palmo
  appoggiato non lascia segni. La mano nella barra lo fa tornare a
  disegnare; col dito che disegna, **due dita** scorrono. Col **tasto
  laterale** premuto la S Pen **cancella**, come su Samsung Notes (la
  gomma scelta nella barra). Quello che non c'è: i gesti a distanza (Air
  actions), che la S Pen del S25 Ultra non ha più
- **[Progetto]** **I disegni seguono quello su cui li si è fatti.** Ogni
  tratto è attaccato alla voce della pagina dove è cominciato — il gruppo
  di righe fra due divisori, un database, un toggle, o la testata col
  titolo — con le coordinate contate dal suo angolo (`InkStrokeEntity`,
  `InkGeometry.kt`). Se sopra si aggiungono righe o un divisore, il disegno
  scende con le sue righe. Cancellato quello su cui era fatto (il database
  che si era cerchiato), il disegno non si vede ma resta: annullando la
  cancellazione torna. Duplicando la pagina i disegni vengono con la copia
- **[Progetto]** **Database alla versione 33**: la tabella `ink_strokes`,
  un tratto per riga (i punti in una stringa compatta, "x,y,pressione" in
  dp), cancellata con la sua pagina. Test `MigrationTest.from32…` su un
  file alla 32 (`schema/v32.sql`); 8 test nuovi dei conti
  (`InkGeometryTest`: salvataggio dei punti, le due gomme, gli ancoraggi,
  gli spessori). Gli strumenti stanno nelle preferenze (`InkPrefs`), non
  nel database, come le impostazioni
- Verificato sul telefono, su "Titolo tre H3", con la S Pen simulata da
  adb (`input stylus`): "Draw" nei tre puntini → barra; due tratti →
  salvati, attaccati alla riga sotto il titolo, e l'avviso "S Pen
  rilevata"; menu della penna come nella foto; Stroke Eraser su una linea →
  sparita, Annulla → tornata; Point Eraser in mezzo all'altra → spezzata in
  due; matita, pennarello ed evidenziatore sul titolo (attaccato alla
  testata); il dito, spento, non disegna; **divisore messo sopra le righe
  disegnate → i tratti sono scesi con le righe**, l'evidenziatore rimasto
  sul titolo; un tratto è rimasto dopo aver ucciso e riaperto l'app. Poi
  tutto rimesso com'era: testo, disegni (zero) e strumenti di partenza.
  **Da provare con la S Pen vera**: la pressione (adb la dà sempre piena),
  il tasto laterale, le due dita
- Notato: le righe-notifica di prova in "Titolo tre H3" non c'erano più.
  Dalla cronologia della pagina le ha svuotate l'utente alle 23:16–23:17,
  prima dell'aggiornamento: non è stata la migrazione

### 🆕 v0.53 — 25/09/2026 · 23:12 — Le notifiche: "/notification", le date dei database, timer e pomodoro
*(Claude Code sul PC; verificato sul telefono lo stesso giorno,
tranne la riaccensione vera, vedi in fondo; 114 test passati; commit solo in
locale)*

- **[Nuova funzionalità]** **[📱 Mobile]** **La riga "/notification".** Si scrive
  `/notification 22.10.2026 19:41 Buttare l'immondizia` e si preme Invio:
  il comando sparisce e resta una riga con la **campanella**, il testo
  "Buttare l'immondizia" e in fondo, in un riquadro blu, **quando suona**;
  la scrittura continua nella riga nuova sotto. Toccando il riquadro si
  sceglie il giorno e poi l'ora, con le finestre di Samsung. La data si
  può scrivere anche `22/10/2026`, `22-10-26`, `2026-10-22` o senza
  l'anno (`22.10`: quest'anno, o il prossimo se è già passata); l'ora
  anche `19.41`. **Senza ora, alle 9:00**; con la sola ora, oggi (o domani
  se è già passata); con niente, le prossime 9:00. Il testo fra parentesi
  perde le parentesi. Vale anche `/notifica`. Senza testo, la notifica
  mostra il **nome della pagina** (la riga vuota lo suggerisce in grigio).
  C'è anche la voce **"Notification"** nel menu "/" e in "Turn into":
  lascia il testo com'è, mette le prossime 9:00 e apre subito la scelta
  del giorno. Backspace a inizio riga la fa tornare testo, come una
  casella da spuntare. Una notifica già passata si fa grigia
- **[Nuova funzionalità]** **[📱 Mobile]** **"Include notification"** nella finestra della
  data dei database, sotto "Include time" (e sotto le sue ore): la data
  manda una notifica col **nome della pagina della riga** ("Revive +") e
  sotto il nome del database. All'ora della data, o **alle 9:00** se l'ora
  non c'è; con una data di fine, **il primo giorno e l'ultimo** (sotto:
  "Album · Inizio", "Album · Fine"); una data che si ripete suona a ogni
  ripetizione. Sotto l'interruttore una riga dice quando suonerà. Nella
  cella, accanto alla data, una campanellina
- **[Nuova funzionalità]** **[📱 Mobile]** **La fine di un timer e di ogni fase del
  pomodoro** mandano una notifica ("Tempo scaduto", sotto "Timer · 10 m";
  alla fine della sessione il suo nome e "È ora della pausa · 5 min"; alla
  fine della pausa "Pausa finita" e di nuovo la sessione),
  e adesso suonano **anche ad app chiusa**
- **[Nuova funzionalità]** **[📱 Mobile]** Toccando una notifica si apre **la sua
  pagina**: quella della riga "/notification", o quella della riga del
  database (se la riga non può avere una pagina, il database)
- **[Nuova funzionalità]** **[📱 Mobile]** Il **permesso delle notifiche** (Android 13 e
  dopo) si chiede **quando serve**, non all'avvio: creando una notifica,
  accendendo "Include notification", facendo partire un timer o un
  pomodoro. Nelle impostazioni, al posto di "Le notifiche non ci sono
  ancora", una riga dice da dove arrivano. **"Turn off notifications" e
  "Turn off for a while" valgono per tutte**; il suono è quello scelto in
  "Notification sound" (o quello del pomodoro), una volta sola
- **[Progetto]** Come funziona (`data/notify/`): **una sola sveglia di
  Android** (`NoteAlarms`, `AlarmManager` esatta, che suona anche col
  telefono in standby) messa alla prima cosa che deve suonare, fra
  promemoria e fine di timer e pomodori. Quando suona, `AlarmReceiver` —
  anche ad app chiusa, Android fa partire l'app solo per questo — manda
  quello che è arrivato **dall'ultima volta a adesso** (`deliveredUpTo`),
  così niente arriva due volte, e una notifica persa a telefono spento
  arriva alla riaccensione se non sono passate più di 12 ore; poi rimette
  la sveglia alla cosa dopo. Dopo un riavvio, un aggiornamento o un cambio
  d'ora la rimette `RescheduleReceiver`. Una sveglia messa per una cosa
  poi cancellata non fa danni: quando suona si rilegge il database. I
  canali "Promemoria" e "Timer e pomodoro" non hanno suono proprio: lo fa
  l'app (`WidgetStore.playOnce`), così il suono scelto nelle impostazioni
  vale subito. Il calcolo di cosa suona e quando (`Reminders.kt`) e il
  comando (`NotificationCommand.kt`) sono senza Android, con 18 test
  (`NotificationsTest`, sugli esempi della richiesta)
- **[Progetto]** Permessi nuovi nel manifest: `POST_NOTIFICATIONS`,
  `USE_EXACT_ALARM` (Android 13 e dopo; `SCHEDULE_EXACT_ALARM` fino al
  12), `RECEIVE_BOOT_COMPLETED`. Ancora nessun permesso di rete
- **[Progetto]** **Database alla versione 32**: `blocks.notifyAt`, l'ora
  delle righe "/notification", aggiunta con `ALTER TABLE` (colonna vuota
  per tutte le righe di prima). Test `MigrationTest.from31…` su un file
  alla 31 (`schema/v31.sql`). Il segno di "Include notification" sta
  invece nel valore della cella data, in fondo (`…|n`): i valori scritti
  prima restano validi
- Verificato sul telefono: `/notification 19:36 Prova notifica` + Invio →
  riga con campanella e "25.09.2026 19:36", e alla prima notifica Android
  ha chiesto il permesso. **Ad app chiusa** (processo ucciso):
  `/notification 19:41 Test app chiusa` → alle 19:41 Android ha fatto
  ripartire l'app e la notifica "Test app chiusa", sotto "Titolo tre H3",
  è arrivata. L'utente ha provato "Include notification" su "Riga
  semplice" alle 19:35, ad app chiusa: "funziona perfettamente"

- **[Bug fix]** **[📱 Mobile]** Non era l'app chiusa: nel database del telefono e nelle
  sveglie di Android si vedeva che la sua riga era per **domani**. Aveva
  scritto `/notification` senza ora alle 19:32, quindi le prossime 9:00,
  cioè domani; poi, toccando il riquadro, aveva cambiato solo l'ora
  (19:34), e il giorno era rimasto il 26. Il riquadro diceva
  "26.09.2026 19:34" e non se n'era accorto. Ora **oggi e domani si
  scrivono per nome** ("Oggi 19:41", "Domani 09:00"), e se l'ora scelta è
  già passata un messaggio dice che la notifica non suonerà. La prova ad
  app chiusa qui sopra è stata fatta proprio per escludere l'altra causa

(erano già le 22:56:36: messa alle 22:58)

- Verificato sul telefono, ad app chiusa (processo ucciso) in tutti i
  casi: **riavvio simulato** — app fermata del tutto, che toglie le sue
  sveglie come un riavvio, poi `RescheduleReceiver` chiamato a mano: la
  sveglia delle 22:59 è tornata, e la notifica è arrivata; **tocco sulla
  notifica** "Tocca per aprire" delle 22:58 → l'app si apre direttamente su
  "Titolo tre H3" (non su My Space, la pagina d'avvio); **pomodoro** da
  1 minuto → alle 23:01 "Session · Time for a break · 5 min"; **timer** di
  14 secondi aggiunto col "+" → "Time's up · Timer · 14 s". Poi tutto
  rimesso com'era: pomodoro fermo su 01:00, timer di prova eliminato col
  cestino, il primo timer riportato alle sue rotelle (00:21:02), il secondo
  mai toccato (00:14:00). Il riavvio vero del telefono non è stato provato:
  spegnerebbe il debug wireless
- **[Bug fix]** **[📱 Mobile]** (trovato provando) **La riga-notifica restava blu dopo aver
  suonato**, finché la pagina non si ridisegnava per altro: l'ora si
  guardava solo al disegno. Ora campanella e riquadro si fanno grigi da
  soli nel momento in cui la notifica suona (provato: "Diventa grigia"
  alle 23:12, a pagina aperta senza toccare niente)
- **[Bug fix]** **[📱 Mobile]** (trovato provando, c'era già prima) **La barra laterale
  senza pagine.** Se Android chiudeva l'app con la barra aperta, alla
  riapertura la barra tornava aperta da sola, ma senza passare dal
  pulsante che legge l'albero: sotto "Pages" non c'era niente, finché non
  la si chiudeva e riapriva. Ora l'albero si legge anche in quel caso
  (provato: barra aperta, app uccisa, riaperta → My Space, Titolo tre H3 e
  i due database al loro posto)
- Notato: le prove hanno lasciato in "Titolo tre H3" quattro righe-notifica
  già suonate ("Test app chiusa", due "Tocca per aprire", "Diventa
  grigia"); lasciate lì, sono sulla pagina di prova dell'utente

### ✏️ v0.52.6 — 25/09/2026 · 18:46 — I selettori di data e ora come quelli di Samsung
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 95 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **Il mese come quello di Samsung**
  (`SamsungCalendar` in `SamsungPickers.kt`), al posto del calendario di
  Material: in alto "Settembre 2026 ▾" con le frecce ‹ › ai lati, e il
  mese si cambia anche **trascinando in orizzontale**. Sei settimane a
  griglia, le domeniche in rosso, i giorni degli altri mesi spenti (ma si
  possono toccare), oggi in arancio, il giorno scelto in un **cerchio
  arancio**. Toccando il mese si passa alle **rotelle** di mese, giorno e
  anno (▴), nell'ordine in cui si scrive la data dove si è (in Italia
  giorno, mese, anno); il giorno si adatta al mese (dal 31 a febbraio si va
  al 28); ritoccando il mese si torna alla griglia, sul mese scelto. Anni
  dal 1900 al 2100
- **[Modifica]** **[📱 Mobile]** **"Set time" a rotelle** (`SamsungTimeDialog`):
  ore e minuti, sulle ventiquattro ore, **un minuto alla volta** — la
  richiesta "intervalli ad ogni secondo" letta come "ogni singolo valore"
  (la foto andava di cinque in cinque, e l'utente ha chiesto ore e minuti,
  non secondi). In fondo **Cancel | Done**, come nella foto
- **[Modifica]** **[📱 Mobile]** **"End date"**: con il calendario nuovo toccare
  un altro giorno **sposta** la data (prima, col calendario di Material, un
  secondo tocco faceva un periodo). Il periodo si fa con l'interruttore
  "End date", come su Notion: acceso, il primo tocco è l'inizio e il
  secondo la fine, con una fascia arancio chiaro fra i due
- **[Modifica]** **[📱 Mobile]** Lo stesso calendario, in una finestra "Set date"
  con Cancel | Done (`SamsungDateDialog`), anche dove si sceglie un giorno
  solo: la fine di una ripetizione, il filtro per data, le date proprie del
  widget Life progress
- **[Progetto]** Le rotelle sono **una sola** (`WheelPicker`), la stessa del
  timer della barra laterale (lì color pesca, qui in bianco); fanno anche le
  rotelle che si fermano ai capi (gli anni). Tolti il calendario e l'ora di
  Material (`DateRangePicker`, `DatePickerDialog`, `TimeInput`) e le loro
  conversioni dei millisecondi in UTC, che non servono più
- **[Bug fix]** **[📱 Mobile]** (trovato provando) La finestra di "Set time" finiva sotto la
  barra di navigazione, con Cancel e Done tagliati: dentro la finestra c'era
  un riquadro a tutto schermo con la scheda in fondo, e la finestra non sa
  dov'è la barra. Ora è la finestra stessa a stare in fondo, alta quanto la
  scheda
- Verificato sul telefono, sulla cella Date di "Riga semplice": il mese di
  settembre come nella foto, tocco sul 25 → cerchio arancio, trascinamento
  → ottobre, tocco sul mese → rotelle SEP · 25 · 2026, rotella del mese su
  OCT → ritorno alla griglia su ottobre col 25 scelto; Include time →
  Start time → "Set time" 09 : 00, minuti girati di tre → 09:03 nella riga.
  Poi Cancel: nella cella non è stato salvato niente
- Notato: in cima a "My Space" ci sono sei righe vuote prima di "Titolo
  uno H1", nate dalle prove dell'utente delle 17:45; lasciate com'erano

### ✏️ v0.52.5 — 25/09/2026 · 18:18 — Il cestino per i timer aggiunti
*(Claude Code sul PC;
verificato sul telefono lo stesso giorno; 95 test passati; commit solo in
locale)*

- **[Modifica]** **[📱 Mobile]** **Il cestino dei timer aggiunti.** Nel widget
  Timer, dalla seconda pagina in poi, in alto a destra c'è un **cestino**
  che elimina quel timer, in qualunque stato: sulle rotelle, in corsa, in
  pausa, finito. Sulla prima pagina non c'è: **il primo timer non si
  elimina mai**, e la regola sta anche nel salvataggio
  (`TimerWidget.withoutTimer`), non solo nella schermata. Se il timer
  eliminato correva, la sua sveglia se ne va con lui. Test nuovo
  `WidgetsTest.onlyTheTimersAddedAfterTheFirstCanBeDeleted`
- **[Rimosso]** **[📱 Mobile]** Il **"Cancel"** sulle rotelle, che c'era con più di un
  timer: eliminava il timer, ma non si capiva — e valeva anche per il
  primo. Ora sulle rotelle c'è solo Start, e per eliminare il cestino
- Verificato sul telefono, coi due timer dell'utente (10:02 e 14:00, sulle
  rotelle): sulla prima pagina solo Start, sulla seconda il cestino; fatto
  partire il secondo, aggiunto un terzo col "+", fatto partire (9:58, con
  cestino, ↻ e +) ed eliminato col cestino → due timer; il secondo
  riportato alle rotelle con ↻ — i due timer dell'utente sono com'erano

### ✏️ v0.52.4 — 25/09/2026 · 18:04 — Quattro bug, il link delle immagini tolto, la pagina vuota
*(Claude Code sul PC; tutto verificato sul telefono lo stesso giorno; 94
test passati; commit solo in locale, **non** su GitHub finché l'utente non
lo dice)*

- **[Rimosso]** **[📱 Mobile]** **Il link delle immagini** (icona e copertina). Prima c'era
  una pastiglia "Link" accanto a "Upload", spenta finché il campo era
  vuoto, che scaricava l'immagine: sembrava un'etichetta. Per qualche ora
  è diventata un pulsante "Download" accanto al campo, col tasto Invio e i
  motivi dell'errore spiegati (un sito che rifiuta, un link che non è
  un'immagine, niente rete); provandolo sul telefono il download è
  risultato lentissimo (oltre 20 secondi anche per una pagina minuscola,
  0,1 s dal PC: probabilmente la rete del telefono), e l'utente ha chiesto
  di togliere il link. Tolto tutto: il campo, il pulsante, lo scaricamento
  (`PageImageStore.saveFromUrl`) e le sue scritte. La finestrella ha solo
  **Upload** (e Reposition / Remove quando c'è un'immagine)
- **[Rimosso]** **[📱 Mobile]** **Il permesso di andare in rete** (`INTERNET` nel
  manifest): serviva solo al link. I font cinesi e giapponesi li scarica
  Google Play Services col permesso suo. Da ora l'app non si collega a
  niente
- **[Bug fix]** **[📱 Mobile]** Scegliendo dalla galleria un file che non è un'immagine,
  la copia fallita restava nella cartella dell'app: si cancellava un file
  che aveva per nome il messaggio d'errore (`PageImageStore.saveFromGallery`)
- **Il PNG con lo sfondo bianco non era un bug dell'app.** Il file scelto
  era `Pictures/Telegram/IMG_20260925_143037_979.jpg`, un **JPEG**
  512×512 di 10.716 byte — esattamente quello copiato dall'app: Telegram,
  mandandolo come foto, l'aveva compresso in JPEG, che la trasparenza non
  ce l'ha. Mandato come file è rimasto trasparente. Vedi
  "Limiti noti"
- **[Bug fix]** **[📱 Mobile]** **Backspace fra righe vuote: la tastiera spariva** (bug 2).
  Con tante righe vuote, il backspace a inizio della seconda toglieva la
  **prima** riga invece della seconda: per il testo è uguale, ma il campo
  condiviso è legato al primo blocco del suo gruppo (la chiave nella
  lista), e tolto quello rinasceva da capo senza cursore. Il confronto fra
  prima e dopo, con righe tutte uguali, attribuiva la riga sparita alla
  prima; ora, quando le righe calano, la riga del cursore è sempre fra
  quelle riscritte, e si toglie la successiva (`updateRun`). Test nuovi in
  `EmptyLinesBackspaceTest`. Verificato: quattro righe vuote, backspace
  dalla seconda → tastiera aperta, "x" scritta sulla prima
- **[Bug fix]** **[📱 Mobile]** **Copia / Incolla / Select all nelle finestrelle che salgono
  dal basso** (bug 3). Il `ModalBottomSheet` di Material 1.2 si apre in una
  finestra aggiunta a mano, che non sa mostrare la barrettina del telefono:
  la parola si selezionava e non compariva niente. Non solo nel colore
  delle barre del Life progress bar: in **tutte** le finestrelle — il
  selettore di colore, il link dell'immagine, la ricerca dei fusi, le
  proprietà del database. Ora tutte passano da `NoteBottomSheet`
  (`SheetTextToolbar.kt`), che dentro usa una barrettina disegnata
  dall'app (`PopupTextToolbar`) con le voci e le scritte di Android.
  Trovata anche una trappola: la prima versione teneva lo stato "aperta"
  osservabile, il campo di testo lo leggeva mentre disegnava le maniglie e
  la richiudeva — aperta e chiusa quattro volte in 30 ms, poi più niente
  (visto nel registro). Verificato nel selettore di colore: Cut / Copy /
  Select all su "#FF0000", Copy → compare Paste, Select all seleziona tutto
- **[Bug fix]** **[📱 Mobile]** **Il toggle storto** (bug 4). Le righe dentro un toggle
  hanno ciascuna un campo suo, e Compose toglieva lo spazio sopra la prima
  riga: il testo stava ~3dp più su della freccetta e dei numeri, che si
  scrivono a parte (i numeri avevano in più 2dp di margine sopra). Ora la
  riga è fatta come quella del testo condiviso (24, testo in mezzo), senza
  i 2dp, e le righe dentro un toggle stanno al passo di quelle fuori (niente
  4dp sopra e sotto). Il quadratino delle caselle non va più alzato a mano
  (via `CHECKBOX_BASELINE_LIFT`). Verificato: freccetta centrata sul
  titolo, "1." sulla riga di "A", stesso passo dell'elenco fuori
- **[Bug fix]** **[📱 Mobile]** **"1." dentro un toggle non faceva l'elenco numerato**
  (bug 4): la regola c'era solo nel testo condiviso. Ora anche nelle righe
  dentro un toggle (`startNumberedListInBlock`). Verificato: Invio sulla
  riga vuota, "1." → elenco da 1, "Nuovo" scritto dentro; poi annullato
- **[Bug fix]** **[📱 Mobile]** **La pagina vuota: il primo Invio perdeva il cursore**
  (trovato da Claude provando il bug 2, sistemato su richiesta). Una
  pagina senza righe ne mostra una vuota che nel database non c'è; il
  primo Invio salvava solo la riga nuova, quella vuota spariva e con lei
  il campo: tastiera aperta, barra sparita, e quello che si scriveva non
  andava da nessuna parte. Per trovarlo è servito far stampare al
  database ogni sua scrittura: al tocco sulla pagina vuota non ne partiva
  nessuna. Ora la riga vuota ha lo stesso id finché si resta sulla pagina
  (prima cambiava a ogni lettura del database) e viene salvata **insieme**
  alla prima modifica, nella stessa transazione (`placeholder`,
  `unsavedPlaceholderIn`). Test nuovi in `EmptyPageTest`. Verificato:
  pagina "Untitled" svuotata, tocco sotto il titolo, Invio, "Riga" → barra
  al suo posto, "Riga" sulla seconda riga, nel database le due righe con la
  prima salvata; poi annullato
- Notato: il toggle "Riga dentro il toggle" di "My Space" alle 17:45 è
  stato modificato dall'utente (righe "A" rifatte, toggle chiuso); lasciato
  com'è

### ✏️ v0.52.3 — 25/09/2026 · 13:31 — Divisore sotto le pagine, menu dei font non più coperto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 89 test
passati; commit solo in locale)*

- **[Modifica]** **[📱 Mobile]** **Un divisore sotto l'albero delle pagine**
  nella barra laterale, uguale a quello sotto "Main menu": l'ultima
  pagina dell'albero e "Search" non sembrano più la stessa lista
  (`Sidebar.kt`). Verificato sul telefono: il divisore sta fra l'ultimo
  "Untitled" e "Search"
- **[Bug fix]** **[📱 Mobile]** **Il menu dei font era coperto dalla barrettina Copy /
  Cut / Select all** del telefono, quando c'era del testo selezionato:
  copriva Garamond e Arial. Il menu non prende il fuoco apposta (così la
  tastiera e la barra Aa restano), quindi il campo restava a fuoco con la
  selezione e il telefono lasciava la barrettina sopra il menu. Ora
  aprendo il menu la barrettina si chiude (`LocalTextToolbar.hide()` in
  `FontPickerChip`); la selezione resta, e la barrettina torna dopo la
  scelta. Verificato: selezionato "sotto", aperto il menu — tutto
  visibile, Garamond compreso — scelto Garamond → solo "sotto" in
  Garamond, ancora selezionato; poi annullato con ↶
- Notato durante la prova: nella pagina "My Space" la parola "Paragrafo"
  è in **Futura**. Non l'ha messa Claude: alle 13:21 era nel font
  normale, alle 13:27, appena reinstallata l'app e prima di toccare la
  pagina, era già in Futura. **Confermato dall'utente**, che l'aveva
  provata. Lasciata com'è

### ✏️ v0.52.2 — 25/09/2026 · 13:23 — Font e dimensione sul testo selezionato
*(Claude Code
sul PC; verificato sul telefono lo stesso giorno; schema alla versione
**31**; 89 test passati; commit solo in locale, **non** su GitHub finché
l'utente non lo dice)*

- **[Rimosso]** **[📱 Mobile]** **"Font" e "Font size" nelle impostazioni.** Erano due
  voci grigie, messe lì per un futuro font di tutto il Workspace: tolte,
  con le loro scritte (`Strings.font`, `Strings.fontSize`)
- **[Modifica]** **[📱 Mobile]** **Font e dimensione valgono solo per il testo
  selezionato.** Prima le due voci della barra Aa cambiavano **tutta la
  pagina**, qualunque cosa fosse selezionata (non era un bug: era fatta
  così il 24/09, "per ogni pagina"). Ora si comportano come il colore:
  si seleziona un pezzo di testo, si sceglie il font o si scrive la
  dimensione, e cambia **solo quel pezzo**; si seleziona un altro pezzo
  e gli si dà un altro font. Le due voci mostrano il font e la
  dimensione del testo selezionato (o di quello subito prima del
  cursore). Toccandole senza niente di selezionato compare **"Select some
  text first"**. Il font e la dimensione stanno nel testo stesso, come
  grassetto e colore (`RichTextSpan.font`, `RichTextSpan.size`;
  `setFontInRange`, `setSizeInRange`, `spanAt`;
  `PageEditorViewModel.applyFontToCapturedSelection`,
  `applySizeToCapturedSelection`, `selectionTypography`; a schermo
  `fontStyleOf`), e si annullano con ↶ come ogni altra modifica. Il 16
  di partenza non si scrive nel testo. Il testo scritto subito dopo un
  pezzo in un altro font ne prende il font, come il grassetto. Non
  c'è più niente "di pagina": via `PageTypography`, `LocalPageTypography`,
  `setPageFont`, `setPageFontSize`. Le celle delle tabelle semplici e il
  titolo della pagina, che prendevano il font della pagina, tornano al
  font di sistema (sono testo senza formattazione)
- **[Modifica]** **[📱 Mobile]** **Le righe si allungano per il testo grande.**
  Con una dimensione più grande di 16 in un gruppo di righe, **tutte**
  le righe di quel gruppo diventano più alte, quanto serve al pezzo più
  grande; perché non solo la sua, vedi "Limiti noti"
- **[Progetto]** Migrazione **30→31** (`MIGRATION_30_31`): le pagine che
  avevano un font o una dimensione "di pagina" li danno a ogni pezzo di
  testo dei loro blocchi che non ne ha già uno suo — così si vedono come
  prima — e poi `pageFont` e `pageFontSize` si svuotano in tutte le
  pagine. Le due colonne restano (toglierle vorrebbe dire ricostruire la
  tabella) ma non le scrive più nessuno. `MigrationTest` prova anche
  dalla 30 (schema in `test/resources/schema/v30.sql`, generato dal
  codice di Room al commit `a68a44c`, con una pagina in Garamond a 20 e i
  suoi blocchi); test nuovi in `RichTextFontTest`. Verificato sul
  telefono: backup del database, `user_version` 31, nessun crash
- Verificato sul telefono, nella pagina "My Space": selezionato "prova"
  → Consolas, **solo quella parola**; selezionato "titolo" → le voci
  dicono "Default" e "16", scelto Georgia e poi 28 → solo "titolo",
  grande, e le righe del gruppo più larghe; nel database
  `{"text":"prova","font":"CONSOLAS"}` e
  `{"text":"titolo","font":"GEORGIA","size":28}`; ↶ riporta "titolo" a 16
  e le righe al passo di prima; col solo cursore "Select some text
  first"; le impostazioni senza Font e Font size. "prova" in Consolas e
  "titolo" in Georgia sono rimasti nella pagina, per farli vedere
- Notato durante la prova (c'era già): con una parola selezionata, il
  menu dei font si apriva **sotto** la barrettina di sistema Copy / Cut /
  Select all, che copriva Garamond e Arial. **Corretto subito dopo**, vedi
  la voce qui sopra

### ✏️ v0.52.1 — 25/09/2026 · 12:59 — Timer: via i tempi pronti da 10, 15 e 30 minuti
*(Claude Code sul PC; verificato sul telefono lo stesso giorno)*

- **[Rimosso]** **[📱 Mobile]** I tempi pronti 00:10:00, 00:15:00 e 00:30:00 sotto le
  rotelle del Timer, quelli della seconda foto (v0.52): resta solo
  **Start**

### 🆕 v0.52 — 25/09/2026 · 12:56 — Cronometro e Timer fra i widget, come quelli dell'orologio Samsung
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 80 test
passati)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Cronometro e Timer fra i widget**, come quelli
  dell'orologio Samsung.
  - **Stopwatch** (`StopwatchWidget`, `StopwatchView` in
    `SidebarTimers.kt`): il quadrante a sessanta tacche coi centesimi al
    centro in color pesca, la lancetta **blu** per i secondi del tempo
    totale e, dopo il primo giro, quella **bianca** per il giro in corso
    (è così nelle foto: a 00:32.52 la bianca segna i 24,76 s del quarto
    giro). Pulsanti come chiesto: **Lap** (spento) e **Start**; in corsa
    **Lap** e **Stop** (rosso scuro); fermo **Reset** e **Resume**. Sotto,
    la lista **Lap / Lap times / Overall time**, dal giro più recente, col
    **più veloce in arancio e il più lento in rosso** come nella foto (da
    tre giri in su: con due sarebbero colorati tutti e due — scelta mia,
    da confermare). Si conta sull'orologio come il pomodoro: il tempo va
    avanti anche a barra o app chiusa
  - **Timer** (`TimerWidget`, `CountdownTimer`, `TimerView`): tre
    **rotelle** che girano senza fine come su Samsung (ore 0-99, minuti e
    secondi 0-59), ognuna si ferma su un numero intero e il tempo si salva
    subito; **Start**. *(I tempi pronti 00:10:00, 00:15:00 e 00:30:00
    della seconda foto c'erano in questa versione e sono stati tolti
    subito dopo, su richiesta dell'utente: vedi v0.52.1)*
    Partito: il quadrante con le **tacche arancio per il tempo che resta**,
    dentro il tempo impostato ("10 m"), il conto alla rovescia ("9:55") e
    la **campanella con l'ora in cui finisce**; sotto **Pause** / **Resume**.
    In alto a destra **↻** (torna alle rotelle) e **+** (un altro timer, che
    corre insieme agli altri): la pagina nuova si apre da sola con
    **Cancel / Start**, come l'"Add timer" della terza foto, e i timer si
    **sfogliano in orizzontale** coi pallini sotto. Finito, **suona** (il
    suono delle notifiche dell'app, una volta sola, anche a barra chiusa)
    e mostra **0:00 in rosso** con **Reset**
  - Verificato sul telefono: cronometro Start → tre Lap → Stop (00:10.28,
    giri 01 bianco, 02 rosso, 03 arancio), Resume da 10.28 a 11.83, Reset;
    timer con le rotelle portate a 00:07:02 e poi a 00:00:05 trascinando,
    Start → 0:03 con le tacche e la fine alle 08:57, a zero il suono
    (08:57:05-08:57:08) e 0:00 rosso; "+" → secondo timer con Cancel/Start
    e i pallini, partito, **lo scorrimento orizzontale sfoglia i timer
    senza chiudere la barra**, ↻ e Cancel lo tolgono. Test nuovi in
    `WidgetsTest` (cronometro, giri e colori coi tempi delle foto, stati
    del timer, rotelle, testi): 80 test passati

### ✏️ v0.51.1 — 25/09/2026 · 08:34 — Fusi orari: l'ora a destra di ogni fuso nella finestra di scelta
*(Claude Code sul PC; verificato sul telefono lo stesso giorno)*

- **[Modifica]** **[📱 Mobile]** **L'ora nella scelta del fuso di "Add to the
  list".** Nell'elenco del widget l'ora c'era già, mancava
  nella finestra di scelta. Ora lì, a destra di ogni fuso (sotto il nome
  resta "Regione · GMT±x"), c'è **l'ora che è in quel posto adesso**, che
  avanza mentre la finestra resta aperta. Ora c'è in tutte e tre le
  strade che aprono la finestra: "Add to the list", "Add clock" e "Change
  time zone" di un orologio (`ZonePickerSheet`). Verificato: Midway 19:33
  → 19:34 allo scatto del minuto, Marquesas (GMT-9:30) 21:03 → 21:04; in
  "Add clock" 19:36 alle 08:36 italiane

### 🔧 v0.51.0.1 — 25/09/2026 · 08:29 — Pomodoro: la cifra cambia a ogni secondo da quando si preme ▶
*(Claude Code sul PC; verificato sul telefono lo stesso giorno)*

- **[Bug fix]** **[📱 Mobile]** **…e partiva con un secondo di ritardo.** Segnalato
  dall'utente dopo la correzione del secondo in più (v0.51): il pomodoro partiva un secondo
  dopo il tocco su play. Il widget si
  ridisegnava allo scoccare dei secondi **dell'orologio**, e il timer
  parte a metà di un secondo qualunque: il primo "00:59" arrivava fino a
  quasi due secondi dopo ▶. Ora il pomodoro ha un'ora sua, rinfrescata
  **quando cambia la cifra del timer** (`rememberTimerNow`,
  `msUntilTimerTicks`); gli orologi restano sui secondi dell'orologio.
  Test nuovo `WidgetsTest.theTimerRedrawsExactlyWhenItsDigitChanges`.
  Verificato con screenshot a tempo dal telefono: "01:00" fino a 0,76 s
  dopo il tocco, "00:59" a 1,13 s, "00:58" a 2,16 s, "00:57" a 3,06 s

### 🆕 v0.51 — 25/09/2026 · 08:25 — Le prove dell'utente: "Reposition cover" nelle schede, il suono del pomodoro, SimSun al posto di MS YaHei
*(Claude Code sul PC; tutto verificato sul telefono lo stesso giorno;
schema alla versione **30**)*

Alle tre domande fatte subito dopo ha risposto: inquadratura **solo
della scheda** (non la copertina della pagina); suono **solo del
pomodoro**; MS YaHei da togliere, con SimSun al suo posto. E durante la
prova, la suoneria che suonava all'infinito.

- **[Nuova funzionalità]** **[📱 Mobile]** **"Reposition cover" nelle schede della
  galleria.** Tenendo premuta una scheda che mostra la copertina, nelle
  azioni c'è "Reposition cover": la copertina **dentro la scheda** si
  trascina e si pizzica (fino a 4×, come nella pagina), mai oltre i bordi;
  ✓ salva, ✕ lascia com'era. Mentre si sposta la scheda ha un bordo e non
  si apre toccandola. L'inquadratura è **solo della scheda**: la copertina
  in cima alla pagina resta com'è, e viceversa. Finché non se ne sceglie
  una, la scheda usa quella della pagina come prima; una copertina nuova
  le azzera tutte e due. La voce c'è solo in galleria con "Page cover",
  una copertina da spostare e il contenuto non bloccato
  (`GalleryCard`, `CardCoverMoveControls`, `DatabaseViewModel.setRowCardCover`;
  colonne `cardCoverScale`, `cardCoverOffsetX`, `cardCoverOffsetY` in
  `pages`). Verificato: trascinata la copertina di "Gioco uno", salvata
  nella sola scheda (`cardCoverOffsetY` 0,085, la pagina a 0)
- **[Nuova funzionalità]** **[📱 Mobile]** **Il suono del pomodoro fra le suonerie del
  telefono.** Nell'ingranaggio c'è "Sound": apre l'elenco di sistema dei
  suoni (con l'anteprima), si sceglie, e con "Fatto" vale **per quel
  pomodoro**; "Use default sound" torna al suono delle notifiche dell'app,
  che resta quello di partenza (`PomodoroWidget.soundUri`, `soundName`).
  Sul S25 Ultra il telefono chiede con quale app sceglierlo (SecSoundPicker
  o Zedge, se installato). Verificato con "Asteroid"
- **[Bug fix]** **[📱 Mobile]** **Un suono che non finiva più.** Le suonerie del telefono
  (le "Galaxy" di Samsung) sono fatte per ripetersi finché non si
  risponde — lo chiede il file stesso, con l'etichetta `ANDROID_LOOP` — e
  suonate come avviso andavano avanti all'infinito (fermate chiudendo
  l'app). Ora ogni suono dell'app (fine fase del pomodoro, e "Play" nelle
  impostazioni del suono) suona **una volta sola** (`isLooping = false`,
  da Android 9) e comunque **si ferma dopo 8 secondi** (`WidgetStore.playOnce`).
  Verificato: partito alle 08:22:58, fermato e rilasciato alle 08:23:06
- **[Bug fix]** **[📱 Mobile]** **Il pomodoro appena avviato mostrava un secondo in
  più** ("01:01" per un attimo, con un minuto). L'ora che la barra
  laterale passa al widget si rinfresca allo scoccare di ogni secondo, e
  subito dopo ▶ era indietro di quasi un secondo rispetto a quando il
  timer era partito. Ora il tempo mostrato non supera mai quello di
  partenza (`PomodoroWidget.remainingAt`). Test nuovo
  `WidgetsTest.aJustStartedPomodoroNeverShowsMoreThanItStartedWith`.
  Verificato: subito dopo ▶ "01:00", e un minuto dopo la pausa in verde
- **[Rimosso]** **[📱 Mobile]** **MS YaHei.** Non era rotto: il suo sosia libero, Noto
  Sans SC, è **lo stesso disegno** che Android usa già per il cinese, e
  sul telefono non si distingueva dal font di sistema. Tolto su richiesta
  dell'utente; le pagine che lo usavano tornano al font di sistema
  (migrazione 29→30)
- **[Nuova funzionalità]** **[📱 Mobile]** **SimSun** al posto di MS YaHei, nel gruppo
  cinese: un Song dai tratti sottili, reso con **Noto Serif SC nel peso
  Light** — lo stesso font del Songti ma più leggero, che a SimSun somiglia
  e dal Songti si distingue. Si scarica come gli altri cinesi. Verificato
  su "我们都是意大利人": Default, Songti e SimSun sono tre disegni diversi
- **[Progetto]** Migrazione **29→30** (`MIGRATION_29_30`): le pagine con
  `pageFont = 'MS_YAHEI'` tornano a NULL — deve succedere prima di
  qualunque lettura, perché il nome non esiste più nell'enum — e tre
  colonne vuote per l'inquadratura delle schede. `MigrationTest` ora prova
  sia dalla 28 sia dalla 29 (schema in `test/resources/schema/v29.sql`,
  generato dal codice di Room al commit `b53a7bf`, con una pagina in MS
  YaHei e una in Songti); `AppDatabase.resetForTests` perché i due test
  non si pestino i piedi. Verificato sul telefono: `user_version` 30,
  colonne in fondo al `CREATE TABLE`, nessun crash
- **[Progetto]** Voce 14 confermata dall'utente: le schede piccole, medie
  e grandi come su Notion, **medie** di partenza — è già così

### 🔧 v0.50.0.1 — 24/09/2026 · 23:17 — Ripresa sul PC del 24/09/2026 — il lavoro cloud provato sul telefono
*(Claude Code sul PC, S25 Ultra in debug wireless, build
firmata con la chiave di debug del PC; esito voce per voce in "Diario
delle sessioni cloud" → "Da verificare sul telefono")*
- **[Progetto]** Clonato il ramo `claude/funny-ramanujan-uuwahs` (la
  pull request #1 non era ancora unita) in `C:\Users\Utente\MyNotesApp`.
  Sul PC mancava Git: installato Git for Windows 2.55 con `winget`. Il
  progetto non ha `gradlew`: compilato col Gradle 8.7 già in cache
  (`%USERPROFILE%\.gradle\wrapper\dists\gradle-8.7-bin`) e la JDK 21
  (`%USERPROFILE%\.jdks\jdk-21.0.12.1+1`), con un `local.properties`
  (ignorato da Git) che dice dov'è l'SDK. `assembleDebug` riuscito, solo
  avvisi; `testDebugUnitTest` 70 su 70. **Android Studio non è stato
  aperto**: la sincronizzazione lì resta da vedere
- **[Progetto]** **Le migrazioni 24→29 provate su un telefono vero.**
  notE non era installata (nessun database da salvare), quindi: build di
  `4a35c64` (schema 24) installata con la chiave del PC, note di prova
  scritte a mano (H1, H2, H3, paragrafo, elenco, toggle con una riga
  dentro, database con una riga e del testo nella sua pagina), database
  copiato, poi build nuova installata **sopra** (`adb install -r`, nessuna
  disinstallazione chiesta). Esito: `user_version` 29; le sette colonne
  nuove in fondo al `CREATE TABLE` di `pages`, senza apici inversi (la
  tabella non è stata rifatta); i tre titoli diventati `PARAGRAPH` col
  testo intatto; tutto il resto identico; `integrity_check` e
  `foreign_key_check` puliti; nessun errore in `logcat`
- **[Bug fix]** **[📱 Mobile]** **Barra Aa: la voce della dimensione era vuota** — solo
  la freccetta, niente "16" (lavoro cloud). `BarValueChip` accorcia il
  testo coi puntini con `weight(1f, fill = false)`, che vuole una
  larghezza massima: la barra scorre in orizzontale, e senza limite
  Compose dava al testo zero pixel. La voce del font l'aveva
  (`FONT_CHIP_MAX_WIDTH`), quella della dimensione no: ora ha
  `SIZE_CHIP_MAX_WIDTH` (64 dp). Verificato: "16", e 32, 72, 5
- **[Bug fix]** **[📱 Mobile]** **Annulla faceva chiudere l'app** in qualunque pagina con
  un toggle non vuoto che non fosse il primo blocco
  (`SQLiteConstraintException: FOREIGN KEY constraint failed` in
  `replaceAllBlocks`). **C'era già prima del cloud.** La foto di Annulla
  arriva ordinata solo per `orderIndex`, che per i figli riparte da 0: il
  figlio veniva reinserito prima del suo toggle, e la chiave esterna verso
  il genitore lo rifiutava. I dati non si perdevano (tutto in una
  transazione), ma l'app si chiudeva. Ora si reinserisce con
  `inDocumentOrder`, genitori prima dei figli. Test nuovo
  `PageRepositoryTest.undoSnapshotPutsBackAToggleWhoseChildComesFirstInOrderIndex`:
  senza la correzione fallisce con lo stesso errore del telefono (787),
  con la correzione passa. Verificato sul telefono
- **[Bug fix]** **[📱 Mobile]** **Dopo "Duplicate" dal menu del blocco il testo restava
  indietro, e la lettera successiva cancellava la copia.** Il campo
  unito mostrava le righe di prima (una sola copia) coi segni degli
  elenchi scalati di una riga; scrivendo una lettera, `updateRun`
  confrontava sette righe con otto blocchi e **cancellava il blocco della
  copia** (visto nel database). Causa: in `MergedTextRunField` il ramo
  normale di `onValueChange` impostava `pendingText` anche quando si
  spostava solo il cursore o si selezionava; senza nessun salvataggio in
  viaggio `pendingText` non si azzerava più, e il campo scambiava per
  "database indietro" ogni cambiamento arrivato da fuori. Prima del cloud
  gli unici erano Annulla e Ripristina, che lo azzerano; il menu del
  blocco è nuovo. Ora `pendingText` si imposta solo se il testo è
  cambiato davvero. Verificato: stessa sequenza, due righe subito, segni
  giusti, la lettera va nel blocco giusto e la copia resta
- **[Bug fix]** **[📱 Mobile]** *(non corretto, c'era già in `4a35c64`)* **La riga fantasma**:
  trasformando dal menu "/" l'**ultima** riga di un gruppo di testo in
  un'isola (toggle, database, pagina, tabella), nel campo resta una riga
  in più col testo che aveva (vuota se era vuota). `applySlashEntry`
  imposta `pendingText` legittimamente (ha appena tolto "/comando"), poi
  la riga esce dal gruppo; Room consegna solo lo stato finale, diverso da
  `pendingText`, e il campo lo scambia per "database indietro". Non perde
  niente: scrivendoci nasce un blocco nuovo, e riaprendo la pagina
  sparisce. Da correggere con calma: è il punto più delicato dell'app
- **[Bug fix]** **[📱 Mobile]** *(non corretto, c'era già)* Creando un database o una pagina
  dal menu "/", il testo del comando ("/table", "/gallery") **resta
  salvato** nel blocco del collegamento: `convertToDatabaseLink(block)`
  salva la copia del blocco presa prima che il comando fosse tolto. Non
  si vede (il collegamento mostra il nome della pagina)
- **[Bug fix]** **[📱 Mobile]** *(non corretto, lavoro cloud, solo aspetto)* Sotto
  l'orologio del widget "Fusi orari" si legge "Thu 24 Sep • GMT+02:00"
  invece della sigla "CEST" che la voce prometteva: su questo telefono il
  nome breve del fuso, con la lingua dell'app in inglese, esce come
  scostamento
- **[Progetto]** Come si è provato: `adb` e `uiautomator dump` per
  toccare gli elementi per nome, uno screenshot fra un passo e l'altro, e
  il database riletto dal PC dopo ogni operazione che scrive (`.db` e
  `-wal`, senza `-shm`). Due trappole viste: il **debug wireless si
  spegne da solo** ogni 10-15 minuti (meglio il cavo); e **uno
  scorrimento fatto con la tastiera aperta parte sulla tastiera
  Samsung** e scrive caratteri — un "⁵" finito in un paragrafo di prova,
  tolto a mano. Il tasto Indietro con la tastiera già chiusa esce
  dall'app

### 🆕 v0.50 — 24/09/2026 · 15:52 — Pedice e apice nella barra Aa
*(verificato in parte sul telefono
il 24/09/2026: voce 30)* *(sessione cloud del 24/09/2026,
richiesta 39: compilato, 70 test passati — 7 nuovi —, nessuna
migrazione, **non provato sul telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** Nella barra **Aa**, subito dopo S, due
  pulsanti: **x₂ pedice** (una x col 2 piccolo in basso a destra) e
  **x² apice** (il 2 in alto a destra), come chiesto. Come B, I, U, S
  agiscono sul testo selezionato, anche a cavallo di più righe, e un
  secondo tocco toglie. **Si escludono**: dare l'apice a un pedice lo
  trasforma, come in Word (`withSubscript`, `withSuperscript`)
- **[Nuova funzionalità]** **[📱 Mobile]** Il testo in pedice o apice è **a sette decimi
  del corpo** della pagina (cresce con lui) e sta sotto o sopra la riga;
  le altre formattazioni — grassetto, colore, spoiler — restano. È solo
  aspetto: le lettere sono dove erano, quindi cursore, selezione,
  ricerca e conteggio del testo non cambiano (`scriptStyleOf`, usato sia
  dal testo unito sia dalle isole)
- **[Progetto]** Nel testo salvato sono due chiavi nuove,
  `subscript` e `superscript`, scritte solo quando sono accese: il testo
  di prima si legge com'era e, senza pedici, si salva identico. Niente
  migrazione (è JSON dentro una colonna che c'era già)
- **[Bug fix]** **[📱 Mobile]** *(trovati col disegno di prova, prima del telefono)*
  Nei pulsanti, il 2 dell'apice spingeva giù la x, che non stava più
  all'altezza di B, U, S; poi, messo il 2 in un angolo, restava a metà
  in tutti e due i pulsanti perché si portava dietro l'altezza di riga
  del testo normale. E nel testo il pedice, con lo spostamento standard,
  scendeva sotto le gambe di g e p: ora scende meno dell'apice
- **[Progetto]** Test: `RichTextScriptTest` (6: solo sulla selezione, uno
  sostituisce l'altro, secondo tocco toglie, le altre formattazioni
  restano, si continua a scrivere in apice, il testo salvato prima si
  legge e si risalva uguale) e `ScriptRenderTest` (i pulsanti e un testo
  con H₂O e mc², in `app/build/widget-screenshots/scripts.png`). Testi
  nuovi nelle otto lingue (`EditorStrings.subscript`, `superscript`, che
  chi usa il lettore di schermo sente sui due pulsanti)

### 🆕 v0.49 — 24/09/2026 · 15:21 — Widget nella barra laterale — fusi orari, avanzamento, contatore, pomodoro
*(verificato in parte sul telefono il 24/09/2026: voce 29)*
*(sessione cloud del 24/09/2026, richiesta 37: compilato, 63
test passati — 12 nuovi —, nessuna migrazione, **non provato sul
telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** **La sezione "Widget" in cima alla barra
  laterale non è più uno spazio vuoto** (`WIDGETS_SPACE`, tolto): i widget
  si aggiungono da **"+ Aggiungi widget"** in fondo alla sezione, arrivano
  in fondo, ognuno nel suo riquadro arrotondato col nome e i **tre
  puntini** (Sposta su, Sposta giù, Rimuovi con la conferma; sul
  contatore anche Rinomina). Di ognuno se ne possono mettere quanti se ne
  vuole (`SidebarWidgets.kt`, `WidgetsSection`)
- **[Nuova funzionalità]** **[📱 Mobile]** **L'interruttore**: una freccetta accanto al
  titolo "Widget", su per chiudere e giù per riaprire, **nasconde tutta
  la sezione** e lascia solo il titolo. La scelta **resta** chiudendo la
  barra e l'app, come chiesto
- **[Nuova funzionalità]** **[📱 Mobile]** **Fusi orari**: **orologi** due per riga, col
  nome della città sopra; l'analogico come nella foto dell'utente —
  tacche, lancette di ore, minuti e secondi, e dentro l'ora scritta
  ("16:05:09") col giorno nella lingua dell'app e la sigla del fuso
  ("gio 24 set • CEST"); il digitale con l'ora grande. **Tenendo premuto
  un orologio**: Analogico / Digitale, Cambia fuso orario, Rimuovi. Il
  primo orologio nasce sul fuso dell'app (Settings)
- **[Nuova funzionalità]** **[📱 Mobile]** **L'elenco dei fusi**, sotto gli orologi:
  città, ora, sole o luna (giorno dalle 6 alle 18). **Sempre da ovest a
  est**, come chiesto: si ordina a ogni secondo sulla distanza da
  Greenwich **di adesso**, perché l'ora legale sposta i fusi (New York e
  Londra non sono sempre a cinque ore); a parità, per nome
  (`sortedWestToEast`). Tenendo premuta una riga, la si toglie
- **[Nuova funzionalità]** **[📱 Mobile]** **La scelta del fuso**: tutti i fusi con un
  nome di città (fuori i doppioni vecchi come "US/Pacific" o
  "Etc/GMT+5"), anche loro da ovest a est, con "Regione · GMT+5:30"
  sotto, e la ricerca per città, regione o "GMT+2" (`ZonePickerSheet`)
- **[Nuova funzionalità]** **[📱 Mobile]** **Avanzamento** (la "Life Progress Bar"):
  **Anno, Mese, Settimana, Giorno** già dentro, e **"Aggiungi una data"**
  per le proprie — nome, Da, A, con il calendario dei database; A prima
  di Da non si accetta. Barra con la cornice, pieno quanto è passato,
  "Anno: 73%" accanto nello stesso colore; **tenendo premuta una barra**
  si cambia **il colore** (la tavolozza del testo, senza il selettore
  testo/sfondo, che qui non vuol dire niente: `ColorPickerSheet` ha ora
  `showTargetSwitch`) e, per le date proprie, Modifica ed Elimina. Le
  barre partono **verdi** come nella foto. La settimana comincia il
  lunedì; una data si conta dalla mezzanotte del primo giorno alla
  mezzanotte dell'ultimo, e da lì è al 100%; tutto nel fuso dell'app
  (`fractionAt`)
- **[Nuova funzionalità]** **[📱 Mobile]** **Contatore**: − e + quadrati ai lati del
  numero, sotto la freccetta che gira per azzerare, come nella foto. Un
  nome facoltativo sopra al numero (Rinomina, dai tre puntini)
- **[Nuova funzionalità]** **[📱 Mobile]** **Pomodoro**: il riquadro arrotondato col
  **tempo grande** ("25:00"; toccandolo lo si scrive a mano, minuti e
  secondi), sotto **il nome della sessione** (toccandolo lo si cambia; in
  pausa c'è scritto "Pausa") e tre pulsanti tondi: **▶** (che diventa ⏸),
  **da capo** (fermo, sulla sessione, alla durata piena), e
  **l'ingranaggio** con **Durata della sessione** e **Durata della
  pausa**, − e + da 1 a 180 minuti. Il colore dice la fase: arancio la
  sessione, verde la pausa. Finita una fase **suona** e comincia l'altra
  da sola, come i timer "25 + 5" della foto
- **[Progetto]** Com'è fatto il pomodoro: **si conta sull'orologio, non a
  colpi di secondo**. Quando corre si salva *quando* finirà la fase
  (`endsAt`), e il tempo che manca si ricava da lì; così resta giusto con
  la barra chiusa, con l'app in secondo piano o chiusa e riaperta.
  Un'attesa di `WidgetStore`, che vive quanto l'app e non quanto la
  barra, dorme fino alla fine della fase e allora fa avanzare e suonare.
  Riaprendo l'app dopo ore, le fasi passate si recuperano tutte, contando
  dal momento esatto in cui ognuna è finita, e **senza suonare** per il
  passato (`advancedTo`). Il suono è quello scelto per le notifiche nelle
  impostazioni (o quello del telefono), e tace se le notifiche sono messe
  a tacere
- **[Progetto]** Dove stanno: **nelle preferenze del telefono**, in un
  file a parte (`widgets`), in JSON, e non nel database delle note: sono
  attrezzi, non contenuto — nel database finirebbero dentro ogni backup e
  ogni copia di pagina. Quindi **nessuna migrazione**, e un widget rotto
  non può toccare le note. `WidgetStore` è osservabile come
  `AppSettings`: la barra si ridisegna da sola a ogni modifica. Orologi e
  barre si aggiornano ogni secondo **solo a barra aperta** (chiusa, resta
  disegnata dietro alla pagina, e ridisegnarla per niente consumerebbe
  batteria)
- **[Progetto]** **Il disegno dei widget si guarda anche in cloud**:
  `WidgetsRenderTest` disegna la sezione (tutti e quattro i widget, e la
  sezione chiusa) con Robolectric e la salva come immagine in
  `app/build/widget-screenshots/`. Servono, solo per i test, la libreria
  `ui-test-junit4` di Compose e `unitTests.isIncludeAndroidResources`
  in `app/build.gradle.kts`. `captureToImage` di Compose sotto
  Robolectric non finisce mai: si disegna a mano la finestra su una
  bitmap
- **[Bug fix]** **[📱 Mobile]** *(trovato con quel disegno, prima di arrivare al
  telefono)* **Il contenuto di ogni widget era disegnato tutto nello
  stesso punto**, righe una sopra l'altra: stava in un `Box` invece che
  in una `Column`. E **le barre dell'avanzamento erano lunghe ognuna a
  modo suo**, perché l'etichetta accanto ("Anno", "Settimana") si
  allargava e si stringeva; ora ha una larghezza fissa
- **[Progetto]** Test: `WidgetsTest` (11: anno, mese, settimana dal
  lunedì, giorno, date proprie, ovest-est anche con l'ora legale, nomi e
  "GMT+5:30", il pomodoro che avanza, recupera più fasi, resta fermo in
  pausa, e il salvataggio in JSON andata e ritorno) e `WidgetsRenderTest`
  (1). Testi nuovi nelle otto lingue (`WidgetStrings`).
  `DayPickerDialog` e i suoi due aiuti in `DatabaseViewScreen.kt` sono
  ora `internal`, per usarli anche qui
- **[Progetto]** README: corrette due affermazioni rimaste indietro nei
  Limiti noti — "la ricerca nei database non esiste" (c'è dalla
  richiesta 35) e "Linked view of data source" fra le voci grigie dei
  menu "+" e "/" (costruita alla richiesta 36). Barrate, non cancellate

### 🆕 v0.48 — 24/09/2026 · 14:56 — Viste collegate — lo stesso database in un'altra pagina
*(verificato in parte sul telefono il 24/09/2026: voce 28)* *(sessione
cloud del 24/09/2026, richiesta 36: compilato, 51 test passati — 9
nuovi, compresa la migrazione 28→29 aperta da Room —, **schema alla
versione 29**, **non provato sul telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** **"Linked view of data source"** nei menu "/"
  e "+" (finora grigia) e in "Turn into": si sceglie un database da un
  elenco con la ricerca in cima — tutti i database dell'app, ognuno con
  sotto la pagina in cui sta, come "Existing data sources" di Notion
  (`LinkedSourcePickerSheet`) — e nella pagina compare **lo stesso
  database**: stesse righe, proprietà, pagine e quello che c'è scritto
  dentro. Non è una copia: scrivere da una parte è scrivere dall'altra
- **[Nuova funzionalità]** **[📱 Mobile]** **La vista nasce identica** all'originale —
  vista, filtro, ordinamento, raggruppamento, galleria, calendario,
  proprietà nascoste — e da lì ha **impostazioni sue**: nella pagina
  "PS5" si filtra Piattaforma = PS5 e nel database principale non cambia
  niente. Anche **le proprietà nascoste** sono della vista
  (`PageEntity.viewHiddenColumnIds`)
- **[Nuova funzionalità]** **[📱 Mobile]** **Il titolo è "↗ Nome del database"**, quello
  vivo dell'origine, e **toccandolo si apre il database principale** a
  schermo intero, come chiesto (`LinkedViewTitle`). Icona, nome, blocco
  del contenuto e "database semplice" sono quelli dell'origine
- **[Nuova funzionalità]** **[📱 Mobile]** **Una riga nuova nasce col valore del filtro**,
  come su Notion — nella vista PS5, "Nuova pagina" crea un gioco PS5 nel
  database principale. Vale per ogni database filtrato, non solo per le
  viste: prima una riga creata con un filtro attivo spariva appena nata.
  Per le selezioni (il primo valore scelto) e per le caselle filtrate
  sulle spuntate (`fillFilterValue`)
- **[Nuova funzionalità]** **[📱 Mobile]** Menu del blocco su una vista: le voci del
  database (semplice / complesso, Lock database, Rename, Edit icon)
  agiscono **sull'origine**, quelle della pagina (Duplicate, Move to,
  Move to trash, preferiti) **sulla vista**; niente "Turn into page".
  **Togliere la vista** (Delete nelle sue impostazioni) lo dice con una
  finestra sua: se ne va solo la vista, il database resta
- **[Nuova funzionalità]** **[📱 Mobile]** Se il database di origine è cancellato o nel
  cestino, al posto della vista una riga lo dice, coi sei puntini per
  toglierla (`LinkedSourceMissing`); ripristinandolo, la vista torna
- **[Nuova funzionalità]** **[📱 Mobile]** Le viste **non compaiono nella barra laterale
  né nella ricerca generale**: sono un altro modo di guardare un database
  che lì c'è già, e lo avrebbero ripetuto. Una pagina con solo viste non
  ha il triangolino dei figli (`BlockDao.countLinksInPage`)
- **[Progetto]** Com'è fatta. Una vista collegata è una pagina-database
  con `sourceDatabaseId` (l'origine) e **nessun dato suo**: il ViewModel
  del database tiene separate **la pagina della vista** (`page`, le
  impostazioni) e **quella dei dati** (`dataPage`, l'origine), e legge
  righe, colonne, celle, icone, copertine e testo per la ricerca dalla
  seconda. Per un database vero sono la stessa pagina, e non cambia
  niente. **Migrazione 28→29**: due colonne `TEXT` in `pages`, vuote per
  tutto quello che c'era
- **[Progetto]** **Attenzione trovata scrivendolo**: nello stato della
  vista le colonne hanno il "nascosto" della vista; ogni funzione che
  salva una colonna partendo da lì — rinominarla, cambiarle le opzioni,
  spostarla, centrarla — avrebbe scritto quel "nascosto" **dentro il
  database di origine**, dove l'avrebbero visto tutti. Ora si parte
  sempre dalle colonne vere (`rawColumns`); controprova fatta: tolta la
  protezione, il test lo coglie
- **[Bug fix]** **[📱 Mobile]** *(trovato scrivendo questo)* **Una riga nuova in un
  database filtrato poteva prendere il numero d'ordine di una riga
  nascosta**: il numero si calcolava sulle righe visibili, già filtrate
  (o, da oggi, cercate). Due righe con lo stesso numero non hanno un
  ordine sicuro fra loro. Ora si conta su tutte (`lastRowOrderIndex`)
- **[Progetto]** Test: `MigrationTest` (un file alla versione 28 con lo
  schema di allora, aperto da `AppDatabase.getInstance` come fa l'app:
  Room esegue la migrazione e controlla lo schema; controprova fatta —
  senza la migrazione l'apertura fallisce), `LinkedViewModelTest` (4:
  righe dell'origine col filtro della vista, proprietà nascosta solo lì,
  riga nuova nell'origine col valore del filtro, origine nel cestino), e
  4 prove nuove in `PageRepositoryTest`. Testi nuovi nelle otto lingue
  (`DbStrings.linkedViewTitle`, `linkedViewHint`, `openSourceDatabase`,
  `linkedSourceMissing`, `noDatabasesToLink`, `removeLinkedViewTitle`,
  `removeLinkedViewText`)

### 🆕 v0.47 — 24/09/2026 · 14:35 — Ricerca dentro un database — la lente accanto a Sort
*(verificato in parte sul telefono il 24/09/2026: voce 27)* *(sessione
cloud del 24/09/2026, richiesta 35: compilato, 42 test passati — 10
nuovi —, nessuna migrazione, **non provato sul telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** Nella riga degli strumenti di ogni database,
  **subito dopo Sort**, la **lente**. Toccandola, lì dov'era la riga
  compare **la barra della ricerca**, col cursore già dentro: si scrive
  subito, come su Notion. La **X** la chiude, cancella la parola e
  rimette la riga degli strumenti (`DatabaseSearchBar`). Si può cercare
  anche con "Lock view": cercare non cambia com'è fatto il database
- **[Nuova funzionalità]** **[📱 Mobile]** **Cosa si cerca**, a ogni lettera, senza
  distinguere maiuscole e minuscole: il **nome** delle righe; il valore
  delle **proprietà** che si leggono come sono scritte — testo, numero,
  selezione, selezione multipla (valore per valore), URL, email,
  telefono; e **tutto quello che c'è dentro la pagina** della riga — il
  testo dei blocchi (anche nei toggle chiusi), le celle delle tabelle, i
  nomi delle sottopagine. Date, caselle e date automatiche no: il loro
  valore salvato ("2026-09-24", "true") non è quello che si legge, e
  "true" troverebbe ogni casella spuntata
- **[Nuova funzionalità]** **[📱 Mobile]** **Cosa si illumina**, come chiesto: se la
  parola è **nel nome**, si illuminano di giallo **le sue lettere**;
  se è **dentro** (una proprietà, la pagina), si illumina di un giallo
  più leggero **tutta la riga** — o la scheda, o la barra. Giallo e non
  l'azzurro dell'app, che vuol dire "selezionato". In **tutte e sei le
  viste**: tabella, bacheca, elenco, galleria, calendario, linea del tempo
  (`highlightedTitle`, `Modifier.foundInside`, `LocalRowSearch`)
- **[Nuova funzionalità]** **[📱 Mobile]** **Le righe senza la parola si nascondono**
  mentre si cerca, come su Notion; senza nessuna riga trovata, sotto la
  barra c'è "Nessun risultato". La ricerca è un filtro in più sopra a
  quello delle impostazioni, e sta nel ViewModel del database
  (`searchRows`, dentro lo stesso calcolo del filtro e dell'ordinamento):
  per questo vale in ogni vista senza che nessuna debba saperlo, e
  aprendo una pagina trovata e tornando indietro la ricerca è ancora lì
  (senza riaprire la tastiera)
- **[Nuova funzionalità]** **[📱 Mobile]** **Una riga creata mentre si cerca resta
  visibile** fino alla chiusura della ricerca: è vuota, non ha la
  parola, e sparendo subito avrebbe fatto sembrare rotto "Nuova pagina"
- **[Progetto]** **Veloce anche con tanto testo**: il testo delle pagine
  delle righe si legge dal database **una volta**, all'apertura della
  ricerca e al ritorno sulla schermata (tre query:
  `DatabaseDao.getRowPageBlockTexts`, `getRowPageTableTexts`,
  `getRowPageLinkTitles`; `DatabaseRepository.searchIndex`); ogni lettera
  scritta poi cerca in memoria. Il nome e le proprietà invece sono quelli
  vivi della tabella
- **[Progetto]** Test: `DatabaseSearchTest` (9 prove su `searchRows`:
  nome, proprietà, selezione multipla, pagina, date e caselle escluse,
  spazi, accenti, riga nuova) e una prova in più in
  `PageRepositoryTest` sull'indice, su un database vero (testo diviso in
  più pezzi con grassetto e spoiler, tabella, sottopagina, riga mai
  aperta). Testi nuovi nelle otto lingue (`DbStrings.searchInDatabase`,
  `closeSearch`)

### ✏️ v0.46.2 — 24/09/2026 · 14:24 — Annulla e Ripristina anche per Move to, Duplicate e Move to trash
*(verificato in parte sul telefono il 24/09/2026: voci 25 e 26)*
*(sessione cloud del 24/09/2026, richiesta 34: compilato, 32 test passati
— 16 nuovi, sul database vero —, nessuna migrazione, **non provato sul
telefono**)*
- **[Modifica]** **[📱 Mobile]** **Annulla disfa anche "Move to", "Duplicate"
  e "Move to trash", e Ripristina li rifà.** Prima Annulla conosceva solo
  le foto dei blocchi della pagina aperta, e queste tre toccano altre
  pagine:
  - **Move to** → la pagina torna **esattamente dov'era**: stessa pagina,
    stesso punto fra le righe, dentro lo stesso toggle. Un database
    torna **aperto dentro la pagina**, se lo era. La pagina di una riga
    torna nella sua riga **con i valori delle proprietà**, che lo
    spostamento aveva tolto
  - **Duplicate** → la copia va **nel Cestino**, come su Notion quando si
    toglie una pagina: se nel frattempo ci si è scritto dentro, niente è
    perso. Se la copia era una riga nuova di un database, la riga si
    toglie. Ripristina la tira fuori dal Cestino e la rimette dov'era
  - **Move to trash** → la pagina esce dal Cestino e torna al suo posto
    (fatto anche questo: stesso meccanismo, e la voce stava nello stesso
    elenco di Limiti noti)
- **[Modifica]** **[📱 Mobile]** **Dove si annulla.** Dal menu del blocco, nella
  pagina stessa. Dai tre puntini la schermata se ne va — si torna
  indietro, o si entra nella copia — e l'Annulla passa alla **prossima
  pagina che compare** (`PendingPageUndo`, preso in
  `adoptPendingPageChange` quando la pagina si apre o ci si torna):
  tornati indietro dopo un Move to, lì Annulla rimette la pagina dov'era;
  dentro la copia dopo un Duplicate, Annulla la manda nel Cestino e
  **torna indietro da solo** (restare davanti a una pagina nel Cestino
  non avrebbe senso)
- **[Progetto]** Com'è fatto. La cronologia della pagina ha **due tipi di
  passo**: le foto dei blocchi, come prima (`BlocksStep`), e i
  cambiamenti alle pagine (`PageStep`), che si portano dietro quello che
  serve a tornare indietro — i collegamenti com'erano, la riga e le sue
  celle (`PageRepository.PageChange`: `Moved`, `Duplicated`, `Trashed`;
  `undoPageChange`, `redoPageChange`). I collegamenti tornano al loro
  posto facendo scalare di uno quelli che seguono; se il toggle in cui
  stavano non c'è più vanno in fondo alla pagina, e se la pagina non si
  potesse rimettere da nessuna parte va in fondo al menu principale
  invece di restare irraggiungibile (`putLinksBack`). Annulla e
  Ripristina ora si eseguono **uno alla volta** (`historyLock`): un passo
  di pagina lavora sul database per un attimo, e due tocchi veloci non
  devono sovrapporsi. Se la pagina nel frattempo è stata cancellata per
  sempre (Cestino svuotato), il passo si salta
- **[Progetto]** **Test su un database vero, senza telefono.** Aggiunti
  Robolectric 4.13 e `androidx.test:core-ktx` **solo per i test**
  (l'APK non cambia) e `PageRepositoryTest`: 16 prove che girano su Room
  e SQLite veri, in memoria, con le chiavi esterne accese come nell'app —
  spostare (in fondo; nella stessa pagina; un database arriva come
  collegamento; dentro una propria sottopagina rifiutato; la pagina di
  una riga esce e con Annulla torna con le sue proprietà; dentro una riga
  mai aperta), duplicare e buttare con Annulla e Ripristina (anche la
  pagina di una riga, e una pagina cancellata per sempre nel frattempo),
  le foto di Annulla (tabelle, collegamento a un database cancellato, a
  una pagina spostata), "Duplicate" di un toggle con sottopagina, il
  toggle che lascia uscire le sue righe, "Turn into simple database"
- **[Progetto]** **Controprova sui due difetti "trovati leggendo il
  codice"**: tolta per un momento ciascuna correzione, il suo test
  fallisce. Senza `withoutStaleLinks` Annulla dopo aver eliminato un
  database finisce in `SQLiteConstraintException: FOREIGN KEY constraint
  failed (code 787)` — è la chiusura dell'app; senza rimettere le celle,
  la tabella resta vuota. Non erano ipotesi. Le correzioni sono al loro
  posto, e i test passano

### ✏️ v0.46.1 — 24/09/2026 · 14:04 — Move to — l'albero con la ricerca
*(verificato in parte sul
telefono il 24/09/2026: voce 24)* *(sessione cloud del 24/09/2026,
richiesta 33: compilato, test passati, nessuna migrazione, **non
provato sul telefono**)*
- **[Modifica]** **[📱 Mobile]** **"Move to" apre l'albero della barra
  laterale con la ricerca in cima** — lo stesso di "Duplicate → Dentro
  una pagina" (`DestinationPickerSheet`, ora condiviso, e
  `MovePagePicker`) — dai tre puntini di una pagina, dai tre puntini di
  un database a schermo intero e dal menu del blocco. Prima era un
  elenco piatto, in ordine alfabetico, delle sole pagine di testo
- **[Modifica]** **[📱 Mobile]** Si può scegliere **anche la pagina di un
  database**: i database si aprono come cartelle, e si sceglie una delle
  loro righe — anche una mai aperta, la cui pagina nasce in quel
  momento. Il database stesso no: il suo contenuto sono righe, non
  blocchi. Prima le pagine delle righe erano escluse
- **[Modifica]** **[📱 Mobile]** **Sempre in fondo** alla pagina scelta, come
  chiesto: anche scegliendo la pagina in cui sta già (prima lì non
  succedeva niente). Un avviso dice "Spostata in …"; dal menu del blocco
  si resta nella pagina, dai tre puntini si torna indietro come prima
- **[Modifica]** **[📱 Mobile]** **Un database spostato arriva sempre come
  collegamento a pagina**, anche se prima era aperto dentro la pagina:
  per riaprirlo lì c'è "Turn into database" (tenendo premuto il
  collegamento, o dai tre puntini del database). Prima si ricreava
  sempre aperto. La riga di spiegazione in cima all'albero lo dice
- **[Modifica]** **[📱 Mobile]** **"Move to" sulla pagina principale c'è ma è
  grigia** (prima non c'era proprio). L'utente ha lasciato la scelta fra
  la voce grigia e un messaggio "The main menu cannot be moved": messa
  la grigia, perché è già il modo in cui l'app dice "non qui" (Import ed
  Export nello stesso menu) e non costa un tocco a vuoto. Cambiarla col
  messaggio è una riga
- **[Bug fix]** **[📱 Mobile]** *(trovato scrivendo questo)* **Si poteva spostare una
  pagina dentro una sua sottopagina**: il vecchio elenco offriva tutte
  le pagine tranne quella stessa. Il risultato era un giro chiuso — la
  pagina dentro la sottopagina, la sottopagina dentro la pagina — e
  nessuna delle due si raggiungeva più dal menu principale. Ora la
  pagina e tutto quello che ha dentro, giù fino in fondo, si vedono
  grigie e non si aprono, e cercando non compaiono
  (`PageRepository.moveExclusions`); il repository rifiuta comunque lo
  spostamento se arriva da un'altra strada
- **[Bug fix]** **[📱 Mobile]** *(trovato scrivendo questo)* **La pagina di una riga
  spostata restava anche nel database**, cioè in due posti. Ora esce dal
  database, come su Notion: la riga si toglie con le sue proprietà, e la
  pagina diventa una pagina come le altre (`isRowPage` spento,
  `PageDao.setRowPage`). La riga di spiegazione lo dice prima di scegliere
- **[Bug fix]** **[📱 Mobile]** *(trovato scrivendo questo; **confermato dai test** della
  richiesta 34)* **Annulla dopo aver
  eliminato un database dentro la pagina faceva chiudere l'app.**
  L'eliminazione (`deleteDatabaseBlock`) scatta una foto per Annulla e
  poi cancella il database; Annulla rimetteva il blocco, che puntava a
  una pagina non più esistente, e il database lo rifiutava (chiave
  esterna) — un errore dentro una coroutine senza nessuno che lo
  raccolga. Ora, a ogni Annulla e Ripristina, i collegamenti della foto
  a pagine **cancellate, nel cestino o spostate in un'altra pagina** non
  tornano (`PageRepository.withoutStaleLinks`, dentro
  `replaceAllBlocks`). Vale per tutte le strade — tre puntini della
  sottopagina, menu del blocco, cestino — e sostituisce `forgetLinksTo`
  della voce precedente, che copriva solo il menu del blocco
- **[Rimosso]** **[📱 Mobile]** La vecchia finestra di "Move to" (`MoveToSheet`), la
  query delle sue destinazioni (`PageDao.getMoveDestinations`,
  `PageRepository.moveDestinations`), le funzioni che la caricavano nei
  due ViewModel e il testo "Non c'è un'altra pagina in cui spostarla"
  (`Strings.noOtherPage`): con l'albero c'è sempre almeno il menu
  principale
- **[Progetto]** `TreeRow` ha lo stato spento (`enabled`), grigio e senza
  triangolino. Testi nuovi nelle otto lingue (`Strings.moveHint`,
  `moveDatabaseHint`, `moveRowPageHint`, `movedInto`)

### 🆕 v0.46 — 24/09/2026 · 13:52 — Il menu del blocco — i sei puntini
*(verificato in parte sul
telefono il 24/09/2026: voci 22 e 23; Duplicate aveva un bug, corretto —
vedi "Ripresa sul PC")* *(sessione cloud del 24/09/2026,
richieste 31-32: compilato, test passati, nessuna migrazione — schema
sempre alla 28 —, **non provato sul telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** Nella barra sopra la tastiera, **prima del
  rientro a sinistra**, un'icona coi **sei puntini** (due colonne da tre,
  `Icons.Filled.DragIndicator`, la stessa di Notion): apre il **menu
  della riga in cui sta il cursore**. Il cursore si spegne prima di
  aprirlo: la tastiera sparirebbe comunque sotto la finestra, e una riga
  che "Turn into" fa sparire non deve morire col fuoco dentro — il modo
  in cui l'app si chiudeva (vedi più giù in Cronologia "La tastiera non
  sparisce più", e la causa di fondo lì sotto). La finestra
  (`BlockActionsHost`, file nuovo `BlockActionsSheet.kt`) sta a livello
  di schermata, come il pennello e la dimensione del testo, perché la
  barra sparisce con la tastiera
- **[Nuova funzionalità]** **[📱 Mobile]** **Per una riga di testo** (paragrafo, elenchi,
  casella, toggle, righe dentro un toggle): **Turn into, Color,
  Duplicate**. Edit icon, Add to favorites, Move to e Move to trash ci
  sono solo per le pagine, come chiesto (richiesta 32). **Rename**
  anche: una riga di testo non ha un nome da cambiare, il suo nome è il
  testo stesso — scelta da confermare con l'utente
  - **Turn into**: le **stesse voci dei menu "/" e "+"**, nello stesso
    ordine (`SLASH_ENTRIES`), grigie quelle non costruite, con la
    **spunta sul tipo attuale**. Rispetto al "/" due differenze, che
    vengono dal punto da cui si parte: il testo della riga **non si
    perde** — diventa il nome della pagina o del database, la prima
    cella della tabella (senza grassetti e colori: le celle sono testo
    semplice), la riga sotto il divisore — e **si resta nella pagina**
    invece di entrare in quella nuova, come su Notion. Dal "/" la riga
    è vuota e non serviva: lì non cambia niente
    (`convertToPageLink`/`convertToDatabaseLink` con `title`,
    `convertToDividerAndFocusNext` con `keepText`,
    `convertToTableKeepingText`)
  - **Color**: la finestra del pennello, ma il colore — del testo o
    dietro al testo — va su **tutta la riga**, senza doverla selezionare
    (`colorWholeBlock`). Parte dal colore che la riga ha già
  - **Duplicate**: la copia nasce **subito sotto**, con tipo, testo,
    colori, rientro e spunta. Un toggle si copia **coi suoi figli**, una
    tabella **con le sue celle**, e le sottopagine o i database dentro
    un toggle si copiano anche loro, come fa "Duplicate" di una pagina:
    due collegamenti alla stessa sottopagina non sarebbero una copia
    (`PageRepository.duplicateBlock`)
- **[Nuova funzionalità]** **[📱 Mobile]** **Tenendo premuto il collegamento a una
  pagina** nel testo (dove il cursore non entra) si apre il menu **della
  pagina**: Turn into (grigio: trasformare una pagina in altro non è
  ancora costruito), **Color** (il colore del nome, solo in quel
  collegamento, tenuto in uno span vuoto del blocco), **Edit icon** (la
  stessa finestra Upload / Link / Remove), **Add to favorites** /
  **Remove from favorites**, **Rename** (una finestrella col nome già
  tutto selezionato; cambia solo il titolo, `PageDao.setTitle`, e il
  nome della riga se è la pagina di una riga), **Duplicate** (la stessa
  scelta del posto dei tre puntini; "accanto all'originale" vuol dire
  **sotto il collegamento toccato**, anche se la pagina è collegata in
  due punti), **Move to**, **Move to trash** (con la stessa conferma dei
  tre puntini). Si resta sempre nella pagina: un avviso dice dove è
  finita la copia quando non è qui. Il tocco normale apre la pagina come
  prima (`combinedClickable`)
- **[Nuova funzionalità]** **[📱 Mobile]** **Per un database**, in cima: **Turn into
  page**, **Turn into simple database**, **Turn into complex database**,
  **Lock database** (interruttore), **Open as page**; sotto le voci
  della pagina, con Turn into e Color grigi. Ci si arriva dai **sei
  puntini** che ora stanno all'inizio della riga degli strumenti di un
  database dentro la pagina — prima del riquadro della vista, perché
  il nome si può nascondere e i puntini devono esserci sempre — o
  tenendo premuto un collegamento a un database
  - **Turn into page** fa quello che fa la voce delle impostazioni del
    database: il blocco diventa un collegamento e il database si apre a
    schermo intero. Se il database è **già** un collegamento, al suo
    posto c'è **Turn into database**, che rimette dentro la pagina
    **quel** collegamento (`turnPageLinkIntoDatabase`; la voce dei tre
    puntini li rimette dentro tutti)
  - **Turn into simple database**: le pagine delle righe si cancellano
    **per sempre**, con quello che contengono — non nel cestino: in un
    database semplice una pagina di riga non avrebbe più una riga a cui
    tornare —; nomi e proprietà delle righe restano
    (`turnIntoSimpleDatabase`). L'utente ha detto che ci sta attento
    lui; comunque, **se almeno una pagina di riga ha qualcosa dentro**
    (testo, un blocco che non sia una riga vuota, un'icona, una
    copertina) una conferma dice **quante sono**. Se non c'è niente da
    perdere si fa e basta (`countRowPagesWithContent`)
  - **Turn into complex database**: le righe tornano pagine. Ognuna
    nasce la prima volta che la si apre, come in ogni database
    (`ensureRowPage`), e compaiono subito nella barra laterale
  - Le due voci ci sono sempre: quella che il database è già si vede
    grigia con la spunta
- **[Bug fix]** **[📱 Mobile]** *(trovato leggendo il codice per il Duplicate;
  **confermato dai test** della richiesta 34)* **Annulla
  e Ripristina svuotavano tutte le tabelle della pagina.** Rimettono la
  pagina com'era riscrivendo tutti i blocchi da capo
  (`replaceAllBlocks`: cancella e reinserisce), e le celle delle tabelle
  sono legate al loro blocco con `ON DELETE CASCADE`, con le chiavi
  esterne accese (`PRAGMA foreign_keys = ON` nel codice generato da
  Room): cancellando la tabella il database portava via il suo testo.
  Bastava annullare una lettera scritta tre righe più su. Ora le celle
  si mettono da parte e si rimettono, tutto in una transazione (e
  `TableCellDao.getCellsForPageOnce`). Il testo delle celle resta quello
  di adesso: Annulla non lo segue, come già scritto nel codice
- **[Bug fix]** **[📱 Mobile]** *(trovato scrivendo "Turn into")* **Un toggle con delle
  righe dentro, cambiato in un altro tipo dal "+"**, lasciava le sue
  righe appese a un blocco che non le mostra: **sparivano alla vista**
  (nel database c'erano ancora). Ora escono subito sotto, allo stesso
  livello e nello stesso ordine, come quando il toggle si toglie col
  backspace — per ogni trasformazione: tipo, pagina, database, tabella,
  divisore (`PageRepository.unnestChildren`, estratta da
  `unnestAndConvert`)
- **[Nuova funzionalità]** **[📱 Mobile]** Il collegamento a una pagina **segue la
  pagina nel tempo** (`observePageInfo` al posto di `getPageInfo`):
  prima nome e icona si leggevano quando la riga compariva e restavano
  quelli; con Rename ed Edit icon dal menu del blocco devono cambiare
  subito
- **[Progetto]** **Annulla non fa ricomparire il collegamento a una
  pagina spostata o buttata** dal menu del blocco: le foto di Annulla
  sono tutti i blocchi della pagina, e quelle scattate prima avevano
  ancora il collegamento. Viene tolto da tutte (`forgetLinksTo`).
  *Superato* nella voce successiva ("Move to — l'albero con la
  ricerca"): `forgetLinksTo` è stata tolta, e al suo posto un controllo
  fatto a ogni Annulla vale per tutte le strade, non solo per il menu
  del blocco (`withoutStaleLinks`)
- **[Progetto]** `SLASH_ENTRIES`, `SlashEntry`, `SlashAction`,
  `SlashCategory` (in `PageEditorScreen.kt`) e `OptionsGroup`,
  `OptionRow` (in `PageOptionsSheet.kt`) da privati a `internal`, per
  usarli nel file nuovo. `PageRepository.duplicatePage` ha un parametro
  in più, `besideLinkBlockId`, messo **prima** di `copyImage` perché le
  chiamate che passano la copia delle immagini come ultima lambda
  continuino a funzionare. Testi nuovi nelle otto lingue
  (`EditorStrings.blockOptions`, `turnInto`, `color`, `editIcon`,
  `addToFavorites`, `removeFromFavorites`, `rename`,
  `turnIntoSimpleDatabase`, `turnIntoComplexDatabase`, `lockDatabase`,
  `openAsPage`, `simpleDatabaseLossTitle`, `simpleDatabaseLossText`)

### 🆕 v0.45 — 24/09/2026 · 13:25 — Database semplice — le righe sono solo testo
*(verificato in parte
sul telefono il 24/09/2026: voci 21 e 22)* *(sessione cloud del
24/09/2026, richiesta 30: compilato, test passati, migrazione 27→28
simulata, **non provato sul telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** **"Simple database"** nei menu "/" e "+"
  (finora grigia) crea un **database semplice**: un database vero, con
  tutte le viste e le impostazioni degli altri — filtri, ordinamento,
  raggruppamento, proprietà, Lock view — ma in cui **le righe sono solo
  testo e non diventano mai pagine**. L'utente lo voleva da sempre, e
  nessuna app lo ha. Nasce come tabella, come "Database - Inline"; la
  vista si cambia dalle impostazioni. Salvato in
  `PageEntity.isSimpleDatabase` (**migrazione 27→28**, 0 per tutti i
  database che c'erano: restano normali)
- **[Nuova funzionalità]** **[📱 Mobile]** **Toccando una riga si apre la sua scheda**
  invece di una pagina, in tutte e sei le viste (tabella dal tocco
  prolungato, bacheca, elenco, galleria, calendario, linea del tempo):
  in cima **il nome da scrivere**, subito sotto **tutte le proprietà**
  modificabili sul posto, poi Delete e le date di creazione e modifica
  (`RowActionsSheet` con `simple = true`, `SimpleRowNameField`). Nella
  tabella il nome si scrive toccandolo, come sempre
- **[Nuova funzionalità]** **[📱 Mobile]** Tolto tutto quello che nei database normali
  riguarda le pagine delle righe: **niente OPEN** nella cella del nome,
  **niente voce "Icon"** nella scheda, **niente "Card preview"** nella
  galleria (copertina e testo sono della pagina della riga: le schede
  hanno nome e proprietà), e i pulsanti dicono **"Nuova riga"** invece di
  "Nuova pagina". In fondo alle impostazioni una riga dice che il
  database è semplice e che cosa vuol dire
- **[Nuova funzionalità]** **[📱 Mobile]** **Nessuna strada porta a una pagina di
  riga**: il divieto sta in `PageRepository.ensureRowPage`, l'unico punto
  da cui una pagina di riga nasce, che per un database semplice rifiuta.
  E in più: nella **barra laterale** il database semplice non ha figli
  (`treeChildren`, `nodeFor`); nella **ricerca** una riga trovata porta
  **al database** e dice in quale sta; il **Duplicate** non offre le sue
  righe come posto in cui copiare
- **[Bug fix]** **[📱 Mobile]** *(trovato scrivendo questo)* La lista dei risultati della
  ricerca usava come chiave l'identificativo della pagina: un risultato
  di riga che porta al database, e due righe dello stesso database,
  avrebbero avuto la stessa chiave, cosa che una `LazyColumn` non
  perdona — l'app si chiude. Ora la chiave è prima la riga, poi la
  pagina (`SearchScreen`)
- **[Progetto]** `RowPropertiesList` è stata estratta dalla finestra
  delle azioni di una riga, per usarla sia in "Edit property" sia nella
  scheda della riga semplice. Testi nuovi nelle otto lingue
  (`DbStrings.newRow`, `row`, `simpleDatabaseNote`)

### ✏️ v0.44.2 — 24/09/2026 · 13:12 — Menu "+" — le stesse voci del menu "/", nello stesso ordine
*(verificato in parte sul telefono il 24/09/2026: voce 20)*
*(sessione cloud del 24/09/2026, richiesta 29: compilato, test passati,
**non provato sul telefono**)*
- **[Modifica]** **[📱 Mobile]** Il menu "+" della barra **legge lo stesso
  catalogo del menu "/"** (`SLASH_ENTRIES`): stesse voci, stesse
  icone, **stesso ordine**, comprese le voci non ancora costruite, che
  ci sono e sono grigie. Prima il "+" aveva un elenco suo, fermo alle
  nove voci dei primi tempi, mentre il "/" cresceva. Ora una voce
  aggiunta al catalogo compare in tutti e due
- **[Modifica]** **[📱 Mobile]** **Due colonne riempite riga per riga**, come
  chiesto: la prima voce del "/" è la prima a sinistra, la seconda la
  prima a destra, e così via. Oggi (27 voci, l'ultima riga ne ha una):

  | sinistra | destra |
  |---|---|
  | Text | Bulleted list |
  | Numbered list | To-do list |
  | Toggle list | Page |
  | *Callout* | *Quote* |
  | Table | Divider |
  | *Link to page* | *Image* |
  | *Video* | *Audio* |
  | *Code* | *File* |
  | *Web bookmark* | Table view |
  | Board view | Gallery view |
  | List view | Calendar view |
  | Timeline view | Database - Inline |
  | *Database - Full page* | Simple database |
  | *Linked view of data source* | |

  (in corsivo le grigie; "Simple database" era grigia fino alla
  richiesta 30). Senza i titoli delle famiglie che ha il "/"
  (Basic blocks, Media, Database): su due colonne riga per riga
  spezzerebbero l'ordine chiesto, lasciando buchi a fine famiglia. Il
  menu scorre, perché le righe sono più di quante ne stiano sopra la
  tastiera. Le icone sono quelle del "/" (prima il "+" aveva le sue,
  con le lettere "T", "H1"...)
- **[Progetto]** **Una sola funzione applica la voce scelta**, da tutti
  e due i menu (`applyInsertAction` in `PageEditorScreen`): erano due
  copie quasi uguali e si stavano già separando — il "+" creava un
  database sempre come tabella, mentre dal "/" "Calendar view" dava un
  calendario. Ora dal "+" ci sono anche tutte le viste di database
  (Table, Board, Gallery, List, Calendar, Timeline view), che prima
  aveva solo il "/". Il comportamento di ogni voce è quello che aveva
  nel "/"

### ✏️ v0.44.1 — 24/09/2026 · 13:06 — Pagine — la dimensione si scrive a mano, e via i titoli (Heading)
*(verificato sul telefono il 24/09/2026: voci 18 e 19, e la migrazione
26→27 su titoli veri)* *(sessione cloud del 24/09/2026, richiesta 28: compilato, test passati,
migrazione 26→27 provata su un database di prova, **non provato sul
telefono**)*
- **[Modifica]** **[📱 Mobile]** **La dimensione del testo si scrive a
  mano**: toccando il numero nella barra Aa si apre una finestrella
  (`FontSizeDialog`) con una piccola casella, il numero in uso già
  selezionato (scrivendo lo si sostituisce) e **la tastiera dei
  numeri**. Accetta solo cifre, al massimo due; "Fatto" — e il tasto
  Fatto della tastiera — funziona solo fra 5 e 72, e sotto la casella
  "Da 5 a 72" diventa rosso se si esce. Sostituisce l'elenco di numeri
  della prima versione, come chiesto dall'utente
- **La finestra e non una casella dentro la barra**, di proposito:
  mentre si scrive il numero la tastiera passa alla casella, nessuna
  riga della pagina ha più il cursore, e la barra Aa — che esiste solo
  quando una riga ce l'ha — se ne va. Una casella nella barra sparirebbe
  a metà numero; per lo stesso motivo la finestra è accesa da uno stato
  della schermata (`showFontSizeDialog`), come quella del pennello, e
  non nasce dentro la barra. Chiusa la finestra, **il cursore non torna
  da solo** nella pagina: si tocca dove si vuole riprendere. Rimetterlo
  a comando vorrebbe dire spostare il fuoco mentre la tastiera passa da
  numeri a lettere, il passaggio che con la tastiera Samsung dà più
  problemi
- **[Rimosso]** **[📱 Mobile]** **I titoli — Heading 1, 2 e 3 — dall'app intera**, su
  richiesta dell'utente: con la dimensione scelta per pagina, dei titoli
  a misura fissa non se ne faceva più niente (e i "Toggle Heading" che
  stava per chiedere non servono più). Tolti dal menu "+" e dal menu
  "/", dal tipo di blocco (`BlockType`: i tre valori non esistono più),
  dal campo unito (`FLOWING_TYPES`, `isFlowingTextType`), dalla sua
  trasformazione visiva, dalle isole, dall'anteprima della Gallery e
  dai testi tradotti. Nessuna scorciatoia di scrittura li creava
- **[Rimosso]** **[📱 Mobile]** **Quelli già scritti sono diventati testo normale**
  (**migrazione 26→27**: `UPDATE blocks SET type = 'PARAGRAPH'`). Fra le
  tre possibilità proposte — testo in grassetto, testo normale,
  lasciarli come sono — l'utente ha scelto il testo normale. Il testo
  non si perde, e la formattazione messa a mano (grassetto, colori)
  resta. Provata su un database di prova: tre titoli con grassetto e
  colore diventano paragrafi identici, gli altri blocchi non si toccano,
  e nessun tipo rimasto nel database è sconosciuto all'app — che è
  quello che conta, perché un nome di tipo che l'enum non conosce più fa
  chiudere l'app alla lettura. Lo schema delle tabelle non cambia
- **"Predefinito"** in cima all'elenco dei font resta, confermato
  dall'utente

### 🆕 v0.44 — 24/09/2026 · 12:55 — Pagine — font e dimensione del testo (barra Aa)
*(verificato sul
telefono il 24/09/2026: voci 17 e 18; la voce della dimensione era
vuota, corretto — vedi "Ripresa sul PC")* *(sessione cloud
del 24/09/2026, richiesta 27: compilato, migrazione 25→26 simulata,
**non provato sul telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** Nella barra Aa, **prima di B**, due voci
  come su OneNote: **il font**, col nome di quello in uso ("Calibri", o
  "Predefinito" se la pagina non ne ha scelto uno), e **la dimensione**,
  col numero in uso ("16"). Toccandole si apre l'elenco per cambiarle.
  Valgono per **tutta la pagina**, come chiesto ("selezionando
  automaticamente tutto quanto"), e sono salvate per pagina
  (`PageEntity.pageFont` e `pageFontSize`, **migrazione 25→26**; null
  vale "come prima", e il 16 si salva come null). Con la pagina bloccata
  le due voci non ci sono
- **[Nuova funzionalità]** **[📱 Mobile]** **I font, e chi li disegna davvero.** Quelli
  chiesti sono commerciali e non si possono mettere nell'app: al loro
  posto ci sono sosia con licenza libera (SIL Open Font License), scelti
  e approvati con l'utente. **Nella barra si legge il nome originale;
  nell'elenco il nome originale col sosia fra parentesi** — scelta
  dell'utente. L'elenco, nell'ordine chiesto, con un divisore fra i
  gruppi e "Predefinito" in cima per tornare al font di sistema:

  | Nome | Sosia | Da dove arriva |
  |---|---|---|
  | Helvetica | Inter | dentro l'app |
  | Garamond | EB Garamond | dentro l'app |
  | Arial | Arimo (stesse misure) | dentro l'app |
  | Verdana | Noto Sans (il più vicino) | dentro l'app |
  | Georgia | Gelasio (stesse misure) | dentro l'app |
  | Calibri | Carlito (stesse misure) | dentro l'app |
  | Futura | Jost | dentro l'app |
  | Times New Roman | Tinos (stesse misure) | dentro l'app |
  | Cambria | Caladea (stesse misure) | dentro l'app |
  | Consolas | Inconsolata (niente corsivo vero) | dentro l'app |
  | Songti | Noto Serif SC | scaricato |
  | Kaiti | Ma Shan Zheng (niente grassetto vero) | scaricato |
  | ~~MS YaHei~~ SimSun *(dal 25/09/2026)* | ~~Noto Sans SC~~ Noto Serif SC nel peso Light | scaricato |
  | Mincho | Noto Serif JP | scaricato |
  | Gothic | Noto Sans JP | scaricato |
  | Kaisei | Kaisei Opti (è proprio lui, già libero) | scaricato |

  **Lishu tolto**: un Lishu libero non esiste, e l'utente ha scelto di
  toglierlo invece di sostituirlo con un'altra calligrafia
- **[Nuova funzionalità]** **[📱 Mobile]** **Da dove arrivano**, scelta dell'utente
  ("Misto"): i dieci occidentali stanno **dentro l'app** — 38 file TTF
  in `app/src/main/res/font` (normale, grassetto, corsivo, grassetto
  corsivo), circa 12 MB, scaricati da Google Fonts il 24/09/2026 — e
  funzionano sempre, anche offline. I sei cinesi e giapponesi pesano
  10-20 MB l'uno e **si scaricano da Google Play Services** la prima
  volta che una pagina li usa, poi restano sul telefono. L'APK di debug
  è passato da 17,8 a 23,9 MB
- **[Nuova funzionalità]** **[📱 Mobile]** **Il corpo scala tutto in proporzione**, non
  solo il testo normale (`PageTypography`, `ui/theme/PageFonts.kt`): i
  titoli H1-H3 (28/22/18 a corpo 16; tolti poco dopo, richiesta 28),
  l'altezza delle righe del campo
  unito (24, o 27 con le caselle), i segni degli elenchi. A 16 ogni
  misura resta esattamente quella di prima. Le misure passano da un
  `CompositionLocal` (`LocalPageTypography`) e arrivano al campo unito
  (dal suo `textStyle`, l'unico posto dove il README dice che le misure
  si toccano senza rischi), alla sua trasformazione visiva (solo
  `fontSize` negli stili, nessun carattere in più: la traduzione delle
  posizioni non cambia), alle isole, ai toggle, alle celle delle tabelle
  e ai collegamenti a pagine. Il titolo della pagina prende **solo il
  font**. Restano fissi i quadratini delle caselle e le frecce dei
  toggle, e i database dentro la pagina
- **[Nuova funzionalità]** **[📱 Mobile]** **Gli elenchi non chiudono la tastiera**
  (`PopupProperties(focusable = false)`): un menu che prende il fuoco
  chiude la tastiera, con lei se ne va la barra — ci è appoggiata sopra
  — e con la barra la voce da cui il menu è partito. Così si sceglie e
  si continua a scrivere. ~~La dimensione si sceglie da un elenco con
  tutti i numeri da 5 a 72~~ — **superato dalla richiesta 28**: la
  dimensione si scrive a mano in una finestrella, vedi la voce qui
  sotto
- **[Nuova funzionalità]** **[📱 Mobile]** Nell'elenco dei font i nomi occidentali sono
  scritti **ognuno col suo font**, come nei menu di Word; quelli cinesi
  e giapponesi no, per non scaricarli tutti solo aprendo il menu
- **[Progetto]** Nuova dipendenza `androidx.compose.ui:ui-text-google-fonts`
  (versione dal BOM di Compose). I certificati del fornitore dei font
  sono in `res/values/font_certs.xml`, copiati dall'esempio ufficiale
  Android "DownloadableFonts" (licenza Apache 2.0). Gli avvisi di
  copyright e il testo della licenza dei dieci font inclusi sono in
  `app/src/main/assets/font_licenses/OFL.txt`, come chiede la licenza.
  Testi nuovi nelle otto lingue (`EditorStrings.defaultFont`, `font`,
  `fontSize`). Primo uso di `R` nel codice (`R.font`, `R.array`)

### 🆕 v0.43 — 24/09/2026 · 12:32 — Pagine — il conteggio del testo ("X words")
*(verificato sul
telefono il 24/09/2026: voce 16)* *(sessione cloud del
24/09/2026, richiesta 25: compilato e con i test automatici passati,
**non provato sul telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** In fondo al menu dei tre puntini di una
  pagina, dopo "Updates", c'è **"X words"** (col numero nel formato
  scelto nelle impostazioni: 2.652 o 2,652). **Toccandola si apre lì
  sotto** l'elenco completo, e ritoccandola si richiude. L'utente
  lasciava scegliere fra una finestra a parte e l'apertura sul posto:
  scelta la seconda, perché una finestra sopra il menu, su un telefono,
  copre proprio il menu da cui si arriva, e per richiuderla serve un
  tocco in più. Da far confermare all'utente
- **[Nuova funzionalità]** **[📱 Mobile]** Le voci, nell'ordine chiesto: **Words,
  Letters, Numbers, Letters and numbers, Characters (no spaces),
  Characters (with spaces), Lines, Japanese characters, Chinese
  characters, Korean characters** (in italiano: Parole, Lettere,
  Numeri, Lettere e numeri, Caratteri senza e con spazi, Righe,
  Caratteri giapponesi, Caratteri cinesi, Lettere coreane — la parola
  dell'utente). Tutte e dieci sempre, anche a zero, come in Word
- **[Nuova funzionalità]** **[📱 Mobile]** *(richiesta 26)* **"Lettere e numeri"** è
  una voce a sé, voluta dall'utente dopo aver visto come si contano i
  caratteri: la somma di lettere e cifre e **nient'altro**
  (`TextStats.lettersAndDigits`), mentre **i caratteri contano tutto**
  — lettere, numeri e ogni altro segno: parentesi, punti, virgole,
  asterischi... Così "hin(...) = 3x!" fa 5 lettere e numeri e 12
  caratteri senza spazi. Le due voci sui caratteri (senza e con spazi)
  restano come nella richiesta originale
- **[Nuova funzionalità]** **[📱 Mobile]** **Le regole sono quelle della finestra
  "Word Count" di Word**, che l'utente ha mostrato come modello
  (`data/TextStats.kt`, `textStatsOf`):
  - una **parola** è quello che sta fra due spazi, anche "=" o "[]";
    in cinese e giapponese **ogni carattere è una parola**, perché
    quelle lingue non separano le parole con gli spazi; il coreano sì,
    e si conta come l'italiano;
  - le **lettere** sono quelle degli alfabeti (latino con gli accenti,
    greco, cirillico...), senza cinese, giapponese e coreano, che hanno
    le loro voci; i **numeri** sono le cifre, una per una ("2026" fa
    4), perché l'utente li descriveva come una parte dei caratteri;
  - i **caratteri** sono tutti i segni scritti, punteggiatura e simboli
    compresi, come in Word. L'utente li aveva descritti come "lettere e
    numeri insieme", ma così sarebbero stati solo la somma delle due
    voci sopra: **scelta da far confermare**. Con gli spazi si contano
    anche gli spazi (compresi quello "che non va a capo" e quello largo
    delle lingue asiatiche), non gli a-capo;
  - **righe** = blocchi con del testo, come ha chiesto l'utente ("in
    realtà quanti blocchi"): i blocchi vuoti non contano, una tabella
    conta una volta;
  - **giapponese o cinese**: gli ideogrammi sono gli stessi, quindi si
    guarda la riga — se ha almeno un hiragana o katakana, i suoi
    ideogrammi sono giapponesi, altrimenti cinesi. Il trattino lungo
    del katakana (ー) conta come giapponese;
  - si conta per **punti di codice** e non per `Char`, così un'emoji o
    un ideogramma raro è un carattere solo e non due
- **[Nuova funzionalità]** **[📱 Mobile]** **Cosa si conta** (`PageRepository.textStats`):
  il testo di paragrafi, titoli, elenchi, caselle e toggle, **anche
  quello dentro i toggle chiusi**, e le celle delle tabelle semplici
  (solo quelle che si vedono: rimpicciolita una tabella, le celle
  rimaste fuori non contano). **Non si contano**, come chiesto, i
  database e tutto quello che l'utente non ha scritto lì: righe e celle
  dei database, i nomi delle pagine collegate, e **il titolo della
  pagina** — spesso messo dall'app ("Senza titolo", "Untitled"). Da far
  confermare all'utente
- **[Nuova funzionalità]** **[📱 Mobile]** Per un **database** aperto a schermo intero
  la voce non c'è: il suo contenuto sono righe, e le righe non si
  contano. Il menu dei tre puntini ora **scorre**, perché aperto il
  conteggio le voci sono più di quante stiano su uno schermo basso
- **[Progetto]** **Primi test automatici del progetto**:
  `app/src/test/java/com/gabriele/notionlocal/data/TextStatsTest.kt`,
  sedici casi — italiano con accenti, liste di vocaboli con "[]" e
  "=", cifre, lettere e numeri separati dai simboli, giapponese con e
  senza kana, katakana col trattino lungo, cinese, coreano, lingue
  mescolate, spazi speciali, emoji, a-capo, testo vuoto. Girano senza
  telefono: `gradle testDebugUnitTest`

### 🆕 v0.42 — 24/09/2026 · 12:13 — Colonne — spostarle tenendo premuta l'intestazione
*(verificato in
parte sul telefono il 24/09/2026: voce 15)* *(trovata già
scritta nel codice arrivato dal PC il 24/09/2026, ma mancava in questo
README: documentata nella sessione cloud dello stesso giorno. **Da
verificare sul telefono**: non risulta che sia mai stata provata)*
- **[Nuova funzionalità]** **[📱 Mobile]** Tenendo premuta l'intestazione di una
  colonna della tabella, il menu ha nell'ordine: **Move to left**,
  **Move to right**, **Edit property**, *Center / Align left* (solo per
  le colonne a caselle) e **Hide**. È la richiesta dell'utente, che
  nella sessione cloud l'ha ripetuta parola per parola (Diario,
  richiesta 20) perché l'aveva appena chiesta a Code sul PC
- **[Nuova funzionalità]** **[📱 Mobile]** *(sessione cloud del 24/09/2026, richiesta
  22)* **Le due voci per spostare sono passate in cima al menu**, sopra
  Edit property. Nel codice arrivato dal PC stavano sotto, come diceva
  la prima richiesta; l'utente, non avendole mai viste sul telefono
  (l'app installata era più vecchia del codice), ha chiesto di metterle
  sopra. Spostare è la cosa per cui si tiene premuta un'intestazione —
  per modificarla basta toccarla — e in cima si trova senza leggere il
  menu. Cambiato solo l'ordine delle voci in `PropertyHeaderCell`
- **[Rimosso]** **[📱 Mobile]** *(sessione cloud del 24/09/2026, richiesta 23)* **"Move
  left" e "Move right" dalla finestra della proprietà** (quella di Edit
  property, `PropertySheet`): lì resta solo "Delete property". Erano un
  doppione del menu dell'intestazione, e per di più comparivano una
  alla volta a seconda della colonna — sulla prima dopo Name c'era solo
  "Move right", ed è quella che l'utente ha chiesto di togliere; tolte
  tutte e due, perché "Move left" è la stessa cosa sulle altre colonne.
  Tolti anche i parametri `canMoveLeft`, `canMoveRight` e `onMove` di
  `PropertySheet`. **Conseguenza**: una colonna nascosta non si può più
  spostare senza rimostrarla (vedi Limiti noti). Il ramo di
  `DatabaseTableState.neighbourForMove` che muoveva le colonne nascoste
  fra tutte resta nel codice ma oggi nessuno lo raggiunge
- **[Nuova funzionalità]** **[📱 Mobile]** **"Move to left" è grigia sulla prima
  colonna dopo Name**, qualunque essa sia: Name è il titolo delle
  pagine, non una proprietà, e resta sempre la prima. **"Move to
  right" è grigia sull'ultima**, e con **"Lock view"** lo sono tutte e
  due. Grigie e non nascoste: una voce che sparisce e ricompare farebbe
  cambiare posto alle altre sotto il dito (`PropertyHeaderCell`,
  `canMoveLeft = !viewLocked && index > 0`)
- **[Nuova funzionalità]** **[📱 Mobile]** Si conta **fra le colonne che si vedono**
  (`DatabaseTableState.neighbourForMove`): scambiarsi con una colonna
  nascosta in mezzo non sposterebbe niente di visibile. Lo spostamento
  rinumera tutte le colonne in un salvataggio solo
  (`DatabaseViewModel.moveColumn` → `DatabaseRepository.updateColumns`,
  una transazione), così la tabella si ridisegna una volta. Con la
  tabella raggruppata l'intestazione si ripete sopra ogni gruppo, con
  lo stesso menu. Le regole sono le stesse di Move left / Move right
  nella finestra della proprietà

### 🆕 v0.41 — 24/09/2026 · 11:50 — Database — vista Gallery
*(verificato in parte sul telefono il
24/09/2026: voci 3-14; mancano copertine, rotazione, fluidità)*
*(sessione cloud del 24/09/2026:
**compilata** in cloud — `assembleDebug` riuscito, query controllate da
Room, migrazione simulata su SQLite, vedi Diario — **ma NON provata sul
telefono**)*
- **[Nuova funzionalità]** **[📱 Mobile]** **Gallery**, la sesta vista: le pagine del
  database come **schede in griglia**. In alto l'anteprima, sotto il
  nome (con l'icona della pagina, se c'è) e le proprietà che si vedono
  e hanno un valore, una per riga come sulle schede della bacheca. Un
  tocco apre la pagina, il tocco prolungato le azioni della riga, come
  nelle altre viste. Filtro e ordinamento valgono anche qui. Si sceglie
  da Settings → Layout (sesto riquadro, icona a griglia) o si crea dal
  menu "/" con **"Gallery view"**, che finora c'era ma era spenta
- **[Nuova funzionalità]** **[📱 Mobile]** **Card preview**, nelle impostazioni della
  vista, con le voci di Notion che qui si possono fare: **None**,
  **Page cover**, **Page content**. Salvata per ogni database
  (`PageEntity.galleryCardPreview`, **migrazione 24→25**). Finché non
  si sceglie vale **Page cover** (`GALLERY_DEFAULT_PREVIEW`): la
  galleria aspettava le copertine proprio perché è la vista che si
  riconosce dalle immagini. La quarta voce di Notion, l'anteprima da una
  proprietà "Files & media", manca perché quel tipo di proprietà non
  esiste nell'app
- **[Nuova funzionalità]** **[📱 Mobile]** **Card size**: **Small / Medium / Large**,
  salvata per ogni database (`PageEntity.galleryCardSize`, stessa
  migrazione), media finché non si sceglie. Le schede non hanno una
  larghezza fissa ma **una minima** (100, 150 e 280 punti): in una riga
  ne entrano quante ci stanno e si allargano insieme a riempirla. Su un
  telefono in verticale sono tre, due o una per riga; girandolo ne
  entrano di più invece di diventare enormi (`galleryColumnsFor`)
- **[Nuova funzionalità]** **[📱 Mobile]** Con **Page cover** la scheda mostra la
  copertina della pagina **con l'inquadratura scelta in
  "Reposition"**. La scheda ha un'altra forma della striscia della
  pagina, e la stessa inquadratura avrebbe potuto portare l'immagine
  oltre il bordo e scoprire un angolo vuoto: `CoverImage` (ora
  `internal`, per usarla da qui) ha un parametro nuovo, `clampOffset`,
  che ferma lo spostamento dove l'immagine smette di coprire. Nella
  pagina resta spento e lì non cambia niente
- **[Nuova funzionalità]** **[📱 Mobile]** Con **Page content** la scheda mostra
  **l'inizio del testo della pagina**, in piccolo (8, 10 o 12 punti
  secondo la dimensione della scheda), scritto come nella pagina: i
  titoli più grandi e in grassetto, gli elenchi con **lo stesso segno
  della pagina** (`bulletMarkerFor` e `numberedMarkerFor`, ora
  `internal`), numerati contando come `numberedListOrdinal` — un
  contatore per livello, `numberStartsAt` rispettato — le caselle con
  ☐ e ☑ (quelle spuntate grigie e barrate), i toggle con ▸. Si leggono
  al massimo 12 blocchi di primo livello per pagina
  (`GALLERY_PREVIEW_MAX_LINES`), saltando quelli senza testo (tabelle,
  divisori, collegamenti) e le righe vuote in testa e in coda; quello
  che non ci sta lo taglia il bordo della scheda
- **[Nuova funzionalità]** **[📱 Mobile]** **Gli spoiler restano coperti
  nell'anteprima**: ogni lettera coperta diventa `▒`, gli spazi
  restano. Senza, un testo nascosto nella pagina si sarebbe letto in
  chiaro nella scheda — che è proprio il colpo d'occhio da cui lo
  spoiler protegge
- **[Nuova funzionalità]** **[📱 Mobile]** Le schede di una stessa riga sono **alte
  uguali** (`IntrinsicSize.Max`), e l'ultima riga, se non è piena,
  tiene le schede larghe come le altre. L'anteprima occupa il suo posto
  anche quando è vuota (pagina senza copertina o senza testo), così le
  schede restano allineate
- **[Nuova funzionalità]** **[📱 Mobile]** "Card preview" e "Card size" compaiono nelle
  impostazioni **solo con la galleria**, subito sotto Filter come le
  voci delle altre viste, e **"Lock view" le spegne**. Scelta una voce
  la finestra resta aperta e il segno ✓ si sposta, come per "Date
  property" e "Group by": la galleria dietro cambia subito
- **[Nuova funzionalità]** **[📱 Mobile]** Testi nuovi nelle otto lingue dell'app
  (`DbStrings.layoutName`, `galleryPreviewName`, `gallerySizeName`,
  `cardPreview`, `cardSize`). In italiano la dimensione è al femminile
  perché si parla della *scheda*
- **[Progetto]** Copertine e testo delle pagine si leggono **solo
  mentre la galleria li mostra** (`DatabaseViewModel.load`,
  `flatMapLatest` sulla pagina). Il testo soprattutto: la query guarda
  la tabella dei blocchi, che cambia a ogni tasto battuto in
  **qualunque** pagina, e il ViewModel di un database resta vivo anche
  quando è rimasto indietro nella pila di navigazione. Seguirla sempre
  avrebbe voluto dire rifare la query a ogni lettera scritta altrove.
  Due query nuove in `DatabaseDao`: `observeRowCovers` e
  `observeRowPreviewBlocks`; il taglio ai primi blocchi lo fa
  `DatabaseRepository.observeRowContentPreviews`, perché in SQL
  richiederebbe funzioni che su Android arrivano solo dalla versione 11
- **Scelte da far confermare all'utente**: la copertina come anteprima
  di partenza, le larghezze minime delle schede e il rapporto 1,6 : 1
  della parte alta (`GALLERY_PREVIEW_RATIO`)

### 🆕 v0.40 — 23/09/2026 · 15:41 — Duplicate: si sceglie dove va la copia, e la copia è davvero una copia
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** "Duplicate" nel menu dei tre puntini (pagine e
  database a schermo intero) apre una finestra che chiede **dove mettere
  la copia**, con tre scelte: **Next to the original** (subito sotto
  l'originale; per la pagina di una riga vuol dire **una riga nuova
  dello stesso database**, subito sotto, con tutte le sue proprietà),
  **End of the main menu** (in fondo alla pagina principale) e **Inside
  a page** (`DuplicateSheet.kt`)
- **Inside a page** apre l'albero della barra laterale: si tocca la
  pagina in cui metterla e la copia finisce in fondo. I database si
  aprono come cartelle e mostrano le loro righe, che si possono
  scegliere tutte, **anche quelle mai aperte** (la loro pagina nasce in
  quel momento, `DuplicateTarget.IntoRow`). I database stessi non si
  scelgono: dentro hanno righe, non blocchi. In alto c'è una ricerca per
  titolo, e sotto ogni risultato si legge dove sta la pagina ("In A",
  "In Untitled"): dopo un Duplicate i nomi uguali sono sempre due. Le
  pagine che non stanno da nessuna parte non compaiono, perché una copia
  messa lì dentro sparirebbe con loro
- Fatta la copia **si apre**, e se non è accanto all'originale un avviso
  dice dov'è finita ("Copy placed in “B”"). Il titolo prende " (copy)"
  nella lingua dell'app; le pagine dentro di lei tengono il loro nome
- **[Bug fix]** **[📱 Mobile]** La copia **non era una copia**. Puntava alle stesse
  sottopagine e, per un database, alle stesse pagine delle righe:
  scrivere in una sottopagina della copia cambiava anche l'originale,
  rinominare la pagina di una riga rinominava la riga in tutti e due i
  database, buttarne una la toglieva da entrambi. Ora si copia **tutto,
  giù fino in fondo**, come su Notion (`PageRepository.copyPageDeep`);
  una pagina richiamata due volte si copia una volta sola, e un
  collegamento all'indietro non fa girare la copia all'infinito
- **[Bug fix]** **[📱 Mobile]** Duplicare **la pagina di una riga** creava una pagina che
  non stava da nessuna parte: non c'era un collegamento da mettere
  accanto, e la copia si trovava solo cercandola. Nel database del
  telefono ce n'è una rimasta così, **"A (copia)"**, di una prova di
  prima. Ora accanto all'originale diventa una riga nuova, e altrove una
  pagina normale
- **[Bug fix]** **[📱 Mobile]** Icona e copertina della copia **condividevano il file**
  con l'originale: cambiando l'immagine di una, o cancellandola per
  sempre, il file spariva e l'altra restava senza. Ora ogni copia ha il
  suo file (`PageImageStore.copy`)
- **[Bug fix]** **[📱 Mobile]** La copia perdeva il testo delle **tabelle semplici**
  (vive in una tabella a parte, per blocco) e, in un database, puntava
  ancora alle colonne dell'originale per raggruppamento, calendario,
  ordine e filtro, che quindi sparivano. I figli dei toggle si salvano
  dopo il loro toggle, come chiede il vincolo sul genitore
- **[Bug fix]** **[📱 Mobile]** Il testo scritto in una **tabella semplice spariva
  riaprendo la pagina** (nel database c'era ancora): ogni casella teneva
  il suo testo in uno stato nato col primo valore visto, e al primo
  disegno le celle non erano ancora arrivate. Ora le caselle rinascono
  una volta quando arrivano (`TableBlockContent`)
- **Provato sul telefono** con una pagina di prova (toggle con due figli,
  tabella 2×2, sottopagina con copertina, icona): copia della riga A
  accanto all'originale, copia dentro la riga B mai aperta scelta dalla
  ricerca, copia del database intero in fondo al menu principale. Nel
  database: nessuna pagina richiamata da due posti, nessun file immagine
  condiviso, figli dei toggle nella pagina giusta, celle copiate,
  `PRAGMA foreign_key_check` vuoto, nessun crash. Il testo nella tabella
  resta dopo essere usciti e rientrati. Poi il database del telefono è
  tornato com'era prima delle prove

### 🆕 v0.39 — 23/09/2026 · 15:06 — Icone per le pagine dei database
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Le pagine contenute in un database (le righe)
  hanno la loro icona, e **si vede nel database accanto al nome**: nella
  tabella, nell'elenco, nelle schede della bacheca, nelle barre del
  calendario e della linea temporale, e fra le righe senza data. Una riga
  senza icona parte dal margine, senza segnaposto
- **Si mette e si toglie in due modi**: tenendo premuto sul nome della riga,
  dalla voce **Icon** in cima alle azioni (dice "Change" o "None"), oppure
  dentro la pagina, toccando l'icona. È la stessa finestra di pagine e
  database: Upload, Link, Remove. Se la riga non era mai stata aperta come
  pagina, la pagina nasce in quel momento: l'icona sta nella pagina. Con il
  database bloccato o nel cestino la voce è spenta
- **La pagina di una riga nasce senza icona**, come su Notion: al posto del
  foglietto 📄 fisso c'è **"Add icon"** accanto a "Add cover". Prima
  "Remove" toglieva l'immagine ma lasciava il 📄, quindi un'icona tolta non
  si poteva davvero togliere. Le altre pagine tengono la loro emoji
- Le icone arrivano da una query che segue insieme righe e pagine
  (`DatabaseDao.observeRowIcons`): cambiata dentro la pagina, l'icona si
  aggiorna nel database senza riaprirlo. Il salvataggio scrive solo la
  colonna dell'icona (`PageDao.setIconImage`), e l'immagine di prima viene
  tolta dalla cartella
- **Provato sul telefono** sulla riga "A" della tabella, nei due sensi:
  messa dal menu della riga e tolta dentro la pagina, poi messa da "Add
  icon" e tolta dal menu della riga. Ogni volta l'icona compare e sparisce
  sia nella tabella sia in testa alla pagina, il menu passa da "None" a
  "Change", il file dell'immagine nasce in `page-images` e viene cancellato
  al Remove, e nel database `iconImage` torna vuoto. Nessun crash. Le altre
  viste (bacheca, elenco, calendario, linea temporale) usano lo stesso
  pezzo ma non le ho aperte

### 🆕 v0.38 — 23/09/2026 · 14:14 — La barra laterale: preferiti, albero, ricerca, cestino, avvio e impostazioni
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Barra laterale** che scorre da sinistra sopra la
  pagina, aperta dal pulsante con le tre lineette: stesso cerchio grigio
  semitrasparente dei tre puntini, sospeso sopra la copertina, e accanto
  a indietro nelle sottopagine (`FloatingPageButton`). Nel database a
  schermo intero sta nella barra in alto, accanto a indietro. Si chiude
  toccando fuori, col gesto indietro o trascinandola; **non si apre
  trascinando dal bordo** perché da lì parte il gesto indietro del
  telefono. È un filo più scura delle pagine (`SidebarBackground`)
- **L'ordine delle voci è quello chiesto**: Widgets (titolo e spazio, da
  riempire — riempito il 24/09/2026, vedi "Widget nella barra laterale"), Notebook grigio in una fascia di un altro colore
  (`NotebookBand`), Main menu con la casetta e sotto un divisore, Favorite
  pages, l'albero delle pagine, Search, Startup window, Backup e
  Notifications grigie, Trash, Import/Export/Connections grigie, Settings.
  Tutte le voci della barra sono tradotte (vedi sotto)
- **[Nuova funzionalità]** **[📱 Mobile]** **L'albero delle pagine**, come Notion ed Esplora
  file: parte dal menu principale (aperto), ogni pagina ha la sua icona e
  il triangolino **solo se dentro c'è qualcosa**. Toccando il triangolino si
  apre e chiude; toccando il nome si va **dritti alla pagina**, senza
  passare da quelle che la contengono, e sotto resta solo il menu
  principale, così "indietro" riporta lì. La pagina aperta è evidenziata
- L'albero **non sta in una colonna "genitore"**: vive nei collegamenti
  dentro le pagine, nell'ordine del testo (anche dentro i toggle), e per i
  database nelle righe. Le righe mai aperte come pagina ci sono lo stesso:
  la loro pagina nasce al primo tocco, come col pulsante OPEN. Si carica
  un ramo alla volta e si ricarica a ogni apertura della barra. Un
  collegamento all'indietro (una pagina che richiama una delle sue
  antenate) si mostra ma non si riapre, altrimenti la fila non finirebbe
- **[Nuova funzionalità]** **[📱 Mobile]** **Pagine preferite**: l'elenco, una sotto
  l'altra, con data e ora di aggiunta. Si ordina per data di aggiunta o per
  nome, crescente o decrescente, e la scelta resta salvata. Nuova colonna
  `favoritedAt` (**migrazione 23→24**); i preferiti che c'erano già non
  hanno una data vera e lo dicono ("Date added unknown") invece di
  inventarla. Le pagine nel cestino non compaiono fra i preferiti
- **[Nuova funzionalità]** **[📱 Mobile]** **Search** rifatta: cerca nei titoli e, con
  "Search in content" (acceso di partenza), nel testo dei blocchi, nelle
  tabelle semplici e nei nomi delle righe dei database. Ogni risultato
  mostra **il pezzo di frase con la parola evidenziata**. "Search in trash"
  include le pagine buttate, segnate "In trash". Le altre otto opzioni ci
  sono, grigie; "Search in titles" è grigia **ma accesa**, perché nei
  titoli si cerca sempre
- **[Nuova funzionalità]** **[📱 Mobile]** **Trash**: le pagine buttate, con data e giorni
  che restano. Aperta, una pagina del cestino è **in sola lettura** e ha in
  cima una barra con **Restore** — torna **in fondo al menu principale**, o
  alla sua riga se era la pagina di una riga — e **Delete permanently**,
  con conferma. In alto a destra "Empty trash". Due regole: **tenere le
  pagine 30 giorni**, oppure **svuotare automaticamente**, cioè cancellarle
  al prossimo avvio dell'app. La pulizia avviene all'avvio
- La cancellazione definitiva porta via anche **le pagine contenute** (i
  collegamenti dentro, le righe se è un database, e così via in giù), ma
  **solo quelle richiamate soltanto da lì**: una sottopagina collegata
  anche da un'altra parte resta. Con le pagine se ne vanno i file di icone
  e copertine
- La conferma di "Move to trash" ora dice che la pagina va nel cestino e
  **quando se ne andrà** secondo la regola scelta (prima diceva che un
  cestino non c'era)
- **[Nuova funzionalità]** **[📱 Mobile]** **Startup window**: l'app parte dall'ultima pagina
  aperta, dal menu principale o da una pagina scelta da un elenco. Se
  quella pagina non c'è più o è nel cestino si parte dal menu principale.
  Una rotazione o un cambio di tema non rifanno il salto
- **[Nuova funzionalità]** **[📱 Mobile]** **Settings**, salvate sul telefono
  (`AppSettings`, `SharedPreferences`, non nel database: sono scelte del
  telefono, non contenuto):
  - **Font** e **Font size** grigie
  - **Theme**: Dark, Light, System. I grigi scritti a mano in circa 150
    punti ora **scelgono da soli la versione chiara o scura**
    (`Color.kt`): i nomi sono rimasti quelli di prima, e in chiaro
    `NotionWhite` è il colore del testo. Le icone della barra del telefono
    diventano scure sul tema chiaro
  - **App sounds**: suono delle notifiche scelto **con l'esplora file del
    telefono** (il permesso di leggerlo resta anche dopo un riavvio), o
    quello predefinito, con un tasto per ascoltarlo; notifiche spente per
    sempre, o per 1 ora / 8 ore / 1 giorno / 1 settimana. **Le notifiche
    non esistono ancora**: le scelte si salvano e varranno quando ci
    saranno, e la pagina lo dice
  - **Animations**: On, Reduced, Off. Valgono per l'apertura della barra
    laterale e per le dissolvenze fra una schermata e l'altra (piene 700
    ms come prima, ridotte 120, nessuna)
  - **Number format**: 1.000.000 o 1,000,000. Le celle numeriche lo
    mostrano quando non ci si scrive; **mentre si scrive il numero resta
    grezzo**, altrimenti i separatori si sposterebbero sotto le dita. Il
    valore salvato non cambia mai
  - **Date format**: i nove formati chiesti. "Full date" la scrive ogni
    lingua a modo suo ("September 23, 2026", "23 settembre 2026"). Vale
    per le celle dei database, la cronologia, preferiti e cestino
  - **Language**: English, Italiano, Deutsch, Français, Español, 中文, 한국어,
    日本語, ognuna scritta nella sua lingua. Traduce **le voci dell'app**:
    barra laterale, menu "/" e "+", barra degli strumenti, menu ⋯,
    database (tipi di proprietà, viste, filtri, ordinamento, tag, date che
    si ripetono, impostazioni), selettore colori, cronologia, finestre di
    conferma. Nel menu "/" si cerca sia col nome tradotto sia con quello
    inglese
  - **Time zone**: Berlino di partenza, che segue l'ora legale; si cerca
    fra tutti i fusi "Regione/Città", con lo scarto da UTC di adesso
- **Le traduzioni** stanno in `ui/i18n/` (`Strings`, `EditorStrings`,
  `DbStrings`): **ogni voce con le sue otto traduzioni nella stessa riga**,
  così una voce nuova si scrive una volta sola e una lingua dimenticata
  non compila. **I nomi delle proprietà**: una proprietà lasciata senza
  nome prende quello inglese del tipo ("Number", "Date") e **si mostra
  tradotto**; un nome scritto dall'utente resta com'è. Le proprietà nuove
  senza nome continuano a salvare il nome inglese, così seguono sempre la
  lingua
- **Limiti dichiarati**: le parole degli elenchi numerati "a parole"
  ("One", "First") restano inglesi, sono uno stile di numerazione; gli
  errori dello scaricamento delle immagini ora sono generici (prima erano
  in italiano e dettagliati); le **date senza ora** sono salvate alla
  mezzanotte del fuso in cui sono nate, quindi scegliendo un fuso molto a
  ovest di Berlino quelle vecchie potrebbero comparire un giorno prima;
  le animazioni dentro le pagine (scorrimenti, finestre) non seguono
  ancora l'impostazione; le pagine che **nessun collegamento richiama**
  non compaiono nell'albero (sul telefono ce ne sono due, un "Esempio" e
  un "Untitled": si ritrovano con la ricerca)
- **Verificato sul telefono**: barra laterale aperta e chiusa; albero con
  le righe del database, tocco su una riga → pagina aperta direttamente;
  preferito aggiunto → in elenco con data e ora; ricerca → pagina trovata
  per contenuto con la parola evidenziata, pannello delle opzioni; pagina
  buttata → nel cestino con "30 days left", aperta con la barra, Restore;
  italiano → impostazioni, barra laterale, menu "/" e intestazioni del
  database tradotti; tema chiaro su pagina, barra laterale e impostazioni;
  numeri col separatore delle migliaia; "Last visited page" → riavviata
  l'app, riparte dal database aperto per ultimo. Nessun crash. Prove
  ripulite e impostazioni rimesse come le aveva l'utente

### ✏️ v0.37.1 — 23/09/2026 · 09:28 — Il menu "/" filtra davvero
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** **Scrivendo "/d" il menu spariva alla "d".** Il menu "/"
  si chiudeva a ogni tocco fuori da sé, e per Android anche **la
  tastiera è "fuori"**: è un'altra finestra. Quindi la prima lettera
  battuta dopo la barra lo chiudeva, e il filtro non si è mai visto
  lavorare. Nelle prove era sfuggito perché barra e lettere arrivavano da
  `adb input text`, che non tocca lo schermo; battendo la "d" sul tasto
  vero il menu è sparito subito. Tolta la chiusura al tocco fuori
  (`dismissOnClickOutside = false`), come per il menu del "+": si chiude
  lo stesso scegliendo una voce, togliendo la barra, portando il cursore
  prima di lei o su un'altra riga, con uno spazio, o perdendo il fuoco
- **[Modifica]** **[📱 Mobile]** **Con un filtro, prima le voci che somigliano
  di più.** Il filtro teneva ogni voce che *contenesse* le lettere,
  nell'ordine del catalogo: con "/d" erano sedici, e "Divider" veniva
  settima, sotto il bordo del riquadro. Ora vince chi **comincia** così,
  poi chi ha una **parola** che comincia così ("/list" trova "Bulleted
  list", "/do" trova "To-do list"), e solo dopo chi le contiene; a parità,
  le voci che funzionano davanti a quelle ancora spente. Con un filtro le
  famiglie (Basic blocks, Media, Database) non si separano più, altrimenti
  con "/data" le voci di base passerebbero comunque davanti. A ogni
  lettera la lista ricomincia dall'alto
- **[Bug fix]** **[📱 Mobile]** **Scegliendo Divider la pagina scivolava fino al database
  e la tastiera accennava a chiudersi.** Ogni gruppo di righe di testo è
  una casella sola, riconosciuta dalla sua prima riga: se la prima riga
  diventa un divisore, la casella cambia identità e viene distrutta col
  cursore dentro. Per un attimo il fuoco non era di nessuno — nel video:
  barra sparita, tastiera che scende e risale, pagina che scorre. Adesso,
  come già per il toggle, il fuoco si posa prima sul campo invisibile e
  da lì passa alla riga nuova sotto il divisore. Vale anche dal "+"
- **[Bug fix]** **[📱 Mobile]** **La barra resta mentre il cursore cambia riga.** Nei
  passaggi dal campo invisibile (divisore, toggle, casella che torna
  testo) nessuna riga ha il fuoco per un centinaio di millisecondi, e la
  barra spariva e tornava. Ora, perso il fuoco a tastiera aperta, aspetta
  tre decimi di secondo prima di andarsene (`BAR_HANDOFF_GRACE_MS`); a
  tastiera chiusa se ne va subito come prima. Il prezzo: passando al
  titolo della pagina la barra resta un attimo in più
- **[Bug fix]** **[📱 Mobile]** **Il divisore si portava dietro il comando.** Veniva
  salvato per intero dalla copia della riga che aveva in mano la
  schermata, che aveva ancora dentro "/d": invisibile, ma nel database.
  Stesso difetto della "/" nei toggle, stesso rimedio: si scrive solo il
  tipo (`turnIntoDividerWithLineBelow`)
- **Verificato sul telefono** battendo sui tasti veri: "/" poi "d" → il
  menu resta aperto, **Divider in cima**; con la "D" maiuscola che la
  tastiera mette da sola a inizio riga, uguale. Toccato Divider su una
  riga in fondo allo schermo, registrando: barra e tastiera ferme in tutti
  i 36 fotogrammi, cursore sulla riga nuova, nessuna chiusura della
  tastiera nel registro, divisore **senza testo** nel database. Dopo le
  prove la pagina è stata rimessa com'era dalla copia fatta prima
- **Da decidere, non toccato**: scegliendo Divider su una riga che ha già
  del testo, **tutta la riga** diventa divisore e il testo resta nascosto
  dentro. Su Notion il divisore va invece sotto e il testo resta

### 🔧 v0.37.0.1 — 23/09/2026 · 09:12 — Gli scatti dentro i toggle
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** **Andando a capo dentro un toggle la riga appena scritta
  spariva per un attimo e il cursore scendeva di colpo.** Le righe dentro
  un toggle sono campi separati, e l'Invio mette la riga nuova **sopra**
  col testo prima del cursore (è quello che tiene ferma la tastiera):
  il campo col cursore passava subito al testo dopo il cursore, ma la
  riga di sopra arrivava solo quando il database l'aveva scritta e
  rimandata indietro. Misurato: 29 ms, e nel video dello schermo c'è un
  fotogramma in cui la lettera appena battuta **non c'è più da nessuna
  parte**, e in quello dopo ricompare una riga più su
- Ora la riga nuova entra nella lista in memoria **nello stesso istante**
  in cui il campo cambia testo (`showBeforeSaved`), e il database la
  conferma dopo. Rifatto il video: da "CB" a "C / B" in un fotogramma,
  senza passaggi intermedi
- **Non basta aggiungerla una volta**: una lettura del database partita
  un attimo prima dell'Invio può arrivare dopo, senza la riga, e
  toglierla di nuovo — e se quella riga ha già il cursore, togliere la
  sua casella di scrittura fa chiudere l'app. La modifica resta quindi
  in sospeso e **si riapplica a ogni lettura che non contiene ancora la
  riga nuova**; la prima che la contiene la chiude. Se il database
  rinuncia a scriverla (il blocco nel frattempo non c'è più) la riga
  viene tolta, e annulla/ripristina buttano via quelle in sospeso
- Stesso trattamento per l'Invio nel **titolo** del toggle e per il
  segnaposto *Empty toggle*: la riga nuova si vede subito. Il cursore ci
  arriva invece **come prima**, dopo che il database l'ha scritta:
  mandarcelo prima vorrebbe dire scrivere lettere in una riga che nel
  database ancora non esiste, e perderle
- **[Bug fix]** **[📱 Mobile]** **Salendo col backspace da una riga vuota la barra sopra
  la tastiera spariva e tornava.** Il fuoco passava dal campo invisibile,
  la riga veniva cancellata e 60 ms dopo il cursore andava su quella di
  sopra: per tutto quel tempo nessuna riga aveva il fuoco, e la barra si
  vede solo se ce n'è una. Nel video: **quattro fotogrammi, cento
  millisecondi, senza barra**, con la tastiera sempre aperta
- Ora il cursore passa **direttamente** da questa riga a quella di sopra
  (nel registro: uno perde il fuoco e l'altro lo prende nello stesso
  millisecondo), e la riga si cancella **dopo**, quando non ha più il
  cursore. Il cursore va in fondo alla riga di sopra: è messo prima di
  prendere il fuoco, altrimenti lo si vedrebbe saltare
- La tastiera Samsung manda lo stesso backspace **due o tre volte** a due
  millisecondi di distanza (con la tastiera vera, non solo con `adb`):
  la riga in uscita si ricorda, e i doppioni non la tolgono una seconda
  volta
- **[Bug fix] mio, preso prima di consegnare**: nel primo giro la riga
  vuota **non veniva mai cancellata**. `withTimeoutOrNull` restituisce
  `null` sia quando scade il tempo sia quando il blocco restituisce
  `null` — ed è proprio quello che diventa la richiesta di fuoco appena
  servita. Visto rileggendo il database dopo la prova, non dallo
  schermo, dove il cursore saliva correttamente
- **Verificato sul telefono**, registrando lo schermo e guardando i
  fotogrammi uno per uno: Invio in mezzo a una riga dentro il toggle →
  nessun fotogramma con la riga mancante; backspace su una riga vuota →
  barra presente in tutti i fotogrammi, riga sparita dal database,
  lettera battuta dopo finita in fondo alla riga di sopra; Invio nel
  titolo → riga nuova visibile subito, backspace → riga tolta, cursore
  in fondo al titolo. Dopo le prove la pagina è stata rimessa com'era
  dalla copia del database fatta prima

### 🆕 v0.37 — 23/09/2026 · 00:49 — Icone anche sui database
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Un database può avere la sua icona, prima del
  nome: più piccola quando sta dentro una pagina, grande quanto quella
  delle pagine quando è aperto a schermo intero. Per metterla c'è la
  voce **Icon** nelle impostazioni del database; quando c'è, si cambia
  o si toglie anche toccandola
- **Stessa finestra delle pagine** (Upload / Link / Remove), non una
  copia: `PageImageSheet` e `PageImage` sono stati resi condivisibili
  fra le due schermate. Togliendo o sostituendo l'icona, il file vecchio
  viene cancellato come per le pagine
- **"Nessuna icona" vuol dire proprio niente accanto al nome.** Le
  pagine hanno un'emoji di riserva (📄) scritta nel campo `icon`; per un
  database quella non si usa, altrimenti ogni database avrebbe avuto un
  foglietto davanti senza che nessuno l'avesse chiesto. Conta solo
  `iconImage`, e senza icona il nome parte dal margine come prima
- Il **collegamento** a un database trasformato in pagina mostra la sua
  icona, se c'è; se non c'è, il segno della tabella invece del 📄
- Con la pagina bloccata ("Lock page") l'icona non si cambia: fa parte
  del contenuto
- **Verificato sul telefono** con una copia di un'immagine già presente
  (per non toccare la copertina): icona visibile accanto al nome,
  toccata → la finestra offre anche Remove, Remove → icona sparita e
  **file della copia cancellato**, copertina intatta

### ✏️ v0.36.1 — 23/09/2026 · 00:37 — La copertina sale fin sotto la barra del telefono
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Tolta la barra in alto delle pagine: occupava
  una fascia vuota fra l'orologio e la copertina solo per ospitare due
  icone. Ora la copertina comincia **subito sotto l'ultimo pixel della
  barra del telefono**, e indietro e tre puntini stanno **sospesi sopra**
  in un cerchietto grigio semitrasparente (`FloatingPageButton`,
  `FloatingButtonBackground`)
- **Semitrasparente e non pieno**, di proposito: pieno sarebbe una toppa
  sulla copertina, del tutto trasparente sparirebbe sulle copertine
  chiare. Così si legge su qualunque immagine e la lascia intravedere
- **Senza copertina** in cima resta la fascia dei pulsanti
  (`FLOATING_BAR_CLEARANCE`), altrimenti il pulsante indietro finirebbe
  sopra l'icona della pagina e su "Add cover"
- Ognuno dei due pulsanti è grande **solo quanto il suo cerchio**:
  intorno la pagina si tocca come prima
- Il database aperto a schermo intero ha ancora la barra classica: lì
  non c'è una copertina da far salire

### 🆕 v0.36 — 23/09/2026 · 00:29 — Turn into page / Turn into database
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Nelle impostazioni di un database dentro una
  pagina c'è **Turn into page**: il database smette di essere mostrato
  lì e diventa un collegamento (📄 col suo nome) che lo apre a schermo
  intero. Toccata la voce, il database **si apre subito** a schermo
  intero, che è come lo si vedrà da lì in poi
- **[Nuova funzionalità]** **[📱 Mobile]** Nel menu dei tre puntini del database a
  schermo intero c'è **Turn into database**: torna a vedersi dentro la
  pagina che lo richiama, e si **torna indietro** su quella pagina, dove
  lo si ritrova aperto come prima
- **Il database non viene toccato**, in nessuno dei due versi: righe,
  colonne, vista, ordinamento e filtro restano identici. Cambia solo
  **il blocco che lo mostra** nella pagina — `DATABASE_LINK` diventa
  `PAGE_LINK` e viceversa — scritto con una query sola sulla colonna del
  tipo, senza riscrivere nient'altro
- **Turn into database compare solo quando serve**: se il database è
  richiamato come pagina da qualche parte. Aperto a schermo intero
  dall'icona ↗ è ancora dentro la sua pagina, e lì la voce non c'è.
  Si ricontrolla a ogni apertura del menu, perché nel frattempo può
  essere cambiato dall'altra schermata
- **[Bug fix]** **[📱 Mobile]** Un collegamento a pagina che punta a un database lo
  apre **come database**. Prima contava solo il tipo del blocco, e un
  collegamento a pagina apriva sempre l'editor di testo: su un database
  avrebbe mostrato un foglio vuoto al posto delle righe
- **Verificato sul telefono** sul database della pagina principale:
  Turn into page → aperto a schermo intero; indietro → nella pagina è
  un collegamento; toccato → si apre come database; Turn into database
  → di nuovo nella pagina principale, aperto, con le stesse righe; e
  aperto dall'icona ↗ la voce non compare

### ✏️ v0.35.2 — 23/09/2026 · 00:18 — I toggle, rifatti
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** **Aprire o chiudere un toggle cancellava tutto quello
  che conteneva.** Il salvataggio dei blocchi usava
  `@Insert(onConflict = REPLACE)`, e in SQLite `REPLACE` non aggiorna:
  **cancella la riga e ne inserisce una nuova**. La cancellazione fa
  scattare il vincolo a cascata verso i figli (`parentBlockId`), quindi
  ogni salvataggio di un blocco con dei figli li distruggeva. Aprire o
  chiudere un toggle lo salva; anche spostarlo su o giù e cambiargli
  tipo. Visto nel database: cinque righe dentro un toggle, un tocco
  sulla freccia, zero righe. Ora è `@Upsert`, che inserisce se il blocco
  non c'è e **aggiorna al suo posto** se c'è: la riga non viene mai
  cancellata e i figli restano. Le altre `REPLACE` del progetto sono
  state controllate: creano sempre righe con id nuovi, quindi non
  toccano mai una riga esistente
- **[Bug fix]** **[📱 Mobile]** **La "/" che restava dentro ogni toggle creato dal menu
  "/".** Si scrive `/toggle`, il campo toglie comando e barra e salva;
  subito dopo il cambio di tipo salvava **il blocco intero** con la
  copia che la UI aveva in mano prima — e riscriveva il testo vecchio,
  barra compresa. Ora `updateBlockType` scrive **solo la colonna del
  tipo**, come già facevano le altre modifiche di un campo solo
- **[Bug fix]** **[📱 Mobile]** **La freccia**: da chiuso un triangolino verso destra,
  da aperto verso il basso, come su Notion. Prima erano le frecce a
  parentesi di Material, e da aperto puntava in su
- **[Bug fix]** **[📱 Mobile]** **In un toggle nuovo non si poteva scrivere.** Aperto e
  vuoto non mostrava niente, e il pulsante "+ Add inside" compariva
  solo se c'era già almeno un figlio. Ora compare la riga grigia
  *Empty toggle. Tap to add a block inside.*; con dei figli non c'è più
  nessun pulsante in più, si aggiunge con l'Invio come su Notion
- **[Bug fix]** **[📱 Mobile]** **L'Invio nel titolo faceva uscire il titolo dal
  toggle.** Veniva trattato come un paragrafo: la riga nuova nasceva
  sopra col testo prima del cursore — cioè col titolo — e il toggle
  restava vuoto sotto. Ora (`enterInToggle`) a toggle **aperto** la riga
  nuova è il **primo figlio**; a toggle **chiuso** è un **altro toggle**
  subito sotto, perché dentro non si vedrebbe
- **[Bug fix]** **[📱 Mobile]** **Dopo un Invio dentro il toggle le lettere finivano
  nella riga sbagliata**: "1", Invio, "2" dava "12" nella riga di
  sopra. Le righe dentro un toggle stavano in una colonna **senza
  `key`**, quindi Compose le riconosceva dalla posizione: la riga nuova
  nasce sopra (è quello che tiene ferma la tastiera, vedi
  `splitBlockAt`), la riga col cursore scendeva di un posto ma il suo
  campo restava dov'era e si ritrovava a mostrare la riga nuova. Trovato
  col registro `NOTE`, che mostrava la lettera arrivare a un campo che
  non aveva il fuoco. Il testo di primo livello non aveva il problema
  perché la lista che lo contiene le chiavi le ha già
- **[Modifica]** **[📱 Mobile]** **Backspace su una riga vuota dentro un
  toggle la toglie** e porta il cursore su quella sopra, o sul titolo
  se era la prima (`removeEmptyNestedLine`). Prima non succedeva niente
  e una riga vuota creata per sbaglio restava lì
- **[Modifica]** **[📱 Mobile]** Creato un toggle dal menu "/" o dal "+", **il
  cursore va sul suo titolo** e la tastiera resta su: prima si chiudeva
  e bisognava ritoccare la riga. Il fuoco si posa per un istante sul
  campo invisibile, perché il campo condiviso in cui si stava scrivendo
  — se quella era la sua unica riga — muore col cursore dentro
- **[Bug fix]** **[📱 Mobile]** Le righe dentro un toggle sono **allineate sotto il testo
  del titolo** (`NESTED_INDENT` = freccia + stacco). Prima il rientro
  era un 20 fisso, quattro punti più corto, e stavano storte a sinistra
- **[Modifica]** **[📱 Mobile]** **Un toggle si toglie col backspace**, come
  su Notion: all'inizio del titolo **torna testo normale**, col suo
  titolo, e le righe che conteneva **escono e restano subito sotto**,
  allo stesso livello (`unmakeToggle`, `unnestAndConvert`). Un secondo
  backspace lo unisce alla riga sopra. Prima non succedeva niente, e un
  toggle col titolo scritto non si poteva togliere in nessun modo: il
  cestino della barra compare solo sulle righe vuote. Le righe interne
  non si cancellano di proposito — un backspace che si porta via mezza
  pagina nascosta in un toggle chiuso sarebbe il modo peggiore di
  perdere del testo
- Il cursore resta **a inizio riga** dopo la trasformazione: il campo
  condiviso, quando riceve il fuoco, ora rispetta anche la posizione
  richiesta (`pendingCaret`) invece di mettersi sempre in fondo. In
  fondo, il backspace successivo avrebbe mangiato l'ultima lettera del
  titolo invece di unire la riga a quella sopra
- **[Bug fix]** **[📱 Mobile]** **Un toggle nuovo nasce aperto.** Lo stato aperto/chiuso
  sta nel blocco e ci restava anche cambiando tipo: una riga che era
  stata un toggle chiuso, rifatta toggle, rinasceva chiusa, e l'Invio
  nel titolo creava un secondo toggle sotto invece di scendere dentro
- **[Bug fix]** **[📱 Mobile]** Aprire/chiudere un toggle scrive **solo quella colonna**
  (`setExpanded`) e parte dalla versione più recente del blocco: prima
  salvava la copia che la freccia aveva in mano, che poteva avere un
  titolo più vecchio di quello appena scritto
- **[Bug fix]** **[📱 Mobile]** Il cestino della barra sulle righe vuote ora posa prima
  il fuoco sul campo invisibile: la riga che sparisce aveva il cursore
  dentro, che è il modo in cui l'app si chiudeva
- **Un dubbio aperto, dichiarato**: nel primo tentativo, trasformando in
  testo un toggle **vuoto**, è sparita anche la riga vuota subito sotto.
  Rifatto col registro acceso, non è successo; il registro mostra però
  che il backspace simulato con `adb` arriva al campo **due volte**, che
  è il sospetto principale. Se capita con la tastiera vera, è da
  riprendere. *Aggiornamento del 23/09/2026*: premendo il tasto della
  tastiera Samsung vera il backspace arriva **due o tre volte** anche
  lì; per la riga vuota dentro un toggle ora i doppioni sono scartati
  (vedi "Gli scatti dentro i toggle")
- **Limiti dichiarati**: il backspace a inizio di una riga **con del
  testo** dentro un toggle non la fonde con quella sopra (sono due campi
  separati, è un lavoro a parte); le righe dentro un toggle restano
  campi a sé, non il testo condiviso, quindi non si selezionano due
  righe insieme
- **Verificato sul telefono**: `/toggle` → toggle senza "/", cursore
  sul titolo; titolo, Invio → prima riga dentro, allineata; `412`,
  Invio, `5`, Invio, `6` → tre righe separate; backspace su riga vuota
  → sparisce, cursore in fondo a quella sopra; due righe dentro,
  chiuso e riaperto → **entrambe ancora nel database**

### ✏️ v0.35.1 — 22/09/2026 · 19:36 — Come si chiude il menu del "+"
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Due modi, e sono quelli che vengono da sé:
  **ritoccare il "+"**, che fa da interruttore, oppure **toccare una
  parte vuota della pagina**. La tastiera resta dov'è in entrambi i
  casi
- **[Bug fix]** **[📱 Mobile]** **Toccare la pagina non sposta più il cursore.** Il
  menu si chiudeva già da solo al primo tocco fuori, ma quel tocco
  arrivava **anche alla pagina sotto**, che portava il cursore altrove:
  si tornava a scrivere e ci si ritrovava da un'altra parte
- Per risolverlo non bastava ignorare il tocco "mentre il menu è
  aperto": fra la chiusura automatica e il tocco sulla pagina **non era
  detto chi arrivasse prima**, quindi a volte il menu risultava già
  chiuso e il cursore si spostava lo stesso. La chiusura automatica è
  stata tolta (`dismissOnClickOutside = false`) e la fa la pagina:
  decidendo noi, l'ordine è sempre lo stesso
- **E non bastava nemmeno intercettare il tocco dalla lista**: quel
  gestore vede solo quello che i figli non hanno già preso, quindi
  toccando **una riga di testo** il tocco se lo prendeva il campo di
  scrittura — cursore spostato, menu ancora aperto. Serve un **velo
  invisibile** steso sopra la pagina mentre il menu è aperto, che
  intercetta prima di chiunque: chiude il menu e non lascia passare
  altro. Copre solo la pagina e non la barra, così il "+" resta un
  interruttore vero
- `focusable = false` sul menu **resta**: un menu che prende il fuoco fa
  chiudere la tastiera, e qui la tastiera deve restare dov'è
- **Strade scartate, provate e tolte il 22/09/2026**: (1) far chiudere
  il menu al **gesto indietro** — a tastiera aperta quel gesto lo
  prende la tastiera e all'app non arriva affatto, quindi ci volevano
  due gesti; (2) chiudere la tastiera all'apertura del menu per
  aggirare il punto (1) — funzionava, ma vedersi cadere la tastiera
  sotto le dita ogni volta che si apre un elenco è peggio del problema
  che risolveva; (3) chiudere menu e tastiera **insieme** col gesto
  indietro — un gesto solo, ma portava via anche la barra, che non si
  era chiesto di chiudere
- **Verificato sul telefono** con la prova che non lascia dubbi:
  scritto `Prova` col cursore in fondo, aperto il "+", toccata la
  **riga di testo** (non il vuoto), e poi battuta una lettera. Se il
  cursore si fosse spostato sarebbe finita all'inizio; è finita in
  fondo — `ProvaXY` — e il menu si era chiuso

### 🆕 v0.35 — 22/09/2026 · 19:06 — Colore del testo e colore dietro al testo (il pennello)
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Un **pennello nella barra, accanto ad Aa**,
  apre un selettore di colore da cui si tinge il testo selezionato o la
  fascia dietro di lui. Due riquadri in cima, *Testo* e *Sfondo*, per
  passare dall'uno all'altro senza chiudere niente
- **Accanto ad Aa e non dentro**, di proposito: il colore non è un
  interruttore come grassetto e corsivo, apre una finestra sua, e
  metterlo in fila con quelli avrebbe promesso un comportamento che non
  ha
- **[Nuova funzionalità]** **[📱 Mobile]** Il selettore è quello di Windows, che è
  quello chiesto guardando: riquadro grande con lo spettro (tinta da
  sinistra a destra, saturazione dall'alto in basso), striscia della
  luminosità a destra dal bianco al nero, i tre riquadri
  Rosso/Verde/Blu e il riquadro **Hex**, dove il codice si scrive o si
  incolla a mano — è il modo in cui i colori si copiano da fuori
- **Una sola verità: tinta, saturazione, luminosità.** I tre modi di
  scegliere sono sempre d'accordo perché leggono e scrivono la stessa
  terna. Tenere invece RGB come verità vorrebbe dire ricalcolare la
  posizione del pallino a ogni giro, e **sui grigi la tinta non
  esiste**: il pallino schizzerebbe a sinistra da solo ogni volta che
  si passa per un grigio
- **Mentre si scrive nel riquadro Hex il colore si muove solo quando
  quello che c'è scritto è un colore vero**: a metà di `#FF00` non c'è
  ancora niente da mostrare, e saltare a un colore a caso a ogni
  lettera sarebbe stato peggio che non muoversi
- **[Bug fix]** **[📱 Mobile]** **La selezione si mette da parte prima di aprire la
  finestra.** Il pannello che sale dal basso toglie il fuoco al campo
  di scrittura, e un campo che perde il fuoco **chiude la selezione**
  riducendola a un cursore: al momento di applicare il colore non
  c'era più niente da colorare, e non succedeva nulla. Visto sul
  telefono al primo tentativo. Ora i campi segnalano la selezione man
  mano (`reportSelection`) e il pennello ne fa una copia nell'istante
  in cui lo si tocca. I comandi della barra Aa non hanno mai avuto
  questo problema perché non aprono niente
- **[Bug fix]** **[📱 Mobile]** I blocchi isola si riallineavano al database **solo
  quando cambiava il testo**, non la formattazione: il colore veniva
  salvato ma la riga restava com'era fino alla battuta successiva
- **[Bug fix]** **[📱 Mobile]** **Passando a "Background" il pallino non si muoveva
  più.** Lo stato del colore era legato anche al bersaglio
  (`remember(initialHex, background)`), quindi toccando il riquadro
  "Background" `remember` fabbricava uno stato **nuovo** — mentre i
  gestori dei tocchi, che una `pointerInput` si tiene congelati dalla
  prima composizione, continuavano a scrivere in quello vecchio, ormai
  buttato via. Da fuori sembrava che il riquadro dei colori avesse
  smesso di rispondere. Due correzioni insieme: lo stato si crea **una
  volta sola** all'apertura, e le lambda dei tocchi si tengono fresche
  con `rememberUpdatedState`. Cambiando bersaglio il colore adesso
  **resta quello che è**, che è anche più comodo — di solito si vuole
  lo stesso colore, o una sua correzione
- **[Bug fix]** **[📱 Mobile]** Il pallino si tiene **dentro i bordi** del riquadro:
  sul rosso pieno cadeva nell'angolo in alto a sinistra e se ne vedeva
  un quarto, cioè sembrava non esserci
- **Nel testo il colore sta sotto lo spoiler**: coperto, il velo deve
  vincere su tutto, altrimenti si leggerebbe il testo colorato
  attraverso
- **Un esadecimale che non si riesce a leggere viene ignorato** invece
  di far saltare il disegno: quei codici li scrive l'utente a mano, e
  una lettera di troppo non deve far sparire il testo
- **Nessuna migrazione**: `color` e `background` sono due campi con
  valore predefinito dentro il JSON del testo, come `spoiler`
- **Verificato sul telefono**: parola selezionata, pennello, colore
  preso dallo spettro, `Applica` — testo verde `#26D931` a schermo e
  nel database; poi lo stesso su *Sfondo*, e `Nessun colore` che toglie
  solo quello lasciando il testo colorato

### 🆕 v0.34 — 22/09/2026 · 18:36 — Il menu dei tre puntini di una pagina, e Updates
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** La **lente della ricerca sparisce** dalla
  barra in alto, su ogni pagina e sulla principale, e al suo posto —
  stessa posizione — ci sono i **tre puntini**. Dentro: Favorite,
  Search, Duplicate, Move to, Move to trash, Lock view, Lock page,
  Import, Export, Updates. Le cose che si fanno a una pagina intera
  stanno in un posto solo invece di essere sparse fra la barra e le
  impostazioni
- **[Nuova funzionalità]** **[📱 Mobile]** Lo stesso menu c'è anche sul **database
  aperto a schermo intero**: un database lì è una pagina come le altre.
  È un file a sé (`PageOptionsSheet.kt`) proprio perché lo aprono due
  schermate diverse, e due copie di un menu di comandi si separano al
  primo cambiamento
- **Import ed Export sono spente**, come chiesto: si vedono per dire
  che arriveranno. Le voci che invece **non hanno senso** su una certa
  pagina non ci sono affatto — sulla principale niente Favorite,
  Duplicate, Move to o Move to trash, e "Lock view" solo sui database
  (*dal 24/09/2026 Move to sulla principale c'è, grigia*: richiesta 33).
  Sono due cose diverse: spenta vuol dire "non ancora", assente vuol
  dire "non qui"
- **Duplicate** copia blocchi e, per un database, colonne righe e
  celle, e mette la copia **accanto all'originale**. Gli id di blocchi
  e colonne vengono tradotti mentre si copia, altrimenti i figli di un
  toggle e le celle resterebbero appesi all'originale. Le **sottopagine
  non si duplicano**: la copia rimanda alle stesse, perché duplicare a
  cascata mezzo archivio non è quasi mai quello che si vuole.
  *Superato*: vedi "Duplicate: si sceglie dove va la copia" più in alto
  — rimandare alle stesse sottopagine voleva dire che la copia non era
  una copia
- **Move to**: qui l'albero di navigazione non è un campo della pagina
  ma **i blocchi che la richiamano**, quindi spostare vuol dire togliere
  quel collegamento e crearne uno in fondo alla pagina scelta. Fra le
  destinazioni non compaiono i database (il loro contenuto sono righe,
  non blocchi) né le pagine nel cestino.
  *Superato* (24/09/2026, richiesta 33): la scelta ora è l'albero con la
  ricerca, comprese le pagine dei database; vedi "Move to — l'albero con
  la ricerca" più in alto
- **Move to trash** non cancella niente: segna `trashedAt` e toglie i
  collegamenti che la mostravano. Il contenuto resta tutto. **Non c'è
  ancora una schermata del cestino** — finché non c'è, una pagina
  buttata si ritrova solo dal database, e la finestra di conferma lo
  dice invece di far finta di niente
- **Lock page** rende il contenuto **di sola lettura**, non spento:
  `readOnly` e non `enabled = false`, così il testo si seleziona ancora
  e si copia. Vale per titolo, corpo, nomi delle righe e celle, e fa
  sparire la barra sopra la tastiera e il pulsante "New page" — sono
  tutte cose che modificano
- **Lock view** è un'altra cosa e vale solo sui database: le righe si
  modificano ancora, **l'impaginazione no** (vista, ordine, filtro,
  raggruppamento, colonne). I comandi restano al loro posto **spenti**
  invece di sparire: un pulsante che scompare fa pensare a un guasto,
  uno spento dice che è stato messo a riposo apposta. Cancellare invece
  resta possibile — il lucchetto protegge come è messo insieme il
  database, non la sua esistenza
- **[Nuova funzionalità]** **[📱 Mobile]** Una schermata che elenca cosa è stato
  cambiato nella pagina, dalla più recente, nella forma chiesta:
  `22.09.2026 18:34` e sotto il testo di prima **sbarrato**, una
  freccia, il testo di adesso
- **Una voce per sessione di scrittura, non per tasto premuto.** È il
  punto delicato: registrando ogni battuta, "Esmepoi → Esempio"
  diventerebbe sette righe illeggibili. Una sessione dura finché si
  scrive nello stesso blocco e si chiude quando il cursore va altrove,
  quando cambia la struttura della pagina o quando si esce. È **lo
  stesso confine che l'Annulla usa già**, riusato invece di
  inventarne un secondo che prima o poi si sarebbe disallineato
- **[Bug fix]** **[📱 Mobile]** Il solo spostare il cursore, senza scrivere, non
  chiudeva la sessione: la modifica restava in sospeso fino alla
  successiva e la cronologia aveva sempre un pezzo di ritardo. Ora la
  chiude la schermata quando cambia la riga a fuoco, e il titolo la
  chiude quando perde il fuoco
- **Il "dopo" si legge dopo una pausa** (`EDIT_SETTLE_MS`, 400): quello
  che si è appena scritto viaggia verso il database e torna indietro, e
  leggendolo subito la cronologia racconterebbe una correzione troncata
  all'ultima lettera
- **E su uno scope che non muore con la schermata**
  (`PageRepository.recordEditAfterSettling`): l'ultima sessione si
  chiude proprio mentre si esce dalla pagina, e lo scope del ViewModel
  a quel punto è già annullato — quella riga, che è proprio quella che
  si vuole ritrovare, non sarebbe stata scritta mai
- **Il titolo entra nella cronologia** con un id di blocco riservato
  (`PageEditEntity.TITLE_BLOCK_ID`): non è un blocco ma si modifica
  come loro, e dargli un id evita di trattare un `null` come caso a sé
  dappertutto
- **`blockId` non ha un vincolo verso i blocchi**, di proposito: la
  cronologia deve sopravvivere al blocco che racconta, altrimenti
  cancellare un paragrafo cancellerebbe anche la prova che c'era
- **Limite dichiarato**: si registrano il testo dei blocchi e il
  titolo. Le **celle di un database no** — vivono in tabelle separate e
  passano da un'altra strada, che andrà agganciata a parte
- **Verificato sul telefono**: migrazione a 23 con le pagine intatte e
  la tabella nuova creata; scritto `Esmepoi`, corretto in `Esempio`,
  spostato il cursore, e in "Updates" è comparsa **una riga sola** con
  la data giusta e la parola vecchia sbarrata

### 🆕 v0.33 — 22/09/2026 · 15:19 — Preferiti, la base
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Una pagina o un database si possono mettere
  fra i preferiti. **Per ora non c'è niente che li mostri**: è
  deliberato, serve la base su cui poggerà la barra laterale che verrà
- **Una colonna sola per pagine e database** (`pages.isFavorite`, con
  la migrazione 21→22): qui un database **è** una pagina con
  `isDatabase = 1`, quindi metterlo fra i preferiti è esattamente la
  stessa operazione. Due elenchi separati sarebbero stati due posti da
  tenere in fila e due modi di sbagliare
- **La pagina principale non si può mettere fra i preferiti**: è quella
  da cui si parte sempre, e un collegamento a lei non porterebbe da
  nessuna parte. La stellina lì non compare, e il divieto è ribadito in
  `PageRepository.setFavorite` — una regola che vive in un posto solo
  prima o poi viene aggirata da una strada nuova
- **Due posti diversi, e il motivo**: per le pagine è una **stellina
  nella barra in alto**, perché lassù di impostazioni non ce n'è
  nessun'altra; per i database è un interruttore dentro **Settings**,
  perché quella finestra è l'unica che si raggiunge sia col database
  dentro una pagina sia aperto a schermo intero, quindi è anche
  l'unico punto dove non va messo due volte
- **Ci stanno anche le pagine-riga**, al contrario dell'elenco della
  Home: là l'esclusione serve a non riempire la Home di pagine che
  nessuno ha creato apposta, qui invece nell'elenco finisce solo quello
  che è stato scelto uno per uno
- Si scrive **solo quella colonna** (`PageDao.setFavorite`), non tutta
  la riga: la stessa pagina può stare aperta in due schermate insieme,
  e salvarla intera da qui rimetterebbe indietro un titolo cambiato
  nell'altra
- **Verificato sul telefono**: migrazione a 22 eseguita con le cinque
  pagine intatte; stellina accesa e spenta su una pagina e
  l'interruttore acceso e spento su un database, controllando ogni
  volta il valore nel database

### 🔧 v0.32.0.1 — 22/09/2026 · 14:52 — La formattazione sopravvive alla scrittura
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** **La parola censurata non riappare più a ogni tasto.**
  Gli span salvati sono sempre un giro indietro rispetto al campo — il
  campo ha già la lettera appena battuta, il database ce l'avrà fra
  qualche decina di millisecondi — e in quel fotogramma la
  trasformazione rinunciava a disegnare ogni stile. Per grassetto e
  corsivo non si notava; uno spoiler che per un istante non viene
  coperto invece **si legge**, ed era lo scatto che si vedeva scrivendo
  uno spazio o cancellando. Adesso, invece di rinunciare, la stessa
  modifica si applica agli span con `applyTextEdit`, che restituisce
  span corrispondenti **esattamente** alla riga mostrata: le posizioni
  non possono sfasarsi
- **Limite dichiarato**: quando il campo ha un numero di righe diverso
  dai blocchi — cioè nell'istante in cui una riga nasce o sparisce — si
  continua a non disegnare niente. Lì la corrispondenza riga-blocco non
  è affidabile, e uno stile preso dal blocco sbagliato sarebbe peggio
  di nessuno stile
- **[Bug fix]** **[📱 Mobile]** Nel testo scorrevole ogni riga toccata veniva riscritta
  come **un unico pezzo senza formato**: bastava aggiungere una lettera
  in fondo a un paragrafo perché il grassetto di una parola lontana
  sparisse. Era un difetto vecchio, che non si notava perché quasi
  sempre si formatta alla fine; con gli spoiler sarebbe stato evidente
  al primo tentativo — si copre una parola, si continua a scrivere e il
  velo cade da solo
- Ora la zona modificata si tratta come **un testo solo**, a-capo
  compresi: la modifica si applica agli span con `applyTextEdit`, che
  tocca soltanto il pezzo davvero cambiato, e poi si rimette in righe
  (`splitLines`/`joinLines`). Vale anche a cavallo di più righe:
  andando a capo in mezzo a una frase, la metà che scende **tiene il
  suo formato**
- **Gli span attaccati con la stessa formattazione si fondono**
  (`merged`). Applicando solo il tratto cambiato, ogni tasto battuto
  lasciava il suo pezzetto separato e il JSON di un paragrafo cresceva
  di una voce per lettera scritta: visto davvero sul telefono, una
  frase di dieci caratteri era dieci span
- **Rete di sicurezza**: se il risultato non corrisponde più alla riga
  che deve descrivere, si scrive la riga in chiaro. Meglio perdere un
  grassetto che scrivere nel database un testo diverso da quello che
  l'utente vede — è già successo una volta, e quella volta è costata
  dei blocchi svuotati
- **[Bug fix]** **[📱 Mobile]** Nei blocchi isola (toggle) la barra **Aa** formattava
  **una lettera prima** di quella selezionata: le posizioni della
  selezione contano anche l'a-capo nascosto in testa, gli span no

### 🆕 v0.32 — 22/09/2026 · 14:45 — Testo coperto (spoiler)
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Come su Discord e Telegram: si scrive
  `||qualcosa||` e le quattro barre spariscono — sono un comando, non
  roba scritta — lasciando al loro posto una **fascia grigia** che
  copre le lettere. La stessa cosa si fa dalla barra **Aa**, con
  l'occhio sbarrato, su qualsiasi pezzo di testo selezionato
- **[Nuova funzionalità]** **[📱 Mobile]** Si scopre **toccandolo**: il pezzo coperto
  torna leggibile quando il cursore, o la selezione, ci sta **dentro**,
  e si ricopre appena il cursore se ne va o il campo perde il fuoco. Non
  serviva nient'altro: in un editor un tocco è già un cursore che si
  posa lì
- **Dentro, non sui bordi**, e il motivo è il momento in cui lo spoiler
  nasce: finito di scrivere `||parola||` il cursore resta appoggiato
  **subito dopo l'ultima lettera**, quindi contando anche il bordo lo
  spoiler veniva al mondo già scoperto — si vedeva la parola con la sua
  fascia chiara invece della censura. Contando solo l'interno si copre
  nell'istante in cui si scrive la quarta barra, ed è quello che ci si
  aspetta. Per leggerlo non cambia niente: un dito cade dentro la
  parola, non sul suo bordo
- **Scelta**: da coperte le lettere si disegnano **trasparenti**, non
  tolte. Restano dov'erano, quindi la riga è larga uguale, va a capo
  dove andava e il cursore ci si muove dentro normalmente; sopra passa
  la fascia. Togliere davvero il testo avrebbe voluto dire rifare tutta
  la traduzione delle posizioni, che è la parte più delicata di questo
  editor
- **Scelta**: la chiusura si lega all'**apertura più vicina**, così
  `||a|| e ||b||` fa due spoiler separati invece di coprire anche la
  "e" in mezzo. In mezzo alle barre non ci può stare né un'altra barra
  né un a-capo
- **[Nuova funzionalità]** **[📱 Mobile]** Uno spoiler **non si allunga scrivendoci
  dopo**. Per tutto il resto (grassetto, corsivo...) continuare a
  scrivere continua la formattazione, che è quello che fanno tutti gli
  editor; qui no, perché il cursore resta proprio lì attaccato e la
  parola dopo sarebbe diventata invisibile senza che nessuno l'avesse
  chiesto. Scrivendo **dentro**, invece, si allarga
- **Nessuna migrazione**: `spoiler` è un campo con valore predefinito
  dentro il JSON del testo, e il testo già salvato — che quella chiave
  non ce l'ha — si rilegge così com'è

### ✏️ v0.31.4 — 22/09/2026 · 14:27 — Gli elenchi numerati cambiano modo di contare a ogni rientro
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Stessa idea dei puntati, otto modi in fila
  (`NUMBERED_STYLES`): `1.`, `i.`, `1)`, `i)`, `a.`, `One.`, `a)`,
  `First.` — e poi si ricomincia da `1.`. Verificato sul telefono
  livello per livello, dallo zero all'otto: al nono rientro torna `1.`
- **Assunto dichiarato**: "lettere con numeri" è stato inteso come
  **lettere col punto** (`a.` `b.` `c.`), in parallelo a "lettere con
  parentesi" (`a)`). Se si intendeva altro è una riga sola da cambiare
- **[Bug fix]** **[📱 Mobile]** **Ogni livello ha il suo contatore che riparte da uno**
  (`numberedListOrdinal`). Prima il numero veniva composto col ramo
  intero — `1.1.1` — e a quel punto i modi di scrivere delle colonne
  più interne non avevano senso: `i.` non può dire "uno punto uno".
  Ora il conteggio si tiene in una pila di contatori, uno per livello,
  e tornando indietro si riprende da dove quella colonna era rimasta
- **Scelte dentro i modi di scrivere**: i numeri romani tornano a
  essere **cifre oltre il 3999** (`romanNumeral`), che è meglio di una
  riga di `m`; le lettere proseguono **come le colonne di un foglio di
  calcolo** (`letterOrdinal`, `z` → `aa`) invece di fermarsi alla zeta;
  le parole (`One`, `First`) si fermano a **venti** e oltre tornano
  cifre, perché "Thirtyseventh" si mangerebbe mezza riga
- **[Modifica]** **[📱 Mobile]** Anche i numeri **dentro un toggle** passano
  dalla stessa funzione: allo stesso rientro un numero dentro un toggle
  e uno fuori devono essere scritti uguali
- **[Bug fix]** **[📱 Mobile]** La riga appena creata con l'Invio prende il numero
  **previsto** (`carriedOrdinal + 1`) invece di nessun numero: è lo
  stesso rimedio dei pallini, e senza si vedeva il segno comparire in
  ritardo

### ✏️ v0.31.3 — 22/09/2026 · 14:09 — Gli elenchi puntati cambiano segno a ogni rientro
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Come su OneNote: `•` pieno, `◦` vuoto, `▪`
  quadratino pieno, `▫` quadratino vuoto, `◆` rombo pieno, `◇` rombo
  vuoto, `▸` triangolino pieno, `▹` triangolino vuoto — e poi si
  ricomincia
- **[Modifica]** **[📱 Mobile]** Il segno dipende **solo dal livello di
  rientro**, non da cosa c'era prima. È quello che serve per riportare
  indietro una riga: ritrova da sola il segno della colonna in cui
  arriva, qualunque strada abbia fatto. Con un contatore "prosegui da
  dove eri", due righe alla stessa altezza potrebbero finire con segni
  diversi
- **[Modifica]** **[📱 Mobile]** Ognuno ha **il suo corpo**
  (`BULLET_MARKER_SIZES`): questi caratteri sono disegnati molto
  diversi fra loro — un pallino occupa meno di un terzo del suo spazio,
  un rombo quasi due terzi — e con un'unica dimensione per tutti i
  pallini venivano minuscoli e i rombi delle macchie. Così si vedono
  della stessa misura
- **[Bug fix]** **[📱 Mobile]** **L'altezza della riga del testo scorrevole è sempre
  fissata** (`RUN_LINE_HEIGHT`, 24). Lasciandola decidere al carattere,
  ogni riga diventava alta quanto il segno più grande che conteneva: le
  righe coi pallini (scritti a 20) erano più alte di quelle coi rombi
  (scritti a 13), e l'elenco veniva a passo irregolare. Sembravano
  storti i segni, ma a essere storte erano le righe. Misurato sulla
  stessa lista di diciassette punti: prima il passo fra una riga e
  l'altra andava da 34 a 48 punti, dopo è 43-44 su tutte
- **[Modifica]** **[📱 Mobile]** I segni scendono di un filo sotto la riga di
  base (`BULLET_MARKER_DROP`): appoggiati esattamente sulla riga di base
  sembravano un po' troppo in alto rispetto al testo. È una **frazione
  del corpo del segno**, non una misura fissa, così i pallini (scritti
  grandi) e i rombi (scritti piccoli) scendono in proporzione e restano
  allineati fra loro. Non è un centraggio calcolato: è un ritocco
  deciso guardando lo schermo
- **Vincolo**: nessun segno supera i **20**. Il limite non è la misura
  del testo (16) ma **l'altezza della riga**: il testo scorrevole sta su
  righe da 24 e un carattere da 16 ne occupa in altezza meno di 19,
  quindi c'è margine. Dentro quei 24 la riga non si allarga; oltre, si
  allargherebbero solo le righe col segno più grande e l'elenco
  verrebbe a scalini
- **[Bug fix]** **[📱 Mobile]** **Il cursore non scatta più a inizio riga andando a
  capo.** Subito dopo un Invio il campo ha una riga in più del
  database, e da quel punto in giù righe e blocchi non si
  corrispondevano: chiedendo "il blocco numero i" si otteneva quello
  della riga *successiva*. Se era un paragrafo — il caso normale, un
  elenco finisce quasi sempre con una riga vuota sotto — la riga nuova
  veniva disegnata **senza pallino**, e il cursore si vedeva scattare a
  sinistra per poi tornare al suo posto appena il database arrivava.
  Ora il punto d'inserimento si trova **dalla fine** (le righe in fondo
  che combaciano ancora coi loro blocchi dicono quante ne vengono dopo
  quella nuova) e da lì in giù la corrispondenza viene spostata di uno.
  Cercarlo dall'inizio non funziona: andando a capo cambia testo anche
  la riga divisa. Verificato catturando il **primo fotogramma** dopo
  l'Invio: il pallino c'è già e il cursore è al suo posto
- **[Modifica]** **[📱 Mobile]** Il tetto ai rientri sale **da 5 a 10**: con
  cinque livelli il giro di otto non si sarebbe mai potuto vedere né
  finire né ricominciare

### 🔧 v0.31.2.1 — 22/09/2026 · 13:54 — Copertina e titolo scorrono con la pagina
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Stavano fuori dalla lista che scorre, quindi restavano
  inchiodati in cima mentre il testo scorreva sotto. Con una copertina
  alta si mangiavano mezza schermata che non si poteva recuperare. Ora
  sono la **prima voce della lista** e se ne vanno scorrendo, come su
  Notion. Non era una regressione recente: sono sempre stati lì fuori,
  si nota da quando c'è una copertina
- **[Bug fix]** **[📱 Mobile]** Sono **una voce sola** di proposito: gli indici della
  lista servono anche a portare in vista la riga che prende il cursore,
  e uno scarto fisso (`HEADER_ITEM_COUNT`) è molto più facile da tenere
  giusto di uno variabile
- **Verificato**: il titolo si scrive ancora, e **messo a fuoco e poi
  fatto scorrere via sei volte di fila non chiude l'app** — era il
  rischio vero, perché ora anche quel campo di testo può essere
  distrutto dallo scorrimento come gli altri dentro la lista

### ✏️ v0.31.2 — 22/09/2026 · 13:29 — La barra sopra la tastiera, rifatta
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Ha un **grigio tutto suo**
  (`DarkSurfaceVariant`), più chiaro della pagina e diverso dalla
  tastiera: prima era dello stesso nero della pagina e non si capiva
  dove finisse una e cominciasse l'altra
- **[Modifica]** **[📱 Mobile]** È **più bassa**: i pulsanti passano dai 48 di
  serie a 40 e le icone a 22 (`BAR_BUTTON_SIZE`, `BAR_ICON_SIZE`), e
  sparisce il margine sopra e sotto — l'altezza la danno i pulsanti, e
  ogni punto in più lì è una fascia in più che copre il testo. Tutti i
  pulsanti passano da `BarButton`, così non possono avere misure
  diverse fra loro
- **[Bug fix]** **[📱 Mobile]** **Se ne va insieme alla tastiera.** Il suo bordo
  inferiore è appoggiato sopra la tastiera, quindi mentre quella scende
  la barra la segue — ma si fermava al bordo dello schermo e restava lì
  finché il sistema non dichiarava la tastiera chiusa: da fuori sembrava
  sparisse qualche istante dopo. Negli ultimi punti della discesa ora la
  si spinge giù di quanto le manca per uscire
  (`slideAway = altezzaBarra - insetTastiera`), così arriva al bordo
  nello stesso momento; quando il sistema poi la smonta è già
  invisibile e non si vede nessuno scatto. Col menu "+" aperto lo
  scorrimento è disattivato, perché lì la barra si vede anche senza
  tastiera
- **[Bug fix]** **[📱 Mobile]** È **attaccata alla tastiera**: restava sospesa di pochi
  punti perché lo spazio per la barra di navigazione arrivava dallo
  `Scaffold` e quello per la tastiera da `imePadding`, e a tastiera
  aperta **si sommavano**. Ora il fondo prende l'unione dei due
  (`WindowInsets.ime.union(WindowInsets.navigationBars)`): vince la più
  alta, che è sempre quella giusta — a tastiera aperta la barra di
  navigazione le sta dietro e non occupa niente

### 🔧 v0.31.1.1 — 22/09/2026 · 11:31 — La barra della formattazione non resta più a mezz'aria
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** È il difetto che tornava da giorni, e la causa non era
  nel codice ma nel **manifesto**: l'attività non dichiarava come
  comportarsi all'apertura della tastiera, e Android sceglieva da sé di
  far **scorrere su tutta la finestra** per tenere in vista il punto in
  cui si scrive — il titolo della pagina usciva dallo schermo in alto.
  Il guaio è che il codice aggiunge *già* lo spazio per la tastiera con
  `imePadding`: i due effetti si sommavano e la barra finiva staccata
  dalla tastiera, di quasi un'altezza di tastiera
- **[Bug fix]** **[📱 Mobile]** Con `android:windowSoftInputMode="adjustResize"` la
  finestra resta ferma e a fare spazio pensa solo `imePadding`, che sa
  dov'è davvero la tastiera. Verificato sullo stesso gesto che lo
  faceva comparire: il titolo ora **resta al suo posto** e la barra è
  incollata alla tastiera
- **[Bug fix]** **[📱 Mobile]** **Il tocco a vuoto non manda più il cursore in fondo
  alla pagina.** Il gestore "hai toccato lo spazio vuoto, continua a
  scrivere" copriva **tutta** la lista, quindi prendeva anche lo spazio
  accanto al titolo e i buchi fra una riga e l'altra: toccandoli, il
  cursore saltava all'ultimo blocco in fondo e si apriva la tastiera.
  Ora scatta solo per i tocchi **sotto l'ultimo blocco** — e solo se
  l'ultimo blocco è davvero l'ultimo, non l'ultimo *visibile*.
  Verificato che il gesto utile continui a funzionare

### ✏️ v0.31.1 — 22/09/2026 · 11:17 — Le caselle da spuntare entrano nel campo di testo condiviso
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** È la cura alla radice di tutta la famiglia di
  difetti delle caselle. Finché ognuna era un campo di testo **a sé**,
  andare a capo spostava il fuoco da un campo all'altro e ogni cambio di
  specie ne distruggeva uno mentre aveva il cursore: da lì la tastiera
  che spariva, la maiuscola che faceva uno scatto, le lettere nella riga
  di prima, il testo cancellato dall'Invio e l'app che si chiudeva.
  Dentro il campo condiviso **andare a capo è solo scrivere un a-capo**,
  come fra due paragrafi: non c'è nessun fuoco da spostare e niente da
  distruggere
- **[Modifica]** **[📱 Mobile]** Il quadratino non può stare *dentro* un campo
  di testo, quindi il testo gli riserva lo spazio con dei caratteri
  invisibili (`CHECKBOX_MARKER`) e il quadratino vero viene disegnato
  **sopra**, alla posizione che il campo stesso dichiara per quel
  carattere (`RunCheckboxOverlay`). Così resta incollato alla sua riga
  anche quando il testo va a capo da solo, e ha una zona di tocco vera
  invece di essere un carattere da centrare col dito
- **[Modifica]** **[📱 Mobile]** L'overlay va messo **dopo** il campo dentro
  il `Box`: in Compose i tocchi li riceve per primo l'ultimo figlio, e
  messo prima il quadratino non rispondeva — il tocco andava al testo
- **[Modifica]** **[📱 Mobile]** Una casella spuntata ha il testo **sbarrato e
  smorto**, come su Notion. L'Invio ne crea un'altra non spuntata, e il
  backspace a inizio riga toglie il quadratino lasciando il testo: sono
  gli stessi due comportamenti che avevano già elenchi puntati e
  numerati, non un ramo a parte
- **Trappola, costata una mezz'ora**: i tipi che vivono nel campo
  condiviso sono elencati **due volte**, in `FLOWING_TYPES` nella
  schermata e in `isFlowingTextType` nel ViewModel. Aggiornando solo il
  primo, il campo mostrava le caselle ma il ViewModel non le riconosceva
  come sue: sullo schermo si scriveva e **nel database non arrivava
  niente**, senza un errore. Se si tocca uno, va toccato l'altro
- **Verificato sul telefono**, misurando gli eventi veri della tastiera:
  spuntare col tocco funziona e si salva; l'Invio crea un'altra casella
  e il testo resta (`AaaaaaaaaaaaaQQ` / `RR` / `SS` nel database, nessun
  indice doppio); il backspace a inizio riga toglie il quadratino;
  creare una casella dal menu "/" lascia la tastiera aperta e si scrive
  subito. In tutte e quattro le prove `ImeTracker` **non ha registrato
  né `onHidden` né `onShown`**, e non c'è stato nessun crash
- **[Modifica]** **[📱 Mobile]** Le righe con una casella **stanno più
  larghe** (`CHECKBOX_LINE_HEIGHT`): il quadratino è piccolo e attaccate
  è facile spuntare quella sbagliata col dito
- **[Bug fix]** **[📱 Mobile]** **I quadratini non saltano più ad ogni tasto.**
  Scrivendo o cancellando, sparivano e riapparivano spostati di lato per
  un istante. Erano disegnati alle posizioni ricalcolate dal testo
  corrente del campo, mentre le coordinate arrivavano dalla misura del
  fotogramma prima: due fonti che per un attimo non combaciano. Ora il
  marcatore si cerca **dentro il testo che il campo ha davvero
  misurato** (`layoutInput.text`), quindi disegno e coordinate non
  possono più sfasarsi
- **Compromesso dichiarato**: **il cursore di scrittura è alto quanto la
  riga**, e il campo di testo lo disegna così — non si può separare. Più
  spazio fra le caselle vuol dire cursore più alto. A 34 si notava; il
  valore sta a 27, appena sopra il naturale (24): cursore di misura
  normale e un po' d'aria fra una casella e l'altra
- **[Modifica]** **[📱 Mobile]** Il testo si **centra** nella riga alta
  (`lineHeightStyle`). Di suo Compose spartisce lo spazio in più in
  proporzione a quanto le lettere salgono e scendono, e il testo non
  finisce in mezzo: i quadratini, che invece stanno in mezzo,
  risultavano disallineati. Chiedendo il centro, testo e quadratino
  coincidono **per costruzione**, qualunque altezza di riga si scelga —
  meglio di una correzione a occhio da ritoccare ogni volta
- **Resta com'era** la casella dentro un *toggle*: lì è ancora una riga
  a sé (`BlockRow`), perché i figli di un toggle si disegnano per conto
  loro

### 🔧 v0.31.0.1 — 22/09/2026 · 10:27 — L'Invio dentro una casella non sposta più il fuoco
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Tre difetti diversi delle caselle da spuntare — la
  maiuscola che faceva uno scatto da minuscola a maiuscola, le lettere
  che finivano nella riga di prima andando a capo in fretta, e l'app che
  si chiudeva **senza messaggio** — avevano **una sola causa**, trovata
  col registro: ogni casella è un campo di testo a sé, e andando a capo
  il fuoco si spostava sul campo nuovo. Nel log si vedeva a ogni Invio
  `onStartInputView restarting=true`, cioè **la tastiera che si stacca e
  si riattacca**. In quel momento la maiuscola automatica si rimette a
  zero (di qui lo scatto), i tasti già premuti arrivano al campo
  vecchio, e il campo appena lasciato viene distrutto mentre la tastiera
  gli sta ancora chiedendo dov'è il cursore — che è l'errore
  `LayoutCoordinate ... isAttached` con cui l'app spariva
- **[Bug fix]** **[📱 Mobile]** La cura è controintuitiva: **la riga nuova nasce sopra,
  non sotto.** Va a capo mettendo il testo *prima* del cursore in un
  blocco nuovo inserito sopra, e lasciando al blocco su cui si sta
  scrivendo — che il fuoco ce l'ha già — il testo *dopo* il cursore.
  Sullo schermo è identico, ma **il fuoco non si muove mai**: niente
  cambio di campo, niente tastiera staccata, niente da distruggere.
  Misurato: in una raffica di sedici a-capo, **zero** cambi di fuoco, e
  le lettere tutte al posto giusto
- **[Bug fix]** **[📱 Mobile]** La spunta e il "riparti da N" restano alla riga di
  sopra, che è la continuazione di quella che c'era; la riga nuova nasce
  non spuntata, come su Notion
- **[Bug fix]** **[📱 Mobile]** L'indice del blocco viene **riletto dal database dentro
  la transazione** (`PageRepository.splitBlockAbove`), e le divisioni si
  mettono in fila con un `Mutex`. Prima usavo l'indice della copia in
  mano alla schermata: andando a capo in fretta era di un a-capo fa, e
  **nascevano due blocchi con lo stesso posto** — visto davvero nel
  database, non ipotizzato
- **[Bug fix]** **[📱 Mobile]** **Il secondo a-capo di fila non veniva più fatto.**
  Effetto collaterale della cura, trovato subito dopo leggendo il
  database: il riconoscimento dell'"eco della tastiera" (vedi
  `SplitEcho`) è per campo, e ora che il campo sopravvive a tutte le
  divisioni restava armato. Scrivendo in fretta due righe che finivano
  con lo stesso testo, il secondo Invio veniva scambiato per un'eco e
  ignorato. Ora la memoria dell'eco si azzera appena si batte un
  carattere vero: un'eco arriva subito dopo la divisione e senza niente
  in mezzo, quindi quella memoria non serve più. Verificato: tre a-capo
  in raffica danno tre righe, `Z` `W` `Q`
- **[Bug fix]** **[📱 Mobile]** **Un salvataggio di testo non sposta più il blocco.**
  `updateBlockSpans` risalvava l'**entità intera**, quindi rispediva
  anche l'`orderIndex` e la spunta che la schermata aveva in mano: dopo
  un a-capo quella copia è di un istante prima, e il primo carattere
  successivo rimandava il blocco al posto che nel frattempo aveva preso
  la riga nuova. Si vedeva come "scrivo, faccio invio, mi cancella
  quello che ho scritto". Ora scrive **solo la colonna del testo**
  (`BlockDao.setText`)
- **[Bug fix]** **[📱 Mobile]** **La tastiera non sparisce più** cancellando una casella
  o andando a capo su una casella vuota, e **l'app non si chiude più**.
  Erano i due corni dello stesso problema: quel campo muore per forza (il
  blocco passa da riga a sé a testo scorrevole, che è un'altra
  composable), e distruggerlo mentre ha il fuoco fa chiudere l'app,
  mentre staccare la tastiera con `clearFocus()` la fa sparire e tornare.
  Misurati tutti e due: senza `clearFocus()` venti cicli su venti
  chiudevano l'app con `LayoutCoordinate ... isAttached`; con
  `clearFocus()` `ImeTracker` segnava `onHidden` e poi `onShown` 315 ms
  dopo
- **[Bug fix]** **[📱 Mobile]** La terza strada è **il posteggio della tastiera**: un
  campo di testo alto un punto e invisibile, fuori dalla lista a
  scorrimento (dentro sparirebbe appena esce dallo schermo), su cui il
  fuoco si posa un istante prima che il campo vero venga distrutto. La
  tastiera resta aperta perché sotto di sé ha sempre un campo, e riparte
  sul blocco nuovo appena esiste. Verificato: gli stessi venti cicli non
  chiudono più l'app, e in una sequenza completa — scrivo, Invio,
  scrivo, Invio, backspace, scrivo — `ImeTracker` **non registra né
  `onHidden` né `onShown`**, cioè la tastiera non si muove mai
- **[Bug fix]** **[📱 Mobile]** Il posteggio serviva **anche al campo del testo
  scorrevole**, non solo alle isole: riga vuota sotto una casella +
  backspace fonde la riga dentro la casella, la riga sparisce e con lei
  il suo campo. Era il caso più facile da incontrare e chiudeva l'app.
  `backspaceAtLineStartDetailed` ora dice *cosa* ha fatto, così il campo
  sa quando sta per morire. Verificato: dodici cicli, nessun crash
- **La causa di fondo, dichiarata**: ogni casella da spuntare è un campo
  di testo **a sé**, mentre i paragrafi ne condividono uno solo. Un
  blocco che passa da una specie all'altra cambia quindi campo, e un
  campo distrutto mentre ha il fuoco fa chiudere l'app per un difetto di
  Compose 1.6. Ogni punto in cui questo può succedere va protetto a
  mano, e finché l'architettura è questa se ne può sempre scoprire uno
  nuovo. Le due vie d'uscita vere sono **aggiornare Compose** (il
  difetto è corretto dalla 1.7) o **mettere anche le caselle dentro il
  campo condiviso** — che risolverebbe pure la selezione fra caselle,
  già in "Limiti noti"
- **Nota sul misurare**: `dumpsys input_method | grep mInputShown` **non
  basta** per dire se la tastiera sparisce: resta `true` anche mentre si
  sta chiudendo. Aveva fatto dichiarare risolto un difetto che c'era
  ancora. Il segnale onesto sono gli eventi `ImeTracker: onHidden` /
  `onShown` in `logcat`
- **Limite dichiarato**: un blocco con dei figli (un toggle) usa ancora
  la vecchia strada, riga nuova sotto e fuoco che si sposta. Spostando
  l'identità del blocco alla riga di sotto, i suoi figli finirebbero
  sotto la riga sbagliata
- **Nota onesta**: lo scatto della maiuscola e la chiusura improvvisa
  **non sono stati riprodotti a comando** — il primo perché iniettando
  il testo via adb la tastiera non entra in gioco e la maiuscola
  automatica non scatta, la seconda perché non è successa in nessuna
  delle raffiche provate. È stata tolta la causa comune, che invece è
  stata misurata; la conferma vera arriva usando l'app a mano

### 🆕 v0.31 — 22/09/2026 · 09:16 — Filtri su numeri e caselle, e la casella al centro della colonna
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Si filtra su **numeri** (da x a y, estremi
  inclusi), **date**, **Select/MultiSelect** e **caselle da spuntare**.
  Restano fuori testo, indirizzo, mail e telefono: lì la domanda
  sarebbe "contiene queste lettere", cioè una ricerca, che è un'altra
  cosa e ancora non c'è
- **[Nuova funzionalità]** **[📱 Mobile]** Sui **numeri** basta un estremo: solo "da"
  vuol dire "da lì in su", solo "a" vuol dire "fino a lì". Metà delle
  domande vere sono aperte da un lato ("sopra i 100"), e obbligare a
  scrivere un secondo numero inventato le renderebbe sbagliate. Una
  cella vuota non passa mai: non è zero, è "non c'è un numero"
- **[Nuova funzionalità]** **[📱 Mobile]** Sulle **caselle** si scelgono *Checked*,
  *Unchecked* o tutte e due. Le celle mai toccate **non esistono nel
  database**: valgono "non spuntata", non "non lo so", altrimenti
  sparirebbero da tutti e due i filtri — il modo peggiore di sbagliare.
  Verificato sul telefono: *Checked* dà A, B, E, G e *Unchecked* dà
  C, F, Esempio, che sono esattamente i complementari letti in SQLite
- **[Bug fix]** **[📱 Mobile]** **Il tocco sullo spazio vuoto di una cella non fa più
  niente.** Prima attraversava la tabella e arrivava alla pagina che le
  sta dietro, che lo intende come "scrivi qui": toccando accanto a una
  casella da spuntare il cursore finiva **sotto** al database e si
  apriva la tastiera con la barra della formattazione appesa in alto.
  Ora la cella si mangia i tocchi che non colpiscono niente; i figli
  vengono serviti per primi, quindi la casella continua a rispondere
  (verificato: `mInputShown=false` prima e dopo il tocco a vuoto, e lo
  schermo byte per byte identico)
- **[Nuova funzionalità]** **[📱 Mobile]** Tenendo premuta l'intestazione di una
  colonna **a caselle** compare **"Center"**, che mette il quadratino
  al centro della colonna invece che appoggiato al bordo sinistro; la
  stessa voce diventa poi "Align left". Essendo un allineamento e non
  una misura, resta al centro **qualunque larghezza** abbia la colonna
- **[Nuova funzionalità]** **[📱 Mobile]** L'opzione c'è solo per le caselle: è l'unico
  contenuto di cella che è un segno solo, dove l'allineamento si nota.
  Un testo o una data centrati renderebbero la colonna illeggibile,
  perché le righe non partirebbero più tutte dallo stesso punto
- Sta in `centerContent` di `database_columns`, **schema alla versione
  21**

### 🆕 v0.30 — 22/09/2026 · 01:46 — I filtri
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Le righe si possono **filtrare**: l'icona a
  sinistra di Sort nella barra della vista, e la voce **"Filter"**
  nelle impostazioni. Non sono due schermate gemelle ma **la stessa
  finestra** aperta da due posti: due copie si sarebbero separate al
  primo cambiamento, ed è il tipo di differenza che si nota solo
  usandole
- **[Nuova funzionalità]** **[📱 Mobile]** Sulle **date** una pagina passa se i suoi
  giorni **toccano** quelli scelti, non se coincidono: un impegno dal 3
  al 7 compare filtrando il 5, altrimenti filtrare un giorno solo non
  mostrerebbe quasi mai niente. Le **date che si ripetono** contano
  tutte le loro volte, non solo la prima
- **[Nuova funzionalità]** **[📱 Mobile]** Sui **tag** passa chi ne ha **almeno uno**
  fra quelli scelti, come il "contains any of" di Notion: chiederli
  tutti renderebbe inutile sceglierne più d'uno su una proprietà a
  selezione singola, dove nessuna riga potrebbe mai passare
- **[Nuova funzionalità]** **[📱 Mobile]** L'icona è **accesa di blu** quando un filtro
  c'è: senza, una tabella con metà delle pagine sembrerebbe una tabella
  che ha perso delle pagine. Il **cestino** in alto a destra nella
  finestra compare solo quando c'è davvero un filtro da buttare
- **[Nuova funzionalità]** **[📱 Mobile]** Il filtro vale per **tutte le viste**: la
  domanda "fammi vedere solo queste pagine" non cambia senso passando
  alla bacheca o al calendario, e un filtro che si spegne cambiando
  vista sarebbe un filtro di cui non ci si può fidare. Se la proprietà
  su cui si filtrava viene cancellata, il filtro smette di valere
  invece di nascondere tutto
- **[Nuova funzionalità]** **[📱 Mobile]** Il filtro si salva sulla pagina
  (`filterColumnId` e `filterValue`, **schema alla versione 20**). Il
  valore usa lo stesso formato delle celle, quindi a leggerlo è la
  stessa funzione che legge una data: due copie non possono separarsi.
  Per questo `DateRange`/`parseDateRange` si sono spostate da
  `DatabaseViewScreen` a `data/entity/DateValue.kt`
- **Limiti dichiarati**: **un filtro per volta** su una proprietà sola,
  come l'ordinamento; Notion ne concatena più d'uno con "and"/"or"

### 🆕 v0.29 — 22/09/2026 · 01:39 — Colonne che si nascondono
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Tenendo premuta l'intestazione** di una
  colonna compare un menu con *Edit property* e **Hide**: la colonna
  sparisce dalla tabella, che si stringe di conseguenza
- **[Nuova funzionalità]** **[📱 Mobile]** Nelle impostazioni della vista c'è
  **"Property visibility"**, col numero di colonne nascoste accanto.
  Dentro, le due liste **"Shown in table"** e **"Hidden in table"**
  come su Notion, ciascuna col suo *Hide all* / *Show all*; toccando
  una riga la si sposta nell'altra lista
- **[Nuova funzionalità]** **[📱 Mobile]** **Nascosta non vuol dire cancellata**: i
  valori restano nelle celle (verificato leggendo il database prima e
  dopo), e la proprietà continua a comparire negli ordinamenti, nei
  raggruppamenti e nelle azioni di una riga. È solo una colonna in meno
  da scorrere in orizzontale, che su un telefono è il motivo per cui
  serve
- **[Nuova funzionalità]** **[📱 Mobile]** Nascondere sta **sull'intestazione** e
  rimostrare **nelle impostazioni**, non per simmetria mancata: si
  decide di nascondere guardando la tabella, mentre una colonna
  nascosta non ha più un'intestazione da tenere premuta
- **[Nuova funzionalità]** **[📱 Mobile]** "Name" non compare in nessuna delle due
  liste: è il titolo della pagina, non una proprietà, e una riga senza
  nome non si saprebbe più aprire
- **[Nuova funzionalità]** **[📱 Mobile]** Lo stato sta nella colonna `hidden` di
  `database_columns` (**schema alla versione 19**), quindi vale per
  tutte le viste dello stesso database e sopravvive al riavvio
  (verificato: nascosta, app chiusa e riaperta, ancora nascosta)

### 🆕 v0.28 — 22/09/2026 · 01:23 — Le date che si ripetono
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Una data può **ripetersi**: dalla finestra
  della data, riga **Repeat**, con le scorciatoie di Google Calendar
  costruite sul giorno scelto — *Daily*, *Weekly on Sunday*, *Monthly
  on the first Sunday*, *Annually on...*, *Every weekday* — e una
  schermata **Custom** per tutto il resto: ogni N giorni/settimane/
  mesi/anni, in quali giorni della settimana, "il 21 del mese" oppure
  "il terzo lunedì", e quando finisce (mai / entro una data / dopo N
  volte)
- **[Nuova funzionalità]** **[📱 Mobile]** **Una pagina che si ripete resta una pagina
  sola.** Non vengono create copie: la regola sta attaccata alla data,
  e il calendario la trasforma in tante comparse quante ne servono per
  il periodo che si sta guardando. Aprirne una qualsiasi apre quella
  pagina, e modificarla cambia tutte le volte — è la sincronizzazione
  che ci si aspetta, e l'unico modo per poter correggere "tutte le
  domeniche" con una modifica sola. Creare davvero una riga per ogni
  ripetizione riempirebbe il database di copie da correggere a una a una
- **[Nuova funzionalità]** **[📱 Mobile]** Le occorrenze si calcolano **solo per il
  periodo mostrato**: una regola senza fine genererebbe date
  all'infinito, e di quelle del 2043 non se ne fa niente nessuno
  finché non ci si arriva
- **[Nuova funzionalità]** **[📱 Mobile]** La riga fissa di ogni pagina nel calendario
  si calcola sulle date **di partenza**, non sulle comparse: quelle
  cambiano da un mese all'altro e farebbero ballare la riga a seconda
  di dove ci si trova
- **[Nuova funzionalità]** **[📱 Mobile]** "Il terzo lunedì" e **"l'ultima domenica"**
  sono due cose diverse: quando il giorno scelto è l'ultimo del suo
  tipo nel mese la regola diventa "l'ultimo", perché chi dice l'ultima
  domenica intende sempre l'ultima, anche nei mesi che ne hanno cinque.
  Allo stesso modo "ogni mese il 31" cade l'ultimo giorno nei mesi
  corti
- **[Nuova funzionalità]** **[📱 Mobile]** La regola si salva **accanto alla data**,
  come quarto campo del valore della cella (`RecurrenceCodec`): niente
  colonne nuove, niente migrazione, e le date scritte prima continuano
  a valere "capita una volta sola"
- **Limiti dichiarati**: le ripetizioni si vedono nel **calendario**,
  non ancora nella linea del tempo; e **le notifiche non ci sono** —
  sono un pezzo a sé, con permesso di sistema, allarmi e
  riprogrammazione al riavvio, ed è stato deciso di farle dopo

### 🆕 v0.27 — 22/09/2026 · 00:59 — Il menu "/"
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Scrivendo **"/"** dentro il testo si apre il
  menu dei blocchi, diviso in **Basic blocks / Media / Database** come
  su Notion. Quello che si scrive dopo la barra **filtra la lista**
  ("/tab" lascia Table e Table view), e la barra col filtro sparisce
  dal testo appena si sceglie: è un comando, non roba scritta
- **[Nuova funzionalità]** **[📱 Mobile]** **Le voci non ancora costruite ci sono
  lo stesso, spente**: Callout, Quote, Link to page, tutto il gruppo
  Media, Gallery view, Database full page, Simple database, Linked
  view of data source. Dicono cosa c'è e cosa manca senza costringere
  a cercarlo, e quando una verrà costruita basterà cambiarle l'azione.
  **È l'unico posto dell'app dove una voce inerte è meglio di una voce
  assente**: qui si sta scegliendo da un catalogo, non cercando un
  comando che dovrebbe esserci
- **[Nuova funzionalità]** **[📱 Mobile]** Le voci delle viste database (**Table,
  Board, List, Calendar, Timeline view**) creano il database dentro la
  pagina e gli mettono subito quella vista: "Calendar view" dà un
  calendario, non una tabella da cambiare a mano. Verificato sul
  telefono
- **[Nuova funzionalità]** **[📱 Mobile]** Il menu **non ruba il fuoco**
  (`focusable = false`): la tastiera resta aperta, perché il modo
  normale di usarlo è continuare a scrivere per filtrare
- **Limite dichiarato**: il menu "/" funziona nel testo scorrevole
  (paragrafi, titoli, elenchi), non dentro una casella da spuntare o
  un toggle, che hanno un campo di testo tutto loro

### ✏️ v0.26.1 — 22/09/2026 · 00:47 — Il calendario anche senza una proprietà Date
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Scegliendo la vista Calendario su un
  database appena creato, **il calendario si vede lo stesso**: mese
  corrente, vuoto, con i suoi giorni. Prima al suo posto c'era un
  cartello che spiegava che serviva una proprietà data e offriva di
  crearla — corretto ma inutile, perché un calendario vuoto si capisce
  da solo
- **[Modifica]** **[📱 Mobile]** **La proprietà data la crea il primo
  tocco.** Toccando un giorno nasce lì la pagina, e con lei la
  proprietà "Date" se il database non ne aveva una — che viene anche
  fissata come quella del calendario, così aggiungendone un'altra
  domani la vista non cambia colonna sotto i piedi. Chi tocca un
  giorno ha già detto tutto quello che serve: vuole una pagina, in
  quel giorno. L'impalcatura è un problema nostro
- **[Modifica]** **[📱 Mobile]** Anche "+ New page" in fondo funziona senza
  proprietà data: crea la pagina datata oggi, e la proprietà con lei.
  La linea del tempo invece continua a chiedere la proprietà, perché
  lì una pagina senza date non ha nemmeno una barra da disegnare

### 🆕 v0.26 — 22/09/2026 · 00:39 — Il menu del numero, come su OneNote
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Tenendo premuto sul numero** si apre il
  menu della lista, con le stesse quattro voci di OneNote: *Begin a new
  list here* (il conteggio riparte da 1 da lì), *Renumber as
  continuation of previous list* (toglie quel "riparti" e rimette la
  riga in fila), *Remove number* (la riga resta ma come paragrafo) e
  *Change to bullet*. Provate tutte e quattro sul telefono
- **[Nuova funzionalità]** **[📱 Mobile]** La voce *Renumber as continuation* c'è
  sempre ma è **spenta quando non serve**, cioè quando quella riga non
  fa ripartire niente: toglierla del tutto farebbe ballare il menu fra
  un'apertura e l'altra, e una voce grigia dice anche *perché* non si
  può fare
- **[Modifica]** **[📱 Mobile]** **Un tocco semplice sul numero non fa più
  ripartire il conteggio.** Era comodo da scoprire e scomodo da
  subire: rinumerava tutta la lista per un dito appoggiato male, senza
  chiedere niente. Ora quella è una voce del menu, dove la si sceglie
  apposta

### 🆕 v0.25 — 22/09/2026 · 00:28 — Raggruppare la tabella
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** La tabella può **raggruppare le righe in
  base a una proprietà**, come su Notion: ogni gruppo ha la sua
  intestazione col valore e quante pagine contiene, la sua riga di
  intestazione delle colonne e il suo "+ New page". Si sceglie dalle
  impostazioni, voce **Group** (`tableGroupColumnId`, migrazione
  17→18)
- **[Nuova funzionalità]** **[📱 Mobile]** Si può raggruppare per **quasi tutto**: tag
  singoli e multipli (l'intestazione è il tag colorato), caselle da
  spuntare (Checked/Unchecked), date (un gruppo per giorno), testo e
  numeri. Restano fuori "Creata il" e "Modificata il", che non sono
  celle ma proprietà della riga, e a istante esatto darebbero un
  gruppo per riga
- **[Nuova funzionalità]** **[📱 Mobile]** Con la **selezione multipla** una riga
  compare in **tutti** i gruppi dei suoi tag: è quello che ci si
  aspetta da un raggruppamento, ed è anche il motivo per cui la
  bacheca non la sostiene — lì una scheda dovrebbe stare in due
  colonne insieme
- **[Nuova funzionalità]** **[📱 Mobile]** **"+ New page" dentro un gruppo crea la
  pagina già con quel valore.** Verificato sul telefono: creata dentro
  il gruppo "PC", la riga nuova nasce col tag PC e resta lì, invece di
  finire fra quelle senza valore
- **[Nuova funzionalità]** **[📱 Mobile]** Il gruppo **"senza valore" sta sempre in
  fondo** e si chiama "No <proprietà>": davanti ruberebbe il primo
  sguardo alle righe già smistate. L'interruttore **"Hide empty
  groups"** nasconde i gruppi senza pagine, ed è acceso di suo, come
  su Notion; compare solo quando un raggruppamento c'è
- **[Nuova funzionalità]** **[📱 Mobile]** I gruppi si **chiudono e riaprono** toccando
  l'intestazione. Quale sia chiuso non si salva nel database: è come
  si sta guardando la tabella adesso, non una proprietà del database
- **[Nuova funzionalità]** **[📱 Mobile]** Il raggruppamento della tabella è
  un'impostazione **diversa** da "Group by" della bacheca, di
  proposito: la bacheca senza colonne non esiste e quindi ripiega su
  una proprietà qualsiasi, la tabella invece funziona benissimo piatta
  e non deve rimescolarsi da sola. Cambiare vista non tocca l'altra
- **[Nuova funzionalità]** **[📱 Mobile]** Un tag rimasto nelle celle dopo che
  l'opzione è stata cancellata **non fa sparire le sue righe**: gli si
  dà comunque un gruppo, in fondo a quelli dichiarati

### 🔧 v0.24.2.2 — 22/09/2026 · 00:03 — Copertine dritte, e la tastiera che non sparisce
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** **Le copertine prese dalla galleria non escono più
  coricate.** Le foto scattate col telefono ruotato non vengono salvate
  ruotate: i pixel restano come li ha letti il sensore e dentro al file
  c'è un'etichetta (EXIF) che dice di quanto girarli. La galleria la
  legge, `BitmapFactory` no. Ora `PageImageStore` la legge e applica la
  rotazione alla copia che si guarda — il file non viene toccato.
  Provato fabbricando un JPEG con l'etichetta "ruota di 90" e
  guardandolo dentro l'app
- **[Bug fix]** **[📱 Mobile]** **La tastiera non sparisce più** creando, cancellando o
  uscendo da una casella da spuntare. Erano due cose insieme: il
  cursore veniva richiesto sul campo nuovo solo `if (!imeVisible)`, ma
  subito dopo un cambio di fuoco la tastiera **risulta ancora aperta
  mentre sta sparendo**, quindi la richiesta veniva saltata e la
  tastiera se ne andava; e l'effetto che spegne il cursore quando la
  tastiera si chiude scattava anche mentre il cursore lo stavamo
  spostando noi. Ora la tastiera si chiede sempre (chiederla aperta non
  fa niente) e quell'effetto si ferma finché c'è una richiesta di fuoco
  in viaggio. Verificato con `dumpsys input_method`: `mInputShown=true`
  dopo l'Invio, dopo l'uscita dalla lista e dopo il backspace che
  toglie la casella

### 🔧 v0.24.2.1 — 21/09/2026 · 20:49 — L'Invio continua la lista anche partendo da zero
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** **L'Invio continua la lista anche quando sotto ci sono
  righe vuote.** Scrivendo `0.` (o qualsiasi numero), poi il testo, poi
  Invio, la riga nuova usciva come paragrafo e la lista si fermava. La
  causa non era la velocità: una riga vuota inserita in mezzo ad altre
  righe vuote è **indistinguibile** dalle sue vicine, e il confronto
  attribuiva l'inserimento all'ultima riga del gruppo, facendo
  ereditare il tipo dal blocco sbagliato. Ora il campo dice anche dove
  sta il cursore. Trovato col log dopo una diagnosi sbagliata: la voce
  per esteso, con l'errore incluso, sta in "Strade già tentate"

### ✏️ v0.24.2 — 21/09/2026 · 20:33 — Caselle da spuntare: cancellarle e uscirne
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** **L'app non si chiude più premendo Invio su una casella
  vuota.** Quella casella diventa un paragrafo (giusto, come Notion),
  ma così facendo cambia specie: da riga a sé a testo scorrevole, che
  per la UI è un posto diverso, e il campo di prima viene distrutto. Se
  succede mentre la tastiera è ancora attaccata a quel campo, lei
  continua a chiedergli dov'è il cursore e Compose 1.6 va in crash
  (`LayoutCoordinate operations are only valid when isAttached is
  true`). Ora il fuoco viene tolto **prima** che la riga sparisca, e
  rimesso subito dopo sul campo nuovo. Riprodotto in modo
  deterministico prima della correzione e riprovato quattro volte dopo
- **[Modifica]** **[📱 Mobile]** **Backspace a inizio casella toglie la
  casella** e lascia il testo, che diventa una riga normale — lo stesso
  gradino che il testo scorrevole fa già per gli elenchi. Da lì in poi
  la riga si fonde con quella sopra come tutte le altre: prima le
  caselle non si riuscivano a cancellare in nessun modo dalla
  tastiera
- **[Modifica]** **[📱 Mobile]** **Backspace a inizio della riga sotto una
  casella la fonde dentro la casella.** Prima lì non succedeva niente:
  sembrava di essere in cima alla pagina mentre sopra c'era ancora
  roba. Le due cose insieme fanno sì che si possa cancellare
  all'indietro attraversando le caselle, senza trovare muri
- **[Modifica]** **[📱 Mobile]** Per accorgersi del backspace a inizio riga,
  anche il campo delle caselle ha ora **l'a-capo nascosto in testa**
  (`RUN_LEAD`), lo stesso meccanismo del campo unito: la tastiera non
  manda eventi tasto intercettabili, ma se il backspace si mangia
  quell'a-capo il testo cambia, e un cambiamento si vede
- **[Bug fix]** **[📱 Mobile]** **La pagina non salta più portando in cima la riga a
  fuoco.** Lo scorrimento automatico aggiunto per il blocco nato dietro
  la tastiera usava `animateScrollToItem`, che incolla la riga al bordo
  superiore: siccome una riga di testo scorrevole è alta quanto tutto
  il gruppo, dopo un Invio o una fusione le caselle sopra finivano
  fuori schermo e sembravano sparite (non lo erano: bastava scorrere,
  e nel database c'erano tutte). Ora si scorre **solo se la riga è
  tutta fuori**, e solo di quanto serve
- **[Bug fix]** **[📱 Mobile]** Il quadratino è alzato di 4,5dp invece di 3: misurando
  ingrandendo lo schermo, il suo centro restava sotto il centro ottico
  delle maiuscole

### ✏️ v0.24.1 — 21/09/2026 · 20:09 — Un elenco numerato può partire da zero
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** **Un elenco può partire da zero.** Di suo
  parte da 1, ma scrivendo `0.` parte da 0 e poi prosegue normale: 0,
  1, 2. Serve quando c'è un passo zero — un preambolo prima del primo
  punto vero. Era un solo controllo a vietarlo
  (`numberedListPrefixJustTyped` rifiutava i numeri sotto l'uno): il
  resto reggeva già, perché il numero di partenza è un campo salvato
  sul blocco (`numberStartsAt`) e il conteggio riparte da lì.
  Verificato sul telefono e nel database: il blocco risulta salvato
  con numero di partenza 0, e le righe dopo sono `1.` e `2.`

### 🔧 v0.24.0.1 — 21/09/2026 · 19:48 — Caselle da spuntare: in riga col testo, con la maiuscola, e l'Invio non si blocca
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Il quadratino è **più piccolo e in riga col testo**.
  Era il `Checkbox` di Material, che porta con sé una zona di tocco da
  48dp e un quadrato da 20 che non si rimpiccioliscono: accanto a una
  riga alta 24 sembrava un bottone piazzato lì in mezzo. Ora è
  disegnato a mano (`CheckboxMark`), alto quanto le lettere, con la
  zona di tocco che resta larga. Va anche **alzato di 3dp**: centrarlo
  sulla riga non basta, perché dentro la riga il testo non sta in
  mezzo — sotto la linea di base c'è lo spazio per le code di g e p, e
  le lettere appaiono più in alto. Misurato sul telefono
- **[Bug fix]** **[📱 Mobile]** **La maiuscola a inizio frase** ora arriva anche nei
  blocchi, caselle comprese: va chiesta alla tastiera
  (`KeyboardCapitalization.Sentences`) e non era mai stata chiesta.
  Verificato guardando la tastiera: a casella vuota mostra le lettere
  maiuscole e lo shift acceso
- **[Bug fix]** **[📱 Mobile]** **Invio + scrivere in fretta non impasta più tutto in
  una riga sola.** Era il bug per cui, facendo Invio e scrivendo senza
  aspettare, le lettere finivano tutte attaccate nella riga di prima,
  la tastiera si chiudeva e la barra sopra restava in uno stato
  sbagliato. Tre cause diverse, trovate col log e sistemate una per
  una: la tempesta di ridisegni della rinumerazione, la riga nuova che
  nasceva dietro la tastiera e quindi non esisteva per il fuoco, e
  l'eco della tastiera che faceva dividere due volte lo stesso pezzo.
  Le prime due hanno la loro voce in "Strade già tentate"
- **[Bug fix]** **[📱 Mobile]** **Invio su una casella vuota** la trasforma in
  paragrafo — come Notion — ma prima lasciava il cursore per strada e
  la tastiera si chiudeva in faccia: il blocco passa da "isola" a
  testo scorrevole, che per la UI è un altro posto, e il campo di
  prima viene distrutto. Ora il fuoco viene richiamato

### 🆕 v0.24 — 20/09/2026 · 10:58 — La copertina si sposta e si ingrandisce
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** La copertina si **sposta e si ingrandisce**:
  dalla finestra della copertina c'è "Reposition", e da lì il dito la
  trascina in tutte le direzioni e la pizzicata la ingrandisce fino a
  quattro volte. In alto una riga dice come si fa — un'immagine che
  smette di rispondere al tocco senza spiegare perché sembra rotta — e
  in basso ci sono "Cancel" e "Save"
- **[Nuova funzionalità]** **[📱 Mobile]** **Il file non viene mai toccato.** Nel
  database finiscono tre numeri (`coverScale`, `coverOffsetX`,
  `coverOffsetY`, migrazione 16→17): l'immagine resta intera alla sua
  risoluzione, e la striscia fa da finestra. Si può cambiare
  inquadratura all'infinito senza consumare niente, e togliendo
  l'ingrandimento si torna esattamente all'originale
- **[Nuova funzionalità]** **[📱 Mobile]** Lo spostamento è salvato in **frazioni di
  striscia**, non in pixel: la stessa inquadratura vale su qualunque
  schermo, anche ruotando il telefono
- **[Nuova funzionalità]** **[📱 Mobile]** L'inquadratura di prova vive nella
  schermata finché non si tocca "Save": scrivere nel database ad ogni
  millimetro di dito vorrebbe dire non poter più annullare, e far
  scrivere il disco sessanta volte al secondo
- **[Nuova funzionalità]** **[📱 Mobile]** L'immagine **non si può trascinare fuori**
  dalla striscia: il limite è calcolato su quanto sporge davvero
  (`coverPanLimit`), perché lasciarla scappare vorrebbe dire restare a
  guardare un rettangolo vuoto
- **[Nuova funzionalità]** **[📱 Mobile]** Cambiando copertina l'inquadratura **torna a
  zero**: era stata scelta guardando un'altra immagine, e su quella
  nuova darebbe un ritaglio deciso a caso
- **[Nuova funzionalità]** **[📱 Mobile]** Ingrandita, l'immagine viene **letta più
  fine** (fino al doppio della larghezza dello schermo), altrimenti a
  ingrandimento 2 si vedrebbe sgranata. Il tetto lo mette
  `PageImageStore`, che non porta in memoria più di sei megapixel: una
  foto molto grande, al massimo dell'ingrandimento, può restare un po'
  morbida. È il prezzo per non far chiudere l'app

### ✏️ v0.23.1 — 20/09/2026 · 10:48 — Le icone della barra della vista più piccole
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Le icone della barra della vista sono **più
  piccole** (20dp invece di 24, dentro un bottone da 40 invece che da
  48). La misura sta in `TOOLBAR_ICON_SIZE` e si usa attraverso
  `ToolbarIconButton`: le icone lì sopra devono restare tutte uguali
  fra loro, comprese quelle che si aggiungeranno, e riscrivere la
  misura a mano ogni volta è il modo sicuro perché prima o poi una sia
  diversa dalle altre

### 🔧 v0.23.0.1 — 20/09/2026 · 10:44 — Rinominare un database a schermo intero non si perde più
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** **Rinominare un database a schermo intero non si
  perde più tornando indietro.** La pagina era letta una volta sola
  all'apertura, e la stessa pagina sta aperta in due schermate insieme
  con due ViewModel diversi: quella rimasta nella pila di navigazione
  aveva in mano il titolo vecchio, e siccome ogni modifica salva la
  pagina **intera**, alla prima cosa che si toccava lì dentro — bastava
  riaccendere "Show title" — il titolo vecchio tornava nel database e
  la rinomina spariva. Verificato sul telefono che il nome nuovo veniva
  davvero salvato e poi sovrascritto, non semplicemente non salvato.
  Ora la pagina è **seguita nel tempo** (`PageDao.observeById`), in
  tutti e due i ViewModel: le due schermate vedono sempre la stessa
  cosa e nessuna delle due riscrive quello che aveva in mano

### 🆕 v0.23 — 20/09/2026 · 10:30 — "Show title" per i database dentro le pagine
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **"Show title"** nelle stesse impostazioni:
  un interruttore che nasconde il titolo del database **quando è dentro
  una pagina**. Serve a chi il titolo l'ha già scritto sopra come
  intestazione e si ritrova a leggerlo due volte. La scelta è salvata
  per ogni database (`showEmbeddedTitle`, migrazione 15→16)
- **[Nuova funzionalità]** **[📱 Mobile]** **A schermo intero il titolo c'è sempre**, e
  l'interruttore lì non compare nemmeno. Non è una dimenticanza:
  aperto da solo, il titolo è l'unica cosa che dice dove si è finiti, e
  poterlo togliere vorrebbe dire poter arrivare a una schermata senza
  nome. Verificato sul telefono: spento dentro la pagina, il titolo
  sparisce; aperto lo stesso database a schermo intero, è ancora lì

### ✏️ v0.22.1 — 20/09/2026 · 10:21 — Un menu solo per il database: Delete nelle impostazioni, "Open as full page" accanto a Sort
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Via i tre puntini accanto al titolo del
  database. **"Delete" è passato nelle impostazioni** — l'icona con le
  lineette e i pallini — in un gruppo a sé sotto quelle della vista:
  due menu diversi per le cose di uno stesso database erano un posto
  in più dove cercare, e il secondo conteneva due voci sole
- **[Modifica]** **[📱 Mobile]** **"Open as full page" è invece un'icona
  nella barra**, subito a destra di Sort. Le due cose sembrano simili ma non lo
  sono: aprire a schermo intero si fa spesso e di fretta, perché la
  tabella dentro la pagina sta stretta, mentre cancellare si fa una
  volta sola e non deve capitare per sbaglio. La prima merita un
  tocco, la seconda due. L'icona compare solo dove ha senso: a
  schermo intero il database è già aperto, e lì non c'è

### 🆕 v0.22 — 20/09/2026 · 10:17 — Ordinare le righe (Sort)
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Le righe si possono **ordinare per una
  qualsiasi proprietà**, crescente o decrescente, dall'icona **Sort**
  nella barra della vista — quella con le tre lineette che si
  accorciano, come su Notion. La scelta è salvata per ogni database
  (`sortColumnId` e `sortDescending`, migrazione 14→15)
- **[Nuova funzionalità]** **[📱 Mobile]** Il confronto dipende dal tipo: i numeri si
  confrontano **come numeri** (altrimenti "10" verrebbe prima di "9"),
  le date come istanti, le date di creazione e modifica dalla riga
  stessa, e tutto il resto alfabeticamente senza distinguere maiuscole
  e minuscole. Verificato sul telefono con 9, 10 e 100: crescente dà
  9-10-100 e decrescente 100-10-9, non l'ordine alfabetico 10-100-9
- **[Nuova funzionalità]** **[📱 Mobile]** Si può ordinare anche per **nome della
  pagina**, che è una colonna della tabella ma non una proprietà del
  database: ha un identificatore riservato (`SORT_BY_NAME`) che
  nessun id vero può avere
- **[Nuova funzionalità]** **[📱 Mobile]** **Le celle vuote stanno in fondo in tutti e
  due i versi.** Non è una svista: invertendo l'ordine ci si aspetta di
  vedere in cima l'ultimo valore, non una fila di righe senza niente
  scritto
- **[Nuova funzionalità]** **[📱 Mobile]** L'icona Sort è **accesa di blu** quando un
  ordinamento c'è: altrimenti non si saprebbe che le righe non sono
  più nell'ordine in cui sono state create. Il verso si cambia
  toccando l'ordinamento attivo dentro la finestra — sono due stati
  della stessa cosa, non due voci diverse

### 🆕 v0.21 — 20/09/2026 · 09:38 — Icone e copertine delle pagine
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Ogni pagina può avere un'**icona** e una
  **copertina** scelte dall'utente, la pagina radice compresa. Si tocca
  l'icona (o la copertina, o "Add cover" quando non c'è) e si sceglie
  fra **Upload** e **Link**. Campi nuovi `iconImage` e `coverImage`
  (migrazione 13→14): senza immagine si continua a vedere l'emoji di
  sempre
- **[Nuova funzionalità]** **[📱 Mobile]** Upload usa il **selettore di foto di
  sistema**, che non chiede il permesso di leggere tutta la galleria:
  l'utente sceglie una foto e l'app vede solo quella. Sul telefono si
  legge "notE avrà accesso solo alle foto che selezioni"
- **[Nuova funzionalità]** **[📱 Mobile]** **Il collegamento viene scaricato una volta
  sola e il file resta sul telefono.** È la decisione importante di
  questa funzione: tenere l'indirizzo e scaricare l'immagine ogni volta
  avrebbe voluto dire un'app che senza rete mostra dei buchi, e che
  perde le immagini quando il sito le toglie. Così invece è una copia
  come tutto il resto. Il prezzo, dichiarato: se l'immagine cambia
  dall'altra parte, la nostra no
- **[Nuova funzionalità]** **[📱 Mobile]** Per questo è comparso il permesso
  **INTERNET** nel manifest — l'unica cosa per cui l'app va in rete,
  solo quando si incolla un collegamento, mai da sola
- **[Nuova funzionalità]** **[📱 Mobile]** Nel database si salva il **nome** del file e
  non il percorso intero: la cartella dell'app cambia posto fra
  un'installazione e l'altra, o al ripristino di un backup, e un
  percorso scritto nel database diventerebbe un puntatore a niente
- **[Nuova funzionalità]** **[📱 Mobile]** Sostituire o togliere un'immagine
  **cancella il file vecchio**. Senza, la cartella si riempirebbe di
  immagini che nessuno può più raggiungere. Verificato: dopo aver
  tolto icona e copertina la cartella è vuota
- **[Nuova funzionalità]** **[📱 Mobile]** Le immagini si caricano già rimpicciolite a
  quanto serve davvero, con la misura presa dal riquadro che le
  contiene. Una foto da dodici megapixel in memoria sono quarantotto
  megabyte: caricarla intera per mostrarla larga un centimetro farebbe
  chiudere l'app
- **[Nuova funzionalità]** **[📱 Mobile]** Anche il blocco "collegamento a pagina"
  mostra l'immagine, se la pagina ne ha una: altrimenti la stessa
  pagina avrebbe due facce diverse a seconda di dove la si guarda

### ✏️ v0.20.1 — 19/09/2026 · 22:35 — Chiusa la tastiera, il cursore si spegne
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Chiusa la tastiera, **il cursore si
  spegne** — nell'editor, nel titolo e nelle celle dei database, sia
  incorporati che a schermo intero. Prima il gesto indietro chiudeva
  solo la tastiera e lasciava il cursore a lampeggiare dentro il
  campo, come se si stesse ancora scrivendo lì. Il segnale non è il
  gesto ma la tastiera che si chiude, comunque la si chiuda: col
  gesto indietro la tastiera se lo mangia e all'app non arriva niente
  (vedi "Strade già tentate"). C'è un'attesa di 250 ms prima di
  spegnere il cursore, perché la tastiera Samsung sparisce e riappare
  per un istante durante i cambi di fuoco: se in quell'istante si
  togliesse il fuoco, si romperebbe l'Invio che passa da un blocco al
  successivo. Se la tastiera torna, l'attesa viene annullata. Provato
  scrivendo tre righe separate da Invio: nessuna si perde

### 🆕 v0.20 — 19/09/2026 · 22:33 — L'ora nella proprietà Date
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** La proprietà data può avere **anche l'ora**,
  come su Notion: un interruttore "Include time" nella finestra della
  data, spento di default. Acceso, compaiono "Start time" e — se c'è
  una data di fine — "End time". Spento, la data continua a valere il
  giorno intero, che è quello che serve alla grandissima parte delle
  pagine
- **[Nuova funzionalità]** **[📱 Mobile]** Il valore della cella non ha cambiato
  forma: erano già millisecondi, e i millisecondi l'ora ce l'hanno
  dentro. È stato aggiunto un terzo campo, `t`, che dice se **conta**.
  Non si indovina dai millisecondi: indovinarlo vorrebbe dire dire "se
  non cade a mezzanotte allora c'è un'ora", e una pagina messa davvero
  a mezzanotte perderebbe la sua. I valori scritti prima non hanno il
  terzo campo e valgono "senza ora", che è quello che erano — nessuna
  migrazione necessaria
- **[Nuova funzionalità]** **[📱 Mobile]** La finestra della data è diventata una
  finestra a scomparsa come quella di Notion, con i pulsanti in testa.
  In fondo, sotto al calendario e agli interruttori, finivano fuori
  schermo: si sceglieva una data senza poterla confermare. Stessa
  lezione della finestra delle proprietà. C'è anche **Clear**, che
  prima si otteneva solo confermando senza aver scelto niente
- **[Bug fix]** **[📱 Mobile]** Il selettore di Material lavora in **mezzanotte UTC**,
  non locale, e il codice ne prendeva i millisecondi e li salvava
  così com'erano. In Italia non si notava — mezzanotte UTC è l'una o
  le due del mattino dello stesso giorno — ma a ovest di Greenwich la
  data si sarebbe spostata indietro di un giorno. Ora la conversione
  passa sempre per il giorno, in un senso e nell'altro
- **[Nuova funzionalità]** **[📱 Mobile]** La linea del tempo colloca le barre
  all'**istante** e non al giorno: con l'ora accesa, a ingrandimento
  "ore" una pagina che comincia alle 14:30 sta a metà pomeriggio. La
  posizione è in giorni con la frazione, non in giorni interi. Una
  pagina senza ora continua a occupare il giorno intero, e una con
  l'ora ma senza fine è un istante — si vede lo stesso perché le
  barre hanno una larghezza minima
- **[Nuova funzionalità]** **[📱 Mobile]** Anche l'ordine delle righe nel calendario e
  nella linea del tempo guarda l'istante: due pagine dello stesso
  giorno a ore diverse hanno un ordine naturale, ed è quello

### 🆕 v0.19 — 19/09/2026 · 22:15 — Sette ingrandimenti della Timeline
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Sette ingrandimenti** come su Notion —
  Hours, Day, Week, Bi-week, Month, Quarter, Year — dalla stessa
  finestra ("Select a zoom level"), salvati per ogni database
  (`PageEntity.timelineZoom`, migrazione 12→13). Manca "5 anni":
  a quella scala un mese è largo due millimetri e non si distingue
  più niente. Il modello è uno solo per tutti: **la larghezza di un
  giorno**. Da lì discende tutto — dove comincia una barra, quanto è
  lunga, dove cade adesso — e le caselle dell'asse sono larghe quanti
  giorni durano, così un mese di ventotto giorni è più stretto di uno
  di trentuno e le barre restano incollate alle loro date invece di
  scivolare rispetto alle etichette
- **[Bug fix]** **[📱 Mobile]** Agli ingrandimenti larghi i numeri dei giorni si
  leggevano sbagliati — "2" dove c'era scritto "25". La casella di un
  giorno è più stretta di due cifre e il testo veniva tagliato a metà.
  Ora si misura libero e sborda sulle caselle accanto, che tanto sono
  mute (`wrapContentWidth(unbounded = true)`), e il numero viene
  scritto una casella ogni tot invece che su tutte
- **[Bug fix]** **[📱 Mobile]** A ingrandimento "Quarter" le etichette delle settimane
  si accavallavano: una casella da una settimana è larga 22 punti e
  "13 Sep" ne vuole il doppio. Ora portano solo il giorno, una
  settimana sì e una no; il mese lo dice l'intestazione ferma sopra
  l'asse
- **[Bug fix]** **[📱 Mobile]** A ingrandimento "ore" la linea del tempo si apriva a
  mezzanotte, cioè mezza giornata lontano da quello che sta
  succedendo. Il bersaglio dell'apertura (e del pulsante Today) è
  l'**istante** di adesso, non la mezzanotte di oggi

### 🆕 v0.18 — 19/09/2026 · 21:55 — La vista Timeline
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Timeline**: un asse di giorni che scorre
  in orizzontale e una riga per pagina, con la barra lunga quanto i
  giorni che copre. È il calendario srotolato — lì il tempo va a capo
  ogni settimana e una pagina lunga si spezza in più pezzi, qui il
  tempo è una riga sola e la barra è intera, che è tutto il punto di
  una linea del tempo: confrontare a colpo d'occhio durate e
  sovrapposizioni
- **[Nuova funzionalità]** **[📱 Mobile]** Le righe seguono **lo stesso ordine del
  calendario** — le pagine per data, la prima in alto, con le stesse
  regole per i pareggi — così passando da una vista all'altra non si
  perde il filo. Qui però ogni riga ha per forza la sua pagina, quindi
  non restano righe vuote
- **[Nuova funzionalità]** **[📱 Mobile]** I nomi stanno in una colonna ferma a
  sinistra: scorrendo di mesi le barre se ne vanno, e senza i nomi
  accanto non si saprebbe più di chi è quella che si sta guardando
- **[Nuova funzionalità]** **[📱 Mobile]** Il mese sta in un'intestazione ferma sopra
  l'asse e segue il primo giorno che si vede. Scritto sull'asse
  starebbe all'inizio del suo tratto e scorrendo di qualche giorno
  sparirebbe a sinistra, lasciando una fila di numeri senza sapere di
  quale mese sono. Legge la posizione dello scorrimento **dentro di
  sé**, così mentre il dito trascina si ridisegna solo quella scritta
  e non tutta la linea del tempo
- **[Nuova funzionalità]** **[📱 Mobile]** I fine settimana si riconoscono dallo
  sfondo invece che da una lettera, e i giorni portano solo il numero:
  le caselle vengono composte tutte insieme, quindi ogni testo in meno
  è un testo in meno moltiplicato per la lunghezza dell'asse

### 🆕 v0.17 — 19/09/2026 · 20:14 — Today nel calendario, sempre al suo posto
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Pulsante **Today** a destra dei tre
  pulsanti della modalità: riporta a oggi da qualunque distanza, in
  tutte e tre. Sta lì **sempre**, anche quando si è già su oggi e non
  farebbe niente: era stato fatto comparire solo da lontano, ma un
  pulsante che compare e sparisce costringe a cercarlo ogni volta e fa
  cambiare aspetto alla riga da sola mentre si sfoglia. Scelta
  dell'utente, ed è quella giusta

### ✏️ v0.16.1 — 19/09/2026 · 20:07 — Ogni pagina ha la sua riga nel calendario, e la settimana non scatta più
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Cambiando settimana col dito le pagine sparivano e
  ricomparivano con uno scatto. Al termine dello scorrimento cambiano
  due cose — la settimana mostrata e l'azzeramento dello spostamento —
  e venivano scritte una dopo l'altra: potevano finire in due
  fotogrammi diversi, e in quello di mezzo si vedeva la settimana
  vecchia già al centro, o quella nuova ancora di lato. Ora le due
  scritture vengono applicate in un colpo solo
  (`Snapshot.withMutableSnapshot`), così non esiste un fotogramma che
  ne veda una senza l'altra. Lo spostamento è diventato uno stato
  normale invece di un `Animatable` proprio per poterlo fare: due
  valori si rendono veri insieme solo se sono tutti e due stati
  normali
- **[Bug fix]** **[📱 Mobile]** Lo scatto però non era finito lì, e la causa vera era
  un'altra: **la molla dell'animazione rimbalzava**. Lanciata dalla
  spinta del dito superava la meta e tornava indietro, e superare la
  meta vuol dire scoprire il bordo oltre la settimana accanto, dove
  non c'è disegnato niente. Con `input swipe` non si vedeva, perché un
  trascinamento sintetico ha una spinta molto più debole di un dito:
  è per questo che le prime due volte sembrava a posto. Ora la molla è
  `DampingRatioNoBouncy` — non supera mai la meta — e il
  trascinamento è limitato a una settimana per parte, così nemmeno
  il dito può scoprire il vuoto. Verificato registrando lo schermo e
  leggendo il numero del primo giorno **fotogramma per fotogramma**:
  la sequenza sale monotona fino alla settimana nuova e si ferma lì,
  senza oltrepassarla e tornare indietro.
  **Nota per chi rilegge quelle registrazioni**: `screenrecord` chiude
  il file riemettendo un fotogramma vecchio, quindi l'ultimo
  fotogramma mostra spesso la settimana di partenza. È un artefatto
  della registrazione, non dell'app — succede in tutte e tre le
  registrazioni fatte, sempre in fondo
- **[Modifica]** **[📱 Mobile]** Le pagine del calendario si comportano come
  **blocchi impilati**: ordinate per data dall'alto verso il basso,
  ognuna con la sua riga, e **la riga non cambia mai**. Quando la data
  di una pagina finisce, la sua riga resta **vuota** nelle settimane
  successive invece di essere occupata da quella sotto — così le barre
  non si rimescolano e seguirne una con l'occhio è immediato.
  La riga viene decisa **una volta sola su tutto il database** e non
  settimana per settimana: è quella la differenza. Deciderla dentro la
  settimana vorrebbe dire farla dipendere da chi altro c'è lì, cioè
  farla cambiare appena una pagina entra o esce — ed era esattamente il
  difetto, con la stessa pagina in alto nella settimana in cui era sola
  e più in basso in quella dopo. L'ordine guarda la data della pagina,
  non il giorno in cui la barra entra nella settimana: una pagina che
  viene da prima resta sopra a una che comincia lunedì. A parità di
  data l'ordine è comunque sempre lo stesso, altrimenti due pagine
  dello stesso giorno si scambierebbero di posto ad ogni ridisegno
- **[Modifica]** **[📱 Mobile]** Le righe si contano su **tutto il
  database**, non sul periodo che si sta guardando: una pagina tiene la
  sua riga in ogni settimana, in ogni mese e in ogni anno, per sempre.
  È una scelta esplicita dell'utente, fatta sapendone il prezzo.
  Contarle sul periodo (come si faceva prima) le fa ricompattare ad
  ogni cambio di mese: nessuna pagina ruba la riga di un'altra, ma la
  stessa pagina si ritrova a un'altezza diversa a seconda di cosa c'è
  attorno. Contarle su tutto costa spazio vuoto — la decima pagina in
  ordine di data sta alla decima riga anche nelle settimane in cui è
  l'unica, con nove righe vuote sopra — e ogni settimana è alta quanto
  la griglia intera, perché la griglia delle righe è una cosa sola
- **[Modifica]** **[📱 Mobile]** Lo scorrimento della settimana è ora un
  `HorizontalPager`, lo stesso componente con cui si sfogliano le
  pagine ovunque su Android. Prima era scritto a mano: tre settimane
  affiancate, uno scostamento seguito col dito e una molla per farlo
  atterrare. Rifarsi la fisica dello scorrimento vuol dire rifare
  anche il lancio, l'aggancio alla pagina, il gesto afferrato a metà
  corsa e la velocità al rilascio — ognuna di quelle cose è un modo di
  sbagliare, e quella giusta esiste già. Le settimane ai lati vengono
  preparate prima che il dito si muova (`beyondBoundsPageCount = 1`),
  e il calcolo dei pezzi e delle loro righe è memorizzato invece di
  essere rifatto ad ogni fotogramma
- **[Bug fix]** **[📱 Mobile]** Nella tabella, toccare una cella di tipo data apriva
  il calendario **solo centrando il trattino**, largo pochi punti: da
  qualsiasi altro punto della cella il tocco cadeva sulla pagina sotto
  il database, che rispondeva mettendosi a scrivere in fondo. La zona
  toccabile era il testo invece della cella. Ora è tutta la cella, con
  il margine laterale spostato **dentro** di essa in modo da non
  lasciare fuori nemmeno i bordi (`CellContent`, parametro
  `cellPadding`)

### 🆕 v0.16 — 19/09/2026 · 15:35 — La vista ad anno, e la settimana che si sfoglia col dito
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Vista ad anno**, a sinistra di Mese e
  Settimana: dodici mesi in miniatura, tre per riga, tutto l'anno in
  una schermata. Niente barre — a un centimetro per mese non si
  leggerebbero — ma i giorni con almeno una pagina sono cerchiati, e
  basta a far vedere dove si concentra il lavoro. Toccando un mese lo
  si apre da vicino, che è l'unica cosa sensata da fare con un mese
  alto un centimetro
- **[Nuova funzionalità]** **[📱 Mobile]** Le tre modalità hanno sostituito il vecchio
  sì/no `calendarWeekView` con `PageEntity.calendarMode`
  (migrazione 11→12), che travasa il valore precedente. La colonna
  vecchia **resta nel database**: SQLite sa cancellare una colonna solo
  dalla 3.35, più recente del minimo che l'app sostiene, e ricreare la
  tabella delle pagine per togliere un campo inutilizzato è un rischio
  che non vale il guadagno. Stessa scelta già fatta per
  `numberResetHere`
- **[Nuova funzionalità]** **[📱 Mobile]** Nella settimana si passa da una all'altra
  **trascinando col dito**. Dentro una finestra larga una settimana ne
  stanno tre affiancate — la precedente, quella mostrata, la successiva
  — e il trascinamento sposta la fila, così sotto il dito si vede
  arrivare davvero la settimana accanto invece di vederla comparire di
  colpo al rilascio. Al rilascio, superato un quarto di schermo o con
  una spinta decisa, la fila finisce di scorrere e la settimana
  mostrata cambia; l'offset torna a zero nello stesso istante, quindi
  al centro resta lo stesso contenuto e non si vede nessun salto. Le
  tre settimane sono alte uguali apposta: se ognuna prendesse la
  propria altezza, trascinando si vedrebbe la riga crescere e
  rimpicciolirsi
- **[Bug fix]** **[📱 Mobile]** Al primo trascinamento la settimana compariva per un
  istante e poi spariva tutto. Le tre settimane erano una `Row` larga
  tre schermi, e lì **due vincoli di larghezza si sono sommati**:
  dentro una `Row` un figlio senza peso viene misurato con *lo spazio
  che avanza*, e `Modifier.width` si lascia schiacciare dentro il
  vincolo che riceve — la prima settimana si prendeva tutta la
  larghezza e alle altre due ne restava zero. Passando a
  `requiredWidth` diventavano visibili, ma si scopriva il secondo
  problema: **una fila più larga del riquadro che la contiene non
  viene piazzata dove ci si aspetta**, risultava già spostata di una
  settimana per conto suo, e sopra ci si sommava lo scostamento del
  dito — così l'intestazione diceva una settimana e la griglia ne
  mostrava un'altra. Via la `Row`: ora le tre settimane sono
  sovrapposte nello stesso riquadro, ognuna larga esattamente quanto
  lui, e ognuna si sposta da sé di una larghezza a sinistra, zero, una
  a destra. Nessuna misura da contrattare e nessun piazzamento da
  indovinare. Stessa famiglia del vecchio bug delle linee della
  tabella invisibili: **quando una misura sembra sparire, è quasi
  sempre il contenitore che sta dando al figlio un vincolo diverso da
  quello che si immagina**

### 🆕 v0.15 — 19/09/2026 · 14:56 — Barre lunghe quanto i giorni, e la vista a settimana
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Barre lunghe quanto i giorni coperti**.
  Ogni settimana calcola da sé quali pagine la attraversano e le
  dispone su corsie, le più lunghe in alto, in modo che due pagine
  sovrapposte nel tempo non finiscano una sopra l'altra
  (`calendarLanes`). Le larghezze sono **pesi, non misure**: una barra
  di tre giorni occupa tre settimi della riga su qualunque schermo. Una
  pagina a cavallo di due settimane produce due pezzi, e il lato dove
  prosegue viene disegnato squadrato invece che arrotondato
- **[Nuova funzionalità]** **[📱 Mobile]** **Vista a settimana**, accanto a quella a
  mese (`PageEntity.calendarWeekView`, migrazione 10→11). Nel mese
  l'altezza di una settimana è fissa — sei settimane devono entrare
  nello schermo — e le barre oltre la terza diventano un "+N"; nella
  settimana l'altezza segue quante pagine ci sono davvero, perché lo
  spazio c'è e nasconderle sarebbe gratuito

### 🆕 v0.14 — 19/09/2026 · 14:42 — Date di inizio e di fine
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Intervalli di date**: una cella data può
  avere un inizio e una fine ("Date started"/"Date finished"), scelti
  con il selettore a intervallo di Material 3. Sono salvati come due
  numeri separati da un trattino nella stessa cella, così il tipo della
  colonna non cambia e le date singole già scritte continuano a
  leggersi

### 🆕 v0.13 — 19/09/2026 · 14:38 — La vista Calendar
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Calendar**: le pagine collocate nel giorno
  della loro proprietà data, mese per mese, con i giorni fuori mese
  smorzati e oggi evidenziato. La proprietà si sceglie da Settings →
  Date property (`PageEntity.calendarDateColumnId`, migrazione 9→10);
  se non è scelta si usa la prima data che c'è
- **[Bug fix]** **[📱 Mobile]** In Calendar e Board `+ New page` sembrava non creare
  niente: la pagina nasceva senza data e senza tag, cioè **fuori da
  quello che si stava guardando** — la si ritrovava solo passando a
  Table. Ora nel calendario nasce datata oggi e il calendario torna al
  mese corrente per mostrarla; nella bacheca nasce già assegnata alla
  colonna
- **[Bug fix]** **[📱 Mobile]** Quando la proprietà su cui la vista si regge non
  esiste, `+ New page` spariva e restava solo un messaggio: creava
  pagine invisibili. Ora il messaggio offre di creare la proprietà che
  manca (`MissingPropertyNotice`), e il pulsante torna appena c'è
- **[Nuova funzionalità]** **[📱 Mobile]** Toccare un giorno crea lì una pagina già
  datata. I giorni stanno in uno strato sotto e le barre in uno sopra:
  gli spazi vuoti fra una barra e l'altra lasciano passare il tocco al
  giorno, mentre toccare una barra apre la sua pagina

### ✏️ v0.12.1 — 19/09/2026 · 14:11 — Il pulsante della vista sotto il titolo cambia la vista
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Il pulsante della vista sotto il titolo apre
  il selettore del layout (prima era un'etichetta inerte)

### 🔧 v0.12.0.1 — 19/09/2026 · 14:06 — Toccando sotto il database si scrive sotto, e il testo non finisce dietro la tastiera
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Toccare sotto l'ultimo blocco riportava a scrivere
  *sopra* di lui quando non era scrivibile (database, divisore,
  tabella, collegamento): si cercava l'ultimo blocco **scrivibile**
  invece dell'ultimo e basta. Ora ne viene creato uno nuovo dopo
- **[Bug fix]** **[📱 Mobile]** Scrivendo in fondo a una pagina lunga il cursore
  finiva dietro la tastiera e la pagina restava ferma. Causa: un campo
  di testo che **cresce in altezza** invece di scorrere al proprio
  interno chiede di essere reso visibile solo quando prende il fuoco,
  non ad ogni spostamento del cursore — e il campo unito è alto quanto
  tutto il gruppo che contiene. Ora il campo calcola dove sta il
  cursore sullo schermo e chiede alla pagina di mostrare **quel punto**.
  **Tentativo fallito prima di arrivarci**: far scorrere la lista
  all'elemento che contiene il blocco. Non funziona perché tutto il
  testo scorrevole è un elemento solo: si finisce in cima a quello,
  cioè lontano dal cursore. In più quello scorrimento litigava con
  l'animazione della tastiera e lasciava un buco fra barra e tastiera

### 🆕 v0.12 — 19/09/2026 · 11:29 — I database dentro le pagine, con "Open as full page" e Delete
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Un database creato dal menu "+" **appare e si
  modifica dentro la pagina**, come un blocco fra gli altri, invece di
  aprirsi come schermata a sé. Il menu "..." accanto al suo nome lo apre
  a schermo intero o lo elimina. Il contenuto della schermata database è
  stato estratto in un unico pezzo (`DatabaseContent`) usato in entrambi
  i posti: due copie avrebbero significato correggere ogni cosa due volte
- **[Nuova funzionalità]** **[📱 Mobile]** Eliminare un database incorporato cancella
  anche la pagina-database con colonne e righe. Diverso da un
  collegamento a pagina, dove il blocco rimanda a qualcosa che vive per
  conto suo: qui il blocco **è** quel contenuto, e lasciarne i dati
  vorrebbe dire lasciarli dove non si può più arrivare
- **[Bug fix]** **[📱 Mobile]** Il database incorporato non scorre per conto suo: prende
  l'altezza che gli serve e lascia scorrere la pagina. Due aree che
  scorrono una dentro l'altra si rubano il gesto a vicenda

### 🆕 v0.11 — 19/09/2026 · 11:27 — Maiuscola automatica nei titoli, e il database largo quanto la pagina
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Maiuscola automatica sui titoli (pagina,
  database, nome di una riga) e sulle celle di testo. Non su email,
  URL e telefono, dove sarebbe da cancellare ogni volta
- **[Bug fix]** **[📱 Mobile]** Il margine laterale della pagina lo applicava la lista a
  tutti i blocchi: ora se lo mette ogni blocco, così il database può
  usare tutta la larghezza come a schermo intero (`PAGE_SIDE_PADDING`)

### ✏️ v0.10.1 — 19/09/2026 · 11:11 — Le proprietà Select diventano tag, come su Notion
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Le proprietà a selezione sono diventate tag
  veri: pastiglie colorate, finestra dedicata con "Create a tag" e
  "Select an existing option", colori assegnati a giro (`TAG_COLORS`).
  Una cella vuota resta vuota, senza trattini

### 🆕 v0.10 — 19/09/2026 · 10:57 — La vista Board
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** **Board**: una colonna per ogni valore di una
  proprietà a selezione singola, righe come schede, colonna delle non
  smistate in fondo a destra, e `+ New` che crea la riga **già
  assegnata** a quella colonna. La proprietà si sceglie da Settings →
  Group by (`PageEntity.boardGroupColumnId`, migrazione 8→9); se non è
  scelta si usa la prima adatta. Solo selezione singola: con quella
  multipla una riga starebbe in più colonne insieme. **Non c'è ancora il
  trascinamento** delle schede tra colonne
- **[Bug fix]** **[📱 Mobile]** Una colonna a selezione appena creata era un vicolo
  cieco: la cella mostrava solo un trattino e le opzioni si potevano
  scrivere unicamente alla creazione, in un campo che stava **sotto**
  l'elenco degli undici tipi e quindi fuori schermo. Ora si creano
  dalla cella, e il campo è sopra l'elenco

### ✏️ v0.9.1 — 19/09/2026 · 10:31 — I grigi di Notion dappertutto
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Tre grigi campionati dalle schermate di
  Notion: `#191919` per le pagine, `#202020` per le finestre che salgono
  dal basso, `#252525` per i riquadri che raggruppano le voci dentro
  quelle finestre
- **[Modifica]** **[📱 Mobile]** L'app è sempre scura e **non usa più i colori
  dinamici** (Material You). Erano una scelta deliberata, ma tingevano
  di viola o verde proprio gli sfondi che devono restare quei grigi
  precisi. Prima l'editor era nero fisso e il resto seguiva il telefono:
  metà app in un modo e metà nell'altro

### 🔧 v0.9.0.2 — 19/09/2026 · 10:22 — All'avvio subito la pagina, senza il lampo bianco
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Lampo bianco all'avvio: il tema della finestra era
  `Theme.Material.Light`, e il suo sfondo è quello che Android usa per
  la schermata iniziale. **Quella schermata da Android 12 non si può
  disattivare**, si può solo farle prendere i colori dell'app: sfondo
  `#191919` e icona trasparente (`values-v31/themes.xml`). L'avvio a
  freddo misura 389 ms, quindi non c'è lentezza da nascondere

### 🔧 v0.9.0.1 — 19/09/2026 · 10:16 — La griglia della tabella, come un foglio di calcolo
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Le linee orizzontali della tabella erano invisibili:
  dentro un contenitore che scorre in orizzontale la larghezza
  disponibile è illimitata, e una linea che chiede "tutta la larghezza"
  ne ottiene zero. Ora la larghezza della tabella viene calcolata

### 🆕 v0.9 — 19/09/2026 · 10:09 — Le viste: Table e List, da Settings → Layout
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** La vista si sceglie da Settings → Layout, con
  la griglia di riquadri come su Notion, ed è salvata per ogni database
  (`PageEntity.databaseLayout`, migrazione 7→8). Oltre a **Table** ci
  sono **List** (le righe senza griglia; si tocca e si apre la pagina) e
  **Board**
- **[Bug fix]** **[📱 Mobile]** Nella finestra delle proprietà i pulsanti Cancel e Add
  stavano in fondo, dopo l'elenco dei tipi: si sceglieva un tipo e non
  si vedeva più come confermare. Ora sono in un'intestazione fissa. E
  con il nome vuoto Add non faceva niente **in silenzio**: adesso la
  proprietà prende il nome del tipo

### 🆕 v0.8 — 19/09/2026 · 09:58 — Il database un punto alla volta: impostazioni, azioni di una riga, Done nei titoli
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Il tocco prolungato su una riga non apriva mai le
  azioni: il campo di testo della cella si prendeva il gesto per
  selezionare le parole, e restavano i pallini della selezione appesi
  sopra le finestre. Il nome è testo semplice finché non lo si tocca,
  così un unico rilevatore gestisce tocco e tocco prolungato
- **[Bug fix]** **[📱 Mobile]** Il titolo della pagina di una riga e il nome della riga
  erano due valori scollegati: rinominando dall'interno, tornando
  indietro si ritrovava il nome vecchio. Ora sono allineati nei due sensi

### 🆕 v0.7 — 18/09/2026 · 23:14 — I database rifatti da capo, come quelli di Notion
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** `DatabaseViewScreen` riscritta da zero
  seguendo la UI mobile di Notion: titolo grande e modificabile dentro
  il contenuto (non nella barra), riga con la vista attiva ("Table") e
  il pulsante per aggiungere una pagina, intestazioni con l'icona del
  tipo, `+ Add property` in coda alle colonne, `OPEN` su ogni riga per
  aprirla come pagina, `+ New page` in fondo. **Il magazzino dati non è
  stato toccato**: colonne/righe/celle con la riga collegata a una
  pagina *sono già* il modello di Notion, era la UI a non assomigliargli
- **[Nuova funzionalità]** **[📱 Mobile]** Finestra delle proprietà a scomparsa dal
  basso, come su Notion: nome, elenco dei tipi con ricerca e icone, e —
  quando si modifica una colonna esistente — sposta a sinistra/destra
  ed elimina. Si apre toccando l'intestazione di una colonna o
  `+ Add property`. Prima una colonna creata era immutabile: sbagliare
  nome o tipo costringeva a rifarla perdendo i dati
- **[Nuova funzionalità]** **[📱 Mobile]** Sei tipi di proprietà nuovi: Multi-select,
  URL, Email, Phone, Created time, Last edited time. Quelli mostrati da
  Notion e **non** realizzabili qui sono elencati nei Limiti noti
- **[Nuova funzionalità]** **[📱 Mobile]** `updatedAt` sulle righe (migrazione 6→7) per
  "Last edited time": viene aggiornato sia cambiando il titolo sia
  modificando una cella. Ammette null, perché per le righe già
  esistenti la data vera non è ricostruibile — in quel caso si mostra
  la data di creazione
- **[Bug fix]** **[📱 Mobile]** Una colonna aggiunta dopo averne eliminata una in mezzo
  riceveva un `orderIndex` già occupato (veniva contato il numero di
  colonne invece di prendere il massimo più uno). Due colonne con lo
  stesso indice non si possono riordinare: lo scambio non cambia nulla.
  Lo stesso difetto è ancora presente in `addRow`, dove per ora non si
  nota perché le righe non si possono riordinare
- **[Bug fix]** **[📱 Mobile]** Cambiare il titolo di una riga riscriveva l'intera
  entità partendo dalla copia in mano alla UI: scrivendo mentre la
  pagina collegata veniva creata si poteva azzerare `linkedPageId`, e
  la riga perdeva la sua pagina. Ora il salvataggio tocca solo il
  titolo (stessa lezione del tocco sul numero negli elenchi)

### 🔧 v0.6.0.1 — 18/09/2026 · 22:51 — Backspace su una riga rientrata toglie un rientro per volta
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Backspace a inizio di una riga rientrata (un `3.3` sotto
  un `3.2`) ora toglie **un rientro per volta** fino al margine, e solo
  allora fonde la riga con quella sopra: è il "gradino alla volta" di
  v0.4.2

### 🆕 v0.6 — 18/09/2026 · 22:43 — Elenchi numerati su più livelli: 1.1, 1.2
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Numerazione su più livelli guidata dal
  rientro: `1.` al margine, `1.1` `1.2` spostando a destra, `1.1.1` a
  due rientri. Spostare un elemento lo rinumera, e con lui tutti quelli
  che seguono. Il conteggio tiene un contatore per livello: risalendo,
  quelli più profondi vengono dimenticati, ed è per questo che `2.1`
  riparte da 1 (`PageEditorViewModel.numberedListLabel`)
- **[Bug fix]** **[📱 Mobile]** Spazi verticali enormi sopra e sotto le righe rientrate.
  Il rientro era fatto con un `ParagraphStyle`, ma applicare uno stile
  di paragrafo a una parte di un `AnnotatedString` spezza il testo in
  paragrafi separati, e l'a-capo che finisce al confine viene reso come
  una riga vuota in più. Ora il rientro sono spazi dentro il prefisso
  (`INDENT_SPACES`) e il testo resta un paragrafo solo
- **[Bug fix]** **[📱 Mobile]** Il pulsante rientro (e sposta su/giù, ed elimina)
  agivano sulla riga precedente se premuti subito dopo un Invio: la
  riga nuova esiste nel campo prima di esistere nella lista dei
  blocchi, quindi non si riusciva ad associarla e il "blocco a fuoco"
  restava indietro di uno. Ora il calcolo viene rifatto anche quando la
  lista dei blocchi cambia, non solo quando si scrive
- **[Bug fix]** **[📱 Mobile]** Per un istante dopo l'Invio il cursore lampeggiava al
  margine sinistro della riga nuova, prima di saltare dopo il numero.
  La riga esiste nel campo prima che il suo blocco arrivi dal database,
  quindi per un attimo veniva disegnata senza numero. Ora il prefisso
  delle righe non ancora salvate viene previsto, ereditandolo dalla
  riga sopra con la stessa regola di `updateRun`. È una previsione solo
  di ciò che si disegna: non tocca né i dati né l'editing, quindi nel
  caso peggiore sbaglia un numero per un fotogramma

### ✏️ v0.5.1 — 18/09/2026 · 22:24 — Toccare il numero fa ripartire l'elenco, anche nel testo
*(Claude Code sul PC)*

- **[Modifica]** **[📱 Mobile]** Toccare il numero di un elenco funziona anche
  nel testo unito, non più solo dentro i toggle: il numero è disegnato
  dalla trasformazione visiva, quindi gli si mette sopra una zona
  invisibile posizionata con il `TextLayoutResult`. Il calcolo dei
  prefissi è stato estratto in `runPrefixes` perché chi li disegna e chi
  li rende toccabili devono per forza essere d'accordo
- **[Bug fix]** **[📱 Mobile]** Toccare il numero cancellava il testo di quella riga.
  `pointerInput` non si rigenera quando cambia solo il contenuto del
  blocco (la chiave era l'id), quindi la lambda del tocco teneva una
  copia dell'entità com'era alla nascita — vuota — e salvarla riscriveva
  sopra il testo. `toggleNumberReset` ora prende l'id e aggiorna solo le
  colonne della numerazione. **Regola generale: non catturare entità
  dentro una lambda che non si ricompone; passare l'id e rileggere.**

### 🆕 v0.5 — 18/09/2026 · 22:14 — Scrivere "1." a inizio riga crea un elenco numerato
*(Claude Code sul PC)*

- **[Nuova funzionalità]** **[📱 Mobile]** Scrivere `1.` (o `5.`, o qualsiasi numero) e
  basta, a inizio blocco, crea un elenco numerato che parte da quel
  numero. Scatta solo se il numero è l'unica cosa scritta nel blocco:
  `abc 1.` non converte niente, come richiesto
- **[Nuova funzionalità]** **[📱 Mobile]** Elenchi numerati che partono da un numero
  scelto: nuovo campo `numberStartsAt`, che sostituisce il vecchio
  `numberResetHere` (era il caso particolare "riparti da 1")
- **[Nuova funzionalità]** **[📱 Mobile]** Prima migrazione vera dello schema (5→6) e
  rimozione di `fallbackToDestructiveMigration`. Da qui in poi i dati
  sul telefono sopravvivono ai cambi di schema

### 🔧 v0.4.2.1 — 18/09/2026 · 22:02 — L'Invio va a capo e la lista continua
*(Claude Code sul PC)*

- **[Bug fix]** **[📱 Mobile]** Il primo carattere scritto dopo un Invio finiva sulla
  riga precedente. Causa: l'area "tocca lo spazio vuoto sotto l'ultimo
  blocco per scrivere" usava `Modifier.clickable`, che in Compose si
  attiva **anche col tasto Invio** quando un elemento interno ha il
  focus. Ogni a-capo faceva quindi partire anche `focusLastBlock()`,
  che dopo 60 ms riportava il cursore sull'ultimo blocco *noto al
  momento della chiamata* — cioè quello vecchio, perché il blocco nuovo
  non era ancora stato salvato. Ora quell'area usa `detectTapGestures`,
  che risponde solo a tocchi veri. Diagnosticato leggendo il log sul
  dispositivo: l'Invio risultava corretto (cursore a 2), e 79 ms dopo
  arrivava una richiesta di focus che lo riportava a 1

### ✏️ v0.4.2 — 18/09/2026 · 21:19 — Backspace a inizio riga smonta la riga un gradino alla volta
*(Claude Code sul PC: la prima cosa fatta sul progetto arrivato
dall'altra chat, scelta dall'utente fra i «Limiti noti» del README)*

- **[Modifica]** **[📱 Mobile]** Backspace a inizio riga "smonta" la riga un
  gradino alla volta invece di fonderla subito con quella sopra, come
  in Notion: prima toglie il marcatore di elenco, poi toglie un rientro
  per volta fino al margine, e solo allora fonde. Vale **anche per il
  primo elemento** — il vecchio limite in cima ai "Limiti noti" non
  c'è più. Riconosciuto senza intercettare tasti: sulle righe dopo la
  prima perché sparisce l'a-capo esattamente dov'è il cursore, sulla
  prima riga perché sparisce il `RUN_LEAD` e il cursore finisce a zero,
  posizione altrimenti irraggiungibile. La decisione sta nel ViewModel
  (`backspaceAtLineStart`), non nella UI, perché solo lì il gruppo di
  blocchi è quello aggiornato

### ✏️ v0.4.1 — 17/09/2026 · 22:37 — L'icona dalla foto dell'utente e il nome "notE"
*(nell'altra chat, «Home»: la data e l'ora sono quelle dei file
dell'icona e del nome dell'app)*

- **[Modifica]** **[📱 Mobile]** Icona adattiva da foto dell'utente; nome
  app cambiato in "notE"

### 🔧 v0.4.0.2 — fra l'8 e il 18/09/2026 · ora non registrata — Sfondo nero e barra dello stesso colore
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- **[Bug fix]** **[📱 Mobile]** Sfondo nero fisso con testo e cursore bianchi
- **[Bug fix]** **[📱 Mobile]** Barra sopra la tastiera dello stesso colore dello
  sfondo, invece di un grigio che creava una cucitura visibile

### 🔧 v0.4.0.1 — fra l'8 e il 18/09/2026 · ora non registrata — Altri bug fix: divisore, tabelle, menu "+"
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- Crash quando il focus finiva su un blocco divisore
- Ridimensionare una tabella cancellava il testo già scritto
- Ciclo di apertura/chiusura continua del menu "+"

### 🆕 v0.4 — fra l'8 e il 18/09/2026 · ora non registrata — Database: il calendario per le date, le righe come pagine
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- **[Nuova funzionalità]** **[📱 Mobile]** Selettore di data vero (calendario)
- **[Nuova funzionalità]** **[📱 Mobile]** Apertura di una riga come pagina
- **[Nuova funzionalità]** **[📱 Mobile]** Eliminazione colonne con tocco prolungato

### 🆕 v0.3 — fra l'8 e il 18/09/2026 · ora non registrata — Tastiera e interazione: Invio, backspace e tastiera Samsung
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- **[Bug fix]** **[📱 Mobile]** Modifiche scartate silenziosamente quando la lista di
  blocchi nota alla UI era obsoleta — causa profonda di Invio da
  premere due volte, liste che non continuavano, tipo ereditato a caso
- **[Bug fix]** **[📱 Mobile]** Invio che non andava a capo al primo colpo
  (risincronizzazione col database troppo aggressiva)
- **[Bug fix]** **[📱 Mobile]** Tipo di blocco ereditato dal blocco sbagliato premendo
  Invio; titoli che si propagavano alla riga successiva
- **[Bug fix]** **[📱 Mobile]** Cambio tipo dal menu "+" applicato al blocco sbagliato
  mentre si scriveva
- **[Bug fix]** **[📱 Mobile]** Backspace su blocco vuoto: due tentativi falliti di
  intercettarlo come tasto, poi rimosso — resta il pulsante nella barra
- **[Nuova funzionalità]** **[📱 Mobile]** Tocco ovunque sotto il titolo per scrivere
- **[Bug fix]** **[📱 Mobile]** Caratteri duplicati e tastiera che si chiudeva da sola
  (mitigazioni per la tastiera Samsung)
- **[Bug fix]** **[📱 Mobile]** Selezione più fluida: niente salvataggio su database ad
  ogni movimento della selezione

### 🆕 v0.2 — fra l'8 e il 18/09/2026 · ora non registrata — Aa, Undo/Redo, numerazione e rientri
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- **[Nuova funzionalità]** **[📱 Mobile]** Formattazione inline "Aa"
  (grassetto/corsivo/sottolineato/barrato), poi estesa al testo unito
- **[Nuova funzionalità]** **[📱 Mobile]** Undo/Redo
- **[Nuova funzionalità]** **[📱 Mobile]** Reset della numerazione nelle liste
- **[Nuova funzionalità]** **[📱 Mobile]** Indenta/disindenta e sposta su/giù

### ✏️ v0.1.2 — fra l'8 e il 18/09/2026 · ora non registrata — Struttura e navigazione: la radice è una pagina, il "+" nella barra
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- Rimossa la Home come schermata separata: la radice è ora una pagina
  normale e modificabile
- Rimosso il pulsante "+" flottante — sostituito dai blocchi
  Page/Database nel menu "+" della barra
- Menu "+" ridisegnato come griglia a icone su due colonne

### ✏️ v0.1.1 — fra l'8 e il 18/09/2026 · ora non registrata — Il testo in un campo solo: ripensato il modello di editing
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

**[Nuova funzionalità] Ripensato il modello di editing del testo** — il
cambiamento più grande dello sviluppo. Vedi "Decisioni di design
deliberate" sopra per il contesto completo.

### 🆕 v0.1 — 08/09/2026 · 09:19 — Nasce notE
*(nell'altra chat, «Home»: la data e l'ora sono quelle dei primi file
del progetto, `build.gradle.kts` e `settings.gradle.kts`)*

- **[Nuova funzionalità]** **[📱 Mobile]** **notE**: un'app di note e database in stile
  Notion per Android, con **tutti i dati salvati sul telefono** (SQLite
  via Room): nessuna rete, nessun account, nessun cloud. Il nome
  interno del progetto è NotionLocal

## Vecchia numerazione

**Fino al 21/10/2026 le versioni avevano un altro numero**: 🆕 v0.N per
una richiesta che portava qualcosa di nuovo o toglieva qualcosa, 🔧
v0.N.1 per una che correggeva soltanto. Quel giorno, su richiesta
dell'utente, la Cronologia è stata riletta da capo e rinumerata come è
adesso (vedi l'inizio della Cronologia). I commit fatti prima portano il
numero vecchio nel messaggio; qui, per ognuno, quello di adesso:

v0.1 → v0.1 · v0.2 → v0.1.1 · v0.3 → v0.1.2 · v0.4 → v0.2 · v0.5 → v0.3 · v0.6 → v0.4 · v0.6.1 → v0.4.0.1 · v0.6.2 → v0.4.0.2 · v0.7 → v0.4.1 · v0.8 → v0.4.2 · v0.8.1 → v0.4.2.1 · v0.9 → v0.5 · v0.10 → v0.5.1 · v0.11 → v0.6 · v0.11.1 → v0.6.0.1 · v0.12 → v0.7 · v0.13 → v0.8 · v0.14 → v0.9 · v0.14.1 → v0.9.0.1 · v0.14.2 → v0.9.0.2 · v0.15 → v0.9.1 · v0.16 → v0.10 · v0.17 → v0.10.1 · v0.18 → v0.11 · v0.19 → v0.12 · v0.19.1 → v0.12.0.1 · v0.20 → v0.12.1 · v0.21 → v0.13 · v0.22 → v0.14 · v0.23 → v0.15 · v0.24 → v0.16 · v0.25 → v0.16.1 · v0.26 → v0.17 · v0.27 → v0.18 · v0.28 → v0.19 · v0.29 → v0.20 · v0.30 → v0.20.1 · v0.31 → v0.21 · v0.32 → v0.22 · v0.33 → v0.22.1 · v0.34 → v0.23 · v0.34.1 → v0.23.0.1 · v0.35 → v0.23.1 · v0.36 → v0.24 · v0.36.1 → v0.24.0.1 · v0.37 → v0.24.1 · v0.38 → v0.24.2 · v0.38.1 → v0.24.2.1 · v0.38.2 → v0.24.2.2 · v0.39 → v0.25 · v0.40 → v0.26 · v0.41 → v0.26.1 · v0.42 → v0.27 · v0.43 → v0.28 · v0.44 → v0.29 · v0.45 → v0.30 · v0.46 → v0.31 · v0.46.1 → v0.31.0.1 · v0.47 → v0.31.1 · v0.47.1 → v0.31.1.1 · v0.48 → v0.31.2 · v0.48.1 → v0.31.2.1 · v0.49 → v0.31.3 · v0.50 → v0.31.4 · v0.51 → v0.32 · v0.51.1 → v0.32.0.1 · v0.52 → v0.33 · v0.53 → v0.34 · v0.54 → v0.35 · v0.55 → v0.35.1 · v0.56 → v0.35.2 · v0.57 → v0.36 · v0.58 → v0.36.1 · v0.59 → v0.37 · v0.59.1 → v0.37.0.1 · v0.60 → v0.37.1 · v0.61 → v0.38 · v0.62 → v0.39 · v0.63 → v0.40 · v0.64 → v0.41 · v0.65 → v0.42 · v0.66 → v0.43 · v0.67 → v0.44 · v0.68 → v0.44.1 · v0.69 → v0.44.2 · v0.70 → v0.45 · v0.71 → v0.46 · v0.72 → v0.46.1 · v0.73 → v0.46.2 · v0.74 → v0.47 · v0.75 → v0.48 · v0.76 → v0.49 · v0.77 → v0.50 · v0.77.1 → v0.50.0.1 · v0.78 → v0.51 · v0.78.1 → v0.51.0.1 · v0.79 → v0.51.1 · v0.80 → v0.52 · v0.81 → v0.52.1 · v0.82 → v0.52.2 · v0.83 → v0.52.3 · v0.84 → v0.52.4 · v0.85 → v0.52.5 · v0.86 → v0.52.6 · v0.87 → v0.53 · v0.88 → v0.54 · v0.89 → v0.54.1 · v0.90 → v0.54.2 · v0.91 → v0.54.3 · v0.92 → v0.55 · v0.93 → v0.56 · v0.94 → v0.57 · v0.94.1 → v0.57.0.1 · v0.95 → v0.58 · v0.95.1 → v0.58.0.1 · v0.96 → v0.59 · v0.97 → v0.60 · v0.98 → v0.61 · v0.98.1 → v0.61.0.1 · v0.99 → v0.62 · v0.100 → v0.63 · v0.101 → v0.64 · v0.101.1 → v0.64.0.1 · v0.102 → v0.65 · v0.102.1 → v0.65.0.1 · v0.102.2 → v0.65.0.2 · v0.103 → v0.65.1 · v0.104 → v0.65.2 · v0.105 → v0.65.3 · v0.105.1 → v0.65.3.1 · v0.105.2 → v0.65.3.2 · v0.106 → v0.66 · v0.106.1 → v0.66.0.1 · v0.107 → v0.67 · v0.108 → v0.68 · v0.109 → v0.69 · v0.110 → v0.70 · v0.110.1 → v0.70.0.1 · v0.110.2 → v0.70.0.2 · v0.111 → v0.71 · v0.112 → v0.71.1 · v0.113 → v0.72 · v0.114 → v0.73 · v0.115 → v0.74 · v0.115.1 → v0.74.0.1 · v0.116 → v0.75 · v0.117 → v0.76 · v0.118 → v0.76.1 · v0.119 → v0.77 · v0.120 → v0.78 · v0.120.1 → v0.78.0.1 · v0.121 → v0.78.1 · v0.122 → v0.79 · v0.122.1 → v0.79.0.1 · v0.123 → v0.79.1 · v0.124 → v0.80 · v0.125 → v0.80.1 · v0.126 → v0.81 · v0.127 → v0.81.1 · v0.128 → v0.82 · v0.129 → v0.82.1 · v0.129.1 → v0.82.1.1 · v0.130 → v0.83 · v0.131 → v0.84 · v0.132 → v0.84.1 · v0.133 → v0.85 · v0.134 → v0.85.1 · v0.135 → v0.86 · v0.136 → v0.86.1 · v0.137 → v0.86.2 · v0.138 → v0.86.3 · v0.139 → v0.86.4 · v0.140 → v0.87 · v0.140.1 → v0.87.0.1 · v0.141 → v0.88 · v0.141.1 → v0.88.0.1 · v0.142 → v0.88.1 · v0.143 → v0.89 · v0.144 → v0.89.1 · v0.145 → v0.90 · v0.145.1 → v0.90.0.1 · v0.146 → v0.91 · v0.147 → v0.92 · v0.148 → v0.93 · v0.149 → v0.94 · v0.150 → v0.95 · v0.151 → v0.96 · v0.152 → v0.97 · v0.153 → v0.98 · v0.153.1 → v0.98.0.1 · v0.154 → v0.98.1 · v0.155 → v0.99 · v0.156 → v0.99.1 · v0.157 → v0.100 · v0.158 → v0.101 · v0.158.1 → v0.101.1 · v0.158.2 → v0.101.2 · v0.158.3 → v0.101.3 · v0.159 → v0.102 · v0.160 → v0.102.1 · v0.161 → v0.102.2 · v0.162 → v0.102.3 · v0.163 → v0.103 · v0.163.1 → v0.103.1 · v0.164 → v0.103.2 · v0.165 → v0.103.3 · v0.166 → v0.104 · v0.167 → v0.104.1 · v0.167.1 → v0.104.1.1 · v0.168 → v0.105 · v0.168.1 → v0.105.1 · v0.169 → v0.105.2 · v0.170 → v0.106 · v0.171 → v0.107 · v0.172 → v0.107.1 · v0.173 → v0.108 · v0.174 → v0.109 · v0.175 → v0.110 · v0.176 → v0.110.1 · v0.177 → v0.110.2 · v0.178 → v0.111 · v0.179 → v0.112 · v0.180 → v0.113 · v0.181 → v0.113.1 · v0.182 → v0.113.2 · v0.182.1 → v0.113.2.1 · v0.183 → v0.113.3 · v0.184 → v0.114 · v0.185 → v0.115 · v0.186 → v0.116 · v0.186.1 → v0.116.0.1 · v0.186.2 → v0.116.0.2 · v0.187 → v0.117 · v0.188 → v0.118 · v0.188.1 → v0.118.0.1 · v0.188.2 → v0.118.0.2 · v0.189 → v0.118.1 · v0.190 → v0.118.2 · v0.190.1 → v0.118.2.1 · v0.191 → v0.119 · v0.192 → v0.120 · v0.193 → v0.121 · v0.194 → v0.122 · v0.195 → v0.123 · v0.195.1 → v0.123.0.1 · v0.196 → v0.124

## Struttura del progetto

**Dal 04/11/2026 i moduli sono tre** (v0.153): `shared` è il codice comune
a telefono e PC, `app` l'app Android, `desktop` quella per Windows e Mac.
I pacchetti sono gli stessi in tutti e tre: un file si sposta da `app` a
`shared` senza cambiare chi lo usa.

```
shared/src/commonMain/kotlin/com/gabriele/notionlocal/
                    # Il codice comune: il database (entità, DAO,
                    # AppDatabase), le repository, le immagini delle
                    # pagine, i ViewModel, formule, testi, tema,
                    # impostazioni, Patch Notes (e cresce)
shared/src/androidMain/
                    # Dal 04/11/2026 (v0.153.6) tutta l'app del telefono
                    # (schermate, notifiche, backup…) e quello che cambia
                    # fra telefono e PC (expect/actual)
shared/src/desktopMain/
                    # La parte del PC di expect/actual
app/src/main/java/com/gabriele/notionlocal/
                    # Solo MainActivity, ShareActivity e il widget
desktop/src/main/kotlin/com/gabriele/notionlocal/desktop/Main.kt
                    # La finestra del PC (gradle :desktop:run)
desktop/src/test/   # DesktopScreensTest: le schermate del PC in immagini;
                    # DesktopDatabaseTest: il database sul PC;
                    # DesktopRepositoryTest: le repository sul PC;
                    # DesktopImageStoreTest: le immagini sul PC;
                    # DesktopViewModelTest: i ViewModel sul PC

app/src/main/java/com/gabriele/notionlocal/
├── data/           # Entità Room, DAO, Repository, AppDatabase
├── ui/
│   ├── screen/     # PageEditorScreen, DatabaseViewScreen, SearchScreen
│   ├── theme/      # Colori, tipografia, tema Material 3
│   └── navigation/ # Routes e NavHost
├── viewmodel/      # Un ViewModel per schermata + ViewModelFactory
└── MainActivity.kt

app/src/test/java/com/gabriele/notionlocal/
└── data/           # Test automatici senza telefono (dal 24/09/2026:
    │               # TextStatsTest, il conteggio del testo)
    └── repository/ # PageRepositoryTest: spostare, duplicare, buttare,
                    # Annulla e Ripristina, l'indice della ricerca nei
                    # database, su un database vero (Robolectric + Room)
app/src/test/java/com/gabriele/notionlocal/viewmodel/
                    # DatabaseSearchTest: quali righe tiene la ricerca
                    # dentro un database e quali illumina;
                    # LinkedViewModelTest: una vista collegata vera
app/src/test/java/com/gabriele/notionlocal/data/entity/
                    # RichTextScriptTest: pedice e apice nel testo salvato
app/src/test/java/com/gabriele/notionlocal/data/widgets/
                    # WidgetsTest: le barre dell'avanzamento, l'ordine
                    # ovest-est, il pomodoro che avanza, il salvataggio
app/src/test/java/com/gabriele/notionlocal/ui/
                    # WidgetsRenderTest: disegna i widget e salva
                    # l'immagine in app/build/widget-screenshots/;
                    # ScriptRenderTest: i pulsanti pedice e apice
app/src/test/java/com/gabriele/notionlocal/data/MigrationTest.kt
                    # la migrazione 28→29 aperta da Room come fa l'app
app/src/test/resources/schema/v28.sql
                    # lo schema della versione 28, per MigrationTest

app/src/main/res/font/            # I 10 font occidentali delle pagine (sosia liberi)
app/src/main/res/values/font_certs.xml   # Certificati per scaricare i font da Google
app/src/main/assets/font_licenses/OFL.txt # Licenza e copyright dei font inclusi
app/src/main/assets/proofing/     # I dizionari del correttore, con le loro licenze
tools/proofing/get_dictionaries.py # Li riscarica, alle versioni provate
```

`data/proofing/` è il correttore: `SpellDictionary` legge un dizionario
di Hunspell e controlla una parola, `SpellSuggestions.kt` trova le parole
al suo posto, `ProofingText` divide il testo in parole da controllare,
`SpellChecker` aggiunge il dizionario dell'utente e le parole ignorate.
`ui/screen/ProofingUi.kt` è come si vede nella pagina (la riga ondulata,
il menu), `ProofingSettings.kt` le scelte in Settings e nei tre puntini.

`ui/theme/PageFonts.kt` è il catalogo dei font delle pagine: nomi,
sosia, gruppi, da dove arrivano, e `PageTypography`, che porta font e
corpo a tutto il testo.

`data/widgets/` sono i widget della barra laterale: `Widgets.kt` com'è
fatto ognuno e i calcoli (barre, ordine dei fusi, pomodoro), senza
Android; `WidgetStore.kt` dove si salvano e l'attesa che fa suonare il
pomodoro. `ui/screen/SidebarWidgets.kt` è come si vedono.

`data/patchnotes/PatchNotes.kt` e `ui/screen/PatchNotesScreen.kt` (in
`shared`, dal 04/11/2026) sono
**Settings → Patch Notes**: la Cronologia di questo README divisa in
versioni e disegnata come su GitHub — l'elenco dei titoli
(`PatchNotesScreen`) e la pagina di ogni versione (`PatchNoteScreen`). Il testo lo copia il task
`patchNotes` di `app/build.gradle.kts` (vedi "Nota tecnica").

`ui/screen/BlockActionsSheet.kt` è il menu di un blocco (i sei puntini):
le voci per una riga di testo, per una pagina e per un database, e tutte
le finestre che apre. Chi lo usa passa solo l'id del blocco.

**File chiave da capire per primi**:
- `PageEditorViewModel.updateRun` — il cuore della logica di modifica
  del testo unito: confronta righe prima/dopo e decide se è modifica,
  divisione o fusione
- `PageEditorScreen.MergedTextRunField` — il campo condiviso e la sua
  sincronizzazione col database
- `MergedRunVisualTransformation` + `MergedRunOffsetMapping` — resa dei
  prefissi (• e numeri) e mappatura fra posizioni reali e mostrate.
  **Questa mappatura è il punto più delicato del progetto**: un errore
  qui sposta il cursore o la selezione in modo difficile da
  diagnosticare. Attenzione: il testo vero comincia a 1, non a 0, per
  via del `RUN_LEAD` — è il parametro `leading` della mappatura
- `DatabaseContent` — il database, usato sia a schermo intero sia
  dentro una pagina. Le uniche differenze le decide `embedded`: il
  titolo più piccolo (e nascondibile, vedi `showEmbeddedTitle`), la
  voce "Delete" nelle impostazioni, e il non scorrere per conto suo
- `backspacedLineStart` + `PageEditorViewModel.backspaceAtLineStart`
  — riconoscimento del backspace a inizio riga e rifiuto della fusione
  finché la riga ha ancora un marcatore o un rientro da perdere

## Nota tecnica

Lo schema è alla **versione 55** (la 55, del 08/10/2026, aggiunge
`pages.proofingMode`, `proofingLanguage` e `proofingIgnored` e la tabella
`personal_words`, il correttore — vedi `ProofingUi.kt`; la 54, dello stesso giorno, aggiunge
`pages.freezeFirstColumn` e `freezeFirstRow`, la prima colonna e la prima riga ferme; la 53, dello stesso giorno, aggiunge
`pages.iconSymbol`, le icone di Material delle pagine; la 52, del 06/10/2026, aggiunge
`blocks.attrsJson`, le voci in più dei blocchi nuovi del menu "/" — vedi `BlockAttrs`; la 51, del 05/10/2026, aggiunge
`pages.thenSortColumnId` e `thenSortDescending`, la seconda regola di ordinamento; la 50, del 30/09/2026, aggiunge
`database_rows.eventColor`, il colore degli eventi del calendario; la 49, dello stesso giorno, aggiunge
`pages.colorBoardCards`, "Color cards" delle bacheche; la 48, dello stesso giorno, aggiunge
`pages.wrapCells`, oggi il testo lungo nelle celle delle tabelle (`PageEntity.cellOverflow`: 0 tre puntini, 1 a capo, 2 colonna allargata); la 47, del 29/09/2026, aggiunge
`database_columns.durationFormat`, la proprietà Duration; la 46, dello stesso giorno, aggiunge
`pages.nameColumnOrder`, il posto della colonna Name; la 45, dello stesso giorno, aggiunge
`database_rows.parentRowId` e `subRowsCollapsed`, le sotto-righe; la 44, dello stesso giorno, aggiunge
`pages.columnWidths` e `fitColumns`, la larghezza delle colonne della tabella; la 43, dello stesso giorno, aggiunge
`pages.viewHostId`, `viewName` e `activeViewId`, le viste di un database; la 42, dello stesso giorno, aggiunge
`pages.hideEmptySubGroups`, l'interruttore dei sottogruppi vuoti; la 41, dello stesso giorno, aggiunge
`pages.tableSubGroupColumnId`, i sottogruppi della tabella; la 40, del 28/09/2026, aggiunge
`pages.chartSettings`, le impostazioni della vista Chart; la 39, del
26/09/2026, aggiunge i
template delle pagine; la 38, dello stesso giorno, aggiunge il
formato della data e dell'ora delle colonne Date; la 37, dello stesso
giorno, aggiunge l'allineamento delle celle; la 36, dello stesso giorno, aggiunge le
impostazioni delle colonne Number e i calcoli delle viste; la 35, dello
stesso giorno, aggiunge i
quaderni: le tabelle `notebooks` e `notebook_sections` e
`pages.notebookSectionId`; la 34, dello stesso giorno, aggiunge
`pages.isProtected`, le pagine bloccate con la password; la 33, del
25/09/2026, aggiunge la tabella `ink_strokes` dei disegni a mano; la 32, dello stesso giorno,
aggiunge `blocks.notifyAt`, l'ora delle righe "/notification"; la 31, dello
stesso giorno, sposta font e
corpo dalla pagina al testo, senza cambiare la forma delle tabelle; la
30, dello stesso giorno, toglie MS YaHei
e aggiunge l'inquadratura delle schede della galleria; dalla 25 alla 29 vengono dalla
sessione cloud del 24/09/2026, vedi Diario: migrazioni simulate su
SQLite in cloud e **provate su un telefono vero la sera stessa**, da
un database alla versione 24 con note di prova, vedi Cronologia
"Ripresa sul PC del 24/09/2026"; la 27 non cambia la
forma delle tabelle, trasforma i titoli in paragrafi; la 28 aggiunge il
database semplice; la 29 le viste collegate, `sourceDatabaseId` e
`viewHiddenColumnIds` in `pages` — questa è anche **provata da un test
automatico con Room**, `MigrationTest`, che apre un database vero alla
versione 28 come fa l'app e fa controllare a Room lo schema che ne
esce), e
da qui in avanti **ogni cambio di
schema vuole una migrazione vera** in `AppDatabase`. Fino alla 5 c'era
`fallbackToDestructiveMigration()`, che ad ogni cambio ricreava il
database da zero perdendo tutto: accettabile finché sul telefono
c'erano solo dati di prova, non più ora che ci sono note vere. La
scorciatoia è stata tolta insieme alla migrazione 5→6, quindi un campo
aggiunto senza la sua migrazione fa rifiutare l'avvio all'app — un
errore rumoroso, molto meglio delle note cancellate in silenzio.

**Le Patch Notes dell'app vengono da questo README** (dal 30/09/2026). A
ogni compilazione il task `patchNotes` (`PatchNotesTask` in
`app/build.gradle.kts`) copia la sezione "Cronologia degli
aggiornamenti" — dal suo titolo fino al `## ` dopo — negli asset
dell'app come `patch_notes.md` (con `addGeneratedSourceDirectory`, così
Gradle sa che va rifatta quando il README cambia). Dallo stesso README
vengono `versionName`, il numero del primo titolo "### … v0.N — " (la
versione più recente; fino a quattro parti, v0.N.M.K, dal 21/10/2026), e
`versionCode`, quanti titoli di versione ci sono: per questo rinumerare la
Cronologia, come il 21/10/2026, non lo fa tornare indietro e il telefono
accetta l'aggiornamento. Quindi: **una voce nuova nel README è una
versione nuova dell'app** alla prossima compilazione. `PatchNotesTest` legge l'asset
come fa l'app e lo confronta col README.

Prima di installare una build che cambia lo schema conviene portarsi
via una copia del database dal telefono:

```
adb exec-out run-as com.gabriele.notionlocal cat databases/notion_local.db > backup.db
```

(su Windows va fatto passare da `cmd /c`, perché la redirezione di
PowerShell rovina i file binari; servono anche `-wal` e `-shm`. La
redirezione **sul telefono**, dentro `adb shell`, produce invece un
file vuoto: il processo dell'app non può scrivere su `/sdcard`).

Dopo l'installazione conviene verificare che la migrazione sia
avvenuta davvero e non di nascosto ricreando il database: nei byte
60-63 del file c'è `user_version` (deve essere il numero nuovo), e la
colonna aggiunta deve comparire **in fondo** al `CREATE TABLE` che sta
in `sqlite_master` — se fosse in mezzo, o fra apici inversi come le
altre, vorrebbe dire che la tabella è stata rifatta.

### Un backup si legge senza il file `-shm`

**Quasi tutto quello che c'è nel database sta nel `-wal`, non nel
file principale.** L'app viene chiusa uccidendo il processo ad ogni
installazione, quindi SQLite non fa quasi mai il travaso (checkpoint):
il `.db` può contenere solo la pagina radice del primo avvio e tutto
il resto vivere nel giornale.

Il `-shm` è l'indice di quel giornale. Se si apre una copia del
database **con il `-shm` che è stato copiato insieme**, SQLite si fida
di quell'indice, dichiara che il giornale ha zero frammenti validi e
**mostra un database quasi vuoto** — senza errori e senza avvisi.
Sembra che il backup sia da buttare, o peggio che i dati siano andati
persi.

Quindi: copiare tutti e tre i file, ma per leggere il backup tenere
solo `.db` e `-wal`, **cancellando il `-shm`**. Così SQLite ricostruisce
l'indice dal giornale e i dati tornano tutti.

Un database che dopo questa accortezza risulta comunque quasi vuoto lo
è davvero — succede normalmente, perché i database di prova vengono
cancellati dall'app appena finito di provarli.
