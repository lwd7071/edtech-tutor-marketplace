import React from 'react';
import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import SubjectProposalsPage from './page';
import { teacherApi } from '@/shared/api/teacher';

jest.mock('@/shared/api/teacher', () => ({
  teacherApi: {
    getSubjectProposals: jest.fn(),
    createSubjectProposal: jest.fn(),
  },
}));

const mockProposals = [
  {
    id: 'prop-1',
    name: 'Khoa học máy tính',
    description: 'Dạy lập trình cơ bản',
    status: 'PENDING',
    submittedAt: '2026-09-01T10:00:00Z',
  },
];

describe('Teacher Subject Proposals Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getSubjectProposals as jest.Mock).mockResolvedValue(mockProposals);
  });

  it('renders proposals list with fetched data', async () => {
    await act(async () => {
      render(<SubjectProposalsPage />);
    });

    expect(screen.queryByText('Đang tải danh sách đề xuất...')).not.toBeInTheDocument();
    expect(screen.getByText('Khoa học máy tính')).toBeInTheDocument();
  });

  it('allows creating a new proposal', async () => {
    (teacherApi.createSubjectProposal as jest.Mock).mockResolvedValue({
      id: 'prop-2',
      name: 'Sinh học',
      description: 'Cấp 3',
      status: 'PENDING',
      submittedAt: '2026-09-05T10:00:00Z',
    });

    await act(async () => {
      render(<SubjectProposalsPage />);
    });

    const nameInput = screen.getByPlaceholderText('Tên môn học');
    const descInput = screen.getByPlaceholderText('Mô tả');
    const submitBtn = screen.getByRole('button', { name: /gửi đề xuất/i });

    expect(nameInput).toBeInTheDocument();
    expect(descInput).toBeInTheDocument();
    expect(submitBtn).toBeInTheDocument();
  });
});
