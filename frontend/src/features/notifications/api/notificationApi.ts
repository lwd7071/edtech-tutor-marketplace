import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/api/types';
import { NotificationView, NotificationReadAllResponse } from '../types';

export const notificationApi = {
  getNotifications: async (
    isRead?: boolean,
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<NotificationView[]>> => {
    const response = await axiosClient.get<ApiResponse<NotificationView[]>>(
      '/api/notifications',
      { params: { isRead, page, size } }
    );
    return response.data;
  },

  markAsRead: async (id: string): Promise<ApiResponse<NotificationView>> => {
    const response = await axiosClient.patch<ApiResponse<NotificationView>>(
      `/api/notifications/${id}/read`
    );
    return response.data;
  },

  markAllAsRead: async (): Promise<ApiResponse<NotificationReadAllResponse>> => {
    const response = await axiosClient.post<ApiResponse<NotificationReadAllResponse>>(
      '/api/notifications/read-all'
    );
    return response.data;
  },
};
