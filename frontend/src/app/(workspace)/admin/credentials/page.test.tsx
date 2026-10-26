import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import AdminCredentialsPage from './page';
import { adminApi } from '@/features/admin/api/adminApi';

jest.mock('@/features/admin/api/adminApi', () => ({ adminApi: {
  getCredentialApprovals: jest.fn(), getCredentialProof: jest.fn(),
  approveCredential: jest.fn(), rejectCredential: jest.fn(),
} }));

beforeEach(() => {
  jest.mocked(adminApi.getCredentialApprovals).mockResolvedValue({ data: [] } as never);
});

afterEach(() => jest.clearAllMocks());

test('loads pending credentials then allows reviewing approved credentials', async () => {
  render(<AdminCredentialsPage />);
  await waitFor(() => expect(adminApi.getCredentialApprovals).toHaveBeenCalledWith('PENDING'));
  fireEvent.click(screen.getByRole('tab', { name: 'Đã duyệt' }));
  await waitFor(() => expect(adminApi.getCredentialApprovals).toHaveBeenCalledWith('APPROVED'));
});
