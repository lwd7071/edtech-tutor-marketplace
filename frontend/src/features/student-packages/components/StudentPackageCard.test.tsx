import React from 'react';
import { render, screen } from '@testing-library/react';
import { StudentPackageCard } from './StudentPackageCard';
import { StudentPackageSummary } from '../types';

describe('StudentPackageCard (TDD)', () => {
  const mockPkg: StudentPackageSummary = {
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
    version: 1,
  };

  it('should render package details, status tag and session counters', () => {
    render(<StudentPackageCard packageData={mockPkg} />);

    expect(screen.getByText('Toán 11 — 10 buổi')).toBeInTheDocument();
    expect(screen.getByText('Trần Thu Hà')).toBeInTheDocument();
    expect(screen.getByText('Toán 11')).toBeInTheDocument();
    expect(screen.getByText('Đang hoạt động')).toBeInTheDocument();
    expect(screen.getByText('1.000.000 ₫')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Xem chi tiết/i })).toHaveAttribute(
      'href',
      '/student/packages/pkg-1'
    );
  });
});
