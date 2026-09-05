import React from 'react';
import { render, screen } from '@testing-library/react';
import { LedgerTable, getEntryTypeLabel } from './LedgerTable';
import { LedgerEntryView } from '../types';

describe('LedgerTable', () => {
  const mockEntries: LedgerEntryView[] = [
    {
      id: 'l-1',
      walletId: 'w-1',
      entryType: 'SETTLEMENT_RELEASE_AVAILABLE',
      amountVnd: 200000,
      balanceBucket: 'AVAILABLE',
      direction: 'CREDIT',
      description: 'Hoàn tất buổi học',
      createdAt: '2026-08-20T10:00:00Z',
    },
    {
      id: 'l-2',
      walletId: 'w-1',
      entryType: 'PAYOUT_RESERVED',
      amountVnd: 500000,
      balanceBucket: 'AVAILABLE',
      direction: 'DEBIT',
      description: 'Lệnh rút tiền #12345',
      createdAt: '2026-08-21T14:30:00Z',
    },
  ];

  it('renders credit and debit entries with correct prefixes and styles', () => {
    render(<LedgerTable entries={mockEntries} total={2} />);

    expect(screen.getByText('Quyết toán buổi học')).toBeInTheDocument();
    expect(screen.getByText('Tạo lệnh rút tiền')).toBeInTheDocument();

    expect(screen.getByText('+200.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('−500.000 ₫')).toBeInTheDocument();
  });

  it('getEntryTypeLabel maps all ledger types properly', () => {
    expect(getEntryTypeLabel('SETTLEMENT_CREDIT_PENDING')).toBe('Nhận tiền gói học');
    expect(getEntryTypeLabel('REFUND_DEBIT_PENDING')).toBe('Khấu trừ hoàn tiền');
    expect(getEntryTypeLabel('PAYOUT_SUCCEEDED')).toBe('Rút tiền thành công');
  });
});
