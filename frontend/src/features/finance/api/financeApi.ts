import { axiosClient } from '@/shared/api/axiosClient';
import { ApiResponse, ApiResponseWithData, requireApiData } from '@/shared/backend';
import {
  WalletView,
  LedgerEntryView,
  BankAccountView,
  UpsertBankAccountRequest,
  PayoutRequestView,
  CreatePayoutRequest,
  PayoutStatus,
  RefundRequestView,
  CreateRefundRequest,
  ExtensionRequestView,
  CreateExtensionRequest,
} from '../types';

const idempotencyConfig = (key?: string) => ({
  headers: { 'Idempotency-Key': key ?? globalThis.crypto.randomUUID() },
});

export const financeApi = {
  // ---- Teacher Wallet & Ledger ----
  getWallet: async (): Promise<ApiResponseWithData<WalletView>> => {
    const response = await axiosClient.get<ApiResponse<WalletView>>('/api/teacher/wallet');
    return requireApiData(response.data);
  },

  getLedger: async (page = 0, size = 20): Promise<ApiResponseWithData<LedgerEntryView[]>> => {
    const response = await axiosClient.get<ApiResponse<LedgerEntryView[]>>('/api/teacher/wallet/ledger', {
      params: { page, size },
    });
    return requireApiData(response.data);
  },

  // ---- Teacher Bank Accounts ----
  getBankAccounts: async (): Promise<ApiResponseWithData<BankAccountView[]>> => {
    const response = await axiosClient.get<ApiResponse<BankAccountView[]>>('/api/teacher/bank-accounts');
    return requireApiData(response.data);
  },

  createBankAccount: async (data: UpsertBankAccountRequest, idempotencyKey?: string): Promise<ApiResponseWithData<BankAccountView>> => {
    const response = await axiosClient.post<ApiResponse<BankAccountView>>('/api/teacher/bank-accounts', data, idempotencyConfig(idempotencyKey));
    return requireApiData(response.data);
  },

  updateBankAccount: async (id: string, data: UpsertBankAccountRequest): Promise<ApiResponseWithData<BankAccountView>> => {
    const response = await axiosClient.put<ApiResponse<BankAccountView>>(`/api/teacher/bank-accounts/${id}`, data);
    return requireApiData(response.data);
  },

  deleteBankAccount: async (id: string, version: number): Promise<void> => {
    await axiosClient.delete(`/api/teacher/bank-accounts/${id}`, { headers: { 'If-Match': `"${version}"` } });
  },

  // ---- Teacher Payout Requests ----
  getPayoutRequests: async (
    status?: PayoutStatus,
    page = 0,
    size = 20
  ): Promise<ApiResponse<PayoutRequestView[]>> => {
    const response = await axiosClient.get<ApiResponse<PayoutRequestView[]>>('/api/teacher/payout-requests', {
      params: { status, page, size },
    });
    return response.data;
  },

  createPayoutRequest: async (data: CreatePayoutRequest, idempotencyKey?: string): Promise<ApiResponseWithData<PayoutRequestView>> => {
    const response = await axiosClient.post<ApiResponse<PayoutRequestView>>('/api/teacher/payout-requests', data, idempotencyConfig(idempotencyKey));
    return requireApiData(response.data);
  },

  // ---- Student Refunds ----
  getStudentRefunds: async (page = 0, size = 20): Promise<ApiResponse<RefundRequestView[]>> => {
    const response = await axiosClient.get<ApiResponse<RefundRequestView[]>>('/api/student/refund-requests', {
      params: { page, size },
    });
    return response.data;
  },

  createRefundRequest: async (data: CreateRefundRequest, idempotencyKey?: string): Promise<ApiResponseWithData<RefundRequestView>> => {
    const response = await axiosClient.post<ApiResponse<RefundRequestView>>('/api/student/refund-requests', data, idempotencyConfig(idempotencyKey));
    return requireApiData(response.data);
  },

  // ---- Student Extensions ----
  getStudentExtensions: async (page = 0, size = 20): Promise<ApiResponse<ExtensionRequestView[]>> => {
    const response = await axiosClient.get<ApiResponse<ExtensionRequestView[]>>('/api/student/extension-requests', {
      params: { page, size },
    });
    return response.data;
  },

  createExtensionRequest: async (data: CreateExtensionRequest, idempotencyKey?: string): Promise<ApiResponse<ExtensionRequestView>> => {
    const response = await axiosClient.post<ApiResponse<ExtensionRequestView>>('/api/student/extension-requests', data, idempotencyConfig(idempotencyKey));
    return response.data;
  },
};
