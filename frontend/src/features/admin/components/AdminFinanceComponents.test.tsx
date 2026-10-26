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

    expect(screen.getByText('Tổng GMV')).toBeInTheDocument();
    expect(screen.getByText('250.000.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('Hoa hồng')).toBeInTheDocument();
    expect(screen.getByText('12.500.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('3 lệnh')).toBeInTheDocument();
    expect(screen.getByText('2 yêu cầu')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Lệnh rút tiền chờ duyệt' })).toHaveAttribute('href', '/admin/payouts');
    expect(screen.getByRole('link', { name: 'Duyệt hồ sơ gia sư' })).toHaveAttribute('href', '/admin/teachers');
    expect(screen.getByText('60 buổi')).toBeInTheDocument();
  });

  it('does not invent zero values when a successful response has no stats', () => {
    render(<AdminDashboardOverview stats={null} />);

    expect(screen.getByText('Chưa có số liệu tổng quan để hiển thị.')).toBeInTheDocument();
    expect(screen.queryByText('0 ₫')).not.toBeInTheDocument();
  });

  it('keeps the dashboard structure visible while stats load', () => {
    render(<AdminDashboardOverview loading />);

    expect(screen.getByLabelText('Đang tải số liệu quản trị')).toBeInTheDocument();
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

    expect(screen.getByText('Tham số nền tảng')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /lưu cấu hình/i })).toBeInTheDocument();
  });

  it('renders AdminAuditLogTable with log items', () => {
    const mockLogs: AuditLogView[] = [
      {
        id: '12345678-0000-0000-0000-000000000000',
        actorId: 'admin-1',
        action: 'TEACHER_APPROVED',
        targetType: 'TeacherProfile',
        targetId: 'tp-1',
        createdAt: '2026-08-31T12:00:00Z',
      },
    ];

    render(<AdminAuditLogTable logs={mockLogs} total={1} />);

    expect(screen.getByText('TEACHER_APPROVED')).toBeInTheDocument();
    expect(screen.getByText('TeacherProfile')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chi tiết' })).toBeInTheDocument();
  });
});
