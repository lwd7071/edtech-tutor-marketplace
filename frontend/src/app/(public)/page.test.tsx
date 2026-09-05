import React from 'react';
import { render, screen } from '@testing-library/react';
import LandingPage from './page';
import { getPublicSubjects, getPublicTeachers } from '@/shared/api/public';

jest.mock('@/shared/api/public', () => ({
  getPublicSubjects: jest.fn(),
  getPublicTeachers: jest.fn(),
}));

jest.mock('@/features/marketplace/components/HeroSearch', () => () => <div data-testid="hero-search" />);
jest.mock('@/features/marketplace/components/SubjectGrid', () => () => <div data-testid="subject-grid" />);
jest.mock('@/features/marketplace/components/TeacherGrid', () => () => <div data-testid="teacher-grid" />);
jest.mock('@/features/marketplace/components/TrustSection', () => () => <div data-testid="trust-section" />);
jest.mock('@/features/marketplace/components/CTASection', () => () => <div data-testid="cta-section" />);

describe('LandingPage', () => {
  it('renders all sections and fetches data', async () => {
    (getPublicSubjects as jest.Mock).mockResolvedValue({ data: [] });
    (getPublicTeachers as jest.Mock).mockResolvedValue({ data: [] });

    const Page = await LandingPage();
    render(Page);

    expect(screen.getByText('Học tập dễ dàng cùng chuyên gia')).toBeInTheDocument();
    expect(screen.getByTestId('hero-search')).toBeInTheDocument();
    expect(screen.getByTestId('subject-grid')).toBeInTheDocument();
    expect(screen.getByTestId('teacher-grid')).toBeInTheDocument();
    expect(screen.getByTestId('trust-section')).toBeInTheDocument();
    expect(screen.getByTestId('cta-section')).toBeInTheDocument();

    expect(getPublicSubjects).toHaveBeenCalled();
    expect(getPublicTeachers).toHaveBeenCalled();
  });
});
