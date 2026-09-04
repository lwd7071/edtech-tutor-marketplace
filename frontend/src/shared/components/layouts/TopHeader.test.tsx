import { render, screen } from '@testing-library/react';
import TopHeader from './TopHeader';
import { useAuthStore } from '@/features/auth';

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
  usePathname: () => '/teacher/dashboard',
}));

describe('TopHeader Component', () => {
  beforeEach(() => {
    useAuthStore.setState({ 
      user: { id: '2', email: 't@x.com', fullName: 'Teacher', role: 'TEACHER', status: 'ACTIVE', avatarUrl: null },
      isAuthenticated: true 
    });
  });

  it('renders user information', () => {
    render(<TopHeader onMenuClick={() => {}} />);
    expect(screen.getByText('Teacher')).toBeInTheDocument();
  });
});
