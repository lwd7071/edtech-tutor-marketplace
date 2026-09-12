import React from 'react';
import { render, screen } from '@testing-library/react';
import { TeacherPackagesTab } from './TeacherPackagesTab';

jest.mock('next/navigation', () => ({ useRouter: () => ({ push: jest.fn() }), usePathname: () => '/teachers/1' }));

jest.mock('@/shared/components/data-display/MoneyText', () => ({
  MoneyText: ({ amount }: { amount: number }) => <span data-testid="money-text">{amount}</span>
}));

jest.mock('@/shared/components/feedback/EmptyState', () => ({
  EmptyState: () => <div data-testid="empty-state">No packages</div>
}));

describe('TeacherPackagesTab', () => {
  it('renders packages correctly', () => {
    const packages = [
      { id: '1', name: 'Gói cơ bản', description: 'Dạy toán 10', priceVnd: 500000, totalSessions: 4, sessionDurationMinutes: 60, durationDays: 30, subjectId: 's1', subjectName: 'Toán', version: 0, status: 'ACTIVE' },
      { id: '2', name: 'Gói nâng cao', priceVnd: 1000000, totalSessions: 8, sessionDurationMinutes: 90, durationDays: 60, subjectId: 's1', subjectName: 'Toán', version: 0, status: 'ACTIVE' }
    ];

    render(<TeacherPackagesTab packages={packages} />);
    
    expect(screen.getByText('Gói cơ bản')).toBeInTheDocument();
    expect(screen.getByText('Dạy toán 10')).toBeInTheDocument();
    expect(screen.getByText(/4 buổi · 60 phút\/buổi/)).toBeInTheDocument();
    
    expect(screen.getByText('Gói nâng cao')).toBeInTheDocument();
    expect(screen.getByText(/1.000.000đ/)).toBeInTheDocument();
  });

  it('renders empty state when no packages', () => {
    render(<TeacherPackagesTab packages={[]} />);
    expect(screen.getByText(/Gia sư chưa mở gói học/)).toBeInTheDocument();
  });
});
