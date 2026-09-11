import axiosClient from './axiosClient';
import type { ApiResponse } from '@/shared/backend';

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
export interface RegistrationResult { email: string; verificationRequired: boolean }

export interface LoginRequest { email: string; password: string; deviceInfo?: string }
export interface RegisterRequest { fullName: string; email: string; password: string; role: 'STUDENT' | 'TEACHER' }
export interface ResetPasswordRequest { token: string; newPassword: string }
export interface OAuthExchangeRequest { exchangeCode: string }
export interface CompleteOAuthRegistrationRequest { registrationToken: string; role: 'STUDENT' | 'TEACHER' }
export interface ParentContactRequest { parentFullName: string | null; parentPhone: string | null; parentEmail: string | null; notifyParent: boolean }
export interface ParentContactResponse extends ParentContactRequest { updatedAt: string | null }

export const authApi = {
  login: async (data: LoginRequest): Promise<ApiResponse<AuthResult>> => {
    const response = await axiosClient.post('/api/auth/login', data);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<ApiResponse<RegistrationResult>> => {
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

  resetPassword: async (data: ResetPasswordRequest): Promise<ApiResponse<void>> => {
    const response = await axiosClient.post('/api/auth/reset-password', data);
    return response.data;
  },

  exchangeOAuthToken: async (data: OAuthExchangeRequest): Promise<ApiResponse<AuthResult>> => {
    const response = await axiosClient.post('/api/auth/oauth2/exchange', data);
    return response.data;
  },

  completeOAuthRegistration: async (data: CompleteOAuthRegistrationRequest): Promise<ApiResponse<AuthResult>> => {
    const response = await axiosClient.post('/api/auth/oauth2/complete-registration', data);
    return response.data;
  },

  updateParentContact: async (data: ParentContactRequest): Promise<ApiResponse<ParentContactResponse>> => {
    const response = await axiosClient.put('/api/student/parent-contact', data);
    return response.data;
  },
  getParentContact: async (): Promise<ApiResponse<ParentContactResponse>> => {
    const response = await axiosClient.get('/api/student/parent-contact');
    return response.data;
  },
};
