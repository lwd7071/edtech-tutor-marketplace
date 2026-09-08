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
    expect(screen.getByText(/5 năm kinh nghiệm/)).toBeInTheDocument();
    expect(screen.getByText('Toán')).toBeInTheDocument();
    expect(screen.getByText('Lý')).toBeInTheDocument();
    expect(screen.getByText('Hanoi')).toBeInTheDocument();
  });

  it('renders rating and teaching languages', () => {
    render(<TeacherProfileHeader teacher={defaultTeacher} />);
    
    expect(screen.getByText(/4.8/)).toBeInTheDocument();
    expect(screen.getByText(/Ngôn ngữ: Tiếng Việt, English/)).toBeInTheDocument();
  });
});
