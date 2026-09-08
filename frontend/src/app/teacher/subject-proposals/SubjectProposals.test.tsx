import React from 'react';
import { act, render, screen } from '@testing-library/react';
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
    proposedName: 'Khoa học máy tính',
    educationLevel: 'HIGH_SCHOOL',
    description: 'Dạy lập trình cơ bản',
    status: 'PENDING',
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

    expect(screen.getByText('Khoa học máy tính')).toBeInTheDocument();
  });

  it('allows creating a new proposal', async () => {
    (teacherApi.createSubjectProposal as jest.Mock).mockResolvedValue({
      id: 'prop-2',
      proposedName: 'Sinh học',
      educationLevel: 'HIGH_SCHOOL',
      description: 'Cấp 3',
      status: 'PENDING',
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
