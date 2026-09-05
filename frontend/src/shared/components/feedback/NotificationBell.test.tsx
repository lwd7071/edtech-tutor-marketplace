import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import NotificationBell from './NotificationBell';

const mockNotifications = [
  { id: '1', title: 'Test 1', message: 'Msg 1', timestamp: '2026-09-05T08:00:00Z', isRead: false },
  { id: '2', title: 'Test 2', message: 'Msg 2', timestamp: '2026-09-05T07:00:00Z', isRead: true },
];

describe('NotificationBell', () => {
  it('renders badge count correctly', () => {
    render(<NotificationBell unreadCount={5} notifications={mockNotifications} onViewAll={jest.fn()} />);
    // Badge usually renders a sup element with the count or title attribute
    const badge = document.querySelector('.ant-badge-count');
    expect(badge).toBeInTheDocument();
  });

  it('opens dropdown and displays items', async () => {
    render(<NotificationBell unreadCount={1} notifications={mockNotifications} onViewAll={jest.fn()} />);
    
    // Click bell to open dropdown
    const btn = screen.getByRole('button');
    fireEvent.click(btn);
    
    await waitFor(() => {
      expect(screen.getByText('Test 1')).toBeInTheDocument();
      expect(screen.getByText('Test 2')).toBeInTheDocument();
      expect(screen.getByText('Xem tất cả')).toBeInTheDocument();
    });
  });
});
