import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TeacherApprovalTable } from './TeacherApprovalTable';
import { TeacherDetailDrawer } from './TeacherDetailDrawer';
import { TeacherApprovalSnapshot } from '../types';

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
              secureUrl: 'https://cdn.example.com/degree.pdf',
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

  it('should display drawer details when teacher profile is selected', () => {
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
          secureUrl: 'https://cdn.example.com/degree.pdf',
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
    expect(screen.getByRole('link', { name: /Xem tài liệu/i })).toHaveAttribute('href', 'https://cdn.example.com/degree.pdf');
    expect(screen.getByRole('button', { name: /Phê duyệt/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Từ chối/i })).toBeInTheDocument();
  });
});
