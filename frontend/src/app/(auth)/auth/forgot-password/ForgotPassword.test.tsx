import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import ForgotPasswordPage from './page';
import { authApi } from '@/shared/api/auth';

jest.mock('@/shared/api/auth');

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders form correctly', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByText('Quên mật khẩu?')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Nhập email của bạn')).toBeInTheDocument();
  });

  it('shows validation error for invalid email', async () => {
    render(<ForgotPasswordPage />);
    
    fireEvent.change(screen.getByPlaceholderText('Nhập email của bạn'), { target: { value: 'invalid-email' } });
    
    await act(async () => {
      fireEvent.submit(document.querySelector('form')!);
    });

    await waitFor(() => {
      expect(screen.getByText('Email không đúng định dạng')).toBeInTheDocument();
    });
  });

  it('shows success message on successful submit without revealing existence', async () => {
    (authApi.forgotPassword as jest.Mock).mockResolvedValueOnce({ data: { success: true } });

    render(<ForgotPasswordPage />);
    
    fireEvent.change(screen.getByPlaceholderText('Nhập email của bạn'), { target: { value: 'test@example.com' } });
    await act(async () => {
      fireEvent.submit(document.querySelector('form')!);
    });

    await waitFor(() => {
      expect(authApi.forgotPassword).toHaveBeenCalledWith('test@example.com');
      // Should show the alert message
      expect(screen.getByText('Đã gửi email khôi phục')).toBeInTheDocument();
      // Should contain aria-live
      expect(screen.getByText('Quay lại đăng nhập').closest('div[aria-live="polite"]')).toBeInTheDocument();
    });
  });
});
