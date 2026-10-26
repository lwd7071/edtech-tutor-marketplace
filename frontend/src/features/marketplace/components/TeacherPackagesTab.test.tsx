import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { TeacherPackagesTab } from './TeacherPackagesTab';

const mockPush = jest.fn();
const mockAuthState: { user: { role: string } | null } = { user: null };

jest.mock('next/navigation', () => ({ useRouter: () => ({ push: mockPush }), usePathname: () => '/teachers/1' }));
jest.mock('@/features/auth', () => ({ useAuthStore: () => mockAuthState }));

jest.mock('@/shared/components/data-display/MoneyText', () => ({
  MoneyText: ({ amount }: { amount: number }) => <span data-testid="money-text">{amount}</span>
}));

jest.mock('@/shared/components/feedback/EmptyState', () => ({
  EmptyState: () => <div data-testid="empty-state">No packages</div>
}));

describe('TeacherPackagesTab', () => {
  beforeEach(() => {
    mockPush.mockClear();
    mockAuthState.user = null;
  });

  it('renders packages correctly', () => {
    const packages = [
      { id: '1', name: 'Gói cơ bản', description: 'Dạy toán 10', priceVnd: 500000, totalSessions: 4, sessionDurationMinutes: 60, durationDays: 30, subjectId: 's1', subjectName: 'Toán', version: 0, status: 'ACTIVE' },
      { id: '2', name: 'Gói nâng cao', priceVnd: 1000000, totalSessions: 8, sessionDurationMinutes: 90, durationDays: 60, subjectId: 's1', subjectName: 'Toán', version: 0, status: 'ACTIVE' }
    ];

    render(<TeacherPackagesTab packages={packages} />);
    
    expect(screen.getByText('Gói cơ bản')).toBeInTheDocument();
    expect(screen.getByText('Dạy toán 10')).toBeInTheDocument();
    expect(screen.getAllByText('Số buổi')).toHaveLength(2);
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('60 phút/buổi')).toBeInTheDocument();
    
    expect(screen.getByText('Gói nâng cao')).toBeInTheDocument();
    expect(screen.getByText(/1.000.000đ/)).toBeInTheDocument();
  });

  it('renders empty state when no packages', () => {
    render(<TeacherPackagesTab packages={[]} />);
    expect(screen.getByText(/Gia sư chưa mở gói học/)).toBeInTheDocument();
  });

  it('sends a guest to login with the selected package return URL', () => {
    render(<TeacherPackagesTab packages={[{ id: 'pkg-1', name: 'Gói Toán', priceVnd: 500000, totalSessions: 4, sessionDurationMinutes: 60, durationDays: 30, subjectId: 'math', subjectName: 'Toán', version: 0, status: 'ACTIVE' }]} />);

    fireEvent.click(screen.getByRole('button', { name: 'Chọn gói học' }));

    expect(mockPush).toHaveBeenCalledWith('/auth/login?redirect=%2Fteachers%2F1%3FpackageId%3Dpkg-1%23packages');
  });

  it('allows only a student account to open the purchase confirmation', () => {
    const packages = [{ id: 'pkg-1', name: 'Gói Toán', priceVnd: 500000, totalSessions: 4, sessionDurationMinutes: 60, durationDays: 30, subjectId: 'math', subjectName: 'Toán', version: 0, status: 'ACTIVE' }];
    mockAuthState.user = { role: 'TEACHER' };
    const { rerender } = render(<TeacherPackagesTab packages={packages} />);

    expect(screen.queryByRole('button', { name: 'Chọn gói học' })).not.toBeInTheDocument();
    expect(screen.queryByRole('dialog', { name: 'Xác nhận gói học' })).not.toBeInTheDocument();
    expect(mockPush).not.toHaveBeenCalled();

    mockAuthState.user = { role: 'STUDENT' };
    rerender(<TeacherPackagesTab packages={packages} />);
    fireEvent.click(screen.getByRole('button', { name: 'Chọn gói học' }));
    expect(screen.getByRole('dialog', { name: 'Xác nhận gói học' })).toBeInTheDocument();
  });
});
