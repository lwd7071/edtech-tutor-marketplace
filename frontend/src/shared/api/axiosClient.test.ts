import axios from 'axios';
import { axiosClient, setupAxiosInterceptors } from './axiosClient';
import { useAuthStore } from '@/features/auth';

// Mock axios instance
jest.mock('axios', () => {
  const mAxiosInstance: any = jest.fn((config) => Promise.resolve({ data: { success: true } }));
  mAxiosInstance.get = jest.fn();
  mAxiosInstance.post = jest.fn();
  mAxiosInstance.request = jest.fn((config) => Promise.resolve({ data: { success: true } }));
  mAxiosInstance.interceptors = {
    request: { use: jest.fn() },
    response: { use: jest.fn() },
  };
  mAxiosInstance.defaults = { headers: { common: {} } };
  return {
    create: jest.fn(() => mAxiosInstance),
    isAxiosError: jest.fn((err) => !!err?.isAxiosError),
  };
});

describe('AxiosClient with Refresh-Token Queue (TDD)', () => {
  let requestInterceptor: (config: any) => any;
  let responseErrorInterceptor: (error: any) => Promise<any>;

  beforeEach(() => {
    jest.clearAllMocks();
    useAuthStore.getState().clear();

    // Setup interceptors capture
    const mockClient = axios.create();
    setupAxiosInterceptors(mockClient);

    const requestUse = (mockClient.interceptors.request.use as jest.Mock).mock;
    const responseUse = (mockClient.interceptors.response.use as jest.Mock).mock;

    requestInterceptor = requestUse.calls[requestUse.calls.length - 1][0];
    responseErrorInterceptor = responseUse.calls[responseUse.calls.length - 1][1];
  });

  it('should attach Bearer token to request headers when user is authenticated', () => {
    useAuthStore.getState().rotate('valid-access-token', 'valid-refresh-token');

    const config = { headers: {} };
    const modifiedConfig = requestInterceptor(config);

    expect(modifiedConfig.headers.Authorization).toBe('Bearer valid-access-token');
  });

  it('should not attach Authorization header if accessToken is null', () => {
    useAuthStore.getState().clear();

    const config = { headers: {} };
    const modifiedConfig = requestInterceptor(config);

    expect(modifiedConfig.headers.Authorization).toBeUndefined();
  });

  it('should handle concurrent 401s: refresh token once and retry all queued requests', async () => {
    useAuthStore.getState().rotate('expired-access-token', 'valid-refresh-token');

    const mockClient = axios.create();
    const mockPost = mockClient.post as jest.Mock;

    // Giả lập API refresh token trả về token mới
    mockPost.mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          accessToken: 'new-refreshed-token',
          refreshToken: 'new-refresh-token',
        },
      },
    });

    // Giả lập 3 requests bị lỗi 401 cùng lúc
    const error1 = {
      config: { url: '/api/v1/bookings', headers: {} },
      response: { status: 401 },
      isAxiosError: true,
    };
    const error2 = {
      config: { url: '/api/v1/packages', headers: {} },
      response: { status: 401 },
      isAxiosError: true,
    };

    // Khi retry chạy lại qua mockClient
    (mockClient as any).mockImplementation = jest.fn().mockResolvedValue({ data: { success: true } });

    // Kích hoạt đồng thời 2 error
    const promise1 = responseErrorInterceptor(error1);
    const promise2 = responseErrorInterceptor(error2);

    // Refresh API chỉ được gọi 1 lần duy nhất
    expect(mockPost).toHaveBeenCalledTimes(1);
    expect(mockPost).toHaveBeenCalledWith(
      '/api/auth/refresh',
      { refreshToken: 'valid-refresh-token' }
    );

    await Promise.all([promise1, promise2]);

    // AuthStore đã được cập nhật token mới
    expect(useAuthStore.getState().accessToken).toBe('new-refreshed-token');
  });

  it('should clear tokens and logout when refresh token fails', async () => {
    useAuthStore.getState().rotate('expired-access-token', 'invalid-refresh-token');

    const mockClient = axios.create();
    const mockPost = mockClient.post as jest.Mock;

    // Giả lập API refresh bị lỗi 401/403
    mockPost.mockRejectedValueOnce({
      response: { status: 401 },
    });

    const error = {
      config: { url: '/api/v1/user/me', headers: {} },
      response: { status: 401 },
      isAxiosError: true,
    };

    await expect(responseErrorInterceptor(error)).rejects.toBeDefined();

    // Store đã bị logout
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().accessToken).toBeNull();
  });
});
