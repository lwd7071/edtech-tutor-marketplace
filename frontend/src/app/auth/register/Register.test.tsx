import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import RegisterPage from './page';
import { authApi } from '@/shared/api/auth';
import { useRouter } from 'next/navigation';
import { BASE_API_URL } from '@/shared/backend';

jest.mock('@/shared/api/auth');
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));

describe('RegisterPage', () => {
  const mockPush = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush });
  });

  it('renders register form correctly', () => {
    render(<RegisterPage />);
    expect(screen.getByText('Đăng ký tài khoản')).toBeInTheDocument();
    expect(screen.getByText('Học viên / Phụ huynh')).toBeInTheDocument();
    expect(screen.getByText('Gia sư')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Đăng ký bằng Google/i }))
      .toHaveAttribute('href', `${BASE_API_URL}/oauth2/authorization/google`);
  });

  it('shows validation error for weak password', async () => {
    render(<RegisterPage />);
    
    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Nguyễn Văn A'), { target: { value: 'Test User' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập email của bạn'), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập mật khẩu'), { target: { value: 'weak' } });
    
    // Check agree terms
    fireEvent.click(document.querySelector('input[type="checkbox"]')!);
    
    fireEvent.submit(document.querySelector('form')!);

    await waitFor(() => {
      expect(screen.getByText('Mật khẩu phải dài ít nhất 8 ký tự')).toBeInTheDocument();
    });
  });

  it('submits form successfully', async () => {
    (authApi.register as jest.Mock).mockResolvedValueOnce({ data: { success: true } });

    render(<RegisterPage />);
    
    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Nguyễn Văn A'), { target: { value: 'Test User' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập email của bạn'), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập mật khẩu'), { target: { value: 'StrongPass123!' } });
    
    fireEvent.click(document.querySelector('input[type="checkbox"]')!);
    
    fireEvent.submit(document.querySelector('form')!); 

    await waitFor(() => {
      expect(authApi.register).toHaveBeenCalled();
      expect(mockPush).toHaveBeenCalledWith('/auth/verify-email');
    });
  });
});
