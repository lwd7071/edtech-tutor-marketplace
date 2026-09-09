import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse } from '@/shared/backend';
import {
  PayoutRequestView,
  RefundRequestView,
  ExtensionRequestView,
} from '@/features/finance/types';
import {
  AdminDashboardView,
  PlatformSettingsView,
  UpdatePlatformSettingsRequest,
  AuditLogView,
  AuditAction,
  ProcessPayoutRequest,
  CompleteTransferRequest,
  ProcessRefundRequest,
  ApproveExtensionRequest,
  RejectRequest,
} from '../types';

export const adminFinanceApi = {
  // ---- Admin Payout Queue ----
  getPayouts: async (status?: string, page = 0, size = 20): Promise<ApiResponse<PayoutRequestView[]>> => {
    const response = await axiosClient.get<ApiResponse<PayoutRequestView[]>>('/api/admin/payout-requests', {
      params: { status, page, size },
    });
    return response.data;
  },

  processPayout: async (id: string, data?: ProcessPayoutRequest): Promise<ApiResponse<PayoutRequestView>> => {
    const response = await axiosClient.post<ApiResponse<PayoutRequestView>>(
      `/api/admin/payout-requests/${id}/process`,
      data || {}
    );
    return response.data;
  },

  completePayout: async (id: string, data: CompleteTransferRequest): Promise<ApiResponse<PayoutRequestView>> => {
    const response = await axiosClient.post<ApiResponse<PayoutRequestView>>(
      `/api/admin/payout-requests/${id}/complete`,
      data
    );
    return response.data;
  },

  rejectPayout: async (id: string, data: RejectRequest): Promise<ApiResponse<PayoutRequestView>> => {
    const response = await axiosClient.post<ApiResponse<PayoutRequestView>>(
      `/api/admin/payout-requests/${id}/reject`,
      data
    );
    return response.data;
  },

  // ---- Admin Refund Queue ----
  getRefunds: async (status?: string, page = 0, size = 20): Promise<ApiResponse<RefundRequestView[]>> => {
    const response = await axiosClient.get<ApiResponse<RefundRequestView[]>>('/api/admin/refund-requests', {
      params: { status, page, size },
    });
    return response.data;
  },

  approveRefund: async (id: string): Promise<ApiResponse<RefundRequestView>> => {
    const response = await axiosClient.post<ApiResponse<RefundRequestView>>(
      `/api/admin/refund-requests/${id}/approve`
    );
    return response.data;
  },

  processRefund: async (id: string, data: ProcessRefundRequest): Promise<ApiResponse<RefundRequestView>> => {
    const response = await axiosClient.post<ApiResponse<RefundRequestView>>(
      `/api/admin/refund-requests/${id}/process`,
      data
    );
    return response.data;
  },

  rejectRefund: async (id: string, data: RejectRequest): Promise<ApiResponse<RefundRequestView>> => {
    const response = await axiosClient.post<ApiResponse<RefundRequestView>>(
      `/api/admin/refund-requests/${id}/reject`,
      data
    );
    return response.data;
  },

  // ---- Admin Extension Queue ----
  getExtensions: async (status?: string, page = 0, size = 20): Promise<ApiResponse<ExtensionRequestView[]>> => {
    const response = await axiosClient.get<ApiResponse<ExtensionRequestView[]>>('/api/admin/extension-requests', {
      params: { status, page, size },
    });
    return response.data;
  },

  approveExtension: async (id: string, data: ApproveExtensionRequest): Promise<ApiResponse<ExtensionRequestView>> => {
    const response = await axiosClient.post<ApiResponse<ExtensionRequestView>>(
      `/api/admin/extension-requests/${id}/approve`,
      data
    );
    return response.data;
  },

  rejectExtension: async (id: string, data: RejectRequest): Promise<ApiResponse<ExtensionRequestView>> => {
    const response = await axiosClient.post<ApiResponse<ExtensionRequestView>>(
      `/api/admin/extension-requests/${id}/reject`,
      data
    );
    return response.data;
  },

  // ---- Admin Dashboard ----
  getDashboardStats: async (): Promise<ApiResponse<AdminDashboardView>> => {
    const response = await axiosClient.get<ApiResponse<AdminDashboardView>>('/api/admin/dashboard');
    return response.data;
  },

  // ---- Admin Platform Settings ----
  getSettings: async (): Promise<ApiResponse<PlatformSettingsView>> => {
    const response = await axiosClient.get<ApiResponse<PlatformSettingsView>>('/api/admin/settings');
    return response.data;
  },

  updateSettings: async (data: UpdatePlatformSettingsRequest): Promise<ApiResponse<PlatformSettingsView>> => {
    const response = await axiosClient.put<ApiResponse<PlatformSettingsView>>('/api/admin/settings', data);
    return response.data;
  },

  // ---- Admin Audit Logs ----
  getAuditLogs: async (
    actorId?: string,
    action?: AuditAction,
    targetType?: string,
    page = 0,
    size = 20
  ): Promise<ApiResponse<AuditLogView[]>> => {
    const response = await axiosClient.get<ApiResponse<AuditLogView[]>>('/api/admin/audit-logs', {
      params: { actorId, action, targetType, page, size },
    });
    return response.data;
  },
};
