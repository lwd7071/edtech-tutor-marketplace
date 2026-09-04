/**
 * Booking Routes & Inter-Module Link Helpers
 * Phục vụ tích hợp giữa Booking với Learning (Assignments) và Chat (Messages)
 */

export function getStudentBookingsRoute(): string {
  return '/student/bookings';
}

export function getBookingDetailRoute(bookingId: string): string {
  return `/student/bookings?bookingId=${encodeURIComponent(bookingId)}`;
}

export function getChatRoute(participantId: string, role: 'STUDENT' | 'TEACHER' = 'STUDENT'): string {
  const prefix = role === 'TEACHER' ? '/teacher/messages' : '/student/messages';
  return `${prefix}?userId=${encodeURIComponent(participantId)}`;
}

export function getAssignmentsRoute(options?: { bookingId?: string; subjectId?: string }): string {
  const params = new URLSearchParams();
  if (options?.bookingId) {
    params.set('bookingId', options.bookingId);
  }
  if (options?.subjectId) {
    params.set('subjectId', options.subjectId);
  }
  const queryString = params.toString();
  return queryString ? `/student/assignments?${queryString}` : '/student/assignments';
}

export function getMeetingLink(meetingUrl?: string | null): string | null {
  if (!meetingUrl || typeof meetingUrl !== 'string') {
    return null;
  }
  const trimmed = meetingUrl.trim();
  if (!trimmed) {
    return null;
  }

  // Chặn các scheme nguy hiểm như javascript:, data:, vbscript:
  if (/^(javascript|data|vbscript):/i.test(trimmed)) {
    return null;
  }

  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }

  // Nếu người dùng nhập domain dạng meet.google.com/abc... thì tự thêm https://
  return `https://${trimmed}`;
}
