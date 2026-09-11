import React from 'react';
import { render, screen } from '@testing-library/react';
import { CreateRefundModal } from './CreateRefundModal';
import { StudentRequestsTable, getRefundStatusTag, getExtensionStatusTag } from './StudentRequestsTable';
import { RefundRequestView, ExtensionRequestView } from '../types';

describe('StudentFinanceComponents', () => {
  it('renders CreateRefundModal with warning alert and inputs', () => {
    render(
      <CreateRefundModal
        open={true}
        packageId="pkg-1"
        packageName="Toán 12 VIP"
        remainingSessions={5}
        estimatedPricePerSession={200000}
        packageVersion={1}
        onCancel={jest.fn()}
        onSubmit={jest.fn()}
      />
    );

    expect(screen.getByText(/Gói học: Toán 12 VIP/i)).toBeInTheDocument();
    expect(screen.getByText(/5 buổi/i)).toBeInTheDocument();
    expect(screen.getByText(/ước tính số tiền hoàn trả:/i)).toBeInTheDocument();
  });

  it('renders StudentRequestsTable with refund and extension tabs', () => {
    const mockRefunds: RefundRequestView[] = [
      {
        id: '11112222-3333-4444-5555-666677778888',
        studentPackageId: 'pkg-1',
        studentId: 'st-1',
        reason: 'Bận ôn thi',
        requestedSessions: 2,
        approvedSessions: 2,
        refundAmountVnd: 400000,
        status: 'REFUNDED',
        bankName: 'MB Bank',
        bankBin: '970422',
        accountNumberMasked: '******1234',
        accountHolderName: 'NGUYEN VAN B',
        version: 1,
        createdAt: '2026-08-25T10:00:00Z',
      },
    ];

    const mockExtensions: ExtensionRequestView[] = [
      {
        id: '99998888-7777-6666-5555-444433332222',
        studentPackageId: 'pkg-1',
        studentId: 'st-1',
        reason: 'Ốm nghỉ',
        requestedExpiryDate: '2026-09-30T00:00:00Z',
        approvedExpiryDate: '2026-09-30T00:00:00Z',
        status: 'APPROVED',
        createdAt: '2026-08-25T10:00:00Z',
      },
    ];

    render(<StudentRequestsTable refunds={mockRefunds} extensions={mockExtensions} />);

    expect(screen.getByText('Yêu cầu hoàn tiền (1)')).toBeInTheDocument();
    expect(screen.getByText('Yêu cầu gia hạn (1)')).toBeInTheDocument();
    expect(screen.getByText('#11112222')).toBeInTheDocument();
    expect(screen.getByText('400.000 ₫')).toBeInTheDocument();
    expect(screen.getByText('Đã hoàn tiền')).toBeInTheDocument();
  });
});
