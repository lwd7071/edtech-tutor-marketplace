-- =====================================================================
-- EDTECH TUTOR MARKETPLACE - COMPREHENSIVE SEED DATA FOR SUPABASE
-- All accounts password: Password123!
-- BCrypt Hash: $2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. SCHEMA SYNC: BỔ SUNG ĐẦY ĐỦ CÁC CỘT THEO SPRING BOOT ENTITIES (V18 - V22)
-- ---------------------------------------------------------------------
DO $$ 
BEGIN 
    -- 0.1 Cập nhật teacher_stats (V19: BaseEntity)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'teacher_stats' AND column_name = 'id'
    ) THEN
        ALTER TABLE teacher_stats 
        ADD COLUMN id uuid DEFAULT gen_random_uuid(),
        ADD COLUMN created_at timestamptz DEFAULT now() NOT NULL,
        ADD COLUMN updated_at timestamptz DEFAULT now() NOT NULL,
        ADD COLUMN is_deleted boolean DEFAULT false NOT NULL;

        ALTER TABLE teacher_stats DROP CONSTRAINT IF EXISTS teacher_stats_pkey;
        ALTER TABLE teacher_stats ADD PRIMARY KEY (id);
        ALTER TABLE teacher_stats ADD CONSTRAINT uq_teacher_stats_teacher_id UNIQUE (teacher_id);
    END IF;

    -- 0.2 Cập nhật invoices (V21: idempotency_key & request_fingerprint)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'invoices' AND column_name = 'idempotency_key'
    ) THEN
        ALTER TABLE invoices 
        ADD COLUMN idempotency_key uuid,
        ADD COLUMN request_fingerprint varchar(64);
    END IF;

    -- 0.3 Cập nhật notifications (V20: BaseEntity soft delete)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'notifications' AND column_name = 'is_deleted'
    ) THEN
        ALTER TABLE notifications 
        ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now(),
        ADD COLUMN is_deleted boolean NOT NULL DEFAULT false;
    END IF;

    -- 0.4 Cập nhật teacher_profiles languages type (V18: varchar[] -> text[])
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'teacher_profiles' AND column_name = 'languages' AND data_type = 'ARRAY'
    ) THEN
        ALTER TABLE teacher_profiles ALTER COLUMN languages TYPE text[] USING languages::text[];
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 1. ADMIN USER
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, email_verified)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@edtech.vn',
    '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa',
    'Quản Trị Viên Hệ Thống',
    '0901000001',
    'ADMIN',
    'ACTIVE',
    true
) ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. STUDENTS (5 HỌC SINH ĐỂ TEST ĐA DẠNG)
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, email_verified)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'student01@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Nguyễn Văn An', '0903000001', 'STUDENT', 'ACTIVE', true),
    ('10000000-0000-0000-0000-000000000002', 'student02@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Trần Thị Bình', '0903000002', 'STUDENT', 'ACTIVE', true),
    ('10000000-0000-0000-0000-000000000003', 'student03@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Lê Quốc Cường', '0903000003', 'STUDENT', 'ACTIVE', true),
    ('10000000-0000-0000-0000-000000000004', 'student04@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Phạm Minh Dung', '0903000004', 'STUDENT', 'ACTIVE', true),
    ('10000000-0000-0000-0000-000000000005', 'student05@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Hoàng Bảo Em', '0903000005', 'STUDENT', 'ACTIVE', true)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. SUBJECTS (12 MÔN HỌC ĐA DẠNG)
