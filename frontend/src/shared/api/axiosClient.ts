import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/features/auth';

export const BASE_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// Biến quản lý trạng thái Refresh Token Mutex và Hàng đợi Request đồng thời
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

export const axiosClient = axios.create({
  baseURL: BASE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Thiết lập các interceptors cho axios client
 * Được tách thành hàm riêng để thuận tiện cho việc kiểm thử đơn vị (Unit Testing)
 */
export function setupAxiosInterceptors(client: AxiosInstance) {
  // 1. Request Interceptor: Tự động gắn Bearer Access Token
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const accessToken = useAuthStore.getState().accessToken;
      if (accessToken && config.headers) {
        config.headers.Authorization = `Bearer ${accessToken}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // 2. Response Interceptor: Bắt lỗi 401 và xử lý hàng đợi Refresh Token
  client.interceptors.response.use(
    (response) => {
      // Chuẩn hóa trả về response data
      return response.data !== undefined ? response.data : response;
    },
    async (error: AxiosError) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

      // Nếu không phải lỗi 401 hoặc request đã retry rồi thì không xử lý tiếp
      if (error.response?.status !== 401 || originalRequest?._retry) {
        return Promise.reject(error);
      }

      // Đánh dấu request này đang retry để tránh lặp vô tận
      originalRequest._retry = true;

      // Nếu đang trong quá trình refresh token, đưa request vào hàng đợi
      if (isRefreshing) {
        return new Promise<any>((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              if (originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${token}`;
              }
              resolve(typeof client.request === 'function' ? client.request(originalRequest) : client(originalRequest));
            },
            reject: (err: any) => {
              reject(err);
            },
          });
        });
      }

      // Bắt đầu quá trình refresh token
      isRefreshing = true;
      const refreshToken = useAuthStore.getState().refreshToken;

      // Nếu không có refresh token thì đăng xuất luôn
      if (!refreshToken) {
        isRefreshing = false;
        useAuthStore.getState().logout();
        return Promise.reject(error);
      }

      try {
        // Gọi endpoint refresh token
        const refreshResponse: any = await client.post('/auth/refresh-token', {
          refreshToken,
        });

        const resData = refreshResponse?.data !== undefined ? refreshResponse.data : refreshResponse;
        const newAccessToken =
          resData?.accessToken || resData?.data?.accessToken || refreshResponse?.accessToken;
        const newRefreshToken =
          resData?.refreshToken ||
          resData?.data?.refreshToken ||
          refreshResponse?.refreshToken ||
          refreshToken;

        if (!newAccessToken) {
          throw new Error('Không nhận được access token mới từ máy chủ');
        }

        // Cập nhật token mới vào Auth Store
        useAuthStore.getState().setTokens(newAccessToken, newRefreshToken);

        if (client.defaults.headers.common) {
          client.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        }

        // Xử lý tất cả các request đang chờ trong hàng đợi
        processQueue(null, newAccessToken);

        // Retry lại request ban đầu với token mới
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        return typeof client.request === 'function' ? client.request(originalRequest) : client(originalRequest);
      } catch (refreshErr) {
        // Khi refresh token thất bại, từ chối toàn bộ hàng đợi và đăng xuất
        processQueue(refreshErr, null);
        useAuthStore.getState().logout();
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }
  );
}

// Khởi chạy interceptors cho instance mặc định
setupAxiosInterceptors(axiosClient);

export default axiosClient;
