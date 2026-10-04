# PHT - Private Health Tracker

Lokale Health-Tracking-Loesung mit Android-App, selfhosted FastAPI-Backend und Vue-3-Dashboard. Phase 1 liefert das Server-Grundgeruest fuer Blutdruckmessungen vom Beurer BM48, Phase 2 die Android-App, die diese Messungen per BLE ausliest und synchronisiert, Phase 3 das Web-Dashboard zur Visualisierung.

## Phase-1-Stand (Server)

- [server/](/C:/Users/LeonB/Projects/PHT/server) mit FastAPI, SQLAlchemy, Alembic, JWT-Auth, API-Key-Auth und structlog
- [deploy/docker-compose.yml](/C:/Users/LeonB/Projects/PHT/deploy/docker-compose.yml) fuer TimescaleDB/PostgreSQL 16 und API
- idempotenter Sync-Endpunkt fuer Blutdruckmessungen
- Health-Check und JWT-geschuetzte Messwertliste
- Bootstrap-CLI fuer ersten User und erstes Geraet

## Phase-2-Stand (Android-App)

- [app/](/C:/Users/LeonB/Projects/PHT/app) : natives Kotlin-Projekt (Gradle, kein Google-Play-Services-Zwang, GrapheneOS-kompatibel)
- BLE-Layer mit `DeviceProfile`-Abstraktion; erstes Profil `BeurerBm48Profile` implementiert den Standard-Bluetooth-SIG-Blutdruck-Service (`0x1810`/`0x2A35`, Indications)
- `BloodPressureMeasurementParser` dekodiert Flags/SFLOAT/Timestamp/Status exakt nach Bluetooth-SIG-Spezifikation (Unit-Test verifiziert gegen den echten BM48-Beispieldatensatz aus Phase 1)
- Room-DB als lokale Offline-Queue (`blood_pressure_measurements`, Sync-Status PENDING/SYNCED/FAILED)
- WorkManager-Sync (`SyncWorker`/`SyncScheduler`) mit Exponential-Backoff: sofortiger Versuch nach neuer Messung + periodischer Sicherheitsnetz-Lauf alle 30 Minuten
- Verschluesselte lokale Ablage (EncryptedSharedPreferences/Keystore) fuer Server-URL, API-Key und Geraete-ID
- Jetpack-Compose-UI: Messwerte-Liste, Geraete-Scan/Verbindung, Einstellungen

## Phase-3-Stand (Web-Dashboard)

- [dashboard/](/C:/Users/LeonB/Projects/PHT/dashboard): Vue 3 + Vite + TypeScript SPA
- JWT-Login (Pinia Auth-Store) mit Access-/Refresh-Token-Rotation inkl. automatischem Token-Refresh bei 401 (Axios-Interceptor)
- Blutdruck-Verlauf: Chart.js-Liniendiagramm (systolisch/diastolisch/puls) + tabellarische Ansicht, Daten ueber `GET /api/v1/measurements`
- Route-Guards: ungeschuetzte Seiten (ausser Login) erfordern ein gueltiges Access-Token
- Eigener Dashboard-Container: Multi-Stage-Dockerfile (Node-Build → nginx), API-Basis-URL wird **zur Laufzeit** via `API_BASE_URL`-Umgebungsvariable gesetzt (kein Rebuild pro Deployment noetig, siehe `docker-entrypoint.sh`)
- Server-seitig ergaenzt: CORS-Middleware (`CORS_ALLOWED_ORIGINS`) sowie `GET /api/v1/measurements` liefert jetzt systolic/diastolic/pulse direkt mit (vorher nur im `raw_payload` versteckt)

Noch offen fuer spaetere Phasen:

- PDF/CSV/JSON-Export-Endpunkte
- Gotify-Benachrichtigungen aktiv verdrahten
- Health-Connect-Integration, weitere Geraetetypen (Waage, Wearable, manuelle Eingabe)
- Dashboard: Geraete-/API-Key-Verwaltung, Benutzerverwaltung, Grenzwert-Konfiguration

