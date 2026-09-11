import React from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useAdminPayouts,
  useProcessPayout,
  useCompletePayout,
  useAdminRefunds,
  useAdminExtensions,
  useAdminDashboardStats,
  usePlatformSettings,
  useUpdatePlatformSettings,
  useAdminAuditLogs,
} from './useAdminFinance';
import { adminFinanceApi } from '../api/adminFinanceApi';

jest.mock('../api/adminFinanceApi');

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

describe('useAdminFinance hooks', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('useAdminDashboardStats fetches dashboard summary', async () => {
    const mockStats = {
      success: true,
      data: {
        totalGmvVnd: 100000000,
        totalCommissionVnd: 5000000,
        totalTeachers: 20,
        totalStudents: 100,
        totalBookings: 250,
      },
    };
    (adminFinanceApi.getDashboardStats as jest.Mock).mockResolvedValueOnce(mockStats);

    const { result } = renderHook(() => useAdminDashboardStats(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.data?.totalGmvVnd).toBe(100000000);
  });

  it('useAdminPayouts and useCompletePayout mutate', async () => {
    (adminFinanceApi.getPayouts as jest.Mock).mockResolvedValueOnce({ success: true, data: [] });
    (adminFinanceApi.completePayout as jest.Mock).mockResolvedValueOnce({
      success: true,
      data: { id: 'p-1', status: 'SUCCEEDED' },
    });

    const wrapper = createWrapper();
    const { result: listRes } = renderHook(() => useAdminPayouts(), { wrapper });
    await waitFor(() => expect(listRes.current.isSuccess).toBe(true));

    const { result: completeMutation } = renderHook(() => useCompletePayout(), { wrapper });
    completeMutation.current.mutate({ id: 'p-1', data: { bankReference: 'REF-123', version: 1 } });

    await waitFor(() => expect(completeMutation.current.isSuccess).toBe(true));
    expect(adminFinanceApi.completePayout).toHaveBeenCalledWith('p-1', { bankReference: 'REF-123', version: 1 });
  });

  it('usePlatformSettings and useUpdatePlatformSettings work', async () => {
    const mockSettings = {
      success: true,
      data: {
        id: 's-1',
        commissionRate: 0.05,
        bayesianMinimumReviews: 5,
        bookingReminderHours: 2,
        bookingExpirationHours: 12,
        updatedAt: new Date().toISOString(),
      },
    };
    (adminFinanceApi.getSettings as jest.Mock).mockResolvedValue(mockSettings);
    (adminFinanceApi.updateSettings as jest.Mock).mockResolvedValueOnce({
      success: true,
      data: { ...mockSettings.data, commissionRate: 0.07 },
    });

    const wrapper = createWrapper();
    const { result: getRes } = renderHook(() => usePlatformSettings(), { wrapper });
    await waitFor(() => expect(getRes.current.isSuccess).toBe(true));

    const { result: updateMutation } = renderHook(() => useUpdatePlatformSettings(), { wrapper });
    updateMutation.current.mutate({
      commissionRate: 0.07,
      bayesianMinimumReviews: 5,
      bookingReminderHours: 2,
      bookingExpirationHours: 12,
    });

    await waitFor(() => expect(updateMutation.current.isSuccess).toBe(true));
    expect(adminFinanceApi.updateSettings).toHaveBeenCalled();
  });

  it('useAdminAuditLogs fetches audit log list', async () => {
    (adminFinanceApi.getAuditLogs as jest.Mock).mockResolvedValueOnce({ success: true, data: [] });

    const { result } = renderHook(() => useAdminAuditLogs(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminFinanceApi.getAuditLogs).toHaveBeenCalled();
  });
});
