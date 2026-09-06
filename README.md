# PHT - Private Health Tracker

Lokale Health-Tracking-Loesung mit Android-App, selfhosted FastAPI-Backend und spaeterem Vue-Dashboard. Phase 1 liefert das erste Server-Grundgeruest fuer Blutdruckmessungen vom Beurer BM48.

## Phase-1-Stand

- [server/](/C:/Users/LeonB/Projects/PHT/server) mit FastAPI, SQLAlchemy, Alembic, JWT-Auth, API-Key-Auth und structlog
- [deploy/docker-compose.yml](/C:/Users/LeonB/Projects/PHT/deploy/docker-compose.yml) fuer TimescaleDB/PostgreSQL 16 und API
- idempotenter Sync-Endpunkt fuer Blutdruckmessungen
- Health-Check und JWT-geschuetzte Messwertliste
- Bootstrap-CLI fuer ersten User und erstes Geraet

Noch offen fuer spaetere Phasen:

- Android-App in [app/](/C:/Users/LeonB/Projects/PHT/app)
- Vue-3-Dashboard in [dashboard/](/C:/Users/LeonB/Projects/PHT/dashboard)
- PDF/CSV/JSON-Export-Endpunkte
- Gotify-Benachrichtigungen aktiv verdrahten

## Projektstruktur

```text
PHT/
├── app/
├── dashboard/
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

Der Stack ist Portainer-kompatibel: Standard-Compose-Syntax, benannte Volumes, sinnvolle Container-Namen. `docker-compose.yml` enthaelt bewusst **keine** `env_file`-Referenz mehr, damit er sich direkt ueber Portainers Stack-Environment-Variablen fuellen laesst, egal ob per Web-Editor oder Git-Repository-Deploy.

1. **Stack anlegen**: Portainer → *Stacks* → *Add stack*, Name z.B. `pht`.
2. **Build-Methode** waehlen:
   - **Repository** (empfohlen, da der `api`-Service ein Image aus [server/Dockerfile](/C:/Users/LeonB/Projects/PHT/server/Dockerfile) mit `context: ../server` baut): URL `https://github.com/Loxitation/PHT.git`, Compose-Pfad `deploy/docker-compose.yml`, Branch `main`. Da das Repo privat ist, unter *Authentication* Username + GitHub Personal Access Token hinterlegen. Portainer klont dabei das ganze Repo, wodurch der relative Build-Context `../server` aufgeloest werden kann.
   - **Web editor**: Funktioniert nur, wenn du vorher ein fertiges `api`-Image in eine Registry gepusht hast und `build:` im Compose durch `image: <registry>/pht-api:<tag>` ersetzt, da beim Web-Editor kein Source-Tree fuer den Build vorhanden ist. Fuer Phase 1 daher **Repository** bevorzugen.
3. **Environment variables** im Stack-Formular (Portainer legt daraus intern eine `.env`-aequivalente Konfiguration an) eintragen, siehe [.env.example](/C:/Users/LeonB/Projects/PHT/.env.example):

   | Variable | Beispiel |
   |---|---|
   | `POSTGRES_DB` | `pht` |
   | `POSTGRES_USER` | `pht` |
   | `POSTGRES_PASSWORD` | *sicheres Passwort* |
   | `POSTGRES_PORT` | `5432` |
   | `API_PORT` | `8000` |
   | `JWT_SECRET` | *langer Zufallswert* |
   | `JWT_ALGORITHM` | `HS256` |
   | `ACCESS_TOKEN_EXPIRE_MINUTES` | `15` |
   | `REFRESH_TOKEN_EXPIRE_DAYS` | `30` |
   | `LOGIN_RATE_LIMIT` | `5/minute` |
   | `GOTIFY_ENABLED` | `false` |
   | `GOTIFY_URL` | *leer, kommt spaeter* |
   | `GOTIFY_TOKEN` | *leer, kommt spaeter* |
   | `LOG_LEVEL` | `INFO` |

4. **Deploy the stack** klicken. Portainer baut das `api`-Image aus [server/Dockerfile](/C:/Users/LeonB/Projects/PHT/server/Dockerfile) und startet `pht-postgres` + `pht-api`.
5. **Migration ausfuehren** ganz ohne SSH/CLI ueber die Portainer-GUI:
   - *Containers* → `pht-api` → *Console* → *Connect* (Shell `/bin/sh` oder `/bin/bash`).
   - Im geoeffneten Browser-Terminal:
     ```sh
     python -m alembic upgrade head
     ```
6. **Ersten Admin-User + Beurer-BM48-Device anlegen**, im selben Console-Fenster:
   ```sh
   python -m app.bootstrap --username admin --password "EIN-SICHERES-PASSWORT" --device-name "Beurer BM48" --device-type beurer_bm48
   ```
   Ausgabe merken (der `api_key` wird nur einmal angezeigt):
   ```json
   {"username":"admin","device_id":"...","api_key":"..."}
   ```
7. **Health-Check** von einem beliebigen LAN-Rechner (Reverse Proxy folgt spaeter):
   ```powershell
   curl.exe http://<server-ip>:8000/healthz
   ```

Die folgenden Beispiele nutzen `http://<server-ip>:8000`; ersetze das bei Bedarf durch `localhost`, falls du direkt auf dem Server testest.

## Alternatives lokales Setup (Docker Compose CLI)

Falls du statt Portainer die Compose-CLI direkt nutzen willst:

1. Beispiel-Umgebung kopieren:

   ```powershell
   Copy-Item .env.example .env
   ```

2. In der neuen `.env` die Secrets anpassen, vor allem `POSTGRES_PASSWORD` und `JWT_SECRET`.

3. Stack mit `--env-file` starten (die Compose-Datei liest Variablen jetzt nur noch aus der Shell-Umgebung bzw. `--env-file`, nicht mehr automatisch aus `../.env`):

   ```powershell
   docker compose --env-file .env -f deploy/docker-compose.yml up -d --build
   ```

4. Migration ausfuehren:

   ```powershell
   docker compose -f deploy/docker-compose.yml exec api python -m alembic upgrade head
   ```

5. Ersten Admin-User und erstes Device anlegen:

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

6. Health-Check pruefen (vom Server selbst mit `localhost`, aus dem LAN mit der Server-IP):

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

## Wichtige Dateien

- [server/app/main.py](/C:/Users/LeonB/Projects/PHT/server/app/main.py)
- [server/app/api/v1/auth.py](/C:/Users/LeonB/Projects/PHT/server/app/api/v1/auth.py)
- [server/app/api/v1/measurements.py](/C:/Users/LeonB/Projects/PHT/server/app/api/v1/measurements.py)
- [server/alembic/versions/20260906_01_initial_schema.py](/C:/Users/LeonB/Projects/PHT/server/alembic/versions/20260906_01_initial_schema.py)
