import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import VerifyEmailPage from './page';
import { authApi } from '@/shared/api/auth';
import { useSearchParams } from 'next/navigation';

jest.mock('@/shared/api/auth');
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));

describe('VerifyEmailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders check email prompt when no token is provided', () => {
    (useSearchParams as jest.Mock).mockReturnValue({ get: jest.fn().mockReturnValue(null) });
    render(<VerifyEmailPage />);
    expect(screen.getByText('Vui lòng xác minh email')).toBeInTheDocument();
  });

  it('verifies token on mount if provided', async () => {
    (useSearchParams as jest.Mock).mockReturnValue({ 
      get: jest.fn((key) => key === 'token' ? 'valid-token' : null) 
    });
    (authApi.verifyEmail as jest.Mock).mockResolvedValueOnce({ data: { success: true } });

    await act(async () => {
      render(<VerifyEmailPage />);
    });
    
    // Shows loading first or finishes
    await waitFor(() => {
      expect(authApi.verifyEmail).toHaveBeenCalledWith('valid-token');
      expect(screen.getByText('Xác minh thành công')).toBeInTheDocument();
    });
  });

  it('allows resending email if email is in params', async () => {
    (useSearchParams as jest.Mock).mockReturnValue({ 
      get: jest.fn((key) => key === 'email' ? 'test@example.com' : null) 
    });
    (authApi.resendVerification as jest.Mock).mockResolvedValueOnce({ data: { success: true } });

    render(<VerifyEmailPage />);
    
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Gửi lại email xác minh/i }));
    });

    await waitFor(() => {
      expect(authApi.resendVerification).toHaveBeenCalledWith('test@example.com');
      expect(screen.getByText('Đã gửi lại email xác minh thành công.')).toBeInTheDocument();
    });
  });
});
