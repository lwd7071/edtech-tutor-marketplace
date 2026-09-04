import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/api/types';
import {
  TeacherApprovalSnapshot,
  SubjectProposalSnapshot,
  IdentitySnapshot,
  ApproveTeacherRequest,
  RejectRequest,
  ApproveSubjectProposalRequest,
  ChangeUserStatusRequest,
} from '../types';

export const adminApi = {
  // 1. Phê duyệt hồ sơ giáo viên
  getTeacherApprovals: async (
    status: string = 'PENDING_APPROVAL',
    page: number = 0,
    size: number = 20,
    sort: string = 'createdAt,asc'
  ): Promise<ApiResponse<TeacherApprovalSnapshot[]>> => {
    return axiosClient.get('/admin/teachers/approvals', {
      params: { status, page, size, sort },
    });
  },

  approveTeacher: async (
    teacherId: string,
    data: ApproveTeacherRequest
  ): Promise<ApiResponse<TeacherApprovalSnapshot>> => {
    return axiosClient.post(`/admin/teachers/${teacherId}/approve`, data);
  },

  rejectTeacher: async (
    teacherId: string,
    data: RejectRequest
  ): Promise<ApiResponse<TeacherApprovalSnapshot>> => {
    return axiosClient.post(`/admin/teachers/${teacherId}/reject`, data);
  },

  // 2. Phê duyệt đề xuất môn học
  getSubjectProposals: async (
    page: number = 0,
    size: number = 20,
    sort: string = 'createdAt,asc'
  ): Promise<ApiResponse<SubjectProposalSnapshot[]>> => {
    return axiosClient.get('/admin/subject-proposals', {
      params: { page, size, sort },
    });
  },

  approveSubjectProposal: async (
    proposalId: string,
    data: ApproveSubjectProposalRequest
  ): Promise<ApiResponse<SubjectProposalSnapshot>> => {
    return axiosClient.post(`/admin/subject-proposals/${proposalId}/approve`, data);
  },

  rejectSubjectProposal: async (
    proposalId: string,
    data: RejectRequest
  ): Promise<ApiResponse<SubjectProposalSnapshot>> => {
    return axiosClient.post(`/admin/subject-proposals/${proposalId}/reject`, data);
  },

  // 3. Kiểm duyệt người dùng
  changeUserStatus: async (
    userId: string,
    data: ChangeUserStatusRequest
  ): Promise<ApiResponse<IdentitySnapshot>> => {
    return axiosClient.patch(`/admin/users/${userId}/status`, data);
  },
};
