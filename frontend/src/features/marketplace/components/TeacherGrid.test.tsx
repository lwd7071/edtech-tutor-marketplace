import React from 'react';
import { render, screen } from '@testing-library/react';
import TeacherGrid from './TeacherGrid';

jest.mock('@/shared/components/data-display/TeacherCard', () => {
  return function MockTeacherCard(props: any) {
    return <div data-testid="teacher-card">{props.name}</div>;
  };
});
jest.mock('@/shared/components/feedback/EmptyState', () => ({ EmptyState: () => <div data-testid="empty-state" /> }));
jest.mock('@/shared/components/feedback/ErrorState', () => ({ ErrorState: ({ onRetry }: any) => <div data-testid="error-state"><button onClick={onRetry}>Retry</button></div> }));
jest.mock('@/shared/components/feedback/Skeleton', () => ({ Skeleton: () => <div data-testid="skeleton" /> }));

describe('TeacherGrid', () => {
  it('renders loading skeleton', () => {
    const { container } = render(<TeacherGrid isLoading={true} teachers={[]} />);
    expect(container.querySelectorAll('.ant-skeleton').length).toBeGreaterThan(0);
  });

  it('renders error state', () => {
    render(<TeacherGrid isError={true} teachers={[]} onRetry={() => {}} />);
    expect(screen.getByTestId('error-state')).toBeInTheDocument();
  });

  it('renders empty state when no teachers', () => {
    render(<TeacherGrid isLoading={false} teachers={[]} />);
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
  });

  it('renders teacher cards when teachers are provided', () => {
    const teachers = [
      { id: '1', fullName: 'Nguyễn Văn A', avatarUrl: null, bioExcerpt: null, provinceName: null, wardName: null, yearsOfExperience: 3, verifiedBadge: true, supportsOnline: true, supportsOffline: false, subjects: [], startingPriceVnd: 250000, averageRating: 4.8, bayesianRating: 4.7, reviewCount: 12, globalRank: 4 },
      { id: '2', fullName: 'Trần Thị B', avatarUrl: null, bioExcerpt: null, provinceName: null, wardName: null, yearsOfExperience: 5, verifiedBadge: false, supportsOnline: true, supportsOffline: true, subjects: [], startingPriceVnd: 300000, averageRating: 5, bayesianRating: 4.9, reviewCount: 8, globalRank: null },
    ];
    render(<TeacherGrid isLoading={false} teachers={teachers} />);
    
    expect(screen.getAllByTestId('teacher-card')).toHaveLength(2);
    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    expect(screen.getByText('Trần Thị B')).toBeInTheDocument();
  });
});
