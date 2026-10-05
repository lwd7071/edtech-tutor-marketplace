# Local Docker deployment

## Services and ports

| Service | Image/runtime | Host address |
|---|---|---|
| Nginx | nginx 1.30.5 | `http://localhost:8081` |
| Frontend | Next.js 16, Node 22 | Internal port 3000 |
| Backend | Spring Boot 3.2.5, Java 21 | Internal port 8080 |
| PostgreSQL | PostgreSQL 16 | `127.0.0.1:5433` |
| Redis | Redis 7 | Internal port 6379 |

Nginx sends `/api/*` and `/health` to Spring Boot, `/ws` to the STOMP endpoint with WebSocket upgrade headers, and other paths to Next.js. PostgreSQL data is stored in the Docker named volume `edtech-postgres-data` on this machine; backups are written under `backups/`. The former `data/postgres` directory is preserved as a local rollback copy. The Compose network is `edtech-network`.

The database is `edtech_db`. The application connects as `edtech_user`; PostgreSQL administration uses a separate `postgres` password. Local generated credentials are stored in the ignored `.env`. PostgreSQL is not reachable from outside this machine.

## Start, stop, logs

```powershell
Set-Location 'D:\RedApple\Edtech'
docker compose up -d --build
docker compose ps
docker compose logs -f nginx frontend backend postgres redis
docker compose stop
```

`docker compose down` removes the containers and network but leaves `edtech-postgres-data` intact. Do not use `down -v`, which deletes the database volume. Flyway applies pending migrations automatically at backend startup; it is currently at V41 in the local database.

## Smoke URLs

- Tutor Match: `http://localhost:8081`
- Backend health: `http://localhost:8081/health`
- Public subjects API: `http://localhost:8081/api/public/subjects`
- STOMP WebSocket: `ws://localhost:8081/ws`

The local profile uses logging mail and disabled payments. Add valid Google OAuth, payment, email, and Cloudinary values in `.env` before testing those external integrations. Do not copy local values to production.

Browser API and WebSocket requests use the current page hostname, routed through Nginx. When using a Cloudflare Tunnel hostname, add its exact HTTPS origin to `APP_CORS_ALLOWED_ORIGINS` in `.env`, then recreate the backend with `docker compose up -d backend`. Update the allowlist if a temporary Quick Tunnel URL changes.

## Backup

```powershell
.\scripts\backup-postgres.ps1
```

This uses `pg_dump -Fc` through the running PostgreSQL container and writes a compressed custom-format dump to `backups/`. To inspect a dump later, use `pg_restore --list <file>`; restore only into a separately confirmed target database.

Cloudflare Tunnel is not configured yet. After local validation, point a tunnel at `http://localhost:8081`; do not expose PostgreSQL.
