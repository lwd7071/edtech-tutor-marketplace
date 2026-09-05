import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/api/types';
import {
  StudentPackageSummary,
  StudentPackageDetail,
  StudentPackageStatus,
} from '../types';

export const studentPackageApi = {
  /**
   * Lấy danh sách gói học của học sinh đang đăng nhập
   */
  getStudentPackages: async (
    status?: StudentPackageStatus,
    page: number = 0,
    size: number = 20,
    sort: string = 'createdAt,desc'
  ): Promise<ApiResponse<StudentPackageSummary[]>> => {
    const params: Record<string, unknown> = { page, size, sort };
    if (status) {
      params.status = status;
    }
    const response = await axiosClient.get<ApiResponse<StudentPackageSummary[]>>(
      '/api/student/packages',
      { params }
    );
    return response.data;
  },

  /**
   * Lấy thông tin chi tiết một gói học theo id
   */
  getStudentPackageDetail: async (
    id: string
  ): Promise<ApiResponse<StudentPackageDetail>> => {
    const response = await axiosClient.get<ApiResponse<StudentPackageDetail>>(
      `/api/student/packages/${id}`
    );
    return response.data;
  },
};
