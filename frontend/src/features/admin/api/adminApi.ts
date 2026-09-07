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
    return (await axiosClient.get('/api/admin/teachers/approvals', {
      params: { status, page, size, sort },
    })).data;
  },

  approveTeacher: async (
    teacherId: string,
    data: ApproveTeacherRequest
  ): Promise<ApiResponse<TeacherApprovalSnapshot>> => {
    return (await axiosClient.post(`/api/admin/teachers/${teacherId}/approve`, data)).data;
  },

  rejectTeacher: async (
    teacherId: string,
    data: RejectRequest
  ): Promise<ApiResponse<TeacherApprovalSnapshot>> => {
    return (await axiosClient.post(`/api/admin/teachers/${teacherId}/reject`, data)).data;
  },

  // 2. Phê duyệt đề xuất môn học
  getSubjectProposals: async (
    page: number = 0,
    size: number = 20,
    sort: string = 'createdAt,asc'
  ): Promise<ApiResponse<SubjectProposalSnapshot[]>> => {
    return (await axiosClient.get('/api/admin/subject-proposals', {
      params: { page, size, sort },
    })).data;
  },

  approveSubjectProposal: async (
    proposalId: string,
    data: ApproveSubjectProposalRequest
  ): Promise<ApiResponse<SubjectProposalSnapshot>> => {
    return (await axiosClient.post(`/api/admin/subject-proposals/${proposalId}/approve`, data)).data;
  },

  rejectSubjectProposal: async (
    proposalId: string,
    data: RejectRequest
  ): Promise<ApiResponse<SubjectProposalSnapshot>> => {
    return (await axiosClient.post(`/api/admin/subject-proposals/${proposalId}/reject`, data)).data;
  },

  // 3. Kiểm duyệt người dùng
  changeUserStatus: async (
    userId: string,
    data: ChangeUserStatusRequest
  ): Promise<ApiResponse<IdentitySnapshot>> => {
    return (await axiosClient.patch(`/api/admin/users/${userId}/status`, data)).data;
  },
};

