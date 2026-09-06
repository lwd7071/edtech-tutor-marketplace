import axiosClient from './axiosClient';

// Types mapping backend DTOs
export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  status: string;
  avatarUrl?: string;
}

export interface AuthResult {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessTokenExpiresIn: number;
  user: UserSummary;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
}

export const authApi = {
  login: async (data: any): Promise<ApiResponse<AuthResult>> => {
    const response = await axiosClient.post('/api/auth/login', data);
    return response.data;
  },

  register: async (data: any): Promise<ApiResponse<AuthResult>> => {
    const response = await axiosClient.post('/api/auth/register', data);
    return response.data;
  },

  refresh: async (refreshToken: string): Promise<ApiResponse<AuthResult>> => {
    const response = await axiosClient.post('/api/auth/refresh', { refreshToken });
    return response.data;
  },

  logout: async (refreshToken: string): Promise<ApiResponse<void>> => {
    const response = await axiosClient.post('/api/auth/logout', { refreshToken });
    return response.data;
  },

  verifyEmail: async (token: string): Promise<ApiResponse<void>> => {
    const response = await axiosClient.post('/api/auth/verify-email', { token });
    return response.data;
  },

  resendVerification: async (email: string): Promise<ApiResponse<void>> => {
    // using ForgotPasswordRequest DTO shape as per backend controller
    const response = await axiosClient.post('/api/auth/resend-verification', { email });
    return response.data;
  },

  forgotPassword: async (email: string): Promise<ApiResponse<void>> => {
    const response = await axiosClient.post('/api/auth/forgot-password', { email });
    return response.data;
  },

  resetPassword: async (data: any): Promise<ApiResponse<void>> => {
    const response = await axiosClient.post('/api/auth/reset-password', data);
    return response.data;
  },

  exchangeOAuthToken: async (data: any): Promise<ApiResponse<AuthResult>> => {
    const response = await axiosClient.post('/api/auth/oauth2/exchange', data);
    return response.data;
  },

  completeOAuthRegistration: async (data: any): Promise<ApiResponse<AuthResult>> => {
    const response = await axiosClient.post('/api/auth/oauth2/complete-registration', data);
    return response.data;
  },

  updateParentContact: async (data: any): Promise<ApiResponse<any>> => {
    const response = await axiosClient.put('/api/student/parent-contact', data);
    return response.data;
  },
};
