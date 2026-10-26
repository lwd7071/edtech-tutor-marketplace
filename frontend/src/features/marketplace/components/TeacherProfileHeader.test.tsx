import React from 'react';
import { render, screen } from '@testing-library/react';
import { TeacherProfileHeader } from './TeacherProfileHeader';

describe('TeacherProfileHeader', () => {
  const defaultTeacher = {
    id: '1',
    fullName: 'John Doe',
    avatarUrl: 'https://example.com/avatar.jpg',
    bio: 'A passionate math teacher.',
    yearsOfExperience: 5,
    languages: ['Tiếng Việt', 'English'],
    supportsOnline: true,
    supportsOffline: false,
    provinceName: 'Hà Nội',
    wardName: 'Phường Ba Đình',
    subjects: ['Toán', 'Lý'],
    averageRating: 4.8,
    reviewCount: 120,
    credentials: [{ id: 'credential-1', label: 'Bằng cử nhân Sư phạm' }],
  };

  it('renders teacher information correctly', () => {
    render(<TeacherProfileHeader teacher={defaultTeacher} />);
    
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('Kinh nghiệm')).toBeInTheDocument();
    expect(screen.getByText('5 năm')).toBeInTheDocument();
    expect(screen.getByText('Toán')).toBeInTheDocument();
    expect(screen.getByText('Lý')).toBeInTheDocument();
    expect(screen.getByText('Đã xác minh')).toBeInTheDocument();
    expect(screen.getByText('Bằng cử nhân Sư phạm')).toBeInTheDocument();
    expect(screen.getByText(/Hà Nội/)).toBeInTheDocument();
  });

  it('renders rating and teaching languages', () => {
    render(<TeacherProfileHeader teacher={defaultTeacher} />);
    
    expect(screen.getByText(/4.8/)).toBeInTheDocument();
    expect(screen.getByText('Ngôn ngữ giảng dạy')).toBeInTheDocument();
    expect(screen.getByText('Tiếng Việt, English')).toBeInTheDocument();
  });
});
