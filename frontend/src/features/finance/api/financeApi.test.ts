import { financeApi } from './financeApi';
import { adminFinanceApi } from '@/features/admin/api/adminFinanceApi';
import { axiosClient } from '@/shared/api/axiosClient';

jest.mock('@/shared/api/axiosClient');

describe('Finance & Admin APIs', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('financeApi', () => {
    it('calls getWallet endpoint', async () => {
      const mockData = {
        success: true,
        data: {
          id: 'wallet-1',
          teacherId: 'teacher-1',
          pendingBalanceVnd: 500000,
          availableBalanceVnd: 1500000,
          reservedBalanceVnd: 0,
          version: 1,
        },
      };
      (axiosClient.get as jest.Mock).mockResolvedValueOnce({ data: mockData });

      const result = await financeApi.getWallet();
      expect(axiosClient.get).toHaveBeenCalledWith('/api/teacher/wallet');
      expect(result.data.availableBalanceVnd).toBe(1500000);
    });

    it('calls getLedger with page and size params', async () => {
      const mockData = { success: true, data: [] };
      (axiosClient.get as jest.Mock).mockResolvedValueOnce({ data: mockData });

      await financeApi.getLedger(1, 10);
      expect(axiosClient.get).toHaveBeenCalledWith('/api/teacher/wallet/ledger', {
        params: { page: 1, size: 10 },
      });
    });

    it('calls createPayoutRequest', async () => {
      const payload = { bankAccountId: 'bank-1', amountVnd: 500000, walletVersion: 1 };
      (axiosClient.post as jest.Mock).mockResolvedValueOnce({ data: { success: true, data: { id: 'payout-1' } } });

      const result = await financeApi.createPayoutRequest(payload);
      expect(axiosClient.post).toHaveBeenCalledWith('/api/teacher/payout-requests', payload,
        expect.objectContaining({ headers: expect.objectContaining({ 'Idempotency-Key': expect.any(String) }) }));
      expect(result.data.id).toBe('payout-1');
    });

    it('calls createRefundRequest', async () => {
      const payload = {
        studentPackageId: 'pkg-1',
        reason: 'Không hợp lịch',
        requestedSessions: 2,
        bankName: 'MB Bank',
        bankBin: '970422',
        accountNumber: '0123456789',
        accountHolderName: 'NGUYEN VAN A',
        packageVersion: 1,
      };
      (axiosClient.post as jest.Mock).mockResolvedValueOnce({ data: { success: true, data: { id: 'ref-1' } } });

      const result = await financeApi.createRefundRequest(payload);
      expect(axiosClient.post).toHaveBeenCalledWith('/api/student/refund-requests', payload,
        expect.objectContaining({ headers: expect.objectContaining({ 'Idempotency-Key': expect.any(String) }) }));
      expect(result.data.id).toBe('ref-1');
    });
  });

  describe('adminFinanceApi', () => {
    it('calls getDashboardStats', async () => {
      const mockData = {
        success: true,
        data: {
          totalGmvVnd: 50000000,
          totalCommissionVnd: 2500000,
          totalTeachers: 10,
          totalStudents: 50,
          totalBookings: 100,
        },
      };
      (axiosClient.get as jest.Mock).mockResolvedValueOnce({ data: mockData });

      const result = await adminFinanceApi.getDashboardStats();
      expect(axiosClient.get).toHaveBeenCalledWith('/api/admin/dashboard');
      expect(result.data.totalCommissionVnd).toBe(2500000);
    });

    it('calls completePayout with bankReference and proofUrl', async () => {
      const payload = { bankReference: 'FT123456', proofUrl: 'https://proof.png', adminNote: 'Done', version: 1 };
      (axiosClient.post as jest.Mock).mockResolvedValueOnce({ data: { success: true, data: { id: 'p-1', status: 'SUCCEEDED' } } });

      const result = await adminFinanceApi.completePayout('p-1', payload);
      expect(axiosClient.post).toHaveBeenCalledWith('/api/admin/payout-requests/p-1/complete', payload,
        expect.objectContaining({ headers: expect.objectContaining({ 'Idempotency-Key': expect.any(String) }) }));
      expect(result.data.status).toBe('SUCCEEDED');
    });
  });
});
