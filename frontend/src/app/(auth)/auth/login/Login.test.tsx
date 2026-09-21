import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import LoginPage from './page';
import { authApi } from '@/shared/api/auth';
import { useRouter, useSearchParams } from 'next/navigation';
import { BASE_API_URL } from '@/shared/backend';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

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
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush, replace: mockPush });
    (useSearchParams as jest.Mock).mockReturnValue({ get: mockGetParams });
  });

  const renderLogin = () => render(<QueryClientProvider client={new QueryClient()}><LoginPage /></QueryClientProvider>);

  it('renders login form correctly', () => {
    renderLogin();
    expect(screen.getByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Nhập email của bạn')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Nhập mật khẩu')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Đăng nhập bằng Google/i }))
      .toHaveAttribute('href', `${BASE_API_URL}/oauth2/authorization/google`);
  });

  it('shows validation errors for empty fields', async () => {
    renderLogin();
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

    renderLogin();
    
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
