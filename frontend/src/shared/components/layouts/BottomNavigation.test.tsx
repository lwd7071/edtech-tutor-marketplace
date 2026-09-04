import { render, screen } from '@testing-library/react';
import BottomNavigation from './BottomNavigation';
import { useAuthStore } from '@/features/auth';

jest.mock('next/navigation', () => ({
  usePathname: () => '/student/dashboard',
  useRouter: () => ({ push: jest.fn() }),
}));

describe('BottomNavigation Component', () => {
  beforeEach(() => {
    useAuthStore.setState({ 
      user: { id: '1', email: 's@x.com', fullName: 'Student', role: 'STUDENT', status: 'ACTIVE', avatarUrl: null },
      isAuthenticated: true 
    });
  });

  it('renders student bottom nav items', () => {
    render(<BottomNavigation />);
    expect(screen.getByText('Tổng quan')).toBeInTheDocument();
    expect(screen.getByText('Lịch học')).toBeInTheDocument();
  });
});
