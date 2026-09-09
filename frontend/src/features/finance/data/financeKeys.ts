import type { PayoutStatus } from '../types';

export const financeKeys = {
  all: ['finance'] as const,
  wallet: () => [...financeKeys.all, 'wallet'] as const,
  ledger: (page?: number, size?: number) => [...financeKeys.all, 'ledger', { page, size }] as const,
  bankAccounts: () => [...financeKeys.all, 'bank-accounts'] as const,
  payouts: (status?: PayoutStatus, page?: number, size?: number) => [...financeKeys.all, 'payouts', { status, page, size }] as const,
  refunds: (page?: number, size?: number) => [...financeKeys.all, 'refunds', { page, size }] as const,
  extensions: (page?: number, size?: number) => [...financeKeys.all, 'extensions', { page, size }] as const,
};
