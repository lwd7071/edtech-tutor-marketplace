import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/backend';
import { TeacherStatsView, TeacherRankingItem } from '../types';

export const rankingApi = {
  getTeacherStats: async (): Promise<ApiResponse<TeacherStatsView>> => {
    const response = await axiosClient.get<ApiResponse<TeacherStatsView>>('/api/teacher/stats');
    return response.data;
  },

  getGlobalRanking: async (
    page: number = 0,
    size: number = 20,
    subjectId?: string
  ): Promise<ApiResponse<TeacherRankingItem[]>> => {
    const params: Record<string, any> = { page, size };
    if (subjectId) {
      params.subjectId = subjectId;
    }
    const response = await axiosClient.get<ApiResponse<TeacherRankingItem[]>>('/api/public/teachers/ranking', { params });
    return response.data;
  },
};
