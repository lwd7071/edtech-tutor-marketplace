import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PackageForm } from './PackageForm';
import { teacherApi } from '@/shared/api/teacher';

jest.mock('@/shared/api/teacher', () => ({ teacherApi: { getSubjects: jest.fn(), getPackage: jest.fn(), createPackage: jest.fn(), updatePackage: jest.fn() } }));
const renderForm = (element: React.ReactElement) => render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><App>{element}</App></QueryClientProvider>);

describe('PackageForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getSubjects as jest.Mock).mockResolvedValue([{ id: 'link-1', subjectId: 'sub-1', name: 'Toán học', category: 'HIGH_SCHOOL' }]);
    (teacherApi.getPackage as jest.Mock).mockResolvedValue({ id: 'pkg-1', name: 'Toán cơ bản', subjectId: 'sub-1', priceVnd: 500000, totalSessions: 10, sessionDurationMinutes: 60, durationDays: 60, description: 'Học cơ bản', status: 'DRAFT', version: 6 });
  });

  it('renders the create form', async () => {
    renderForm(<PackageForm mode="create" onSave={jest.fn()} onCancel={jest.fn()} />);
    expect(await screen.findByText('Tạo gói học')).toBeInTheDocument();
    expect(screen.getByLabelText('Tên gói học')).toBeInTheDocument();
  });

  it('loads the package in edit mode', async () => {
    renderForm(<PackageForm mode="edit" packageId="pkg-1" onSave={jest.fn()} onCancel={jest.fn()} />);
    expect(await screen.findByDisplayValue('Toán cơ bản')).toBeInTheDocument();
    expect(screen.getByText('Chỉnh sửa gói học')).toBeInTheDocument();
  });

  it('sends the loaded package version when editing', async () => {
    (teacherApi.updatePackage as jest.Mock).mockResolvedValue({});
    renderForm(<PackageForm mode="edit" packageId="pkg-1" onSave={jest.fn()} onCancel={jest.fn()} />);
    await screen.findByDisplayValue('Toán cơ bản');

    fireEvent.click(screen.getByRole('button', { name: 'Lưu gói học' }));

    await waitFor(() => expect(teacherApi.updatePackage).toHaveBeenCalledWith(
      'pkg-1', expect.objectContaining({ version: 6 })));
  });
});
