import React from 'react';
import { render, screen } from '@testing-library/react';
import StudentDashboardPage from './page';

jest.mock('@/features/auth', () => ({ useAuthStore: () => ({ user: { fullName: 'Student One' } }) }));
jest.mock('@/features/student-dashboard/hooks/useStudentDashboard', () => ({
  useStudentDashboard: jest.fn(() => ({
    data: { data: { nextBookingStartTime: '2026-09-14T10:00:00Z', remainingSessions: 12, todoAssignments: 3, unreadNotifications: 4, pendingRequests: 2 } },
    isLoading: false,
    isError: false,
    refetch: jest.fn(),
  })),
}));

describe('StudentDashboardPage', () => {
  it('renders aggregate data from one dashboard query', () => {
    render(<StudentDashboardPage />);
    expect(screen.getByText('12 buổi')).toBeInTheDocument();
    expect(screen.getByText('3 bài')).toBeInTheDocument();
    expect(screen.getByText('4 thông báo')).toBeInTheDocument();
    expect(screen.getByText('2 yêu cầu')).toBeInTheDocument();
  });
});
