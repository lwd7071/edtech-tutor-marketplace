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
  fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Trạng thái minh chứng' }));
  fireEvent.click(await screen.findByText('Đã duyệt'));
  await waitFor(() => expect(adminApi.getCredentialApprovals).toHaveBeenCalledWith('APPROVED'));
});
