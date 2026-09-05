import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { UpcomingSessionCard } from './UpcomingSessionCard';
import { BookingDetail } from '../types';

const mockBooking: BookingDetail = {
  id: 'b-up-1',
  studentPackageId: 'pkg-1',
  student: { id: 's-1', fullName: 'Học sinh B' },
  teacher: { id: 't-1', fullName: 'Cô Lê Hoàng Mai' },
  subject: { id: 'sub-1', name: 'Vật lý 11' },
  startTime: new Date(Date.now() + 10 * 60 * 1000).toISOString(), // 10 phút nữa
  endTime: new Date(Date.now() + 70 * 60 * 1000).toISOString(),
  status: 'SCHEDULED',
  deliveryMode: 'ONLINE',
  meetingLink: 'https://meet.google.com/test-room',
  trial: false,
  outsideAvailabilityWarning: false,
  version: 1,
};

describe('UpcomingSessionCard Component (Task B6.3)', () => {
  it('should render skeleton when isLoading is true', () => {
    const { container } = render(<UpcomingSessionCard isLoading={true} />);
    expect(container.querySelector('.ant-skeleton')).toBeInTheDocument();
  });

  it('should return null if no booking is provided', () => {
    const { container } = render(<UpcomingSessionCard booking={null} />);
    expect(container.firstChild).toBeNull();
  });

  it('should render upcoming booking details correctly', () => {
    render(<UpcomingSessionCard booking={mockBooking} />);

    expect(screen.getByText('Vật lý 11')).toBeInTheDocument();
    expect(screen.getByText('Cô Lê Hoàng Mai')).toBeInTheDocument();
    expect(screen.getByText(/Vào phòng học/i)).toBeInTheDocument();
    expect(screen.getByText(/Nhắn tin/i)).toBeInTheDocument();
    expect(screen.getByText(/Xem chi tiết/i)).toBeInTheDocument();
  });

  it('should trigger callback when buttons are clicked', () => {
    const handleViewDetail = jest.fn();
    const handleChat = jest.fn();

    render(
      <UpcomingSessionCard
        booking={mockBooking}
        onViewDetail={handleViewDetail}
        onChat={handleChat}
      />
    );

    const chatBtn = screen.getByText(/Nhắn tin/i);
    fireEvent.click(chatBtn);
    expect(handleChat).toHaveBeenCalledWith('t-1');

    const detailBtn = screen.getByText(/Xem chi tiết/i);
    fireEvent.click(detailBtn);
    expect(handleViewDetail).toHaveBeenCalledWith(mockBooking);
  });
});
