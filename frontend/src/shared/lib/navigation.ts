import type { UserRole } from '@/features/auth';
export const roleHome = (role?: UserRole) => role === 'ADMIN' ? '/admin' : role === 'TEACHER' ? '/teacher' : '/student';
export function safeReturnTo(value: string | null, fallback: string) {
  return value && value.startsWith('/') && !value.startsWith('//') && !value.includes('\\') ? value : fallback;
}
export const workspaceLinks: Record<UserRole, { href: string; label: string }[]> = {
 STUDENT: [
 ['/student','Tổng quan'], ['/student/packages','Gói học'], ['/student/bookings','Lịch học'],
 ['/student/assignments','Bài tập'], ['/student/messages','Tin nhắn'], ['/student/requests','Yêu cầu của tôi'],
 ['/student/notifications','Thông báo'], ['/student/profile','Hồ sơ'],
 ].map(([href,label]) => ({href,label})),
 TEACHER: [
 ['/teacher','Tổng quan'], ['/teacher/bookings','Lịch dạy'], ['/teacher/students','Học viên'], ['/teacher/trial-requests','Yêu cầu học thử'],
 ['/teacher/profile','Hồ sơ gia sư'], ['/teacher/subjects','Môn đang dạy'], ['/teacher/documents','Tài liệu xác minh'],
 ['/teacher/subject-proposals','Đề xuất môn học'], ['/teacher/packages','Gói học'], ['/teacher/availability','Lịch rảnh'],
 ['/teacher/assignments','Bài tập'], ['/teacher/wallet','Ví thu nhập'], ['/teacher/payouts','Yêu cầu rút tiền'],
 ['/teacher/bank-accounts','Ngân hàng'], ['/teacher/stats','Thống kê'], ['/teacher/messages','Tin nhắn'], ['/teacher/notifications','Thông báo'],
 ].map(([href,label]) => ({href,label})),
 ADMIN: [
 ['/admin','Tổng quan'], ['/admin/teachers','Duyệt gia sư'], ['/admin/subjects','Đề xuất môn học'],
 ['/admin/refunds','Hoàn tiền'], ['/admin/extensions','Gia hạn'], ['/admin/payouts','Rút tiền'],
 ['/admin/settings','Cài đặt'], ['/admin/audit-logs','Nhật ký hoạt động'],
 ].map(([href,label]) => ({href,label})),
};

export function isWorkspaceLinkActive(pathname: string, href: string): boolean {
  if (pathname === href) return true;
  return href.split('/').length > 2 && pathname.startsWith(`${href}/`);
}

