import React from 'react';
import { render, screen } from '@testing-library/react';
import CTASection from './CTASection';

describe('CTASection', () => {
  it('renders correctly', () => {
    render(<CTASection />);
    expect(screen.getByText(/Bắt đầu hành trình học tập/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Đăng ký ngay/i })).toBeInTheDocument();
  });
});
