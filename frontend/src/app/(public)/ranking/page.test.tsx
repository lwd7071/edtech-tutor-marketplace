import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import RankingPage from './page';
import { getGlobalRanking, getPublicSubjects } from '@/shared/api/public';

jest.mock('@/shared/api/public', () => ({
  getGlobalRanking: jest.fn(),
  getPublicSubjects: jest.fn(),
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
    (getPublicSubjects as jest.Mock).mockResolvedValue({ data: [] });
    (getGlobalRanking as jest.Mock).mockResolvedValue({ data: mockRanking, meta: {} });
  });

  it('renders loading state initially', async () => {
    let resolvePromise: any;
    const promise = new Promise(resolve => { resolvePromise = resolve; });
    (getGlobalRanking as jest.Mock).mockReturnValue(promise);

    render(<RankingPage />);
    expect(screen.getByTestId('ranking-loading')).toBeInTheDocument();
    
    await act(async () => {
      resolvePromise({ data: mockRanking, meta: {} });
    });
  });

  it('renders podium and list after loading', async () => {
    await act(async () => {
      render(<RankingPage />);
    });

    // Top 3 in Podium
    expect(screen.getAllByText('Teacher One').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Teacher Two').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Teacher Three').length).toBeGreaterThan(0);
    
    // Rest in List
    expect(screen.getAllByText('Teacher Four').length).toBeGreaterThan(0);
  });
});
