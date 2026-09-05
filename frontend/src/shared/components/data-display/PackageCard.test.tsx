import { render, screen } from '@testing-library/react';
import PackageCard from './PackageCard';

const defaultProps = {
  id: 'p1',
  name: 'Khóa 10 buổi luyện thi',
  price: 2500000,
  sessionCount: 10,
  description: 'Khóa học tập trung giải đề',
  variant: 'public' as const,
  onClick: jest.fn(),
};

describe('PackageCard', () => {
  it('renders public variant correctly', () => {
    render(<PackageCard {...defaultProps} />);
    
    expect(screen.getByText('Khóa 10 buổi luyện thi')).toBeInTheDocument();
    expect(screen.getByText('Khóa học tập trung giải đề')).toBeInTheDocument();
    expect(screen.getByText(/2\.500\.000/)).toBeInTheDocument();
    expect(screen.getByText('10 buổi')).toBeInTheDocument();
  });

  it('renders purchased variant correctly with progress', () => {
    render(
      <PackageCard 
        {...defaultProps} 
        variant="purchased"
        completedSessions={3}
      />
    );
    
    expect(screen.getByText('Khóa 10 buổi luyện thi')).toBeInTheDocument();
    expect(screen.getByText('Đã học: 3/10 buổi')).toBeInTheDocument();
  });
});
