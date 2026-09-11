ALTER TABLE invoices
    ADD COLUMN subject_id_snapshot uuid,
    ADD COLUMN package_name_snapshot varchar(150),
    ADD COLUMN total_sessions_snapshot int,
    ADD COLUMN duration_days_snapshot int,
    ADD COLUMN session_duration_minutes_snapshot int,
    ADD COLUMN commission_rate_snapshot numeric(5,2),
    ADD COLUMN return_url varchar(500),
    ADD COLUMN cancel_url varchar(500),
    ADD COLUMN fingerprint_version smallint NOT NULL DEFAULT 1;

UPDATE invoices i SET
    subject_id_snapshot = sp.subject_id,
    package_name_snapshot = sp.package_name_snapshot,
    total_sessions_snapshot = sp.total_sessions,
    duration_days_snapshot = GREATEST(1, ROUND(EXTRACT(EPOCH FROM (sp.expires_at-sp.starts_at))/86400)::int),
    commission_rate_snapshot = sp.commission_rate
FROM student_packages sp
WHERE sp.invoice_id=i.id;

UPDATE invoices i SET
    subject_id_snapshot = COALESCE(i.subject_id_snapshot,p.subject_id),
    package_name_snapshot = COALESCE(i.package_name_snapshot,p.name),
    total_sessions_snapshot = COALESCE(i.total_sessions_snapshot,p.total_sessions),
    duration_days_snapshot = COALESCE(i.duration_days_snapshot,p.duration_days),
    session_duration_minutes_snapshot = p.session_duration_minutes,
    commission_rate_snapshot = COALESCE(i.commission_rate_snapshot,(SELECT commission_rate FROM platform_settings ORDER BY updated_at DESC LIMIT 1))
FROM pricing_packages p
WHERE p.id=i.pricing_package_id;

DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM invoices WHERE subject_id_snapshot IS NULL OR package_name_snapshot IS NULL
    OR total_sessions_snapshot IS NULL OR duration_days_snapshot IS NULL
    OR session_duration_minutes_snapshot IS NULL OR commission_rate_snapshot IS NULL) THEN
   RAISE EXCEPTION 'Cannot backfill immutable invoice purchase terms';
 END IF;
END $$;

ALTER TABLE invoices
    ALTER COLUMN subject_id_snapshot SET NOT NULL,
    ALTER COLUMN package_name_snapshot SET NOT NULL,
    ALTER COLUMN total_sessions_snapshot SET NOT NULL,
    ALTER COLUMN duration_days_snapshot SET NOT NULL,
    ALTER COLUMN session_duration_minutes_snapshot SET NOT NULL,
    ALTER COLUMN commission_rate_snapshot SET NOT NULL,
    ADD CONSTRAINT ck_invoice_snapshot_terms CHECK (total_sessions_snapshot>0 AND duration_days_snapshot>0
      AND session_duration_minutes_snapshot>0 AND commission_rate_snapshot BETWEEN 0 AND 100);
