# KiwiCar — Documento di progetto

_Data: 2026-11-09_

## Intento e perché

KiwiCar è un'app Android personale per registrare i rifornimenti di **benzina** e i **rabbocchi d'olio** di un'auto (tipicamente un'auto con qualche anno che consuma olio) e capire, a colpo d'occhio, **quanto consuma** la macchina e **quanto costa** mantenerla.

Il bisogno nasce da un momento concreto descritto dall'utente: voler sapere **quanta benzina e quanto olio consuma l'auto** e il **costo medio per mese e per anno**. L'utente fa sempre il **pieno completo** di benzina (quindi il consumo è calcolabile con precisione da un pieno all'altro) e aggiunge olio **0,5 L o 1 L quando serve**, controllandolo lui stesso.

L'app gira **offline**, salva tutto **in locale sul telefono** e deve conservare in modo affidabile ogni inserimento.

## Obiettivi

- Registrare inserimenti di **benzina** (pieno) e di **olio** con: **data**, **chilometraggio (km)**, **litri**.
- Calcolare e mostrare:
  - **Consumo benzina** in **L/100 km** (metodo pieno-a-pieno: litri del pieno ÷ km percorsi dal pieno precedente × 100).
  - **Consumo olio** in **L/1.000 km** (litri d'olio aggiunti ÷ km percorsi dal rabbocco d'olio precedente × 1.000).
  - **Costo medio benzina** per **mese** e per **anno**, calcolato come litri × **prezzo configurabile (€/L)**.
- Prezzi **configurabili** nelle Impostazioni: prezzo benzina (€/L) e prezzo olio (€/L).
- **Auto configurabile** nelle Impostazioni: marca/modello e chilometraggio iniziale, mostrati in alto nella home.
- **Grafici** nel tempo (selettore Mese/Anno): rabbocchi, consumo benzina, consumo olio, costo medio benzina. **Asse X = data**; **asse Y etichettato con l'unità propria di ciascun grafico** (rabbocchi → litri; consumo benzina → L/100 km; consumo olio → L/1.000 km; costo → €). **Eventi cliccabili** che aprono un **popup di dettaglio** (data, km, litri, costo stimato, km dal rifornimento precedente, consumo) con azioni **Modifica** ed **Elimina**.
- **Storico** degli inserimenti e **export CSV** come backup.
- Dati **persistenti in locale**; nessun dato va perso tra un avvio e l'altro.
- Installabile sul telefono Android dell'utente come **APK**.

## Non-obiettivi

