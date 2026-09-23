import { fireEvent, render, screen } from '@testing-library/react';
import { AdminBookingSettlementsPage } from './AdminBookingSettlementsPage';

const mockUseAdminBookingSettlements = jest.fn();
const mockMutateAsync = jest.fn();
jest.mock('../hooks/useAdminFinance', () => ({
  useAdminBookingSettlements: (...args: unknown[]) => mockUseAdminBookingSettlements(...args),
  useBookingSettlementAction: () => ({ mutateAsync: mockMutateAsync, isPending: false }),
}));

const row = (overrides: Record<string, unknown> = {}) => ({
  bookingId: 'c4436e6a-d11a-4f54-931f-61798de4b66e',
  studentId: '7f9a5cce-29de-4eb9-b07e-259d0b216eef', studentName: 'Nguyễn Minh An',
  teacherId: '752169a4-cc2f-455f-a567-f55399eeb8f8', teacherName: 'Trần Thu Hà',
  bookingStatus: 'COMPLETED', startTime: '2026-09-22T12:00:00Z', endTime: '2026-09-22T13:00:00Z',
  status: 'DISPUTE_PENDING', teacherConfirmedAt: '2026-09-22T13:05:00Z', studentConfirmedAt: null,
  confirmationDeadline: '2026-09-23T13:00:00Z', reopenDeadline: null, netAmountVnd: 180000,
  disputeReason: 'Học viên chưa xác nhận', disputedAt: '2026-09-24T08:00:00Z', version: 2,
  ...overrides,
});

describe('AdminBookingSettlementsPage', () => {
  beforeEach(() => mockUseAdminBookingSettlements.mockReturnValue({ data: { data: [row()], meta: { totalElements: 1 } }, isLoading: false, isError: false, refetch: jest.fn() }));

  it('shows decision context in details before allowing an action', () => {
    render(<AdminBookingSettlementsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Xem chi tiết' }));
    expect(screen.getAllByText('Nguyễn Minh An')).toHaveLength(2);
    expect(screen.getAllByText('Trần Thu Hà')).toHaveLength(2);
    expect(screen.getByText('Học viên chưa xác nhận')).toBeInTheDocument();
    expect(screen.getByText('Hạn xác nhận ban đầu')).toBeInTheDocument();
    expect(screen.getAllByText(/c4436e6a…b66e/)).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Mở lại' })).toBeInTheDocument();
  });

  it('blocks decision actions when a dispute has no recorded reason', () => {
    mockUseAdminBookingSettlements.mockReturnValue({ data: { data: [row({ disputeReason: null })], meta: { totalElements: 1 } }, isLoading: false, isError: false, refetch: jest.fn() });
    render(<AdminBookingSettlementsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Xem chi tiết' }));
    expect(screen.getByText('Không có lý do khiếu nại được ghi nhận.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mở lại' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Giữ nền tảng' })).not.toBeInTheDocument();
  });
});
