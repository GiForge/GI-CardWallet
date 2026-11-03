# 🎫 Smart Barcode Wallet

Un portafoglio digitale per codici a barre e QR Code progettato per conservare carte fedeltà, tessere cliente, coupon e pass digitali direttamente sul dispositivo, con un'interfaccia moderna, veloce e completamente offline.

✅ Scanner ultra rapido  
✅ Supporto codici 1D e 2D  
✅ Presenter a schermo intero per lettori di cassa  
✅ Backup e ripristino JSON  
✅ Personalizzazione completa delle carte  
✅ Privacy 100% Offline-First

---

## 📱 Screenshot

<p align="center">
  <img src="https://github.com/user-attachments/assets/901c898c-93a5-4e66-ae88-e926d14d9652"
       alt="Elenco Carte"
       width="340"/>
  <img src="https://github.com/user-attachments/assets/b193e17e-eeb6-42c7-ba41-77e8f297c219"
       alt="Visualizzazione Carta"
       width="334"/>
</p>

---

## 🏷️ Badge

https://img.shields.io/badge/Platform-Android-green

https://img.shields.io/badge/Kotlin-Latest-blueviolet

https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue

https://img.shields.io/badge/Google-ML%20Kit-orange

https://img.shields.io/badge/Database-Room%20DB-success

https://img.shields.io/badge/Privacy-100%25%20Offline--First-brightgreen

---

# 📖 Descrizione

**Smart Barcode Wallet** consente di digitalizzare e organizzare tutte le proprie carte fedeltà, tessere punti, coupon e codici QR in un unico luogo.

Grazie alla scansione tramite fotocamera e al riconoscimento automatico dei codici, è possibile creare una raccolta ordinata e sempre disponibile senza dover portare con sé le versioni cartacee.

Tutti i dati vengono archiviati localmente sul dispositivo. Nessun account, nessuna sincronizzazione cloud obbligatoria e nessun tracciamento dell'utente.

---

# ✨ Funzionalità Principali

## 📷 Scansione Ultra Rapida

Acquisizione immediata dei codici tramite:

- CameraX
- Google ML Kit
- Decodifica automatica
- Messa a fuoco rapida
- Riconoscimento istantaneo

Supporta sia codici a barre tradizionali che codici bidimensionali.

---

## 🎤 Presenter Full Screen

Modalità dedicata all'utilizzo presso:

- Supermercati
- Farmacie
- Negozi
- Centri commerciali
- Punti vendita convenzionati

### Caratteristiche

- Visualizzazione a schermo intero.
- Codice massimizzato automaticamente.
- Nascondimento elementi superflui.
- Aumento automatico della luminosità.
- Lettura ottimizzata dagli scanner da cassa.

---

## 🎴 Interfaccia 3D Card Deck

Esperienza utente moderna basata su carte animate.

### Include

- Animazioni fluide.
- Effetti profondità.
- Visualizzazione tipo deck.
- Scorrimento intuitivo.
- Layout responsive.

---

## ⚪ Box Logo Universale

Ogni carta dispone di un contenitore logo dedicato.

### Vantaggi

- Sfondo bianco puro.
- Compatibilità con qualsiasi brand.
- Migliore leggibilità.
- Aspetto uniforme dell'interfacia.

---

## 🎨 Personalizzazione Carte

Ogni carta può essere configurata individualmente.

### Opzioni Disponibili

- Nome carta.
- Colore principale.
- Logo personalizzato.
- Categoria.
- Note.

---

## 💡 Suggerimenti Automatici Brand

L'app può proporre:

- Nome del brand.
- Colori associati.
- Layout coerenti.
- Miglior visualizzazione del codice.

---

## 🖼️ Loghi Personalizzati

Importazione diretta dalla galleria Android.

Supporta:

- PNG
- JPG
- JPEG
- WEBP

Funzionalità:

- Ritaglio automatico.
- Anteprima immediata.
- Salvataggio locale.

---

## 🔐 Salvataggio Locale Sicuro

Tutte le informazioni vengono archiviate tramite Room SQLite.

Nessun dato:

- Inviato online.
- Tracciato.
- Condiviso con terze parti.

---

## 💾 Backup & Ripristino

Esportazione dell'intero archivio in formato JSON.

### Backup

