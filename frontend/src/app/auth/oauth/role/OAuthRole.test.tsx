import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import OAuthRolePage from './page';
import { authApi } from '@/shared/api/auth';
import { useRouter, useSearchParams } from 'next/navigation';

jest.mock('@/shared/api/auth');
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));
jest.mock('@/shared/store/useAuthStore', () => ({
  useAuthStore: jest.fn((selector) => selector({
    setAuth: jest.fn(),
  })),
}));

describe('OAuthRolePage', () => {
  const mockPush = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({ push: mockPush });
    (useSearchParams as jest.Mock).mockReturnValue({ get: jest.fn().mockReturnValue('mock-temp-token') });
  });

  it('renders role selection correctly', () => {
    render(<OAuthRolePage />);
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

    render(<OAuthRolePage />);
    
    // Select TEACHER
    const teacherCard = screen.getByText('Gia sư').closest('.radio-card');
    fireEvent.click(teacherCard!);
    
    fireEvent.click(screen.getByRole('button', { name: /Hoàn tất đăng ký/i }));

    await waitFor(() => {
      expect(authApi.completeOAuthRegistration).toHaveBeenCalledWith({
        tempToken: 'mock-temp-token',
        role: 'TEACHER'
      });
      expect(mockPush).toHaveBeenCalledWith('/');
    });
  });

  it('shows error if no temp token', async () => {
    (useSearchParams as jest.Mock).mockReturnValue({ get: jest.fn().mockReturnValue(null) });
    render(<OAuthRolePage />);
    
    fireEvent.click(screen.getByRole('button', { name: /Hoàn tất đăng ký/i }));

    await waitFor(() => {
      expect(screen.getByText('Token không hợp lệ. Vui lòng đăng nhập lại.')).toBeInTheDocument();
    });
  });
});
