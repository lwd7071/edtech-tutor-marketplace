# ADR-0012: Self-declared residence and private credential evidence

## Status

Accepted; schema V38–V40 and credential/residence hardening complete; real Cloudinary provider smoke remains unverified.

## Decision

- Teacher residence is a separate command and does not reopen profile approval. Only province/ward codes are public; the legacy detailed address is not returned by public teacher APIs.
- Credential evidence is stored by private asset identity and metadata. The browser receives bytes through an authorized backend proxy with `Cache-Control: no-store`; public responses contain approved labels only.
- Editing an approved credential removes its public badge until it is approved again. Credential approval does not change teacher profile status.
- On-demand Next.js Data Cache tag revalidation is triggered after transaction commit via internal endpoint with shared secret header; transaction rollback never triggers cache invalidation.

## Consequences

The schema is deployed additively and old profiles remain valid. Storage cleanup on transaction rollback and commit, optimistic locking (409 CONCURRENT_MODIFICATION with version), comprehensive audit trail logging, and Next Data Cache on-demand tag revalidation with TTL fallback are fully implemented and regression-tested. Real Cloudinary provider smoke test remains unverified.
