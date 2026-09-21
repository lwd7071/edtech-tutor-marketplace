import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useLogout } from './useLogout';
import { useAuthStore } from './sessionStore';
import { authApi } from '@/shared/api/auth';

const replace = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ replace }),
}));

jest.mock('@/shared/api/auth', () => ({
  authApi: { logout: jest.fn() },
}));

function Harness() {
  const logout = useLogout();
  return <button onClick={() => void logout()}>logout</button>;
}

describe('useLogout', () => {
  beforeEach(() => {
    replace.mockReset();
    (authApi.logout as jest.Mock).mockReset();
    useAuthStore.getState().clear();
  });

  it('clears the source tab and redirects even when revoke fails', async () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['student', 'student-1'], { studentId: 'student-1' });
    useAuthStore.getState().establish({
      user: { id: 'student-1', email: 'student@example.test', fullName: 'Student', role: 'STUDENT', status: 'ACTIVE' },
      accessToken: 'student-access',
      refreshToken: 'student-refresh',
    });
    (authApi.logout as jest.Mock).mockRejectedValue(new Error('network down'));

    render(<QueryClientProvider client={queryClient}><Harness /></QueryClientProvider>);
    fireEvent.click(screen.getByRole('button', { name: 'logout' }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith('/auth/login'));
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(queryClient.getQueryData(['student', 'student-1'])).toBeUndefined();
  });
});
