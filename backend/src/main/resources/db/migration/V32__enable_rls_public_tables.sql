-- V32: default-deny row-level security for tables that must remain Backend-owned.
-- No policies are created: PostgREST anon/authenticated access is denied by default.
DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'refresh_tokens',
        'teacher_documents',
        'subject_proposals',
        'subjects',
        'pricing_packages',
        'teacher_profiles',
        'teacher_subjects',
        'teacher_availabilities'
    ] LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);
    END LOOP;
END $$;
