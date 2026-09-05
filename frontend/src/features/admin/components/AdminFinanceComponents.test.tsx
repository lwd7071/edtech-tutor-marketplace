import React from 'react';
import { render, screen } from '@testing-library/react';
import { AdminDashboardOverview } from './AdminDashboardOverview';
import { AdminSettingsForm } from './AdminSettingsForm';
import { AdminAuditLogTable } from './AdminAuditLogTable';
import { AdminDashboardView, PlatformSettingsView, AuditLogView } from '../types';

describe('AdminFinanceComponents', () => {
  it('renders AdminDashboardOverview with stats', () => {
    const mockStats: AdminDashboardView = {
      totalGmvVnd: 250000000,
      totalCommissionVnd: 12500000,
      totalTeachers: 45,
      totalStudents: 150,
      totalBookings: 600,
      completedBookings: 520,
      scheduledBookings: 60,
      cancelledBookings: 20,
      pendingPayoutsCount: 3,
      pendingPayoutsAmountVnd: 4500000,
      pendingRefundsCount: 2,
    };

    render(<AdminDashboardOverview stats={mockStats} />);

    expect(screen.getByText('Tổng GMV Nền tảng')).toBeInTheDocument();
    expect(screen.getByText('250.000.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('Doanh thu Hoa hồng')).toBeInTheDocument();
    expect(screen.getByText('12.500.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('3 lệnh')).toBeInTheDocument();
    expect(screen.getByText('2 yêu cầu')).toBeInTheDocument();
  });

  it('renders AdminSettingsForm with initial values', () => {
    const mockSettings: PlatformSettingsView = {
      id: 's-1',
      commissionRate: 0.05,
      bayesianMinimumReviews: 5,
      bookingReminderHours: 2,
      bookingExpirationHours: 12,
      updatedAt: '2026-08-30T10:00:00Z',
    };

    render(<AdminSettingsForm settings={mockSettings} onUpdate={jest.fn()} />);

    expect(screen.getByText(/Cấu hình Tham số Nền tảng/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /lưu cấu hình/i })).toBeInTheDocument();
  });

  it('renders AdminAuditLogTable with log items', () => {
    const mockLogs: AuditLogView[] = [
      {
        id: '12345678-0000-0000-0000-000000000000',
        actorId: 'admin-1',
        action: 'APPROVE',
        targetType: 'TeacherProfile',
        targetId: 'tp-1',
        createdAt: '2026-08-31T12:00:00Z',
      },
    ];

    render(<AdminAuditLogTable logs={mockLogs} total={1} />);

    expect(screen.getByText('APPROVE')).toBeInTheDocument();
    expect(screen.getByText('TeacherProfile')).toBeInTheDocument();
    expect(screen.getByText('Xem JSON')).toBeInTheDocument();
  });
});
