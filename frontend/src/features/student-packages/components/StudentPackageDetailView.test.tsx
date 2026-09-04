import React from 'react';
import { render, screen } from '@testing-library/react';
import { StudentPackageDetailView } from './StudentPackageDetailView';
import { StudentPackageDetail } from '../types';

describe('StudentPackageDetailView (TDD)', () => {
  const activePackage: StudentPackageDetail = {
    id: 'pkg-1',
    teacher: { id: 't-1', fullName: 'Trần Thu Hà' },
    subject: { id: 's-1', name: 'Toán 11' },
    packageName: 'Toán 11 — 10 buổi',
    totalSessions: 10,
    remainingSessions: 7,
    reservedSessions: 1,
    completedSessions: 2,
    refundedSessions: 0,
    purchasePriceVnd: 1000000,
    startsAt: '2026-08-19T02:00:00Z',
    expiresAt: '2026-11-17T02:00:00Z',
    status: 'ACTIVE',
    description: 'Chương trình ôn tập toàn diện',
    version: 1,
  };

  it('should render active package details with session counter and booking action', () => {
    render(<StudentPackageDetailView packageData={activePackage} />);

    expect(screen.getByText('Toán 11 — 10 buổi')).toBeInTheDocument();
    expect(screen.getByText('Trần Thu Hà')).toBeInTheDocument();
    expect(screen.getByText('Đang hoạt động')).toBeInTheDocument();
    expect(screen.getByText('1.000.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('Chương trình ôn tập toàn diện')).toBeInTheDocument();
  });

  it('should render warning banner for LOCKED_EXPIRED status', () => {
    const expiredPackage: StudentPackageDetail = {
      ...activePackage,
      status: 'LOCKED_EXPIRED',
    };

    render(<StudentPackageDetailView packageData={expiredPackage} />);

    expect(screen.getByText('Hết hạn')).toBeInTheDocument();
    expect(screen.getByText(/Gói học này đã hết thời hạn sử dụng/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Yêu cầu gia hạn/i })).toBeInTheDocument();
  });

  it('should render warning banner for REFUND_PENDING status', () => {
    const refundPendingPackage: StudentPackageDetail = {
      ...activePackage,
      status: 'REFUND_PENDING',
    };

    render(<StudentPackageDetailView packageData={refundPendingPackage} />);

    expect(screen.getAllByText(/Đang xử lý hoàn tiền/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/Gói học đang trong tiến trình xử lý hoàn tiền/i)).toBeInTheDocument();
  });
});
