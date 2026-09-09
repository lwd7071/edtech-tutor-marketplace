import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import {
  ApproveTeacherRequest,
  RejectRequest,
  ApproveSubjectProposalRequest,
  ChangeUserStatusRequest,
} from '../types';

export const ADMIN_QUERY_KEYS = {
  all: ['admin'] as const,
  teacherApprovalsRoot: ['admin', 'teacher-approvals'] as const,
  subjectProposalsRoot: ['admin', 'subject-proposals'] as const,
  teacherApprovals: (status?: string, page?: number, size?: number) => [
    'admin',
    'teacher-approvals',
    status,
    page,
    size,
  ],
  subjectProposals: (page?: number, size?: number) => [
    'admin',
    'subject-proposals',
    page,
    size,
  ],
};

/**
 * Hook lấy danh sách hồ sơ giáo viên chờ duyệt / đã duyệt
 */
export function useTeacherApprovals(
  status: string = 'PENDING_APPROVAL',
  page: number = 0,
  size: number = 20,
  sort: string = 'createdAt,asc'
) {
  return useQuery({
    queryKey: ADMIN_QUERY_KEYS.teacherApprovals(status, page, size),
    queryFn: () => adminApi.getTeacherApprovals(status, page, size, sort),
  });
}

/**
 * Hook phê duyệt hồ sơ giáo viên
 */
export function useApproveTeacher() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ teacherId, data }: { teacherId: string; data: ApproveTeacherRequest }) =>
      adminApi.approveTeacher(teacherId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.teacherApprovalsRoot });
    },
  });
}

/**
 * Hook từ chối hồ sơ giáo viên
 */
export function useRejectTeacher() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ teacherId, data }: { teacherId: string; data: RejectRequest }) =>
      adminApi.rejectTeacher(teacherId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.teacherApprovalsRoot });
    },
  });
}

/**
 * Hook lấy danh sách đề xuất môn học
 */
export function useSubjectProposals(page: number = 0, size: number = 20, sort: string = 'createdAt,asc') {
  return useQuery({
    queryKey: ADMIN_QUERY_KEYS.subjectProposals(page, size),
    queryFn: () => adminApi.getSubjectProposals(page, size, sort),
  });
}

/**
 * Hook phê duyệt đề xuất môn học
 */
export function useApproveSubjectProposal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      proposalId,
      data,
    }: {
      proposalId: string;
      data: ApproveSubjectProposalRequest;
    }) => adminApi.approveSubjectProposal(proposalId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.subjectProposalsRoot });
    },
  });
}

/**
 * Hook từ chối đề xuất môn học
 */
export function useRejectSubjectProposal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ proposalId, data }: { proposalId: string; data: RejectRequest }) =>
      adminApi.rejectSubjectProposal(proposalId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.subjectProposalsRoot });
    },
  });
}

/**
 * Hook khóa / mở khóa tài khoản người dùng
 */
export function useChangeUserStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: ChangeUserStatusRequest }) =>
      adminApi.changeUserStatus(userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.all });
    },
  });
}
