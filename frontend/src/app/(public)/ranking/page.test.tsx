import React from 'react';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import RankingPage from './page';
import { getGlobalRankingServer, getPublicSubjectsServer } from '@/shared/api/public.server';

jest.mock('@/shared/api/public.server', () => ({
  getGlobalRankingServer: jest.fn(),
  getPublicSubjectsServer: jest.fn(),
}));

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

describe('RankingPage', () => {
  const mockRanking = [
    { teacherId: 't1', fullName: 'Teacher One', globalRank: 1, bayesianRating: 4.9, completedSessionCount: 150 },
    { teacherId: 't2', fullName: 'Teacher Two', globalRank: 2, bayesianRating: 4.8, completedSessionCount: 120 },
    { teacherId: 't3', fullName: 'Teacher Three', globalRank: 3, bayesianRating: 4.7, completedSessionCount: 100 },
    { teacherId: 't4', fullName: 'Teacher Four', globalRank: 4, bayesianRating: 4.6, completedSessionCount: 90 },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (getPublicSubjectsServer as jest.Mock).mockResolvedValue({ data: [], meta: {} });
    (getGlobalRankingServer as jest.Mock).mockResolvedValue({ data: mockRanking, meta: {} });
  });

  it('renders server-fetched initial ranking without a client loading fetch', async () => {
    const page = await RankingPage();
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={client}>{page}</QueryClientProvider>);

    expect(screen.getAllByText('Teacher One').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Teacher Four').length).toBeGreaterThan(0);
    expect(getPublicSubjectsServer).toHaveBeenCalledWith({ size: 100 });
    expect(getGlobalRankingServer).toHaveBeenCalledWith(undefined, 0, 50);
  });
});
