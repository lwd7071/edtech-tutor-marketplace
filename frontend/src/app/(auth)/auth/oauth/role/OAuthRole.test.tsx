import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import OAuthRolePage from './page';
import { authApi } from '@/shared/api/auth';
import { useRouter, useSearchParams } from 'next/navigation';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

jest.mock('@/shared/api/auth');
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));
jest.mock('@/features/auth', () => ({
  useAuthStore: jest.fn((selector) => selector({
    establish: jest.fn(),
  })),
}));

describe('OAuthRolePage', () => {
  const mockReplace = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ replace: mockReplace });
    (useSearchParams as jest.Mock).mockReturnValue({ get: jest.fn().mockReturnValue('mock-temp-token') });
  });

  const renderOAuthRole = () => render(<QueryClientProvider client={new QueryClient()}><OAuthRolePage /></QueryClientProvider>);

  it('renders role selection correctly', () => {
    renderOAuthRole();
    expect(screen.getByRole('heading', { name: 'Hoàn tất đăng ký' })).toBeInTheDocument();
    expect(screen.getByText('Học viên / Phụ huynh')).toBeInTheDocument();
    expect(screen.getByText('Gia sư')).toBeInTheDocument();
    expect(screen.getByText('Vai trò này không thể thay đổi sau khi bạn hoàn tất đăng ký.')).toBeInTheDocument();
  });

  it('submits selected role and redirects', async () => {
    (authApi.completeOAuthRegistration as jest.Mock).mockResolvedValueOnce({
      data: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        user: { id: '1', role: 'STUDENT' }
      }
    });

    renderOAuthRole();
    
    // Select TEACHER
    const teacherCard = screen.getByText('Gia sư').closest('.radio-card');
    fireEvent.click(teacherCard!);
    
    fireEvent.click(screen.getByRole('button', { name: /Hoàn tất đăng ký/i }));

    await waitFor(() => {
      expect(authApi.completeOAuthRegistration).toHaveBeenCalledWith({
        registrationToken: 'mock-temp-token',
        role: 'TEACHER'
      });
      expect(mockReplace).toHaveBeenCalledWith('/student');
    });
  });

  it('shows error if no temp token', async () => {
    (useSearchParams as jest.Mock).mockReturnValue({ get: jest.fn().mockReturnValue(null) });
    renderOAuthRole();
    
    fireEvent.click(screen.getByRole('button', { name: /Hoàn tất đăng ký/i }));

    await waitFor(() => {
      expect(screen.getByText('Token không hợp lệ. Vui lòng đăng nhập lại.')).toBeInTheDocument();
    });
  });
});
