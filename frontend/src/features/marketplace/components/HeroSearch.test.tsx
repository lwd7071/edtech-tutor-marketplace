import React from 'react';
import { render, screen } from '@testing-library/react';
import HeroSearch from './HeroSearch';

describe('HeroSearch', () => {
  it('shows only a clear link to subject selection', () => {
    const { container } = render(<HeroSearch />);

    expect(screen.getByRole('link', { name: 'Tìm gia sư' })).toHaveAttribute('href', '/subjects');
    expect(container.querySelector('.tm-hero-cta-arrow')).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(container.querySelector('.tm-search-form')).not.toBeInTheDocument();
  });
});
