import React from 'react';
import { render, screen } from '@testing-library/react';
import { AdminPayoutTable } from './AdminPayoutTable';
import { AdminRefundTable } from './AdminRefundTable';
import { AdminExtensionTable } from './AdminExtensionTable';
import { PayoutRequestView, RefundRequestView, ExtensionRequestView } from '@/features/finance/types';

describe('AdminFinanceTables', () => {
  it('renders AdminPayoutTable with tabs and payout row', () => {
    const mockPayouts: PayoutRequestView[] = [
      {
        id: '12345678-0000-0000-0000-000000000000',
        teacherId: 't-1',
        walletId: 'w-1',
        bankAccountId: 'b-1',
        amountVnd: 1500000,
        status: 'PENDING',
        version: 1,
        createdAt: '2026-08-30T10:00:00Z',
      },
    ];

    render(
      <AdminPayoutTable
        payouts={mockPayouts}
        total={1}
        onProcessPayout={jest.fn()}
        onCompletePayout={jest.fn()}
        onRejectPayout={jest.fn()}
      />
    );

    expect(screen.getAllByText('Chờ duyệt').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('1.500.000 ₫')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /xử lý/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /từ chối/i })).toBeInTheDocument();
  });

  it('renders AdminRefundTable with approve and reject buttons for PENDING', () => {
    const mockRefunds: RefundRequestView[] = [
      {
        id: '87654321-0000-0000-0000-000000000000',
        studentPackageId: 'pkg-1',
        studentId: 'st-1',
        reason: 'Lý do cá nhân',
        requestedSessions: 3,
        status: 'PENDING',
        bankName: 'Vietcombank',
        accountNumberMasked: '******4321',
        accountHolderName: 'TRAN VAN C',
        version: 1,
        createdAt: '2026-08-30T10:00:00Z',
      },
    ];

    render(
      <AdminRefundTable
        refunds={mockRefunds}
        total={1}
        onApproveRefund={jest.fn()}
        onProcessRefund={jest.fn()}
        onRejectRefund={jest.fn()}
      />
    );

    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getAllByText(/buổi/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: /duyệt/i })).toBeInTheDocument();
  });

  it('renders AdminExtensionTable with extension row', () => {
    const mockExtensions: ExtensionRequestView[] = [
      {
        id: '11223344-0000-0000-0000-000000000000',
        studentPackageId: 'pkg-1',
        studentId: 'st-1',
        reason: 'Thi học kỳ',
        requestedExpiryDate: '2026-10-15T00:00:00Z',
        status: 'PENDING',
        createdAt: '2026-08-30T10:00:00Z',
      },
    ];

    render(
      <AdminExtensionTable
        extensions={mockExtensions}
        total={1}
        onApproveExtension={jest.fn()}
        onRejectExtension={jest.fn()}
      />
    );

    expect(screen.getByText('Thi học kỳ')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /duyệt/i })).toBeInTheDocument();
  });
});
