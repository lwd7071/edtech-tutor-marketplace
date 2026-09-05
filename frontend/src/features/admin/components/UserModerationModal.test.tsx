import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { UserModerationModal } from './UserModerationModal';

const mockChangeUserStatus = jest.fn().mockResolvedValue({});

jest.mock('../hooks/useAdminApprovals', () => ({
  useChangeUserStatus: jest.fn(() => ({
    mutateAsync: mockChangeUserStatus,
    isPending: false,
  })),
}));

describe('UserModerationModal (TDD)', () => {
  const mockUser = {
    id: 'u-123',
    fullName: 'Trần Thị B',
    email: 'tranthib@gmail.com',
    currentStatus: 'ACTIVE' as const,
  };

  it('should render modal with lock action when user is ACTIVE', () => {
    const handleClose = jest.fn();
    render(
      <UserModerationModal
        open={true}
        user={mockUser}
        onClose={handleClose}
      />
    );

    expect(screen.getByText(/Khóa tài khoản người dùng/i)).toBeInTheDocument();
    expect(screen.getByText('Trần Thị B')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Nhập lý do/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Xác nhận khóa/i })).toBeInTheDocument();
  });

  it('should render modal with unlock action when user is LOCKED', () => {
    const handleClose = jest.fn();
    const lockedUser = { ...mockUser, currentStatus: 'LOCKED' as const };
    render(
      <UserModerationModal
        open={true}
        user={lockedUser}
        onClose={handleClose}
      />
    );

    expect(screen.getByText(/Mở khóa tài khoản người dùng/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Xác nhận mở khóa/i })).toBeInTheDocument();
  });
});
