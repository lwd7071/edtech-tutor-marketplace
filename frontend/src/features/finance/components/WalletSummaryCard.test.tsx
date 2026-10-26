import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { WalletSummaryCard } from './WalletSummaryCard';
import { WalletView } from '../types';

describe('WalletSummaryCard', () => {
  const mockWallet: WalletView = {
    id: 'w-1',
    teacherId: 't-1',
    availableBalanceVnd: 1500000,
    pendingBalanceVnd: 500000,
    reservedBalanceVnd: 200000,
    heldBalanceVnd: 300000,
    version: 1,
  };

  it('renders all four balance buckets formatted in VND', () => {
    render(<WalletSummaryCard wallet={mockWallet} />);

    expect(screen.getByText('Số dư khả dụng')).toBeInTheDocument();
    expect(screen.getByText('Chờ quyết toán')).toBeInTheDocument();
    expect(screen.getByText('Đang xử lý rút')).toBeInTheDocument();
    expect(screen.getByText('Tiền buổi học đang giữ')).toBeInTheDocument();

    expect(screen.getByText('1.500.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('500.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('200.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('300.000 ₫')).toBeInTheDocument();
  });

  it('enables Payout button when available balance > 0', () => {
    const onRequestPayout = jest.fn();
    render(<WalletSummaryCard wallet={mockWallet} onRequestPayout={onRequestPayout} />);

    const button = screen.getByRole('button', { name: /yêu cầu rút tiền/i });
    expect(button).not.toBeDisabled();

    fireEvent.click(button);
    expect(onRequestPayout).toHaveBeenCalledTimes(1);
  });

  it('disables Payout button when available balance is 0', () => {
    const zeroWallet: WalletView = { ...mockWallet, availableBalanceVnd: 0 };
    render(<WalletSummaryCard wallet={zeroWallet} />);

    const button = screen.getByRole('button', { name: /yêu cầu rút tiền/i });
    expect(button).toBeDisabled();
  });

  it('does not show a default zero balance when the wallet request fails', () => {
    render(<WalletSummaryCard wallet={null} error onRetry={jest.fn()} />);

    expect(screen.getByText('Chưa tải được số dư ví')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thử tải số dư' })).toBeInTheDocument();
    expect(screen.queryByText('0 ₫')).not.toBeInTheDocument();
  });
});
