import { render, screen } from '@testing-library/react';
import Sidebar from './Sidebar';
import { useAuthStore } from '@/features/auth';

jest.mock('next/navigation', () => ({
  usePathname: () => '/teacher/dashboard',
  useRouter: () => ({ push: jest.fn() }),
}));

describe('Sidebar Component', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, isAuthenticated: false });
  });

  it('renders student menu for STUDENT role', () => {
    useAuthStore.setState({ 
      user: { id: '1', email: 's@x.com', fullName: 'Student', role: 'STUDENT', status: 'ACTIVE', avatarUrl: null },
      isAuthenticated: true 
    });

    render(<Sidebar />);
    expect(screen.getByText('Tổng quan')).toBeInTheDocument();
    expect(screen.getByText('Lịch học')).toBeInTheDocument();
    expect(screen.getByText('Gói học')).toBeInTheDocument();
  });

  it('renders teacher menu and disables items if PENDING', () => {
    useAuthStore.setState({ 
      user: { id: '2', email: 't@x.com', fullName: 'Teacher', role: 'TEACHER', status: 'PENDING', avatarUrl: null },
      isAuthenticated: true 
    });

    render(<Sidebar />);
    expect(screen.getByText('Lịch dạy')).toBeInTheDocument();
    
    // Ant Design Menu items get 'ant-menu-item-disabled' class when disabled
    const packageMenuItem = screen.getByText('Gói học').closest('li');
    expect(packageMenuItem).toHaveClass('ant-menu-item-disabled');
  });
});
