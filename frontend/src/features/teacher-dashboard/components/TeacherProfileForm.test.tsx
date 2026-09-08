import React from 'react';
import { render, screen } from '@testing-library/react';
import { App } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TeacherProfileForm } from './TeacherProfileForm';
import { teacherApi } from '@/shared/api/teacher';

jest.mock('@/shared/api/teacher', () => ({ teacherApi: { getProfile: jest.fn(), updateProfile: jest.fn(), submitProfile: jest.fn() } }));
const renderForm = () => render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><App><TeacherProfileForm /></App></QueryClientProvider>);

describe('TeacherProfileForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getProfile as jest.Mock).mockResolvedValue({ bio: 'Dạy Toán theo mục tiêu', yearsOfExperience: 5, languages: ['Tiếng Việt'], supportsOnline: true, supportsOffline: false, approvalStatus: 'DRAFT' });
  });

  it('loads the tutor profile into the form', async () => {
    renderForm();
    expect(await screen.findByDisplayValue('Dạy Toán theo mục tiêu')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lưu hồ sơ' })).toBeInTheDocument();
  });
});
