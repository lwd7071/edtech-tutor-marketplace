import React from 'react';
import { render, screen } from '@testing-library/react';
import TrustSection from './TrustSection';

describe('TrustSection', () => {
  it('renders the 3 steps', () => {
    render(<TrustSection />);
    expect(screen.getByText(/Tìm kiếm dễ dàng/i)).toBeInTheDocument();
    expect(screen.getByText(/Kết nối nhanh chóng/i)).toBeInTheDocument();
    expect(screen.getByText(/Học tập hiệu quả/i)).toBeInTheDocument();
  });
});
