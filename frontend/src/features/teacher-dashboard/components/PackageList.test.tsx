import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PackageList } from './PackageList';
import { teacherApi } from '@/shared/api/teacher';
import { axiosClient } from '@/shared/api/axiosClient';

jest.mock('@/shared/api/teacher', () => ({ teacherApi: { getProfile: jest.fn(), updatePackageStatus: jest.fn() } }));
jest.mock('@/shared/api/axiosClient', () => ({ axiosClient: { get: jest.fn() } }));

const packages = [
  { id: '1', name: 'Toán cơ bản', subjectName: 'Toán', priceVnd: 500000, totalSessions: 10, sessionDurationMinutes: 60, durationDays: 60, status: 'ACTIVE' },
  { id: '2', name: 'Toán nâng cao', subjectName: 'Toán', priceVnd: 800000, totalSessions: 12, sessionDurationMinutes: 60, durationDays: 90, status: 'INACTIVE' },
];
const renderPage = () => render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><App><PackageList onEdit={jest.fn()} onCreate={jest.fn()} /></App></QueryClientProvider>);

describe('PackageList', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getProfile as jest.Mock).mockResolvedValue({ approvalStatus: 'APPROVED' });
    (axiosClient.get as jest.Mock).mockResolvedValue({ data: { data: packages, meta: { page: 0, size: 12, totalElements: 2, totalPages: 1 } } });
  });

  it('renders packages and enables creation for an approved tutor', async () => {
    renderPage();
    expect(await screen.findByText('Toán cơ bản')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tạo gói học' })).toBeEnabled();
  });

  it('disables creation while the profile is not approved', async () => {
    (teacherApi.getProfile as jest.Mock).mockResolvedValue({ approvalStatus: 'PENDING_APPROVAL' });
    renderPage();
    await screen.findByText('Toán cơ bản');
    expect(screen.getByRole('button', { name: 'Tạo gói học' })).toBeDisabled();
  });

  it('updates the package status', async () => {
    (teacherApi.updatePackageStatus as jest.Mock).mockResolvedValue({});
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Ngừng bán' }));
    await waitFor(() => expect(teacherApi.updatePackageStatus).toHaveBeenCalledWith('1', 'INACTIVE'));
  });
});
