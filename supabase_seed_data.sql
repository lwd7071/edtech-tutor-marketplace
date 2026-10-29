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
    ('20000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 1620000, 180000, 0, 1),
    ('20000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 2430000, 270000, 0, 1),
    ('20000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006', 0, 0, 0, 0),
    ('20000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 3240000, 0, 0, 0),
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
     3600000, 'PAID', 100003, '50000000-0000-0000-0000-000000000003', 'fp_inv_prog_003', now() - interval '20 days',
     'd0000000-0000-0000-0000-000000000008', 'Python Cơ Bản & Thuật Toán 12 Buổi', 12, 60, 60, 10.00, 1, 0);

-- ---------------------------------------------------------------------
-- PHẦN 15: GÓI HỌC CỦA HỌC VIÊN (STUDENT PACKAGES)
-- Đảm bảo invariant: remaining + reserved + completed + refunded = total_sessions
-- ---------------------------------------------------------------------
INSERT INTO student_packages (
    id, pricing_package_id, student_id, teacher_id, subject_id, invoice_id, package_name_snapshot,
    total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions, purchase_price_vnd,
    commission_rate, starts_at, expires_at, status, session_duration_minutes_snapshot
) VALUES
    ('40000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
     'Toán Nền Tảng 10 Buổi', 10, 8, 1, 1, 0, 2000000, 10.00, now() - interval '10 days', now() + interval '50 days', 'ACTIVE', 60),

    ('40000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002',
     'IELTS Speaking 1-1 (10 Buổi)', 10, 9, 0, 1, 0, 3000000, 10.00, now() - interval '8 days', now() + interval '37 days', 'ACTIVE', 60),

    ('40000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000008', '50000000-0000-0000-0000-000000000003',
     'Python Cơ Bản & Thuật Toán 12 Buổi', 12, 12, 0, 0, 0, 3600000, 10.00, now() - interval '18 days', now() + interval '42 days', 'ACTIVE', 60);

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
     now() - interval '2 days' + interval '1 hour', 180000, 1),

    ('61000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', 'AWAITING_CONFIRMATION',
     null, null, now() + interval '3 days' + interval '19 hours', null, 0),

    ('61000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000003', 'RELEASED',
     now() - interval '2 days' + interval '1 hour', now() - interval '2 days' + interval '2 hours',
     now() - interval '1 day' + interval '1 hour', 270000, 1);

-- ---------------------------------------------------------------------
-- PHẦN 18: SỔ CÁI VÍ TIỀN (LEDGER ENTRIES)
-- Khớp số dư ví cho gia sư 1 & gia sư 2
-- ---------------------------------------------------------------------
INSERT INTO ledger_entries (
    id, wallet_id, entry_type, amount_vnd, balance_bucket, direction,
    reference_type, reference_id, idempotency_key, description, created_at
) VALUES
    ('21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'PACKAGE_FUNDED',
     1800000, 'PENDING', 'CREDIT', 'INVOICE', '50000000-0000-0000-0000-000000000001', 'WH-PAY-50000000-0000-0000-0000-000000000001', 'Ghi nhận tiền ròng của gói Toán', now() - interval '10 days'),

    ('21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'SESSION_CREDIT_AVAILABLE',
     180000, 'AVAILABLE', 'CREDIT', 'BOOKING', '60000000-0000-0000-0000-000000000001', 'booking:60000000-0000-0000-0000-000000000001:settlement:available', 'Thanh toán hoàn thành buổi 1 Toán Nền Tảng', now() - interval '3 days'),

    ('21000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'SESSION_CREDIT_AVAILABLE',
     270000, 'AVAILABLE', 'CREDIT', 'BOOKING', '60000000-0000-0000-0000-000000000003', 'booking:60000000-0000-0000-0000-000000000003:settlement:available', 'Thanh toán hoàn thành buổi 1 IELTS Speaking', now() - interval '2 days'),
    ('21000000-0000-0000-0000-000000000015', '20000000-0000-0000-0000-000000000001', 'SESSION_RELEASE_PENDING', 180000, 'PENDING', 'DEBIT', 'BOOKING', '60000000-0000-0000-0000-000000000001', 'booking:60000000-0000-0000-0000-000000000001:settlement:pending', 'Giải phóng số dư chờ buổi Toán', now() - interval '3 days'),
    ('21000000-0000-0000-0000-000000000016', '20000000-0000-0000-0000-000000000002', 'PACKAGE_FUNDED', 2700000, 'PENDING', 'CREDIT', 'INVOICE', '50000000-0000-0000-0000-000000000002', 'WH-PAY-50000000-0000-0000-0000-000000000002', 'Ghi nhận tiền ròng gói IELTS', now() - interval '8 days'),
    ('21000000-0000-0000-0000-000000000017', '20000000-0000-0000-0000-000000000002', 'SESSION_RELEASE_PENDING', 270000, 'PENDING', 'DEBIT', 'BOOKING', '60000000-0000-0000-0000-000000000003', 'booking:60000000-0000-0000-0000-000000000003:settlement:pending', 'Giải phóng số dư chờ buổi IELTS', now() - interval '2 days'),
    ('21000000-0000-0000-0000-000000000018', '20000000-0000-0000-0000-000000000007', 'PACKAGE_FUNDED', 3240000, 'PENDING', 'CREDIT', 'INVOICE', '50000000-0000-0000-0000-000000000003', 'WH-PAY-50000000-0000-0000-0000-000000000003', 'Ghi nhận tiền ròng gói Python', now() - interval '20 days');

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

-- ---------------------------------------------------------------------
-- PHẦN 24: FIXTURE CHO CÁC BẢNG/WORKFLOW CÒN THIẾU (SCHEMA V41)
-- Tài khoản ở trên được giữ nguyên; các trạng thái user không thể phủ đủ
-- mà không thay đổi tài khoản hiện có.
-- ---------------------------------------------------------------------

-- Session và tài liệu gia sư: đủ trạng thái verification.
INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, revoked_at, device_info, is_deleted) VALUES
    ('11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'seed-refresh-active-001', now() + interval '30 days', null, 'Web demo', false),
    ('11000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'seed-refresh-revoked-002', now() - interval '1 day', now() - interval '2 days', 'Mobile demo', true);

INSERT INTO teacher_documents (id, teacher_id, document_type, title, cloudinary_public_id, secure_url, mime_type, file_size, verification_status, verified_by, verified_at) VALUES
    ('12000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'DEGREE', 'Bằng đại học đã xác minh', 'seed/docs/verified', 'https://example.invalid/verified.pdf', 'application/pdf', 120000, 'VERIFIED', 'a0000000-0000-0000-0000-000000000001', now() - interval '20 days'),
    ('12000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'CERTIFICATE', 'Chứng chỉ chờ duyệt', 'seed/docs/pending', 'https://example.invalid/pending.pdf', 'application/pdf', 90000, 'PENDING', null, null),
    ('12000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'DEGREE', 'Tài liệu bị từ chối', 'seed/docs/rejected', 'https://example.invalid/rejected.pdf', 'application/pdf', 80000, 'REJECTED', 'a0000000-0000-0000-0000-000000000001', now() - interval '10 days');

INSERT INTO subject_proposals (id, teacher_id, proposed_name, education_level, description, status, review_note, reviewed_by, reviewed_at, created_subject_id) VALUES
    ('13000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Toán tư duy nâng cao', 'PRIMARY_SCHOOL', 'Đề xuất môn học mới', 'PENDING', null, null, null, null),
    ('13000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'English Conversation', 'OTHER', 'Đề xuất đã duyệt', 'APPROVED', 'Đã tạo danh mục', 'a0000000-0000-0000-0000-000000000001', now() - interval '5 days', 'd0000000-0000-0000-0000-000000000002'),
    ('13000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'Vật lý lượng tử', 'UNIVERSITY', 'Đề xuất chưa phù hợp', 'REJECTED', 'Trùng danh mục hiện có', 'a0000000-0000-0000-0000-000000000001', now() - interval '4 days', null);

-- Trial requests, bao gồm cả booking học thử đã chấp nhận.
INSERT INTO bookings (id, teacher_id, student_id, subject_id, student_package_id, start_time, end_time, delivery_mode, meeting_link, status, is_trial, completed_at, settlement_processed) VALUES
    ('60000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', null, now() + interval '5 days', now() + interval '5 days 1 hour', 'ONLINE', 'https://meet.example.invalid/trial', 'SCHEDULED', true, null, false);
INSERT INTO trial_requests (id, teacher_id, student_id, subject_id, preferred_start_time, note, status, booking_id, responded_at) VALUES
    ('14000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', now() + interval '5 days', 'Học thử đã được xác nhận', 'ACCEPTED', '60000000-0000-0000-0000-000000000004', now()),
    ('14000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', now() + interval '8 days', 'Đang chờ phản hồi', 'PENDING', null, null),
    ('14000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000006', now() + interval '9 days', 'Đã từ chối', 'REJECTED', null, now()),
    ('14000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000005', now() + interval '10 days', 'Học viên hủy', 'CANCELLED', null, now());

-- Hóa đơn và giao dịch cổng thanh toán: giữ cùng snapshot package/commission.
INSERT INTO invoices (id, invoice_number, student_id, teacher_id, pricing_package_id, amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint, payment_expired_at, subject_id_snapshot, package_name_snapshot, total_sessions_snapshot, duration_days_snapshot, session_duration_minutes_snapshot, commission_rate_snapshot, fingerprint_version, version) VALUES
    ('50000000-0000-0000-0000-000000000004', 'INV-2026-000004', '10000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000004', 2200000, 'PENDING', 100004, '50000000-0000-0000-0000-000000000004', 'fp_inv_004', now() + interval '1 day', 'd0000000-0000-0000-0000-000000000003', 'Vật Lý Cấp Tốc 10 Buổi', 10, 60, 60, 10.00, 1, 0),
    ('50000000-0000-0000-0000-000000000005', 'INV-2026-000005', '10000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000005', 3150000, 'CANCELLED', 100005, '50000000-0000-0000-0000-000000000005', 'fp_inv_005', null, 'd0000000-0000-0000-0000-000000000004', 'Hóa Học Chuyên Sâu 15 Buổi', 15, 90, 75, 10.00, 1, 0),
    ('50000000-0000-0000-0000-000000000006', 'INV-2026-000006', '10000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000006', 2500000, 'EXPIRED', 100006, '50000000-0000-0000-0000-000000000006', 'fp_inv_006', now() - interval '1 day', 'd0000000-0000-0000-0000-000000000006', 'Luyện Viết Văn Nghị Luận 10 Buổi', 10, 60, 90, 10.00, 1, 0);
INSERT INTO payment_transactions (id, invoice_id, provider, provider_reference, order_code, amount_vnd, transaction_datetime, raw_payload, signature_valid, processed_at) VALUES
    ('15000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'PAYOS', 'seed-payment-success-001', 100001, 2000000, now() - interval '10 days', '{"code":"00","demo":true}'::jsonb, true, now() - interval '10 days'),
    ('15000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000004', 'PAYOS', 'seed-payment-pending-002', 100004, 2200000, now(), '{"code":"PENDING","demo":true}'::jsonb, true, null),
    ('15000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000002', 'PAYOS', 'seed-payment-success-003', 100002, 3000000, now() - interval '8 days', '{"code":"00","demo":true}'::jsonb, true, now() - interval '8 days'),
    ('15000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000003', 'PAYOS', 'seed-payment-success-004', 100003, 3600000, now() - interval '20 days', '{"code":"00","demo":true}'::jsonb, true, now() - interval '20 days'),
    ('15000000-0000-0000-0000-000000000005', '50000000-0000-0000-0000-000000000007', 'PAYOS', 'seed-payment-success-007', 100007, 2000000, now() - interval '20 days', '{"code":"00","demo":true}'::jsonb, true, now() - interval '20 days'),
    ('15000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000008', 'PAYOS', 'seed-payment-success-008', 100008, 2000000, now() - interval '20 days', '{"code":"00","demo":true}'::jsonb, true, now() - interval '20 days'),
    ('15000000-0000-0000-0000-000000000007', '50000000-0000-0000-0000-000000000009', 'PAYOS', 'seed-payment-success-009', 100009, 2000000, now() - interval '100 days', '{"code":"00","demo":true}'::jsonb, true, now() - interval '100 days'),
    ('15000000-0000-0000-0000-000000000008', '50000000-0000-0000-0000-000000000010', 'PAYOS', 'seed-payment-success-010', 100010, 2000000, now() - interval '100 days', '{"code":"00","demo":true}'::jsonb, true, now() - interval '100 days');
INSERT INTO invoices (id, invoice_number, student_id, teacher_id, pricing_package_id, amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint, paid_at, subject_id_snapshot, package_name_snapshot, total_sessions_snapshot, duration_days_snapshot, session_duration_minutes_snapshot, commission_rate_snapshot, fingerprint_version, version) VALUES
    ('50000000-0000-0000-0000-000000000007', 'INV-2026-000007', '10000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 2000000, 'PAID', 100007, '50000000-0000-0000-0000-000000000007', 'fp_inv_007', now() - interval '20 days', 'd0000000-0000-0000-0000-000000000001', 'Toán Nền Tảng 10 Buổi', 10, 60, 60, 10.00, 1, 0),
    ('50000000-0000-0000-0000-000000000008', 'INV-2026-000008', '10000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 2000000, 'PAID', 100008, '50000000-0000-0000-0000-000000000008', 'fp_inv_008', now() - interval '20 days', 'd0000000-0000-0000-0000-000000000001', 'Toán Nền Tảng 10 Buổi', 10, 60, 60, 10.00, 1, 0),
    ('50000000-0000-0000-0000-000000000009', 'INV-2026-000009', '10000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 2000000, 'PAID', 100009, '50000000-0000-0000-0000-000000000009', 'fp_inv_009', now() - interval '100 days', 'd0000000-0000-0000-0000-000000000001', 'Toán Nền Tảng 10 Buổi', 10, 60, 60, 10.00, 1, 0),
    ('50000000-0000-0000-0000-000000000010', 'INV-2026-000010', '10000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 2000000, 'PAID', 100010, '50000000-0000-0000-0000-000000000010', 'fp_inv_010', now() - interval '100 days', 'd0000000-0000-0000-0000-000000000001', 'Toán Nền Tảng 10 Buổi', 10, 60, 60, 10.00, 1, 0);
INSERT INTO student_packages (id, pricing_package_id, student_id, teacher_id, subject_id, invoice_id, package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions, purchase_price_vnd, commission_rate, starts_at, expires_at, status, locked_reason, session_duration_minutes_snapshot) VALUES
    ('40000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000004', 'Vật Lý Cấp Tốc 10 Buổi', 10, 10, 0, 0, 0, 2200000, 10.00, now(), now() + interval '60 days', 'PENDING_PAYMENT', null, 60),
    ('40000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000007', 'Toán Nền Tảng 10 Buổi', 10, 0, 0, 10, 0, 2000000, 10.00, now() - interval '20 days', now() + interval '40 days', 'COMPLETED', null, 60),
    ('40000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000008', 'Toán Nền Tảng 10 Buổi', 10, 7, 0, 2, 1, 2000000, 10.00, now() - interval '20 days', now() + interval '40 days', 'REFUND_PENDING', null, 60),
    ('40000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000009', 'Toán Nền Tảng 10 Buổi', 10, 0, 0, 7, 3, 2000000, 10.00, now() - interval '100 days', now() - interval '40 days', 'REFUNDED', null, 60),
    ('40000000-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000010', 'Toán Nền Tảng 10 Buổi', 10, 6, 0, 4, 0, 2000000, 10.00, now() - interval '100 days', now() - interval '40 days', 'LOCKED_EXPIRED', 'Gói đã hết hạn', 60);

-- Learning: các loại bài, trạng thái draft/submitted/graded và attachment.
INSERT INTO assignments (id, teacher_id, student_id, subject_id, title, assignment_type, content_blocks, quiz_schema, due_at, status, version) VALUES
    ('c1000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000001', 'Quiz đại số bản nháp', 'SYSTEM_QUIZ', '{"instructions":"Bản nháp"}'::jsonb, '{"questions":[]}'::jsonb, now() + interval '5 days', 'DRAFT', 0),
    ('c1000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000002', 'Quiz IELTS đã đóng', 'SYSTEM_QUIZ', '{"instructions":"Hoàn thành quiz"}'::jsonb, '{"questions":[]}'::jsonb, now() - interval '1 day', 'CLOSED', 1);
INSERT INTO submissions (id, assignment_id, student_id, content_blocks, submitted_at, status, score, feedback_text, graded_at, version) VALUES
    ('c2000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '{"text":"Đã nộp, chờ chấm"}'::jsonb, now() - interval '2 hours', 'SUBMITTED', null, null, null, 0),
    ('c2000000-0000-0000-0000-000000000003', 'c1000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004', '{"answers":[]}'::jsonb, null, 'DRAFT', null, null, null, 0);

-- Finance admin queues: mọi status hiện hành, không dùng trạng thái đã bị V29 loại.
INSERT INTO payout_requests (id, teacher_id, wallet_id, bank_account_id, amount_vnd, status, teacher_note, admin_note, bank_reference, proof_public_id, proof_url, transferred_at, processed_by, processed_at, version) VALUES
    ('16000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 50000, 'PENDING', 'Xin rút tiền', null, null, null, null, null, null, null, 0),
    ('16000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 50000, 'PROCESSING', 'Đang xử lý', 'Đã tiếp nhận', null, null, null, null, 'a0000000-0000-0000-0000-000000000001', now(), 1),
    ('16000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 50000, 'SUCCEEDED', 'Đã rút', null, 'SEED-BANK-001', 'seed/payout/proof', 'https://example.invalid/payout-proof', now() - interval '1 day', 'a0000000-0000-0000-0000-000000000001', now() - interval '1 day', 1),
    ('16000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000007', 50000, 'REJECTED', 'Yêu cầu rút', 'Thông tin cần cập nhật', null, null, null, null, 'a0000000-0000-0000-0000-000000000001', now(), 1);

INSERT INTO refund_requests (id, student_package_id, student_id, reason, requested_sessions, approved_sessions, refund_amount_vnd, status, admin_note, bank_name, bank_bin, account_number_encrypted, account_holder_name, bank_reference, proof_public_id, proof_url, processed_by, processed_at, transferred_at, version) VALUES
    ('17000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000005', 'Đang chờ duyệt', 1, 0, null, 'PENDING', null, null, null, null, null, null, null, null, null, null, null, 0),
    ('17000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Đã duyệt, chờ chuyển', 1, 1, 270000, 'APPROVED', 'Đã duyệt', null, null, null, null, null, null, null, 'a0000000-0000-0000-0000-000000000001', now(), null, 1),
    ('17000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000006', 'Đã hoàn tiền', 3, 3, 600000, 'REFUNDED', 'Hoàn tất', 'Demo Bank', '970436', 'ENC_REFUND_001', 'DO THAO VY', 'SEED-REFUND-001', 'seed/refund/proof', 'https://example.invalid/refund-proof', 'a0000000-0000-0000-0000-000000000001', now() - interval '1 day', now() - interval '1 day', 1),
    ('17000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000003', 'Bị từ chối', 1, 0, null, 'REJECTED', 'Không đủ điều kiện', null, null, null, null, null, null, null, 'a0000000-0000-0000-0000-000000000001', now(), null, 1);
INSERT INTO package_extension_requests (id, student_package_id, student_id, reason, requested_expiry_date, approved_expiry_date, status, admin_note, reviewed_by, reviewed_at) VALUES
    ('18000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Xin gia hạn', now() + interval '90 days', null, 'PENDING', null, null, null),
    ('18000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Đã gia hạn', now() + interval '90 days', now() + interval '90 days', 'APPROVED', 'Đồng ý', 'a0000000-0000-0000-0000-000000000001', now()),
    ('18000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'Từ chối', now() + interval '90 days', null, 'REJECTED', 'Gói còn hạn dài', 'a0000000-0000-0000-0000-000000000001', now());

-- Credentials, file attachments, message types, audit and outbox.
INSERT INTO teacher_credentials (id, teacher_id, label, evidence_public_id, evidence_resource_type, evidence_format, evidence_mime_type, evidence_size, status, rejected_reason) VALUES
    ('cc000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000012', 'Chứng chỉ đang chờ', 'credentials/pending', 'image', 'png', 'image/png', 100000, 'PENDING', null),
    ('cc000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000013', 'Chứng chỉ bị từ chối', 'credentials/rejected', 'image', 'jpg', 'image/jpeg', 100000, 'REJECTED', 'Ảnh không rõ');
INSERT INTO attachments (id, owner_id, attachable_type, attachable_id, cloudinary_public_id, secure_url, original_filename, mime_type, file_size) VALUES
    ('19000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'SUBMISSION', 'c2000000-0000-0000-0000-000000000001', 'seed/homework/001', 'https://example.invalid/homework.jpg', 'homework.jpg', 'image/jpeg', 240000),
    ('19000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'MESSAGE', 'e2000000-0000-0000-0000-000000000001', 'seed/chat/001', 'https://example.invalid/chat.png', 'question.png', 'image/png', 80000);
INSERT INTO attachments (id, owner_id, attachable_type, attachable_id, cloudinary_public_id, secure_url, original_filename, mime_type, file_size) VALUES
    ('19000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'ASSIGNMENT', 'c1000000-0000-0000-0000-000000000001', 'seed/assignment/001', 'https://example.invalid/worksheet.pdf', 'worksheet.pdf', 'application/pdf', 60000),
    ('19000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'MESSAGE', 'e2000000-0000-0000-0000-000000000005', 'seed/chat/002', 'https://example.invalid/voice.mp3', 'voice.mp3', 'audio/mpeg', 120000);
INSERT INTO messages (id, conversation_id, sender_id, client_message_id, message_type, content, attachment_id, sent_at) VALUES
    ('e2000000-0000-0000-0000-000000000005', 'e1000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'e3000000-0000-0000-0000-000000000005', 'FILE', 'Em gửi file bài làm ạ', '19000000-0000-0000-0000-000000000004', now() - interval '2 hours');
UPDATE messages SET message_type = 'IMAGE', attachment_id = '19000000-0000-0000-0000-000000000002' WHERE id = 'e2000000-0000-0000-0000-000000000001';
INSERT INTO audit_logs (id, actor_id, action, target_type, target_id, before_data, after_data, ip_address, user_agent) VALUES
    ('1a000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'TEACHER_DOCUMENT_REVIEWED', 'TEACHER_DOCUMENT', '12000000-0000-0000-0000-000000000001', '{"status":"PENDING"}'::jsonb, '{"status":"VERIFIED"}'::jsonb, '127.0.0.1', 'TutorMatch seed');
INSERT INTO email_outbox (id, recipient, subject, body, status, attempts, next_attempt_at, created_at, sent_at) VALUES
    ('1b000000-0000-0000-0000-000000000001', 'student01@edtech.vn', 'Chào mừng', 'Email demo đã gửi', 'SENT', 1, now(), now() - interval '2 days', now() - interval '2 days'),
    ('1b000000-0000-0000-0000-000000000002', 'student02@edtech.vn', 'Nhắc lịch học', 'Email demo đang chờ', 'PENDING', 0, now() + interval '1 hour', now(), null),
    ('1b000000-0000-0000-0000-000000000003', 'student03@edtech.vn', 'Thông báo demo', 'Email demo lỗi gửi', 'FAILED', 3, now(), now() - interval '1 day', null);

-- Finance receipts và V41 platform escrow: fixture cho queue/admin decision.
INSERT INTO finance_command_receipts (id, actor_id, operation, idempotency_key, request_fingerprint, response_type, response_payload, expires_at) VALUES
    ('1c000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'PAYOUT_APPROVE', '1c000000-0000-0000-0000-000000000002', 'seed-fingerprint-001', 'PayoutResponse', '{"status":"SUCCEEDED"}'::jsonb, now() + interval '1 day');
-- Bổ sung 4 booking đã học để minh họa toàn bộ nhánh xử lý settlement V41.
UPDATE student_packages SET remaining_sessions = 8, completed_sessions = 4 WHERE id = '40000000-0000-0000-0000-000000000003';
INSERT INTO bookings (id, teacher_id, student_id, subject_id, student_package_id, start_time, end_time, delivery_mode, meeting_link, status, is_trial, completed_at, settlement_processed) VALUES
    ('60000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', now() - interval '4 days', now() - interval '4 days' + interval '1 hour', 'ONLINE', 'https://meet.example.invalid/python-1', 'COMPLETED', false, now() - interval '4 days' + interval '1 hour', false),
    ('60000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', now() - interval '6 days', now() - interval '6 days' + interval '1 hour', 'ONLINE', 'https://meet.example.invalid/python-2', 'COMPLETED', false, now() - interval '6 days' + interval '1 hour', false),
    ('60000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', now() - interval '8 days', now() - interval '8 days' + interval '1 hour', 'OFFLINE', null, 'COMPLETED', false, now() - interval '8 days' + interval '1 hour', false),
    ('60000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', now() - interval '10 days', now() - interval '10 days' + interval '1 hour', 'ONLINE', 'https://meet.example.invalid/python-4', 'COMPLETED', false, now() - interval '10 days' + interval '1 hour', false),
    ('60000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', now() - interval '16 days', now() - interval '16 days' + interval '1 hour', 'OFFLINE', null, 'COMPLETED', false, now() - interval '16 days' + interval '1 hour', false);
UPDATE student_packages SET remaining_sessions = 7, completed_sessions = 5 WHERE id = '40000000-0000-0000-0000-000000000003';
INSERT INTO bookings (id, teacher_id, student_id, subject_id, student_package_id, start_time, end_time, delivery_mode, status, cancel_reason, cancel_initiated_by, cancelled_at, expired_at) VALUES
    ('60000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', now() - interval '18 days', now() - interval '18 days' + interval '1 hour', 'OFFLINE', 'CANCELLED', 'Học viên xin hủy', 'STUDENT_REQUEST', now() - interval '19 days', null),
    ('60000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', now() - interval '20 days', now() - interval '20 days' + interval '1 hour', 'ONLINE', 'EXPIRED', null, 'SYSTEM', null, now() - interval '19 days');
INSERT INTO booking_settlements (id, booking_id, status, teacher_confirmed_at, student_confirmed_at, initial_deadline, reopen_deadline, net_amount_vnd, dispute_reason, disputed_at, reopened_at, version) VALUES
    ('61000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000005', 'HELD', now() - interval '4 days' + interval '1 hour', null, now() - interval '3 days' + interval '1 hour', null, 0, null, null, null, 0),
    ('61000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000006', 'DISPUTE_PENDING', now() - interval '6 days' + interval '2 hours', null, now() - interval '5 days' + interval '1 hour', null, 270000, 'Học viên báo buổi học chưa hoàn tất', now() - interval '1 day', null, 1),
    ('61000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000007', 'REOPENED', now() - interval '8 days' + interval '2 hours', null, now() - interval '7 days' + interval '1 hour', now() + interval '1 day', 270000, 'Học viên đề nghị xem lại buổi học', now() - interval '2 days', now() - interval '1 day', 2),
    ('61000000-0000-0000-0000-000000000007', '60000000-0000-0000-0000-000000000008', 'AWAITING_ADMIN_DECISION', now() - interval '10 days' + interval '2 hours', null, now() - interval '9 days' + interval '1 hour', now() - interval '1 day', 270000, 'Hai bên chưa thống nhất về chất lượng buổi học', now() - interval '3 days', now() - interval '2 days', 2),
    ('61000000-0000-0000-0000-000000000008', '60000000-0000-0000-0000-000000000011', 'RETAINED', now() - interval '16 days' + interval '2 hours', null, now() - interval '15 days' + interval '1 hour', null, 270000, 'Gia sư không đủ điều kiện nhận tiền', now() - interval '4 days', null, 1);
INSERT INTO ledger_entries (id, wallet_id, entry_type, amount_vnd, balance_bucket, direction, reference_type, reference_id, idempotency_key, description) VALUES
    ('21000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000003', 'ADJUSTMENT', 50000, 'AVAILABLE', 'CREDIT', 'ADJUSTMENT', '16000000-0000-0000-0000-000000000003', 'seed-wallet-teacher3-credit', 'Số dư trước payout demo'),
    ('21000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000003', 'PAYOUT_SUCCEEDED', 50000, 'AVAILABLE', 'DEBIT', 'PAYOUT', '16000000-0000-0000-0000-000000000003', 'seed-wallet-teacher3-payout', 'Payout demo đã chuyển'),
    ('21000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000001', 'PAYOUT_RESERVED', 50000, 'AVAILABLE', 'DEBIT', 'PAYOUT', '16000000-0000-0000-0000-000000000001', 'seed-wallet-teacher1-reserve-out', 'Giữ số dư cho yêu cầu payout'),
    ('21000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000001', 'PAYOUT_RESERVED', 50000, 'RESERVED', 'CREDIT', 'PAYOUT', '16000000-0000-0000-0000-000000000001', 'seed-wallet-teacher1-reserve-in', 'Số dư đã giữ cho payout'),
    ('21000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000002', 'PAYOUT_RESERVED', 50000, 'AVAILABLE', 'DEBIT', 'PAYOUT', '16000000-0000-0000-0000-000000000002', 'seed-wallet-teacher2-reserve-out', 'Giữ số dư cho payout'),
    ('21000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000002', 'PAYOUT_RESERVED', 50000, 'RESERVED', 'CREDIT', 'PAYOUT', '16000000-0000-0000-0000-000000000002', 'seed-wallet-teacher2-reserve-in', 'Số dư đã giữ cho payout');
UPDATE wallets SET available_balance_vnd = 130000, reserved_balance_vnd = 50000 WHERE id = '20000000-0000-0000-0000-000000000001';
UPDATE wallets SET available_balance_vnd = 220000, reserved_balance_vnd = 50000 WHERE id = '20000000-0000-0000-0000-000000000002';

-- Số dư ledger khớp wallet sau khi giữ 50.000 cho mỗi payout đang mở;
-- payout thành công có proof và debit sổ cái tương ứng.

-- Các trạng thái catalog/profile được giữ ở fixture để kiểm thử màn quản trị.
UPDATE pricing_packages SET status = 'DRAFT' WHERE id = 'f0000000-0000-0000-0000-000000000011';
UPDATE pricing_packages SET status = 'INACTIVE' WHERE id = 'f0000000-0000-0000-0000-000000000012';
UPDATE teacher_profiles SET profile_status = 'DRAFT', is_visible = false, verified_badge = false WHERE id = 'c0000000-0000-0000-0000-000000000018';
UPDATE teacher_profiles SET profile_status = 'PENDING_APPROVAL', is_visible = false, verified_badge = false WHERE id = 'c0000000-0000-0000-0000-000000000019';
UPDATE teacher_profiles SET profile_status = 'REJECTED', is_visible = false, verified_badge = false WHERE id = 'c0000000-0000-0000-0000-000000000020';

-- ---------------------------------------------------------------------
-- PHẦN 25: DỮ LIỆU DÀY CHO MỌI CẶP GIA SƯ - HỌC SINH (20 x 6)
-- Mỗi cặp: 1 gói 8 buổi, 3 buổi đã học, 2 buổi sắp tới, review, bài tập, chat.
-- ---------------------------------------------------------------------
DO $seed_full_marketplace$
DECLARE
    t record;
    s record;
    v_subject_id uuid;
    v_subject_name varchar(150);
    v_pricing_package_id uuid;
    v_invoice_id uuid;
    v_student_package_id uuid;
    v_booking_id uuid;
    v_report_id uuid;
    v_assignment_id uuid;
    v_submission_id uuid;
    v_conversation_id uuid;
    v_pair_no integer;
    v_session_no integer;
    v_future_no integer;
    v_start_at timestamptz;
    v_end_at timestamptz;
    v_purchase_at timestamptz := now() - interval '450 days';
    v_invoice_number varchar(50);
    v_rating smallint;
    v_teacher_net bigint := 1440000;
    v_session_net bigint := 180000;
BEGIN
    -- Make the existing tutor accounts eligible for paid marketplace fixtures.
    UPDATE teacher_profiles
    SET profile_status = 'APPROVED', is_visible = true, verified_badge = true,
        approved_by = 'a0000000-0000-0000-0000-000000000001',
        approved_at = COALESCE(approved_at, now()), rejection_reason = null
    WHERE is_deleted = false;

    INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, device_info)
    SELECT gen_random_uuid(), u.id, 'seed-refresh-' || u.id::text, now() + interval '30 days', 'Seed demo session'
    FROM users u
    WHERE NOT EXISTS (SELECT 1 FROM refresh_tokens rt WHERE rt.user_id = u.id AND rt.is_deleted = false);

    INSERT INTO teacher_availabilities (id, teacher_id, day_of_week, start_time, end_time, is_active)
    SELECT gen_random_uuid(), tp.id, days.day_of_week, '18:00', '21:00', true
    FROM teacher_profiles tp CROSS JOIN (VALUES ('MONDAY'), ('WEDNESDAY')) AS days(day_of_week)
    WHERE NOT EXISTS (SELECT 1 FROM teacher_availabilities a WHERE a.teacher_id = tp.id AND a.is_deleted = false);

    INSERT INTO teacher_bank_accounts (id, teacher_id, bank_bin, bank_name, account_number_encrypted, account_holder_name, is_verified, is_default, version)
    SELECT gen_random_uuid(), tp.id, '970436', 'Ngân hàng demo', 'ENC_SEED_' || replace(tp.id::text, '-', ''), 'SEED TEACHER ' || right(tp.id::text, 8), true, true, 0
    FROM teacher_profiles tp
    WHERE NOT EXISTS (SELECT 1 FROM teacher_bank_accounts ba WHERE ba.teacher_id = tp.id AND ba.is_default = true AND ba.is_deleted = false);

    INSERT INTO teacher_documents (id, teacher_id, document_type, title, cloudinary_public_id, secure_url, mime_type, file_size, verification_status, verified_by, verified_at, created_at)
    SELECT gen_random_uuid(), tp.id, 'DEGREE', 'Bằng cấp demo đã xác minh', 'seed/documents/' || tp.id::text,
           'https://example.invalid/teacher-document.pdf', 'application/pdf', 120000, 'VERIFIED',
           'a0000000-0000-0000-0000-000000000001', now() - interval '60 days', now() - interval '90 days'
    FROM teacher_profiles tp
    WHERE NOT EXISTS (SELECT 1 FROM teacher_documents td WHERE td.teacher_id = tp.id AND td.is_deleted = false);

    INSERT INTO teacher_credentials (id, teacher_id, label, evidence_public_id, evidence_resource_type, evidence_format, evidence_mime_type, evidence_size, status, approved_by, approved_at, created_at)
    SELECT gen_random_uuid(), tp.id, 'Chứng chỉ demo', 'seed/credentials/' || tp.id::text, 'image', 'png', 'image/png', 100000,
           'APPROVED', 'a0000000-0000-0000-0000-000000000001', now() - interval '60 days', now() - interval '90 days'
    FROM teacher_profiles tp
    WHERE NOT EXISTS (SELECT 1 FROM teacher_credentials tc WHERE tc.teacher_id = tp.id AND tc.is_deleted = false);

    FOR t IN
        SELECT tp.id AS teacher_id, tp.user_id AS teacher_user_id,
               row_number() OVER (ORDER BY tp.id)::integer AS teacher_no
        FROM teacher_profiles tp WHERE tp.is_deleted = false ORDER BY tp.id
    LOOP
        SELECT ts.subject_id, sub.name INTO v_subject_id, v_subject_name FROM teacher_subjects ts
        JOIN subjects sub ON sub.id = ts.subject_id
        WHERE ts.teacher_id = t.teacher_id AND ts.is_active = true AND ts.is_deleted = false
        ORDER BY ts.id LIMIT 1;
        IF v_subject_id IS NULL THEN RAISE EXCEPTION 'Seed teacher % has no active subject', t.teacher_id; END IF;

        v_pricing_package_id := gen_random_uuid();
        INSERT INTO pricing_packages (id, teacher_id, subject_id, name, description, total_sessions, duration_days, price_vnd, session_duration_minutes, status, created_at)
        VALUES (v_pricing_package_id, t.teacher_id, v_subject_id, 'Gói đồng hành 8 buổi', 'Lộ trình demo gồm các buổi học và bài tập theo tiến độ.', 8, 800, 1600000, 60, 'ACTIVE', v_purchase_at);

        FOR s IN
            SELECT u.id AS student_id, row_number() OVER (ORDER BY u.id)::integer AS student_no
            FROM users u WHERE u.role = 'STUDENT' AND u.status = 'ACTIVE' ORDER BY u.id
        LOOP
            v_pair_no := (t.teacher_no - 1) * 6 + s.student_no;
            v_invoice_id := gen_random_uuid();
            v_student_package_id := gen_random_uuid();
            v_invoice_number := format('INV-DEMO-%s-%s', lpad(t.teacher_no::text, 2, '0'), lpad(s.student_no::text, 2, '0'));

            INSERT INTO invoices (id, invoice_number, student_id, teacher_id, pricing_package_id, amount_vnd, status, payos_order_code, idempotency_key, request_fingerprint, paid_at, subject_id_snapshot, package_name_snapshot, total_sessions_snapshot, duration_days_snapshot, session_duration_minutes_snapshot, commission_rate_snapshot, fingerprint_version, version, created_at)
            VALUES (v_invoice_id, v_invoice_number, s.student_id, t.teacher_id, v_pricing_package_id, 1600000, 'PAID', 900000000 + v_pair_no,
                    gen_random_uuid(), md5(v_invoice_id::text) || md5(v_invoice_number), v_purchase_at + interval '1 day', v_subject_id,
                    'Gói đồng hành 8 buổi', 8, 800, 60, 10.00, 1, 0, v_purchase_at);
            INSERT INTO payment_transactions (id, invoice_id, provider, provider_reference, order_code, amount_vnd, transaction_datetime, raw_payload, signature_valid, processed_at, created_at)
            VALUES (gen_random_uuid(), v_invoice_id, 'PAYOS', 'seed-payment-' || v_pair_no::text, 900000000 + v_pair_no, 1600000,
                    v_purchase_at + interval '1 day', jsonb_build_object('demo', true, 'pair', v_pair_no), true,
                    v_purchase_at + interval '1 day', v_purchase_at + interval '1 day');
            INSERT INTO student_packages (id, pricing_package_id, student_id, teacher_id, subject_id, invoice_id, package_name_snapshot, total_sessions, remaining_sessions, reserved_sessions, completed_sessions, refunded_sessions, purchase_price_vnd, commission_rate, starts_at, expires_at, status, created_at, session_duration_minutes_snapshot)
            VALUES (v_student_package_id, v_pricing_package_id, s.student_id, t.teacher_id, v_subject_id, v_invoice_id,
                    'Gói đồng hành 8 buổi', 8, 3, 2, 3, 0, 1600000, 10.00, v_purchase_at + interval '2 days',
                    v_purchase_at + interval '802 days', 'ACTIVE', v_purchase_at + interval '1 day', 60);

            -- Mirror the paid package and every released session in the wallet ledger.
            UPDATE wallets SET pending_balance_vnd = pending_balance_vnd + v_teacher_net, version = version + 1 WHERE teacher_id = t.teacher_id;
            INSERT INTO ledger_entries (id, wallet_id, entry_type, amount_vnd, balance_bucket, direction, reference_type, reference_id, idempotency_key, description, created_at)
            SELECT gen_random_uuid(), w.id, 'PACKAGE_FUNDED', v_teacher_net, 'PENDING', 'CREDIT', 'INVOICE', v_invoice_id,
                   'WH-PAY-' || v_invoice_id::text, 'Ghi nhận học phí gói demo', v_purchase_at + interval '1 day'
            FROM wallets w WHERE w.teacher_id = t.teacher_id;

            FOR v_session_no IN 1..3 LOOP
                v_booking_id := gen_random_uuid();
                v_report_id := gen_random_uuid();
                v_start_at := now() - make_interval(days => 30 + (v_pair_no - 1) * 3 + v_session_no) + interval '18 hours';
                v_end_at := v_start_at + interval '1 hour';
                INSERT INTO bookings (id, teacher_id, student_id, subject_id, student_package_id, start_time, end_time, delivery_mode, meeting_link, status, is_trial, completed_at, settlement_processed, created_at)
                VALUES (v_booking_id, t.teacher_id, s.student_id, v_subject_id, v_student_package_id, v_start_at, v_end_at, 'ONLINE',
                        'https://meet.example.invalid/lesson-' || v_pair_no::text || '-' || v_session_no::text, 'COMPLETED', false, v_end_at, true, v_start_at - interval '2 days');
                INSERT INTO session_reports (id, booking_id, content, feedback, follow_up_note, teacher_self_rating, submitted_at, created_at)
                VALUES (v_report_id, v_booking_id, format('Buổi %s: học theo mục tiêu cá nhân môn %s.', v_session_no, v_subject_name),
                        'Học sinh hoàn thành phần nội dung chính và đặt câu hỏi phù hợp.', 'Ôn tập ghi chú và chuẩn bị bài tiếp theo.', (4 + (v_session_no % 2))::smallint, v_end_at + interval '30 minutes', v_end_at + interval '30 minutes');
                INSERT INTO booking_settlements (id, booking_id, status, teacher_confirmed_at, student_confirmed_at, initial_deadline, net_amount_vnd, version, created_at, updated_at)
                VALUES (gen_random_uuid(), v_booking_id, 'RELEASED', v_end_at + interval '30 minutes', v_end_at + interval '90 minutes', v_end_at + interval '24 hours', v_session_net, 1, v_end_at + interval '30 minutes', v_end_at + interval '90 minutes');
                UPDATE wallets SET pending_balance_vnd = pending_balance_vnd - v_session_net,
                                   available_balance_vnd = available_balance_vnd + v_session_net,
                                   version = version + 1 WHERE teacher_id = t.teacher_id;
                INSERT INTO ledger_entries (id, wallet_id, entry_type, amount_vnd, balance_bucket, direction, reference_type, reference_id, idempotency_key, description, created_at)
                SELECT gen_random_uuid(), w.id, 'SESSION_RELEASE_PENDING', v_session_net, 'PENDING', 'DEBIT', 'BOOKING', v_booking_id,
                       'booking:' || v_booking_id::text || ':settlement:pending', 'Giải phóng tiền chờ của buổi demo', v_end_at + interval '90 minutes'
                FROM wallets w WHERE w.teacher_id = t.teacher_id;
                INSERT INTO ledger_entries (id, wallet_id, entry_type, amount_vnd, balance_bucket, direction, reference_type, reference_id, idempotency_key, description, created_at)
                SELECT gen_random_uuid(), w.id, 'SESSION_CREDIT_AVAILABLE', v_session_net, 'AVAILABLE', 'CREDIT', 'BOOKING', v_booking_id,
                       'booking:' || v_booking_id::text || ':settlement:available', 'Ghi nhận thu nhập khả dụng của buổi demo', v_end_at + interval '90 minutes'
                FROM wallets w WHERE w.teacher_id = t.teacher_id;

                v_rating := CASE WHEN (v_pair_no + v_session_no) % 4 = 0 THEN 4 ELSE 5 END;
                INSERT INTO reviews (id, booking_id, student_id, teacher_id, rating, comment, is_visible, created_at)
                VALUES (gen_random_uuid(), v_booking_id, s.student_id, t.teacher_id, v_rating,
                        CASE WHEN v_rating = 5 THEN 'Bài giảng dễ hiểu, theo sát mục tiêu học tập.' ELSE 'Buổi học hữu ích, có thêm nội dung để tiếp tục cải thiện.' END,
                        true, v_end_at + interval '2 hours');
            END LOOP;

            FOR v_future_no IN 1..2 LOOP
                v_booking_id := gen_random_uuid();
                v_start_at := now() + make_interval(days => 20 + (v_pair_no - 1) * 2 + v_future_no) + interval '18 hours';
                v_end_at := v_start_at + interval '1 hour';
                INSERT INTO bookings (id, teacher_id, student_id, subject_id, student_package_id, start_time, end_time, delivery_mode, meeting_link, status, is_trial, settlement_processed)
                VALUES (v_booking_id, t.teacher_id, s.student_id, v_subject_id, v_student_package_id, v_start_at, v_end_at, 'ONLINE',
                        'https://meet.example.invalid/lesson-' || v_pair_no::text || '-' || (3 + v_future_no)::text, 'SCHEDULED', false, false);
                INSERT INTO booking_settlements (id, booking_id, status, initial_deadline, version)
                VALUES (gen_random_uuid(), v_booking_id, 'AWAITING_CONFIRMATION', v_end_at + interval '24 hours', 0);
            END LOOP;

            v_assignment_id := gen_random_uuid();
            v_submission_id := gen_random_uuid();
            INSERT INTO assignments (id, teacher_id, student_id, subject_id, title, assignment_type, content_blocks, due_at, status, version, created_at)
            VALUES (v_assignment_id, t.teacher_id, s.student_id, v_subject_id, 'Bài luyện tập theo lộ trình', 'FREEFORM',
                    jsonb_build_object('instructions', 'Hoàn thành phần bài tập theo nội dung vừa học.'), now() + interval '14 days', 'PUBLISHED', 0, now() - interval '3 days');
            INSERT INTO submissions (id, assignment_id, student_id, content_blocks, submitted_at, status, score, feedback_text, graded_at, version, created_at)
            VALUES (v_submission_id, v_assignment_id, s.student_id, jsonb_build_object('text', 'Em đã hoàn thành bài và ghi lại cách làm.'),
                    now() - interval '2 days', 'GRADED', 8.0 + ((v_pair_no % 21)::numeric / 10),
                    'Lời giải rõ ràng; xem lại các bước kiểm tra kết quả.', now() - interval '1 day', 1, now() - interval '2 days');
            INSERT INTO notifications (id, user_id, type, title, content, reference_type, reference_id, is_read, created_at)
            VALUES (gen_random_uuid(), s.student_id, 'ASSIGNMENT_GRADED', 'Bài tập đã được chấm', 'Gia sư đã gửi nhận xét cho bài tập của bạn.', 'SUBMISSION', v_submission_id, false, now() - interval '1 day');

            INSERT INTO conversations (id, teacher_id, student_id, last_message_at, created_at)
            VALUES (gen_random_uuid(), t.teacher_id, s.student_id, now() - interval '1 hour', now() - interval '30 days')
            ON CONFLICT (teacher_id, student_id) DO UPDATE SET last_message_at = EXCLUDED.last_message_at
            RETURNING id INTO v_conversation_id;
            INSERT INTO messages (id, conversation_id, sender_id, client_message_id, message_type, content, sent_at)
            VALUES (gen_random_uuid(), v_conversation_id, s.student_id, gen_random_uuid(), 'TEXT', 'Em muốn hỏi thêm phần bài tập của buổi học.', now() - interval '2 hours'),
                   (gen_random_uuid(), v_conversation_id, t.teacher_user_id, gen_random_uuid(), 'TEXT', 'Thầy/cô đã xem, mình cùng trao đổi thêm nhé.', now() - interval '1 hour');
        END LOOP;
    END LOOP;

    UPDATE teacher_stats ts
    SET average_rating = COALESCE(r.avg_rating, 0), review_count = COALESCE(r.review_count, 0),
        bayesian_rating = CASE WHEN COALESCE(r.review_count, 0) = 0 THEN 0
            ELSE round((r.review_count * r.avg_rating + 10 * COALESCE(global_stats.avg_rating, 0)) / (r.review_count + 10), 2) END,
        completed_session_count = b.completed_count,
        completion_rate = CASE WHEN b.total_count = 0 THEN 0 ELSE round(b.completed_count::numeric / b.total_count, 4) END,
        trial_session_count = b.trial_count, trial_conversion_rate = 0, calculated_at = now()
    FROM teacher_profiles tp
    LEFT JOIN LATERAL (SELECT round(avg(rv.rating)::numeric, 2) AS avg_rating, count(*)::integer AS review_count
                       FROM reviews rv WHERE rv.teacher_id = tp.id AND rv.is_visible AND rv.is_deleted = false) r ON true
    LEFT JOIN LATERAL (SELECT count(*) FILTER (WHERE bk.status = 'COMPLETED')::integer AS completed_count,
                              count(*)::integer AS total_count,
                              count(*) FILTER (WHERE bk.is_trial AND bk.status = 'COMPLETED')::integer AS trial_count
                       FROM bookings bk WHERE bk.teacher_id = tp.id AND bk.is_deleted = false) b ON true
    CROSS JOIN LATERAL (SELECT round(avg(rv.rating)::numeric, 2) AS avg_rating FROM reviews rv WHERE rv.is_visible AND rv.is_deleted = false) global_stats
    WHERE ts.teacher_id = tp.id;

    WITH ranked AS (
        SELECT teacher_id, row_number() OVER (ORDER BY bayesian_rating DESC, average_rating DESC, teacher_id)::integer AS rank_no
        FROM teacher_stats
    )
    UPDATE teacher_stats ts SET global_rank = ranked.rank_no FROM ranked WHERE ranked.teacher_id = ts.teacher_id;
END
$seed_full_marketplace$;

-- Match escrow ledger entries and wallet debits for admin queue settlements.
UPDATE booking_settlements SET net_amount_vnd = 270000 WHERE booking_id IN (
    '60000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000006',
    '60000000-0000-0000-0000-000000000007', '60000000-0000-0000-0000-000000000008',
    '60000000-0000-0000-0000-000000000011'
);
INSERT INTO ledger_entries (id, wallet_id, entry_type, amount_vnd, balance_bucket, direction, reference_type, reference_id, idempotency_key, description) VALUES
    ('21000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000007', 'SESSION_ESCROW_HELD', 270000, 'PENDING', 'DEBIT', 'BOOKING', '60000000-0000-0000-0000-000000000005', 'booking:60000000-0000-0000-0000-000000000005:escrow:pending', 'Tiền buổi học chuyển vào escrow'),
    ('21000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000007', 'SESSION_ESCROW_HELD', 270000, 'PENDING', 'DEBIT', 'BOOKING', '60000000-0000-0000-0000-000000000006', 'booking:60000000-0000-0000-0000-000000000006:escrow:pending', 'Tiền buổi học chuyển vào escrow'),
    ('21000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000007', 'SESSION_ESCROW_HELD', 270000, 'PENDING', 'DEBIT', 'BOOKING', '60000000-0000-0000-0000-000000000007', 'booking:60000000-0000-0000-0000-000000000007:escrow:pending', 'Tiền buổi học chuyển vào escrow'),
    ('21000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000007', 'SESSION_ESCROW_HELD', 270000, 'PENDING', 'DEBIT', 'BOOKING', '60000000-0000-0000-0000-000000000008', 'booking:60000000-0000-0000-0000-000000000008:escrow:pending', 'Tiền buổi học chuyển vào escrow'),
    ('21000000-0000-0000-0000-000000000014', '20000000-0000-0000-0000-000000000007', 'SESSION_ESCROW_HELD', 270000, 'PENDING', 'DEBIT', 'BOOKING', '60000000-0000-0000-0000-000000000011', 'booking:60000000-0000-0000-0000-000000000011:escrow:pending', 'Tiền buổi học chuyển vào escrow');
INSERT INTO platform_ledger_entries (id, booking_id, bucket, direction, amount_vnd, idempotency_key, description) VALUES
    ('1d000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000005', 'ESCROW', 'CREDIT', 270000, 'booking:60000000-0000-0000-0000-000000000005:escrow:credit', 'Giữ tiền buổi học chờ xác nhận'),
    ('1d000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000006', 'ESCROW', 'CREDIT', 270000, 'booking:60000000-0000-0000-0000-000000000006:escrow:credit', 'Giữ tiền buổi học đang tranh chấp'),
    ('1d000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000007', 'ESCROW', 'CREDIT', 270000, 'booking:60000000-0000-0000-0000-000000000007:escrow:credit', 'Giữ tiền buổi học đang mở lại'),
    ('1d000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000008', 'ESCROW', 'CREDIT', 270000, 'booking:60000000-0000-0000-0000-000000000008:escrow:credit', 'Giữ tiền buổi học chờ quyết định admin'),
    ('1d000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000011', 'ESCROW', 'CREDIT', 270000, 'booking:60000000-0000-0000-0000-000000000011:escrow:credit', 'Giữ tiền buổi học trước khi retain'),
    ('1d000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000011', 'ESCROW', 'DEBIT', 270000, 'booking:60000000-0000-0000-0000-000000000011:escrow:retain', 'Đóng nghĩa vụ escrow theo quyết định'),
    ('1d000000-0000-0000-0000-000000000007', '60000000-0000-0000-0000-000000000011', 'REVENUE', 'CREDIT', 270000, 'booking:60000000-0000-0000-0000-000000000011:revenue', 'Khoản retain theo quyết định admin');
