import type { BookingFilterParams } from '../types';

export const bookingKeys = {
  all: ['bookings'] as const,
  lists: () => [...bookingKeys.all, 'list'] as const,
  list: (role: 'student' | 'teacher', params?: BookingFilterParams) => [...bookingKeys.lists(), role, params] as const,
  details: () => [...bookingKeys.all, 'detail'] as const,
  detail: (role: 'student' | 'teacher', id: string) => [...bookingKeys.details(), role, id] as const,
  trialRequests: (status?: string, page?: number, size?: number) => [...bookingKeys.all, 'trial-requests', { status, page, size }] as const,
};
