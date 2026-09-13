import React from 'react';
import { render, screen } from '@testing-library/react';
import { SubjectProposalTable } from './SubjectProposalTable';
import { ApproveSubjectModal } from './ApproveSubjectModal';
import { SubjectProposalSnapshot } from '../types';

const mockProposal: SubjectProposalSnapshot = {
  proposalId: 'sub-prop-1',
  teacherId: 't-101',
  teacherName: 'Nguyễn Văn A',
  proposedName: 'Toán học nâng cao 12',
  educationLevel: 'HIGH_SCHOOL',
  description: 'Chương trình luyện thi đại học môn Toán nâng cao',
  status: 'PENDING',
  createdAt: '2026-03-01T10:00:00Z',
  version: 0,
};

jest.mock('../hooks/useAdminApprovals', () => ({
  useSubjectProposals: jest.fn(() => ({
    data: {
      data: [
        {
          proposalId: 'sub-prop-1',
          teacherId: 't-101',
          teacherName: 'Nguyễn Văn A',
          proposedName: 'Toán học nâng cao 12',
          educationLevel: 'HIGH_SCHOOL',
          description: 'Chương trình luyện thi đại học môn Toán nâng cao',
          status: 'PENDING',
          createdAt: '2026-03-01T10:00:00Z',
        },
      ],
      meta: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    },
    isLoading: false,
  })),
  useApproveSubjectProposal: jest.fn(() => ({
    mutateAsync: jest.fn().mockResolvedValue({}),
    isPending: false,
  })),
  useRejectSubjectProposal: jest.fn(() => ({
    mutateAsync: jest.fn().mockResolvedValue({}),
    isPending: false,
  })),
}));

describe('SubjectProposalTable & ApproveSubjectModal (TDD)', () => {
  it('should render table with subject proposals and actions', () => {
    render(<SubjectProposalTable />);

    expect(screen.getByText('Toán học nâng cao 12')).toBeInTheDocument();
    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    expect(screen.getAllByText(/Chờ duyệt/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: /Phê duyệt/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Từ chối/i })).toBeInTheDocument();
  });

  it('should render ApproveSubjectModal with prefilled values from proposal', () => {
    const handleClose = jest.fn();
    render(<ApproveSubjectModal open={true} proposal={mockProposal} onClose={handleClose} />);

    expect(screen.getByText('Chuẩn hóa & Phê duyệt Môn học')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Toán học nâng cao 12')).toBeInTheDocument();
    expect(screen.getByText('THPT')).toBeInTheDocument();
  });
});
