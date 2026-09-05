import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ResetPasswordPage from './page';
import { authApi } from '@/shared/api/auth';
import { useRouter, useSearchParams } from 'next/navigation';

jest.mock('@/shared/api/auth');
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));

describe('ResetPasswordPage', () => {
  const mockPush = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush });
    (useSearchParams as jest.Mock).mockReturnValue({ get: jest.fn().mockReturnValue('valid-token') });
  });

  it('renders form correctly when token is present', () => {
    render(<ResetPasswordPage />);
    expect(screen.getByText('Đặt lại mật khẩu')).toBeInTheDocument();
  });

  it('shows error state when token is missing', () => {
    (useSearchParams as jest.Mock).mockReturnValue({ get: jest.fn().mockReturnValue(null) });
    render(<ResetPasswordPage />);
    expect(screen.getByText('Liên kết không hợp lệ hoặc đã hết hạn')).toBeInTheDocument();
  });

  it('validates matching passwords', async () => {
    render(<ResetPasswordPage />);
    
    fireEvent.change(screen.getByPlaceholderText('Nhập mật khẩu mới'), { target: { value: 'StrongPass123!' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập lại mật khẩu mới'), { target: { value: 'DifferentPass123!' } });
    
    fireEvent.submit(document.querySelector('form')!);

    await waitFor(() => {
      expect(screen.getByText('Mật khẩu xác nhận không khớp')).toBeInTheDocument();
    });
  });

  it('submits correctly and redirects', async () => {
    (authApi.resetPassword as jest.Mock).mockResolvedValueOnce({ data: { success: true } });

    render(<ResetPasswordPage />);
    
    fireEvent.change(screen.getByPlaceholderText('Nhập mật khẩu mới'), { target: { value: 'StrongPass123!' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập lại mật khẩu mới'), { target: { value: 'StrongPass123!' } });
    
    fireEvent.submit(document.querySelector('form')!);

    await waitFor(() => {
      expect(authApi.resetPassword).toHaveBeenCalledWith({ token: 'valid-token', newPassword: 'StrongPass123!' });
      expect(mockPush).toHaveBeenCalledWith('/auth/login?reset=success');
    });
  });
});
