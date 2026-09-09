import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { bookingApi } from '../api/bookingApi';
import {
  BookingFilterParams,
  CreateBookingRequest,
  CompleteBookingRequest,
  CancelBookingRequest,
  CreateTrialRequest,
  AcceptTrialRequest,
  RejectTrialRequest,
} from '../types';
import { bookingKeys } from '../data/bookingKeys';
import { studentPackageKeys } from '@/features/student-packages/data/studentPackageKeys';
import { financeKeys } from '@/features/finance/data/financeKeys';

export const BOOKING_KEYS = bookingKeys;

/**
 * Hook lấy danh sách buổi học của học sinh
 */
export function useStudentBookings(params?: BookingFilterParams) {
  return useQuery({
    queryKey: bookingKeys.list('student', params),
    queryFn: () => bookingApi.getStudentBookings(params),
  });
}

/**
 * Hook tạo buổi học mới
 */
export function useCreateBooking() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateBookingRequest) => bookingApi.createTeacherBooking(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bookingKeys.all });
      queryClient.invalidateQueries({ queryKey: studentPackageKeys.all });
    },
  });
}

/**
 * Hook hoàn thành buổi học kèm nộp SessionReport
 */
export function useCompleteBooking() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CompleteBookingRequest }) =>
      bookingApi.completeBooking(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bookingKeys.all });
      queryClient.invalidateQueries({ queryKey: studentPackageKeys.all });
      queryClient.invalidateQueries({ queryKey: financeKeys.wallet() });
    },
  });
}

/**
 * Hook hủy buổi học
 */
export function useCancelBooking() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CancelBookingRequest }) =>
      bookingApi.cancelBooking(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bookingKeys.all });
      queryClient.invalidateQueries({ queryKey: studentPackageKeys.all });
    },
  });
}

/**
 * Hook lấy danh sách yêu cầu học thử
 */
export function useTeacherTrialRequests(status?: string, page?: number, size?: number) {
  return useQuery({
    queryKey: bookingKeys.trialRequests(status, page, size),
    queryFn: () => bookingApi.getTeacherTrialRequests(status, page, size),
  });
}

/**
 * Hook gửi yêu cầu học thử (học sinh)
 */
export function useCreateTrialRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateTrialRequest) => bookingApi.createTrialRequest(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: BOOKING_KEYS.all });
    },
  });
}

/**
 * Hook chấp nhận yêu cầu học thử (giáo viên)
 */
export function useAcceptTrialRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AcceptTrialRequest }) =>
      bookingApi.acceptTrialRequest(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: BOOKING_KEYS.all });
    },
  });
}

/**
 * Hook từ chối yêu cầu học thử (giáo viên)
 */
export function useRejectTrialRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectTrialRequest }) =>
      bookingApi.rejectTrialRequest(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: BOOKING_KEYS.all });
    },
  });
}
