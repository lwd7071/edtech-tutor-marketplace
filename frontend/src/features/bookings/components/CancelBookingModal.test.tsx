import React from 'react';
import { render, screen } from '@testing-library/react';
import { CancelBookingModal } from './CancelBookingModal';
import { BookingDetail } from '../types';

const mockCancelBooking = jest.fn().mockResolvedValue({});

jest.mock('../hooks/useBookings', () => ({
  useCancelBooking: jest.fn(() => ({
    mutateAsync: mockCancelBooking,
    isPending: false,
  })),
}));

describe('CancelBookingModal (TDD)', () => {
  const mockBooking: BookingDetail = {
    id: 'b-1',
    teacher: { id: 't-1', fullName: 'Trần Thu Hà' },
    student: { id: 's-1', fullName: 'Nguyễn Minh An' },
    studentPackageId: 'pkg-1',
    subject: { id: 'sub-1', name: 'Toán 11' },
    startTime: '2026-08-20T19:00:00Z',
    endTime: '2026-08-20T20:30:00Z',
    deliveryMode: 'ONLINE',
    status: 'SCHEDULED',
    trial: false,
    outsideAvailabilityWarning: false,
    version: 1,
  };

  it('should render modal with cancellation reason and initiator options', () => {
    const handleClose = jest.fn();
    render(<CancelBookingModal open={true} booking={mockBooking} onClose={handleClose} />);

    expect(screen.getByText('Hủy lịch học')).toBeInTheDocument();
    expect(screen.getByText('Học sinh yêu cầu đổi/hủy lịch')).toBeInTheDocument();
    expect(screen.getByText('Giáo viên có việc đột xuất')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Nhập lý do hủy lịch/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Xác nhận hủy buổi học/i })).toBeInTheDocument();
  });
});
