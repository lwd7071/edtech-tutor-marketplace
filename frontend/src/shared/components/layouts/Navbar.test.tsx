import { render, screen } from '@testing-library/react';
import Navbar from './Navbar';
import { useAuthStore } from '@/features/auth';

// Mock Next.js router
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
  usePathname: () => '/',
}));

describe('Navbar Component', () => {
  beforeEach(() => {
    // Reset store before each test
    useAuthStore.setState({ user: null, isAuthenticated: false, accessToken: null });
  });

  it('renders guest links when not authenticated', () => {
    render(<Navbar />);
    
    // Should see Logo
    expect(screen.getByRole('link', { name: /tutor match/i })).toBeInTheDocument();
    
    // Should see Menu items
    expect(screen.getByText('Tìm gia sư')).toBeInTheDocument();
    
    // Should see Auth buttons
    expect(screen.getByRole('link', { name: /đăng nhập/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /bắt đầu học/i })).toBeInTheDocument();
  });

  it('renders user menu when authenticated', () => {
    useAuthStore.setState({ 
      user: { id: '1', email: 'test@example.com', fullName: 'John Doe', role: 'STUDENT', status: 'ACTIVE', avatarUrl: null },
      isAuthenticated: true 
    });

    render(<Navbar />);
    
    // Should NOT see Login/Register
    expect(screen.queryByRole('link', { name: /đăng nhập/i })).not.toBeInTheDocument();
    
    // Should see user name or avatar
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });
});
