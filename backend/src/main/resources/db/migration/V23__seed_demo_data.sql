-- =====================================================================
-- V23: SEED DEMO DATA
-- Pre-populates sample data for end-to-end demo and testing.
-- BCrypt hash for 'Password123!': $2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa
-- =====================================================================

-- 1. ADMIN USER
INSERT INTO users (id, email, password_hash, full_name, phone, role, status)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@edtech.vn',
    '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa',
    'Hệ thống Quản trị viên',
    '0901000001',
    'ADMIN',
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

-- 2. TEACHER USERS & PROFILES
-- Teacher 1: Toán học
INSERT INTO users (id, email, password_hash, full_name, phone, role, status)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'teacher.math@edtech.vn',
    '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa',
    'Thầy Nguyễn Văn Toán',
    '0902000001',
    'TEACHER',
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO teacher_profiles (id, user_id, bio, years_of_experience, languages, supports_online, supports_offline, profile_status, is_visible)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'Thạc sĩ Toán học ĐH Sư Phạm Hà Nội, 10 năm kinh nghiệm luyện thi ĐH và HSG.',
    10,
    ARRAY['Vietnamese', 'English'],
    true,
    false,
    'APPROVED',
    true
) ON CONFLICT (id) DO NOTHING;

-- Teacher 2: Tiếng Anh
INSERT INTO users (id, email, password_hash, full_name, phone, role, status)
VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'teacher.english@edtech.vn',
    '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa',
    'Cô Trần Thị Anh',
    '0902000002',
    'TEACHER',
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO teacher_profiles (id, user_id, bio, years_of_experience, languages, supports_online, supports_offline, profile_status, is_visible)
VALUES (
    'c0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000002',
    'IELTS 8.5, Cử nhân Ngôn ngữ Anh ĐH Ngoại Thương, 6 năm luyện thi IELTS & Giao tiếp.',
    6,
    ARRAY['Vietnamese', 'English'],
    true,
    true,
    'APPROVED',
    true
) ON CONFLICT (id) DO NOTHING;

-- Teacher 3: Vật Lý
INSERT INTO users (id, email, password_hash, full_name, phone, role, status)
VALUES (
    'b0000000-0000-0000-0000-000000000003',
    'teacher.physics@edtech.vn',
    '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa',
    'Thầy Lê Văn Lý',
    '0902000003',
    'TEACHER',
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO teacher_profiles (id, user_id, bio, years_of_experience, languages, supports_online, supports_offline, profile_status, is_visible)
VALUES (
    'c0000000-0000-0000-0000-000000000003',
    'b0000000-0000-0000-0000-000000000003',
    'Cựu giảng viên ĐH Bách Khoa, phương pháp giải nhanh trắc nghiệm Lý THPT.',
    8,
    ARRAY['Vietnamese'],
    true,
    false,
    'APPROVED',
    true
) ON CONFLICT (id) DO NOTHING;

-- 3. SUBJECTS
INSERT INTO subjects (id, code, name, slug, education_level, description, is_active, created_source)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'SUB_MATH', 'Toán Học', 'toan-hoc', 'Cấp 3', 'Môn Toán cấp 2 và cấp 3', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000002', 'SUB_ENG', 'Tiếng Anh', 'tieng-anh', 'Mọi cấp độ', 'Tiếng Anh giao tiếp & IELTS', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000003', 'SUB_PHY', 'Vật Lý', 'vat-ly', 'Cấp 3', 'Môn Vật lý cấp 3 & Luyện thi ĐH', true, 'ADMIN')
ON CONFLICT (id) DO NOTHING;

