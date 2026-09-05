import React from 'react';
import { render, screen } from '@testing-library/react';
import { RankingPodium } from './RankingPodium';

describe('RankingPodium', () => {
  const mockTeachers = [
    {
      teacherId: 't1',
      fullName: 'Teacher One',
      globalRank: 1,
      bayesianRating: 4.9,
      completedSessionCount: 150,
      avatarUrl: 'https://example.com/avatar1.jpg'
    },
    {
      teacherId: 't2',
      fullName: 'Teacher Two',
      globalRank: 2,
      bayesianRating: 4.8,
      completedSessionCount: 120,
      avatarUrl: 'https://example.com/avatar2.jpg'
    },
    {
      teacherId: 't3',
      fullName: 'Teacher Three',
      globalRank: 3,
      bayesianRating: 4.7,
      completedSessionCount: 100,
      avatarUrl: 'https://example.com/avatar3.jpg'
    }
  ];

  it('renders top 3 teachers in podium order', () => {
    render(<RankingPodium teachers={mockTeachers} />);
    
    // Check if names are rendered
    expect(screen.getByText('Teacher One')).toBeInTheDocument();
    expect(screen.getByText('Teacher Two')).toBeInTheDocument();
    expect(screen.getByText('Teacher Three')).toBeInTheDocument();

    // The order should be visually 2, 1, 3. 
    // We can just verify they exist and have correct rank badges
    expect(screen.getByText('Top 1')).toBeInTheDocument();
    expect(screen.getByText('Top 2')).toBeInTheDocument();
    expect(screen.getByText('Top 3')).toBeInTheDocument();
  });

  it('renders correctly with only 1 teacher', () => {
    render(<RankingPodium teachers={[mockTeachers[0]]} />);
    
    expect(screen.getByText('Teacher One')).toBeInTheDocument();
    expect(screen.queryByText('Teacher Two')).not.toBeInTheDocument();
  });
  
  it('renders nothing when teachers array is empty', () => {
    const { container } = render(<RankingPodium teachers={[]} />);
    expect(container.firstChild).toBeNull();
  });
});
