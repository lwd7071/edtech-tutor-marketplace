import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { TeacherProfileForm } from './TeacherProfileForm';
import { teacherApi } from '@/shared/api/teacher';
import { message } from 'antd';

jest.mock('@/shared/api/teacher', () => ({
  teacherApi: {
    getProfile: jest.fn(),
    updateProfile: jest.fn(),
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

describe('TeacherProfileForm', () => {
  const mockProfile = {
    bio: 'Experienced math teacher',
    experience: '5 years teaching high school math',
    education: 'BSc Mathematics',
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getProfile as jest.Mock).mockResolvedValue(mockProfile);
  });

  it('renders loading state initially and then the form', async () => {
    await act(async () => {
      render(<TeacherProfileForm />);
    });
    
    const bioInput = await screen.findByLabelText(/Tiểu sử/i);
    expect(bioInput).toHaveValue('Experienced math teacher');
  });

  it('calls updateProfile when form is submitted', async () => {
    (teacherApi.updateProfile as jest.Mock).mockResolvedValue({});
    
    await act(async () => {
      render(<TeacherProfileForm />);
    });
    
    const bioInput = await screen.findByLabelText(/Tiểu sử/i);
    expect(bioInput).toHaveValue('Experienced math teacher');

    fireEvent.change(screen.getByLabelText(/Kinh nghiệm/i), { target: { value: '6 years teaching' } });
    fireEvent.click(screen.getByRole('button', { name: /Lưu thay đổi/i }));

    await act(async () => {
      await Promise.resolve();
    });

    expect(teacherApi.updateProfile).toHaveBeenCalledWith(expect.objectContaining({
      bio: 'Experienced math teacher',
      experience: '6 years teaching',
      education: 'BSc Mathematics',
    }));
    expect(message.success).toHaveBeenCalledWith('Cập nhật hồ sơ thành công');
  });
});
