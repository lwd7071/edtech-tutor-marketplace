import { studentDashboardApi } from './studentDashboardApi';
import { axiosClient } from '@/shared/api/axiosClient';

jest.mock('@/shared/api/axiosClient', () => ({ axiosClient: { get: jest.fn() } }));

describe('studentDashboardApi', () => {
  it('calls the aggregate endpoint and unwraps the standard envelope', async () => {
    (axiosClient.get as jest.Mock).mockResolvedValue({ data: {
      success: true, message: null,
      data: { nextBookingStartTime: null, remainingSessions: 0, todoAssignments: 0, unreadNotifications: 0, pendingRequests: 0 },
      errors: null, meta: null,
    } });
    const result = await studentDashboardApi.getSummary();
    expect(axiosClient.get).toHaveBeenCalledWith('/api/student/dashboard');
    expect(result.data.pendingRequests).toBe(0);
  });
});
