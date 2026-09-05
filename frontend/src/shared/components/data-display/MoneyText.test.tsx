import React from 'react';
import { render, screen } from '@testing-library/react';
import { MoneyText } from './MoneyText';

describe('MoneyText Component', () => {
  it('renders normal variant correctly with dot separators', () => {
    render(<MoneyText amount={1250000} />);
    const el = screen.getByText('1.250.000 ₫');
    expect(el).toBeInTheDocument();
    expect(el).toHaveClass('tabular-nums');
  });

  it('renders positive variant with + prefix and success color', () => {
    render(<MoneyText amount={500000} variant="positive" />);
    const el = screen.getByText('+ 500.000 ₫');
    expect(el).toBeInTheDocument();
    expect(el).toHaveStyle({ color: 'var(--color-success-600)' });
  });

  it('renders negative variant with Unicode minus and error color', () => {
    render(<MoneyText amount={-500000} variant="negative" />);
    const el = screen.getByText('\u2212 500.000 ₫'); // U+2212
    expect(el).toBeInTheDocument();
    expect(el).toHaveStyle({ color: 'var(--color-error-600)' });
  });

  it('renders compact variant correctly', () => {
    render(<MoneyText amount={1500000} variant="compact" />);
    // compact will abbreviate based on scale. e.g. 1.5tr
    const el = screen.getByText('1,5tr ₫');
    expect(el).toBeInTheDocument();
  });
});
