-- =====================================================================
-- V35: ENABLE ROW LEVEL SECURITY & REVOKE TRUNCATE ON BOOKING & LEARNING
-- =====================================================================

-- 1. Bật Row Level Security default-deny cho 9 bảng Booking & Learning
ALTER TABLE student_packages ENABLE ROW LEVEL SECURITY;
ALTER TABLE package_extension_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE trial_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE session_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE submissions ENABLE ROW LEVEL SECURITY;

-- 2. REVOKE TRUNCATE đối với unprivileged roles để triệt tiêu nguy cơ RLS bypass
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE TRUNCATE ON TABLE
            student_packages,
            package_extension_requests,
            trial_requests,
            bookings,
            session_reports,
            reviews,
            teacher_stats,
            assignments,
            submissions
        FROM anon;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE TRUNCATE ON TABLE
            student_packages,
            package_extension_requests,
            trial_requests,
            bookings,
            session_reports,
            reviews,
            teacher_stats,
            assignments,
            submissions
        FROM authenticated;
    END IF;
END $$;
