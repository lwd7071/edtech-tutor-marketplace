import React from 'react';
import { render, screen } from '@testing-library/react';
import SubjectGrid from './SubjectGrid';

// Mocking dependencies to focus on grid logic
jest.mock('@/shared/components/data-display/SubjectCard', () => {
  return function MockSubjectCard(props: any) {
    return <div data-testid="subject-card">{props.name}</div>;
  };
});
jest.mock('@/shared/components/feedback/EmptyState', () => ({ EmptyState: () => <div data-testid="empty-state" /> }));
jest.mock('@/shared/components/feedback/ErrorState', () => ({ ErrorState: ({ onRetry }: any) => <div data-testid="error-state"><button onClick={onRetry}>Retry</button></div> }));
jest.mock('@/shared/components/feedback/Skeleton', () => ({ Skeleton: () => <div data-testid="skeleton" /> }));

describe('SubjectGrid', () => {
  it('renders loading skeleton', () => {
    const { container } = render(<SubjectGrid isLoading={true} subjects={[]} />);
    expect(container.querySelectorAll('.ant-skeleton').length).toBeGreaterThan(0);
  });

  it('renders error state', () => {
    render(<SubjectGrid isError={true} subjects={[]} onRetry={() => {}} />);
    expect(screen.getByTestId('error-state')).toBeInTheDocument();
  });

  it('renders empty state when no subjects', () => {
    render(<SubjectGrid isLoading={false} subjects={[]} />);
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
  });

  it('renders subject cards when subjects are provided', () => {
    const subjects = [
      { id: '1', code: 'MATH', slug: 'toan', name: 'Toán', educationLevel: 'HIGH_SCHOOL', description: null },
      { id: '2', code: 'PHYSICS', slug: 'ly', name: 'Lý', educationLevel: 'HIGH_SCHOOL', description: null },
    ];
    render(<SubjectGrid isLoading={false} subjects={subjects} />);
    
    expect(screen.getAllByRole('link')).toHaveLength(2);
    expect(screen.getByText('Toán')).toBeInTheDocument();
    expect(screen.getByText('Lý')).toBeInTheDocument();
  });

  it('renders a compact card with the subject level and without its long description', () => {
    render(<SubjectGrid isLoading={false} subjects={[{ id: 'math-id', code: 'MATH', slug: 'toan', name: 'Toán', educationLevel: 'HIGH_SCHOOL', description: 'Ôn tập kiến thức và luyện thi.' }]} />);

    expect(screen.getByText('THPT')).toBeInTheDocument();
    expect(screen.queryByText('Ôn tập kiến thức và luyện thi.')).not.toBeInTheDocument();
    expect(screen.queryByText('Chọn môn này →')).not.toBeInTheDocument();
  });
});
