import React from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useStudentPackages, useStudentPackageDetail } from './useStudentPackages';
import { studentPackageApi } from '../api/studentPackageApi';

jest.mock('../api/studentPackageApi');

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

describe('useStudentPackages hooks (TDD)', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('useStudentPackages should fetch student packages list', async () => {
    const mockPackages = {
      success: true,
      message: 'Success',
      data: [
        {
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
          status: 'ACTIVE' as const,
          version: 1,
        },
      ],
      errors: null,
      meta: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    };

    (studentPackageApi.getStudentPackages as jest.Mock).mockResolvedValue(mockPackages);

    const { result } = renderHook(() => useStudentPackages('ACTIVE', 0, 20), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(studentPackageApi.getStudentPackages).toHaveBeenCalledWith('ACTIVE', 0, 20, 'createdAt,desc');
    expect(result.current.data?.data).toHaveLength(1);
    expect(result.current.data?.data[0].packageName).toBe('Toán 11 — 10 buổi');
  });

  it('useStudentPackageDetail should fetch single student package detail', async () => {
    const mockDetail = {
      success: true,
      message: 'Success',
      data: {
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
        status: 'ACTIVE' as const,
        description: 'Mô tả chi tiết',
        version: 1,
      },
      errors: null,
      meta: null,
    };

    (studentPackageApi.getStudentPackageDetail as jest.Mock).mockResolvedValue(mockDetail);

    const { result } = renderHook(() => useStudentPackageDetail('pkg-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(studentPackageApi.getStudentPackageDetail).toHaveBeenCalledWith('pkg-1');
    expect(result.current.data?.data?.packageName).toBe('Toán 11 — 10 buổi');
  });
});
