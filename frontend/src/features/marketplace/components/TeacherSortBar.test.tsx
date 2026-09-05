import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { TeacherSortBar } from './TeacherSortBar';

jest.mock('antd', () => {
  const original = jest.requireActual('antd');
  return {
    ...original,
    Select: Object.assign(({ value, onChange, 'data-testid': testId, options }: any) => (
      <select data-testid={testId || 'mock-select'} value={value} onChange={(e) => onChange?.(e.target.value)}>
        {options?.map((opt: any) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    ), { Option: ({ value, children }: any) => <option value={value}>{children}</option> })
  };
});

describe('TeacherSortBar', () => {
  it('renders total elements and calls onChange when sort changes', () => {
    const mockOnChange = jest.fn();
    render(<TeacherSortBar totalElements={15} value="price_asc" onChange={mockOnChange} />);
    
    expect(screen.getByText('Tìm thấy 15 giáo viên')).toBeInTheDocument();
    
    const select = screen.getByTestId('sort-select');
    fireEvent.change(select, { target: { value: 'rating_desc' } });
    
    expect(mockOnChange).toHaveBeenCalledWith('rating_desc');
  });

  it('handles undefined value', () => {
    render(<TeacherSortBar totalElements={0} value={undefined} onChange={jest.fn()} />);
    expect(screen.getByText('Tìm thấy 0 giáo viên')).toBeInTheDocument();
    const select = screen.getByTestId('sort-select');
    expect(select).toHaveValue('');
  });
});
