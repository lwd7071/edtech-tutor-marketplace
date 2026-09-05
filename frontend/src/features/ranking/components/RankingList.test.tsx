import React from 'react';
import { render, screen } from '@testing-library/react';
import { RankingList } from './RankingList';

describe('RankingList', () => {
  const mockTeachers = [
    {
      teacherId: 't4',
      fullName: 'Teacher Four',
      globalRank: 4,
      bayesianRating: 4.6,
      completedSessionCount: 90,
      avatarUrl: 'https://example.com/avatar4.jpg'
    },
    {
      teacherId: 't5',
      fullName: 'Teacher Five',
      globalRank: 5,
      bayesianRating: 4.5,
      completedSessionCount: 80,
      avatarUrl: 'https://example.com/avatar5.jpg'
    }
  ];

  it('renders a list of teachers from rank 4 onwards', () => {
    render(<RankingList teachers={mockTeachers} />);
    
    expect(screen.getByText('Teacher Four')).toBeInTheDocument();
    expect(screen.getByText('Teacher Five')).toBeInTheDocument();
    
    // Check ranks
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('renders empty state when array is empty', () => {
    render(<RankingList teachers={[]} />);
    expect(screen.getByText('Chưa có dữ liệu xếp hạng')).toBeInTheDocument();
  });
});
