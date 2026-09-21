import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/backend';
import {
  TeacherApprovalSnapshot,
  SubjectProposalSnapshot,
  IdentitySnapshot,
  ApproveTeacherRequest,
  RejectRequest,
  SubjectProposalRejectRequest,
  ApproveSubjectProposalRequest,
  ChangeUserStatusRequest,
} from '../types';
import type { BookingSettlementAdminView } from '../types/finance';

export const adminApi = {
  getBookingSettlements: async (status?: string, page = 0, size = 20): Promise<ApiResponse<BookingSettlementAdminView[]>> => (await axiosClient.get('/api/admin/booking-settlements', { params: { status, page, size } })).data,
  reopenBookingSettlement: async (id: string, data: { version: number; note: string }) => (await axiosClient.post(`/api/admin/booking-settlements/${id}/reopen`, data)).data,
  releaseBookingSettlement: async (id: string, data: { version: number; note: string }) => (await axiosClient.post(`/api/admin/booking-settlements/${id}/release`, data)).data,
  retainBookingSettlement: async (id: string, data: { version: number; note: string }) => (await axiosClient.post(`/api/admin/booking-settlements/${id}/retain`, data)).data,
  getCredentialApprovals: async (status = 'PENDING'): Promise<ApiResponse<{id:string;teacherId:string;label:string;proofUrl:string;status:string;rejectedReason?:string;version:number}[]>> => (await axiosClient.get('/api/admin/credentials', {params:{status}})).data,
  approveCredential: async (id: string, version: number) => (await axiosClient.post(`/api/admin/credentials/${id}/approve`, null, {params:{version}})).data,
  rejectCredential: async (id: string, reason: string, version: number) => (await axiosClient.post(`/api/admin/credentials/${id}/reject`, null, {params:{reason,version}})).data,
  getCredentialProof: async (id: string): Promise<Blob> => (await axiosClient.get<Blob>(`/api/admin/credentials/${id}/proof`, {responseType:'blob'})).data,
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
    data: SubjectProposalRejectRequest
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

