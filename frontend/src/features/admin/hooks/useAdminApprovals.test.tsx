import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useTeacherApprovals,
  useApproveTeacher,
  useRejectTeacher,
  useSubjectProposals,
  useApproveSubjectProposal,
  useRejectSubjectProposal,
  useChangeUserStatus,
} from './useAdminApprovals';
import { adminApi } from '../api/adminApi';

// Mock adminApi
jest.mock('../api/adminApi', () => ({
  adminApi: {
    getTeacherApprovals: jest.fn(),
    approveTeacher: jest.fn(),
    rejectTeacher: jest.fn(),
    getSubjectProposals: jest.fn(),
    approveSubjectProposal: jest.fn(),
    rejectSubjectProposal: jest.fn(),
    changeUserStatus: jest.fn(),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe('Admin Approvals TanStack Hooks (TDD)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('useTeacherApprovals should fetch teacher approval queue', async () => {
    const mockData = {
      data: [{ teacherProfileId: 't1', status: 'PENDING_APPROVAL', documents: [] }],
      meta: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    };
    (adminApi.getTeacherApprovals as jest.Mock).mockResolvedValueOnce(mockData);

    const { result } = renderHook(() => useTeacherApprovals('PENDING_APPROVAL', 0, 20), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.data).toEqual(mockData.data);
    expect(adminApi.getTeacherApprovals).toHaveBeenCalledWith('PENDING_APPROVAL', 0, 20, 'createdAt,asc');
  });

  it('useApproveTeacher should call approve API with note', async () => {
    (adminApi.approveTeacher as jest.Mock).mockResolvedValueOnce({
      data: { teacherProfileId: 't1', status: 'APPROVED' },
    });

    const { result } = renderHook(() => useApproveTeacher(), {
      wrapper: createWrapper(),
    });

    await result.current.mutateAsync({ teacherId: 't1', data: { note: 'Hồ sơ đầy đủ' } });

    expect(adminApi.approveTeacher).toHaveBeenCalledWith('t1', { note: 'Hồ sơ đầy đủ' });
  });

  it('useRejectTeacher should call reject API with reason', async () => {
    (adminApi.rejectTeacher as jest.Mock).mockResolvedValueOnce({
      data: { teacherProfileId: 't1', status: 'REJECTED' },
    });

    const { result } = renderHook(() => useRejectTeacher(), {
      wrapper: createWrapper(),
    });

    await result.current.mutateAsync({ teacherId: 't1', data: { reason: 'Thiếu chứng chỉ sư phạm' } });

    expect(adminApi.rejectTeacher).toHaveBeenCalledWith('t1', { reason: 'Thiếu chứng chỉ sư phạm' });
  });

  it('useSubjectProposals should fetch subject proposals', async () => {
    const mockData = {
      data: [{ proposalId: 'p1', proposedName: 'Luyện thi IELTS', status: 'PENDING' }],
      meta: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    };
    (adminApi.getSubjectProposals as jest.Mock).mockResolvedValueOnce(mockData);

    const { result } = renderHook(() => useSubjectProposals(0, 20), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.data).toEqual(mockData.data);
    expect(adminApi.getSubjectProposals).toHaveBeenCalledWith(0, 20, 'createdAt,asc');
  });

  it('useApproveSubjectProposal should call approve subject API with category and name', async () => {
    (adminApi.approveSubjectProposal as jest.Mock).mockResolvedValueOnce({
      data: { proposalId: 'p1', status: 'APPROVED' },
    });

    const { result } = renderHook(() => useApproveSubjectProposal(), {
      wrapper: createWrapper(),
    });

    await result.current.mutateAsync({
      proposalId: 'p1',
      data: { name: 'IELTS Academic', category: 'Ngoại ngữ', description: 'Ôn thi IELTS 4 kỹ năng' },
    });

    expect(adminApi.approveSubjectProposal).toHaveBeenCalledWith('p1', {
      name: 'IELTS Academic',
      category: 'Ngoại ngữ',
      description: 'Ôn thi IELTS 4 kỹ năng',
    });
  });

  it('useChangeUserStatus should call change status API', async () => {
    (adminApi.changeUserStatus as jest.Mock).mockResolvedValueOnce({
      data: { id: 'u1', statusName: 'LOCKED' },
    });

    const { result } = renderHook(() => useChangeUserStatus(), {
      wrapper: createWrapper(),
    });

    await result.current.mutateAsync({
      userId: 'u1',
      data: { status: 'LOCKED', reason: 'Vi phạm chính sách cộng đồng' },
    });

    expect(adminApi.changeUserStatus).toHaveBeenCalledWith('u1', {
      status: 'LOCKED',
      reason: 'Vi phạm chính sách cộng đồng',
    });
  });
});
