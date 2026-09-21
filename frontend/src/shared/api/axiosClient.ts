import axios, { AxiosInstance } from 'axios';
import Cookies from 'js-cookie';
import { useAuthStore } from '@/features/auth';
import { BASE_API_URL } from '@/shared/backend/config';

export { BASE_API_URL };

export const axiosClient = axios.create({
  baseURL: BASE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let refreshingSessionId: string | null = null;
let refreshingRefreshToken: string | null = null;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: any) => void;
  sessionId: string | null;
}> = [];

const processQueue = (error: any, token: string | null = null, sessionId: string | null = null) => {
  const queue = failedQueue;
  failedQueue = [];
  queue.forEach((prom) => {
    if (prom.sessionId !== sessionId) {
      prom.reject(error ?? new Error('Authentication session changed'));
      return;
    }
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
      const session = useAuthStore.getState();
      let token = session.accessToken;
      if (!token) {
        token = Cookies.get('accessToken') || null;
      }
      
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      (config as any)._authSessionId = session.sessionId;
      (config as any)._authAccessToken = token;
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
        const current = useAuthStore.getState();
        const requestSessionId = originalRequest._authSessionId ?? null;
        if (!requestSessionId || current.sessionId !== requestSessionId) return Promise.reject(error);

        // A refresh in this session may have completed while this request was
        // still in flight. Reuse the current token instead of starting a
        // second refresh for the stale access-token snapshot.
        if (originalRequest._authAccessToken && current.accessToken && originalRequest._authAccessToken !== current.accessToken) {
          originalRequest._retry = true;
          originalRequest.headers.Authorization = `Bearer ${current.accessToken}`;
          return client(originalRequest);
        }

        if (isRefreshing) {
          if (refreshingSessionId !== requestSessionId) return Promise.reject(error);
          return new Promise(function (resolve, reject) {
            failedQueue.push({ resolve, reject, sessionId: requestSessionId });
          })
            .then((token) => {
              originalRequest.headers.Authorization = 'Bearer ' + token;
              return client(originalRequest);
            })
            .catch((err) => Promise.reject(err));
        }

        originalRequest._retry = true;
        isRefreshing = true;
        refreshingSessionId = requestSessionId;

        const refreshToken = current.refreshToken || Cookies.get('refreshToken') || null;
        refreshingRefreshToken = refreshToken;

        if (!refreshToken) {
          isRefreshing = false;
          processQueue(error, null, requestSessionId);
          if (useAuthStore.getState().sessionId === requestSessionId) useAuthStore.getState().clear();
          refreshingSessionId = null;
          refreshingRefreshToken = null;
          return Promise.reject(error);
        }

        try {
          const { data } = await client.post('/api/auth/refresh', { refreshToken });
          
          if (data.success && data.data) {
            const newAccessToken = data.data.accessToken;
            const newRefreshToken = data.data.refreshToken;

            const latest = useAuthStore.getState();
            if (latest.sessionId !== requestSessionId || latest.refreshToken !== refreshingRefreshToken) {
              processQueue(new Error('Authentication session changed'), null, requestSessionId);
              return Promise.reject(new Error('Authentication session changed'));
            }
            useAuthStore.getState().rotate(newAccessToken, newRefreshToken);
            
            processQueue(null, newAccessToken, requestSessionId);
            originalRequest.headers.Authorization = 'Bearer ' + newAccessToken;
            return client(originalRequest);
          } else {
            throw new Error('Refresh token failed');
          }
        } catch (refreshError) {
          processQueue(refreshError, null, requestSessionId);
          const latest = useAuthStore.getState();
          if (latest.sessionId === requestSessionId && latest.refreshToken === refreshingRefreshToken) latest.clear();
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
          refreshingSessionId = null;
          refreshingRefreshToken = null;
        }
      }
      return Promise.reject(error);
    }
  );
}

setupAxiosInterceptors(axiosClient);

export default axiosClient;
