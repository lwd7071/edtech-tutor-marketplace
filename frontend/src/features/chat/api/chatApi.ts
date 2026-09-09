import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse, PaginationMeta } from '@/shared/backend';
import { ConversationView, MessageView } from '../types';

export const chatApi = {
  /**
   * Lấy danh sách cuộc hội thoại
   */
  getConversations: async (
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<ConversationView[]>> => {
    const response = await axiosClient.get<ApiResponse<ConversationView[]>>(
      '/api/conversations',
      { params: { page, size } }
    );
    return response.data;
  },

  /**
   * Lấy lịch sử tin nhắn của một cuộc hội thoại
   */
  getMessages: async (
    conversationId: string,
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<MessageView[]>> => {
    const response = await axiosClient.get<ApiResponse<MessageView[]>>(
      `/api/conversations/${conversationId}/messages`,
      { params: { page, size } }
    );
    return response.data;
  },
};
