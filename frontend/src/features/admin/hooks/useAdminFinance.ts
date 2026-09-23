import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminFinanceApi } from '../api/adminFinanceApi';
import { adminApi } from '../api/adminApi';
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
import { useCommandKey } from '@/shared/lib/useCommandKey';

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
  auditLogs: (actorId?: string, action?: AuditAction, targetType?: string, targetId?: string, page?: number, size?: number) =>
    [...ADMIN_FINANCE_KEYS.all, 'auditLogs', actorId, action, targetType, targetId, page, size] as const,
  bookingSettlements: (status?: string, page?: number, size?: number) => [...ADMIN_FINANCE_KEYS.all, 'bookingSettlements', status, page, size] as const,
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
  const commandKey = useCommandKey<{ id: string; data: ProcessPayoutRequest }>();
  return useMutation({
    mutationFn: (command: { id: string; data: ProcessPayoutRequest }) =>
      adminFinanceApi.processPayout(command.id, command.data, commandKey.forPayload(command)),
    onSuccess: (_, command) => {
      commandKey.clear(command);
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.payouts() });
    },
  });
}

export function useCompletePayout() {
  const queryClient = useQueryClient();
  const commandKey = useCommandKey<{ id: string; data: CompleteTransferRequest }>();
  return useMutation({
    mutationFn: (command: { id: string; data: CompleteTransferRequest }) =>
      adminFinanceApi.completePayout(command.id, command.data, commandKey.forPayload(command)),
    onSuccess: (_, command) => {
      commandKey.clear(command);
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.payouts() });
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.dashboard() });
    },
  });
}

export function useRejectPayout() {
  const queryClient = useQueryClient();
  const commandKey = useCommandKey<{ id: string; data: RejectFinanceRequest }>();
  return useMutation({
    mutationFn: (command: { id: string; data: RejectFinanceRequest }) =>
      adminFinanceApi.rejectPayout(command.id, command.data, commandKey.forPayload(command)),
    onSuccess: (_, command) => {
      commandKey.clear(command);
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
  const commandKey = useCommandKey<{ id: string; data: ApproveRefundRequest }>();
  return useMutation({
    mutationFn: (command: { id: string; data: ApproveRefundRequest }) => adminFinanceApi.approveRefund(command.id, command.data, commandKey.forPayload(command)),
    onSuccess: (_, command) => {
      commandKey.clear(command);
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.refunds() });
    },
  });
}

export function useCompleteRefund() {
  const queryClient = useQueryClient();
  const commandKey = useCommandKey<{ id: string; data: CompleteTransferRequest }>();
  return useMutation({
    mutationFn: (command: { id: string; data: CompleteTransferRequest }) =>
      adminFinanceApi.completeRefund(command.id, command.data, commandKey.forPayload(command)),
    onSuccess: (_, command) => {
      commandKey.clear(command);
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.refunds() });
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.dashboard() });
    },
  });
}

export function useRejectRefund() {
  const queryClient = useQueryClient();
  const commandKey = useCommandKey<{ id: string; data: RejectFinanceRequest }>();
  return useMutation({
    mutationFn: (command: { id: string; data: RejectFinanceRequest }) =>
      adminFinanceApi.rejectRefund(command.id, command.data, commandKey.forPayload(command)),
    onSuccess: (_, command) => {
      commandKey.clear(command);
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
  const commandKey = useCommandKey<{ id: string; data: ApproveExtensionRequest }>();
  return useMutation({
    mutationFn: (command: { id: string; data: ApproveExtensionRequest }) =>
      adminFinanceApi.approveExtension(command.id, command.data, commandKey.forPayload(command)),
    onSuccess: (_, command) => {
      commandKey.clear(command);
      queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.extensions() });
    },
  });
}

export function useRejectExtension() {
  const queryClient = useQueryClient();
  const commandKey = useCommandKey<{ id: string; data: RejectRequest }>();
  return useMutation({
    mutationFn: (command: { id: string; data: RejectRequest }) =>
      adminFinanceApi.rejectExtension(command.id, command.data, commandKey.forPayload(command)),
    onSuccess: (_, command) => {
      commandKey.clear(command);
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
  targetId?: string,
  page = 0,
  size = 20
) {
  return useQuery({
    queryKey: ADMIN_FINANCE_KEYS.auditLogs(actorId, action, targetType, targetId, page, size),
    queryFn: () => adminFinanceApi.getAuditLogs(actorId, action, targetType, targetId, page, size),
  });
}

export function useAdminBookingSettlements(status?: string, page = 0, size = 20) {
  return useQuery({ queryKey: ADMIN_FINANCE_KEYS.bookingSettlements(status, page, size), queryFn: () => adminApi.getBookingSettlements(status, page, size) });
}

export function useBookingSettlementAction(action: 'reopen' | 'release' | 'retain') {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, version, note }: { id: string; version: number; note: string }) => {
      const data = { version, note };
      if (action === 'reopen') return adminApi.reopenBookingSettlement(id, data);
      if (action === 'release') return adminApi.releaseBookingSettlement(id, data);
      return adminApi.retainBookingSettlement(id, data);
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.bookingSettlements() }); queryClient.invalidateQueries({ queryKey: ADMIN_FINANCE_KEYS.dashboard() }); },
  });
}
