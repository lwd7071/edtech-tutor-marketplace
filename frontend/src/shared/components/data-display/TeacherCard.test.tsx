import { render, screen } from '@testing-library/react';
import TeacherCard from './TeacherCard';

// Setup default props
const defaultProps = {
  id: 't1',
  name: 'Nguyen Van A',
  avatarUrl: 'https://example.com/avatar.jpg',
  isVerified: true,
  subjects: ['Toán', 'Lý'],
  rating: 4.8,
  reviewCount: 120,
  lowestPrice: 150000,
  onClick: jest.fn(),
};

describe('TeacherCard', () => {
  it('renders correctly in full variant by default', () => {
    render(<TeacherCard {...defaultProps} />);
    
    // Name
    expect(screen.getByText('Nguyen Van A')).toBeInTheDocument();
    
    // Subjects
    expect(screen.getByText('Toán')).toBeInTheDocument();
    expect(screen.getByText('Lý')).toBeInTheDocument();
    
    // Rating & reviews
    expect(screen.getByText('4.8')).toBeInTheDocument();
    expect(screen.getByText('(120 đánh giá)')).toBeInTheDocument();
    
    // Price
    expect(screen.getByText(/150\.000/)).toBeInTheDocument();
  });

  it('renders correctly in compact variant', () => {
    render(<TeacherCard {...defaultProps} variant="compact" />);
    
    // Name
    expect(screen.getByText('Nguyen Van A')).toBeInTheDocument();
    
    // In compact mode, we might only show 1 subject or abbreviate
    expect(screen.getByText('Toán')).toBeInTheDocument();
    expect(screen.getByText('Lý')).toBeInTheDocument();
    
    // Rating
    expect(screen.getByText('4.8')).toBeInTheDocument();
  });

  it('handles empty subjects gracefully', () => {
    render(<TeacherCard {...defaultProps} subjects={[]} />);
    expect(screen.getByText('Chưa cập nhật môn học')).toBeInTheDocument();
  });
});