-- 4. TEACHER SUBJECTS
INSERT INTO teacher_subjects (id, teacher_id, subject_id, level_description, experience_description, is_active)
VALUES
    ('e0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Lớp 10, 11, 12 và Luyện thi THPT QG', '10 năm kinh nghiệm dạy kèm', true),
    ('e0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'IELTS 6.5 - 8.0 & Tiếng Anh giao tiếp', '6 năm kinh nghiệm dạy IELTS', true),
    ('e0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'Lớp 11, 12 và Luyện đề trắc nghiệm', '8 năm giảng dạy', true)
ON CONFLICT (id) DO NOTHING;

-- 5. PRICING PACKAGES
INSERT INTO pricing_packages (id, teacher_id, subject_id, name, description, total_sessions, duration_days, price_vnd, session_duration_minutes, status)
VALUES
    ('f0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Gói Toán Cơ Bản 10 Buổi', 'Khóa học ôn tập kiến thức nền tảng', 10, 90, 2250000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Gói Toán Chuyên Sâu 20 Buổi', 'Khóa học luyện đề và nâng cao', 20, 180, 4200000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'Gói IELTS Speaking 10 Buổi', 'Luyện kỹ năng nói theo chủ đề', 10, 60, 3200000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'Gói Vật Lý Luyện Đề 10 Buổi', 'Luyện giải đề trắc nghiệm nhanh', 10, 90, 1900000, 60, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 6. STUDENTS
INSERT INTO users (id, email, password_hash, full_name, phone, role, status)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'student.an@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Nguyễn Văn An', '0903000001', 'STUDENT', 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000002', 'student.binh@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Trần Văn Bình', '0903000002', 'STUDENT', 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000003', 'student.chi@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Lê Thị Chi', '0903000003', 'STUDENT', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 7. WALLETS & BANK ACCOUNTS
INSERT INTO wallets (id, teacher_id, pending_balance_vnd, available_balance_vnd, reserved_balance_vnd, version)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 1923750, 213750, 0, 1),
    ('20000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 3040000, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 0, 0, 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO teacher_bank_accounts (id, teacher_id, bank_bin, bank_name, account_number_encrypted, account_holder_name, is_verified, is_default)
VALUES
    ('30000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', '970436', 'Vietcombank', 'ENC_ACC_123456789', 'NGUYEN VAN TOAN', true, true),
    ('30000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', '970422', 'MBBank', 'ENC_ACC_987654321', 'TRAN THI ANH', true, true)
ON CONFLICT (id) DO NOTHING;

-- 8. INVOICES
INSERT INTO invoices (
    id, invoice_number, student_id, teacher_id, pricing_package_id,
    amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint, paid_at
) VALUES
    ('50000000-0000-0000-0000-000000000001', 'INV-2026-000001', '10000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 2250000, 'PAID', 100001, '50000000-0000-0000-0000-000000000001', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', now() - interval '5 days'),
    ('50000000-0000-0000-0000-000000000002', 'INV-2026-000002', '10000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000003', 3200000, 'PAID', 100002, '50000000-0000-0000-0000-000000000002', 'ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb', now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;

-- 9. STUDENT PACKAGES
-- Gói 1: Student An mua gói Toán (10 buổi: 8 remaining + 1 reserved + 1 completed + 0 refunded = 10)
INSERT INTO student_packages (
    id, pricing_package_id, student_id, teacher_id, subject_id, invoice_id,
    package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions,
    purchase_price_vnd, commission_rate, starts_at, expires_at, status
) VALUES (
    '40000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000001',
    'Gói Toán Cơ Bản 10 Buổi',
    10, 8, 1, 1, 0,
    2250000, 5.00, now() - interval '5 days', now() + interval '85 days', 'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

-- Gói 2: Student Binh mua gói Tiếng Anh (10 buổi: 10 remaining = 10)
INSERT INTO student_packages (
    id, pricing_package_id, student_id, teacher_id, subject_id, invoice_id,
    package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions,
    purchase_price_vnd, commission_rate, starts_at, expires_at, status
) VALUES (
    '40000000-0000-0000-0000-000000000002',
    'f0000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000002',
    'd0000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000002',
    'Gói IELTS Speaking 10 Buổi',
    10, 10, 0, 0, 0,
    3200000, 5.00, now() - interval '2 days', now() + interval '58 days', 'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

-- 10. BOOKINGS & SESSION REPORTS
-- Booking 1: Đã hoàn tất (Toán)
INSERT INTO bookings (
    id, teacher_id, student_id, subject_id, student_package_id,
    start_time, end_time, delivery_mode, meeting_link, status, is_trial, completed_at, settlement_processed
) VALUES (
    '60000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    now() - interval '3 days',
    now() - interval '3 days' + interval '1 hour',
    'ONLINE',
    'https://meet.edtech.vn/math-session-1',
    'COMPLETED',
    false,
    now() - interval '3 days' + interval '1 hour',
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO session_reports (id, booking_id, content, feedback, follow_up_note, teacher_self_rating, submitted_at)
VALUES (
    '70000000-0000-0000-0000-000000000001',
    '60000000-0000-0000-0000-000000000001',
    'Học sinh tiếp thu tốt bài giảng Khối đa diện, tính toán nhanh.',
    'Làm bài tập 1 đến 5 trang 45 SGK Hình học 12.',
    'Cần luyện thêm về tính thể tích chóp cụt.',
    5,
    now() - interval '3 days' + interval '1 hour'
) ON CONFLICT (id) DO NOTHING;

-- Booking 2: Đang lên lịch (Toán)
INSERT INTO bookings (
    id, teacher_id, student_id, subject_id, student_package_id,
    start_time, end_time, delivery_mode, meeting_link, status, is_trial
) VALUES (
    '60000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    now() + interval '2 days',
    now() + interval '2 days' + interval '1 hour',
    'ONLINE',
    'https://meet.edtech.vn/math-session-2',
    'SCHEDULED',
    false
) ON CONFLICT (id) DO NOTHING;

-- 11. LEDGER ENTRIES (Sổ cái)
-- Ví 1: Package funded 2,137,500 PENDING, Session 1 hoàn tất chuyển 213,750 sang AVAILABLE
-- Ví 2: Package funded 3,040,000 PENDING
INSERT INTO ledger_entries (
    id, wallet_id, entry_type, amount_vnd, balance_bucket, direction,
    reference_type, reference_id, idempotency_key, description, created_at
) VALUES
    ('80000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'PACKAGE_FUNDED', 2137500, 'PENDING', 'CREDIT', 'STUDENT_PACKAGE', '40000000-0000-0000-0000-000000000001', 'PACKAGE:40000000-0000-0000-0000-000000000001', 'Ghi nhận học phí gói Toán', now() - interval '5 days'),
    ('80000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'SESSION_RELEASE_PENDING', 213750, 'PENDING', 'DEBIT', 'SESSION_REPORT', '70000000-0000-0000-0000-000000000001', 'SETTLE:70000000-0000-0000-0000-000000000001:DEBIT', 'Giải phóng học phí buổi học Toán', now() - interval '3 days'),
    ('80000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', 'SESSION_CREDIT_AVAILABLE', 213750, 'AVAILABLE', 'CREDIT', 'SESSION_REPORT', '70000000-0000-0000-0000-000000000001', 'SETTLE:70000000-0000-0000-0000-000000000001:CREDIT', 'Chuyển số dư khả dụng sau buổi học Toán', now() - interval '3 days'),
    ('80000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002', 'PACKAGE_FUNDED', 3040000, 'PENDING', 'CREDIT', 'STUDENT_PACKAGE', '40000000-0000-0000-0000-000000000002', 'PACKAGE:40000000-0000-0000-0000-000000000002', 'Ghi nhận học phí gói Tiếng Anh', now() - interval '2 days')
ON CONFLICT (idempotency_key) DO NOTHING;