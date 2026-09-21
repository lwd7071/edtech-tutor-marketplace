import React from 'react';
import { render, waitFor } from '@testing-library/react';
import OAuthCallbackPage from './page';
import { useRouter, useSearchParams } from 'next/navigation';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

jest.mock('@/shared/api/auth');
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));
jest.mock('@/features/auth', () => ({
  useAuthStore: jest.fn((selector) => selector({ establish: jest.fn() })),
}));

describe('OAuthCallbackPage', () => {
  it('sends a new Google user to role selection', async () => {
    const replace = jest.fn();
    (useRouter as jest.Mock).mockReturnValue({ replace });
    (useSearchParams as jest.Mock).mockReturnValue({
      get: (key: string) => key === 'registrationToken' ? 'token-123' : null,
    });

    render(<QueryClientProvider client={new QueryClient()}><OAuthCallbackPage /></QueryClientProvider>);

    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith('/auth/oauth/role?registrationToken=token-123');
    });
  });
});
