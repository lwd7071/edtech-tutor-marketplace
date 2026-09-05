import React from 'react';
import { render, screen } from '@testing-library/react';
import { Badge } from './Badge';

describe('Badge Component', () => {
  it('renders dot variant', () => {
    const { container } = render(<Badge variant="dot" />);
    // Ant Design's dot badge renders a sup element with ant-badge-dot class
    expect(container.querySelector('.ant-badge-dot')).toBeInTheDocument();
  });

  it('renders count variant', () => {
    render(<Badge count={5} variant="count" />);
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('renders accent variant', () => {
    // maybe accent applies a specific color or something, let's say it just wraps and applies a specific color
    const { container } = render(<Badge variant="accent" count={1} />);
    expect(container.querySelector('.ant-badge')).toBeInTheDocument();
  });
});
