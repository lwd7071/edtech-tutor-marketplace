import React from 'react';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useCreateInvoice, useInvoiceDetail } from './usePayments';
import { paymentApi } from '../api/paymentApi';

jest.mock('../api/paymentApi');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return Wrapper;
};

describe('usePayments hooks (TDD)', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('useCreateInvoice should call paymentApi.createInvoice', async () => {
    const mockInvoice = {
      success: true,
      message: 'Created',
      data: {
        id: 'inv-123',
        invoiceNumber: 'INV-001',
        pricingPackageId: 'pkg-1',
        amountVnd: 1000000,
        status: 'PENDING' as const,
        checkoutUrl: 'https://pay.payos.vn/123',
        qrCode: '000201...',
        paymentExpiredAt: '2026-08-20T12:00:00Z',
      },
      errors: null,
      meta: null,
    };

    (paymentApi.createInvoice as jest.Mock).mockResolvedValue(mockInvoice);

    const { result } = renderHook(() => useCreateInvoice(), {
      wrapper: createWrapper(),
    });

    let mutateResult;
    await act(async () => {
      mutateResult = await result.current.mutateAsync({
        pricingPackageId: 'pkg-1',
        returnUrl: 'https://app.example/payment-result/inv-123',
      });
    });

    expect(paymentApi.createInvoice).toHaveBeenCalledWith({
      pricingPackageId: 'pkg-1',
      returnUrl: 'https://app.example/payment-result/inv-123',
    });
    expect(mutateResult).toEqual(mockInvoice);
  });

  it('useInvoiceDetail should fetch invoice status and detail', async () => {
    const mockInvoice = {
      success: true,
      message: 'Success',
      data: {
        id: 'inv-123',
        invoiceNumber: 'INV-001',
        pricingPackageId: 'pkg-1',
        amountVnd: 1000000,
        status: 'PAID' as const,
        checkoutUrl: 'https://pay.payos.vn/123',
        qrCode: '000201...',
        paymentExpiredAt: '2026-08-20T12:00:00Z',
        paidAt: '2026-08-20T11:00:00Z',
      },
      errors: null,
      meta: null,
    };

    (paymentApi.getInvoiceDetail as jest.Mock).mockResolvedValue(mockInvoice);

    const { result } = renderHook(() => useInvoiceDetail('inv-123', { polling: true }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(paymentApi.getInvoiceDetail).toHaveBeenCalledWith('inv-123');
    expect(result.current.data?.data?.status).toBe('PAID');
  });
});
