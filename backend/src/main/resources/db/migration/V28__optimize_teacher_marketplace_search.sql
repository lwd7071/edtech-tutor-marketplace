-- Accent-insensitive teacher search uses an immutable wrapper so PostgreSQL
-- can build functional indexes. This assumes the public.unaccent dictionary
-- never changes. If it changes, REINDEX the dependent indexes and invalidate
-- teacher-search cache entries before serving traffic again.

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

DO $$
DECLARE
    required_extension text;
    extension_schema text;
BEGIN
    FOR required_extension IN SELECT unnest(ARRAY['unaccent', 'pg_trgm'])
    LOOP
        SELECT n.nspname
        INTO extension_schema
        FROM pg_extension e
        JOIN pg_namespace n ON n.oid = e.extnamespace
        WHERE e.extname = required_extension;

        IF extension_schema IS DISTINCT FROM 'public' THEN
            RAISE EXCEPTION 'Extension % must be installed in public schema, found %',
                required_extension, COALESCE(extension_schema, '<missing>');
        END IF;
    END LOOP;
END
$$;

CREATE OR REPLACE FUNCTION public.f_unaccent_immutable(value text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
STRICT
SET search_path = public, pg_temp
AS $$
    SELECT public.unaccent(
        'public.unaccent'::regdictionary,
        lower(value)
    )
$$;

CREATE INDEX ix_users_search_full_name_trgm
ON users
USING gin (
    public.f_unaccent_immutable(full_name) public.gin_trgm_ops
)
WHERE status = 'ACTIVE' AND is_deleted = false;

CREATE INDEX ix_teacher_profiles_search_bio_trgm
ON teacher_profiles
USING gin (
    public.f_unaccent_immutable(bio) public.gin_trgm_ops
)
WHERE profile_status = 'APPROVED'
  AND is_visible = true
  AND is_deleted = false;

CREATE INDEX ix_pricing_packages_active_teacher_price
ON pricing_packages (teacher_id, price_vnd)
WHERE status = 'ACTIVE' AND is_deleted = false;
