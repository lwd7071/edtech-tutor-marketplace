import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { financeApi } from '../api/financeApi';
import {
  UpsertBankAccountRequest,
  CreatePayoutRequest,
  PayoutStatus,
  CreateRefundRequest,
  CreateExtensionRequest,
} from '../types';
import { financeKeys } from '../data/financeKeys';

export const FINANCE_KEYS = financeKeys;

// ---- Teacher Wallet Hooks ----
export function useTeacherWallet() {
  return useQuery({
    queryKey: FINANCE_KEYS.wallet(),
    queryFn: () => financeApi.getWallet(),
  });
}

export function useTeacherLedger(page = 0, size = 20) {
  return useQuery({
    queryKey: FINANCE_KEYS.ledger(page, size),
    queryFn: () => financeApi.getLedger(page, size),
  });
}

// ---- Teacher Bank Account Hooks ----
export function useTeacherBankAccounts() {
  return useQuery({
    queryKey: FINANCE_KEYS.bankAccounts(),
    queryFn: () => financeApi.getBankAccounts(),
  });
}

export function useCreateBankAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpsertBankAccountRequest) => financeApi.createBankAccount(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FINANCE_KEYS.bankAccounts() });
    },
  });
}

export function useUpdateBankAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpsertBankAccountRequest }) =>
      financeApi.updateBankAccount(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FINANCE_KEYS.bankAccounts() });
    },
  });
}

export function useDeleteBankAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => financeApi.deleteBankAccount(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FINANCE_KEYS.bankAccounts() });
    },
  });
}

// ---- Teacher Payout Hooks ----
export function useTeacherPayouts(status?: PayoutStatus, page = 0, size = 20) {
  return useQuery({
    queryKey: FINANCE_KEYS.payouts(status, page, size),
    queryFn: () => financeApi.getPayoutRequests(status, page, size),
  });
}

export function useCreatePayout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePayoutRequest) => financeApi.createPayoutRequest(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FINANCE_KEYS.payouts() });
      queryClient.invalidateQueries({ queryKey: FINANCE_KEYS.wallet() });
    },
  });
}

// ---- Student Refund Hooks ----
export function useStudentRefunds(page = 0, size = 20) {
  return useQuery({
    queryKey: FINANCE_KEYS.refunds(page, size),
    queryFn: () => financeApi.getStudentRefunds(page, size),
  });
}

export function useCreateRefund() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateRefundRequest) => financeApi.createRefundRequest(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FINANCE_KEYS.refunds(0, 20) });
    },
  });
}

// ---- Student Extension Hooks ----
export function useStudentExtensions(page = 0, size = 20) {
  return useQuery({
    queryKey: FINANCE_KEYS.extensions(page, size),
    queryFn: () => financeApi.getStudentExtensions(page, size),
  });
}

export function useCreateExtension() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateExtensionRequest) => financeApi.createExtensionRequest(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FINANCE_KEYS.extensions(0, 20) });
    },
  });
}
