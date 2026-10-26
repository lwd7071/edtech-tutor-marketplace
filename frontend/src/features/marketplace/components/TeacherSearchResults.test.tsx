import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { TeacherSearchResults } from './TeacherSearchResults';

jest.mock('next/link', () => {
  const MockLink = ({ children, href, ...props }: any) => <a href={href} {...props}>{children}</a>;
  MockLink.displayName = 'MockLink';
  return MockLink;
});

const teacher = {
  id: 'teacher-1', fullName: 'Nguyễn An', avatarUrl: null, bioExcerpt: 'Dạy Toán theo mục tiêu cá nhân.',
  yearsOfExperience: 8, verifiedBadge: true, supportsOnline: true, supportsOffline: false,
  subjects: [{ id: 'math', name: 'Toán' }], startingPriceVnd: 400000,
  averageRating: 4.9, bayesianRating: 4.8, reviewCount: 24, globalRank: 2,
  provinceName: 'Hà Nội', wardName: 'Cầu Giấy',
};

describe('TeacherSearchResults', () => {
  it('shows the quick profile on keyboard focus using the loaded teacher card', () => {
    const { container } = render(<TeacherSearchResults teachers={[teacher]} />);
    fireEvent.focus(screen.getByRole('button', { name: 'Xem nhanh Nguyễn An' }));

    expect(screen.getAllByText('Dạy Toán theo mục tiêu cá nhân.').length).toBeGreaterThan(1);
    expect(screen.getAllByText('Từ 400.000đ').length).toBeGreaterThan(0);
    expect(container.querySelector('.tm-search-preview-aside .tm-teacher-preview')).toBeInTheDocument();
  });

  it('opens the inline preview when a tutor card is tapped', () => {
    const { container } = render(<TeacherSearchResults teachers={[teacher]} />);
    fireEvent.click(screen.getByRole('button', { name: 'Xem nhanh Nguyễn An' }));

    expect(container.querySelector('.tm-teacher-preview-mobile')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: /Xem hồ sơ và lịch rảnh/i })[0]).toHaveAttribute('href', '/teachers/teacher-1');
  });

  it('renders the loading skeleton as a status and keeps the empty state distinct', () => {
    const { rerender } = render(<TeacherSearchResults teachers={[]} isLoading />);
    expect(screen.getByRole('status', { name: 'Đang tải danh sách gia sư' })).toBeInTheDocument();
    rerender(<TeacherSearchResults teachers={[]} />);
    expect(screen.getByRole('heading', { name: 'Chưa tìm thấy gia sư phù hợp' })).toBeInTheDocument();
  });

  it('exposes retry for an API error instead of presenting an empty list', () => {
    const onRetry = jest.fn();
    render(<TeacherSearchResults teachers={[]} isError onRetry={onRetry} />);
    fireEvent.click(screen.getByRole('button', { name: 'Tải lại' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('heading', { name: 'Chưa tìm thấy gia sư phù hợp' })).not.toBeInTheDocument();
  });
});
