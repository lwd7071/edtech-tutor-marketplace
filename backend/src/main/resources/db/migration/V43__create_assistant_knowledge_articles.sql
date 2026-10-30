CREATE TABLE assistant_knowledge_articles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    slug varchar(100) NOT NULL UNIQUE,
    title varchar(180) NOT NULL,
    content text NOT NULL,
    aliases text[] NOT NULL DEFAULT '{}',
    source_path varchar(255) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    is_published boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_assistant_knowledge_slug CHECK (slug ~ '^[a-z0-9-]+$'),
    CONSTRAINT ck_assistant_knowledge_source_path CHECK (source_path LIKE '/%' AND source_path NOT LIKE '//%'),
    CONSTRAINT ck_assistant_knowledge_content CHECK (length(btrim(title)) > 0 AND length(btrim(content)) > 0)
);

CREATE INDEX ix_assistant_knowledge_published_order
    ON assistant_knowledge_articles (sort_order, slug)
    WHERE is_published = true;
CREATE TRIGGER trg_assistant_knowledge_updated
    BEFORE UPDATE ON assistant_knowledge_articles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE assistant_knowledge_articles ENABLE ROW LEVEL SECURITY;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL ON TABLE assistant_knowledge_articles FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON TABLE assistant_knowledge_articles FROM authenticated;
    END IF;
END $$;

INSERT INTO assistant_knowledge_articles (slug, title, content, aliases, source_path, sort_order, is_published) VALUES
('getting-started', 'Hướng dẫn sử dụng Tutor Match',
 'Tutor Match kết nối học viên với gia sư dạy 1-1. Học viên có thể tìm và lọc gia sư theo môn học, hình thức học và mức giá, rồi mở hồ sơ để xem thông tin công khai. Bạn có thể gửi yêu cầu học thử hoặc mua gói học. Sau khi mua gói, gửi yêu cầu đặt buổi; gia sư xác nhận giờ học. Lịch học, bài tập và gói đã mua được theo dõi trong không gian học viên.',
 ARRAY['hướng dẫn sử dụng', 'hướng dẫn sử dụng web', 'hướng dẫn sử dụng website', 'cách dùng', 'cách sử dụng tutor match', 'dùng website tutor match', 'web dùng sao', 'bắt đầu sử dụng', 'tutor match hoạt động', 'xin chào', 'chào bạn', 'hello', 'hi'],
 '/how-it-works', 10, true),
('find-a-tutor', 'Cách tìm gia sư',
 'Mở trang Tìm gia sư để lọc theo môn học, hình thức học và mức giá. Mở hồ sơ công khai để xem môn dạy, kinh nghiệm, đánh giá và các gói học đang bán. Dữ liệu trong chatbot chỉ hiển thị các gia sư đã được duyệt và đang công khai.',
 ARRAY['tìm gia sư', 'tìm giáo viên', 'tìm người dạy', 'gia sư', 'có gia sư môn', 'gia sư dạy môn', 'giáo viên dạy môn', 'lọc gia sư', 'xem hồ sơ gia sư'],
 '/teachers', 20, true),
('trial-and-scheduling', 'Học thử và đặt lịch',
 'Học viên có thể gửi yêu cầu học thử để gia sư phản hồi. Yêu cầu học thử chưa phải buổi học đã xác nhận. Với gói đã mua, học viên gửi yêu cầu đặt buổi; gia sư xác nhận thời gian cụ thể. Hãy theo dõi trạng thái yêu cầu và lịch trong tài khoản học viên.',
 ARRAY['học thử', 'đăng ký học thử', 'gửi yêu cầu học thử', 'đặt lịch', 'đặt buổi học', 'lịch học', 'mua gói học', 'thanh toán gói học'],
 '/how-it-works', 30, true),
('become-a-tutor', 'Đăng ký trở thành gia sư',
 'Gia sư tạo hồ sơ, giới thiệu môn giảng dạy và kinh nghiệm, rồi gửi hồ sơ để xét duyệt. Sau khi hồ sơ được duyệt, gia sư có thể tạo gói học, cập nhật lịch rảnh và phản hồi yêu cầu học thử.',
 ARRAY['đăng ký gia sư', 'trở thành gia sư', 'làm gia sư', 'tạo hồ sơ gia sư', 'duyệt hồ sơ gia sư', 'tạo gói học gia sư', 'cập nhật lịch rảnh'],
 '/become-a-tutor', 40, true),
('student-support', 'Các mục hỗ trợ học viên',
 'Sau khi đăng nhập, học viên có thể xem yêu cầu học thử, hoàn tiền hoặc gia hạn trong mục Yêu cầu; theo dõi thông báo; trao đổi với gia sư trong Tin nhắn; và xem bài tập, báo cáo buổi học trong không gian học tập. Nếu cần hỗ trợ ngoài các hướng dẫn này, liên hệ admin qua luongvietvidong@gmail.com.',
 ARRAY['trung tâm hỗ trợ', 'hỗ trợ học viên', 'xem yêu cầu', 'hoàn tiền', 'gia hạn', 'thông báo', 'tin nhắn với gia sư', 'bài tập', 'báo cáo buổi học'],
 '/support', 50, true);
