#!/bin/sh
set -eu

# Automatically bring the schema up to date on every container start, so a fresh Portainer
# deployment (or an upgrade to a newer image) needs no manual `alembic upgrade head` step.
alembic upgrade head

exec uvicorn app.main:app --host 0.0.0.0 --port 8000
