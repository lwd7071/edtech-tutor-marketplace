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
    render(Page);

    expect(screen.getByText(/Một người hướng dẫn/)).toBeInTheDocument();
    expect(screen.getByTestId('hero-search')).toBeInTheDocument();
    expect(screen.getByTestId('subject-grid')).toBeInTheDocument();
    expect(screen.getByTestId('teacher-grid')).toBeInTheDocument();
    expect(screen.getByTestId('public-data-recovery')).toHaveAttribute('data-error', 'false');
    expect(screen.getByText('Một khởi đầu rõ ràng')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Trở thành gia sư/i })).toBeInTheDocument();

    expect(getPublicSubjectsServer).toHaveBeenCalled();
    expect(getPublicTeachersServer).toHaveBeenCalled();
  });

  it('enables client recovery when a server-side public request fails', async () => {
    (getPublicSubjectsServer as jest.Mock).mockRejectedValue(new Error('backend unavailable'));
    (getPublicTeachersServer as jest.Mock).mockResolvedValue({ data: [], meta: {} });

    const Page = await LandingPage();
    render(Page);

    expect(screen.getByTestId('public-data-recovery')).toHaveAttribute('data-error', 'true');
  });
});
