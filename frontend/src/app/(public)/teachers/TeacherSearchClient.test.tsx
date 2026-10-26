import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import TeacherSearchClient from './TeacherSearchClient';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, refresh: jest.fn() }),
  usePathname: () => '/teachers',
  useSearchParams: () => new URLSearchParams('subjectId=subject-math&sort=rating_desc'),
}));

jest.mock('@/features/marketplace/components/TeacherFilterSidebar', () => ({
  TeacherFilterSidebar: () => <div />,
}));

jest.mock('@/features/marketplace/components/TeacherSortBar', () => ({
  TeacherSortBar: () => <div />,
}));

jest.mock('@/features/marketplace/components/TeacherSearchResults', () => ({
  TeacherSearchResults: () => <div />,
}));

describe('TeacherSearchClient', () => {
  beforeEach(() => mockPush.mockClear());

  it('changes to the selected page while preserving active query filters', () => {
    render(
      <TeacherSearchClient
        initialFilters={{ subjectId: 'subject-math', sort: 'rating_desc', page: 0, size: 12 }}
        initialTeachers={{ data: [], meta: { page: 0, size: 12, totalElements: 13, totalPages: 2 } }}
        subjects={[{ id: 'subject-math', code: 'MATH', name: 'Toán học', slug: 'toan-hoc', educationLevel: 'HIGH_SCHOOL', description: null }]}
      />,
    );

    fireEvent.click(screen.getByTitle('2'));

    expect(mockPush).toHaveBeenCalledWith('/teachers?subjectId=subject-math&sort=rating_desc&page=2');
  });
});
