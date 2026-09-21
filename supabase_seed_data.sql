-- =====================================================================
-- EDTECH TUTOR MARKETPLACE - TOÀN BỘ BỘ DỮ LIỆU SEED MỚI CHO SUPABASE (SCHEMA V41)
-- Tất cả tài khoản dùng mật khẩu chung: Password123!
-- BCrypt Hash: $2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa
-- =====================================================================

-- ---------------------------------------------------------------------
-- PHẦN 1: DỌN DẸP DỮ LIỆU CŨ (CLEANUP CÁC BẢNG DỮ LIỆU NGHIỆP VỤ)
-- Lưu ý: Giữ nguyên lịch sử flyway_schema_history và danh mục provinces, wards
-- ---------------------------------------------------------------------
TRUNCATE TABLE
    attachments,
    messages,
    conversations,
    submissions,
    assignments,
    notifications,
    audit_logs,
    email_outbox,
    booking_settlements,
    platform_ledger_entries,
    session_reports,
    reviews,
    bookings,
    trial_requests,
    student_packages,
    payment_transactions,
    invoices,
    package_extension_requests,
    refund_requests,
    payout_requests,
    finance_command_receipts,
    ledger_entries,
    teacher_bank_accounts,
    wallets,
    pricing_packages,
    teacher_subjects,
    subject_proposals,
    teacher_availabilities,
    teacher_documents,
    teacher_credentials,
    teacher_stats,
    teacher_profiles,
    refresh_tokens,
    users,
    subjects,
    platform_settings
CASCADE;

-- ---------------------------------------------------------------------
-- PHẦN 2: CẤU HÌNH HỆ THỐNG (PLATFORM SETTINGS)
-- ---------------------------------------------------------------------
INSERT INTO platform_settings (
    id, commission_rate, bayesian_minimum_reviews, booking_reminder_hours, booking_expiration_hours, is_singleton
) VALUES (
    '00000000-0000-0000-0000-000000000001', 10.00, 10, 11, 12, true
);

-- ---------------------------------------------------------------------
-- PHẦN 3: TÀI KHOẢN QUẢN TRỊ VIÊN (ADMIN USER)
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, email_verified, notify_parent)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@edtech.vn',
    '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa',
    'Quản Trị Viên Hệ Thống',
    '0901000001',
    'ADMIN',
    'ACTIVE',
    true,
    false
);

-- ---------------------------------------------------------------------
-- PHẦN 4: HỌC VIÊN (6 STUDENTS)
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, email_verified, notify_parent)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'student01@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Nguyễn Văn An', '0903000001', 'STUDENT', 'ACTIVE', true, true),
    ('10000000-0000-0000-0000-000000000002', 'student02@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Trần Thị Bình', '0903000002', 'STUDENT', 'ACTIVE', true, false),
    ('10000000-0000-0000-0000-000000000003', 'student03@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Lê Quốc Cường', '0903000003', 'STUDENT', 'ACTIVE', true, true),
    ('10000000-0000-0000-0000-000000000004', 'student04@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Phạm Minh Dung', '0903000004', 'STUDENT', 'ACTIVE', true, false),
    ('10000000-0000-0000-0000-000000000005', 'student05@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Hoàng Bảo Em', '0903000005', 'STUDENT', 'ACTIVE', true, false),
    ('10000000-0000-0000-0000-000000000006', 'student06@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Đỗ Thảo Vy', '0903000006', 'STUDENT', 'ACTIVE', true, true);

-- ---------------------------------------------------------------------
-- PHẦN 5: DANH MỤC MÔN HỌC (12 SUBJECTS)
-- ---------------------------------------------------------------------
INSERT INTO subjects (id, code, name, slug, education_level, description, is_active, created_source)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'SUB_MATH', 'Toán Học', 'toan-hoc', 'HIGH_SCHOOL', 'Môn Toán cấp 2, 3 và ôn thi THPT Quốc Gia môn Toán đạt 8.5+', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000002', 'SUB_ENG', 'Tiếng Anh (IELTS & Giao tiếp)', 'tieng-anh', 'OTHER', 'Tiếng Anh giao tiếp phản xạ, TOEIC và luyện thi IELTS chuyên sâu 6.5 - 8.5', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000003', 'SUB_PHY', 'Vật Lý', 'vat-ly', 'HIGH_SCHOOL', 'Vật Lý nâng cao, phương pháp giải nhanh trắc nghiệm điện xoay chiều & sóng cơ', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000004', 'SUB_CHEM', 'Hóa Học', 'hoa-hoc', 'HIGH_SCHOOL', 'Hóa Học hữu cơ, vô cơ, bồi dưỡng học sinh mất gốc và luyện thi trường chuyên', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000005', 'SUB_BIO', 'Sinh Học', 'sinh-hoc', 'HIGH_SCHOOL', 'Sinh Học đại cương, di truyền phân tử và luyện đề thi khối B Y Dược', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000006', 'SUB_LIT', 'Ngữ Văn', 'ngu-van', 'HIGH_SCHOOL', 'Phân tích văn học, rèn luyện kỹ năng viết bài nghị luận xã hội giàu cảm xúc và tư duy logic', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000007', 'SUB_HIST', 'Lịch Sử', 'lich-su', 'HIGH_SCHOOL', 'Lịch Sử Việt Nam và Thế Giới hiện đại theo phương pháp sơ đồ tư duy Mindmap', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000008', 'SUB_PROG', 'Lập Trình & Khoa Học Máy Tính', 'lap-trinh', 'UNIVERSITY', 'Lập trình Python, C++, thuật toán cấu trúc dữ liệu và phát triển ứng dụng Web Fullstack', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000009', 'SUB_JPN', 'Tiếng Nhật (JLPT N5-N2)', 'tieng-nhat', 'OTHER', 'Tiếng Nhật giao tiếp chuẩn âm Tokyo, kaiwa và luyện thi chứng chỉ năng lực JLPT N5 đến N2', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000010', 'SUB_KOR', 'Tiếng Hàn (TOPIK)', 'tieng-han', 'OTHER', 'Tiếng Hàn giao tiếp thực tế đời sống, phát âm chuẩn Seoul và luyện thi TOPIK I-II', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000011', 'SUB_CHN', 'Tiếng Trung (HSK)', 'tieng-trung', 'OTHER', 'Tiếng Trung phát âm chuẩn Bắc Kinh, khẩu ngữ thương mại và luyện thi HSK 1-6', true, 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000012', 'SUB_MATH_PRI', 'Toán Tiểu Học & Tư Duy Logic', 'toan-tieu-hoc', 'PRIMARY_SCHOOL', 'Toán tư duy Singapore, hình thành phản xạ tính nhẩm và rèn luyện logic cho trẻ tiểu học', true, 'ADMIN');

