import React from 'react';
import { render, screen } from '@testing-library/react';
import { StudentAppLayout } from './StudentAppLayout';

// Mock next/navigation usePathname
jest.mock('next/navigation', () => ({
  usePathname: () => '/student/packages',
}));

describe('StudentAppLayout (TDD)', () => {
  it('should render sidebar navigation links for student zone', () => {
    render(
      <StudentAppLayout>
        <div>Content học sinh</div>
      </StudentAppLayout>
    );

    expect(screen.getByText('EdTech Student')).toBeInTheDocument();
    expect(screen.getByText('Gói học của tôi')).toBeInTheDocument();
    expect(screen.getByText('Lịch học')).toBeInTheDocument();
    expect(screen.getByText('Bài tập')).toBeInTheDocument();
    expect(screen.getByText('Content học sinh')).toBeInTheDocument();
  });
});
