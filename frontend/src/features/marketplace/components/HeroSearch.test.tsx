import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import HeroSearch from './HeroSearch';
import { useRouter } from 'next/navigation';

// Mock useRouter
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));
jest.mock('antd', () => {
  const original = jest.requireActual('antd');
  return {
    ...original,
    Select: ({ id, value, onChange, options, placeholder }: any) => <select id={id} aria-label={placeholder} value={value || ''} onChange={event => onChange?.(event.target.value || undefined)}><option value="">{placeholder}</option>{options.map((option: any) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>,
  };
});

describe('HeroSearch', () => {
  const mockPush = jest.fn();

  beforeEach(() => {
    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
    });
    jest.clearAllMocks();
  });

  it('renders search input and button', () => {
    render(<HeroSearch subjects={[{ id: 'math', name: 'Toán học' }]} />);
    
    expect(screen.getByLabelText('Tất cả môn học')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tìm gia sư/i })).toBeInTheDocument();
  });

  it('navigates to /teachers?keyword=... on submit', () => {
    render(<HeroSearch subjects={[{ id: 'math', name: 'Toán học' }]} />);
    
    const input = screen.getByLabelText('Tất cả môn học');
    const button = screen.getByRole('button', { name: /Tìm gia sư/i });

    fireEvent.change(input, { target: { value: 'math' } });
    fireEvent.click(button);

    expect(mockPush).toHaveBeenCalledWith('/teachers?subjectId=math');
  });

  it('opens the full tutor list if no filter is selected', () => {
    render(<HeroSearch />);
    
    const button = screen.getByRole('button', { name: /Tìm gia sư/i });
    fireEvent.click(button);

    expect(mockPush).toHaveBeenCalledWith('/teachers');
  });
});
