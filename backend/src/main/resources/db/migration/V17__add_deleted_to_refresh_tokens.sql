-- =====================================================================
-- V17: Add missing deleted column for BaseEntity inheritance
-- =====================================================================

ALTER TABLE refresh_tokens ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
