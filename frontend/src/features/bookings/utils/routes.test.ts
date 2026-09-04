import {
  getStudentBookingsRoute,
  getBookingDetailRoute,
  getChatRoute,
  getAssignmentsRoute,
  getMeetingLink,
} from './routes';

describe('Booking Routes & Inter-Module Links (Task B6.1)', () => {
  it('should return correct booking routes', () => {
    expect(getStudentBookingsRoute()).toBe('/student/bookings');
    expect(getBookingDetailRoute('b123')).toBe('/student/bookings?bookingId=b123');
  });

  it('should return correct chat route based on role', () => {
    expect(getChatRoute('teacher-456', 'STUDENT')).toBe('/student/messages?userId=teacher-456');
    expect(getChatRoute('student-789', 'TEACHER')).toBe('/teacher/messages?userId=student-789');
    expect(getChatRoute('user-101')).toBe('/student/messages?userId=user-101');
  });

  it('should return correct assignments route', () => {
    expect(getAssignmentsRoute()).toBe('/student/assignments');
    expect(getAssignmentsRoute({ bookingId: 'b-999' })).toBe('/student/assignments?bookingId=b-999');
    expect(getAssignmentsRoute({ subjectId: 's-111' })).toBe('/student/assignments?subjectId=s-111');
    expect(getAssignmentsRoute({ bookingId: 'b-999', subjectId: 's-111' })).toBe(
      '/student/assignments?bookingId=b-999&subjectId=s-111'
    );
  });

  it('should sanitize and return safe meeting links', () => {
    expect(getMeetingLink(undefined)).toBeNull();
    expect(getMeetingLink('')).toBeNull();
    expect(getMeetingLink('   ')).toBeNull();
    expect(getMeetingLink('https://meet.google.com/abc-def-ghi')).toBe('https://meet.google.com/abc-def-ghi');
    expect(getMeetingLink('meet.google.com/abc-def-ghi')).toBe('https://meet.google.com/abc-def-ghi');
    expect(getMeetingLink('javascript:alert(1)')).toBeNull();
  });
});
