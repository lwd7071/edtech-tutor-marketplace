export interface WalletView {
  teacherId: string;
  availableBalance: number;
  pendingBalance: number;
  totalEarned: number;
  version: number;
}

export interface LedgerEntryView {
  id: string;
  amount: number;
  type: 'CREDIT' | 'DEBIT' | 'LOCK' | 'RELEASE';
  description: string;
  balanceAfter: number;
  createdAt: string;
}

export interface BankAccountView {
  id: string;
  bankName: string;
  accountNumberMasked: string;
  accountHolderName: string;
  isDefault: boolean;
}

export type PayoutStatus = 'PENDING' | 'PROCESSING' | 'SUCCEEDED' | 'REJECTED' | 'FAILED';

export interface PayoutRequestView {
  id: string;
  teacherId: string;
  amount: number;
  bankAccountId: string;
  status: PayoutStatus;
  createdAt: string;
  processedAt?: string | null;
  adminNote?: string | null;
}
