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
    locationAddress: 'Hanoi',
    subjects: ['Toán', 'Lý'],
    averageRating: 4.8,
    reviewCount: 120
  };

  it('renders teacher information correctly', () => {
    render(<TeacherProfileHeader teacher={defaultTeacher} />);
    
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('A passionate math teacher.')).toBeInTheDocument();
    expect(screen.getByText('5 năm kinh nghiệm')).toBeInTheDocument();
    expect(screen.getByText('Toán, Lý')).toBeInTheDocument();
    expect(screen.getByText('Hanoi')).toBeInTheDocument();
  });

  it('renders CTAs', () => {
    render(<TeacherProfileHeader teacher={defaultTeacher} />);
    
    expect(screen.getByRole('button', { name: /Mua gói/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Yêu cầu học thử/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Nhắn tin/i })).toBeInTheDocument();
  });
});
