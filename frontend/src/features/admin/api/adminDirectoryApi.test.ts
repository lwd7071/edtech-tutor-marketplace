import { axiosClient } from '@/shared/api/axiosClient';
import { adminApi } from './adminApi';

jest.mock('@/shared/api/axiosClient', () => ({ axiosClient: { get: jest.fn() } }));

describe('admin user directory API', () => {
  it('sends the complete filter, sort, and pagination contract', async () => {
    (axiosClient.get as jest.Mock).mockResolvedValueOnce({ data: { success: true, data: [], meta: null } });
    const params = { keyword: 'minh', role: 'STUDENT' as const, status: 'LOCKED' as const, page: 2, size: 50, sort: 'email,asc' };
    await adminApi.getUsers(params);
    expect(axiosClient.get).toHaveBeenCalledWith('/api/admin/users', { params });
  });
});
