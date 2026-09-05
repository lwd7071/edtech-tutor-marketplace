import React from 'react';
import { render, screen } from '@testing-library/react';
import { RatingStars } from './RatingStars';

describe('RatingStars Component', () => {
  it('renders readonly correctly', () => {
    const { container } = render(<RatingStars value={4.5} readonly />);
    // Rate component renders an unordered list
    const ul = container.querySelector('.ant-rate');
    expect(ul).toBeInTheDocument();
    expect(ul).toHaveClass('ant-rate-disabled'); // readonly makes it disabled in antd
  });

  it('renders interactive correctly', () => {
    const { container } = render(<RatingStars value={3} />);
    const ul = container.querySelector('.ant-rate');
    expect(ul).toBeInTheDocument();
    expect(ul).not.toHaveClass('ant-rate-disabled');
  });
});
