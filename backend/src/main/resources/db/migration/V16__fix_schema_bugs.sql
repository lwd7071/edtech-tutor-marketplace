-- =====================================================================
-- V16: Fix Schema Bugs
-- Fix precision for commission_rate, add status check constraint for bookings,
-- and add singleton pattern to platform_settings.
-- =====================================================================

-- 1. Fix commission_rate precision in student_packages
ALTER TABLE student_packages
    ALTER COLUMN commission_rate TYPE NUMERIC(5, 2);

-- 2. Add CHECK constraint for bookings.status
ALTER TABLE bookings
    ADD CONSTRAINT ck_bookings_status
        CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'EXPIRED'));

-- 3. Add Singleton pattern to platform_settings
ALTER TABLE platform_settings
    ADD COLUMN is_singleton BOOLEAN NOT NULL DEFAULT true,
    ADD CONSTRAINT uq_platform_settings_singleton UNIQUE (is_singleton),
    ADD CONSTRAINT ck_platform_settings_singleton CHECK (is_singleton);
