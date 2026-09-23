import { render, screen } from '@testing-library/react';
import { BookingDetailDrawer } from './BookingDetailDrawer';
import type { BookingDetail } from '../types';

const booking = (overrides: Partial<BookingDetail> = {}): BookingDetail => ({
  id: 'booking-1',
  teacher: { id: 'teacher-1', fullName: 'Trần Thu Hà' },
  student: { id: 'student-1', fullName: 'Nguyễn Minh An' },
  studentPackageId: 'package-1',
  subject: { id: 'subject-1', name: 'Toán' },
  startTime: '2026-09-22T12:00:00Z',
  endTime: '2026-09-22T13:00:00Z',
  deliveryMode: 'ONLINE',
  status: 'CANCELLED',
  trial: false,
  outsideAvailabilityWarning: false,
  version: 0,
  ...overrides,
});

describe('BookingDetailDrawer cancellation reason', () => {
  it('shows the full cancellation reason with preserved line breaks and safe text rendering', () => {
    const reason = `Dòng 1\n${'<script>alert(1)</script>'}${'ă'.repeat(1000)}`;
    render(<BookingDetailDrawer open booking={booking({ cancelReason: reason })} onClose={jest.fn()} />);

    expect(screen.getByText('Lý do hủy')).toBeInTheDocument();
    const reasonText = screen.getByRole('alert').querySelector('div[style]') as HTMLElement;
    expect(reasonText.textContent).toBe(reason);
    expect(reasonText).toHaveStyle({ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' });
    expect(document.querySelector('script')).not.toBeInTheDocument();
  });

  it.each([
    ['null reason', null, 'CANCELLED'],
    ['blank reason', '  ', 'CANCELLED'],
    ['non-cancelled booking', 'reason', 'SCHEDULED'],
  ] as const)('does not show an alert for %s', (_case, cancelReason, status) => {
    render(<BookingDetailDrawer open booking={booking({ cancelReason, status })} onClose={jest.fn()} />);
    expect(screen.queryByText('Lý do hủy')).not.toBeInTheDocument();
  });
});
