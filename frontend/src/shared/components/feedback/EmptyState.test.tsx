import React from 'react';
import { render, screen } from '@testing-library/react';
import { EmptyState } from './EmptyState';

jest.mock('@ant-design/icons', () => ({
  InboxOutlined: () => <span data-testid="inbox-icon" />,
  SearchOutlined: () => <span data-testid="search-icon" />,
  AppstoreAddOutlined: () => <span data-testid="appstore-icon" />
}));

describe('EmptyState Component', () => {
  it('renders default variant (no-data) with text', () => {
    render(<EmptyState title="Không có dữ liệu" />);
    expect(screen.getByText('Không có dữ liệu')).toBeInTheDocument();
  });

  it('renders no-search variant with description', () => {
    render(<EmptyState variant="no-search" title="Không tìm thấy" description="Hãy thử lại với từ khóa khác" />);
    expect(screen.getByText('Không tìm thấy')).toBeInTheDocument();
    expect(screen.getByText('Hãy thử lại với từ khóa khác')).toBeInTheDocument();
  });

  it('renders first-use variant with action button', () => {
    render(
      <EmptyState 
        variant="first-use" 
        title="Chào mừng bạn" 
        action={<button>Bắt đầu</button>} 
      />
    );
    expect(screen.getByText('Bắt đầu')).toBeInTheDocument();
  });
});
