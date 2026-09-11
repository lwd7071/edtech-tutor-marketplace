INSERT INTO conversations (id, teacher_id, student_id, created_at, updated_at, is_deleted)
SELECT gen_random_uuid(), relation.teacher_id, relation.student_id, now(), now(), false
FROM (
    SELECT teacher_id, student_id FROM student_packages WHERE is_deleted=false
    UNION
    SELECT teacher_id, student_id FROM bookings WHERE is_deleted=false
    UNION
    SELECT teacher_id, student_id FROM trial_requests WHERE is_deleted=false AND status='ACCEPTED'
) relation
ON CONFLICT (teacher_id, student_id) DO NOTHING;