-- ---------------------------------------------------------------------
-- PHẦN 6: 20 GIA SƯ (TEACHER USERS & PROFILES)
-- Đã liên kết tỉnh/thành & phường/xã chuẩn theo danh mục hành chính V38/V39
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, avatar_url, email_verified, notify_parent)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'teacher01@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Nguyễn Văn Toán', '0902000001', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000002', 'teacher02@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Trần Thị Mai Anh', '0902000002', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000003', 'teacher03@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Lê Văn Quang', '0902000003', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000004', 'teacher04@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Phạm Bích Hóa', '0902000004', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000005', 'teacher05@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Vũ Minh Văn', '0902000005', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000006', 'teacher06@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Đỗ Thu Sinh', '0902000006', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000007', 'teacher07@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Hoàng Nam Tech', '0902000007', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000008', 'teacher08@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Đặng Thùy Dương', '0902000008', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000009', 'teacher09@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Bùi Quang Nhật', '0902000009', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000010', 'teacher10@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Mai Hương Ly', '0902000010', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000011', 'teacher11@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Trịnh Quốc Hoa', '0902000011', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000012', 'teacher12@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Ngô Mỹ Linh', '0902000012', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000013', 'teacher13@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Cao Bá Quát', '0902000013', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000014', 'teacher14@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Phan Khánh Vy', '0902000014', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000015', 'teacher15@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Đoàn Thanh Tùng', '0902000015', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000016', 'teacher16@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Lý Kim Chi', '0902000016', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1548142813-c348350df52b?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000017', 'teacher17@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Huỳnh Minh Tuấn', '0902000017', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1513956589380-bad6acb9b9d4?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000018', 'teacher18@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Tạ Ánh Nguyệt', '0902000018', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000019', 'teacher19@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Thầy Đinh Tiến Dũng', '0902000019', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=200', true, false),
    ('b0000000-0000-0000-0000-000000000020', 'teacher20@edtech.vn', '$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa', 'Cô Dương Thanh Trúc', '0902000020', 'TEACHER', 'ACTIVE', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=200', true, false);

-- Hồ sơ gia sư (teacher_profiles)
INSERT INTO teacher_profiles (
    id, user_id, bio, years_of_experience, languages, supports_online, supports_offline,
    profile_status, is_visible, verified_badge, province_code, ward_code
) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'Thạc sĩ Toán học ĐH Sư Phạm Hà Nội, 10 năm kinh nghiệm luyện thi ĐH và bồi dưỡng học sinh giỏi. Phương pháp dạy tư duy mạch lạc, dễ nhớ.', 10, ARRAY['Vietnamese', 'English'], true, false, 'APPROVED', true, true, '01', '00166'),
    ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'IELTS 8.5 Overall (Speaking 8.5), Cử nhân Ngôn ngữ Anh ĐH Ngoại Thương. 6 năm chuyên luyện kỹ năng Speaking & Writing cấp tốc.', 6, ARRAY['Vietnamese', 'English'], true, true, 'APPROVED', true, true, '79', '27073'),
    ('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'Cựu giảng viên Vật Lý ĐH Bách Khoa, chuyên gia tổng hợp bí quyết giải nhanh trắc nghiệm Lý 12 và chuyên đề thi ĐH điểm 9+.', 8, ARRAY['Vietnamese'], true, false, 'APPROVED', true, true, '01', '00235'),
    ('c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'Thạc sĩ Hóa học Hữu cơ ĐH KHTN, chuyên sâu lấy lại gốc Hóa cấp tốc cho học sinh lớp 11, 12 và bồi dưỡng đội tuyển thi chuyên.', 7, ARRAY['Vietnamese'], true, true, 'APPROVED', true, true, '01', '00367'),
    ('c0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', 'Giáo viên trường THPT Chuyên, hướng dẫn viết nghị luận xã hội sâu sắc, cảm xúc và kỹ năng phân tích tác phẩm văn học đạt 8.5+.', 9, ARRAY['Vietnamese'], true, false, 'APPROVED', true, true, '01', '00166'),
    ('c0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 'Bác sĩ Đa khoa tốt nghiệp ĐH Y Dược TP.HCM, chuyên gia ôn thi Sinh học khối B với phương pháp sơ đồ phân tử hóa trực quan sinh động.', 5, ARRAY['Vietnamese'], true, true, 'APPROVED', true, true, '79', '27343'),
    ('c0000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000007', 'Senior Software Engineer tại tập đoàn công nghệ lớn, dạy lập trình Python, tư duy thuật toán, cấu trúc dữ liệu cho học sinh và sinh viên.', 6, ARRAY['Vietnamese', 'English'], true, false, 'APPROVED', true, true, '01', '00070'),
    ('c0000000-0000-0000-0000-000000000008', 'b0000000-0000-0000-0000-000000000008', 'Cử nhân Sư phạm Giáo dục Tiểu học, phương pháp dạy Toán tư duy Singapore trực quan qua trò chơi, kích thích niềm đam mê học tập của trẻ.', 4, ARRAY['Vietnamese'], true, true, 'APPROVED', true, false, '79', '27154'),
    ('c0000000-0000-0000-0000-000000000009', 'b0000000-0000-0000-0000-000000000009', 'JLPT N1, cựu du học sinh ĐH Tokyo, 5 năm kinh nghiệm giảng dạy tiếng Nhật giao tiếp thương mại và luyện thi JLPT N5 đến N2 bao đỗ.', 5, ARRAY['Vietnamese', 'Japanese'], true, true, 'APPROVED', true, true, '48', '20434'),
    ('c0000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000010', 'TOPIK cấp 6, cựu sinh viên Đại học Quốc gia Seoul (SNU), chuyên gia phát âm chuẩn giọng Seoul và giao tiếp văn phòng hàng ngày.', 4, ARRAY['Vietnamese', 'Korean'], true, false, 'APPROVED', true, false, '01', '00256'),
    ('c0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000011', 'HSK 6 cao cấp, tu nghiệp thạc sĩ tại ĐH Ngôn ngữ Bắc Kinh, chuyên luyện khẩu ngữ phản xạ tự nhiên và luyện đề thi HSK cấp tốc.', 7, ARRAY['Vietnamese', 'Chinese'], true, true, 'APPROVED', true, true, '79', '27475'),
    ('c0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000012', 'IELTS 8.0, giảng viên tiếng Anh đại học, chuyên gia trị mất gốc ngữ pháp và xây dựng nền tảng tiếng Anh toàn diện cho học sinh cấp 2 & 3.', 5, ARRAY['Vietnamese', 'English'], true, true, 'APPROVED', true, false, '01', '00025'),
    ('c0000000-0000-0000-0000-000000000013', 'b0000000-0000-0000-0000-000000000013', 'Thủ khoa Sư phạm Lịch Sử ĐH Sư Phạm Hà Nội, phương pháp ghi nhớ mốc thời gian và sự kiện lịch sử qua sơ đồ tư duy Mindmap logic.', 8, ARRAY['Vietnamese'], true, false, 'APPROVED', true, true, '01', '00103'),
    ('c0000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000014', 'IELTS 8.5 Overall, cựu sinh viên RMIT, chuyên luyện phát âm chuẩn IPA, tăng phản xạ giao tiếp tự nhiên và phản biện trong IELTS Speaking.', 4, ARRAY['Vietnamese', 'English'], true, false, 'APPROVED', true, true, '79', '27094'),
    ('c0000000-0000-0000-0000-000000000015', 'b0000000-0000-0000-0000-000000000015', 'Thạc sĩ Toán Tin ĐH Bách Khoa, chuyên sâu Hình học không gian, Tọa độ Oxyz và Giải tích 12 luyện thi tốt nghiệp THPT Quốc Gia.', 6, ARRAY['Vietnamese'], true, true, 'APPROVED', true, true, '01', '09556'),
    ('c0000000-0000-0000-0000-000000000016', 'b0000000-0000-0000-0000-000000000016', 'Cử nhân Hóa Sư Phạm loại Giỏi, chuyên bồi dưỡng học sinh thi vào lớp 10 chuyên Hóa và ôn thi học sinh giỏi các cấp.', 5, ARRAY['Vietnamese'], true, true, 'APPROVED', true, false, '01', '00367'),
    ('c0000000-0000-0000-0000-000000000017', 'b0000000-0000-0000-0000-000000000017', 'Kỹ sư Cơ điện tử Bách Khoa TP.HCM, đam mê giảng dạy Vật Lý thực nghiệm trực quan, giải thích hiện tượng đời sống gắn liền công thức.', 3, ARRAY['Vietnamese', 'English'], true, false, 'APPROVED', true, false, '79', '27442'),
    ('c0000000-0000-0000-0000-000000000018', 'b0000000-0000-0000-0000-000000000018', 'Thạc sĩ Văn học Việt Nam, 11 năm kinh nghiệm rèn luyện cách lập dàn ý và phát triển ý tưởng sáng tạo trong bài thi môn Văn THPT.', 11, ARRAY['Vietnamese'], true, false, 'APPROVED', true, true, '01', '00166'),
    ('c0000000-0000-0000-0000-000000000019', 'b0000000-0000-0000-0000-000000000019', 'Tech Lead tại công ty công nghệ, giảng dạy Lập trình Web Fullstack (TypeScript, ReactJS, Java Spring Boot) từ nền tảng đến dự án thực chiến.', 5, ARRAY['Vietnamese', 'English'], true, true, 'APPROVED', true, true, '79', '27073'),
    ('c0000000-0000-0000-0000-000000000020', 'b0000000-0000-0000-0000-000000000020', 'Giáo viên Tiểu học dạy giỏi cấp Thành phố, chuyên kèm Toán tư duy Singapore, rèn chữ đẹp và chuẩn bị hành trang vào lớp 1 cho bé.', 8, ARRAY['Vietnamese'], true, true, 'APPROVED', true, true, '79', '27163');

-- ---------------------------------------------------------------------
-- PHẦN 7: BẰNG CẤP & CHỨNG CHỈ ĐÃ DUYỆT (TEACHER CREDENTIALS V40)
-- ---------------------------------------------------------------------
INSERT INTO teacher_credentials (
    id, teacher_id, label, evidence_public_id, evidence_resource_type,
    evidence_format, evidence_mime_type, evidence_size, status, approved_by, approved_at, version
) VALUES
    ('cc000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Bằng Thạc sĩ Sư phạm Toán học - ĐH Sư Phạm Hà Nội', 'credentials/toan_thacsi', 'image', 'jpg', 'image/jpeg', 1048576, 'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '30 days', 1),
    ('cc000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'Chứng chỉ IELTS 8.5 Overall (British Council)', 'credentials/ielts_85', 'image', 'png', 'image/png', 2097152, 'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '28 days', 1),
    ('cc000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'Bằng Kỹ sư Vật lý Kỹ thuật - ĐH Bách Khoa', 'credentials/ly_bachkhoa', 'image', 'jpg', 'image/jpeg', 1572864, 'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '25 days', 1),
    ('cc000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 'Bằng Thạc sĩ Hóa học Hữu cơ - ĐH Khoa học Tự nhiên', 'credentials/hoa_thacsi', 'image', 'jpg', 'image/jpeg', 1835008, 'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '20 days', 1),
    ('cc000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000007', 'Chứng chỉ AWS Solutions Architect & Python Professional', 'credentials/aws_cert', 'image', 'png', 'image/png', 1048576, 'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '22 days', 1),
    ('cc000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000009', 'Chứng chỉ JLPT N1 Tiếng Nhật (Japan Foundation)', 'credentials/jlpt_n1', 'image', 'jpg', 'image/jpeg', 1258291, 'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '18 days', 1),
    ('cc000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000010', 'Chứng chỉ TOPIK Cấp 6 Tiếng Hàn (NIIED)', 'credentials/topik_6', 'image', 'pdf', 'application/pdf', 3145728, 'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '15 days', 1),
    ('cc000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000011', 'Chứng chỉ HSK Cấp 6 Tiếng Trung Quốc tế (Hanban)', 'credentials/hsk_6', 'image', 'jpg', 'image/jpeg', 1468006, 'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '12 days', 1);

-- ---------------------------------------------------------------------
-- PHẦN 8: GÁN MÔN HỌC CHO GIA SƯ (TEACHER SUBJECTS)
-- ---------------------------------------------------------------------
INSERT INTO teacher_subjects (id, teacher_id, subject_id, level_description, experience_description, is_active)
VALUES
    ('e0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Lớp 10, 11, 12 và Luyện thi THPT Quốc Gia', '10 năm kinh nghiệm chuyên luyện đề nâng cao và bồi dưỡng học sinh thi chuyên Toán', true),
    ('e0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'IELTS 6.5 - 8.5 & Tiếng Anh giao tiếp phản xạ', '6 năm luyện thi IELTS chuyên sâu, kèm học viên đạt mục tiêu du học và định cư', true),
    ('e0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'Lớp 11, 12 và Luyện đề trắc nghiệm Vật Lý nhanh', '8 năm giảng dạy Vật Lý đại cương và luyện thi đại học điểm 9+', true),
    ('e0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', 'Hóa 10, 11, 12 và thi vào lớp 10 chuyên Hóa', '7 năm chuyên kèm học sinh mất gốc và bồi dưỡng học sinh giỏi Hóa', true),
    ('e0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000006', 'Văn cấp 3 và Ôn thi THPT Quốc Gia môn Văn', '9 năm chấm thi và huấn luyện kỹ năng lập luận nghị luận sắc sảo', true),
    ('e0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000005', 'Sinh học ôn thi khối B và Y Dược', '5 năm luyện đề thi Sinh chuyên sâu, giúp học sinh nắm trọn lý thuyết và bài tập phả hệ', true),
    ('e0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000008', 'Python cho người mới & Tư duy thuật toán', '6 năm kinh nghiệm phát triển phần mềm và hướng dẫn thuật toán cơ bản đến nâng cao', true),
    ('e0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000008', 'd0000000-0000-0000-0000-000000000012', 'Toán lớp 1 đến 5 và Toán tư duy logic', '4 năm kèm trẻ em tiểu học yêu thích và không sợ môn Toán', true),
    ('e0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000009', 'd0000000-0000-0000-0000-000000000009', 'Tiếng Nhật JLPT N5 đến N2 & Kaiwa', '5 năm luyện thi năng lực Nhật ngữ và đào tạo tiếng Nhật cho nhân viên doanh nghiệp', true),
    ('e0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000010', 'd0000000-0000-0000-0000-000000000010', 'Tiếng Hàn giao tiếp & Luyện thi TOPIK I-II', '4 năm dạy kèm du học sinh và người đi làm tại các tập đoàn Hàn Quốc', true),
    ('e0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000011', 'd0000000-0000-0000-0000-000000000011', 'Tiếng Trung giao tiếp & Luyện thi HSK 1-6', '7 năm đào tạo tiếng Trung thương mại và khẩu ngữ chuyên nghiệp', true),
    ('e0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000012', 'd0000000-0000-0000-0000-000000000002', 'Tiếng Anh cấp 2 & Luyện thi vào lớp 10', '5 năm đồng hành cùng học sinh mất gốc lấy lại căn bản tiếng Anh', true),
    ('e0000000-0000-0000-0000-000000000013', 'c0000000-0000-0000-0000-000000000013', 'd0000000-0000-0000-0000-000000000007', 'Lịch Sử THPT & Ôn thi đại học khối C', '8 năm bồi dưỡng học sinh giỏi Lịch Sử và luyện đề trắc nghiệm Sử đạt điểm cao', true),
    ('e0000000-0000-0000-0000-000000000014', 'c0000000-0000-0000-0000-000000000014', 'd0000000-0000-0000-0000-000000000002', 'IELTS Speaking & Writing chuyên sâu', '4 năm hướng dẫn học viên chinh phục band điểm IELTS Speaking 7.5+', true),
    ('e0000000-0000-0000-0000-000000000015', 'c0000000-0000-0000-0000-000000000015', 'd0000000-0000-0000-0000-000000000001', 'Hình học không gian & Tọa độ Oxyz', '6 năm luyện thi đại học phần Toán hình không gian đạt điểm tối đa', true),
    ('e0000000-0000-0000-0000-000000000016', 'c0000000-0000-0000-0000-000000000016', 'd0000000-0000-0000-0000-000000000004', 'Hóa học cấp 2 & Ôn thi chuyên Hóa', '5 năm ôn luyện học sinh đỗ vào trường chuyên Lam Sơn và KHTN', true),
    ('e0000000-0000-0000-0000-000000000017', 'c0000000-0000-0000-0000-000000000017', 'd0000000-0000-0000-0000-000000000003', 'Vật Lý lớp 10, 11 cơ bản & nâng cao', '3 năm kèm bài tập Vật lý gắn liền với thực hành mô phỏng', true),
    ('e0000000-0000-0000-0000-000000000018', 'c0000000-0000-0000-0000-000000000018', 'd0000000-0000-0000-0000-000000000006', 'Văn THCS & Luyện thi vào lớp 10', '11 năm giảng dạy văn học trung học cơ sở và rèn kỹ năng viết đoạn văn cảm thụ', true),
    ('e0000000-0000-0000-0000-000000000019', 'c0000000-0000-0000-0000-000000000019', 'd0000000-0000-0000-0000-000000000008', 'Lập trình Web Frontend & Backend', '5 năm hướng dẫn đồ án thực tế và đào tạo lập trình viên mới bắt đầu', true),
    ('e0000000-0000-0000-0000-000000000020', 'c0000000-0000-0000-0000-000000000020', 'd0000000-0000-0000-0000-000000000012', 'Toán tư duy Singapore cho bé tiểu học', '8 năm sư phạm tiểu học, rèn thói quen tự giác và tư duy phân tích hình ảnh', true);

-- ---------------------------------------------------------------------
-- PHẦN 9: CÁC GÓI HỌC PHÍ (PRICING PACKAGES)
-- ---------------------------------------------------------------------
INSERT INTO pricing_packages (
    id, teacher_id, subject_id, name, description, total_sessions, duration_days, price_vnd, session_duration_minutes, status
) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Toán Nền Tảng 10 Buổi', 'Lấy lại kiến thức cơ bản đại số & hình học lớp 12', 10, 60, 2000000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Toán Luyện Thi THPT 20 Buổi', 'Chuyên đề vận dụng cao 8+ và luyện đề thi thử bám sát cấu trúc Bộ GD', 20, 120, 3800000, 90, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'IELTS Speaking 1-1 (10 Buổi)', 'Sửa phát âm chuẩn IPA, mở rộng từ vựng collocations và luyện phản xạ Part 1-3', 10, 45, 3000000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'Vật Lý Cấp Tốc 10 Buổi', 'Công thức giải nhanh trắc nghiệm điện xoay chiều và sóng cơ', 10, 60, 2200000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', 'Hóa Học Chuyên Sâu 15 Buổi', 'Phân loại bài tập este, lipit và peptit nâng cao bồi dưỡng học sinh khá giỏi', 15, 90, 3150000, 75, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000006', 'Luyện Viết Văn Nghị Luận 10 Buổi', 'Rèn luyện mở bài ấn tượng, kỹ năng phát triển luận điểm và chiều sâu phân tích tác phẩm', 10, 60, 2500000, 90, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000008', 'Python Cơ Bản & Thuật Toán 12 Buổi', 'Từ cú pháp biến, hàm, cấu trúc dữ liệu đến thuật toán đệ quy & quy hoạch động', 12, 60, 3600000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000009', 'd0000000-0000-0000-0000-000000000009', 'Tiếng Nhật Sơ Cấp N5 (20 Buổi)', 'Học bảng chữ cái Hiragana/Katakana, ngữ pháp căn bản và giao tiếp hàng ngày', 20, 90, 4000000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000010', 'd0000000-0000-0000-0000-000000000010', 'Tiếng Hàn Giao Tiếp 10 Buổi', 'Giao tiếp hàng ngày, phản xạ du lịch, mua sắm và văn hóa giao tiếp công sở', 10, 45, 2400000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000011', 'd0000000-0000-0000-0000-000000000011', 'Tiếng Trung HSK 3 Cấp Tốc (15 Buổi)', '15 buổi chuẩn hóa phát âm, ngữ pháp trọng tâm và 600 từ vựng thi đỗ HSK 3', 15, 75, 3300000, 60, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000019', 'd0000000-0000-0000-0000-000000000008', 'Khóa Web Fullstack Thực Chiến (24 Buổi)', 'Xây dựng ứng dụng hoàn chỉnh từ con số 0 với React, Spring Boot & PostgreSQL', 24, 120, 6000000, 90, 'ACTIVE'),
    ('f0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000020', 'd0000000-0000-0000-0000-000000000012', 'Toán Tư Duy Tiểu Học (10 Buổi)', 'Kích thích tư duy logic trực quan và sự hào hứng học toán cho các bé', 10, 60, 2000000, 60, 'ACTIVE');

-- ---------------------------------------------------------------------
-- PHẦN 10: THỐNG KÊ GIA SƯ (TEACHER STATS CHO BẢNG XẾP HẠNG RANKING)
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
    ('90000000-0000-0000-0000-000000000020', 'c0000000-0000-0000-0000-000000000020', 4.45, 4.40, 12, 60, 0.8750, 6, 0.6100, 20, now());

-- ---------------------------------------------------------------------
-- PHẦN 11: VÍ TIỀN CHO GIA SƯ (WALLETS)
-- ---------------------------------------------------------------------
INSERT INTO wallets (id, teacher_id, pending_balance_vnd, available_balance_vnd, reserved_balance_vnd, version)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 180000, 180000, 0, 1),
    ('20000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 0, 270000, 0, 1),
    ('20000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000008', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000009', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000010', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000011', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000012', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000013', 'c0000000-0000-0000-0000-000000000013', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000014', 'c0000000-0000-0000-0000-000000000014', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000015', 'c0000000-0000-0000-0000-000000000015', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000016', 'c0000000-0000-0000-0000-000000000016', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000017', 'c0000000-0000-0000-0000-000000000017', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000018', 'c0000000-0000-0000-0000-000000000018', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000019', 'c0000000-0000-0000-0000-000000000019', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000020', 'c0000000-0000-0000-0000-000000000020', 0, 0, 0, 0);

-- ---------------------------------------------------------------------
-- PHẦN 12: TÀI KHOẢN NGÂN HÀNG GIA SƯ (TEACHER BANK ACCOUNTS)
-- ---------------------------------------------------------------------
INSERT INTO teacher_bank_accounts (
    id, teacher_id, bank_bin, bank_name, account_number_encrypted, account_holder_name, is_verified, is_default, version
) VALUES
    ('30000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', '970436', 'Ngân hàng TMCP Ngoại thương Việt Nam (Vietcombank)', 'ENC_ACC_1012345678', 'NGUYEN VAN TOAN', true, true, 0),
    ('30000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', '970422', 'Ngân hàng Quân Đội (MBBank)', 'ENC_ACC_0902000002', 'TRAN THI MAI ANH', true, true, 0),
    ('30000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', '970415', 'Ngân hàng TMCP Công thương Việt Nam (VietinBank)', 'ENC_ACC_1098765432', 'LE VAN QUANG', true, true, 0),
    ('30000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', '970407', 'Ngân hàng Kỹ Thương (Techcombank)', 'ENC_ACC_1903456789', 'HOANG NAM TECH', true, true, 0);

-- ---------------------------------------------------------------------
-- PHẦN 13: LỊCH RẢNH CỦA GIA SƯ (TEACHER AVAILABILITIES)
-- ---------------------------------------------------------------------
INSERT INTO teacher_availabilities (id, teacher_id, day_of_week, start_time, end_time, is_active)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'MONDAY', '18:00:00', '21:00:00', true),
    ('b1000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'WEDNESDAY', '18:00:00', '21:00:00', true),
    ('b1000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'FRIDAY', '18:00:00', '21:00:00', true),
    ('b1000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000002', 'TUESDAY', '19:00:00', '22:00:00', true),
    ('b1000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', 'THURSDAY', '19:00:00', '22:00:00', true),
    ('b1000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002', 'SATURDAY', '14:00:00', '18:00:00', true),
    ('b1000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'SATURDAY', '08:30:00', '11:30:00', true),
    ('b1000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000007', 'SUNDAY', '14:00:00', '17:00:00', true);

-- ---------------------------------------------------------------------
-- PHẦN 14: HÓA ĐƠN ĐÃ THANH TOÁN (INVOICES - BỔ SUNG ĐẦY ĐỦ SNAPSHOT V26)
-- ---------------------------------------------------------------------
INSERT INTO invoices (
    id, invoice_number, student_id, teacher_id, pricing_package_id,
    amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint, paid_at,
    subject_id_snapshot, package_name_snapshot, total_sessions_snapshot, duration_days_snapshot,
    session_duration_minutes_snapshot, commission_rate_snapshot, fingerprint_version, version
) VALUES
    ('50000000-0000-0000-0000-000000000001', 'INV-2026-000001', '10000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001',
     2000000, 'PAID', 100001, '50000000-0000-0000-0000-000000000001', 'fp_inv_math_001', now() - interval '10 days',
     'd0000000-0000-0000-0000-000000000001', 'Toán Nền Tảng 10 Buổi', 10, 60, 60, 10.00, 1, 0),

    ('50000000-0000-0000-0000-000000000002', 'INV-2026-000002', '10000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000003',
     3000000, 'PAID', 100002, '50000000-0000-0000-0000-000000000002', 'fp_inv_eng_002', now() - interval '8 days',
     'd0000000-0000-0000-0000-000000000002', 'IELTS Speaking 1-1 (10 Buổi)', 10, 45, 60, 10.00, 1, 0),

    ('50000000-0000-0000-0000-000000000003', 'INV-2026-000003', '10000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000007',
     3600000, 'PAID', 100003, '50000000-0000-0000-0000-000000000003', 'fp_inv_prog_003', now() - interval '5 days',
     'd0000000-0000-0000-0000-000000000008', 'Python Cơ Bản & Thuật Toán 12 Buổi', 12, 60, 60, 10.00, 1, 0);

-- ---------------------------------------------------------------------
-- PHẦN 15: GÓI HỌC CỦA HỌC VIÊN (STUDENT PACKAGES)
-- Đảm bảo invariant: remaining + reserved + completed + refunded = total_sessions
-- ---------------------------------------------------------------------
INSERT INTO student_packages (
    id, pricing_package_id, student_id, teacher_id, subject_id, invoice_id,
    package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions,
    purchase_price_vnd, commission_rate, starts_at, expires_at, status
) VALUES
    ('40000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
     'Toán Nền Tảng 10 Buổi', 10, 8, 1, 1, 0, 2000000, 10.00, now() - interval '10 days', now() + interval '50 days', 'ACTIVE'),

    ('40000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002',
     'IELTS Speaking 1-1 (10 Buổi)', 10, 9, 0, 1, 0, 3000000, 10.00, now() - interval '8 days', now() + interval '37 days', 'ACTIVE'),

    ('40000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000008', '50000000-0000-0000-0000-000000000003',
     'Python Cơ Bản & Thuật Toán 12 Buổi', 12, 12, 0, 0, 0, 3600000, 10.00, now() - interval '5 days', now() + interval '55 days', 'ACTIVE');

-- ---------------------------------------------------------------------
-- PHẦN 16: LỊCH HỌC (BOOKINGS)
-- ---------------------------------------------------------------------
INSERT INTO bookings (
    id, teacher_id, student_id, subject_id, student_package_id,
    start_time, end_time, delivery_mode, meeting_link, status, is_trial, completed_at, settlement_processed
) VALUES
    ('60000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001',
     now() - interval '3 days', now() - interval '3 days' + interval '1 hour', 'ONLINE', 'https://meet.google.com/abc-math-001', 'COMPLETED', false, now() - interval '3 days' + interval '1 hour', true),

    ('60000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001',
     now() + interval '2 days' + interval '18 hours', now() + interval '2 days' + interval '19 hours', 'ONLINE', 'https://meet.google.com/abc-math-002', 'SCHEDULED', false, null, false),

    ('60000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002',
     now() - interval '2 days', now() - interval '2 days' + interval '1 hour', 'ONLINE', 'https://meet.google.com/eng-room-002', 'COMPLETED', false, now() - interval '2 days' + interval '1 hour', true);

-- ---------------------------------------------------------------------
-- PHẦN 17: QUYẾT TOÁN HAI BÊN (BOOKING SETTLEMENTS V41)
-- ---------------------------------------------------------------------
INSERT INTO booking_settlements (
    id, booking_id, status, teacher_confirmed_at, student_confirmed_at, initial_deadline, net_amount_vnd, version
) VALUES
    ('61000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'RELEASED',
     now() - interval '3 days' + interval '1 hour', now() - interval '3 days' + interval '2 hours',
     now() - interval '2 days', 180000, 1),

    ('61000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', 'AWAITING_CONFIRMATION',
     null, null, now() + interval '3 days' + interval '19 hours', 180000, 0),

    ('61000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000003', 'RELEASED',
     now() - interval '2 days' + interval '1 hour', now() - interval '2 days' + interval '2 hours',
     now() - interval '1 day', 270000, 1);

-- ---------------------------------------------------------------------
-- PHẦN 18: SỔ CÁI VÍ TIỀN (LEDGER ENTRIES)
-- Khớp số dư ví cho gia sư 1 & gia sư 2
-- ---------------------------------------------------------------------
INSERT INTO ledger_entries (
    id, wallet_id, entry_type, amount_vnd, balance_bucket, direction,
    reference_type, reference_id, idempotency_key, description, created_at
) VALUES
    ('21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'SESSION_RELEASE_PENDING',
     180000, 'PENDING', 'CREDIT', 'BOOKING', '60000000-0000-0000-0000-000000000002', 'IDEM_LEDGER_MATH_001', 'Giữ tiền buổi học sắp tới Toán Nền Tảng', now() - interval '1 day'),

    ('21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'SESSION_CREDIT_AVAILABLE',
     180000, 'AVAILABLE', 'CREDIT', 'BOOKING', '60000000-0000-0000-0000-000000000001', 'IDEM_LEDGER_MATH_002', 'Thanh toán hoàn thành buổi 1 Toán Nền Tảng', now() - interval '3 days'),

    ('21000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'SESSION_CREDIT_AVAILABLE',
     270000, 'AVAILABLE', 'CREDIT', 'BOOKING', '60000000-0000-0000-0000-000000000003', 'IDEM_LEDGER_ENG_001', 'Thanh toán hoàn thành buổi 1 IELTS Speaking', now() - interval '2 days');

-- ---------------------------------------------------------------------
-- PHẦN 19: BÁO CÁO BUỔI HỌC (SESSION REPORTS)
-- ---------------------------------------------------------------------
INSERT INTO session_reports (id, booking_id, content, feedback, follow_up_note, teacher_self_rating, submitted_at)
VALUES
    ('70000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001',
     'Buổi 1: Ôn tập Hàm số lũy thừa, hàm số mũ và logarit. Luyện tập các dạng toán tìm tập xác định và đạo hàm cơ bản.',
     'Em An tiếp thu bài nhanh, nắm vững định nghĩa và công thức. Cần cẩn thận hơn ở bước biến đổi điều kiện xác định.',
     'Giao 10 câu bài tập trắc nghiệm chương 1 để em làm trước buổi học thứ 2.', 5, now() - interval '3 days' + interval '1 hour'),

    ('70000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000003',
     'Buổi 1: Luyện phát âm nguyên âm đôi và chuẩn hóa ngữ điệu câu hỏi IELTS Speaking Part 1 chủ đề Hometown & Accommodation.',
     'Em Bình nói lưu loát, âm đuôi rõ ràng. Cần chú ý nhấn trọng âm từ đa âm tiết và mở rộng câu trả lời bằng cấu trúc nối.',
     'Chuẩn bị từ vựng theo chủ đề Hobbies & Free Time cho buổi học kế tiếp.', 5, now() - interval '2 days' + interval '1 hour');

-- ---------------------------------------------------------------------
-- PHẦN 20: ĐÁNH GIÁ CỦA HỌC VIÊN (REVIEWS)
-- ---------------------------------------------------------------------
INSERT INTO reviews (id, booking_id, student_id, teacher_id, rating, comment, is_visible, created_at)
VALUES
    ('a1000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001',
     5, 'Thầy Toán dạy rất dễ hiểu, chỉ ra ngay các lỗi sai mình hay mắc khi làm bài khảo sát hàm số. Rất mong chờ buổi học tiếp theo!', true, now() - interval '3 days'),

    ('a1000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002',
     5, 'Cô Mai Anh hướng dẫn phát âm cực kỳ tỉ mỉ và tự nhiên. Buổi học vui vẻ, tạo cảm giác thoải mái và tự tin khi nói tiếng Anh.', true, now() - interval '2 days');

-- ---------------------------------------------------------------------
-- PHẦN 21: BÀI TẬP & NỘP BÀI (ASSIGNMENTS & SUBMISSIONS V12)
-- ---------------------------------------------------------------------
INSERT INTO assignments (
    id, teacher_id, student_id, subject_id, title, assignment_type,
    content_blocks, quiz_schema, due_at, status, version
) VALUES
    ('c1000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
     'Bài tập rèn luyện: Khảo sát sự biến thiên và Đồ thị hàm số', 'FREEFORM',
     '{"instructions": "Làm chi tiết bài tập 1 đến bài tập 5 trong phiếu ôn tập buổi 1. Chụp ảnh bài giải gửi lên hệ thống.", "questions_count": 5}'::jsonb,
     null, now() + interval '2 days', 'PUBLISHED', 0),

    ('c1000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002',
     'IELTS Speaking Record: Describe your favorite place in your hometown', 'FREEFORM',
     '{"instructions": "Ghi âm bài nói tối thiểu 1 phút 30 giây, áp dụng ít nhất 3 từ vựng chủ đề hometown đã học.", "topic": "Places"}'::jsonb,
     null, now() + interval '3 days', 'PUBLISHED', 0);

-- Bài nộp của học sinh và điểm số
INSERT INTO submissions (
    id, assignment_id, student_id, content_blocks, submitted_at, status, score, feedback_text, graded_at, version
) VALUES
    ('c2000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
     '{"text": "Em đã hoàn thành cả 5 bài tập tự luận và gửi lời giải trong phần đính kèm ạ.", "file_note": "Bài làm viết tay rõ ràng"}'::jsonb,
     now() - interval '1 day', 'GRADED', 9.50,
     'Thầy khen An trình bày sạch đẹp, lập bảng biến thiên chính xác tuyệt đối. Lưu ý bài số 4 cần kết luận rõ khoảng đồng biến.',
     now() - interval '12 hours', 1);

-- ---------------------------------------------------------------------
-- PHẦN 22: HỘI THOẠI & TIN NHẮN (CONVERSATIONS & MESSAGES V13)
-- ---------------------------------------------------------------------
INSERT INTO conversations (id, teacher_id, student_id, last_message_at)
VALUES
    ('e1000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', now() - interval '12 hours'),
    ('e1000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', now() - interval '1 day');

INSERT INTO messages (
    id, conversation_id, sender_id, client_message_id, message_type, content, sent_at, read_at
) VALUES
    ('e2000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000001', 'e3000000-0000-0000-0000-000000000001',
     'TEXT', 'Em chào thầy Toán ạ! Thầy cho em hỏi bài tập số 3 phần tiệm cận xiên em áp dụng công thức giới hạn này đúng chưa ạ?',
     now() - interval '1 day', now() - interval '23 hours'),

    ('e2000000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000001',
     'b0000000-0000-0000-0000-000000000001', 'e3000000-0000-0000-0000-000000000002',
     'TEXT', 'Chào An! Thầy đã xem bài giải của em rồi, cách biến đổi của em hoàn toàn đúng nhé. Em tiếp tục làm nốt bài 4 và 5 nhé!',
     now() - interval '12 hours', now() - interval '11 hours'),

    ('e2000000-0000-0000-0000-000000000003', 'e1000000-0000-0000-0000-000000000002',
     '10000000-0000-0000-0000-000000000002', 'e3000000-0000-0000-0000-000000000003',
     'TEXT', 'Em chào cô Mai Anh! Buổi học tiếp theo em muốn luyện sâu hơn về chủ đề Work & Study được không cô?',
     now() - interval '1 day', now() - interval '20 hours'),

    ('e2000000-0000-0000-0000-000000000004', 'e1000000-0000-0000-0000-000000000002',
     'b0000000-0000-0000-0000-000000000002', 'e3000000-0000-0000-0000-000000000004',
     'TEXT', 'Hoàn toàn được em nhé! Cô đã chuẩn bị sẵn bộ câu hỏi và bộ collocations nâng cao cho chủ đề đó rồi.',
     now() - interval '20 hours', now() - interval '19 hours');

-- ---------------------------------------------------------------------
-- PHẦN 23: THÔNG BÁO HỆ THỐNG (NOTIFICATIONS V13 & V20)
-- ---------------------------------------------------------------------
INSERT INTO notifications (
    id, user_id, type, title, content, reference_type, reference_id, is_read, created_at, updated_at, is_deleted
) VALUES
    ('f1000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'PAYMENT_SUCCESS',
     'Thanh toán thành công', 'Bạn đã thanh toán thành công 2.000.000đ cho gói học Toán Nền Tảng 10 Buổi.',
     'INVOICE', '50000000-0000-0000-0000-000000000001', true, now() - interval '10 days', now() - interval '10 days', false),

    ('f1000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'SESSION_REPORT_SUBMITTED',
     'Báo cáo buổi học mới', 'Thầy Nguyễn Văn Toán đã gửi báo cáo đánh giá buổi học 1 môn Toán.',
     'SESSION_REPORT', '70000000-0000-0000-0000-000000000001', true, now() - interval '3 days', now() - interval '3 days', false),

    ('f1000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'ASSIGNMENT_GRADED',
     'Bài tập đã được chấm điểm', 'Thầy Nguyễn Văn Toán đã chấm bài tập Khảo sát sự biến thiên của bạn với số điểm 9.5.',
     'SUBMISSION', 'c2000000-0000-0000-0000-000000000001', false, now() - interval '12 hours', now() - interval '12 hours', false),

    ('f1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'BOOKING_CONFIRMED',
     'Buổi học đã lên lịch', 'Học viên Nguyễn Văn An đã đặt lịch học môn Toán vào 2 ngày tới.',
     'BOOKING', '60000000-0000-0000-0000-000000000002', false, now() - interval '1 day', now() - interval '1 day', false);
