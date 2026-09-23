import { render, screen } from '@testing-library/react';
import { AdminAuditLogsPage } from './AdminAuditLogsPage';

const mockUseAdminAuditLogs = jest.fn();
jest.mock('../hooks/useAdminFinance', () => ({ useAdminAuditLogs: (...args: unknown[]) => mockUseAdminAuditLogs(...args) }));
jest.mock('next/navigation', () => ({ useSearchParams: () => new URLSearchParams('targetType=USER&targetId=7f9a5cce-29de-4eb9-b07e-259d0b216eef') }));

describe('AdminAuditLogsPage deep links', () => {
  it('loads user audit history using both target filters from the URL', () => {
    mockUseAdminAuditLogs.mockReturnValue({ data: { data: [], meta: { totalElements: 0 } }, isLoading: false });
    render(<AdminAuditLogsPage />);
    expect(mockUseAdminAuditLogs).toHaveBeenCalledWith(undefined, undefined, 'USER', '7f9a5cce-29de-4eb9-b07e-259d0b216eef', 0, 20);
    expect(screen.getByText(/Đang lọc theo người dùng/)).toBeInTheDocument();
  });
});
