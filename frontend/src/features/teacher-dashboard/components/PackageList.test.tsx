import React from 'react';
import { render, screen, fireEvent, act, waitFor, within } from '@testing-library/react';
import { PackageList } from './PackageList';
import { teacherApi } from '@/shared/api/teacher';
import { message } from 'antd';

jest.mock('@/shared/api/teacher', () => ({
  teacherApi: {
    getPackages: jest.fn(),
    getProfile: jest.fn(),
    updatePackageStatus: jest.fn(),
  }
}));

jest.mock('antd', () => {
  const antd = jest.requireActual('antd');
  return {
    ...antd,
    message: {
      success: jest.fn(),
      error: jest.fn(),
    }
  };
});

describe('PackageList', () => {
  const mockPackages = [
    { id: '1', name: 'Toán Cơ Bản', priceVnd: 500000, sessionCount: 10, status: 'ACTIVE' },
    { id: '2', name: 'Toán Nâng Cao', priceVnd: 800000, sessionCount: 12, status: 'INACTIVE' }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getProfile as jest.Mock).mockResolvedValue({ approvalStatus: 'APPROVED' });
    (teacherApi.getPackages as jest.Mock).mockResolvedValue(mockPackages);
  });

  it('renders loading initially and then shows list', async () => {
    await act(async () => {
      render(<PackageList onEdit={() => {}} onCreate={() => {}} />);
    });
    
    expect(screen.getByText('Toán Cơ Bản')).toBeInTheDocument();
    expect(screen.getByText('Toán Nâng Cao')).toBeInTheDocument();
  });

  it('enables create button if teacher is APPROVED', async () => {
    const onCreate = jest.fn();
    await act(async () => {
      render(<PackageList onEdit={() => {}} onCreate={onCreate} />);
    });

    const createBtn = screen.getByRole('button', { name: /Tạo gói học/i });
    expect(createBtn).not.toBeDisabled();
    
    fireEvent.click(createBtn);
    expect(onCreate).toHaveBeenCalled();
  });

  it('disables create button if teacher is NOT APPROVED', async () => {
    (teacherApi.getProfile as jest.Mock).mockResolvedValue({ approvalStatus: 'PENDING_APPROVAL' });
    
    await act(async () => {
      render(<PackageList onEdit={() => {}} onCreate={() => {}} />);
    });

    const createBtn = screen.getByRole('button', { name: /Tạo gói học/i });
    expect(createBtn).toBeDisabled();
  });

  it('calls updatePackageStatus when toggling status', async () => {
    (teacherApi.updatePackageStatus as jest.Mock).mockResolvedValue({});
    
    await act(async () => {
      render(<PackageList onEdit={() => {}} onCreate={() => {}} />);
    });

    // Assume we have a button/switch for "Vô hiệu hóa" or "Kích hoạt"
    // The first row is ACTIVE, so it has a button to change to INACTIVE
    // We can find the row for "Toán Cơ Bản"
    const row = screen.getByText('Toán Cơ Bản').closest('tr');
    expect(row).toBeInTheDocument();
    
    const toggleBtn = within(row as HTMLElement).getByRole('switch');
    fireEvent.click(toggleBtn);

    await act(async () => {
      await Promise.resolve();
    });

    expect(teacherApi.updatePackageStatus).toHaveBeenCalledWith('1', 'INACTIVE');
    expect(message.success).toHaveBeenCalledWith('Cập nhật trạng thái thành công');
  });
});
