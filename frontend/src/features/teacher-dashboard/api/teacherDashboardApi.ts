import { axiosClient } from '@/shared/api/axiosClient';
import type { ApiResponse } from '@/shared/backend';
import type { PricingPackageView } from '@/shared/api/public';

export interface LearnerPackage {
  studentId: string;
  studentName: string;
  subjectName: string;
  studentPackage: {
    id: string;
    subjectId: string;
    packageName: string;
    remainingSessions: number;
    expiresAt: string;
    status: string;
  };
}

export const teacherDashboardApi = {
  getPackages: async (page = 0, size = 12): Promise<ApiResponse<PricingPackageView[]>> =>
    (await axiosClient.get<ApiResponse<PricingPackageView[]>>('/api/teacher/packages', { params: { page, size } })).data,

  getLearners: async (page = 0, size = 12, studentId?: string): Promise<ApiResponse<LearnerPackage[]>> =>
    (await axiosClient.get<ApiResponse<LearnerPackage[]>>('/api/teacher/students', { params: { page, size, studentId } })).data,
};