- Nessun account, login, cloud o sincronizzazione tra dispositivi.
- Nessun inserimento dell'importo reale pagato: il costo è **sempre** derivato dai litri × prezzo configurato (scelta esplicita dell'utente).
- Nessuna gestione di rifornimenti parziali di benzina: si assume **sempre il pieno completo**.
- Nessun supporto multi-auto nella prima versione (una sola auto).
- Nessuna gestione del cambio olio completo / tagliando (solo rabbocchi d'olio).
- Nessuna pubblicazione sul Play Store (consegna come APK sideload).

## Anti-obiettivi (cosa renderebbe il progetto un fallimento anche se "funziona")

- Perdere inserimenti già salvati (persistenza inaffidabile): inaccettabile.
- Calcoli di consumo errati o fuorvianti.
- Interfaccia confusa al distributore, col telefono in mano: l'inserimento deve essere rapido.

## Vincoli

- Piattaforma: **Android** (telefono dell'utente).
- Funzionamento **offline**, dati **solo in locale**.
- Lingua dell'interfaccia: **italiano**. Numeri in formato italiano (virgola decimale, es. `6,8`).
- Ambiente di sviluppo: Windows.

## Scelte di realizzazione (decise — mie, salvo diversa indicazione dell'utente)

- **App Android nativa** in **Kotlin** con **Jetpack Compose** (Material 3).
- **Persistenza locale** con **Room** (database SQLite) — scelto per affidabilità dei dati rispetto a soluzioni più fragili.
- **Grafici**: resi con Compose (Canvas) o libreria grafici Compose-compatibile; assi etichettati ed eventi cliccabili con popup. Il builder sceglie l'implementazione specifica purché rispetti il comportamento descritto.
- **Design**: tema scuro, accento verde (`#2E7D32` / `#66BB6A`), arancione (`#FFB74D`) per l'olio; **icone minimali a linea** in tutta l'app; **titoli delle sezioni di raggruppamento in grassetto maiuscolo** in tutta l'app.
- **Navigazione** in basso a 4 voci (Home, Grafici, Storico, Impostazioni), stile moderno: **tab attivo come "pillola" verde con icona + testo**, gli altri tab **solo icona**.
- Consegna come **APK** installabile (sideload). La build dell'APK richiede l'Android SDK/Gradle: la disponibilità della toolchain va verificata all'inizio dell'implementazione.

## Schermate (validate con mockup)

1. **Home** — intestazione con auto e km attuali; card consumi (benzina L/100 km, olio L/1.000 km); card costo medio benzina (mese / anno); mini-grafico rabbocchi; pulsante **+ Nuovo rifornimento**; navigazione in basso.
2. **Nuovo inserimento** — interruttore **Benzina / Olio** in alto (icona pompa / icona goccia); campi Data (default oggi) e Chilometraggio; per benzina **litri del pieno**, per olio **litri aggiunti** (scelte rapide 0,5 L / 1 L / altro); **costo stimato** dal prezzo configurato; pulsante **Salva**.
3. **Grafici** — selettore **Mese / Anno** (= **finestra temporale** mostrata: ultimo mese / ultimo anno di dati; non cambia il tipo di dato, solo l'intervallo visibile); quattro grafici (rabbocchi nel tempo, consumo benzina, consumo olio, costo medio benzina); asse X = data, asse Y con l'unità propria del grafico; ogni evento cliccabile → **popup dettaglio** con Modifica/Elimina. Il grafico "costo medio benzina" è aggregato per mese.
4. **Storico** — elenco cronologico degli inserimenti; tocco su una voce → stesso popup/dettaglio; modifica ed elimina.
5. **Impostazioni** — sezione **AUTO** (marca/modello, km iniziale), **PREZZI** (€/L benzina, €/L olio, icona €), **DATI** (Esporta CSV, icona download).

## Modello dati (indicativo)

- **Entry**: id, tipo (`BENZINA` | `OLIO`), data, chilometraggio (km), litri. (Il costo non si memorizza: si calcola sempre dai litri × prezzo corrente.)
- **Impostazioni**: marca/modello auto, km iniziale, prezzo benzina €/L, prezzo olio €/L.

## Regole di calcolo

- L'inserimento **precedente** di riferimento è quello **immediatamente sotto per chilometraggio** (odometro) **dello stesso tipo**, non per data di inserimento (così inserimenti aggiunti fuori ordine restano coerenti). Si assume km non decrescente (validato in input).
- **Consumo benzina (L/100 km)** per il pieno N = `litri_N ÷ (km_N − km_{pieno benzina precedente}) × 100`. Il primo pieno non ha consumo (manca il riferimento precedente; il km iniziale dell'auto **non** è usato come riferimento, perché non si conosce lo stato del serbatoio a quel km).
- **Consumo olio (L/1.000 km)** per il rabbocco N = `litri_olio_N ÷ (km_N − km_{rabbocco olio precedente}) × 1.000`. Il primo rabbocco non ha consumo.
- **Modifica o eliminazione** di un inserimento **ricalcola** il consumo dell'inserimento successivo dello stesso tipo (che cambia riferimento).
- **Costo di un inserimento** = `litri × prezzo configurato del tipo corrispondente`.
- **Costo medio mensile benzina** = costo benzina totale di **tutto lo storico** ÷ **numero di mesi trascorsi** dal primo inserimento a oggi (media stabile, non la spesa del solo mese corrente). **Costo medio annuale** = costo benzina totale ÷ numero di anni trascorsi (equivalente alla media mensile × 12).
- Cambiare il prezzo configurato aggiorna i costi mostrati (i costi sono sempre ricalcolati, non congelati).

## Lasciato al builder

- Scelta della libreria/implementazione specifica dei grafici (purché: assi X=data Y=litri, eventi cliccabili con popup di dettaglio).
- Impaginazione fine, spaziature, animazioni, stati vuoti (prima dell'inserimento di dati) e validazione input (es. km non decrescente, litri > 0).
- Struttura del CSV di export.
- Dettagli di packaging/firma dell'APK per il sideload.
