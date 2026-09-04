import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/api/types';
import {
  BookingDetail,
  BookingFilterParams,
  CreateBookingRequest,
  CompleteBookingRequest,
  CancelBookingRequest,
  TrialRequestView,
  CreateTrialRequest,
  AcceptTrialRequest,
  RejectTrialRequest,
} from '../types';

export const bookingApi = {
  /**
   * Lấy danh sách lịch học của học sinh đang đăng nhập
   */
  getStudentBookings: async (
    params?: BookingFilterParams
  ): Promise<ApiResponse<BookingDetail[]>> => {
    const response = await axiosClient.get<ApiResponse<BookingDetail[]>>(
      '/api/student/bookings',
      { params }
    );
    return response.data;
  },

  /**
   * Giáo viên tạo buổi học mới từ gói học sinh
   */
  createTeacherBooking: async (
    data: CreateBookingRequest
  ): Promise<ApiResponse<BookingDetail>> => {
    const response = await axiosClient.post<ApiResponse<BookingDetail>>(
      '/api/teacher/bookings',
      data
    );
    return response.data;
  },

  /**
   * Giáo viên hoàn thành buổi học kèm nộp SessionReport
   */
  completeBooking: async (
    id: string,
    data: CompleteBookingRequest
  ): Promise<ApiResponse<BookingDetail>> => {
    const response = await axiosClient.post<ApiResponse<BookingDetail>>(
      `/api/teacher/bookings/${id}/complete`,
      data
    );
    return response.data;
  },

  /**
   * Giáo viên hủy buổi học kèm lý do
   */
  cancelBooking: async (
    id: string,
    data: CancelBookingRequest
  ): Promise<ApiResponse<BookingDetail>> => {
    const response = await axiosClient.post<ApiResponse<BookingDetail>>(
      `/api/teacher/bookings/${id}/cancel`,
      data
    );
    return response.data;
  },

  /**
   * Lấy danh sách yêu cầu học thử cho giáo viên
   */
  getTeacherTrialRequests: async (
    status?: string,
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<TrialRequestView[]>> => {
    const response = await axiosClient.get<ApiResponse<TrialRequestView[]>>(
      '/api/teacher/trial-requests',
      { params: { status, page, size } }
    );
    return response.data;
  },

  /**
   * Học sinh gửi yêu cầu học thử
   */
  createTrialRequest: async (
    data: CreateTrialRequest
  ): Promise<ApiResponse<TrialRequestView>> => {
    const response = await axiosClient.post<ApiResponse<TrialRequestView>>(
      '/api/student/trials/requests',
      data
    );
    return response.data;
  },

  /**
   * Giáo viên chấp nhận yêu cầu học thử (tạo booking trial)
   */
  acceptTrialRequest: async (
    id: string,
    data: AcceptTrialRequest
  ): Promise<ApiResponse<BookingDetail>> => {
    const response = await axiosClient.post<ApiResponse<BookingDetail>>(
      `/api/teacher/trial-requests/${id}/accept`,
      data
    );
    return response.data;
  },

  /**
   * Giáo viên từ chối yêu cầu học thử
   */
  rejectTrialRequest: async (
    id: string,
    data: RejectTrialRequest
  ): Promise<ApiResponse<TrialRequestView>> => {
    const response = await axiosClient.post<ApiResponse<TrialRequestView>>(
      `/api/teacher/trial-requests/${id}/reject`,
      data
    );
    return response.data;
  },
};
