import React from 'react';
import { render, screen } from '@testing-library/react';
import { TeacherReviewsTab } from './TeacherReviewsTab';

jest.mock('@/shared/components/feedback/EmptyState', () => ({
  EmptyState: () => <div data-testid="empty-state">No reviews</div>
}));

jest.mock('@/shared/components/data-display/DateTimeText', () => ({
  DateTimeText: ({ value }: any) => <span data-testid="date-text">{value}</span>
}));

describe('TeacherReviewsTab', () => {
  it('renders reviews correctly', () => {
    const reviews = [
      { id: '1', rating: 5, comment: 'Great teacher', createdAt: '2023-10-01', reviewerName: 'Alice' },
      { id: '2', rating: 4, comment: 'Good', createdAt: '2023-10-02', reviewerName: 'Bob' }
    ];

    render(<TeacherReviewsTab reviews={reviews} meta={{ page: 0, totalPages: 1, totalElements: 2, size: 10, hasNext: false, hasPrevious: false }} onPageChange={jest.fn()} />);
    
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Great teacher')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('Good')).toBeInTheDocument();
  });

  it('renders empty state when no reviews', () => {
    render(<TeacherReviewsTab reviews={[]} meta={{ page: 0, totalPages: 0, totalElements: 0, size: 10, hasNext: false, hasPrevious: false }} onPageChange={jest.fn()} />);
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
  });
});
