import React from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { TeacherPublicDetail } from '@/shared/api/public';
import TeacherDetailClient from './TeacherDetailClient';

const mockPush = jest.fn();
const mockRefresh = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, refresh: mockRefresh }),
  usePathname: () => '/teachers/22222222-2222-4222-8222-222222222222',
  useSearchParams: () => new URLSearchParams('campaign=public'),
}));

jest.mock('@/features/marketplace/components/TeacherPackagesTab', () => ({
  TeacherPackagesTab: ({ packages }: { packages: unknown[] }) => <div data-testid="package-list">{packages.length}</div>,
}));

jest.mock('@/features/marketplace/components/TrialRequestForm', () => ({
  __esModule: true,
  default: () => <div data-testid="trial-request-form" />,
}));

jest.mock('@/features/chat/components/MessageTeacherButton', () => ({
  MessageTeacherButton: () => <button type="button">Nhắn tin</button>,
}));

const teacher: TeacherPublicDetail = {
  id: '22222222-2222-4222-8222-222222222222',
  fullName: 'Nguyễn An',
  bio: 'Giới thiệu gia sư.',
  yearsOfExperience: 5,
  languages: ['Tiếng Việt'],
  supportsOnline: true,
  supportsOffline: false,
  subjects: ['Toán'],
  averageRating: 4.8,
  reviewCount: 11,
};

function renderPage({ errors = [], reviewCount = 11 }: { errors?: string[]; reviewCount?: number } = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <TeacherDetailClient
        teacher={teacher}
        packages={{
          data: [],
          meta: { page: 0, size: 6, totalElements: 13, totalPages: 3 },
        }}
        availability={[]}
        reviews={{
          data: reviewCount ? [{ id: 'review-1', rating: 5, createdAt: '2026-10-01T10:00:00Z', reviewerName: 'Học viên', comment: 'Dễ hiểu.' }] : [],
          meta: { page: 0, size: 10, totalElements: reviewCount, totalPages: reviewCount > 10 ? 2 : 1 },
        }}
        subjectOptions={[]}
        errors={errors}
      />
    </QueryClientProvider>,
  );
}

describe('TeacherDetailClient', () => {
  beforeEach(() => {
    mockPush.mockClear();
    mockRefresh.mockClear();
  });

  it('keeps the current query while paging packages and reviews', () => {
    renderPage();

    fireEvent.click(within(screen.getByRole('navigation', { name: 'Trang gói học' })).getByTitle('2'));
    expect(mockPush).toHaveBeenLastCalledWith(
      '/teachers/22222222-2222-4222-8222-222222222222?campaign=public&packagesPage=2#packages',
      { scroll: false },
    );

    fireEvent.click(within(screen.getByRole('navigation', { name: 'Trang đánh giá' })).getByTitle('2'));
    expect(mockPush).toHaveBeenLastCalledWith(
      '/teachers/22222222-2222-4222-8222-222222222222?campaign=public&reviewsPage=2#reviews',
      { scroll: false },
    );
  });

  it('keeps section navigation and separate empty messages for availability and reviews', () => {
    renderPage({ reviewCount: 0 });

    expect(screen.getByRole('link', { name: 'Gói học' })).toHaveAttribute('href', '#packages');
    expect(screen.getByRole('link', { name: 'Lịch rảnh' })).toHaveAttribute('href', '#availability');
    expect(screen.getByRole('link', { name: 'Đánh giá' })).toHaveAttribute('href', '#reviews');
    expect(screen.getByText('Gia sư chưa cập nhật lịch rảnh.')).toBeInTheDocument();
    expect(screen.getByText('Chưa có đánh giá.')).toBeInTheDocument();
  });

  it('shows section errors independently and retries through the existing router refresh', () => {
    renderPage({ errors: ['packages', 'reviews'] });

    expect(screen.getAllByText('Chưa tải được dữ liệu')).toHaveLength(2);
    fireEvent.click(screen.getAllByRole('button', { name: 'Thử lại' })[1]);
    expect(mockRefresh).toHaveBeenCalledTimes(1);
  });
});
