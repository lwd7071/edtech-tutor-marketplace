/**
 * Finance Domain Types for Member B (Transaction & Operations)
 * Khớp 100% với Backend DTOs và SPEC-FE
 */

export interface WalletView {
  id: string;
  teacherId: string;
  pendingBalanceVnd: number;
  availableBalanceVnd: number;
  reservedBalanceVnd: number;
  version: number;
}

export type LedgerEntryType =
  | 'SETTLEMENT_CREDIT_PENDING'
  | 'SETTLEMENT_RELEASE_AVAILABLE'
  | 'REFUND_DEBIT_PENDING'
  | 'PAYOUT_RESERVED'
  | 'PAYOUT_RELEASED'
  | 'PAYOUT_SUCCEEDED'
  | 'ADJUSTMENT';

export type BalanceBucket = 'PENDING' | 'AVAILABLE' | 'RESERVED';

export type LedgerDirection = 'CREDIT' | 'DEBIT';

export interface LedgerEntryView {
  id: string;
  walletId: string;
  entryType: LedgerEntryType;
  amountVnd: number;
  balanceBucket: BalanceBucket;
  direction: LedgerDirection;
  referenceType?: string | null;
  referenceId?: string | null;
  description: string;
  createdAt: string;
}

export interface BankAccountView {
  id: string;
  bankBin: string;
  bankName: string;
  accountNumberMasked: string;
  accountHolderName: string;
  isVerified: boolean;
  isDefault: boolean;
  createdAt: string;
  version: number;
}

export interface UpsertBankAccountRequest {
  bankBin: string;
  bankName: string;
  accountNumber: string;
  accountHolderName: string;
  isDefault: boolean;
  version: number;
}

export type PayoutStatus = 'PENDING' | 'PROCESSING' | 'SUCCEEDED' | 'REJECTED' | 'FAILED';

export interface PayoutRequestView {
  id: string;
  teacherId: string;
  walletId: string;
  bankAccountId: string;
  amountVnd: number;
  status: PayoutStatus;
  teacherNote?: string | null;
  adminNote?: string | null;
  bankReference?: string | null;
  proofUrl?: string | null;
  transferredAt?: string | null;
  processedBy?: string | null;
  processedAt?: string | null;
  version: number;
  createdAt: string;
}

export interface CreatePayoutRequest {
  bankAccountId: string;
  amountVnd: number;
  teacherNote?: string;
  walletVersion: number;
}

export type RefundStatus = 'PENDING' | 'APPROVED' | 'PROCESSING' | 'REFUNDED' | 'REJECTED' | 'FAILED';

export interface RefundRequestView {
  id: string;
  studentPackageId: string;
  studentId: string;
  reason: string;
  requestedSessions: number;
  approvedSessions?: number | null;
  refundAmountVnd?: number | null;
  status: RefundStatus;
  adminNote?: string | null;
  bankName?: string | null;
  bankBin?: string | null;
  accountNumberMasked?: string | null;
  accountHolderName?: string | null;
  bankReference?: string | null;
  proofUrl?: string | null;
  processedBy?: string | null;
  processedAt?: string | null;
  version: number;
  createdAt: string;
}

export interface CreateRefundRequest {
  studentPackageId: string;
  reason: string;
  requestedSessions: number;
  bankName: string;
  bankBin: string;
  accountNumber: string;
  accountHolderName: string;
  packageVersion: number;
}

export type ExtensionStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ExtensionRequestView {
  id: string;
  studentPackageId: string;
  studentId: string;
  reason: string;
  requestedExpiryDate: string;
  approvedExpiryDate?: string | null;
  status: ExtensionStatus;
  adminNote?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  createdAt: string;
}

export interface CreateExtensionRequest {
  studentPackageId: string;
  reason: string;
  requestedExpiryDate: string;
  packageVersion: number;
}
