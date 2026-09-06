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

## Lokales Setup

1. Beispiel-Umgebung kopieren:

   ```powershell
   Copy-Item .env.example .env
   ```

2. In [.env.example](/C:/Users/LeonB/Projects/PHT/.env.example) die Secrets anpassen, vor allem `POSTGRES_PASSWORD` und `JWT_SECRET`.

3. Stack starten:

   ```powershell
   docker compose -f deploy/docker-compose.yml up -d --build
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

6. Health-Check pruefen:

   ```powershell
   curl.exe http://localhost:8000/healthz
   ```

## Auth-Endpunkte

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`

Login-Beispiel:

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
