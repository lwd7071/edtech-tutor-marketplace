import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { TeacherFilterSidebar } from './TeacherFilterSidebar';

// We mock antd components to simplify testing
jest.mock('antd', () => {
  const original = jest.requireActual('antd');
  return {
    ...original,
    Select: Object.assign(({ placeholder, onChange, 'data-testid': testId, children }: any) => (
      <select data-testid={testId || 'mock-select'} onChange={(e) => onChange?.(e.target.value)}>
        <option value="">{placeholder}</option>
        {children}
      </select>
    ), { Option: ({ value, children }: any) => <option value={value}>{children}</option> }),
    Radio: Object.assign(({ children }: any) => <div>{children}</div>, {
      Group: ({ onChange, value, children }: any) => (
        <div data-testid="mock-radio-group" onChange={(e: any) => onChange?.({ target: { value: e.target.value } })}>
          {children}
        </div>
      )
    }),
    InputNumber: ({ placeholder, onChange, 'data-testid': testId }: any) => (
      <input type="number" data-testid={testId || 'mock-input-number'} placeholder={placeholder} onChange={(e) => onChange?.(Number(e.target.value))} />
    )
  };
});

describe('TeacherFilterSidebar', () => {
  const defaultProps = {
    filters: {
      keyword: '',
      subjectId: '',
      minPrice: undefined,
      maxPrice: undefined,
      minRating: undefined,
      deliveryMode: ''
    },
    subjects: [{ id: 'sub1', code: 'MATH', slug: 'toan', name: 'Toán', educationLevel: 'HIGH_SCHOOL', description: null }, { id: 'sub2', code: 'PHYSICS', slug: 'ly', name: 'Lý', educationLevel: 'HIGH_SCHOOL', description: null }],
    onChange: jest.fn(),
    onClear: jest.fn()
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders all filter sections', () => {
    render(<TeacherFilterSidebar {...defaultProps} />);
    expect(screen.getByText('Lọc kết quả')).toBeInTheDocument();
    expect(screen.getByText('Môn học')).toBeInTheDocument();
    expect(screen.getByText('Giá trọn gói (VNĐ)')).toBeInTheDocument();
    expect(screen.getByText('Hình thức')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Bộ lọc khác/i }));
    expect(screen.getByText('Đánh giá')).toBeInTheDocument();
  });

  it('calls onClear when clear button is clicked', () => {
    render(<TeacherFilterSidebar {...defaultProps} />);
    const clearBtn = screen.getByRole('button', { name: /Xóa bộ lọc/i });
    fireEvent.click(clearBtn);
    expect(defaultProps.onClear).toHaveBeenCalled();
  });
});
