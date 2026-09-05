import { render, screen } from '@testing-library/react';
import NotificationItem from './NotificationItem';

const defaultProps = {
  id: 'n1',
  title: 'Có booking mới',
  message: 'Bạn nhận được một yêu cầu đặt lịch học mới.',
  timestamp: '2026-09-05T08:00:00Z', // Assume ISO string
  isRead: false,
  onClick: jest.fn(),
};

describe('NotificationItem', () => {
  it('renders correctly in unread state', () => {
    render(<NotificationItem {...defaultProps} />);
    
    expect(screen.getByText('Có booking mới')).toBeInTheDocument();
    expect(screen.getByText('Bạn nhận được một yêu cầu đặt lịch học mới.')).toBeInTheDocument();
    
    const wrapper = screen.getByText('Có booking mới').closest('div.notification-item');
    expect(wrapper!.className).toContain('unread');
  });

  it('renders correctly in read state', () => {
    render(<NotificationItem {...defaultProps} isRead={true} />);
    
    const wrapper = screen.getByText('Có booking mới').closest('div.notification-item');
    expect(wrapper!.className).toContain('read');
  });
});
