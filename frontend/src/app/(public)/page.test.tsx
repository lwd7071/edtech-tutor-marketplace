import React from 'react';
import { render, screen } from '@testing-library/react';
import LandingPage from './page';
import { getPublicSubjectsServer, getPublicTeachersServer } from '@/shared/api/public.server';

jest.mock('@/shared/api/public.server', () => ({
  getPublicSubjectsServer: jest.fn(),
  getPublicTeachersServer: jest.fn(),
}));

jest.mock('@/features/marketplace/components/HeroSearch', () => {
  const MockHeroSearch = () => <div data-testid="hero-search" />;
  MockHeroSearch.displayName = 'MockHeroSearch';
  return MockHeroSearch;
});
jest.mock('@/features/marketplace/components/SubjectGrid', () => {
  const MockSubjectGrid = () => <div data-testid="subject-grid" />;
  MockSubjectGrid.displayName = 'MockSubjectGrid';
  return MockSubjectGrid;
});
jest.mock('@/features/marketplace/components/TeacherGrid', () => {
  const MockTeacherGrid = () => <div data-testid="teacher-grid" />;
  MockTeacherGrid.displayName = 'MockTeacherGrid';
  return MockTeacherGrid;
});
jest.mock('@/features/marketplace/components/PublicDataRecovery', () => {
  const MockPublicDataRecovery = ({ hasError }: { hasError: boolean }) => <div data-testid="public-data-recovery" data-error={hasError} />;
  MockPublicDataRecovery.displayName = 'MockPublicDataRecovery';
  return MockPublicDataRecovery;
});
jest.mock('@/features/marketplace/components/TrustSection', () => {
  const MockTrustSection = () => <div data-testid="trust-section" />;
  MockTrustSection.displayName = 'MockTrustSection';
  return MockTrustSection;
});
jest.mock('@/features/marketplace/components/CTASection', () => {
  const MockCTASection = () => <div data-testid="cta-section" />;
  MockCTASection.displayName = 'MockCTASection';
  return MockCTASection;
});
describe('LandingPage', () => {
  it('renders all sections and fetches data', async () => {
    (getPublicSubjectsServer as jest.Mock).mockResolvedValue({ data: [], meta: {} });
    (getPublicTeachersServer as jest.Mock).mockResolvedValue({ data: [], meta: {} });

    const Page = await LandingPage();
    const { container } = render(Page);

    expect(screen.getByRole('heading', { name: /Học đúng người\. Tiến đúng mục tiêu\./i })).toBeInTheDocument();
    expect(screen.getByRole('main')).toHaveClass('tm-home-page');
    expect(screen.getByTestId('hero-search')).toBeInTheDocument();
    expect(screen.getByTestId('subject-grid')).toBeInTheDocument();
    expect(screen.getByTestId('teacher-grid')).toBeInTheDocument();
    expect(screen.getByTestId('public-data-recovery')).toHaveAttribute('data-error', 'false');
    expect(screen.getByRole('heading', { name: 'Cách hoạt động' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Đăng ký làm gia sư/i })).toHaveAttribute('href', '/become-a-tutor');
    expect(screen.getByRole('heading', { name: 'Một người đồng hành phù hợp tạo nên khác biệt.' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Khám phá các môn học phổ biến' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Trở thành gia sư' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Học viên viết bài trong buổi học trực tuyến cùng gia sư' })).toBeInTheDocument();
    const sections = [...document.querySelectorAll('section')];
    expect(sections.findIndex(section => section.getAttribute('aria-labelledby') === 'home-subjects-title'))
      .toBeLessThan(sections.findIndex(section => section.getAttribute('aria-labelledby') === 'home-story-title'));
    expect(screen.getByRole('img', { name: 'Học viên trao đổi bài cùng gia sư trong lớp học trực tuyến' }).getAttribute('src')).toContain('tutor-learning-illustration.png');
    expect(container.querySelector('.tm-home-tutor-art')).toHaveAttribute('alt', '');
    expect(screen.getByRole('link', { name: 'Tất cả môn học' })).toHaveAttribute('href', '/subjects');
    expect(screen.getByRole('link', { name: 'Tất cả gia sư' })).toHaveAttribute('href', '/teachers');

    expect(getPublicSubjectsServer).toHaveBeenCalledWith({ page: 0, size: 9 });
    expect(getPublicTeachersServer).toHaveBeenCalledWith({ sort: 'rating_desc', page: 0, size: 6 });
  });

  it('enables client recovery when a server-side public request fails', async () => {
    (getPublicSubjectsServer as jest.Mock).mockRejectedValue(new Error('backend unavailable'));
    (getPublicTeachersServer as jest.Mock).mockResolvedValue({ data: [], meta: {} });

    const Page = await LandingPage();
    render(Page);

    expect(screen.getByTestId('public-data-recovery')).toHaveAttribute('data-error', 'true');
  });
});