## Projektstruktur

```text
PHT/
├── app/                        # Android-App (Kotlin, Gradle)
│   ├── app/
│   │   ├── src/main/kotlin/de/loxitation/pht/
│   │   │   ├── ble/            # DeviceProfile-Abstraktion, BM48-Parser, GATT-Verbindung
│   │   │   ├── data/local/     # Room-DB (Offline-Queue)
│   │   │   ├── data/remote/    # Retrofit/kotlinx.serialization
│   │   │   ├── data/repository/# Measurement-/Settings-Repository
│   │   │   ├── sync/           # WorkManager Sync-Worker/-Scheduler
│   │   │   └── ui/             # Jetpack Compose Screens/ViewModels
│   │   └── src/test/kotlin/    # Unit-Tests (u.a. BM48-Parser-Regressionstest)
│   ├── gradlew / gradlew.bat
│   └── settings.gradle.kts
├── dashboard/                   # Vue 3 + Vite + TypeScript SPA
│   ├── src/
│   │   ├── api/                 # Axios-Client, Auth-/Measurement-Requests
│   │   ├── stores/               # Pinia Auth-Store
│   │   ├── router/               # Vue Router + Auth-Guards
│   │   ├── views/                 # Login-/Measurements-View
│   │   └── types/                 # Shared TS-Typen (spiegeln Server-Schemas)
│   ├── Dockerfile               # Multi-Stage: Node-Build + nginx
│   └── nginx.conf
├── deploy/
│   └── docker-compose.yml
├── docs/
├── server/
│   ├── alembic/
│   ├── app/
│   ├── Dockerfile
│   └── requirements.txt
├── .env.example
└── README.md
```

## Datenmodell

Phase 1 legt diese Tabellen an:

- `users`
- `refresh_tokens`
- `devices`
- `measurements`
- `blood_pressure_readings`

Die generische Huellentabelle `measurements` bleibt absichtlich erweiterbar fuer weitere Geraettypen. Die Blutdruck-Details liegen in `blood_pressure_readings`.

## Deployment via Portainer (empfohlener Weg)

