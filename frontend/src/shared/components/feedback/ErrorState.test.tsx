import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ErrorState } from './ErrorState';

describe('ErrorState Component', () => {
  it('renders default message', () => {
    render(<ErrorState />);
    expect(screen.getByText('Đã có lỗi xảy ra')).toBeInTheDocument();
  });

  it('hides technical error details by default', () => {
    const errorMsg = 'System exception: memory limit exceeded';
    render(<ErrorState error={new Error(errorMsg)} />);
    // Should not contain the exact technical string
    expect(screen.queryByText(errorMsg)).not.toBeInTheDocument();
  });

  it('renders custom title and message', () => {
    render(<ErrorState title="Lỗi tải trang" message="Vui lòng thử lại sau" />);
    expect(screen.getByText('Lỗi tải trang')).toBeInTheDocument();
    expect(screen.getByText('Vui lòng thử lại sau')).toBeInTheDocument();
  });

  it('calls onRetry when retry button is clicked', () => {
    const onRetryMock = jest.fn();
    render(<ErrorState onRetry={onRetryMock} />);
    
    const retryBtn = screen.getByRole('button', { name: /thử lại/i });
    fireEvent.click(retryBtn);
    expect(onRetryMock).toHaveBeenCalledTimes(1);
  });

  it('renders tracking ID if provided', () => {
    render(<ErrorState trackingId="REQ-12345" />);
    expect(screen.getByText('Tracking ID: REQ-12345')).toBeInTheDocument();
  });

  it('applies an optional presentation class to the inline state root', () => {
    const { container } = render(<ErrorState variant="inline" className="route-error" title="403" message="Access denied" />);
    expect(container.querySelector('.route-error')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '403', level: 2 })).toBeInTheDocument();
    expect(screen.getByText('Access denied')).toBeInTheDocument();
  });

  it('keeps the auth result status and layout when no presentation class is passed', () => {
    const { container } = render(<ErrorState variant="auth" title="403" />);
    expect(container.querySelector('.ant-result-403')).toBeInTheDocument();
  });
});
