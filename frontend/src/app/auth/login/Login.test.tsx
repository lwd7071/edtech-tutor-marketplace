import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import LoginPage from './page';
import { authApi } from '@/shared/api/auth';
import { useRouter, useSearchParams } from 'next/navigation';

jest.mock('@/shared/api/auth');
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));

describe('LoginPage', () => {
  const mockPush = jest.fn();
  const mockGetParams = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush });
    (useSearchParams as jest.Mock).mockReturnValue({ get: mockGetParams });
  });

  it('renders login form correctly', () => {
    render(<LoginPage />);
    expect(screen.getByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Nhập email của bạn')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Nhập mật khẩu')).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    render(<LoginPage />);
    await act(async () => {
      fireEvent.submit(document.querySelector('form')!);
    });

    await waitFor(() => {
      expect(screen.getByText('Email không được để trống')).toBeInTheDocument();
      expect(screen.getByText('Mật khẩu không được để trống')).toBeInTheDocument();
    });
  });

  it('shows ACCOUNT_LOCKED error state', async () => {
    (authApi.login as jest.Mock).mockRejectedValueOnce({
      response: { data: { errorCode: 'ACCOUNT_LOCKED' } }
    });

    render(<LoginPage />);
    
    fireEvent.change(screen.getByPlaceholderText('Nhập email của bạn'), { target: { value: 'locked@example.com' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập mật khẩu'), { target: { value: 'password123' } });
    
    await act(async () => {
      fireEvent.submit(document.querySelector('form')!);
    });

    await waitFor(() => {
      expect(screen.getByText('Tài khoản bị khóa')).toBeInTheDocument();
      expect(screen.getByText(/liên hệ bộ phận hỗ trợ/i)).toBeInTheDocument();
    });
  });
});
