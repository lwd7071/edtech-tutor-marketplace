# ADR 0009: Server-owned finance proof and atomic audit

## Status

Accepted — 2026-09-13

## Decision

Refund and payout completion use `multipart/form-data`: a JSON metadata part and one required proof file. The server validates MIME/extension/size, uploads through `FileStoragePort`, and persists only adapter-returned identifiers. Upload cleanup is registered for transaction rollback. Idempotency fingerprints include file size, MIME and SHA-256 bytes.

All finance, extension and platform-settings state changes append an `AuditLog` through the neutral `AuditTrailFacade` in the same transaction. Snapshots contain only safe identifiers, status, amounts/counts, timestamps and versions.

Migration V31 adds fail-fast preflight and terminal-state proof constraints. Existing migrations and historical rows are not rewritten or backfilled automatically.

## Consequences

- Client-supplied proof URLs/public IDs are no longer trusted.
- Replayed idempotent commands return the original result without duplicate upload, ledger mutation or audit.
- Production rollout must stop for any existing terminal record missing proof and requires manual reconciliation.
