import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminFinanceApi } from '../api/adminFinanceApi';
import {
  AuditAction,
  ProcessPayoutRequest,
  CompleteTransferRequest,
  ApproveRefundRequest,
  RejectFinanceRequest,
  ApproveExtensionRequest,
  RejectRequest,
  UpdatePlatformSettingsRequest,
} from '../types';

export const ADMIN_FINANCE_KEYS = {
  all: ['adminFinance'] as const,
  payouts: (status?: string, page?: number, size?: number) =>
    [...ADMIN_FINANCE_KEYS.all, 'payouts', status, page, size] as const,
  refunds: (status?: string, page?: number, size?: number) =>
    [...ADMIN_FINANCE_KEYS.all, 'refunds', status, page, size] as const,
  extensions: (status?: string, page?: number, size?: number) =>
    [...ADMIN_FINANCE_KEYS.all, 'extensions', status, page, size] as const,
  dashboard: () => [...ADMIN_FINANCE_KEYS.all, 'dashboard'] as const,
  settings: () => [...ADMIN_FINANCE_KEYS.all, 'settings'] as const,
  auditLogs: (actorId?: string, action?: AuditAction, targetType?: string, page?: number, size?: number) =>
    [...ADMIN_FINANCE_KEYS.all, 'auditLogs', actorId, action, targetType, page, size] as const,
};

// ---- Admin Payout Queue Hooks ----
export function useAdminPayouts(status?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.payouts(status, page, size),
    queryFn: () => adminFinanceApi.getPayouts(status, page, size),
  });
}

export function useProcessPayout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ProcessPayoutRequest }) =>
      adminFinanceApi.processPayout(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.payouts() });
    },
  });
}

export function useCompletePayout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CompleteTransferRequest }) =>
      adminFinanceApi.completePayout(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.payouts() });
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.dashboard() });
    },
  });
}

export function useRejectPayout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectFinanceRequest }) =>
      adminFinanceApi.rejectPayout(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.payouts() });
    },
  });
}

// ---- Admin Refund Queue Hooks ----
export function useAdminRefunds(status?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.refunds(status, page, size),
    queryFn: () => adminFinanceApi.getRefunds(status, page, size),
  });
}

export function useApproveRefund() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ApproveRefundRequest }) => adminFinanceApi.approveRefund(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.refunds() });
    },
  });
}

export function useCompleteRefund() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CompleteTransferRequest }) =>
      adminFinanceApi.completeRefund(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.refunds() });
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.dashboard() });
    },
  });
}

export function useRejectRefund() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectFinanceRequest }) =>
      adminFinanceApi.rejectRefund(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.refunds() });
    },
  });
}

// ---- Admin Extension Queue Hooks ----
export function useAdminExtensions(status?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.extensions(status, page, size),
    queryFn: () => adminFinanceApi.getExtensions(status, page, size),
  });
}

export function useApproveExtension() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ApproveExtensionRequest }) =>
      adminFinanceApi.approveExtension(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.extensions() });
    },
  });
}

export function useRejectExtension() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectRequest }) =>
      adminFinanceApi.rejectExtension(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.extensions() });
    },
  });
}

// ---- Admin Dashboard, Settings & Audit Logs Hooks ----
export function useAdminDashboardStats() {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.dashboard(),
    queryFn: () => adminFinanceApi.getDashboardStats(),
  });
}

export function usePlatformSettings() {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.settings(),
    queryFn: () => adminFinanceApi.getSettings(),
  });
}

export function useUpdatePlatformSettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdatePlatformSettingsRequest) => adminFinanceApi.updateSettings(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.settings() });
    },
  });
}

export function useAdminAuditLogs(
  actorId?: string,
  action?: AuditAction,
  targetType?: string,
  page = 0,
  size = 20
) {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.auditLogs(actorId, action, targetType, page, size),
    queryFn: () => adminFinanceApi.getAuditLogs(actorId, action, targetType, page, size),
  });
}
