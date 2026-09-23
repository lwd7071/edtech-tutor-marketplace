import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { AdminUsersPage } from './AdminUsersPage';

const mockUseAdminUsers = jest.fn();
jest.mock('../hooks/useAdminApprovals', () => ({
  useAdminUsers: (...args: unknown[]) => mockUseAdminUsers(...args),
}));
jest.mock('../components/UserModerationModal', () => ({
  UserModerationModal: ({ open, user }: { open: boolean; user: { status: string } | null }) =>
    open ? <div role="dialog">Moderate {user?.status}</div> : null,
}));

const user = {
  id: '7f9a5cce-29de-4eb9-b07e-259d0b216eef', fullName: 'Nguyễn Minh An', email: 'an@example.test',
  role: 'STUDENT' as const, status: 'ACTIVE' as const, createdAt: '2026-09-01T00:00:00Z', lastLoginAt: null,
};

describe('AdminUsersPage', () => {
  beforeEach(() => mockUseAdminUsers.mockReturnValue({ data: { data: [user], meta: { totalElements: 1 } }, isLoading: false, isError: false }));

  it('submits search explicitly, shows user actions, and links audit by the full UUID', async () => {
    render(<AdminUsersPage />);
    expect(mockUseAdminUsers).toHaveBeenCalledWith(expect.objectContaining({ keyword: undefined, page: 0, size: 20, sort: 'createdAt,desc' }));
    const input = screen.getByRole('searchbox', { name: 'Tìm người dùng' });
    fireEvent.change(input, { target: { value: 'minh' } });
    expect(mockUseAdminUsers.mock.calls.every(([params]) => (params as { keyword?: string }).keyword === undefined)).toBe(true);
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', charCode: 13 });
    await waitFor(() => expect(mockUseAdminUsers).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: 'minh', page: 0 })));
    expect(screen.getByRole('link', { name: 'Lịch sử khóa/mở khóa' })).toHaveAttribute(
      'href', `/admin/audit-logs?targetType=USER&targetId=${user.id}`,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Khóa' }));
    expect(screen.getByRole('dialog')).toHaveTextContent('Moderate ACTIVE');
  });
});
