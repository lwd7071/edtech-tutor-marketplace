-- =====================================================================
-- V19: Fix Entity Schemas for BaseEntity Inheritance
-- =====================================================================

DO $$
BEGIN
    -- Add id column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'teacher_stats' AND column_name = 'id'
    ) THEN
        ALTER TABLE teacher_stats ADD COLUMN id uuid DEFAULT gen_random_uuid();
    END IF;

    -- Add created_at column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'teacher_stats' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE teacher_stats ADD COLUMN created_at timestamptz DEFAULT now() NOT NULL;
    END IF;

    -- Add updated_at column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'teacher_stats' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE teacher_stats ADD COLUMN updated_at timestamptz DEFAULT now() NOT NULL;
    END IF;

    -- Add is_deleted column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'teacher_stats' AND column_name = 'is_deleted'
    ) THEN
        ALTER TABLE teacher_stats ADD COLUMN is_deleted boolean DEFAULT false NOT NULL;
    END IF;

    -- Check and update primary key if needed
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.constraint_schema = kcu.constraint_schema
        WHERE tc.table_schema = current_schema()
          AND tc.table_name = 'teacher_stats'
          AND tc.constraint_type = 'PRIMARY KEY'
          AND kcu.column_name = 'teacher_id'
    ) THEN
        ALTER TABLE teacher_stats DROP CONSTRAINT teacher_stats_pkey;
        ALTER TABLE teacher_stats ADD PRIMARY KEY (id);
    END IF;

    -- Check unique constraint on teacher_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = current_schema()
          AND table_name = 'teacher_stats'
          AND constraint_name IN ('uq_teacher_stats_teacher_id', 'teacher_stats_teacher_id_key')
    ) THEN
        ALTER TABLE teacher_stats ADD CONSTRAINT uq_teacher_stats_teacher_id UNIQUE (teacher_id);
    END IF;
END $$;
