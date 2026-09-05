import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import HeroSearch from './HeroSearch';
import { useRouter } from 'next/navigation';

// Mock useRouter
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));

describe('HeroSearch', () => {
  const mockPush = jest.fn();

  beforeEach(() => {
    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
    });
    jest.clearAllMocks();
  });

  it('renders search input and button', () => {
    render(<HeroSearch />);
    
    expect(screen.getByPlaceholderText(/Tìm kiếm môn học, kỹ năng.../i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tìm gia sư/i })).toBeInTheDocument();
  });

  it('navigates to /teachers?keyword=... on submit', () => {
    render(<HeroSearch />);
    
    const input = screen.getByPlaceholderText(/Tìm kiếm môn học, kỹ năng.../i);
    const button = screen.getByRole('button', { name: /Tìm gia sư/i });

    fireEvent.change(input, { target: { value: 'Toán học' } });
    fireEvent.click(button);

    expect(mockPush).toHaveBeenCalledWith('/teachers?keyword=To%C3%A1n%20h%E1%BB%8Dc');
  });

  it('does not navigate if input is empty', () => {
    render(<HeroSearch />);
    
    const button = screen.getByRole('button', { name: /Tìm gia sư/i });
    fireEvent.click(button);

    expect(mockPush).not.toHaveBeenCalled();
  });
});
