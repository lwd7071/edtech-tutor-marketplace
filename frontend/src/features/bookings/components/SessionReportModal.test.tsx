import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SessionReportModal } from './SessionReportModal';
import { BookingDetail } from '../types';

const mockCompleteBooking = jest.fn().mockResolvedValue({});

jest.mock('../hooks/useBookings', () => ({
  useCompleteBooking: jest.fn(() => ({
    mutateAsync: mockCompleteBooking,
    isPending: false,
  })),
}));

describe('SessionReportModal (TDD)', () => {
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

  it('should render modal with required fields for SessionReport', () => {
    const handleClose = jest.fn();
    render(<SessionReportModal open={true} booking={mockBooking} onClose={handleClose} />);

    expect(screen.getByText('Báo cáo Hoàn thành Buổi học')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Nhập tóm tắt nội dung đã dạy/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Nhận xét tinh thần, mức độ tiếp thu/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Hoàn thành & Gửi báo cáo/i })).toBeInTheDocument();
  });
});
