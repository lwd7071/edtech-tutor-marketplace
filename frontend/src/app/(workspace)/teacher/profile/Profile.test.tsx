import React from 'react';
import { render } from '@testing-library/react';
import ProfilePage from './page';

jest.mock('@/features/teacher-dashboard/components/TeacherProfileForm', () => ({
  TeacherProfileForm: () => <div data-testid="mock-form">TeacherProfileForm</div>
}));

describe('Teacher Profile Page', () => {
  it('renders TeacherProfileForm', () => {
    const { getByTestId } = render(<ProfilePage />);
    expect(getByTestId('mock-form')).toBeInTheDocument();
  });
});
