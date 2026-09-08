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
        <div>Nội dung học viên</div>
      </StudentAppLayout>
    );

    expect(screen.getByRole('link', { name: /tutor match/i })).toBeInTheDocument();
    expect(screen.getAllByText('Gói học').length).toBeGreaterThan(0);
    expect(screen.getByText('Lịch học')).toBeInTheDocument();
    expect(screen.getByText('Bài tập')).toBeInTheDocument();
    expect(screen.getByText('Nội dung học viên')).toBeInTheDocument();
  });
});
