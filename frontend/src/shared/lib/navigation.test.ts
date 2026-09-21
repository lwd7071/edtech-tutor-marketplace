import { isWorkspaceLinkActive, roleHome, safeReturnTo } from './navigation';

describe('workspace navigation', () => {
  it('keeps role homes and safe internal redirects stable', () => {
    expect(roleHome('STUDENT')).toBe('/student');
    expect(roleHome('TEACHER')).toBe('/teacher');
    expect(roleHome('ADMIN')).toBe('/admin');
    expect(safeReturnTo('/student/bookings?status=SCHEDULED', 'STUDENT')).toBe('/student/bookings?status=SCHEDULED');
    expect(safeReturnTo('/student/bookings?status=SCHEDULED', 'TEACHER')).toBe('/teacher');
    expect(safeReturnTo('/student/bookings?redirect=https://evil.test&status=SCHEDULED', 'STUDENT')).toBe('/student/bookings?status=SCHEDULED');
    expect(safeReturnTo('/teachers/t1?packageId=p1&redirect=https://evil.test#packages', 'STUDENT')).toBe('/teachers/t1?packageId=p1#packages');
    expect(safeReturnTo('/admin', 'ADMIN')).toBe('/admin');
    expect(safeReturnTo('//outside.test', 'STUDENT')).toBe('/student');
  });

  it('matches detail pages to their most specific navigation item', () => {
    expect(isWorkspaceLinkActive('/student/packages/package-1', '/student/packages')).toBe(true);
    expect(isWorkspaceLinkActive('/student/packages/package-1', '/student')).toBe(false);
    expect(isWorkspaceLinkActive('/teacher/bookings', '/teacher/bookings')).toBe(true);
    expect(isWorkspaceLinkActive('/teacher/bookings-old', '/teacher/bookings')).toBe(false);
  });
});
