import React from 'react';
import { render, screen, within } from '@testing-library/react';
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
    expect(screen.getByRole('heading', { name: 'Chào Student One!' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Xem lịch học' })).toHaveAttribute('href', '/student/bookings');
    expect(screen.getByText('12 buổi')).toBeInTheDocument();
    expect(screen.getByText('3 bài')).toBeInTheDocument();
    expect(screen.getByText('4 thông báo')).toBeInTheDocument();
    expect(screen.getByText('2 yêu cầu')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Lượt học còn lại/ })).toHaveAttribute('href', '/student/packages');
    expect(screen.getByRole('link', { name: /Bài cần làm/ })).toHaveAttribute('href', '/student/assignments');
    expect(screen.getByRole('link', { name: /Thông báo chưa đọc/ })).toHaveAttribute('href', '/student/notifications');
    expect(screen.getByRole('link', { name: /Yêu cầu đang chờ/ })).toHaveAttribute('href', '/student/requests');
    const shortcuts = within(screen.getByRole('navigation', { name: 'Lối tắt học tập' }));
    expect(shortcuts.getByRole('link', { name: /^Báo cáo buổi học/ })).toHaveAttribute('href', '/student/session-reports');
    expect(shortcuts.getByRole('link', { name: /^Tin nhắn/ })).toHaveAttribute('href', '/student/messages');
  });
});
