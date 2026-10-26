import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { AvailabilityEditor } from './AvailabilityEditor';
import { teacherApi } from '@/shared/api/teacher';

jest.mock('@/shared/api/teacher', () => ({
  teacherApi: { getAvailabilities: jest.fn(), replaceAvailabilities: jest.fn() },
}));

jest.mock('antd', () => {
  const antd = jest.requireActual('antd');
  return { ...antd, message: { success: jest.fn(), error: jest.fn() } };
});

jest.mock('react-responsive', () => ({ useMediaQuery: () => false }));

describe('AvailabilityEditor', () => {
  const mockAvailabilities = [
    { id: '1', dayOfWeek: 'MONDAY', startTime: '08:00:00', endTime: '09:00:00', timezone: 'Asia/Ho_Chi_Minh', isActive: true },
    { id: '2', dayOfWeek: 'TUESDAY', startTime: '09:00:00', endTime: '10:00:00', timezone: 'Asia/Ho_Chi_Minh', isActive: true },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getAvailabilities as jest.Mock).mockResolvedValue(mockAvailabilities);
  });

  it('loads and displays the schedule and its timezone', async () => {
    await act(async () => {
      render(<AvailabilityEditor />);
      await Promise.resolve();
    });
    expect(await screen.findByRole('button', { name: /Thứ 2, 08:00/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('heading', { name: 'Khung giờ trong tuần' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lưu lịch rảnh' })).toBeEnabled();
  });

  it('toggles slots and sends the existing replace contract with browser timezone', async () => {
    (teacherApi.replaceAvailabilities as jest.Mock).mockResolvedValue({});
    const originalIntl = Intl.DateTimeFormat;
    global.Intl.DateTimeFormat = function() {
      return { resolvedOptions: () => ({ timeZone: 'Asia/Ho_Chi_Minh' }) } as any;
    } as any;

    try {
      render(<AvailabilityEditor />);
      await act(async () => { await Promise.resolve(); });
      await screen.findByRole('button', { name: /Thứ 2, 08:00/ });
      fireEvent.click(screen.getByRole('button', { name: /Thứ 2, 08:00/ }));
      fireEvent.click(screen.getByRole('button', { name: /Thứ 4, 10:00/ }));
      fireEvent.click(screen.getByRole('button', { name: 'Lưu lịch rảnh' }));

      await waitFor(() => expect(teacherApi.replaceAvailabilities).toHaveBeenCalledWith({
        items: [
          { dayOfWeek: 'TUESDAY', startTime: '09:00', endTime: '10:00', timezone: 'Asia/Ho_Chi_Minh', isActive: true },
          { dayOfWeek: 'WEDNESDAY', startTime: '10:00', endTime: '11:00', timezone: 'Asia/Ho_Chi_Minh', isActive: true },
        ],
      }));
    } finally {
      global.Intl.DateTimeFormat = originalIntl;
    }
  });

  it('blocks saving after a load error', async () => {
    (teacherApi.getAvailabilities as jest.Mock).mockRejectedValue(new Error('unavailable'));
    render(<AvailabilityEditor />);
    await act(async () => { await Promise.resolve(); });
    expect(await screen.findByText('Chưa tải được lịch rảnh')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lưu lịch rảnh' })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Thứ 2, 08:00/ })).toBeDisabled();
  });
});
