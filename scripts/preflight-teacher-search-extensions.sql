-- Run this exact file through the same connection and role used by Flyway.
-- Do not substitute an admin/SQL Editor role when validating production access.
SELECT current_user;

SELECT e.extname, n.nspname
FROM pg_extension e
JOIN pg_namespace n ON n.oid = e.extnamespace
WHERE e.extname IN ('unaccent', 'pg_trgm')
ORDER BY e.extname;

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_extension e
        JOIN pg_namespace n ON n.oid = e.extnamespace
        WHERE e.extname IN ('unaccent', 'pg_trgm')
          AND n.nspname <> 'public'
    ) OR (
        SELECT COUNT(*)
        FROM pg_extension
        WHERE extname IN ('unaccent', 'pg_trgm')
    ) <> 2 THEN
        RAISE EXCEPTION 'unaccent and pg_trgm must both exist in public schema';
    END IF;
END
$$;