Seit dem ersten Release (siehe [GitHub Releases](https://github.com/Loxitation/PHT/releases)) sind `api` und `dashboard` fertig gebaute Images auf GitHub Container Registry (`ghcr.io/loxitation/pht-api`, `ghcr.io/loxitation/pht-dashboard`, jeweils fuer linux/amd64 und linux/arm64). Portainer muss also nichts selbst bauen - nur Images pullen und starten.

1. Falls die Packages privat sind (Standard bei GITHUB_TOKEN-Pushes aus einem privaten Repo): einmalig auf GitHub unter Profil/Org -> Packages -> pht-api / pht-dashboard -> Package settings -> Change visibility -> Public umstellen. Die Images enthalten keine Secrets (alle Zugangsdaten kommen als Umgebungsvariablen zur Laufzeit rein). Alternativ in Portainer unter Registries eine GHCR-Registry mit einem GitHub-PAT (Scope read:packages) hinterlegen.
2. Stack anlegen: Portainer -> Stacks -> Add stack, Name z.B. pht.
3. Build-Methode: Web editor waehlen und den Inhalt von deploy/docker-compose.yml einfuegen (kein Repository-Checkout noetig, da nichts gebaut wird).
4. Environment variables im Stack-Formular eintragen, siehe .env.example:

   | Variable | Beispiel |
   |---|---|
   | `PHT_VERSION` | `latest` oder z.B. `0.1.0` zum Pinnen auf ein bestimmtes Release |
   | `POSTGRES_DB` | `pht` |
   | `POSTGRES_USER` | `pht` |
   | `POSTGRES_PASSWORD` | *sicheres Passwort* |
   | `POSTGRES_PORT` | `5432` |
   | `API_PORT` | `8000` |
   | `DASHBOARD_PORT` | `8080` |
   | `DASHBOARD_API_BASE_URL` | `https://pht.loxitation.de` (muss vom Browser aus erreichbar sein) |
   | `JWT_SECRET` | *langer Zufallswert* |
   | `JWT_ALGORITHM` | `HS256` |
   | `ACCESS_TOKEN_EXPIRE_MINUTES` | `15` |
   | `REFRESH_TOKEN_EXPIRE_DAYS` | `30` |
   | `LOGIN_RATE_LIMIT` | `5/minute` |
   | `CORS_ALLOWED_ORIGINS` | `https://pht.loxitation.de,http://pht.leda:8080` |
   | `GOTIFY_ENABLED` | `false` |
   | `GOTIFY_URL` | *leer, kommt spaeter* |
   | `GOTIFY_TOKEN` | *leer, kommt spaeter* |
   | `LOG_LEVEL` | `INFO` |

5. Deploy the stack klicken. Portainer zieht die drei Images (timescale/timescaledb, pht-api, pht-dashboard) und startet den Stack. Die Datenbank-Migration laeuft automatisch beim Start des api-Containers (siehe server/docker-entrypoint.sh) - kein manueller Schritt noetig.
6. Einmalig ersten Admin-User + Beurer-BM48-Device anlegen, ueber die Portainer-GUI (Containers -> pht-api -> Console -> Connect, Shell /bin/sh):
   ```sh
   python -m app.bootstrap --username admin --password "EIN-SICHERES-PASSWORT" --device-name "Beurer BM48" --device-type beurer_bm48
   ```
   Ausgabe merken (der `api_key` wird nur einmal angezeigt):
   ```json
   {"username":"admin","device_id":"...","api_key":"..."}
   ```
7. Health-Check:
   ```powershell
   curl.exe http://<server-ip>:8000/healthz
   ```
8. Dashboard unter `http://<server-ip>:8080` (bzw. ueber Nginx Proxy Manager) aufrufen und mit dem Admin-User einloggen.

Ein neues Release einspielen: `PHT_VERSION` auf die neue Versionsnummer setzen (oder bei `latest` einfach den Stack in Portainer neu pullen & redeployen) - Migrationen laufen automatisch beim Neustart des api-Containers mit.

Die folgenden Beispiele nutzen `http://<server-ip>:8000`; ersetze das bei Bedarf durch `localhost`, falls du direkt auf dem Server testest.

## Releases erstellen (fuer Maintainer)

Ein git-Tag im Format `v<version>` (z.B. `v0.1.0`) pushen - das Release-Workflow baut dann automatisch:

- die signierte Android-APK (`pht-<version>.apk`) als GitHub-Release-Asset
- `ghcr.io/loxitation/pht-api:<version>` + `:latest` (linux/amd64, linux/arm64)
- `ghcr.io/loxitation/pht-dashboard:<version>` + `:latest` (linux/amd64, linux/arm64)

Voraussetzung: die Repository-Secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` muessen unter Settings -> Secrets and variables -> Actions gesetzt sein (siehe naechster Abschnitt). Der Signing-Keystore wird bewusst nicht im Repo gespeichert.

```powershell
git tag v0.1.0
git push origin v0.1.0
```

## Android-Signing-Secrets einrichten (einmalig, fuer Maintainer)

Die Android-App braucht fuer Release-Builds einen stabilen Signing-Key (sonst laesst sich eine neu installierte Version nicht als Update ueber eine bestehende installieren). Der Key wird nicht im Repo abgelegt, sondern als GitHub Actions Secret hinterlegt:

1. GitHub -> Repo -> Settings -> Secrets and variables -> Actions -> New repository secret, vier Secrets anlegen:
   - `ANDROID_KEYSTORE_BASE64`: Base64-kodierter Inhalt der .keystore-Datei
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
2. Falls noch kein Keystore existiert, lokal erzeugen (JDK muss installiert sein):
   ```powershell
   keytool -genkeypair -v -keystore pht-release.keystore -alias pht -keyalg RSA -keysize 2048 -validity 36500
   ```
   Danach die Datei base64-kodieren und den Inhalt als `ANDROID_KEYSTORE_BASE64` einfuegen:
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("pht-release.keystore")) | Set-Clipboard
   ```
3. Keystore-Datei danach sicher aufbewahren (z.B. Passwort-Manager) und nicht committen - ohne ihn koennen spaetere Releases nicht mehr mit derselben Signatur gebaut werden.

## Alternatives lokales Setup (Docker Compose CLI, aus Quellcode gebaut)

Falls du statt vorgefertigter Images lieber lokal aus dem Quellcode baust (z.B. fuer eigene Aenderungen):

1. Beispiel-Umgebung kopieren:

   ```powershell
   Copy-Item .env.example .env
   ```

2. In der neuen `.env` die Secrets anpassen, vor allem `POSTGRES_PASSWORD` und `JWT_SECRET`.

3. Stack mit dem Build-Override starten:

   ```powershell
   docker compose --env-file .env -f deploy/docker-compose.yml -f deploy/docker-compose.build.yml up -d --build
   ```

4. Migration laeuft automatisch beim Start des api-Containers. Ersten Admin-User und erstes Device anlegen:

   ```powershell
   docker compose -f deploy/docker-compose.yml exec api python -m app.bootstrap `
     --username admin `
     --password change-me-now `
     --device-name "Beurer BM48" `
     --device-type beurer_bm48
   ```

   Ausgabe-Beispiel:

   ```json
   {"username":"admin","device_id":"...","api_key":"..."}
   ```

5. Health-Check pruefen (vom Server selbst mit `localhost`, aus dem LAN mit der Server-IP):

   ```powershell
   curl.exe http://localhost:8000/healthz
   ```

## Auth-Endpunkte

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`

Login-Beispiel (`localhost` durch die Server-IP ersetzen, falls von einem anderen LAN-Rechner getestet wird):

```powershell
curl.exe -X POST http://localhost:8000/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"admin\",\"password\":\"change-me-now\"}"
```

## Blood-Pressure-Sync testen

Der Sync-Endpunkt ist per `X-API-Key` geschuetzt:

- `POST /api/v1/measurements/blood-pressure`

Beispiel mit dem abgestimmten BM48-Datensatz:

- Rohdaten: `16 96 00 5b 00 00 00 ea 07 09 06 14 19 00 59 00 04 40`
- Dekodiert: systolic `150`, diastolic `91`, pulse `89`, time `2026-09-06T20:25:00+02:00`, measurement status raw `16388`

```powershell
$API_KEY = "<api-key-aus-bootstrap>"
$DEVICE_ID = "<device-id-aus-bootstrap>"
$MEASUREMENT_UUID = [guid]::NewGuid().ToString()

curl.exe -X POST http://localhost:8000/api/v1/measurements/blood-pressure `
  -H "Content-Type: application/json" `
  -H "X-API-Key: $API_KEY" `
  -d "{""measurementUuid"":""$MEASUREMENT_UUID"",""deviceId"":""$DEVICE_ID"",""systolic"":150,""diastolic"":91,""pulse"":89,""time"":""2026-09-06T20:25:00+02:00"",""zoneOffset"":7200,""measurementStatusRaw"":16388,""rawPayload"":{""rawHex"":""16 96 00 5b 00 00 00 ea 07 09 06 14 19 00 59 00 04 40"",""bluetoothServiceUuid"":""00001810-0000-1000-8000-00805f9b34fb"",""bluetoothCharacteristicUuid"":""00002a35-0000-1000-8000-00805f9b34fb""}}"
```

Erneutes Senden mit derselben `measurementUuid` ist idempotent und liefert den bereits gespeicherten Datensatz zurueck.

## Messwerte abrufen

JWT-geschuetzt:

- `GET /api/v1/measurements`

Beispiel:

```powershell
$TOKEN = "<access-token-aus-login>"
curl.exe http://localhost:8000/api/v1/measurements -H "Authorization: Bearer $TOKEN"
```

## Wichtige Dateien (Server)

- [server/app/main.py](/C:/Users/LeonB/Projects/PHT/server/app/main.py)
- [server/app/api/v1/auth.py](/C:/Users/LeonB/Projects/PHT/server/app/api/v1/auth.py)
- [server/app/api/v1/measurements.py](/C:/Users/LeonB/Projects/PHT/server/app/api/v1/measurements.py)
- [server/alembic/versions/20260906_01_initial_schema.py](/C:/Users/LeonB/Projects/PHT/server/alembic/versions/20260906_01_initial_schema.py)

## Android-App installieren

**Einfachster Weg**: Fertige APK von der [Releases-Seite](https://github.com/Loxitation/PHT/releases) herunterladen (`pht-<version>.apk`), auf das Android-Geraet uebertragen und oeffnen. Android fragt beim ersten Mal nach Erlaubnis fuer "App-Installation von dieser Quelle" - zustimmen und installieren. Die APK ist signiert (siehe "Android-Signing-Secrets" oben), sodass spaetere Updates ueber die bestehende Installation installiert werden koennen, ohne vorher deinstallieren zu muessen.

## Android-App selbst bauen und testen

Nur noetig, wenn du Aenderungen am Code vornehmen willst - fuer reine Nutzung reicht die fertige APK von oben.

Voraussetzungen: JDK 17, Android SDK (mind. Platform 34 + Build-Tools 34.0.0). Android Studio (aktuelle Version) ist der einfachste Weg, beides zu bekommen; alternativ reichen JDK 17 + `sdkmanager` aus den Android-Commandline-Tools.

1. `app/local.properties` anlegen (wird nicht committet) mit dem Pfad zu deinem Android SDK:

   ```properties
   sdk.dir=C:\\Pfad\\zum\\Android\\Sdk
   ```

2. Debug-APK bauen:

   ```powershell
   cd app
   .\gradlew.bat assembleDebug
   ```

   Das APK liegt danach unter `app/app/build/outputs/apk/debug/app-debug.apk`.

3. Unit-Tests ausfuehren (u.a. Regressionstest des BLE-Parsers gegen den echten BM48-Beispieldatensatz):

   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```

4. Lint pruefen:

   ```powershell
   .\gradlew.bat lintDebug
   ```

5. App auf einem Geraet/Emulator installieren (USB-Debugging bzw. `adb` im PATH):

   ```powershell
   .\gradlew.bat installDebug
   ```

### App mit dem Phase-1-Server verbinden

1. Auf dem Server per Bootstrap-Kommando (siehe oben) einen Admin-User + ein Geraet mit API-Key anlegen, falls noch nicht geschehen.
2. In der App unter **Einstellungen**: Server-URL (z.B. `https://pht.loxitation.de` oder `http://pht.leda:8000` im LAN), den API-Key und die Geraete-ID aus Schritt 1 eintragen und speichern.
3. Unter **Geraet**: Scan starten (Bluetooth-Berechtigungen bei Bedarf erteilen), Beurer BM48 einschalten/messen. Die App verbindet sich automatisch, empfaengt die Messung per Indication, speichert sie lokal in der Room-Offline-Queue und stoesst sofort einen Sync-Versuch an.
4. Unter **Messwerte** erscheint die Messung mit Sync-Status (wartend/synchronisiert/fehlgeschlagen). Bei Netzwerkfehlern retried WorkManager automatisch mit exponentiellem Backoff, zusaetzlich laeuft alle 30 Minuten ein periodischer Sicherheitsnetz-Sync.

### GrapheneOS-Hinweise

- Die App fragt zur Laufzeit nur Bluetooth-Berechtigungen an (`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` ab Android 12, sonst `BLUETOOTH`/`ACCESS_FINE_LOCATION`); `BLUETOOTH_SCAN` ist mit `neverForLocation` deklariert, da nur nach der eigenen Service-UUID gescannt wird, nie nach Standortdaten.
- Keine Google-Play-Services-Abhaengigkeit; Internetzugriff kann in GrapheneOS pro App wie gewohnt eingeschraenkt werden (Netzwerkzugriff wird nur fuer den Sync zum eigenen Server benoetigt).
- `android:usesCleartextTraffic="true"` erlaubt zusaetzlich `http://` im LAN (z.B. `*.leda`); extern sollte ausschliesslich `https://*.loxitation.de` verwendet werden.

## Wichtige Dateien (Android-App)

- [app/app/src/main/kotlin/de/loxitation/pht/ble/BloodPressureMeasurementParser.kt](/C:/Users/LeonB/Projects/PHT/app/app/src/main/kotlin/de/loxitation/pht/ble/BloodPressureMeasurementParser.kt)
- [app/app/src/main/kotlin/de/loxitation/pht/ble/BleConnectionManager.kt](/C:/Users/LeonB/Projects/PHT/app/app/src/main/kotlin/de/loxitation/pht/ble/BleConnectionManager.kt)
- [app/app/src/main/kotlin/de/loxitation/pht/sync/SyncWorker.kt](/C:/Users/LeonB/Projects/PHT/app/app/src/main/kotlin/de/loxitation/pht/sync/SyncWorker.kt)
- [app/app/src/main/kotlin/de/loxitation/pht/data/repository/MeasurementRepository.kt](/C:/Users/LeonB/Projects/PHT/app/app/src/main/kotlin/de/loxitation/pht/data/repository/MeasurementRepository.kt)
- [app/app/src/test/kotlin/de/loxitation/pht/ble/BloodPressureMeasurementParserTest.kt](/C:/Users/LeonB/Projects/PHT/app/app/src/test/kotlin/de/loxitation/pht/ble/BloodPressureMeasurementParserTest.kt)

## Dashboard lokal entwickeln

1. Abhaengigkeiten installieren:

   ```powershell
   cd dashboard
   npm install
   ```

2. `.env.local` anlegen (wird nicht committet), damit das Dashboard den lokal laufenden Server findet:

   ```properties
   VITE_API_BASE_URL=http://localhost:8000
   ```

3. Dev-Server starten:

   ```powershell
   npm run dev
   ```

   Das Dashboard laeuft danach unter `http://localhost:5173`. Damit der Server CORS-Requests von dort akzeptiert, in der Server-`.env` `CORS_ALLOWED_ORIGINS=http://localhost:5173` setzen (bzw. zusaetzlich zur Produktions-Domain ergaenzen).

4. Production-Build + Typecheck:

   ```powershell
   npm run build
   ```

## Dashboard per Docker betreiben (ohne Portainer)

Fuer den Portainer-Weg siehe den Abschnitt "Deployment via Portainer" oben. Der `dashboard`-Service in [deploy/docker-compose.yml](/C:/Users/LeonB/Projects/PHT/deploy/docker-compose.yml) nutzt das vorgefertigte `ghcr.io/loxitation/pht-dashboard`-Image und liest die API-Basis-URL **zur Laufzeit** aus der `API_BASE_URL`-Umgebungsvariable (siehe `docker-entrypoint.sh` im Dashboard-Image) - kein Rebuild pro Deployment noetig.

```powershell
docker compose --env-file .env -f deploy/docker-compose.yml up -d
```

`DASHBOARD_API_BASE_URL` in der `.env` muss eine URL sein, die der **Browser des Nutzers** erreichen kann (z.B. `https://pht.loxitation.de` oder `http://pht.leda:8000`) - nicht der interne Docker-Service-Name `api`.

## Wichtige Dateien (Dashboard)

- [dashboard/src/stores/auth.ts](/C:/Users/LeonB/Projects/PHT/dashboard/src/stores/auth.ts)
- [dashboard/src/api/client.ts](/C:/Users/LeonB/Projects/PHT/dashboard/src/api/client.ts)
- [dashboard/src/views/MeasurementsView.vue](/C:/Users/LeonB/Projects/PHT/dashboard/src/views/MeasurementsView.vue)
- [dashboard/src/router/index.ts](/C:/Users/LeonB/Projects/PHT/dashboard/src/router/index.ts)
- [dashboard/Dockerfile](/C:/Users/LeonB/Projects/PHT/dashboard/Dockerfile)
