import { axiosClient } from '@/shared/api/axiosClient';
import { requireApiData, type ApiResponse, type ApiResponseWithData } from '@/shared/backend';

export interface StudentDashboardView {
  nextBookingStartTime: string | null;
  remainingSessions: number;
  todoAssignments: number;
  unreadNotifications: number;
  pendingRequests: number;
}

export const studentDashboardApi = {
  getSummary: async (): Promise<ApiResponseWithData<StudentDashboardView>> =>
    requireApiData((await axiosClient.get<ApiResponse<StudentDashboardView>>('/api/student/dashboard')).data),
};
