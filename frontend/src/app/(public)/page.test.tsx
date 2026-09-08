import React from 'react';
import { render, screen } from '@testing-library/react';
import LandingPage from './page';
import { getPublicSubjects, getPublicTeachers } from '@/shared/api/public';

jest.mock('@/shared/api/public', () => ({
  getPublicSubjects: jest.fn(),
  getPublicTeachers: jest.fn(),
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
    (getPublicSubjects as jest.Mock).mockResolvedValue({ data: [] });
    (getPublicTeachers as jest.Mock).mockResolvedValue({ data: [] });

    const Page = await LandingPage();
    render(Page);

    expect(screen.getByText(/Một người hướng dẫn/)).toBeInTheDocument();
    expect(screen.getByTestId('hero-search')).toBeInTheDocument();
    expect(screen.getByTestId('subject-grid')).toBeInTheDocument();
    expect(screen.getByTestId('teacher-grid')).toBeInTheDocument();
    expect(screen.getByText('Một khởi đầu rõ ràng')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Trở thành gia sư/i })).toBeInTheDocument();

    expect(getPublicSubjects).toHaveBeenCalled();
    expect(getPublicTeachers).toHaveBeenCalled();
  });
});