```json
{
  "cards": [
    {
      "name": "Supermarket Card",
      "code": "1234567890123"
    }
  ]
}
```

### Ripristino

- Importazione file JSON.
- Ripristino completo del database.
- Migrazione automatica dei dati salvati.

---

# 📋 Formati Supportati

## Codici 1D

| Formato | Supportato |
|----------|------------|
| EAN-13 | ✅ |
| EAN-8 | ✅ |
| UPC-A | ✅ |
| UPC-E | ✅ |
| Code 128 | ✅ |
| Code 39 | ✅ |
| ITF | ✅ |
| Codabar | ✅ |

---

## Codici 2D

| Formato | Supportato |
|----------|------------|
| QR Code | ✅ |
| Data Matrix | ✅ |
| Aztec | ✅ |
| PDF-417 | ✅ |

---

# 🏗️ Architettura Applicativa

L'app segue il pattern MVVM con flusso dati unidirezionale.

```text
┌─────────────────────────────┐
│      Jetpack Compose UI     │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│         ViewModel           │
│         StateFlow           │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│         Repository          │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│          Room DB            │
└─────────────────────────────┘
```

---

# 🧰 Stack Tecnologico

| Tecnologia | Utilizzo |
|------------|----------|
| Kotlin | Linguaggio principale |
| Jetpack Compose M3 | Interfaccia utente |
| MVVM | Architettura |
| Unidirectional Data Flow | Gestione stato |
| Google ML Kit | Riconoscimento codici |
| ZXing | Decodifica barcode |
| Room SQLite | Persistenza dati |
| Coil | Gestione immagini |
| KSP | Code Generation |
| Coroutines | Programmazione asincrona |
| StateFlow | Stato reattivo |

---

# 📂 Struttura del Repository

```text
app/
│
├── data/
│   ├── database/
│   ├── dao/
│   ├── entity/
│   ├── backup/
│   └── repository/
│
├── domain/
│   ├── scanner/
│   ├── barcode/
│   ├── importexport/
│   └── model/
│
├── ui/
│   ├── home/
│   ├── scanner/
│   ├── card/
│   ├── presenter/
│   └── settings/
│
├── viewmodel/
│
├── utils/
│
└── resources/
```

---

# 🚀 Installazione e Compilazione

## Clonazione Repository

```bash
git clone https://github.com/username/smart-barcode-wallet.git
cd smart-barcode-wallet
```

---

## Apertura in Android Studio

1. Aprire Android Studio.
2. Selezionare **Open Existing Project**.
3. Scegliere la cartella del repository.
4. Attendere il completamento della sincronizzazione Gradle.

---

## Build Debug

```bash
./gradlew assembleDebug
```

---

## Build Release

```bash
./gradlew assembleRelease
```

---

## Test Unitari

```bash
./gradlew testDebugUnitTest
```

---

## Installazione APK

```bash
./gradlew installDebug
```

---

# 🔒 Permessi Utilizzati

## CAMERA

Necessario per:

- Scansione barcode.
- Lettura QR Code.
- Acquisizione tramite CameraX.

---

## INTERNET

Utilizzato esclusivamente per:

- Eventuali librerie di supporto.
- Apertura di collegamenti esterni.
- Nessun invio di dati personali.

---

# 🛡️ Privacy

## Privacy First

L'app è progettata seguendo il principio:

> I tuoi dati restano sul tuo dispositivo.

### Nessun Account Richiesto

- Nessuna registrazione.
- Nessun login.
- Nessun tracking.

### Nessun Cloud Obbligatorio

- Database completamente locale.
- Backup esportabile dall'utente.
- Ripristino autonomo.

---

# ✅ Funzionalità Riassunte

✅ Scanner CameraX + ML Kit  
✅ Supporto QR Code e Barcode  
✅ Modalità Presenter Full Screen  
✅ Luminosità automatica per scanner retail  
✅ Interfaccia 3D Card Deck  
✅ Loghi personalizzati  
✅ Personalizzazione colori delle carte  
✅ Backup e ripristino JSON  
✅ Room SQLite locale  
✅ Architettura MVVM moderna  
✅ Supporto ZXing  
✅ Privacy 100% Offline-First

---

# 📄 Licenza

Distribuito secondo la licenza definita nel repository.

```text
Apache License 2.0
```

Per maggiori dettagli consultare:

```text
LICENSE
```
