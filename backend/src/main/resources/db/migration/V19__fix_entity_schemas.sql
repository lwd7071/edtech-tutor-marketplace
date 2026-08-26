-- =====================================================================
-- V19: Fix Entity Schemas for BaseEntity Inheritance
-- =====================================================================

-- For teacher_stats table, we need to add the BaseEntity fields and change the primary key
ALTER TABLE teacher_stats 
ADD COLUMN id uuid DEFAULT gen_random_uuid(),
ADD COLUMN created_at timestamptz DEFAULT now() NOT NULL,
ADD COLUMN updated_at timestamptz DEFAULT now() NOT NULL,
ADD COLUMN is_deleted boolean DEFAULT false NOT NULL;

-- Remove the old primary key
ALTER TABLE teacher_stats DROP CONSTRAINT teacher_stats_pkey;

-- Set the new primary key
ALTER TABLE teacher_stats ADD PRIMARY KEY (id);

-- Keep teacher_id as a unique constraint since a teacher should only have one stats record
ALTER TABLE teacher_stats ADD CONSTRAINT uq_teacher_stats_teacher_id UNIQUE (teacher_id);
