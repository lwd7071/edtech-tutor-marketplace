import React from 'react';
import { renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useUpcomingBooking, getUpcomingBookingFromList } from './useUpcomingBooking';
import { BookingDetail } from '../types';

const mockBookings: BookingDetail[] = [
  {
    id: 'b-past',
    studentPackageId: 'pkg-1',
    student: { id: 's-1', fullName: 'Nguyễn Minh An' },
    teacher: { id: 't-1', fullName: 'Thầy Nguyễn Văn A' },
    subject: { id: 'sub-1', name: 'Toán học 12' },
    startTime: '2026-09-01T08:00:00Z',
    endTime: '2026-09-01T09:30:00Z',
    status: 'COMPLETED',
    deliveryMode: 'ONLINE',
    meetingLink: 'https://meet.google.com/past',
    trial: false,
    outsideAvailabilityWarning: false,
    version: 1,
  },
  {
    id: 'b-next',
    studentPackageId: 'pkg-1',
    student: { id: 's-1', fullName: 'Nguyễn Minh An' },
    teacher: { id: 't-1', fullName: 'Thầy Nguyễn Văn A' },
    subject: { id: 'sub-1', name: 'Toán học 12' },
    // 30 phút nữa
    startTime: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
    endTime: new Date(Date.now() + 90 * 60 * 1000).toISOString(),
    status: 'SCHEDULED',
    deliveryMode: 'ONLINE',
    meetingLink: 'https://meet.google.com/next',
    trial: false,
    outsideAvailabilityWarning: false,
    version: 1,
  },
  {
    id: 'b-later',
    studentPackageId: 'pkg-1',
    student: { id: 's-1', fullName: 'Nguyễn Minh An' },
    teacher: { id: 't-1', fullName: 'Thầy Nguyễn Văn A' },
    subject: { id: 'sub-1', name: 'Toán học 12' },
    // 2 ngày nữa
    startTime: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
    endTime: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000 + 60 * 60 * 1000).toISOString(),
    status: 'SCHEDULED',
    deliveryMode: 'ONLINE',
    meetingLink: 'https://meet.google.com/later',
    trial: false,
    outsideAvailabilityWarning: false,
    version: 1,
  },
];

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return Wrapper;
};

describe('useUpcomingBooking Hook (Task B6.2)', () => {
  it('should find the earliest confirmed upcoming booking', () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useUpcomingBooking({ bookings: mockBookings }), { wrapper });

    expect(result.current.upcomingBooking).toBeDefined();
    expect(result.current.upcomingBooking?.id).toBe('b-next');
    expect(result.current.isHappeningNow).toBe(false);
    expect(result.current.minutesUntilStart).toBeGreaterThan(0);
    expect(result.current.canJoinMeeting).toBe(false); // Vì còn 30 phút (> 15 phút)
  });

  it('should return isHappeningNow = true if current time is within session window', () => {
    const wrapper = createWrapper();
    const activeBooking: BookingDetail = {
      ...mockBookings[1],
      id: 'b-active',
      startTime: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
      endTime: new Date(Date.now() + 45 * 60 * 1000).toISOString(),
    };

    const { result } = renderHook(() => useUpcomingBooking({ bookings: [activeBooking] }), { wrapper });

    expect(result.current.upcomingBooking?.id).toBe('b-active');
    expect(result.current.isHappeningNow).toBe(true);
    expect(result.current.canJoinMeeting).toBe(true);
    expect(result.current.minutesUntilStart).toBe(0);
  });

  it('should return canJoinMeeting = true if within 15 minutes before start', () => {
    const wrapper = createWrapper();
    const soonBooking: BookingDetail = {
      ...mockBookings[1],
      id: 'b-soon',
      startTime: new Date(Date.now() + 10 * 60 * 1000).toISOString(), // 10 phút nữa
      endTime: new Date(Date.now() + 70 * 60 * 1000).toISOString(),
    };

    const { result } = renderHook(() => useUpcomingBooking({ bookings: [soonBooking] }), { wrapper });

    expect(result.current.upcomingBooking?.id).toBe('b-soon');
    expect(result.current.isHappeningNow).toBe(false);
    expect(result.current.canJoinMeeting).toBe(true);
    expect(result.current.minutesUntilStart).toBeLessThanOrEqual(10);
  });

  it('should return null when there are no upcoming confirmed bookings', () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useUpcomingBooking({ bookings: [mockBookings[0]] }), { wrapper });

    expect(result.current.upcomingBooking).toBeNull();
    expect(result.current.isHappeningNow).toBe(false);
    expect(result.current.canJoinMeeting).toBe(false);
    expect(result.current.minutesUntilStart).toBeNull();
  });

  it('getUpcomingBookingFromList pure function should work correctly', () => {
    const calculation = getUpcomingBookingFromList(mockBookings);
    expect(calculation.upcomingBooking?.id).toBe('b-next');
  });
});
