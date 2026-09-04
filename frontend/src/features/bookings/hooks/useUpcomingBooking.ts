import { useMemo } from 'react';
import { BookingDetail } from '../types';
import { useStudentBookings } from './useBookings';

export interface UseUpcomingBookingOptions {
  bookings?: BookingDetail[];
}

export interface UpcomingBookingCalculation {
  upcomingBooking: BookingDetail | null;
  isHappeningNow: boolean;
  minutesUntilStart: number | null;
  canJoinMeeting: boolean;
}

export interface UseUpcomingBookingResult extends UpcomingBookingCalculation {
  isLoading: boolean;
}

/**
 * Pure function tính toán buổi học sớm nhất sắp diễn ra từ danh sách
 */
export function getUpcomingBookingFromList(
  bookings: BookingDetail[],
  now: number = Date.now()
): UpcomingBookingCalculation {
  const activeOrFutureBookings = bookings
    .filter((b) => b.status === 'SCHEDULED')
    .filter((b) => {
      const endMs = new Date(b.endTime).getTime();
      return endMs > now;
    })
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());

  const upcomingBooking = activeOrFutureBookings[0] ?? null;

  if (!upcomingBooking) {
    return {
      upcomingBooking: null,
      isHappeningNow: false,
      minutesUntilStart: null,
      canJoinMeeting: false,
    };
  }

  const startMs = new Date(upcomingBooking.startTime).getTime();
  const endMs = new Date(upcomingBooking.endTime).getTime();

  const isHappeningNow = now >= startMs && now <= endMs;
  const minutesUntilStart = isHappeningNow ? 0 : Math.max(0, Math.round((startMs - now) / 60000));
  // Cho phép vào phòng học trước 15 phút và trong suốt buổi học
  const canJoinMeeting = isHappeningNow || (now >= startMs - 15 * 60 * 1000 && now <= endMs);

  return {
    upcomingBooking,
    isHappeningNow,
    minutesUntilStart,
    canJoinMeeting,
  };
}

/**
 * Hook lấy buổi học sắp tới của học sinh (hoặc từ danh sách truyền vào)
 */
export function useUpcomingBooking(options?: UseUpcomingBookingOptions): UseUpcomingBookingResult {
  const queryResult = useStudentBookings({ status: 'SCHEDULED' });
  const isLoading = options?.bookings ? false : queryResult.isLoading;

  const rawBookings = useMemo(() => {
    return options?.bookings ?? queryResult.data?.data ?? [];
  }, [options?.bookings, queryResult.data?.data]);

  const calculation = useMemo(() => {
    return getUpcomingBookingFromList(rawBookings);
  }, [rawBookings]);

  return {
    ...calculation,
    isLoading,
  };
}
