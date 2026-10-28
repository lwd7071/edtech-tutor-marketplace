import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { TeacherApprovalTable } from './TeacherApprovalTable';
import { TeacherDetailDrawer } from './TeacherDetailDrawer';
import { TeacherApprovalSnapshot } from '../types';
import { adminApi } from '../api/adminApi';

jest.mock('../api/adminApi', () => ({ adminApi: { getTeacherDocumentProof: jest.fn() } }));

// Mock hook useTeacherApprovals, useApproveTeacher, useRejectTeacher
jest.mock('../hooks/useAdminApprovals', () => ({
  useTeacherApprovals: jest.fn(() => ({
    data: {
      data: [
        {
          teacherProfileId: 't-101',
          userId: 'u-1',
          fullName: 'Nguyễn Văn A',
          email: 'nguyenvana@gmail.com',
          bio: 'Giáo viên Toán 5 năm kinh nghiệm',
          yearsOfExperience: 5,
          languages: ['Tiếng Việt'],
          supportsOnline: true,
          status: 'PENDING_APPROVAL',
          documents: [
            {
              id: 'd-1',
              type: 'DEGREE',
              title: 'Bằng Cử nhân Sư phạm Toán',
              mimeType: 'application/pdf',
              fileSize: 1048576,
              verificationStatus: 'PENDING',
            },
          ],
        },
      ],
      meta: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    },
    isLoading: false,
  })),
  useApproveTeacher: jest.fn(() => ({
    mutateAsync: jest.fn().mockResolvedValue({}),
    isPending: false,
  })),
  useRejectTeacher: jest.fn(() => ({
    mutateAsync: jest.fn().mockResolvedValue({}),
    isPending: false,
  })),
}));

describe('TeacherApprovalTable & Drawer (TDD)', () => {
  it('should render table with teacher information and pending status', () => {
    render(<TeacherApprovalTable />);

    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    expect(screen.getByText('nguyenvana@gmail.com')).toBeInTheDocument();
    expect(screen.getByText('5 năm')).toBeInTheDocument();
    expect(screen.getAllByText(/Chờ duyệt/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: /Xem hồ sơ/i })).toBeInTheDocument();
  });

  it('should load a private document through the admin proxy', async () => {
    const blob = new Blob(['proof'], { type: 'application/pdf' });
    jest.mocked(adminApi.getTeacherDocumentProof).mockResolvedValue(blob);
    URL.createObjectURL = jest.fn(() => 'blob:proof');
    URL.revokeObjectURL = jest.fn();
    const mockTeacher: TeacherApprovalSnapshot = {
      teacherProfileId: 't-101',
      userId: 'u-1',
      fullName: 'Nguyễn Văn A',
      email: 'nguyenvana@gmail.com',
      bio: 'Giáo viên Toán 5 năm kinh nghiệm',
      yearsOfExperience: 5,
      languages: ['Tiếng Việt'],
      supportsOnline: true,
      status: 'PENDING_APPROVAL',
      documents: [
        {
          id: 'd-1',
          type: 'DEGREE',
          title: 'Bằng Cử nhân Sư phạm Toán',
          mimeType: 'application/pdf',
          fileSize: 1048576,
          verificationStatus: 'PENDING',
        },
      ],
    };

    const handleClose = jest.fn();
    render(<TeacherDetailDrawer open={true} teacher={mockTeacher} onClose={handleClose} />);

    expect(screen.getByText('Chi tiết hồ sơ gia sư')).toBeInTheDocument();
    expect(screen.getByText('Tiếng Việt')).toBeInTheDocument();
    expect(screen.getByText('Bằng Cử nhân Sư phạm Toán')).toBeInTheDocument();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Xem tài liệu/i }));
      await Promise.resolve();
    });
    await waitFor(() => expect(adminApi.getTeacherDocumentProof).toHaveBeenCalledWith('d-1'));
    expect(await screen.findByTitle('Giấy tờ xác minh')).toHaveAttribute('src', 'blob:proof');
    expect(screen.getByRole('button', { name: /Phê duyệt/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Từ chối/i })).toBeInTheDocument();
  });
});
