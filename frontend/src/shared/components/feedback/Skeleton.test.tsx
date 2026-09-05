import React from 'react';
import { render } from '@testing-library/react';
import { Skeleton } from './Skeleton';

describe('Skeleton Component', () => {
  it('renders text variant', () => {
    const { container } = render(<Skeleton variant="text" />);
    // Ant Design's Skeleton.Input or similar might be rendered, check base classes
    expect(container.querySelector('.ant-skeleton')).toBeInTheDocument();
  });

  it('renders card variant', () => {
    const { container } = render(<Skeleton variant="card" />);
    expect(container.querySelector('.ant-skeleton')).toBeInTheDocument();
  });

  it('renders table variant', () => {
    const { container } = render(<Skeleton variant="table" />);
    expect(container.querySelector('.ant-skeleton')).toBeInTheDocument();
  });

  it('renders avatar variant', () => {
    const { container } = render(<Skeleton variant="avatar" />);
    expect(container.querySelector('.ant-skeleton-avatar')).toBeInTheDocument();
  });
});
