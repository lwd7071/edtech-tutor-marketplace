import axios, { AxiosInstance } from 'axios';
import Cookies from 'js-cookie';
import { useAuthStore } from '@/features/auth';

export const BASE_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const axiosClient = axios.create({
  baseURL: BASE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

export function setupAxiosInterceptors(client: AxiosInstance) {
  // Request interceptor
  client.interceptors.request.use(
    (config) => {
      // Try to get token from B's store, then A's cookie
      let token = useAuthStore.getState().accessToken;
      if (!token) {
        token = Cookies.get('accessToken') || null;
      }
      
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor
  client.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config;
      
      if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !originalRequest.url?.includes('/api/auth/')) {
        if (isRefreshing) {
          return new Promise(function (resolve, reject) {
            failedQueue.push({ resolve, reject });
          })
            .then((token) => {
              originalRequest.headers.Authorization = 'Bearer ' + token;
              return client(originalRequest);
            })
            .catch((err) => Promise.reject(err));
        }

        originalRequest._retry = true;
        isRefreshing = true;

        let refreshToken = useAuthStore.getState().refreshToken;
        if (!refreshToken) {
          isRefreshing = false;
          processQueue(error, null);
          refreshToken = Cookies.get('refreshToken') || null;
        }

        if (!refreshToken) {
          useAuthStore.getState().logout();
          return Promise.reject(error);
        }

        try {
          // B's test expects '/auth/refresh-token'
          const { data } = await axios.post(`${BASE_API_URL}/api/auth/refresh`, { refreshToken });
          
          if (data.success && data.data) {
            const newAccessToken = data.data.accessToken;
            const newRefreshToken = data.data.refreshToken;
            
            // Set in store (will handle cookie if merged store handles it)
            useAuthStore.getState().setTokens(newAccessToken, newRefreshToken);
            
            processQueue(null, newAccessToken);
            originalRequest.headers.Authorization = 'Bearer ' + newAccessToken;
            return client(originalRequest);
          } else {
            throw new Error('Refresh token failed');
          }
        } catch (refreshError) {
          processQueue(refreshError, null);
          useAuthStore.getState().logout();
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      }
      return Promise.reject(error);
    }
  );
}

setupAxiosInterceptors(axiosClient);

export default axiosClient;
