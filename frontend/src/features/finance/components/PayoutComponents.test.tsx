import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { CreatePayoutModal } from './CreatePayoutModal';
import { PayoutListTable, getPayoutStatusTag } from './PayoutListTable';
import { BankAccountView, PayoutRequestView } from '../types';

describe('CreatePayoutModal & PayoutListTable', () => {
  const mockAccounts: BankAccountView[] = [
    {
      id: 'b-1',
      bankBin: '970422',
      bankName: 'MB Bank',
      accountNumberMasked: '******1234',
      accountHolderName: 'NGUYEN VAN A',
      isVerified: true,
      isDefault: true,
      createdAt: '2026-08-01T00:00:00Z',
    },
  ];

  it('renders CreatePayoutModal with balance alert and quick amount buttons', () => {
    render(
      <CreatePayoutModal
        open={true}
        availableBalanceVnd={1000000}
        bankAccounts={mockAccounts}
        onCancel={jest.fn()}
        onSubmit={jest.fn()}
      />
    );

    expect(screen.getAllByText(/1.000.000/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: '200.000 ₫' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '500.000 ₫' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '1.000.000 ₫' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Rút toàn bộ' })).toBeInTheDocument();
  });

  it('renders PayoutListTable with status tags and formatted amounts', () => {
    const mockPayouts: PayoutRequestView[] = [
      {
        id: '12345678-abcd-ef01-2345-6789abcdef01',
        teacherId: 't-1',
        walletId: 'w-1',
        bankAccountId: 'b-1',
        amountVnd: 500000,
        status: 'SUCCEEDED',
        bankReference: 'FT999888',
        proofUrl: 'https://proof.example.com',
        version: 1,
        createdAt: '2026-08-20T10:00:00Z',
      },
    ];

    render(<PayoutListTable payouts={mockPayouts} total={1} />);

    expect(screen.getByText('#12345678')).toBeInTheDocument();
    expect(screen.getByText('500.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('Đã chuyển')).toBeInTheDocument();
    expect(screen.getByText(/Mã GD: FT999888/i)).toBeInTheDocument();
    expect(screen.getByText('Xem biên lai')).toBeInTheDocument();
  });
});
