import React from 'react';
import { render, screen } from '@testing-library/react';
import { TeacherPackagesTab } from './TeacherPackagesTab';

jest.mock('@/shared/components/data-display/MoneyText', () => ({
  MoneyText: ({ amount }: { amount: number }) => <span data-testid="money-text">{amount}</span>
}));

jest.mock('@/shared/components/feedback/EmptyState', () => ({
  EmptyState: () => <div data-testid="empty-state">No packages</div>
}));

describe('TeacherPackagesTab', () => {
  it('renders packages correctly', () => {
    const packages = [
      { id: '1', name: 'Gói cơ bản', description: 'Dạy toán 10', priceVnd: 500000, sessionCount: 4, durationMinutes: 60, status: 'ACTIVE' },
      { id: '2', name: 'Gói nâng cao', priceVnd: 1000000, sessionCount: 8, durationMinutes: 90, status: 'ACTIVE' }
    ];

    render(<TeacherPackagesTab packages={packages} />);
    
    expect(screen.getByText('Gói cơ bản')).toBeInTheDocument();
    expect(screen.getByText('Dạy toán 10')).toBeInTheDocument();
    expect(screen.getByText('4 buổi')).toBeInTheDocument();
    expect(screen.getByText('60 phút/buổi')).toBeInTheDocument();
    
    expect(screen.getByText('Gói nâng cao')).toBeInTheDocument();
    expect(screen.getAllByTestId('money-text')).toHaveLength(2);
  });

  it('renders empty state when no packages', () => {
    render(<TeacherPackagesTab packages={[]} />);
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
  });
});
