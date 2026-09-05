import React from 'react';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useStudentBookings,
  useCreateBooking,
  useCompleteBooking,
  useCancelBooking,
} from './useBookings';
import { bookingApi } from '../api/bookingApi';

jest.mock('../api/bookingApi');

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

describe('useBookings hooks (TDD)', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('useStudentBookings should fetch bookings list for student', async () => {
    const mockBookings = {
      success: true,
      message: 'Success',
      data: [
        {
          id: 'b-1',
          teacher: { id: 't-1', fullName: 'Trần Thu Hà' },
          student: { id: 's-1', fullName: 'Nguyễn Minh An' },
          studentPackageId: 'pkg-1',
          subject: { id: 'sub-1', name: 'Toán 11' },
          startTime: '2026-08-20T19:00:00Z',
          endTime: '2026-08-20T20:30:00Z',
          deliveryMode: 'ONLINE' as const,
          meetingLink: 'https://meet.example/abc',
          status: 'SCHEDULED' as const,
          trial: false,
          outsideAvailabilityWarning: false,
          version: 1,
        },
      ],
      errors: null,
      meta: null,
    };

    (bookingApi.getStudentBookings as jest.Mock).mockResolvedValue(mockBookings);

    const { result } = renderHook(() => useStudentBookings({ status: 'SCHEDULED' }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(bookingApi.getStudentBookings).toHaveBeenCalledWith({ status: 'SCHEDULED' });
    expect(result.current.data?.data).toHaveLength(1);
    expect(result.current.data?.data[0].status).toBe('SCHEDULED');
  });

  it('useCreateBooking should call bookingApi.createTeacherBooking', async () => {
    const mockCreated = {
      success: true,
      message: 'Created',
      data: { id: 'b-2' },
      errors: null,
      meta: null,
    };

    (bookingApi.createTeacherBooking as jest.Mock).mockResolvedValue(mockCreated);

    const { result } = renderHook(() => useCreateBooking(), {
      wrapper: createWrapper(),
    });

    let res;
    await act(async () => {
      res = await result.current.mutateAsync({
        studentPackageId: 'pkg-1',
        startTime: '2026-08-21T19:00:00Z',
        endTime: '2026-08-21T20:30:00Z',
        deliveryMode: 'ONLINE',
      });
    });

    expect(bookingApi.createTeacherBooking).toHaveBeenCalled();
    expect(res).toEqual(mockCreated);
  });

  it('useCompleteBooking should call bookingApi.completeBooking with SessionReport', async () => {
    const mockCompleted = {
      success: true,
      message: 'Completed',
      data: { id: 'b-1', status: 'COMPLETED' },
      errors: null,
      meta: null,
    };

    (bookingApi.completeBooking as jest.Mock).mockResolvedValue(mockCompleted);

    const { result } = renderHook(() => useCompleteBooking(), {
      wrapper: createWrapper(),
    });

    let res;
    await act(async () => {
      res = await result.current.mutateAsync({
        id: 'b-1',
        data: {
          version: 1,
          report: {
            content: 'Đã hoàn thành Dao động cơ',
            feedback: 'Nắm chắc kiến thức',
            teacherSelfRating: 5,
          },
        },
      });
    });

    expect(bookingApi.completeBooking).toHaveBeenCalledWith('b-1', {
      version: 1,
      report: {
        content: 'Đã hoàn thành Dao động cơ',
        feedback: 'Nắm chắc kiến thức',
        teacherSelfRating: 5,
      },
    });
    expect(res).toEqual(mockCompleted);
  });

  it('useCancelBooking should call bookingApi.cancelBooking with reason', async () => {
    const mockCancelled = {
      success: true,
      message: 'Cancelled',
      data: { id: 'b-1', status: 'CANCELLED' },
      errors: null,
      meta: null,
    };

    (bookingApi.cancelBooking as jest.Mock).mockResolvedValue(mockCancelled);

    const { result } = renderHook(() => useCancelBooking(), {
      wrapper: createWrapper(),
    });

    let res;
    await act(async () => {
      res = await result.current.mutateAsync({
        id: 'b-1',
        data: {
          version: 1,
          reason: 'Học sinh bận việc đột xuất',
          initiatedBy: 'STUDENT_REQUEST',
        },
      });
    });

    expect(bookingApi.cancelBooking).toHaveBeenCalledWith('b-1', {
      version: 1,
      reason: 'Học sinh bận việc đột xuất',
      initiatedBy: 'STUDENT_REQUEST',
    });
    expect(res).toEqual(mockCancelled);
  });
});
