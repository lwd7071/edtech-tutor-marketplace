import type { UserRole } from '@/features/auth';
export const roleHome = (role?: UserRole) => role === 'ADMIN' ? '/admin' : role === 'TEACHER' ? '/teacher' : '/student';
const rolePrefixes: Record<UserRole, string> = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  ADMIN: '/admin',
};

const publicReturnPaths = ['/', '/teachers', '/subjects', '/ranking', '/how-it-works', '/become-a-tutor', '/terms', '/privacy', '/support'];

function isRolePath(pathname: string, role: UserRole) {
  const prefix = rolePrefixes[role];
  return pathname === prefix || pathname.startsWith(`${prefix}/`);
}

function isPublicPath(pathname: string) {
  return publicReturnPaths.some((path) => pathname === path || pathname.startsWith(`${path}/`));
}

function filteredReturnQuery(url: URL) {
  const params = new URLSearchParams();
  const allowedByPath: Record<string, string[]> = {
    '/student/bookings': ['status'],
    '/student/payments/callback': ['invoiceId'],
  };
  const allowed = allowedByPath[url.pathname] ?? (url.pathname.startsWith('/teachers/') ? ['packageId'] : []);
  for (const key of allowed) {
    const value = url.searchParams.get(key);
    if (value) params.set(key, value);
  }
  return params.toString();
}

export function safeReturnTo(value: string | null, role: UserRole, fallback = roleHome(role)) {
  if (!value || value.includes('\\') || /[\u0000-\u001f\u007f]/.test(value)) return fallback;
  try {
    const url = new URL(value, window.location.origin);
    if (url.origin !== window.location.origin || !url.pathname.startsWith('/') || url.pathname.startsWith('//')) return fallback;
    if (!isRolePath(url.pathname, role) && !isPublicPath(url.pathname)) return fallback;
    const query = filteredReturnQuery(url);
    const hash = url.pathname.startsWith('/teachers/') && ['#packages', '#trial'].includes(url.hash) ? url.hash : '';
    return `${url.pathname}${query ? `?${query}` : ''}${hash}`;
  } catch {
    return fallback;
  }
}
export interface WorkspaceLink { href: string; label: string }
export interface WorkspaceGroup { label: string; items: WorkspaceLink[] }

export const teacherWorkspaceGroups: WorkspaceGroup[] = [
  { label: 'Tổng quan', items: [{ href: '/teacher', label: 'Tổng quan' }] },
  { label: 'Hồ sơ', items: [
    { href: '/teacher/profile', label: 'Hồ sơ gia sư' },
    { href: '/teacher/documents', label: 'Xác minh danh tính' },
    { href: '/teacher/credentials', label: 'Minh chứng năng lực' },
    { href: '/teacher/subjects', label: 'Môn đang dạy' },
    { href: '/teacher/subject-proposals', label: 'Đề xuất môn học' },
  ] },
  { label: 'Giảng dạy', items: [
    { href: '/teacher/packages', label: 'Gói học' },
    { href: '/teacher/availability', label: 'Lịch rảnh' },
    { href: '/teacher/students', label: 'Học viên' },
    { href: '/teacher/bookings', label: 'Lịch dạy' },
    { href: '/teacher/trial-requests', label: 'Yêu cầu học thử' },
  ] },
  { label: 'Học tập', items: [{ href: '/teacher/assignments', label: 'Bài tập' }] },
  { label: 'Tài chính', items: [
    { href: '/teacher/wallet', label: 'Ví thu nhập' },
    { href: '/teacher/bank-accounts', label: 'Ngân hàng' },
    { href: '/teacher/payouts', label: 'Yêu cầu rút tiền' },
    { href: '/teacher/stats', label: 'Thống kê' },
  ] },
  { label: 'Giao tiếp', items: [
    { href: '/teacher/messages', label: 'Tin nhắn' },
    { href: '/teacher/notifications', label: 'Thông báo' },
  ] },
];

export const workspaceLinks: Record<UserRole, WorkspaceLink[]> = {
 STUDENT: [
 ['/student','Tổng quan'], ['/student/packages','Gói học'], ['/student/bookings','Lịch học'],
 ['/student/assignments','Bài tập'], ['/student/messages','Tin nhắn'], ['/student/requests','Yêu cầu của tôi'],
 ['/student/session-reports','Báo cáo buổi học'],
 ['/student/notifications','Thông báo'], ['/student/profile','Hồ sơ'],
 ].map(([href,label]) => ({href,label})),
 TEACHER: teacherWorkspaceGroups.flatMap(group => group.items),
 ADMIN: [
 ['/admin','Tổng quan'], ['/admin/teachers','Duyệt gia sư'], ['/admin/subjects','Đề xuất môn học'],
 ['/admin/users','Quản lý người dùng'],
 ['/admin/refunds','Hoàn tiền'], ['/admin/extensions','Gia hạn'], ['/admin/payouts','Rút tiền'], ['/admin/booking-settlements','Quyết toán buổi học'], ['/admin/credentials','Duyệt chứng chỉ'],
 ['/admin/settings','Cài đặt'], ['/admin/audit-logs','Nhật ký hoạt động'],
 ].map(([href,label]) => ({href,label})),
};

export function isWorkspaceLinkActive(pathname: string, href: string): boolean {
  if (pathname === href) return true;
  return href.split('/').length > 2 && pathname.startsWith(`${href}/`);
}

