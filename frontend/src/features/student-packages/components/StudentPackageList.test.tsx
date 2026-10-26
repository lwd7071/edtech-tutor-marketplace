import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { StudentPackageList } from './StudentPackageList';
import { useStudentPackages } from '../hooks/useStudentPackages';
import { StudentPackageSummary } from '../types';

jest.mock('../hooks/useStudentPackages', () => ({ useStudentPackages: jest.fn() }));

const packageSummary: StudentPackageSummary = {
  id: 'package-1',
  teacher: { id: 'teacher-1', fullName: 'Trần Thu Hà' },
  subject: { id: 'subject-1', name: 'Toán 11' },
  packageName: 'Toán 11 — 10 buổi',
  totalSessions: 10,
  remainingSessions: 7,
  reservedSessions: 1,
  completedSessions: 2,
  refundedSessions: 0,
  purchasePriceVnd: 1000000,
  expiresAt: '2026-11-17T02:00:00Z',
  status: 'ACTIVE',
  version: 1,
};

const query = useStudentPackages as jest.MockedFunction<typeof useStudentPackages>;
const refetch = jest.fn();

describe('StudentPackageList', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    query.mockReturnValue({
      data: { data: [packageSummary], meta: { totalElements: 1 } },
      isLoading: false,
      isError: false,
      refetch,
    } as unknown as ReturnType<typeof useStudentPackages>);
  });

  it('shows the returned package terms and links to its own detail route', () => {
    render(<StudentPackageList />);

    expect(screen.getByRole('heading', { name: 'Toán 11 — 10 buổi' })).toBeInTheDocument();
    expect(screen.getByText(/Gia sư Trần Thu Hà/)).toBeInTheDocument();
    expect(screen.getByText('7')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Xem chi tiết' })).toHaveAttribute('href', '/student/packages/package-1');
  });

  it('keeps the existing status filter and resets pagination when changing tabs', () => {
    render(<StudentPackageList />);

    fireEvent.click(screen.getByRole('tab', { name: 'Đang hoạt động' }));

    expect(query).toHaveBeenLastCalledWith('ACTIVE', 0, 9);
  });

  it('provides a retry action on API failure and does not present the list as empty', () => {
    query.mockReturnValue({ data: undefined, isLoading: false, isError: true, refetch } as unknown as ReturnType<typeof useStudentPackages>);
    render(<StudentPackageList />);

    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }));

    expect(screen.getByText('Chưa tải được danh sách gói học')).toBeInTheDocument();
    expect(screen.queryByText('Bạn chưa có gói học nào.')).not.toBeInTheDocument();
    expect(refetch).toHaveBeenCalledTimes(1);
  });

  it('offers marketplace navigation only when the unfiltered list is empty', () => {
    query.mockReturnValue({ data: { data: [], meta: { totalElements: 0 } }, isLoading: false, isError: false, refetch } as unknown as ReturnType<typeof useStudentPackages>);
    render(<StudentPackageList />);

    expect(screen.getByText('Bạn chưa có gói học nào.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Tìm gia sư' })).toHaveAttribute('href', '/subjects');
  });

  it('announces loading while the package query is pending', () => {
    query.mockReturnValue({ data: undefined, isLoading: true, isError: false, refetch } as unknown as ReturnType<typeof useStudentPackages>);
    render(<StudentPackageList />);

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải danh sách gói học');
    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true');
  });
});
