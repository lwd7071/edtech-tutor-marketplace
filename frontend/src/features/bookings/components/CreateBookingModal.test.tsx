import React from 'react';
import { render, screen } from '@testing-library/react';
import { CreateBookingModal } from './CreateBookingModal';

const mockCreateBooking = jest.fn().mockResolvedValue({});

jest.mock('../hooks/useBookings', () => ({
  useCreateBooking: jest.fn(() => ({
    mutateAsync: mockCreateBooking,
    isPending: false,
  })),
}));

describe('CreateBookingModal (TDD)', () => {
  it('should render create booking modal with package id, time picker and delivery options', () => {
    const handleClose = jest.fn();
    render(
      <CreateBookingModal
        open={true}
        studentPackageId="pkg-123"
        onClose={handleClose}
      />
    );

    expect(screen.getByText('Đặt lịch buổi học mới')).toBeInTheDocument();
    expect(screen.getByText('Trực tuyến (ONLINE)')).toBeInTheDocument();
    expect(screen.getByText('Trực tiếp (OFFLINE)')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Xác nhận tạo lịch học/i })).toBeInTheDocument();
  });
});
