-- Read-only RLS preflight. Run with the Flyway connection/user.
-- Do not save output containing connection details or secrets.
SELECT grantee,
       table_schema,
       table_name,
       privilege_type,
       is_grantable
FROM information_schema.role_table_grants
WHERE table_schema = 'public'
  AND table_name IN (
    'flyway_schema_history',
    'refresh_tokens',
    'teacher_documents',
    'subject_proposals',
    'subjects',
    'pricing_packages',
    'teacher_profiles',
    'teacher_subjects',
    'teacher_availabilities'
  )
ORDER BY table_name, grantee, privilege_type;

SELECT current_user,
       session_user,
       r.rolsuper,
       r.rolbypassrls,
       r.rolcanlogin
FROM pg_roles r
WHERE r.rolname = current_user;

SELECT schemaname, tablename, rowsecurity, forcerowsecurity
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN (
    'flyway_schema_history',
    'refresh_tokens',
    'teacher_documents',
    'subject_proposals',
    'subjects',
    'pricing_packages',
    'teacher_profiles',
    'teacher_subjects',
    'teacher_availabilities'
  )
ORDER BY tablename;
