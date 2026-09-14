import React from 'react';
import { render, screen } from '@testing-library/react';
import TeacherDetailPage from './page';
import { getTeacherDetailServer, getTeacherPackagesServer, getTeacherAvailabilityServer, getTeacherReviewsServer, getPublicSubjectsServer } from '@/shared/api/public.server';

jest.mock('@/shared/api/public.server', () => ({
  getTeacherDetailServer: jest.fn(),
  getTeacherPackagesServer: jest.fn(),
  getTeacherAvailabilityServer: jest.fn(),
  getTeacherReviewsServer: jest.fn(),
  getPublicSubjectsServer: jest.fn(),
}));

jest.mock('@/features/marketplace/components/TeacherProfileHeader', () => ({
  TeacherProfileHeader: () => <div data-testid="teacher-profile-header" />
}));

jest.mock('./TeacherDetailClient', () => ({
  __esModule: true,
  default: () => <div data-testid="teacher-detail-client" />,
}));

jest.mock('@/features/marketplace/components/TeacherPackagesTab', () => ({
  TeacherPackagesTab: () => <div data-testid="teacher-packages-tab" />
}));

jest.mock('@/features/marketplace/components/TeacherReviewsTab', () => ({
  TeacherReviewsTab: () => <div data-testid="teacher-reviews-tab" />
}));

jest.mock('@/shared/components/data-display/WeeklyScheduleGrid', () => ({
  WeeklyScheduleGrid: () => <div data-testid="weekly-schedule-grid" />
}));

// Mock window.matchMedia for Ant Design Tabs
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(), // Deprecated
    removeListener: jest.fn(), // Deprecated
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn() }),
  usePathname: () => '/teachers/1',
  useSearchParams: () => new URLSearchParams(),
  notFound: jest.fn(),
}));

describe('TeacherDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders correctly and calls APIs', async () => {
    const teacherId = 'c0000000-0000-0000-0000-000000000001';
    (getTeacherDetailServer as jest.Mock).mockResolvedValue({ id: teacherId, fullName: 'John Doe', bio: 'Bio', subjects: [] });
    (getTeacherPackagesServer as jest.Mock).mockResolvedValue({ data: [], meta: {} });
    (getTeacherAvailabilityServer as jest.Mock).mockResolvedValue([]);
    (getTeacherReviewsServer as jest.Mock).mockResolvedValue({ data: [], meta: {} });
    (getPublicSubjectsServer as jest.Mock).mockResolvedValue({ data: [], meta: {} });

    // Server Component call
    const PageComponent = await TeacherDetailPage({ params: Promise.resolve({ id: teacherId }) });
    render(PageComponent);

    expect(screen.getByTestId('teacher-profile-header')).toBeInTheDocument();
    expect(screen.getByTestId('teacher-detail-client')).toBeInTheDocument();
    
    expect(getTeacherDetailServer).toHaveBeenCalledWith(teacherId);
    expect(getTeacherPackagesServer).toHaveBeenCalledWith(teacherId, 0, 6);
    expect(getTeacherAvailabilityServer).toHaveBeenCalledWith(teacherId);
    expect(getTeacherReviewsServer).toHaveBeenCalledWith(teacherId, 0, 10);
    expect(getPublicSubjectsServer).toHaveBeenCalledWith({ size: 100 });
  });
});