-- ---------------------------------------------------------------------
INSERT INTO subjects (id, code, name, slug, education_level, description, is_active, created_source)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'SUB_MATH', 'Toán Học', 'toan-hoc', 'HIGH_SCHOOL', 'Môn Toán cấp 2, 3 và luyện thi THPT Quốc Gia', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000002', 'SUB_ENG', 'Tiếng Anh (IELTS & Giao tiếp)', 'tieng-anh', 'OTHER', 'Tiếng Anh giao tiếp, TOEIC và luyện thi IELTS 6.5+', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000003', 'SUB_PHY', 'Vật Lý', 'vat-ly', 'HIGH_SCHOOL', 'Vật Lý nâng cao và chuyên đề trắc nghiệm THPT', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000004', 'SUB_CHEM', 'Hóa Học', 'hoa-hoc', 'HIGH_SCHOOL', 'Hóa Học hữu cơ, vô cơ và phương pháp giải nhanh', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000005', 'SUB_BIO', 'Sinh Học', 'sinh-hoc', 'HIGH_SCHOOL', 'Sinh Học đại cương, di truyền học và luyện đề', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000006', 'SUB_LIT', 'Ngữ Văn', 'ngu-van', 'HIGH_SCHOOL', 'Phân tích văn học, rèn luyện kỹ năng viết nghị luận xã hội', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000007', 'SUB_HIST', 'Lịch Sử', 'lich-su', 'HIGH_SCHOOL', 'Lịch Sử Việt Nam và Thế Giới theo sơ đồ tư duy', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000008', 'SUB_PROG', 'Lập Trình (Python/Web)', 'lap-trinh', 'UNIVERSITY', 'Khoa học máy tính cơ bản, lập trình Python & Web Fullstack', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000009', 'SUB_JPN', 'Tiếng Nhật (JLPT N5-N2)', 'tieng-nhat', 'OTHER', 'Tiếng Nhật sơ cấp, trung cấp và luyện thi JLPT', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000010', 'SUB_KOR', 'Tiếng Hàn (TOPIK)', 'tieng-han', 'OTHER', 'Tiếng Hàn giao tiếp và luyện thi chứng chỉ TOPIK', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000011', 'SUB_CHN', 'Tiếng Trung (HSK)', 'tieng-trung', 'OTHER', 'Tiếng Trung phát âm chuẩn Bắc Kinh, luyện thi HSK', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000012', 'SUB_MATH_PRI', 'Toán Tiểu Học & Tư Duy', 'toan-tieu-hoc', 'PRIMARY_SCHOOL', 'Toán tư duy logic và toán tiếng Anh tiểu học', true, 'ADMIN')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 4. 20 TEACHER USERS & PROFILES
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, avatar_url, email_verified)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'teacher01@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Nguyễn Văn Toán', '0902000001', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', true),
    ('b0000000-0000-0000-0000-000000000002', 'teacher02@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Trần Thị Anh', '0902000002', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', true),
    ('b0000000-0000-0000-0000-000000000003', 'teacher03@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Lê Văn Lý', '0902000003', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', true),
    ('b0000000-0000-0000-0000-000000000004', 'teacher04@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Phạm Bích Hóa', '0902000004', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', true),
    ('b0000000-0000-0000-0000-000000000005', 'teacher05@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Vũ Minh Văn', '0902000005', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', true),
    ('b0000000-0000-0000-0000-000000000006', 'teacher06@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Đỗ Thu Sinh', '0902000006', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150', true),
    ('b0000000-0000-0000-0000-000000000007', 'teacher07@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Hoàng Nam Code', '0902000007', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150', true),
    ('b0000000-0000-0000-0000-000000000008', 'teacher08@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Đặng Thùy Dương', '0902000008', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150', true),
    ('b0000000-0000-0000-0000-000000000009', 'teacher09@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Bùi Quang Nhật', '0902000009', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150', true),
    ('b0000000-0000-0000-0000-000000000010', 'teacher10@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Mai Hương Hàn', '0902000010', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=150', true),
    ('b0000000-0000-0000-0000-000000000011', 'teacher11@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Trịnh Trung Hoa', '0902000011', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150', true),
    ('b0000000-0000-0000-0000-000000000012', 'teacher12@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Ngô Mỹ Linh', '0902000012', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', true),
    ('b0000000-0000-0000-0000-000000000013', 'teacher13@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Cao Bá Quát', '0902000013', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150', true),
    ('b0000000-0000-0000-0000-000000000014', 'teacher14@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Phan Khánh Vy', '0902000014', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=150', true),
    ('b0000000-0000-0000-0000-000000000015', 'teacher15@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Đoàn Thanh Tùng', '0902000015', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=150', true),
    ('b0000000-0000-0000-0000-000000000016', 'teacher16@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Lý Kim Chi', '0902000016', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1548142813-c348350df52b?w=150', true),
    ('b0000000-0000-0000-0000-000000000017', 'teacher17@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Huỳnh Minh Tuấn', '0902000017', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1513956589380-bad6acb9b9d4?w=150', true),
    ('b0000000-0000-0000-0000-000000000018', 'teacher18@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Tạ Ánh Nguyệt', '0902000018', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150', true),
    ('b0000000-0000-0000-0000-000000000019', 'teacher19@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Đinh Tiến Dũng', '0902000019', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150', true),
    ('b0000000-0000-0000-0000-000000000020', 'teacher20@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Dương Thanh Trúc', '0902000020', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150', true)
ON CONFLICT (id) DO NOTHING;

-- 20 Teacher Profiles
INSERT INTO teacher_profiles (id, user_id, bio, years_of_experience, languages, supports_online, supports_offline, profile_status, is_visible, verified_badge)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'Thạc sĩ Toán học ĐH Sư Phạm Hà Nội, 10 năm kinh nghiệm luyện thi ĐH và HSG.', 10, ARRAY['Vietnamese', 'English'], true, false, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'IELTS 8.5, Cử nhân Ngôn ngữ Anh ĐH Ngoại Thương, 6 năm luyện thi IELTS & Giao tiếp.', 6, ARRAY['Vietnamese', 'English'], true, true, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'Cựu giảng viên ĐH Bách Khoa, phương pháp giải nhanh trắc nghiệm Lý THPT.', 8, ARRAY['Vietnamese'], true, false, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'Thạc sĩ Hóa học Hữu cơ, chuyên sâu ôn thi chuyên và bồi dưỡng mất gốc.', 7, ARRAY['Vietnamese'], true, true, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', 'Giáo viên trường THPT Chuyên, hướng dẫn viết nghị luận cảm xúc và logic.', 9, ARRAY['Vietnamese'], true, false, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 'Tốt nghiệp ĐH Y Dược, chuyên gia Sinh học ôn thi khối B điểm 9+.', 5, ARRAY['Vietnamese'], true, true, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000007', 'Senior Software Engineer tại Big Tech, dạy Python, C++ và tư duy thuật toán.', 6, ARRAY['Vietnamese', 'English'], true, false, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000008', 'b0000000-0000-0000-0000-000000000008', 'Cử nhân Sư phạm Toán Tiểu học, phương pháp dạy qua trò chơi trực quan.', 4, ARRAY['Vietnamese'], true, true, 'APPROVED', true, false),
    ('c0000000-0000-0000-0000-000000000009', 'b0000000-0000-0000-0000-000000000009', 'JLPT N1, cựu du học sinh ĐH Tokyo, 5 năm giảng dạy tiếng Nhật văn phòng & JLPT.', 5, ARRAY['Vietnamese', 'Japanese'], true, true, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000010', 'TOPIK 6, chuyên gia phát âm chuẩn Seoul và giao tiếp ứng dụng hàng ngày.', 4, ARRAY['Vietnamese', 'Korean'], true, false, 'APPROVED', true, false),
    ('c0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000011', 'HSK 6 cao cấp, tu nghiệp tại ĐH Bắc Kinh, giảng dạy khẩu ngữ & HSK.', 7, ARRAY['Vietnamese', 'Chinese'], true, true, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000012', 'IELTS 8.0, chuyên gia trị mất gốc tiếng Anh cho học sinh cấp 2 & 3.', 5, ARRAY['Vietnamese', 'English'], true, true, 'APPROVED', true, false),
    ('c0000000-0000-0000-0000-000000000013', 'b0000000-0000-0000-0000-000000000013', 'Thủ khoa Sư phạm Lịch Sử, phương pháp ghi nhớ lịch sử qua sơ đồ tư duy Mindmap.', 8, ARRAY['Vietnamese'], true, false, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000014', 'IELTS 8.5, cựu sinh viên RMIT, chuyên luyện kỹ năng Writing & Speaking.', 4, ARRAY['Vietnamese', 'English'], true, false, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000015', 'b0000000-0000-0000-0000-000000000015', 'Thạc sĩ Toán Tin ĐH KHTN, chuyên sâu Toán Hình không gian và Giải tích 12.', 6, ARRAY['Vietnamese'], true, true, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000016', 'b0000000-0000-0000-0000-000000000016', 'Cử nhân Hóa Sư Phạm, chuyên bồi dưỡng học sinh thi vào lớp 10 chuyên Hóa.', 5, ARRAY['Vietnamese'], true, true, 'APPROVED', true, false),
    ('c0000000-0000-0000-0000-000000000017', 'b0000000-0000-0000-0000-000000000017', 'Kỹ sư Cơ điện tử Bách Khoa, yêu thích giảng dạy Vật Lý thực nghiệm và mô phỏng.', 3, ARRAY['Vietnamese', 'English'], true, false, 'APPROVED', true, false),
    ('c0000000-0000-0000-0000-000000000018', 'b0000000-0000-0000-0000-000000000018', 'Thạc sĩ Văn học Việt Nam, rèn luyện cách viết văn đạt điểm 8.5+ trong kỳ thi quốc gia.', 11, ARRAY['Vietnamese'], true, false, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000019', 'b0000000-0000-0000-0000-000000000019', 'Fullstack Web Developer, giảng dạy Javascript/TypeScript, ReactJS & Node.js từ số 0.', 5, ARRAY['Vietnamese', 'English'], true, true, 'APPROVED', true, true),
    ('c0000000-0000-0000-0000-000000000020', 'b0000000-0000-0000-0000-000000000020', 'Giáo viên Tiểu học chuẩn Quốc gia, dạy Toán tư duy Singapore & rèn chữ đẹp.', 8, ARRAY['Vietnamese'], true, true, 'APPROVED', true, true)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 5. TEACHER SUBJECTS (GÁN MÔN HỌC CHO 20 GIA SƯ)
-- ---------------------------------------------------------------------
INSERT INTO teacher_subjects (id, teacher_id, subject_id, level_description, experience_description, is_active)
VALUES
    ('e0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Lớp 10, 11, 12 và Luyện thi THPT QG', '10 năm chuyên luyện đề nâng cao', true),
    ('e0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'IELTS 6.5 - 8.5 & Tiếng Anh giao tiếp', '6 năm luyện thi IELTS', true),
    ('e0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'Lớp 11, 12 và Luyện đề trắc nghiệm nhanh', '8 năm giảng dạy Vật Lý', true),
    ('e0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', 'Hóa 10, 11, 12 và thi chuyên', '7 năm kèm học sinh mất gốc', true),
    ('e0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000006', 'Văn cấp 3 và thi THPT Quốc Gia', '9 năm chấm thi HSG Văn', true),
    ('e0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000005', 'Sinh học ôn thi Y Dược', '5 năm luyện đề khối B', true),
    ('e0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000008', 'Python cho người mới & Thuật toán', '6 năm kinh nghiệm lập trình phần mềm', true),
    ('e0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000008', 'd0000000-0000-0000-0000-000000000012', 'Toán lớp 1 đến 5 và tư duy logic', '4 năm kèm trẻ em tiểu học', true),
    ('e0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000009', 'd0000000-0000-0000-0000-000000000009', 'Tiếng Nhật JLPT N5 đến N2', '5 năm luyện thi năng lực Nhật ngữ', true),
    ('e0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000010', 'd0000000-0000-0000-0000-000000000010', 'Tiếng Hàn giao tiếp & TOPIK I-II', '4 năm dạy kèm du học sinh', true),
    ('e0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000011', 'd0000000-0000-0000-0000-000000000011', 'Tiếng Trung HSK 1-5', '7 năm đào tạo tiếng Trung thương mại', true),
    ('e0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000012', 'd0000000-0000-0000-0000-000000000002', 'Tiếng Anh cấp 2 & Phổ thông', '5 năm lấy lại gốc Tiếng Anh', true),
    ('e0000000-0000-0000-0000-000000000013', 'c0000000-0000-0000-0000-000000000013', 'd0000000-0000-0000-0000-000000000007', 'Lịch Sử THPT & Ôn thi ĐH', '8 năm bồi dưỡng HSG Sử', true),
    ('e0000000-0000-0000-0000-000000000014', 'c0000000-0000-0000-0000-000000000014', 'd0000000-0000-0000-0000-000000000002', 'IELTS Speaking & Writing chuyên sâu', '4 năm cựu du học sinh', true),
    ('e0000000-0000-0000-0000-000000000015', 'c0000000-0000-0000-0000-000000000015', 'd0000000-0000-0000-0000-000000000001', 'Hình học không gian & Tọa độ Oxyz', '6 năm luyện thi THPT QG', true),
    ('e0000000-0000-0000-0000-000000000016', 'c0000000-0000-0000-0000-000000000016', 'd0000000-0000-0000-0000-000000000004', 'Hóa học cấp 2 & Thi vào 10 chuyên', '5 năm ôn luyện trường chuyên', true),
    ('e0000000-0000-0000-0000-000000000017', 'c0000000-0000-0000-0000-000000000017', 'd0000000-0000-0000-0000-000000000003', 'Vật Lý lớp 10, 11 cơ bản & nâng cao', '3 năm dạy kèm tại nhà', true),
    ('e0000000-0000-0000-0000-000000000018', 'c0000000-0000-0000-0000-000000000018', 'd0000000-0000-0000-0000-000000000006', 'Văn THCS & Luyện thi vào lớp 10', '11 năm giảng dạy văn học', true),
    ('e0000000-0000-0000-0000-000000000019', 'c0000000-0000-0000-0000-000000000019', 'd0000000-0000-0000-0000-000000000008', 'Lập trình Web Frontend & Backend', '5 năm hướng dẫn đồ án CNTT', true),
    ('e0000000-0000-0000-0000-000000000020', 'c0000000-0000-0000-0000-000000000020', 'd0000000-0000-0000-0000-000000000012', 'Toán tư duy Singapore cho bé', '8 năm sư phạm mầm non & tiểu học', true)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 6. PRICING PACKAGES (CÁC GÓI HỌC PHÍ)
-- ---------------------------------------------------------------------
INSERT INTO pricing_packages (id, teacher_id, subject_id, name, description, total_sessions, duration_days, price_vnd, session_duration_minutes, status)
VALUES
    ('f0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Toán Nền Tảng 10 Buổi', 'Lấy lại kiến thức cơ bản đại số & hình học', 10, 60, 2000000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Toán Luyện Thi THPT 20 Buổi', 'Chuyên đề 8+ và luyện đề thi thử', 20, 120, 3800000, 90, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'IELTS Speaking 1-1 (10 Buổi)', 'Sửa phát âm và luyện phản xạ câu hỏi Part 1-3', 10, 45, 3000000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'Vật Lý Cấp Tốc 10 Buổi', 'Công thức giải nhanh trắc nghiệm điện xoay chiều và sóng cơ', 10, 60, 2200000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', 'Hóa Học Chuyên Sâu 15 Buổi', 'Phân loại bài tập este, lipit và peptit nâng cao', 15, 90, 3150000, 75, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000006', 'Luyện Viết Văn Nghị Luận 10 Buổi', 'Rèn luyện mở bài ấn tượng và phân tích chiều sâu tác phẩm', 10, 60, 2500000, 90, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000008', 'Python Cơ Bản & Thuật Toán 12 Buổi', 'Từ cú pháp biến, hàm đến thuật toán đệ quy & quy hoạch động', 12, 60, 3600000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000009', 'd0000000-0000-0000-0000-000000000009', 'Tiếng Nhật Sơ Cấp N5 (20 Buổi)', 'Học bảng chữ cái Hiragana/Katakana và ngữ pháp cơ bản', 20, 90, 4000000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000010', 'd0000000-0000-0000-0000-000000000010', 'Tiếng Hàn Giao Tiếp 10 Buổi', 'Giao tiếp hàng ngày, phản xạ du lịch & mua sắm', 10, 45, 2400000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000011', 'd0000000-0000-0000-0000-000000000011', 'Tiếng Trung HSK 3 Cấp Tốc (15 Buổi)', '15 buổi chuẩn hóa ngữ pháp và từ vựng HSK 3', 15, 75, 3300000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000019', 'd0000000-0000-0000-0000-000000000008', 'Khóa Web Fullstack Thực Chiến (24 Buổi)', 'Xây dựng ứng dụng hoàn chỉnh với React & Spring Boot', 24, 120, 6000000, 90, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000020', 'd0000000-0000-0000-0000-000000000012', 'Toán Tư Duy Tiểu Học (10 Buổi)', 'Phương pháp kích thích tư duy logic trực quan cho bé', 10, 60, 2000000, 60, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 7. TEACHER STATS (THỐNG KÊ XẾP HẠNG CHO 20 GIA SƯ ĐỂ HIỂN THỊ RANKING)
-- ---------------------------------------------------------------------
INSERT INTO teacher_stats (
    id, teacher_id, average_rating, bayesian_rating, review_count, completed_session_count,
    completion_rate, trial_session_count, trial_conversion_rate, global_rank, calculated_at
) VALUES
    ('90000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 4.95, 4.92, 128, 450, 0.9850, 45, 0.8500, 1, now()),
    ('90000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 4.92, 4.89, 96, 380, 0.9780, 38, 0.8200, 2, now()),
    ('90000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 4.88, 4.85, 84, 320, 0.9650, 30, 0.8000, 3, now()),
    ('90000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 4.85, 4.81, 72, 290, 0.9600, 28, 0.7850, 4, now()),
    ('90000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 4.82, 4.79, 65, 260, 0.9500, 25, 0.7600, 5, now()),
    ('90000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006', 4.80, 4.76, 58, 240, 0.9450, 22, 0.7500, 6, now()),
    ('90000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 4.78, 4.74, 52, 210, 0.9400, 20, 0.7400, 7, now()),
    ('90000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000008', 4.75, 4.71, 48, 195, 0.9350, 19, 0.7300, 8, now()),
    ('90000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000009', 4.72, 4.68, 45, 180, 0.9300, 18, 0.7200, 9, now()),
    ('90000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000010', 4.70, 4.65, 40, 160, 0.9250, 16, 0.7100, 10, now()),
    ('90000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000011', 4.68, 4.63, 38, 150, 0.9200, 15, 0.7000, 11, now()),
    ('90000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000012', 4.65, 4.60, 35, 140, 0.9150, 14, 0.6900, 12, now()),
    ('90000000-0000-0000-0000-000000000013', 'c0000000-0000-0000-0000-000000000013', 4.62, 4.58, 30, 130, 0.9100, 13, 0.6800, 13, now()),
    ('90000000-0000-0000-0000-000000000014', 'c0000000-0000-0000-0000-000000000014', 4.60, 4.55, 28, 120, 0.9050, 12, 0.6700, 14, now()),
    ('90000000-0000-0000-0000-000000000015', 'c0000000-0000-0000-0000-000000000015', 4.58, 4.52, 25, 110, 0.9000, 11, 0.6600, 15, now()),
    ('90000000-0000-0000-0000-000000000016', 'c0000000-0000-0000-0000-000000000016', 4.55, 4.50, 22, 100, 0.8950, 10, 0.6500, 16, now()),
    ('90000000-0000-0000-0000-000000000017', 'c0000000-0000-0000-0000-000000000017', 4.52, 4.47, 20, 90, 0.8900, 9, 0.6400, 17, now()),
    ('90000000-0000-0000-0000-000000000018', 'c0000000-0000-0000-0000-000000000018', 4.50, 4.45, 18, 80, 0.8850, 8, 0.6300, 18, now()),
    ('90000000-0000-0000-0000-000000000019', 'c0000000-0000-0000-0000-000000000019', 4.48, 4.42, 15, 70, 0.8800, 7, 0.6200, 19, now()),
    ('90000000-0000-0000-0000-000000000020', 'c0000000-0000-0000-0000-000000000020', 4.45, 4.40, 12, 60, 0.8750, 6, 0.6100, 20, now())
ON CONFLICT (teacher_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 8. WALLETS CHO TẤT CẢ 20 GIA SƯ
-- ---------------------------------------------------------------------
INSERT INTO wallets (id, teacher_id, pending_balance_vnd, available_balance_vnd, reserved_balance_vnd, version)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 2500000, 15000000, 0, 1),
    ('20000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 3200000, 12500000, 0, 1),
    ('20000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 1800000, 8400000, 0, 1),
    ('20000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 1500000, 7200000, 0, 1),
    ('20000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 1200000, 6500000, 0, 1),
    ('20000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006', 900000, 5100000, 0, 1),
    ('20000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 3000000, 9800000, 0, 1),
    ('20000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000008', 800000, 4200000, 0, 1),
    ('20000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000009', 2000000, 7600000, 0, 1),
    ('20000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000010', 1100000, 3900000, 0, 1),
    ('20000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000011', 1400000, 5500000, 0, 1),
    ('20000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000012', 700000, 3200000, 0, 1),
    ('20000000-0000-0000-0000-000000000013', 'c0000000-0000-0000-0000-000000000013', 1000000, 4100000, 0, 1),
    ('20000000-0000-0000-0000-000000000014', 'c0000000-0000-0000-0000-000000000014', 1600000, 6200000, 0, 1),
    ('20000000-0000-0000-0000-000000000015', 'c0000000-0000-0000-0000-000000000015', 1300000, 4800000, 0, 1),
    ('20000000-0000-0000-0000-000000000016', 'c0000000-0000-0000-0000-000000000016', 600000, 2700000, 0, 1),
    ('20000000-0000-0000-0000-000000000017', 'c0000000-0000-0000-0000-000000000017', 500000, 2100000, 0, 1),
    ('20000000-0000-0000-0000-000000000018', 'c0000000-0000-0000-0000-000000000018', 1900000, 8300000, 0, 1),
    ('20000000-0000-0000-0000-000000000019', 'c0000000-0000-0000-0000-000000000019', 2200000, 9100000, 0, 1),
    ('20000000-0000-0000-0000-000000000020', 'c0000000-0000-0000-0000-000000000020', 800000, 3500000, 0, 1)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 9. TEACHER BANK ACCOUNTS
-- ---------------------------------------------------------------------
INSERT INTO teacher_bank_accounts (id, teacher_id, bank_bin, bank_name, account_number_encrypted, account_holder_name, is_verified, is_default)
VALUES
    ('30000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', '970436', 'Vietcombank', 'ENC_ACC_1012345678', 'NGUYEN VAN TOAN', true, true),
    ('30000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', '970422', 'MBBank', 'ENC_ACC_0902000002', 'TRAN THI ANH', true, true),
    ('30000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', '970415', 'Vietinbank', 'ENC_ACC_1098765432', 'LE VAN LY', true, true),
    ('30000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', '970407', 'Techcombank', 'ENC_ACC_1903456789', 'HOANG NAM', true, true)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 10. INVOICES & STUDENT PACKAGES
-- ---------------------------------------------------------------------
INSERT INTO invoices (
    id, invoice_number, student_id, teacher_id, pricing_package_id,
    amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint, paid_at
) VALUES
    ('50000000-0000-0000-0000-000000000001', 'INV-2026-000001', '10000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 2000000, 'PAID', 100001, '50000000-0000-0000-0000-000000000001', 'fingerprint_inv1', now() - interval '10 days'),
    ('50000000-0000-0000-0000-000000000002', 'INV-2026-000002', '10000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000003', 3000000, 'PAID', 100002, '50000000-0000-0000-0000-000000000002', 'fingerprint_inv2', now() - interval '8 days'),
    ('50000000-0000-0000-0000-000000000003', 'INV-2026-000003', '10000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000007', 3600000, 'PAID', 100003, '50000000-0000-0000-0000-000000000003', 'fingerprint_inv3', now() - interval '5 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO student_packages (
    id, pricing_package_id, student_id, teacher_id, subject_id, invoice_id,
    package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions,
    purchase_price_vnd, commission_rate, starts_at, expires_at, status
) VALUES
    ('40000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Toán Nền Tảng 10 Buổi', 10, 8, 1, 1, 0, 2000000, 5.00, now() - interval '10 days', now() + interval '50 days', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002', 'IELTS Speaking 1-1 (10 Buổi)', 10, 9, 0, 1, 0, 3000000, 5.00, now() - interval '8 days', now() + interval '52 days', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000008', '50000000-0000-0000-0000-000000000003', 'Python Cơ Bản & Thuật Toán 12 Buổi', 12, 12, 0, 0, 0, 3600000, 5.00, now() - interval '5 days', now() + interval '55 days', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 11. BOOKINGS & SESSIONS
-- ---------------------------------------------------------------------
INSERT INTO bookings (
    id, teacher_id, student_id, subject_id, student_package_id,
    start_time, end_time, delivery_mode, meeting_link, status, is_trial, completed_at, settlement_processed
) VALUES
    ('60000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', now() - interval '3 days', now() - interval '3 days' + interval '1 hour', 'ONLINE', 'https://meet.google.com/abc-defg-hij', 'COMPLETED', false, now() - interval '3 days' + interval '1 hour', true),
    ('60000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', now() + interval '1 day', now() + interval '1 day' + interval '1 hour', 'ONLINE', 'https://meet.google.com/xyz-uvwx-rst', 'SCHEDULED', false, null, false),
    ('60000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', now() - interval '2 days', now() - interval '2 days' + interval '1 hour', 'ONLINE', 'https://meet.google.com/eng-room-002', 'COMPLETED', false, now() - interval '2 days' + interval '1 hour', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO session_reports (id, booking_id, content, feedback, follow_up_note, teacher_self_rating, submitted_at)
VALUES
    ('70000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'Buổi 1: Ôn tập Hàm số lũy thừa và logarit. Học sinh nắm vững công thức.', 'Cần làm thêm bài tập trắc nghiệm chương 2.', 'Theo dõi tiến độ buổi 2.', 5, now() - interval '3 days' + interval '1 hour'),
    ('70000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000003', 'Buổi 1: Phát âm nguyên âm đôi và luyện ngữ điệu câu hỏi IELTS Speaking Part 1.', 'Phát âm rõ ràng, tự tin hơn.', 'Chuẩn bị bài nói về chủ đề Work/Study.', 5, now() - interval '2 days' + interval '1 hour')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 12. REVIEWS (ĐÁNH GIÁ CỦA HỌC SINH)
-- ---------------------------------------------------------------------
INSERT INTO reviews (id, booking_id, student_id, teacher_id, rating, comment, is_visible, created_at)
VALUES
    ('a1000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 5, 'Thầy giảng cực kỳ dễ hiểu, tận tình chỉ dẫn từng bước làm bài!', true, now() - interval '3 days'),
    ('a1000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 5, 'Cô phát âm rất chuẩn bản xứ, buổi học vui vẻ không bị áp lực.', true, now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 13. TEACHER AVAILABILITIES (LỊCH RẢNH MẪU ĐỂ TEST ĐẶT LỊCH)
-- ---------------------------------------------------------------------
INSERT INTO teacher_availabilities (id, teacher_id, day_of_week, start_time, end_time, is_active)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'MONDAY', '18:00:00', '21:00:00', true),
    ('b1000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'WEDNESDAY', '18:00:00', '21:00:00', true),
    ('b1000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'FRIDAY', '18:00:00', '21:00:00', true),
    ('b1000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000002', 'TUESDAY', '19:00:00', '22:00:00', true),
    ('b1000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', 'THURSDAY', '19:00:00', '22:00:00', true),
    ('b1000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002', 'SATURDAY', '14:00:00', '18:00:00', true),
    ('b1000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'SUNDAY', '08:00:00', '12:00:00', true)
ON CONFLICT (id) DO NOTHING;
