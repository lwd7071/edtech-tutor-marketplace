import React from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useTeacherWallet,
  useTeacherLedger,
  useTeacherBankAccounts,
  useCreatePayout,
  useStudentRefunds,
  useStudentExtensions,
} from './useFinance';
import { financeApi } from '../api/financeApi';

jest.mock('../api/financeApi');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'QueryClientWrapper';
  return Wrapper;
};

describe('useFinance hooks', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('useTeacherWallet fetches wallet successfully', async () => {
    const mockWallet = {
      success: true,
      data: {
        id: 'w-1',
        teacherId: 't-1',
        pendingBalanceVnd: 200000,
        availableBalanceVnd: 1000000,
        reservedBalanceVnd: 0,
        version: 1,
      },
    };
    (financeApi.getWallet as jest.Mock).mockResolvedValueOnce(mockWallet);

    const { result } = renderHook(() => useTeacherWallet(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.data?.availableBalanceVnd).toBe(1000000);
  });

  it('useTeacherLedger fetches ledger entries', async () => {
    const mockLedger = {
      success: true,
      data: [
        {
          id: 'l-1',
          walletId: 'w-1',
          entryType: 'PAYOUT_RESERVED',
          amountVnd: 500000,
          balanceBucket: 'AVAILABLE',
          direction: 'DEBIT',
          description: 'Rút tiền',
          createdAt: new Date().toISOString(),
        },
      ],
    };
    (financeApi.getLedger as jest.Mock).mockResolvedValueOnce(mockLedger);

    const { result } = renderHook(() => useTeacherLedger(0, 10), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.data).toHaveLength(1);
    expect(result.current.data?.data?.[0].amountVnd).toBe(500000);
  });

  it('useTeacherBankAccounts fetches bank accounts', async () => {
    const mockAccounts = {
      success: true,
      data: [
        {
          id: 'b-1',
          bankBin: '970422',
          bankName: 'MB Bank',
          accountNumberMasked: '******1234',
          accountHolderName: 'NGUYEN VAN A',
          isVerified: true,
          isDefault: true,
          createdAt: new Date().toISOString(),
        },
      ],
    };
    (financeApi.getBankAccounts as jest.Mock).mockResolvedValueOnce(mockAccounts);

    const { result } = renderHook(() => useTeacherBankAccounts(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.data?.[0].bankName).toBe('MB Bank');
  });

  it('useCreatePayout mutates and invalidates queries', async () => {
    (financeApi.createPayoutRequest as jest.Mock).mockResolvedValueOnce({
      success: true,
      data: { id: 'payout-1', amountVnd: 500000, status: 'PENDING' },
    });

    const { result } = renderHook(() => useCreatePayout(), { wrapper: createWrapper() });

    result.current.mutate({ bankAccountId: 'b-1', amountVnd: 500000 });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(financeApi.createPayoutRequest).toHaveBeenCalledWith({ bankAccountId: 'b-1', amountVnd: 500000 });
  });

  it('useStudentRefunds and useStudentExtensions fetch lists', async () => {
    (financeApi.getStudentRefunds as jest.Mock).mockResolvedValueOnce({ success: true, data: [] });
    (financeApi.getStudentExtensions as jest.Mock).mockResolvedValueOnce({ success: true, data: [] });

    const wrapper = createWrapper();
    const { result: refundRes } = renderHook(() => useStudentRefunds(), { wrapper });
    const { result: extRes } = renderHook(() => useStudentExtensions(), { wrapper });

    await waitFor(() => expect(refundRes.current.isSuccess).toBe(true));
    await waitFor(() => expect(extRes.current.isSuccess).toBe(true));
  });
});
