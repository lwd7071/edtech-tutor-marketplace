-- =====================================================================
-- V30: Fix Data Constraints and add Optimistic Locking
-- =====================================================================

-- 1. Thêm Optimistic Locking (version) cho các bảng có nguy cơ race condition
ALTER TABLE invoices ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE trial_requests ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE assignments ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE submissions ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE subject_proposals ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE teacher_bank_accounts ADD COLUMN version bigint NOT NULL DEFAULT 0;
