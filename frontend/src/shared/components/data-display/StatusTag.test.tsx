import React from 'react';
import { render, screen } from '@testing-library/react';
import { StatusTag } from './StatusTag';

describe('StatusTag Component', () => {
  it('renders TeacherProfile APPROVED status as success', () => {
    render(<StatusTag domain="TeacherProfile" status="APPROVED" />);
    const tag = screen.getByText('Đã duyệt');
    expect(tag).toBeInTheDocument();
    expect(tag).toHaveClass('ant-tag-success');
  });

  it('renders Invoice PENDING status as warning', () => {
    render(<StatusTag domain="Invoice" status="PENDING" />);
    const tag = screen.getByText('Đang xử lý');
    expect(tag).toBeInTheDocument();
    expect(tag).toHaveClass('ant-tag-warning');
  });

  it('renders unknown status with default neutral state', () => {
    render(<StatusTag domain="TeacherProfile" status="UNKNOWN_STATUS" />);
    const tag = screen.getByText('UNKNOWN_STATUS');
    expect(tag).toBeInTheDocument();
    expect(tag).toHaveClass('ant-tag-default');
  });
});
