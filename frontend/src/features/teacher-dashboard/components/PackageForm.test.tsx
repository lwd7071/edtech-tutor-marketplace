import React from 'react';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import { PackageForm } from './PackageForm';
import { teacherApi } from '@/shared/api/teacher';
import { message } from 'antd';

jest.mock('@/shared/api/teacher', () => ({
  teacherApi: {
    getSubjects: jest.fn(),
    getPackages: jest.fn(),
    createPackage: jest.fn(),
    updatePackage: jest.fn(),
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

describe('PackageForm', () => {
  const mockSubjects = [
    { id: 'sub-1', name: 'Toán học', category: 'Toán' }
  ];
  
  const mockPackage = {
    id: 'pkg-1',
    name: 'Toán Cơ Bản',
    subjectId: 'sub-1',
    priceVnd: 500000,
    sessionCount: 10,
    durationMonths: 3,
    description: 'Học cơ bản',
    trialEnabled: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getSubjects as jest.Mock).mockResolvedValue(mockSubjects);
    (teacherApi.getPackages as jest.Mock).mockResolvedValue([mockPackage]);
  });

  it('renders create form correctly', async () => {
    await act(async () => {
      render(<PackageForm mode="create" onSave={() => {}} onCancel={() => {}} />);
    });
    
    expect(screen.getByText('Tạo Gói Học Mới')).toBeInTheDocument();
    expect(screen.getByLabelText(/Tên gói học/i)).toBeInTheDocument();
  });

  it('renders edit form correctly and populates data', async () => {
    await act(async () => {
      render(<PackageForm mode="edit" packageId="pkg-1" onSave={() => {}} onCancel={() => {}} />);
    });
    
    expect(screen.getByText('Sửa Gói Học')).toBeInTheDocument();
    
    const nameInput = await screen.findByLabelText(/Tên gói học/i);
    expect(nameInput).toHaveValue('Toán Cơ Bản');
    
    // In edit mode, price and session count should be disabled to prevent issues with purchased packages
    expect(screen.getByLabelText(/Giá tiền/i)).toBeDisabled();
    expect(screen.getByLabelText(/Số buổi/i)).toBeDisabled();
  });
});
