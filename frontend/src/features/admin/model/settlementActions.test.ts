import { availableSettlementActions } from './settlementActions';
import type { BookingSettlementAdminView } from '../types/finance';

const row = (status: string, overrides: Partial<BookingSettlementAdminView> = {}): BookingSettlementAdminView =>
  ({ bookingId: 'booking-1', studentId: 'student-1', studentName: 'Student', teacherId: 'teacher-1',
    teacherName: 'Teacher', bookingStatus: 'COMPLETED', startTime: '2026-09-22T12:00:00Z',
    endTime: '2026-09-22T13:00:00Z', confirmationDeadline: '2026-09-23T13:00:00Z',
    status, version: 2, ...overrides });

describe('admin settlement actions', () => {
  const now = Date.parse('2026-09-22T12:00:00Z');
  it('permits reopening or retaining a pending dispute', () => {
    expect(availableSettlementActions(row('DISPUTE_PENDING'), now)).toEqual(['reopen', 'retain']);
  });
  it('permits decision after the reopened confirmation window', () => {
    expect(availableSettlementActions(row('REOPENED', { reopenDeadline: '2026-09-22T11:00:00Z' }), now)).toEqual(['release', 'retain']);
    expect(availableSettlementActions(row('REOPENED', { reopenDeadline: '2026-09-22T13:00:00Z' }), now)).toEqual([]);
    expect(availableSettlementActions(row('REOPENED', { reopenDeadline: '2026-09-22T11:00:00Z', teacherConfirmedAt: 'x', studentConfirmedAt: 'y' }), now)).toEqual([]);
  });
  it('permits decisions only for pending admin review and not terminal states', () => {
    expect(availableSettlementActions(row('AWAITING_ADMIN_DECISION'), now)).toEqual(['release', 'retain']);
    expect(availableSettlementActions(row('RELEASED'), now)).toEqual([]);
  });
});
